---
name: CineHub CMS
description: One cinema's schedule as a split-flap departures board on a light stone concourse.
colors:
  ground: "#E6E1D6"
  paper: "#F3EFE6"
  paper-hi: "#FBF9F4"
  line: "#CFC8BA"
  line-soft: "#DDD7CB"
  ink: "#1C1D20"
  ink-2: "#4A463F"
  ink-3: "#625D53"
  board: "#17191C"
  board-2: "#24272B"
  board-3: "#2E3237"
  board-line: "#33373C"
  board-ink: "#F3EFE6"
  board-mute: "#A9ACB2"
  hinge: "#0C0D0F"
  signal: "#F2C230"
  signal-hover: "#E5B21C"
  signal-ink: "#17191C"
  ok: "#23744A"
  warn: "#E08A1E"
  bad: "#B8322A"
  ok-soft: "#DCEBDF"
  bad-soft: "#F3D9D5"
  tier-vip: "#24396F"
  tier-couple: "#7B2D6B"
  seat-line: "#8F897C"
typography:
  display:
    fontFamily: "Barlow Condensed, Arial Narrow, Roboto Condensed, Segoe UI, sans-serif"
    fontSize: "30px"
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: "0.01em"
  headline:
    fontFamily: "Barlow Condensed, Arial Narrow, Roboto Condensed, Segoe UI, sans-serif"
    fontSize: "26px"
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: "0.01em"
  title:
    fontFamily: "Barlow Condensed, Arial Narrow, Roboto Condensed, Segoe UI, sans-serif"
    fontSize: "19px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0.04em"
  body:
    fontFamily: "Be Vietnam Pro, Segoe UI, system-ui, -apple-system, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.5
    fontFeature: "tnum"
  label:
    fontFamily: "Barlow Condensed, Arial Narrow, Roboto Condensed, Segoe UI, sans-serif"
    fontSize: "12px"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "0.14em"
  numeral:
    fontFamily: "Barlow Condensed, Arial Narrow, Roboto Condensed, Segoe UI, sans-serif"
    fontSize: "21px"
    fontWeight: 600
    lineHeight: 1
    fontFeature: "tnum"
  code:
    fontFamily: "Barlow Condensed, Arial Narrow, Roboto Condensed, Segoe UI, sans-serif"
    fontSize: "11px"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "0.08em"
rounded:
  glyph: "2px"
  sm: "4px"
  md: "8px"
  lg: "12px"
spacing:
  field: "6px"
  rhythm: "14px"
  section: "20px"
  gutter: "clamp(16px, 2.2vw, 32px)"
components:
  button-signal:
    backgroundColor: "{colors.signal}"
    textColor: "{colors.signal-ink}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "40px"
  button-signal-hover:
    backgroundColor: "{colors.signal-hover}"
  button-dark:
    backgroundColor: "{colors.board}"
    textColor: "{colors.board-ink}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "40px"
  button-dark-hover:
    backgroundColor: "{colors.board-3}"
  button-quiet:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "40px"
  button-quiet-hover:
    backgroundColor: "{colors.paper-hi}"
  tab:
    backgroundColor: "transparent"
    textColor: "{colors.ink-2}"
    rounded: "{rounded.md}"
    padding: "8px 12px"
  tab-active:
    backgroundColor: "{colors.board}"
    textColor: "{colors.board-ink}"
  input:
    backgroundColor: "{colors.paper-hi}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    padding: "8px 12px"
    height: "40px"
  panel:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: "20px 22px"
  board:
    backgroundColor: "{colors.board}"
    textColor: "{colors.board-ink}"
    rounded: "{rounded.lg}"
    padding: "16px 14px 10px"
  flap-glyph:
    backgroundColor: "{colors.board-2}"
    textColor: "{colors.board-ink}"
    typography: "{typography.numeral}"
    rounded: "{rounded.glyph}"
  status-ok:
    backgroundColor: "{colors.ok}"
    textColor: "#FFFFFF"
    typography: "{typography.label}"
    rounded: "{rounded.glyph}"
  status-low:
    backgroundColor: "{colors.warn}"
    textColor: "{colors.signal-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.glyph}"
  status-out:
    backgroundColor: "{colors.bad}"
    textColor: "#FFFFFF"
    typography: "{typography.label}"
    rounded: "{rounded.glyph}"
  seat:
    backgroundColor: "{colors.paper-hi}"
    textColor: "{colors.ink-2}"
    width: "38px"
    height: "34px"
  seat-selected:
    backgroundColor: "{colors.signal}"
    textColor: "{colors.signal-ink}"
  stub:
    backgroundColor: "{colors.board}"
    textColor: "{colors.board-ink}"
    rounded: "{rounded.lg}"
    padding: "18px 20px"
