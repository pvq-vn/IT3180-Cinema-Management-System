-- =============================================================================
-- CINEHUB CINEMA MANAGEMENT & TICKET BOOKING SYSTEM (CMS)
-- Học phần: IT3180 - Nhập môn Công nghệ phần mềm (Nhóm 3 - CineHub)
-- Tập tin: config/seed_data.sql (Dữ liệu mẫu khởi tạo hệ thống rạp chiếu phim)
-- =============================================================================

-- 1. Tài khoản người dùng (Mật khẩu đã băm SHA-256 kèm salt 'CINEHUB_SALT_2026')
-- admin / admin123  (Quản lý rạp - Phạm Văn Quyết)
-- staff / staff123  (Nhân viên bán vé & soát vé - Lê Anh Tuấn)
-- fnb   / staff123  (Nhân viên dịch vụ F&B - Nguyễn Xuân Hồng Quân)
-- customer / khach123 (Khách hàng thành viên - Vũ Việt Tiến)
-- locked_user / khach123 (Tài khoản bị khóa để kiểm thử luồng thay thế)

INSERT INTO users (username, password_hash, full_name, email, phone, role, status) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Phạm Văn Quyết (Quản lý Rạp)', 'quyet.pv2416331@sis.hust.edu.vn', '0912345001', 'MANAGER', 'ACTIVE'),
('staff', '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6', 'Lê Anh Tuấn (NV Quầy POS & Soát vé)', 'tuan.la2416639@sis.hust.edu.vn', '0912345002', 'STAFF', 'ACTIVE'),
('fnb_staff', '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6', 'Nguyễn Xuân Hồng Quân (NV Quầy F&B)', 'quan.nxh2416321@sis.hust.edu.vn', '0912345003', 'STAFF', 'ACTIVE'),
('customer', '79c4de8c8577ebfbebe7ecf1951d0b51e66f80cf7b7255d20798917f4886de87', 'Vũ Việt Tiến (Khách hàng VIP)', 'tien.vv2416362@sis.hust.edu.vn', '0987654321', 'CUSTOMER', 'ACTIVE'),
('bach_hs', '79c4de8c8577ebfbebe7ecf1951d0b51e66f80cf7b7255d20798917f4886de87', 'Hà Sỹ Bách (Khách hàng thành viên)', 'bach.hs2416129@sis.hust.edu.vn', '0987654322', 'CUSTOMER', 'ACTIVE'),
('locked_user', '79c4de8c8577ebfbebe7ecf1951d0b51e66f80cf7b7255d20798917f4886de87', 'Trần Văn Khóa (Tài khoản bị khóa)', 'locked@cinehub.vn', '0900000000', 'CUSTOMER', 'LOCKED');

