package mn.edu.num.cafe.ui.desktop;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.domain.BrewJournalEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Brew Journal pane — TableView with CRUD and CSV export.
 */
public class JournalPane {

    private final BorderPane root;
    private final BorgolService service;
    private final ObservableList<BrewJournalEntry> items = FXCollections.observableArrayList();
    private final TableView<BrewJournalEntry> table;

    public JournalPane(BorgolService service) {
        this.service = service;
        root  = new BorderPane();
        table = buildTable();
        root.setTop(buildToolbar());
        root.setCenter(table);
        root.getStyleClass().add("content-pane");
        loadData();
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private TableView<BrewJournalEntry> buildTable() {
        TableView<BrewJournalEntry> tv = new TableView<>(items);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.getStyleClass().add("data-table");

        tv.getColumns().addAll(List.of(
            col("Coffee Bean", 180, e -> e.getCoffeeBean()),
            col("Origin",      120, e -> e.getOrigin()),
            col("Roast",        80, e -> e.getRoastLevel()),
            col("Method",      110, e -> e.getBrewMethod()),
            col("Grind",        90, e -> e.getGrindSize()),
            col("Temp °C",      70, e -> String.valueOf(e.getWaterTempC())),
            col("Dose g",       65, e -> String.valueOf(e.getDoseGrams())),
            col("⭐ Avg",        65, e -> {
                int[] v = {e.getRatingAroma(), e.getRatingFlavor(), e.getRatingAcidity(),
                            e.getRatingBody(), e.getRatingSweetness(), e.getRatingFinish()};
                double avg = 0; for (int x : v) avg += x; avg /= v.length;
                return String.format("%.1f", avg);
            }),
            col("Notes",       200, e -> e.getNotes())
        ));
        return tv;
    }

    private TableColumn<BrewJournalEntry, String> col(
            String name, int w, java.util.function.Function<BrewJournalEntry, String> fn) {
        TableColumn<BrewJournalEntry, String> c = new TableColumn<>(name);
        c.setCellValueFactory(cd -> new SimpleStringProperty(fn.apply(cd.getValue())));
        c.setPrefWidth(w);
        return c;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📓 Brew Journal");
        title.getStyleClass().add("pane-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNew    = new Button("+ New Entry");
        Button btnEdit   = new Button("Edit");
        Button btnDel    = new Button("Delete");
        Button btnCsv    = new Button("📊 Export CSV");

        btnNew.getStyleClass().add("btn-primary");
        btnEdit.getStyleClass().add("btn-secondary");
        btnDel.getStyleClass().add("btn-danger");
        btnCsv.getStyleClass().add("btn-secondary");

        btnNew.setOnAction(e  -> showNewDialog());
        btnEdit.setOnAction(e -> showEditDialog());
        btnDel.setOnAction(e  -> deleteSelected());
        btnCsv.setOnAction(e  -> exportCsv());

        if (!AppSession.loggedIn()) {
            btnNew.setDisable(true);
            btnEdit.setDisable(true);
            btnDel.setDisable(true);
        }

        bar.getChildren().addAll(title, spacer, btnCsv, btnNew, btnEdit, btnDel);
        return bar;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        items.clear();
        if (!AppSession.loggedIn()) return;
        try {
            items.setAll(service.getJournalEntries(AppSession.userId()));
        } catch (Exception e) {
            MainWindow.alert("Error", e.getMessage());
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    private void showNewDialog() {
        if (!AppSession.loggedIn()) { MainWindow.alert("Login required", "Please log in first."); return; }
        entryDialog("New Brew Entry", null);
    }

    private void showEditDialog() {
        BrewJournalEntry sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { MainWindow.info("Select an entry", "Select a journal entry first."); return; }
        entryDialog("Edit Brew Entry", sel);
    }

    private void entryDialog(String title, BrewJournalEntry existing) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().add(
            getClass().getResource("/style.css").toExternalForm());
        dlg.getDialogPane().setPrefWidth(500);

        // ── Two-column form ───────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        grid.setPadding(new Insets(16));
        ColumnConstraints c0 = new ColumnConstraints(100);
        ColumnConstraints c1 = new ColumnConstraints(160);
        ColumnConstraints c2 = new ColumnConstraints(100);
        ColumnConstraints c3 = new ColumnConstraints(160);
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
        TextArea  notes  = MainWindow.styledArea("Tasting notes…", 2);

        // Rating sliders
        Slider[] sliders = new Slider[6];
        String[] ratingNames = {"Aroma", "Flavor", "Acidity", "Body", "Sweetness", "Finish"};
        VBox ratingBox = new VBox(6);
        for (int i = 0; i < 6; i++) {
            sliders[i] = new Slider(0, 10, 7);
            sliders[i].setShowTickLabels(true);
            sliders[i].setMajorTickUnit(5);
            sliders[i].setBlockIncrement(1);
            sliders[i].setSnapToTicks(false);
            Label valLbl = new Label("7");
            valLbl.setMinWidth(22);
            int idx = i;
            sliders[i].valueProperty().addListener((o, old, v) ->
                valLbl.setText(String.valueOf((int) Math.round(v.doubleValue()))));
            HBox row = new HBox(8, new Label(ratingNames[i] + ":"), sliders[i], valLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            ((Label)row.getChildren().get(0)).setMinWidth(72);
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

        grid.add(lbl("Coffee Bean"),    0, 0); grid.add(bean,   1, 0);
        grid.add(lbl("Origin"),         2, 0); grid.add(origin, 3, 0);
        grid.add(lbl("Roast Level"),    0, 1); grid.add(roast,  1, 1);
        grid.add(lbl("Brew Method"),    2, 1); grid.add(method, 3, 1);
        grid.add(lbl("Grind Size"),     0, 2); grid.add(grind,  1, 2);
        grid.add(lbl("Temp (°C)"),      2, 2); grid.add(temp,   3, 2);
        grid.add(lbl("Dose (g)"),       0, 3); grid.add(dose,   1, 3);
        grid.add(lbl("Yield (g)"),      2, 3); grid.add(yield,  3, 3);
        grid.add(lbl("Brew Time (s)"),  0, 4); grid.add(time,   1, 4);
        grid.add(lbl("Ratings"),        0, 5); grid.add(ratingBox, 1, 5, 3, 1);
        grid.add(lbl("Notes"),          0, 6); grid.add(notes,     1, 6, 3, 1);

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
                    } else {
                        service.updateJournalEntry(existing.getId(), uid,
                            bean.getText().trim(), origin.getText().trim(),
                            roast.getValue(), method.getText().trim(), grind.getText().trim(),
                            parseInt(temp.getText(), 93), parseDouble(dose.getText(), 18),
                            parseDouble(yield.getText(), 36), parseInt(time.getText(), 0),
                            r[0], r[1], r[2], r[3], r[4], r[5], notes.getText().trim());
                    }
                    loadData();
                } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
            }
        });
    }

