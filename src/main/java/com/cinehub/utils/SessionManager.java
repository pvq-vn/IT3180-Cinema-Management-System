package com.cinehub.utils;

import com.cinehub.models.User;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp quản lý Phiên đăng nhập (Session Token) và Phân quyền truy cập theo Vai trò (RBAC).
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.0
 */
public final class SessionManager {

    private static final Map<String, User> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    /**
     * Khởi tạo phiên đăng nhập mới cho người dùng đã xác thực thành công.
     *
     * @param user Đối tượng người dùng hợp lệ
     * @return Chuỗi mã phiên (Session Token)
     */
    public static String createSession(User user) {
        String token = "CHSESS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        ACTIVE_SESSIONS.put(token, user);
        return token;
    }

    /**
     * Truy xuất thông tin người dùng từ mã phiên làm việc.
     *
     * @param token Mã phiên làm việc
     * @return Đối tượng {@link User} hoặc null nếu phiên không hợp lệ
     */
    public static User getUserByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return ACTIVE_SESSIONS.get(token.trim());
    }

    /**
     * Hủy phiên đăng nhập khi người dùng đăng xuất.
     *
     * @param token Mã phiên cần hủy
     */
    public static void invalidateSession(String token) {
        if (token != null) {
            ACTIVE_SESSIONS.remove(token.trim());
        }
    }

    /**
     * Kiểm tra người dùng có thuộc một trong các vai trò được phép hay không.
     *
     * @param user         Người dùng hiện tại
     * @param allowedRoles Danh sách vai trò được phép (MANAGER, STAFF, CUSTOMER)
     * @return true nếu có quyền truy cập
     */
    public static boolean hasRole(User user, String... allowedRoles) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        for (String role : allowedRoles) {
            if (user.getRole().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }
}

