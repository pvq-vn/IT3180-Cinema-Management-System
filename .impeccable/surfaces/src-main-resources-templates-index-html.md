---
version: 1
slug: "src-main-resources-templates-index-html"
primary_target: "src/main/resources/templates/index.html"
related_targets: ["src/main/resources/static/css/style.css","src/main/resources/static/js/app.js"]
---

# Surface brief: CineHub app shell (index.html)

Scope: the single-page CineHub CMS app (all five tabs: online booking, POS, films & showtimes, F&B combos, QR check-in & revenue). Mode: Operate. Build path: code-led.

Audience & job: customers pick a showtime and seats and pay; counter staff sell at POS and scan tickets fast; the manager schedules shows and reads revenue. Subsystem/BR codes stay as small secondary labels for evaluators.

Constraints: vanilla HTML/CSS/JS, no build; Google Fonts with system fallback; Vietnamese copy; WCAG AA; server RBAC already decides what each role sees.

## Direction contract

THESIS: The showtime schedule is a station departures board: every show, auditorium, seats left and sale status sits in one dark split-flap board on a light concourse, and values flip when they change. Refuses the category default of a black poster carousel with a red button.

OWN-WORLD: Light stone concourse ground (#E6E1D6) with paper-white panels; matte black flap boards (#17191C) with warm-white condensed capitals and a hinge line through each glyph tile; signal yellow (#F2C230) for the current selection and primary action; status flaps green / amber / red only where they mean on sale / few left / sold out. Barlow Condensed for board, numerals and labels; Be Vietnam Pro for UI text. Tabular figures everywhere money, time or counts appear.

STORY: A visitor reads today's board, sees at once what is on, where, and how full; picks a row; the seat map and a ticket-stub cart open beside it; hold, pay, receive QR stubs. Staff and the manager work in the same board-and-panel grammar.

FIRST VIEWPORT: 64px light top bar (wordmark, role tabs with small CSD codes, user pill). Below, two columns: left 40% the departures board of showtimes (time flaps, film, room, seats, status), selected row marked by a yellow edge; right the selected show's panel: title, meta, screen, seat grid, legend, then a black stub cart with perforation and yellow total plus "Thanh toán". Film filter sits above the board.

FORM: assigned direction, candidate 7 of 7 on my resonance list (split-flap departures board); seed:d60c172bdb1695df. Raises kept: seat state by form not color (emission rail); focused auditorium corridor with context (transit); color quarantined to status and seat tiers (lexicon); tabular right-aligned numbers (j-card); one step per screen on phones (feed). Signature interaction: split-flap flip on time, seat-count and countdown glyphs when values change, settled in under 300ms, disabled under reduced motion.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Recorded deviations (after finish review round 1)

- FIRST VIEWPORT at ≥1360px uses three columns (board 37% · seats 39% · stub cart 24%) instead of the stub stacked under the seat grid. Reason: with the stub under the grid, the total and "Thanh toán" fall below the fold at 1440×900; a dedicated sticky stub column keeps the total and pay action always visible (its body scrolls, the pay bar is pinned). Between 1024 and 1359px the build matches the contract (board | seats over cart).
- Top bar wraps tabs to a second row below 1500px: brand + five role tabs + demo role switch + user + logout do not fit 64px at 1440 without truncating tab labels.
