package com.cinehub.controllers;

import com.cinehub.models.User;
import com.cinehub.utils.JsonUtils;
import com.cinehub.utils.SessionManager;
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

    /** Vai trò Quản lý rạp. */
    protected static final String ROLE_MANAGER = "MANAGER";
    /** Vai trò Nhân viên quầy (POS, soát vé, F&amp;B). */
    protected static final String ROLE_STAFF = "STAFF";
    /** Vai trò Khách hàng thành viên. */
    protected static final String ROLE_CUSTOMER = "CUSTOMER";

    private static final String BEARER_PREFIX = "Bearer ";

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

    /**
     * Xử lý ngoại lệ thống nhất cho mọi controller:
     * dữ liệu sai (số sai định dạng, vi phạm ràng buộc nghiệp vụ) trả 400 kèm thông báo cho người dùng;
     * lỗi hệ thống trả 500 với thông báo chung, chi tiết chỉ ghi vào log máy chủ (không lộ ra trình duyệt).
     *
     * @param exchange Đối tượng giao tiếp HTTP
     * @param ex       Ngoại lệ phát sinh
     * @throws IOException Nếu gặp lỗi ghi luồng
     */
    protected void handleException(HttpExchange exchange, Exception ex) throws IOException {
        if (ex instanceof NumberFormatException) {
            sendError(exchange, 400, "VALIDATION_ERROR", "Dữ liệu số gửi lên không hợp lệ.");
        } else if (ex instanceof IllegalArgumentException) {
            sendError(exchange, 400, "VALIDATION_ERROR", ex.getMessage());
        } else {
            System.err.println("[ERROR] " + exchange.getRequestMethod() + " " + exchange.getRequestURI() + ": " + ex);
            ex.printStackTrace();
            sendError(exchange, 500, "SERVER_ERROR", "Lỗi hệ thống, vui lòng thử lại sau.");
        }
    }

    /**
     * Đọc mã phiên từ tiêu đề {@code Authorization: Bearer <token>} và tra cứu người dùng tương ứng.
     * Danh tính người dùng chỉ được xác định qua mã phiên do máy chủ cấp, không bao giờ lấy từ
     * thân yêu cầu (userId/staffId do trình duyệt gửi lên đều bị bỏ qua).
     *
     * @param exchange Đối tượng giao tiếp HTTP
     * @return Người dùng đang đăng nhập hoặc null nếu thiếu/sai mã phiên
     */
    protected User currentUser(HttpExchange exchange) {
        return SessionManager.getUserByToken(extractToken(exchange));
    }

    /**
     * Trích mã phiên từ tiêu đề Authorization (chấp nhận cả dạng có và không có tiền tố Bearer).
     *
     * @param exchange Đối tượng giao tiếp HTTP
     * @return Mã phiên hoặc null
     */
    protected String extractToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) {
            return null;
        }
        String value = header.trim();
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            value = value.substring(BEARER_PREFIX.length()).trim();
        }
        return value.isEmpty() ? null : value;
    }

    /**
     * Yêu cầu người gọi đã đăng nhập. Nếu chưa, gửi phản hồi 401 và trả về null.
     *
     * @param exchange Đối tượng giao tiếp HTTP
     * @return Người dùng đang đăng nhập hoặc null (khi đó phản hồi lỗi đã được gửi)
     * @throws IOException Nếu gặp lỗi ghi luồng
     */
    protected User requireLogin(HttpExchange exchange) throws IOException {
        User user = currentUser(exchange);
        if (user == null) {
            sendError(exchange, 401, "UNAUTHORIZED",
                    "Vui lòng đăng nhập để thực hiện thao tác này (phiên làm việc không hợp lệ hoặc đã hết hạn).");
        }
        return user;
    }

    /**
     * Yêu cầu người gọi đã đăng nhập và thuộc một trong các vai trò được phép (RBAC).
     * Gửi 401 nếu chưa đăng nhập, 403 nếu sai vai trò.
     *
     * @param exchange     Đối tượng giao tiếp HTTP
     * @param allowedRoles Các vai trò được phép
     * @return Người dùng hợp lệ hoặc null (khi đó phản hồi lỗi đã được gửi)
     * @throws IOException Nếu gặp lỗi ghi luồng
     */
    protected User requireRole(HttpExchange exchange, String... allowedRoles) throws IOException {
        User user = requireLogin(exchange);
        if (user == null) {
            return null;
        }
        if (!SessionManager.hasRole(user, allowedRoles)) {
            sendError(exchange, 403, "FORBIDDEN",
                    "Tài khoản vai trò " + user.getRole() + " không có quyền thực hiện thao tác này (yêu cầu: "
                    + String.join(" / ", allowedRoles) + ").");
            return null;
        }
        return user;
    }
}

