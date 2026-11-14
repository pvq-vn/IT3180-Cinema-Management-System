# MISSING EVIDENCE – Nội dung thiếu bằng chứng hoặc cần bổ sung

Mỗi mục dưới đây là một khẳng định đang xuất hiện trong repo (README, CHANGELOG, comment trong code) hoặc là thứ mà tài liệu học phần bắt buộc phải có, nhưng **chưa kiểm chứng được**. Không được đưa các nội dung này vào báo cáo dưới dạng "đã hoàn thành" cho tới khi có bằng chứng.

| ID | Khẳng định / nội dung | Nguồn | Vì sao chưa có bằng chứng | Bằng chứng cần bổ sung |
|---|---|---|---|---|
| M-01 | 6 tài liệu baseline (Scope, ProjectPlan, SRS, SDD, TestPlan, TestReport) | `CHANGELOG.md:10` | Không có trên máy, không có trong 2 nhánh GitHub, không có release asset | Nộp 6 file DOCX (hoặc PDF) để rà soát |
| M-02 | Các baseline Sprint 1–4 ngày 30/03, 20/04, 10/05, 25/05/2026 | `CHANGELOG.md` | Repo chỉ có 2 commit: "Initial commit" (07/11/2026) và commit v1.0.0 (13/11/2026). Không có tag `v0.1.0`…`v1.0.0` | Lịch sử commit/tag thật, biên bản Sprint Review, hoặc ghi rõ trong ProjectPlan rằng mã được đẩy lên repo một lần vào cuối dự án |
| M-03 | Thư mục `01-ManagedArea`, `02-DevelopmentArea`, `03-SourceCodes` (cấu trúc SCM) | `CHANGELOG.md`, `README.md:19, 50` | Không tồn tại | Tạo đúng cấu trúc, hoặc sửa README/CHANGELOG cho khớp repo hiện tại |
| M-04 | Vai trò CCB (Configuration Control Board), quy trình kiểm soát thay đổi | `README.md` (bảng thành viên) | Không có Change Request, biên bản CCB hay issue/PR nào trên GitHub | Mẫu Change Request và ít nhất 1 bản ghi duyệt thay đổi |
| M-05 | Hình CFG và cách tính V(G) cho `authenticate` (5), `createShowtime` (6), `holdSeats` (6) | Javadoc `AuthService`, `ShowtimeService`, `BookingService:159` | Chỉ có danh sách node dạng chữ; không có hình CFG. `holdSeats` có 5 ca, không phải 6 | Vẽ CFG, liệt kê đường cơ sở; thêm 1 ca cho nhánh `SEAT_NOT_FOUND` hoặc sửa V(G) |
| M-06 | "Kiểm thử Hộp đen bảng quyết định" | `CineHubTestRunner.java` (tên nhóm test) | Code có ca kiểm thử nhưng không có bảng quyết định (điều kiện × hành động) | Bảng quyết định cho giá ghế (3 hạng) và soát vé (4 trạng thái) |
| M-07 | Mức coverage | – | Không có công cụ coverage (JaCoCo…) trong `pom.xml`; không có báo cáo | **Không ghi % coverage**. Nếu cần, chạy JaCoCo và lưu báo cáo |
| M-08 | Kết quả kiểm thử trong TestReport | – | Không có file TestReport. Bằng chứng duy nhất là lần chạy ngày 14/11/2026 (23/23 trên v1.0.0, 32/32 trên v1.1.0, Java 25) | Lưu log console của lần chạy chính thức (ngày, máy, JDK) vào phụ lục TestReport. Nên chạy lại trên JDK 21 như `pom.xml` |
| M-09 | "Chuẩn 3NF" | `schema.sql:5`, CHANGELOG | Có phi chuẩn hóa có chủ đích (F-16) | Phân tích phụ thuộc hàm, hoặc ghi rõ các cột snapshot |
| M-10 | "Hỗ trợ MySQL Server 8.0+" | Javadoc `DatabaseConnection`, `db.properties.example` | Không có driver MySQL trong `pom.xml`; URL hardcode H2 in-memory; chưa từng chạy với MySQL | Hoặc bỏ khẳng định này, hoặc cài đặt đọc `db.properties` và kiểm thử với MySQL |
| M-11 | Thanh toán VietQR / MoMo / ZaloPay / Visa | Nhãn trong `index.html` | Không tích hợp cổng thanh toán nào; `payment_method` chỉ là chuỗi | Ghi trong Scope là **ngoài phạm vi / mô phỏng** |
| M-12 | Vé điện tử "QR" | BR04, README | v1.1.0 chỉ có chuỗi `qr_payload` (F-24). v1.2.0 đã sinh ảnh QR, nhưng chưa kiểm chứng bằng máy quét thật | Ảnh chụp vé có mã QR quét được |
| M-13 | Yêu cầu phi chức năng (hiệu năng, số người dùng đồng thời) | – | Không có số đo nào. Test đồng thời chỉ có 2 luồng | Đo thời gian phản hồi API và kịch bản tải, hoặc không đưa con số vào SRS |
| M-14 | Mật khẩu "kèm salt" | `seed_data.sql:7` | Code không dùng salt (F-11) | Sửa comment hoặc sửa code |
| M-15 | Doanh thu theo ngày/tháng, tỷ lệ lấp đầy, tỷ lệ check-in | – | API không cung cấp (AUDIT §4.1) | Chỉ mô tả trong tài liệu khi đã được triển khai |
| M-16 | Poster phim (`/posters/*.svg`) | `seed_data.sql` | Không có file poster; server trả 404 | Bỏ trường này khỏi mô tả giao diện, hoặc bổ sung ảnh có bản quyền phù hợp |
