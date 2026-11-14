package com.cinehub.services;

import com.cinehub.models.Combo;
import com.cinehub.models.Invoice;
import com.cinehub.models.SeatPricingPolicy;
import com.cinehub.models.ShowtimeSeat;
import com.cinehub.models.Ticket;
import com.cinehub.utils.AppConfig;
import com.cinehub.utils.DatabaseConnection;
import com.cinehub.utils.PasswordUtils;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Lớp dịch vụ trung tâm xử lý Đặt vé trực tuyến (CSD01), Bán vé tại quầy POS (CSD02)
 * và Soát vé điện tử bằng mã QR (CSD05).
 * Đảm bảo thực thi nghiêm ngặt các quy tắc nghiệp vụ BR02, BR03, BR04, BR05 và xử lý
 * triệt để rủi ro R01 (xung đột 2 người dùng đặt trùng 1 ghế tại cùng thời điểm).
 *
 * @author Hà Sỹ Bách (202416129)
 * @author Lê Anh Tuấn (202416639)
 * @version 1.0
 */
public class BookingService {

    /** Thời gian khóa ghế tạm thời theo quy tắc BR03 (mặc định 5 phút, cấu hình booking.hold.minutes). */
    public static final int HOLD_DURATION_MINUTES = AppConfig.getInt("booking.hold.minutes", 5);

    /** Số ghế tối đa trong một lần giữ ghế hoặc một hóa đơn. */
    public static final int MAX_SEATS_PER_ORDER = 8;

    /** Kênh bán hợp lệ (báo cáo doanh thu Online + POS phải cộng đúng bằng tổng). */
    private static final List<String> CHANNELS = List.of("ONLINE", "POS");
    /** Phương thức thanh toán hợp lệ cho kênh Online. */
    private static final List<String> ONLINE_PAYMENTS = List.of("VIETQR", "E_WALLET", "CARD");
    /** Phương thức thanh toán hợp lệ tại quầy POS. */
    private static final List<String> POS_PAYMENTS = List.of("CASH", "VIETQR", "CARD");

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter INV_FMT = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final DateTimeFormatter VN_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Khóa đồng bộ mức tiến trình để đảm bảo tuần tự hóa việc giữ ghế và thanh toán (R01). */
    private static final Object SEAT_LOCK_MONITOR = new Object();

    /**
     * Kết quả trả về của thao tác Giữ ghế tạm thời (holdSeats) hoặc Thanh toán (confirmCheckout).
     */
    public static class BookingResult {
        private final boolean success;
        private final String code;
        private final String message;
        private final String lockedUntil;
        private final Invoice invoice;
        private final List<Ticket> tickets;

        public BookingResult(boolean success, String code, String message,
                             String lockedUntil, Invoice invoice, List<Ticket> tickets) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.lockedUntil = lockedUntil;
            this.invoice = invoice;
            this.tickets = tickets;
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

        public String getLockedUntil() {
            return lockedUntil;
        }

        public Invoice getInvoice() {
            return invoice;
        }

        public List<Ticket> getTickets() {
            return tickets;
        }
    }

