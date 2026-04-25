# Borgol Desktop — Web Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the JavaFX desktop app to full feature parity with the web reference (`index.html`), adding notifications, map, selective journal export (CSV+PDF), save-to-timer, and a quick-brew card overlay.

**Architecture:** Hexagonal — all work is in `ui/desktop/` (adapter layer). `BorgolService` is complete; only one additive domain field (`Recipe.savedByCurrentUser`) is added. No service logic changes.

**Tech Stack:** JavaFX 21, Apache PDFBox 3.0.1 (new), `java.util.prefs.Preferences` (settings persistence), Leaflet.js via CDN inside JavaFX WebView.

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `core/domain/Recipe.java` | Add `savedByCurrentUser` field |
| Modify | `infrastructure/persistence/BorgolRepository.java` | Populate `savedByCurrentUser` in `findAllRecipes()` |
| Modify | `pom.xml` | Add PDFBox 3.0.1 dependency |
| Modify | `ui/desktop/UiUtils.java` | `createAvatar()` URL support; `showRecipeDetailDialog()` timer overload |
| Modify | `ui/desktop/FeedPane.java` | Save button, ▶ Timer button, accept `Consumer<Recipe>` |
| Modify | `ui/desktop/RecipesPane.java` | Save button on cards |
| Modify | `ui/desktop/ProfilePane.java` | Avatar URL field in edit dialog |
| Modify | `ui/desktop/JournalPane.java` | Date/time display, selective export dialog, PDF export |
| Modify | `ui/desktop/BrewTimerPane.java` | Read default method from `Preferences` |
| Modify | `ui/desktop/MainWindow.java` | Notifications bell+panel, settings dialog, admin wiring, pass timer consumer to FeedPane |
| Create | `ui/desktop/MapPane.java` | WebView + Leaflet map pane |
| Create | `ui/desktop/AdminPane.java` | Report queue table, resolve/dismiss |
| Create | `ui/desktop/QuickBrewOverlay.java` | Step-by-step fullscreen brew card |

---

## Task 0 — Prerequisite: `Recipe.savedByCurrentUser` field

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/core/domain/Recipe.java`
- Modify: `src/main/java/mn/edu/num/cafe/infrastructure/persistence/BorgolRepository.java`

- [ ] **Step 0.1 — Add the field and accessor to `Recipe.java`**

Open `Recipe.java`. After line 24 (`private boolean likedByCurrentUser;`), add:

```java
private boolean      savedByCurrentUser;
```

After `isLikedByCurrentUser()` getter, add:

```java
public boolean      isSavedByCurrentUser()   { return savedByCurrentUser; }
```

After `setLikedByCurrentUser()` setter, add:

```java
public void setSavedByCurrentUser(boolean saved)        { this.savedByCurrentUser = saved; }
```

- [ ] **Step 0.2 — Populate the field in `BorgolRepository.findAllRecipes()`**

Open `BorgolRepository.java`. Find the method `findAllRecipes(int currentUserId, ...)`. Locate where `isLikedByCurrentUser` is set on each Recipe (look for `setLikedByCurrentUser`). Immediately after that line, add:

```java
r.setSavedByCurrentUser(currentUserId > 0 && isRecipeSaved(currentUserId, r.getId()));
```

Do the same in `getFeedRecipes()`, `getUserRecipes()`, `getLikedRecipes()`, and `getSavedRecipes()` — anywhere a `Recipe` object is constructed and returned with user-specific state.

- [ ] **Step 0.3 — Run the existing test to confirm nothing is broken**

```bash
cd C:/Users/thatu/OneDrive/Desktop/cafe-project
mvn test -q
```

Expected: `BUILD SUCCESS` (or same failures as before — do not introduce new failures).

- [ ] **Step 0.4 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/core/domain/Recipe.java \
        src/main/java/mn/edu/num/cafe/infrastructure/persistence/BorgolRepository.java
git commit -m "feat: add savedByCurrentUser field to Recipe domain entity"
```

---

## Task 1 — pom.xml: Add PDFBox dependency

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1.1 — Add dependency**

Open `pom.xml`. Inside `<dependencies>`, add:

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.1</version>
</dependency>
```

- [ ] **Step 1.2 — Verify Maven resolves it**

```bash
mvn dependency:resolve -q
```

Expected: `BUILD SUCCESS` (PDFBox downloaded to local Maven cache).

- [ ] **Step 1.3 — Commit**

```bash
git add pom.xml
git commit -m "build: add Apache PDFBox 3.0.1 for journal PDF export"
```

---

## Task 2 — Sprint A1: Save button on recipe cards

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/UiUtils.java`
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/FeedPane.java`
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/RecipesPane.java`

- [ ] **Step 2.1 — Add `showRecipeDetailDialog` overload in `UiUtils.java`**

The current signature is `showRecipeDetailDialog(BorgolService, Recipe, Runnable)`. Add an overload that also accepts a timer consumer (used in later tasks). Place it directly after the existing method:

```java
static void showRecipeDetailDialog(BorgolService service, Recipe r,
                                    Runnable onRefresh,
                                    java.util.function.Consumer<Recipe> onUseInTimer) {
    // delegate to existing method but inject timer + save buttons
    showRecipeDetailDialog(service, r, onRefresh); // placeholder until Task 8 replaces body
}
```

> Note: This overload will be fully fleshed out in Task 8 (QuickBrewOverlay). For now it delegates.

- [ ] **Step 2.2 — Add a save button helper in `UiUtils.java`**

Add this static helper after `createAvatar()`:

```java
/** Bookmark toggle button wired to service.toggleSave(). Calls onToggle after each toggle. */
static Button saveButton(BorgolService service, Recipe r, Runnable onToggle) {
    boolean saved = r.isSavedByCurrentUser();
    Button btn = new Button(saved ? "\uD83D\uDD16" : "\uD83D\uDD16");
    btn.setStyle(
        "-fx-background-color:transparent;-fx-border-width:0;-fx-cursor:hand;-fx-font-size:15px;" +
        "-fx-text-fill:" + (saved ? "#E8A030" : "#8A7054") + ";-fx-padding:5 8 5 8;");
    btn.setOnAction(e -> {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }
        service.toggleSave(AppSession.userId(), r.getId());
        showToast(r.isSavedByCurrentUser() ? "Removed from saved" : "\uD83D\uDD16 Saved!");
        onToggle.run();
    });
    return btn;
}
```

- [ ] **Step 2.3 — Add save button to `FeedPane.buildRecipeCard()` footer**

Open `FeedPane.java`. In `buildRecipeCard()`, find the footer `HBox` construction. It currently ends with:

```java
footer.getChildren().addAll(likeBtn, commentLbl, timeInfo, btnSpacer, viewBtn);
```

Replace with:

```java
Button saveBtn = UiUtils.saveButton(service, r, this::loadData);
footer.getChildren().addAll(likeBtn, commentLbl, timeInfo, btnSpacer, saveBtn, viewBtn);
```

- [ ] **Step 2.4 — Add save button to `RecipesPane` card builder**

Open `RecipesPane.java`. Find the card builder method (look for where `likeBtn` and `viewBtn` are added to a footer `HBox`). Add `UiUtils.saveButton(service, r, this::loadData)` between the like button and the view button, matching the same pattern.

- [ ] **Step 2.5 — Run the app and verify**

