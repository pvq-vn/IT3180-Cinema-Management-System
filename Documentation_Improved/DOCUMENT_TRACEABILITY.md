# DOCUMENT TRACEABILITY – Yêu cầu ↔ Thiết kế ↔ Kiểm thử ↔ Code

> **Quan trọng:** Không có `SRS_Document.docx` nên **không có Requirement ID gốc** để truy vết. Các mã `REQ-xx` dưới đây là **mã tạm, do người rà soát suy ra từ mã nguồn** (Javadoc, comment BR, tên hàm, API). Chúng **không phải** yêu cầu đã được nhóm hay giảng viên duyệt. Khi có SRS, hãy thay `REQ-xx` bằng ID thật và bổ sung các yêu cầu SRS có nhưng code chưa có (cột "Ghi chú" sẽ chuyển thành [CHƯA TRIỂN KHAI]).
>
> Code tham chiếu: bản v1.1.0 (cục bộ, có RBAC). Đường dẫn rút gọn: `svc/` = `src/main/java/com/cinehub/services/`, `ctl/` = `.../controllers/`. Mã TC theo `test/com/cinehub/CineHubTestRunner.java`.

## 1. Quy tắc nghiệp vụ và rủi ro

| Mã | Nội dung (theo comment trong code) | Cài đặt | Kiểm thử | Trạng thái |
|---|---|---|---|---|
| BR01 | Không chồng lấn suất chiếu trong cùng phòng; `end = start + thời lượng + 15 phút dọn phòng` | `svc/ShowtimeService.createShowtime` (Node 8–9), `CLEANING_BUFFER_MINUTES = 15` | TC_WB_SCHED_05, TC_WB_SCHED_06 | ✔ Đạt. Chỉ so với suất `SCHEDULED`; không chặn giờ bắt đầu trong quá khứ |
| BR02 | Tách ghế vật lý (`seats`) khỏi trạng thái ghế theo suất (`showtime_seats`) | `schema.sql:45–82`, `UNIQUE(showtime_id, seat_id)` | Gián tiếp: TC_WB_HOLD_03…05 | ✔ |
| BR03 | Giữ ghế tạm 5 phút, tối đa 8 ghế/lần, tự giải phóng khi hết hạn | `svc/BookingService.holdSeats`, `releaseExpiredHolds` | TC_WB_HOLD_01…05 | ⚠ Bước thanh toán không bắt buộc phải giữ ghế trước (AUDIT F-03) |
| BR04 | Phát hành vé QR; soát vé; hủy vé trước giờ chiếu | `svc/BookingService.confirmCheckout` (bước 6), `checkInTicket`, `cancelTicket` | TC_BB_CHK_01, TC_BB_QR_01…04 | ⚠ Không kiểm tra giờ chiếu khi hủy/soát (F-05, F-06); QR chỉ là chuỗi (F-24) |
| BR05 | Giá ghế: Thường = gốc, VIP = gốc + 20.000, Đôi = gốc × 2,2 (làm tròn nghìn); cộng combo | `models/SeatPricingPolicy` + `NullComboOrderItem` | TC_BB_PRICE_01, TC_BB_CHK_01 (tổng 248.000) | ✔ |
| R01 | Hai người không thể giữ/mua cùng một ghế | `SEAT_LOCK_MONITOR` + JDBC transaction | TC_CONCURRENCY_R01 | ✔ trong 1 JVM (F-10) |

## 2. Yêu cầu chức năng suy ra từ code

