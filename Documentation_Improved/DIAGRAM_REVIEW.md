# DIAGRAM REVIEW – Sơ đồ, biểu đồ, bảng

## 1. Tình trạng

**Không kiểm tra hay sửa được sơ đồ nào trong 6 tài liệu**, vì không tìm thấy file DOCX nào (xem `AUDIT_REPORT.md` §1). Repo cũng không có file hình ảnh, `.puml`, `.drawio` hay `.mmd` nào.

Thay vào đó, file này cung cấp hai phần:

- **§2**: danh sách các sơ đồ mà mỗi tài liệu cần có, kèm các điểm phải khớp với code. Nhóm dùng như checklist khi tự kiểm tra sơ đồ hiện có.
- **§3**: **sơ đồ tham chiếu dựng từ mã nguồn v1.1.0**, viết bằng Mermaid (xem được trên GitHub và VS Code; xuất PNG/SVG bằng mermaid.live hoặc `mmdc`). Đây là "đáp án" để so với sơ đồ của nhóm, **không phải bản sửa của sơ đồ gốc**. Mọi tên lớp, bảng, trạng thái đều lấy nguyên văn từ code.

## 2. Checklist theo tài liệu

| Tài liệu | Sơ đồ/bảng cần có | Điểm bắt buộc phải khớp với code |
|---|---|---|
| ScopeDocument | Sơ đồ ngữ cảnh, bảng CSD01–CSD05 | 3 actor: Khách hàng (CUSTOMER), Nhân viên (STAFF), Quản lý rạp (MANAGER); thêm "Khách vãng lai" nếu mô tả xem phim không cần đăng nhập. Không có tác nhân "Cổng thanh toán": hệ thống **không tích hợp** VietQR/MoMo/Visa thật, `payment_method` chỉ là nhãn |
| ProjectPlan | Gantt, WBS, sơ đồ tổ chức, timeline Sprint, bảng phân bổ nguồn lực | Tên, MSSV, vai trò phải khớp `README.md` (5 thành viên). Ngày Sprint trong CHANGELOG (30/03, 20/04, 10/05, 25/05/2026) **không có bằng chứng trong git** (chỉ có commit 07/11 và 13/11/2026) – xem MISSING_EVIDENCE M-02 |
| SRS | Use Case Diagram, Activity Diagram cho đặt vé/POS/soát vé | Use case lấy từ `DOCUMENT_TRACEABILITY.md` §2. «include»: *Đặt vé online* include *Giữ ghế*? **Sai với code**, vì checkout không bắt buộc giữ ghế (F-03); nếu SRS yêu cầu bắt buộc thì phải ghi là yêu cầu chưa đạt. *Soát vé* không extend *Hủy vé*. Không có use case "Quản lý phòng chiếu" (chỉ có xem) |
| SDD | Kiến trúc, Package/Component, Class, Sequence, State, ERD, Deployment | Không có Spring/Servlet/Tomcat/JPA/ORM: dùng `com.sun.net.httpserver.HttpServer` + JDBC thuần. CSDL **H2 in-memory** (không phải MySQL đang chạy). **10 bảng**. Null Object là `NullComboOrderItem`. RBAC qua `BaseController` |
| TestPlan | CFG cho `authenticate`, `createShowtime`, `holdSeats`; bảng quyết định giá ghế/soát vé | V(G): `authenticate` = 5 (4 quyết định + 1), `createShowtime` = 6 theo Javadoc; `holdSeats` ghi 6 nhưng mới có 5 ca test |
| TestReport | Bảng kết quả, biểu đồ Pass/Fail | Chỉ được dùng số liệu đã chạy thật: 23/23 (v1.0.0), 32/32 (v1.1.0). **Không có số liệu coverage** |

## 3. Sơ đồ tham chiếu dựng từ code

### 3.1 ERD (10 bảng – `config/schema.sql`)

Quan hệ nét liền là FK có thật trong schema. Bốn quan hệ tới `users` **không có FK** (chỉ là liên kết logic, xem F-15); chúng được ghi bằng nhãn "logical" vì Mermaid không vẽ được nét đứt cho từng quan hệ.

