package com.cinehub.services;

import com.cinehub.models.SeatPricingPolicy;
import com.cinehub.models.Showtime;
import com.cinehub.utils.AppConfig;
import com.cinehub.utils.DatabaseConnection;
import com.cinehub.utils.PasswordUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp dịch vụ quản lý Lịch chiếu phim (Phân hệ CSD03) và thực thi Quy tắc nghiệp vụ BR01:
 * Chống chồng lấn thời gian chiếu trong cùng một phòng chiếu (đã cộng 15 phút vệ sinh phòng).
 *
 * @author Vũ Việt Tiến (202416362)
 * @author Hà Sỹ Bách (202416129)
 * @version 1.0
 */
public class ShowtimeService {

    /** Thời gian đệm dọn vệ sinh phòng giữa 2 suất chiếu liên tiếp theo BR01 (15 phút). */
    public static final int CLEANING_BUFFER_MINUTES = AppConfig.getInt("showtime.cleaning.buffer.minutes", 15);

    /** Khóa dùng chung mọi instance để kiểm tra BR01 và chèn suất chiếu là một bước nguyên tử. */
    private static final Object SCHEDULE_LOCK = new Object();

    private static PreparedStatement prepare(Connection conn, String sql, List<Object> params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
        return ps;
    }

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Kết quả trả về của nghiệp vụ lập lịch chiếu mới.
     */
    public static class ScheduleResult {
        private final boolean success;
        private final String code;
        private final String message;
        private final Showtime showtime;

        public ScheduleResult(boolean success, String code, String message, Showtime showtime) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.showtime = showtime;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Showtime getShowtime() {
            return showtime;
        }
    }

    /**
     * Truy xuất danh sách các suất chiếu có kèm số ghế trống theo thời gian thực.
     *
     * @param movieId    Lọc theo mã phim (truyền null hoặc <= 0 để lấy tất cả)
     * @param dateFilter Lọc theo ngày YYYY-MM-DD (truyền null hoặc rỗng để lấy tất cả)
     * @return Danh sách {@link Showtime}
     */
    public List<Showtime> getShowtimes(Integer movieId, String dateFilter) {
        List<Showtime> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT st.showtime_id, st.movie_id, m.title AS movie_title, m.duration_minutes, m.age_rating, "
                + "st.auditorium_id, a.name AS auditorium_name, a.room_type, a.total_seats, "
                + "st.start_time, st.end_time, st.base_price, st.status, "
                + "(SELECT COUNT(*) FROM showtime_seats ss WHERE ss.showtime_id = st.showtime_id AND ss.status = 'AVAILABLE') AS available_seats "
                + "FROM showtimes st "
                + "JOIN movies m ON st.movie_id = m.movie_id "
                + "JOIN auditoriums a ON st.auditorium_id = a.auditorium_id "
                + "WHERE st.status <> 'CANCELLED' ");

