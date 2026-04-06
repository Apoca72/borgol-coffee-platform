# Borgol Desktop — Web Parity Design Spec
**Date:** 2026-04-06  
**Project:** cafe-project (Borgol Coffee Platform)  
**Scope:** Bring the JavaFX desktop app to feature parity with the existing web app (`index.html` + `brew-timer.html`)

---

## Context

The Borgol platform uses Hexagonal Architecture. The `BorgolService` core is already complete — every missing desktop feature has a working service method. All implementation work is in `src/main/java/mn/edu/num/cafe/ui/desktop/`.

The web app (`src/main/resources/public/index.html`) is the reference implementation. No changes to `core/`, `infrastructure/`, or the web layer are required.

---

## Gap Analysis Summary

| # | Feature | Web ✓ | Desktop | Service method |
|---|---|---|---|---|
| 1 | Save button on recipe/feed cards | ✓ | ✗ | `toggleSave()` |
| 2 | Avatar URL in Edit Profile | ✓ | ✗ | `updateProfile()` |
| 3 | Notifications bell + panel | ✓ | ✗ | `getNotifications()`, `markNotificationsRead()`, `getNotificationCount()` |
| 4 | Map pane (cafes on map, radius) | ✓ | ✗ | `getCafesNearby()` |
| 5 | Settings panel | ✓ | partial (dark mode only) | local `Preferences` |
| 6 | Admin pane | ✓ | ✗ | `getReports()`, `resolveReport()`, `getAdminStats()` |
| 7 | Journal date/time display | ✓ | ✗ | `createdAt` already in DB |
| 8 | Selective journal export (CSV + PDF) | ✓ CSV only | CSV only, no selection | Apache PDFBox |
| 9 | Save brew → Use in Timer (feed cards) | ✓ | partial (Recipes only) | existing callback |
| 10 | Quick Brew Card (easy-make format) | ✗ | ✗ | `BrewTimerPane.loadRecipe()` |

---

## Delivery Plan

### Sprint A — Quick Wins (backend fully ready, UI-only changes)

#### A1. Save Button on Recipe & Feed Cards

**Files:** `RecipesPane.java`, `FeedPane.java`, `UiUtils.java` (recipe detail dialog)

- Add a `🔖` toggle button to each recipe card footer in `FeedPane.buildRecipeCard()` and `RecipesPane` card builder
- Button calls `service.toggleSave(AppSession.userId(), r.getId())`
- Visual state: filled bookmark if saved (`r.isSavedByCurrentUser()`), outline if not
- Disabled / hidden when not logged in
- Shows `UiUtils.showToast("Saved!")` / `"Removed from saved"` on toggle
- Also add to the recipe detail dialog in `UiUtils.showRecipeDetailDialog()`

#### A2. Avatar URL in Edit Profile

**File:** `ProfilePane.java` → `showEditProfileDialog()`

- Add one `TextField` row labelled `"Avatar URL"` pre-filled with `profile.avatarUrl()`
- Pass value to existing `service.updateProfile(userId, bio, avatarUrl, level, prefs)`
- If URL is non-empty, `UiUtils.createAvatar()` should render an `ImageView` instead of the letter-initial label — update `UiUtils.createAvatar()` to accept an optional URL

#### A3. Notifications Bell + Panel

**File:** `MainWindow.java`

- Add `🔔` `Button` to `buildNavbar()` right of search (only when `AppSession.loggedIn()`)
- Red badge `Label` overlaid on button showing unread count from `service.getNotificationCount(userId)`
- Clicking toggles a slide-out `VBox` panel (same `TranslateTransition` pattern as Bean AI chat panel), anchored top-right below navbar
- Panel header: "Notifications" + "Mark all read" button → calls `service.markNotificationsRead()`
- Panel body: `ScrollPane` with one row per notification from `service.getNotifications(userId)`:
  - Avatar initial, message text, relative timestamp
  - Unread rows have a subtle amber left-border highlight
