package mn.edu.num.cafe.ui.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.domain.BrewGuide;
import mn.edu.num.cafe.core.domain.LearnArticle;

/**
 * Learn pane — Brew Guides and Articles displayed in a master-detail layout.
 * Left: tabbed ListView of guides / articles.
 * Right: detail panel with full content.
 */
public class LearnPane {

    private final BorderPane root;
    private final BorgolService service;

    public LearnPane(BorgolService service) {
        this.service = service;
        root = new BorderPane();
        root.getStyleClass().add("content-pane");
        root.setCenter(buildContent());
    }

    private SplitPane buildContent() {
        // ── Left: tab between guides and articles ─────────────────────────────
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Detail pane (right side)
        ScrollPane detail = new ScrollPane();
        detail.setFitToWidth(true);
        detail.getStyleClass().add("detail-scroll");
        VBox detailBox = new VBox(12);
        detailBox.setPadding(new Insets(20));
        detail.setContent(detailBox);

        // ── Brew Guides tab ───────────────────────────────────────────────────
        ListView<BrewGuide> guideList = new ListView<>();
        try {
            guideList.getItems().setAll(service.getBrewGuides());
        } catch (Exception ignored) {}

        guideList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(BrewGuide g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) { setText(null); }
                else setText(g.getIcon() + "  " + g.getMethodName()
                    + "  ·  " + g.getDifficulty()
                    + "  ·  " + g.getBrewTimeMin() + " min");
            }
        });

        guideList.getSelectionModel().selectedItemProperty().addListener((o, old, g) -> {
            if (g == null) return;
            detailBox.getChildren().clear();
            detailBox.getChildren().addAll(
                bigTitle(g.getIcon() + " " + g.getMethodName()),
                chip(g.getDifficulty() + "  ·  " + g.getBrewTimeMin() + " min"),
                section("About", g.getDescription()),
                section("Parameters", g.getParameters()),
                section("Steps", formatSteps(g.getSteps()))
            );
        });

        Tab guideTab = new Tab("☕ Brew Guides", guideList);

        // ── Learn Articles tab ────────────────────────────────────────────────
        ListView<LearnArticle> articleList = new ListView<>();
        try {
            articleList.getItems().setAll(service.getLearnArticles());
        } catch (Exception ignored) {}

        articleList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(LearnArticle a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setText(null); }
                else setText(a.getIcon() + "  " + a.getTitle()
                    + "  ·  " + a.getReadTimeMin() + " min read");
            }
        });

        articleList.getSelectionModel().selectedItemProperty().addListener((o, old, a) -> {
            if (a == null) return;
            detailBox.getChildren().clear();
            detailBox.getChildren().addAll(
                bigTitle(a.getIcon() + " " + a.getTitle()),
                chip(a.getCategory() + "  ·  " + a.getReadTimeMin() + " min read"),
                section("Content", a.getContent())
            );
        });

        Tab articleTab = new Tab("📖 Articles", articleList);
        tabs.getTabs().addAll(guideTab, articleTab);

        // Auto-select first guide
        if (!guideList.getItems().isEmpty())
            guideList.getSelectionModel().select(0);

        SplitPane split = new SplitPane(tabs, detail);
        split.setDividerPositions(0.35);
        return split;
    }

    // ── Detail UI helpers ─────────────────────────────────────────────────────

    private static Label bigTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("detail-title");
        l.setWrapText(true);
        return l;
    }

    private static Label chip(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("detail-chip");
        return l;
    }

    private static VBox section(String heading, String body) {
        if (body == null || body.isBlank()) return new VBox();
        Label h = new Label(heading);
        h.getStyleClass().add("detail-heading");
        Label b = new Label(body);
        b.getStyleClass().add("detail-body");
        b.setWrapText(true);
        VBox box = new VBox(4, h, b);
        box.getStyleClass().add("detail-section");
        return box;
    }

    /** Converts newline-separated steps into a numbered string for display. */
    private static String formatSteps(String steps) {
        if (steps == null || steps.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (String line : steps.split("\\n")) {
            String t = line.trim();
            if (!t.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(n++).append(". ").append(t);
            }
        }
        return sb.toString();
    }

    public BorderPane getRoot() { return root; }
}