Run the JavaFX app via Eclipse → `BorgolApp.java` → Run. Log in, go to Recipes, click the bookmark button on a card. It should turn amber. Refresh — it should stay amber. Click again — it should revert.

- [ ] **Step 2.6 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/UiUtils.java \
        src/main/java/mn/edu/num/cafe/ui/desktop/FeedPane.java \
        src/main/java/mn/edu/num/cafe/ui/desktop/RecipesPane.java
git commit -m "feat: add save/bookmark button to feed and recipe cards"
```

---

## Task 3 — Sprint A2: Avatar URL in Edit Profile

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/UiUtils.java`
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/ProfilePane.java`

- [ ] **Step 3.1 — Update `UiUtils.createAvatar()` to support image URLs**

Replace the existing `createAvatar(String username, int size)` method with:

```java
static javafx.scene.Node createAvatar(String username, int size) {
    return createAvatar(username, null, size);
}

static javafx.scene.Node createAvatar(String username, String imageUrl, int size) {
    if (imageUrl != null && !imageUrl.isBlank()) {
        try {
            javafx.scene.image.Image img =
                new javafx.scene.image.Image(imageUrl, size, size, true, true, true);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(size); iv.setFitHeight(size);
            javafx.scene.shape.Circle clip =
                new javafx.scene.shape.Circle(size / 2.0, size / 2.0, size / 2.0);
            iv.setClip(clip);
            return iv;
        } catch (Exception ignored) {}
    }
    // Fallback: letter initial label
    String initial = (username != null && !username.isEmpty())
        ? username.substring(0, 1).toUpperCase() : "?";
    Label av = new Label(initial);
    int radius = size / 2;
    int fontSize = Math.max(9, size / 3);
    av.setStyle(
        "-fx-background-color:" + MainWindow.avatarColor(username) + ";" +
        "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:" + fontSize + "px;" +
        "-fx-min-width:" + size + "px;-fx-min-height:" + size + "px;" +
        "-fx-max-width:" + size + "px;-fx-max-height:" + size + "px;" +
        "-fx-background-radius:" + radius + "px;-fx-alignment:center;");
    return av;
}
```

> The return type changes from `Label` to `Node`. All existing call sites that assign to a `Label` variable must be updated: change `Label av = UiUtils.createAvatar(...)` to `javafx.scene.Node av = UiUtils.createAvatar(...)`. Do a project-wide search for `createAvatar` and update each assignment.

- [ ] **Step 3.2 — Add Avatar URL field to `ProfilePane.showEditProfileDialog()`**

Open `ProfilePane.java`, find `showEditProfileDialog()`. The `GridPane grid` currently has rows: Bio (0), Level (1), Flavor Prefs (2). Add a row for avatar URL after flavor prefs:

```java
TextField avatarField = MainWindow.styledField("https://example.com/photo.jpg");
avatarField.setText(profile.avatarUrl() != null ? profile.avatarUrl() : "");
grid.add(lbl("Avatar URL"), 0, 3);
grid.add(avatarField, 1, 3);
```

Then in the `dlg.showAndWait()` handler, update the `service.updateProfile()` call to pass `avatarField.getText().trim()` as the second argument (replacing the empty string `""`):

```java
service.updateProfile(AppSession.userId(),
    bioArea.getText().trim(),
    avatarField.getText().trim(),   // was "" before
    levelBox.getValue(), prefs);
```

- [ ] **Step 3.3 — Run the app and verify**

Run the app, log in, go to Profile → Edit Profile. Enter a valid image URL (e.g. any `.jpg` link). Save. The avatar in the navbar pill and profile header should now show the image instead of the letter initial.

- [ ] **Step 3.4 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/UiUtils.java \
        src/main/java/mn/edu/num/cafe/ui/desktop/ProfilePane.java
git commit -m "feat: support avatar image URL in profile edit and createAvatar()"
```

---

## Task 4 — Sprint A3: Notifications bell + panel

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java`

- [ ] **Step 4.1 — Add notification panel state fields**

At the top of the `MainWindow` class, alongside `chatPanel` and `chatOpen`, add:

```java
private VBox    notifPanel;
private boolean notifOpen = false;
private Label   notifBadge;
```

- [ ] **Step 4.2 — Build the notification panel**

Add this method to `MainWindow`:

```java
private VBox buildNotifPanel() {
    VBox panel = new VBox(0);
    panel.setPrefWidth(340);
    panel.setMaxHeight(480);
    panel.setStyle(
        "-fx-background-color:" + UiUtils.card() + ";" +
        "-fx-background-radius:0 0 0 16;" +
        "-fx-effect:dropshadow(gaussian,rgba(12,4,0,0.25),24,0,0,0);" +
        "-fx-border-color:" + UiUtils.border() + " transparent " +
            UiUtils.border() + " " + UiUtils.border() + ";" +
        "-fx-border-width:0 0 1 1;-fx-border-radius:0 0 0 16;");

    // Header
    HBox header = new HBox(10);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setPadding(new Insets(14, 16, 14, 16));
    header.setStyle("-fx-background-color:#0C0400;-fx-background-radius:0;");
    Label title = new Label("\uD83D\uDD14 Notifications");
    title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#F5C060;");
    Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
    Button markRead = new Button("Mark all read");
    markRead.setStyle("-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:rgba(255,255,255,0.8);" +
        "-fx-font-size:11px;-fx-padding:4 10 4 10;-fx-background-radius:6;-fx-border-width:0;-fx-cursor:hand;");
    header.getChildren().addAll(title, hSpacer, markRead);

    // Messages area
    VBox msgBox = new VBox(0);
    ScrollPane scroll = new ScrollPane(msgBox);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setStyle("-fx-background:" + UiUtils.card() + ";-fx-background-color:" + UiUtils.card() + ";");
    VBox.setVgrow(scroll, Priority.ALWAYS);

    panel.getChildren().addAll(header, scroll);

    // Wire mark-read
    markRead.setOnAction(e -> {
        if (AppSession.loggedIn()) {
            service.markNotificationsRead(AppSession.userId());
            notifBadge.setVisible(false);
            loadNotifications(msgBox);
        }
    });

    // Initial load
    loadNotifications(msgBox);
    return panel;
}