        List<Object> params = new ArrayList<>();
        if (movieId != null && movieId > 0) {
            sql.append("AND st.movie_id = ? ");
            params.add(movieId);
        }
        if (!PasswordUtils.isBlank(dateFilter) && !"ALL".equalsIgnoreCase(dateFilter.trim())) {
            // Tham số hóa thay cho ghép chuỗi (chống SQL injection)
            sql.append("AND SUBSTRING(CAST(st.start_time AS VARCHAR), 1, 10) = ? ");
            params.add(dateFilter.trim());
        }
        sql.append("ORDER BY st.start_time ASC, st.auditorium_id ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepare(conn, sql.toString(), params);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Showtime st = new Showtime(
                        rs.getInt("showtime_id"),
                        rs.getInt("movie_id"),
                        rs.getString("movie_title"),
                        rs.getInt("auditorium_id"),
                        rs.getString("auditorium_name"),
                        formatTimestamp(rs.getString("start_time")),
                        formatTimestamp(rs.getString("end_time")),
                        rs.getDouble("base_price"),
                        rs.getString("status")
                );
                st.setDurationMinutes(rs.getInt("duration_minutes"));
                st.setAgeRating(rs.getString("age_rating"));
                st.setRoomType(rs.getString("room_type"));
                st.setTotalSeats(rs.getInt("total_seats"));
                st.setAvailableSeats(rs.getInt("available_seats"));
                list.add(st);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn lịch chiếu: " + ex.getMessage(), ex);
        }
        return list;
    }

    /**
     * Tạo mới một suất chiếu tuân thủ Quy tắc nghiệp vụ BR01 & BR02.
     * Thiết kế luồng điều khiển tường minh phục vụ Kiểm thử Hộp trắng (CFG V(G) = 6):
     * - Node 1: Kiểm tra basePrice <= 0 hoặc startTimeStr rỗng
     * - Node 2: Trả về lỗi INVALID_INPUT
     * - Node 3: Phân tích chuỗi thời gian bắt đầu startTimeStr
     * - Node 4: Truy vấn thông tin Phim (Movie) và kiểm tra tồn tại / trạng thái khác ENDED
     * - Node 5: Trả về lỗi MOVIE_UNAVAILABLE
     * - Node 6: Truy vấn thông tin Phòng chiếu (Auditorium) và kiểm tra trạng thái ACTIVE
     * - Node 7: Trả về lỗi AUDITORIUM_UNAVAILABLE
     * - Node 8: Tính end_time = start_time + duration_minutes + 15 phút vệ sinh (BR01)
     *           và kiểm tra xung đột giờ chiếu trong cùng phòng: (newStart < oldEnd) AND (newEnd > oldStart)
     * - Node 9: Trả về lỗi SCHEDULE_CONFLICT (Vi phạm BR01)
     * - Node 10: Lưu bản ghi Showtime và khởi tạo toàn bộ trạng thái ghế ShowtimeSeat (BR02 & BR05)
     * - Node 11: Trả về kết quả CREATED thành công
     *
     * @param movieId       Mã bộ phim
     * @param auditoriumId  Mã phòng chiếu
     * @param startTimeStr  Thời gian bắt đầu chiếu (YYYY-MM-DD HH:mm:ss hoặc YYYY-MM-DDTHH:mm)
     * @param basePrice     Giá vé cơ sở (VNĐ)
     * @return Đối tượng {@link ScheduleResult}
     */
    public ScheduleResult createShowtime(int movieId, int auditoriumId, String startTimeStr, double basePrice) {
        synchronized (SCHEDULE_LOCK) {
            return createShowtimeLocked(movieId, auditoriumId, startTimeStr, basePrice);
        }
    }

    private ScheduleResult createShowtimeLocked(int movieId, int auditoriumId,
                                                String startTimeStr, double basePrice) {
        // [Node 1] Kiểm tra tính hợp lệ của giá cơ sở và chuỗi thời gian
        if (basePrice <= 0 || PasswordUtils.isBlank(startTimeStr)) {
            // [Node 2] Trả về lỗi tham số đầu vào
            return new ScheduleResult(false, "INVALID_INPUT",
                    "Giá vé cơ sở phải lớn hơn 0 và thời gian bắt đầu không được để trống.", null);
        }

        // [Node 3] Chuẩn hóa và phân tích thời gian bắt đầu
        LocalDateTime startLdt;
        try {
            String normalized = startTimeStr.trim().replace("T", " ");
            if (normalized.length() == 16) {
                normalized = normalized + ":00";
            }
            startLdt = LocalDateTime.parse(normalized, TS_FMT);
        } catch (DateTimeParseException ex) {
            return new ScheduleResult(false, "INVALID_DATETIME",
                    "Định dạng thời gian bắt đầu không hợp lệ (chuẩn YYYY-MM-DD HH:mm:ss).", null);
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // [Node 4] Kiểm tra phim có tồn tại và đang được phép xếp lịch chiếu hay không
            int durationMinutes = 0;
            String movieTitle = null;
            String movieStatus = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT title, duration_minutes, status FROM movies WHERE movie_id = ?")) {
                ps.setInt(1, movieId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        movieTitle = rs.getString("title");
                        durationMinutes = rs.getInt("duration_minutes");
                        movieStatus = rs.getString("status");
                    }
                }
            }
            if (movieTitle == null || "ENDED".equalsIgnoreCase(movieStatus)) {
                // [Node 5] Trả về lỗi phim không khả dụng
                return new ScheduleResult(false, "MOVIE_UNAVAILABLE",
                        "Phim không tồn tại hoặc đã ngừng chiếu (ENDED), không thể tạo suất chiếu mới.", null);
            }

            // [Node 6] Kiểm tra phòng chiếu có tồn tại và đang hoạt động (ACTIVE) hay không
            String auditoriumName = null;
            String audStatus = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name, status FROM auditoriums WHERE auditorium_id = ?")) {
                ps.setInt(1, auditoriumId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        auditoriumName = rs.getString("name");
                        audStatus = rs.getString("status");
                    }
                }
            }
            if (auditoriumName == null || !"ACTIVE".equalsIgnoreCase(audStatus)) {
                // [Node 7] Trả về lỗi phòng chiếu không khả dụng
                return new ScheduleResult(false, "AUDITORIUM_UNAVAILABLE",
                        "Phòng chiếu không tồn tại hoặc đang bảo trì.", null);
            }

            // [Node 8] Áp dụng BR01: end_time = start_time + duration_minutes + 15 phút dọn vệ sinh
            LocalDateTime endLdt = startLdt.plusMinutes(durationMinutes + CLEANING_BUFFER_MINUTES);
            String startFormatted = startLdt.format(TS_FMT);
            String endFormatted = endLdt.format(TS_FMT);

            String conflictSql = "SELECT st.showtime_id, m.title, st.start_time, st.end_time "
                    + "FROM showtimes st JOIN movies m ON st.movie_id = m.movie_id "
                    + "WHERE st.auditorium_id = ? AND st.status = 'SCHEDULED' "
                    + "AND (? < st.end_time) AND (? > st.start_time)";
            try (PreparedStatement ps = conn.prepareStatement(conflictSql)) {
                ps.setInt(1, auditoriumId);
                ps.setString(2, startFormatted);
                ps.setString(3, endFormatted);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // [Node 9] Phát hiện xung đột lịch chiếu theo BR01
                        String conflictMovie = rs.getString("title");
                        String cStart = formatTimestamp(rs.getString("start_time"));
                        String cEnd = formatTimestamp(rs.getString("end_time"));
                        return new ScheduleResult(false, "SCHEDULE_CONFLICT",
                                "Vi phạm BR01 (Trùng lịch chiếu tại " + auditoriumName + "): Đã có suất chiếu phim '"
                                + conflictMovie + "' từ " + cStart + " đến " + cEnd
                                + " (đã tính 15 phút dọn vệ sinh phòng).", null);
                    }
                }
            }

            // [Node 10] Lưu suất chiếu và khởi tạo trạng thái ghế theo suất chiếu (BR02 & BR05)
            conn.setAutoCommit(false);
            try {
                int newShowtimeId;
                String insertSt = "INSERT INTO showtimes (movie_id, auditorium_id, start_time, end_time, base_price, status) "
                        + "VALUES (?, ?, ?, ?, ?, 'SCHEDULED')";
                try (PreparedStatement ps = conn.prepareStatement(insertSt, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, movieId);
                    ps.setInt(2, auditoriumId);
                    ps.setString(3, startFormatted);
                    ps.setString(4, endFormatted);
                    ps.setDouble(5, basePrice);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newShowtimeId = keys.getInt(1);
                    }
                }

                String selectSeats = "SELECT seat_id, seat_type FROM seats WHERE auditorium_id = ? AND is_active = TRUE";
                String insertSS = "INSERT INTO showtime_seats (showtime_id, seat_id, status, final_price) VALUES (?, ?, 'AVAILABLE', ?)";
                int seatCount = 0;
                try (PreparedStatement psSeats = conn.prepareStatement(selectSeats);
                     PreparedStatement psSS = conn.prepareStatement(insertSS)) {
                    psSeats.setInt(1, auditoriumId);
                    try (ResultSet seatRs = psSeats.executeQuery()) {
                        while (seatRs.next()) {
                            int seatId = seatRs.getInt("seat_id");
                            String seatType = seatRs.getString("seat_type");
                            double seatPrice = SeatPricingPolicy.forSeatType(seatType).calculateSeatPrice(basePrice);
                            psSS.setInt(1, newShowtimeId);
                            psSS.setInt(2, seatId);
                            psSS.setDouble(3, seatPrice);
                            psSS.addBatch();
                            seatCount++;
                        }
                    }
                    psSS.executeBatch();
                }

                conn.commit();
                Showtime created = new Showtime(newShowtimeId, movieId, movieTitle,
                        auditoriumId, auditoriumName, startFormatted, endFormatted, basePrice, "SCHEDULED");
                created.setDurationMinutes(durationMinutes);
                created.setTotalSeats(seatCount);
                created.setAvailableSeats(seatCount);

                // [Node 11] Trả về thành công
                return new ScheduleResult(true, "CREATED",
                        "Đã tạo suất chiếu mới thành công (" + startFormatted + " -> " + endFormatted
                        + ", khởi tạo " + seatCount + " ghế).", created);
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi tạo lịch chiếu: " + ex.getMessage(), ex);
        }
    }

    private String formatTimestamp(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.length() >= 19) {
            return trimmed.substring(0, 19);
        }
        return trimmed;
    }
}

