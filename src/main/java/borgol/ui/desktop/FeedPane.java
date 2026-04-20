package borgol.ui.desktop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import borgol.core.application.BorgolService;
import borgol.core.application.BorgolService.UserView;
import borgol.core.domain.Recipe;

import java.util.List;

/**
 * Feed pane — three-column Instagram-style layout.
 * Center: feed cards  |  Right: suggested users + trending recipes
 */
public class FeedPane {

    private final BorderPane root;
    private final BorgolService service;
    private final java.util.function.Consumer<Recipe> onUseInTimer;
    private final ObservableList<Recipe> items = FXCollections.observableArrayList();
    private VBox feedBox;
    private VBox rightPanel;

    public FeedPane(BorgolService service, java.util.function.Consumer<Recipe> onUseInTimer) {
        this.service = service;
        this.onUseInTimer = onUseInTimer;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        root.setTop(buildToolbar());
        root.setCenter(buildMainArea());
        loadData();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("\uD83D\uDCF0  Feed");
        title.getStyleClass().add("pane-title");

        Label sub = new Label("Recipes from people you follow");
        sub.setStyle("-fx-text-fill:#65676B;-fx-font-size:13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = new Button("\u21BB  Refresh");
        btnRefresh.getStyleClass().add("btn-secondary");
        btnRefresh.setOnAction(e -> loadData());

        bar.getChildren().addAll(title, sub, spacer, btnRefresh);
        return bar;
    }

    // ── Three-column layout ───────────────────────────────────────────────────

    private HBox buildMainArea() {
        feedBox = new VBox(16);
        feedBox.setPadding(new Insets(20, 16, 20, 20));
        feedBox.setStyle("-fx-background-color:" + UiUtils.bg() + ";");

        ScrollPane feedScroll = new ScrollPane(feedBox);
        feedScroll.setFitToWidth(true);
        feedScroll.getStyleClass().add("feed-scroll");
        feedScroll.setStyle("-fx-background-color:" + UiUtils.bg() + ";-fx-background:" + UiUtils.bg() + ";");
        feedScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox.setHgrow(feedScroll, Priority.ALWAYS);

        rightPanel = new VBox(16);
        rightPanel.setPadding(new Insets(20, 16, 20, 4));
        rightPanel.setMinWidth(264);
        rightPanel.setMaxWidth(264);
        rightPanel.setStyle("-fx-background-color:" + UiUtils.bg() + ";");

        ScrollPane rightScroll = new ScrollPane(rightPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background-color:" + UiUtils.bg() + ";-fx-background:" + UiUtils.bg() + ";");
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setMinWidth(264);
        rightScroll.setMaxWidth(264);

        return new HBox(0, feedScroll, rightScroll);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        feedBox.getChildren().clear();
        rightPanel.getChildren().clear();

        if (!AppSession.loggedIn()) {
            feedBox.getChildren().add(UiUtils.emptyState(
                "\uD83D\uDCF0", "Your feed is empty",
                "Log in and follow other users to see their recipes here."));
            buildRightPanel();
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                List<Recipe> feed = service.getFeed(AppSession.userId());
                int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
                List<Recipe> popular = feed.isEmpty() ? service.getRecipes(uid, "", "ALL", "POPULAR") : List.of();
                List<borgol.core.application.BorgolService.UserView> users = service.getAllUsers(uid);
                List<Recipe> trending = service.getRecipes(uid, "", "ALL", "POPULAR");

                javafx.application.Platform.runLater(() -> {
                    items.setAll(feed);

                    if (items.isEmpty()) {
                        // Discover fallback — show popular recipes from everyone
                        HBox discoverHeader = new HBox(8);
                        discoverHeader.setAlignment(Pos.CENTER_LEFT);
                        discoverHeader.setPadding(new Insets(0, 0, 4, 4));
                        Label star = new Label("\u2728");
                        star.setStyle("-fx-font-size:18px;");
                        Label discoverLbl = new Label("Discover Popular Recipes");
                        discoverLbl.setStyle("-fx-font-size:16px;-fx-font-weight:700;-fx-text-fill:#1C1E21;");
                        Label sub = new Label("  \u00B7  Follow people to personalise your feed");
                        sub.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");
                        discoverHeader.getChildren().addAll(star, discoverLbl, sub);
                        feedBox.getChildren().add(discoverHeader);

                        if (popular.isEmpty()) {
                            feedBox.getChildren().add(UiUtils.emptyState(
                                "\uD83D\uDC64", "No posts yet",
                                "Follow some coffee enthusiasts to see their recipes here."));
                        } else {
                            for (Recipe r : popular) feedBox.getChildren().add(buildRecipeCard(r));
                        }
                    } else {
                        for (Recipe r : items) feedBox.getChildren().add(buildRecipeCard(r));
                    }

                    buildRightPanelWithData(users, trending);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    MainWindow.alert("Error", e.getMessage())
                );
            }
        });
    }

    // ── Right suggestions panel ───────────────────────────────────────────────

    private void buildRightPanel() {
        // Called only when not logged in — no service calls needed
        VBox suggestCard = UiUtils.rightCard("PEOPLE YOU MAY KNOW");
        Label none = new Label("Log in to see suggestions.");
        none.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
        suggestCard.getChildren().add(none);
        rightPanel.getChildren().add(suggestCard);

        VBox trendCard = UiUtils.rightCard("TRENDING RECIPES");
        Label none2 = new Label("No recipes yet.");
        none2.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
        trendCard.getChildren().add(none2);
        rightPanel.getChildren().add(trendCard);
    }

    private void buildRightPanelWithData(List<UserView> users, List<Recipe> trending) {
        // Suggested users
        VBox suggestCard = UiUtils.rightCard("PEOPLE YOU MAY KNOW");
        int shown = 0;
        for (UserView u : users) {
            if (AppSession.loggedIn() && u.id() == AppSession.userId()) continue;
            if (u.isFollowing()) continue;
            suggestCard.getChildren().add(buildSuggestRow(u));
            if (++shown >= 5) break;
        }
        if (shown == 0) {
            Label none = new Label("You\u2019re following everyone \uD83C\uDF89");
            none.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
            suggestCard.getChildren().add(none);
        }
        rightPanel.getChildren().add(suggestCard);

        // Trending recipes
        VBox trendCard = UiUtils.rightCard("TRENDING RECIPES");
        shown = 0;
        for (Recipe r : trending) {
            trendCard.getChildren().add(buildTrendRow(r));
            if (++shown >= 5) break;
        }
        if (shown == 0) {
            Label none = new Label("No recipes yet.");
            none.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
            trendCard.getChildren().add(none);
        }
        rightPanel.getChildren().add(trendCard);
    }

    private HBox buildSuggestRow(UserView u) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.Node av = UiUtils.createAvatar(u.username(), 32);

        VBox info = new VBox(1);
        Label name = new Label("@" + u.username());
        name.setStyle("-fx-font-weight:700;-fx-font-size:12px;-fx-text-fill:#1C1E21;" +
            "-fx-cursor:hand;");
        name.setOnMouseClicked(e -> UiUtils.showUserProfileDialog(service, u.id()));
        Label sub = new Label(u.followerCount() + " followers");
        sub.setStyle("-fx-font-size:11px;-fx-text-fill:#65676B;");
        info.getChildren().addAll(name, sub);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button followBtn = new Button("+ Follow");
        followBtn.setStyle("-fx-background-color:#D4621A;-fx-text-fill:white;" +
            "-fx-font-weight:700;-fx-font-size:11px;-fx-padding:4 10 4 10;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        followBtn.setOnAction(e -> {
            if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in."); return; }
            service.followUser(AppSession.userId(), u.id());
            loadData();
        });

