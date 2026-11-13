package com.cinehub;

import com.cinehub.models.SeatPricingPolicy;
import com.cinehub.models.ShowtimeSeat;
import com.cinehub.services.AuthService;
import com.cinehub.services.BookingService;
import com.cinehub.services.ShowtimeService;
import com.cinehub.utils.DatabaseConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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

        // 4. Hủy vé ISSUED (TKT-002-D04) rồi thử quét QR -> TICKET_CANCELLED
        BookingService.BookingResult rCancel = bookingService.cancelTicket("TKT-002-D04");
        BookingService.BookingResult rQr4 = bookingService.checkInTicket("TKT-002-D04", 2);
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

