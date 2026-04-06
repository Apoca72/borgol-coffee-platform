package mn.edu.num.cafe.ui.desktop;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Brew Timer pane — matches the web brew-timer.html.
 * Method presets: Espresso, Pour Over, French Press, Cold Brew, Aeropress, Moka Pot.
 * Animated ring timer with step-by-step guide.
 */
class BrewTimerPane {

    private final BorderPane root;

    // Timer state
    private int    totalSeconds;
    private int    elapsed = 0;
    private boolean running = false;
    private Timeline clock;

    // UI refs updated during ticks
    private Canvas  ringCanvas;
    private Label   timeDisplay;
    private Label   stepLabel;
    private Label   phaseLabel;
    private Button  startBtn;
    private VBox    stepsBox;

    // Selected method
    private BrewMethod current;

    // ── Brew method data ──────────────────────────────────────────────────────

    record BrewStep(int atSecond, String label) {}

    record BrewMethod(
        String name, String emoji, int durationSeconds,
        String ratio, String grind, String temp,
        BrewStep[] steps
    ) {}

    private static final BrewMethod[] METHODS = {
        new BrewMethod("Espresso", "\u2615", 30,
            "1:2  (18g → 36g)", "Fine", "93°C",
            new BrewStep[]{
                new BrewStep(0,  "Lock in portafilter, start shot"),
                new BrewStep(5,  "First drops — check flow rate"),
                new BrewStep(15, "Mid extraction — watch color"),
                new BrewStep(25, "Blonde-ing — stop soon"),
                new BrewStep(30, "Done! Remove cup")
            }),
        new BrewMethod("Pour Over", "\ud83d\udc3c", 210,
            "1:15  (25g → 375ml)", "Medium-fine", "93°C",
            new BrewStep[]{
                new BrewStep(0,   "Pour 50ml bloom water"),
                new BrewStep(30,  "Bloom complete — start first pour"),
                new BrewStep(60,  "Pour to 200ml in circles"),
                new BrewStep(120, "Second pour to 300ml"),
                new BrewStep(165, "Final pour to 375ml"),
                new BrewStep(210, "Drawdown complete — enjoy!")
            }),
        new BrewMethod("French Press", "\u2615", 240,
            "1:15  (30g → 450ml)", "Coarse", "94°C",
            new BrewStep[]{
                new BrewStep(0,   "Add coffee, pour all water"),
                new BrewStep(30,  "Stir gently, place lid"),
                new BrewStep(60,  "Let it steep — don't press yet"),
                new BrewStep(180, "Almost ready"),
                new BrewStep(240, "Press slowly and pour immediately")
            }),
        new BrewMethod("Cold Brew", "\ud83e\uddc8", 43200,
            "1:8  (100g → 800ml)", "Extra coarse", "Cold",
            new BrewStep[]{
                new BrewStep(0,      "Combine coffee and cold water"),
                new BrewStep(3600,   "1 hour — stir gently"),
                new BrewStep(21600,  "6 hours — halfway there"),
                new BrewStep(36000,  "10 hours — almost done"),
                new BrewStep(43200,  "Strain and refrigerate — ready!")
            }),
        new BrewMethod("Aeropress", "\ud83e\uddd1\u200d\ud83c\udf73", 90,
            "1:12  (17g → 200ml)", "Medium", "85°C",
            new BrewStep[]{
                new BrewStep(0,  "Add coffee, pour to 200ml"),
                new BrewStep(10, "Stir 10 seconds"),
                new BrewStep(60, "Attach cap, flip if inverted"),
                new BrewStep(75, "Begin pressing — 15 sec press"),
                new BrewStep(90, "Press complete!")
            }),
        new BrewMethod("Moka Pot", "\ud83d\uded1", 300,
            "1:7  (20g → 140ml)", "Medium-fine", "Low heat",
            new BrewStep[]{
                new BrewStep(0,   "Fill bottom with hot water to valve"),
                new BrewStep(30,  "Add coffee, assemble, heat on low"),
                new BrewStep(120, "Watch for first coffee flow"),
                new BrewStep(180, "Steady flow — almost done"),
                new BrewStep(260, "Hear gurgling — remove from heat"),
                new BrewStep(300, "Pour immediately, enjoy!")
            }),
    };

    // ─────────────────────────────────────────────────────────────────────────