private void loadNotifications(VBox msgBox) {
    msgBox.getChildren().clear();
    if (!AppSession.loggedIn()) {
        Label empty = new Label("Log in to see notifications.");
        empty.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";-fx-padding:20;");
        msgBox.getChildren().add(empty);
        return;
    }
    try {
        var notifs = service.getNotifications(AppSession.userId());
        if (notifs.isEmpty()) {
            Label empty = new Label("No notifications yet.");
            empty.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";-fx-padding:20;");
            msgBox.getChildren().add(empty);
            return;
        }
        for (var n : notifs) {
            boolean unread = Boolean.TRUE.equals(n.get("read")) == false;
            HBox row = new HBox(10);
            row.setPadding(new Insets(12, 14, 12, 14));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(
                "-fx-background-color:" + (unread ? "rgba(168,98,30,0.06)" : UiUtils.card()) + ";" +
                "-fx-border-color:transparent transparent " + UiUtils.border() + " transparent;" +
                "-fx-border-width:0 0 1 0;");
            if (unread) {
                javafx.scene.shape.Rectangle bar = new javafx.scene.shape.Rectangle(3, 36);
                bar.setFill(javafx.scene.paint.Color.web("#A8621E"));
                row.getChildren().add(bar);
            }
            String actor = n.getOrDefault("actorUsername", "Someone").toString();
            Label av = UiUtils.createAvatar(actor, 30);
            VBox info = new VBox(2);
            Label msg = new Label(actor + " " + n.getOrDefault("message", ""));
            msg.setWrapText(true);
            msg.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.text() + ";");
            String ts = n.getOrDefault("createdAt", "").toString();
            Label time = new Label(ts.length() > 10 ? ts.substring(0, 10) : ts);
            time.setStyle("-fx-font-size:11px;-fx-text-fill:" + UiUtils.sub() + ";");
            info.getChildren().addAll(msg, time);
            HBox.setHgrow(info, Priority.ALWAYS);
            row.getChildren().addAll(av, info);
            msgBox.getChildren().add(row);
        }
    } catch (Exception ignored) {}
}
```

- [ ] **Step 4.3 — Add `toggleNotif()` method**

```java
private void toggleNotif() {
    notifOpen = !notifOpen;
    notifPanel.setVisible(true);
    javafx.animation.TranslateTransition tt =
        new javafx.animation.TranslateTransition(javafx.util.Duration.millis(220), notifPanel);
    tt.setToX(notifOpen ? 0 : 340);
    tt.setOnFinished(e -> { if (!notifOpen) notifPanel.setVisible(false); });
    tt.play();
}
```

- [ ] **Step 4.4 — Wire bell button into `buildNavbar()` and `buildNotifPanel()` into `buildPanes()`**

In `buildNavbar()`, find where `rightSection` is built. After the search `TextField` and before `rightSection`, add:

```java
// Bell button with badge overlay
Button bellBtn = new Button("\uD83D\uDD14");
bellBtn.setStyle("-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:rgba(255,255,255,0.85);" +
    "-fx-font-size:16px;-fx-padding:5 10 5 10;-fx-background-radius:8;" +
    "-fx-border-width:0;-fx-cursor:hand;");
bellBtn.setOnAction(e -> toggleNotif());
bellBtn.setVisible(AppSession.loggedIn());

notifBadge = new Label("");
notifBadge.setStyle("-fx-background-color:#B5321E;-fx-text-fill:white;-fx-font-size:9px;" +
    "-fx-font-weight:800;-fx-background-radius:8;-fx-padding:1 4 1 4;");
notifBadge.setVisible(false);
StackPane.setAlignment(notifBadge, Pos.TOP_RIGHT);
StackPane bellStack = new StackPane(bellBtn, notifBadge);
```

Add `bellStack` to `bar.getChildren()` before `rightSection`.

Then refresh the badge count by adding a `refreshNotifBadge()` helper and calling it in `buildNavbar()` and `refreshAll()`:

```java
private void refreshNotifBadge() {
    if (!AppSession.loggedIn() || notifBadge == null) return;
    try {
        var countMap = service.getNotificationCount(AppSession.userId());
        int unread = ((Number) countMap.getOrDefault("unread", 0)).intValue();
        notifBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
        notifBadge.setVisible(unread > 0);
    } catch (Exception ignored) {}
}
```

In `buildPanes()`, after building `chatPanel`, add:

```java
notifPanel = buildNotifPanel();
notifPanel.setTranslateX(340);
notifPanel.setVisible(false);
center.getChildren().add(notifPanel);
StackPane.setAlignment(notifPanel, Pos.TOP_RIGHT);
```

- [ ] **Step 4.5 — Run the app and verify**

Run the app. Log in. The bell `🔔` button appears in the navbar. Click it — a panel slides in from the right listing notifications. Click "Mark all read" — badge disappears.

- [ ] **Step 4.6 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java
git commit -m "feat: add notifications bell and slide-out panel to navbar"
```

---

## Task 5 — Sprint B4: MapPane

**Files:**
- Create: `src/main/java/mn/edu/num/cafe/ui/desktop/MapPane.java`
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java`

- [ ] **Step 5.1 — Create `MapPane.java`**

```java
package mn.edu.num.cafe.ui.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.domain.CafeListing;

import java.util.List;

/**
 * Map pane — Leaflet.js map inside a JavaFX WebView showing cafes near a location.
 */
class MapPane {

    private final BorderPane root;
    private final BorgolService service;
    private WebEngine engine;
    private double currentLat = 47.9077;
    private double currentLng = 106.8832;
    private double currentRadius = 5.0;
    private VBox cafeList;

    MapPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        root.setTop(buildToolbar());

        WebView map = new WebView();
        engine = map.getEngine();
        engine.loadContent(buildLeafletHtml());

        cafeList = new VBox(10);
        cafeList.setPadding(new Insets(16));
        ScrollPane cafeScroll = new ScrollPane(cafeList);
        cafeScroll.setFitToWidth(true);
        cafeScroll.setStyle("-fx-background-color:" + UiUtils.bg() + ";-fx-background:" + UiUtils.bg() + ";");
        cafeScroll.setPrefHeight(200);

        VBox center = new VBox(0, map, cafeScroll);
        VBox.setVgrow(map, Priority.ALWAYS);
        root.setCenter(center);

