package com.techway.coursemanagementdesktop.controller;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * صفحة "من نحن" – تخطيط مرتب + خطوط كبيرة + شبكة ثابتة
 */
public class AboutPageController {

    private static final double CONTENT_WIDTH = 1100;
    private static final double SECTION_SPACING = 28;
    private static final double GRID_GAP = 20;

    private final MainController mainController;

    public AboutPageController(MainController mainController) {
        this.mainController = mainController;
    }

    public VBox createAboutPage() {
        // جذر الصفحة
        VBox root = new VBox(SECTION_SPACING);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(36));
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.getStyleClass().add("about-root");

        // حمّل CSS الخاص بالصفحة
        try {
            root.getStylesheets().add(
                    getClass().getResource("/css/about-styles.css").toExternalForm()
            );
        } catch (Exception ignore) { /* لو الملف مو موجود، الصفحة تشتغل برضه */ }

        // حاوية المحتوى بعرض ثابت
        VBox content = new VBox(SECTION_SPACING);
        content.getStyleClass().add("about-container");
        content.setMaxWidth(CONTENT_WIDTH);

        // الأقسام
        content.getChildren().addAll(
                buildHero(),
                buildMissionVision(),
                buildStatsBand(),
                buildValues(),
                buildTeam(),
                buildWhy(),
                buildCta()
        );

