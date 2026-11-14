-- =============================================================================
-- CINEHUB CINEMA MANAGEMENT & TICKET BOOKING SYSTEM (CMS)
-- Học phần: IT3180 - Nhập môn Công nghệ phần mềm (Nhóm 3 - CineHub)
-- Giảng viên hướng dẫn: TS. Nguyễn Nhất Hải
-- Tập tin: config/schema.sql (DDL khởi tạo 10 bảng cơ sở dữ liệu quan hệ)
-- Thiết kế theo 3NF, trừ một số cột lưu "ảnh chụp" (snapshot) có chủ đích để giữ đúng lịch sử giao dịch:
--   tickets.seat_code/seat_type/showtime_id/ticket_price, invoices.combo_summary, showtime_seats.final_price,
--   auditoriums.total_seats (= total_rows x seats_per_row).
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER', -- CUSTOMER, STAFF, MANAGER
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, LOCKED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS movies (
    movie_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    duration_minutes INT NOT NULL,
    age_rating VARCHAR(10) NOT NULL DEFAULT 'P',   -- P, T13, T16, T18
    director VARCHAR(100),
    cast_list VARCHAR(255),
    description VARCHAR(1000),
    poster_url VARCHAR(255),
    release_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'SHOWING'  -- SHOWING, COMING_SOON, ENDED
);

CREATE TABLE IF NOT EXISTS auditoriums (
    auditorium_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    room_type VARCHAR(30) NOT NULL DEFAULT '2D Standard', -- 2D Standard, IMAX Laser, Gold Class
    total_rows INT NOT NULL,
    seats_per_row INT NOT NULL,
    total_seats INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- BR02: Tách biệt ghế vật lý (seats) và trạng thái ghế theo suất chiếu (showtime_seats)
CREATE TABLE IF NOT EXISTS seats (
    seat_id INT AUTO_INCREMENT PRIMARY KEY,
    auditorium_id INT NOT NULL,
    row_label VARCHAR(5) NOT NULL,
    seat_number INT NOT NULL,
    seat_code VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD', -- STANDARD, VIP, COUPLE
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_seats_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums(auditorium_id),
    CONSTRAINT uq_auditorium_seat UNIQUE (auditorium_id, seat_code)
);

-- BR01: Chống chồng lấn lịch chiếu trong cùng phòng chiếu (end_time = start_time + duration + 15 phút vệ sinh)
CREATE TABLE IF NOT EXISTS showtimes (
    showtime_id INT AUTO_INCREMENT PRIMARY KEY,
    movie_id INT NOT NULL,
    auditorium_id INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    base_price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED (CANCELLED, COMPLETED: dự phòng, chưa có chức năng chuyển)
    CONSTRAINT fk_showtimes_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id),
    CONSTRAINT fk_showtimes_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums(auditorium_id)
);

-- BR02 & BR03: Quản lý vòng đời trạng thái ghế (AVAILABLE -> HELD -> BOOKED) và khóa ghế 5 phút
CREATE TABLE IF NOT EXISTS showtime_seats (
    showtime_seat_id INT AUTO_INCREMENT PRIMARY KEY,
    showtime_id INT NOT NULL,
    seat_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, HELD, BOOKED
    held_by_user_id INT NULL,
    locked_until TIMESTAMP NULL,
    final_price DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_ss_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id) ON DELETE CASCADE,
    CONSTRAINT fk_ss_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id),
    CONSTRAINT fk_ss_held_by FOREIGN KEY (held_by_user_id) REFERENCES users(user_id),
    CONSTRAINT uq_showtime_seat UNIQUE (showtime_id, seat_id)
);

-- CSD04: Quản lý Đồ ăn, Thức uống & Combo F&B
CREATE TABLE IF NOT EXISTS combos (
    combo_id INT AUTO_INCREMENT PRIMARY KEY,
    combo_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    items_included VARCHAR(255) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 100,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' -- AVAILABLE, OUT_OF_STOCK, DISCONTINUED
);

-- BR04 & BR05: Hóa đơn giao dịch (Online & Tại quầy POS)
CREATE TABLE IF NOT EXISTS invoices (
    invoice_id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_code VARCHAR(30) NOT NULL UNIQUE,
    user_id INT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20),
    staff_id INT NULL,
    showtime_id INT NOT NULL,
    booking_channel VARCHAR(20) NOT NULL DEFAULT 'ONLINE', -- ONLINE, POS
    payment_method VARCHAR(30) NOT NULL DEFAULT 'VIETQR',  -- VIETQR, CASH, CARD, E_WALLET
    seat_total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    combo_total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    combo_summary VARCHAR(255),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID',    -- PAID, REFUNDED (hủy hết vé); PENDING: dự phòng
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoices_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id),
    CONSTRAINT fk_invoices_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_invoices_staff FOREIGN KEY (staff_id) REFERENCES users(user_id)
);

-- Chi tiết Combo trong Hóa đơn
CREATE TABLE IF NOT EXISTS invoice_combos (
    invoice_combo_id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id INT NOT NULL,
    combo_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_ic_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE CASCADE,
    CONSTRAINT fk_ic_combo FOREIGN KEY (combo_id) REFERENCES combos(combo_id)
);

-- BR04 & CSD05: Vé điện tử có mã QR để soát vé Check-in
CREATE TABLE IF NOT EXISTS tickets (
    ticket_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_code VARCHAR(30) NOT NULL UNIQUE,
    qr_payload VARCHAR(255) NOT NULL UNIQUE,
    invoice_id INT NOT NULL,
    showtime_id INT NOT NULL,
    showtime_seat_id INT NOT NULL,
    seat_code VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    ticket_price DECIMAL(12, 2) NOT NULL,
    ticket_status VARCHAR(20) NOT NULL DEFAULT 'ISSUED', -- ISSUED, CHECKED_IN, CANCELLED
    checked_in_at TIMESTAMP NULL,
    checked_in_by INT NULL,
    CONSTRAINT fk_tickets_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE CASCADE,
    CONSTRAINT fk_tickets_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id),
    CONSTRAINT fk_tickets_ss FOREIGN KEY (showtime_seat_id) REFERENCES showtime_seats(showtime_seat_id),
    CONSTRAINT fk_tickets_checked_by FOREIGN KEY (checked_in_by) REFERENCES users(user_id)
);