```mermaid
erDiagram
    auditoriums ||--o{ seats : "fk_seats_auditorium"
    auditoriums ||--o{ showtimes : "fk_showtimes_auditorium"
    movies ||--o{ showtimes : "fk_showtimes_movie"
    showtimes ||--o{ showtime_seats : "fk_ss_showtime (CASCADE)"
    seats ||--o{ showtime_seats : "fk_ss_seat"
    showtimes ||--o{ invoices : "fk_invoices_showtime"
    invoices ||--o{ invoice_combos : "fk_ic_invoice (CASCADE)"
    combos ||--o{ invoice_combos : "fk_ic_combo"
    invoices ||--o{ tickets : "fk_tickets_invoice (CASCADE)"
    showtimes ||--o{ tickets : "fk_tickets_showtime"
    showtime_seats ||--o{ tickets : "fk_tickets_ss"
    users |o..o{ invoices : "logical: user_id, staff_id"
    users |o..o{ tickets : "logical: checked_in_by"
    users |o..o{ showtime_seats : "logical: held_by_user_id"

    users {
        int user_id PK
        varchar username UK
        varchar password_hash
        varchar role "CUSTOMER|STAFF|MANAGER"
        varchar status "ACTIVE|LOCKED"
    }
    movies {
        int movie_id PK
        varchar title
        int duration_minutes
        varchar age_rating "P|T13|T16|T18"
        varchar status "SHOWING|COMING_SOON|ENDED"
    }
    auditoriums {
        int auditorium_id PK
        varchar name UK
        int total_rows
        int seats_per_row
        int total_seats
    }
    seats {
        int seat_id PK
        int auditorium_id FK
        varchar seat_code "UK(auditorium_id, seat_code)"
        varchar seat_type "STANDARD|VIP|COUPLE"
    }
    showtimes {
        int showtime_id PK
        int movie_id FK
        int auditorium_id FK
        timestamp start_time
        timestamp end_time "start + duration + 15"
        decimal base_price
        varchar status
    }
    showtime_seats {
        int showtime_seat_id PK
        int showtime_id FK
        int seat_id FK "UK(showtime_id, seat_id)"
        varchar status "AVAILABLE|HELD|BOOKED"
        int held_by_user_id
        timestamp locked_until
        decimal final_price
    }
    combos {
        int combo_id PK
        varchar combo_code UK
        decimal price
        int stock_quantity
        varchar status
    }
    invoices {
        int invoice_id PK
        varchar invoice_code UK
        int user_id
        int staff_id
        int showtime_id FK
        varchar booking_channel "ONLINE|POS"
        decimal seat_total
        decimal combo_total
        decimal total_amount
        varchar payment_status
    }
    invoice_combos {
        int invoice_combo_id PK
        int invoice_id FK
        int combo_id FK
        int quantity
        decimal subtotal
    }
    tickets {
        int ticket_id PK
        varchar ticket_code UK
        varchar qr_payload UK
        int invoice_id FK
        int showtime_seat_id FK
        varchar ticket_status "ISSUED|CHECKED_IN|CANCELLED"
        timestamp checked_in_at
    }
```

### 3.2 Kiến trúc phân tầng / package

```mermaid
flowchart TB
    Browser["Trình duyệt<br/>index.html · style.css · app.js<br/>(fetch + Authorization: Bearer)"]
    subgraph JVM["JVM – CineHubApplication (com.sun.net.httpserver.HttpServer, FixedThreadPool(8))"]
        Static["StaticViewHandler  /"]
        subgraph C["controllers"]
            Base["BaseController<br/>requireLogin / requireRole"]
            AuthC["AuthController /api/auth"]
            MovieC["MovieController /api/movies, /api/auditoriums"]
            ShowC["ShowtimeController /api/showtimes"]
            ComboC["ComboController /api/combos"]
            BookC["BookingController /api/booking"]
            RepC["ReportController /api/reports"]
        end
        subgraph S["services"]
            AuthS[AuthService]; MovieS[MovieService]; AudS[AuditoriumService]; ShowS[ShowtimeService]
            ComboS[ComboService]; BookS[BookingService]; RepS[ReportService]
        end
        subgraph U["utils"]
            DB[DatabaseConnection]; Sess[SessionManager]; Json[JsonUtils]; Pwd[PasswordUtils]
        end
        M["models (POJO) + SeatPricingPolicy"]
    end
    H2[("H2 1.4.200 in-memory<br/>MODE=MySQL · 10 bảng")]
    Browser -->|HTTP JSON| C
    Browser -->|GET /, /css, /js| Static
    AuthC --> AuthS
    MovieC --> MovieS & AudS
    ShowC --> ShowS
    ComboC --> ComboS
    BookC --> BookS
    RepC --> RepS
    C -.-> Sess
    S --> DB --> H2
    S --> M
```

### 3.3 Lớp tính giá (Polymorphism + Null Object)

