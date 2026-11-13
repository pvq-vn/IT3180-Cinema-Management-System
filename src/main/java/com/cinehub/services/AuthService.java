package com.cinehub.services;

import com.cinehub.models.User;
import com.cinehub.utils.DatabaseConnection;
import com.cinehub.utils.PasswordUtils;
import com.cinehub.utils.SessionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Lớp dịch vụ xử lý nghiệp vụ Xác thực đăng nhập và Phân quyền người dùng (AuthService).
 * Cấu trúc luồng điều khiển của phương thức {@link #authenticate(String, String)} được thiết kế
 * tường minh phục vụ Kiểm thử Hộp trắng (Control Flow Graph - CFG) trong Chương 9 (IT3180).
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.0
 */
public class AuthService {

    /**
     * Kết quả trả về của nghiệp vụ xác thực đăng nhập.
     */
    public static class AuthResult {
        private final boolean success;
        private final String statusCode;
        private final String message;
        private final String sessionToken;
        private final User user;

        /**
         * Khởi tạo kết quả xác thực.
         *
         * @param success      Trạng thái thành công hay thất bại
         * @param statusCode   Mã kết quả (SUCCESS, INVALID_INPUT, USER_NOT_FOUND, ACCOUNT_LOCKED, WRONG_PASSWORD)
         * @param message      Thông báo tiếng Việt
         * @param sessionToken Mã phiên làm việc (nếu thành công)
         * @param user         Đối tượng người dùng (nếu thành công)
         */
        public AuthResult(boolean success, String statusCode, String message,
                          String sessionToken, User user) {
            this.success = success;
            this.statusCode = statusCode;
            this.message = message;
            this.sessionToken = sessionToken;
            this.user = user;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public String getMessage() {
            return message;
        }

        public String getSessionToken() {
            return sessionToken;
        }

        public User getUser() {
            return user;
        }
    }

    /**
     * Xác thực thông tin đăng nhập của người dùng (Kiểm thử Hộp trắng CFG: V(G) = 5).
     * Các nút điều khiển (CFG Nodes):
     * - Node 1: Kiểm tra username hoặc password bị rỗng/null
     * - Node 2: Trả về lỗi INVALID_INPUT
     * - Node 3: Truy vấn thông tin User từ CSDL theo username
     * - Node 4: Kiểm tra bản ghi User có tồn tại hay không
     * - Node 5: Trả về lỗi USER_NOT_FOUND
     * - Node 6: Kiểm tra trạng thái tài khoản có bị khóa (LOCKED) hay không
     * - Node 7: Trả về lỗi ACCOUNT_LOCKED
     * - Node 8: Kiểm tra chuỗi băm mật khẩu SHA-256 có khớp hay không
     * - Node 9: Trả về lỗi WRONG_PASSWORD
     * - Node 10: Khởi tạo Session Token và trả về kết quả SUCCESS
     * - Node 11: Điểm kết thúc phương thức (Exit)
     *
     * @param username Tên đăng nhập
     * @param password Mật khẩu dạng văn bản thuần
     * @return Đối tượng {@link AuthResult} chứa trạng thái và thông tin phiên
     */
    public AuthResult authenticate(String username, String password) {
        // [Node 1] Kiểm tra dữ liệu đầu vào rỗng hoặc null
        if (PasswordUtils.isBlank(username) || PasswordUtils.isBlank(password)) {
            // [Node 2] Trả về lỗi dữ liệu đầu vào không hợp lệ
            return new AuthResult(false, "INVALID_INPUT",
                    "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.", null, null);
        }

        // [Node 3] Truy vấn người dùng theo tên đăng nhập
        User foundUser = findByUsername(username.trim());

        // [Node 4] Kiểm tra sự tồn tại của tài khoản
        if (foundUser == null) {
            // [Node 5] Trả về lỗi không tìm thấy tài khoản
            return new AuthResult(false, "USER_NOT_FOUND",
                    "Tài khoản không tồn tại trên hệ thống.", null, null);
        }

        // [Node 6] Kiểm tra trạng thái khóa của tài khoản
        if ("LOCKED".equalsIgnoreCase(foundUser.getStatus())) {
            // [Node 7] Trả về lỗi tài khoản bị khóa
            return new AuthResult(false, "ACCOUNT_LOCKED",
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản lý rạp.", null, null);
        }

        // [Node 8] Đối chiếu mật khẩu băm SHA-256
        if (!PasswordUtils.verifyPassword(password, foundUser.getPasswordHash())) {
            // [Node 9] Trả về lỗi sai mật khẩu
            return new AuthResult(false, "WRONG_PASSWORD",
                    "Mật khẩu không chính xác. Vui lòng kiểm tra lại.", null, null);
        }

        // [Node 10] Khởi tạo phiên đăng nhập hợp lệ
        String token = SessionManager.createSession(foundUser);
        return new AuthResult(true, "SUCCESS",
                "Đăng nhập thành công. Xin chào " + foundUser.getFullName() + "!", token, foundUser);
        // [Node 11] Kết thúc hàm
    }

    /**
     * Tìm kiếm người dùng trong cơ sở dữ liệu theo tên đăng nhập.
     *
     * @param username Tên đăng nhập cần tìm
     * @return Đối tượng {@link User} hoặc null nếu không tồn tại
     */
    public User findByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash, full_name, email, phone, role, status "
                + "FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("role"),
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn người dùng: " + ex.getMessage(), ex);
        }
        return null;
    }
}

