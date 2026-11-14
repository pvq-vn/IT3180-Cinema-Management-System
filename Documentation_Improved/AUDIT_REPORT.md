# AUDIT REPORT – IT3180 CineHub (Nhóm 3)

- Ngày rà soát: 14/11/2026
- Phạm vi: 6 tài liệu baseline (Scope, ProjectPlan, SRS, SDD, TestPlan, TestReport) và toàn bộ mã nguồn trong repository.
- Phiên bản mã được rà soát:
  - **v1.0.0**: bản trên GitHub `pvq-vn/IT3180-Cinema-Management-System`, nhánh `main`, commit ngày 13/11/2026.
  - **v1.1.0 (bản cục bộ)**: v1.0.0 cộng thêm phân quyền RBAC phía máy chủ (xem `CHANGELOG.md`).
- Lưu ý: sau khi báo cáo này được viết, giao diện đã được thiết kế lại thành **v1.2.0** (xem `CHANGELOG.md`). Các nhận xét về frontend áp dụng cho v1.1.0; số dòng trong các file frontend đã thay đổi. F-13 và F-24 đã được xử lý ở v1.2.0; các phát hiện về backend không thay đổi. v1.2.0 có thêm `GET /api/auth/me` và ca `TC_RBAC_10` (tổng 33 ca); người rà soát đã chạy lại ngày 14/11/2026: **33/33 PASS** (Java 25).

> **Kết luận chính.** Không tìm thấy bất kỳ tài liệu nào trong 6 tài liệu baseline. Vì vậy **không có bản `*_Improved.docx` nào được tạo**, đúng với yêu cầu "không tạo bản Improved giả". Mọi so sánh "tài liệu ↔ code" bên dưới chỉ ghi nhận cột Source Code; các cột tài liệu được đánh dấu **Không có tài liệu**. Phần còn lại của báo cáo là kết quả audit mã nguồn. Đây là cơ sở để nhóm sửa 6 tài liệu khi có bản gốc.

> **Cập nhật v1.3.0 (14/11/2026):** các phát hiện F-02 … F-27 trong mã nguồn **đã được sửa** (chi tiết theo từng mã trong `CHANGELOG.md` v1.3.0). Riêng F-24 (QR) và F-13 (XSS) đã xử lý ở v1.2.0; F-20 đã sửa ở v1.1.0. Bộ kiểm thử có thêm 10 ca hồi quy `TC_REG_*`; lần chạy ngày 14/11/2026 đạt **43/43 PASS** (Java 25). Các bảng bên dưới giữ nguyên để làm hồ sơ lịch sử phát hiện lỗi; cột "Trạng thái" phản ánh thời điểm rà soát. Phần số liệu §4.2 là bằng chứng **trước khi sửa**.
>
> Các điểm vẫn chưa thay đổi vì là giới hạn hoặc phạm vi, không phải lỗi: CSDL mặc định vẫn là H2 in-memory (có thể đổi bằng `config/db.properties`); chưa có báo cáo doanh thu theo ngày/tháng và tỷ lệ lấp đầy (cần quyết định phạm vi); trạng thái suất chiếu `CANCELLED/COMPLETED` vẫn là giá trị dự phòng.

---

## 1. Kết quả tìm kiếm tài liệu (Giai đoạn 1)

