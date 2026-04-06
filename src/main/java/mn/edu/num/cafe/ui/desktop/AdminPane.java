package mn.edu.num.cafe.ui.desktop;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import mn.edu.num.cafe.core.application.BorgolService;

import java.util.List;
import java.util.Map;

class AdminPane {

    private final BorderPane root;
    private final BorgolService service;
    private Label statsLabel;
    private TableView<Map<String, Object>> table;

    AdminPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        root.setTop(buildToolbar());
        root.setCenter(buildTable());
        loadData();
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-border-color:" +
            UiUtils.border() + ";-fx-border-width:0 0 1 0;");
        Label title = new Label("\u2699\uFE0F  Admin");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + UiUtils.text() + ";");
        statsLabel = new Label("Loading\u2026");
        statsLabel.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refresh = new Button("\u21BB Refresh");
        refresh.setStyle("-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:" + UiUtils.text() + ";" +
            "-fx-font-size:13px;-fx-padding:6 14 6 14;-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        refresh.setOnAction(e -> loadData());
        bar.getChildren().addAll(title, statsLabel, spacer, refresh);
        return bar;
    }

    @SuppressWarnings("unchecked")
    private TableView<Map<String, Object>> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color:" + UiUtils.bg() + ";");

        TableColumn<Map<String,Object>, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getOrDefault("contentType", "").toString()));

        TableColumn<Map<String,Object>, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getOrDefault("reason", "").toString()));

        TableColumn<Map<String,Object>, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getOrDefault("description", "").toString()));

        TableColumn<Map<String,Object>, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button resolve = new Button("Resolve");
            private final Button dismiss = new Button("Dismiss");
            {
                resolve.setStyle("-fx-background-color:#2E7D32;-fx-text-fill:white;-fx-font-size:12px;" +
                    "-fx-padding:4 10 4 10;-fx-background-radius:6;-fx-border-width:0;-fx-cursor:hand;");
                dismiss.setStyle("-fx-background-color:rgba(0,0,0,0.1);-fx-text-fill:" + UiUtils.text() + ";-fx-font-size:12px;" +
                    "-fx-padding:4 10 4 10;-fx-background-radius:6;-fx-border-width:0;-fx-cursor:hand;");
                resolve.setOnAction(e -> handleReport(getTableRow().getItem(), "resolved"));
                dismiss.setOnAction(e -> handleReport(getTableRow().getItem(), "dismissed"));
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(new HBox(6, resolve, dismiss));
            }
        });

        table.getColumns().addAll(typeCol, reasonCol, descCol, actionsCol);
        return table;
    }

    private void handleReport(Map<String, Object> report, String action) {
        if (report == null) return;
        int id = ((Number) report.getOrDefault("id", 0)).intValue();
        try {
            service.resolveReport(id, AppSession.userId(), action);
            UiUtils.showToast(action.equals("resolved") ? "Report resolved." : "Report dismissed.");
            loadData();
        } catch (Exception ex) { MainWindow.alert("Error", ex.getMessage()); }
    }

    private void loadData() {
        try {
            Map<String, Object> stats = service.getAdminStats();
            int pending = ((Number) stats.getOrDefault("pendingReports", 0)).intValue();
            statsLabel.setText(pending + " pending report" + (pending == 1 ? "" : "s"));
            List<Map<String, Object>> reports = service.getReports("pending");
            table.setItems(FXCollections.observableArrayList(reports));
        } catch (Exception e) {
            statsLabel.setText("Error loading stats");
        }
    }

    BorderPane getRoot() { return root; }
}
