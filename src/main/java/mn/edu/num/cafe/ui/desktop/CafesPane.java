package mn.edu.num.cafe.ui.desktop;

import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.domain.CafeListing;

import java.util.List;

/**
 * Cafes content pane — card-based layout with add, rating, and Near Me features.
 */
public class CafesPane {

    private final BorderPane root;
    private final BorgolService service;
    private VBox listBox;
    private String lastSearch = "";
    private Timeline searchDebounce;

    public CafesPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setStyle("-fx-background-color:#F0F2F5;");
        root.setTop(buildToolbar());
        root.setCenter(buildScrollList());
        searchDebounce = UiUtils.debounce(300, () -> loadData(lastSearch));
        loadData("");
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("\u2615  Cafes");
        title.getStyleClass().add("pane-title");

        TextField search = new TextField();
        search.setPromptText("Search cafes\u2026");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((o, old, v) -> {
            lastSearch = v;
            searchDebounce.playFromStart();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNearMe = new Button("\uD83D\uDCCD Near Me");
        Button btnNew    = new Button("+ Add Cafe");
        btnNearMe.getStyleClass().add("btn-secondary");
        btnNew.getStyleClass().add("btn-primary");
        btnNearMe.setOnAction(e -> showNearMeDialog());
        btnNew.setOnAction(e -> showNewDialog());
        if (!AppSession.loggedIn()) btnNew.setDisable(true);

        bar.getChildren().addAll(title, search, spacer, btnNearMe, btnNew);
        return bar;
    }

    // ── Scrollable card list ──────────────────────────────────────────────────

    private ScrollPane buildScrollList() {
        listBox = new VBox(12);
        listBox.setPadding(new Insets(20, 40, 20, 40));
        listBox.setStyle("-fx-background-color:#F0F2F5;");
        listBox.setMaxWidth(780);

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F0F2F5;-fx-background:#F0F2F5;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData(String search) {
        listBox.getChildren().clear();
        try {
            int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
            List<CafeListing> cafes = service.getCafes(uid, search, null);
            if (cafes.isEmpty()) {
                listBox.getChildren().add(emptyLabel("No cafes found."));
            } else {
                for (CafeListing c : cafes) listBox.getChildren().add(buildCafeCard(c));
            }
        } catch (Exception e) {
            MainWindow.alert("Error", e.getMessage());
        }
    }

    // ── Cafe card ─────────────────────────────────────────────────────────────

    private HBox buildCafeCard(CafeListing c) {
        HBox card = new HBox(16);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
            "-fx-padding:16 20 16 20;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(740);

        // Cafe icon circle
        Label icon = new Label("\u2615");
        icon.setStyle("-fx-font-size:26px;-fx-min-width:46px;-fx-min-height:46px;" +
            "-fx-max-width:46px;-fx-max-height:46px;" +
            "-fx-background-color:#FFF0E0;-fx-background-radius:23;-fx-alignment:center;");

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(c.getName());
        nameLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
        nameLabel.setWrapText(true);

        int rounded = (int) Math.round(c.getAvgRating());
        String stars = "\u2605".repeat(Math.max(0, rounded)) +
                       "\u2606".repeat(Math.max(0, 5 - rounded));
        Label starsLabel = new Label(stars + "  " +
            String.format("%.1f", c.getAvgRating()) + "  (" + c.getRatingCount() + " ratings)");
        starsLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#D4621A;");

        info.getChildren().addAll(nameLabel, starsLabel);

        String location = "";
        if (c.getAddress() != null && !c.getAddress().isBlank()) location += c.getAddress();
        if (c.getDistrict() != null && !c.getDistrict().isBlank())
            location += (location.isEmpty() ? "" : "  \u00B7  ") + c.getDistrict();
        if (!location.isEmpty()) {
            Label addrLabel = new Label("\uD83D\uDCCD  " + location);
            addrLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
            addrLabel.setWrapText(true);
            info.getChildren().add(addrLabel);
        }
        if (c.getHours() != null && !c.getHours().isBlank()) {
            Label hoursLabel = new Label("\u23F0  " + c.getHours());
            hoursLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;");
            info.getChildren().add(hoursLabel);
        }

        // Actions
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);

        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:#E4E6EA;-fx-text-fill:#1C1E21;" +
            "-fx-font-weight:600;-fx-font-size:13px;-fx-padding:6 14 6 14;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> showDetailDialog(c));

        Button rateBtn = new Button("\u2B50 Rate");
        rateBtn.setStyle("-fx-background-color:#D4621A;-fx-text-fill:white;" +
            "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:6 14 6 14;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        rateBtn.setOnAction(e -> showRateDialog(c));
        if (!AppSession.loggedIn()) rateBtn.setDisable(true);

        actions.getChildren().addAll(viewBtn, rateBtn);
        card.getChildren().addAll(icon, info, actions);
        return card;
    }

    // ── Detail dialog ─────────────────────────────────────────────────────────

    private void showDetailDialog(CafeListing c) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(c.getName());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        UiUtils.addStylesheet(dlg, CafesPane.class);
        dlg.getDialogPane().setPrefWidth(520);

        VBox box = new VBox(16);
        box.setPadding(new Insets(20));

        Label nameLabel = new Label(c.getName());
        nameLabel.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#1C1E21;");
        nameLabel.setWrapText(true);

        int rounded = (int) Math.round(c.getAvgRating());
        String stars = "\u2605".repeat(Math.max(0, rounded)) + "\u2606".repeat(Math.max(0, 5 - rounded));
        Label starsLabel = new Label(stars + "  " +
            String.format("%.1f", c.getAvgRating()) + "  (" + c.getRatingCount() + " ratings)");
        starsLabel.setStyle("-fx-font-size:16px;-fx-text-fill:#D4621A;");
        box.getChildren().addAll(nameLabel, starsLabel, new Separator());

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
            grid.add(infoValue("@" + c.getSubmittedByUsername()), 1, row);
        }
        box.getChildren().add(grid);