- Badge updates: refresh count on login, on each pane switch, and after mark-read
- Unread count = 0 → badge hidden

---

### Sprint B — New Panes & Dialogs

#### B4. Map Pane (`MapPane.java`)

**File:** new `MapPane.java`, registered in `MainWindow.buildPanes()` as `"Map"` with emoji `🗺️`

- Uses JavaFX `WebView` with a self-contained Leaflet.js HTML string (no external file dependency)
- Layout: toolbar (title + radius `ComboBox` [2/5/10/25/50 km] + `📍 Locate Me` button) above the `WebView`
- **Locate Me**: opens an `InputDialog` for manual lat/lng entry (fallback since JavaFX WebView does not expose the browser Geolocation API); pre-fills with Ulaanbaatar center (47.9077° N, 106.8832° E) as default
- On radius change or coordinate entry: calls `service.getCafesNearby(userId, lat, lng, radius)` → injects GeoJSON markers via `webEngine.executeScript("addMarkers([...])")`
- Below the WebView: a compact scrollable list of matched cafes (name, address, rating) matching `CafesPane` card style — clicking navigates to `CafesPane` detail
- WebView HTML is stored as a `static final String` constant inside `MapPane` to avoid file-path issues in Eclipse

#### B5. Settings Panel

**File:** `MainWindow.java` → new `showSettingsDialog()` method

- `⚙️` button added to navbar (always visible)
- Opens a `Dialog` with two setting groups:
  - **Appearance**: Light / Dark toggle (moves existing `darkBtn()` logic here; navbar dark button removed to avoid duplication)
  - **Timer Default Method**: `ComboBox` of brew method names; selected value saved to `java.util.prefs.Preferences` node `"borgol/desktop"`; `BrewTimerPane` reads this pref on construction to set default selection
- Cancel / Save buttons; Save applies changes immediately

#### B6. Admin Pane (`AdminPane.java`)

**File:** new `AdminPane.java`, conditionally added to `MainWindow.buildPanes()` only when `AppSession.username().equals("admin")`

- Toolbar: "⚙ Admin" title + stats label ("X pending reports" from `service.getAdminStats()`)
- `TableView<Map<String,Object>>` with columns: Type, Reason, Description, Reporter, Status
- Data from `service.getReports("pending")`
- Per-row buttons: `Resolve` → `service.resolveReport(id, adminId, "resolved")`, `Dismiss` → `"dismissed"`
- After action: refresh table + update stats label
- No nav button shown if not admin

---

### Sprint B (cont.) — Journal Enhancements

#### B7. Date/Time on Journal Entries

**Files:** `JournalPane.java`, journal new/edit dialog

- `BrewJournalEntry.createdAt` is already stored in DB; surface it in the card header as `"Apr 6, 2026 · 14:32"`
- In the new-entry dialog, add a `DatePicker` (defaults to today) and an `HH:mm` `TextField` (defaults to current time) so users can back-date entries
- Pass the combined `LocalDateTime` to `service.createJournalEntry()` — add an overload or use the existing field if the repo already accepts it
- Display format on card: `DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm")`

#### B8. Selective Export (CSV + PDF)

**File:** `JournalPane.java`

- Replace current "Export CSV" direct-export with a selection dialog:
  - `ListView<BrewJournalEntry>` with `CheckBoxListCell`
  - "Select All" / "Select None" buttons
  - "Export CSV" and "Export PDF" buttons at the bottom
- **CSV**: existing logic, filtered to selected entries
- **PDF** (new):
  - Add `org.apache.pdfbox:pdfbox:3.0.1` to `pom.xml`
  - One page per entry: heading (bean name + date), parameters table (roast, method, dose, yield, temp, time), ratings section, notes
  - Mini radar chart: render `Canvas` snapshot to `BufferedImage` via JavaFX `WritableImage` → embed in PDF page
  - Save via `FileChooser` with `.pdf` extension filter

