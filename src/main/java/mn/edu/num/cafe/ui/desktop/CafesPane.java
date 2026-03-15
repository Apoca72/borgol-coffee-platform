package mn.edu.num.cafe.ui.desktop;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.domain.CafeListing;

import java.util.List;

/**
 * Cafes content pane — TableView with add and rating features.
 */
public class CafesPane {

    private final BorderPane root;
    private final BorgolService service;
    private final ObservableList<CafeListing> items = FXCollections.observableArrayList();
    private final TableView<CafeListing> table;

    public CafesPane(BorgolService service) {
        this.service = service;
        root  = new BorderPane();
        table = buildTable();
        root.setTop(buildToolbar());
        root.setCenter(table);
        root.getStyleClass().add("content-pane");
        loadData();
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private TableView<CafeListing> buildTable() {
        TableView<CafeListing> tv = new TableView<>(items);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.getStyleClass().add("data-table");

        TableColumn<CafeListing, String> nameCol = new TableColumn<>("Cafe Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<CafeListing, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        addrCol.setPrefWidth(200);

        TableColumn<CafeListing, String> distCol = new TableColumn<>("District");
        distCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDistrict()));
        distCol.setPrefWidth(100);

        TableColumn<CafeListing, String> ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(c -> {
            CafeListing cafe = c.getValue();
            String stars = "★".repeat((int) Math.round(cafe.getAvgRating()))
                         + "☆".repeat(5 - (int) Math.round(cafe.getAvgRating()));
            return new SimpleStringProperty(stars + "  " +
                String.format("%.1f", cafe.getAvgRating()) + " (" + cafe.getRatingCount() + ")");
        });
        ratingCol.setPrefWidth(160);

        TableColumn<CafeListing, String> hoursCol = new TableColumn<>("Hours");
        hoursCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHours()));
        hoursCol.setPrefWidth(180);

        tv.getColumns().addAll(List.of(nameCol, addrCol, distCol, ratingCol, hoursCol));
        // Double-click to view cafe details
        tv.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                CafeListing sel = tv.getSelectionModel().getSelectedItem();
                if (sel != null) showDetailDialog(sel);
            }
        });
        return tv;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("☕ Cafes");
        title.getStyleClass().add("pane-title");

        TextField search = new TextField();
        search.setPromptText("Search cafes…");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((o, old, v) -> loadData(v));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnView = new Button("\u2615 View");
        Button btnNew  = new Button("+ Add Cafe");
        Button btnRate = new Button("\u2B50 Rate");

        btnView.getStyleClass().add("btn-secondary");
        btnNew.getStyleClass().add("btn-primary");
        btnRate.getStyleClass().add("btn-secondary");

        btnView.setOnAction(e -> {
            CafeListing sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) showDetailDialog(sel);
            else MainWindow.info("Select a cafe", "Select a cafe to view its details.");
        });
        btnNew.setOnAction(e  -> showNewDialog());
        btnRate.setOnAction(e -> showRateDialog());

        if (!AppSession.loggedIn()) {
            btnNew.setDisable(true);
            btnRate.setDisable(true);
        }

        bar.getChildren().addAll(title, search, spacer, btnView, btnRate, btnNew);
        return bar;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() { loadData(""); }

    private void loadData(String search) {
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            items.setAll(service.getCafes(uid, search, null));
        } catch (Exception e) {
            MainWindow.alert("Error", e.getMessage());
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void showNewDialog() {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }

        Dialog<ButtonType> dlg = dialog("Add a Cafe");
        GridPane grid = MainWindow.formGrid();

        TextField name   = MainWindow.styledField("Blue Sky Cafe");
        TextField addr   = MainWindow.styledField("Seoul Street 15");
        TextField dist   = MainWindow.styledField("Sukhbaatar");
        TextField phone  = MainWindow.styledField("+976 9911-2233");
        TextArea  desc   = MainWindow.styledArea("What's special about this place?", 2);
        TextField hours  = MainWindow.styledField("Mon-Fri 8:00-20:00");

        grid.add(lbl("Name *"),   0, 0); grid.add(name,  1, 0);
        grid.add(lbl("Address"),  0, 1); grid.add(addr,  1, 1);
        grid.add(lbl("District"), 0, 2); grid.add(dist,  1, 2);
        grid.add(lbl("Phone"),    0, 3); grid.add(phone, 1, 3);
        grid.add(lbl("Hours"),    0, 4); grid.add(hours, 1, 4);
        grid.add(lbl("About"),    0, 5); grid.add(desc,  1, 5);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    service.createCafe(AppSession.userId(),
                        name.getText().trim(), addr.getText().trim(),
                        dist.getText().trim(), "Ulaanbaatar",
                        phone.getText().trim(), desc.getText().trim(),
                        hours.getText().trim(), "");
                    loadData();
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    private void showDetailDialog(CafeListing c) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(c.getName());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().getStylesheets().add(
            CafesPane.class.getResource("/style.css").toExternalForm());
        dlg.getDialogPane().setPrefWidth(520);

        VBox box = new VBox(16);
        box.setPadding(new Insets(20));

        // ── Header row ──
        Label nameLabel = new Label(c.getName());
        nameLabel.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
        nameLabel.setWrapText(true);

        // Stars row
        int rounded = (int) Math.round(c.getAvgRating());
        String stars = "\u2605".repeat(Math.max(0, rounded)) + "\u2606".repeat(Math.max(0, 5 - rounded));
        Label starsLabel = new Label(stars + "  " +
            String.format("%.1f", c.getAvgRating()) + "  (" + c.getRatingCount() + " ratings)");
        starsLabel.setStyle("-fx-font-size:16px;-fx-text-fill:#D4621A;");

        box.getChildren().addAll(nameLabel, starsLabel);

        Separator sep1 = new Separator();
        box.getChildren().add(sep1);

        // ── Info grid ──
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(8);
        int row = 0;
        if (c.getAddress() != null && !c.getAddress().isBlank()) {
            grid.add(infoLabel("\uD83D\uDCCD  Location"), 0, row);
            grid.add(infoValue(c.getAddress() +
                (c.getDistrict() != null ? ", " + c.getDistrict() : "") +
                (c.getCity() != null ? ", " + c.getCity() : "")), 1, row++);
        }
        if (c.getPhone() != null && !c.getPhone().isBlank()) {
            grid.add(infoLabel("\uD83D\uDCDE  Phone"), 0, row);
            grid.add(infoValue(c.getPhone()), 1, row++);
        }
        if (c.getHours() != null && !c.getHours().isBlank()) {
            grid.add(infoLabel("\u23F0  Hours"), 0, row);
            grid.add(infoValue(c.getHours()), 1, row++);
        }
        if (c.getSubmittedByUsername() != null && !c.getSubmittedByUsername().isBlank()) {
            grid.add(infoLabel("\uD83D\uDC64  Added by"), 0, row);
            grid.add(infoValue("@" + c.getSubmittedByUsername()), 1, row++);
        }
        box.getChildren().add(grid);

        if (c.getDescription() != null && !c.getDescription().isBlank()) {
            Separator sep2 = new Separator();
            Label descHeading = new Label("ABOUT");
            descHeading.getStyleClass().add("detail-heading");
            Label desc = new Label(c.getDescription());
            desc.setStyle("-fx-font-size:14px;-fx-text-fill:#1C1E21;-fx-line-spacing:3;");
            desc.setWrapText(true);
            box.getChildren().addAll(sep2, descHeading, desc);
        }

        // Current user rating (if any)
        if (AppSession.loggedIn() && c.getCurrentUserRating() > 0) {
            Separator sep3 = new Separator();
            Label myRatingHeading = new Label("YOUR RATING");
            myRatingHeading.getStyleClass().add("detail-heading");
            String myStars = "\u2605".repeat(c.getCurrentUserRating()) +
                             "\u2606".repeat(5 - c.getCurrentUserRating());
            Label myRating = new Label(myStars);
            myRating.setStyle("-fx-font-size:18px;-fx-text-fill:#D4621A;");
            box.getChildren().addAll(sep3, myRatingHeading, myRating);
            if (c.getCurrentUserReview() != null && !c.getCurrentUserReview().isBlank()) {
                Label myReview = new Label("\"" + c.getCurrentUserReview() + "\"");
                myReview.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;-fx-font-style:italic;");
                myReview.setWrapText(true);
                box.getChildren().add(myReview);
            }
        }

        // Rate button
        if (AppSession.loggedIn()) {
            Button rateBtn = new Button("\u2B50 Rate this Cafe");
            rateBtn.getStyleClass().add("btn-primary");
            rateBtn.setOnAction(e -> {
                dlg.close();
                table.getSelectionModel().select(c);
                showRateDialog();
            });
            box.getChildren().add(rateBtn);
        }

        dlg.getDialogPane().setContent(box);
        dlg.showAndWait();
    }

    private static Label infoLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#65676B;-fx-min-width:100px;");
        return l;
    }

    private static Label infoValue(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:14px;-fx-text-fill:#1C1E21;");
        l.setWrapText(true);
        return l;
    }

    private void showRateDialog() {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }
        CafeListing sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { MainWindow.info("Select a cafe", "Select a cafe from the table first."); return; }

        Dialog<ButtonType> dlg = dialog("Rate — " + sel.getName());
        GridPane grid = MainWindow.formGrid();

        Spinner<Integer> stars = new Spinner<>(1, 5, 4);
        stars.setEditable(true);
        stars.getStyleClass().add("form-field");
        TextField review = MainWindow.styledField("Your review…");

        grid.add(lbl("Stars (1-5)"), 0, 0); grid.add(stars,  1, 0);
        grid.add(lbl("Review"),      0, 1); grid.add(review, 1, 1);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    service.rateCafe(AppSession.userId(), sel.getId(),
                        stars.getValue(), review.getText().trim());
                    loadData();
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Dialog<ButtonType> dialog(String title) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().add(
            CafesPane.class.getResource("/style.css").toExternalForm());
        dlg.getDialogPane().setPrefWidth(440);
        return dlg;
    }

    private static Label lbl(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("form-label");
        return l;
    }

    public BorderPane getRoot() { return root; }
}