    BrewTimerPane() {
        String pref = java.util.prefs.Preferences
            .userRoot().node("borgol/desktop").get("defaultTimerMethod", "Pour Over");
        current = java.util.Arrays.stream(METHODS)
            .filter(m -> m.name().equals(pref))
            .findFirst().orElse(METHODS[1]);
        totalSeconds = current.durationSeconds();

        // ── Toolbar ──────────────────────────────────────────────────────────
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("\u23F1\uFE0F Brew Timer");
        title.getStyleClass().add("pane-title");
        toolbar.getChildren().add(title);

        // ── Method selector chips ─────────────────────────────────────────────
        HBox methodRow = new HBox(8);
        methodRow.setPadding(new Insets(16, 24, 0, 24));
        methodRow.setAlignment(Pos.CENTER_LEFT);
        methodRow.setStyle("-fx-background-color:" + UiUtils.card() + ";");

        ToggleGroup tg = new ToggleGroup();
        for (BrewMethod m : METHODS) {
            ToggleButton tb = new ToggleButton(m.emoji() + " " + m.name());
            tb.setToggleGroup(tg);
            tb.setStyle(chipStyle(false));
            tb.selectedProperty().addListener((obs, o, n) -> {
                tb.setStyle(chipStyle(n));
                if (n) selectMethod(m);
            });
            if (m == current) tb.setSelected(true);
            methodRow.getChildren().add(tb);
        }

        // ── Timer ring ────────────────────────────────────────────────────────
        ringCanvas = new Canvas(220, 220);
        timeDisplay = new Label(formatTime(totalSeconds));
        timeDisplay.setStyle(
            "-fx-font-size:42px;-fx-font-weight:bold;-fx-text-fill:#A8621E;");
        phaseLabel = new Label(current.steps()[0].label());
        phaseLabel.setStyle("-fx-font-size:14px;-fx-text-fill:" + UiUtils.sub() + ";");
        phaseLabel.setWrapText(true);
        phaseLabel.setMaxWidth(200);

        StackPane ringStack = new StackPane(ringCanvas,
            new VBox(4, timeDisplay, phaseLabel) {{
                setAlignment(Pos.CENTER);
                ((VBox) this).setAlignment(Pos.CENTER);
            }});
        ringStack.setAlignment(Pos.CENTER);
        drawRing(0);

        // ── Controls ──────────────────────────────────────────────────────────
        startBtn = new Button("\u25B6 Start");
        startBtn.getStyleClass().add("btn-primary");
        startBtn.setStyle(startBtn.getStyle() + "-fx-font-size:16px;-fx-padding:12 36 12 36;");
        Button resetBtn = new Button("\u21BA Reset");
        resetBtn.getStyleClass().add("btn-secondary");

        startBtn.setOnAction(e -> toggleTimer());
        resetBtn.setOnAction(e -> resetTimer());

        HBox controls = new HBox(12, startBtn, resetBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(12, 0, 0, 0));

        // ── Params ────────────────────────────────────────────────────────────
        HBox params = new HBox(10);
        params.setAlignment(Pos.CENTER);
        params.setPadding(new Insets(8, 0, 0, 0));
        params.getChildren().addAll(
            paramChip("Ratio", current.ratio()),
            paramChip("Grind", current.grind()),
            paramChip("Temp", current.temp())
        );

        VBox timerCenter = new VBox(12, ringStack, controls, params);
        timerCenter.setAlignment(Pos.CENTER);
        timerCenter.setPadding(new Insets(24));
        timerCenter.setStyle("-fx-background-color:" + UiUtils.card() + ";");

        // ── Steps guide ───────────────────────────────────────────────────────
        stepsBox = new VBox(6);
        stepsBox.setPadding(new Insets(16));
        Label stepsTitle = new Label("STEPS");
        stepsTitle.getStyleClass().add("detail-heading");
        stepsTitle.setPadding(new Insets(0, 0, 8, 0));
        stepsBox.getChildren().add(stepsTitle);
        rebuildSteps(0);

        ScrollPane stepsScroll = new ScrollPane(stepsBox);
        stepsScroll.setFitToWidth(true);
        stepsScroll.getStyleClass().add("detail-scroll");
        stepsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox rightCol = new VBox(0);
        rightCol.setMinWidth(280);
        rightCol.setMaxWidth(320);
        rightCol.setStyle(
            "-fx-background-color:" + UiUtils.card() + ";" +
            "-fx-border-color:transparent transparent transparent " + UiUtils.border() + ";" +
            "-fx-border-width:0 0 0 1;");
        rightCol.getChildren().add(stepsScroll);

        HBox body = new HBox(0, timerCenter, rightCol);
        HBox.setHgrow(timerCenter, Priority.ALWAYS);

        VBox content = new VBox(0, methodRow, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(content);
        content.setStyle("-fx-background-color:" + UiUtils.bg() + ";");

        // Init clock
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        clock.setCycleCount(Timeline.INDEFINITE);
    }

    // ── Timer logic ───────────────────────────────────────────────────────────

    private void toggleTimer() {
        if (!running) {
            if (elapsed >= totalSeconds) return;
            running = true;
            clock.play();
            startBtn.setText("\u23F8 Pause");
        } else {
            running = false;
            clock.stop();
            startBtn.setText("\u25B6 Resume");
        }
    }

    private void resetTimer() {
        clock.stop();
        running = false;
        elapsed = 0;
        drawRing(0);
        timeDisplay.setText(formatTime(totalSeconds));
        startBtn.setText("\u25B6 Start");
        phaseLabel.setText(current.steps()[0].label());
        rebuildSteps(0);
    }

    private void tick() {
        elapsed++;
        drawRing((double) elapsed / totalSeconds);
        timeDisplay.setText(formatTime(totalSeconds - elapsed));
        updateCurrentStep();
        rebuildSteps(elapsed);
        if (elapsed >= totalSeconds) {
            clock.stop();
            running = false;
            startBtn.setText("\u2714 Done!");
            phaseLabel.setText("Brew complete — enjoy!");
        }
    }

    private void updateCurrentStep() {
        BrewStep current = null;
        for (BrewStep s : this.current.steps()) {
            if (elapsed >= s.atSecond()) current = s;
        }
        if (current != null) phaseLabel.setText(current.label());
    }

    private void rebuildSteps(int elapsed) {
        stepsBox.getChildren().clear();
        Label stepsTitle = new Label("STEPS");
        stepsTitle.getStyleClass().add("detail-heading");
        stepsTitle.setPadding(new Insets(0, 0, 8, 0));
        stepsBox.getChildren().add(stepsTitle);
        BrewStep[] steps = current.steps();
        for (int i = 0; i < steps.length; i++) {
            BrewStep s = steps[i];
            boolean done   = elapsed > s.atSecond() && i < steps.length - 1;
            boolean active = i == currentStepIndex(elapsed);
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            row.setStyle(
                "-fx-background-color:" + (active ? "#F6E8CC" : UiUtils.card()) + ";" +
                "-fx-background-radius:8;" +
                (active ? "-fx-border-color:#A8621E;-fx-border-radius:8;-fx-border-width:1.5;" : ""));
            Label num = new Label(done ? "\u2714" : String.valueOf(i + 1));
            num.setStyle(
                "-fx-min-width:24px;-fx-min-height:24px;-fx-max-width:24px;-fx-max-height:24px;" +
                "-fx-background-radius:12;-fx-alignment:center;-fx-font-size:11px;-fx-font-weight:800;" +
                "-fx-background-color:" + (done ? "#A8621E" : active ? "#2B1005" : UiUtils.btn()) + ";" +
                "-fx-text-fill:" + (done || active ? "white" : UiUtils.sub()) + ";");
            Label lbl = new Label(formatTime(s.atSecond()) + "  " + s.label());
            lbl.setWrapText(true);
            lbl.setStyle("-fx-font-size:13px;-fx-text-fill:" +
                (active ? "#2B1005" : done ? UiUtils.sub() : UiUtils.text()) + ";" +
                (active ? "-fx-font-weight:700;" : "") +
                (done ? "-fx-strikethrough:true;" : ""));
            HBox.setHgrow(lbl, Priority.ALWAYS);
            row.getChildren().addAll(num, lbl);
            stepsBox.getChildren().add(row);
        }
    }

    private int currentStepIndex(int elapsed) {
        int idx = 0;
        for (int i = 0; i < current.steps().length; i++) {
            if (elapsed >= current.steps()[i].atSecond()) idx = i;
        }
        return idx;
    }

    private void selectMethod(BrewMethod m) {
        current = m;
        totalSeconds = m.durationSeconds();
        resetTimer();
        // Update params (they're rebuilt on next reset already via rebuildSteps)
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private void drawRing(double progress) {
        GraphicsContext gc = ringCanvas.getGraphicsContext2D();
        double w = ringCanvas.getWidth(), h = ringCanvas.getHeight();
        gc.clearRect(0, 0, w, h);
        double cx = w / 2, cy = h / 2, r = 90, thick = 12;

        // Background ring
        gc.setStroke(Color.web(UiUtils.border()));
        gc.setLineWidth(thick);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, -360, javafx.scene.shape.ArcType.OPEN);

        // Progress ring
        if (progress > 0) {
            gc.setStroke(Color.web("#A8621E"));
            gc.setLineWidth(thick);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, -360 * progress, javafx.scene.shape.ArcType.OPEN);
        }

        // Pulsing dot at tip
        if (progress > 0 && progress < 1) {
            double angle = Math.toRadians(90 - 360 * progress);
            double dotX  = cx + r * Math.cos(angle);
            double dotY  = cy - r * Math.sin(angle);
            gc.setFill(Color.web("#A8621E"));
            gc.fillOval(dotX - 7, dotY - 7, 14, 14);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String formatTime(int seconds) {
        if (seconds >= 3600) {
            int h = seconds / 3600, m = (seconds % 3600) / 60;
            return h + "h " + String.format("%02dm", m);
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static String chipStyle(boolean active) {
        if (active) return
            "-fx-background-color:#2B1005;-fx-text-fill:#F5C060;" +
            "-fx-font-size:13px;-fx-font-weight:700;-fx-padding:6 16 6 16;" +
            "-fx-background-radius:20;-fx-border-width:0;-fx-cursor:hand;";
        return
            "-fx-background-color:" + UiUtils.btn() + ";-fx-text-fill:" + UiUtils.sub() + ";" +
            "-fx-font-size:13px;-fx-font-weight:600;-fx-padding:6 16 6 16;" +
            "-fx-background-radius:20;-fx-border-color:" + UiUtils.border() + ";" +
            "-fx-border-radius:20;-fx-border-width:1.5;-fx-cursor:hand;";
    }

    private static HBox paramChip(String key, String val) {
        Label k = new Label(key.toUpperCase());
        k.setStyle("-fx-font-size:10px;-fx-font-weight:800;-fx-text-fill:" + UiUtils.sub() + ";");
        Label v = new Label(val);
        v.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:" + UiUtils.text() + ";");
        VBox box = new VBox(2, k, v);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8, 16, 8, 16));
        box.setStyle(
            "-fx-background-color:" + UiUtils.cardAlt() + ";" +
            "-fx-background-radius:10;-fx-border-color:" + UiUtils.border() + ";" +
            "-fx-border-radius:10;-fx-border-width:1;");
        HBox wrap = new HBox(box);
        return wrap;
    }

    BorderPane getRoot() { return root; }

    /**
     * Loads a recipe into the timer.
     * Creates one step per instruction line, distributing time evenly.
     * Called when user clicks "Use in Timer" on a recipe card.
     */
    void loadRecipe(mn.edu.num.cafe.core.domain.Recipe recipe) {
        // Build steps from instructions
        String[] lines = (recipe.getInstructions() != null ? recipe.getInstructions() : "")
            .split("\n");
        List<String> stepTexts = new ArrayList<>();
        for (String l : lines) { if (!l.isBlank()) stepTexts.add(l.trim()); }
        if (stepTexts.isEmpty()) stepTexts.add("Brew for " + recipe.getBrewTime() + " minutes");

        int totalSecs = recipe.getBrewTime() * 60;
        int stepGap   = totalSecs / stepTexts.size();
        BrewStep[] steps = new BrewStep[stepTexts.size()];
        for (int i = 0; i < stepTexts.size(); i++) {
            steps[i] = new BrewStep(i * stepGap, stepTexts.get(i));
        }

        // Inject as a custom method
        current = new BrewMethod(
            recipe.getTitle(), "\uD83D\uDCD6", totalSecs,
            recipe.getIngredients() != null ? recipe.getIngredients().lines().findFirst().orElse("") : "",
            recipe.getDifficulty(), recipe.getDrinkType(),
            steps
        );
        totalSeconds = totalSecs;
        resetTimer();
        UiUtils.showToast("Loaded: " + recipe.getTitle());
    }
}