| # | Tài liệu | Kết quả | Nơi đã tìm |
|---|---|---|---|
| 1 | `ScopeDocument.docx` | **Không tìm thấy** | (a) thư mục repo và mọi thư mục con; (b) thư mục cha `3. Nhập môn CNPM/` (chỉ có slide bài giảng `IT3180/*.pdf` và 2 file PDF quiz); (c) toàn ổ `D:\`, cùng `Downloads`, `Documents`, `Desktop`, `OneDrive` của người dùng, tìm theo tên file; (d) GitHub: cây file nhánh `main` và `develop`; repo không có release asset nào |
| 2 | `ProjectPlan.docx` | **Không tìm thấy** | như trên |
| 3 | `SRS_Document.docx` | **Không tìm thấy** | như trên |
| 4 | `SDD_Document.docx` | **Không tìm thấy** | như trên |
| 5 | `TestPlan.docx` | **Không tìm thấy** | như trên |
| 6 | `TestReport.docx` | **Không tìm thấy** | như trên |
| – | Thư mục `01-ManagedArea/`, `02-DevelopmentArea/`, `03-SourceCodes/` | **Không tồn tại** | như trên |

Hệ quả:

- `CHANGELOG.md:10` khẳng định đã "Đóng gói Baseline toàn bộ 6 tài liệu trong `01-ManagedArea`". Thư mục này không có trong repo, kể cả lịch sử git. Đây là **khẳng định chưa có bằng chứng** (xem `MISSING_EVIDENCE.md`).
- Không có Use Case Diagram, Activity Diagram, ERD, Class Diagram, Gantt hay WBS nào để kiểm tra. Hình thức trình bày Word (mục lục, caption, header/footer…) cũng **chưa kiểm tra được**.

## 2. Bảng đối chiếu (Giai đoạn 3)

Ký hiệu: **N/A** = không có tài liệu để đối chiếu; ✔ = có trong code và đúng mô tả trong README/CHANGELOG; ⚠ = có nhưng lệch hoặc thiếu; ✘ = được mô tả nhưng chưa triển khai.

| Nội dung | Scope | ProjectPlan | SRS | SDD | TestPlan | TestReport | Source Code (bằng chứng) |
|---|---|---|---|---|---|---|---|
| Phạm vi dự án (CSD01–CSD05) | N/A | N/A | N/A | N/A | N/A | N/A | ✔ Đủ 5 phân hệ: `BookingService`, `ShowtimeService`, `MovieService`, `ComboService`, `ReportService` và 5 tab trong `index.html` |
| Stakeholders / vai trò | N/A | N/A | N/A | N/A | N/A | N/A | ✔ 3 vai trò `CUSTOMER`/`STAFF`/`MANAGER` (`schema.sql:15`). RBAC phía server chỉ có từ v1.1.0; ở v1.0.0 RBAC chỉ tồn tại trên giao diện (xem F-01) |
| Functional Requirements | N/A | N/A | N/A | N/A | N/A | N/A | ⚠ Nhiều luồng chưa chặt chẽ (F-03…F-09) |
| Non-functional Requirements | N/A | N/A | N/A | N/A | N/A | N/A | ⚠ Không có đo hiệu năng; mật khẩu băm không salt (F-11); phiên không hết hạn (F-12) |
| Use Case | N/A | N/A | N/A | N/A | N/A | N/A | Use case suy ra từ code: `DOCUMENT_TRACEABILITY.md` §2 |
| Database Design | N/A | N/A | N/A | N/A | N/A | N/A | ⚠ **10 bảng**, không phải 9 (F-14). Thiếu FK tới `users` (F-15). Có phi chuẩn hóa có chủ đích (F-16) |
| Software Architecture | N/A | N/A | N/A | N/A | N/A | N/A | ✔ Controller → Service → JDBC/Model, `com.sun.net.httpserver` (không có Spring/Servlet). ⚠ `db.properties` không được dùng (F-17) |
| Business Rules BR01–BR05, R01 | N/A | N/A | N/A | N/A | N/A | N/A | BR01 ✔, BR02 ✔, BR03 ⚠ (F-03), BR04 ⚠ (F-05, F-06), BR05 ✔, R01 ✔ trong 1 JVM (F-10) |
| Test Cases | N/A | N/A | N/A | N/A | N/A | N/A | ⚠ README ghi 21 ca; v1.0.0 thực tế có 23 ca; v1.1.0 có 32 ca (F-20) |
| Test Results | N/A | N/A | N/A | N/A | N/A | N/A | ✔ Đã chạy thật: 23/23 đạt (v1.0.0) và 32/32 đạt (v1.1.0) ngày 14/11/2026 (§5) |

## 3. Danh sách phát hiện trong mã nguồn

Mức độ: **Nghiêm trọng** (sai nghiệp vụ, bảo mật, số liệu báo cáo sai) · **Trung bình** (lệch quy tắc, khó bảo trì) · **Thấp** (tài liệu, comment, trình bày).

Phân loại theo yêu cầu: **[LỆCH]** nội dung không còn phù hợp với implementation · **[CHƯA TRIỂN KHAI]** được mô tả nhưng chưa làm · **[CHƯA MÔ TẢ]** đã làm nhưng chưa được mô tả · **[MÂU THUẪN]** · **[THIẾU BẰNG CHỨNG]**.

| ID | Mức độ | Loại | Phát hiện | Vị trí | Trạng thái |
|---|---|---|---|---|---|
| F-01 | Nghiêm trọng | [LỆCH] | Ở v1.0.0, server **không kiểm tra quyền**. `SessionManager.hasRole()` không được gọi ở đâu; `userId`/`staffId` lấy từ body request. Bất kỳ ai cũng sửa được phim, lịch chiếu, combo, xem doanh thu, hủy vé. | v1.0.0 `controllers/*.java`; `BookingController` (`userId` mặc định "4") | **Đã sửa ở v1.1.0**: `BaseController.requireLogin/requireRole`, 9 ca TC_RBAC |
| F-02 | Nghiêm trọng | [LỆCH] | **Báo cáo doanh thu không trừ vé đã hủy.** `cancelTicket` chỉ đổi vé sang `CANCELLED` và trả ghế; hóa đơn vẫn `PAID`, `total_amount` giữ nguyên. Không có trạng thái `REFUNDED` nào được ghi, dù `schema.sql:112` có khai báo. | `BookingService.java:647` (cancelTicket); `ReportService.java:87–91` | Chưa sửa. **Đã kiểm chứng**: sau khi hủy `TKT-002-D04`, `totalRevenue` vẫn 1.660.000 ₫ còn `ticketsSold` giảm 8→7 (§4.2) |
| F-03 | Nghiêm trọng | [LỆCH] | **BR03 có thể bị bỏ qua.** `confirmCheckout` chấp nhận cả ghế đang `AVAILABLE` (chưa giữ), và cả ghế `HELD` có `held_by_user_id = NULL`. Bước thanh toán cũng không giới hạn 8 ghế như `holdSeats`. | `BookingService.java:337–338` (validForCheckout), `:271` | Chưa sửa. **Đã kiểm chứng**: thanh toán ghế chưa giữ → `CHECKOUT_SUCCESS` |
| F-04 | Nghiêm trọng | [LỆCH] | **Online + POS ≠ Tổng doanh thu** khi `bookingChannel` khác ONLINE/POS. Server lưu nguyên giá trị kênh do client gửi. | `BookingService.java:277`; `ReportService.java:89–90` | Chưa sửa. **Đã kiểm chứng**: một đơn kênh `KIOSK` làm tổng = 1.755.000 nhưng Online + POS = 1.660.000 |
| F-05 | Trung bình | [CHƯA TRIỂN KHAI] | BR04 "Hủy vé **trước giờ chiếu**" (Javadoc) không được kiểm tra: vé của suất đã chiếu vẫn hủy được. | `BookingService.java:641–647` | Chưa sửa |
| F-06 | Trung bình | [CHƯA TRIỂN KHAI] | Soát vé không kiểm tra ngày/giờ suất chiếu: vé ngày mai vẫn soát được hôm nay. Hàm cũng không dùng `synchronized`/transaction, nên hai máy quét cùng lúc có thể cùng báo `CHECKIN_SUCCESS`. | `BookingService.java:573` | Chưa sửa |
| F-07 | Trung bình | [LỆCH] | Mã hóa đơn `"INV-" + System.currentTimeMillis() % 1000000` lặp lại sau khoảng 16,7 phút, có thể vi phạm UNIQUE và gây lỗi 500. | `BookingService.java:417` | Chưa sửa |
| F-08 | Trung bình | [LỆCH] | "Doanh thu theo phim" cộng cả tiền **combo F&B** của hóa đơn vào phim. `showtimeCount` đếm cả suất đã hủy. | `ReportService.java:123–130` | Chưa sửa; cần định nghĩa lại trong SRS |
| F-09 | Trung bình | [MÂU THUẪN] | Ràng buộc nhập liệu khác nhau giữa giao diện và server: thời lượng phim UI 45–300 nhưng server 30–360; giá combo UI ≥ 10.000 nhưng server > 0; giá vé cơ sở UI ≥ 45.000 nhưng server > 0. | `index.html:356, 456, 321` ↔ `MovieService.java:100`, `ComboService.java:68`, `ShowtimeService.java` (Node 1) | Chưa sửa |
| F-10 | Trung bình | [CHƯA MÔ TẢ] | R01 chỉ được bảo vệ bằng `synchronized` trên một monitor tĩnh, nên chỉ đúng khi chạy **1 tiến trình JVM**. Không có khóa ở mức CSDL (`SELECT … FOR UPDATE`). Giới hạn này cần được ghi rõ trong SDD. | `BookingService.java:40` | Ghi chú thiết kế |
| F-11 | Trung bình | [MÂU THUẪN] | Comment trong seed ghi mật khẩu băm "SHA-256 kèm salt 'CINEHUB_SALT_2026'", nhưng `hashSha256` **không dùng salt**: hash của `admin123` là SHA-256 thuần. | `seed_data.sql:7` ↔ `PasswordUtils.java:24–29` | Chưa sửa |
| F-12 | Trung bình | [CHƯA MÔ TẢ] | Phiên đăng nhập lưu trong bộ nhớ, **không có thời hạn**, mất khi khởi động lại server. | `SessionManager.java:16` | Chưa sửa |
| F-13 | Trung bình | [LỆCH] | XSS: frontend chèn dữ liệu người dùng (tên khách, tên phim, tên combo) vào `innerHTML` mà không escape. | `app.js` v1.1.0 (19 chỗ dùng `innerHTML`) | **Đã sửa ở v1.2.0** (escape mọi dữ liệu chèn vào HTML, theo CHANGELOG v1.2.0) |
| F-14 | Thấp | [MÂU THUẪN] | **Số bảng CSDL**: `schema.sql` có **10** lệnh `CREATE TABLE` (users, movies, auditoriums, seats, showtimes, showtime_seats, combos, invoices, invoice_combos, tickets). Nhưng comment `schema.sql:5`, log khởi động `CineHubApplication.java:68` và `CHANGELOG.md:37` ghi **9 bảng**; còn `DatabaseConnection.java:76` ghi 10 bảng. | như trên | Chưa sửa; tài liệu phải ghi **10 bảng** |
| F-15 | Thấp | [CHƯA MÔ TẢ] | Không có khóa ngoại tới `users` từ `invoices.user_id`, `invoices.staff_id`, `tickets.checked_in_by`, `showtime_seats.held_by_user_id`. Quan hệ chỉ tồn tại ở mức logic. | `schema.sql:71–146` | ERD phải vẽ đúng (nét đứt / ghi chú "logical FK") |
| F-16 | Thấp | [MÂU THUẪN] | Khẳng định "chuẩn 3NF" chưa chính xác tuyệt đối. Có các cột suy ra được: `tickets.seat_code/seat_type/showtime_id`, `invoices.combo_summary`, `auditoriums.total_seats`, `showtime_seats.final_price`. Đây là **phi chuẩn hóa có chủ đích** (snapshot lịch sử) và cần được ghi rõ như vậy. | `schema.sql` | Sửa mô tả |
| F-17 | Thấp | [LỆCH] | `config/db.properties.example` (MySQL/H2 file, `booking.hold.minutes`, `showtime.cleaning.buffer.minutes`) **không được đọc ở đâu**. URL CSDL hardcode là H2 in-memory, dữ liệu mất mỗi lần khởi động lại. | `DatabaseConnection.java:28–29` | Chưa sửa |
| F-18 | Thấp | [LỆCH] | Lược đồ dự phòng `getFallbackSchemaSql()` thiếu toàn bộ FK và UNIQUE (`uq_auditorium_seat`, `uq_showtime_seat`), nên khác `schema.sql`. | `DatabaseConnection.java:416` | Chưa sửa |
| F-19 | Thấp | [MÂU THUẪN] | CHANGELOG gọi lớp Null Object là `NullComboSelection`; tên thật trong code là `NullComboOrderItem`. Cũng cần lưu ý `ComboOrderItem`/`NullComboOrderItem` chỉ là lớp lồng bên trong `SeatPricingPolicy`, **không kế thừa** `SeatPricingPolicy`. | `CHANGELOG.md` (v0.3.0) ↔ `SeatPricingPolicy.java:107, 153` | Sửa tài liệu |
| F-20 | Thấp | [MÂU THUẪN] | Số ca kiểm thử: README ghi "21 ca"; commit v1.0.0 và bộ chạy thực tế có **23 ca**; v1.1.0 có **32 ca**. | `README.md` ↔ `CineHubTestRunner.java` | Đã sửa README ở v1.1.0 (32 ca) |
| F-21 | Thấp | [MÂU THUẪN] | `pom.xml` khai báo JUnit 5 nhưng không có test JUnit nào; bộ kiểm thử là một `main()` tự viết, nên `mvn test` không chạy gì. | `pom.xml:33–40` | Ghi rõ trong TestPlan |
| F-22 | Thấp | [LỆCH] | README hướng dẫn chạy "tại thư mục `03-SourceCodes`", nhưng repo không có thư mục này. | `README.md:19, 50` | Chưa sửa |
| F-23 | Thấp | [MÂU THUẪN] | Seed ghi tài khoản `fnb / staff123`; tên đăng nhập thật là `fnb_staff`. | `seed_data.sql:10` | Chưa sửa |
| F-24 | Thấp | [CHƯA TRIỂN KHAI] | "Vé điện tử **QR**": hiện chỉ sinh chuỗi `qr_payload`; giao diện hiển thị chữ `[QR CODE]` chứ không có ảnh QR thật. | `app.js` v1.1.0 (`openTicketModal`) | **Đã bổ sung ở v1.2.0**: ảnh QR sinh từ `qr_payload` bằng qrcodejs 1.0.0 (cần Internet; không có mạng thì hiện chuỗi mã) |
| F-25 | Thấp | [CHƯA TRIỂN KHAI] | Trạng thái `showtimes.status` = `CANCELLED/COMPLETED`, `combos.status` = `OUT_OF_STOCK` (thực tế), `payment_status` = `PENDING/REFUNDED` được khai báo trong schema nhưng **không có luồng nào ghi các giá trị này**. Ngoài ra `toggleComboStatus` có thể mở bán lại combo đã hết hàng. | `schema.sql:65, 93, 112`; `ComboService.java:143` | Tài liệu không được mô tả các trạng thái này là đã hoạt động |
| F-26 | Thấp | [LỆCH] | Một số controller trả lỗi nghiệp vụ với HTTP **200** (`VALIDATION_ERROR`, `SCHEDULE_ERROR`). Thông báo lỗi trả về client chứa nguyên `ex.getMessage()`. | `ComboController.java:71`, `MovieController.java:98`, `ShowtimeController.java:68` | Chưa sửa |
| F-27 | Thấp | [LỆCH] | `getShowtimes` ghép chuỗi `dateFilter` trực tiếp vào SQL (chỉ loại bỏ dấu `'`). | `ShowtimeService.java:90` | Chưa sửa |

## 4. Rà soát riêng phân hệ CSD05 (Giai đoạn 6)

### 4.1 Công thức thực tế trong code (`ReportService.buildDashboardReportJson`)

| Chỉ số | Công thức trong code | Đánh giá |
|---|---|---|
| Total Revenue | `SUM(total_amount)` trên `invoices` với `payment_status='PAID'` | ⚠ Chỉ tính hóa đơn PAID là đúng, nhưng vé đã hủy vẫn nằm trong hóa đơn PAID (F-02) |
| Seat Revenue | `SUM(seat_total)` (PAID) | ⚠ Có cùng lỗi với vé hủy |
| Combo Revenue | `SUM(combo_total)` (PAID) | ✔ Seat + Combo = Total vì `discount_amount` luôn bằng 0 |
| Online / POS Revenue | `SUM(CASE booking_channel=…)` | ⚠ Không khớp tổng khi có kênh khác (F-04) |
| Total Invoices | `COUNT(*)` hóa đơn PAID | ✔ |
| Tickets Sold | Số vé `ISSUED` + `CHECKED_IN` | ✔ Đã loại vé hủy |
| Checked-in | Số vé `CHECKED_IN` | ✔ |
| Cancelled | Số vé `CANCELLED` | ✔ |
| Tỷ lệ check-in | **Không có** trong API | ✘ Nếu bổ sung, mẫu số nên là số vé hợp lệ (ISSUED + CHECKED_IN) |
| Revenue by Movie | Subquery `SUM(i.total_amount)` theo `showtimes.movie_id` | ✔ Không bị đếm trùng (dùng subquery, không JOIN nhân bản). ⚠ Có tính cả combo (F-08) |
| Showtime Count theo phim | `COUNT(DISTINCT st.showtime_id)` | ⚠ Đếm cả suất có status khác SCHEDULED |
| Revenue theo ngày / tháng | **Không có** | ✘ Chưa triển khai, không có tham số thời gian trong API |
| Occupancy Rate | **Không có** | ✘ Chưa triển khai |

### 4.2 Bằng chứng chạy thật (14/11/2026, CSDL mẫu sau `resetDatabase()`)

Chương trình kiểm chứng gọi trực tiếp `ReportService` và `BookingService` trên dữ liệu seed (không sửa mã nghiệp vụ):

| Bước | totalRevenue | seat | combo | online | POS | invoices | ticketsSold | cancelled |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Dữ liệu mẫu ban đầu | 1.660.000 | 1.078.000 | 582.000 | 1.023.000 | 637.000 | 4 | 8 | 0 |
| Sau khi hủy `TKT-002-D04` (VIP 110.000 ₫) | **1.660.000** | 1.078.000 | 582.000 | 1.023.000 | 637.000 | 4 | 7 | 1 |
| Sau 1 đơn kênh `KIOSK` (95.000 ₫) | 1.755.000 | 1.173.000 | 582.000 | **1.023.000** | **637.000** | 5 | 8 | 1 |

Đối chiếu tính tay từ `DatabaseConnection.seedSampleInvoicesAndTickets`:

| Hóa đơn | Vé | Combo | Tổng |
|---|---|---|---:|
| INV-202605-001 (ONLINE) | C05, C06 VIP suất #1: 2 × 110.000 | CB02 115.000 | 335.000 |
| INV-202605-002 (ONLINE) | D04, D05 VIP suất #1: 2 × 110.000 | CB01 79.000 | 299.000 |
| INV-202605-003 (POS) | F01, F02 ghế đôi suất #2: 2 × 209.000 | CB03 219.000 | 637.000 |
| INV-202605-004 (ONLINE) | A04, A05 thường suất #4: 2 × 110.000 | CB04 169.000 | 389.000 |
| **Tổng** | 1.078.000 | 582.000 | **1.660.000** |

Số liệu ban đầu khớp với tính tay. Hai lần chạy tiếp theo xác nhận F-02 và F-04.

### 4.3 Đề xuất cho Dashboard (chưa triển khai)

Chưa thêm biểu đồ nào vào Dashboard, vì hai lý do. Thứ nhất, phần giao diện đang được thiết kế lại ở phiên khác. Thứ hai, các biểu đồ theo ngày/tháng và tỷ lệ lấp đầy cần **API mới** (mở rộng nghiệp vụ), nên phải được nhóm duyệt trước. Đề xuất, theo thứ tự ưu tiên:

1. Sửa F-02 và F-04 trước. Nếu chưa sửa, mọi biểu đồ doanh thu đều hiển thị số sai.
2. Tỷ trọng Online/POS và Vé/Combo: dùng được ngay dữ liệu API hiện có, sau khi sửa F-04.
3. Doanh thu theo phim: cần quyết định có tách combo ra hay không (F-08).
4. Doanh thu theo ngày/tháng: cần thêm `GROUP BY CAST(created_at AS DATE)` và tham số khoảng thời gian.
5. Tỷ lệ lấp đầy: `BOOKED / tổng showtime_seats` của các suất `SCHEDULED`, sau khi định nghĩa rõ mẫu số trong SRS.

## 5. Kết quả kiểm thử đã thực thi (bằng chứng thật)

| Lần chạy | Mã nguồn | Môi trường | Kết quả |
|---|---|---|---|
| 14/11/2026, lần 1 | v1.0.0 (chưa sửa) | Windows 11, `java version "25"` (2025-09-16 LTS), H2 1.4.200 in-memory; biên dịch `javac -encoding UTF-8` | **23/23 PASS** |
| 14/11/2026, lần 2 | v1.1.0 (thêm RBAC) | như trên; server HTTP thật trên cổng 18089 cho TC_RBAC | **32/32 PASS** |

Giới hạn của bằng chứng này:

- Môi trường chạy là **Java 25**, trong khi `pom.xml` đặt `release 21`; chưa chạy trên JDK 21.
- Không có đo coverage. **Không được ghi bất kỳ % coverage nào** vào TestReport.
- Các ca TC_WB đã gán số node CFG trong Javadoc, nhưng **không có hình CFG** nào trong repo để đối chiếu.

## 6. Việc nhóm cần làm tiếp

1. **Cung cấp 6 file DOCX gốc** (hoặc bản PDF/ảnh chụp). Khi có, có thể làm tiếp Giai đoạn 4, 5, 7, 8 (sửa nội dung, sửa sơ đồ, chuẩn hóa Word) trên các bản thật.
2. Quyết định cách xử lý F-02, F-03, F-04 (ảnh hưởng nghiệp vụ và số liệu báo cáo), rồi cập nhật SRS cho khớp.
3. Thống nhất **10 bảng**, kèm ghi chú về phi chuẩn hóa có chủ đích trong SRS/SDD (F-14, F-16).
4. Sửa CHANGELOG cho khớp lịch sử git, hoặc bổ sung bằng chứng baseline (xem `MISSING_EVIDENCE.md`).
5. Dùng các sơ đồ tham chiếu trong `DIAGRAM_REVIEW.md` để đối chiếu, sửa các sơ đồ trong SDD/SRS.
