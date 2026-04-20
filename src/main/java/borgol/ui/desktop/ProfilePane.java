package borgol.ui.desktop;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import borgol.core.application.BorgolService;
import borgol.core.application.BorgolService.UserView;
import borgol.core.domain.Equipment;
import borgol.core.domain.Recipe;

import java.util.List;

/**
 * Profile pane — view/edit own profile, manage equipment, browse own recipes + liked.
 */
public class ProfilePane {

    private final BorderPane root;
    private final BorgolService service;
    private final Runnable onProfileUpdated;

    public ProfilePane(BorgolService service) {
        this(service, null);
    }

    public ProfilePane(BorgolService service, Runnable onProfileUpdated) {
        this.service = service;
        this.onProfileUpdated = onProfileUpdated;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        build();
    }

    private void build() {
        if (!AppSession.loggedIn()) {
            Label msg = new Label("Please log in to view your profile.");
            msg.setStyle("-fx-text-fill:#65676B;-fx-font-size:14px;");
            root.setCenter(new StackPane(msg));
            return;
        }

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            new Tab("\uD83D\uDC64 Profile",    buildProfileTab()),
            new Tab("\uD83D\uDCD6 My Recipes", buildMyRecipesTab()),
            new Tab("\u2764 Liked",             buildLikedTab()),
            new Tab("\uD83D\uDD27 Equipment",   buildEquipmentTab()),
            new Tab("\uD83D\uDC65 Following",   buildFollowingTab()),
            new Tab("\uD83D\uDC64 Followers",   buildFollowersTab())
        );
        root.setCenter(tabs);
    }

    // ── Profile tab ───────────────────────────────────────────────────────────

    private ScrollPane buildProfileTab() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F0F2F5;-fx-background:#F0F2F5;");

        VBox box = new VBox(16);
        box.setPadding(new Insets(24));
        scroll.setContent(box);

        Thread.ofVirtual().start(() -> {
            UserView profile;
            try {
                profile = service.getMe(AppSession.userId());
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    box.getChildren().add(new Label("Failed to load profile: " + e.getMessage()))
                );
                return;
            }
            javafx.application.Platform.runLater(() -> populateProfileBox(box, profile));
        });

        return scroll;
    }

    private void populateProfileBox(VBox box, UserView profile) {
        // ── Header card ──────────────────────────────────────────────────────
        javafx.scene.Node avatar = UiUtils.createAvatar(profile.username(), 64);

        Label username = new Label("@" + profile.username());
        username.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");

        Label email = new Label(profile.email());
        email.setStyle("-fx-text-fill:#65676B;-fx-font-size:13px;");

        Label levelChip = new Label(profile.expertiseLevel() != null ? profile.expertiseLevel() : "BEGINNER");
        levelChip.getStyleClass().add("detail-chip");

        HBox titleRow = new HBox(10, username, levelChip);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Clickable stats
        Label recipesLbl = clickableStat(profile.recipeCount() + " recipes", null);
        Label sepDot1 = dot();
        Label followersLbl = clickableStat(profile.followerCount() + " followers",
            () -> UiUtils.showFollowersDialog(service, AppSession.userId(), true));
        Label sepDot2 = dot();
        Label followingLbl = clickableStat(profile.followingCount() + " following",
            () -> UiUtils.showFollowersDialog(service, AppSession.userId(), false));

        HBox statsRow = new HBox(4, recipesLbl, sepDot1, followersLbl, sepDot2, followingLbl);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(4, titleRow, email, statsRow);
        HBox header = new HBox(16, avatar, nameBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:white;-fx-padding:20;-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");

        // ── Bio card ─────────────────────────────────────────────────────────
        Label bioHeading = new Label("BIO");
        bioHeading.getStyleClass().add("detail-heading");

        Label bioText = new Label(profile.bio() != null && !profile.bio().isBlank()
            ? profile.bio() : "(no bio yet)");
        bioText.setWrapText(true);
        bioText.setStyle("-fx-text-fill:#1C1E21;-fx-font-size:14px;");

        Label prefsHeading = new Label("FLAVOR PREFERENCES");
        prefsHeading.getStyleClass().add("detail-heading");

        Label prefsText = new Label(profile.flavorPrefs() != null && !profile.flavorPrefs().isEmpty()
            ? "\uD83E\uDEB7  " + String.join("  \u00B7  ", profile.flavorPrefs()) : "(none set)");
        prefsText.setStyle("-fx-text-fill:#65676B;-fx-font-size:13px;");

        VBox bioCard = new VBox(8, bioHeading, bioText, prefsHeading, prefsText);
        bioCard.setStyle("-fx-background-color:white;-fx-padding:16;-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");

        Button btnEdit = new Button("\u270F Edit Profile");
        btnEdit.getStyleClass().add("btn-primary");
        btnEdit.setOnAction(e -> showEditProfileDialog());

        box.getChildren().addAll(header, bioCard, btnEdit);
    }

    private Label clickableStat(String text, Runnable onClick) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#65676B;-fx-font-size:13px;" +
            (onClick != null ? "-fx-cursor:hand;" : ""));
        if (onClick != null) {
            l.setOnMouseClicked(e -> onClick.run());
            l.setOnMouseEntered(e -> l.setStyle(l.getStyle() + "-fx-underline:true;"));
            l.setOnMouseExited(e -> l.setStyle(
                "-fx-text-fill:#65676B;-fx-font-size:13px;-fx-cursor:hand;"));
        }
        return l;
    }

    private static Label dot() {
        Label l = new Label("  \u00B7  ");
        l.setStyle("-fx-text-fill:#65676B;-fx-font-size:13px;");
        return l;
    }

    private void showEditProfileDialog() {
        UserView profile;
        try { profile = service.getMe(AppSession.userId()); }
        catch (Exception e) { MainWindow.alert("Error", e.getMessage()); return; }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Edit Profile");
        dlg.setHeaderText(null);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        UiUtils.addStylesheet(dlg, ProfilePane.class);
        dlg.getDialogPane().setPrefWidth(460);

        GridPane grid = MainWindow.formGrid();

        TextArea bioArea = MainWindow.styledArea("Tell the community about yourself\u2026", 3);
        bioArea.setText(profile.bio() != null ? profile.bio() : "");

        ComboBox<String> levelBox = new ComboBox<>(
            FXCollections.observableArrayList("BEGINNER", "ENTHUSIAST", "BARISTA", "EXPERT"));
        levelBox.setValue(profile.expertiseLevel() != null ? profile.expertiseLevel() : "BEGINNER");
        levelBox.getStyleClass().add("form-field");

        TextField flavorField = MainWindow.styledField("FLORAL, FRUITY, BITTER\u2026");
        flavorField.setText(profile.flavorPrefs() != null
            ? String.join(", ", profile.flavorPrefs()) : "");

        TextField avatarField = MainWindow.styledField("https://example.com/photo.jpg");
        avatarField.setText(profile.avatarUrl() != null ? profile.avatarUrl() : "");

        grid.add(lbl("Bio"),          0, 0); grid.add(bioArea,     1, 0);
        grid.add(lbl("Level"),        0, 1); grid.add(levelBox,    1, 1);
        grid.add(lbl("Flavor Prefs"), 0, 2); grid.add(flavorField, 1, 2);
        grid.add(lbl("Avatar URL"),   0, 3); grid.add(avatarField, 1, 3);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    List<String> prefs = List.of(flavorField.getText().split(","))
                        .stream().map(String::trim).filter(s -> !s.isBlank()).toList();
                    service.updateProfile(AppSession.userId(),
                        bioArea.getText().trim(), avatarField.getText().trim(), levelBox.getValue(), prefs);
                    root.getChildren().clear();
                    build();
                    UiUtils.showToast("Profile updated!");
                    if (onProfileUpdated != null) onProfileUpdated.run();
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    // ── My Recipes tab ────────────────────────────────────────────────────────

    private ScrollPane buildMyRecipesTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20, 24, 24, 24));
        box.setStyle("-fx-background-color:#F0F2F5;");

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F0F2F5;-fx-background:#F0F2F5;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Thread.ofVirtual().start(() -> {
            try {
                int uid = AppSession.userId();
                List<Recipe> recipes = service.getUserRecipes(uid, uid);
                javafx.application.Platform.runLater(() -> {
                    if (recipes.isEmpty()) {
                        box.getChildren().add(UiUtils.emptyState(
                            "\uD83D\uDCD6", "No recipes yet",
                            "Share your first coffee recipe!"));
                    } else {
                        for (Recipe r : recipes)
                            box.getChildren().add(UiUtils.buildMiniCard(service, r));
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    box.getChildren().add(new Label("Failed to load: " + e.getMessage()))
                );
            }
        });

        return scroll;
    }

    // ── Liked tab ─────────────────────────────────────────────────────────────

    private ScrollPane buildLikedTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20, 24, 24, 24));
        box.setStyle("-fx-background-color:#F0F2F5;");

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F0F2F5;-fx-background:#F0F2F5;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Thread.ofVirtual().start(() -> {
            try {
                int uid = AppSession.userId();
                List<Recipe> liked = service.getLikedRecipes(uid, uid);
                javafx.application.Platform.runLater(() -> {
                    if (liked.isEmpty()) {
                        box.getChildren().add(UiUtils.emptyState(
                            "\u2661", "Nothing liked yet",
                            "Like recipes in the feed to save them here."));
                    } else {
                        for (Recipe r : liked)
                            box.getChildren().add(UiUtils.buildMiniCard(service, r));
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    box.getChildren().add(new Label("Failed to load: " + e.getMessage()))
                );
            }
        });

        return scroll;
    }

    // ── Equipment tab ─────────────────────────────────────────────────────────

    private BorderPane buildEquipmentTab() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color:#F0F2F5;");

        HBox bar = new HBox(8);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("My Equipment");
        title.getStyleClass().add("pane-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnAdd = new Button("+ Add Equipment");
        btnAdd.getStyleClass().add("btn-primary");
        Button btnDel = new Button("Delete");
        btnDel.getStyleClass().add("btn-danger");
        bar.getChildren().addAll(title, spacer, btnAdd, btnDel);
        pane.setTop(bar);

        ListView<Equipment> list = new ListView<>();
        list.getStyleClass().add("list-view");
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Equipment eq, boolean empty) {
                super.updateItem(eq, empty);
                if (empty || eq == null) { setText(null); setGraphic(null); return; }
                String brand = eq.getBrand() != null && !eq.getBrand().isBlank()
                    ? " \u00B7 " + eq.getBrand() : "";
                setText("[" + eq.getCategory() + "]  " + eq.getName() + brand);
            }
        });
        pane.setCenter(list);
        loadEquipment(list);

        btnAdd.setOnAction(e -> showAddEquipmentDialog(list));
        btnDel.setOnAction(e -> {
            Equipment sel = list.getSelectionModel().getSelectedItem();
            if (sel == null) { MainWindow.info("Select item", "Select an equipment item to delete."); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + sel.getName() + "\"?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try {
                        service.deleteEquipment(sel.getId(), AppSession.userId());
                        loadEquipment(list);
                    } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
                }
            });
        });

        return pane;
    }

    private void loadEquipment(ListView<Equipment> list) {
        Thread.ofVirtual().start(() -> {
            try {
                List<Equipment> equipment = service.getEquipment(AppSession.userId());
                javafx.application.Platform.runLater(() -> list.getItems().setAll(equipment));
            } catch (Exception ignored) {}
        });
    }

    private void showAddEquipmentDialog(ListView<Equipment> list) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Add Equipment");
        dlg.setHeaderText(null);
        ButtonType saveBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        UiUtils.addStylesheet(dlg, ProfilePane.class);
        dlg.getDialogPane().setPrefWidth(440);

        GridPane grid = MainWindow.formGrid();
        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(
            "GRINDER", "BREWER", "ESPRESSO_MACHINE", "SCALE", "KETTLE", "OTHER"));
        catBox.setValue("GRINDER");
        catBox.getStyleClass().add("form-field");
        TextField nameField  = MainWindow.styledField("e.g. Fellow Ode");
        TextField brandField = MainWindow.styledField("e.g. Fellow");
        TextArea  notesArea  = MainWindow.styledArea("Notes\u2026", 2);

        grid.add(lbl("Category"), 0, 0); grid.add(catBox,     1, 0);
        grid.add(lbl("Name *"),   0, 1); grid.add(nameField,  1, 1);
        grid.add(lbl("Brand"),    0, 2); grid.add(brandField, 1, 2);
        grid.add(lbl("Notes"),    0, 3); grid.add(notesArea,  1, 3);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    service.addEquipment(AppSession.userId(),
                        catBox.getValue(), nameField.getText().trim(),
                        brandField.getText().trim(), notesArea.getText().trim());
                    loadEquipment(list);
                    UiUtils.showToast("Equipment added!");
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    // ── Following tab ─────────────────────────────────────────────────────────

    private ScrollPane buildFollowingTab() {
        VBox list = new VBox(8);
        list.setPadding(new Insets(16));
        list.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        try {
            int uid = AppSession.userId();
            var users = service.getFollowingUsers(uid, uid);
            if (users.isEmpty()) {
                list.getChildren().add(UiUtils.emptyState("\uD83D\uDC65", "Not following anyone", "Find people in the People tab."));
            } else {
                for (var u : users) {
                    list.getChildren().add(buildUserRow(u));
                }
            }
        } catch (Exception e) {
            list.getChildren().add(new Label("Could not load."));
        }
        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + UiUtils.bg() + ";-fx-background-color:" + UiUtils.bg() + ";");
        return sp;
    }

    // ── Followers tab ─────────────────────────────────────────────────────────

    private ScrollPane buildFollowersTab() {
        VBox list = new VBox(8);
        list.setPadding(new Insets(16));
        list.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        try {
            int uid = AppSession.userId();
            var users = service.getFollowerUsers(uid, uid);
            if (users.isEmpty()) {
                list.getChildren().add(UiUtils.emptyState("\uD83D\uDC65", "No followers yet", "Share your recipes to gain followers."));
            } else {
                for (var u : users) {
                    list.getChildren().add(buildUserRow(u));
                }
            }
        } catch (Exception e) {
            list.getChildren().add(new Label("Could not load."));
        }
        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + UiUtils.bg() + ";-fx-background-color:" + UiUtils.bg() + ";");
        return sp;
    }

    private HBox buildUserRow(UserView u) {
        javafx.scene.Node av = UiUtils.createAvatar(u.username(), 36);
        Label name = new Label(u.username());
        name.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:" + UiUtils.text() + ";");
        Label bio = new Label(u.bio() != null ? u.bio() : "");
        bio.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.sub() + ";");
        VBox info = new VBox(2, name, bio);
        HBox row = new HBox(10, av, info);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-background-radius:10;");
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Label lbl(String t) {
        Label l = new Label(t); l.getStyleClass().add("form-label"); return l;
    }

    public BorderPane getRoot() { return root; }
}
