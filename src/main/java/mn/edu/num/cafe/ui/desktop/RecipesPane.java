package mn.edu.num.cafe.ui.desktop;

import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.application.BorgolService.UserView;
import mn.edu.num.cafe.core.domain.Recipe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/**
 * Recipes pane — card feed with full CRUD.
 * Complex recipe dialog: photo picker, structured ingredients/steps, flavor tags,
 * brew time, difficulty, and "Use in Timer" integration.
 */
public class RecipesPane {

    private final BorderPane root;
    private final BorgolService service;
    private VBox feedBox;
    private VBox rightPanel;
    private String lastSearch  = "";
    private String filterType  = "ALL";
    private String sortOrder   = "RECENT";
    private Timeline searchDebounce;

    /** Called when user taps "Use in Timer" — passes recipe brew time + steps */
    private Consumer<Recipe> onUseInTimer;

    public RecipesPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        root.setTop(buildToolbar());
        root.setCenter(buildMainArea());
        searchDebounce = UiUtils.debounce(300, this::loadData);
        loadData();
    }

    public void setOnUseInTimer(Consumer<Recipe> callback) { this.onUseInTimer = callback; }

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

        // Sort toggle
        ToggleGroup sortGroup = new ToggleGroup();
        ToggleButton tRecent  = new ToggleButton("Recent");
        ToggleButton tPopular = new ToggleButton("Popular");
        tRecent.setToggleGroup(sortGroup);
        tPopular.setToggleGroup(sortGroup);
        tRecent.setSelected(true);
        String tActive = "-fx-background-color:#A8621E;-fx-text-fill:#F5C060;-fx-font-size:11px;" +
            "-fx-font-weight:700;-fx-padding:6 12 6 12;-fx-cursor:hand;-fx-border-width:0;";
        String tInact  = "-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.sub() + ";-fx-font-size:11px;" +
            "-fx-font-weight:600;-fx-padding:6 12 6 12;-fx-cursor:hand;-fx-border-width:0;";
        tRecent.setStyle(tActive); tPopular.setStyle(tInact);
        sortGroup.selectedToggleProperty().addListener((o, old, v) -> {
            if (v == tRecent)       { sortOrder = "RECENT";  tRecent.setStyle(tActive);  tPopular.setStyle(tInact); }
            else if (v == tPopular) { sortOrder = "POPULAR"; tPopular.setStyle(tActive); tRecent.setStyle(tInact); }
            if (v != null) loadData();
        });
        HBox sortBox = new HBox(0, tRecent, tPopular);
        sortBox.setStyle("-fx-border-radius:8;-fx-background-radius:8;" +
            "-fx-border-color:" + UiUtils.border() + ";-fx-border-width:1;");

        Button btnNew = new Button("+ New Recipe");
        btnNew.getStyleClass().add("btn-primary");
        btnNew.setOnAction(e -> showNewDialog());
        if (!AppSession.loggedIn()) btnNew.setDisable(true);

        bar.getChildren().addAll(title, search, spacer, sortBox, btnNew);
        return bar;
    }

    // ── Layout ────────────────────────────────────────────────────────────────

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
        rightPanel.setMinWidth(260);
        rightPanel.setMaxWidth(260);

        ScrollPane rightScroll = new ScrollPane(rightPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background-color:" + UiUtils.bg() + ";-fx-background:" + UiUtils.bg() + ";");
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setMinWidth(260); rightScroll.setMaxWidth(260);

        return new HBox(0, feedScroll, rightScroll);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        feedBox.getChildren().clear();
        rightPanel.getChildren().clear();
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<Recipe> recipes = service.getRecipes(uid, lastSearch, filterType, sortOrder);
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
        // Filter chips
        VBox filterCard = UiUtils.rightCard("FILTER BY TYPE");
        String[] types = {"ALL","ESPRESSO","LATTE","POUR_OVER","COLD_BREW","CAPPUCCINO","FRENCH_PRESS","TEA","SMOOTHIE"};
        javafx.scene.layout.FlowPane chips = new javafx.scene.layout.FlowPane(6, 6);
        for (String t : types) {
            Button chip = new Button(t.replace("_", " "));
            boolean active = t.equals(filterType);
            chip.setStyle(active
                ? "-fx-background-color:#A8621E;-fx-text-fill:#F5C060;" +
                  "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:4 10 4 10;" +
                  "-fx-background-radius:12;-fx-border-width:0;-fx-cursor:hand;"
                : "-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.sub() + ";" +
                  "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:4 10 4 10;" +
                  "-fx-background-radius:12;-fx-border-width:0;-fx-cursor:hand;");
            chip.setOnAction(e -> { filterType = t; loadData(); });
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
                if (++shown >= 5) break;
            }
        } catch (Exception ignored) {}
        rightPanel.getChildren().add(trendCard);

        // Saved by me
        if (AppSession.loggedIn()) {
            VBox savedCard = UiUtils.rightCard("\u2665 MY SAVED");
            try {
                List<Recipe> liked = service.getSavedRecipes(AppSession.userId(), AppSession.userId());
                int shown = 0;
                for (Recipe r : liked) {
                    Label lbl = new Label("\u2764 " + r.getTitle());
                    lbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.text() + ";-fx-cursor:hand;");
                    lbl.setWrapText(true);
                    lbl.setOnMouseClicked(e -> UiUtils.showRecipeDetailDialog(service, r, this::loadData));
                    savedCard.getChildren().add(lbl);
                    if (++shown >= 4) break;
                }
                if (shown == 0) {
                    Label none = new Label("No liked recipes yet.");
                    none.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.sub() + ";");
                    savedCard.getChildren().add(none);
                }
            } catch (Exception ignored) {}
            rightPanel.getChildren().add(savedCard);
        }
    }

    private VBox buildTrendRow(Recipe r) {
        VBox row = new VBox(2);
        row.setPadding(new Insets(4, 0, 4, 0));
        row.setStyle("-fx-border-color:transparent transparent " + UiUtils.border() + " transparent;-fx-border-width:0 0 1 0;");
        Label titleLbl = new Label(r.getTitle());
        titleLbl.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:" + UiUtils.text() + ";-fx-cursor:hand;");
        titleLbl.setWrapText(true);
        titleLbl.setOnMouseClicked(e -> UiUtils.showRecipeDetailDialog(service, r, this::loadData));
        Label meta = new Label("\u2661 " + r.getLikesCount() + "  \u00B7  @" + r.getAuthorUsername());
        meta.setStyle("-fx-font-size:11px;-fx-text-fill:" + UiUtils.sub() + ";");
        row.getChildren().addAll(titleLbl, meta);
        return row;
    }

    // ── Recipe card ───────────────────────────────────────────────────────────

    private VBox buildRecipeCard(Recipe r) {
        VBox card = new VBox(0);
        card.getStyleClass().add("recipe-card");
        card.setMaxWidth(680);
        card.setStyle(
            "-fx-background-color:" + UiUtils.card() + ";" +
            "-fx-background-radius:14;-fx-border-radius:14;" +
            "-fx-border-color:" + UiUtils.border() + ";-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(12,4,0,0.07),8,0,0,1);");

        // Image
        if (r.getImageUrl() != null && !r.getImageUrl().isBlank()) {
            try {
                Image img = new Image(r.getImageUrl(), 0, 0, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                StackPane banner = new StackPane(iv);
                banner.setPrefHeight(200); banner.setMaxHeight(200);
                banner.setStyle("-fx-background-color:" + UiUtils.border() + ";-fx-background-radius:14 14 0 0;");
                Rectangle clip = new Rectangle();
                clip.widthProperty().bind(banner.widthProperty());
                clip.setHeight(200); clip.setArcWidth(14); clip.setArcHeight(14);
                banner.setClip(clip);
                iv.fitWidthProperty().bind(banner.widthProperty());
                card.getChildren().add(banner);
            } catch (Exception ignored) {}
        }

        // Header
        HBox header = new HBox(10);
        header.setPadding(new Insets(14, 18, 10, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.Node avatar = UiUtils.createAvatar(r.getAuthorUsername(), 34);
        VBox authorInfo = new VBox(2);
        Label authorLabel = new Label("@" + r.getAuthorUsername());
        authorLabel.getStyleClass().add("username-link");
        authorLabel.setOnMouseClicked(e -> UiUtils.showUserProfileDialog(service, r.getAuthorId()));
        HBox chipRow = new HBox(5);
        Label typeChip = chip(r.getDrinkType().replace("_", " "), "#A8621E", "#FFF0D8");
        Label diffChip = chip(r.getDifficulty(), diffColor(r.getDifficulty()), diffBg(r.getDifficulty()));
        chipRow.getChildren().addAll(typeChip, diffChip);
        authorInfo.getChildren().addAll(authorLabel, chipRow);
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Label timeLabel = new Label("\u23F1 " + r.getBrewTime() + " min");
        timeLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.sub() + ";");
        header.getChildren().addAll(avatar, authorInfo, hSpacer, timeLabel);

        // Body
        VBox body = new VBox(5);
        body.setPadding(new Insets(0, 18, 12, 18));
        Label titleLabel = new Label(r.getTitle());
        titleLabel.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + UiUtils.text() + ";");
        titleLabel.setWrapText(true);
        body.getChildren().add(titleLabel);
        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            String excerpt = r.getDescription().length() > 130
                ? r.getDescription().substring(0, 130) + "\u2026" : r.getDescription();
            Label desc = new Label(excerpt);
            desc.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";");
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }
        // Flavor tags
        if (r.getFlavorTags() != null && !r.getFlavorTags().isEmpty()) {
            HBox tags = new HBox(5);
            for (String t : r.getFlavorTags()) {
                Label tag = new Label(t);
                tag.setStyle("-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.sub() + ";" +
                    "-fx-font-size:10px;-fx-font-weight:700;-fx-padding:2 7 2 7;" +
                    "-fx-background-radius:8;-fx-text-transform:uppercase;");
                tags.getChildren().add(tag);
            }
            body.getChildren().add(tags);
        }

        // Footer
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + UiUtils.border() + ";");

        HBox footer = new HBox(8);
        footer.setPadding(new Insets(10, 18, 12, 18));
        footer.setAlignment(Pos.CENTER_LEFT);

        boolean liked = r.isLikedByCurrentUser();
        Button likeBtn = new Button((liked ? "\u2764" : "\u2661") + "  " + r.getLikesCount());
        likeBtn.setStyle(liked
            ? "-fx-background-color:transparent;-fx-text-fill:#E03040;-fx-font-weight:700;" +
              "-fx-cursor:hand;-fx-border-width:0;-fx-font-size:13px;"
            : "-fx-background-color:transparent;-fx-text-fill:" + UiUtils.sub() + ";" +
              "-fx-font-weight:600;-fx-cursor:hand;-fx-border-width:0;-fx-font-size:13px;");
        likeBtn.setOnAction(e -> {
            if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in."); return; }
            service.toggleLike(AppSession.userId(), r.getId());
            loadData();
        });

        Label commentLbl = new Label("\uD83D\uDCAC  " + r.getCommentCount());
        commentLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";");

        Region btnSpacer = new Region(); HBox.setHgrow(btnSpacer, Priority.ALWAYS);

        // Timer button
        Button timerBtn = new Button("\u23F1 Timer");
        timerBtn.setStyle(
            "-fx-background-color:rgba(168,98,30,0.12);-fx-text-fill:#A8621E;" +
            "-fx-font-weight:700;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        timerBtn.setOnAction(e -> {
            if (onUseInTimer != null) onUseInTimer.accept(r);
        });

        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.text() + ";" +
            "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> UiUtils.showRecipeDetailDialog(service, r, this::loadData));

        Button saveBtn = UiUtils.saveButton(service, r, this::loadData);
        footer.getChildren().addAll(likeBtn, commentLbl, btnSpacer, saveBtn, timerBtn, viewBtn);

        if (AppSession.loggedIn() && r.getAuthorId() == AppSession.userId()) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.text() + ";" +
                "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
                "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
            editBtn.setOnAction(e -> showEditDialog(r));
            Button delBtn = new Button("Delete");
            delBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#DC2626;" +
                "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
                "-fx-background-radius:8;-fx-border-color:#DC2626;-fx-border-radius:8;-fx-border-width:1;-fx-cursor:hand;");
            delBtn.setOnAction(e -> deleteRecipe(r));
            footer.getChildren().addAll(editBtn, delBtn);
        }

        card.getChildren().addAll(header, body, sep, footer);
        return card;
    }

    // ── Complex Recipe Dialog ─────────────────────────────────────────────────

    private void showNewDialog() {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }
        RecipeFormResult result = showRecipeForm("New Recipe", null);
        if (result == null) return;
        try {
            service.createRecipe(AppSession.userId(),
                result.title(), result.description(),
                result.drinkType(), result.ingredients(), result.instructions(),
                result.brewTime(), result.difficulty(),
                result.flavorTags(), result.imageData());
            loadData();
            UiUtils.showToast("Recipe saved!");
        } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
    }

    private void showEditDialog(Recipe sel) {
        if (!AppSession.loggedIn() || AppSession.userId() != sel.getAuthorId()) {
            MainWindow.alert("Not allowed", "You can only edit your own recipes."); return;
        }
        RecipeFormResult result = showRecipeForm("Edit Recipe", sel);
        if (result == null) return;
        try {
            service.updateRecipe(sel.getId(), AppSession.userId(),
                result.title(), result.description(), result.drinkType(),
                result.ingredients(), result.instructions(),
                result.brewTime(), result.difficulty(),
                result.flavorTags(),
                result.imageData().isBlank() ? (sel.getImageUrl() != null ? sel.getImageUrl() : "") : result.imageData());
            loadData();
            UiUtils.showToast("Recipe updated!");
        } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
    }

    /**
     * Shows the full recipe creation/edit form dialog.
     * Tabbed layout: Basic Info | Ingredients | Instructions | Details
     * Returns null if cancelled.
     */
    private RecipeFormResult showRecipeForm(String title, Recipe prefill) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        ButtonType saveBtn = new ButtonType("Save Recipe", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        UiUtils.addStylesheet(dlg, RecipesPane.class);
        dlg.getDialogPane().setPrefWidth(600);
        dlg.getDialogPane().setPrefHeight(640);

        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color:" + UiUtils.card() + ";");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ── Tab 1: Basic Info ────────────────────────────────────────────────
        TextField titleFld = styledField(prefill != null ? prefill.getTitle() : "", "Recipe name e.g. Honey Latte");
        ComboBox<String> typeFld = new ComboBox<>(FXCollections.observableArrayList(
            "ESPRESSO","LATTE","CAPPUCCINO","AMERICANO","COLD_BREW",
            "POUR_OVER","FRENCH_PRESS","TEA","SMOOTHIE","OTHER"));
        typeFld.setValue(prefill != null ? prefill.getDrinkType() : "POUR_OVER");
        typeFld.setMaxWidth(Double.MAX_VALUE);
        typeFld.getStyleClass().add("form-field");

        ComboBox<String> diffFld = new ComboBox<>(FXCollections.observableArrayList("EASY","MEDIUM","HARD","EXPERT"));
        diffFld.setValue(prefill != null ? prefill.getDifficulty() : "MEDIUM");
        diffFld.setMaxWidth(Double.MAX_VALUE);
        diffFld.getStyleClass().add("form-field");

        TextField timeFld = styledField(prefill != null ? String.valueOf(prefill.getBrewTime()) : "5", "minutes");

        // Brew time quick-select
        HBox timeRow = new HBox(8);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        timeRow.getChildren().add(timeFld);
        HBox.setHgrow(timeFld, Priority.ALWAYS);
        for (int t : new int[]{1, 3, 5, 10, 15, 30}) {
            Button q = new Button(t + "m");
            q.setStyle("-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.sub() + ";" +
                "-fx-font-size:11px;-fx-padding:4 8 4 8;-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
            q.setOnAction(e -> timeFld.setText(String.valueOf(t)));
            timeRow.getChildren().add(q);
        }

        // Difficulty visual
        Label diffHint = new Label("");
        diffHint.setStyle("-fx-font-size:11px;");
        diffFld.valueProperty().addListener((obs, o, n) -> {
            diffHint.setText(switch (n) {
                case "EASY"   -> "Beginner-friendly, basic equipment";
                case "MEDIUM" -> "Some experience needed";
                case "HARD"   -> "Advanced technique required";
                case "EXPERT" -> "Barista-level skill needed";
                default       -> "";
            });
        });

        GridPane tab1 = tabGrid();
        tab1.add(lbl("Title *"),     0, 0); tab1.add(titleFld, 1, 0);
        tab1.add(lbl("Drink Type *"),0, 1); tab1.add(typeFld,  1, 1);
        tab1.add(lbl("Difficulty"),  0, 2); tab1.add(diffFld,  1, 2);
        tab1.add(new Label(""),      1, 3); tab1.add(diffHint, 1, 3);
        tab1.add(lbl("Brew Time"),   0, 4); tab1.add(timeRow,  1, 4);
        tabs.getTabs().add(tabOf("Basic Info", tab1));

        // ── Tab 2: Ingredients ───────────────────────────────────────────────
        VBox ingredientsBox = new VBox(6);
        ingredientsBox.setPadding(new Insets(12));
        Label ingHint = new Label("Add each ingredient with amount (e.g. \"20g coffee beans\", \"200ml water\")");
        ingHint.setStyle("-fx-font-size:11px;-fx-text-fill:" + UiUtils.sub() + ";");
        ingredientsBox.getChildren().add(ingHint);

        VBox ingRows = new VBox(6);
        List<TextField[]> ingFields = new ArrayList<>();

        // Pre-fill existing ingredients
        if (prefill != null && prefill.getIngredients() != null && !prefill.getIngredients().isBlank()) {
            for (String line : prefill.getIngredients().split("\n")) {
                if (!line.isBlank()) addIngRow(ingRows, ingFields, line.trim());
            }
        } else {
            addIngRow(ingRows, ingFields, "");
            addIngRow(ingRows, ingFields, "");
        }

        Button addIngBtn = new Button("+ Add Ingredient");
        addIngBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#A8621E;" +
            "-fx-font-weight:700;-fx-font-size:12px;-fx-padding:4 0 4 0;-fx-border-width:0;-fx-cursor:hand;");
        addIngBtn.setOnAction(e -> addIngRow(ingRows, ingFields, ""));

        ScrollPane ingScroll = new ScrollPane(ingRows);
        ingScroll.setFitToWidth(true);
        ingScroll.setMaxHeight(280);
        ingScroll.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-background:" + UiUtils.card() + ";");
        ingredientsBox.getChildren().addAll(ingScroll, addIngBtn);
        tabs.getTabs().add(tabOf("Ingredients", ingredientsBox));

        // ── Tab 3: Instructions ──────────────────────────────────────────────
        VBox stepsBox = new VBox(6);
        stepsBox.setPadding(new Insets(12));
        Label stepsHint = new Label("Add numbered steps. Each step will appear as a timer checkpoint.");
        stepsHint.setStyle("-fx-font-size:11px;-fx-text-fill:" + UiUtils.sub() + ";");
        stepsBox.getChildren().add(stepsHint);

        VBox stepRows = new VBox(6);
        List<TextField> stepFields = new ArrayList<>();

        if (prefill != null && prefill.getInstructions() != null && !prefill.getInstructions().isBlank()) {
            int[] stepNum = {1};
            for (String line : prefill.getInstructions().split("\n")) {
                if (!line.isBlank()) addStepRow(stepRows, stepFields, stepNum[0]++, line.trim());
            }
        } else {
            addStepRow(stepRows, stepFields, 1, "");
            addStepRow(stepRows, stepFields, 2, "");
            addStepRow(stepRows, stepFields, 3, "");
        }

        Button addStepBtn = new Button("+ Add Step");
        addStepBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#A8621E;" +
            "-fx-font-weight:700;-fx-font-size:12px;-fx-padding:4 0 4 0;-fx-border-width:0;-fx-cursor:hand;");
        addStepBtn.setOnAction(e -> addStepRow(stepRows, stepFields, stepFields.size() + 1, ""));

        ScrollPane stepsScroll = new ScrollPane(stepRows);
        stepsScroll.setFitToWidth(true);
        stepsScroll.setMaxHeight(280);
        stepsScroll.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-background:" + UiUtils.card() + ";");
        stepsBox.getChildren().addAll(stepsScroll, addStepBtn);
        tabs.getTabs().add(tabOf("Instructions", stepsBox));

        // ── Tab 4: Details ───────────────────────────────────────────────────
        TextArea descFld = new TextArea(prefill != null ? prefill.getDescription() : "");
        descFld.setPromptText("What makes this recipe special? Taste notes, origin, tips…");
        descFld.setPrefRowCount(4);
        descFld.setWrapText(true);
        descFld.getStyleClass().add("form-field");

        // Flavor tags
        String[] allFlavors = {"Fruity","Nutty","Chocolatey","Floral","Spicy","Earthy","Sweet","Bright","Bold","Smooth","Caramel","Vanilla","Citrus","Berry"};
        List<String> selectedFlavors = new ArrayList<>(prefill != null ? prefill.getFlavorTags() : List.of());
        javafx.scene.layout.FlowPane flavorFlow = new javafx.scene.layout.FlowPane(6, 6);
        for (String fl : allFlavors) {
            ToggleButton tb = new ToggleButton(fl);
            tb.setSelected(selectedFlavors.contains(fl));
            tb.setStyle(tb.isSelected() ? activeChipStyle() : inactiveChipStyle());
            tb.selectedProperty().addListener((obs, o, n) -> {
                tb.setStyle(n ? activeChipStyle() : inactiveChipStyle());
                if (n) selectedFlavors.add(fl); else selectedFlavors.remove(fl);
            });
            flavorFlow.getChildren().add(tb);
        }

        // Photo picker
        Label[] photoPath = {new Label("No photo selected")};
        photoPath[0].setStyle("-fx-font-size:11px;-fx-text-fill:" + UiUtils.sub() + ";");
        String[] base64Image = {prefill != null && prefill.getImageUrl() != null && prefill.getImageUrl().startsWith("data:") ? prefill.getImageUrl() : ""};

        Button pickPhoto = new Button("\uD83D\uDDBC\uFE0F  Choose Photo");
        pickPhoto.setStyle("-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.text() + ";" +
            "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:7 16 7 16;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        ImageView photoPreview = new ImageView();
        photoPreview.setFitWidth(180); photoPreview.setFitHeight(100);
        photoPreview.setPreserveRatio(true);
        photoPreview.setStyle("-fx-background-radius:8;");
        if (!base64Image[0].isBlank()) {
            try { photoPreview.setImage(new Image(base64Image[0])); } catch (Exception ignored) {}
        }
        pickPhoto.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Recipe Photo");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg","*.jpeg","*.png","*.webp"));
            File f = fc.showOpenDialog(dlg.getDialogPane().getScene().getWindow());
            if (f != null) {
                try {
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    // Resize if too large (keep under 500KB)
                    if (bytes.length > 500_000) {
                        bytes = scaleImageBytes(bytes, f.getName(), 800);
                    }
                    String mime = f.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                    base64Image[0] = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
                    photoPath[0].setText(f.getName() + " (" + (bytes.length / 1024) + " KB)");
                    photoPreview.setImage(new Image(base64Image[0]));
                } catch (Exception ex) { MainWindow.alert("Photo Error", ex.getMessage()); }
            }
        });

        HBox photoRow = new HBox(12, pickPhoto, photoPath[0]);
        photoRow.setAlignment(Pos.CENTER_LEFT);

        GridPane tab4 = tabGrid();
        tab4.add(lbl("Description"), 0, 0); tab4.add(descFld,    1, 0);
        tab4.add(lbl("Flavor Tags"), 0, 1); tab4.add(flavorFlow, 1, 1);
        tab4.add(lbl("Photo"),       0, 2); tab4.add(photoRow,   1, 2);
        tab4.add(new Label(""),      0, 3); tab4.add(photoPreview, 1, 3);
        tabs.getTabs().add(tabOf("Details", tab4));

        dlg.getDialogPane().setContent(tabs);

        var clicked = dlg.showAndWait().orElse(null);
        if (clicked == null || clicked.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;

        // Collect ingredients
        StringBuilder ingBuilder = new StringBuilder();
        for (TextField[] row : ingFields) {
            String amount = row[0].getText().trim();
            String ingName = row[1].getText().trim();
            if (!ingName.isBlank()) {
                if (!amount.isBlank()) ingBuilder.append(amount).append(" ");
                ingBuilder.append(ingName).append("\n");
            }
        }

        // Collect steps
        StringBuilder stepsBuilder = new StringBuilder();
        for (TextField sf : stepFields) {
            String step = sf.getText().trim();
            if (!step.isBlank()) stepsBuilder.append(step).append("\n");
        }

        String titleText = titleFld.getText().trim();
        if (titleText.isBlank()) { MainWindow.alert("Required", "Title is required."); return null; }

        return new RecipeFormResult(
            titleText, descFld.getText().trim(), typeFld.getValue(),
            ingBuilder.toString().trim(), stepsBuilder.toString().trim(),
            parseIntOr(timeFld.getText(), 5), diffFld.getValue(),
            List.copyOf(selectedFlavors), base64Image[0]);
    }

    // ── Ingredient row ────────────────────────────────────────────────────────

    private void addIngRow(VBox container, List<TextField[]> fields, String prefill) {
        TextField amtFld = new TextField();
        amtFld.setPromptText("Amount");
        amtFld.setMaxWidth(90);
        amtFld.getStyleClass().add("form-field");

        TextField nameFld = new TextField();
        nameFld.setPromptText("Ingredient name");
        nameFld.getStyleClass().add("form-field");
        HBox.setHgrow(nameFld, Priority.ALWAYS);

        // Pre-fill: first word = amount candidate, rest = name
        if (!prefill.isBlank()) {
            String[] parts = prefill.split(" ", 2);
            if (parts[0].matches("\\d+[a-zA-Z]*")) {
                amtFld.setText(parts[0]);
                if (parts.length > 1) nameFld.setText(parts[1]);
            } else {
                nameFld.setText(prefill);
            }
        }

        Button rem = new Button("\u2715");
        rem.setStyle("-fx-background-color:transparent;-fx-text-fill:#DC2626;" +
            "-fx-font-size:13px;-fx-border-width:0;-fx-cursor:hand;-fx-padding:3 6 3 6;");

        HBox row = new HBox(6, amtFld, nameFld, rem);
        row.setAlignment(Pos.CENTER_LEFT);
        TextField[] pair = {amtFld, nameFld};
        fields.add(pair);
        rem.setOnAction(e -> {
            container.getChildren().remove(row);
            fields.remove(pair);
        });
        container.getChildren().add(row);
    }

    // ── Step row ──────────────────────────────────────────────────────────────

    private void addStepRow(VBox container, List<TextField> fields, int num, String prefill) {
        Label numLbl = new Label(num + ".");
        numLbl.setStyle("-fx-font-weight:800;-fx-font-size:13px;-fx-text-fill:#A8621E;" +
            "-fx-min-width:24px;-fx-alignment:center-right;");
        TextField stepFld = new TextField(prefill);
        stepFld.setPromptText("Describe step " + num + "…");
        stepFld.getStyleClass().add("form-field");
        HBox.setHgrow(stepFld, Priority.ALWAYS);

        Button rem = new Button("\u2715");
        rem.setStyle("-fx-background-color:transparent;-fx-text-fill:#DC2626;" +
            "-fx-font-size:13px;-fx-border-width:0;-fx-cursor:hand;-fx-padding:3 6 3 6;");
        HBox row = new HBox(8, numLbl, stepFld, rem);
        row.setAlignment(Pos.CENTER_LEFT);
        fields.add(stepFld);
        rem.setOnAction(e -> {
            container.getChildren().remove(row);
            fields.remove(stepFld);
            // Renumber
            int n = 1;
            for (var node : container.getChildren()) {
                if (node instanceof HBox hb && hb.getChildren().get(0) instanceof Label nl)
                    nl.setText(n++ + ".");
            }
        });
        container.getChildren().add(row);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void deleteRecipe(Recipe sel) {
        if (!AppSession.loggedIn() || AppSession.userId() != sel.getAuthorId()) {
            MainWindow.alert("Not allowed", "You can only delete your own recipes."); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + sel.getTitle() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) { service.deleteRecipe(sel.getId(), AppSession.userId()); loadData(); }
        });
    }

    // ── Image scaling ─────────────────────────────────────────────────────────

    private static byte[] scaleImageBytes(byte[] original, String name, int maxDim) {
        try {
            var stream = new java.io.ByteArrayInputStream(original);
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(stream);
            if (img == null) return original;
            int w = img.getWidth(), h = img.getHeight();
            if (w <= maxDim && h <= maxDim) return original;
            double scale = (double) maxDim / Math.max(w, h);
            int nw = (int)(w * scale), nh = (int)(h * scale);
            var scaled = new java.awt.image.BufferedImage(nw, nh, java.awt.image.BufferedImage.TYPE_INT_RGB);
            scaled.getGraphics().drawImage(img.getScaledInstance(nw, nh, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            var out = new ByteArrayOutputStream();
            String fmt = name.toLowerCase().endsWith(".png") ? "png" : "jpeg";
            javax.imageio.ImageIO.write(scaled, fmt, out);
            return out.toByteArray();
        } catch (Exception e) { return original; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Label chip(String text, String fg, String bg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
            "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:3 9 3 9;-fx-background-radius:10;");
        return l;
    }
    private static String diffColor(String d) { return switch (d) { case "EASY" -> "#2D6A4F"; case "MEDIUM" -> "#A8621E"; case "HARD" -> "#B5321E"; default -> "#5A189A"; }; }
    private static String diffBg(String d)    { return switch (d) { case "EASY" -> "#D8F3DC"; case "MEDIUM" -> "#FFF0D8"; case "HARD" -> "#FDECEA"; default -> "#EDE7F6"; }; }
    private static String activeChipStyle()   { return "-fx-background-color:#A8621E;-fx-text-fill:#F5C060;-fx-font-size:11px;-fx-font-weight:700;-fx-padding:4 10 4 10;-fx-background-radius:14;-fx-border-width:0;-fx-cursor:hand;"; }
    private static String inactiveChipStyle() { return "-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.sub() + ";-fx-font-size:11px;-fx-font-weight:600;-fx-padding:4 10 4 10;-fx-background-radius:14;-fx-border-color:" + UiUtils.border() + ";-fx-border-radius:14;-fx-border-width:1;-fx-cursor:hand;"; }

    private static Tab tabOf(String title, javafx.scene.Node content) {
        Tab t = new Tab(title);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-background:" + UiUtils.card() + ";");
        t.setContent(sp);
        return t;
    }

    private static GridPane tabGrid() {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(12);
        g.setPadding(new Insets(16));
        ColumnConstraints c0 = new ColumnConstraints(110);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }

    private static TextField styledField(String value, String prompt) {
        TextField f = new TextField(value);
        f.setPromptText(prompt);
        f.getStyleClass().add("form-field");
        return f;
    }

    private static Label lbl(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        l.setWrapText(true);
        return l;
    }

    private static int parseIntOr(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    public BorderPane getRoot() { return root; }

    // ── Result record ─────────────────────────────────────────────────────────

    record RecipeFormResult(
        String title, String description, String drinkType,
        String ingredients, String instructions,
        int brewTime, String difficulty,
        List<String> flavorTags, String imageData
    ) {}
}
