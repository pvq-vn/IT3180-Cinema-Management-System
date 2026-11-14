# CHANGELOG - Hệ thống Quản lý và Đặt vé Rạp chiếu phim CineHub (CMS)

Mọi thay đổi quan trọng của mã nguồn và tài liệu cấu hình trong dự án **IT3180 - Nhóm 3 (CineHub)** đều được ghi nhận tại tập tin này theo chuẩn Quản lý Cấu hình Phần mềm (IEEE Std 828 / SCM).

## [v1.3.0] - 2026-11-14
### Fixed (theo `Documentation_Improved/AUDIT_REPORT.md`)
- **F-02** Hủy vé trừ tiền vé khỏi hóa đơn; hủy hết vé của hóa đơn thì hóa đơn chuyển `REFUNDED`, hoàn cả combo và trả lại tồn kho combo. Báo cáo doanh thu phản ánh đúng số đã thu.
- **F-03** Thanh toán Online bắt buộc ghế đang được giữ bởi chính người mua (BR03); POS vẫn bán thẳng ghế trống. Giới hạn 8 ghế áp dụng cả khi thanh toán. Giao diện tự giữ/gia hạn ghế ngay trước khi thanh toán.
- **F-04** Chỉ chấp nhận kênh `ONLINE`/`POS` và phương thức thanh toán đúng kênh, nên Online + POS luôn bằng tổng doanh thu.
- **F-05** Không hủy được vé khi suất chiếu đã bắt đầu. **F-06** Soát vé từ chối vé của ngày khác; 2 máy quét cùng lúc chỉ 1 máy thành công.
- **F-07** Mã hóa đơn duy nhất `INV-yyMMddHHmmss-XXXX`. **F-08** Doanh thu theo phim chỉ tính tiền vé hiệu lực (tổng khớp doanh thu vé), không đếm suất đã hủy.
- **F-09** Ràng buộc nhập liệu trên giao diện khớp máy chủ. **F-10** Giữ ghế/thanh toán dùng thêm `UPDATE` có điều kiện ở CSDL (chống bán trùng cả khi chạy nhiều tiến trình).
- **F-11** Mật khẩu băm PBKDF2-HMAC-SHA256 có salt; mật khẩu SHA-256 cũ của dữ liệu mẫu tự được băm lại sau lần đăng nhập đầu. **F-12** Mã phiên 256 bit, tự hết hạn sau 480 phút không hoạt động.
- **F-15/F-18** Thêm khóa ngoại tới `users`; lược đồ dự phòng có đủ khóa ngoại và ràng buộc UNIQUE như `schema.sql`.
- **F-17** Đọc `config/db.properties` (CSDL, cổng, thời gian giữ ghế, đệm dọn phòng, hạn phiên) qua lớp mới `AppConfig`.
- **F-25** Combo tự chuyển `OUT_OF_STOCK` khi hết hàng; không mở bán lại combo có tồn kho 0; sửa combo không còn vô tình mở bán lại combo đã ngừng. Không trùng mã combo.
- **F-26** Lỗi dữ liệu trả HTTP 400, lỗi hệ thống trả 500 với thông báo chung (chi tiết chỉ ghi log máy chủ). **F-27** Lọc lịch chiếu theo ngày dùng tham số SQL.
- **F-14, F-16, F-19, F-21, F-22, F-23** Sửa số bảng (10), ghi chú phi chuẩn hóa có chủ đích, tên `NullComboOrderItem`, bỏ phụ thuộc JUnit không dùng, đường dẫn README, tên tài khoản `fnb_staff`, chú thích mật khẩu mẫu.

### Added
- 10 ca kiểm thử hồi quy `TC_REG_*` (tổng 43 ca).

## [v1.2.0] - 2026-11-14
### Changed
- Thiết kế lại toàn bộ giao diện theo hướng "Bảng giờ chiếu lật chữ" (`index.html`, `style.css`, `app.js` viết lại): sảnh sáng màu đá, bảng suất chiếu kiểu bảng khởi hành split-flap, sơ đồ ghế phân biệt trạng thái bằng hình dạng (không chỉ màu), giỏ hàng dạng cuống vé, hiệu ứng lật chữ khi số liệu thay đổi (tắt khi người dùng bật giảm chuyển động).
- Tiêu đề viết cho người dùng; mã phân hệ CSDxx và quy tắc BRxx chuyển thành nhãn phụ cạnh tiêu đề.
- Bán tại quầy (CSD02): chọn ghế trực tiếp trên sơ đồ thay cho danh sách chọn nhiều.
- Lập lịch (CSD03): ô nhập `datetime-local`; thêm dòng thời gian theo phòng chiếu, thể hiện phần 15 phút dọn phòng (BR01).
- Soát vé (CSD05): bảng kết quả soát vé, bảng KPI doanh thu (chỉ Quản lý), cột tỷ trọng doanh thu theo phim.
- Vé điện tử hiển thị mã QR thật từ `qr_payload` (thư viện qrcodejs 1.0.0 qua cdnjs; không có mạng thì hiện chuỗi mã).
- Điện thoại: đặt vé theo 3 bước (Suất → Ghế → Thanh toán), mỗi bước một màn hình.