        row.getChildren().addAll(av, info, followBtn);
        return row;
    }

    private VBox buildTrendRow(Recipe r) {
        VBox row = new VBox(2);
        row.setPadding(new Insets(4, 0, 4, 0));
        row.setStyle("-fx-border-color:transparent transparent #F0F2F5 transparent;" +
            "-fx-border-width:0 0 1 0;");

        Label titleLbl = new Label(r.getTitle());
        titleLbl.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#1C1E21;");
        titleLbl.setWrapText(true);

        Label meta = new Label((r.isLikedByCurrentUser() ? "\u2764 " : "\u2661 ") +
            r.getLikesCount() + "  \u00B7  @" + r.getAuthorUsername() +
            "  \u00B7  " + r.getDrinkType());
        meta.setStyle("-fx-font-size:11px;-fx-text-fill:#65676B;");

        row.getChildren().addAll(titleLbl, meta);
        return row;
    }

    // ── Feed card ─────────────────────────────────────────────────────────────

    private VBox buildRecipeCard(Recipe r) {
        VBox card = new VBox(0);
        card.getStyleClass().add("recipe-card");
        card.setMaxWidth(680);

        // Image banner — preserves ratio, fills width, clips to banner height
        if (r.getImageUrl() != null && !r.getImageUrl().isBlank()) {
            try {
                Image img = new Image(r.getImageUrl(), 0, 0, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);

                StackPane banner = new StackPane(iv);
                banner.setPrefHeight(200);
                banner.setMaxHeight(200);
                banner.setStyle("-fx-background-color:#E4E6EA;");
                StackPane.setAlignment(iv, Pos.CENTER);

                Rectangle clip = new Rectangle();
                clip.widthProperty().bind(banner.widthProperty());
                clip.setHeight(200);
                banner.setClip(clip);

                iv.fitWidthProperty().bind(banner.widthProperty());
                card.getChildren().add(banner);
            } catch (Exception ignored) {}
        }

        // Header
        HBox header = new HBox(10);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.Node avatar = UiUtils.createAvatar(r.getAuthorUsername(), 36);

        VBox authorInfo = new VBox(1);
        Label authorLabel = new Label("@" + r.getAuthorUsername());
        authorLabel.getStyleClass().add("username-link");
        authorLabel.setOnMouseClicked(e -> UiUtils.showUserProfileDialog(service, r.getAuthorId()));

        Label subtitleLbl = new Label(r.getDrinkType() + "  \u00B7  " + r.getDifficulty());
        subtitleLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
        authorInfo.getChildren().addAll(authorLabel, subtitleLbl);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label typeChip = new Label(r.getDrinkType());
        typeChip.getStyleClass().add("detail-chip");

        header.getChildren().addAll(avatar, authorInfo, hSpacer, typeChip);

        // Body
        VBox body = new VBox(8);
        body.setPadding(new Insets(0, 20, 16, 20));

        Label titleLabel = new Label(r.getTitle());
        titleLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + UiUtils.text() + ";");
        titleLabel.setWrapText(true);
        body.getChildren().add(titleLabel);

        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            String excerpt = r.getDescription().length() > 140
                ? r.getDescription().substring(0, 140) + "\u2026" : r.getDescription();
            Label desc = new Label(excerpt);
            desc.setStyle("-fx-font-size:14px;-fx-text-fill:" + UiUtils.sub() + ";-fx-line-spacing:2;");
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        // Footer
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#E4E6EA;");

        HBox footer = new HBox(20);
        footer.setPadding(new Insets(10, 20, 14, 20));
        footer.setAlignment(Pos.CENTER_LEFT);

        boolean liked = r.isLikedByCurrentUser();
        Button likeBtn = new Button((liked ? "\u2764" : "\u2661") + "  " + r.getLikesCount());
        if (liked) likeBtn.getStyleClass().add("liked-heart");
        else likeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#65676B;" +
            "-fx-font-weight:600;-fx-cursor:hand;-fx-border-width:0;-fx-font-size:13px;");
        likeBtn.setOnAction(e -> {
            if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in."); return; }
            service.toggleLike(AppSession.userId(), r.getId());
            loadData();
        });

        Label commentLbl = new Label("\uD83D\uDCAC  " + r.getCommentCount());
        commentLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");

        Label timeInfo = new Label("\u23F1  " + r.getBrewTime() + " min");
        timeInfo.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);

        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.text() + ";" +
            "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> UiUtils.showRecipeDetailDialog(service, r, this::loadData));

        Button saveBtn = UiUtils.saveButton(service, r, this::loadData);

        Button timerBtn = new Button("\u25B6 Timer");
        timerBtn.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:#A8621E;" +
            "-fx-font-size:12px;-fx-font-weight:700;-fx-padding:5 8 5 8;" +
            "-fx-border-width:0;-fx-cursor:hand;");
        timerBtn.setOnAction(e -> { if (onUseInTimer != null) onUseInTimer.accept(r); });

        footer.getChildren().addAll(likeBtn, commentLbl, timeInfo, btnSpacer, saveBtn, timerBtn, viewBtn);
        card.getChildren().addAll(header, body, sep, footer);
        return card;
    }

    public BorderPane getRoot() { return root; }
}
