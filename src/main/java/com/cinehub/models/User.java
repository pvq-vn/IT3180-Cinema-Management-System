package com.cinehub.models;

/**
 * Lớp thực thể biểu diễn Tài khoản người dùng trong hệ thống CineHub.
 * Hỗ trợ phân quyền theo vai trò (Role-Based Access Control): CUSTOMER, STAFF, MANAGER.
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.0
 */
public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String status;

    /**
     * Khởi tạo mặc định một đối tượng người dùng.
     */
    public User() {
    }

    /**
     * Khởi tạo đầy đủ thông tin một đối tượng người dùng.
     *
     * @param userId       Mã định danh người dùng
     * @param username     Tên đăng nhập duy nhất
     * @param passwordHash Chuỗi băm mật khẩu SHA-256
     * @param fullName     Họ và tên đầy đủ
     * @param email        Địa chỉ thư điện tử
     * @param phone        Số điện thoại liên hệ
     * @param role         Vai trò phân quyền (CUSTOMER, STAFF, MANAGER)
     * @param status       Trạng thái tài khoản (ACTIVE, LOCKED)
     */
    public User(int userId, String username, String passwordHash, String fullName,
                String email, String phone, String role, String status) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }

    /**
     * Lấy mã định danh người dùng.
     *
     * @return Mã người dùng
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Thiết lập mã định danh người dùng.
     *
     * @param userId Mã người dùng
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Lấy tên đăng nhập.
     *
     * @return Tên đăng nhập
     */
    public String getUsername() {
        return username;
    }

    /**
     * Thiết lập tên đăng nhập.
     *
     * @param username Tên đăng nhập
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Lấy chuỗi băm mật khẩu.
     *
     * @return Chuỗi băm mật khẩu
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Thiết lập chuỗi băm mật khẩu.
     *
     * @param passwordHash Chuỗi băm mật khẩu
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Lấy họ và tên đầy đủ.
     *
     * @return Họ và tên
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Thiết lập họ và tên đầy đủ.
     *
     * @param fullName Họ và tên
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Lấy địa chỉ email.
     *
     * @return Địa chỉ email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Thiết lập địa chỉ email.
     *
     * @param email Địa chỉ email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Lấy số điện thoại.
     *
     * @return Số điện thoại
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Thiết lập số điện thoại.
     *
     * @param phone Số điện thoại
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Lấy vai trò phân quyền của tài khoản.
     *
     * @return Vai trò (CUSTOMER, STAFF, MANAGER)
     */
    public String getRole() {
        return role;
    }

    /**
     * Thiết lập vai trò phân quyền của tài khoản.
     *
     * @param role Vai trò
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Lấy trạng thái hoạt động của tài khoản.
     *
     * @return Trạng thái (ACTIVE, LOCKED)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Thiết lập trạng thái hoạt động của tài khoản.
     *
     * @param status Trạng thái
     */
    public void setStatus(String status) {
        this.status = status;
    }
}

