package com.cinehub.utils;

import com.cinehub.models.SeatPricingPolicy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp quản lý kết nối Cơ sở dữ liệu quan hệ (JDBC) và khởi tạo 10 bảng cùng dữ liệu mẫu.
 * Mặc định dùng H2 in-memory ở chế độ tương thích MySQL (dữ liệu mất khi tắt máy chủ).
 * Có thể đổi sang H2 dạng file (giữ dữ liệu) qua {@code db.url} trong {@code config/db.properties}.
 * Cấu hình MySQL có trong mẫu nhưng cần tự thêm driver MySQL và chưa được kiểm thử.
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.1
 */
public final class DatabaseConnection {

    private static final String DEFAULT_DRIVER = AppConfig.get("db.driver", "org.h2.Driver");
    private static final String DEFAULT_URL = AppConfig.get("db.url",
            "jdbc:h2:mem:cinehub_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    private static final String DEFAULT_USER = AppConfig.get("db.username", "sa");
    private static final String DEFAULT_PASSWORD = AppConfig.get("db.password", "");
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static boolean initialized = false;

    private DatabaseConnection() {
    }

    /**
     * Mở một kết nối JDBC tới cơ sở dữ liệu CineHub.
     * Tự động khởi tạo lược đồ bảng và dữ liệu mẫu trong lần gọi đầu tiên.
     *
     * @return Đối tượng {@link Connection} đang mở
     * @throws SQLException Nếu xảy ra lỗi kết nối CSDL
     */
    public static synchronized Connection getConnection() throws SQLException {
        try {
            Class.forName(DEFAULT_DRIVER);
        } catch (ClassNotFoundException ignored) {
            // Driver có thể đã được nạp tự động qua ServiceLoader
        }
        Connection conn = DriverManager.getConnection(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
        if (!initialized) {
            initialized = true;
            initializeDatabase(conn);
        }
        return conn;
    }

    /**
     * Làm mới hoàn toàn cơ sở dữ liệu về trạng thái ban đầu (phục vụ Kiểm thử tự động JUnit).
     *
     * @throws SQLException Nếu có lỗi SQL khi khởi tạo lại
     */
    public static synchronized void resetDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
            initialized = true;
            initializeDatabase(conn);
        }
    }

