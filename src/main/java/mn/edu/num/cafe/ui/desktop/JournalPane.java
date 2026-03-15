package mn.edu.num.cafe.ui.desktop;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.domain.BrewJournalEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        Button btnCsv  = new Button("\uD83D\uDCCA Export CSV");
        btnNew.getStyleClass().add("btn-primary");
        btnCsv.getStyleClass().add("btn-secondary");
        btnNew.setOnAction(e -> showNewDialog());
        btnCsv.setOnAction(e -> exportCsv());
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
        try {
            entries = service.getJournalEntries(AppSession.userId());
            if (entries.isEmpty()) {
                listBox.getChildren().add(UiUtils.emptyState(
                    "\uD83D\uDCD3", "No entries yet",
                    "Start logging your coffee brews to track flavors over time."));
            } else {
                for (BrewJournalEntry e : entries) listBox.getChildren().add(buildEntryCard(e));
            }
        } catch (Exception e) {
            MainWindow.alert("Error", e.getMessage());
        }
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
        headerRow.getChildren().addAll(beanIcon, titleBox);

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
            roast.setValue(existing.getRoastLevel().isBlank() ? "MEDIUM" : existing.getRoastLevel());
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

        grid.add(lbl("Coffee Bean"),   0, 0); grid.add(bean,      1, 0);
        grid.add(lbl("Origin"),        2, 0); grid.add(origin,    3, 0);
        grid.add(lbl("Roast Level"),   0, 1); grid.add(roast,     1, 1);
        grid.add(lbl("Brew Method"),   2, 1); grid.add(method,    3, 1);
        grid.add(lbl("Grind Size"),    0, 2); grid.add(grind,     1, 2);
        grid.add(lbl("Temp (\u00B0C)"),2, 2); grid.add(temp,      3, 2);
        grid.add(lbl("Dose (g)"),      0, 3); grid.add(dose,      1, 3);
        grid.add(lbl("Yield (g)"),     2, 3); grid.add(yield,     3, 3);
        grid.add(lbl("Brew Time (s)"), 0, 4); grid.add(time,      1, 4);
        grid.add(lbl("Ratings"),       0, 5); grid.add(ratingBox, 1, 5, 3, 1);
        grid.add(lbl("Notes"),         0, 6); grid.add(notes,     1, 6, 3, 1);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    int uid = AppSession.userId();
                    int[] r = new int[6];
                    for (int i = 0; i < 6; i++) r[i] = (int) Math.round(sliders[i].getValue());
                    if (existing == null) {
                        service.createJournalEntry(uid,
                            bean.getText().trim(), origin.getText().trim(),
                            roast.getValue(), method.getText().trim(), grind.getText().trim(),
                            parseInt(temp.getText(), 93), parseDouble(dose.getText(), 18),
                            parseDouble(yield.getText(), 36), parseInt(time.getText(), 0),
                            r[0], r[1], r[2], r[3], r[4], r[5], notes.getText().trim());
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
                service.deleteJournalEntry(e.getId(), AppSession.userId());
                loadData();
            }
        });
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    private void exportCsv() {
        if (entries.isEmpty()) { MainWindow.info("Nothing to export", "No journal entries to export."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Journal CSV");
        fc.setInitialFileName("borgol-journal.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        java.io.File file = fc.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;

        String header = "id,coffeeBean,origin,roastLevel,brewMethod,grindSize," +
            "waterTempC,doseGrams,yieldGrams,brewTimeSec," +
            "ratingAroma,ratingFlavor,ratingAcidity,ratingBody,ratingSweetness,ratingFinish," +
            "notes,createdAt";
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            pw.println(header);
            for (BrewJournalEntry e : entries) {
                pw.printf("%d,%s,%s,%s,%s,%s,%d,%.1f,%.1f,%d,%d,%d,%d,%d,%d,%d,%s,%s%n",
                    e.getId(), csvQ(e.getCoffeeBean()), csvQ(e.getOrigin()),
                    csvQ(e.getRoastLevel()), csvQ(e.getBrewMethod()), csvQ(e.getGrindSize()),
                    e.getWaterTempC(), e.getDoseGrams(), e.getYieldGrams(), e.getBrewTimeSec(),
                    e.getRatingAroma(), e.getRatingFlavor(), e.getRatingAcidity(),
                    e.getRatingBody(), e.getRatingSweetness(), e.getRatingFinish(),
                    csvQ(e.getNotes()), csvQ(e.getCreatedAt()));
            }
            MainWindow.info("Exported", "Saved " + entries.size() + " entries to:\n" + file.getPath());
        } catch (IOException ex) {
            MainWindow.alert("Export failed", ex.getMessage());
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
