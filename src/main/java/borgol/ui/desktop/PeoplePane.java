package borgol.ui.desktop;

import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import borgol.core.application.BorgolService;
import borgol.core.application.BorgolService.UserView;

import java.util.List;

/**
 * People pane — card-row layout for user discovery, follow/unfollow, profiles.
 */
public class PeoplePane {

    private final BorderPane root;
    private final BorgolService service;
    private VBox listBox;
    private String lastQuery = "";
    private Timeline searchDebounce;

    public PeoplePane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setStyle("-fx-background-color:#F0F2F5;");
        root.setTop(buildToolbar());
        root.setCenter(buildScrollList());
        searchDebounce = UiUtils.debounce(300, () -> loadData(lastQuery));
        loadData("");
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("\uD83D\uDC65  People");
        title.getStyleClass().add("pane-title");

        TextField search = new TextField();
        search.setPromptText("Search users\u2026");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((o, old, v) -> {
            lastQuery = v;
            searchDebounce.playFromStart();
        });

        bar.getChildren().addAll(title, search);
        return bar;
    }

    // ── Scrollable list ───────────────────────────────────────────────────────

    private ScrollPane buildScrollList() {
        listBox = new VBox(12);
        listBox.setPadding(new Insets(20, 40, 20, 40));
        listBox.setStyle("-fx-background-color:#F0F2F5;");
        listBox.setMaxWidth(720);

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F0F2F5;-fx-background:#F0F2F5;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData(String query) {
        listBox.getChildren().clear();
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<UserView> users = (query == null || query.isBlank())
                ? service.getAllUsers(uid)
                : service.searchUsers(query, uid);

            if (users.isEmpty()) {
                listBox.getChildren().add(emptyLabel("No users found."));
                return;
            }
            for (UserView u : users) {
                if (AppSession.loggedIn() && u.id() == AppSession.userId()) continue;
                listBox.getChildren().add(buildUserCard(u));
            }
        } catch (Exception e) {
            MainWindow.alert("Error", e.getMessage());
        }
    }

    // ── User card row ─────────────────────────────────────────────────────────

    private HBox buildUserCard(UserView u) {
        HBox card = new HBox(14);
        card.getStyleClass().add("user-card");
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(680);

        // Avatar
        javafx.scene.Node avatar = UiUtils.createAvatar(u.username(), 44);

        // Info
        VBox info = new VBox(3);
        Label nameLabel = new Label("@" + u.username());
        nameLabel.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:#1C1E21;");

        Label levelChip = new Label(u.expertiseLevel() != null ? u.expertiseLevel() : "BEGINNER");
        levelChip.getStyleClass().add("detail-chip");
        levelChip.setStyle("-fx-background-color:#FFF0E0;-fx-text-fill:#D4621A;" +
            "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:3 10 3 10;-fx-background-radius:10;");

        HBox nameRow = new HBox(8, nameLabel, levelChip);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label stats = new Label(
            u.recipeCount() + " recipes  \u00B7  " +
            u.followerCount() + " followers  \u00B7  " +
            u.followingCount() + " following");
        stats.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");

        if (u.bio() != null && !u.bio().isBlank()) {
            String excerpt = u.bio().length() > 80 ? u.bio().substring(0, 80) + "\u2026" : u.bio();
            Label bio = new Label(excerpt);
            bio.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");
            info.getChildren().addAll(nameRow, stats, bio);
        } else {
            info.getChildren().addAll(nameRow, stats);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        Button profileBtn = new Button("View");
        profileBtn.setStyle("-fx-background-color:#E4E6EA;-fx-text-fill:#1C1E21;" +
            "-fx-font-weight:600;-fx-font-size:13px;-fx-padding:6 14 6 14;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        profileBtn.setOnAction(e -> UiUtils.showUserProfileDialog(service, u.id()));

        if (AppSession.loggedIn()) {
            if (u.isFollowing()) {
                Button unfollowBtn = new Button("\u2714 Following");
                unfollowBtn.setStyle("-fx-background-color:#FFF0E0;-fx-text-fill:#D4621A;" +
                    "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:6 14 6 14;" +
                    "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
                unfollowBtn.setOnAction(e -> {
                    service.unfollowUser(AppSession.userId(), u.id());
                    loadData(lastQuery);
                });
                actions.getChildren().addAll(profileBtn, unfollowBtn);
            } else {
                Button followBtn = new Button("+ Follow");
                followBtn.setStyle("-fx-background-color:#D4621A;-fx-text-fill:white;" +
                    "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:6 14 6 14;" +
                    "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
                followBtn.setOnAction(e -> {
                    service.followUser(AppSession.userId(), u.id());
                    loadData(lastQuery);
                });
                actions.getChildren().addAll(profileBtn, followBtn);
            }
        } else {
            actions.getChildren().add(profileBtn);
        }

        card.getChildren().addAll(avatar, info, spacer, actions);
        return card;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#65676B;-fx-font-size:14px;-fx-padding:20;");
        return l;
    }

    public BorderPane getRoot() { return root; }
}
