/**
 * CINEHUB — tầng tương tác giao diện (static/js/app.js)
 * Học phần IT3180 - Nhập môn Công nghệ Phần mềm (Nhóm 3 - HUST)
 *
 * Danh tính người dùng do máy chủ xác định qua mã phiên (Authorization: Bearer).
 * Mọi giá trị đưa vào innerHTML đều đi qua escapeHtml() để chặn XSS.
 */
(function () {
    "use strict";

    // =========================================================================
    // 0. TRẠNG THÁI & TIỆN ÍCH
    // =========================================================================
    const state = {
        currentUser: null,
        token: null,
        movies: [],          // danh sách phim theo bộ lọc của bảng giờ chiếu
        allMovies: [],       // toàn bộ phim (lập lịch, sắp chiếu)
        auditoriums: [],
        showtimes: [],
        combos: [],
        selectedShowtimeId: null,
        seatMap: [],
        selectedSeatIds: [],
        comboQuantities: {},
        holdCountdownTimer: null,
        holdRemainingSeconds: 0,
        pos: { showtimeId: null, seatMap: [], selected: [] },
        timelineDate: null,
        lastCreatedShowtimeId: null,
        boardFiltered: false
    };

    const SESSION_KEY = "cinehub.session";
    const MAX_SEATS = 8;
    const LOW_SEAT_RATIO = 0.15;
    const vndFormatter = new Intl.NumberFormat("vi-VN");
    const WEEKDAYS = ["Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"];
    const SEAT_TIER_LABEL = { STANDARD: "Thường", VIP: "VIP", COUPLE: "Ghế đôi" };
    const PAYMENT_LABEL = {
        VIETQR: "VietQR", E_WALLET: "Ví điện tử", CARD: "Thẻ", CASH: "Tiền mặt"
    };
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

    const $ = (id) => document.getElementById(id);

    function escapeHtml(value) {
        return String(value === null || value === undefined ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function formatVnd(amount) {
        return vndFormatter.format(Math.round(Number(amount) || 0)) + " ₫";
    }

    function parseTs(ts) {
        if (!ts) return null;
        const d = new Date(String(ts).trim().replace(" ", "T"));
        return isNaN(d.getTime()) ? null : d;
    }

    const pad = (n) => String(n).padStart(2, "0");

    function fmtTime(ts) {
        const d = parseTs(ts);
        return d ? `${pad(d.getHours())}:${pad(d.getMinutes())}` : "--:--";
    }

    function fmtDate(ts) {
        const d = parseTs(ts);
        return d ? `${pad(d.getDate())}/${pad(d.getMonth() + 1)}` : "";
    }

    function fmtDateTime(ts) {
        return ts ? `${fmtDate(ts)} ${fmtTime(ts)}` : "";
    }

    function dayKey(ts) {
        const d = parseTs(ts);
        return d ? `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` : "";
    }

    function dayLabel(key) {
        const d = new Date(key + "T00:00:00");
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const diff = Math.round((d - today) / 86400000);
        const base = `${WEEKDAYS[d.getDay()]} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;
        if (diff === 0) return `Hôm nay · ${base}`;
        if (diff === 1) return `Ngày mai · ${base}`;
        if (diff === -1) return `Hôm qua · ${base}`;
        return base;
    }

    /** Ô nhập giờ chiếu theo kiểu Việt Nam: "15/11/2026 20:30". */
    function toViInputValue(ts) {
        const d = parseTs(ts);
        return d ? `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}` : "";
    }

    /** "15/11/2026 20:30" -> "2026-11-15 20:30:00" (định dạng máy chủ nhận); null nếu sai. */
    function parseViInput(text) {
        const m = /^\s*(\d{1,2})\/(\d{1,2})\/(\d{4})\s+(\d{1,2}):(\d{2})\s*$/.exec(text || "");
        if (!m) return null;
        const [dd, mm, yyyy, hh, mi] = m.slice(1).map(Number);
        const d = new Date(yyyy, mm - 1, dd, hh, mi);
        if (d.getMonth() !== mm - 1 || d.getDate() !== dd || hh > 23 || mi > 59) return null;
        return `${yyyy}-${pad(mm)}-${pad(dd)} ${pad(hh)}:${pad(mi)}:00`;
    }

    function hasRole(...roles) {
        return !!state.currentUser && roles.includes(state.currentUser.role);
    }

    function isPhone() {
        return window.matchMedia("(max-width: 639px)").matches;
    }

    // -------------------------------------------------------------------------
    // Chữ lật (split-flap): chỉ ký tự đổi giá trị mới lật
    // -------------------------------------------------------------------------
    const flapMemory = new Map();

    function flapMarkup(text, prev) {
        const chars = Array.from(String(text));
        const old = prev === undefined ? null : Array.from(String(prev));
        let flipIndex = 0;
        return chars.map((ch, i) => {
            const isSpace = ch === " " || ch === " ";
            const changed = old !== null && old[i] !== ch && !isSpace;
            const cls = "g" + (isSpace ? " sp" : "") + (changed ? " is-flipping" : "");
            const style = changed ? ` style="--i:${flipIndex++}"` : "";
            return `<span class="${cls}"${style}>${isSpace ? "&nbsp;" : escapeHtml(ch)}</span>`;
        }).join("");
    }

    /** Ghi giá trị vào phần tử .flap; key mặc định là id của phần tử. */
    function flap(el, text, key) {
        if (!el) return;
        const k = key || el.id;
        const prev = k ? flapMemory.get(k) : undefined;
        const value = String(text);
        if (prev === value && el.childElementCount) return;
        el.innerHTML = flapMarkup(value, reduceMotion.matches ? undefined : prev);
        el.setAttribute("aria-label", value);
        if (k) flapMemory.set(k, value);
    }

    function flapStatic(text, key) {
        const prev = flapMemory.get(key);
        flapMemory.set(key, String(text));
        return `<span class="flap" aria-hidden="true">${flapMarkup(text, reduceMotion.matches ? undefined : prev)}</span>`;
    }

    // -------------------------------------------------------------------------
    // Thông báo
    // -------------------------------------------------------------------------
    function showBanner(message, type) {
        const banner = $("global-banner");
        if (!banner) return;
        banner.textContent = message;
        banner.className = "toast " + (type || "info");
        banner.hidden = false;
        clearTimeout(banner._hideTimer);
        banner._hideTimer = setTimeout(() => {
            banner.hidden = true;
        }, type === "error" ? 8000 : 5000);
    }

    // =========================================================================
    // 1. PHIÊN ĐĂNG NHẬP & GỌI API (RBAC)
    // =========================================================================
    async function apiFetch(url, options) {
        const opts = Object.assign({}, options || {});
        opts.headers = Object.assign({}, opts.headers || {});
        if (state.token) {
            opts.headers["Authorization"] = "Bearer " + state.token;
        }
        const resp = await fetch(url, opts);
        if (resp.status === 401 && state.token) {
            setSession(null, null);
            showBanner("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", "error");
        }
        return resp;
    }

    async function readJson(resp, fallback) {
        try {
            return await resp.json();
        } catch (ignored) {
            return fallback;
        }
    }

    function postJson(url, body) {
        return apiFetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body || {})
        });
    }

    function setSession(user, token) {
        state.currentUser = user;
        state.token = token;
        try {
            if (user && token) {
                sessionStorage.setItem(SESSION_KEY, JSON.stringify({ user, token }));
            } else {
                sessionStorage.removeItem(SESSION_KEY);
            }
        } catch (ignored) {
            // sessionStorage bị chặn: phiên chỉ tồn tại tới khi tải lại trang
        }
        renderUserPanel();
        applyRoleVisibility();
    }

    function restoreSession() {
        try {
            const raw = sessionStorage.getItem(SESSION_KEY);
            if (raw) {
                const saved = JSON.parse(raw);
                state.currentUser = saved.user || null;
                state.token = saved.token || null;
            }
        } catch (ignored) {
            state.currentUser = null;
            state.token = null;
        }
        renderUserPanel();
        applyRoleVisibility();
    }

    /** Xác minh phiên đã lưu với máy chủ (máy chủ khởi động lại thì phiên cũ mất hiệu lực). */
    async function validateSession() {
        if (!state.token) return;
        try {
            const resp = await fetch("/api/auth/me", { headers: { Authorization: "Bearer " + state.token } });
            if (resp.status === 401) {
                setSession(null, null);
                showBanner("Phiên đăng nhập trước đã hết hạn. Vui lòng đăng nhập lại.", "info");
                return;
            }
            const data = await readJson(resp, null);
            if (data && data.success && data.user) {
                setSession(data.user, state.token);
            }
        } catch (ignored) {
            // Mất kết nối: giữ phiên, lần gọi API sau sẽ kiểm tra lại
        }
    }

    function renderUserPanel() {
        const u = state.currentUser;
        const pill = $("current-role-pill");
        pill.textContent = u ? roleLabel(u.role) : "KHÁCH";
        pill.dataset.role = u ? u.role : "";
        const nameEl = $("current-user-name");
        nameEl.textContent = u ? u.fullName.replace(/\s*\(.*\)\s*$/, "") : "Chưa đăng nhập";
        nameEl.title = u ? u.fullName : "";
        $("btn-logout").hidden = !u;
        $("btn-open-login").hidden = !!u;
        document.querySelectorAll("[data-quick-login]").forEach((c) => {
            const on = !!u && c.dataset.quickLogin.split(":")[0] === u.username;
            c.classList.toggle("is-active", on);
            c.setAttribute("aria-pressed", String(on));
        });
        if (u) {
            const nameInput = $("cust-name-input");
            const phoneInput = $("cust-phone-input");
            if (!nameInput.value) nameInput.value = u.fullName.replace(/\s*\(.*\)\s*$/, "");
            if (!phoneInput.value && u.phone) phoneInput.value = u.phone;
        }
    }

    function roleLabel(role) {
        return { MANAGER: "QUẢN LÝ", STAFF: "NHÂN VIÊN", CUSTOMER: "KHÁCH HÀNG" }[role] || role;
    }

    /** Ẩn tab không thuộc quyền (máy chủ vẫn kiểm tra quyền độc lập). */
    function applyRoleVisibility() {
        let activeHidden = false;
        document.querySelectorAll(".tab[data-roles]").forEach((btn) => {
            const visible = hasRole(...btn.dataset.roles.split(","));
            btn.hidden = !visible;
            if (!visible && btn.getAttribute("aria-selected") === "true") activeHidden = true;
        });
        if (activeHidden) activateTab("tab-booking");
    }

    function requireLoginUi(message) {
        if (state.currentUser) return true;
        showBanner(message || "Vui lòng đăng nhập để tiếp tục.", "error");
        const loginModal = $("login-modal");
        if (loginModal && !loginModal.open) loginModal.showModal();
        return false;
    }

    async function performLogin(username, password) {
        try {
            const resp = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password })
            });
            const data = await readJson(resp, { success: false, message: "Máy chủ trả về dữ liệu không hợp lệ." });
            if (!data.success) {
                showBanner(data.message, "error");
                return false;
            }
            if (state.token && state.token !== data.token) {
                await apiFetch("/api/auth/logout", { method: "POST" }).catch(() => {});
            }
            resetCart();
            $("cust-name-input").value = "";
            $("cust-phone-input").value = "";
            setSession(data.user, data.token);
            showBanner(data.message, "success");
            await loadSeatMap(state.selectedShowtimeId);
            await refreshActiveTab();
            return true;
        } catch (err) {
            showBanner("Không kết nối được máy chủ: " + err.message, "error");
            return false;
        }
    }

    async function performLogout() {
        try {
            await apiFetch("/api/auth/logout", { method: "POST" });
        } catch (ignored) {
            // vẫn xóa phiên phía trình duyệt
        }
        resetCart();
        $("cust-name-input").value = "";
        $("cust-phone-input").value = "";
        setSession(null, null);
        showBanner("Đã đăng xuất.", "info");
        await loadSeatMap(state.selectedShowtimeId);
    }

    function resetCart() {
        state.selectedSeatIds = [];
        state.comboQuantities = {};
        clearInterval(state.holdCountdownTimer);
        $("hold-timer-badge").hidden = true;
        renderBookingCombos();
        updateCartTotals();
    }

    // =========================================================================
    // 2. TAB, HỘP THOẠI, KIỂM TRA BIỂU MẪU
    // =========================================================================
    function activateTab(panelId) {
        document.querySelectorAll(".tab").forEach((tab) => {
            const on = tab.dataset.tab === panelId;
            tab.setAttribute("aria-selected", String(on));
            tab.tabIndex = on ? 0 : -1;
        });
        document.querySelectorAll(".tab-panel").forEach((panel) => {
            const on = panel.id === panelId;
            panel.hidden = !on;
            panel.classList.toggle("is-active", on);
        });
        refreshActiveTab();
    }

    async function refreshActiveTab() {
        const active = document.querySelector(".tab[aria-selected='true']");
        const id = active ? active.dataset.tab : "tab-booking";
        if (id === "tab-pos") await populatePosForm();
        if (id === "tab-schedule") renderTimeline();
        if (id === "tab-checkin") await loadDashboardAndTickets();
    }

    function initTabs() {
        const tabs = Array.from(document.querySelectorAll(".tab"));
        tabs.forEach((tab) => {
            tab.addEventListener("click", () => activateTab(tab.dataset.tab));
            tab.addEventListener("keydown", (e) => {
                if (e.key !== "ArrowRight" && e.key !== "ArrowLeft") return;
                const visible = tabs.filter((t) => !t.hidden);
                const idx = visible.indexOf(tab);
                const next = visible[(idx + (e.key === "ArrowRight" ? 1 : -1) + visible.length) % visible.length];
                next.focus();
                activateTab(next.dataset.tab);
            });
        });
    }

    function initDialogs() {
        const ticketModal = $("ticket-modal");
        const loginModal = $("login-modal");
        $("btn-close-ticket-modal").addEventListener("click", () => ticketModal.close());
        $("btn-done-ticket-modal").addEventListener("click", () => ticketModal.close());
        $("btn-open-login").addEventListener("click", () => {
            loginModal.showModal();
            $("login-username-input").focus();
        });
        $("btn-close-login-modal").addEventListener("click", () => loginModal.close());
        $("btn-cancel-login-modal").addEventListener("click", () => loginModal.close());
        [ticketModal, loginModal].forEach((dlg) => {
            dlg.addEventListener("click", (e) => {
                if (e.target === dlg) dlg.close();
            });
        });
    }

    function initFormValidation() {
        document.querySelectorAll("form.validated-form").forEach((form) => {
            const sync = (input, invalid) => {
                input.classList.toggle("user-invalid-fallback", invalid);
                input.setAttribute("aria-invalid", String(invalid));
                const err = input.parentElement ? input.parentElement.querySelector(".error-msg") : null;
                if (err) {
                    if (!err.id) err.id = "err-" + (input.id || Math.random().toString(36).slice(2));
                    if (invalid) input.setAttribute("aria-describedby", err.id);
                    else input.removeAttribute("aria-describedby");
                }
            };
            form.querySelectorAll("input, select").forEach((input) => {
                input.addEventListener("blur", () => {
                    if (input.value) sync(input, !input.checkValidity());
                });
                input.addEventListener("input", () => {
                    if (input.getAttribute("aria-invalid") === "true") sync(input, !input.checkValidity());
                });
                input.addEventListener("invalid", () => sync(input, true));
            });
        });
    }

    function formValid(form) {
        if (form.checkValidity()) return true;
        const firstBad = form.querySelector(":invalid");
        if (firstBad) firstBad.focus();
        return false;
    }

    // =========================================================================
    // 3. TẢI DỮ LIỆU
    // =========================================================================
    async function loadInitialData() {
        await Promise.all([
            loadMovies("", "SHOWING"),
            loadAllMovies(),
            loadAuditoriums(),
            loadShowtimes(),
            loadCombos()
        ]);
        if (state.showtimes.length > 0 && !state.selectedShowtimeId) {
            const now = Date.now();
            const upcoming = state.showtimes.find((s) => {
                const start = parseTs(s.startTime);
                return start && start.getTime() > now && s.availableSeats > 0;
            });
            state.selectedShowtimeId = (upcoming || state.showtimes[0]).showtimeId;
            renderBoard();
            await loadSeatMap(state.selectedShowtimeId);
        }
        await refreshActiveTab();
    }

    async function loadMovies(keyword, status) {
        const q = new URLSearchParams({ keyword: keyword || "", status: status || "ALL" });
        const resp = await apiFetch("/api/movies?" + q.toString());
        state.movies = await readJson(resp, []);
        state.boardFiltered = !!(keyword && keyword.trim()) || (status && status !== "ALL" && status !== "SHOWING");
        renderBoard();
    }

    async function loadAllMovies() {
        const resp = await apiFetch("/api/movies?status=ALL");
        state.allMovies = await readJson(resp, []);
        populateScheduleSelects();
        renderComingSoon();
    }

    async function loadAuditoriums() {
        const resp = await apiFetch("/api/auditoriums");
        state.auditoriums = await readJson(resp, []);
        populateScheduleSelects();
    }

    async function loadShowtimes() {
        const resp = await apiFetch("/api/showtimes");
        state.showtimes = await readJson(resp, []);
        renderBoard();
        renderComingSoon();
        renderShowtimesTable();
        populateTimelineDates();
        renderTimeline();
    }

    async function loadCombos() {
        const resp = await apiFetch("/api/combos");
        state.combos = await readJson(resp, []);
        renderBookingCombos();
        renderCombosTable();
        updateCartTotals();
    }

    // =========================================================================
    // 4. BẢNG GIỜ CHIẾU
    // =========================================================================
    function showtimeStatus(s) {
        const now = Date.now();
        const start = parseTs(s.startTime);
        const end = parseTs(s.endTime);
        if (end && end.getTime() < now) return { cls: "status-past", text: "Đã chiếu" };
        if (start && start.getTime() <= now) return { cls: "status-past", text: "Đang chiếu" };
        if (s.availableSeats <= 0) return { cls: "status-out", text: "Hết vé" };
        if (s.totalSeats && s.availableSeats / s.totalSeats <= LOW_SEAT_RATIO) return { cls: "status-low", text: "Sắp hết" };
        return { cls: "status-ok", text: "Đang bán" };
    }

    function roomShort(name) {
        const m = /Phòng\s*(\d+)\s*-\s*([^\s]+)/i.exec(name || "");
        return m ? `${m[1]} ${m[2]}` : (name || "");
    }

    function renderBoard() {
        const list = $("showtime-board");
        if (!list) return;
        const allowed = new Set(state.movies.map((m) => m.movieId));
        const rows = state.showtimes.filter((s) => allowed.has(s.movieId));

        if (rows.length === 0) {
            list.innerHTML = `<li class="board-empty">${state.showtimes.length === 0
                ? "Chưa có suất chiếu nào được mở bán."
                : "Không có suất chiếu nào khớp bộ lọc."}</li>`;
            return;
        }

        let html = "";
        let currentDay = null;
        rows.forEach((s) => {
            const key = dayKey(s.startTime);
            if (key !== currentDay) {
                currentDay = key;
                html += `<li class="board-day" aria-hidden="true">${escapeHtml(dayLabel(key))}</li>`;
            }
            const st = showtimeStatus(s);
            const movie = state.allMovies.find((m) => m.movieId === s.movieId) || {};
            const selected = s.showtimeId === state.selectedShowtimeId;
            const time = fmtTime(s.startTime);
            const label = `${time} ${dayLabel(key)}, ${s.movieTitle}, ${s.auditoriumName}, còn ${s.availableSeats} ghế, ${st.text}`;
            html += `
                <li>
                    <button type="button" class="board-row" data-st-id="${s.showtimeId}"
                            aria-current="${selected}" aria-label="${escapeHtml(label)}">
                        ${flapStatic(time, "bt-time-" + s.showtimeId)}
                        <span class="bt-film">
                            <span class="bt-title" title="${escapeHtml(s.movieTitle)}">${escapeHtml(s.movieTitle)}</span>
                            <span class="bt-sub"><span class="bt-room">${escapeHtml(roomShort(s.auditoriumName))}</span><span class="age">${escapeHtml(s.ageRating || movie.ageRating || "")}</span>${escapeHtml(s.durationMinutes)} phút</span>
                        </span>
                        <span class="bt-seats">${flapStatic(String(s.availableSeats), "bt-seats-" + s.showtimeId)}</span>
                        <span class="status ${st.cls}">${escapeHtml(st.text)}</span>
                    </button>
                </li>`;
        });
        list.innerHTML = html;

        list.querySelectorAll(".board-row").forEach((btn) => {
            btn.addEventListener("click", () => selectShowtime(parseInt(btn.dataset.stId, 10)));
        });
    }

    function renderComingSoon() {
        const box = $("coming-soon-strip");
        if (!box) return;
        const withShows = new Set(state.showtimes.map((s) => s.movieId));
        const soon = state.allMovies.filter((m) => m.status === "COMING_SOON" || (m.status === "SHOWING" && !withShows.has(m.movieId)));
        if (soon.length === 0) {
            box.hidden = true;
            return;
        }
        box.hidden = false;
        box.innerHTML = `<h3>Sắp chiếu</h3><ul>${soon.map((m) => `
            <li><span>${escapeHtml(m.title)} <span class="age">${escapeHtml(m.ageRating)}</span></span>
            <span>${m.status === "COMING_SOON" ? "Khởi chiếu " + escapeHtml(fmtDate(m.releaseDate + " 00:00:00")) : "Chưa có suất"}</span></li>`).join("")}</ul>`;
    }

    function tickClock() {
        const now = new Date();
        flap($("board-clock"), `${pad(now.getHours())}:${pad(now.getMinutes())}`);
    }

    async function selectShowtime(id) {
        if (state.selectedShowtimeId !== id) {
            state.selectedSeatIds = [];
            clearInterval(state.holdCountdownTimer);
            $("hold-timer-badge").hidden = true;
        }
        state.selectedShowtimeId = id;
        renderBoard();
        await loadSeatMap(id);
        if (isPhone()) goStep(2);
    }

    // =========================================================================
    // 5. SƠ ĐỒ GHẾ (dùng chung cho đặt vé online và bán tại quầy)
    // =========================================================================
    function seatStateText(s, opts) {
        if (opts.selected.includes(s.showtimeSeatId)) return "đang chọn";
        if (s.status === "BOOKED") return "đã bán";
        if (s.status === "HELD") return s.heldByUserId === opts.myId ? "bạn đang giữ" : "người khác đang giữ";
        return "còn trống";
    }

    function renderSeatMap(container, seats, opts) {
        if (!container) return;
        if (!seats || seats.length === 0) {
            container.innerHTML = `<p class="empty">Chưa có sơ đồ ghế cho suất này.</p>`;
            return;
        }
        const rows = {};
        seats.forEach((s) => {
            (rows[s.rowLabel] = rows[s.rowLabel] || []).push(s);
        });

        const seatBtn = (s) => {
            const isSel = opts.selected.includes(s.showtimeSeatId);
            const heldByOther = s.status === "HELD" && s.heldByUserId !== opts.myId;
            const isMine = s.status === "HELD" && s.heldByUserId === opts.myId && !isSel;
            const disabled = s.status === "BOOKED" || heldByOther;
            const cls = ["seat", "tier-" + s.seatType];
            if (isSel) cls.push("is-selected");
            if (isMine) cls.push("is-mine");
            if (heldByOther) cls.push("is-held");
            if (s.status === "BOOKED") cls.push("is-booked");
            const label = `Ghế ${s.seatCode}, ${SEAT_TIER_LABEL[s.seatType] || s.seatType}, ${formatVnd(s.finalPrice)}, ${seatStateText(s, opts)}`;
            return `<button type="button" class="${cls.join(" ")}" data-ss-id="${s.showtimeSeatId}"
                        aria-label="${escapeHtml(label)}" title="${escapeHtml(label)}"
                        ${disabled ? "disabled" : `aria-pressed="${isSel}"`}>${escapeHtml(s.seatCode)}</button>`;
        };

        container.innerHTML = Object.keys(rows).sort().map((rowLabel) => {
            const list = rows[rowLabel].slice().sort((a, b) => a.seatNumber - b.seatNumber);
            let inner = "";
            for (let i = 0; i < list.length; i++) {
                const s = list[i];
                const next = list[i + 1];
                if (s.seatType === "COUPLE" && next && next.seatType === "COUPLE") {
                    inner += `<span class="seat-pair">${seatBtn(s)}${seatBtn(next)}</span>`;
                    i++;
                } else {
                    inner += seatBtn(s);
                }
            }
            return `<div class="seat-row"><span class="row-label" aria-hidden="true">${escapeHtml(rowLabel)}</span>${inner}<span class="row-label" aria-hidden="true">${escapeHtml(rowLabel)}</span></div>`;
        }).join("");

        container.querySelectorAll("button.seat:not([disabled])").forEach((btn) => {
            btn.addEventListener("click", () => opts.onToggle(parseInt(btn.dataset.ssId, 10)));
        });
    }

    function toggleIn(list, id) {
        const idx = list.indexOf(id);
        if (idx >= 0) {
            list.splice(idx, 1);
            return true;
        }
        if (list.length >= MAX_SEATS) {
            showBanner(`Mỗi giao dịch chọn tối đa ${MAX_SEATS} ghế.`, "error");
            return false;
        }
        list.push(id);
        return true;
    }

    async function loadSeatMap(showtimeId) {
        if (!showtimeId) return;
        const resp = await apiFetch("/api/booking/seats?showtimeId=" + showtimeId);
        state.seatMap = await readJson(resp, []);
        const st = state.showtimes.find((s) => s.showtimeId === showtimeId);
        if (st) {
            $("selected-movie-heading").textContent = st.movieTitle;
            $("selected-showtime-meta").textContent =
                `${st.auditoriumName} · ${dayLabel(dayKey(st.startTime))} · ${fmtTime(st.startTime)}–${fmtTime(st.endTime)} (đã gồm 15 phút dọn phòng) · ${st.ageRating || ""} · giá gốc ${formatVnd(st.basePrice)}`;
            $("cart-showtime-label").textContent = `${fmtTime(st.startTime)} ${fmtDate(st.startTime)} · ${roomShort(st.auditoriumName)}`;
            const boardSeats = document.querySelector(`.board-row[data-st-id="${showtimeId}"] .bt-seats .flap`);
            const freeNow = state.seatMap.filter((s) => s.status === "AVAILABLE").length;
            if (boardSeats && freeNow !== st.availableSeats) {
                st.availableSeats = freeNow;
                renderBoard();
            }
        }
        // Bỏ chọn những ghế vừa bị người khác giữ hoặc đã bán
        const myId = state.currentUser ? state.currentUser.userId : null;
        state.selectedSeatIds = state.selectedSeatIds.filter((id) => {
            const s = state.seatMap.find((x) => x.showtimeSeatId === id);
            return s && (s.status === "AVAILABLE" || (s.status === "HELD" && s.heldByUserId === myId));
        });
        // Ghế chính mình đang giữ (ví dụ sau khi tải lại trang) luôn nằm trong giỏ, kèm đồng hồ giữ chỗ
        const mine = state.seatMap.filter((s) => s.status === "HELD" && myId !== null && s.heldByUserId === myId);
        mine.forEach((s) => {
            if (!state.selectedSeatIds.includes(s.showtimeSeatId) && state.selectedSeatIds.length < MAX_SEATS) {
                state.selectedSeatIds.push(s.showtimeSeatId);
            }
        });
        if (mine.length && $("hold-timer-badge").hidden) {
            const until = Math.min(...mine.map((s) => (parseTs(s.lockedUntil) || new Date()).getTime()));
            const left = Math.round((until - Date.now()) / 1000);
            if (left > 0) startHoldCountdown(left);
        }
        renderSeatGrid();
        renderLegendPrices();
        updateCartTotals();
    }

    function renderSeatGrid() {
        renderSeatMap($("seat-grid-container"), state.seatMap, {
            selected: state.selectedSeatIds,
            myId: state.currentUser ? state.currentUser.userId : null,
            onToggle: (id) => {
                if (toggleIn(state.selectedSeatIds, id)) {
                    renderSeatGrid();
                    updateCartTotals();
                    const btn = document.querySelector(`#seat-grid-container [data-ss-id="${id}"]`);
                    if (btn) btn.focus();
                }
            }
        });
    }

    // =========================================================================
    // 6. GIỎ HÀNG (cuống vé)
    // =========================================================================
    function renderBookingCombos() {
        const listEl = $("booking-combo-list");
        if (!listEl) return;
        const active = state.combos.filter((c) => c.status === "AVAILABLE" && c.stockQuantity > 0);
        if (active.length === 0) {
            listEl.innerHTML = `<li class="combo-item"><small>Hiện chưa có combo đang bán.</small></li>`;
            return;
        }
        listEl.innerHTML = active.map((c) => {
            const qty = state.comboQuantities[c.comboId] || 0;
            const max = Math.min(10, c.stockQuantity);
            return `
                <li class="combo-item">
                    <span><b>${escapeHtml(c.name)}</b><small>${escapeHtml(c.itemsIncluded)} · ${escapeHtml(formatVnd(c.price))}</small></span>
                    <span class="stepper" role="group" aria-label="Số lượng ${escapeHtml(c.name)}">
                        <button type="button" data-combo-step="-1" data-combo-id="${c.comboId}" aria-label="Bớt một" ${qty <= 0 ? "disabled" : ""}><svg viewBox="0 0 16 16" aria-hidden="true"><path d="M3 8h10"/></svg></button>
                        <output aria-live="polite">${qty}</output>
                        <button type="button" data-combo-step="1" data-combo-id="${c.comboId}" aria-label="Thêm một" ${qty >= max ? "disabled" : ""}><svg viewBox="0 0 16 16" aria-hidden="true"><path d="M3 8h10M8 3v10"/></svg></button>
                    </span>
                </li>`;
        }).join("");
        listEl.querySelectorAll("[data-combo-step]").forEach((btn) => {
            btn.addEventListener("click", () => {
                const id = parseInt(btn.dataset.comboId, 10);
                const c = state.combos.find((x) => x.comboId === id);
                const max = c ? Math.min(10, c.stockQuantity) : 10;
                const next = (state.comboQuantities[id] || 0) + parseInt(btn.dataset.comboStep, 10);
                state.comboQuantities[id] = Math.max(0, Math.min(max, next));
                renderBookingCombos();
                updateCartTotals();
                const again = listEl.querySelector(`[data-combo-id="${id}"][data-combo-step="${btn.dataset.comboStep}"]`);
                if (again && !again.disabled) again.focus();
            });
        });
    }

    function updateCartTotals() {
        const chosen = state.seatMap.filter((s) => state.selectedSeatIds.includes(s.showtimeSeatId));
        const seatTotal = chosen.reduce((acc, s) => acc + s.finalPrice, 0);
        let comboTotal = 0;
        state.combos.forEach((c) => {
            comboTotal += c.price * (state.comboQuantities[c.comboId] || 0);
        });
        $("cart-seat-count").textContent = String(chosen.length);
        $("cart-seat-codes").textContent = chosen.length ? chosen.map((s) => s.seatCode).join(", ") : "Chưa chọn ghế";
        $("cart-seat-subtotal").textContent = formatVnd(seatTotal);
        $("final-seat-total").textContent = formatVnd(seatTotal);
        $("final-combo-total").textContent = formatVnd(comboTotal);
        flap($("final-grand-total"), formatVnd(seatTotal + comboTotal));
        $("btn-hold-seats").disabled = chosen.length === 0;
        $("step-seat-count").textContent = String(chosen.length);
        $("step-total").textContent = formatVnd(seatTotal + comboTotal);
    }

    function renderLegendPrices() {
        ["STANDARD", "VIP", "COUPLE"].forEach((tier) => {
            const seat = state.seatMap.find((s) => s.seatType === tier);
            const el = $("legend-price-" + tier);
            if (!el) return;
            el.textContent = seat ? formatVnd(seat.finalPrice) : "";
            el.parentElement.hidden = !seat;
        });
    }

    function startHoldCountdown(seconds) {
        clearInterval(state.holdCountdownTimer);
        state.holdRemainingSeconds = seconds;
        const badge = $("hold-timer-badge");
        badge.hidden = false;
        const tick = () => {
            const left = Math.max(0, state.holdRemainingSeconds);
            flap($("hold-countdown-text"), `${pad(Math.floor(left / 60))}:${pad(left % 60)}`);
            badge.classList.toggle("is-urgent", left <= 60);
            if (state.holdRemainingSeconds <= 0) {
                clearInterval(state.holdCountdownTimer);
                badge.hidden = true;
                showBanner("Hết thời gian giữ chỗ, ghế đã được trả lại.", "info");
                loadSeatMap(state.selectedShowtimeId);
            }
            state.holdRemainingSeconds--;
        };
        tick();
        state.holdCountdownTimer = setInterval(tick, 1000);
    }

    function goStep(step) {
        const layout = $("booking-layout");
        layout.dataset.step = String(step);
        document.querySelectorAll("[data-step-go]").forEach((b) => {
            if (b.dataset.stepGo === String(step)) b.setAttribute("aria-current", "step");
            else b.removeAttribute("aria-current");
        });
        if (isPhone()) window.scrollTo({ top: 0, behavior: reduceMotion.matches ? "auto" : "smooth" });
    }

    // =========================================================================
    // 7. VÉ ĐIỆN TỬ QR
    // =========================================================================
    function openTicketModal(invoice, tickets) {
        const modal = $("ticket-modal");
        const box = $("ticket-modal-content");
        box.innerHTML = `
            <dl class="ticket-summary">
                <div><dt>Hóa đơn</dt><dd>${escapeHtml(invoice.invoiceCode)}</dd></div>
                <div><dt>Khách</dt><dd>${escapeHtml(invoice.customerName)} · ${escapeHtml(invoice.customerPhone)}</dd></div>
                <div><dt>Suất</dt><dd>${escapeHtml(invoice.movieTitle)} · ${escapeHtml(invoice.auditoriumName)} · ${escapeHtml(fmtDateTime(invoice.startTime))}</dd></div>
                <div><dt>Bắp nước</dt><dd>${escapeHtml(invoice.comboSummary)}</dd></div>
                <div class="grand"><dt>Đã thanh toán (${escapeHtml(PAYMENT_LABEL[invoice.paymentMethod] || invoice.paymentMethod)})</dt><dd>${escapeHtml(formatVnd(invoice.totalAmount))}</dd></div>
            </dl>
            <div class="tickets">
                ${tickets.map((t, i) => `
                    <article class="ticket" aria-label="Vé ${escapeHtml(t.ticketCode)}">
                        <div class="ticket-main">
                            <span class="flap" aria-hidden="true">${flapMarkup(t.seatCode)}</span>
                            <span class="ticket-film">${escapeHtml(t.movieTitle)}</span>
                            <span class="ticket-meta">${escapeHtml(t.auditoriumName)} · ${escapeHtml(fmtDateTime(t.startTime))}</span>
                            <span class="ticket-meta">Ghế ${escapeHtml(t.seatCode)} (${escapeHtml(SEAT_TIER_LABEL[t.seatType] || t.seatType)}) · ${escapeHtml(formatVnd(t.ticketPrice))}</span>
                        </div>
                        <div class="ticket-qr">
                            <div class="qr" id="qr-slot-${i}" data-qr="${escapeHtml(t.qrPayload)}" role="img" aria-label="Mã QR của vé ${escapeHtml(t.ticketCode)}"></div>
                            <span>${escapeHtml(t.ticketCode)}</span>
                        </div>
                    </article>`).join("")}
            </div>`;
        box.querySelectorAll(".qr[data-qr]").forEach((slot) => {
            const text = slot.dataset.qr;
            if (window.QRCode) {
                try {
                    // eslint-disable-next-line no-new
                    new window.QRCode(slot, {
                        text, width: 224, height: 224,
                        colorDark: "#17191C", colorLight: "#FBF9F4",
                        correctLevel: window.QRCode.CorrectLevel.M
                    });
                    slot.removeAttribute("title");
                    return;
                } catch (ignored) {
                    // rơi xuống hiển thị dạng chữ
                }
            }
            slot.innerHTML = `<span class="qr-fallback">${escapeHtml(text)}</span>`;
        });
        modal.showModal();
    }

    // =========================================================================
    // 8. SỰ KIỆN ĐẶT VÉ ONLINE
    // =========================================================================
    function initBookingHandlers() {
        $("movie-search-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            await loadMovies($("search-movie-kw").value, $("filter-movie-status").value);
        });
        $("filter-movie-status").addEventListener("change", () => {
            loadMovies($("search-movie-kw").value, $("filter-movie-status").value);
        });

        $("btn-hold-seats").addEventListener("click", async () => {
            if (state.selectedSeatIds.length === 0) {
                showBanner("Chọn ít nhất 1 ghế trên sơ đồ để giữ chỗ.", "error");
                return;
            }
            if (!requireLoginUi("Vui lòng đăng nhập để giữ ghế.")) return;
            const resp = await postJson("/api/booking/hold", {
                showtimeId: state.selectedShowtimeId,
                seatIds: state.selectedSeatIds.join(",")
            });
            const data = await readJson(resp, { success: false, message: "Không giữ được ghế." });
            if (!data.success) {
                showBanner(data.message, "error");
                await loadSeatMap(state.selectedShowtimeId);
                return;
            }
            showBanner(`Đã giữ ${state.selectedSeatIds.length} ghế trong 5 phút.`, "success");
            startHoldCountdown(300);
            await loadSeatMap(state.selectedShowtimeId);
        });

        const onlineForm = $("online-checkout-form");
        onlineForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (state.selectedSeatIds.length === 0) {
                showBanner("Chọn ít nhất 1 ghế trước khi thanh toán.", "error");
                if (isPhone()) goStep(2);
                return;
            }
            if (!requireLoginUi("Vui lòng đăng nhập để thanh toán.")) return;
            if (!formValid(onlineForm)) return;
            const comboPairs = Object.entries(state.comboQuantities)
                .filter(([, q]) => q > 0)
                .map(([id, q]) => `${id}:${q}`)
                .join(",");
            const submitBtn = document.querySelector("[form=\"online-checkout-form\"]");
            submitBtn.disabled = true;
            try {
                // BR03: máy chủ chỉ nhận thanh toán online cho ghế đang được giữ bởi chính mình,
                // nên giữ (hoặc gia hạn giữ) ghế ngay trước khi thanh toán.
                const holdResp = await postJson("/api/booking/hold", {
                    showtimeId: state.selectedShowtimeId,
                    seatIds: state.selectedSeatIds.join(",")
                });
                const hold = await readJson(holdResp, { success: false, message: "Không giữ được ghế." });
                if (!hold.success) {
                    showBanner(hold.message, "error");
                    await loadSeatMap(state.selectedShowtimeId);
                    return;
                }
                const resp = await postJson("/api/booking/checkout", {
                    showtimeId: state.selectedShowtimeId,
                    customerName: $("cust-name-input").value,
                    customerPhone: $("cust-phone-input").value,
                    bookingChannel: "ONLINE",
                    paymentMethod: $("online-payment-method").value,
                    seatIds: state.selectedSeatIds.join(","),
                    comboSpec: comboPairs
                });
                const data = await readJson(resp, { success: false, message: "Thanh toán không thành công." });
                if (!data.success) {
                    showBanner(data.message, "error");
                    await loadSeatMap(state.selectedShowtimeId);
                    return;
                }
                showBanner(data.message, "success");
                state.selectedSeatIds = [];
                state.comboQuantities = {};
                clearInterval(state.holdCountdownTimer);
                $("hold-timer-badge").hidden = true;
                openTicketModal(data.invoice, data.tickets);
                await Promise.all([loadShowtimes(), loadCombos()]);
                await loadSeatMap(state.selectedShowtimeId);
                if (isPhone()) goStep(1);
            } finally {
                submitBtn.disabled = false;
            }
        });

        document.querySelectorAll("[data-step-go]").forEach((b) => {
            b.addEventListener("click", () => goStep(parseInt(b.dataset.stepGo, 10)));
        });
        goStep(1);
    }

    // =========================================================================
    // 9. BÁN TẠI QUẦY (CSD02)
    // =========================================================================
    async function populatePosForm() {
        if (!hasRole("STAFF", "MANAGER")) return;
        const stSelect = $("pos-showtime-select");
        const now = Date.now();
        const open = state.showtimes.filter((s) => {
            const end = parseTs(s.endTime);
            return !end || end.getTime() > now;
        });
        const list = open.length ? open : state.showtimes;
        const keep = state.pos.showtimeId && list.some((s) => s.showtimeId === state.pos.showtimeId)
            ? state.pos.showtimeId : (list[0] ? list[0].showtimeId : null);
        stSelect.innerHTML = list.map((s) =>
            `<option value="${s.showtimeId}" ${s.showtimeId === keep ? "selected" : ""}>${escapeHtml(fmtDateTime(s.startTime))} · ${escapeHtml(s.movieTitle)} · ${escapeHtml(roomShort(s.auditoriumName))} · còn ${s.availableSeats}</option>`
        ).join("");
        const comboSelect = $("pos-combo-select");
        const active = state.combos.filter((c) => c.status === "AVAILABLE" && c.stockQuantity > 0);
        const prevCombo = comboSelect.value;
        comboSelect.innerHTML = `<option value="">Không mua combo</option>` +
            active.map((c) => `<option value="${c.comboId}">${escapeHtml(c.comboCode)} · ${escapeHtml(c.name)} · ${escapeHtml(formatVnd(c.price))}</option>`).join("");
        if (prevCombo && active.some((c) => String(c.comboId) === prevCombo)) comboSelect.value = prevCombo;
        if (keep) await loadPosSeats(keep);
    }

    async function loadPosSeats(showtimeId) {
        if (state.pos.showtimeId !== showtimeId) state.pos.selected = [];
        state.pos.showtimeId = showtimeId;
        const resp = await apiFetch("/api/booking/seats?showtimeId=" + showtimeId);
        state.pos.seatMap = await readJson(resp, []);
        state.pos.selected = state.pos.selected.filter((id) => {
            const s = state.pos.seatMap.find((x) => x.showtimeSeatId === id);
            return s && s.status === "AVAILABLE";
        });
        renderPosSeats();
    }

    function renderPosSeats() {
        renderSeatMap($("pos-seat-grid"), state.pos.seatMap, {
            selected: state.pos.selected,
            myId: state.currentUser ? state.currentUser.userId : null,
            onToggle: (id) => {
                if (toggleIn(state.pos.selected, id)) {
                    renderPosSeats();
                    const btn = document.querySelector(`#pos-seat-grid [data-ss-id="${id}"]`);
                    if (btn) btn.focus();
                }
            }
        });
        updatePosTotal();
    }

    function updatePosTotal() {
        const chosen = state.pos.seatMap.filter((s) => state.pos.selected.includes(s.showtimeSeatId));
        const seatTotal = chosen.reduce((a, s) => a + s.finalPrice, 0);
        const combo = state.combos.find((c) => String(c.comboId) === $("pos-combo-select").value);
        const qty = Math.max(0, parseInt($("pos-combo-qty").value || "0", 10));
        $("pos-seat-summary").textContent = chosen.length
            ? `${chosen.map((s) => s.seatCode).join(", ")} · ${formatVnd(seatTotal)}` : "chưa chọn";
        $("pos-total").textContent = formatVnd(seatTotal + (combo ? combo.price * qty : 0));
    }

    function initPosHandlers() {
        $("pos-showtime-select").addEventListener("change", (e) => loadPosSeats(parseInt(e.target.value, 10)));
        $("pos-combo-select").addEventListener("change", updatePosTotal);
        $("pos-combo-qty").addEventListener("input", updatePosTotal);

        const posForm = $("pos-checkout-form");
        posForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (state.pos.selected.length === 0) {
                showBanner("Chọn ít nhất 1 ghế trống trên sơ đồ.", "error");
                return;
            }
            if (!formValid(posForm)) return;
            const comboId = $("pos-combo-select").value;
            const comboQty = parseInt($("pos-combo-qty").value || "0", 10);
            const submitBtn = posForm.querySelector("[type=submit]");
            submitBtn.disabled = true;
            try {
                const resp = await postJson("/api/booking/checkout", {
                    showtimeId: state.pos.showtimeId,
                    customerName: $("pos-customer-name").value,
                    customerPhone: "0900112233",
                    bookingChannel: "POS",
                    paymentMethod: $("pos-payment-method").value,
                    seatIds: state.pos.selected.join(","),
                    comboSpec: (comboId && comboQty > 0) ? `${comboId}:${comboQty}` : ""
                });
                const data = await readJson(resp, { success: false, message: "Không bán được vé." });
                if (!data.success) {
                    showBanner(data.message, "error");
                    await loadPosSeats(state.pos.showtimeId);
                    return;
                }
                showBanner(data.message, "success");
                const inv = data.invoice;
                $("pos-latest-receipt").innerHTML = `
                    <dl class="receipt-lines">
                        <div><dt>Hóa đơn</dt><dd>${escapeHtml(inv.invoiceCode)}</dd></div>
                        <div><dt>Khách</dt><dd>${escapeHtml(inv.customerName)}</dd></div>
                        <div><dt>Suất</dt><dd>${escapeHtml(inv.movieTitle)} · ${escapeHtml(roomShort(inv.auditoriumName))}</dd></div>
                        <div><dt>Vé</dt><dd>${data.tickets.map((t) => escapeHtml(t.ticketCode)).join(", ")}</dd></div>
                        <div><dt>Combo</dt><dd>${escapeHtml(inv.comboSummary)}</dd></div>
                        <div class="grand"><dt>Đã thu (${escapeHtml(PAYMENT_LABEL[inv.paymentMethod] || inv.paymentMethod)})</dt><dd>${escapeHtml(formatVnd(inv.totalAmount))}</dd></div>
                    </dl>`;
                state.pos.selected = [];
                openTicketModal(data.invoice, data.tickets);
                await Promise.all([loadShowtimes(), loadCombos()]);
                await populatePosForm();
            } finally {
                submitBtn.disabled = false;
            }
        });
    }

    // =========================================================================
    // 10. PHIM & LỊCH CHIẾU (CSD03)
    // =========================================================================
    function populateScheduleSelects() {
        const movieSel = $("sched-movie-id");
        const audSel = $("sched-auditorium-id");
        const keepMovie = movieSel.value;
        const keepAud = audSel.value;
        const schedulable = state.allMovies.filter((m) => m.status !== "ENDED");
        if (schedulable.length) {
            movieSel.innerHTML = schedulable.map((m) =>
                `<option value="${m.movieId}">${escapeHtml(m.title)} · ${escapeHtml(m.durationMinutes)} phút</option>`).join("");
            if (keepMovie) movieSel.value = keepMovie;
        }
        if (state.auditoriums.length) {
            audSel.innerHTML = state.auditoriums.map((a) =>
                `<option value="${a.auditoriumId}">${escapeHtml(a.name)} · ${escapeHtml(a.totalSeats)} ghế</option>`).join("");
            if (keepAud) audSel.value = keepAud;
        }
        const timeInput = $("sched-start-time");
        if (!timeInput.value) {
            const t = new Date(Date.now() + 86400000);
            timeInput.value = `${pad(t.getDate())}/${pad(t.getMonth() + 1)}/${t.getFullYear()} 20:30`;
        }
    }

    function renderShowtimesTable() {
        const tbody = $("showtimes-table-body");
        if (!tbody) return;
        if (!state.showtimes.length) {
            tbody.innerHTML = `<tr><td colspan="7" class="t-empty">Chưa có suất chiếu.</td></tr>`;
            return;
        }
        tbody.innerHTML = state.showtimes.map((s) => `
            <tr>
                <td class="t-code">SC${s.showtimeId}</td>
                <td>${escapeHtml(s.movieTitle)}<small>${escapeHtml(s.durationMinutes)} phút</small></td>
                <td>${escapeHtml(s.auditoriumName)}</td>
                <td>${escapeHtml(fmtDateTime(s.startTime))}</td>
                <td>${escapeHtml(fmtDateTime(s.endTime))}</td>
                <td class="num">${escapeHtml(formatVnd(s.basePrice))}</td>
                <td class="num">${escapeHtml(s.availableSeats)}/${escapeHtml(s.totalSeats)}</td>
            </tr>`).join("");
    }

    function populateTimelineDates() {
        const sel = $("timeline-date");
        if (!sel) return;
        const keys = Array.from(new Set(state.showtimes.map((s) => dayKey(s.startTime)))).sort();
        if (!keys.length) {
            sel.innerHTML = "";
            return;
        }
        if (!state.timelineDate || !keys.includes(state.timelineDate)) {
            const now = new Date();
            const today = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
            state.timelineDate = keys.includes(today) ? today : keys[0];
        }
        sel.innerHTML = keys.map((k) => `<option value="${k}" ${k === state.timelineDate ? "selected" : ""}>${escapeHtml(dayLabel(k))}</option>`).join("");
    }

    function renderTimeline() {
        const box = $("schedule-timeline");
        if (!box || !state.auditoriums.length) return;
        const DAY_START = 8;
        const DAY_HOURS = 16; // 08:00 → 24:00
        if (!state.timelineDate) return;
        const dayStart = new Date(state.timelineDate + "T00:00:00");
        dayStart.setHours(DAY_START);
        const span = DAY_HOURS * 3600000;
        const pct = (d) => Math.max(0, Math.min(100, ((d - dayStart) / span) * 100));

        let axis = `<div class="tl-axis" aria-hidden="true">`;
        for (let h = DAY_START; h <= DAY_START + DAY_HOURS; h += 2) {
            axis += `<span style="left:${((h - DAY_START) / DAY_HOURS) * 100}%">${pad(h)}:00</span>`;
        }
        axis += `</div>`;

        const rows = state.auditoriums.map((a) => {
            const shows = state.showtimes.filter((s) => s.auditoriumId === a.auditoriumId && dayKey(s.startTime) === state.timelineDate);
            const blocks = shows.map((s) => {
                const start = parseTs(s.startTime);
                const end = parseTs(s.endTime);
                const left = pct(start);
                const width = Math.max(0.6, pct(end) - left);
                const cleanShare = Math.min(100, (15 * 60000) / Math.max(1, end - start) * 100);
                const isNew = s.showtimeId === state.lastCreatedShowtimeId;
                const label = `${s.movieTitle}, ${fmtTime(s.startTime)} đến ${fmtTime(s.endTime)}, đã gồm 15 phút dọn phòng`;
                return `<div class="tl-block${isNew ? " is-new" : ""}" style="left:${left}%;width:${width}%" title="${escapeHtml(label)}" role="listitem" aria-label="${escapeHtml(label)}">
                    <span class="tl-play">${escapeHtml(s.movieTitle)}<small>${escapeHtml(fmtTime(s.startTime))}–${escapeHtml(fmtTime(s.endTime))}</small></span>
                    <span class="tl-clean" style="width:${cleanShare}%"></span>
                </div>`;
            }).join("");
            return `<div class="tl-row">
                <div class="tl-room">${escapeHtml(roomShort(a.name))}<small>${escapeHtml(a.roomType)}</small></div>
                <div class="tl-track" role="list" aria-label="Lịch ${escapeHtml(a.name)}">${blocks}</div>
            </div>`;
        }).join("");
        box.innerHTML = axis + rows;
    }

    async function submitShowtimeCreation(movieId, auditoriumId, startTime, basePrice) {
        const resp = await postJson("/api/showtimes", { movieId, auditoriumId, startTime, basePrice });
        const data = await readJson(resp, { success: false, message: "Không tạo được suất chiếu." });
        showBanner(data.message, data.success ? "success" : "error");
        if (data.success && data.showtime) {
            state.lastCreatedShowtimeId = data.showtime.showtimeId;
            state.timelineDate = dayKey(data.showtime.startTime);
            await loadShowtimes();
        }
    }

    function initScheduleHandlers() {
        $("timeline-date").addEventListener("change", (e) => {
            state.timelineDate = e.target.value;
            renderTimeline();
        });

        const createForm = $("create-showtime-form");
        createForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!formValid(createForm)) return;
            const start = parseViInput($("sched-start-time").value);
            if (!start) {
                showBanner("Ngày giờ không hợp lệ. Nhập dạng 15/11/2026 20:30.", "error");
                $("sched-start-time").focus();
                return;
            }
            await submitShowtimeCreation(
                $("sched-movie-id").value,
                $("sched-auditorium-id").value,
                start,
                $("sched-base-price").value
            );
        });

        $("btn-test-br01-conflict").addEventListener("click", async () => {
            if (!state.showtimes.length) return;
            const first = state.showtimes[0];
            $("sched-movie-id").value = String(first.movieId);
            $("sched-auditorium-id").value = String(first.auditoriumId);
            $("sched-start-time").value = toViInputValue(first.startTime);
            await submitShowtimeCreation(first.movieId, first.auditoriumId, first.startTime, 95000);
        });

        const movieForm = $("save-movie-form");
        movieForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!formValid(movieForm)) return;
            const resp = await postJson("/api/movies", {
                action: "save",
                title: $("movie-title-input").value,
                genre: $("movie-genre-input").value,
                durationMinutes: $("movie-duration-input").value,
                ageRating: $("movie-rating-input").value,
                status: $("movie-status-input").value,
                director: $("movie-director-input").value,
                castList: $("movie-cast-input").value
            });
            const data = await readJson(resp, { success: false, message: "Không lưu được phim." });
            showBanner(data.message, data.success ? "success" : "error");
            if (data.success) {
                $("movie-title-input").value = "";
                await Promise.all([loadAllMovies(), loadMovies($("search-movie-kw").value, $("filter-movie-status").value)]);
            }
        });
    }

    // =========================================================================
    // 11. COMBO F&B (CSD04)
    // =========================================================================
    function comboStatusTag(status) {
        if (status === "AVAILABLE") return `<span class="tag tag-ok">Đang bán</span>`;
        if (status === "OUT_OF_STOCK") return `<span class="tag">Hết hàng</span>`;
        return `<span class="tag tag-bad">Ngừng bán</span>`;
    }

    function renderCombosTable() {
        const tbody = $("combos-table-body");
        if (!tbody) return;
        if (!state.combos.length) {
            tbody.innerHTML = `<tr><td colspan="6" class="t-empty">Chưa có combo nào.</td></tr>`;
            return;
        }
        tbody.innerHTML = state.combos.map((c) => `
            <tr>
                <td class="t-code">${escapeHtml(c.comboCode)}</td>
                <td>${escapeHtml(c.name)}<small>${escapeHtml(c.itemsIncluded)}</small></td>
                <td class="num">${escapeHtml(formatVnd(c.price))}</td>
                <td class="num">${escapeHtml(c.stockQuantity)}</td>
                <td>${comboStatusTag(c.status)}</td>
                <td class="t-actions">
                    <button type="button" class="btn btn-quiet btn-sm" data-edit-combo="${c.comboId}">Sửa</button>
                    <button type="button" class="btn ${c.status === "AVAILABLE" ? "btn-danger" : "btn-quiet"} btn-sm" data-toggle-combo="${c.comboId}">
                        ${c.status === "AVAILABLE" ? "Ngừng bán" : "Mở bán"}
                    </button>
                </td>
            </tr>`).join("");

        tbody.querySelectorAll("[data-edit-combo]").forEach((btn) => {
            btn.addEventListener("click", () => {
                const c = state.combos.find((x) => x.comboId === parseInt(btn.dataset.editCombo, 10));
                if (!c) return;
                $("combo-id-input").value = c.comboId;
                $("combo-code-input").value = c.comboCode;
                $("combo-name-input").value = c.name;
                $("combo-items-input").value = c.itemsIncluded;
                $("combo-price-input").value = c.price;
                $("combo-stock-input").value = c.stockQuantity;
                $("combo-form-title").textContent = `Sửa combo ${c.comboCode}`;
                $("combo-name-input").focus();
            });
        });
        tbody.querySelectorAll("[data-toggle-combo]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const resp = await postJson("/api/combos", { action: "toggle", comboId: btn.dataset.toggleCombo });
                const data = await readJson(resp, { success: false, message: "Không cập nhật được combo." });
                showBanner(data.message, data.success ? "success" : "error");
                await loadCombos();
            });
        });
    }

    function resetComboForm() {
        $("combo-id-input").value = "0";
        $("combo-code-input").value = "";
        $("combo-name-input").value = "";
        $("combo-items-input").value = "1 Bắp phô mai (XL) + 2 Pepsi (L)";
        $("combo-price-input").value = "125000";
        $("combo-stock-input").value = "100";
        $("combo-form-title").textContent = "Thêm hoặc sửa combo";
    }

    function initComboHandlers() {
        resetComboForm();
        const form = $("save-combo-form");
        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!formValid(form)) return;
            const resp = await postJson("/api/combos", {
                action: "save",
                comboId: $("combo-id-input").value,
                comboCode: $("combo-code-input").value,
                name: $("combo-name-input").value,
                itemsIncluded: $("combo-items-input").value,
                price: $("combo-price-input").value,
                stockQuantity: $("combo-stock-input").value
            });
            const data = await readJson(resp, { success: false, message: "Không lưu được combo." });
            showBanner(data.message, data.success ? "success" : "error");
            if (data.success) {
                resetComboForm();
                await loadCombos();
            }
        });
        $("btn-reset-combo-form").addEventListener("click", resetComboForm);
    }

    // =========================================================================
    // 12. SOÁT VÉ & DOANH THU (CSD05)
    // =========================================================================
    const GATE_TEXT = {
        CHECKIN_SUCCESS: { cls: "status-ok", text: "Hợp lệ · mời vào" },
        ALREADY_CHECKED_IN: { cls: "status-low", text: "Đã soát rồi" },
        TICKET_CANCELLED: { cls: "status-out", text: "Vé đã hủy" },
        TICKET_NOT_FOUND: { cls: "status-out", text: "Không tồn tại" },
        EMPTY_CODE: { cls: "status-past", text: "Thiếu mã" }
    };

    async function performQrCheckin(code) {
        const resp = await postJson("/api/booking/checkin", { code });
        const data = await readJson(resp, { success: false, code: "ERROR", message: "Không soát được vé." });
        const box = $("checkin-result-box");
        const verdict = GATE_TEXT[data.code] || { cls: data.success ? "status-ok" : "status-out", text: data.success ? "Hợp lệ" : "Từ chối" };
        const t = data.ticket;
        box.className = "gate-result " + (data.success ? "is-valid" : "is-invalid");
        box.innerHTML = `
            <div class="gate-verdict">
                <span class="status ${verdict.cls}">${escapeHtml(verdict.text)}</span>
                ${t ? `<span class="flap" aria-hidden="true">${flapMarkup(t.seatCode)}</span>` : ""}
            </div>
            ${t ? `<p>${escapeHtml(t.movieTitle)} · ${escapeHtml(t.auditoriumName)} · ${escapeHtml(fmtDateTime(t.startTime))}</p>` : ""}
            <p class="gate-detail">${escapeHtml(data.message)}</p>`;
        await loadDashboardAndTickets();
    }

    function initCheckinHandlers() {
        const form = $("qr-checkin-form");
        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!formValid(form)) return;
            await performQrCheckin($("qr-code-input").value.trim());
        });
        document.querySelectorAll("[data-sample-qr]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                $("qr-code-input").value = btn.dataset.sampleQr;
                await performQrCheckin(btn.dataset.sampleQr);
            });
        });
    }

    function ticketStatusTag(status) {
        if (status === "ISSUED") return `<span class="tag tag-dark">Chưa soát</span>`;
        if (status === "CHECKED_IN") return `<span class="tag tag-ok">Đã soát</span>`;
        if (status === "CANCELLED") return `<span class="tag tag-bad">Đã hủy</span>`;
        return `<span class="tag">${escapeHtml(status)}</span>`;
    }

    async function loadDashboardAndTickets() {
        if (!hasRole("STAFF", "MANAGER")) return;
        const isManager = hasRole("MANAGER");
        const [repResp, tktResp] = await Promise.all([
            isManager ? apiFetch("/api/reports/dashboard") : Promise.resolve(null),
            apiFetch("/api/booking/tickets")
        ]);
        const rep = repResp && repResp.ok ? await readJson(repResp, null) : null;
        const tickets = tktResp.ok ? await readJson(tktResp, []) : [];

        const kpiBoard = document.querySelector(".kpi-board");
        kpiBoard.classList.toggle("is-locked", !rep);
        $("kpi-locked").hidden = !!rep;
        const movieBody = $("movie-rev-table-body");
        if (!rep) {
            ["kpi-total-revenue", "kpi-seat-revenue", "kpi-combo-revenue"].forEach((id) => flap($(id), "— ₫"));
            flap($("kpi-tickets-count"), "—");
            $("kpi-channel-split").textContent = "Online — · Quầy —";
            $("kpi-checkin-breakdown").textContent = "Đã soát — · Đã hủy —";
            movieBody.innerHTML = `<tr><td colspan="5" class="t-empty">Báo cáo doanh thu chỉ hiển thị với tài khoản Quản lý rạp.</td></tr>`;
        } else {
            flap($("kpi-total-revenue"), formatVnd(rep.totalRevenue));
            flap($("kpi-seat-revenue"), formatVnd(rep.seatRevenue));
            flap($("kpi-combo-revenue"), formatVnd(rep.comboRevenue));
            flap($("kpi-tickets-count"), String(rep.ticketsSold));
            $("kpi-channel-split").textContent = `Online ${formatVnd(rep.onlineRevenue)} · Quầy ${formatVnd(rep.posRevenue)} · ${rep.totalInvoices} hóa đơn`;
            $("kpi-checkin-breakdown").textContent = `Đã soát ${rep.checkedInTickets} · Đã hủy ${rep.cancelledTickets}`;
            const max = Math.max(1, ...rep.movieBreakdown.map((m) => m.revenue));
            movieBody.innerHTML = rep.movieBreakdown.map((m) => `
                <tr>
                    <td>${escapeHtml(m.title)}</td>
                    <td class="num">${escapeHtml(m.showtimeCount)}</td>
                    <td class="num">${escapeHtml(m.ticketsSold)}</td>
                    <td class="num">${escapeHtml(formatVnd(m.revenue))}</td>
                    <td class="bar-col"><span class="bar" aria-hidden="true"><span style="width:${(m.revenue / max) * 100}%"></span></span></td>
                </tr>`).join("");
        }

        const tktBody = $("tickets-table-body");
        if (!tickets.length) {
            tktBody.innerHTML = `<tr><td colspan="7" class="t-empty">Chưa có vé nào được phát hành.</td></tr>`;
            return;
        }
        tktBody.innerHTML = tickets.map((t) => `
            <tr>
                <td class="t-code">${escapeHtml(t.ticketCode)}</td>
                <td>${escapeHtml(t.customerName)}</td>
                <td>${escapeHtml(t.movieTitle)}<small>${escapeHtml(roomShort(t.auditoriumName))} · ${escapeHtml(fmtDateTime(t.startTime))}</small></td>
                <td class="t-code">${escapeHtml(t.seatCode)}<small>${escapeHtml(SEAT_TIER_LABEL[t.seatType] || t.seatType)}</small></td>
                <td class="num">${escapeHtml(formatVnd(t.ticketPrice))}</td>
                <td>${ticketStatusTag(t.ticketStatus)}</td>
                <td class="t-actions">
                    ${t.ticketStatus === "ISSUED" ? `
                        <button type="button" class="btn btn-quiet btn-sm" data-quick-checkin="${escapeHtml(t.ticketCode)}">Soát</button>
                        <button type="button" class="btn btn-danger btn-sm" data-cancel-ticket="${escapeHtml(t.ticketCode)}">Hủy vé</button>` : ""}
                </td>
            </tr>`).join("");

        tktBody.querySelectorAll("[data-quick-checkin]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                $("qr-code-input").value = btn.dataset.quickCheckin;
                await performQrCheckin(btn.dataset.quickCheckin);
                $("checkin-result-box").scrollIntoView({ block: "nearest", behavior: reduceMotion.matches ? "auto" : "smooth" });
            });
        });
        tktBody.querySelectorAll("[data-cancel-ticket]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const code = btn.dataset.cancelTicket;
                if (!window.confirm(`Hủy vé ${code}? Ghế sẽ được trả về trạng thái trống.`)) return;
                const resp = await postJson("/api/booking/cancel", { ticketCode: code });
                const data = await readJson(resp, { success: false, message: "Không hủy được vé." });
                showBanner(data.message, data.success ? "success" : "error");
                await loadShowtimes();
                await Promise.all([loadSeatMap(state.selectedShowtimeId), loadDashboardAndTickets()]);
            });
        });
    }

    // =========================================================================
    // 13. KHỞI ĐỘNG
    // =========================================================================
    function initAuthHandlers() {
        document.querySelectorAll("[data-quick-login]").forEach((chip) => {
            chip.addEventListener("click", () => {
                const [username, password] = chip.dataset.quickLogin.split(":");
                performLogin(username, password);
            });
        });
        $("btn-logout").addEventListener("click", performLogout);
        const loginForm = $("login-dialog-form");
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!formValid(loginForm)) return;
            const ok = await performLogin($("login-username-input").value, $("login-password-input").value);
            if (ok) {
                $("login-modal").close();
                $("login-password-input").value = "";
            }
        });
    }

    document.addEventListener("DOMContentLoaded", async () => {
        initFormValidation();
        initDialogs();
        initTabs();
        initAuthHandlers();
        initBookingHandlers();
        initPosHandlers();
        initScheduleHandlers();
        initComboHandlers();
        initCheckinHandlers();
        restoreSession();
        tickClock();
        setInterval(tickClock, 15000);
        await validateSession();
        await loadInitialData();
    });
})();