        if (c.getDescription() != null && !c.getDescription().isBlank()) {
            Label descHeading = new Label("ABOUT");
            descHeading.getStyleClass().add("detail-heading");
            Label desc = new Label(c.getDescription());
            desc.setStyle("-fx-font-size:14px;-fx-text-fill:#1C1E21;-fx-line-spacing:3;");
            desc.setWrapText(true);
            box.getChildren().addAll(new Separator(), descHeading, desc);
        }

        if (AppSession.loggedIn() && c.getCurrentUserRating() > 0) {
            Label myHead = new Label("YOUR RATING");
            myHead.getStyleClass().add("detail-heading");
            String myStars = "\u2605".repeat(c.getCurrentUserRating()) +
                             "\u2606".repeat(5 - c.getCurrentUserRating());
            Label myRating = new Label(myStars);
            myRating.setStyle("-fx-font-size:18px;-fx-text-fill:#D4621A;");
            box.getChildren().addAll(new Separator(), myHead, myRating);
            if (c.getCurrentUserReview() != null && !c.getCurrentUserReview().isBlank()) {
                Label myReview = new Label("\u201C" + c.getCurrentUserReview() + "\u201D");
                myReview.setStyle("-fx-font-size:13px;-fx-text-fill:#65676B;-fx-font-style:italic;");
                myReview.setWrapText(true);
                box.getChildren().add(myReview);
            }
        }

        if (AppSession.loggedIn()) {
            Button rateBtn = new Button("\u2B50 Rate this Cafe");
            rateBtn.getStyleClass().add("btn-primary");
            rateBtn.setOnAction(e -> { dlg.close(); showRateDialog(c); });
            box.getChildren().add(rateBtn);
        }

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dlg.getDialogPane().setContent(scroll);
        dlg.showAndWait();
    }

    // ── Rate dialog ───────────────────────────────────────────────────────────

    private void showRateDialog(CafeListing c) {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }

        Dialog<ButtonType> dlg = dialog("Rate \u2014 " + c.getName());
        GridPane grid = MainWindow.formGrid();

        Spinner<Integer> stars = new Spinner<>(1, 5, 4);
        stars.setEditable(true);
        stars.getStyleClass().add("form-field");
        TextField review = MainWindow.styledField("Your review\u2026");

        grid.add(lbl("Stars (1-5)"), 0, 0); grid.add(stars,  1, 0);
        grid.add(lbl("Review"),      0, 1); grid.add(review, 1, 1);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    service.rateCafe(AppSession.userId(), c.getId(),
                        stars.getValue(), review.getText().trim());
                    loadData(lastSearch);
                    UiUtils.showToast("\u2B50 Rating submitted!");
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    // ── Add Cafe dialog ───────────────────────────────────────────────────────

    private void showNewDialog() {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }

        Dialog<ButtonType> dlg = dialog("Add a Cafe");
        GridPane grid = MainWindow.formGrid();

        TextField name  = MainWindow.styledField("Blue Sky Cafe");
        TextField addr  = MainWindow.styledField("Seoul Street 15");
        TextField dist  = MainWindow.styledField("Sukhbaatar");
        TextField phone = MainWindow.styledField("+976 9911-2233");
        TextArea  desc  = MainWindow.styledArea("What's special about this place?", 2);
        TextField hours = MainWindow.styledField("Mon-Fri 8:00-20:00");

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
                    loadData(lastSearch);
                    UiUtils.showToast("Cafe added!");
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    // ── Near Me dialog ────────────────────────────────────────────────────────

    private void showNearMeDialog() {
        Dialog<ButtonType> dlg = dialog("Find Cafes Near You");
        GridPane grid = MainWindow.formGrid();

        TextField lat    = MainWindow.styledField("47.9203");
        TextField lng    = MainWindow.styledField("106.9174");
        TextField radius = MainWindow.styledField("5.0");
        Label note = new Label("Default: Ulaanbaatar city center");
        note.setStyle("-fx-text-fill:#65676B;-fx-font-size:12px;");

        grid.add(lbl("Latitude"),    0, 0); grid.add(lat,    1, 0);
        grid.add(lbl("Longitude"),   0, 1); grid.add(lng,    1, 1);
        grid.add(lbl("Radius (km)"), 0, 2); grid.add(radius, 1, 2);
        grid.add(note,               1, 3);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
                    double latD = Double.parseDouble(lat.getText().trim());
                    double lngD = Double.parseDouble(lng.getText().trim());
                    double radD = Double.parseDouble(radius.getText().trim());
                    List<CafeListing> nearby = service.getCafesNearby(uid, latD, lngD, radD);
                    listBox.getChildren().clear();
                    if (nearby.isEmpty()) {
                        listBox.getChildren().add(emptyLabel(
                            "No cafes found within " + radD + " km."));
                    } else {
                        for (CafeListing c : nearby) listBox.getChildren().add(buildCafeCard(c));
                    }
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#65676B;-fx-font-size:14px;-fx-padding:20;");
        return l;
    }

    private static Dialog<ButtonType> dialog(String title) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        UiUtils.addStylesheet(dlg, CafesPane.class);
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
