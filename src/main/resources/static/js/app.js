/**
 * CINEHUB CMS - TẦNG XỬ LÝ TƯƠNG TÁC GIAO DIỆN NGƯỜI DÙNG (static/js/app.js)
 * Học phần: IT3180 - Nhập môn Công nghệ Phần mềm (Nhóm 3 - HUST)
 * Phụ trách Frontend & UI/UX: Lê Anh Tuấn (202416639)
 */

(function () {
    "use strict";

    // Trạng thái ứng dụng phía Client
    const state = {
        currentUser: {
            userId: 1,
            username: "admin",
            fullName: "Phạm Văn Quyết (Quản lý Rạp)",
            phone: "0912345001",
            role: "MANAGER"
        },
        movies: [],
        auditoriums: [],
        showtimes: [],
        combos: [],
        selectedMovieId: 1,
        selectedShowtimeId: 1,
        seatMap: [],
        selectedSeatIds: [],
        comboQuantities: {},
        holdCountdownTimer: null,
        holdRemainingSeconds: 0
    };

    const vndFormatter = new Intl.NumberFormat("vi-VN");

    function formatVnd(amount) {
        return vndFormatter.format(Math.round(amount || 0)) + " ₫";
    }

    function showBanner(message, type) {
        const banner = document.getElementById("global-banner");
        if (!banner) return;
        banner.textContent = message;
        banner.className = "global-banner " + (type || "info");
        banner.hidden = false;
        clearTimeout(banner._hideTimer);
        banner._hideTimer = setTimeout(() => {
            banner.hidden = true;
        }, 6500);
    }

    // =========================================================================
    // 1. MODERN WEB GUIDANCE: VALIDATE INPUT AFTER INTERACTION & NATIVE DIALOG
    // =========================================================================
    function initFormValidation() {
        document.querySelectorAll("form.validated-form").forEach((form) => {
            const syncValidity = (input, isInvalid) => {
                input.classList.toggle("user-invalid-fallback", isInvalid);
                input.setAttribute("aria-invalid", String(isInvalid));
                const err = input.parentElement ? input.parentElement.querySelector(".error-msg") : null;
                if (err && err.id) {
                    const ids = new Set((input.getAttribute("aria-describedby") || "").split(/\s+/).filter(Boolean));
                    if (isInvalid) {
                        ids.add(err.id);
                    } else {
                        ids.delete(err.id);
                    }
                    if (ids.size) {
                        input.setAttribute("aria-describedby", [...ids].join(" "));
                    } else {
                        input.removeAttribute("aria-describedby");
                    }
                }
            };

            form.querySelectorAll("input, select, textarea").forEach((input) => {
                input.addEventListener("blur", () => {
                    if (input.value) {
                        syncValidity(input, !input.checkValidity());
                    }
                });
                input.addEventListener("input", () => {
                    if (input.getAttribute("aria-invalid") === "true") {
                        syncValidity(input, !input.checkValidity());
                    }
                });
                input.addEventListener("invalid", () => {
                    syncValidity(input, true);
                });
            });
        });
    }

    function initDialogs() {
        const ticketModal = document.getElementById("ticket-modal");
        const loginModal = document.getElementById("login-modal");

        if (ticketModal) {
            ticketModal.addEventListener("close", () => {
                ticketModal.returnValue = "";
            });
            document.getElementById("btn-close-ticket-modal").addEventListener("click", () => ticketModal.close());
            document.getElementById("btn-done-ticket-modal").addEventListener("click", () => ticketModal.close());
        }

        if (loginModal) {
            loginModal.addEventListener("close", () => {
                loginModal.returnValue = "";
            });
            document.getElementById("btn-open-login").addEventListener("click", () => loginModal.showModal());
            document.getElementById("btn-close-login-modal").addEventListener("click", () => loginModal.close());
            document.getElementById("btn-cancel-login-modal").addEventListener("click", () => loginModal.close());
        }
    }

    function openTicketModal(invoice, tickets) {
        const modal = document.getElementById("ticket-modal");
        const container = document.getElementById("ticket-modal-content");
        if (!modal || !container) return;

        let html = `
            <div class="summary-block">
                <div class="summary-line"><span>Mã Hóa Đơn:</span><strong>${invoice.invoiceCode}</strong></div>
                <div class="summary-line"><span>Khách hàng:</span><strong>${invoice.customerName} (${invoice.customerPhone})</strong></div>
                <div class="summary-line"><span>Bộ phim:</span><strong>${invoice.movieTitle}</strong></div>
                <div class="summary-line"><span>Phòng &amp; Suất chiếu:</span><strong>${invoice.auditoriumName} | ${invoice.startTime}</strong></div>
                <div class="summary-line"><span>Combo F&amp;B:</span><span>${invoice.comboSummary}</span></div>
                <div class="summary-line grand-total"><span>Tổng đã thanh toán (${invoice.paymentMethod}):</span><strong>${formatVnd(invoice.totalAmount)}</strong></div>
            </div>
            <h4>Danh sách Vé điện tử QR đã phát hành (${tickets.length} vé):</h4>
        `;

        tickets.forEach((t) => {
            html += `
                <div class="qr-ticket-card">
                    <div>
                        <strong>Mã vé: ${t.ticketCode} — Ghế ${t.seatCode} (${t.seatType})</strong>
                        <p class="field-hint">${t.movieTitle} | ${t.auditoriumName} | ${t.startTime}</p>
                        <p class="field-hint">Giá vé: ${formatVnd(t.ticketPrice)} | Trạng thái: ${t.ticketStatus}</p>
                    </div>
                    <div class="qr-visual-box">
                        [QR CODE]<br>${t.ticketCode}
                    </div>
                </div>
            `;
        });

        container.innerHTML = html;
        modal.showModal();
    }

    // =========================================================================
    // 2. ĐIỀU HƯỚNG TAB & ĐĂNG NHẬP CHUYỂN VAI TRÒ (RBAC)
    // =========================================================================
    function initNavigationAndAuth() {
        document.querySelectorAll(".nav-tab").forEach((btn) => {
            btn.addEventListener("click", () => {
                document.querySelectorAll(".nav-tab").forEach((b) => b.classList.remove("active"));
                document.querySelectorAll(".tab-panel").forEach((p) => p.classList.remove("active"));
                btn.classList.add("active");
                const target = document.getElementById(btn.dataset.tab);
                if (target) target.classList.add("active");

                if (btn.dataset.tab === "tab-pos") {
                    populatePosForm();
                } else if (btn.dataset.tab === "tab-checkin") {
                    loadDashboardAndTickets();
                }
            });
        });

        document.querySelectorAll("[data-quick-login]").forEach((chip) => {
            chip.addEventListener("click", async () => {
                const [username, password] = chip.dataset.quickLogin.split(":");
                await performLogin(username, password);
                document.querySelectorAll("[data-quick-login]").forEach((c) => c.classList.remove("active"));
                chip.classList.add("active");
            });
        });

        const loginForm = document.getElementById("login-dialog-form");
        if (loginForm) {
            loginForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                if (!loginForm.checkValidity()) return;
                const u = document.getElementById("login-username-input").value;
                const p = document.getElementById("login-password-input").value;
                const ok = await performLogin(u, p);
                if (ok) {
                    document.getElementById("login-modal").close();
                }
            });
        }
    }

    async function performLogin(username, password) {
        try {
            const resp = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password })
            });
            const data = await resp.json();
            if (!data.success) {
                showBanner(data.message, "error");
                return false;
            }
            state.currentUser = data.user;
            document.getElementById("current-role-pill").textContent = data.user.role;
            document.getElementById("current-user-name").textContent = data.user.fullName;
            document.getElementById("cust-name-input").value = data.user.fullName;
            document.getElementById("cust-phone-input").value = data.user.phone || "0987654321";
            showBanner(data.message, "success");
            await loadSeatMap(state.selectedShowtimeId);
            return true;
        } catch (err) {
            showBanner("Lỗi kết nối máy chủ: " + err.message, "error");
            return false;
        }
    }

    // =========================================================================
    // 3. TẢI DỮ LIỆU PHIM, PHÒNG CHIẾU, SUẤT CHIẾU & SƠ ĐỒ GHẾ (CSD01 & CSD03)
    // =========================================================================
    async function loadInitialData() {
        await Promise.all([
            loadMovies("", "SHOWING"),
            loadAuditoriums(),
            loadShowtimes(),
            loadCombos()
        ]);
        if (state.showtimes.length > 0) {
            state.selectedShowtimeId = state.showtimes[0].showtimeId;
            state.selectedMovieId = state.showtimes[0].movieId;
            renderMovieCards();
            renderShowtimePills();
            await loadSeatMap(state.selectedShowtimeId);
        }
        await loadDashboardAndTickets();
    }

    async function loadMovies(keyword, status) {
        const q = new URLSearchParams({ keyword: keyword || "", status: status || "ALL" });
        const resp = await fetch("/api/movies?" + q.toString());
        state.movies = await resp.json();
        renderMovieCards();
        populateScheduleSelects();
    }

    async function loadAuditoriums() {
        const resp = await fetch("/api/auditoriums");
        state.auditoriums = await resp.json();
        populateScheduleSelects();
    }

    async function loadShowtimes() {
        const resp = await fetch("/api/showtimes");
        state.showtimes = await resp.json();
        renderShowtimePills();
        renderShowtimesTable();
        populatePosForm();
    }

    async function loadCombos() {
        const resp = await fetch("/api/combos");
        state.combos = await resp.json();
        renderBookingCombos();
        renderCombosTable();
        populatePosForm();
    }

    function renderMovieCards() {
        const grid = document.getElementById("movie-cards-grid");
        if (!grid) return;
        if (state.movies.length === 0) {
            grid.innerHTML = `<p class="muted-text">Không tìm thấy bộ phim nào khớp điều kiện lọc.</p>`;
            return;
        }
        grid.innerHTML = state.movies.map((m) => {
            const isSelected = m.movieId === state.selectedMovieId;
            return `
                <article class="movie-card ${isSelected ? "active" : ""}" data-movie-id="${m.movieId}">
                    <div>
                        <div class="movie-card-top">
                            <span class="age-badge ${m.ageRating}">${m.ageRating}</span>
                            <span class="movie-meta">${m.durationMinutes} phút</span>
                        </div>
                        <h3>${m.title}</h3>
                        <p class="movie-meta">${m.genre}</p>
                    </div>
                    <div class="movie-meta">
                        Đạo diễn: ${m.director || "Đang cập nhật"}
                    </div>
                </article>
            `;
        }).join("");

        grid.querySelectorAll(".movie-card").forEach((card) => {
            card.addEventListener("click", async () => {
                state.selectedMovieId = parseInt(card.dataset.movieId, 10);
                renderMovieCards();
                const movieShowtimes = state.showtimes.filter((s) => s.movieId === state.selectedMovieId);
                if (movieShowtimes.length > 0) {
                    state.selectedShowtimeId = movieShowtimes[0].showtimeId;
                    renderShowtimePills();
                    await loadSeatMap(state.selectedShowtimeId);
                } else {
                    renderShowtimePills();
                    document.getElementById("seat-grid-container").innerHTML =
                        `<p class="muted-text">Bộ phim này chưa có suất chiếu nào được mở bán.</p>`;
                }
            });
        });
    }

    function renderShowtimePills() {
        const container = document.getElementById("showtime-pills-container");
        if (!container) return;
        const filtered = state.showtimes.filter((s) => !state.selectedMovieId || s.movieId === state.selectedMovieId);
        const list = filtered.length > 0 ? filtered : state.showtimes;

        container.innerHTML = list.map((s) => {
            const active = s.showtimeId === state.selectedShowtimeId;
            const timePart = s.startTime ? s.startTime.substring(11, 16) : "";
            const datePart = s.startTime ? s.startTime.substring(0, 10) : "";
            return `
                <button type="button" class="showtime-pill ${active ? "active" : ""}" data-st-id="${s.showtimeId}">
                    <strong>${timePart} (${datePart})</strong>
                    <span>${s.auditoriumName} | Trống ${s.availableSeats}/${s.totalSeats} ghế</span>
                </button>
            `;
        }).join("");

        container.querySelectorAll(".showtime-pill").forEach((pill) => {
            pill.addEventListener("click", async () => {
                state.selectedShowtimeId = parseInt(pill.dataset.stId, 10);
                state.selectedSeatIds = [];
                renderShowtimePills();
                await loadSeatMap(state.selectedShowtimeId);
            });
        });
    }

    async function loadSeatMap(showtimeId) {
        if (!showtimeId) return;
        const resp = await fetch("/api/booking/seats?showtimeId=" + showtimeId);
        state.seatMap = await resp.json();

        const st = state.showtimes.find((s) => s.showtimeId === showtimeId);
        if (st) {
            document.getElementById("selected-movie-heading").textContent =
                `${st.movieTitle} — ${st.auditoriumName}`;
            document.getElementById("selected-showtime-meta").textContent =
                `Giờ chiếu: ${st.startTime} đến ${st.endTime} (đã cộng 15p vệ sinh) | Giá gốc: ${formatVnd(st.basePrice)}`;
            document.getElementById("cart-showtime-label").textContent =
                `${st.movieTitle} (${st.startTime.substring(11, 16)} - ${st.auditoriumName})`;
        }

        renderSeatGrid();
        updateCartTotals();
    }

    function renderSeatGrid() {
        const container = document.getElementById("seat-grid-container");
        if (!container) return;

        const rowsMap = {};
        state.seatMap.forEach((seat) => {
            if (!rowsMap[seat.rowLabel]) {
                rowsMap[seat.rowLabel] = [];
            }
            rowsMap[seat.rowLabel].push(seat);
        });

        container.innerHTML = Object.keys(rowsMap).sort().map((rowLabel) => {
            const seatsHtml = rowsMap[rowLabel].map((s) => {
                const isSelected = state.selectedSeatIds.includes(s.showtimeSeatId);
                const stateClass = "state-" + s.status;
                const tierClass = "tier-" + s.seatType;
                const disabled = s.status === "BOOKED" ||
                    (s.status === "HELD" && s.heldByUserId && s.heldByUserId !== state.currentUser.userId);
                const title = `Ghế ${s.seatCode} (${s.seatType}) - ${formatVnd(s.finalPrice)} - Trạng thái: ${s.status}`;
                return `
                    <button type="button"
                            class="seat-btn ${tierClass} ${stateClass} ${isSelected ? "selected" : ""}"
                            data-ss-id="${s.showtimeSeatId}"
                            title="${title}"
                            ${disabled ? "disabled" : ""}>
                        ${s.seatCode}
                    </button>
                `;
            }).join("");

            return `
                <div class="seat-row">
                    <span class="row-label-tag">${rowLabel}</span>
                    ${seatsHtml}
                    <span class="row-label-tag">${rowLabel}</span>
                </div>
            `;
        }).join("");

        container.querySelectorAll(".seat-btn:not([disabled])").forEach((btn) => {
            btn.addEventListener("click", () => {
                const ssId = parseInt(btn.dataset.ssId, 10);
                const idx = state.selectedSeatIds.indexOf(ssId);
                if (idx >= 0) {
                    state.selectedSeatIds.splice(idx, 1);
                } else {
                    if (state.selectedSeatIds.length >= 8) {
                        showBanner("Mỗi giao dịch được chọn tối đa 8 ghế.", "error");
                        return;
                    }
                    state.selectedSeatIds.push(ssId);
                }
                renderSeatGrid();
                updateCartTotals();
            });
        });
    }

    function renderBookingCombos() {
        const listEl = document.getElementById("booking-combo-list");
        if (!listEl) return;
        const activeCombos = state.combos.filter((c) => c.status === "AVAILABLE" && c.stockQuantity > 0);
        listEl.innerHTML = activeCombos.map((c) => {
            const qty = state.comboQuantities[c.comboId] || 0;
            return `
                <div class="combo-pick-item">
                    <div class="combo-pick-info">
                        <strong>${c.comboCode} - ${c.name} (${formatVnd(c.price)})</strong>
                        <span>${c.itemsIncluded} | Tồn: ${c.stockQuantity}</span>
                    </div>
                    <input type="number" class="combo-qty-input" min="0" max="10"
                           value="${qty}" data-combo-id="${c.comboId}"
                           aria-label="Số lượng ${c.name}">
                </div>
            `;
        }).join("");

        listEl.querySelectorAll(".combo-qty-input").forEach((inp) => {
            inp.addEventListener("input", () => {
                const cId = parseInt(inp.dataset.comboId, 10);
                state.comboQuantities[cId] = Math.max(0, parseInt(inp.value || "0", 10));
                updateCartTotals();
            });
        });
    }

    function updateCartTotals() {
        const selectedObjects = state.seatMap.filter((s) => state.selectedSeatIds.includes(s.showtimeSeatId));
        const seatCodes = selectedObjects.map((s) => `${s.seatCode} (${s.seatType})`).join(", ");
        const seatSubtotal = selectedObjects.reduce((acc, s) => acc + s.finalPrice, 0);

        let comboSubtotal = 0;
        state.combos.forEach((c) => {
            const qty = state.comboQuantities[c.comboId] || 0;
            if (qty > 0) {
                comboSubtotal += c.price * qty;
            }
        });

        document.getElementById("cart-seat-count").textContent = String(selectedObjects.length);
        document.getElementById("cart-seat-codes").textContent = seatCodes || "Chưa chọn ghế";
        document.getElementById("cart-seat-subtotal").textContent = formatVnd(seatSubtotal);
        document.getElementById("final-seat-total").textContent = formatVnd(seatSubtotal);
        document.getElementById("final-combo-total").textContent = formatVnd(comboSubtotal);
        document.getElementById("final-grand-total").textContent = formatVnd(seatSubtotal + comboSubtotal);
    }

    function startHoldCountdown(seconds) {
        clearInterval(state.holdCountdownTimer);
        state.holdRemainingSeconds = seconds;
        const badge = document.getElementById("hold-timer-badge");
        const text = document.getElementById("hold-countdown-text");
        badge.hidden = false;

        const tick = () => {
            const mins = String(Math.floor(state.holdRemainingSeconds / 60)).padStart(2, "0");
            const secs = String(state.holdRemainingSeconds % 60).padStart(2, "0");
            text.textContent = `${mins}:${secs}`;
            if (state.holdRemainingSeconds <= 0) {
                clearInterval(state.holdCountdownTimer);
                badge.hidden = true;
                loadSeatMap(state.selectedShowtimeId);
            }
            state.holdRemainingSeconds--;
        };
        tick();
        state.holdCountdownTimer = setInterval(tick, 1000);
    }

    // =========================================================================
    // 4. XỬ LÝ SỰ KIỆN ĐẶT VÉ, POS, LẬP LỊCH CHIẾU, COMBO & SOÁT VÉ QR
    // =========================================================================
    function initEventHandlers() {
        // Tìm kiếm phim
        document.getElementById("movie-search-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const kw = document.getElementById("search-movie-kw").value;
            const st = document.getElementById("filter-movie-status").value;
            await loadMovies(kw, st);
        });

        // Giữ ghế tạm thời 5 phút (BR03)
        document.getElementById("btn-hold-seats").addEventListener("click", async () => {
            if (state.selectedSeatIds.length === 0) {
                showBanner("Vui lòng chọn ít nhất 1 ghế trên sơ đồ để giữ chỗ tạm thời.", "error");
                return;
            }
            const resp = await fetch("/api/booking/hold", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    showtimeId: state.selectedShowtimeId,
                    userId: state.currentUser.userId,
                    seatIds: state.selectedSeatIds.join(",")
                })
            });
            const data = await resp.json();
            if (!data.success) {
                showBanner(data.message, "error");
                await loadSeatMap(state.selectedShowtimeId);
                return;
            }
            showBanner(data.message, "success");
            startHoldCountdown(300);
            await loadSeatMap(state.selectedShowtimeId);
        });

        // Thanh toán đặt vé Online (CSD01 - BR04 & BR05)
        const onlineForm = document.getElementById("online-checkout-form");
        onlineForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!onlineForm.checkValidity()) return;
            if (state.selectedSeatIds.length === 0) {
                showBanner("Vui lòng chọn ít nhất 1 ghế trên sơ đồ trước khi thanh toán!", "error");
                return;
            }

            const comboPairs = Object.entries(state.comboQuantities)
                .filter(([, q]) => q > 0)
                .map(([id, q]) => `${id}:${q}`)
                .join(",");

            const resp = await fetch("/api/booking/checkout", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    showtimeId: state.selectedShowtimeId,
                    userId: state.currentUser.userId,
                    customerName: document.getElementById("cust-name-input").value,
                    customerPhone: document.getElementById("cust-phone-input").value,
                    bookingChannel: "ONLINE",
                    paymentMethod: document.getElementById("online-payment-method").value,
                    seatIds: state.selectedSeatIds.join(","),
                    comboSpec: comboPairs
                })
            });
            const data = await resp.json();
            if (!data.success) {
                showBanner(data.message, "error");
                return;
            }
            showBanner(data.message, "success");
            state.selectedSeatIds = [];
            state.comboQuantities = {};
            clearInterval(state.holdCountdownTimer);
            document.getElementById("hold-timer-badge").hidden = true;
            openTicketModal(data.invoice, data.tickets);
            await Promise.all([loadShowtimes(), loadCombos(), loadSeatMap(state.selectedShowtimeId), loadDashboardAndTickets()]);
        });

        // Bán vé trực tiếp tại quầy POS (CSD02)
        const posShowtimeSelect = document.getElementById("pos-showtime-select");
        posShowtimeSelect.addEventListener("change", async () => {
            await populatePosSeats(parseInt(posShowtimeSelect.value, 10));
        });

        const posForm = document.getElementById("pos-checkout-form");
        posForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const stId = parseInt(posShowtimeSelect.value, 10);
            const seatSelect = document.getElementById("pos-seat-select");
            const chosenSeats = Array.from(seatSelect.selectedOptions).map((o) => o.value);
            if (chosenSeats.length === 0) {
                showBanner("Nhân viên POS vui lòng chọn ít nhất 1 ghế trống!", "error");
                return;
            }
            const comboId = document.getElementById("pos-combo-select").value;
            const comboQty = parseInt(document.getElementById("pos-combo-qty").value || "0", 10);
            const comboSpec = (comboId && comboQty > 0) ? `${comboId}:${comboQty}` : "";

            const resp = await fetch("/api/booking/checkout", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    showtimeId: stId,
                    userId: state.currentUser.userId,
                    customerName: document.getElementById("pos-customer-name").value,
                    customerPhone: "0900112233",
                    bookingChannel: "POS",
                    paymentMethod: document.getElementById("pos-payment-method").value,
                    seatIds: chosenSeats.join(","),
                    comboSpec: comboSpec
                })
            });
            const data = await resp.json();
            if (!data.success) {
                showBanner(data.message, "error");
                return;
            }
            showBanner(data.message, "success");
            const inv = data.invoice;
            document.getElementById("pos-latest-receipt").innerHTML = `
                <div class="summary-block">
                    <div class="summary-line"><span>Hóa đơn POS:</span><strong>${inv.invoiceCode}</strong></div>
                    <div class="summary-line"><span>Khách hàng:</span><strong>${inv.customerName}</strong></div>
                    <div class="summary-line"><span>Phim &amp; Phòng:</span><strong>${inv.movieTitle} (${inv.auditoriumName})</strong></div>
                    <div class="summary-line"><span>Combo kèm:</span><span>${inv.comboSummary}</span></div>
                    <div class="summary-line grand-total"><span>Tổng thu (${inv.paymentMethod}):</span><strong>${formatVnd(inv.totalAmount)}</strong></div>
                </div>
                <p class="field-hint">Mã vé QR đã in: ${data.tickets.map((t) => t.ticketCode).join(", ")}</p>
            `;
            openTicketModal(data.invoice, data.tickets);
            await Promise.all([loadShowtimes(), loadCombos(), populatePosSeats(stId), loadDashboardAndTickets()]);
        });

        // Tạo suất chiếu mới & Kiểm tra chống trùng lịch BR01 (CSD03)
        const createShowtimeForm = document.getElementById("create-showtime-form");
        createShowtimeForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!createShowtimeForm.checkValidity()) return;
            await submitShowtimeCreation(
                document.getElementById("sched-movie-id").value,
                document.getElementById("sched-auditorium-id").value,
                document.getElementById("sched-start-time").value,
                document.getElementById("sched-base-price").value
            );
        });

        document.getElementById("btn-test-br01-conflict").addEventListener("click", async () => {
            if (state.showtimes.length === 0) return;
            const firstSt = state.showtimes[0];
            document.getElementById("sched-movie-id").value = String(firstSt.movieId);
            document.getElementById("sched-auditorium-id").value = String(firstSt.auditoriumId);
            document.getElementById("sched-start-time").value = firstSt.startTime;
            await submitShowtimeCreation(firstSt.movieId, firstSt.auditoriumId, firstSt.startTime, 95000);
        });

        // Lưu Phim mới (CSD03)
        const saveMovieForm = document.getElementById("save-movie-form");
        saveMovieForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!saveMovieForm.checkValidity()) return;
            const resp = await fetch("/api/movies", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    action: "save",
                    title: document.getElementById("movie-title-input").value,
                    genre: document.getElementById("movie-genre-input").value,
                    durationMinutes: document.getElementById("movie-duration-input").value,
                    ageRating: document.getElementById("movie-rating-input").value,
                    status: document.getElementById("movie-status-input").value,
                    director: document.getElementById("movie-director-input").value,
                    castList: document.getElementById("movie-cast-input").value
                })
            });
            const data = await resp.json();
            showBanner(data.message, data.success ? "success" : "error");
            if (data.success) {
                document.getElementById("movie-title-input").value = "";
                await loadMovies("", "ALL");
            }
        });

        // Lưu Combo F&B (CSD04)
        const saveComboForm = document.getElementById("save-combo-form");
        saveComboForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!saveComboForm.checkValidity()) return;
            const resp = await fetch("/api/combos", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    action: "save",
                    comboId: document.getElementById("combo-id-input").value,
                    comboCode: document.getElementById("combo-code-input").value,
                    name: document.getElementById("combo-name-input").value,
                    itemsIncluded: document.getElementById("combo-items-input").value,
                    price: document.getElementById("combo-price-input").value,
                    stockQuantity: document.getElementById("combo-stock-input").value
                })
            });
            const data = await resp.json();
            showBanner(data.message, data.success ? "success" : "error");
            if (data.success) {
                resetComboForm();
                await loadCombos();
            }
        });

        document.getElementById("btn-reset-combo-form").addEventListener("click", resetComboForm);

        // Soát vé điện tử QR Check-in (CSD05)
        const qrForm = document.getElementById("qr-checkin-form");
        qrForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const code = document.getElementById("qr-code-input").value;
            await performQrCheckin(code);
        });

        document.querySelectorAll("[data-sample-qr]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const sample = btn.dataset.sampleQr;
                document.getElementById("qr-code-input").value = sample;
                await performQrCheckin(sample);
            });
        });
    }

    async function submitShowtimeCreation(movieId, auditoriumId, startTime, basePrice) {
        const resp = await fetch("/api/showtimes", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ movieId, auditoriumId, startTime, basePrice })
        });
        const data = await resp.json();
        showBanner(data.message, data.success ? "success" : "error");
        if (data.success) {
            await loadShowtimes();
        }
    }

    async function performQrCheckin(code) {
        const resp = await fetch("/api/booking/checkin", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ code, staffId: state.currentUser.userId })
        });
        const data = await resp.json();
        const box = document.getElementById("checkin-result-box");
        box.className = "checkin-result-box " + (data.success ? "valid" : "invalid");
        box.innerHTML = `<strong>[${data.code}]</strong> ${data.message}`;
        showBanner(data.message, data.success ? "success" : "error");
        await loadDashboardAndTickets();
    }

    function populateScheduleSelects() {
        const movieSel = document.getElementById("sched-movie-id");
        const audSel = document.getElementById("sched-auditorium-id");
        if (movieSel && state.movies.length > 0) {
            movieSel.innerHTML = state.movies.map((m) =>
                `<option value="${m.movieId}">${m.title} (${m.durationMinutes} phút - ${m.status})</option>`
            ).join("");
        }
        if (audSel && state.auditoriums.length > 0) {
            audSel.innerHTML = state.auditoriums.map((a) =>
                `<option value="${a.auditoriumId}">${a.name} (${a.totalSeats} ghế - ${a.roomType})</option>`
            ).join("");
        }
        const timeInput = document.getElementById("sched-start-time");
        if (timeInput && !timeInput.value) {
            const tomorrow = new Date(Date.now() + 86400000).toISOString().substring(0, 10);
            timeInput.value = `${tomorrow} 20:30:00`;
        }
    }

    async function populatePosForm() {
        const stSelect = document.getElementById("pos-showtime-select");
        const comboSelect = document.getElementById("pos-combo-select");
        if (stSelect && state.showtimes.length > 0) {
            stSelect.innerHTML = state.showtimes.map((s) =>
                `<option value="${s.showtimeId}">[#${s.showtimeId}] ${s.movieTitle} | ${s.auditoriumName} | ${s.startTime} (Trống ${s.availableSeats})</option>`
            ).join("");
            await populatePosSeats(state.showtimes[0].showtimeId);
        }
        if (comboSelect) {
            const active = state.combos.filter((c) => c.status === "AVAILABLE" && c.stockQuantity > 0);
            comboSelect.innerHTML = `<option value="">-- Không mua kèm Combo --</option>` +
                active.map((c) => `<option value="${c.comboId}">${c.comboCode} - ${c.name} (${formatVnd(c.price)})</option>`).join("");
        }
    }

    async function populatePosSeats(showtimeId) {
        const seatSelect = document.getElementById("pos-seat-select");
        if (!seatSelect || !showtimeId) return;
        const resp = await fetch("/api/booking/seats?showtimeId=" + showtimeId);
        const seats = await resp.json();
        const avail = seats.filter((s) => s.status === "AVAILABLE");
        seatSelect.innerHTML = avail.map((s) =>
            `<option value="${s.showtimeSeatId}">Ghế ${s.seatCode} (${s.seatType}) — ${formatVnd(s.finalPrice)}</option>`
        ).join("");
    }

    function renderShowtimesTable() {
        const tbody = document.getElementById("showtimes-table-body");
        if (!tbody) return;
        tbody.innerHTML = state.showtimes.map((s) => `
            <tr>
                <th scope="row">#SC${s.showtimeId}</th>
                <td><strong>${s.movieTitle}</strong> (${s.durationMinutes}p)</td>
                <td>${s.auditoriumName}</td>
                <td>${s.startTime}</td>
                <td>${s.endTime}</td>
                <td class="num-col">${formatVnd(s.basePrice)}</td>
                <td class="num-col"><strong>${s.availableSeats}/${s.totalSeats}</strong></td>
            </tr>
        `).join("");
    }

    function renderCombosTable() {
        const tbody = document.getElementById("combos-table-body");
        if (!tbody) return;
        tbody.innerHTML = state.combos.map((c) => `
            <tr>
                <th scope="row">${c.comboCode}</th>
                <td>
                    <strong>${c.name}</strong><br>
                    <span class="field-hint">${c.itemsIncluded}</span>
                </td>
                <td class="num-col">${formatVnd(c.price)}</td>
                <td class="num-col">${c.stockQuantity}</td>
                <td><span class="role-pill">${c.status}</span></td>
                <td>
                    <button type="button" class="btn-outline-sm" data-edit-combo="${c.comboId}">Sửa</button>
                    <button type="button" class="btn-outline-sm" data-toggle-combo="${c.comboId}">
                        ${c.status === "AVAILABLE" ? "Ngừng KD" : "Mở bán"}
                    </button>
                </td>
            </tr>
        `).join("");

        tbody.querySelectorAll("[data-edit-combo]").forEach((btn) => {
            btn.addEventListener("click", () => {
                const c = state.combos.find((x) => x.comboId === parseInt(btn.dataset.editCombo, 10));
                if (!c) return;
                document.getElementById("combo-id-input").value = c.comboId;
                document.getElementById("combo-code-input").value = c.comboCode;
                document.getElementById("combo-name-input").value = c.name;
                document.getElementById("combo-items-input").value = c.itemsIncluded;
                document.getElementById("combo-price-input").value = c.price;
                document.getElementById("combo-stock-input").value = c.stockQuantity;
            });
        });

        tbody.querySelectorAll("[data-toggle-combo]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const resp = await fetch("/api/combos", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ action: "toggle", comboId: btn.dataset.toggleCombo })
                });
                const data = await resp.json();
                showBanner(data.message, data.success ? "success" : "error");
                await loadCombos();
            });
        });
    }

    function resetComboForm() {
        document.getElementById("combo-id-input").value = "0";
        document.getElementById("combo-code-input").value = "";
        document.getElementById("combo-name-input").value = "";
        document.getElementById("combo-items-input").value = "1 Bắp Phô mai (XL) + 2 Pepsi (L)";
        document.getElementById("combo-price-input").value = "125000";
        document.getElementById("combo-stock-input").value = "100";
    }

    async function loadDashboardAndTickets() {
        const [repResp, tktResp] = await Promise.all([
            fetch("/api/reports/dashboard"),
            fetch("/api/booking/tickets")
        ]);
        const rep = await repResp.json();
        const tickets = await tktResp.json();

        document.getElementById("kpi-total-revenue").textContent = formatVnd(rep.totalRevenue);
        document.getElementById("kpi-channel-split").textContent =
            `Online: ${formatVnd(rep.onlineRevenue)} | POS: ${formatVnd(rep.posRevenue)}`;
        document.getElementById("kpi-seat-revenue").textContent = formatVnd(rep.seatRevenue);
        document.getElementById("kpi-combo-revenue").textContent = formatVnd(rep.comboRevenue);
        document.getElementById("kpi-tickets-count").textContent = `${rep.ticketsSold} vé hiệu lực`;
        document.getElementById("kpi-checkin-breakdown").textContent =
            `Đã Check-in: ${rep.checkedInTickets} | Đã hủy: ${rep.cancelledTickets}`;

        const movieRevBody = document.getElementById("movie-rev-table-body");
        if (movieRevBody && rep.movieBreakdown) {
            movieRevBody.innerHTML = rep.movieBreakdown.map((m) => `
                <tr>
                    <th scope="row">${m.title}</th>
                    <td class="num-col">${m.showtimeCount}</td>
                    <td class="num-col">${m.ticketsSold}</td>
                    <td class="num-col"><strong>${formatVnd(m.revenue)}</strong></td>
                </tr>
            `).join("");
        }

        const tktBody = document.getElementById("tickets-table-body");
        if (tktBody) {
            tktBody.innerHTML = tickets.map((t) => `
                <tr>
                    <th scope="row">${t.ticketCode}</th>
                    <td>${t.customerName}</td>
                    <td>${t.movieTitle}<br><span class="field-hint">${t.auditoriumName} | ${t.startTime}</span></td>
                    <td><strong>${t.seatCode}</strong> (${t.seatType})</td>
                    <td class="num-col">${formatVnd(t.ticketPrice)}</td>
                    <td><span class="role-pill">${t.ticketStatus}</span></td>
                    <td>
                        <button type="button" class="btn-outline-sm" data-quick-checkin="${t.ticketCode}">Soát QR</button>
                        ${t.ticketStatus === "ISSUED" ? `<button type="button" class="btn-outline-sm" data-cancel-ticket="${t.ticketCode}">Hủy vé</button>` : ""}
                    </td>
                </tr>
            `).join("");

            tktBody.querySelectorAll("[data-quick-checkin]").forEach((btn) => {
                btn.addEventListener("click", async () => {
                    const code = btn.dataset.quickCheckin;
                    document.getElementById("qr-code-input").value = code;
                    await performQrCheckin(code);
                });
            });

            tktBody.querySelectorAll("[data-cancel-ticket]").forEach((btn) => {
                btn.addEventListener("click", async () => {
                    const code = btn.dataset.cancelTicket;
                    const resp = await fetch("/api/booking/cancel", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ ticketCode: code })
                    });
                    const data = await resp.json();
                    showBanner(data.message, data.success ? "success" : "error");
                    await Promise.all([loadShowtimes(), loadSeatMap(state.selectedShowtimeId), loadDashboardAndTickets()]);
                });
            });
        }
    }

    document.addEventListener("DOMContentLoaded", () => {
        initFormValidation();
        initDialogs();
        initNavigationAndAuth();
        initEventHandlers();
        loadInitialData();
    });
})();

