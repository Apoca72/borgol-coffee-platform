package mn.edu.num.cafe.ui.desktop;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.application.BorgolService.UserView;
import mn.edu.num.cafe.core.domain.Recipe;
import mn.edu.num.cafe.core.domain.RecipeComment;

import java.util.List;
import java.util.Objects;

/**
 * Shared UI utilities — eliminates duplication across all panes.
 * Provides: avatars, empty states, right-panel cards, dialogs, debounce.
 */
class UiUtils {

    private UiUtils() {} // utility class, no instances

    // ── Theme color helpers ───────────────────────────────────────────────────
    // Set `dark = true` before rebuilding panes; these are read at construction time.

    static boolean dark = false;

    /** Main page / feed background */
    static String bg()     { return dark ? "#18191A" : "#F0F2F5"; }
    /** Card / panel background */
    static String card()   { return dark ? "#242526" : "white";   }
    /** Slightly-offset card background (dialog body, section bg) */
    static String cardAlt(){ return dark ? "#1E1F20" : "#F8F9FA"; }
    /** Primary text */
    static String text()   { return dark ? "#E4E6EB" : "#1C1E21"; }
    /** Secondary / muted text */
    static String sub()    { return dark ? "#B0B3B8" : "#65676B"; }
    /** Divider / border */
    static String border() { return dark ? "#3E4042" : "#E4E6EA"; }
    /** Neutral button / chip background */
    static String btn()    { return dark ? "#3A3B3C" : "#F0F2F5"; }

    // ── Avatar ────────────────────────────────────────────────────────────────

