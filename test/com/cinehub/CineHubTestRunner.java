package com.cinehub;

import com.cinehub.models.Combo;
import com.cinehub.models.SeatPricingPolicy;
import com.cinehub.models.ShowtimeSeat;
import com.cinehub.services.AuthService;
import com.cinehub.services.BookingService;
import com.cinehub.services.ComboService;
import com.cinehub.services.ReportService;
import com.cinehub.services.ShowtimeService;
import com.cinehub.utils.DatabaseConnection;
import com.cinehub.utils.PasswordUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bộ thực thi Kiểm thử tự động (Automated Unit &amp; Integration Test Suite) cho hệ thống CineHub CMS.
 * Bao phủ đầy đủ:
 * 1. Kiểm thử Hộp trắng (White-box CFG Basis Path) cho AuthService, ShowtimeService (BR01), BookingService (BR03).
 * 2. Kiểm thử Hộp đen (Black-box Decision Table &amp; Phân tích giá trị biên) cho Thanh toán (BR04, BR05) &amp; Soát vé QR (CSD05).
 * 3. Kiểm thử Đồng thời Đa luồng (Concurrency Test) xác minh triệt tiêu rủi ro R01 (2 luồng giữ cùng 1 ghế).
 *
 * @author Nguyễn Xuân Hồng Quân (202416321)
 * @author Hà Sỹ Bách (202416129)
 * @version 1.0
 */
public class CineHubTestRunner {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    /** Cổng riêng cho kiểm thử RBAC qua HTTP (tránh đụng cổng 8080 của máy chủ demo). */
    private static final int RBAC_TEST_PORT = 18089;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /**
     * Điểm chạy bộ kiểm thử tự động và xuất bảng kết quả kiểm thử chi tiết ra màn hình Console.
     *
     * @param args Tham số dòng lệnh
     * @throws Exception Nếu khởi tạo CSDL gặp lỗi
     */
    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("  BÁO CÁO THỰC THI KIỂM THỬ TỰ ĐỘNG - DỰ ÁN CINEHUB CMS (IT3180 - NHÓM 3)");
        System.out.println("  Người phụ trách QA/QC: Nguyễn Xuân Hồng Quân (202416321)");
        System.out.println("================================================================================");

        DatabaseConnection.resetDatabase();
        AuthService authService = new AuthService();
        ShowtimeService showtimeService = new ShowtimeService();
        BookingService bookingService = new BookingService();

        // -------------------------------------------------------------------------
        // PHẦN 1: KIỂM THỬ HỘP TRẮNG (WHITE-BOX CFG) - AUTHSERVICE.AUTHENTICATE
        // -------------------------------------------------------------------------
        System.out.println("\n[PHẦN 1] KIỂM THỬ HỘP TRẮNG (CFG V(G)=5) - AuthService.authenticate():");

        // Đường cơ sở 1: 1 -> 2 -> 11 (Rỗng đầu vào)
        AuthService.AuthResult rAuth1 = authService.authenticate("", "admin123");
        assertCase("TC_WB_AUTH_01", "Đường 1->2->11: Tên đăng nhập rỗng",
                !rAuth1.isSuccess() && "INVALID_INPUT".equals(rAuth1.getStatusCode()), rAuth1.getStatusCode());

        // Đường cơ sở 2: 1 -> 3 -> 4 -> 5 -> 11 (Tài khoản không tồn tại)
        AuthService.AuthResult rAuth2 = authService.authenticate("ghost_user", "123456");
        assertCase("TC_WB_AUTH_02", "Đường 1->3->4->5->11: Tài khoản không tồn tại",
                !rAuth2.isSuccess() && "USER_NOT_FOUND".equals(rAuth2.getStatusCode()), rAuth2.getStatusCode());

        // Đường cơ sở 3: 1 -> 3 -> 4 -> 6 -> 7 -> 11 (Tài khoản bị khóa LOCKED)
        AuthService.AuthResult rAuth3 = authService.authenticate("locked_user", "khach123");
        assertCase("TC_WB_AUTH_03", "Đường 1->3->4->6->7->11: Tài khoản bị khóa (LOCKED)",
                !rAuth3.isSuccess() && "ACCOUNT_LOCKED".equals(rAuth3.getStatusCode()), rAuth3.getStatusCode());

        // Đường cơ sở 4: 1 -> 3 -> 4 -> 6 -> 8 -> 9 -> 11 (Sai mật khẩu)
        AuthService.AuthResult rAuth4 = authService.authenticate("admin", "wrong_pass");
        assertCase("TC_WB_AUTH_04", "Đường 1->3->4->6->8->9->11: Nhập sai mật khẩu",
                !rAuth4.isSuccess() && "WRONG_PASSWORD".equals(rAuth4.getStatusCode()), rAuth4.getStatusCode());

