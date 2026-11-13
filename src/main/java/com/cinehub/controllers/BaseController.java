package com.cinehub.controllers;

import com.cinehub.utils.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp điều khiển cơ sở (BaseController) cung cấp các phương thức dùng chung cho tầng Controller
 * trong kiến trúc Web MVC của hệ thống CineHub.
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.0
 */
public abstract class BaseController {

    /**
     * Đọc nội dung văn bản UTF-8 từ thân yêu cầu HTTP (Request Body).
     *
     * @param exchange Đối tượng giao tiếp HTTP
     * @return Chuỗi văn bản UTF-8
     * @throws IOException Nếu gặp lỗi luồng vào/ra
     */
    protected String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * Phân tích chuỗi tham số truy vấn trên URL (Query String) thành bản đồ khóa - giá trị.
     *
     * @param exchange Đối tượng giao tiếp HTTP
     * @return Bản đồ tham số truy vấn
     */
    protected Map<String, String> parseQueryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, val);
            }
        }
        return params;
    }

    /**
     * Gửi phản hồi JSON chuẩn mã hóa UTF-8 về trình duyệt phía Client.
     *
     * @param exchange   Đối tượng giao tiếp HTTP
     * @param statusCode Mã trạng thái HTTP (200, 400, 401, 404, 500)
     * @param jsonBody   Chuỗi nội dung JSON
     * @throws IOException Nếu gặp lỗi ghi luồng phản hồi
     */
    protected void sendJson(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Gửi phản hồi lỗi nghiệp vụ dạng JSON.
     *
     * @param exchange   Đối tượng giao tiếp HTTP
     * @param statusCode Mã trạng thái HTTP
     * @param code       Mã lỗi nghiệp vụ
     * @param message    Thông báo lỗi tiếng Việt
     * @throws IOException Nếu gặp lỗi ghi luồng
     */
    protected void sendError(HttpExchange exchange, int statusCode, String code, String message) throws IOException {
        sendJson(exchange, statusCode, JsonUtils.statusResponse(false, code, message));
    }
}

