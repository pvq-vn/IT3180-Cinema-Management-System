package com.cinehub.utils;

import com.cinehub.models.User;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp quản lý Phiên đăng nhập (Session Token) và Phân quyền truy cập theo Vai trò (RBAC).
 * Mã phiên sinh ngẫu nhiên 256 bit; phiên tự hết hạn khi không hoạt động quá
 * {@code session.timeout.minutes} phút (mặc định 480 phút = 8 giờ).
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.1
 */
public final class SessionManager {

    /** Thời gian phiên được giữ khi không có hoạt động (phút). */
    public static final int SESSION_TIMEOUT_MINUTES = AppConfig.getInt("session.timeout.minutes", 480);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, Entry> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    /** Một phiên đăng nhập: người dùng và thời điểm hoạt động gần nhất. */
    private static final class Entry {
        private final User user;
        private volatile long lastAccessMillis;

        private Entry(User user) {
            this.user = user;
            this.lastAccessMillis = System.currentTimeMillis();
        }
    }

    private SessionManager() {
    }

    /**
     * Khởi tạo phiên đăng nhập mới cho người dùng đã xác thực thành công.
     *
     * @param user Đối tượng người dùng hợp lệ
     * @return Chuỗi mã phiên (Session Token)
     */
    public static String createSession(User user) {
        purgeExpired();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = "CHSESS-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ACTIVE_SESSIONS.put(token, new Entry(user));
        return token;
    }

    /**
     * Truy xuất thông tin người dùng từ mã phiên; gia hạn phiên nếu còn hiệu lực.
     *
     * @param token Mã phiên làm việc
     * @return Đối tượng {@link User} hoặc null nếu phiên không hợp lệ hoặc đã hết hạn
     */
    public static User getUserByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String key = token.trim();
        Entry entry = ACTIVE_SESSIONS.get(key);
        if (entry == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - entry.lastAccessMillis > SESSION_TIMEOUT_MINUTES * 60_000L) {
            ACTIVE_SESSIONS.remove(key);
            return null;
        }
        entry.lastAccessMillis = now;
        return entry.user;
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

    private static void purgeExpired() {
        long limit = System.currentTimeMillis() - SESSION_TIMEOUT_MINUTES * 60_000L;
        ACTIVE_SESSIONS.values().removeIf(e -> e.lastAccessMillis < limit);
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