### Fixed
- Chặn XSS: mọi dữ liệu chèn vào HTML đều được escape.
- Phiên lưu trong trình duyệt được xác minh lại qua `GET /api/auth/me` khi tải trang (trước đây vẫn hiện người dùng cũ sau khi máy chủ khởi động lại).
- Ngày giờ hiển thị theo kiểu Việt Nam (14/11 09:00), tiền theo dạng 1.250.000 ₫.

### Added
- API `GET /api/auth/me` và ca kiểm thử `TC_RBAC_10` (tổng 33 ca).
- `PRODUCT.md` (mô tả sản phẩm cho các lần thiết kế sau).

## [v1.1.0] - 2026-11-14
### Security
- Thực thi phân quyền RBAC phía máy chủ: mọi API ghi dữ liệu yêu cầu mã phiên qua tiêu đề `Authorization: Bearer <token>` (401 nếu thiếu/sai, 403 nếu sai vai trò). Bổ sung `currentUser`, `requireLogin`, `requireRole` trong `BaseController`.
- Danh tính người dùng/nhân viên lấy từ phiên đăng nhập; bỏ qua `userId`/`staffId` do trình duyệt gửi lên (chặn mạo danh).
- Khách hàng chỉ xem/hủy được vé của chính mình (`BookingService.getTicketsByUser`, `getTicketOwnerUserId`); chỉ STAFF/MANAGER được bán vé POS và soát vé; chỉ MANAGER được quản lý phim, lịch chiếu và xem báo cáo doanh thu.

### Changed
- Giao diện mặc định ở trạng thái khách chưa đăng nhập (trước đây tự đăng nhập sẵn tài khoản Quản lý), gửi mã phiên kèm mọi lời gọi API, ẩn tab theo vai trò, thêm nút Đăng xuất.

### Added
- 9 ca kiểm thử RBAC qua HTTP (`TC_RBAC_01` – `TC_RBAC_09`) trong `CineHubTestRunner` (tổng 32 ca).

## [v1.0.0] - 2026-05-25 (Sprint 4 Baseline - Release)
### Added
- Hoàn thiện phân hệ **CSD05**: Soát vé điện tử bằng mã QR (`checkInTicket`) và Bảng điều khiển Thống kê báo cáo doanh thu (`ReportService`, `ReportController`).
- Hoàn thiện bộ kiểm thử tự động Hộp trắng (CFG Basis Path) và Hộp đen (Decision Table) cùng kiểm thử đồng thời đa luồng (`CineHubTestRunner`).
- Đóng gói Baseline toàn bộ 6 tài liệu trong `01-ManagedArea`: `ScopeDocument.docx`, `ProjectPlan.docx`, `SRS_Document.docx`, `SDD_Document.docx`, `TestPlan.docx`, `TestReport.docx`.

## [v0.3.0] - 2026-05-10 (Sprint 3 Baseline)
### Added
- Hoàn thiện phân hệ **CSD01** (Đặt vé trực tuyến & Giữ ghế tạm thời 5 phút theo **BR02, BR03**) và **CSD02** (Bán vé trực tiếp tại quầy POS).
- Áp dụng kỹ thuật Tái cấu trúc mã nguồn (Refactoring - Chương 8): Đa hình hóa chính sách tính giá ghế (`SeatPricingPolicy`: `StandardSeatPricing`, `VipSeatPricing`, `CoupleSeatPricing`) và Null Object `NullComboOrderItem` theo **BR05** (đính chính v1.3.0: bản trước ghi nhầm tên `NullComboSelection`).
- Bổ sung cơ chế khóa đồng bộ giao dịch (`synchronized` + JDBC Transaction) xử lý triệt để rủi ro **R01** (2 người đặt trùng 1 ghế tại cùng thời điểm).

## [v0.2.0] - 2026-04-20 (Sprint 2 Baseline)
### Added
- Hoàn thiện phân hệ **CSD03**: Quản lý Phim, Phòng chiếu, Sơ đồ ghế vật lý 3 hạng (Standard, VIP, Couple) và Lập lịch chiếu kiểm tra chống chồng lấn thời gian có cộng 15 phút dọn vệ sinh (**BR01**).
- Hoàn thiện phân hệ **CSD04**: Quản lý Danh mục Đồ ăn, Thức uống & Combo F&B kèm cơ chế khóa mềm trạng thái ngừng kinh doanh (`DISCONTINUED`).

## [v0.1.0] - 2026-03-30 (Sprint 1 Baseline)
### Added
- Khởi tạo kiến trúc khung ứng dụng Web MVC (`models`, `views`, `controllers`, `services`, `utils`).
- Thiết kế CSDL quan hệ 10 bảng (đính chính v1.3.0: bản trước ghi nhầm 9 bảng) (`config/schema.sql`, `config/seed_data.sql`).
- Cài đặt chức năng Đăng nhập, Phân quyền RBAC (`CUSTOMER`, `STAFF`, `MANAGER`) và Quản lý phiên làm việc (`AuthService`, `SessionManager`).