-- 2. Danh mục Phim chiếu rạp
INSERT INTO movies (title, genre, duration_minutes, age_rating, director, cast_list, description, poster_url, release_date, status) VALUES
('Địa Đạo: Mặt Trời Trong Bóng Tối', 'Lịch sử / Hành động', 128, 'T16', 'Bùi Thạc Chuyên', 'Thái Hòa, Quang Tuấn, Hồ Thu Anh', 'Bộ phim tái hiện cuộc chiến đấu kiên cường của đội du kích Củ Chi trong lòng đất năm 1967 với những thước phim chân thực và xúc động.', '/posters/diadao.svg', '2026-04-30', 'SHOWING'),
('Lật Mặt 8: Vòng Tay Nắng', 'Gia đình / Tâm lý / Hành động', 135, 'T13', 'Lý Hải', 'NSƯT Kim Phương, Long Đẹp Trai, Quách Ngọc Tuyên', 'Hành trình theo đuổi ước mơ âm nhạc của nhóm bạn trẻ miền Trung đầy nắng gió và tình cảm gia đình thiêng liêng.', '/posters/latmat8.svg', '2026-05-01', 'SHOWING'),
('Thám Tử Kiên: Kỳ Án Không Đầu', 'Trinh thám / Cổ trang', 122, 'T16', 'Victor Vũ', 'Quốc Huy, Đinh Ngọc Diệp, Quốc Anh', 'Thám tử Kiên điều tra chuỗi vụ án bí ẩn tại vùng sơn cước phía Bắc với những nút thắt nghẹt thở.', '/posters/thamtukien.svg', '2026-05-05', 'SHOWING'),
('Doraemon: Nobita Và Cuộc Phiêu Lưu Vào Thế Giới Trong Tranh', 'Hoạt hình / Phiêu lưu', 105, 'P', 'Teramoto Yukiyo', 'Wasabi Mizuta, Megumi Oohara', 'Nhóm bạn Doraemon bước vào thế giới hội họa thời Trung cổ châu Âu để giải cứu vương quốc Artoria.', '/posters/doraemon.svg', '2026-05-15', 'SHOWING'),
('Mission: Impossible - Nghiệp Báo Cuối Cùng', 'Hành động / Điệp viên', 163, 'T16', 'Christopher McQuarrie', 'Tom Cruise, Hayley Atwell, Ving Rhames', 'Ethan Hunt đối mặt với thực thể trí tuệ nhân tạo The Entity trong nhiệm vụ sinh tử giải cứu thế giới.', '/posters/mi8.svg', '2026-05-22', 'SHOWING'),
('Biệt Đội Sấm Sét (Thunderbolts*)', 'Hành động / Siêu anh hùng', 126, 'T13', 'Jake Schreier', 'Florence Pugh, Sebastian Stan, David Harbour', 'Nhóm phản anh hùng bất đắc dĩ của vũ trụ Marvel hợp lực thực hiện điệp vụ tối mật đầy nguy hiểm.', '/posters/thunderbolts.svg', '2026-06-01', 'COMING_SOON');

-- 3. Danh mục Phòng chiếu (Auditoriums)
INSERT INTO auditoriums (name, room_type, total_rows, seats_per_row, total_seats, status) VALUES
('Phòng 01 - IMAX Laser', 'IMAX Laser', 6, 10, 60, 'ACTIVE'),
('Phòng 02 - Dolby Atmos', '2D Dolby Atmos', 5, 10, 50, 'ACTIVE'),
('Phòng 03 - Gold Class VIP', 'Gold Class', 4, 8, 32, 'ACTIVE');

-- 4. Danh mục Đồ ăn, Thức uống & Combo F&B (CSD04)
INSERT INTO combos (combo_code, name, description, items_included, price, stock_quantity, status) VALUES
('CB01', 'Solo Bắp Ngọt & Pepsi', 'Combo tiết kiệm dành cho 1 người xem phim', '1 Bắp rang bơ vị Ngọt (Size L) + 1 Nước ngọt Pepsi (Size L)', 79000, 150, 'AVAILABLE'),
('CB02', 'Couple Caramel Ngọt Ngào', 'Combo đôi bán chạy nhất cho các cặp đôi', '1 Bắp rang bơ Phô mai/Caramel (Size XL) + 2 Nước ngọt Pepsi/Mirinda (Size L)', 115000, 120, 'AVAILABLE'),
('CB03', 'Family Party 4 Người', 'Combo gia đình hoặc nhóm bạn 4 người tiết kiệm 25%', '2 Bắp rang bơ Mix 2 vị (Size XL) + 4 Nước ngọt tự chọn (Size L) + 1 Snack Khoai tây', 219000, 80, 'AVAILABLE'),
('CB04', 'CineHub Collector Ly Đổi Màu', 'Combo phiên bản giới hạn kèm Ly nhân vật điện ảnh', '1 Ly nhân vật giới hạn 32oz + 1 Bắp Phô mai (Size L) + 1 Nước ngọt (Size L)', 169000, 45, 'AVAILABLE'),
('CB05', 'Hotdog & Trà Đào Cam Sả', 'Bữa ăn nhẹ nóng hổi kèm trà trái cây tươi mát', '1 Bánh mì xúc xích Đức phô mai + 1 Trà đào cam sả (Size L)', 89000, 60, 'AVAILABLE'),
('CB06', 'Combo Bắp Cay Tứ Xuyên (Ngừng KD)', 'Sản phẩm thử nghiệm mùa lễ hội đã có đơn hàng lịch sử', '1 Bắp vị Cay Tứ Xuyên + 1 Coca Zero', 85000, 0, 'DISCONTINUED');