        // Đường cơ sở 5: 1 -> 3 -> 4 -> 6 -> 8 -> 10 -> 11 (Đăng nhập hợp lệ)
        AuthService.AuthResult rAuth5 = authService.authenticate("admin", "admin123");
        assertCase("TC_WB_AUTH_05", "Đường 1->3->4->6->8->10->11: Đăng nhập Quản lý thành công",
                rAuth5.isSuccess() && "SUCCESS".equals(rAuth5.getStatusCode()) && rAuth5.getSessionToken() != null,
                rAuth5.getStatusCode());

        // -------------------------------------------------------------------------
        // PHẦN 2: KIỂM THỬ HỘP TRẮNG (WHITE-BOX CFG) - SHOWTIMESERVICE.CREATESHOWTIME (BR01)
        // -------------------------------------------------------------------------
        System.out.println("\n[PHẦN 2] KIỂM THỬ HỘP TRẮNG (CFG V(G)=6) - ShowtimeService.createShowtime() [BR01]:");

        // Đường 1: Giá cơ sở <= 0
        ShowtimeService.ScheduleResult rSch1 = showtimeService.createShowtime(1, 1, "2026-06-10 09:00:00", -50000);
        assertCase("TC_WB_SCHED_01", "Đường 1->2: Giá vé cơ sở âm (-50.000đ)",
                !rSch1.isSuccess() && "INVALID_INPUT".equals(rSch1.getCode()), rSch1.getCode());

        // Đường 2: Định dạng ngày giờ sai
        ShowtimeService.ScheduleResult rSch2 = showtimeService.createShowtime(1, 1, "INVALID-DATE", 90000);
        assertCase("TC_WB_SCHED_02", "Đường 1->3(Ex): Sai định dạng ngày giờ",
                !rSch2.isSuccess() && "INVALID_DATETIME".equals(rSch2.getCode()), rSch2.getCode());

        // Đường 3: Phim không tồn tại
        ShowtimeService.ScheduleResult rSch3 = showtimeService.createShowtime(999, 1, "2026-06-10 09:00:00", 90000);
        assertCase("TC_WB_SCHED_03", "Đường 1->3->4->5: Mã phim không tồn tại (#999)",
                !rSch3.isSuccess() && "MOVIE_UNAVAILABLE".equals(rSch3.getCode()), rSch3.getCode());

        // Đường 4: Phòng chiếu không tồn tại
        ShowtimeService.ScheduleResult rSch4 = showtimeService.createShowtime(1, 999, "2026-06-10 09:00:00", 90000);
        assertCase("TC_WB_SCHED_04", "Đường 1->3->4->6->7: Mã phòng chiếu không tồn tại (#999)",
                !rSch4.isSuccess() && "AUDITORIUM_UNAVAILABLE".equals(rSch4.getCode()), rSch4.getCode());

        // Đường 5: Tạo suất chiếu hợp lệ (Phim #1 thời lượng 128p + 15p VS = 143p -> 09:00 đến 11:23)
        ShowtimeService.ScheduleResult rSch5 = showtimeService.createShowtime(1, 1, "2026-06-15 09:00:00", 95000);
        assertCase("TC_WB_SCHED_05", "Đường 1->3->4->6->8->10->11: Tạo lịch chiếu hợp lệ (09:00 -> 11:23)",
                rSch5.isSuccess() && "CREATED".equals(rSch5.getCode()), rSch5.getCode());

        // Đường 6: Vi phạm BR01 - Tạo suất chiếu lúc 11:15 cùng ngày cùng Phòng 1 (chồng lấn vào 15p dọn vệ sinh 11:08-11:23)
        ShowtimeService.ScheduleResult rSch6 = showtimeService.createShowtime(2, 1, "2026-06-15 11:15:00", 95000);
        assertCase("TC_WB_SCHED_06", "Đường 1->3->4->6->8->9: Chặn trùng lịch trong 15p vệ sinh phòng (BR01)",
                !rSch6.isSuccess() && "SCHEDULE_CONFLICT".equals(rSch6.getCode()), rSch6.getCode());

        // -------------------------------------------------------------------------
        // PHẦN 3: KIỂM THỬ HỘP TRẮNG & HỘP ĐEN - BOOKINGSERVICE & ĐA HÌNH GIÁ (BR02-BR05)
        // -------------------------------------------------------------------------
        System.out.println("\n[PHẦN 3] KIỂM THỬ HỘP TRẮNG & BẢNG QUYẾT ĐỊNH - Giữ ghế, Đa hình giá & Thanh toán:");