| REQ (tạm) | Phân hệ | Chức năng | Actor (sau RBAC) | API | Service / Code | Test | Ghi chú |
|---|---|---|---|---|---|---|---|
| REQ-01 | Chung | Đăng nhập, khóa tài khoản LOCKED, cấp mã phiên | Mọi người | `POST /api/auth/login` | `svc/AuthService.authenticate` | TC_WB_AUTH_01…05 | Mật khẩu SHA-256 không salt (F-11) |
| REQ-02 | Chung | Đăng xuất, hủy mã phiên | Đã đăng nhập | `POST /api/auth/logout` | `SessionManager.invalidateSession` | TC_RBAC_09 | |
| REQ-03 | Chung | Phân quyền RBAC phía server | – | mọi API ghi dữ liệu | `ctl/BaseController.requireLogin/requireRole` | TC_RBAC_01…09 | Chỉ có từ v1.1.0 |
| REQ-04 | CSD01 | Tra cứu phim theo từ khóa và trạng thái | Khách vãng lai | `GET /api/movies` | `svc/MovieService.getAllMovies` | – | **Chưa có test** |
| REQ-05 | CSD01 | Xem suất chiếu và số ghế trống | Khách vãng lai | `GET /api/showtimes` | `svc/ShowtimeService.getShowtimes` | – | **Chưa có test** |
| REQ-06 | CSD01 | Xem sơ đồ ghế theo thời gian thực | Khách vãng lai | `GET /api/booking/seats` | `svc/BookingService.getSeatMapForShowtime` | gián tiếp | |
| REQ-07 | CSD01 | Giữ ghế 5 phút | Đã đăng nhập | `POST /api/booking/hold` | `holdSeats` | TC_WB_HOLD_01…05, TC_RBAC_05 | |
| REQ-08 | CSD01 | Thanh toán online kèm combo, phát hành vé | Đã đăng nhập | `POST /api/booking/checkout` (ONLINE) | `confirmCheckout` | TC_BB_CHK_01 | Chưa có test cho COMBO_OUT_OF_STOCK, SEAT_CONFLICT, SHOWTIME_INVALID |
| REQ-09 | CSD02 | Bán vé tại quầy POS | STAFF, MANAGER | `POST /api/booking/checkout` (POS) | `confirmCheckout` (`staff_id`) | TC_RBAC_04 (chỉ test bị chặn) | **Chưa có test luồng POS thành công** |
| REQ-10 | CSD03 | Thêm/sửa phim | MANAGER | `POST /api/movies` | `svc/MovieService.saveMovie` | TC_RBAC_01 (chỉ test bị chặn) | **Chưa có test nghiệp vụ** |
| REQ-11 | CSD03 | Ngừng chiếu (ENDED) hoặc xóa phim chưa có suất | MANAGER | `POST /api/movies` (`action=delete`) | `discontinueOrDeleteMovie` | – | **Chưa có test**; giao diện v1.1.0 không có nút gọi chức năng này |
| REQ-12 | CSD03 | Xem danh sách phòng chiếu | Khách vãng lai | `GET /api/auditoriums` | `svc/AuditoriumService.getAllAuditoriums` | – | Không có chức năng thêm/sửa phòng chiếu; `getPhysicalSeatsByAuditorium` không có API gọi |
| REQ-13 | CSD03 | Lập lịch chiếu có kiểm tra BR01 | MANAGER | `POST /api/showtimes` | `createShowtime` | TC_WB_SCHED_01…06, TC_RBAC_02 | |
| REQ-14 | CSD04 | Xem danh mục combo | Khách vãng lai | `GET /api/combos` | `svc/ComboService.getAllCombos` | – | |
| REQ-15 | CSD04 | Thêm/sửa combo | STAFF, MANAGER | `POST /api/combos` | `saveCombo` | – | **Chưa có test nghiệp vụ** |
| REQ-16 | CSD04 | Ngừng/mở bán combo | STAFF, MANAGER | `POST /api/combos` (`action=toggle`) | `toggleComboStatus` | – | **Chưa có test**; có thể mở bán combo đã hết hàng (F-25) |
| REQ-17 | CSD05 | Soát vé QR (chặn vé giả, vé trùng, vé đã hủy) | STAFF, MANAGER | `POST /api/booking/checkin` | `checkInTicket` | TC_BB_QR_01…04, TC_RBAC_08 | F-06 |
| REQ-18 | CSD05 | Hủy vé, trả ghế về AVAILABLE | STAFF/MANAGER; CUSTOMER chỉ vé của mình | `POST /api/booking/cancel` | `cancelTicket` | TC_BB_QR_04, TC_RBAC_06 | Không hoàn tiền (F-02) |
| REQ-19 | CSD05 | Danh sách vé | STAFF/MANAGER: tất cả; CUSTOMER: vé của mình | `GET /api/booking/tickets` | `getAllTickets`, `getTicketsByUser` | TC_RBAC_07 | |
| REQ-20 | CSD05 | Báo cáo doanh thu (KPI, theo phim, danh sách hóa đơn) | MANAGER | `GET /api/reports/dashboard` | `svc/ReportService.buildDashboardReportJson` | TC_RBAC_03 (chỉ phân quyền) | **Chưa có test kiểm tra số liệu**; F-02, F-04, F-08 |

## 3. Danh mục 32 ca kiểm thử hiện có (v1.1.0)

Ghi chú: v1.2.0 thêm `TC_RBAC_10` cho `GET /api/auth/me` (tổng 33 ca; đã chạy lại ngày 14/11/2026: 33/33 PASS).

| Nhóm | Mã ca | Kỹ thuật (theo code) | Truy vết |
|---|---|---|---|
| Hộp trắng `authenticate` (V(G)=5) | TC_WB_AUTH_01…05 | Basis path, 5 đường | REQ-01 |
| Hộp trắng `createShowtime` (V(G)=6) | TC_WB_SCHED_01…06 | Basis path, 6 đường | REQ-13, BR01 |
| Giá ghế | TC_BB_PRICE_01 | Bảng quyết định (3 hạng) | BR05 |
| Giữ ghế | TC_WB_HOLD_01…05 | Biên 0/9 ghế; trạng thái BOOKED/HELD | REQ-07, BR03 |
| Thanh toán | TC_BB_CHK_01 | Hộp đen | REQ-08, BR04, BR05 |
| Soát/hủy vé | TC_BB_QR_01…04 | Bảng quyết định trạng thái vé | REQ-17, REQ-18, BR04 |
| Đồng thời | TC_CONCURRENCY_R01 | 2 luồng, `CountDownLatch` | R01 |
| RBAC qua HTTP | TC_RBAC_01…09 | Tích hợp (server HTTP thật) | REQ-02, REQ-03, REQ-07, REQ-09, REQ-10, REQ-13, REQ-17…REQ-20 |

Ghi chú khi viết TestPlan:

- Javadoc ghi "V(G) = 6" cho `holdSeats`, nhưng chỉ có 5 ca TC_WB_HOLD và chưa có ca cho nhánh `SEAT_NOT_FOUND`. Nếu giữ V(G)=6 thì cần thêm 1 ca hoặc sửa lại V(G).
- Javadoc của `createShowtime` liệt kê 11 node; cần vẽ CFG thật để xác nhận V(G)=6 trước khi đưa vào báo cáo.

## 4. Khoảng trống truy vết (cần bổ sung khi có SRS/TestPlan)

1. REQ-04, 05, 10, 11, 12, 14, 15, 16 không có ca kiểm thử nghiệp vụ nào.
2. REQ-20 (báo cáo) không có ca kiểm tra số liệu, trong khi đây là nơi có lỗi thật (F-02, F-04).
3. Luồng POS thành công (REQ-09) chưa được kiểm thử.
4. Chưa có ca cho các luồng ngoại lệ của checkout (combo hết hàng, suất không hợp lệ, xung đột ghế).
