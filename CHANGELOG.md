# CHANGELOG - Hệ thống Quản lý và Đặt vé Rạp chiếu phim CineHub (CMS)

Mọi thay đổi quan trọng của mã nguồn và tài liệu cấu hình trong dự án **IT3180 - Nhóm 3 (CineHub)** đều được ghi nhận tại tập tin này theo chuẩn Quản lý Cấu hình Phần mềm (IEEE Std 828 / SCM).

## [v1.0.0] - 2026-05-25 (Sprint 4 Baseline - Release)
### Added
- Hoàn thiện phân hệ **CSD05**: Soát vé điện tử bằng mã QR (`checkInTicket`) và Bảng điều khiển Thống kê báo cáo doanh thu (`ReportService`, `ReportController`).
- Hoàn thiện bộ kiểm thử tự động Hộp trắng (CFG Basis Path) và Hộp đen (Decision Table) cùng kiểm thử đồng thời đa luồng (`CineHubTestRunner`).
- Đóng gói Baseline toàn bộ 6 tài liệu trong `01-ManagedArea`: `ScopeDocument.docx`, `ProjectPlan.docx`, `SRS_Document.docx`, `SDD_Document.docx`, `TestPlan.docx`, `TestReport.docx`.

## [v0.3.0] - 2026-05-10 (Sprint 3 Baseline)
### Added
- Hoàn thiện phân hệ **CSD01** (Đặt vé trực tuyến & Giữ ghế tạm thời 5 phút theo **BR02, BR03**) và **CSD02** (Bán vé trực tiếp tại quầy POS).
- Áp dụng kỹ thuật Tái cấu trúc mã nguồn (Refactoring - Chương 8): Đa hình hóa chính sách tính giá ghế (`SeatPricingPolicy`: `StandardSeatPricing`, `VipSeatPricing`, `CoupleSeatPricing`) và `NullComboSelection` theo **BR05**.
- Bổ sung cơ chế khóa đồng bộ giao dịch (`synchronized` + JDBC Transaction) xử lý triệt để rủi ro **R01** (2 người đặt trùng 1 ghế tại cùng thời điểm).

## [v0.2.0] - 2026-04-20 (Sprint 2 Baseline)
### Added
- Hoàn thiện phân hệ **CSD03**: Quản lý Phim, Phòng chiếu, Sơ đồ ghế vật lý 3 hạng (Standard, VIP, Couple) và Lập lịch chiếu kiểm tra chống chồng lấn thời gian có cộng 15 phút dọn vệ sinh (**BR01**).
- Hoàn thiện phân hệ **CSD04**: Quản lý Danh mục Đồ ăn, Thức uống & Combo F&B kèm cơ chế khóa mềm trạng thái ngừng kinh doanh (`DISCONTINUED`).

## [v0.1.0] - 2026-03-30 (Sprint 1 Baseline)
### Added
- Khởi tạo kiến trúc khung ứng dụng Web MVC (`models`, `views`, `controllers`, `services`, `utils`).
- Thiết kế CSDL quan hệ 9 bảng chuẩn 3NF (`config/schema.sql`, `config/seed_data.sql`).
- Cài đặt chức năng Đăng nhập, Phân quyền RBAC (`CUSTOMER`, `STAFF`, `MANAGER`) và Quản lý phiên làm việc (`AuthService`, `SessionManager`).

