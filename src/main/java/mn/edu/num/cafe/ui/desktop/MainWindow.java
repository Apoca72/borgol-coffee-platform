package mn.edu.num.cafe.ui.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import mn.edu.num.cafe.core.application.BorgolService;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Main application window — Facebook-style left sidebar + content area.
 * Navigation: Recipes | Cafes | Journal | Learn | Feed | People | Profile
 */
public class MainWindow {

    private final BorderPane root;
    private final StackPane  center;
    private final Map<String, Node> panes = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final BorgolService service;
    private final Stage stage;
    private VBox sidebar;
    private boolean darkMode = false;

    public MainWindow(BorgolService service, Stage stage) {
        this.service = service;
        this.stage   = stage;

        center = new StackPane();
        center.setStyle("-fx-background-color:#F0F2F5;");

        buildPanes();

        root = new BorderPane();
        sidebar = buildSidebar();
        root.setLeft(sidebar);
        root.setCenter(center);

        UiUtils.setToastRoot(center);
        showPane("Recipes");
    }

    // ── Pane construction ─────────────────────────────────────────────────────

    private void buildPanes() {
        panes.clear();
        navButtons.clear();
        center.getChildren().clear();

        RecipesPane  rp = new RecipesPane(service);
        CafesPane    cp = new CafesPane(service);
        JournalPane  jp = new JournalPane(service);
        LearnPane    lp = new LearnPane(service);
        FeedPane     fp = new FeedPane(service);
        PeoplePane   pp = new PeoplePane(service);
        ProfilePane  pr = new ProfilePane(service, this::refreshSidebarUser);

        panes.put("Recipes", rp.getRoot());
        panes.put("Cafes",   cp.getRoot());
        panes.put("Journal", jp.getRoot());
        panes.put("Learn",   lp.getRoot());
        panes.put("Feed",    fp.getRoot());
        panes.put("People",  pp.getRoot());
        panes.put("Profile", pr.getRoot());

        center.getChildren().addAll(panes.values());
        panes.values().forEach(p -> p.setVisible(false));
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sb = new VBox();
        sb.getStyleClass().add("sidebar");

        // Brand
        Label brand = new Label("\uD83E\uDEB7 Borgol");
        brand.getStyleClass().add("sidebar-brand");
        brand.setOnMouseClicked(e -> showPane("Recipes"));

        // Nav items
        VBox navBox = new VBox(2);
        navBox.setPadding(new Insets(4, 12, 4, 12));
        for (String name : panes.keySet()) {
            Button btn = new Button("  " + name);
            btn.setGraphic(navIcon(name));
            btn.setGraphicTextGap(6);
            btn.getStyleClass().add("nav-item");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> showPane(name));
            navBox.getChildren().add(btn);
            navButtons.put(name, btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox userSection = buildUserSection();

        sb.getChildren().addAll(brand, navBox, spacer, userSection);
        return sb;
    }

    private VBox buildUserSection() {
        VBox box = new VBox(8);
        box.getStyleClass().add("sidebar-user-section");

        // Dark mode toggle (always visible)
        Button darkBtn = new Button(darkMode ? "\u2600\uFE0F Light" : "\uD83C\uDF19 Dark");
        darkBtn.getStyleClass().add("btn-secondary");
        darkBtn.setMaxWidth(Double.MAX_VALUE);
        darkBtn.setOnAction(e -> {
            toggleDarkMode();
            darkBtn.setText(darkMode ? "\u2600\uFE0F Light" : "\uD83C\uDF19 Dark");
        });
        box.getChildren().add(darkBtn);

        if (AppSession.loggedIn()) {
            Label avatar = UiUtils.createAvatar(AppSession.username(), 52);

            Label username = new Label("@" + AppSession.username());
            username.getStyleClass().add("sidebar-username");

            // Stats row: recipes · followers
            Label statsRow = new Label("\u2014");
            statsRow.setStyle("-fx-font-size:11px;-fx-text-fill:#65676B;");
            try {
                var profile = service.getUserProfile(AppSession.userId(), AppSession.userId());
                statsRow.setText(
                    profile.recipeCount() + " recipes  \u00B7  " +
                    profile.followerCount() + " followers");
            } catch (Exception ignored) {}

            VBox nameBox = new VBox(3, username, statsRow);

            HBox userRow = new HBox(10, avatar, nameBox);
            userRow.setAlignment(Pos.CENTER_LEFT);

            Button logout = new Button("Log Out");
            logout.getStyleClass().add("btn-secondary");
            logout.setMaxWidth(Double.MAX_VALUE);
            logout.setOnAction(e -> { AppSession.logout(); refreshAll(); });

            box.getChildren().addAll(userRow, logout);
        } else {
            Button login    = new Button("Log In");
            Button register = new Button("Sign Up");
            login.getStyleClass().add("btn-primary");
            register.getStyleClass().add("btn-secondary");
            login.setMaxWidth(Double.MAX_VALUE);
            register.setMaxWidth(Double.MAX_VALUE);
            login.setOnAction(e    -> showLoginDialog());
            register.setOnAction(e -> showRegisterDialog());
            box.getChildren().addAll(login, register);
        }

        return box;
    }

    private void refreshAll() {
        buildPanes();
        VBox navBox = (VBox) sidebar.getChildren().get(1);
        navBox.getChildren().clear();
        for (String name : panes.keySet()) {
            Button btn = new Button("  " + name);
            btn.setGraphic(navIcon(name));
            btn.setGraphicTextGap(6);
            btn.getStyleClass().add("nav-item");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> showPane(name));
            navBox.getChildren().add(btn);
            navButtons.put(name, btn);
        }
        sidebar.getChildren().set(sidebar.getChildren().size() - 1, buildUserSection());
        showPane("Recipes");
    }

    /** Rebuilds only the user section (after profile edit) — cheaper than refreshAll(). */
    private void refreshSidebarUser() {
        sidebar.getChildren().set(sidebar.getChildren().size() - 1, buildUserSection());
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        var sheets = stage.getScene().getStylesheets();
        String darkCss;
        try {
            darkCss = Objects.requireNonNull(
                getClass().getResource("/style-dark.css")).toExternalForm();
        } catch (Exception e) { return; }
        if (darkMode) sheets.add(darkCss); else sheets.remove(darkCss);
    }

    private void showPane(String name) {
        panes.values().forEach(p -> p.setVisible(false));
        Node target = panes.get(name);
        if (target != null) target.setVisible(true);
        navButtons.forEach((k, b) -> b.getStyleClass().remove("nav-item-active"));
        Button active = navButtons.get(name);
        if (active != null) active.getStyleClass().add("nav-item-active");
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

    private static FontIcon navIcon(String pane) {
        String code = switch (pane) {
            case "Recipes"  -> "fas-book-open";
            case "Cafes"    -> "fas-coffee";
            case "Journal"  -> "fas-book";
            case "Learn"    -> "fas-graduation-cap";
            case "Feed"     -> "fas-newspaper";
            case "People"   -> "fas-users";
            case "Profile"  -> "fas-user-circle";
            default         -> "fas-circle";
        };
        FontIcon icon = new FontIcon(code);
        icon.setIconSize(16);
        return icon;
    }

    /** Returns a deterministic color for a given username. */
    static String avatarColor(String username) {
        if (username == null || username.isEmpty()) return "#65676B";
        String[] palette = {"#E17055","#6C5CE7","#00B894","#0984E3","#FDCB6E","#00CEC9","#A29BFE","#D4621A"};
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
