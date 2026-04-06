package mn.edu.num.cafe.ui.desktop;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import mn.edu.num.cafe.core.application.BorgolService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Main application window — top dark espresso navbar (matches web design).
 * Navigation: Feed | Recipes | Cafes | Explore | Journal | Learn | Timer
 */
public class MainWindow {

    private final BorderPane root;
    private final StackPane  center;
    private final Map<String, Node>   panes      = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final BorgolService service;
    private final Stage stage;
    private HBox navbar;
    private boolean darkMode = false;

    // Bean AI chat panel state
    private VBox   chatPanel;
    private boolean chatOpen = false;

    public MainWindow(BorgolService service, Stage stage) {
        this.service = service;
        this.stage   = stage;

        center = new StackPane();
        center.setStyle("-fx-background-color:" + UiUtils.bg() + ";");

        buildPanes();

        navbar = buildNavbar();
        root   = new BorderPane();
        root.setTop(navbar);
        root.setCenter(center);

        UiUtils.setToastRoot(center);
        showPane("Feed");
    }

    // ── Pane construction ─────────────────────────────────────────────────────

    private void buildPanes() {
        panes.clear();
        navButtons.clear();
        center.getChildren().clear();

        RecipesPane    rp = new RecipesPane(service);
        CafesPane      cp = new CafesPane(service);
        JournalPane    jp = new JournalPane(service);
        LearnPane      lp = new LearnPane(service);
        FeedPane       fp = new FeedPane(service);
        PeoplePane     pp = new PeoplePane(service);
        ProfilePane    pr = new ProfilePane(service, this::refreshNavUser);
        BrewTimerPane  tp = new BrewTimerPane();

        // Wire recipe → timer: clicking "Use in Timer" opens the Timer pane
        // loaded with that recipe's steps
        rp.setOnUseInTimer(recipe -> {
            tp.loadRecipe(recipe);
            showPane("Timer");
        });

        panes.put("Feed",    fp.getRoot());
        panes.put("Recipes", rp.getRoot());
        panes.put("Cafes",   cp.getRoot());
        panes.put("Explore", pp.getRoot());
        panes.put("Journal", jp.getRoot());
        panes.put("Learn",   lp.getRoot());
        panes.put("Timer",   tp.getRoot());
        panes.put("Profile", pr.getRoot());

        center.getChildren().addAll(panes.values());
        panes.values().forEach(p -> p.setVisible(false));

        // Bean AI chat panel overlay
        chatPanel = buildChatPanel();
        chatPanel.setTranslateX(360);
        chatPanel.setVisible(false);
        center.getChildren().add(chatPanel);
        StackPane.setAlignment(chatPanel, Pos.CENTER_RIGHT);
    }

    // ── Top Navbar ────────────────────────────────────────────────────────────

