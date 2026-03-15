package mn.edu.num.cafe.ui.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import mn.edu.num.cafe.core.application.BorgolService;

import java.io.File;
import java.util.Objects;

/**
 * JavaFX Application entry point for the Borgol desktop app.
 *
 * Architecture: ui/desktop layer — calls BorgolService directly,
 * no REST API or HTTP server needed for desktop mode.
 */
public class BorgolApp extends Application {

    // Injected before launch() via setService()
    private static BorgolService borgolService;

    /** Called by Main.java — sets service before JavaFX starts. */
    public static void setService(BorgolService service) {
        borgolService = service;
    }

    @Override
    public void start(Stage stage) {
        MainWindow window = new MainWindow(borgolService, stage);

        Scene scene = new Scene(window.getRoot(), 1280, 760);
        scene.getStylesheets().add(
            Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());

        stage.setTitle("Borgol ☕ Coffee Enthusiast Platform");
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        // Show welcome tutorial on first launch
        Platform.runLater(this::showWelcomeIfNeeded);
    }

    private void showWelcomeIfNeeded() {
        File flag = new File("data/welcomed.flag");
        if (flag.exists()) return;

        // Create flag so we don't show again
        try { flag.getParentFile().mkdirs(); flag.createNewFile(); } catch (Exception ignored) {}

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Welcome to Borgol!");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getStylesheets().add(
            Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());

        ButtonType startBtn = new ButtonType("Get Started!", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().add(startBtn);
        dlg.getDialogPane().setPrefWidth(500);

        VBox content = new VBox(0);

        // Top banner
        VBox banner = new VBox(6);
        banner.setAlignment(Pos.CENTER);
        banner.setPadding(new Insets(28, 24, 20, 24));
        banner.setStyle("-fx-background-color:#D4621A;");

        Label logo = new Label("\uD83E\uDEB7");
        logo.setStyle("-fx-font-size:48px;");

        Label title = new Label("Borgol Coffee Platform");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;");

        Label sub = new Label("Your home for specialty coffee recipes,\ncafes, and community.");
        sub.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(255,255,255,0.85);-fx-text-alignment:center;");
        sub.setTextAlignment(TextAlignment.CENTER);

        banner.getChildren().addAll(logo, title, sub);

        // Body
        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 24, 24, 24));
        body.setStyle("-fx-background-color:white;");

        body.getChildren().add(infoSection("🗺️ Navigation",
            "Recipes · Cafes · Journal · Learn · Feed · People · Profile"));

        body.getChildren().add(infoSection("👤 Demo Accounts  (password: password123)",
            "coffee@borgol.mn  — coffee_master (Barista)\n" +
            "sara@borgol.mn    — barista_sara (Expert)\n" +
            "tea@borgol.mn     — tea_lover (Enthusiast)\n" +
            "latte@borgol.mn   — latte_king  ·  espresso@borgol.mn — espresso_pro\n" +
            "coldbrew@borgol.mn — cold_brew_queen"));

        body.getChildren().add(infoSection("✨ Features",
            "Browse & post recipes  ·  Brew Journal with CSV export\n" +
            "Cafe finder with GPS map  ·  Brew Guides  ·  Learn articles\n" +
            "Follow users  ·  Like & comment  ·  Flavor radar chart"));

        content.getChildren().addAll(banner, body);
        dlg.getDialogPane().setContent(content);
        dlg.showAndWait();
    }

    private static VBox infoSection(String heading, String text) {
        VBox box = new VBox(4);
        Label h = new Label(heading);
        h.setStyle("-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:#D4621A;");
        Label t = new Label(text);
        t.setStyle("-fx-font-size:13px;-fx-text-fill:#1C1E21;-fx-line-spacing:3;");
        t.setWrapText(true);
        box.getChildren().addAll(h, t);
        return box;
    }
}
