package mn.edu.num.cafe.ui.desktop;

import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.FlowPane;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.application.BorgolService.UserView;
import mn.edu.num.cafe.core.domain.Recipe;

import java.util.List;

/**
 * Recipes pane — Instagram-style card feed with full CRUD + right panel.
 */
public class RecipesPane {

    private final BorderPane root;
    private final BorgolService service;
    private VBox feedBox;
    private VBox rightPanel;
    private String lastSearch  = "";
    private String filterType  = "ALL";
    private Timeline searchDebounce;

    public RecipesPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setStyle("-fx-background-color:#F0F2F5;");
        root.setTop(buildToolbar());
        root.setCenter(buildMainArea());
        searchDebounce = UiUtils.debounce(300, this::loadData);
        loadData();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("\uD83D\uDCD6  Recipes");
        title.getStyleClass().add("pane-title");

        TextField search = new TextField();
        search.setPromptText("Search recipes\u2026");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((o, old, v) -> {
            lastSearch = v;
            searchDebounce.playFromStart();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNew = new Button("+ New Recipe");
        btnNew.getStyleClass().add("btn-primary");
        btnNew.setOnAction(e -> showNewDialog());
        if (!AppSession.loggedIn()) btnNew.setDisable(true);

        bar.getChildren().addAll(title, search, spacer, btnNew);
        return bar;
    }

    // ── Three-column layout ───────────────────────────────────────────────────

    private HBox buildMainArea() {
        feedBox = new VBox(16);
        feedBox.setPadding(new Insets(20, 16, 20, 20));
        feedBox.setStyle("-fx-background-color:#F0F2F5;");

        ScrollPane feedScroll = new ScrollPane(feedBox);
        feedScroll.setFitToWidth(true);
        feedScroll.getStyleClass().add("feed-scroll");
        feedScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox.setHgrow(feedScroll, Priority.ALWAYS);

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

        return new HBox(0, feedScroll, rightScroll);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        feedBox.getChildren().clear();
        rightPanel.getChildren().clear();
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<Recipe> recipes = service.getRecipes(uid, lastSearch, filterType, "RECENT");
            if (recipes.isEmpty()) {
                feedBox.getChildren().add(UiUtils.emptyState(
                    "\uD83D\uDCD6", "No recipes yet",
                    "Be the first to share a coffee recipe!"));
            } else {
                for (Recipe r : recipes) feedBox.getChildren().add(buildRecipeCard(r));
            }
        } catch (Exception e) {
            MainWindow.alert("Error", e.getMessage());
        }
        buildRightPanel();
    }

    // ── Right panel ───────────────────────────────────────────────────────────

    private void buildRightPanel() {
        // Filter by type chips
        VBox filterCard = UiUtils.rightCard("FILTER BY TYPE");
        String[] types = {"ALL", "ESPRESSO", "LATTE", "POUR_OVER", "COLD_BREW", "CAPPUCCINO", "TEA"};
        FlowPane chips = new FlowPane(6, 6);
        for (String t : types) {
            Button chip = new Button(t);
            boolean active = t.equals(filterType);
            chip.setStyle(active
                ? "-fx-background-color:#D4621A;-fx-text-fill:white;" +
                  "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:4 10 4 10;" +
                  "-fx-background-radius:12;-fx-border-width:0;-fx-cursor:hand;"
                : "-fx-background-color:#F0F2F5;-fx-text-fill:#65676B;" +
                  "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:4 10 4 10;" +
                  "-fx-background-radius:12;-fx-border-width:0;-fx-cursor:hand;");
            chip.setOnAction(e -> {
                filterType = t;
                loadData();
            });
            chips.getChildren().add(chip);
        }
        filterCard.getChildren().add(chips);
        rightPanel.getChildren().add(filterCard);

        // Top liked
        VBox trendCard = UiUtils.rightCard("TOP LIKED");
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<Recipe> top = service.getRecipes(uid, "", "ALL", "POPULAR");
            int shown = 0;
            for (Recipe r : top) {
                trendCard.getChildren().add(buildTrendRow(r));
                if (++shown >= 6) break;
            }
        } catch (Exception ignored) {}
        rightPanel.getChildren().add(trendCard);

