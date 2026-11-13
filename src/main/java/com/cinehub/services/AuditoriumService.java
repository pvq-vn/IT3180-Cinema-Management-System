package com.cinehub.services;

import com.cinehub.models.Auditorium;
import com.cinehub.models.Seat;
import com.cinehub.utils.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp dịch vụ quản lý Phòng chiếu và Sơ đồ ghế vật lý cố định (Phân hệ CSD03 - Quy tắc BR02).
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.0
 */
public class AuditoriumService {

    /**
     * Lấy danh sách toàn bộ các phòng chiếu trong rạp.
     *
     * @return Danh sách {@link Auditorium}
     */
    public List<Auditorium> getAllAuditoriums() {
        List<Auditorium> list = new ArrayList<>();
        String sql = "SELECT auditorium_id, name, room_type, total_rows, seats_per_row, total_seats, status "
                + "FROM auditoriums ORDER BY auditorium_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Auditorium(
                        rs.getInt("auditorium_id"),
                        rs.getString("name"),
                        rs.getString("room_type"),
                        rs.getInt("total_rows"),
                        rs.getInt("seats_per_row"),
                        rs.getInt("total_seats"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn danh sách phòng chiếu: " + ex.getMessage(), ex);
        }
        return list;
    }

    /**
     * Lấy danh sách sơ đồ ghế vật lý cố định của một phòng chiếu.
     *
     * @param auditoriumId Mã phòng chiếu
     * @return Danh sách {@link Seat}
     */
    public List<Seat> getPhysicalSeatsByAuditorium(int auditoriumId) {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT seat_id, auditorium_id, row_label, seat_number, seat_code, seat_type, is_active "
                + "FROM seats WHERE auditorium_id = ? ORDER BY row_label ASC, seat_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auditoriumId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seats.add(new Seat(
                            rs.getInt("seat_id"),
                            rs.getInt("auditorium_id"),
                            rs.getString("row_label"),
                            rs.getInt("seat_number"),
                            rs.getString("seat_code"),
                            rs.getString("seat_type"),
                            rs.getBoolean("is_active")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn ghế vật lý: " + ex.getMessage(), ex);
        }
        return seats;
    }
}