        root.getChildren().setAll(content);
        return root;
    }

    // ===================== أقسام الصفحة =====================

    private Node buildHero() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);

        Label title = new Label("عن TechWay");
        title.getStyleClass().add("about-hero-title");

        Label sub = new Label("نحن منصة تعليمية متخصصة في تقديم أفضل الكورسات التقنية لتطوير المهارات وبناء مستقبل مهني متميز.");
        sub.getStyleClass().add("about-hero-subtitle");
        sub.setWrapText(true);
        sub.setMaxWidth(CONTENT_WIDTH - 200);

        box.getChildren().addAll(title, sub);
        return box;
    }

    private Node buildMissionVision() {
        GridPane grid = createGrid(2);
        grid.getChildren().addAll(
                gridItem(0, 0, createInfoCard("📘", "مهمتنا",
                        "تقديم تعليم تقني عالي الجودة من خلال كورسات متخصصة يقدمها خبراء، مع بيئة تفاعلية مناسبة لاحتياجات المتعلمين.")),
                gridItem(1, 0, createInfoCard("🎯", "رؤيتنا",
                        "أن نكون المنصة الرائدة في تعليم التقنية بالمنطقة، ونمكّن كل شخص من اكتساب المهارات المطلوبة لسوق العمل الحديث."))
        );
        return grid;
    }

    private Node buildStatsBand() {
        StackPane band = new StackPane();
        band.getStyleClass().add("stats-band");
        band.setPadding(new Insets(22));

        HBox row = new HBox(40);
        row.setAlignment(Pos.CENTER);

        row.getChildren().addAll(
                createKpi("500+", "طالب نشط"),
                createKpi("+50", "كورس متاح"),
                createKpi("+20", "مدرب خبير"),
                createKpi("95%", "معدل الرضا")
        );

        band.getChildren().add(row);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        band.setClip(clip);
        band.layoutBoundsProperty().addListener((obs, o, n) -> {
            clip.setWidth(n.getWidth());
            clip.setHeight(n.getHeight());
        });

        return band;
    }

    private Node buildValues() {
        VBox section = new VBox(16);
        Label title = new Label("قيمنا");
        title.getStyleClass().add("section-title");

        GridPane grid = createGrid(2);
        List<Node> cards = List.of(
                createValueCard("⚡", "التعلم المستمر", "نؤمن بأهمية التطوير المستمر ومواكبة التغيرات."),
                createValueCard("🛡️", "الجودة", "محتوى مُراجع من خبراء وبمعايير عالية."),
                createValueCard("🔍", "الشفافية", "وضوح في المحتوى والتقييم والسعر."),
                createValueCard("🤝", "الدعم", "مساندة حقيقية خلال الرحلة التعليمية.")
        );
        addToGrid(grid, cards, 2);

        section.getChildren().addAll(title, grid);
        return section;
    }

    private Node buildTeam() {
        VBox section = new VBox(16);
        Label title = new Label("فريقنا");
        title.getStyleClass().add("section-title");

        GridPane grid = createGrid(3);
        List<Node> cards = List.of(
                createTeamCard("أحمد محمد", "مؤسس ومدير تنفيذي", "خبرة 15 عام في تطوير البرمجيات والتعليم التقني."),
                createTeamCard("سارة أحمد", "مديرة المحتوى التعليمي", "متخصصة في تصميم المناهج والتقييم التعليمي."),
                createTeamCard("محمد العلي", "مدير التقنية", "خبير في تطوير المنصات التعليمية وتحليل البيانات.")
        );
        addToGrid(grid, cards, 3);

        section.getChildren().addAll(title, grid);
        return section;
    }

    private Node buildWhy() {
        VBox section = new VBox(16);
        Label title = new Label("لماذا TechWay");
        title.getStyleClass().add("section-title");

        GridPane grid = createGrid(2);
        List<Node> cards = List.of(
                createWhyCard("⚙️", "تعلم تفاعلي", "منصة تفاعلية مع تمارين ومشاريع تطبيقية."),
                createWhyCard("✅", "شهادات معتمدة", "احصل على شهادات بعد إكمال الكورسات."),
                createWhyCard("🛡️", "محتوى عالي الجودة", "محتوى مُراجع من قبل خبراء في المجال."),
                createWhyCard("🕒", "مرونة في التعلم", "تعلم في الوقت والوتيرة التي تناسبك.")
        );
        addToGrid(grid, cards, 2);

        section.getChildren().addAll(title, grid);
        return section;
    }

    private Node buildCta() {
        VBox box = new VBox(12);
        box.getStyleClass().add("cta-banner");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(22));

        Label title = new Label("انضم إلى رحلة التطوير معنا");
        title.getStyleClass().add("cta-title");

        Label sub = new Label("ابدأ رحلتك في تعلم التقنية مع خبراء المجال واكتسب المهارات التي تحتاجها لتحقيق أهدافك المهنية.");
        sub.getStyleClass().add("cta-subtitle");
        sub.setWrapText(true);
        sub.setMaxWidth(CONTENT_WIDTH - 240);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        Button explore = new Button("استكشف الكورسات");
        explore.getStyleClass().add("primary-button");
        explore.setOnAction(e -> mainController.navigateToCourses());

        Button contact = new Button("تواصل معنا");
        contact.getStyleClass().add("secondary-button");
        // استدعاء صفحة التواصل من الـ Main (دالة خاصة، نستدعي بالانعكاس)
        contact.setOnAction(e -> {
            try {
                mainController.getClass().getDeclaredMethod("loadContactPage").setAccessible(true);
                mainController.getClass().getDeclaredMethod("loadContactPage").invoke(mainController);
            } catch (Exception ignored) {}
        });

        actions.getChildren().addAll(explore, contact);

        box.getChildren().addAll(title, sub, actions);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        box.setClip(clip);
        box.layoutBoundsProperty().addListener((obs, o, n) -> {
            clip.setWidth(n.getWidth());
            clip.setHeight(n.getHeight());
        });

        return box;
    }

    // ===================== عناصر جاهزة =====================

    private VBox createInfoCard(String emoji, String title, String text) {
        VBox card = new VBox(10);
        card.getStyleClass().add("about-card");

        Label badge = new Label(emoji);
        badge.getStyleClass().add("badge-icon");

        Label t = new Label(title);
        t.getStyleClass().add("card-title");

        Label d = new Label(text);
        d.getStyleClass().add("card-text");
        d.setWrapText(true);

        card.getChildren().addAll(badge, t, d);
        return card;
    }

    private VBox createValueCard(String emoji, String title, String text) {
        return createInfoCard(emoji, title, text);
    }

    private VBox createTeamCard(String name, String role, String bio) {
        VBox card = new VBox(8);
        card.getStyleClass().add("about-card");

        Label n = new Label(name);
        n.getStyleClass().add("card-title");

        Label r = new Label(role);
        r.getStyleClass().add("card-subtitle");

        Label b = new Label(bio);
        b.getStyleClass().add("card-text");
        b.setWrapText(true);

        card.getChildren().addAll(n, r, b);
        return card;
    }

    private VBox createWhyCard(String emoji, String title, String text) {
        return createInfoCard(emoji, title, text);
    }

    private VBox createKpi(String number, String label) {
        VBox v = new VBox(2);
        v.setAlignment(Pos.CENTER);

        Label n = new Label(number);
        n.getStyleClass().add("kpi-number");

        Label l = new Label(label);
        l.getStyleClass().add("kpi-label");

        v.getChildren().addAll(n, l);
        return v;
    }

    // ===================== Grid helpers =====================

    private GridPane createGrid(int cols) {
        GridPane grid = new GridPane();
        grid.setHgap(GRID_GAP);
        grid.setVgap(GRID_GAP);

        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        return grid;
    }

    private Node gridItem(int col, int row, Node node) {
        GridPane.setColumnIndex(node, col);
        GridPane.setRowIndex(node, row);
        return node;
    }

    private void addToGrid(GridPane grid, List<Node> nodes, int cols) {
        int col = 0, row = 0;
        for (Node n : new ArrayList<>(nodes)) {
            grid.add(n, col, row);
            col++;
            if (col == cols) {
                col = 0;
                row++;
            }
        }
    }
}
