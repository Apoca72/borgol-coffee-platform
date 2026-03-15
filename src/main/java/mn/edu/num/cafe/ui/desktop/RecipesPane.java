package mn.edu.num.cafe.ui.desktop;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.FlowPane;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.application.BorgolService.UserView;
import mn.edu.num.cafe.core.domain.Recipe;
import mn.edu.num.cafe.core.domain.RecipeComment;

import java.util.List;

/**
 * Recipes pane — Instagram-style card feed with full CRUD + right panel.
 */
public class RecipesPane {

    private final BorderPane root;
    private final BorgolService service;
    private VBox feedBox;
    private VBox rightPanel;
    private String lastSearch = "";

    public RecipesPane(BorgolService service) {
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

        Label title = new Label("\uD83D\uDCD6  Recipes");
        title.getStyleClass().add("pane-title");

        TextField search = new TextField();
        search.setPromptText("Search recipes\u2026");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((o, old, v) -> {
            lastSearch = v;
            loadData();
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

        HBox area = new HBox(0, feedScroll, rightScroll);
        area.setStyle("-fx-background-color:#F0F2F5;");
        return area;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        feedBox.getChildren().clear();
        rightPanel.getChildren().clear();
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<Recipe> recipes = service.getRecipes(uid, lastSearch, "ALL", "RECENT");
            if (recipes.isEmpty()) {
                feedBox.getChildren().add(emptyState(
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
        VBox filterCard = rightCard("FILTER BY TYPE");
        String[] types = {"ALL", "ESPRESSO", "LATTE", "POUR_OVER", "COLD_BREW", "CAPPUCCINO", "TEA"};
        FlowPane chips = new FlowPane(6, 6);
        for (String t : types) {
            Button chip = new Button(t);
            chip.setStyle("-fx-background-color:#F0F2F5;-fx-text-fill:#65676B;" +
                "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:4 10 4 10;" +
                "-fx-background-radius:12;-fx-border-width:0;-fx-cursor:hand;");
            chip.setOnAction(e -> {
                lastSearch = t.equals("ALL") ? "" : t;
                loadData();
            });
            chips.getChildren().add(chip);
        }
        filterCard.getChildren().add(chips);
        rightPanel.getChildren().add(filterCard);

        // Top liked
        VBox trendCard = rightCard("TOP LIKED");
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
            VBox peopleCard = rightCard("PEOPLE TO FOLLOW");
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

    private VBox rightCard(String heading) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
            "-fx-padding:16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");
        Label h = new Label(heading);
        h.getStyleClass().add("detail-heading");
        card.getChildren().add(h);
        return card;
    }

    private VBox buildTrendRow(Recipe r) {
        VBox row = new VBox(2);
        row.setPadding(new Insets(4, 0, 4, 0));
        row.setStyle("-fx-border-color:transparent transparent #F0F2F5 transparent;" +
            "-fx-border-width:0 0 1 0;");
        Label titleLbl = new Label(r.getTitle());
        titleLbl.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#1C1E21;");
        titleLbl.setWrapText(true);
        Label meta = new Label("\u2764 " + r.getLikesCount() + "  \u00B7  @" + r.getAuthorUsername());
        meta.setStyle("-fx-font-size:11px;-fx-text-fill:#65676B;");
        row.getChildren().addAll(titleLbl, meta);
        return row;
    }

    private HBox buildSuggestRow(UserView u) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        String ini = u.username().substring(0, 1).toUpperCase();
        Label av = new Label(ini);
        av.setStyle("-fx-background-color:" + MainWindow.avatarColor(u.username()) + ";-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-font-size:12px;" +
            "-fx-min-width:30px;-fx-min-height:30px;-fx-max-width:30px;-fx-max-height:30px;" +
            "-fx-background-radius:15px;-fx-alignment:center;");
        Label name = new Label("@" + u.username());
        name.setStyle("-fx-font-weight:700;-fx-font-size:12px;-fx-text-fill:#1C1E21;");
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

        // ── Header ──
        HBox header = new HBox(10);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setAlignment(Pos.CENTER_LEFT);

        String initial = r.getAuthorUsername() != null && !r.getAuthorUsername().isEmpty()
            ? r.getAuthorUsername().substring(0, 1).toUpperCase() : "?";
        Label avatar = new Label(initial);
        avatar.setStyle(
            "-fx-background-color:" + MainWindow.avatarColor(r.getAuthorUsername()) + ";-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-font-size:14px;" +
            "-fx-min-width:36px;-fx-min-height:36px;-fx-max-width:36px;-fx-max-height:36px;" +
            "-fx-background-radius:18px;-fx-alignment:center;");

        VBox authorInfo = new VBox(2);
        Label authorLabel = new Label("@" + r.getAuthorUsername());
        authorLabel.setStyle("-fx-font-weight:700;-fx-font-size:14px;-fx-text-fill:#1C1E21;");
        HBox chipRow = new HBox(6);
        chipRow.setAlignment(Pos.CENTER_LEFT);
        Label typeChip = new Label(r.getDrinkType());
        typeChip.getStyleClass().add("detail-chip");
        Label diffChip = new Label(r.getDifficulty());
        diffChip.setStyle(
            "-fx-background-color:#F0F2F5;-fx-text-fill:#65676B;" +
            "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:3 10 3 10;-fx-background-radius:10;");
        chipRow.getChildren().addAll(typeChip, diffChip);
        authorInfo.getChildren().addAll(authorLabel, chipRow);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label timeLabel = new Label("\u23F1  " + r.getBrewTime() + " min");
        timeLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");

        header.getChildren().addAll(avatar, authorInfo, hSpacer, timeLabel);

        // ── Body ──
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

        // ── Footer ──
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#E4E6EA;");

        HBox footer = new HBox(10);
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

        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:#F0F2F5;-fx-text-fill:#1C1E21;" +
            "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> showDetailDialog(r));

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

    // ── Detail dialog ─────────────────────────────────────────────────────────

    private void showDetailDialog(Recipe r) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(r.getTitle());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().getStylesheets().add(
            RecipesPane.class.getResource("/style.css").toExternalForm());
        dlg.getDialogPane().setPrefWidth(560);
        dlg.getDialogPane().setPrefHeight(680);

        VBox box = new VBox(0);

        // Header
        VBox dlgHeader = new VBox(8);
        dlgHeader.setPadding(new Insets(20, 24, 16, 24));
        dlgHeader.setStyle("-fx-background-color:white;");

        Label titleLbl = new Label(r.getTitle());
        titleLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
        titleLbl.setWrapText(true);

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        Label typeChip = new Label(r.getDrinkType()); typeChip.getStyleClass().add("detail-chip");
        Label diffChip = new Label(r.getDifficulty()); diffChip.getStyleClass().add("detail-chip");
        Label timeChip = new Label("\u23F1 " + r.getBrewTime() + " min"); timeChip.getStyleClass().add("detail-chip");
        Label likesChip = new Label("\u2764 " + r.getLikesCount()); likesChip.getStyleClass().add("detail-chip");
        chips.getChildren().addAll(typeChip, diffChip, timeChip, likesChip);

        String ini = r.getAuthorUsername() != null && !r.getAuthorUsername().isEmpty()
            ? r.getAuthorUsername().substring(0, 1).toUpperCase() : "?";
        Label av = new Label(ini);
        av.setStyle("-fx-background-color:" + MainWindow.avatarColor(r.getAuthorUsername()) + ";-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-font-size:12px;-fx-min-width:28px;-fx-min-height:28px;-fx-max-width:28px;-fx-max-height:28px;" +
            "-fx-background-radius:14px;-fx-alignment:center;");
        Label authorLbl = new Label("@" + r.getAuthorUsername());
        authorLbl.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#65676B;");
        HBox authorRow = new HBox(8, av, authorLbl);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        dlgHeader.getChildren().addAll(titleLbl, chips, authorRow);

        // Scrollable body
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
            VBox section = contentSection("INGREDIENTS");
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
            VBox section = contentSection("INSTRUCTIONS");
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

        // Comments
        VBox commSection = contentSection("COMMENTS");
        ListView<RecipeComment> commentList = new ListView<>();
        commentList.setPrefHeight(140);
        commentList.getStyleClass().add("list-view");
        commentList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(RecipeComment c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) { setText(null); setGraphic(null); return; }
                HBox row = new HBox(10);
                row.setAlignment(Pos.TOP_LEFT);
                String i2 = c.getAuthorUsername() != null && !c.getAuthorUsername().isEmpty()
                    ? c.getAuthorUsername().substring(0, 1).toUpperCase() : "?";
                Label av2 = new Label(i2);
                av2.setStyle("-fx-background-color:" + MainWindow.avatarColor(c.getAuthorUsername()) + ";-fx-text-fill:white;" +
                    "-fx-font-weight:bold;-fx-font-size:10px;" +
                    "-fx-min-width:24px;-fx-min-height:24px;-fx-max-width:24px;-fx-max-height:24px;" +
                    "-fx-background-radius:12px;-fx-alignment:center;");
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
        refreshComments(r.getId(), commentList);
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
                        refreshComments(r.getId(), commentList);
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
    }

    private VBox contentSection(String heading) {
        VBox section = new VBox(8);
        section.setStyle("-fx-background-color:white;-fx-background-radius:10;" +
            "-fx-padding:14 16 14 16;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,1);");
        Label h = new Label(heading);
        h.getStyleClass().add("detail-heading");
        section.getChildren().add(h);
        return section;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    private void showNewDialog() {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }

        Dialog<ButtonType> dlg = recipeDialog("New Recipe");
        GridPane grid  = MainWindow.formGrid();
        TextField title  = MainWindow.styledField("e.g. Perfect Pour-Over");
        TextField type   = MainWindow.styledField("ESPRESSO / LATTE / POUR_OVER \u2026");
        TextArea  desc   = MainWindow.styledArea("What makes this recipe special?", 2);
        TextArea  ing    = MainWindow.styledArea("One ingredient per line", 3);
        TextArea  inst   = MainWindow.styledArea("One step per line", 4);
        TextField time   = MainWindow.styledField("minutes");
        ComboBox<String> diff = new ComboBox<>(
            FXCollections.observableArrayList("EASY", "MEDIUM", "HARD"));
        diff.setValue("MEDIUM"); diff.getStyleClass().add("form-field");

        grid.add(lbl("Title *"),       0, 0); grid.add(title, 1, 0);
        grid.add(lbl("Drink type *"),  0, 1); grid.add(type,  1, 1);
        grid.add(lbl("Description"),   0, 2); grid.add(desc,  1, 2);
        grid.add(lbl("Ingredients"),   0, 3); grid.add(ing,   1, 3);
        grid.add(lbl("Instructions"),  0, 4); grid.add(inst,  1, 4);
        grid.add(lbl("Brew time"),     0, 5); grid.add(time,  1, 5);
        grid.add(lbl("Difficulty"),    0, 6); grid.add(diff,  1, 6);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    service.createRecipe(AppSession.userId(),
                        title.getText().trim(), desc.getText().trim(),
                        type.getText().trim().toUpperCase(),
                        ing.getText().trim(), inst.getText().trim(),
                        parseIntOr(time.getText(), 0), diff.getValue(), List.of(), "");
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
        TextArea desc = MainWindow.styledArea("", 2); desc.setText(sel.getDescription());
        TextArea ing  = MainWindow.styledArea("", 3); ing.setText(sel.getIngredients());
        TextArea inst = MainWindow.styledArea("", 4); inst.setText(sel.getInstructions());
        TextField time = MainWindow.styledField(""); time.setText(String.valueOf(sel.getBrewTime()));
        ComboBox<String> diff = new ComboBox<>(
            FXCollections.observableArrayList("EASY", "MEDIUM", "HARD"));
        diff.setValue(sel.getDifficulty()); diff.getStyleClass().add("form-field");

        grid.add(lbl("Title"),        0, 0); grid.add(title, 1, 0);
        grid.add(lbl("Description"),  0, 1); grid.add(desc,  1, 1);
        grid.add(lbl("Ingredients"),  0, 2); grid.add(ing,   1, 2);
        grid.add(lbl("Instructions"), 0, 3); grid.add(inst,  1, 3);
        grid.add(lbl("Brew time"),    0, 4); grid.add(time,  1, 4);
        grid.add(lbl("Difficulty"),   0, 5); grid.add(diff,  1, 5);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    service.updateRecipe(sel.getId(), AppSession.userId(),
                        title.getText().trim(), desc.getText().trim(), sel.getDrinkType(),
                        ing.getText().trim(), inst.getText().trim(),
                        parseIntOr(time.getText(), 0), diff.getValue(),
                        sel.getFlavorTags(), sel.getImageUrl());
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

    private void refreshComments(int recipeId, ListView<RecipeComment> list) {
        try { list.getItems().setAll(service.getComments(recipeId)); } catch (Exception ignored) {}
    }

    private static Dialog<ButtonType> recipeDialog(String title) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().add(
            RecipesPane.class.getResource("/style.css").toExternalForm());
        dlg.getDialogPane().setPrefWidth(480);
        return dlg;
    }

    private static VBox emptyState(String emoji, String heading, String sub) {
        Label icon = new Label(emoji); icon.setStyle("-fx-font-size:48px;");
        Label h = new Label(heading); h.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
        Label s = new Label(sub); s.setStyle("-fx-font-size:14px;-fx-text-fill:#65676B;"); s.setWrapText(true);
        VBox box = new VBox(10, icon, h, s);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 40, 40, 40));
        box.setStyle("-fx-background-color:white;-fx-background-radius:12;");
        box.setMaxWidth(480);
        return box;
    }

    private static Label lbl(String text) {
        Label l = new Label(text); l.getStyleClass().add("form-label"); return l;
    }

    private static int parseIntOr(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    public BorderPane getRoot() { return root; }
}
