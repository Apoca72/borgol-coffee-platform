# Parity Sprint Design — Borgol Coffee Platform
**Date:** 2026-04-14  
**Status:** Approved  
**Scope:** Web/desktop feature parity + seed data enrichment

---

## 1. Background

Borgol is a dual-platform coffee community app: a JavaFX desktop app and a web SPA (`index.html`). All panes exist on both platforms **except** the Brew Timer, which is a separate page (`brew-timer.html`) on the web instead of an embedded SPA tab. Two desktop-only UI affordances (QuickBrewOverlay, recipe→timer data handoff) are also missing from the web. Seed data lacks real specialty coffee content and local cafe listings.

---

## 2. Gap Audit (confirmed)

| Gap | Desktop | Web | Priority |
|-----|---------|-----|----------|
| Brew Timer | Embedded `Timer` pane in main window | Separate `brew-timer.html` page — nav link leaves SPA | Critical |
| Recipe → Timer handoff | "Use in Timer" loads recipe steps into Timer pane | Link navigates to `brew-timer.html` (no data passed) | Critical |
| QuickBrewOverlay | Modal from feed/recipe cards with step-by-step + progress | Missing | Medium |
| Specialty bean data | — | — | Content |
| UB cafe seed data | — | — | Content |
| Drink category learn content | — | — | Content |

---

## 3. Fix 1 — Brew Timer as embedded SPA tab

### What changes
- Add `<div class="page" id="page-timer">` to `index.html` containing the full timer UI (method picker, ratio calculator, ring timer, step list, done banner).
- Change the navbar Timer link from `<a href="brew-timer.html">` to `<button class="nav-link" onclick="showPage('timer')">` — same pattern as all other nav buttons.
- Update mobile nav similarly.
- Extract timer CSS from `brew-timer.html` and merge into `index.html`'s `<style>` block (prefixed to avoid collisions with existing classes where needed).
- Extract timer JS from `brew-timer.html` and merge into `index.html`'s script section as a self-contained `timerModule` block.
- `brew-timer.html` is **kept** as a standalone page for bookmarks/direct links, but is no longer the primary entry point.

### Recipe → Timer handoff
- Feed cards and recipe detail modal already have a "Use in Timer" button. Change its handler from `window.location = 'brew-timer.html'` to:
  ```js
  loadRecipeIntoTimer(recipe); // sets steps, title, notes in timer module
  showPage('timer');
  ```
- `loadRecipeIntoTimer(recipe)` populates the step list from `recipe.instructions` (newline-split), sets a header with the recipe title, and resets timer state.

---

## 4. Fix 2 — QuickBrewOverlay in web

### What it does
A lightweight modal triggered from feed cards and the recipe detail modal via an "Easy Make" / "Quick Steps" button.

### Behaviour
- Shows recipe instructions as numbered steps, one section at a time (no timer running).
- Progress bar at top shows position in steps.
- Prev / Next navigation buttons.
- "Start Full Timer" button at the bottom — calls `loadRecipeIntoTimer(recipe)` + `showPage('timer')` + closes modal.
- Closes on backdrop click or ✕ button.

### Implementation
- Single `<div class="modal-overlay" id="quick-brew-modal">` added to `index.html`.
- JS function `openQuickBrew(recipe)` — parses instructions, renders steps, opens modal.
- CSS reuses existing `.modal-overlay` / `.modal-content` styles from `index.html`.

---

## 5. Fix 3 — Seed data enrichment

### 5a. Specialty coffee bean articles (6 entries)
Added as `LearnArticle` entries via `seedStaticContent()`, category `"Beans"`:

| Bean | Origin | Roast | Flavor profile |
|------|--------|-------|----------------|
| Ethiopian Yirgacheffe | Ethiopia | Light | Floral, blueberry, jasmine, bright acidity |
| Colombian Huila | Colombia | Medium | Caramel, red apple, citrus, smooth body |
| Kenyan AA | Kenya | Light-Medium | Blackcurrant, tomato, wine-like brightness |
| Guatemalan Antigua | Guatemala | Medium-Dark | Dark chocolate, brown sugar, mild spice |
| Indonesian Mandheling | Sumatra, Indonesia | Dark | Earthy, cedar, full body, low acidity |
| Brazilian Cerrado | Minas Gerais, Brazil | Medium | Nutty, milk chocolate, low acid, soft sweetness |

### 5b. Ulaanbaatar cafe seed data
Added via new `seedCafes()` helper called from `seedStaticContent()`. 8–10 real/well-known cafes:

| Name | District | Address | Notes |
|------|----------|---------|-------|
| Luna Blanca | Sukhbaatar | Peace Ave, near State Dept Store | Specialty coffee, popular brunch spot |
| Espresso Yourself | Khan-Uul | Zaisan area | Cozy, specialty roasts |
| Grand Coffee | Bayanzurkh | Narnii Rd | Large seating, working-friendly |
| Nomads Coffee | Sukhbaatar | Olympic St | Local roastery |
| Café Amsterdam | Sukhbaatar | Baga Toiruu | European-style, pastries |
| Merkuri Coffee | Bayangol | Tsagdaagiin gudamj | Minimalist, pour-over focus |
| Coffee Lab UB | Sukhbaatar | Seoul St | Training/tasting lab |
| Rocky Mountain Coffee | Sukhbaatar | Ulaanbaatar Hotel lobby | International chain presence |

Each seeded with: name, address, district, description, coordinates (approximate lat/lng), rating 0. For `submittedBy`, use the first registered user's id if available, otherwise skip calling `createCafe` and insert directly via `repo.seedCafe(...)` — a new dedicated seed method that bypasses user ownership.

### 5c. Drink categories article
One `LearnArticle` entry, category `"Drinks"`, title `"Coffee Drink Guide"`:
- Covers: Espresso, Americano, Latte, Cappuccino, Flat White, Cold Brew, Pour Over, French Press
- Each entry: what it is, milk ratio, flavor character, best bean/roast pairing

---

## 6. Out of scope for this sprint

- Desktop i18n (Mongolian) — web-ahead, separate sprint
- Cafe owner dashboards
- Enhanced social loop (challenges, check-ins)
- Journal ↔ Timer ↔ Recipe deep linking beyond the "Use in Timer" handoff

---

## 7. Files changed

| File | Change |
|------|--------|
| `src/main/resources/public/index.html` | Add `page-timer` section, timer CSS, timer JS, QuickBrewOverlay modal + JS, update nav links, update "Use in Timer" handlers |
| `src/main/resources/public/brew-timer.html` | No changes (kept as standalone) |
| `src/main/java/mn/edu/num/cafe/core/application/BorgolService.java` | Add specialty bean articles and UB cafe seed entries to `seedStaticContent()` |
