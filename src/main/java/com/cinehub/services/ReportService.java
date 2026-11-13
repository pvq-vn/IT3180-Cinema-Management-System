package com.cinehub.services;

import com.cinehub.models.Invoice;
import com.cinehub.utils.DatabaseConnection;
import com.cinehub.utils.JsonUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp dịch vụ tổng hợp dữ liệu Thống kê và Báo cáo doanh thu rạp chiếu phim (Phân hệ CSD05).
 * Phục vụ Quản lý rạp theo dõi tổng doanh thu vé, doanh thu Combo F&B, tỷ lệ lấp đầy ghế
 * và hiệu quả kinh doanh theo từng bộ phim / kênh bán vé.
 *
 * @author Phạm Văn Quyết (202416331)
 * @version 1.0
 */
public class ReportService {

    /**
     * Lấy danh sách toàn bộ hóa đơn giao dịch gần nhất.
     *
     * @return Danh sách {@link Invoice}
     */
    public List<Invoice> getAllInvoices() {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT i.invoice_id, i.invoice_code, i.user_id, i.customer_name, i.customer_phone, "
                + "i.staff_id, i.showtime_id, m.title AS movie_title, a.name AS auditorium_name, "
                + "st.start_time, i.booking_channel, i.payment_method, i.seat_total, i.combo_total, "
                + "i.discount_amount, i.total_amount, i.combo_summary, i.payment_status, i.created_at "
                + "FROM invoices i "
                + "JOIN showtimes st ON i.showtime_id = st.showtime_id "
                + "JOIN movies m ON st.movie_id = m.movie_id "
                + "JOIN auditoriums a ON st.auditorium_id = a.auditorium_id "
                + "ORDER BY i.invoice_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Invoice inv = new Invoice();
                inv.setInvoiceId(rs.getInt("invoice_id"));
                inv.setInvoiceCode(rs.getString("invoice_code"));
                inv.setUserId(rs.getObject("user_id") != null ? rs.getInt("user_id") : null);
                inv.setCustomerName(rs.getString("customer_name"));
                inv.setCustomerPhone(rs.getString("customer_phone"));
                inv.setShowtimeId(rs.getInt("showtime_id"));
                inv.setMovieTitle(rs.getString("movie_title"));
                inv.setAuditoriumName(rs.getString("auditorium_name"));
                String stTime = rs.getString("start_time");
                inv.setStartTime(stTime != null && stTime.length() >= 19 ? stTime.substring(0, 19) : stTime);
                inv.setBookingChannel(rs.getString("booking_channel"));
                inv.setPaymentMethod(rs.getString("payment_method"));
                inv.setSeatTotal(rs.getDouble("seat_total"));
                inv.setComboTotal(rs.getDouble("combo_total"));
                inv.setDiscountAmount(rs.getDouble("discount_amount"));
                inv.setTotalAmount(rs.getDouble("total_amount"));
                inv.setComboSummary(rs.getString("combo_summary"));
                inv.setPaymentStatus(rs.getString("payment_status"));
                String cAt = rs.getString("created_at");
                inv.setCreatedAt(cAt != null && cAt.length() >= 19 ? cAt.substring(0, 19) : cAt);
                invoices.add(inv);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn danh sách hóa đơn: " + ex.getMessage(), ex);
        }
        return invoices;
    }

    /**
     * Tổng hợp báo cáo doanh thu, lượng vé bán ra, tỷ lệ soát vé Check-in và doanh thu theo phim
     * dưới dạng chuỗi JSON chuẩn để hiển thị trực tiếp lên Dashboard Quản trị.
     *
     * @return Chuỗi JSON chứa toàn bộ chỉ số KPI và bảng phân tích doanh thu
     */
    public String buildDashboardReportJson() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            double totalRevenue = 0;
            double seatRevenue = 0;
            double comboRevenue = 0;
            int totalInvoices = 0;
            double onlineRevenue = 0;
            double posRevenue = 0;

            String kpiSql = "SELECT COUNT(*) AS inv_cnt, COALESCE(SUM(total_amount), 0) AS tot_rev, "
                    + "COALESCE(SUM(seat_total), 0) AS seat_rev, COALESCE(SUM(combo_total), 0) AS combo_rev, "
                    + "COALESCE(SUM(CASE WHEN booking_channel = 'ONLINE' THEN total_amount ELSE 0 END), 0) AS online_rev, "
                    + "COALESCE(SUM(CASE WHEN booking_channel = 'POS' THEN total_amount ELSE 0 END), 0) AS pos_rev "
                    + "FROM invoices WHERE payment_status = 'PAID'";
            try (PreparedStatement ps = conn.prepareStatement(kpiSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalInvoices = rs.getInt("inv_cnt");
                    totalRevenue = rs.getDouble("tot_rev");
                    seatRevenue = rs.getDouble("seat_rev");
                    comboRevenue = rs.getDouble("combo_rev");
                    onlineRevenue = rs.getDouble("online_rev");
                    posRevenue = rs.getDouble("pos_rev");
                }
            }

            int issuedTickets = 0;
            int checkedInTickets = 0;
            int cancelledTickets = 0;
            String tktSql = "SELECT "
                    + "COALESCE(SUM(CASE WHEN ticket_status IN ('ISSUED', 'CHECKED_IN') THEN 1 ELSE 0 END), 0) AS sold_cnt, "
                    + "COALESCE(SUM(CASE WHEN ticket_status = 'CHECKED_IN' THEN 1 ELSE 0 END), 0) AS checkin_cnt, "
                    + "COALESCE(SUM(CASE WHEN ticket_status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancel_cnt "
                    + "FROM tickets";
            try (PreparedStatement ps = conn.prepareStatement(tktSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    issuedTickets = rs.getInt("sold_cnt");
                    checkedInTickets = rs.getInt("checkin_cnt");
                    cancelledTickets = rs.getInt("cancel_cnt");
                }
            }

            // Doanh thu theo từng bộ phim
            StringBuilder movieRevJson = new StringBuilder("[");
            String movieSql = "SELECT m.movie_id, m.title, "
                    + "COUNT(DISTINCT st.showtime_id) AS showtime_cnt, "
                    + "(SELECT COUNT(*) FROM tickets t JOIN showtimes s2 ON t.showtime_id = s2.showtime_id "
                    + " WHERE s2.movie_id = m.movie_id AND t.ticket_status <> 'CANCELLED') AS tickets_sold, "
                    + "COALESCE((SELECT SUM(i.total_amount) FROM invoices i JOIN showtimes s3 ON i.showtime_id = s3.showtime_id "
                    + " WHERE s3.movie_id = m.movie_id AND i.payment_status = 'PAID'), 0) AS revenue "
                    + "FROM movies m LEFT JOIN showtimes st ON m.movie_id = st.movie_id "
                    + "GROUP BY m.movie_id, m.title ORDER BY revenue DESC, m.movie_id ASC";
            try (PreparedStatement ps = conn.prepareStatement(movieSql);
                 ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        movieRevJson.append(",");
                    }
                    first = false;
                    movieRevJson.append("{\"movieId\":").append(rs.getInt("movie_id"))
                            .append(",\"title\":\"").append(JsonUtils.escape(rs.getString("title"))).append("\"")
                            .append(",\"showtimeCount\":").append(rs.getInt("showtime_cnt"))
                            .append(",\"ticketsSold\":").append(rs.getInt("tickets_sold"))
                            .append(",\"revenue\":").append(rs.getDouble("revenue"))
                            .append("}");
                }
            }
            movieRevJson.append("]");

            // Danh sách hóa đơn
            List<Invoice> invoices = getAllInvoices();
            StringBuilder invJson = new StringBuilder("[");
            for (int i = 0; i < invoices.size(); i++) {
                if (i > 0) {
                    invJson.append(",");
                }
                Invoice inv = invoices.get(i);
                invJson.append("{\"invoiceId\":").append(inv.getInvoiceId())
                        .append(",\"invoiceCode\":\"").append(JsonUtils.escape(inv.getInvoiceCode())).append("\"")
                        .append(",\"customerName\":\"").append(JsonUtils.escape(inv.getCustomerName())).append("\"")
                        .append(",\"customerPhone\":\"").append(JsonUtils.escape(inv.getCustomerPhone())).append("\"")
                        .append(",\"movieTitle\":\"").append(JsonUtils.escape(inv.getMovieTitle())).append("\"")
                        .append(",\"auditoriumName\":\"").append(JsonUtils.escape(inv.getAuditoriumName())).append("\"")
                        .append(",\"startTime\":\"").append(JsonUtils.escape(inv.getStartTime())).append("\"")
                        .append(",\"bookingChannel\":\"").append(JsonUtils.escape(inv.getBookingChannel())).append("\"")
                        .append(",\"paymentMethod\":\"").append(JsonUtils.escape(inv.getPaymentMethod())).append("\"")
                        .append(",\"seatTotal\":").append(inv.getSeatTotal())
                        .append(",\"comboTotal\":").append(inv.getComboTotal())
                        .append(",\"totalAmount\":").append(inv.getTotalAmount())
                        .append(",\"comboSummary\":\"").append(JsonUtils.escape(inv.getComboSummary())).append("\"")
                        .append(",\"createdAt\":\"").append(JsonUtils.escape(inv.getCreatedAt())).append("\"")
                        .append("}");
            }
            invJson.append("]");

            return "{\"totalRevenue\":" + totalRevenue
                    + ",\"seatRevenue\":" + seatRevenue
                    + ",\"comboRevenue\":" + comboRevenue
                    + ",\"onlineRevenue\":" + onlineRevenue
                    + ",\"posRevenue\":" + posRevenue
                    + ",\"totalInvoices\":" + totalInvoices
                    + ",\"ticketsSold\":" + issuedTickets
                    + ",\"checkedInTickets\":" + checkedInTickets
                    + ",\"cancelledTickets\":" + cancelledTickets
                    + ",\"movieBreakdown\":" + movieRevJson
                    + ",\"invoices\":" + invJson
                    + "}";
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi tổng hợp báo cáo doanh thu: " + ex.getMessage(), ex);
        }
    }
}