    private void deleteSelected() {
        BrewJournalEntry sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { MainWindow.info("Select an entry", "Select an entry to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete this brew entry?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                service.deleteJournalEntry(sel.getId(), AppSession.userId());
                loadData();
            }
        });
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    private void exportCsv() {
        if (items.isEmpty()) { MainWindow.info("Nothing to export", "No journal entries to export."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Journal CSV");
        fc.setInitialFileName("borgol-journal.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = fc.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;

        String header = "id,coffeeBean,origin,roastLevel,brewMethod,grindSize," +
            "waterTempC,doseGrams,yieldGrams,brewTimeSec," +
            "ratingAroma,ratingFlavor,ratingAcidity,ratingBody,ratingSweetness,ratingFinish," +
            "notes,createdAt";

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            pw.println(header);
            for (BrewJournalEntry e : items) {
                pw.printf("%d,%s,%s,%s,%s,%s,%d,%.1f,%.1f,%d,%d,%d,%d,%d,%d,%d,%s,%s%n",
                    e.getId(), csvQ(e.getCoffeeBean()), csvQ(e.getOrigin()),
                    csvQ(e.getRoastLevel()), csvQ(e.getBrewMethod()), csvQ(e.getGrindSize()),
                    e.getWaterTempC(), e.getDoseGrams(), e.getYieldGrams(), e.getBrewTimeSec(),
                    e.getRatingAroma(), e.getRatingFlavor(), e.getRatingAcidity(),
                    e.getRatingBody(), e.getRatingSweetness(), e.getRatingFinish(),
                    csvQ(e.getNotes()), csvQ(e.getCreatedAt()));
            }
            MainWindow.info("Exported", "Saved " + items.size() + " entries to:\n" + file.getPath());
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

    private static int    parseInt(String s, int def)       { try { return Integer.parseInt(s.trim()); } catch(Exception e){return def;} }
    private static double parseDouble(String s, double def) { try { return Double.parseDouble(s.trim()); } catch(Exception e){return def;} }

    public BorderPane getRoot() { return root; }
}
