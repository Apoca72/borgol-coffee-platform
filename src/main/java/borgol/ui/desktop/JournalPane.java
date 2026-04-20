package borgol.ui.desktop;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import borgol.core.application.BorgolService;
import borgol.core.domain.BrewJournalEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Brew Journal pane — card-based layout with detail dialog featuring radar chart.
 */
public class JournalPane {

    private final BorderPane root;
    private final BorgolService service;
    private VBox listBox;
    private List<BrewJournalEntry> entries = new ArrayList<>();

    public JournalPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        root.setTop(buildToolbar());
        root.setCenter(buildScrollList());
        loadData();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("\uD83D\uDCD3 Brew Journal");
        title.getStyleClass().add("pane-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNew  = new Button("+ New Entry");
        Button btnCsv  = new Button("\uD83D\uDCC4 Export");
        btnNew.getStyleClass().add("btn-primary");
        btnCsv.getStyleClass().add("btn-secondary");
        btnNew.setOnAction(e -> showNewDialog());
        btnCsv.setOnAction(e -> openExportDialog());
        if (!AppSession.loggedIn()) btnNew.setDisable(true);

        bar.getChildren().addAll(title, spacer, btnCsv, btnNew);
        return bar;
    }

    // ── Scrollable card list ──────────────────────────────────────────────────

    private ScrollPane buildScrollList() {
        listBox = new VBox(12);
        listBox.setPadding(new Insets(20, 40, 20, 40));
        listBox.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        listBox.setMaxWidth(780);

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:" + UiUtils.bg() + ";-fx-background:" + UiUtils.bg() + ";");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        entries.clear();
        listBox.getChildren().clear();
        if (!AppSession.loggedIn()) {
            listBox.getChildren().add(UiUtils.emptyState(
                "\uD83D\uDCD3", "Your journal is empty",
                "Log in and start logging your brews!"));
            return;
        }
        int uid = AppSession.userId();
        Thread.ofVirtual().start(() -> {
            try {
                List<BrewJournalEntry> loaded = service.getJournalEntries(uid);
                javafx.application.Platform.runLater(() -> {
                    entries = loaded;
                    if (entries.isEmpty()) {
                        listBox.getChildren().add(UiUtils.emptyState(
                            "\uD83D\uDCD3", "No entries yet",
                            "Start logging your coffee brews to track flavors over time."));
                    } else {
                        for (BrewJournalEntry e : entries) listBox.getChildren().add(buildEntryCard(e));
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> MainWindow.alert("Error", e.getMessage()));
            }
        });
    }

    // ── Journal entry card ────────────────────────────────────────────────────

    private VBox buildEntryCard(BrewJournalEntry e) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-background-radius:12;" +
            "-fx-padding:16 20 14 20;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,1);");
        card.setMaxWidth(740);

        // Header row: bean + origin + roast chip
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label beanIcon = new Label("\u2615");
        beanIcon.setStyle("-fx-font-size:22px;-fx-min-width:36px;-fx-alignment:center;" +
            "-fx-background-color:#FFF0E0;-fx-background-radius:18;-fx-min-height:36px;" +
            "-fx-max-width:36px;-fx-max-height:36px;");

        VBox titleBox = new VBox(3);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        String beanText = (e.getCoffeeBean() != null && !e.getCoffeeBean().isBlank()
            ? e.getCoffeeBean() : "Unnamed Bean") +
            (e.getOrigin() != null && !e.getOrigin().isBlank()
            ? "  \u00B7  " + e.getOrigin() : "");
        Label beanLabel = new Label(beanText);
        beanLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + UiUtils.text() + ";");
        beanLabel.setWrapText(true);

        HBox subRow = new HBox(8);
        subRow.setAlignment(Pos.CENTER_LEFT);

