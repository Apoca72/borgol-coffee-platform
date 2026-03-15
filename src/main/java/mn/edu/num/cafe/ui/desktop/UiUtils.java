package mn.edu.num.cafe.ui.desktop;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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

    static VBox emptyState(String emoji, String heading, String sub) {
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size:48px;");
        Label h = new Label(heading);
        h.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
        Label s = new Label(sub);
        s.setStyle("-fx-font-size:14px;-fx-text-fill:#65676B;");
        s.setWrapText(true);
        VBox box = new VBox(10, icon, h, s);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 40, 40, 40));
        box.setStyle("-fx-background-color:white;-fx-background-radius:12;");
        box.setMaxWidth(480);
        return box;
    }

    // ── Right panel card ──────────────────────────────────────────────────────

    static VBox rightCard(String heading) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
            "-fx-padding:16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");
        Label h = new Label(heading);
        h.getStyleClass().add("detail-heading");
        card.getChildren().add(h);
        return card;
    }

    // ── Section card (detail dialog body) ────────────────────────────────────

    static VBox sectionCard(String heading) {
        VBox s = new VBox(8);
        s.setStyle("-fx-background-color:white;-fx-background-radius:10;" +
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
        dlgHeader.setStyle("-fx-background-color:white;");

        Label titleLbl = new Label(r.getTitle());
        titleLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
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
        dlgBody.setStyle("-fx-background-color:#F8F9FA;");

        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            Label desc = new Label(r.getDescription());
            desc.setStyle("-fx-font-size:14px;-fx-text-fill:#1C1E21;-fx-line-spacing:3;");
            desc.setWrapText(true);
            dlgBody.getChildren().add(desc);
        }

        if (r.getIngredients() != null && !r.getIngredients().isBlank()) {
            VBox section = sectionCard("INGREDIENTS");
            for (String line : r.getIngredients().split("\\n")) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    Label item = new Label("\u2022  " + t);
                    item.setStyle("-fx-font-size:14px;-fx-text-fill:#1C1E21;");
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
                    item.setStyle("-fx-font-size:14px;-fx-text-fill:#1C1E21;-fx-line-spacing:2;");
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
                name.setStyle("-fx-font-weight:700;-fx-font-size:12px;-fx-text-fill:#1C1E21;");
                Label content = new Label(c.getContent());
                content.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");
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
        banner.setStyle("-fx-background-color:white;" +
            "-fx-border-color:transparent transparent #E4E6EA transparent;" +
            "-fx-border-width:0 0 1 0;");

        Label avatar = createAvatar(profile.username(), 56);

        Label nameLabel = new Label("@" + profile.username());
        nameLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
        Label levelChip = new Label(profile.expertiseLevel() != null ? profile.expertiseLevel() : "BEGINNER");
        levelChip.getStyleClass().add("detail-chip");
        HBox nameRow = new HBox(10, nameLabel, levelChip);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label stats = new Label(
            profile.recipeCount() + " recipes  \u00B7  " +
            profile.followerCount() + " followers  \u00B7  " +
            profile.followingCount() + " following");
        stats.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");

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
        bodyBox.setStyle("-fx-background-color:#F8F9FA;");

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
        title.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#1C1E21;");
        title.setWrapText(true);
        Label meta = new Label(r.getDrinkType() + "  \u00B7  \u2764 " + r.getLikesCount());
        meta.setStyle("-fx-font-size:11px;-fx-text-fill:#65676B;");
        VBox info = new VBox(2, title, meta);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:#F0F2F5;-fx-text-fill:#1C1E21;-fx-font-weight:600;" +
            "-fx-font-size:11px;-fx-padding:4 10 4 10;-fx-background-radius:6;" +
            "-fx-border-width:0;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> showRecipeDetailDialog(service, r, null));

        row.getChildren().addAll(info, viewBtn);
        return row;
    }

    // ── Mini recipe card (used in Profile My Recipes / Liked tabs) ────────────

    static VBox buildMiniCard(BorgolService service, Recipe r) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:white;-fx-background-radius:10;" +
            "-fx-padding:12 16 12 16;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,1);");

        Label title = new Label(r.getTitle());
        title.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#1C1E21;");
        title.setWrapText(true);

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label typeChip = new Label(r.getDrinkType());
        typeChip.getStyleClass().add("detail-chip");
        Label likes = new Label((r.isLikedByCurrentUser() ? "\u2764 " : "\u2661 ") + r.getLikesCount());
        likes.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:#F0F2F5;-fx-text-fill:#1C1E21;-fx-font-weight:600;" +
            "-fx-font-size:12px;-fx-padding:4 12 4 12;-fx-background-radius:6;" +
            "-fx-border-width:0;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> showRecipeDetailDialog(service, r, null));
        meta.getChildren().addAll(typeChip, likes, spacer, viewBtn);

        card.getChildren().addAll(title, meta);
        return card;
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
                    row.setStyle("-fx-border-color:transparent transparent #F0F2F5 transparent;" +
                        "-fx-border-width:0 0 1 0;");

                    Label av = createAvatar(u.username(), 36);
                    VBox info = new VBox(2);
                    Label name = new Label("@" + u.username());
                    name.setStyle("-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:#1C1E21;" +
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
