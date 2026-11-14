# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users
Designed as a real cinema system used every day (confirmed: "như sản phẩm thật"; the course demo is secondary).

- **Customer (CUSTOMER):** browses showing films and showtimes, picks seats on a live seat map, holds them for 5 minutes, adds F&B combos, pays online and receives e-tickets with QR codes. Often on a phone; also desktop.
- **Counter staff (STAFF):** sells tickets at the POS counter to walk-in customers (cash/card/e-wallet), scans/enters QR codes at the auditorium door, maintains the F&B combo catalog. Desktop at a counter, under time pressure with a queue.
- **Cinema manager (MANAGER):** manages the film catalog, schedules showtimes per auditorium, and reads revenue reports (by channel, seats vs F&B, per film).

Secondary audience: lecturers of course IT3180 (HUST) evaluating the group project, who need to trace screens back to subsystem codes (CSD01–CSD05) and business rules (BR01–BR05, R01).

## Product Purpose
CineHub CMS lets one cinema sell tickets online and at the counter from a single seat inventory, without double-booking a seat, and lets the manager schedule shows and see revenue. Success: a customer completes seat selection to paid QR ticket without confusion; staff complete a POS sale or check-in in seconds; the manager sees accurate numbers.

## Positioning
A single shared seat inventory across online and POS channels with temporary 5-minute seat holds and transactional locking, so two people can never buy the same seat (risk R01). Scheduling enforces a 15-minute cleaning buffer between shows in the same auditorium (BR01).

## Operating Context
- Single-page web app served by an embedded Java HttpServer at `/`; tabs per subsystem, shown or hidden by role; server-side RBAC via bearer session token.
- Three auditoriums (IMAX Laser 6×10, Dolby Atmos 5×10, Gold Class 4×8). Seat tiers: Standard (base price), VIP (+20.000đ), Couple/Sweetbox (×2.2). Seat states: AVAILABLE, HELD (5 min), BOOKED.
- Currency VND; Vietnamese-language UI; age ratings P, T13, T16, T18.
- Quick role-switch chips (Khách hàng / NV POS/QR / Quản lý) exist for testing and demos.

## Capabilities and Constraints
- Stack: plain HTML/CSS/vanilla JS, no build step, served from `src/main/resources`. External scripts/fonts allowed (internet confirmed available); must degrade to system fonts when offline.
- Subsystems: CSD01 online booking, CSD02 POS sales, CSD03 films & showtimes, CSD04 F&B combos, CSD05 QR check-in & revenue report.
- QR codes are currently a textual placeholder; no real QR image is generated.
- Poster URLs exist in data (`/posters/*.svg`) but no poster assets are shipped.

## Brand Commitments
- Name: **CineHub** (CineHub Cinema CMS). No logo asset exists.
- Subsystem and business-rule codes (CSDxx, BRxx, R01) stay visible as small secondary labels so evaluators can trace them; headings speak to users first.

## Evidence on Hand
- Seed data: 6 Vietnamese/international films, 3 auditoriums, 6 combos, sample invoices and tickets (`config/seed_data.sql`, `DatabaseConnection`).
- No posters, photography, testimonials or real customer data; do not fabricate them.

## Product Principles
1. The seat map is the heart of the product: seat state must be unmistakable at a glance.
2. Every role sees only what it can act on; the server is the authority.
3. Money and time are always shown in Vietnamese conventions (1.250.000 ₫, 14/11 09:00).
4. Speed over decoration for staff flows; clarity and confidence for customer checkout.
5. Business-rule codes support traceability without dominating the interface.

## Accessibility & Inclusion
Target WCAG 2.1 AA: keyboard operable seat map, visible focus, seat state not conveyed by color alone, sufficient contrast, reduced-motion respected.