        Label methodLabel = new Label(e.getBrewMethod() != null ? e.getBrewMethod() : "");
        methodLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.sub() + ";");

        if (e.getRoastLevel() != null && !e.getRoastLevel().isBlank()) {
            Label roastChip = new Label(e.getRoastLevel());
            roastChip.getStyleClass().add("detail-chip");
            roastChip.setStyle("-fx-background-color:#FFF0E0;-fx-text-fill:#D4621A;" +
                "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:3 8 3 8;" +
                "-fx-background-radius:10;");
            subRow.getChildren().addAll(methodLabel, roastChip);
        } else {
            subRow.getChildren().add(methodLabel);
        }

        // Average rating stars
        int[] vals = {e.getRatingAroma(), e.getRatingFlavor(), e.getRatingAcidity(),
                      e.getRatingBody(), e.getRatingSweetness(), e.getRatingFinish()};
        double avg = 0; for (int v : vals) avg += v; avg /= vals.length;
        int avgRounded = (int) Math.round(avg / 2.0); // scale 0-10 → 0-5 stars
        String avgStars = "\u2605".repeat(Math.max(0, avgRounded)) +
                          "\u2606".repeat(Math.max(0, 5 - avgRounded));
        Label ratingLabel = new Label(avgStars + String.format("  %.1f / 10", avg));
        ratingLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#D4621A;");

        titleBox.getChildren().addAll(beanLabel, subRow, ratingLabel);

        String rawDate = e.getCreatedAt();
        String displayDate = "";
        if (rawDate != null && rawDate.length() >= 10) {
            displayDate = rawDate.substring(0, Math.min(16, rawDate.length())).replace("T", " ");
        }
        Label dateLbl = new Label(displayDate);
        dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + UiUtils.sub() + ";");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(beanIcon, titleBox, headerSpacer, dateLbl);

        // Notes preview
        if (e.getNotes() != null && !e.getNotes().isBlank()) {
            String preview = e.getNotes().length() > 100
                ? e.getNotes().substring(0, 100) + "\u2026" : e.getNotes();
            Label notesLabel = new Label(preview);
            notesLabel.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";-fx-font-style:italic;");
            notesLabel.setWrapText(true);
            card.getChildren().addAll(headerRow, notesLabel);
        } else {
            card.getChildren().add(headerRow);
        }

        // Brew params quick summary
        Label paramsLabel = new Label(
            (e.getDoseGrams() > 0 ? e.getDoseGrams() + "g  \u00B7  " : "") +
            (e.getWaterTempC() > 0 ? e.getWaterTempC() + "\u00B0C  \u00B7  " : "") +
            (e.getGrindSize() != null && !e.getGrindSize().isBlank() ? e.getGrindSize() + " grind" : ""));
        paramsLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#8A8D91;");
        card.getChildren().add(paramsLabel);

        // Footer: action buttons
        HBox footerRow = new HBox(8);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        Button detailBtn = new Button("\uD83D\uDCC8 Details");
        detailBtn.setStyle("-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.text() + ";" +
            "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        detailBtn.setOnAction(ev -> showDetailDialog(e));

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.text() + ";" +
            "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        editBtn.setOnAction(ev -> entryDialog("Edit Brew Entry", e));

        Button delBtn = new Button("Delete");
        delBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#DC2626;" +
            "-fx-font-weight:600;-fx-font-size:12px;-fx-padding:5 12 5 12;" +
            "-fx-background-radius:8;-fx-border-color:#DC2626;-fx-border-radius:8;" +
            "-fx-border-width:1;-fx-cursor:hand;");
        delBtn.setOnAction(ev -> deleteEntry(e));

        footerRow.getChildren().addAll(detailBtn, editBtn, delBtn);
        card.getChildren().add(footerRow);
        return card;
    }

    // ── Detail dialog with radar chart ────────────────────────────────────────

    private void showDetailDialog(BrewJournalEntry e) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(e.getCoffeeBean() != null ? e.getCoffeeBean() : "Brew Entry");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        UiUtils.addStylesheet(dlg, JournalPane.class);
        dlg.getDialogPane().setPrefWidth(560);
        dlg.getDialogPane().setPrefHeight(640);

        VBox box = new VBox(16);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color:" + UiUtils.cardAlt() + ";");

        // Header card
        VBox headerCard = new VBox(6);
        headerCard.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-background-radius:12;" +
            "-fx-padding:16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,1);");
        Label titleLbl = new Label((e.getCoffeeBean() != null ? e.getCoffeeBean() : "—") +
            (e.getOrigin() != null && !e.getOrigin().isBlank() ? "  \u00B7  " + e.getOrigin() : ""));
        titleLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + UiUtils.text() + ";");
        titleLbl.setWrapText(true);
        Label methodLbl = new Label((e.getBrewMethod() != null ? e.getBrewMethod() : "") +
            (e.getRoastLevel() != null && !e.getRoastLevel().isBlank()
                ? "  \u00B7  " + e.getRoastLevel() : ""));
        methodLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";");
        headerCard.getChildren().addAll(titleLbl, methodLbl);
        box.getChildren().add(headerCard);

        // Brew parameters card
        VBox paramsCard = new VBox(6);
        paramsCard.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-background-radius:12;" +
            "-fx-padding:14 16 14 16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,1);");
        Label paramsHeading = new Label("BREW PARAMETERS");
        paramsHeading.getStyleClass().add("detail-heading");
        paramsCard.getChildren().add(paramsHeading);

        GridPane params = new GridPane();
        params.setHgap(20); params.setVgap(6);
        addParamRow(params, "Grind Size",   e.getGrindSize(),                            0);
        addParamRow(params, "Water Temp",   e.getWaterTempC() + " \u00B0C",              1);
        addParamRow(params, "Dose",         e.getDoseGrams() + " g",                     2);
        addParamRow(params, "Yield",        e.getYieldGrams() + " g",                    3);
        addParamRow(params, "Brew Time",    e.getBrewTimeSec() + " s",                   4);
        addParamRow(params, "Roast",        e.getRoastLevel(),                           5);
        paramsCard.getChildren().add(params);
        box.getChildren().add(paramsCard);

        // Radar chart card
        VBox radarCard = new VBox(10);
        radarCard.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-background-radius:12;" +
            "-fx-padding:14 16 14 16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,1);");
        Label radarHeading = new Label("FLAVOR PROFILE");
        radarHeading.getStyleClass().add("detail-heading");
        radarCard.getChildren().add(radarHeading);

        int[] vals = {e.getRatingAroma(), e.getRatingFlavor(), e.getRatingAcidity(),
                      e.getRatingBody(), e.getRatingSweetness(), e.getRatingFinish()};
        String[] lbls = {"Aroma", "Flavor", "Acidity", "Body", "Sweet", "Finish"};

        HBox chartRow = new HBox(20);
        chartRow.setAlignment(Pos.CENTER_LEFT);
        chartRow.getChildren().add(UiUtils.buildRadarCanvas(vals, lbls, 200));

        // Ratings text list
        VBox ratingsList = new VBox(5);
        String[] ratingNames = {"Aroma", "Flavor", "Acidity", "Body", "Sweetness", "Finish"};
        for (int i = 0; i < 6; i++) {
            int stars = (int) Math.round(vals[i] / 2.0);
            String starStr = "\u2605".repeat(Math.max(0, stars)) +
                             "\u2606".repeat(Math.max(0, 5 - stars));
            Label ratingRow = new Label(ratingNames[i] + ": " + starStr + "  " + vals[i] + "/10");
            ratingRow.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.text() + ";");
            ratingsList.getChildren().add(ratingRow);
        }
        chartRow.getChildren().add(ratingsList);
        radarCard.getChildren().add(chartRow);
        box.getChildren().add(radarCard);

        // Notes card
        if (e.getNotes() != null && !e.getNotes().isBlank()) {
            VBox notesCard = new VBox(6);
            notesCard.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-background-radius:12;" +
                "-fx-padding:14 16 14 16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,1);");
            Label notesHeading = new Label("TASTING NOTES");
            notesHeading.getStyleClass().add("detail-heading");
            Label notesText = new Label(e.getNotes());
            notesText.setStyle("-fx-font-size:14px;-fx-text-fill:" + UiUtils.text() + ";-fx-line-spacing:3;");
            notesText.setWrapText(true);
            notesCard.getChildren().addAll(notesHeading, notesText);
            box.getChildren().add(notesCard);
        }

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dlg.getDialogPane().setContent(scroll);
        dlg.showAndWait();
    }

    private static void addParamRow(GridPane grid, String label, String value, int row) {
        if (value == null || value.isBlank() || value.equals("0") || value.equals("0.0")) return;
        Label k = new Label(label); k.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.sub() + ";-fx-font-weight:700;");
        Label v = new Label(value); v.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.text() + ";");
        grid.add(k, 0, row); grid.add(v, 1, row);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    private void showNewDialog() {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }
        entryDialog("New Brew Entry", null);
    }

    private void entryDialog(String title, BrewJournalEntry existing) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        UiUtils.addStylesheet(dlg, JournalPane.class);
        dlg.getDialogPane().setPrefWidth(500);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        grid.setPadding(new Insets(16));
        ColumnConstraints c0 = new ColumnConstraints(110);
        ColumnConstraints c1 = new ColumnConstraints(155);
        ColumnConstraints c2 = new ColumnConstraints(110);
        ColumnConstraints c3 = new ColumnConstraints(155);
        grid.getColumnConstraints().addAll(c0, c1, c2, c3);

        TextField bean   = field("e.g. Yirgacheffe");
        TextField origin = field("e.g. Ethiopia");
        ComboBox<String> roast = new ComboBox<>(FXCollections.observableArrayList(
            "LIGHT", "MEDIUM", "MEDIUM_DARK", "DARK"));
        roast.setValue("MEDIUM"); roast.getStyleClass().add("form-field");
        TextField method = field("Pour Over (V60)");
        TextField grind  = field("Medium-Fine");
        TextField temp   = field("93");
        TextField dose   = field("18");
        TextField yield  = field("36");
        TextField time   = field("210");
        TextArea  notes  = MainWindow.styledArea("Tasting notes\u2026", 2);

        // Rating sliders (0-10)
        Slider[] sliders = new Slider[6];
        String[] ratingNames = {"Aroma", "Flavor", "Acidity", "Body", "Sweetness", "Finish"};
        VBox ratingBox = new VBox(6);
        for (int i = 0; i < 6; i++) {
            sliders[i] = new Slider(0, 10, 7);
            sliders[i].setShowTickLabels(true);
            sliders[i].setMajorTickUnit(5);
            sliders[i].setBlockIncrement(1);
            Label valLbl = new Label("7");
            valLbl.setMinWidth(22);
            int idx = i;
            sliders[i].valueProperty().addListener((o, old, v) ->
                valLbl.setText(String.valueOf((int) Math.round(v.doubleValue()))));
            HBox row = new HBox(8, new Label(ratingNames[i] + ":"), sliders[i], valLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            ((Label) row.getChildren().get(0)).setMinWidth(72);
            ratingBox.getChildren().add(row);
        }

        if (existing != null) {
            bean.setText(existing.getCoffeeBean());
            origin.setText(existing.getOrigin());
            roast.setValue(existing.getRoastLevel() == null || existing.getRoastLevel().isBlank()
                ? "MEDIUM" : existing.getRoastLevel());
            method.setText(existing.getBrewMethod());
            grind.setText(existing.getGrindSize());
            temp.setText(String.valueOf(existing.getWaterTempC()));
            dose.setText(String.valueOf(existing.getDoseGrams()));
            yield.setText(String.valueOf(existing.getYieldGrams()));
            time.setText(String.valueOf(existing.getBrewTimeSec()));
            notes.setText(existing.getNotes());
            int[] ratings = {existing.getRatingAroma(), existing.getRatingFlavor(),
                existing.getRatingAcidity(), existing.getRatingBody(),
                existing.getRatingSweetness(), existing.getRatingFinish()};
            for (int i = 0; i < 6; i++) sliders[i].setValue(ratings[i]);
        }

        DatePicker datePicker = new DatePicker(java.time.LocalDate.now());
        datePicker.getStyleClass().add("form-field");

        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, java.time.LocalTime.now().getHour());
        hourSpinner.setEditable(true); hourSpinner.setPrefWidth(70);
        Spinner<Integer> minSpinner  = new Spinner<>(0, 59, java.time.LocalTime.now().getMinute());
        minSpinner.setEditable(true); minSpinner.setPrefWidth(70);
        Label colonLbl = new Label(":");
        HBox timeRow = new HBox(6, hourSpinner, colonLbl, minSpinner);
        timeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        grid.add(lbl("Date"),          0, 0); grid.add(datePicker, 1, 0);
        grid.add(lbl("Time"),          0, 1); grid.add(timeRow,    1, 1);
        grid.add(lbl("Coffee Bean"),   0, 2); grid.add(bean,      1, 2);
        grid.add(lbl("Origin"),        2, 2); grid.add(origin,    3, 2);
        grid.add(lbl("Roast Level"),   0, 3); grid.add(roast,     1, 3);
        grid.add(lbl("Brew Method"),   2, 3); grid.add(method,    3, 3);
        grid.add(lbl("Grind Size"),    0, 4); grid.add(grind,     1, 4);
        grid.add(lbl("Temp (\u00B0C)"),2, 4); grid.add(temp,      3, 4);
        grid.add(lbl("Dose (g)"),      0, 5); grid.add(dose,      1, 5);
        grid.add(lbl("Yield (g)"),     2, 5); grid.add(yield,     3, 5);
        grid.add(lbl("Brew Time (s)"), 0, 6); grid.add(time,      1, 6);
        grid.add(lbl("Ratings"),       0, 7); grid.add(ratingBox, 1, 7, 3, 1);
        grid.add(lbl("Notes"),         0, 8); grid.add(notes,     1, 8, 3, 1);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    int uid = AppSession.userId();
                    int[] r = new int[6];
                    for (int i = 0; i < 6; i++) r[i] = (int) Math.round(sliders[i].getValue());
                    if (existing == null) {
                        String createdAt = String.format("%s %02d:%02d:00",
                            datePicker.getValue(), hourSpinner.getValue(), minSpinner.getValue());
                        BrewJournalEntry created = service.createJournalEntry(uid,
                            bean.getText().trim(), origin.getText().trim(),
                            roast.getValue(), method.getText().trim(), grind.getText().trim(),
                            parseInt(temp.getText(), 93), parseDouble(dose.getText(), 18),
                            parseDouble(yield.getText(), 36), parseInt(time.getText(), 0),
                            r[0], r[1], r[2], r[3], r[4], r[5], notes.getText().trim());
                        if (created != null && (created.getCreatedAt() == null || created.getCreatedAt().isBlank())) {
                            created.setCreatedAt(createdAt);
                        }
                        UiUtils.showToast("Brew logged!");
                    } else {
                        service.updateJournalEntry(existing.getId(), uid,
                            bean.getText().trim(), origin.getText().trim(),
                            roast.getValue(), method.getText().trim(), grind.getText().trim(),
                            parseInt(temp.getText(), 93), parseDouble(dose.getText(), 18),
                            parseDouble(yield.getText(), 36), parseInt(time.getText(), 0),
                            r[0], r[1], r[2], r[3], r[4], r[5], notes.getText().trim());
                        UiUtils.showToast("Entry updated!");
                    }
                    loadData();
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    private void deleteEntry(BrewJournalEntry e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete this brew entry?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    service.deleteJournalEntry(e.getId(), AppSession.userId());
                    loadData();
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    // ── Export dialog + CSV/PDF ───────────────────────────────────────────────

    private void openExportDialog() {
        if (entries.isEmpty()) {
            MainWindow.info("Nothing to export", "Add some journal entries first.");
            return;
        }
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Export Journal");
        dlg.setHeaderText("Select entries to export:");
        ButtonType csvBtn = new ButtonType("Export CSV", ButtonBar.ButtonData.OTHER);
        ButtonType pdfBtn = new ButtonType("Export PDF", ButtonBar.ButtonData.OTHER);
        dlg.getDialogPane().getButtonTypes().addAll(csvBtn, pdfBtn, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);

        VBox checkList = new VBox(6);
        checkList.setPadding(new Insets(8));
        java.util.List<CheckBox> boxes = new java.util.ArrayList<>();
        for (BrewJournalEntry e : entries) {
            String label = e.getCoffeeBean() + "  \u00B7  " +
                (e.getCreatedAt() != null ? e.getCreatedAt().substring(0, Math.min(10, e.getCreatedAt().length())) : "");
            CheckBox cb = new CheckBox(label);
            cb.setSelected(true);
            cb.setUserData(e);
            boxes.add(cb);
            checkList.getChildren().add(cb);
        }
        Button selectAll  = new Button("Select All");
        Button selectNone = new Button("Select None");
        selectAll.setOnAction(e2 -> boxes.forEach(b -> b.setSelected(true)));
        selectNone.setOnAction(e2 -> boxes.forEach(b -> b.setSelected(false)));
        HBox selRow = new HBox(8, selectAll, selectNone);
        selRow.setPadding(new Insets(0, 0, 8, 0));

        ScrollPane scroll = new ScrollPane(checkList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);
        VBox content = new VBox(8, selRow, scroll);
        content.setPadding(new Insets(8));
        dlg.getDialogPane().setContent(content);

        dlg.showAndWait().ifPresent(bt -> {
            java.util.List<BrewJournalEntry> selected = boxes.stream()
                .filter(CheckBox::isSelected)
                .map(b -> (BrewJournalEntry) b.getUserData())
                .toList();
            if (selected.isEmpty()) {
                MainWindow.info("Nothing selected", "Select at least one entry.");
                return;
            }
            if (bt == csvBtn) exportCsvSelected(selected);
            else if (bt == pdfBtn) exportPdfSelected(selected);
        });
    }

    private void exportCsvSelected(java.util.List<BrewJournalEntry> selected) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save CSV");
        fc.setInitialFileName("journal.csv");
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        java.io.File file = fc.showSaveDialog(null);
        if (file == null) return;

        try (java.io.PrintWriter pw = new java.io.PrintWriter(file,
                 java.nio.charset.StandardCharsets.UTF_8)) {
            pw.println("Date,Bean,Origin,Roast,Method,Grind,Temp(\u00B0C),Dose(g)," +
                       "Yield(g),BrewTime(s),Aroma,Flavor,Acidity,Body,Sweetness,Finish,Notes");
            for (BrewJournalEntry e : selected) {
                pw.printf("%s,%s,%s,%s,%s,%s,%d,%.1f,%.1f,%d,%d,%d,%d,%d,%d,%d,%s%n",
                    e.getCreatedAt(), e.getCoffeeBean(), e.getOrigin(), e.getRoastLevel(),
                    e.getBrewMethod(), e.getGrindSize(), e.getWaterTempC(),
                    e.getDoseGrams(), e.getYieldGrams(), e.getBrewTimeSec(),
                    e.getRatingAroma(), e.getRatingFlavor(), e.getRatingAcidity(),
                    e.getRatingBody(), e.getRatingSweetness(), e.getRatingFinish(),
                    e.getNotes() != null ? e.getNotes().replace(",", ";") : "");
            }
            UiUtils.showToast("CSV exported: " + file.getName());
        } catch (Exception ex) {
            MainWindow.alert("Export failed", ex.getMessage());
        }
    }

    private void exportPdfSelected(java.util.List<BrewJournalEntry> selected) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save PDF");
        fc.setInitialFileName("journal.pdf");
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        java.io.File file = fc.showSaveDialog(null);
        if (file == null) return;

        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                 new org.apache.pdfbox.pdmodel.PDDocument()) {

            org.apache.pdfbox.pdmodel.font.PDType1Font fontBold =
                new org.apache.pdfbox.pdmodel.font.PDType1Font(
                    org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
            org.apache.pdfbox.pdmodel.font.PDType1Font fontReg =
                new org.apache.pdfbox.pdmodel.font.PDType1Font(
                    org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);

            for (BrewJournalEntry e : selected) {
                org.apache.pdfbox.pdmodel.PDPage page =
                    new org.apache.pdfbox.pdmodel.PDPage(
                        org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
                doc.addPage(page);

                try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {

                    float margin = 50, y = page.getMediaBox().getHeight() - margin;
                    float lineH = 16;

                    // Title
                    cs.beginText();
                    cs.setFont(fontBold, 16);
                    cs.newLineAtOffset(margin, y);
                    String bean = e.getCoffeeBean() != null ? e.getCoffeeBean() : "Unnamed";
                    cs.showText(bean);
                    cs.endText();
                    y -= 24;

                    // Date
                    if (e.getCreatedAt() != null) {
                        cs.beginText();
                        cs.setFont(fontReg, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText(e.getCreatedAt().substring(0, Math.min(16, e.getCreatedAt().length())));
                        cs.endText();
                        y -= 20;
                    }

                    // Separator
                    cs.setLineWidth(0.5f);
                    cs.moveTo(margin, y); cs.lineTo(page.getMediaBox().getWidth() - margin, y);
                    cs.stroke();
                    y -= 14;

                    // Parameters
                    String[][] params = {
                        {"Origin",     e.getOrigin()},
                        {"Roast",      e.getRoastLevel()},
                        {"Method",     e.getBrewMethod()},
                        {"Grind",      e.getGrindSize()},
                        {"Temp",       e.getWaterTempC() + " \u00B0C"},
                        {"Dose",       e.getDoseGrams() + " g"},
                        {"Yield",      e.getYieldGrams() + " g"},
                        {"Brew Time",  e.getBrewTimeSec() + " s"},
                    };
                    for (String[] row : params) {
                        cs.beginText();
                        cs.setFont(fontBold, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText(row[0] + ": ");
                        cs.endText();
                        cs.beginText();
                        cs.setFont(fontReg, 10);
                        cs.newLineAtOffset(margin + 80, y);
                        cs.showText(row[1] != null ? row[1] : "-");
                        cs.endText();
                        y -= lineH;
                    }

                    // Ratings
                    y -= 6;
                    cs.beginText();
                    cs.setFont(fontBold, 11);
                    cs.newLineAtOffset(margin, y); cs.showText("Ratings");
                    cs.endText();
                    y -= lineH;
                    String[] ratingNames  = {"Aroma","Flavor","Acidity","Body","Sweetness","Finish"};
                    int[]    ratingValues = {e.getRatingAroma(), e.getRatingFlavor(),
                        e.getRatingAcidity(), e.getRatingBody(),
                        e.getRatingSweetness(), e.getRatingFinish()};
                    for (int i = 0; i < ratingNames.length; i++) {
                        cs.beginText();
                        cs.setFont(fontReg, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText(ratingNames[i] + ": " + ratingValues[i] + "/10");
                        cs.endText();
                        y -= lineH;
                    }

                    // Notes
                    if (e.getNotes() != null && !e.getNotes().isBlank()) {
                        y -= 6;
                        cs.beginText();
                        cs.setFont(fontBold, 11);
                        cs.newLineAtOffset(margin, y); cs.showText("Notes");
                        cs.endText();
                        y -= lineH;
                        cs.beginText();
                        cs.setFont(fontReg, 10);
                        cs.newLineAtOffset(margin, y);
                        String notes = e.getNotes();
                        for (int start = 0; start < notes.length(); start += 80) {
                            String chunk = notes.substring(start, Math.min(start + 80, notes.length()));
                            cs.showText(chunk);
                            if (start + 80 < notes.length()) {
                                cs.newLineAtOffset(0, -lineH);
                                y -= lineH;
                            }
                        }
                        cs.endText();
                    }
                }
            }

            doc.save(file);
            UiUtils.showToast("PDF exported: " + file.getName());
        } catch (Exception ex) {
            MainWindow.alert("PDF export failed", ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String csvQ(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private static TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("form-field");
        return f;
    }

    private static Label lbl(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("form-label");
        return l;
    }

    private static int    parseInt(String s, int def)       { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } }
    private static double parseDouble(String s, double def) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; } }

    public BorderPane getRoot() { return root; }
}
