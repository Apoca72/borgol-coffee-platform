package mn.edu.num.cafe.ui.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.domain.CafeListing;

import java.util.List;

class MapPane {

    private final BorderPane root;
    private final BorgolService service;
    private WebEngine engine;
    private double currentLat = 47.9077;
    private double currentLng = 106.8832;
    private double currentRadius = 5.0;
    private VBox cafeList;

    MapPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        root.setTop(buildToolbar());

        WebView map = new WebView();
        engine = map.getEngine();
        engine.loadContent(buildLeafletHtml());

        cafeList = new VBox(10);
        cafeList.setPadding(new Insets(16));
        ScrollPane cafeScroll = new ScrollPane(cafeList);
        cafeScroll.setFitToWidth(true);
        cafeScroll.setStyle("-fx-background-color:" + UiUtils.bg() + ";-fx-background:" + UiUtils.bg() + ";");
        cafeScroll.setPrefHeight(200);

        VBox center = new VBox(0, map, cafeScroll);
        VBox.setVgrow(map, Priority.ALWAYS);
        root.setCenter(center);

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) refreshMap();
        });
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-border-color:" +
            UiUtils.border() + ";-fx-border-width:0 0 1 0;");

        Label title = new Label("\uD83D\uDDFA\uFE0F  Map");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + UiUtils.text() + ";");

        Label radiusLbl = new Label("Radius:");
        radiusLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";");

        ComboBox<String> radiusBox = new ComboBox<>();
        radiusBox.getItems().addAll("2 km", "5 km", "10 km", "25 km", "50 km");
        radiusBox.setValue("5 km");
        radiusBox.setOnAction(e -> {
            String v = radiusBox.getValue().replace(" km", "");
            currentRadius = Double.parseDouble(v);
            refreshMap();
        });

        Button locateBtn = new Button("\uD83D\uDCCD Locate Me");
        locateBtn.setStyle("-fx-background-color:#A8621E;-fx-text-fill:white;-fx-font-weight:700;" +
            "-fx-padding:7 16 7 16;-fx-background-radius:20;-fx-border-width:0;-fx-cursor:hand;");
        locateBtn.setOnAction(e -> showLocateDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(title, spacer, radiusLbl, radiusBox, locateBtn);
        return bar;
    }

    private void showLocateDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Enter Location");
        dlg.setHeaderText("Enter coordinates (Ulaanbaatar: 47.9077, 106.8832)");
        ButtonType okBtn = new ButtonType("Go", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField latFld = new TextField("47.9077");
        TextField lngFld = new TextField("106.8832");
        grid.add(new Label("Latitude:"),  0, 0); grid.add(latFld, 1, 0);
        grid.add(new Label("Longitude:"), 0, 1); grid.add(lngFld, 1, 1);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == okBtn) {
                try {
                    currentLat = Double.parseDouble(latFld.getText().trim());
                    currentLng = Double.parseDouble(lngFld.getText().trim());
                    refreshMap();
                } catch (NumberFormatException ex) {
                    MainWindow.alert("Invalid input", "Please enter numeric coordinates.");
                }
            }
        });
    }

    private void refreshMap() {
        int uid = AppSession.loggedIn() ? AppSession.userId() : 0;
        List<CafeListing> cafes;
        try {
            cafes = service.getCafesNearby(uid, currentLat, currentLng, currentRadius);
        } catch (Exception e) { cafes = List.of(); }

        StringBuilder js = new StringBuilder("clearMarkers();setView(")
            .append(currentLat).append(",").append(currentLng).append(");");
        for (CafeListing c : cafes) {
            Double lat = c.getLat();
            Double lng = c.getLng();
            if (lat == null || lng == null || (lat == 0 && lng == 0)) continue;
            String name = c.getName().replace("'", "\\'");
            String addr = (c.getAddress() != null ? c.getAddress() : "").replace("'", "\\'");
            js.append("addMarker(").append(lat).append(",")
              .append(lng).append(",'").append(name)
              .append("','").append(addr).append("');");
        }
        engine.executeScript(js.toString());

        cafeList.getChildren().clear();
        if (cafes.isEmpty()) {
            cafeList.getChildren().add(UiUtils.emptyState(
                "\uD83D\uDDFA\uFE0F", "No cafes found nearby",
                "Try increasing the radius or adjusting your location."));
        } else {
            for (CafeListing c : cafes) {
                Label name = new Label(c.getName());
                name.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:" + UiUtils.text() + ";");
                String addrStr = c.getAddress() != null ? c.getAddress() : "";
                Label addr = new Label(addrStr);
                addr.setStyle("-fx-font-size:12px;-fx-text-fill:" + UiUtils.sub() + ";");
                VBox card = new VBox(3, name, addr);
                card.setStyle("-fx-background-color:" + UiUtils.card() + ";-fx-padding:10 14 10 14;" +
                    "-fx-background-radius:10;-fx-border-color:" + UiUtils.border() + ";" +
                    "-fx-border-radius:10;-fx-border-width:1;");
                cafeList.getChildren().add(card);
            }
        }
    }

    private static String buildLeafletHtml() {
        return """
            <!DOCTYPE html><html><head>
            <meta charset="UTF-8"/>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>html,body,#map{margin:0;padding:0;width:100%;height:100%}</style>
            </head><body>
            <div id="map"></div>
            <script>
              var map = L.map('map').setView([47.9077, 106.8832], 13);
              L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
                attribution:'&copy; OpenStreetMap contributors'}).addTo(map);
              var markers = [];
              function clearMarkers() { markers.forEach(function(m){map.removeLayer(m);}); markers=[]; }
              function setView(lat,lng) { map.setView([lat,lng], 13); }
              function addMarker(lat,lng,name,addr) {
                var m = L.marker([lat,lng]).addTo(map);
                m.bindPopup('<b>'+name+'</b><br/>'+addr);
                markers.push(m);
              }
            </script></body></html>
            """;
    }

    BorderPane getRoot() { return root; }
}