---

# Design System: CineHub CMS

## Overview

**Creative North Star: "The Departures Board"**

Every showtime, auditorium, seat count and sale status lives on matte black split-flap boards set into a light stone concourse. The board is the system's one loud object: dark, condensed, all-caps, hinged glyph tiles that flip when a value changes. Everything around it (paper panels, forms, tables, dialogs) is quiet concourse furniture in warm off-whites and hairline borders, so the eye always lands on the board first.

The density is operational, not promotional. Customers, counter staff and the manager all work in the same board-and-panel grammar: read the board, pick a row, act in the panel beside it, settle on a black ticket stub. Colour is rationed: signal yellow means "your selection / your next action"; green, amber and red appear only where they mean on sale, few left, sold out; navy and plum appear only as seat tiers. The world refuses the cinema category default of a black poster carousel with a red buy button.

**Key Characteristics:**
- Light stone ground with paper panels; black flap boards as the focal surfaces.
- Split-flap glyph tiles with a hinge line for times, counts, totals, countdowns and KPIs.
- Signal yellow is the only accent; status and tier colours are semantic, never decorative.
- Barlow Condensed uppercase for anything a station board would print; Be Vietnam Pro for everything you read in sentences.
- Tabular figures everywhere; money and counts right-aligned.
- Seat state carried by form (stroke, weight, pattern), with colour as a second channel.

## Colors

A warm-neutral concourse palette with one signal yellow and a small quarantined set of semantic hues.

### Primary
- **Signal Yellow** (signal): the current selection and the primary action: the pay button, selected seats, the selected board row's flap tiles, the active mobile step, focus halos, text selection, totals and KPI glyphs on the board. Hover deepens to **Signal Amber** (signal-hover). Text on yellow is always **Board Ink-Black** (signal-ink).

### Secondary
- **Tier Navy** (tier-vip): VIP seats only (stroke and seat number).
- **Tier Plum** (tier-couple): Couple / Sweetbox seats only.

### Tertiary (status)
- **Platform Green** (ok): "on sale" status flaps, valid check-in ring. Soft form (ok-soft) behind success tags.
- **Caution Amber** (warn): "few left" status flaps and the urgent hold-countdown glyphs.
- **Stop Red** (bad): "sold out" flaps, invalid check-in ring, danger buttons, field errors. Soft form (bad-soft) behind error tags and danger hover.

### Neutral
- **Stone Concourse** (ground): page background, table header band, perforation notches.
- **Ticket Paper** (paper): panels, top bar, dialogs, mobile step bar.
- **Bright Paper** (paper-hi): inputs, seats at rest, hover wash on rows and quiet buttons, timeline tracks.
- **Hairline** (line) and **Soft Hairline** (line-soft): panel and input borders; dividers, tags, bars.
- **Ink** (ink), **Ink 2** (ink-2), **Ink 3** (ink-3): primary text, labels and secondary text, meta and code labels. Ink 3 is the lightest text allowed on paper (AA).
- **Flap Black** (board), **Flap Tile** (board-2), **Flap Raised** (board-3), **Board Rule** (board-line): the board family: board surface, glyph tiles and dark inputs, selected row and hover on dark, row dividers.
- **Board Ink** (board-ink) and **Board Mute** (board-mute): text on the board; mute for column heads, sublines and labels.
- **Hinge** (hinge): the 1px line through every flap glyph and the brand mark.
- **Seat Stroke** (seat-line): standard seat outline, hover border on light controls, scrollbar thumb.