---

### Sprint C — Recipe Features

#### C9. Save Other User's Brew → Use in Timer

**Files:** `FeedPane.java`, `UiUtils.java` (recipe detail dialog), `MainWindow.java`

- `MainWindow` passes the `onUseInTimer` `Consumer<Recipe>` down to `FeedPane` constructor (same pattern already used for `RecipesPane`)
- In `FeedPane.buildRecipeCard()` footer: add `▶ Timer` button alongside save button
- In `UiUtils.showRecipeDetailDialog()`: add `▶ Use in Timer` button to the dialog footer; the consumer is passed as a parameter (add overload accepting `Consumer<Recipe>`)
- Clicking either: calls `consumer.accept(recipe)` → `MainWindow.showPane("Timer")` + `BrewTimerPane.loadRecipe(recipe)` (already implemented)
- `BrewTimerPane.loadRecipe()` already handles arbitrary recipe steps — no changes needed there

#### C10. Quick Brew Card (Easy-Make Format)

**Files:** `UiUtils.java`, new `QuickBrewOverlay.java`

- In the recipe detail dialog, add a `📋 Quick Card` button in the header
- Opens `QuickBrewOverlay` as a full-window `Stage` (modal, undecorated)
- Layout: dark espresso background, single step displayed at a time
  - Large step number circle (matches web `.step-num` style)
  - Step text in large font (24px, wrapped)
  - Progress bar at top showing current step / total steps
  - `← Prev` / `Next →` navigation buttons
  - `⏱ Start Timer` button — calls `BrewTimerPane.loadRecipe()` and closes overlay
  - `✕ Close` top-right
- Steps sourced from `recipe.getInstructions().split("\n")` (same split used in `BrewTimerPane.loadRecipe()`)
- No timer logic inside the overlay itself — it delegates to `BrewTimerPane`

---

## Architectural Decisions

1. **No core changes** — `BorgolService` and all domain/infrastructure classes are untouched.
2. **MapPane uses inline HTML** — avoids Eclipse classpath issues with external HTML files; Leaflet.js loaded from CDN inside the WebView (requires internet connection, same as web version).
3. **PDF uses PDFBox 3.x** — Apache license, no cost, no API key. Added as Maven dependency.
4. **Notifications polling is on-demand** — no background thread; count refreshes on login and pane navigation. Avoids threading complexity.
5. **Admin pane is conditionally registered** — checked at login time in `refreshAll()`; not shown in nav for non-admin users.
6. **`QuickBrewOverlay` is a separate `Stage`** — keeps `UiUtils` from growing into a god-class; Stage is modal to prevent interaction with main window while in quick-card mode.

---

## Files to Create / Modify

### New files
- `src/main/java/mn/edu/num/cafe/ui/desktop/MapPane.java`
- `src/main/java/mn/edu/num/cafe/ui/desktop/AdminPane.java`
- `src/main/java/mn/edu/num/cafe/ui/desktop/QuickBrewOverlay.java`

### Modified files
- `MainWindow.java` — notifications bell, settings dialog, admin pane wiring, pass `onUseInTimer` to `FeedPane`
- `FeedPane.java` — save button, `▶ Timer` button, accept `Consumer<Recipe>` constructor param
- `RecipesPane.java` — save button (already has `Use in Timer`)
- `ProfilePane.java` — avatar URL field in edit dialog
- `UiUtils.java` — `createAvatar()` URL support, `showRecipeDetailDialog()` accepts timer consumer + Quick Card button
- `JournalPane.java` — date/time display, selective export dialog, PDF export
- `BrewTimerPane.java` — read default method from `Preferences`
- `pom.xml` — add PDFBox 3.0.1 dependency

---

## Out of Scope
- i18n / localization (language switching)
- Web app changes
- Any core domain or infrastructure changes
- Mobile responsiveness (N/A for desktop)