    /**
     * Giải phóng tự động tất cả các ghế đang giữ tạm thời (HELD) đã quá hạn 5 phút (Quy tắc BR03).
     *
     * @return Số lượng ghế quá hạn vừa được giải phóng về trạng thái AVAILABLE
     */
    public int releaseExpiredHolds() {
        synchronized (SEAT_LOCK_MONITOR) {
            String nowStr = LocalDateTime.now().format(TS_FMT);
            String sql = "UPDATE showtime_seats SET status = 'AVAILABLE', held_by_user_id = NULL, locked_until = NULL "
                    + "WHERE status = 'HELD' AND locked_until IS NOT NULL AND locked_until < ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nowStr);
                return ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("Lỗi giải phóng ghế quá hạn: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Lấy sơ đồ trạng thái ghế theo thời gian thực của một suất chiếu (Quy tắc BR02 & BR03).
     * Tự động giải phóng các ghế hết hạn giữ trước khi trả về dữ liệu.
     *
     * @param showtimeId Mã suất chiếu
     * @return Danh sách {@link ShowtimeSeat}
     */
    public List<ShowtimeSeat> getSeatMapForShowtime(int showtimeId) {
        releaseExpiredHolds();
        List<ShowtimeSeat> seats = new ArrayList<>();
        String sql = "SELECT ss.showtime_seat_id, ss.showtime_id, ss.seat_id, s.seat_code, "
                + "s.row_label, s.seat_number, s.seat_type, ss.status, ss.held_by_user_id, "
                + "ss.locked_until, ss.final_price "
                + "FROM showtime_seats ss "
                + "JOIN seats s ON ss.seat_id = s.seat_id "
                + "WHERE ss.showtime_id = ? "
                + "ORDER BY s.row_label ASC, s.seat_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer heldBy = rs.getObject("held_by_user_id") != null
                            ? rs.getInt("held_by_user_id") : null;
                    String lockedUntil = rs.getString("locked_until");
                    if (lockedUntil != null && lockedUntil.length() >= 19) {
                        lockedUntil = lockedUntil.substring(0, 19);
                    }
                    seats.add(new ShowtimeSeat(
                            rs.getInt("showtime_seat_id"),
                            rs.getInt("showtime_id"),
                            rs.getInt("seat_id"),
                            rs.getString("seat_code"),
                            rs.getString("row_label"),
                            rs.getInt("seat_number"),
                            rs.getString("seat_type"),
                            rs.getString("status"),
                            heldBy,
                            lockedUntil,
                            rs.getDouble("final_price")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn sơ đồ ghế: " + ex.getMessage(), ex);
        }
        return seats;
    }

    /**
     * Giữ ghế tạm thời trong 5 phút cho người dùng đang chọn ghế (Quy tắc BR02, BR03 & R01).
     * Cấu trúc luồng điều khiển tường minh phục vụ Kiểm thử Hộp trắng (CFG V(G) = 6):
     * - Node 1: Kiểm tra showtimeId <= 0 hoặc danh sách ghế rỗng hoặc vượt quá 8 ghế/lần
     * - Node 2: Trả về lỗi INVALID_SELECTION
     * - Node 3: Giải phóng các ghế HELD đã quá hạn (releaseExpiredHolds) và mở Transaction
     * - Node 4: Vòng lặp kiểm tra từng ghế trong danh sách yêu cầu
     * - Node 5: Kiểm tra ghế có tồn tại trong suất chiếu và đang AVAILABLE (hoặc đang HELD bởi chính userId)
     * - Node 6: Nếu ghế đã BOOKED hoặc đang HELD bởi người khác -> Rollback và trả về SEAT_UNAVAILABLE
     * - Node 7: Cập nhật trạng thái tất cả các ghế được chọn sang HELD và đặt locked_until = NOW + 5 phút
     * - Node 8: Commit Transaction và trả về kết quả HELD_SUCCESS
     *
     * @param showtimeId      Mã suất chiếu
     * @param showtimeSeatIds Danh sách mã showtime_seat_id cần giữ
     * @param userId          Mã người dùng thực hiện giữ ghế
     * @return Đối tượng {@link BookingResult}
     */
    public BookingResult holdSeats(int showtimeId, List<Integer> requestedSeatIds, int userId) {
        synchronized (SEAT_LOCK_MONITOR) {
            List<Integer> showtimeSeatIds = distinct(requestedSeatIds);
            // [Node 1] Kiểm tra điều kiện biên của danh sách ghế chọn
            if (showtimeId <= 0 || showtimeSeatIds.isEmpty() || showtimeSeatIds.size() > MAX_SEATS_PER_ORDER) {
                // [Node 2] Trả về lỗi số lượng ghế chọn không hợp lệ
                return new BookingResult(false, "INVALID_SELECTION",
                        "Vui lòng chọn từ 1 đến tối đa 8 ghế trong một lần giao dịch.", null, null, null);
            }

            // [Node 3] Giải phóng ghế quá hạn trước khi kiểm tra khóa
            releaseExpiredHolds();
            String lockedUntil = LocalDateTime.now().plusMinutes(HOLD_DURATION_MINUTES).format(TS_FMT);

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    String checkSql = "SELECT ss.status, ss.held_by_user_id, s.seat_code "
                            + "FROM showtime_seats ss JOIN seats s ON ss.seat_id = s.seat_id "
                            + "WHERE ss.showtime_id = ? AND ss.showtime_seat_id = ?";
                    // UPDATE có điều kiện: CSDL tự từ chối nếu ghế vừa đổi trạng thái (an toàn cả khi chạy nhiều tiến trình)
                    String updateSql = "UPDATE showtime_seats SET status = 'HELD', held_by_user_id = ?, locked_until = ? "
                            + "WHERE showtime_seat_id = ? AND (status = 'AVAILABLE' "
                            + "OR (status = 'HELD' AND held_by_user_id = ?))";

                    try (PreparedStatement psCheck = conn.prepareStatement(checkSql);
                         PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                        // [Node 4] Duyệt kiểm tra từng ghế được yêu cầu
                        for (int ssId : showtimeSeatIds) {
                            psCheck.setInt(1, showtimeId);
                            psCheck.setInt(2, ssId);
                            try (ResultSet rs = psCheck.executeQuery()) {
                                // [Node 5] Kiểm tra sự tồn tại và trạng thái khả dụng của ghế
                                if (!rs.next()) {
                                    conn.rollback();
                                    return new BookingResult(false, "SEAT_NOT_FOUND",
                                            "Ghế chọn (#" + ssId + ") không thuộc suất chiếu này.", null, null, null);
                                }
                                String status = rs.getString("status");
                                String seatCode = rs.getString("seat_code");
                                Integer heldBy = rs.getObject("held_by_user_id") != null
                                        ? rs.getInt("held_by_user_id") : null;

                                boolean canHold = "AVAILABLE".equalsIgnoreCase(status)
                                        || ("HELD".equalsIgnoreCase(status) && heldBy != null && heldBy == userId);
                                if (!canHold) {
                                    // [Node 6] Ghế đã bị người khác giữ hoặc đặt -> Chặn xung đột R01
                                    conn.rollback();
                                    return new BookingResult(false, "SEAT_ALREADY_TAKEN",
                                            "Ghế " + seatCode + " vừa được khách hàng khác giữ hoặc đặt mua. Vui lòng chọn ghế khác!",
                                            null, null, null);
                                }
                            }
                            // [Node 7] Khóa ghế tạm thời cho người dùng hiện tại
                            psUpdate.setInt(1, userId);
                            psUpdate.setString(2, lockedUntil);
                            psUpdate.setInt(3, ssId);
                            psUpdate.setInt(4, userId);
                            if (psUpdate.executeUpdate() != 1) {
                                conn.rollback();
                                return new BookingResult(false, "SEAT_ALREADY_TAKEN",
                                        "Ghế vừa được khách hàng khác giữ hoặc đặt mua. Vui lòng chọn ghế khác!",
                                        null, null, null);
                            }
                        }
                    }

                    // [Node 8] Xác nhận giao dịch giữ ghế thành công
                    conn.commit();
                    return new BookingResult(true, "HELD_SUCCESS",
                            "Đã giữ tạm thời " + showtimeSeatIds.size() + " ghế trong " + HOLD_DURATION_MINUTES
                            + " phút (đến " + lockedUntil + ").",
                            lockedUntil, null, null);
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Lỗi giao dịch giữ ghế: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Xác nhận thanh toán đơn đặt vé Online (CSD01) hoặc Bán vé tại quầy POS (CSD02)
     * theo Quy tắc nghiệp vụ BR04 & BR05.
     * Tính tổng tiền bằng Đa hình {@link SeatPricingPolicy} + {@link SeatPricingPolicy.ComboOrderItem},
     * trừ tồn kho Combo, chuyển trạng thái ghế sang BOOKED và phát hành Vé điện tử QR (ISSUED).
     *
     * @param showtimeId      Mã suất chiếu
     * @param showtimeSeatIds Danh sách mã ghế theo suất chiếu cần mua
     * @param comboQuantities Bản đồ (comboId -> số lượng) khách chọn mua kèm (có thể rỗng)
     * @param userId          Mã người dùng (Khách hàng hoặc Nhân viên)
     * @param customerName    Họ tên khách hàng in trên hóa đơn
     * @param customerPhone   Số điện thoại khách hàng
     * @param bookingChannel  Kênh đặt vé (ONLINE hoặc POS)
     * @param paymentMethod   Phương thức thanh toán (VIETQR, CASH, CARD, E_WALLET)
     * @return Đối tượng {@link BookingResult} chứa Hóa đơn và Danh sách Vé điện tử QR
     */
    public BookingResult confirmCheckout(int showtimeId, List<Integer> requestedSeatIds,
                                         Map<Integer, Integer> comboQuantities,
                                         int userId, String customerName, String customerPhone,
                                         String bookingChannel, String paymentMethod) {
        synchronized (SEAT_LOCK_MONITOR) {
            List<Integer> showtimeSeatIds = distinct(requestedSeatIds);
            if (showtimeId <= 0 || showtimeSeatIds.isEmpty()) {
                return new BookingResult(false, "EMPTY_CART",
                        "Vui lòng chọn ít nhất 1 ghế trước khi thanh toán.", null, null, null);
            }
            if (showtimeSeatIds.size() > MAX_SEATS_PER_ORDER) {
                return new BookingResult(false, "INVALID_SELECTION",
                        "Mỗi hóa đơn được mua tối đa " + MAX_SEATS_PER_ORDER + " ghế.", null, null, null);
            }

            String channel = PasswordUtils.isBlank(bookingChannel) ? "ONLINE" : bookingChannel.trim().toUpperCase();
            String payMethod = PasswordUtils.isBlank(paymentMethod) ? "VIETQR" : paymentMethod.trim().toUpperCase();
            // Chỉ nhận đúng 2 kênh để báo cáo Online + POS luôn bằng tổng doanh thu
            if (!CHANNELS.contains(channel)) {
                return new BookingResult(false, "INVALID_CHANNEL",
                        "Kênh bán '" + channel + "' không hợp lệ (chỉ ONLINE hoặc POS).", null, null, null);
            }
            List<String> allowedPayments = "POS".equals(channel) ? POS_PAYMENTS : ONLINE_PAYMENTS;
            if (!allowedPayments.contains(payMethod)) {
                return new BookingResult(false, "INVALID_PAYMENT_METHOD",
                        "Phương thức thanh toán '" + payMethod + "' không áp dụng cho kênh " + channel + ".",
                        null, null, null);
            }
            releaseExpiredHolds();

            String custName = PasswordUtils.isBlank(customerName) ? "Khách hàng CineHub" : customerName.trim();
            String custPhone = PasswordUtils.isBlank(customerPhone) ? "0900000000" : customerPhone.trim();

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // 1. Lấy thông tin suất chiếu và giá cơ sở
                    String movieTitle;
                    String auditoriumName;
                    String startTime;
                    double basePrice;
                    String stSql = "SELECT m.title, a.name, st.start_time, st.base_price, st.status "
                            + "FROM showtimes st JOIN movies m ON st.movie_id = m.movie_id "
                            + "JOIN auditoriums a ON st.auditorium_id = a.auditorium_id "
                            + "WHERE st.showtime_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(stSql)) {
                        ps.setInt(1, showtimeId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || !"SCHEDULED".equalsIgnoreCase(rs.getString("status"))) {
                                conn.rollback();
                                return new BookingResult(false, "SHOWTIME_INVALID",
                                        "Suất chiếu không tồn tại hoặc đã ngừng nhận đặt vé.", null, null, null);
                            }
                            movieTitle = rs.getString("title");
                            auditoriumName = rs.getString("name");
                            startTime = rs.getString("start_time");
                            if (startTime != null && startTime.length() >= 19) {
                                startTime = startTime.substring(0, 19);
                            }
                            basePrice = rs.getDouble("base_price");
                        }
                    }

                    // 2. Kiểm tra trạng thái ghế và tính tổng tiền ghế theo BR05 (Đa hình SeatPricingPolicy)
                    double seatTotal = 0.0;
                    List<String> seatCodes = new ArrayList<>();
                    List<String> seatTypes = new ArrayList<>();
                    List<Double> seatPrices = new ArrayList<>();

                    String checkSeatSql = "SELECT ss.status, ss.held_by_user_id, s.seat_code, s.seat_type "
                            + "FROM showtime_seats ss JOIN seats s ON ss.seat_id = s.seat_id "
                            + "WHERE ss.showtime_id = ? AND ss.showtime_seat_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(checkSeatSql)) {
                        for (int ssId : showtimeSeatIds) {
                            ps.setInt(1, showtimeId);
                            ps.setInt(2, ssId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (!rs.next()) {
                                    conn.rollback();
                                    return new BookingResult(false, "SEAT_NOT_FOUND",
                                            "Ghế chọn không hợp lệ.", null, null, null);
                                }
                                String status = rs.getString("status");
                                String seatCode = rs.getString("seat_code");
                                String seatType = rs.getString("seat_type");
                                Integer heldBy = rs.getObject("held_by_user_id") != null
                                        ? rs.getInt("held_by_user_id") : null;

                                boolean heldByMe = "HELD".equalsIgnoreCase(status) && heldBy != null && heldBy == userId;
                                // BR03: Online phải giữ ghế trước; POS bán trực tiếp ghế trống tại quầy
                                if ("ONLINE".equals(channel) && !heldByMe) {
                                    conn.rollback();
                                    boolean takenByOther = "BOOKED".equalsIgnoreCase(status)
                                            || "HELD".equalsIgnoreCase(status);
                                    return new BookingResult(false, takenByOther ? "SEAT_CONFLICT" : "SEAT_NOT_HELD",
                                            takenByOther
                                                    ? "Ghế " + seatCode + " đã được khách khác thanh toán hoặc đang được giữ."
                                                    : "Ghế " + seatCode + " chưa được giữ cho bạn hoặc đã hết "
                                                    + HOLD_DURATION_MINUTES + " phút giữ chỗ. Vui lòng giữ ghế lại.",
                                            null, null, null);
                                }
                                boolean validForCheckout = "AVAILABLE".equalsIgnoreCase(status) || heldByMe;
                                if (!validForCheckout) {
                                    conn.rollback();
                                    return new BookingResult(false, "SEAT_CONFLICT",
                                            "Ghế " + seatCode + " đã được khách khác thanh toán hoặc đang được giữ.",
                                            null, null, null);
                                }
                                double price = SeatPricingPolicy.forSeatType(seatType).calculateSeatPrice(basePrice);
                                seatCodes.add(seatCode);
                                seatTypes.add(seatType);
                                seatPrices.add(price);
                                seatTotal += price;
                            }
                        }
                    }

                    // 3. Kiểm tra và tính tổng tiền Combo F&B theo BR05 (áp dụng NullComboOrderItem nếu không chọn)
                    List<SeatPricingPolicy.ComboOrderItem> comboItems = new ArrayList<>();
                    if (comboQuantities != null && !comboQuantities.isEmpty()) {
                        String comboSql = "SELECT combo_id, combo_code, name, description, items_included, "
                                + "price, stock_quantity, status FROM combos WHERE combo_id = ?";
                        try (PreparedStatement psCombo = conn.prepareStatement(comboSql)) {
                            for (Map.Entry<Integer, Integer> entry : comboQuantities.entrySet()) {
                                int cId = entry.getKey();
                                int qty = entry.getValue() != null ? entry.getValue() : 0;
                                if (qty <= 0) {
                                    continue;
                                }
                                psCombo.setInt(1, cId);
                                try (ResultSet rs = psCombo.executeQuery()) {
                                    if (!rs.next()) {
                                        conn.rollback();
                                        return new BookingResult(false, "COMBO_NOT_FOUND",
                                                "Gói Combo #" + cId + " không tồn tại.", null, null, null);
                                    }
                                    String cStatus = rs.getString("status");
                                    int stock = rs.getInt("stock_quantity");
                                    String cName = rs.getString("name");
                                    if (!"AVAILABLE".equalsIgnoreCase(cStatus) || stock < qty) {
                                        conn.rollback();
                                        return new BookingResult(false, "COMBO_OUT_OF_STOCK",
                                                "Combo '" + cName + "' không đủ tồn kho (còn " + stock + " phần).",
                                                null, null, null);
                                    }
                                    Combo comboObj = new Combo(
                                            cId,
                                            rs.getString("combo_code"),
                                            cName,
                                            rs.getString("description"),
                                            rs.getString("items_included"),
                                            rs.getDouble("price"),
                                            stock,
                                            cStatus
                                    );
                                    comboItems.add(new SeatPricingPolicy.ComboOrderItem(comboObj, qty));
                                }
                            }
                        }
                    }
                    if (comboItems.isEmpty()) {
                        comboItems.add(new SeatPricingPolicy.NullComboOrderItem());
                    }

                    double comboTotal = 0.0;
                    StringBuilder comboSummarySb = new StringBuilder();
                    for (SeatPricingPolicy.ComboOrderItem item : comboItems) {
                        comboTotal += item.getSubtotal();
                        if (!item.isNull()) {
                            if (comboSummarySb.length() > 0) {
                                comboSummarySb.append(", ");
                            }
                            comboSummarySb.append(item.getQuantity()).append("x ").append(item.getCombo().getName());
                        }
                    }
                    String comboSummary = comboSummarySb.length() > 0
                            ? comboSummarySb.toString() : "Không chọn Combo";
                    double totalAmount = seatTotal + comboTotal;

                    // 4. Tạo bản ghi Hóa đơn (Invoice)
                    String invoiceCode = newInvoiceCode();
                    int invoiceId;
                    String insertInv = "INSERT INTO invoices (invoice_code, user_id, customer_name, customer_phone, "
                            + "staff_id, showtime_id, booking_channel, payment_method, seat_total, combo_total, "
                            + "discount_amount, total_amount, combo_summary, payment_status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, 'PAID')";
                    try (PreparedStatement ps = conn.prepareStatement(insertInv, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, invoiceCode);
                        ps.setInt(2, userId);
                        ps.setString(3, custName);
                        ps.setString(4, custPhone);
                        if ("POS".equalsIgnoreCase(channel)) {
                            ps.setInt(5, userId);
                        } else {
                            ps.setNull(5, java.sql.Types.INTEGER);
                        }
                        ps.setInt(6, showtimeId);
                        ps.setString(7, channel);
                        ps.setString(8, payMethod);
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

                    // 5. Lưu chi tiết Combo và trừ tồn kho
                    String insertIC = "INSERT INTO invoice_combos (invoice_id, combo_id, quantity, unit_price, subtotal) "
                            + "VALUES (?, ?, ?, ?, ?)";
                    // Trừ tồn kho có điều kiện; hết hàng thì tự chuyển OUT_OF_STOCK
                    String deductStock = "UPDATE combos SET stock_quantity = stock_quantity - ?, "
                            + "status = CASE WHEN stock_quantity - ? <= 0 THEN 'OUT_OF_STOCK' ELSE status END "
                            + "WHERE combo_id = ? AND stock_quantity >= ?";
                    try (PreparedStatement psIC = conn.prepareStatement(insertIC);
                         PreparedStatement psStock = conn.prepareStatement(deductStock)) {
                        for (SeatPricingPolicy.ComboOrderItem item : comboItems) {
                            if (!item.isNull()) {
                                psIC.setInt(1, invoiceId);
                                psIC.setInt(2, item.getCombo().getComboId());
                                psIC.setInt(3, item.getQuantity());
                                psIC.setDouble(4, item.getCombo().getPrice());
                                psIC.setDouble(5, item.getSubtotal());
                                psIC.executeUpdate();

                                psStock.setInt(1, item.getQuantity());
                                psStock.setInt(2, item.getQuantity());
                                psStock.setInt(3, item.getCombo().getComboId());
                                psStock.setInt(4, item.getQuantity());
                                if (psStock.executeUpdate() != 1) {
                                    conn.rollback();
                                    return new BookingResult(false, "COMBO_OUT_OF_STOCK",
                                            "Combo '" + item.getCombo().getName() + "' vừa hết hàng.", null, null, null);
                                }
                            }
                        }
                    }

                    // 6. Cập nhật ghế sang BOOKED (UPDATE có điều kiện chống bán trùng - R01) và phát hành vé QR
                    String updateSeat = "UPDATE showtime_seats SET status = 'BOOKED', held_by_user_id = NULL, "
                            + "locked_until = NULL WHERE showtime_seat_id = ? AND (status = 'AVAILABLE' "
                            + "OR (status = 'HELD' AND held_by_user_id = ?))";
                    String insertTkt = "INSERT INTO tickets (ticket_code, qr_payload, invoice_id, showtime_id, "
                            + "showtime_seat_id, seat_code, seat_type, ticket_price, ticket_status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ISSUED')";
                    List<Ticket> issuedTickets = new ArrayList<>();

                    try (PreparedStatement psSeat = conn.prepareStatement(updateSeat);
                         PreparedStatement psTkt = conn.prepareStatement(insertTkt, Statement.RETURN_GENERATED_KEYS)) {
                        for (int i = 0; i < showtimeSeatIds.size(); i++) {
                            int ssId = showtimeSeatIds.get(i);
                            String sCode = seatCodes.get(i);
                            String sType = seatTypes.get(i);
                            double sPrice = seatPrices.get(i);

                            psSeat.setInt(1, ssId);
                            psSeat.setInt(2, userId);
                            if (psSeat.executeUpdate() != 1) {
                                conn.rollback();
                                return new BookingResult(false, "SEAT_CONFLICT",
                                        "Ghế " + sCode + " vừa được khách khác thanh toán.", null, null, null);
                            }

                            String ticketCode = String.format("TKT-%03d-%s", invoiceId, sCode);
                            String qrPayload = "CINEHUB|QR|" + ticketCode + "|SHOW" + showtimeId + "|" + sCode;

                            psTkt.setString(1, ticketCode);
                            psTkt.setString(2, qrPayload);
                            psTkt.setInt(3, invoiceId);
                            psTkt.setInt(4, showtimeId);
                            psTkt.setInt(5, ssId);
                            psTkt.setString(6, sCode);
                            psTkt.setString(7, sType);
                            psTkt.setDouble(8, sPrice);
                            psTkt.executeUpdate();

                            int ticketId;
                            try (ResultSet keys = psTkt.getGeneratedKeys()) {
                                keys.next();
                                ticketId = keys.getInt(1);
                            }

                            Ticket t = new Ticket();
                            t.setTicketId(ticketId);
                            t.setTicketCode(ticketCode);
                            t.setQrPayload(qrPayload);
                            t.setInvoiceId(invoiceId);
                            t.setInvoiceCode(invoiceCode);
                            t.setCustomerName(custName);
                            t.setShowtimeId(showtimeId);
                            t.setShowtimeSeatId(ssId);
                            t.setSeatCode(sCode);
                            t.setSeatType(sType);
                            t.setMovieTitle(movieTitle);
                            t.setAuditoriumName(auditoriumName);
                            t.setStartTime(startTime);
                            t.setTicketPrice(sPrice);
                            t.setTicketStatus("ISSUED");
                            issuedTickets.add(t);
                        }
                    }

                    conn.commit();

                    Invoice invoice = new Invoice();
                    invoice.setInvoiceId(invoiceId);
                    invoice.setInvoiceCode(invoiceCode);
                    invoice.setUserId(userId);
                    invoice.setCustomerName(custName);
                    invoice.setCustomerPhone(custPhone);
                    invoice.setShowtimeId(showtimeId);
                    invoice.setMovieTitle(movieTitle);
                    invoice.setAuditoriumName(auditoriumName);
                    invoice.setStartTime(startTime);
                    invoice.setBookingChannel(channel);
                    invoice.setPaymentMethod(payMethod);
                    invoice.setSeatTotal(seatTotal);
                    invoice.setComboTotal(comboTotal);
                    invoice.setTotalAmount(totalAmount);
                    invoice.setComboSummary(comboSummary);
                    invoice.setPaymentStatus("PAID");
                    invoice.setCreatedAt(LocalDateTime.now().format(TS_FMT));

                    return new BookingResult(true, "CHECKOUT_SUCCESS",
                            "Thanh toán thành công hóa đơn " + invoiceCode + "! Đã phát hành "
                            + issuedTickets.size() + " vé điện tử QR.",
                            null, invoice, issuedTickets);
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Lỗi giao dịch thanh toán: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Nghiệp vụ Soát vé điện tử bằng mã QR hoặc mã vé tại cửa phòng chiếu (Phân hệ CSD05 - Quy tắc BR04).
     *
     * @param qrOrTicketCode Chuỗi mã QR quét được hoặc mã vé (ví dụ: TKT-002-D04)
     * @param staffId        Mã nhân viên soát vé
     * @return Đối tượng {@link BookingResult} chứa thông tin vé sau khi kiểm tra
     */
    public BookingResult checkInTicket(String qrOrTicketCode, int staffId) {
        if (PasswordUtils.isBlank(qrOrTicketCode)) {
            return new BookingResult(false, "EMPTY_CODE",
                    "Vui lòng nhập hoặc quét mã vé QR cần soát.", null, null, null);
        }
        String input = qrOrTicketCode.trim();
        String sql = "SELECT t.ticket_id, t.ticket_code, t.qr_payload, t.invoice_id, i.invoice_code, "
                + "i.customer_name, t.showtime_id, t.showtime_seat_id, t.seat_code, t.seat_type, "
                + "t.ticket_price, t.ticket_status, t.checked_in_at, m.title AS movie_title, "
                + "a.name AS auditorium_name, st.start_time "
                + "FROM tickets t "
                + "JOIN invoices i ON t.invoice_id = i.invoice_id "
                + "JOIN showtimes st ON t.showtime_id = st.showtime_id "
                + "JOIN movies m ON st.movie_id = m.movie_id "
                + "JOIN auditoriums a ON st.auditorium_id = a.auditorium_id "
                + "WHERE UPPER(t.ticket_code) = UPPER(?) OR UPPER(t.qr_payload) = UPPER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, input);
            ps.setString(2, input);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new BookingResult(false, "TICKET_NOT_FOUND",
                            "CẢNH BÁO: Mã vé '" + input + "' không tồn tại trên hệ thống!", null, null, null);
                }
                Ticket t = mapRowToTicket(rs);
                if ("CANCELLED".equalsIgnoreCase(t.getTicketStatus())) {
                    List<Ticket> list = new ArrayList<>();
                    list.add(t);
                    return new BookingResult(false, "TICKET_CANCELLED",
                            "TỪ CHỐI: Vé " + t.getTicketCode() + " đã bị hủy trước đó, không có giá trị vào phòng chiếu!",
                            null, null, list);
                }
                if ("CHECKED_IN".equalsIgnoreCase(t.getTicketStatus())) {
                    List<Ticket> list = new ArrayList<>();
                    list.add(t);
                    return new BookingResult(false, "ALREADY_CHECKED_IN",
                            "CẢNH BÁO TRÙNG VÉ: Vé " + t.getTicketCode() + " (Ghế " + t.getSeatCode()
                            + ") đã được soát lúc " + formatViTime(t.getCheckedInAt()) + "!",
                            null, null, list);
                }

                // Vé chỉ có giá trị trong ngày chiếu của suất (chặn vé ngày khác)
                String showDate = t.getStartTime() != null && t.getStartTime().length() >= 10
                        ? t.getStartTime().substring(0, 10) : "";
                if (!LocalDate.now().toString().equals(showDate)) {
                    List<Ticket> list = new ArrayList<>();
                    list.add(t);
                    return new BookingResult(false, "WRONG_DAY",
                            "TỪ CHỐI: Vé " + t.getTicketCode() + " dành cho suất chiếu lúc "
                            + formatViTime(t.getStartTime()) + ", không phải hôm nay.",
                            null, null, list);
                }

                String checkedInTime = LocalDateTime.now().format(TS_FMT);
                // UPDATE có điều kiện: 2 máy quét cùng lúc chỉ 1 máy thành công
                try (PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE tickets SET ticket_status = 'CHECKED_IN', checked_in_at = ?, checked_in_by = ? "
                        + "WHERE ticket_id = ? AND ticket_status = 'ISSUED'")) {
                    psUpd.setString(1, checkedInTime);
                    psUpd.setInt(2, staffId);
                    psUpd.setInt(3, t.getTicketId());
                    if (psUpd.executeUpdate() != 1) {
                        List<Ticket> list = new ArrayList<>();
                        list.add(t);
                        return new BookingResult(false, "ALREADY_CHECKED_IN",
                                "CẢNH BÁO TRÙNG VÉ: Vé " + t.getTicketCode() + " vừa được soát ở cửa khác!",
                                null, null, list);
                    }
                }
                t.setTicketStatus("CHECKED_IN");
                t.setCheckedInAt(checkedInTime);
                t.setCheckedInBy(staffId);
                List<Ticket> list = new ArrayList<>();
                list.add(t);

                return new BookingResult(true, "CHECKIN_SUCCESS",
                        "HỢP LỆ - MỜI VÀO PHÒNG CHIẾU: Vé " + t.getTicketCode() + " | Phim: "
                        + t.getMovieTitle() + " | " + t.getAuditoriumName() + " | Ghế: " + t.getSeatCode(),
                        null, null, list);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi soát vé QR: " + ex.getMessage(), ex);
        }
    }

    /**
     * Hủy vé xem phim trước giờ chiếu và giải phóng ghế về trạng thái AVAILABLE (Quy tắc BR04).
     *
     * @param ticketCode Mã vé cần hủy
     * @return Đối tượng {@link BookingResult}
     */
    public BookingResult cancelTicket(String ticketCode) {
        synchronized (SEAT_LOCK_MONITOR) {
            if (PasswordUtils.isBlank(ticketCode)) {
                return new BookingResult(false, "EMPTY_CODE", "Vui lòng cung cấp mã vé cần hủy.", null, null, null);
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int ticketId;
                    int ssId;
                    int invoiceId;
                    double ticketPrice;
                    String status;
                    String seatCode;
                    String startTime;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT t.ticket_id, t.showtime_seat_id, t.invoice_id, t.ticket_price, t.ticket_status, "
                            + "t.seat_code, st.start_time FROM tickets t "
                            + "JOIN showtimes st ON t.showtime_id = st.showtime_id "
                            + "WHERE UPPER(t.ticket_code) = UPPER(?)")) {
                        ps.setString(1, ticketCode.trim());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                return new BookingResult(false, "TICKET_NOT_FOUND",
                                        "Không tìm thấy mã vé " + ticketCode + ".", null, null, null);
                            }
                            ticketId = rs.getInt("ticket_id");
                            ssId = rs.getInt("showtime_seat_id");
                            invoiceId = rs.getInt("invoice_id");
                            ticketPrice = rs.getDouble("ticket_price");
                            status = rs.getString("ticket_status");
                            seatCode = rs.getString("seat_code");
                            startTime = rs.getString("start_time");
                        }
                    }

                    if (!"ISSUED".equalsIgnoreCase(status)) {
                        conn.rollback();
                        return new BookingResult(false, "CANNOT_CANCEL",
                                "Chỉ được phép hủy vé ở trạng thái ISSUED (trạng thái hiện tại: " + status + ").",
                                null, null, null);
                    }

                    // BR04: chỉ hủy được trước giờ chiếu
                    LocalDateTime start = parseTimestamp(startTime);
                    if (start != null && !LocalDateTime.now().isBefore(start)) {
                        conn.rollback();
                        return new BookingResult(false, "TOO_LATE_TO_CANCEL",
                                "Suất chiếu đã bắt đầu lúc " + start.format(VN_FMT)
                                + ", không thể hủy vé " + ticketCode + ".", null, null, null);
                    }

                    try (PreparedStatement ps1 = conn.prepareStatement(
                            "UPDATE tickets SET ticket_status = 'CANCELLED' WHERE ticket_id = ? AND ticket_status = 'ISSUED'");
                         PreparedStatement ps2 = conn.prepareStatement(
                            "UPDATE showtime_seats SET status = 'AVAILABLE', held_by_user_id = NULL, locked_until = NULL WHERE showtime_seat_id = ?")) {
                        ps1.setInt(1, ticketId);
                        if (ps1.executeUpdate() != 1) {
                            conn.rollback();
                            return new BookingResult(false, "CANNOT_CANCEL",
                                    "Vé " + ticketCode + " vừa đổi trạng thái, vui lòng tải lại.", null, null, null);
                        }
                        ps2.setInt(1, ssId);
                        ps2.executeUpdate();
                    }

                    // Hoàn tiền vé vào hóa đơn để báo cáo doanh thu phản ánh đúng số đã thu
                    String refundNote = refundTicketOnInvoice(conn, invoiceId, ticketPrice);

                    conn.commit();
                    return new BookingResult(true, "CANCEL_SUCCESS",
                            "Đã hủy vé " + ticketCode + ", trả ghế " + seatCode + " về trạng thái trống. " + refundNote,
                            null, null, null);
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Lỗi hủy vé: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Lấy danh sách toàn bộ Vé điện tử đã phát hành trên hệ thống.
     *
     * @return Danh sách {@link Ticket}
     */
    public List<Ticket> getAllTickets() {
        return queryTickets(null);
    }

    /**
     * Lấy danh sách Vé điện tử thuộc các hóa đơn của một khách hàng (phục vụ RBAC: khách hàng
     * chỉ được xem vé của chính mình).
     *
     * @param userId Mã người dùng sở hữu hóa đơn
     * @return Danh sách {@link Ticket}
     */
    public List<Ticket> getTicketsByUser(int userId) {
        return queryTickets(userId);
    }

    /**
     * Tra cứu mã người dùng sở hữu hóa đơn chứa vé (phục vụ kiểm tra quyền hủy vé).
     *
     * @param ticketCode Mã vé
     * @return Mã người dùng sở hữu, hoặc null nếu vé không tồn tại hoặc hóa đơn không gắn tài khoản
     */
    public Integer getTicketOwnerUserId(String ticketCode) {
        if (PasswordUtils.isBlank(ticketCode)) {
            return null;
        }
        String sql = "SELECT i.user_id FROM tickets t JOIN invoices i ON t.invoice_id = i.invoice_id "
                + "WHERE UPPER(t.ticket_code) = UPPER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getObject("user_id") != null) {
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi tra cứu chủ sở hữu vé: " + ex.getMessage(), ex);
        }
        return null;
    }

    private List<Ticket> queryTickets(Integer ownerUserId) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.ticket_id, t.ticket_code, t.qr_payload, t.invoice_id, i.invoice_code, "
                + "i.customer_name, t.showtime_id, t.showtime_seat_id, t.seat_code, t.seat_type, "
                + "t.ticket_price, t.ticket_status, t.checked_in_at, m.title AS movie_title, "
                + "a.name AS auditorium_name, st.start_time "
                + "FROM tickets t "
                + "JOIN invoices i ON t.invoice_id = i.invoice_id "
                + "JOIN showtimes st ON t.showtime_id = st.showtime_id "
                + "JOIN movies m ON st.movie_id = m.movie_id "
                + "JOIN auditoriums a ON st.auditorium_id = a.auditorium_id "
                + (ownerUserId != null ? "WHERE i.user_id = ? " : "")
                + "ORDER BY t.ticket_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (ownerUserId != null) {
                ps.setInt(1, ownerUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapRowToTicket(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn danh sách vé: " + ex.getMessage(), ex);
        }
        return tickets;
    }

    /** Đổi "yyyy-MM-dd HH:mm:ss" sang kiểu hiển thị Việt Nam "dd/MM HH:mm". */
    /** Loại ghế trùng trong danh sách gửi lên, giữ nguyên thứ tự. */
    private static List<Integer> distinct(List<Integer> ids) {
        return ids == null ? new ArrayList<>() : new ArrayList<>(new LinkedHashSet<>(ids));
    }

    /** Mã hóa đơn duy nhất: INV-yyMMddHHmmss-XXXX (thời điểm + 4 ký tự hex ngẫu nhiên). */
    private static String newInvoiceCode() {
        return "INV-" + LocalDateTime.now().format(INV_FMT) + "-"
                + String.format("%04X", RANDOM.nextInt(0x10000));
    }

    private static LocalDateTime parseTimestamp(String ts) {
        if (ts == null || ts.length() < 19) {
            return null;
        }
        try {
            return LocalDateTime.parse(ts.substring(0, 19), TS_FMT);
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * Trừ tiền vé đã hủy khỏi hóa đơn. Khi mọi vé của hóa đơn đều đã hủy, hóa đơn chuyển REFUNDED
     * (hoàn cả tiền combo, trả lại tồn kho combo) và không còn được tính vào doanh thu.
     */
    private String refundTicketOnInvoice(Connection conn, int invoiceId, double ticketPrice) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE invoices SET seat_total = seat_total - ?, total_amount = total_amount - ? WHERE invoice_id = ?")) {
            ps.setDouble(1, ticketPrice);
            ps.setDouble(2, ticketPrice);
            ps.setInt(3, invoiceId);
            ps.executeUpdate();
        }
        int remaining;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM tickets WHERE invoice_id = ? AND ticket_status <> 'CANCELLED'")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                remaining = rs.getInt(1);
            }
        }
        String refund = "Hoàn " + String.format(java.util.Locale.US, "%,.0f", ticketPrice).replace(',', '.') + " ₫ tiền vé.";
        if (remaining == 0) {
            try (PreparedStatement psStock = conn.prepareStatement(
                    "UPDATE combos SET stock_quantity = stock_quantity + ?, "
                    + "status = CASE WHEN status = 'OUT_OF_STOCK' THEN 'AVAILABLE' ELSE status END WHERE combo_id = ?");
                 PreparedStatement psItems = conn.prepareStatement(
                    "SELECT combo_id, quantity FROM invoice_combos WHERE invoice_id = ?")) {
                psItems.setInt(1, invoiceId);
                try (ResultSet rs = psItems.executeQuery()) {
                    while (rs.next()) {
                        psStock.setInt(1, rs.getInt("quantity"));
                        psStock.setInt(2, rs.getInt("combo_id"));
                        psStock.executeUpdate();
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE invoices SET payment_status = 'REFUNDED' WHERE invoice_id = ?")) {
                ps.setInt(1, invoiceId);
                ps.executeUpdate();
            }
            refund += " Hóa đơn không còn vé hiệu lực nên đã hoàn toàn bộ (kể cả combo).";
        }
        return refund;
    }

    private static String formatViTime(String ts) {
        if (ts == null || ts.length() < 16) {
            return ts;
        }
        return ts.substring(8, 10) + "/" + ts.substring(5, 7) + " " + ts.substring(11, 16);
    }

    private Ticket mapRowToTicket(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setTicketId(rs.getInt("ticket_id"));
        t.setTicketCode(rs.getString("ticket_code"));
        t.setQrPayload(rs.getString("qr_payload"));
        t.setInvoiceId(rs.getInt("invoice_id"));
        t.setInvoiceCode(rs.getString("invoice_code"));
        t.setCustomerName(rs.getString("customer_name"));
        t.setShowtimeId(rs.getInt("showtime_id"));
        t.setShowtimeSeatId(rs.getInt("showtime_seat_id"));
        t.setSeatCode(rs.getString("seat_code"));
        t.setSeatType(rs.getString("seat_type"));
        t.setTicketPrice(rs.getDouble("ticket_price"));
        t.setTicketStatus(rs.getString("ticket_status"));
        String checkedInAt = rs.getString("checked_in_at");
        if (checkedInAt != null && checkedInAt.length() >= 19) {
            checkedInAt = checkedInAt.substring(0, 19);
        }
        t.setCheckedInAt(checkedInAt);
        t.setMovieTitle(rs.getString("movie_title"));
        t.setAuditoriumName(rs.getString("auditorium_name"));
        String stTime = rs.getString("start_time");
        if (stTime != null && stTime.length() >= 19) {
            stTime = stTime.substring(0, 19);
        }
        t.setStartTime(stTime);
        return t;
    }
}