### Named Rules
**The One Signal Rule.** Signal yellow marks only what is selected or what to press next. It never decorates a heading, a border or an empty state. Board-section titles printed in yellow on black (the board head, the stub head, KPI board title) are the single typographic exception, because the board world prints them that way.

**The Colour-Means-State Rule.** Green, amber and red appear only as sale or validity status; navy and plum only as seat tiers. If a colour does not answer "what state is this?", it is not on the palette.

## Typography

**Display Font:** Barlow Condensed (with Arial Narrow, Roboto Condensed, Segoe UI)
**Body Font:** Be Vietnam Pro (with Segoe UI, system-ui)
**Label/Numeral Font:** Barlow Condensed, weights 500/600/700

**Character:** Barlow Condensed is the station board's voice: narrow uppercase capitals and numerals that read at a glance. Be Vietnam Pro carries Vietnamese UI copy with full diacritic support and calm proportions. Body sets `font-variant-numeric: tabular-nums` globally.

### Hierarchy
- **Display** (700, 30px, 1.1, uppercase): page heading per tab; 24px under 640px, 22px on the phone booking flow.
- **Headline** (700, 26px, 1.1, uppercase): the selected show's title above the seat map. Dialog titles sit between at 22px.
- **Title** (700, 19px, 1.2, 0.04em, uppercase): panel headings; board and stub heads are 15–18px with 0.14–0.18em tracking.
- **Body** (400, 15px, 1.5): all UI copy, lede capped at 70ch; 14.5px on phones. Meta text 13.5px in Ink 3.
- **Label** (600, 11–12px, 0.1–0.16em, uppercase): board column heads, day dividers, KPI labels, table headers, status flaps, tags.
- **Numeral** (600, 16–42px by context): flap glyphs. Board rows 21px, totals 30px, KPI clamp(30px, 3vw, 42px).
- **Code** (600, 11px, 0.08em): subsystem and business-rule codes (CSD01–05, BR01–05, R01), 10px inside tabs.

### Named Rules
**The Board Prints, the Panel Speaks Rule.** Anything a departures board would print (times, rooms, counts, statuses, film titles on the board, section heads) is Barlow Condensed uppercase. Sentences, form labels, buttons and helper text are Be Vietnam Pro in sentence case.

**The Code-After Rule.** BR/CSD codes are small secondary labels set inline after a heading or tab label, in Ink 3 (Board Mute on dark, Signal on an active tab). They never sit above a heading as an eyebrow.

**The Tabular Rule.** Money, time and counts use tabular figures and right-align in tables, stubs, receipts and summaries.

## Layout

A fluid workspace capped at 1680px with a gutter of clamp(16px, 2.2vw, 32px). Panels stack and sit side by side with a 20px gap; inside forms, fields step on a 14px rhythm with 6px between label and control; action rows gap 10px with 20px above.

The booking tab is a board-and-panel corridor:
- **≥1360px:** three columns, board 35fr (min 420px) · seats 39fr · stub cart 26fr (min 340px). The stub is sticky; its body scrolls and its pay bar is pinned.
- **1024–1359px:** board on the left (5fr, min 360px) spanning two rows; seats over cart on the right (7fr). The stub turns horizontal: details and perforation on the left, combos and pay on the right.
- **<1024px:** single column, board → seats → cart; POS, combo and gate layouts also collapse to one column.
- **<720px:** the stub returns to vertical; KPI grid becomes one column.
- **<640px:** one booking step per screen (board, seats, cart) driven by a fixed bottom step bar of three 50px steps; the workspace reserves 96px at the bottom.