        // Load default view after WebView initialises
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) refreshMap();
        });
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("\uD83D\uDDFA\uFE0F  Map");
        title.getStyleClass().add("pane-title");

        Label radiusLbl = new Label("Radius:");
        radiusLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";");

        ComboBox<String> radiusBox = new ComboBox<>();
        radiusBox.getItems().addAll("2 km", "5 km", "10 km", "25 km", "50 km");
        radiusBox.setValue("5 km");
        radiusBox.setOnAction(e -> {
            String v = radiusBox.getValue().replace(" km", "");
            currentRadius = Double.parseDouble(v);
            refreshMap();
        });

        Button locateBtn = new Button("\uD83D\uDCCD Locate Me");
        locateBtn.getStyleClass().add("btn-primary");
        locateBtn.setOnAction(e -> showLocateDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(title, spacer, radiusLbl, radiusBox, locateBtn);
        return bar;
    }

    // ── Locate dialog ─────────────────────────────────────────────────────────

    private void showLocateDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Enter Location");
        dlg.setHeaderText("Enter coordinates (Ulaanbaatar: 47.9077, 106.8832)");
        ButtonType okBtn = new ButtonType("Go", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = MainWindow.formGrid();
        TextField latFld = MainWindow.styledField("47.9077");
        TextField lngFld = MainWindow.styledField("106.8832");
        grid.add(new Label("Latitude:"),  0, 0); grid.add(latFld, 1, 0);
        grid.add(new Label("Longitude:"), 0, 1); grid.add(lngFld, 1, 1);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == okBtn) {
                try {
                    currentLat = Double.parseDouble(latFld.getText().trim());
                    currentLng = Double.parseDouble(lngFld.getText().trim());
                    refreshMap();
                } catch (NumberFormatException ex) {
                    MainWindow.alert("Invalid input", "Please enter numeric coordinates.");
                }
            }
        });
    }

    // ── Map refresh ───────────────────────────────────────────────────────────

    private void refreshMap() {
        int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
        List<CafeListing> cafes;
        try {
            cafes = service.getCafesNearby(uid, currentLat, currentLng, currentRadius);
        } catch (Exception e) { cafes = List.of(); }

        // Build JS marker array
        StringBuilder js = new StringBuilder("clearMarkers();setView(")
            .append(currentLat).append(",").append(currentLng).append(");");
        for (CafeListing c : cafes) {
            if (c.getLatitude() == 0 && c.getLongitude() == 0) continue;
            String name = c.getName().replace("'", "\\'");
            String addr = (c.getAddress() != null ? c.getAddress() : "").replace("'", "\\'");
            js.append("addMarker(").append(c.getLatitude()).append(",")
              .append(c.getLongitude()).append(",'").append(name)
              .append("','").append(addr).append("');");
        }
        engine.executeScript(js.toString());

        // Cafe list below map
        cafeList.getChildren().clear();
        if (cafes.isEmpty()) {
            cafeList.getChildren().add(UiUtils.emptyState(
                "\uD83D\uDDFA\uFE0F", "No cafes found nearby",
                "Try increasing the radius or adjusting your location."));
        } else {
            for (CafeListing c : cafes) {
                Label name = new Label(c.getName());
                name.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:" + UiUtils.text() + ";");
                Label addr = new Label(c.getAddress() != null ? c.getAddress() : "");
                addr.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.sub() + ";");
                VBox card = new VBox(3, name, addr);
                card.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-padding:10 14 10 14;" +
                    "-fx-background-radius:10;-fx-border-color:" + UiUtils.border() + ";" +
                    "-fx-border-radius:10;-fx-border-width:1;");
                cafeList.getChildren().add(card);
            }
        }
    }

    // ── Leaflet HTML ──────────────────────────────────────────────────────────

    private static String buildLeafletHtml() {
        return """
            <!DOCTYPE html><html><head>
            <meta charset="UTF-8"/>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>html,body,#map{margin:0;padding:0;width:100%;height:100%}</style>
            </head><body>
            <div id="map"></div>
            <script>
              var map = L.map('map').setView([47.9077, 106.8832], 13);
              L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
                attribution:'&copy; OpenStreetMap contributors'}).addTo(map);
              var markers = [];
              function clearMarkers() { markers.forEach(function(m){map.removeLayer(m);}); markers=[]; }
              function setView(lat,lng) { map.setView([lat,lng], 13); }
              function addMarker(lat,lng,name,addr) {
                var m = L.marker([lat,lng]).addTo(map);
                m.bindPopup('<b>'+name+'</b><br/>'+addr);
                markers.push(m);
              }
            </script></body></html>
            """;
    }

    BorderPane getRoot() { return root; }
}
```

- [ ] **Step 5.2 — Register MapPane in `MainWindow.buildPanes()`**

In `MainWindow.buildPanes()`, add:

```java
MapPane mp = new MapPane(service);
```

Then in the panes map, add:

```java
panes.put("Map", mp.getRoot());
```

In `buildNavbar()`, add `"Map"` to the `pages` array:

```java
String[] pages = {"Feed", "Recipes", "Cafes", "Explore", "Journal", "Learn", "Timer", "Map"};
```

Add a `"Map"` case in `navEmoji()`:

```java
case "Map"     -> "\uD83D\uDDFA\uFE0F";
```

- [ ] **Step 5.3 — Run the app and verify**

Run the app. Click `🗺️ Map` in the navbar. The WebView should load an OpenStreetMap centred on Ulaanbaatar. Click "📍 Locate Me", enter coordinates, click Go — map re-centres and shows cafe markers if any cafes have GPS coordinates in the DB.

- [ ] **Step 5.4 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/MapPane.java \
        src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java
git commit -m "feat: add Map pane with Leaflet.js and radius-based cafe search"
```

---

## Task 6 — Sprint B5: Settings panel

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java`
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/BrewTimerPane.java`

- [ ] **Step 6.1 — Add `showSettingsDialog()` to `MainWindow`**

```java
private void showSettingsDialog() {
    Dialog<ButtonType> dlg = new Dialog<>();
    dlg.setTitle("Settings");
    dlg.setHeaderText(null);
    ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
    dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
    dlg.getDialogPane().getStylesheets().add(
        getClass().getResource("/style.css").toExternalForm());
    dlg.getDialogPane().setPrefWidth(400);

    VBox box = new VBox(20);
    box.setPadding(new Insets(20));

    // ── Appearance ───────────────────────────────────────────────────────────
    Label appearLbl = new Label("APPEARANCE");
    appearLbl.getStyleClass().add("form-label");
    ToggleButton lightBtn = new ToggleButton("☀️  Light");
    ToggleButton darkBtn2 = new ToggleButton("🌙  Dark");
    ToggleGroup tg = new ToggleGroup();
    lightBtn.setToggleGroup(tg); darkBtn2.setToggleGroup(tg);
    if (darkMode) darkBtn2.setSelected(true); else lightBtn.setSelected(true);
    HBox themeRow = new HBox(0, lightBtn, darkBtn2);

    // ── Default Timer Method ─────────────────────────────────────────────────
    Label timerLbl = new Label("DEFAULT TIMER METHOD");
    timerLbl.getStyleClass().add("form-label");
    ComboBox<String> methodBox = new ComboBox<>();
    methodBox.getItems().addAll(
        "Espresso", "Pour Over", "French Press", "Cold Brew", "Aeropress", "Moka Pot");
    java.util.prefs.Preferences prefs =
        java.util.prefs.Preferences.userRoot().node("borgol/desktop");
    methodBox.setValue(prefs.get("defaultTimerMethod", "Pour Over"));

    box.getChildren().addAll(appearLbl, themeRow, timerLbl, methodBox);
    dlg.getDialogPane().setContent(box);

    dlg.showAndWait().ifPresent(bt -> {
        if (bt == saveBtn) {
            // Theme
            boolean wantDark = darkBtn2.isSelected();
            if (wantDark != darkMode) toggleDarkMode();
            // Timer default
            prefs.put("defaultTimerMethod", methodBox.getValue());
        }
    });
}
```

- [ ] **Step 6.2 — Add `⚙️` button to navbar**

In `buildNavbar()`, create a settings button and add it to `bar` before `rightSection`:

```java
Button settingsBtn = new Button("\u2699\uFE0F");
settingsBtn.setStyle("-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:rgba(255,255,255,0.85);" +
    "-fx-font-size:16px;-fx-padding:5 10 5 10;-fx-background-radius:8;" +
    "-fx-border-width:0;-fx-cursor:hand;");
settingsBtn.setOnAction(e -> showSettingsDialog());
```

Add `settingsBtn` to `bar.getChildren()` after `bellStack`.

- [ ] **Step 6.3 — Read default method preference in `BrewTimerPane`**

In `BrewTimerPane` constructor, replace:

```java
current = METHODS[1]; // Pour Over default
```

With:

```java
String pref = java.util.prefs.Preferences
    .userRoot().node("borgol/desktop").get("defaultTimerMethod", "Pour Over");
current = java.util.Arrays.stream(METHODS)
    .filter(m -> m.name().equals(pref))
    .findFirst().orElse(METHODS[1]);
```

- [ ] **Step 6.4 — Run the app and verify**

Run the app. Click `⚙️`. Change theme to Dark, change default method to Espresso. Save. Dark mode applies immediately. Restart app, go to Timer — Espresso should be pre-selected.

- [ ] **Step 6.5 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java \
        src/main/java/mn/edu/num/cafe/ui/desktop/BrewTimerPane.java
git commit -m "feat: add settings panel with theme toggle and default timer method preference"
```

---

## Task 7 — Sprint B6: Admin pane

**Files:**
- Create: `src/main/java/mn/edu/num/cafe/ui/desktop/AdminPane.java`
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java`

- [ ] **Step 7.1 — Create `AdminPane.java`**

