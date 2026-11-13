package com.cinehub.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Lớp tiện ích hỗ trợ băm mật khẩu một chiều (SHA-256) và kiểm tra tính hợp lệ dữ liệu đầu vào.
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.0
 */
public final class PasswordUtils {

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
     * Kiểm tra mật khẩu nhập vào có khớp với chuỗi băm lưu trong CSDL hay không.
     *
     * @param plainPassword Mật khẩu người dùng nhập
     * @param storedHash    Chuỗi băm SHA-256 lưu trong CSDL
     * @return true nếu trùng khớp
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        String computed = hashSha256(plainPassword);
        return computed.equalsIgnoreCase(storedHash.trim());
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