        // Kiểm tra Đa hình tính giá ghế (Refactoring Chương 8 - BR05)
        double stdPrice = SeatPricingPolicy.forSeatType("STANDARD").calculateSeatPrice(100000);
        double vipPrice = SeatPricingPolicy.forSeatType("VIP").calculateSeatPrice(100000);
        double couplePrice = SeatPricingPolicy.forSeatType("COUPLE").calculateSeatPrice(100000);
        assertCase("TC_BB_PRICE_01", "Đa hình giá ghế BR05: Standard=100k, VIP=120k (+20k), Couple=220k (2.2x)",
                stdPrice == 100000 && vipPrice == 120000 && couplePrice == 220000,
                "STD=" + stdPrice + ", VIP=" + vipPrice + ", COUPLE=" + couplePrice);

        // Kiểm tra giữ ghế rỗng (Node 1 -> Node 2)
        BookingService.BookingResult rHold1 = bookingService.holdSeats(1, Collections.emptyList(), 4);
        assertCase("TC_WB_HOLD_01", "Giữ ghế với danh sách rỗng -> INVALID_SELECTION",
                !rHold1.isSuccess() && "INVALID_SELECTION".equals(rHold1.getCode()), rHold1.getCode());

        // Kiểm tra giữ quá 8 ghế (Biên trên > 8)
        BookingService.BookingResult rHold2 = bookingService.holdSeats(
                1, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), 4);
        assertCase("TC_WB_HOLD_02", "Giữ vượt quá biên 8 ghế (9 ghế) -> INVALID_SELECTION",
                !rHold2.isSuccess() && "INVALID_SELECTION".equals(rHold2.getCode()), rHold2.getCode());

        // Lấy sơ đồ ghế suất chiếu #1 để lấy mã ghế trống
        List<ShowtimeSeat> map1 = bookingService.getSeatMapForShowtime(1);
        int availSeat1 = map1.stream().filter(s -> "AVAILABLE".equals(s.getStatus())).findFirst().get().getShowtimeSeatId();
        int bookedSeat = map1.stream().filter(s -> "BOOKED".equals(s.getStatus())).findFirst().get().getShowtimeSeatId();

        // Giữ ghế đã BOOKED -> SEAT_ALREADY_TAKEN
        BookingService.BookingResult rHold3 = bookingService.holdSeats(1, Collections.singletonList(bookedSeat), 4);
        assertCase("TC_WB_HOLD_03", "Giữ ghế đã bán (BOOKED) -> SEAT_ALREADY_TAKEN",
                !rHold3.isSuccess() && "SEAT_ALREADY_TAKEN".equals(rHold3.getCode()), rHold3.getCode());

        // Giữ ghế hợp lệ cho User #4
        BookingService.BookingResult rHold4 = bookingService.holdSeats(1, Collections.singletonList(availSeat1), 4);
        assertCase("TC_WB_HOLD_04", "Giữ ghế trống hợp lệ trong 5 phút (BR03) -> HELD_SUCCESS",
                rHold4.isSuccess() && "HELD_SUCCESS".equals(rHold4.getCode()), rHold4.getCode());

        // Người dùng khác (User #5) cố tình giữ ghế đang được User #4 giữ -> SEAT_ALREADY_TAKEN
        BookingService.BookingResult rHold5 = bookingService.holdSeats(1, Collections.singletonList(availSeat1), 5);
        assertCase("TC_WB_HOLD_05", "Khách #5 giữ ghế đang bị Khách #4 khóa tạm -> SEAT_ALREADY_TAKEN",
                !rHold5.isSuccess() && "SEAT_ALREADY_TAKEN".equals(rHold5.getCode()), rHold5.getCode());

        // Thanh toán thành công ghế đã giữ của User #4 kèm 2 phần Combo #1 (BR04, BR05)
        Map<Integer, Integer> comboOrder = new HashMap<>();
        comboOrder.put(1, 2);
        BookingService.BookingResult rChk1 = bookingService.confirmCheckout(
                1, Collections.singletonList(availSeat1), comboOrder,
                4, "Vũ Việt Tiến", "0987654321", "ONLINE", "VIETQR");
        assertCase("TC_BB_CHK_01", "Thanh toán ghế đang giữ + 2x Combo CB01 -> Phát hành vé QR",
                rChk1.isSuccess() && "CHECKOUT_SUCCESS".equals(rChk1.getCode())
                        && rChk1.getTickets() != null && rChk1.getTickets().size() == 1,
                rChk1.getCode() + " | Total=" + (rChk1.getInvoice() != null ? rChk1.getInvoice().getTotalAmount() : 0));

        // -------------------------------------------------------------------------
        // PHẦN 4: KIỂM THỬ HỘP ĐEN (BẢNG QUYẾT ĐỊNH) - SOÁT VÉ QR & HỦY VÉ (CSD05, BR04)
        // -------------------------------------------------------------------------
        System.out.println("\n[PHẦN 4] KIỂM THỬ BẢNG QUYẾT ĐỊNH - Soát vé QR Check-in & Hủy vé (CSD05):");

        String newTicketCode = rChk1.getTickets().get(0).getTicketCode();

        // 1. Soát vé hợp lệ lần đầu (ISSUED -> CHECKED_IN)
        BookingService.BookingResult rQr1 = bookingService.checkInTicket(newTicketCode, 2);
        assertCase("TC_BB_QR_01", "Soát vé QR hợp lệ ở trạng thái ISSUED -> CHECKIN_SUCCESS",
                rQr1.isSuccess() && "CHECKIN_SUCCESS".equals(rQr1.getCode()), rQr1.getCode());

        // 2. Quét lại cùng mã vé QR lần thứ 2 -> Cảnh báo trùng vé ALREADY_CHECKED_IN
        BookingService.BookingResult rQr2 = bookingService.checkInTicket(newTicketCode, 2);
        assertCase("TC_BB_QR_02", "Chặn quét trùng vé QR lần 2 -> ALREADY_CHECKED_IN",
                !rQr2.isSuccess() && "ALREADY_CHECKED_IN".equals(rQr2.getCode()), rQr2.getCode());

        // 3. Quét mã vé giả / không tồn tại -> TICKET_NOT_FOUND
        BookingService.BookingResult rQr3 = bookingService.checkInTicket("TKT-FAKE-999", 2);
        assertCase("TC_BB_QR_03", "Chặn mã vé QR giả mạo -> TICKET_NOT_FOUND",
                !rQr3.isSuccess() && "TICKET_NOT_FOUND".equals(rQr3.getCode()), rQr3.getCode());

        // 4. Hủy vé ISSUED của suất NGÀY MAI (suất #8, chưa chiếu) rồi thử quét QR -> TICKET_CANCELLED
        String futureTicket = bookOnline(bookingService, 8, 4, 1, null).getTickets().get(0).getTicketCode();
        BookingService.BookingResult rCancel = bookingService.cancelTicket(futureTicket);
        BookingService.BookingResult rQr4 = bookingService.checkInTicket(futureTicket, 2);
        assertCase("TC_BB_QR_04", "Hủy vé trước giờ chiếu (giải phóng ghế) & Chặn soát vé đã hủy -> TICKET_CANCELLED",
                rCancel.isSuccess() && !rQr4.isSuccess() && "TICKET_CANCELLED".equals(rQr4.getCode()),
                rCancel.getCode() + " -> " + rQr4.getCode());

        // -------------------------------------------------------------------------
        // PHẦN 5: KIỂM THỬ ĐỒNG THỜI ĐA LUỒNG (CONCURRENCY TEST - RỦI RO R01)
        // -------------------------------------------------------------------------
        System.out.println("\n[PHẦN 5] KIỂM THỬ ĐỒNG THỜI ĐA LUỒNG (R01: 2 luồng đặt trùng 1 ghế cùng thời điểm):");

        List<ShowtimeSeat> mapAfter = bookingService.getSeatMapForShowtime(1);
        int targetRaceSeatId = mapAfter.stream()
                .filter(s -> "AVAILABLE".equals(s.getStatus()))
                .findFirst().get().getShowtimeSeatId();

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Runnable raceWorkerA = () -> {
            readyLatch.countDown();
            try {
                startGun.await();
                BookingService.BookingResult res = bookingService.holdSeats(
                        1, Collections.singletonList(targetRaceSeatId), 4);
                if (res.isSuccess()) {
                    successCount.incrementAndGet();
                } else if ("SEAT_ALREADY_TAKEN".equals(res.getCode())) {
                    conflictCount.incrementAndGet();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable raceWorkerB = () -> {
            readyLatch.countDown();
            try {
                startGun.await();
                BookingService.BookingResult res = bookingService.holdSeats(
                        1, Collections.singletonList(targetRaceSeatId), 5);
                if (res.isSuccess()) {
                    successCount.incrementAndGet();
                } else if ("SEAT_ALREADY_TAKEN".equals(res.getCode())) {
                    conflictCount.incrementAndGet();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        new Thread(raceWorkerA, "Thread-Customer-A").start();
        new Thread(raceWorkerB, "Thread-Customer-B").start();
        readyLatch.await();
        startGun.countDown();
        doneLatch.await();

        assertCase("TC_CONCURRENCY_R01",
                "2 luồng đồng thời giữ cùng ghế #" + targetRaceSeatId + ": Đúng 1 luồng thành công, 1 luồng bị từ chối",
                successCount.get() == 1 && conflictCount.get() == 1,
                "Success=" + successCount.get() + ", Conflict=" + conflictCount.get());

        // -------------------------------------------------------------------------
        // PHẦN 6: KIỂM THỬ PHÂN QUYỀN RBAC QUA HTTP (tầng Controller + SessionManager)
        // -------------------------------------------------------------------------
        System.out.println("\n[PHẦN 6] KIỂM THỬ PHÂN QUYỀN RBAC PHÍA MÁY CHỦ (gọi API thật qua HTTP):");
        runRbacTests(bookingService);

        // -------------------------------------------------------------------------
        // PHẦN 7: KIỂM THỬ HỒI QUY CÁC LỖI ĐÃ SỬA (AUDIT F-02 ... F-25)
        // -------------------------------------------------------------------------
        System.out.println("\n[PHẦN 7] KIỂM THỬ HỒI QUY CÁC LỖI NGHIỆP VỤ ĐÃ SỬA (BR03, BR04, CSD05, bảo mật):");
        runRegressionTests(new BookingService(), new ShowtimeService());

        // Khôi phục lại dữ liệu mẫu sạch sẽ sau khi chạy kiểm thử
        DatabaseConnection.resetDatabase();

        System.out.println("\n================================================================================");
        System.out.printf("  TỔNG KẾT KIỂM THỬ: Tổng số = %d | ĐẠT (PASSED) = %d | LỖI (FAILED) = %d (Tỷ lệ đạt: %.1f%%)%n",
                totalTests, passedTests, failedTests, (passedTests * 100.0 / totalTests));
        System.out.println("================================================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    /** Đặt vé online đúng quy trình (giữ ghế rồi thanh toán) cho n ghế trống đầu tiên của suất chiếu. */
    private static BookingService.BookingResult bookOnline(BookingService bs, int showtimeId, int userId,
                                                           int seatCount, Map<Integer, Integer> combos) {
        List<Integer> seats = freeSeats(bs, showtimeId, seatCount);
        BookingService.BookingResult hold = bs.holdSeats(showtimeId, seats, userId);
        if (!hold.isSuccess()) {
            throw new IllegalStateException("Không giữ được ghế: " + hold.getMessage());
        }
        BookingService.BookingResult res = bs.confirmCheckout(showtimeId, seats,
                combos == null ? new HashMap<>() : combos, userId, "Khách kiểm thử", "0900000000", "ONLINE", "VIETQR");
        if (!res.isSuccess()) {
            throw new IllegalStateException("Không thanh toán được: " + res.getMessage());
        }
        return res;
    }

    private static List<Integer> freeSeats(BookingService bs, int showtimeId, int count) {
        List<Integer> ids = new ArrayList<>();
        for (ShowtimeSeat s : bs.getSeatMapForShowtime(showtimeId)) {
            if ("AVAILABLE".equals(s.getStatus()) && ids.size() < count) {
                ids.add(s.getShowtimeSeatId());
            }
        }
        return ids;
    }

    private static double num(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\":(-?[0-9.E]+)").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : Double.NaN;
    }

    private static double sumMovieRevenue(String json) {
        String part = json.substring(json.indexOf("\"movieBreakdown\""), json.indexOf("\"invoices\""));
        Matcher m = Pattern.compile("\"revenue\":(-?[0-9.E]+)").matcher(part);
        double sum = 0;
        while (m.find()) {
            sum += Double.parseDouble(m.group(1));
        }
        return sum;
    }

    private static String comboStatus(int comboId) {
        for (Combo c : new ComboService().getAllCombos(false)) {
            if (c.getComboId() == comboId) {
                return c.getStatus() + "/" + c.getStockQuantity();
            }
        }
        return "?";
    }

    private static void runRegressionTests(BookingService bs, ShowtimeService ss) throws Exception {
        DatabaseConnection.resetDatabase();
        ReportService report = new ReportService();

        // F-03: Online phải giữ ghế trước; POS được bán thẳng ghế trống
        List<Integer> s8 = freeSeats(bs, 8, 1);
        BookingService.BookingResult noHold = bs.confirmCheckout(8, s8, new HashMap<>(), 4,
                "A", "0900000000", "ONLINE", "VIETQR");
        BookingService.BookingResult pos = bs.confirmCheckout(8, s8, new HashMap<>(), 2,
                "B", "0900000000", "POS", "CASH");
        assertCase("TC_REG_BR03_01", "Online chưa giữ ghế -> SEAT_NOT_HELD; POS bán thẳng ghế trống -> CHECKOUT_SUCCESS",
                "SEAT_NOT_HELD".equals(noHold.getCode()) && pos.isSuccess(),
                noHold.getCode() + " / " + pos.getCode());

        // F-03: giới hạn 8 ghế áp dụng cả khi thanh toán
        BookingService.BookingResult nine = bs.confirmCheckout(8, freeSeats(bs, 8, 9), new HashMap<>(), 2,
                "C", "0900000000", "POS", "CASH");
        assertCase("TC_REG_BR03_02", "Thanh toán 9 ghế một lần -> INVALID_SELECTION",
                "INVALID_SELECTION".equals(nine.getCode()), nine.getCode());

        // F-04: kênh/phương thức không hợp lệ bị từ chối
        BookingService.BookingResult kiosk = bs.confirmCheckout(8, freeSeats(bs, 8, 1), new HashMap<>(), 2,
                "D", "0900000000", "KIOSK", "CASH");
        BookingService.BookingResult cashOnline = bs.confirmCheckout(8, freeSeats(bs, 8, 1), new HashMap<>(), 2,
                "E", "0900000000", "ONLINE", "CASH");
        assertCase("TC_REG_CSD05_01", "Kênh KIOSK -> INVALID_CHANNEL; ONLINE trả CASH -> INVALID_PAYMENT_METHOD",
                "INVALID_CHANNEL".equals(kiosk.getCode()) && "INVALID_PAYMENT_METHOD".equals(cashOnline.getCode()),
                kiosk.getCode() + " / " + cashOnline.getCode());

        // F-06: soát vé của suất ngày mai bị từ chối
        Map<Integer, Integer> combo = new HashMap<>();
        combo.put(1, 1);
        BookingService.BookingResult order = bookOnline(bs, 8, 4, 2, combo);
        String t1 = order.getTickets().get(0).getTicketCode();
        String t2 = order.getTickets().get(1).getTicketCode();
        BookingService.BookingResult wrongDay = bs.checkInTicket(t1, 2);
        assertCase("TC_REG_BR04_01", "Soát vé của suất ngày mai -> WRONG_DAY",
                "WRONG_DAY".equals(wrongDay.getCode()), wrongDay.getCode());

        // F-07: mã hóa đơn theo mẫu mới, không trùng
        String code1 = order.getInvoice().getInvoiceCode();
        String code2 = pos.getInvoice().getInvoiceCode();
        assertCase("TC_REG_INV_01", "Mã hóa đơn dạng INV-yyMMddHHmmss-XXXX và không trùng nhau",
                code1.matches("INV-\\d{12}-[0-9A-F]{4}") && !code1.equals(code2), code1 + " / " + code2);

        // F-02: hủy vé trừ doanh thu; hủy hết vé -> hóa đơn REFUNDED, hoàn combo và tồn kho
        double price1 = order.getTickets().get(0).getTicketPrice();
        double comboPrice = order.getInvoice().getComboTotal();
        String before = report.buildDashboardReportJson();
        int stockBefore = Integer.parseInt(comboStatus(1).split("/")[1]);
        bs.cancelTicket(t1);
        String mid = report.buildDashboardReportJson();
        bs.cancelTicket(t2);
        String after = report.buildDashboardReportJson();
        double price2 = order.getTickets().get(1).getTicketPrice();
        boolean partialOk = Math.abs(num(before, "totalRevenue") - num(mid, "totalRevenue") - price1) < 0.5;
        boolean fullOk = Math.abs(num(mid, "totalRevenue") - num(after, "totalRevenue") - price2 - comboPrice) < 0.5;
        assertCase("TC_REG_BR04_02", "Hủy 1 vé trừ đúng giá vé; hủy vé cuối -> hoàn cả combo, trả lại tồn kho combo",
                partialOk && fullOk && Integer.parseInt(comboStatus(1).split("/")[1]) == stockBefore + 1,
                "doanh thu " + (long) num(before, "totalRevenue") + " -> " + (long) num(mid, "totalRevenue")
                + " -> " + (long) num(after, "totalRevenue") + "; tồn CB01 " + comboStatus(1));

        // F-05: không hủy được vé khi suất chiếu đã bắt đầu (suất hôm qua)
        String yesterday = LocalDate.now().minusDays(1) + " 10:00:00";
        ShowtimeService.ScheduleResult past = ss.createShowtime(4, 3, yesterday, 80000);
        int pastId = past.getShowtime().getShowtimeId();
        BookingService.BookingResult pastOrder = bs.confirmCheckout(pastId, freeSeats(bs, pastId, 1),
                new HashMap<>(), 2, "F", "0900000000", "POS", "CASH");
        BookingService.BookingResult late = bs.cancelTicket(pastOrder.getTickets().get(0).getTicketCode());
        assertCase("TC_REG_BR04_03", "Hủy vé khi suất chiếu đã bắt đầu -> TOO_LATE_TO_CANCEL",
                "TOO_LATE_TO_CANCEL".equals(late.getCode()), late.getCode());

        // F-04 + F-08: các con số báo cáo khớp nhau
        String rep = report.buildDashboardReportJson();
        boolean channelsOk = Math.abs(num(rep, "onlineRevenue") + num(rep, "posRevenue") - num(rep, "totalRevenue")) < 0.5;
        boolean partsOk = Math.abs(num(rep, "seatRevenue") + num(rep, "comboRevenue") - num(rep, "totalRevenue")) < 0.5;
        boolean movieOk = Math.abs(sumMovieRevenue(rep) - num(rep, "seatRevenue")) < 0.5;
        assertCase("TC_REG_CSD05_02", "Online + POS = Tổng; Vé + Combo = Tổng; Σ doanh thu theo phim = doanh thu vé",
                channelsOk && partsOk && movieOk,
                "tổng=" + (long) num(rep, "totalRevenue") + ", vé=" + (long) num(rep, "seatRevenue")
                + ", Σphim=" + (long) sumMovieRevenue(rep));

        // F-25: mở bán lại combo đã ngừng nhưng hết hàng (CB06, tồn 0) -> OUT_OF_STOCK, không thành AVAILABLE
        new ComboService().toggleComboStatus(6);
        assertCase("TC_REG_F25_01", "Mở lại combo đã ngừng KD có tồn kho 0 -> OUT_OF_STOCK",
                comboStatus(6).startsWith("OUT_OF_STOCK"), comboStatus(6));

        // F-11: mật khẩu mẫu SHA-256 được băm lại sang PBKDF2 có salt sau lần đăng nhập đầu tiên
        AuthService auth = new AuthService();
        boolean legacy = PasswordUtils.needsUpgrade(auth.findByUsername("staff").getPasswordHash());
        boolean firstLogin = auth.authenticate("staff", "staff123").isSuccess();
        String newHash = auth.findByUsername("staff").getPasswordHash();
        boolean secondLogin = auth.authenticate("staff", "staff123").isSuccess();
        boolean wrongRejected = !auth.authenticate("staff", "staff124").isSuccess();
        assertCase("TC_REG_SEC_01", "Băm SHA-256 cũ -> PBKDF2 có salt sau đăng nhập; vẫn đăng nhập được, sai mật khẩu bị chặn",
                legacy && firstLogin && newHash.startsWith("pbkdf2$") && secondLogin && wrongRejected,
                newHash.substring(0, Math.min(14, newHash.length())) + "...");
    }

    private static void runRbacTests(BookingService bookingService) throws Exception {
        DatabaseConnection.resetDatabase();
        CineHubApplication.startServer(RBAC_TEST_PORT);
        try {
            String customerToken = login("customer", "khach123");   // userId = 4
            String otherCustomer = login("bach_hs", "khach123");    // userId = 5
            String staffToken = login("staff", "staff123");          // STAFF
            String managerToken = login("admin", "admin123");        // MANAGER

            HttpResponse<String> r1 = call("POST", "/api/movies", null,
                    "{\"title\":\"Phim lậu\",\"durationMinutes\":\"100\"}");
            assertCase("TC_RBAC_01", "Thêm phim khi chưa đăng nhập -> 401 UNAUTHORIZED",
                    r1.statusCode() == 401, "HTTP " + r1.statusCode());

            HttpResponse<String> r2 = call("POST", "/api/showtimes", customerToken,
                    "{\"movieId\":\"1\",\"auditoriumId\":\"3\",\"startTime\":\"2026-07-01 08:00:00\",\"basePrice\":\"90000\"}");
            assertCase("TC_RBAC_02", "Khách hàng tự lập lịch chiếu -> 403 FORBIDDEN",
                    r2.statusCode() == 403, "HTTP " + r2.statusCode());

            HttpResponse<String> r3a = call("GET", "/api/reports/dashboard", staffToken, null);
            HttpResponse<String> r3b = call("GET", "/api/reports/dashboard", managerToken, null);
            assertCase("TC_RBAC_03", "Báo cáo doanh thu: Nhân viên -> 403, Quản lý -> 200",
                    r3a.statusCode() == 403 && r3b.statusCode() == 200,
                    "STAFF=" + r3a.statusCode() + ", MANAGER=" + r3b.statusCode());

            int freeSeat = bookingService.getSeatMapForShowtime(1).stream()
                    .filter(s -> "AVAILABLE".equals(s.getStatus())).findFirst().get().getShowtimeSeatId();
            HttpResponse<String> r4 = call("POST", "/api/booking/checkout", customerToken,
                    "{\"showtimeId\":\"1\",\"seatIds\":\"" + freeSeat + "\",\"bookingChannel\":\"POS\"}");
            assertCase("TC_RBAC_04", "Khách hàng bán vé qua kênh POS -> 403 FORBIDDEN",
                    r4.statusCode() == 403, "HTTP " + r4.statusCode());

            // Khách #4 gửi kèm userId=5 để mạo danh -> máy chủ phải ghi nhận người giữ ghế là #4
            HttpResponse<String> r5 = call("POST", "/api/booking/hold", customerToken,
                    "{\"showtimeId\":\"1\",\"userId\":\"5\",\"seatIds\":\"" + freeSeat + "\"}");
            Integer heldBy = bookingService.getSeatMapForShowtime(1).stream()
                    .filter(s -> s.getShowtimeSeatId() == freeSeat).findFirst().get().getHeldByUserId();
            assertCase("TC_RBAC_05", "Giữ ghế kèm userId giả mạo -> bỏ qua, dùng danh tính từ phiên (#4)",
                    r5.statusCode() == 200 && heldBy != null && heldBy == 4,
                    "HTTP " + r5.statusCode() + ", heldBy=" + heldBy);

            HttpResponse<String> r6 = call("POST", "/api/booking/cancel", otherCustomer,
                    "{\"ticketCode\":\"TKT-004-A04\"}");
            assertCase("TC_RBAC_06", "Khách #5 hủy vé của khách #4 -> 403 FORBIDDEN",
                    r6.statusCode() == 403, "HTTP " + r6.statusCode());

            HttpResponse<String> r7 = call("GET", "/api/booking/tickets", customerToken, null);
            boolean onlyOwn = r7.statusCode() == 200 && r7.body().contains("TKT-004-A04")
                    && !r7.body().contains("TKT-002-") && !r7.body().contains("TKT-003-");
            assertCase("TC_RBAC_07", "Khách hàng xem danh sách vé -> chỉ thấy vé của chính mình",
                    onlyOwn, "HTTP " + r7.statusCode());

            HttpResponse<String> r8a = call("POST", "/api/booking/checkin", customerToken,
                    "{\"code\":\"TKT-004-A05\"}");
            HttpResponse<String> r8b = call("POST", "/api/booking/checkin", staffToken,
                    "{\"code\":\"TKT-004-A05\"}");
            assertCase("TC_RBAC_08", "Soát vé QR: Khách hàng -> 403, Nhân viên -> CHECKIN_SUCCESS",
                    r8a.statusCode() == 403 && r8b.body().contains("CHECKIN_SUCCESS"),
                    "CUSTOMER=" + r8a.statusCode() + ", STAFF=" + r8b.statusCode());

            HttpResponse<String> r10a = call("GET", "/api/auth/me", customerToken, null);
            HttpResponse<String> r10b = call("GET", "/api/auth/me", null, null);
            assertCase("TC_RBAC_10", "Kiểm tra phiên /api/auth/me: có token -> 200 đúng người, không token -> 401",
                    r10a.statusCode() == 200 && r10a.body().contains("\"username\":\"customer\"")
                            && r10b.statusCode() == 401,
                    "WITH=" + r10a.statusCode() + ", WITHOUT=" + r10b.statusCode());

            call("POST", "/api/auth/logout", customerToken, "");
            HttpResponse<String> r9 = call("GET", "/api/booking/tickets", customerToken, null);
            assertCase("TC_RBAC_09", "Dùng lại mã phiên sau khi đăng xuất -> 401 UNAUTHORIZED",
                    r9.statusCode() == 401, "HTTP " + r9.statusCode());
        } finally {
            CineHubApplication.stopServer();
        }
    }

    private static String login(String username, String password) throws Exception {
        HttpResponse<String> resp = call("POST", "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(resp.body());
        if (!m.find()) {
            throw new IllegalStateException("Không đăng nhập được tài khoản " + username + ": " + resp.body());
        }
        return m.group(1);
    }

    private static HttpResponse<String> call(String method, String path, String token, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create("http://localhost:" + RBAC_TEST_PORT + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void assertCase(String testId, String description, boolean condition, String actualOutput) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.printf("  [PASS] %-18s | %-62s | Thực tế: %s%n", testId, description, actualOutput);
        } else {
            failedTests++;
            System.out.printf("  [FAIL] %-18s | %-62s | Thực tế: %s%n", testId, description, actualOutput);
        }
    }
}

