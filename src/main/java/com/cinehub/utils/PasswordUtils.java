package com.cinehub.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Lớp tiện ích băm mật khẩu một chiều (PBKDF2 có salt; đọc được SHA-256 cũ của dữ liệu mẫu)
 * và kiểm tra tính hợp lệ dữ liệu đầu vào.
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.1
 */
public final class PasswordUtils {

    private static final String PBKDF2_PREFIX = "pbkdf2$";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    /**
     * Băm mật khẩu văn bản thuần sang chuỗi thập lục phân SHA-256 (64 ký tự).
     *
     * @param plainPassword Mật khẩu văn bản thuần
     * @return Chuỗi băm SHA-256 dạng chữ thường
     */
    public static String hashSha256(String plainPassword) {
        if (plainPassword == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không tìm thấy thuật toán SHA-256", ex);
        }
    }

    /**
     * Băm mật khẩu theo chuẩn PBKDF2-HMAC-SHA256 với salt ngẫu nhiên 16 byte cho từng tài khoản.
     * Định dạng lưu: {@code pbkdf2$<số vòng lặp>$<salt Base64>$<hash Base64>}.
     *
     * @param plainPassword Mật khẩu văn bản thuần
     * @return Chuỗi băm có salt
     */
    public static String hashPassword(String plainPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword == null ? "" : plainPassword, salt, PBKDF2_ITERATIONS);
        return PBKDF2_PREFIX + PBKDF2_ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Kiểm tra mật khẩu nhập vào có khớp với chuỗi băm lưu trong CSDL hay không.
     * Hỗ trợ cả định dạng PBKDF2 có salt và định dạng SHA-256 cũ (dữ liệu mẫu);
     * tài khoản định dạng cũ sẽ được nâng cấp ngay sau lần đăng nhập thành công ({@link #needsUpgrade}).
     *
     * @param plainPassword Mật khẩu người dùng nhập
     * @param storedHash    Chuỗi băm lưu trong CSDL
     * @return true nếu trùng khớp
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        String stored = storedHash.trim();
        if (stored.startsWith(PBKDF2_PREFIX)) {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                return MessageDigest.isEqual(expected, pbkdf2(plainPassword, salt, iterations));
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        byte[] computed = hashSha256(plainPassword).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(computed, stored.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Cho biết chuỗi băm còn ở định dạng SHA-256 cũ (không salt) và cần băm lại.
     *
     * @param storedHash Chuỗi băm lưu trong CSDL
     * @return true nếu cần nâng cấp
     */
    public static boolean needsUpgrade(String storedHash) {
        return storedHash != null && !storedHash.trim().startsWith(PBKDF2_PREFIX);
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Không khởi tạo được PBKDF2", ex);
        }
    }

    /**
     * Kiểm tra chuỗi có rỗng hoặc chỉ chứa khoảng trắng hay không.
     *
     * @param value Chuỗi cần kiểm tra
     * @return true nếu null hoặc rỗng
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