The top bar is 64px minimum and sticky. Below 1500px the role tabs wrap to a full-width second row under brand and account.

Other surfaces reuse the same split: POS 1.5fr seat map | 1fr sale panel; combos 2fr form | 3fr table; gate 1fr scan | 1.2fr KPI board. The seat map uses a container query so seats size to their panel, not the viewport.

## Elevation & Depth

A light hybrid. Paper panels sit almost flat with one soft ambient shadow; black boards sit heavier and carry an inset bezel (1px black, then a 5px Flap Raised rim) so they read as mounted hardware. Within the board, depth is tonal: Flap Tile glyphs on Flap Black, Flap Raised for the selected row.

### Shadow Vocabulary
- **Panel** (`box-shadow: 0 1px 2px rgba(23,25,28,0.06), 0 10px 28px -18px rgba(23,25,28,0.35)`): every paper panel.
- **Board** (`box-shadow: 0 2px 3px rgba(12,13,15,0.25), 0 18px 40px -22px rgba(12,13,15,0.6)`): boards, the stub, toasts; boards add `inset 0 0 0 1px #000, inset 0 0 0 5px var(--board-3)`.
- **Dialog** (`box-shadow: 0 30px 80px -20px rgba(12,13,15,0.55)`): modal dialogs over a 55% Flap Black backdrop.
- **Pay bar lift** (`box-shadow: 0 -12px 16px -12px rgba(0,0,0,0.7)`): the pinned pay bar over the scrolling stub body.

### Named Rules
**The Mounted Board Rule.** Only black boards (schedule board, KPI board) get the inset bezel. Paper stays paper.

## Shapes

Corners are modest and graded by object size: 2px on flap glyphs, status flaps and age badges; 3–4px on tags, code chips, selected board rows and timeline blocks; 8px on buttons, inputs, tabs, tables and tickets; 12px on panels, boards, the stub and dialogs. Seats have a chair silhouette: rounder top than bottom (7px 7px 4px 4px; 5px 5px 3px 3px on phones). Ticket objects carry a perforation: a 2px dashed rule with circular Stone Concourse notches cut into both edges. The cinema screen is a 3px Flap Black arc.

## Components