    private HBox buildNavbar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 20, 0, 20));
        bar.setMinHeight(62); bar.setMaxHeight(62);
        bar.setStyle(
            "-fx-background-color:linear-gradient(to right,#0C0400,#1A0800);" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.55),24,0,0,2);");

        // Brand
        Label brand = new Label("\uD83E\uDEB7 Borgol");
        brand.setStyle(
            "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#F5C060;" +
            "-fx-cursor:hand;-fx-padding:0 20 0 4;");
        brand.setOnMouseClicked(e -> showPane("Feed"));

        // Nav link buttons
        HBox navLinks = new HBox(2);
        navLinks.setAlignment(Pos.CENTER_LEFT);
        String[] pages = {"Feed", "Recipes", "Cafes", "Explore", "Journal", "Learn", "Timer"};
        for (String name : pages) {
            Button btn = navLinkBtn(name);
            btn.setOnAction(e -> showPane(name));
            navLinks.getChildren().add(btn);
            navButtons.put(name, btn);
        }

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search
        TextField search = new TextField();
        search.setPromptText("Search…");
        search.setStyle(
            "-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:white;" +
            "-fx-prompt-text-fill:rgba(255,255,255,0.45);-fx-border-color:rgba(255,255,255,0.18);" +
            "-fx-border-radius:20;-fx-background-radius:20;" +
            "-fx-padding:6 12 6 12;-fx-font-size:13px;-fx-pref-width:160px;");
        search.focusedProperty().addListener((obs, o, n) -> {
            if (n) search.setStyle(
                "-fx-background-color:rgba(255,255,255,0.16);-fx-text-fill:white;" +
                "-fx-prompt-text-fill:rgba(255,255,255,0.45);-fx-border-color:#CB8840;" +
                "-fx-border-radius:20;-fx-background-radius:20;" +
                "-fx-padding:6 12 6 12;-fx-font-size:13px;-fx-pref-width:200px;");
            else search.setStyle(
                "-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:white;" +
                "-fx-prompt-text-fill:rgba(255,255,255,0.45);-fx-border-color:rgba(255,255,255,0.18);" +
                "-fx-border-radius:20;-fx-background-radius:20;" +
                "-fx-padding:6 12 6 12;-fx-font-size:13px;-fx-pref-width:160px;");
        });

        // Bean AI button
        Button beanBtn = new Button("\uD83E\uDEB5 Bean");
        beanBtn.setStyle(
            "-fx-background-color:rgba(232,160,48,0.18);-fx-text-fill:#F5C060;" +
            "-fx-border-color:rgba(232,160,48,0.4);-fx-border-radius:20;" +
            "-fx-background-radius:20;-fx-padding:6 14 6 14;" +
            "-fx-font-size:13px;-fx-font-weight:700;-fx-cursor:hand;");
        beanBtn.setOnAction(e -> toggleChat());
        beanBtn.setOnMouseEntered(e -> beanBtn.setStyle(
            "-fx-background-color:rgba(232,160,48,0.28);-fx-text-fill:#F5C060;" +
            "-fx-border-color:rgba(232,160,48,0.6);-fx-border-radius:20;" +
            "-fx-background-radius:20;-fx-padding:6 14 6 14;" +
            "-fx-font-size:13px;-fx-font-weight:700;-fx-cursor:hand;"));
        beanBtn.setOnMouseExited(e -> beanBtn.setStyle(
            "-fx-background-color:rgba(232,160,48,0.18);-fx-text-fill:#F5C060;" +
            "-fx-border-color:rgba(232,160,48,0.4);-fx-border-radius:20;" +
            "-fx-background-radius:20;-fx-padding:6 14 6 14;" +
            "-fx-font-size:13px;-fx-font-weight:700;-fx-cursor:hand;"));

        // Dark mode toggle
        Button darkBtn = darkBtn();

        // Right section: auth or user pill
        HBox rightSection = buildNavRight(darkBtn, beanBtn);

        bar.getChildren().addAll(brand, navLinks, spacer, search, rightSection);
        return bar;
    }

    private Button navLinkBtn(String label) {
        Button b = new Button(navEmoji(label) + " " + label);
        b.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.7);" +
            "-fx-font-size:13.5px;-fx-font-weight:600;-fx-padding:8 14 8 14;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        b.setOnMouseEntered(e -> {
            if (!b.getStyleClass().contains("nav-active"))
                b.setStyle(b.getStyle().replace("rgba(255,255,255,0.7)", "white")
                             .replace("transparent", "rgba(255,255,255,0.1)"));
        });
        b.setOnMouseExited(e -> {
            if (!b.getStyleClass().contains("nav-active"))
                b.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.7);" +
                    "-fx-font-size:13.5px;-fx-font-weight:600;-fx-padding:8 14 8 14;" +
                    "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
        });
        return b;
    }

    private static String navEmoji(String page) {
        return switch (page) {
            case "Feed"    -> "\uD83C\uDFE0";
            case "Recipes" -> "\uD83D\uDCDA";
            case "Cafes"   -> "\u2615";
            case "Explore" -> "\uD83D\uDD2D";
            case "Journal" -> "\uD83D\uDCD3";
            case "Learn"   -> "\uD83C\uDF93";
            case "Timer"   -> "\u23F1\uFE0F";
            case "Profile" -> "\uD83D\uDC64";
            default        -> "";
        };
    }

    private HBox buildNavRight(Button darkBtn, Button beanBtn) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.getChildren().addAll(beanBtn, darkBtn);

        if (AppSession.loggedIn()) {
            // User pill
            javafx.scene.Node av = UiUtils.createAvatar(AppSession.username(), 28);
            Label name = new Label(AppSession.username());
            name.setStyle("-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:600;");
            HBox pill = new HBox(8, av, name);
            pill.setAlignment(Pos.CENTER);
            pill.setPadding(new Insets(4, 12, 4, 6));
            pill.setStyle(
                "-fx-background-color:rgba(255,255,255,0.1);-fx-background-radius:20;" +
                "-fx-cursor:hand;");
            pill.setOnMouseClicked(e -> showPane("Profile"));
            pill.setOnMouseEntered(e -> pill.setStyle(
                "-fx-background-color:rgba(255,255,255,0.18);-fx-background-radius:20;-fx-cursor:hand;"));
            pill.setOnMouseExited(e -> pill.setStyle(
                "-fx-background-color:rgba(255,255,255,0.1);-fx-background-radius:20;-fx-cursor:hand;"));

            Button logout = new Button("Log Out");
            logout.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.6);" +
                "-fx-font-size:12px;-fx-padding:6 10 6 10;-fx-background-radius:6;" +
                "-fx-border-width:0;-fx-cursor:hand;");
            logout.setOnMouseEntered(e -> logout.setStyle(
                "-fx-background-color:rgba(255,255,255,0.08);-fx-text-fill:rgba(255,255,255,0.9);" +
                "-fx-font-size:12px;-fx-padding:6 10 6 10;-fx-background-radius:6;" +
                "-fx-border-width:0;-fx-cursor:hand;"));
            logout.setOnMouseExited(e -> logout.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.6);" +
                "-fx-font-size:12px;-fx-padding:6 10 6 10;-fx-background-radius:6;" +
                "-fx-border-width:0;-fx-cursor:hand;"));
            logout.setOnAction(e -> { AppSession.logout(); refreshAll(); });

            box.getChildren().addAll(pill, logout);
        } else {
            Button login = new Button("Log In");
            Button signup = new Button("Sign Up");
            login.setStyle(
                "-fx-background-color:linear-gradient(135deg,#A8621E,#CB8840);" +
                "-fx-text-fill:white;-fx-font-weight:700;-fx-font-size:13px;" +
                "-fx-padding:7 18 7 18;-fx-background-radius:20;-fx-border-width:0;" +
                "-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(168,98,30,0.3),8,0,0,0);");
            signup.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.85);" +
                "-fx-font-weight:600;-fx-font-size:13px;-fx-padding:6 16 6 16;" +
                "-fx-background-radius:20;-fx-border-color:rgba(255,255,255,0.4);" +
                "-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
            login.setOnAction(e  -> showLoginDialog());
            signup.setOnAction(e -> showRegisterDialog());
            box.getChildren().addAll(login, signup);
        }
        return box;
    }

    private Button darkBtn() {
        Button b = new Button(darkMode ? "\u2600\uFE0F" : "\uD83C\uDF19");
        b.setStyle(
            "-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:white;" +
            "-fx-font-size:16px;-fx-padding:5 10 5 10;-fx-background-radius:8;" +
            "-fx-border-width:0;-fx-cursor:hand;");
        b.setOnAction(e -> {
            toggleDarkMode();
            b.setText(darkMode ? "\u2600\uFE0F" : "\uD83C\uDF19");
        });
        return b;
    }

    // ── Bean AI Chat Panel ────────────────────────────────────────────────────

    private VBox buildChatPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(340);
        panel.setMaxHeight(520);
        panel.setStyle(
            "-fx-background-color:" + UiUtils.card() + ";" +
            "-fx-background-radius:16 0 0 16;" +
            "-fx-effect:dropshadow(gaussian,rgba(12,4,0,0.25),24,0,0,0);" +
            "-fx-border-color:" + UiUtils.border() + " transparent " +
            UiUtils.border() + " " + UiUtils.border() + ";" +
            "-fx-border-width:1 0 1 1;-fx-border-radius:16 0 0 16;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color:#0C0400;-fx-background-radius:16 0 0 0;");
        Label beanTitle = new Label("\uD83E\uDEB5 Bean AI");
        beanTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#F5C060;");
        Label subLbl = new Label("Coffee assistant");
        subLbl.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.5);");
        VBox titleBox = new VBox(1, beanTitle, subLbl);
        Region headerSpacer = new Region(); HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button closeBtn = new Button("\u2715");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.1);-fx-text-fill:white;" +
            "-fx-background-radius:6;-fx-border-width:0;-fx-cursor:hand;-fx-font-size:13px;-fx-padding:3 7 3 7;");
        closeBtn.setOnAction(e -> toggleChat());
        header.getChildren().addAll(titleBox, headerSpacer, closeBtn);

        // Messages area
        VBox messages = new VBox(10);
        messages.setPadding(new Insets(14, 14, 10, 14));

        // Welcome message
        Label welcome = new Label("Hi! I'm Bean \uD83C\uDF31 Ask me anything about coffee — brewing methods, ratios, roasts, or cafe recommendations!");
        welcome.setWrapText(true);
        welcome.setStyle(
            "-fx-font-size:13px;-fx-text-fill:" + UiUtils.text() + ";" +
            "-fx-background-color:" + UiUtils.cardAlt() + ";" +
            "-fx-background-radius:12 12 12 4;-fx-padding:10 12 10 12;");
        messages.getChildren().add(welcome);

        ScrollPane scrollPane = new ScrollPane(messages);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background:" + UiUtils.card() + ";-fx-background-color:" + UiUtils.card() + ";");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Input area
        HBox inputRow = new HBox(8);
        inputRow.setPadding(new Insets(10, 12, 12, 12));
        inputRow.setAlignment(Pos.CENTER);
        inputRow.setStyle("-fx-border-color:" + UiUtils.border() + " transparent transparent transparent;-fx-border-width:1 0 0 0;");
        TextField inputField = new TextField();
        inputField.setPromptText("Ask Bean…");
        inputField.getStyleClass().add("form-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().add("btn-primary");

        Runnable sendMessage = () -> {
            String q = inputField.getText().trim();
            if (q.isEmpty()) return;
            inputField.clear();

            // User bubble
            Label userMsg = new Label(q);
            userMsg.setWrapText(true);
            userMsg.setStyle(
                "-fx-font-size:13px;-fx-text-fill:white;" +
                "-fx-background-color:#A8621E;" +
                "-fx-background-radius:12 12 4 12;-fx-padding:10 12 10 12;");
            HBox userRow = new HBox(userMsg);
            userRow.setAlignment(Pos.CENTER_RIGHT);
            messages.getChildren().add(userRow);

            // Loading bubble
            Label loadingMsg = new Label("\u22EF thinking…");
            loadingMsg.setStyle(
                "-fx-font-size:13px;-fx-text-fill:" + UiUtils.sub() + ";" +
                "-fx-background-color:" + UiUtils.cardAlt() + ";" +
                "-fx-background-radius:12 12 12 4;-fx-padding:10 12 10 12;");
            messages.getChildren().add(loadingMsg);

            // Scroll to bottom
            scrollPane.setVvalue(1.0);

            // Call API in background thread
            Thread.ofVirtual().start(() -> {
                String answer = callBeanApi(q);
                javafx.application.Platform.runLater(() -> {
                    messages.getChildren().remove(loadingMsg);
                    Label beanReply = new Label(answer);
                    beanReply.setWrapText(true);
                    beanReply.setStyle(
                        "-fx-font-size:13px;-fx-text-fill:" + UiUtils.text() + ";" +
                        "-fx-background-color:" + UiUtils.cardAlt() + ";" +
                        "-fx-background-radius:12 12 12 4;-fx-padding:10 12 10 12;");
                    messages.getChildren().add(beanReply);
                    scrollPane.setVvalue(1.0);
                });
            });
        };

        sendBtn.setOnAction(e -> sendMessage.run());
        inputField.setOnAction(e -> sendMessage.run());

        inputRow.getChildren().addAll(inputField, sendBtn);
        panel.getChildren().addAll(header, scrollPane, inputRow);
        return panel;
    }

    private void toggleChat() {
        chatOpen = !chatOpen;
        chatPanel.setVisible(true);
        TranslateTransition tt = new TranslateTransition(Duration.millis(240), chatPanel);
        tt.setToX(chatOpen ? 0 : 360);
        tt.setOnFinished(e -> { if (!chatOpen) chatPanel.setVisible(false); });
        tt.play();
    }

    private String callBeanApi(String question) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return "Bean AI is not active. Please set the GEMINI_API_KEY environment variable.";
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            String safe = question.replace("\\", "\\\\").replace("\"", "\\\"")
                                  .replace("\n", "\\n").replace("\r", "");
            String body = "{\"contents\":[{\"parts\":[{\"text\":\"You are Bean, a friendly and knowledgeable coffee expert assistant. Be concise and helpful. User question: " + safe + "\"}]}]}";
            var req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();
            var resp = java.net.http.HttpClient.newHttpClient()
                .send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            return parseGeminiText(resp.body());
        } catch (java.net.http.HttpTimeoutException e) {
            return "Request timed out. Check your internet connection.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /** Extracts the text field from a Gemini JSON response safely. */
    private static String parseGeminiText(String json) {
        if (json == null || json.isBlank()) return "No response received.";
        // Error response?
        if (json.contains("\"error\"")) {
            int mi = json.indexOf("\"message\":");
            if (mi >= 0) {
                String sub = json.substring(mi + 10).stripLeading();
                if (sub.startsWith("\"")) {
                    sub = sub.substring(1);
                    int end = sub.indexOf('"');
                    if (end > 0) return "API Error: " + sub.substring(0, end);
                }
            }
            return "API error — check your GEMINI_API_KEY.";
        }
        int ti = json.indexOf("\"text\":");
        if (ti < 0) return "No response received.";
        String after = json.substring(ti + 7).stripLeading();
        if (!after.startsWith("\"")) return "Parse error.";
        after = after.substring(1); // skip opening quote
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < after.length()) {
            char c = after.charAt(i);
            if (c == '\\' && i + 1 < after.length()) {
                char nx = after.charAt(i + 1);
                switch (nx) {
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'r'  -> { /* skip CR */ }
                    default   -> sb.append(nx);
                }
                i += 2;
            } else if (c == '"') {
                break; // end of string
            } else {
                sb.append(c);
                i++;
            }
        }
        String result = sb.toString().strip();
        return result.isEmpty() ? "No response received." : result;
    }

    // ── Refresh / Reload ──────────────────────────────────────────────────────

    private void refreshAll() {
        buildPanes();
        HBox rightSection = buildNavRight(darkBtn(), findBeanBtn());
        // Rebuild navbar completely
        navbar = buildNavbar();
        root.setTop(navbar);
        center.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        showPane("Feed");
    }

    private Button findBeanBtn() {
        Button b = new Button("\uD83E\uDEB5 Bean");
        b.setStyle(
            "-fx-background-color:rgba(232,160,48,0.18);-fx-text-fill:#F5C060;" +
            "-fx-border-color:rgba(232,160,48,0.4);-fx-border-radius:20;" +
            "-fx-background-radius:20;-fx-padding:6 14 6 14;" +
            "-fx-font-size:13px;-fx-font-weight:700;-fx-cursor:hand;");
        b.setOnAction(e -> toggleChat());
        return b;
    }

    private void refreshNavUser() {
        navbar = buildNavbar();
        root.setTop(navbar);
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        UiUtils.dark = darkMode;

        var sheets = stage.getScene().getStylesheets();
        String darkCss;
        try {
            darkCss = Objects.requireNonNull(
                getClass().getResource("/style-dark.css")).toExternalForm();
        } catch (Exception e) { return; }
        if (darkMode) sheets.add(darkCss); else sheets.remove(darkCss);

        buildPanes();
        navbar = buildNavbar();
        root.setTop(navbar);
        center.setStyle("-fx-background-color:" + UiUtils.bg() + ";");
        showPane("Feed");
    }

    private void showPane(String name) {
        panes.values().forEach(p -> p.setVisible(false));
        Node target = panes.get(name);
        if (target != null) target.setVisible(true);

        // Update active nav button styles
        navButtons.forEach((k, b) -> b.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.7);" +
            "-fx-font-size:13.5px;-fx-font-weight:600;-fx-padding:8 14 8 14;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;"));
        Button active = navButtons.get(name);
        if (active != null) active.setStyle(
            "-fx-background-color:rgba(232,160,48,0.18);-fx-text-fill:#F5C060;" +
            "-fx-font-size:13.5px;-fx-font-weight:700;-fx-padding:8 14 8 14;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-cursor:hand;");
    }

    // ── Login / Register dialogs ──────────────────────────────────────────────

    private void showLoginDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Log In to Borgol");
        dlg.setHeaderText(null);
        ButtonType loginBtn = new ButtonType("Log In", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(loginBtn, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().add(
            getClass().getResource("/style.css").toExternalForm());

        GridPane grid = formGrid();
        TextField emailFld    = styledField("email@borgol.mn");
        PasswordField passFld = new PasswordField();
        passFld.setPromptText("Password");
        passFld.getStyleClass().add("form-field");
        grid.add(lbl("Email"),    0, 0); grid.add(emailFld, 1, 0);
        grid.add(lbl("Password"), 0, 1); grid.add(passFld,  1, 1);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == loginBtn) {
                try {
                    var result = service.login(emailFld.getText().trim(), passFld.getText());
                    AppSession.login(result.user().id(), result.user().username());
                    refreshAll();
                } catch (Exception ex) { alert("Login Failed", ex.getMessage()); }
            }
        });
    }

    private void showRegisterDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Create Account");
        dlg.setHeaderText(null);
        ButtonType regBtn = new ButtonType("Sign Up", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(regBtn, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().add(
            getClass().getResource("/style.css").toExternalForm());

        GridPane grid = formGrid();
        TextField userFld  = styledField("username");
        TextField emailFld = styledField("email@borgol.mn");
        PasswordField passFld = new PasswordField();
        passFld.setPromptText("Password (min 6 chars)");
        passFld.getStyleClass().add("form-field");
        grid.add(lbl("Username"), 0, 0); grid.add(userFld,  1, 0);
        grid.add(lbl("Email"),    0, 1); grid.add(emailFld, 1, 1);
        grid.add(lbl("Password"), 0, 2); grid.add(passFld,  1, 2);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == regBtn) {
                try {
                    var result = service.register(
                        userFld.getText().trim(),
                        emailFld.getText().trim(),
                        passFld.getText());
                    AppSession.login(result.user().id(), result.user().username());
                    refreshAll();
                } catch (Exception ex) { alert("Registration Failed", ex.getMessage()); }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public BorderPane getRoot() { return root; }

    /** Returns a deterministic color for a given username. */
    static String avatarColor(String username) {
        if (username == null || username.isEmpty()) return "#8A7054";
        String[] palette = {"#A8621E","#6C5CE7","#00B894","#0984E3","#CB8840","#00CEC9","#A29BFE","#E8A030"};
        return palette[Math.abs(username.hashCode()) % palette.length];
    }

    private static Label lbl(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("form-label");
        return l;
    }

    static GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(12);
        g.setPadding(new Insets(16));
        g.getStyleClass().add("form-grid");
        ColumnConstraints c0 = new ColumnConstraints(100);
        ColumnConstraints c1 = new ColumnConstraints(260);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }

    static TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("form-field");
        return f;
    }

    static TextArea styledArea(String prompt, int rows) {
        TextArea a = new TextArea();
        a.setPromptText(prompt);
        a.setPrefRowCount(rows);
        a.setWrapText(true);
        a.getStyleClass().add("form-field");
        return a;
    }

    static void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    static void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
