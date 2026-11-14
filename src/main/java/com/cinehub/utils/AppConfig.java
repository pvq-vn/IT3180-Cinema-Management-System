package com.cinehub.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Đọc cấu hình ứng dụng từ tập tin {@code config/db.properties} (nếu có).
 * Khi không có tập tin, mọi giá trị dùng mặc định: H2 in-memory, cổng 8080,
 * giữ ghế 5 phút (BR03), đệm dọn phòng 15 phút (BR01), phiên hết hạn sau 480 phút.
 * Mẫu cấu hình: {@code config/db.properties.example}.
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.1
 */
public final class AppConfig {

    private static final Properties PROPS = load();

    private AppConfig() {
    }

    private static Properties load() {
        Properties props = new Properties();
        for (Path p : new Path[]{Paths.get("config", "db.properties"),
                Paths.get("03-SourceCodes", "config", "db.properties")}) {
            if (Files.exists(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    props.load(in);
                    System.out.println("[CONFIG] Đã nạp cấu hình từ " + p.toAbsolutePath());
                } catch (IOException ex) {
                    System.err.println("[CONFIG] Không đọc được " + p + ": " + ex.getMessage());
                }
                break;
            }
        }
        return props;
    }

    /**
     * Lấy giá trị chuỗi; ưu tiên thuộc tính hệ thống (-Dkey=value), sau đó tập tin cấu hình.
     *
     * @param key          Khóa cấu hình
     * @param defaultValue Giá trị mặc định
     * @return Giá trị cấu hình
     */
    public static String get(String key, String defaultValue) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.trim().isEmpty()) {
            return sys.trim();
        }
        String val = PROPS.getProperty(key);
        return (val == null || val.trim().isEmpty()) ? defaultValue : val.trim();
    }

    /**
     * Lấy giá trị số nguyên dương; giá trị sai định dạng hoặc không dương sẽ dùng mặc định.
     *
     * @param key          Khóa cấu hình
     * @param defaultValue Giá trị mặc định
     * @return Giá trị cấu hình
     */
    public static int getInt(String key, int defaultValue) {
        try {
            int v = Integer.parseInt(get(key, String.valueOf(defaultValue)));
            return v > 0 ? v : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
