package com.cinehub.controllers;

import com.cinehub.models.Invoice;
import com.cinehub.models.ShowtimeSeat;
import com.cinehub.models.Ticket;
import com.cinehub.models.User;
import com.cinehub.services.BookingService;
import com.cinehub.utils.JsonUtils;
import com.cinehub.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bộ điều khiển xử lý các yêu cầu Đặt vé trực tuyến (CSD01), Bán vé tại quầy POS (CSD02)
 * và Soát vé điện tử QR (CSD05) qua đường dẫn /api/booking/*.
 *
 * @author Hà Sỹ Bách (202416129)
 * @author Lê Anh Tuấn (202416639)
 * @version 1.0
 */
public class BookingController extends BaseController implements HttpHandler {

    private final BookingService bookingService = new BookingService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.endsWith("/seats") && "GET".equalsIgnoreCase(method)) {
                Map<String, String> params = parseQueryParams(exchange);
                int showtimeId = Integer.parseInt(params.getOrDefault("showtimeId", "1"));
                List<ShowtimeSeat> seats = bookingService.getSeatMapForShowtime(showtimeId);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < seats.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    ShowtimeSeat s = seats.get(i);
                    sb.append("{\"showtimeSeatId\":").append(s.getShowtimeSeatId())
                            .append(",\"showtimeId\":").append(s.getShowtimeId())
                            .append(",\"seatId\":").append(s.getSeatId())
                            .append(",\"seatCode\":\"").append(JsonUtils.escape(s.getSeatCode())).append("\"")
                            .append(",\"rowLabel\":\"").append(JsonUtils.escape(s.getRowLabel())).append("\"")
                            .append(",\"seatNumber\":").append(s.getSeatNumber())
                            .append(",\"seatType\":\"").append(JsonUtils.escape(s.getSeatType())).append("\"")
                            .append(",\"status\":\"").append(JsonUtils.escape(s.getStatus())).append("\"")
                            .append(",\"heldByUserId\":").append(s.getHeldByUserId() != null ? s.getHeldByUserId() : "null")
                            .append(",\"lockedUntil\":\"").append(JsonUtils.escape(s.getLockedUntil())).append("\"")
                            .append(",\"finalPrice\":").append(s.getFinalPrice()).append("}");
                }
                sb.append("]");
                sendJson(exchange, 200, sb.toString());
            } else if (path.endsWith("/tickets") && "GET".equalsIgnoreCase(method)) {
                // RBAC: Nhân viên/Quản lý xem toàn bộ vé; Khách hàng chỉ xem vé của chính mình
                User user = requireLogin(exchange);
                if (user == null) {
                    return;
                }
                List<Ticket> tickets = isStaffOrManager(user)
                        ? bookingService.getAllTickets()
                        : bookingService.getTicketsByUser(user.getUserId());
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < tickets.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(ticketToJson(tickets.get(i)));
                }
                sb.append("]");
                sendJson(exchange, 200, sb.toString());
            } else if (path.endsWith("/hold") && "POST".equalsIgnoreCase(method)) {
                User user = requireLogin(exchange);
                if (user == null) {
                    return;
                }
                Map<String, String> body = JsonUtils.parseFlatObject(readRequestBody(exchange));
                int showtimeId = Integer.parseInt(body.getOrDefault("showtimeId", "0"));
                // Danh tính lấy từ phiên đăng nhập, bỏ qua mọi userId gửi kèm trong thân yêu cầu
                int userId = user.getUserId();
                List<Integer> seatIds = JsonUtils.parseIntList(body.get("seatIds"));

                BookingService.BookingResult res = bookingService.holdSeats(showtimeId, seatIds, userId);
                String json = "{\"success\":" + res.isSuccess()
                        + ",\"code\":\"" + JsonUtils.escape(res.getCode()) + "\""
                        + ",\"message\":\"" + JsonUtils.escape(res.getMessage()) + "\""
                        + ",\"lockedUntil\":\"" + JsonUtils.escape(res.getLockedUntil()) + "\"}";
                sendJson(exchange, 200, json);
            } else if (path.endsWith("/checkout") && "POST".equalsIgnoreCase(method)) {
                User user = requireLogin(exchange);
                if (user == null) {
                    return;
                }
                Map<String, String> body = JsonUtils.parseFlatObject(readRequestBody(exchange));
                int showtimeId = Integer.parseInt(body.getOrDefault("showtimeId", "0"));
                int userId = user.getUserId();
                String customerName = body.get("customerName");
                String customerPhone = body.get("customerPhone");
                String bookingChannel = body.getOrDefault("bookingChannel", "ONLINE");
                // RBAC: chỉ Nhân viên/Quản lý được bán vé tại quầy POS
                if ("POS".equalsIgnoreCase(bookingChannel == null ? "" : bookingChannel.trim())
                        && !isStaffOrManager(user)) {
                    sendError(exchange, 403, "FORBIDDEN",
                            "Chỉ Nhân viên quầy hoặc Quản lý rạp được bán vé qua kênh POS.");
                    return;
                }
                String paymentMethod = body.getOrDefault("paymentMethod", "VIETQR");
                List<Integer> seatIds = JsonUtils.parseIntList(body.get("seatIds"));

                // Phân tích chuỗi comboItems dạng "1:2,3:1" (comboId:quantity)
                Map<Integer, Integer> comboMap = new HashMap<>();
                String comboSpec = body.get("comboSpec");
                if (comboSpec != null && !comboSpec.trim().isEmpty()) {
                    for (String pair : comboSpec.split(",")) {
                        String[] kv = pair.trim().split(":");
                        if (kv.length == 2) {
                            try {
                                int cId = Integer.parseInt(kv[0].trim());
                                int qty = Integer.parseInt(kv[1].trim());
                                if (qty > 0) {
                                    comboMap.put(cId, qty);
                                }
                            } catch (NumberFormatException ignored) {
                                // Bỏ qua cặp không hợp lệ
                            }
                        }
                    }
                }

                BookingService.BookingResult res = bookingService.confirmCheckout(
                        showtimeId, seatIds, comboMap, userId, customerName, customerPhone, bookingChannel, paymentMethod);
                if (!res.isSuccess()) {
                    sendJson(exchange, 200, JsonUtils.statusResponse(false, res.getCode(), res.getMessage()));
                    return;
                }
                Invoice inv = res.getInvoice();
                StringBuilder tktSb = new StringBuilder("[");
                List<Ticket> tktList = res.getTickets();
                for (int i = 0; i < tktList.size(); i++) {
                    if (i > 0) {
                        tktSb.append(",");
                    }
                    tktSb.append(ticketToJson(tktList.get(i)));
                }
                tktSb.append("]");

                String json = "{\"success\":true"
                        + ",\"code\":\"" + JsonUtils.escape(res.getCode()) + "\""
                        + ",\"message\":\"" + JsonUtils.escape(res.getMessage()) + "\""
                        + ",\"invoice\":{"
                        + "\"invoiceId\":" + inv.getInvoiceId()
                        + ",\"invoiceCode\":\"" + JsonUtils.escape(inv.getInvoiceCode()) + "\""
                        + ",\"customerName\":\"" + JsonUtils.escape(inv.getCustomerName()) + "\""
                        + ",\"customerPhone\":\"" + JsonUtils.escape(inv.getCustomerPhone()) + "\""
                        + ",\"movieTitle\":\"" + JsonUtils.escape(inv.getMovieTitle()) + "\""
                        + ",\"auditoriumName\":\"" + JsonUtils.escape(inv.getAuditoriumName()) + "\""
                        + ",\"startTime\":\"" + JsonUtils.escape(inv.getStartTime()) + "\""
                        + ",\"bookingChannel\":\"" + JsonUtils.escape(inv.getBookingChannel()) + "\""
                        + ",\"paymentMethod\":\"" + JsonUtils.escape(inv.getPaymentMethod()) + "\""
                        + ",\"seatTotal\":" + inv.getSeatTotal()
                        + ",\"comboTotal\":" + inv.getComboTotal()
                        + ",\"totalAmount\":" + inv.getTotalAmount()
                        + ",\"comboSummary\":\"" + JsonUtils.escape(inv.getComboSummary()) + "\""
                        + ",\"createdAt\":\"" + JsonUtils.escape(inv.getCreatedAt()) + "\""
                        + "},\"tickets\":" + tktSb + "}";
                sendJson(exchange, 200, json);
            } else if (path.endsWith("/checkin") && "POST".equalsIgnoreCase(method)) {
                // RBAC: chỉ Nhân viên/Quản lý được soát vé tại cửa phòng chiếu
                User staff = requireRole(exchange, ROLE_STAFF, ROLE_MANAGER);
                if (staff == null) {
                    return;
                }
                Map<String, String> body = JsonUtils.parseFlatObject(readRequestBody(exchange));
                String code = body.get("code");
                int staffId = staff.getUserId();

                BookingService.BookingResult res = bookingService.checkInTicket(code, staffId);
                String tktJson = (res.getTickets() != null && !res.getTickets().isEmpty())
                        ? ticketToJson(res.getTickets().get(0)) : "null";
                String json = "{\"success\":" + res.isSuccess()
                        + ",\"code\":\"" + JsonUtils.escape(res.getCode()) + "\""
                        + ",\"message\":\"" + JsonUtils.escape(res.getMessage()) + "\""
                        + ",\"ticket\":" + tktJson + "}";
                sendJson(exchange, 200, json);
            } else if (path.endsWith("/cancel") && "POST".equalsIgnoreCase(method)) {
                User user = requireLogin(exchange);
                if (user == null) {
                    return;
                }
                Map<String, String> body = JsonUtils.parseFlatObject(readRequestBody(exchange));
                String ticketCode = body.get("ticketCode");
                // RBAC: Khách hàng chỉ được hủy vé thuộc hóa đơn của chính mình
                if (!isStaffOrManager(user)) {
                    Integer ownerId = bookingService.getTicketOwnerUserId(ticketCode);
                    if (ownerId == null || ownerId != user.getUserId()) {
                        sendError(exchange, 403, "FORBIDDEN", "Bạn chỉ được hủy vé do chính tài khoản mình đặt.");
                        return;
                    }
                }
                BookingService.BookingResult res = bookingService.cancelTicket(ticketCode);
                sendJson(exchange, 200, JsonUtils.statusResponse(res.isSuccess(), res.getCode(), res.getMessage()));
            } else {
                sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ.");
            }
        } catch (Exception ex) {
            handleException(exchange, ex);
        }
    }

    private boolean isStaffOrManager(User user) {
        return SessionManager.hasRole(user, ROLE_STAFF, ROLE_MANAGER);
    }

    private String ticketToJson(Ticket t) {
        return "{\"ticketId\":" + t.getTicketId()
                + ",\"ticketCode\":\"" + JsonUtils.escape(t.getTicketCode()) + "\""
                + ",\"qrPayload\":\"" + JsonUtils.escape(t.getQrPayload()) + "\""
                + ",\"invoiceId\":" + t.getInvoiceId()
                + ",\"invoiceCode\":\"" + JsonUtils.escape(t.getInvoiceCode()) + "\""
                + ",\"customerName\":\"" + JsonUtils.escape(t.getCustomerName()) + "\""
                + ",\"showtimeId\":" + t.getShowtimeId()
                + ",\"showtimeSeatId\":" + t.getShowtimeSeatId()
                + ",\"seatCode\":\"" + JsonUtils.escape(t.getSeatCode()) + "\""
                + ",\"seatType\":\"" + JsonUtils.escape(t.getSeatType()) + "\""
                + ",\"movieTitle\":\"" + JsonUtils.escape(t.getMovieTitle()) + "\""
                + ",\"auditoriumName\":\"" + JsonUtils.escape(t.getAuditoriumName()) + "\""
                + ",\"startTime\":\"" + JsonUtils.escape(t.getStartTime()) + "\""
                + ",\"ticketPrice\":" + t.getTicketPrice()
                + ",\"ticketStatus\":\"" + JsonUtils.escape(t.getTicketStatus()) + "\""
                + ",\"checkedInAt\":\"" + JsonUtils.escape(t.getCheckedInAt()) + "\"}";
    }
}