```java
package mn.edu.num.cafe.ui.desktop;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import mn.edu.num.cafe.core.application.BorgolService;

import java.util.List;
import java.util.Map;

/**
 * Admin pane — report queue. Only shown when AppSession.username() is "admin".
 */
class AdminPane {

    private final BorderPane root;
    private final BorgolService service;
    private Label statsLabel;
    private TableView<Map<String, Object>> table;

    AdminPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        root.setTop(buildToolbar());
        root.setCenter(buildTable());
        loadData();
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("\u2699\uFE0F  Admin");
        title.getStyleClass().add("pane-title");
        statsLabel = new Label("Loading…");
        statsLabel.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refresh = new Button("\u21BB Refresh");
        refresh.getStyleClass().add("btn-secondary");
        refresh.setOnAction(e -> loadData());
        bar.getChildren().addAll(title, statsLabel, spacer, refresh);
        return bar;
    }

    @SuppressWarnings("unchecked")
    private TableView<Map<String, Object>> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Map<String,Object>, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getOrDefault("contentType", "").toString()));

        TableColumn<Map<String,Object>, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getOrDefault("reason", "").toString()));

        TableColumn<Map<String,Object>, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getOrDefault("description", "").toString()));

        TableColumn<Map<String,Object>, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button resolve  = new Button("Resolve");
            private final Button dismiss = new Button("Dismiss");
            { resolve.getStyleClass().add("btn-primary");
              dismiss.getStyleClass().add("btn-secondary");
              resolve.setOnAction(e -> handleReport(getItem2(), "resolved"));
              dismiss.setOnAction(e -> handleReport(getItem2(), "dismissed")); }
            private Map<String, Object> getItem2() {
                return getTableView().getItems().get(getIndex());
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                setGraphic(new HBox(6, resolve, dismiss));
            }
        });

        table.getColumns().addAll(typeCol, reasonCol, descCol, actionsCol);
        return table;
    }

    private void handleReport(Map<String, Object> report, String action) {
        int id = ((Number) report.getOrDefault("id", 0)).intValue();
        try {
            service.resolveReport(id, AppSession.userId(), action);
            UiUtils.showToast(action.equals("resolved") ? "Report resolved." : "Report dismissed.");
            loadData();
        } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
    }

    private void loadData() {
        try {
            Map<String, Object> stats = service.getAdminStats();
            int pending = ((Number) stats.getOrDefault("pendingReports", 0)).intValue();
            statsLabel.setText(pending + " pending report" + (pending == 1 ? "" : "s"));
            List<Map<String, Object>> reports = service.getReports("pending");
            table.setItems(FXCollections.observableArrayList(reports));
        } catch (Exception e) {
            statsLabel.setText("Error loading stats");
        }
    }

    BorderPane getRoot() { return root; }
}
```

- [ ] **Step 7.2 — Conditionally register AdminPane in `MainWindow.buildPanes()`**

In `buildPanes()`, after other panes, add:

```java
if ("admin".equalsIgnoreCase(AppSession.username())) {
    AdminPane ap = new AdminPane(service);
    panes.put("Admin", ap.getRoot());
}
```

And in `buildNavbar()` pages array, keep it conditional — add `"Admin"` to the array only when admin:

```java
List<String> pageList = new java.util.ArrayList<>(
    java.util.Arrays.asList("Feed", "Recipes", "Cafes", "Explore", "Journal", "Learn", "Timer", "Map"));
if ("admin".equalsIgnoreCase(AppSession.username())) pageList.add("Admin");
String[] pages = pageList.toArray(new String[0]);
```

Add `"Admin"` emoji in `navEmoji()`:

```java
case "Admin"   -> "\u2699\uFE0F";
```

- [ ] **Step 7.3 — Run the app and verify**

Log in as `admin`. The `⚙ Admin` nav button appears. Click it — a table of pending reports is shown with Resolve and Dismiss buttons.

- [ ] **Step 7.4 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/AdminPane.java \
        src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java
git commit -m "feat: add admin pane with report queue (resolve/dismiss)"
```

---

## Task 8 — Sprint B7: Journal date/time display

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/JournalPane.java`

- [ ] **Step 8.1 — Display `createdAt` on journal cards**

Open `JournalPane.java`. Find the method that builds journal entry cards. Locate where the card header row is built. Add a date label using `entry.getCreatedAt()`:

```java
String rawDate = entry.getCreatedAt();
String displayDate = "";
if (rawDate != null && rawDate.length() >= 10) {
    // DB stores as "2026-04-06 14:32:00" or ISO — take first 16 chars
    displayDate = rawDate.substring(0, Math.min(16, rawDate.length())).replace("T", " ");
}
Label dateLbl = new Label(displayDate);
dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + UiUtils.sub() + ";");
```

Add `dateLbl` to the card header `HBox` (right-aligned using a spacer).

- [ ] **Step 8.2 — Add date/time fields to the new-entry dialog**

In the new-entry `Dialog` builder inside `JournalPane`, find where the form grid is built. Add at the top (row 0, shifting others down):

```java
DatePicker datePicker = new DatePicker(java.time.LocalDate.now());
datePicker.getStyleClass().add("form-field");

Spinner<Integer> hourSpinner = new Spinner<>(0, 23, java.time.LocalTime.now().getHour());
hourSpinner.setEditable(true); hourSpinner.setPrefWidth(70);
Spinner<Integer> minSpinner  = new Spinner<>(0, 59, java.time.LocalTime.now().getMinute());
minSpinner.setEditable(true); minSpinner.setPrefWidth(70);
HBox timeRow = new HBox(6, hourSpinner, new Label(":"), minSpinner);
timeRow.setAlignment(Pos.CENTER_LEFT);

grid.add(new Label("Date"),  0, 0); grid.add(datePicker, 1, 0);
grid.add(new Label("Time"),  0, 1); grid.add(timeRow,    1, 1);
// shift remaining rows by +2
```

When the dialog result is processed, build the `createdAt` string:

```java
String createdAt = String.format("%s %02d:%02d:00",
    datePicker.getValue(), hourSpinner.getValue(), minSpinner.getValue());
```

Pass `createdAt` to `service.createJournalEntry()`. Since `BorgolService.createJournalEntry()` doesn't currently accept a `createdAt` parameter, pass it via `repo.createJournalEntry()` — check if `BorgolRepository` accepts it. If the DB column has a `DEFAULT CURRENT_TIMESTAMP`, the simplest approach is to set `e.setCreatedAt(createdAt)` on the `BrewJournalEntry` before calling `repo.createJournalEntry(e)`. Verify the repository passes this value through in its SQL INSERT; if not, note it for manual SQL inspection.

- [ ] **Step 8.3 — Run the app and verify**

Run the app, log in, go to Journal. Existing entries show a date on each card. Click "+ New Entry" — date picker and time fields appear at the top of the form.

- [ ] **Step 8.4 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/JournalPane.java
git commit -m "feat: show date/time on journal cards and allow back-dating new entries"
```

---

## Task 9 — Sprint B8: Selective export (CSV + PDF)

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/JournalPane.java`

- [ ] **Step 9.1 — Add export selection dialog**

Replace the current `exportCsv()` call in the toolbar with a new `openExportDialog()` method. Also rename the CSV export button label to `📤 Export` and point it to `openExportDialog()`:

