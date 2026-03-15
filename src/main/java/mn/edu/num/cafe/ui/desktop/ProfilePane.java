package mn.edu.num.cafe.ui.desktop;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.application.BorgolService.UserView;
import mn.edu.num.cafe.core.domain.Equipment;

import java.util.List;

/**
 * Profile pane — view/edit own profile, manage equipment.
 */
public class ProfilePane {

    private final BorderPane root;
    private final BorgolService service;

    public ProfilePane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        build();
    }

    private void build() {
        if (!AppSession.loggedIn()) {
            Label msg = new Label("Please log in to view your profile.");
            msg.setStyle("-fx-text-fill:#8A7054;-fx-font-size:14px;");
            StackPane placeholder = new StackPane(msg);
            root.setCenter(placeholder);
            return;
        }

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
            new Tab("👤 Profile", buildProfileTab()),
            new Tab("🔧 Equipment", buildEquipmentTab())
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

        UserView profile;
        try {
            profile = service.getMe(AppSession.userId());
        } catch (Exception e) {
            box.getChildren().add(new Label("Failed to load profile: " + e.getMessage()));
            return scroll;
        }

        // Header card
        // Avatar
        String initial = profile.username().substring(0, 1).toUpperCase();
        Label avatar = new Label(initial);
        avatar.setStyle(
            "-fx-background-color:#D4621A;-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-font-size:26px;-fx-min-width:64px;-fx-min-height:64px;" +
            "-fx-max-width:64px;-fx-max-height:64px;" +
            "-fx-background-radius:32px;-fx-alignment:center;");

        Label username = new Label("@" + profile.username());
        username.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");

        Label email = new Label(profile.email());
        email.setStyle("-fx-text-fill:#65676B;-fx-font-size:13px;");

        Label levelChip = new Label(profile.expertiseLevel() != null ? profile.expertiseLevel() : "BEGINNER");
        levelChip.getStyleClass().add("detail-chip");

        HBox titleRow = new HBox(10, username, levelChip);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label stats = new Label(
            profile.recipeCount() + " recipes  \u00B7  " +
            profile.followerCount() + " followers  \u00B7  " +
            profile.followingCount() + " following");
        stats.setStyle("-fx-text-fill:#65676B;-fx-font-size:13px;");

        VBox nameBox = new VBox(4, titleRow, email, stats);
        HBox header = new HBox(16, avatar, nameBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:white;-fx-padding:20;-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");

        // Bio section
        Label bioHeading = new Label("BIO");
        bioHeading.getStyleClass().add("detail-heading");

        Label bioText = new Label(profile.bio() != null && !profile.bio().isBlank()
            ? profile.bio() : "(no bio yet)");
        bioText.setWrapText(true);
        bioText.setStyle("-fx-text-fill:#1C1E21;-fx-font-size:14px;");

        // Flavor prefs
        Label prefsHeading = new Label("FLAVOR PREFERENCES");
        prefsHeading.getStyleClass().add("detail-heading");

        Label prefsText = new Label(profile.flavorPrefs() != null && !profile.flavorPrefs().isEmpty()
            ? "\uD83E\uDEB7  " + String.join("  \u00B7  ", profile.flavorPrefs()) : "(none set)");
        prefsText.setStyle("-fx-text-fill:#65676B;-fx-font-size:13px;");

        // Edit button
        Button btnEdit = new Button("✏ Edit Profile");
        btnEdit.getStyleClass().add("btn-primary");
        btnEdit.setOnAction(e -> showEditProfileDialog());

        VBox bioCard = new VBox(8);
        bioCard.setStyle("-fx-background-color:white;-fx-padding:16;-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");
        bioCard.getChildren().addAll(bioHeading, bioText, prefsHeading, prefsText);

        box.getChildren().addAll(header, bioCard, btnEdit);
        return scroll;
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
        dlg.getDialogPane().getStylesheets().add(
            ProfilePane.class.getResource("/style.css").toExternalForm());
        dlg.getDialogPane().setPrefWidth(460);

        GridPane grid = MainWindow.formGrid();

        TextArea bioArea = MainWindow.styledArea("Tell the community about yourself…", 3);
        bioArea.setText(profile.bio() != null ? profile.bio() : "");

        ComboBox<String> levelBox = new ComboBox<>(
            FXCollections.observableArrayList("BEGINNER", "ENTHUSIAST", "BARISTA", "EXPERT"));
        levelBox.setValue(profile.expertiseLevel() != null ? profile.expertiseLevel() : "BEGINNER");
        levelBox.getStyleClass().add("form-field");

        TextField flavorField = MainWindow.styledField("FLORAL, FRUITY, BITTER…");
        flavorField.setText(profile.flavorPrefs() != null
            ? String.join(", ", profile.flavorPrefs()) : "");

        grid.add(lbl("Bio"),           0, 0); grid.add(bioArea,     1, 0);
        grid.add(lbl("Level"),         0, 1); grid.add(levelBox,    1, 1);
        grid.add(lbl("Flavor Prefs"),  0, 2); grid.add(flavorField, 1, 2);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    List<String> prefs = List.of(flavorField.getText().split(","))
                        .stream()
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
                    service.updateProfile(AppSession.userId(),
                        bioArea.getText().trim(), "",
                        levelBox.getValue(), prefs);
                    // Rebuild to reflect changes
                    root.getChildren().clear();
                    build();
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    // ── Equipment tab ─────────────────────────────────────────────────────────

    private BorderPane buildEquipmentTab() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color:#F0F2F5;");

        // Toolbar
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

        // List
        ListView<Equipment> list = new ListView<>();
        list.getStyleClass().add("list-view");
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Equipment eq, boolean empty) {
                super.updateItem(eq, empty);
                if (empty || eq == null) { setText(null); }
                else {
                    String brand = eq.getBrand() != null && !eq.getBrand().isBlank()
                        ? " · " + eq.getBrand() : "";
                    setText("[" + eq.getCategory() + "]  " + eq.getName() + brand);
                }
            }
        });
        pane.setCenter(list);
        loadEquipment(list);

        btnAdd.setOnAction(e -> showAddEquipmentDialog(list));
        btnDel.setOnAction(e -> {
            Equipment sel = list.getSelectionModel().getSelectedItem();
            if (sel == null) { MainWindow.info("Select item", "Select an equipment item to delete."); return; }
            try {
                service.deleteEquipment(sel.getId(), AppSession.userId());
                loadEquipment(list);
            } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
        });

        return pane;
    }

    private void loadEquipment(ListView<Equipment> list) {
        try { list.getItems().setAll(service.getEquipment(AppSession.userId())); }
        catch (Exception ignored) {}
    }

    private void showAddEquipmentDialog(ListView<Equipment> list) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Add Equipment");
        dlg.setHeaderText(null);
        ButtonType saveBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().add(
            ProfilePane.class.getResource("/style.css").toExternalForm());
        dlg.getDialogPane().setPrefWidth(440);

        GridPane grid = MainWindow.formGrid();
        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(
            "GRINDER", "BREWER", "ESPRESSO_MACHINE", "SCALE", "KETTLE", "OTHER"));
        catBox.setValue("GRINDER");
        catBox.getStyleClass().add("form-field");
        TextField nameField  = MainWindow.styledField("e.g. Fellow Ode");
        TextField brandField = MainWindow.styledField("e.g. Fellow");
        TextArea  notesArea  = MainWindow.styledArea("Notes…", 2);

        grid.add(lbl("Category"), 0, 0); grid.add(catBox,    1, 0);
        grid.add(lbl("Name *"),   0, 1); grid.add(nameField, 1, 1);
        grid.add(lbl("Brand"),    0, 2); grid.add(brandField,1, 2);
        grid.add(lbl("Notes"),    0, 3); grid.add(notesArea, 1, 3);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    service.addEquipment(AppSession.userId(),
                        catBox.getValue(), nameField.getText().trim(),
                        brandField.getText().trim(), notesArea.getText().trim());
                    loadEquipment(list);
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Label lbl(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("form-label");
        return l;
    }

    public BorderPane getRoot() { return root; }
}
