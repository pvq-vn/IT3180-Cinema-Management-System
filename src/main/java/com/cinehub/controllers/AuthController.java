package com.cinehub.controllers;

import com.cinehub.models.User;
import com.cinehub.services.AuthService;
import com.cinehub.utils.JsonUtils;
import com.cinehub.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;

/**
 * Bộ điều khiển xử lý các yêu cầu Xác thực đăng nhập và Đăng xuất (/api/auth/*).
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.0
 */
public class AuthController extends BaseController implements HttpHandler {

    private final AuthService authService = new AuthService();

    private String userToJson(User u) {
        return "{\"userId\":" + u.getUserId()
                + ",\"username\":\"" + JsonUtils.escape(u.getUsername()) + "\""
                + ",\"fullName\":\"" + JsonUtils.escape(u.getFullName()) + "\""
                + ",\"email\":\"" + JsonUtils.escape(u.getEmail()) + "\""
                + ",\"phone\":\"" + JsonUtils.escape(u.getPhone()) + "\""
                + ",\"role\":\"" + JsonUtils.escape(u.getRole()) + "\"}";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.endsWith("/login") && "POST".equalsIgnoreCase(method)) {
                Map<String, String> body = JsonUtils.parseFlatObject(readRequestBody(exchange));
                String username = body.get("username");
                String password = body.get("password");

                AuthService.AuthResult res = authService.authenticate(username, password);
                if (!res.isSuccess()) {
                    sendJson(exchange, 200, JsonUtils.statusResponse(false, res.getStatusCode(), res.getMessage()));
                    return;
                }
                String json = "{\"success\":true"
                        + ",\"code\":\"" + JsonUtils.escape(res.getStatusCode()) + "\""
                        + ",\"message\":\"" + JsonUtils.escape(res.getMessage()) + "\""
                        + ",\"token\":\"" + JsonUtils.escape(res.getSessionToken()) + "\""
                        + ",\"user\":" + userToJson(res.getUser()) + "}";
                sendJson(exchange, 200, json);
            } else if (path.endsWith("/me") && "GET".equalsIgnoreCase(method)) {
                // Kiểm tra mã phiên còn hiệu lực (trình duyệt gọi khi tải lại trang)
                User user = requireLogin(exchange);
                if (user == null) {
                    return;
                }
                sendJson(exchange, 200, "{\"success\":true,\"user\":" + userToJson(user) + "}");
            } else if (path.endsWith("/logout") && "POST".equalsIgnoreCase(method)) {
                String token = extractToken(exchange);
                if (token == null) {
                    token = JsonUtils.parseFlatObject(readRequestBody(exchange)).get("token");
                }
                SessionManager.invalidateSession(token);
                sendJson(exchange, 200, JsonUtils.statusResponse(true, "LOGGED_OUT", "Đã đăng xuất khỏi hệ thống."));
            } else {
                sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Phương thức HTTP không được hỗ trợ.");
            }
        } catch (Exception ex) {
            handleException(exchange, ex);
        }
    }
}