        // People to follow
        if (AppSession.loggedIn()) {
            VBox peopleCard = UiUtils.rightCard("PEOPLE TO FOLLOW");
            try {
                List<UserView> users = service.getAllUsers(AppSession.userId());
                int shown = 0;
                for (UserView u : users) {
                    if (u.id() == AppSession.userId() || u.isFollowing()) continue;
                    peopleCard.getChildren().add(buildSuggestRow(u));
                    if (++shown >= 3) break;
                }
                if (shown == 0) {
                    Label none = new Label("You\u2019re following everyone!");
                    none.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
                    peopleCard.getChildren().add(none);
                }
            } catch (Exception ignored) {}
            rightPanel.getChildren().add(peopleCard);
        }
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
            r.getLikesCount() + "  \u00B7  @" + r.getAuthorUsername());
        meta.setStyle("-fx-font-size:11px;-fx-text-fill:#65676B;");
        row.getChildren().addAll(titleLbl, meta);
        return row;
    }

    private HBox buildSuggestRow(UserView u) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label av = UiUtils.createAvatar(u.username(), 30);
        Label name = new Label("@" + u.username());
        name.setStyle("-fx-font-weight:700;-fx-font-size:12px;-fx-text-fill:#1C1E21;" +
            "-fx-cursor:hand;");
        name.setOnMouseClicked(e -> UiUtils.showUserProfileDialog(service, u.id()));
        HBox.setHgrow(name, Priority.ALWAYS);
        Button followBtn = new Button("+ Follow");
        followBtn.setStyle("-fx-background-color:#D4621A;-fx-text-fill:white;" +
            "-fx-font-weight:700;-fx-font-size:11px;-fx-padding:4 10 4 10;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        followBtn.setOnAction(e -> {
            service.followUser(AppSession.userId(), u.id());
            loadData();
        });
        row.getChildren().addAll(av, name, followBtn);
        return row;
    }

    // ── Recipe card ───────────────────────────────────────────────────────────

    private VBox buildRecipeCard(Recipe r) {
        VBox card = new VBox(0);
        card.getStyleClass().add("recipe-card");
        card.setMaxWidth(680);

        // Header
        HBox header = new HBox(10);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = UiUtils.createAvatar(r.getAuthorUsername(), 36);

        VBox authorInfo = new VBox(2);
        Label authorLabel = new Label("@" + r.getAuthorUsername());
        authorLabel.getStyleClass().add("username-link");
        authorLabel.setOnMouseClicked(e -> UiUtils.showUserProfileDialog(service, r.getAuthorId()));

        HBox chipRow = new HBox(6);
        chipRow.setAlignment(Pos.CENTER_LEFT);
        Label typeChip = new Label(r.getDrinkType());
        typeChip.getStyleClass().add("detail-chip");
        Label diffChip = new Label(r.getDifficulty());
        diffChip.setStyle("-fx-background-color:#F0F2F5;-fx-text-fill:#65676B;" +
            "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:3 10 3 10;-fx-background-radius:10;");
        chipRow.getChildren().addAll(typeChip, diffChip);
        authorInfo.getChildren().addAll(authorLabel, chipRow);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Label timeLabel = new Label("\u23F1  " + r.getBrewTime() + " min");
        timeLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");

        header.getChildren().addAll(avatar, authorInfo, hSpacer, timeLabel);

        // Body
        VBox body = new VBox(6);
        body.setPadding(new Insets(0, 20, 14, 20));
        Label titleLabel = new Label(r.getTitle());
        titleLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
        titleLabel.setWrapText(true);
        body.getChildren().add(titleLabel);

        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            String excerpt = r.getDescription().length() > 120
                ? r.getDescription().substring(0, 120) + "\u2026" : r.getDescription();
            Label desc = new Label(excerpt);
            desc.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;");
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        // Footer
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#E4E6EA;");

        HBox footer = new HBox(10);
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

        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:#F0F2F5;-fx-text-fill:#1C1E21;" +
            "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> UiUtils.showRecipeDetailDialog(service, r, this::loadData));

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);
        footer.getChildren().addAll(likeBtn, btnSpacer, viewBtn);

        if (AppSession.loggedIn() && r.getAuthorId() == AppSession.userId()) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color:#F0F2F5;-fx-text-fill:#1C1E21;" +
                "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
                "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
            editBtn.setOnAction(e -> showEditDialog(r));

            Button delBtn = new Button("Delete");
            delBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#DC2626;" +
                "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
                "-fx-background-radius:8;-fx-border-color:#DC2626;-fx-border-radius:8;" +
                "-fx-border-width:1;-fx-cursor:hand;");
            delBtn.setOnAction(e -> deleteRecipe(r));
            footer.getChildren().addAll(editBtn, delBtn);
        }

        card.getChildren().addAll(header, body, sep, footer);
        return card;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    private void showNewDialog() {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }

        Dialog<ButtonType> dlg = recipeDialog("New Recipe");
        GridPane grid  = MainWindow.formGrid();
        TextField title  = MainWindow.styledField("e.g. Perfect Pour-Over");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
            "ESPRESSO", "LATTE", "CAPPUCCINO", "AMERICANO", "COLD_BREW",
            "POUR_OVER", "FRENCH_PRESS", "TEA", "SMOOTHIE", "OTHER"));
        type.setValue("POUR_OVER"); type.getStyleClass().add("form-field");
        TextArea  desc   = MainWindow.styledArea("What makes this recipe special?", 2);
        TextArea  ing    = MainWindow.styledArea("One ingredient per line", 3);
        TextArea  inst   = MainWindow.styledArea("One step per line", 4);
        TextField time   = MainWindow.styledField("minutes");
        ComboBox<String> diff = new ComboBox<>(
            FXCollections.observableArrayList("EASY", "MEDIUM", "HARD"));
        diff.setValue("MEDIUM"); diff.getStyleClass().add("form-field");
        TextField imgUrl = MainWindow.styledField("https://… (optional)");

        grid.add(lbl("Title *"),      0, 0); grid.add(title,  1, 0);
        grid.add(lbl("Drink type *"), 0, 1); grid.add(type,   1, 1);
        grid.add(lbl("Description"),  0, 2); grid.add(desc,   1, 2);
        grid.add(lbl("Ingredients"),  0, 3); grid.add(ing,    1, 3);
        grid.add(lbl("Instructions"), 0, 4); grid.add(inst,   1, 4);
        grid.add(lbl("Brew time"),    0, 5); grid.add(time,   1, 5);
        grid.add(lbl("Difficulty"),   0, 6); grid.add(diff,   1, 6);
        grid.add(lbl("Image URL"),    0, 7); grid.add(imgUrl, 1, 7);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    service.createRecipe(AppSession.userId(),
                        title.getText().trim(), desc.getText().trim(),
                        type.getValue(),
                        ing.getText().trim(), inst.getText().trim(),
                        parseIntOr(time.getText(), 0), diff.getValue(),
                        List.of(), imgUrl.getText().trim());
                    loadData();
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    private void showEditDialog(Recipe sel) {
        if (!AppSession.loggedIn() || AppSession.userId() != sel.getAuthorId()) {
            MainWindow.alert("Not allowed", "You can only edit your own recipes."); return;
        }

        Dialog<ButtonType> dlg = recipeDialog("Edit Recipe");
        GridPane grid = MainWindow.formGrid();
        TextField title = MainWindow.styledField(""); title.setText(sel.getTitle());
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
            "ESPRESSO", "LATTE", "CAPPUCCINO", "AMERICANO", "COLD_BREW",
            "POUR_OVER", "FRENCH_PRESS", "TEA", "SMOOTHIE", "OTHER"));
        type.setValue(sel.getDrinkType()); type.getStyleClass().add("form-field");
        TextArea desc = MainWindow.styledArea("", 2); desc.setText(sel.getDescription());
        TextArea ing  = MainWindow.styledArea("", 3); ing.setText(sel.getIngredients());
        TextArea inst = MainWindow.styledArea("", 4); inst.setText(sel.getInstructions());
        TextField time = MainWindow.styledField(""); time.setText(String.valueOf(sel.getBrewTime()));
        ComboBox<String> diff = new ComboBox<>(
            FXCollections.observableArrayList("EASY", "MEDIUM", "HARD"));
        diff.setValue(sel.getDifficulty()); diff.getStyleClass().add("form-field");
        TextField imgUrl = MainWindow.styledField(""); imgUrl.setText(sel.getImageUrl() != null ? sel.getImageUrl() : "");

        grid.add(lbl("Title"),        0, 0); grid.add(title,  1, 0);
        grid.add(lbl("Drink type"),   0, 1); grid.add(type,   1, 1);
        grid.add(lbl("Description"),  0, 2); grid.add(desc,   1, 2);
        grid.add(lbl("Ingredients"),  0, 3); grid.add(ing,    1, 3);
        grid.add(lbl("Instructions"), 0, 4); grid.add(inst,   1, 4);
        grid.add(lbl("Brew time"),    0, 5); grid.add(time,   1, 5);
        grid.add(lbl("Difficulty"),   0, 6); grid.add(diff,   1, 6);
        grid.add(lbl("Image URL"),    0, 7); grid.add(imgUrl, 1, 7);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    service.updateRecipe(sel.getId(), AppSession.userId(),
                        title.getText().trim(), desc.getText().trim(), type.getValue(),
                        ing.getText().trim(), inst.getText().trim(),
                        parseIntOr(time.getText(), 0), diff.getValue(),
                        sel.getFlavorTags(), imgUrl.getText().trim());
                    loadData();
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    private void deleteRecipe(Recipe sel) {
        if (!AppSession.loggedIn() || AppSession.userId() != sel.getAuthorId()) {
            MainWindow.alert("Not allowed", "You can only delete your own recipes."); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + sel.getTitle() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                service.deleteRecipe(sel.getId(), AppSession.userId());
                loadData();
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Dialog<ButtonType> recipeDialog(String title) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        UiUtils.addStylesheet(dlg, RecipesPane.class);
        dlg.getDialogPane().setPrefWidth(480);
        return dlg;
    }

    private static Label lbl(String text) {
        Label l = new Label(text); l.getStyleClass().add("form-label"); return l;
    }

    private static int parseIntOr(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    public BorderPane getRoot() { return root; }
}
