package com.cinehub.services;

import com.cinehub.models.Combo;
import com.cinehub.utils.DatabaseConnection;
import com.cinehub.utils.PasswordUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp dịch vụ quản lý Đồ ăn, Thức uống và Combo F&B (Phân hệ CSD04).
 * Đảm bảo quy tắc toàn vẹn lịch sử hóa đơn: các Combo đã phát sinh giao dịch chỉ được phép
 * chuyển sang trạng thái ngừng kinh doanh (DISCONTINUED) thay vì xóa vật lý.
 *
 * @author Nguyễn Xuân Hồng Quân (202416321)
 * @version 1.0
 */
public class ComboService {

    /**
     * Lấy danh sách các gói Combo F&B.
     *
     * @param activeOnly Nếu true, chỉ trả về các Combo có trạng thái AVAILABLE và còn tồn kho
     * @return Danh sách {@link Combo}
     */
    public List<Combo> getAllCombos(boolean activeOnly) {
        List<Combo> combos = new ArrayList<>();
        String sql = "SELECT combo_id, combo_code, name, description, items_included, price, stock_quantity, status "
                + "FROM combos "
                + (activeOnly ? "WHERE status = 'AVAILABLE' AND stock_quantity > 0 " : "")
                + "ORDER BY combo_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                combos.add(new Combo(
                        rs.getInt("combo_id"),
                        rs.getString("combo_code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("items_included"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn danh sách Combo F&B: " + ex.getMessage(), ex);
        }
        return combos;
    }

    /**
     * Thêm mới hoặc cập nhật một gói Combo F&B.
     *
     * @param combo Đối tượng Combo cần lưu
     * @return Đối tượng {@link Combo} đã lưu
     * @throws IllegalArgumentException Nếu dữ liệu đầu vào không hợp lệ
     */
    public Combo saveCombo(Combo combo) {
        if (combo == null || PasswordUtils.isBlank(combo.getName())) {
            throw new IllegalArgumentException("Tên gói Combo không được để trống.");
        }
        if (combo.getPrice() <= 0) {
            throw new IllegalArgumentException("Đơn giá Combo phải lớn hơn 0 VNĐ.");
        }
        if (combo.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không được âm.");
        }
        String requestedStatus = PasswordUtils.isBlank(combo.getStatus())
                ? null : combo.getStatus().trim().toUpperCase();
        if (requestedStatus != null && !List.of("AVAILABLE", "OUT_OF_STOCK", "DISCONTINUED").contains(requestedStatus)) {
            throw new IllegalArgumentException("Trạng thái Combo không hợp lệ: " + requestedStatus + ".");
        }
        String items = PasswordUtils.isBlank(combo.getItemsIncluded())
                ? "1 Bắp rang bơ + 1 Nước ngọt" : combo.getItemsIncluded().trim();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String code = PasswordUtils.isBlank(combo.getComboCode())
                    ? generateUniqueCode(conn) : combo.getComboCode().trim().toUpperCase();
            // Sửa combo mà không gửi trạng thái: giữ trạng thái cũ (không vô tình mở bán lại combo đã ngừng)
            String baseStatus = requestedStatus;
            if (baseStatus == null && combo.getComboId() > 0) {
                baseStatus = findStatus(conn, combo.getComboId());
                if (baseStatus == null) {
                    throw new IllegalArgumentException("Không tìm thấy gói Combo cần cập nhật.");
                }
            }
            // Trạng thái bán phụ thuộc tồn kho: còn hàng = AVAILABLE, hết hàng = OUT_OF_STOCK
            String status = "DISCONTINUED".equals(baseStatus)
                    ? "DISCONTINUED" : (combo.getStockQuantity() > 0 ? "AVAILABLE" : "OUT_OF_STOCK");
            combo.setStatus(status);
            combo.setComboCode(code);

            if (combo.getComboId() <= 0) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM combos WHERE combo_code = ?")) {
                    ps.setString(1, code);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new IllegalArgumentException("Mã Combo " + code + " đã tồn tại.");
                        }
                    }
                }
                String insertSql = "INSERT INTO combos (combo_code, name, description, items_included, "
                        + "price, stock_quantity, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, code);
                    ps.setString(2, combo.getName().trim());
                    ps.setString(3, combo.getDescription() == null ? "" : combo.getDescription().trim());
                    ps.setString(4, items);
                    ps.setDouble(5, combo.getPrice());
                    ps.setInt(6, combo.getStockQuantity());
                    ps.setString(7, status);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            combo.setComboId(keys.getInt(1));
                        }
                    }
                }
            } else {
                String updateSql = "UPDATE combos SET name = ?, description = ?, items_included = ?, "
                        + "price = ?, stock_quantity = ?, status = ? WHERE combo_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, combo.getName().trim());
                    ps.setString(2, combo.getDescription() == null ? "" : combo.getDescription().trim());
                    ps.setString(3, items);
                    ps.setDouble(4, combo.getPrice());
                    ps.setInt(5, combo.getStockQuantity());
                    ps.setString(6, status);
                    ps.setInt(7, combo.getComboId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi lưu Combo F&B: " + ex.getMessage(), ex);
        }
        return combo;
    }

    private String findStatus(Connection conn, int comboId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM combos WHERE combo_id = ?")) {
            ps.setInt(1, comboId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }

    /** Sinh mã Combo dạng CBnn chưa được dùng. */
    private String generateUniqueCode(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM combos WHERE combo_code = ?")) {
            for (int n = 1; n < 1000; n++) {
                String code = String.format("CB%02d", n);
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        return code;
                    }
                }
            }
        }
        throw new IllegalStateException("Không sinh được mã Combo mới.");
    }

    /**
     * Chuyển đổi trạng thái kinh doanh hoặc ngừng kinh doanh một Combo F&B.
     *
     * @param comboId Mã Combo cần chuyển trạng thái
     * @return Thông báo kết quả
     */
    public String toggleComboStatus(int comboId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String currentStatus = null;
            int currentStock = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status, stock_quantity FROM combos WHERE combo_id = ?")) {
                ps.setInt(1, comboId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentStatus = rs.getString("status");
                        currentStock = rs.getInt("stock_quantity");
                    }
                }
            }
            if (currentStatus == null) {
                throw new IllegalArgumentException("Không tìm thấy gói Combo cần cập nhật.");
            }
            String nextStatus;
            if (!"DISCONTINUED".equalsIgnoreCase(currentStatus)) {
                nextStatus = "DISCONTINUED";
            } else {
                // Mở bán lại: hết hàng thì chuyển OUT_OF_STOCK, không mở bán combo không còn tồn kho
                nextStatus = currentStock > 0 ? "AVAILABLE" : "OUT_OF_STOCK";
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE combos SET status = ? WHERE combo_id = ?")) {
                ps.setString(1, nextStatus);
                ps.setInt(2, comboId);
                ps.executeUpdate();
            }
            if ("OUT_OF_STOCK".equals(nextStatus)) {
                return "Combo đã được mở lại nhưng tồn kho bằng 0 nên vẫn ở trạng thái Hết hàng (OUT_OF_STOCK). "
                        + "Hãy nhập thêm tồn kho để bán.";
            }
            return "Đã cập nhật trạng thái Combo sang " + nextStatus + ".";
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi cập nhật trạng thái Combo: " + ex.getMessage(), ex);
        }
    }
}