```mermaid
classDiagram
    class SeatPricingPolicy {
        <<abstract>>
        +double VIP_SURCHARGE$
        +double COUPLE_MULTIPLIER$
        +calculateSeatPrice(basePrice) double*
        +getSeatTierLabel() String*
        +forSeatType(seatType)$ SeatPricingPolicy
    }
    class StandardSeatPricing
    class VipSeatPricing
    class CoupleSeatPricing
    SeatPricingPolicy <|-- StandardSeatPricing
    SeatPricingPolicy <|-- VipSeatPricing
    SeatPricingPolicy <|-- CoupleSeatPricing
    class ComboOrderItem {
        -combo: Combo
        -quantity: int
        +isNull() boolean
        +getSubtotal() double
    }
    class NullComboOrderItem {
        +isNull() boolean
        +getSubtotal() double
    }
    ComboOrderItem <|-- NullComboOrderItem
    ComboOrderItem --> Combo
    BookingService ..> SeatPricingPolicy : forSeatType()
    BookingService ..> ComboOrderItem
    note for ComboOrderItem "Lớp lồng static trong SeatPricingPolicy,\nKHÔNG kế thừa SeatPricingPolicy"
```

### 3.4 Máy trạng thái `showtime_seats.status` (đúng như code đang chạy)

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE : tạo suất chiếu
    AVAILABLE --> HELD : holdSeats() · locked_until = now + 5'
    HELD --> HELD : holdSeats() cùng người dùng (gia hạn)
    HELD --> AVAILABLE : releaseExpiredHolds() khi quá hạn 5 phút
    HELD --> BOOKED : confirmCheckout() bởi người giữ
    AVAILABLE --> BOOKED : confirmCheckout() KHÔNG cần giữ trước (F-03)
    BOOKED --> AVAILABLE : cancelTicket()
```

### 3.5 Máy trạng thái vé (`tickets.ticket_status`)

```mermaid
stateDiagram-v2
    [*] --> ISSUED : confirmCheckout()
    ISSUED --> CHECKED_IN : checkInTicket()
    ISSUED --> CANCELLED : cancelTicket()
    CHECKED_IN --> [*]
    CANCELLED --> [*]
```

Các trạng thái **có trong schema nhưng không có chuyển trạng thái nào trong code**, nên không được vẽ như thể đã hoạt động: `showtimes.status` = `CANCELLED`/`COMPLETED`; `invoices.payment_status` = `PENDING`/`REFUNDED`.

### 3.6 Sequence: giữ ghế rồi thanh toán online

```mermaid
sequenceDiagram
    actor K as Khách hàng
    participant UI as app.js
    participant BC as BookingController
    participant SM as SessionManager
    participant BS as BookingService
    participant DB as H2 (JDBC)
    K->>UI: chọn ghế, bấm "Giữ ghế"
    UI->>BC: POST /api/booking/hold {showtimeId, seatIds} + Bearer
    BC->>SM: getUserByToken(token)
    SM-->>BC: User (hoặc null → 401)
    BC->>BS: holdSeats(showtimeId, seatIds, user.userId)
    activate BS
    Note over BS: synchronized(SEAT_LOCK_MONITOR)
    BS->>DB: releaseExpiredHolds()
    BS->>DB: setAutoCommit(false), SELECT trạng thái từng ghế
    alt ghế BOOKED / HELD bởi người khác
        BS->>DB: rollback
        BS-->>BC: SEAT_ALREADY_TAKEN
    else hợp lệ
        BS->>DB: UPDATE status='HELD', locked_until, commit
        BS-->>BC: HELD_SUCCESS
    end
    deactivate BS
    BC-->>UI: JSON
    K->>UI: điền thông tin, bấm "Thanh toán"
    UI->>BC: POST /api/booking/checkout + Bearer
    BC->>BS: confirmCheckout(..., user.userId, ...)
    activate BS
    Note over BS: synchronized + 1 transaction
    BS->>DB: kiểm tra suất SCHEDULED, ghế, tồn kho combo
    BS->>DB: INSERT invoices, invoice_combos, UPDATE combos.stock
    BS->>DB: UPDATE showtime_seats='BOOKED', INSERT tickets (ISSUED)
    BS->>DB: commit
    deactivate BS
    BC-->>UI: invoice + tickets
```

### 3.7 Deployment

```mermaid
flowchart LR
    subgraph PC["Máy chạy demo (Windows)"]
        subgraph JRE["JRE 21+ (đã chạy thật trên Java 25)"]
            App["CineHubApplication<br/>cổng 8080<br/>classpath: out/ + h2-1.4.200.jar"]
            H2[("H2 in-memory<br/>jdbc:h2:mem:cinehub_db")]
            App --- H2
        end
        Res["src/main/resources/<br/>(đọc từ đĩa theo đường dẫn tương đối)"]
        App --> Res
    end
    Client["Trình duyệt"] -->|HTTP| App
```

## 4. Biểu đồ thống kê

Không có biểu đồ thống kê nào để kiểm tra, cả trong tài liệu lẫn trong giao diện (Dashboard v1.1.0 chỉ có ô KPI và bảng). Các biểu đồ đề xuất cùng điều kiện tiên quyết ở `AUDIT_REPORT.md` §4.3. Không biểu đồ nào được tạo bằng số liệu giả.
