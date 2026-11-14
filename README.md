# CineHub CMS — Hệ thống Website Quản lý và Đặt vé Rạp chiếu phim

**Học phần:** IT3180 - Nhập môn Công nghệ Phần mềm (Trường CNTT&TT - Đại học Bách Khoa Hà Nội)  
**Giảng viên hướng dẫn:** TS. Nguyễn Nhất Hải  
**Nhóm thực hiện:** Nhóm 3 (CineHub Team)

| STT | Họ và tên | MSSV | Vai trò dự án (Scrum & SCM) | Phân hệ phụ trách |
| :--- | :--- | :--- | :--- | :--- |
| 1 | **Phạm Văn Quyết** | `202416331` | Project Manager / Scrum Master / Trưởng ban CCB | Scope, ProjectPlan, Thống kê & Báo cáo doanh thu (`CSD05`) |
| 2 | **Vũ Việt Tiến** | `202416362` | Product Owner / Business Analyst / Thành viên CCB | SRS Document, Quản lý Phim & Lịch chiếu (`CSD03`), Đặt vé (`CSD01`) |
| 3 | **Hà Sỹ Bách** | `202416129` | Software Architect / Lead Dev / SCM Manager | SDD Document, Kiến trúc MVC, Sơ đồ ghế & Khóa giao dịch (`BR02`, `BR03`, `R01`) |
| 4 | **Lê Anh Tuấn** | `202416639` | UI/UX Designer / Frontend Developer | Thiết kế Giao diện & Mẫu thử 4 mức độ, Bán vé POS (`CSD02`), Soát vé QR (`CSD05`) |
| 5 | **Nguyễn Xuân Hồng Quân** | `202416321` | QA/QC Lead / Tester / Configuration Auditor | TestPlan, TestReport (Kiểm thử Hộp trắng CFG & Hộp đen), Quản lý Combo F&B (`CSD04`) |

---

## 1. Kiến trúc Phần mềm (Web MVC)

Mã nguồn (thư mục gốc của repository) được tổ chức theo đúng kiến trúc phân tầng **Model - View - Controller - Service (MVC)** và tuân thủ **Sun Java Coding Conventions & Javadoc** (Chương 8 - IT3180):

- `src/main/java/com/cinehub/models/`: Các lớp thực thể (`User`, `Movie`, `Auditorium`, `Seat`, `Showtime`, `ShowtimeSeat`, `Combo`, `Invoice`, `Ticket`) và lớp đa hình tính giá ghế `SeatPricingPolicy` (`StandardSeatPricing`, `VipSeatPricing`, `CoupleSeatPricing`, `NullComboOrderItem` — minh họa kỹ thuật **Refactoring: Replace Conditional with Polymorphism & Introduce Null Object**).
- `src/main/java/com/cinehub/services/`: Tầng xử lý logic nghiệp vụ (`AuthService`, `MovieService`, `AuditoriumService`, `ShowtimeService`, `ComboService`, `BookingService`, `ReportService`) thực thi nghiêm ngặt 5 quy tắc nghiệp vụ **BR01 – BR05**.
- `src/main/java/com/cinehub/controllers/`: Tầng điều khiển HTTP RESTful (`AuthController`, `MovieController`, `ShowtimeController`, `ComboController`, `BookingController`, `ReportController`).
- `src/main/resources/templates/index.html`, `static/css/style.css`, `static/js/app.js`: Giao diện Web tiếng Việt tương tác thời gian thực cho cả 5 phân hệ **CSD01 – CSD05**.
- `config/`: Chứa lược đồ CSDL chuẩn 3NF (`schema.sql`), dữ liệu mẫu (`seed_data.sql`), cấu hình kết nối (`db.properties.example`) và quy tắc kiểm tra mã nguồn (`checkstyle.xml`).
- `test/com/cinehub/CineHubTestRunner.java`: Bộ kiểm thử tự động Hộp trắng (CFG Basis Path), Hộp đen (Bảng quyết định), Kiểm thử đồng thời đa luồng (Concurrency Test chống đặt trùng ghế `R01`) và Kiểm thử phân quyền RBAC qua HTTP.

### Phân quyền RBAC phía máy chủ

Sau khi đăng nhập (`POST /api/auth/login`), trình duyệt gửi mã phiên ở tiêu đề `Authorization: Bearer <token>`. Máy chủ xác định danh tính **chỉ** từ mã phiên; các trường `userId`/`staffId` trong thân yêu cầu bị bỏ qua. `GET /api/auth/me` trả về người dùng của mã phiên (401 nếu phiên đã hết hạn) để giao diện kiểm tra lại phiên khi tải trang.

| API | Khách (chưa đăng nhập) | CUSTOMER | STAFF | MANAGER |
| :--- | :---: | :---: | :---: | :---: |
| `GET` phim, phòng chiếu, suất chiếu, combo, sơ đồ ghế | ✔ | ✔ | ✔ | ✔ |
| `POST /api/booking/hold`, `checkout` kênh ONLINE | 401 | ✔ | ✔ | ✔ |
| `POST /api/booking/checkout` kênh POS | 401 | 403 | ✔ | ✔ |
| `GET /api/booking/tickets` | 401 | Chỉ vé của mình | Tất cả | Tất cả |
| `POST /api/booking/cancel` | 401 | Chỉ vé của mình | ✔ | ✔ |
| `POST /api/booking/checkin` | 401 | 403 | ✔ | ✔ |
| `POST /api/combos` | 401 | 403 | ✔ | ✔ |
| `POST /api/movies`, `POST /api/showtimes` | 401 | 403 | 403 | ✔ |
| `GET /api/reports/dashboard` | 401 | 403 | 403 | ✔ |

Giao diện cũng ẩn các tab không thuộc quyền của vai trò hiện tại, nhưng đó chỉ là tiện ích hiển thị — quyền thật luôn do máy chủ kiểm tra.

---

## 2. Hướng dẫn Biên dịch, Kiểm thử và Chạy Demo

Chạy trực tiếp tập tin `run-demo.bat` hoặc thực thi các lệnh sau tại thư mục gốc của repository:

```powershell
# 1. Biên dịch toàn bộ mã nguồn và bộ kiểm thử
javac -encoding UTF-8 -cp "$env:USERPROFILE\.m2\repository\com\h2database\h2\1.4.200\h2-1.4.200.jar" -d out src/main/java/com/cinehub/models/*.java src/main/java/com/cinehub/utils/*.java src/main/java/com/cinehub/services/*.java src/main/java/com/cinehub/controllers/*.java src/main/java/com/cinehub/CineHubApplication.java test/com/cinehub/CineHubTestRunner.java

# 2. Chạy bộ kiểm thử tự động (43 ca kiểm thử Hộp trắng, Hộp đen, Đa luồng, RBAC & hồi quy; dùng cổng 18089)
java "-Dfile.encoding=UTF-8" -cp "out;$env:USERPROFILE\.m2\repository\com\h2database\h2\1.4.200\h2-1.4.200.jar" com.cinehub.CineHubTestRunner

# 3. Khởi động máy chủ Web MVC tại http://localhost:8080
java "-Dfile.encoding=UTF-8" -cp "out;$env:USERPROFILE\.m2\repository\com\h2database\h2\1.4.200\h2-1.4.200.jar" com.cinehub.CineHubApplication
```