```java
private void openExportDialog() {
    if (entries.isEmpty()) {
        MainWindow.info("Nothing to export", "Add some journal entries first.");
        return;
    }
    Dialog<ButtonType> dlg = new Dialog<>();
    dlg.setTitle("Export Journal");
    dlg.setHeaderText("Select entries to export:");
    ButtonType csvBtn = new ButtonType("Export CSV", ButtonBar.ButtonData.OTHER);
    ButtonType pdfBtn = new ButtonType("Export PDF", ButtonBar.ButtonData.OTHER);
    dlg.getDialogPane().getButtonTypes().addAll(csvBtn, pdfBtn, ButtonType.CANCEL);
    dlg.getDialogPane().setPrefWidth(420);

    // Checkbox list
    VBox checkList = new VBox(6);
    checkList.setPadding(new Insets(8));
    java.util.List<CheckBox> boxes = new java.util.ArrayList<>();
    for (BrewJournalEntry e : entries) {
        String label = e.getCoffeeBean() + "  ·  " +
            (e.getCreatedAt() != null ? e.getCreatedAt().substring(0, 10) : "");
        CheckBox cb = new CheckBox(label);
        cb.setSelected(true);
        cb.setUserData(e);
        boxes.add(cb);
        checkList.getChildren().add(cb);
    }
    Button selectAll  = new Button("Select All");
    Button selectNone = new Button("Select None");
    selectAll.setOnAction(e2 -> boxes.forEach(b -> b.setSelected(true)));
    selectNone.setOnAction(e2 -> boxes.forEach(b -> b.setSelected(false)));
    HBox selRow = new HBox(8, selectAll, selectNone);
    selRow.setPadding(new Insets(0, 0, 8, 0));

    ScrollPane scroll = new ScrollPane(checkList);
    scroll.setFitToWidth(true);
    scroll.setPrefHeight(280);
    VBox content = new VBox(8, selRow, scroll);
    content.setPadding(new Insets(8));
    dlg.getDialogPane().setContent(content);

    dlg.showAndWait().ifPresent(bt -> {
        java.util.List<BrewJournalEntry> selected = boxes.stream()
            .filter(CheckBox::isSelected)
            .map(b -> (BrewJournalEntry) b.getUserData())
            .toList();
        if (selected.isEmpty()) {
            MainWindow.info("Nothing selected", "Select at least one entry.");
            return;
        }
        if (bt == csvBtn) exportCsvSelected(selected);
        else if (bt == pdfBtn) exportPdfSelected(selected);
    });
}
```

- [ ] **Step 9.2 — Implement `exportCsvSelected()`**

Rename the existing `exportCsv()` implementation to `exportCsvSelected(List<BrewJournalEntry> selected)` and replace its loop to iterate `selected` instead of `entries`:

```java
private void exportCsvSelected(java.util.List<BrewJournalEntry> selected) {
    javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
    fc.setTitle("Save CSV");
    fc.setInitialFileName("journal.csv");
    fc.getExtensionFilters().add(
        new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
    java.io.File file = fc.showSaveDialog(null);
    if (file == null) return;

    try (java.io.PrintWriter pw = new java.io.PrintWriter(file,
             java.nio.charset.StandardCharsets.UTF_8)) {
        pw.println("Date,Bean,Origin,Roast,Method,Grind,Temp(°C),Dose(g)," +
                   "Yield(g),BrewTime(s),Aroma,Flavor,Acidity,Body,Sweetness,Finish,Notes");
        for (BrewJournalEntry e : selected) {
            pw.printf("%s,%s,%s,%s,%s,%s,%d,%.1f,%.1f,%d,%d,%d,%d,%d,%d,%d,%s%n",
                e.getCreatedAt(), e.getCoffeeBean(), e.getOrigin(), e.getRoastLevel(),
                e.getBrewMethod(), e.getGrindSize(), e.getWaterTempC(),
                e.getDoseGrams(), e.getYieldGrams(), e.getBrewTimeSec(),
                e.getRatingAroma(), e.getRatingFlavor(), e.getRatingAcidity(),
                e.getRatingBody(), e.getRatingSweetness(), e.getRatingFinish(),
                e.getNotes() != null ? e.getNotes().replace(",", ";") : "");
        }
        UiUtils.showToast("CSV exported: " + file.getName());
    } catch (Exception ex) {
        MainWindow.alert("Export failed", ex.getMessage());
    }
}
```

- [ ] **Step 9.3 — Implement `exportPdfSelected()`**

```java
private void exportPdfSelected(java.util.List<BrewJournalEntry> selected) {
    javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
    fc.setTitle("Save PDF");
    fc.setInitialFileName("journal.pdf");
    fc.getExtensionFilters().add(
        new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
    java.io.File file = fc.showSaveDialog(null);
    if (file == null) return;

    try (org.apache.pdfbox.pdmodel.PDDocument doc =
             new org.apache.pdfbox.pdmodel.PDDocument()) {

        for (BrewJournalEntry e : selected) {
            org.apache.pdfbox.pdmodel.PDPage page =
                new org.apache.pdfbox.pdmodel.PDPage(
                    org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            doc.addPage(page);

            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                     new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {

                float margin = 50, y = page.getMediaBox().getHeight() - margin;
                float lineH = 16;

                // Title
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(margin, y);
                String bean = e.getCoffeeBean() != null ? e.getCoffeeBean() : "Unnamed";
                cs.showText(bean);
                cs.endText();
                y -= 24;

                // Date
                if (e.getCreatedAt() != null) {
                    cs.beginText();
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(e.getCreatedAt().substring(0, Math.min(16, e.getCreatedAt().length())));
                    cs.endText();
                    y -= 20;
                }

                // Separator line
                cs.setLineWidth(0.5f);
                cs.moveTo(margin, y); cs.lineTo(page.getMediaBox().getWidth() - margin, y);
                cs.stroke();
                y -= 14;

                // Parameters table
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10);
                String[][] params = {
                    {"Origin",     e.getOrigin()},
                    {"Roast",      e.getRoastLevel()},
                    {"Method",     e.getBrewMethod()},
                    {"Grind",      e.getGrindSize()},
                    {"Temp",       e.getWaterTempC() + " °C"},
                    {"Dose",       e.getDoseGrams() + " g"},
                    {"Yield",      e.getYieldGrams() + " g"},
                    {"Brew Time",  e.getBrewTimeSec() + " s"},
                };
                for (String[] row : params) {
                    cs.beginText();
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(row[0] + ": ");
                    cs.endText();
                    cs.beginText();
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(margin + 80, y);
                    cs.showText(row[1] != null ? row[1] : "-");
                    cs.endText();
                    y -= lineH;
                }

                // Ratings
                y -= 6;
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(margin, y); cs.showText("Ratings");
                cs.endText();
                y -= lineH;
                String[] ratingNames  = {"Aroma","Flavor","Acidity","Body","Sweetness","Finish"};
                int[]    ratingValues = {e.getRatingAroma(), e.getRatingFlavor(),
                    e.getRatingAcidity(), e.getRatingBody(),
                    e.getRatingSweetness(), e.getRatingFinish()};
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                for (int i = 0; i < ratingNames.length; i++) {
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(ratingNames[i] + ": " + ratingValues[i] + "/10");
                    cs.endText();
                    y -= lineH;
                }

                // Notes
                if (e.getNotes() != null && !e.getNotes().isBlank()) {
                    y -= 6;
                    cs.beginText();
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 11);
                    cs.newLineAtOffset(margin, y); cs.showText("Notes");
                    cs.endText();
                    y -= lineH;
                    cs.beginText();
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(margin, y);
                    // Wrap long notes at 80 chars
                    String notes = e.getNotes();
                    for (int start = 0; start < notes.length(); start += 80) {
                        String chunk = notes.substring(start, Math.min(start + 80, notes.length()));
                        cs.showText(chunk);
                        if (start + 80 < notes.length()) {
                            cs.newLineAtOffset(0, -lineH);
                            y -= lineH;
                        }
                    }
                    cs.endText();
                }
            }
        }

        doc.save(file);
        UiUtils.showToast("PDF exported: " + file.getName());
    } catch (Exception ex) {
        MainWindow.alert("PDF export failed", ex.getMessage());
    }
}
```

