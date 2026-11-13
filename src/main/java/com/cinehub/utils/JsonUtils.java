package com.cinehub.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp tiện ích chuyển đổi và phân tích dữ liệu JSON nhẹ cho tầng giao tiếp RESTful API.
 * Đảm bảo ứng dụng Web MVC vận hành độc lập, nhanh chóng và an toàn ký tự tiếng Việt UTF-8.
 *
 * @author Lê Anh Tuấn (202416639)
 * @version 1.0
 */
public final class JsonUtils {

    private static final Pattern KV_STRING_PATTERN =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern KV_PRIMITIVE_PATTERN =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?|true|false|null)");
    private static final Pattern KV_ARRAY_PATTERN =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\[([^\\]]*)\\]");

    private JsonUtils() {
    }

    /**
     * Thoát các ký tự đặc biệt trong chuỗi để nhúng an toàn vào JSON.
     *
     * @param raw Chuỗi đầu vào
     * @return Chuỗi đã thoát ký tự đặc biệt JSON
     */
    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Tạo phản hồi JSON chuẩn cho thông báo trạng thái thành công hoặc lỗi.
     *
     * @param success Trạng thái thực thi
     * @param code    Mã phản hồi nghiệp vụ
     * @param message Thông điệp tiếng Việt hiển thị cho người dùng
     * @return Chuỗi JSON chuẩn
     */
    public static String statusResponse(boolean success, String code, String message) {
        return "{\"success\":" + success
                + ",\"code\":\"" + escape(code) + "\""
                + ",\"message\":\"" + escape(message) + "\"}";
    }

    /**
     * Phân tích thân yêu cầu JSON phẳng thành bản đồ cặp khóa - giá trị chuỗi.
     *
     * @param jsonBody Chuỗi JSON gửi từ trình duyệt
     * @return Bản đồ các trường dữ liệu
     */
    public static Map<String, String> parseFlatObject(String jsonBody) {
        Map<String, String> map = new HashMap<>();
        if (jsonBody == null || jsonBody.trim().isEmpty()) {
            return map;
        }
        Matcher strMatcher = KV_STRING_PATTERN.matcher(jsonBody);
        while (strMatcher.find()) {
            String key = strMatcher.group(1);
            String val = strMatcher.group(2)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\\\", "\\");
            map.put(key, val);
        }
        Matcher primMatcher = KV_PRIMITIVE_PATTERN.matcher(jsonBody);
        while (primMatcher.find()) {
            String key = primMatcher.group(1);
            if (!map.containsKey(key)) {
                String val = primMatcher.group(2);
                if (!"null".equals(val)) {
                    map.put(key, val);
                }
            }
        }
        Matcher arrMatcher = KV_ARRAY_PATTERN.matcher(jsonBody);
        while (arrMatcher.find()) {
            String key = arrMatcher.group(1);
            String val = arrMatcher.group(2).trim();
            map.put(key, val);
        }
        return map;
    }

    /**
     * Phân tích chuỗi danh sách số nguyên (ví dụ: "12, 13, 14") thành danh sách {@link Integer}.
     *
     * @param csv Chuỗi chứa các số nguyên phân tách bởi dấu phẩy
     * @return Danh sách số nguyên
     */
    public static List<Integer> parseIntList(String csv) {
        List<Integer> result = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) {
            return result;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            String cleaned = part.replace("\"", "").trim();
            if (!cleaned.isEmpty()) {
                try {
                    result.add(Integer.parseInt(cleaned));
                } catch (NumberFormatException ignored) {
                    // Bỏ qua phần tử không phải số nguyên
                }
            }
        }
        return result;
    }
}

