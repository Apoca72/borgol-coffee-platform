package mn.edu.num.cafe.ui.desktop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.application.BorgolService.UserView;
import mn.edu.num.cafe.core.domain.Recipe;

import java.util.List;

/**
 * Feed pane — three-column Instagram-style layout.
 * Center: feed cards  |  Right: suggested users + trending recipes
 */
public class FeedPane {

    private final BorderPane root;
    private final BorgolService service;
    private final ObservableList<Recipe> items = FXCollections.observableArrayList();
    private VBox feedBox;
    private VBox rightPanel;

    public FeedPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setStyle("-fx-background-color:#F0F2F5;");
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
        // Feed scroll
        feedBox = new VBox(16);
        feedBox.setPadding(new Insets(20, 16, 20, 20));
        feedBox.setStyle("-fx-background-color:#F0F2F5;");

        ScrollPane feedScroll = new ScrollPane(feedBox);
        feedScroll.setFitToWidth(true);
        feedScroll.getStyleClass().add("feed-scroll");
        feedScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox.setHgrow(feedScroll, Priority.ALWAYS);

        // Right panel
        rightPanel = new VBox(16);
        rightPanel.setPadding(new Insets(20, 16, 20, 4));
        rightPanel.setMinWidth(264);
        rightPanel.setMaxWidth(264);
        rightPanel.setStyle("-fx-background-color:#F0F2F5;");

        ScrollPane rightScroll = new ScrollPane(rightPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background-color:#F0F2F5;-fx-background:#F0F2F5;");
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setMinWidth(264);
        rightScroll.setMaxWidth(264);

        HBox area = new HBox(0, feedScroll, rightScroll);
        area.setStyle("-fx-background-color:#F0F2F5;");
        return area;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        feedBox.getChildren().clear();
        rightPanel.getChildren().clear();

        if (!AppSession.loggedIn()) {
            feedBox.getChildren().add(emptyState(
                "\uD83D\uDCF0", "Your feed is empty",
                "Log in and follow other users to see their recipes here."));
            buildRightPanel();
            return;
        }

        try {
            items.setAll(service.getFeed(AppSession.userId()));
        } catch (Exception e) {
            MainWindow.alert("Error", e.getMessage());
        }

        if (items.isEmpty()) {
            feedBox.getChildren().add(emptyState(
                "\uD83D\uDC64", "No posts yet",
                "Follow some coffee enthusiasts to see their recipes here."));
        } else {
            for (Recipe r : items) feedBox.getChildren().add(buildRecipeCard(r));
        }

        buildRightPanel();
    }

    // ── Right suggestions panel ───────────────────────────────────────────────

    private void buildRightPanel() {
        // Suggested users
        VBox suggestCard = rightCard("PEOPLE YOU MAY KNOW");
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<UserView> users = service.getAllUsers(uid);
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
        } catch (Exception ignored) {}
        rightPanel.getChildren().add(suggestCard);

        // Trending recipes
        VBox trendCard = rightCard("TRENDING RECIPES");
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<Recipe> all = service.getRecipes(uid, "", "ALL", "POPULAR");
            int shown = 0;
            for (Recipe r : all) {
                trendCard.getChildren().add(buildTrendRow(r));
                if (++shown >= 5) break;
            }
            if (shown == 0) {
                Label none = new Label("No recipes yet.");
                none.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
                trendCard.getChildren().add(none);
            }
        } catch (Exception ignored) {}
        rightPanel.getChildren().add(trendCard);
    }

    private VBox rightCard(String heading) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
            "-fx-padding:16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");
        Label h = new Label(heading);
        h.getStyleClass().add("detail-heading");
        card.getChildren().add(h);
        return card;
    }

    private HBox buildSuggestRow(UserView u) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        String ini = u.username() != null && !u.username().isEmpty()
            ? u.username().substring(0, 1).toUpperCase() : "?";
        Label av = new Label(ini);
        av.setStyle("-fx-background-color:" + MainWindow.avatarColor(u.username()) + ";-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-font-size:12px;" +
            "-fx-min-width:32px;-fx-min-height:32px;-fx-max-width:32px;-fx-max-height:32px;" +
            "-fx-background-radius:16px;-fx-alignment:center;");

        VBox info = new VBox(1);
        Label name = new Label("@" + u.username());
        name.setStyle("-fx-font-weight:700;-fx-font-size:12px;-fx-text-fill:#1C1E21;");
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

        Label meta = new Label("\u2764 " + r.getLikesCount() +
            "  \u00B7  @" + r.getAuthorUsername() +
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

        // Header
        HBox header = new HBox(10);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setAlignment(Pos.CENTER_LEFT);

        String initial = r.getAuthorUsername() != null && !r.getAuthorUsername().isEmpty()
            ? r.getAuthorUsername().substring(0, 1).toUpperCase() : "?";
        Label avatar = new Label(initial);
        avatar.setStyle(
            "-fx-background-color:" + MainWindow.avatarColor(r.getAuthorUsername()) + ";-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-font-size:14px;" +
            "-fx-min-width:36px;-fx-min-height:36px;" +
            "-fx-max-width:36px;-fx-max-height:36px;" +
            "-fx-background-radius:18px;-fx-alignment:center;");

        VBox authorInfo = new VBox(1);
        Label authorLabel = new Label("@" + r.getAuthorUsername());
        authorLabel.setStyle("-fx-font-weight:700;-fx-font-size:14px;-fx-text-fill:#1C1E21;");
        Label timeLabel = new Label(r.getDrinkType() + "  \u00B7  " + r.getDifficulty());
        timeLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
        authorInfo.getChildren().addAll(authorLabel, timeLabel);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label typeChip = new Label(r.getDrinkType());
        typeChip.getStyleClass().add("detail-chip");

        header.getChildren().addAll(avatar, authorInfo, hSpacer, typeChip);

        // Body
        VBox body = new VBox(8);
        body.setPadding(new Insets(0, 20, 16, 20));

        Label titleLabel = new Label(r.getTitle());
        titleLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
        titleLabel.setWrapText(true);
        body.getChildren().add(titleLabel);

        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            String excerpt = r.getDescription().length() > 140
                ? r.getDescription().substring(0, 140) + "\u2026" : r.getDescription();
            Label desc = new Label(excerpt);
            desc.setStyle("-fx-font-size:14px;-fx-text-fill:#65676B;-fx-line-spacing:2;");
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        // Footer
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#E4E6EA;");

        HBox footer = new HBox(20);
        footer.setPadding(new Insets(10, 20, 14, 20));
        footer.setAlignment(Pos.CENTER_LEFT);

        Button likeBtn = new Button("\u2764  " + r.getLikesCount());
        likeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#65676B;" +
            "-fx-font-weight:600;-fx-cursor:hand;-fx-border-width:0;-fx-font-size:13px;");
        likeBtn.setOnAction(e -> {
            if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in."); return; }
            service.toggleLike(AppSession.userId(), r.getId());
            loadData();
        });

        Label timeInfo = new Label("\u23F1  " + r.getBrewTime() + " min");
        timeInfo.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");

        footer.getChildren().addAll(likeBtn, timeInfo);
        card.getChildren().addAll(header, body, sep, footer);
        return card;
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    private VBox emptyState(String emoji, String heading, String sub) {
        Label icon = new Label(emoji); icon.setStyle("-fx-font-size:48px;");
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

    public BorderPane getRoot() { return root; }
}