    /**
     * Khởi tạo cấu trúc 10 bảng CSDL và nạp dữ liệu mẫu rạp chiếu phim Việt Nam.
     *
     * @param conn Kết nối JDBC
     * @throws SQLException Nếu câu lệnh DDL/DML gặp lỗi
     */
    private static void initializeDatabase(Connection conn) throws SQLException {
        executeSqlFileOrFallback(conn, "config/schema.sql", getFallbackSchemaSql());
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }
        executeSqlFileOrFallback(conn, "config/seed_data.sql", getFallbackSeedSql());
        generatePhysicalSeats(conn);
        generateInitialShowtimesAndBookings(conn);
    }

    /**
     * Đọc và thực thi tập tin SQL từ thư mục cấu hình hoặc dùng chuỗi SQL dự phòng.
     *
     * @param conn         Kết nối JDBC
     * @param relativePath Đường dẫn tương đối tới tập tin .sql
     * @param fallbackSql  Nội dung SQL dự phòng
     * @throws SQLException Nếu thực thi SQL thất bại
     */
    private static void executeSqlFileOrFallback(Connection conn, String relativePath,
                                                 String fallbackSql) throws SQLException {
        String sqlContent = fallbackSql;
        Path p1 = Paths.get(relativePath);
        Path p2 = Paths.get("03-SourceCodes", relativePath);
        try {
            if (Files.exists(p1)) {
                sqlContent = Files.readString(p1, StandardCharsets.UTF_8);
            } else if (Files.exists(p2)) {
                sqlContent = Files.readString(p2, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            sqlContent = fallbackSql;
        }
        StringBuilder cleaned = new StringBuilder();
        for (String line : sqlContent.split("\r?\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--")) {
                int commentIdx = line.indexOf("--");
                if (commentIdx >= 0 && !line.contains("'/")) {
                    cleaned.append(line, 0, commentIdx).append("\n");
                } else {
                    cleaned.append(line).append("\n");
                }
            }
        }
        try (Statement stmt = conn.createStatement()) {
            for (String statement : cleaned.toString().split(";")) {
                String sql = statement.trim();
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }
        }
    }

    /**
     * Sinh tự động sơ đồ ghế vật lý (Bảng seats) cho 3 phòng chiếu theo quy tắc BR02:
     * - Hàng A, B: Ghế Thường (STANDARD)
     * - Hàng C, D (và E với phòng lớn): Ghế VIP Trung tâm (VIP)
     * - Hàng cuối cùng: Ghế Đôi Sweetbox (COUPLE)
     *
     * @param conn Kết nối JDBC
     * @throws SQLException Nếu có lỗi truy vấn
     */
    private static void generatePhysicalSeats(Connection conn) throws SQLException {
        String selectAud = "SELECT auditorium_id, total_rows, seats_per_row FROM auditoriums ORDER BY auditorium_id";
        String insertSeat = "INSERT INTO seats (auditorium_id, row_label, seat_number, seat_code, seat_type, is_active) "
                + "VALUES (?, ?, ?, ?, ?, TRUE)";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(selectAud);
             PreparedStatement ps = conn.prepareStatement(insertSeat)) {
            while (rs.next()) {
                int audId = rs.getInt("auditorium_id");
                int totalRows = rs.getInt("total_rows");
                int seatsPerRow = rs.getInt("seats_per_row");
                for (int r = 0; r < totalRows; r++) {
                    String rowLabel = String.valueOf((char) ('A' + r));
                    String seatType;
                    if (r == totalRows - 1) {
                        seatType = "COUPLE";
                    } else if (r >= 2) {
                        seatType = "VIP";
                    } else {
                        seatType = "STANDARD";
                    }
                    for (int s = 1; s <= seatsPerRow; s++) {
                        String seatCode = String.format("%s%02d", rowLabel, s);
                        ps.setInt(1, audId);
                        ps.setString(2, rowLabel);
                        ps.setInt(3, s);
                        ps.setString(4, seatCode);
                        ps.setString(5, seatType);
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();
        }
    }

    /**
     * Khởi tạo 8 suất chiếu mẫu (tuân thủ BR01) cùng toàn bộ trạng thái ghế (showtime_seats)
     * và các giao dịch Hóa đơn / Vé điện tử QR mẫu để trình diễn đầy đủ 5 phân hệ CSD01-CSD05.
     *
     * @param conn Kết nối JDBC
     * @throws SQLException Nếu có lỗi truy vấn
     */
    private static void generateInitialShowtimesAndBookings(Connection conn) throws SQLException {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        Object[][] showtimeData = new Object[][]{
            {1, 1, today + " 09:00:00", 128, 90000.0},
            {2, 1, today + " 12:00:00", 135, 95000.0},
            {3, 1, today + " 15:15:00", 122, 95000.0},
            {5, 1, today + " 18:30:00", 163, 110000.0},
            {4, 2, today + " 09:30:00", 105, 80000.0},
            {1, 2, today + " 13:00:00", 128, 90000.0},
            {2, 3, today + " 19:00:00", 135, 120000.0},
            {1, 1, tomorrow + " 10:00:00", 128, 90000.0}
        };

        String insertShowtime = "INSERT INTO showtimes (movie_id, auditorium_id, start_time, end_time, base_price, status) "
                + "VALUES (?, ?, ?, ?, ?, 'SCHEDULED')";
        String selectSeats = "SELECT seat_id, seat_type FROM seats WHERE auditorium_id = ? AND is_active = TRUE ORDER BY seat_id";
        String insertShowtimeSeat = "INSERT INTO showtime_seats (showtime_id, seat_id, status, final_price) "
                + "VALUES (?, ?, 'AVAILABLE', ?)";

        try (PreparedStatement psShow = conn.prepareStatement(insertShowtime, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psSelectSeats = conn.prepareStatement(selectSeats);
             PreparedStatement psSS = conn.prepareStatement(insertShowtimeSeat)) {

            for (Object[] row : showtimeData) {
                int movieId = (Integer) row[0];
                int audId = (Integer) row[1];
                String startStr = (String) row[2];
                int duration = (Integer) row[3];
                double basePrice = (Double) row[4];

                LocalDateTime startLdt = LocalDateTime.parse(startStr, TS_FMT);
                // BR01: end_time = start_time + duration_minutes + 15 phút vệ sinh phòng
                LocalDateTime endLdt = startLdt.plusMinutes(duration + 15);

                psShow.setInt(1, movieId);
                psShow.setInt(2, audId);
                psShow.setString(3, startLdt.format(TS_FMT));
                psShow.setString(4, endLdt.format(TS_FMT));
                psShow.setDouble(5, basePrice);
                psShow.executeUpdate();

                int showtimeId;
                try (ResultSet keys = psShow.getGeneratedKeys()) {
                    keys.next();
                    showtimeId = keys.getInt(1);
                }

                psSelectSeats.setInt(1, audId);
                try (ResultSet seatRs = psSelectSeats.executeQuery()) {
                    while (seatRs.next()) {
                        int seatId = seatRs.getInt("seat_id");
                        String seatType = seatRs.getString("seat_type");
                        double finalPrice = SeatPricingPolicy.forSeatType(seatType).calculateSeatPrice(basePrice);
                        psSS.setInt(1, showtimeId);
                        psSS.setInt(2, seatId);
                        psSS.setDouble(3, finalPrice);
                        psSS.addBatch();
                    }
                }
            }
            psSS.executeBatch();
        }

        // Tạo dữ liệu mẫu Hóa đơn (Invoices) & Vé QR (Tickets) cho Suất chiếu #1 và #2
        seedSampleInvoicesAndTickets(conn);
    }

    /**
     * Nạp các giao dịch đặt vé Online và POS mẫu để báo cáo doanh thu và soát vé QR hoạt động ngay.
     *
     * @param conn Kết nối JDBC
     * @throws SQLException Nếu có lỗi SQL
     */
    private static void seedSampleInvoicesAndTickets(Connection conn) throws SQLException {
        // Đơn 1: Đặt vé Online (Khách hàng Vũ Việt Tiến - 2 ghế VIP C05, C06 suất chiếu #1 + 1 Combo CB02)
        createSampleOrder(conn, "INV-202605-001", 4, "Vũ Việt Tiến", "0987654321",
                null, 1, "ONLINE", "VIETQR", new String[]{"C05", "C06"}, 2, 1, "CHECKED_IN");

        // Đơn 2: Đặt vé Online (Khách hàng Hà Sỹ Bách - 2 ghế VIP D04, D05 suất chiếu #1 + 1 Combo CB01)
        createSampleOrder(conn, "INV-202605-002", 5, "Hà Sỹ Bách", "0987654322",
                null, 1, "ONLINE", "E_WALLET", new String[]{"D04", "D05"}, 1, 1, "ISSUED");

        // Đơn 3: Bán vé tại quầy POS (Nhân viên Lê Anh Tuấn bán cho khách vãng lai - Ghế đôi F01, F02 suất chiếu #2)
        createSampleOrder(conn, "INV-202605-003", null, "Nguyễn Minh Hoàng (Khách tại quầy)", "0903112233",
                2, 2, "POS", "CASH", new String[]{"F01", "F02"}, 3, 1, "ISSUED");

        // Đơn 4: Đặt vé Online Suất chiếu #4 (Mission Impossible - 2 ghế Thường A04, A05)
        createSampleOrder(conn, "INV-202605-004", 4, "Vũ Việt Tiến", "0987654321",
                null, 4, "ONLINE", "VIETQR", new String[]{"A04", "A05"}, 4, 1, "ISSUED");

        // Đánh dấu 2 ghế C07, C08 ở suất chiếu #1 đang được giữ tạm thời (HELD) trong 5 phút (BR03)
        String holdUntil = LocalDateTime.now().plusMinutes(5).format(TS_FMT);
        String holdSql = "UPDATE showtime_seats SET status = 'HELD', held_by_user_id = 5, locked_until = ? "
                + "WHERE showtime_id = 1 AND seat_id IN ("
                + "SELECT seat_id FROM seats WHERE auditorium_id = 1 AND seat_code IN ('C07', 'C08'))";
        try (PreparedStatement ps = conn.prepareStatement(holdSql)) {
            ps.setString(1, holdUntil);
            ps.executeUpdate();
        }
    }

    private static void createSampleOrder(Connection conn, String invoiceCode, Integer userId,
                                          String customerName, String customerPhone, Integer staffId,
                                          int showtimeId, String channel, String paymentMethod,
                                          String[] seatCodes, int comboId, int comboQty,
                                          String initialTicketStatus) throws SQLException {
        double seatTotal = 0.0;
        int[] ssIds = new int[seatCodes.length];
        String[] seatTypes = new String[seatCodes.length];
        double[] seatPrices = new double[seatCodes.length];

        String findSeatSql = "SELECT ss.showtime_seat_id, ss.final_price, s.seat_type "
                + "FROM showtime_seats ss JOIN seats s ON ss.seat_id = s.seat_id "
                + "WHERE ss.showtime_id = ? AND s.seat_code = ?";
        try (PreparedStatement ps = conn.prepareStatement(findSeatSql)) {
            for (int i = 0; i < seatCodes.length; i++) {
                ps.setInt(1, showtimeId);
                ps.setString(2, seatCodes[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ssIds[i] = rs.getInt("showtime_seat_id");
                        seatPrices[i] = rs.getDouble("final_price");
                        seatTypes[i] = rs.getString("seat_type");
                        seatTotal += seatPrices[i];
                    }
                }
            }
        }

        double comboPrice = 0.0;
        String comboName = "";
        try (PreparedStatement ps = conn.prepareStatement("SELECT name, price FROM combos WHERE combo_id = ?")) {
            ps.setInt(1, comboId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    comboName = rs.getString("name");
                    comboPrice = rs.getDouble("price");
                }
            }
        }
        double comboTotal = comboPrice * comboQty;
        double totalAmount = seatTotal + comboTotal;
        String comboSummary = comboQty > 0 ? (comboQty + "x " + comboName) : "Không chọn Combo";

        String insertInv = "INSERT INTO invoices (invoice_code, user_id, customer_name, customer_phone, "
                + "staff_id, showtime_id, booking_channel, payment_method, seat_total, combo_total, "
                + "discount_amount, total_amount, combo_summary, payment_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, 'PAID')";
        int invoiceId;
        try (PreparedStatement ps = conn.prepareStatement(insertInv, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, invoiceCode);
            if (userId != null) {
                ps.setInt(2, userId);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, customerName);
            ps.setString(4, customerPhone);
            if (staffId != null) {
                ps.setInt(5, staffId);
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            ps.setInt(6, showtimeId);
            ps.setString(7, channel);
            ps.setString(8, paymentMethod);
            ps.setDouble(9, seatTotal);
            ps.setDouble(10, comboTotal);
            ps.setDouble(11, totalAmount);
            ps.setString(12, comboSummary);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                invoiceId = keys.getInt(1);
            }
        }

        if (comboQty > 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO invoice_combos (invoice_id, combo_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, invoiceId);
                ps.setInt(2, comboId);
                ps.setInt(3, comboQty);
                ps.setDouble(4, comboPrice);
                ps.setDouble(5, comboTotal);
                ps.executeUpdate();
            }
        }

        String updateSS = "UPDATE showtime_seats SET status = 'BOOKED', held_by_user_id = NULL, locked_until = NULL "
                + "WHERE showtime_seat_id = ?";
        String insertTicket = "INSERT INTO tickets (ticket_code, qr_payload, invoice_id, showtime_id, "
                + "showtime_seat_id, seat_code, seat_type, ticket_price, ticket_status, checked_in_at, checked_in_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement psUpd = conn.prepareStatement(updateSS);
             PreparedStatement psTkt = conn.prepareStatement(insertTicket)) {
            for (int i = 0; i < seatCodes.length; i++) {
                psUpd.setInt(1, ssIds[i]);
                psUpd.executeUpdate();

                String ticketCode = String.format("TKT-%03d-%s", invoiceId, seatCodes[i]);
                String qrPayload = "CINEHUB|QR|" + ticketCode + "|SHOW" + showtimeId + "|" + seatCodes[i];
                psTkt.setString(1, ticketCode);
                psTkt.setString(2, qrPayload);
                psTkt.setInt(3, invoiceId);
                psTkt.setInt(4, showtimeId);
                psTkt.setInt(5, ssIds[i]);
                psTkt.setString(6, seatCodes[i]);
                psTkt.setString(7, seatTypes[i]);
                psTkt.setDouble(8, seatPrices[i]);
                psTkt.setString(9, initialTicketStatus);
                if ("CHECKED_IN".equals(initialTicketStatus)) {
                    psTkt.setString(10, LocalDateTime.now().minusMinutes(15).format(TS_FMT));
                    psTkt.setInt(11, 2);
                } else {
                    psTkt.setNull(10, java.sql.Types.TIMESTAMP);
                    psTkt.setNull(11, java.sql.Types.INTEGER);
                }
                psTkt.executeUpdate();
            }
        }
    }

    private static String getFallbackSchemaSql() {
        return "CREATE TABLE IF NOT EXISTS users ("
                + "user_id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, "
                + "password_hash VARCHAR(255) NOT NULL, full_name VARCHAR(100) NOT NULL, "
                + "email VARCHAR(100) NOT NULL, phone VARCHAR(20) NOT NULL, "
                + "role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER', status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);"
                + "CREATE TABLE IF NOT EXISTS movies ("
                + "movie_id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(150) NOT NULL, "
                + "genre VARCHAR(100) NOT NULL, duration_minutes INT NOT NULL, "
                + "age_rating VARCHAR(10) NOT NULL DEFAULT 'P', director VARCHAR(100), "
                + "cast_list VARCHAR(255), description VARCHAR(1000), poster_url VARCHAR(255), "
                + "release_date DATE, status VARCHAR(20) NOT NULL DEFAULT 'SHOWING');"
                + "CREATE TABLE IF NOT EXISTS auditoriums ("
                + "auditorium_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL UNIQUE, "
                + "room_type VARCHAR(30) NOT NULL DEFAULT '2D Standard', total_rows INT NOT NULL, "
                + "seats_per_row INT NOT NULL, total_seats INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE');"
                + "CREATE TABLE IF NOT EXISTS seats ("
                + "seat_id INT AUTO_INCREMENT PRIMARY KEY, auditorium_id INT NOT NULL, "
                + "row_label VARCHAR(5) NOT NULL, seat_number INT NOT NULL, seat_code VARCHAR(10) NOT NULL, "
                + "seat_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD', is_active BOOLEAN NOT NULL DEFAULT TRUE, "
                + "CONSTRAINT fk_seats_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums(auditorium_id), "
                + "CONSTRAINT uq_auditorium_seat UNIQUE (auditorium_id, seat_code));"
                + "CREATE TABLE IF NOT EXISTS showtimes ("
                + "showtime_id INT AUTO_INCREMENT PRIMARY KEY, movie_id INT NOT NULL, auditorium_id INT NOT NULL, "
                + "start_time TIMESTAMP NOT NULL, end_time TIMESTAMP NOT NULL, base_price DECIMAL(12,2) NOT NULL, "
                + "status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', "
                + "CONSTRAINT fk_showtimes_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id), "
                + "CONSTRAINT fk_showtimes_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums(auditorium_id));"
                + "CREATE TABLE IF NOT EXISTS showtime_seats ("
                + "showtime_seat_id INT AUTO_INCREMENT PRIMARY KEY, showtime_id INT NOT NULL, seat_id INT NOT NULL, "
                + "status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', held_by_user_id INT NULL, "
                + "locked_until TIMESTAMP NULL, final_price DECIMAL(12,2) NOT NULL, "
                + "CONSTRAINT fk_ss_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_ss_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id), "
                + "CONSTRAINT fk_ss_held_by FOREIGN KEY (held_by_user_id) REFERENCES users(user_id), "
                + "CONSTRAINT uq_showtime_seat UNIQUE (showtime_id, seat_id));"
                + "CREATE TABLE IF NOT EXISTS combos ("
                + "combo_id INT AUTO_INCREMENT PRIMARY KEY, combo_code VARCHAR(20) NOT NULL UNIQUE, "
                + "name VARCHAR(100) NOT NULL, description VARCHAR(255), items_included VARCHAR(255) NOT NULL, "
                + "price DECIMAL(12,2) NOT NULL, stock_quantity INT NOT NULL DEFAULT 100, "
                + "status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE');"
                + "CREATE TABLE IF NOT EXISTS invoices ("
                + "invoice_id INT AUTO_INCREMENT PRIMARY KEY, invoice_code VARCHAR(30) NOT NULL UNIQUE, "
                + "user_id INT NULL, customer_name VARCHAR(100) NOT NULL, customer_phone VARCHAR(20), "
                + "staff_id INT NULL, showtime_id INT NOT NULL, booking_channel VARCHAR(20) NOT NULL DEFAULT 'ONLINE', "
                + "payment_method VARCHAR(30) NOT NULL DEFAULT 'VIETQR', seat_total DECIMAL(12,2) NOT NULL DEFAULT 0, "
                + "combo_total DECIMAL(12,2) NOT NULL DEFAULT 0, discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0, "
                + "total_amount DECIMAL(12,2) NOT NULL DEFAULT 0, combo_summary VARCHAR(255), "
                + "payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "CONSTRAINT fk_invoices_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id), "
                + "CONSTRAINT fk_invoices_user FOREIGN KEY (user_id) REFERENCES users(user_id), "
                + "CONSTRAINT fk_invoices_staff FOREIGN KEY (staff_id) REFERENCES users(user_id));"
                + "CREATE TABLE IF NOT EXISTS invoice_combos ("
                + "invoice_combo_id INT AUTO_INCREMENT PRIMARY KEY, invoice_id INT NOT NULL, combo_id INT NOT NULL, "
                + "quantity INT NOT NULL, unit_price DECIMAL(12,2) NOT NULL, subtotal DECIMAL(12,2) NOT NULL, "
                + "CONSTRAINT fk_ic_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_ic_combo FOREIGN KEY (combo_id) REFERENCES combos(combo_id));"
                + "CREATE TABLE IF NOT EXISTS tickets ("
                + "ticket_id INT AUTO_INCREMENT PRIMARY KEY, ticket_code VARCHAR(30) NOT NULL UNIQUE, "
                + "qr_payload VARCHAR(255) NOT NULL UNIQUE, invoice_id INT NOT NULL, showtime_id INT NOT NULL, "
                + "showtime_seat_id INT NOT NULL, seat_code VARCHAR(10) NOT NULL, seat_type VARCHAR(20) NOT NULL, "
                + "ticket_price DECIMAL(12,2) NOT NULL, ticket_status VARCHAR(20) NOT NULL DEFAULT 'ISSUED', "
                + "checked_in_at TIMESTAMP NULL, checked_in_by INT NULL, "
                + "CONSTRAINT fk_tickets_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_tickets_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id), "
                + "CONSTRAINT fk_tickets_ss FOREIGN KEY (showtime_seat_id) REFERENCES showtime_seats(showtime_seat_id), "
                + "CONSTRAINT fk_tickets_checked_by FOREIGN KEY (checked_in_by) REFERENCES users(user_id));";
    }

    private static String getFallbackSeedSql() {
        return "INSERT INTO users (username, password_hash, full_name, email, phone, role, status) VALUES "
                + "('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Phạm Văn Quyết (Quản lý Rạp)', 'quyet.pv2416331@sis.hust.edu.vn', '0912345001', 'MANAGER', 'ACTIVE'),"
                + "('staff', '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6', 'Lê Anh Tuấn (NV Quầy POS & Soát vé)', 'tuan.la2416639@sis.hust.edu.vn', '0912345002', 'STAFF', 'ACTIVE'),"
                + "('fnb_staff', '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6', 'Nguyễn Xuân Hồng Quân (NV Quầy F&B)', 'quan.nxh2416321@sis.hust.edu.vn', '0912345003', 'STAFF', 'ACTIVE'),"
                + "('customer', '79c4de8c8577ebfbebe7ecf1951d0b51e66f80cf7b7255d20798917f4886de87', 'Vũ Việt Tiến (Khách hàng VIP)', 'tien.vv2416362@sis.hust.edu.vn', '0987654321', 'CUSTOMER', 'ACTIVE'),"
                + "('bach_hs', '79c4de8c8577ebfbebe7ecf1951d0b51e66f80cf7b7255d20798917f4886de87', 'Hà Sỹ Bách (Khách hàng thành viên)', 'bach.hs2416129@sis.hust.edu.vn', '0987654322', 'CUSTOMER', 'ACTIVE'),"
                + "('locked_user', '79c4de8c8577ebfbebe7ecf1951d0b51e66f80cf7b7255d20798917f4886de87', 'Trần Văn Khóa (Tài khoản bị khóa)', 'locked@cinehub.vn', '0900000000', 'CUSTOMER', 'LOCKED');"
                + "INSERT INTO movies (title, genre, duration_minutes, age_rating, director, cast_list, description, poster_url, release_date, status) VALUES "
                + "('Địa Đạo: Mặt Trời Trong Bóng Tối', 'Lịch sử / Hành động', 128, 'T16', 'Bùi Thạc Chuyên', 'Thái Hòa, Quang Tuấn', 'Cuộc chiến đấu kiên cường tại địa đạo Củ Chi.', '/posters/diadao.svg', '2026-04-30', 'SHOWING'),"
                + "('Lật Mặt 8: Vòng Tay Nắng', 'Gia đình / Tâm lý', 135, 'T13', 'Lý Hải', 'NSƯT Kim Phương, Long Đẹp Trai', 'Hành trình theo đuổi ước mơ âm nhạc.', '/posters/latmat8.svg', '2026-05-01', 'SHOWING'),"
                + "('Thám Tử Kiên: Kỳ Án Không Đầu', 'Trinh thám / Cổ trang', 122, 'T16', 'Victor Vũ', 'Quốc Huy, Đinh Ngọc Diệp', 'Thám tử Kiên điều tra chuỗi vụ án bí ẩn.', '/posters/thamtukien.svg', '2026-05-05', 'SHOWING'),"
                + "('Doraemon: Thế Giới Trong Tranh', 'Hoạt hình / Phiêu lưu', 105, 'P', 'Teramoto Yukiyo', 'Wasabi Mizuta', 'Cuộc phiêu lưu vào vương quốc hội họa.', '/posters/doraemon.svg', '2026-05-15', 'SHOWING'),"
                + "('Mission: Impossible - Nghiệp Báo Cuối Cùng', 'Hành động / Điệp viên', 163, 'T16', 'Christopher McQuarrie', 'Tom Cruise', 'Nhiệm vụ giải cứu thế giới.', '/posters/mi8.svg', '2026-05-22', 'SHOWING'),"
                + "('Biệt Đội Sấm Sét (Thunderbolts*)', 'Hành động / Siêu anh hùng', 126, 'T13', 'Jake Schreier', 'Florence Pugh', 'Nhóm phản anh hùng Marvel.', '/posters/thunderbolts.svg', '2026-06-01', 'COMING_SOON');"
                + "INSERT INTO auditoriums (name, room_type, total_rows, seats_per_row, total_seats, status) VALUES "
                + "('Phòng 01 - IMAX Laser', 'IMAX Laser', 6, 10, 60, 'ACTIVE'),"
                + "('Phòng 02 - Dolby Atmos', '2D Dolby Atmos', 5, 10, 50, 'ACTIVE'),"
                + "('Phòng 03 - Gold Class VIP', 'Gold Class', 4, 8, 32, 'ACTIVE');"
                + "INSERT INTO combos (combo_code, name, description, items_included, price, stock_quantity, status) VALUES "
                + "('CB01', 'Solo Bắp Ngọt & Pepsi', 'Combo tiết kiệm 1 người', '1 Bắp Ngọt (L) + 1 Pepsi (L)', 79000, 150, 'AVAILABLE'),"
                + "('CB02', 'Couple Caramel Ngọt Ngào', 'Combo đôi bán chạy nhất', '1 Bắp Caramel (XL) + 2 Pepsi (L)', 115000, 120, 'AVAILABLE'),"
                + "('CB03', 'Family Party 4 Người', 'Combo gia đình 4 người', '2 Bắp Mix (XL) + 4 Nước (L) + 1 Snack', 219000, 80, 'AVAILABLE'),"
                + "('CB04', 'CineHub Collector Ly Đổi Màu', 'Combo kèm Ly giới hạn', '1 Ly 32oz + 1 Bắp Phô mai (L) + 1 Nước (L)', 169000, 45, 'AVAILABLE'),"
                + "('CB05', 'Hotdog & Trà Đào Cam Sả', 'Bữa ăn nhẹ nóng hổi', '1 Hotdog Đức + 1 Trà đào cam sả (L)', 89000, 60, 'AVAILABLE'),"
                + "('CB06', 'Combo Bắp Cay Tứ Xuyên (Ngừng KD)', 'Sản phẩm ngừng kinh doanh', '1 Bắp Cay + 1 Coca Zero', 85000, 0, 'DISCONTINUED');";
    }
}