- [ ] **Step 9.4 — Wire up: replace `btnCsv.setOnAction` in toolbar with `openExportDialog()`**

In `buildToolbar()`, change:

```java
btnCsv.setOnAction(e -> exportCsv());
```

to:

```java
btnCsv.setText("\uD83D\uDCC4 Export");
btnCsv.setOnAction(e -> openExportDialog());
```

- [ ] **Step 9.5 — Run and verify**

Run the app, go to Journal, click `📄 Export`. A dialog appears with checkboxes for each entry. Select some, click "Export PDF" — a file chooser opens. Save the file. Open the PDF — each entry is on its own page with parameters and ratings listed.

- [ ] **Step 9.6 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/JournalPane.java
git commit -m "feat: selective journal export with CSV and PDF (PDFBox)"
```

---

## Task 10 — Sprint C9: Save brew → Use in Timer from Feed

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/FeedPane.java`
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/UiUtils.java`
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java`

- [ ] **Step 10.1 — Update `FeedPane` to accept `onUseInTimer` consumer**

Change the `FeedPane` constructor to accept the consumer:

```java
private final java.util.function.Consumer<Recipe> onUseInTimer;

public FeedPane(BorgolService service, java.util.function.Consumer<Recipe> onUseInTimer) {
    this.service = service;
    this.onUseInTimer = onUseInTimer;
    // ... rest of constructor unchanged
}
```

- [ ] **Step 10.2 — Add `▶ Timer` button to `FeedPane.buildRecipeCard()` footer**

In `buildRecipeCard()`, after the save button, add:

```java
Button timerBtn = new Button("\u25B6 Timer");
timerBtn.setStyle(
    "-fx-background-color:transparent;-fx-text-fill:#A8621E;" +
    "-fx-font-size:12px;-fx-font-weight:700;-fx-padding:5 8 5 8;" +
    "-fx-border-width:0;-fx-cursor:hand;");
timerBtn.setOnAction(e -> { if (onUseInTimer != null) onUseInTimer.accept(r); });
```

Add `timerBtn` to `footer.getChildren()` after `saveBtn` and before `viewBtn`.

- [ ] **Step 10.3 — Update `MainWindow.buildPanes()` to pass the consumer to `FeedPane`**

In `buildPanes()`, change:

```java
FeedPane fp = new FeedPane(service);
```

to:

```java
FeedPane fp = new FeedPane(service, recipe -> {
    tp.loadRecipe(recipe);
    showPane("Timer");
});
```

(Note: `tp` is the `BrewTimerPane` local variable in `buildPanes()` — ensure `fp` is declared after `tp`.)

- [ ] **Step 10.4 — Add `▶ Use in Timer` button to `UiUtils.showRecipeDetailDialog()`**

Open `UiUtils.java`. In `showRecipeDetailDialog(BorgolService, Recipe, Runnable)`, find the footer area of the dialog. Add a timer button there:

```java
// In the dialog footer / action area:
Button timerBtn = new Button("\u25B6 Use in Timer");
timerBtn.setStyle("-fx-background-color:#A8621E;-fx-text-fill:white;-fx-font-weight:700;" +
    "-fx-font-size:13px;-fx-padding:8 18 8 18;-fx-background-radius:20;-fx-border-width:0;" +
    "-fx-cursor:hand;");
```

The recipe detail dialog doesn't currently have access to the timer consumer. Update the full overload signature introduced in Task 2 to properly pass the consumer through:

```java
static void showRecipeDetailDialog(BorgolService service, Recipe r,
                                    Runnable onRefresh,
                                    java.util.function.Consumer<Recipe> onUseInTimer) {
    // ... full dialog body identical to the single-arg version ...
    // In the action bar, add timerBtn wired to: onUseInTimer.accept(r); dlg.close();
}
```

Call sites in `FeedPane` and `RecipesPane` that call `UiUtils.showRecipeDetailDialog(service, r, this::loadData)` should be updated to pass the consumer too, or leave the existing 3-arg overload as a fallback (null consumer = no timer button shown).

- [ ] **Step 10.5 — Run and verify**

Run the app, go to Feed. Each recipe card now shows a `▶ Timer` button. Click it — the Timer pane opens pre-loaded with that recipe's steps and name.

- [ ] **Step 10.6 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/FeedPane.java \
        src/main/java/mn/edu/num/cafe/ui/desktop/UiUtils.java \
        src/main/java/mn/edu/num/cafe/ui/desktop/MainWindow.java
git commit -m "feat: add Use in Timer button to feed cards and recipe detail dialog"
```

---

## Task 11 — Sprint C10: QuickBrewOverlay (easy-make format)

**Files:**
- Create: `src/main/java/mn/edu/num/cafe/ui/desktop/QuickBrewOverlay.java`
- Modify: `src/main/java/mn/edu/num/cafe/ui/desktop/UiUtils.java`

- [ ] **Step 11.1 — Create `QuickBrewOverlay.java`**

```java
package mn.edu.num.cafe.ui.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import mn.edu.num.cafe.core.domain.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Full-screen step-by-step brew card overlay.
 * Shows one instruction step at a time with Prev/Next navigation.
 * "Start Timer" closes this and loads the recipe into BrewTimerPane.
 */
class QuickBrewOverlay {

