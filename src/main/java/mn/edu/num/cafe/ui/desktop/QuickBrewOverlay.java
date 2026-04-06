package mn.edu.num.cafe.ui.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import mn.edu.num.cafe.core.domain.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class QuickBrewOverlay {

    static void show(Recipe recipe, Consumer<Recipe> onStartTimer) {
        List<String> steps = new ArrayList<>();
        String instructions = recipe.getInstructions() != null ? recipe.getInstructions() : "";
        for (String line : instructions.split("\n")) {
            if (!line.isBlank()) steps.add(line.trim());
        }
        if (steps.isEmpty()) steps.add("Brew for " + recipe.getBrewTime() + " minutes.");

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Quick Brew Card");

        int[] idx = {0};

        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(Double.MAX_VALUE);
        progress.setStyle("-fx-accent:#A8621E;-fx-background-color:#2B1005;" +
            "-fx-background-radius:0;-fx-pref-height:4px;");

        Label stepNumLbl = new Label();
        stepNumLbl.setStyle(
            "-fx-min-width:56px;-fx-min-height:56px;-fx-max-width:56px;-fx-max-height:56px;" +
            "-fx-background-color:linear-gradient(135deg,#A8621E,#CB8840);" +
            "-fx-background-radius:28;-fx-text-fill:white;-fx-font-size:22px;" +
            "-fx-font-weight:800;-fx-alignment:center;");

        Label stepText = new Label();
        stepText.setWrapText(true);
        stepText.setMaxWidth(640);
        stepText.setStyle("-fx-font-size:26px;-fx-text-fill:#F6E8CC;" +
            "-fx-font-weight:600;-fx-line-spacing:4;-fx-text-alignment:center;");
        stepText.setAlignment(Pos.CENTER);

        Label meta = new Label(recipe.getTitle() + "  \u00B7  " +
            recipe.getBrewTime() + " min  \u00B7  " + recipe.getDifficulty());
        meta.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(246,232,204,0.55);");

        Button prevBtn  = new Button("\u2190 Prev");
        Button nextBtn  = new Button("Next \u2192");
        Button timerBtn = new Button("\u23F1 Start Timer");
        Button closeBtn = new Button("\u2715 Close");

        prevBtn.setStyle("-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:#F6E8CC;" +
            "-fx-font-size:14px;-fx-font-weight:600;-fx-padding:10 24 10 24;" +
            "-fx-background-radius:20;-fx-border-width:0;-fx-cursor:hand;");
        nextBtn.setStyle("-fx-background-color:#A8621E;-fx-text-fill:white;" +
            "-fx-font-size:14px;-fx-font-weight:700;-fx-padding:10 28 10 28;" +
            "-fx-background-radius:20;-fx-border-width:0;-fx-cursor:hand;");
        timerBtn.setStyle("-fx-background-color:rgba(255,255,255,0.12);-fx-text-fill:#F5C060;" +
            "-fx-font-size:13px;-fx-font-weight:700;-fx-padding:9 22 9 22;" +
            "-fx-background-radius:20;-fx-border-color:rgba(232,160,48,0.5);" +
            "-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
        closeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.4);" +
            "-fx-font-size:12px;-fx-padding:6 14 6 14;-fx-border-width:0;-fx-cursor:hand;");

        HBox navRow = new HBox(14, prevBtn, nextBtn, timerBtn, closeBtn);
        navRow.setAlignment(Pos.CENTER);

        int total = steps.size();
        Runnable updateView = () -> {
            int i = idx[0];
            stepNumLbl.setText(String.valueOf(i + 1));
            stepText.setText(steps.get(i));
            progress.setProgress((double)(i + 1) / total);
            prevBtn.setDisable(i == 0);
            nextBtn.setText(i == total - 1 ? "Done \u2713" : "Next \u2192");
        };
        updateView.run();

        prevBtn.setOnAction(e -> { if (idx[0] > 0) { idx[0]--; updateView.run(); } });
        nextBtn.setOnAction(e -> {
            if (idx[0] < total - 1) { idx[0]++; updateView.run(); }
            else stage.close();
        });
        timerBtn.setOnAction(e -> {
            stage.close();
            if (onStartTimer != null) onStartTimer.accept(recipe);
        });
        closeBtn.setOnAction(e -> stage.close());

        VBox center = new VBox(32, stepNumLbl, stepText, meta);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(48, 64, 32, 64));
        VBox.setVgrow(center, Priority.ALWAYS);

        VBox root = new VBox(0, progress, center, navRow);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0, 0, 40, 0));
        root.setStyle("-fx-background-color:linear-gradient(160deg,#0C0400,#1E0800,#3A1505);");

        Scene scene = new Scene(root, 820, 560);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }
}