    /** Creates a colored circular avatar Label for the given username and pixel size. */
    static Label createAvatar(String username, int size) {
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

    // ── Empty state ───────────────────────────────────────────────────────────

    static VBox emptyState(String emoji, String heading, String subText) {
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size:48px;");
        Label h = new Label(heading);
        h.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + text() + ";");
        Label s = new Label(subText);
        s.setStyle("-fx-font-size:14px;-fx-text-fill:" + sub() + ";");
        s.setWrapText(true);
        VBox box = new VBox(10, icon, h, s);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 40, 40, 40));
        box.setStyle("-fx-background-color:" + card() + ";-fx-background-radius:12;");
        box.setMaxWidth(480);
        return box;
    }

    // ── Right panel card ──────────────────────────────────────────────────────

    static VBox rightCard(String heading) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:" + card() + ";-fx-background-radius:12;" +
            "-fx-padding:16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");
        Label h = new Label(heading);
        h.getStyleClass().add("detail-heading");
        card.getChildren().add(h);
        return card;
    }

    // ── Section card (detail dialog body) ────────────────────────────────────

    static VBox sectionCard(String heading) {
        VBox s = new VBox(8);
        s.setStyle("-fx-background-color:" + card() + ";-fx-background-radius:10;" +
            "-fx-padding:14 16 14 16;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,1);");
        Label h = new Label(heading);
        h.getStyleClass().add("detail-heading");
        s.getChildren().add(h);
        return s;
    }

    // ── Stylesheet helper ─────────────────────────────────────────────────────

    static void addStylesheet(Dialog<?> dlg, Class<?> cls) {
        dlg.getDialogPane().getStylesheets().add(
            Objects.requireNonNull(cls.getResource("/style.css")).toExternalForm());
    }

    // ── Search debounce ───────────────────────────────────────────────────────

    /** Returns a 1-shot Timeline. Call playFromStart() on each keystroke. */
    static Timeline debounce(int ms, Runnable action) {
        Timeline t = new Timeline(new KeyFrame(Duration.millis(ms), e -> action.run()));
        t.setCycleCount(1);
        return t;
    }

    // ── Recipe detail dialog (shared by RecipesPane + FeedPane) ──────────────

    /**
     * Shows the full recipe detail dialog.
     * @param onRefresh called after dialog closes (e.g. to refresh like count in parent pane)
     */
    static void showRecipeDetailDialog(BorgolService service, Recipe r, Runnable onRefresh) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(r.getTitle());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        addStylesheet(dlg, UiUtils.class);
        dlg.getDialogPane().setPrefWidth(560);
        dlg.getDialogPane().setPrefHeight(680);

        VBox box = new VBox(0);

        // ── Header ──────────────────────────────────────────────────────────
        VBox dlgHeader = new VBox(8);
        dlgHeader.setPadding(new Insets(20, 24, 16, 24));
        dlgHeader.setStyle("-fx-background-color:" + card() + ";");

        Label titleLbl = new Label(r.getTitle());
        titleLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + text() + ";");
        titleLbl.setWrapText(true);

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        Label typeChip  = new Label(r.getDrinkType());  typeChip.getStyleClass().add("detail-chip");
        Label diffChip  = new Label(r.getDifficulty()); diffChip.getStyleClass().add("detail-chip");
        Label timeChip  = new Label("\u23F1 " + r.getBrewTime() + " min"); timeChip.getStyleClass().add("detail-chip");
        Label likeChip  = new Label((r.isLikedByCurrentUser() ? "\u2764 " : "\u2661 ") + r.getLikesCount());
        likeChip.getStyleClass().add(r.isLikedByCurrentUser() ? "detail-chip-liked" : "detail-chip");
        chips.getChildren().addAll(typeChip, diffChip, timeChip, likeChip);

        Label av = createAvatar(r.getAuthorUsername(), 28);
        Label authorLbl = new Label("@" + r.getAuthorUsername());
        authorLbl.getStyleClass().add("username-link");
        authorLbl.setOnMouseClicked(e -> showUserProfileDialog(service, r.getAuthorId()));
        HBox authorRow = new HBox(8, av, authorLbl);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        dlgHeader.getChildren().addAll(titleLbl, chips, authorRow);

        // ── Body ─────────────────────────────────────────────────────────────
        VBox dlgBody = new VBox(14);
        dlgBody.setPadding(new Insets(16, 24, 24, 24));
        dlgBody.setStyle("-fx-background-color:" + cardAlt() + ";");

        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            Label desc = new Label(r.getDescription());
            desc.setStyle("-fx-font-size:14px;-fx-text-fill:" + text() + ";-fx-line-spacing:3;");
            desc.setWrapText(true);
            dlgBody.getChildren().add(desc);
        }

        if (r.getIngredients() != null && !r.getIngredients().isBlank()) {
            VBox section = sectionCard("INGREDIENTS");
            for (String line : r.getIngredients().split("\\n")) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    Label item = new Label("\u2022  " + t);
                    item.setStyle("-fx-font-size:14px;-fx-text-fill:" + text() + ";");
                    item.setWrapText(true);
                    section.getChildren().add(item);
                }
            }
            dlgBody.getChildren().add(section);
        }

        if (r.getInstructions() != null && !r.getInstructions().isBlank()) {
            VBox section = sectionCard("INSTRUCTIONS");
            int step = 1;
            for (String line : r.getInstructions().split("\\n")) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    Label item = new Label(step++ + ".  " + t);
                    item.setStyle("-fx-font-size:14px;-fx-text-fill:" + text() + ";-fx-line-spacing:2;");
                    item.setWrapText(true);
                    section.getChildren().add(item);
                }
            }
            dlgBody.getChildren().add(section);
        }

        // ── Comments ─────────────────────────────────────────────────────────
        VBox commSection = sectionCard("COMMENTS");
        ListView<RecipeComment> commentList = new ListView<>();
        commentList.setPrefHeight(140);
        commentList.getStyleClass().add("list-view");
        commentList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(RecipeComment c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) { setText(null); setGraphic(null); return; }
                HBox row = new HBox(10);
                row.setAlignment(Pos.TOP_LEFT);
                Label av2 = createAvatar(c.getAuthorUsername(), 24);
                VBox info = new VBox(2);
                Label name = new Label("@" + c.getAuthorUsername());
                name.setStyle("-fx-font-weight:700;-fx-font-size:12px;-fx-text-fill:" + text() + ";");
                Label content = new Label(c.getContent());
                content.setStyle("-fx-font-size:13px;-fx-text-fill:" + sub() + ";");
                content.setWrapText(true);
                info.getChildren().addAll(name, content);
                row.getChildren().addAll(av2, info);
                setGraphic(row); setText(null);
            }
        });
        refreshComments(r.getId(), commentList, service);
        commSection.getChildren().add(commentList);

        if (AppSession.loggedIn()) {
            TextField commentField = new TextField();
            commentField.setPromptText("Write a comment\u2026");
            commentField.getStyleClass().add("form-field");
            Button postBtn = new Button("Post");
            postBtn.getStyleClass().add("btn-primary");
            HBox addRow = new HBox(8, commentField, postBtn);
            HBox.setHgrow(commentField, Priority.ALWAYS);
            postBtn.setOnAction(ev -> {
                String text = commentField.getText().trim();
                if (!text.isEmpty()) {
                    try {
                        service.addComment(r.getId(), AppSession.userId(), text);
                        commentField.clear();
                        refreshComments(r.getId(), commentList, service);
                    } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
                }
            });
            commSection.getChildren().add(addRow);
        }
        dlgBody.getChildren().add(commSection);

        ScrollPane scroll = new ScrollPane(dlgBody);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("detail-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        box.getChildren().addAll(dlgHeader, scroll);
        dlg.getDialogPane().setContent(box);
        dlg.showAndWait();
        if (onRefresh != null) onRefresh.run();
    }

    private static void refreshComments(int recipeId, ListView<RecipeComment> list,
                                        BorgolService service) {
        try { list.getItems().setAll(service.getComments(recipeId)); } catch (Exception ignored) {}
    }

    // ── User profile dialog ───────────────────────────────────────────────────

    /** Opens a public profile dialog for any user. Safe to call even for own profile. */
    static void showUserProfileDialog(BorgolService service, int userId) {
        UserView profile;
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            profile = service.getUserProfile(userId, uid);
        } catch (Exception e) { MainWindow.alert("Error", e.getMessage()); return; }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("@" + profile.username());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        addStylesheet(dlg, UiUtils.class);
        dlg.getDialogPane().setPrefWidth(520);
        dlg.getDialogPane().setPrefHeight(580);

        VBox box = new VBox(0);

        // ── Profile banner ───────────────────────────────────────────────────
        VBox banner = new VBox(10);
        banner.setPadding(new Insets(20, 24, 16, 24));
        banner.setStyle("-fx-background-color:" + card() + ";" +
            "-fx-border-color:transparent transparent " + border() + " transparent;" +
            "-fx-border-width:0 0 1 0;");

        Label avatar = createAvatar(profile.username(), 56);

        Label nameLabel = new Label("@" + profile.username());
        nameLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:" + text() + ";");
        Label levelChip = new Label(profile.expertiseLevel() != null ? profile.expertiseLevel() : "BEGINNER");
        levelChip.getStyleClass().add("detail-chip");
        HBox nameRow = new HBox(10, nameLabel, levelChip);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label stats = new Label(
            profile.recipeCount() + " recipes  \u00B7  " +
            profile.followerCount() + " followers  \u00B7  " +
            profile.followingCount() + " following");
        stats.setStyle("-fx-font-size:13px;-fx-text-fill:" + sub() + ";");

        VBox infoBox = new VBox(4, nameRow, stats);
        if (profile.bio() != null && !profile.bio().isBlank()) {
            Label bio = new Label(profile.bio());
            bio.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");
            bio.setWrapText(true);
            infoBox.getChildren().add(bio);
        }
        HBox headerRow = new HBox(14, avatar, infoBox);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        banner.getChildren().add(headerRow);

        // Follow / Unfollow button (hidden for own profile)
        if (AppSession.loggedIn() && profile.id() != AppSession.userId()) {
            boolean[] following = {profile.isFollowing()};
            Button followBtn = followButton(following[0]);
            followBtn.setOnAction(e -> {
                if (following[0]) {
                    service.unfollowUser(AppSession.userId(), profile.id());
                    following[0] = false;
                } else {
                    service.followUser(AppSession.userId(), profile.id());
                    following[0] = true;
                }
                updateFollowButton(followBtn, following[0]);
            });
            banner.getChildren().add(followBtn);
        }

        // ── Recipe list ──────────────────────────────────────────────────────
        VBox bodyBox = new VBox(0);
        bodyBox.setPadding(new Insets(16, 24, 24, 24));
        bodyBox.setStyle("-fx-background-color:" + cardAlt() + ";");

        Label recipesHeading = new Label("RECIPES");
        recipesHeading.getStyleClass().add("detail-heading");
        recipesHeading.setPadding(new Insets(0, 0, 10, 0));
        bodyBox.getChildren().add(recipesHeading);

        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<Recipe> recipes = service.getUserRecipes(profile.id(), uid);
            if (recipes.isEmpty()) {
                Label none = new Label("No recipes yet.");
                none.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");
                bodyBox.getChildren().add(none);
            } else {
                for (Recipe r : recipes)
                    bodyBox.getChildren().add(buildMiniRecipeRow(service, r));
            }
        } catch (Exception ignored) {}

        ScrollPane scroll = new ScrollPane(bodyBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("detail-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        box.getChildren().addAll(banner, scroll);
        dlg.getDialogPane().setContent(box);
        dlg.showAndWait();
    }

    private static Button followButton(boolean isFollowing) {
        Button btn = new Button(isFollowing ? "\u2714 Following" : "+ Follow");
        updateFollowButton(btn, isFollowing);
        return btn;
    }

    private static void updateFollowButton(Button btn, boolean isFollowing) {
        btn.setText(isFollowing ? "\u2714 Following" : "+ Follow");
        btn.setStyle(isFollowing
            ? "-fx-background-color:#FFF0E0;-fx-text-fill:#D4621A;-fx-font-weight:700;" +
              "-fx-font-size:13px;-fx-padding:6 14 6 14;-fx-background-radius:8;" +
              "-fx-border-width:0;-fx-cursor:hand;"
            : "-fx-background-color:#D4621A;-fx-text-fill:white;-fx-font-weight:700;" +
              "-fx-font-size:13px;-fx-padding:6 14 6 14;-fx-background-radius:8;" +
              "-fx-border-width:0;-fx-cursor:hand;");
    }

    private static HBox buildMiniRecipeRow(BorgolService service, Recipe r) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color:transparent transparent #F0F2F5 transparent;" +
            "-fx-border-width:0 0 1 0;");

        Label title = new Label(r.getTitle());
        title.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:" + text() + ";");
        title.setWrapText(true);
        Label meta = new Label(r.getDrinkType() + "  \u00B7  \u2764 " + r.getLikesCount());
        meta.setStyle("-fx-font-size:11px;-fx-text-fill:" + sub() + ";");
        VBox info = new VBox(2, title, meta);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:" + btn() + ";-fx-text-fill:" + text() + ";-fx-font-weight:600;" +
            "-fx-font-size:11px;-fx-padding:4 10 4 10;-fx-background-radius:6;" +
            "-fx-border-width:0;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> showRecipeDetailDialog(service, r, null));

        row.getChildren().addAll(info, viewBtn);
        return row;
    }

    // ── Mini recipe card (used in Profile My Recipes / Liked tabs) ────────────

    static VBox buildMiniCard(BorgolService service, Recipe r) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:" + card() + ";-fx-background-radius:10;" +
            "-fx-padding:12 16 12 16;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,1);");

        Label title = new Label(r.getTitle());
        title.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:" + text() + ";");
        title.setWrapText(true);

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label typeChip = new Label(r.getDrinkType());
        typeChip.getStyleClass().add("detail-chip");
        Label likes = new Label((r.isLikedByCurrentUser() ? "\u2764 " : "\u2661 ") + r.getLikesCount());
        likes.setStyle("-fx-font-size:12px;-fx-text-fill:" + sub() + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:" + btn() + ";-fx-text-fill:" + text() + ";-fx-font-weight:600;" +
            "-fx-font-size:12px;-fx-padding:4 12 4 12;-fx-background-radius:6;" +
            "-fx-border-width:0;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> showRecipeDetailDialog(service, r, null));
        meta.getChildren().addAll(typeChip, likes, spacer, viewBtn);

        card.getChildren().addAll(title, meta);
        return card;
    }

    // ── Toast notification ────────────────────────────────────────────────────

    private static StackPane toastRoot;

    static void setToastRoot(StackPane root) { toastRoot = root; }

    static void showToast(String message) {
        if (toastRoot == null) return;
        Label toast = new Label(message);
        toast.setStyle("-fx-background-color:#D4621A;-fx-text-fill:white;" +
            "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:10 22 10 22;" +
            "-fx-background-radius:20;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),8,0,0,2);");
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 36, 0));
        toast.setOpacity(0);
        toastRoot.getChildren().add(toast);

        FadeTransition fadeIn  = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        PauseTransition pause  = new PauseTransition(Duration.millis(2000));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> toastRoot.getChildren().remove(toast));
        new SequentialTransition(fadeIn, pause, fadeOut).play();
    }

    // ── Radar chart Canvas (6 axes: Aroma/Flavor/Acidity/Body/Sweet/Finish) ──

    /**
     * Builds a JavaFX Canvas radar chart for 6 rating dimensions.
     * @param values  int[6] with values 0–10
     * @param labels  String[6] axis labels
     * @param size    canvas size in px (square)
     */
    static Canvas buildRadarCanvas(int[] values, String[] labels, int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cx = size / 2.0, cy = size / 2.0;
        double maxR = size * 0.34;
        int n = 6;

        // Background rings
        gc.setStroke(Color.web("#E4E6EA")); gc.setLineWidth(0.8);
        for (int ring = 1; ring <= 5; ring++) {
            double r = maxR * ring / 5.0;
            double[] rpx = new double[n], rpy = new double[n];
            for (int i = 0; i < n; i++) {
                double a = Math.PI / 2 - i * 2 * Math.PI / n;
                rpx[i] = cx + r * Math.cos(a); rpy[i] = cy - r * Math.sin(a);
            }
            gc.strokePolygon(rpx, rpy, n);
        }

        // Axis lines
        for (int i = 0; i < n; i++) {
            double a = Math.PI / 2 - i * 2 * Math.PI / n;
            gc.strokeLine(cx, cy, cx + maxR * Math.cos(a), cy - maxR * Math.sin(a));
        }

        // Data polygon
        double[] dpx = new double[n], dpy = new double[n];
        for (int i = 0; i < n; i++) {
            double a = Math.PI / 2 - i * 2 * Math.PI / n;
            double r = maxR * Math.max(0, Math.min(10, values[i])) / 10.0;
            dpx[i] = cx + r * Math.cos(a); dpy[i] = cy - r * Math.sin(a);
        }
        gc.setFill(Color.web("#D4621A", 0.22)); gc.fillPolygon(dpx, dpy, n);
        gc.setStroke(Color.web("#D4621A")); gc.setLineWidth(2); gc.strokePolygon(dpx, dpy, n);

        // Data dots
        gc.setFill(Color.web("#D4621A"));
        for (int i = 0; i < n; i++) gc.fillOval(dpx[i] - 3, dpy[i] - 3, 6, 6);

        // Axis labels
        gc.setFill(Color.web("#65676B")); gc.setFont(Font.font(9.5));
        double labelR = maxR + 18;
        for (int i = 0; i < n; i++) {
            double a = Math.PI / 2 - i * 2 * Math.PI / n;
            double lx = cx + labelR * Math.cos(a);
            double ly = cy - labelR * Math.sin(a);
            gc.fillText(labels[i], lx - labels[i].length() * 2.8, ly + 4);
        }
        return canvas;
    }

    // ── Followers / Following list dialog ─────────────────────────────────────

    static void showFollowersDialog(BorgolService service, int userId, boolean showFollowers) {
        String title = showFollowers ? "Followers" : "Following";
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        addStylesheet(dlg, UiUtils.class);
        dlg.getDialogPane().setPrefWidth(400);
        dlg.getDialogPane().setPrefHeight(460);

        VBox box = new VBox(8);
        box.setPadding(new Insets(16));

        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<UserView> users = showFollowers
                ? service.getFollowerUsers(userId, uid)
                : service.getFollowingUsers(userId, uid);

            if (users.isEmpty()) {
                Label none = new Label(showFollowers ? "No followers yet." : "Not following anyone yet.");
                none.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");
                box.getChildren().add(none);
            } else {
                for (UserView u : users) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 0, 6, 0));
                    row.setStyle("-fx-border-color:transparent transparent " + border() + " transparent;" +
                        "-fx-border-width:0 0 1 0;");

                    Label av = createAvatar(u.username(), 36);
                    VBox info = new VBox(2);
                    Label name = new Label("@" + u.username());
                    name.setStyle("-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:" + text() + ";" +
                        "-fx-cursor:hand;");
                    name.setOnMouseClicked(e -> {
                        dlg.close();
                        showUserProfileDialog(service, u.id());
                    });
                    Label sub = new Label(u.followerCount() + " followers");
                    sub.setStyle("-fx-font-size:11px;-fx-text-fill:#65676B;");
                    info.getChildren().addAll(name, sub);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    row.getChildren().addAll(av, info);
                    box.getChildren().add(row);
                }
            }
        } catch (Exception e) {
            MainWindow.alert("Error", e.getMessage());
        }

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dlg.getDialogPane().setContent(scroll);
        dlg.showAndWait();
    }
}