    static void show(Recipe recipe, Consumer<Recipe> onStartTimer) {
        List<String> steps = new ArrayList<>();
        String instructions = recipe.getInstructions() != null ? recipe.getInstructions() : "";
        for (String line : instructions.split("\n")) {
            if (!line.isBlank()) steps.add(line.trim());
        }
        if (steps.isEmpty()) steps.add("Brew for " + recipe.getBrewTime() + " minutes.");

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Quick Brew Card");

        int[] idx = {0};

        // ── Progress bar ──────────────────────────────────────────────────────
        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(Double.MAX_VALUE);
        progress.setStyle("-fx-accent:#A8621E;-fx-background-color:#2B1005;" +
            "-fx-background-radius:0;-fx-pref-height:4px;");

        // ── Step number ───────────────────────────────────────────────────────
        Label stepNumLbl = new Label();
        stepNumLbl.setStyle(
            "-fx-min-width:56px;-fx-min-height:56px;-fx-max-width:56px;-fx-max-height:56px;" +
            "-fx-background-color:linear-gradient(135deg,#A8621E,#CB8840);" +
            "-fx-background-radius:28;-fx-text-fill:white;-fx-font-size:22px;" +
            "-fx-font-weight:800;-fx-alignment:center;");

        // ── Step text ─────────────────────────────────────────────────────────
        Label stepText = new Label();
        stepText.setWrapText(true);
        stepText.setMaxWidth(640);
        stepText.setStyle("-fx-font-size:26px;-fx-text-fill:#F6E8CC;" +
            "-fx-font-weight:600;-fx-line-spacing:4;-fx-text-alignment:center;");
        stepText.setAlignment(Pos.CENTER);

        // ── Metadata ──────────────────────────────────────────────────────────
        Label meta = new Label(recipe.getTitle() + "  ·  " +
            recipe.getBrewTime() + " min  ·  " + recipe.getDifficulty());
        meta.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(246,232,204,0.55);");

        // ── Navigation buttons ────────────────────────────────────────────────
        Button prevBtn  = new Button("← Prev");
        Button nextBtn  = new Button("Next →");
        Button timerBtn = new Button("⏱ Start Timer");
        Button closeBtn = new Button("✕ Close");

        prevBtn.setStyle("-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:#F6E8CC;" +
            "-fx-font-size:14px;-fx-font-weight:600;-fx-padding:10 24 10 24;" +
            "-fx-background-radius:20;-fx-border-width:0;-fx-cursor:hand;");
        nextBtn.setStyle("-fx-background-color:#A8621E;-fx-text-fill:white;" +
            "-fx-font-size:14px;-fx-font-weight:700;-fx-padding:10 28 10 28;" +
            "-fx-background-radius:20;-fx-border-width:0;-fx-cursor:hand;");
        timerBtn.setStyle("-fx-background-color:rgba(255,255,255,0.12);-fx-text-fill:#F5C060;" +
            "-fx-font-size:13px;-fx-font-weight:700;-fx-padding:9 22 9 22;" +
            "-fx-background-radius:20;-fx-border-color:rgba(232,160,48,0.5);" +
            "-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
        closeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.4);" +
            "-fx-font-size:12px;-fx-padding:6 14 6 14;-fx-border-width:0;-fx-cursor:hand;");

        HBox navRow = new HBox(14, prevBtn, nextBtn, timerBtn, closeBtn);
        navRow.setAlignment(Pos.CENTER);

        // ── Update display ────────────────────────────────────────────────────
        int total = steps.size();
        Runnable updateView = () -> {
            int i = idx[0];
            stepNumLbl.setText(String.valueOf(i + 1));
            stepText.setText(steps.get(i));
            progress.setProgress((double)(i + 1) / total);
            prevBtn.setDisable(i == 0);
            nextBtn.setText(i == total - 1 ? "Done ✓" : "Next →");
        };
        updateView.run();

        prevBtn.setOnAction(e -> { if (idx[0] > 0) { idx[0]--; updateView.run(); } });
        nextBtn.setOnAction(e -> {
            if (idx[0] < total - 1) { idx[0]++; updateView.run(); }
            else stage.close();
        });
        timerBtn.setOnAction(e -> {
            stage.close();
            if (onStartTimer != null) onStartTimer.accept(recipe);
        });
        closeBtn.setOnAction(e -> stage.close());

        // ── Layout ────────────────────────────────────────────────────────────
        VBox center = new VBox(32, stepNumLbl, stepText, meta);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(48, 64, 32, 64));
        VBox.setVgrow(center, Priority.ALWAYS);

        VBox root = new VBox(0, progress, center, navRow);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0, 0, 40, 0));
        root.setStyle("-fx-background-color:linear-gradient(160deg,#0C0400,#1E0800,#3A1505);");

        Scene scene = new Scene(root, 820, 560);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }
}
```

- [ ] **Step 11.2 — Add `📋 Quick Card` button to recipe detail dialog in `UiUtils.java`**

In `showRecipeDetailDialog()` (and its consumer overload), in the `dlgHeader` section, add a "Quick Card" button right after the `chips` `HBox`:

```java
Button quickCardBtn = new Button("\uD83D\uDCCB Quick Card");
quickCardBtn.setStyle("-fx-background-color:rgba(168,98,30,0.1);-fx-text-fill:#A8621E;" +
    "-fx-font-size:12px;-fx-font-weight:700;-fx-padding:6 14 6 14;" +
    "-fx-background-radius:20;-fx-border-color:rgba(168,98,30,0.4);" +
    "-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
// onStartTimer may be null when called from the 3-arg overload
quickCardBtn.setOnAction(e -> QuickBrewOverlay.show(r, onUseInTimer));
dlgHeader.getChildren().add(quickCardBtn);
```

For the 3-arg overload (no consumer), pass `null` as the consumer — the overlay will simply not show the Start Timer button when `onStartTimer` is null.

- [ ] **Step 11.3 — Run and verify**

Run the app. Click any recipe → View. A `📋 Quick Card` button appears in the recipe detail dialog header. Click it — a dark full-screen overlay opens showing the first step with a large number badge. Click `Next →` to advance through steps. `⏱ Start Timer` closes the overlay and loads the recipe into the Timer pane.

- [ ] **Step 11.4 — Commit**

```bash
git add src/main/java/mn/edu/num/cafe/ui/desktop/QuickBrewOverlay.java \
        src/main/java/mn/edu/num/cafe/ui/desktop/UiUtils.java
git commit -m "feat: add QuickBrewOverlay step-by-step easy-make brew card"
```

---

## Task 12 — Final: Run full test suite and integration check

- [ ] **Step 12.1 — Run all tests**

```bash
cd C:/Users/thatu/OneDrive/Desktop/cafe-project
mvn test
```

Expected: `BUILD SUCCESS`. Fix any compilation errors introduced by signature changes (e.g. `createAvatar()` return type change, `FeedPane` constructor change).

- [ ] **Step 12.2 — End-to-end smoke test checklist**

Run the app and verify:
- [ ] Log in → notifications bell appears, badge shows unread count
- [ ] Recipe card: `🔖` bookmark + `▶ Timer` buttons visible
- [ ] `▶ Timer` on Feed card → Timer pane opens pre-loaded with recipe steps
- [ ] `📋 Quick Card` in recipe detail → step overlay opens, `⏱ Start Timer` loads timer
- [ ] `🗺️ Map` nav → Leaflet map loads, radius dropdown filters cafes
- [ ] Profile → Edit Profile shows Avatar URL field
- [ ] Journal cards show date/time; `📄 Export` opens selection dialog with CSV + PDF options
- [ ] `⚙️` Settings → theme and default timer method work
- [ ] Log in as `admin` → `⚙ Admin` nav appears with report table

- [ ] **Step 12.3 — Final commit**

```bash
git add -A
git commit -m "feat: desktop-web parity complete — all 10 features implemented"
```

---

## Self-Review Notes

**Spec coverage:** All 10 spec features mapped:
- A1 save button ✓ (Tasks 2, 10)
- A2 avatar URL ✓ (Task 3)
- A3 notifications ✓ (Task 4)
- B4 map pane ✓ (Task 5)
- B5 settings ✓ (Task 6)
- B6 admin ✓ (Task 7)
- B7 journal date/time ✓ (Task 8)
- B8 selective export CSV+PDF ✓ (Task 9)
- C9 save brew→timer ✓ (Task 10)
- C10 quick brew card ✓ (Task 11)

**Type consistency:** `UiUtils.createAvatar()` return type changes from `Label` to `Node` in Task 3 — all call sites that store to `Label av` must be updated to `Node av`. This is flagged in Task 3 Step 3.1.

**`FeedPane` constructor change** affects `MainWindow.buildPanes()` — addressed in Task 10 Step 10.3.

**PDFBox imports** are fully qualified in Task 9 to avoid ambiguity with any existing imports.