### Buttons
Firm, plain, 40px tall.
- **Shape:** gently rounded (8px).
- **Signal:** Signal Yellow on Board Ink-Black with a darker amber hairline (#C99A10), 600 weight 14px; the primary action only (pay, confirm). Large variant 48px.
- **Dark:** Flap Black with Board Ink; hover to Flap Raised.
- **Quiet:** transparent with Hairline border; hover Bright Paper and Seat Stroke border.
- **Danger:** transparent, Stop Red text, pale red border; hover Soft Red.
- **Outline light:** for dark surfaces; hover turns border and text Signal Yellow.
- **States:** press nudges 1px down; disabled 50% opacity. Focus everywhere is a 2px Flap Black outline plus a 5px Signal Yellow halo.

### Chips and Tags
- **Role-switch chips:** 13px pills inside a Stone Concourse tray; active is Flap Black with Signal text.
- **Tags:** Barlow 11.5px uppercase on Soft Hairline; ok/bad variants on their soft tints with deep text; dark variant Flap Black with Signal.

### Cards / Containers
- **Corner Style:** 12px.
- **Background:** Ticket Paper, Hairline border, Panel shadow.
- **Internal Padding:** 20px 22px.
- Panel headings are Title type with optional inline meta in body type.

### Inputs / Fields
- **Style:** Bright Paper, Hairline border, 8px radius, 40px tall; custom chevron on selects.
- **Focus:** border Flap Black plus a 3px Signal Yellow ring.
- **Error:** Stop Red border and 12.5px message. On dark (inside the stub) fields switch to Flap Tile with a #41454B border.
- The gate's ticket-code field uses Barlow 18px at 46px tall for fast scanning.

### Navigation
Top bar on Ticket Paper with a hairline bottom. Brand mark is a 32px Flap Black tile with a yellow condensed glyph and a hinge line. Tabs are Be Vietnam Pro 500 in Ink 2; hover Soft Hairline; active is Flap Black with Board Ink and its code label in Signal. On phones the bottom step bar replaces in-flow navigation for booking.

### Split-Flap Display (signature)
Each character is its own tile (Flap Tile, 2px radius) with a 1px Hinge line through the middle, spaces render as gaps. When a value changes, only the changed glyphs flip: a 240ms rotateX from -88deg with a brief brightness flash, ease-out (cubic-bezier(0.16, 1, 0.3, 1)), staggered 20ms per changed glyph and capped at 40ms so any flip settles within 280ms. Unchanged glyphs never move. Under `prefers-reduced-motion` no glyph flips. The flap carries an aria-label with the plain value. In the selected board row the tiles turn Signal Yellow; total and KPI glyphs print yellow on dark; urgent countdown glyphs turn Caution Amber.

### Departures Board
Grid of time flap (74px) · film title and subline · room · seats (right-aligned) · status flap (86px), with day dividers in Label type. Rows divide with Board Rule hairlines; hover lifts to #1F2226; the selected row is Flap Raised with a 1px Signal edge and yellow flap tiles.

### Seat Map
Seats encode state by form first:
- **Standard:** 2px Seat Stroke outline on Bright Paper.
- **VIP:** Tier Navy outline with a 5px top edge.
- **Couple:** Tier Plum, rounder top and 4px bottom edge, paired with its partner and a 2px joint.
- **Selected:** Signal Yellow fill with Flap Black stroke.
- **Mine (held by you):** Signal fill with a 4px double Flap Black border.
- **Held by others:** dashed grey outline, transparent.
- **Booked:** 135deg hatching with a diagonal strike, disabled.
A legend with mini seats and tier prices sits under a hairline below the map.

### Ticket Stub Cart
A Flap Black stub: top section with the show details, then a dashed perforation with notches, then a slightly lighter body (#1D2023) holding combo steppers, customer fields, and a pay bar with totals (grand total as a yellow flap) and the Signal pay button. At ≥1360px the pay bar is pinned while the body scrolls. Issued e-tickets reuse the stub: dark main with the time flap, a dashed tear line, and a Bright Paper QR panel.

### Schedule Timeline
Per-auditorium tracks on Bright Paper with a 16-step hairline grid. Show blocks are Flap Black with condensed uppercase titles and a Signal time line; the cleaning buffer that follows each show is a hatched segment; a newly added show turns Signal Yellow.

## Do's and Don'ts

### Do:
- **Do** put live schedule, count, total and countdown values in split-flap tiles and update them through the flip so only changed glyphs move.
- **Do** keep Signal Yellow (#F2C230) for the current selection and the next action; one Signal button per decision.
- **Do** encode seat state by stroke, weight, pattern and pairing, with colour as the second channel.
- **Do** set money, time and counts in tabular figures and right-align them.
- **Do** place BR/CSD codes inline after the heading or tab label as small Code type.
- **Do** keep the total and pay action visible: pinned pay bar on wide screens, own step on phones.
- **Do** use the perforation (dashed rule with notches) wherever a ticket or receipt is torn.

### Don't:
- **Don't** use status green, amber or red as decoration, and don't use tier navy or plum outside seats.
- **Don't** flip glyphs that did not change, let a flip run past 300ms (240ms glyph + at most 40ms stagger), or animate under reduced motion.
- **Don't** set code labels, kickers or eyebrows above headings.
- **Don't** apply the board bezel or black board surface to ordinary forms and tables; they stay on paper.
- **Don't** rely on colour alone to tell a booked seat from an available one.
- **Don't** build a black poster carousel with a red buy button; the board is the hero.
