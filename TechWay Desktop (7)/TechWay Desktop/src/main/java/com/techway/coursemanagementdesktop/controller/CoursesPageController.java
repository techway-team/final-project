package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import okhttp3.HttpUrl;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Courses Page – مصفوفة 3 كروت بالصف + بحث كبير + بانل تصفية أنيقة + ترتيب + ترقيم صفحات
 */
public class CoursesPageController {

    private static final String API_BASE = "http://localhost:8080";
    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

    // كاش بسيط للصور
    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    private final MainController mainController;
    private final ApiService apiService;
    private final SessionManager sessionManager;

    // === مفضلة: نحفظ الـ IDs محلياً لتحديث القلوب بسرعة ===
    private final Set<Long> favoriteIds = ConcurrentHashMap.newKeySet();

    // UI
    private VBox root;
    private Label statusLabel;
    private TextField searchField;
    private Button filterToggleBtn;
    private ComboBox<String> sortCombo;

    private VBox filtersPanel;                // بانل التصفية (يظهر/يختفي)
    private ToggleGroup priceGroup;           // كل / مجاني / مدفوع
    private RadioButton priceAll, priceFree, pricePaid;
    private ComboBox<String> locationCombo;   // المواقع

    private GridPane cardsGrid;               // شبكة 3×N
    private Button prevBtn, nextBtn;
    private Label pageInfo;

    // بيانات
    private List<Course> allCourses = new ArrayList<>();
    private List<Course> filtered = new ArrayList<>();

    // ترقيم الصفحات
    private static final int PAGE_SIZE = 9;   // 3 صفوف × 3 أعمدة
    private int currentPage = 1;

    public CoursesPageController(MainController mainController, ApiService apiService, SessionManager sessionManager) {
        this.mainController = mainController;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
    }

    public VBox createCoursesPage() {
        root = new VBox(22);
        root.setPadding(new Insets(24, 28, 28, 28));
        root.setAlignment(Pos.TOP_CENTER);

        // ===== Header =====
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        Label title = new Label("الكورسات التقنية");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label("اكتشف مجموعة واسعة من الكورسات التقنية المتخصصة");
        subtitle.getStyleClass().add("course-instructor");
        header.getChildren().addAll(title, subtitle);

        // ===== Search + actions =====
        VBox searchBlock = new VBox(12);
        searchBlock.setAlignment(Pos.CENTER);
        searchBlock.setPadding(new Insets(8, 0, 0, 0));

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER);

        searchField = new TextField();
        searchField.setPromptText("ابحث عن الكورسات، المدربين، أو المواضيع…");
        searchField.getStyleClass().add("text-field");
        searchField.setPrefWidth(760);
        searchField.setPrefHeight(42);
        searchField.setOnAction(e -> onSearchClicked());

        filterToggleBtn = new Button("تصفية ▾");
        filterToggleBtn.getStyleClass().add("secondary-button");
        filterToggleBtn.setPrefHeight(42);
        filterToggleBtn.setOnAction(e -> toggleFiltersPanel());

        searchRow.getChildren().addAll(searchField, filterToggleBtn);

        // sort row
        HBox sortRow = new HBox(8);
        sortRow.setAlignment(Pos.CENTER_RIGHT);
        sortRow.setPrefWidth(900);

        sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("الأحدث", "الأقل سعراً", "الأعلى سعراً", "الأطول مدة");
        sortCombo.setValue("الأحدث");
        sortCombo.getStyleClass().add("combo-box");
        sortCombo.setOnAction(e -> refreshView());


        // خليه في أقصى اليمين لكن محافظ على المنتصف العام
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label sortLbl = new Label("الترتيب");
        sortLbl.getStyleClass().add("course-instructor");
        Button searchBtn = new Button("بحث");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setPrefHeight(42);
        searchBtn.setOnAction(e -> onSearchClicked());

        sortRow.getChildren().addAll(spacer, sortLbl, sortCombo, searchBtn);

        searchBlock.getChildren().addAll(searchRow, sortRow);

        // ===== Filters Panel (collapsed by default) =====
        filtersPanel = buildFiltersPanel();
        filtersPanel.setVisible(false);
        filtersPanel.setManaged(false);

        // ===== Status =====
        statusLabel = new Label();
        statusLabel.getStyleClass().add("course-instructor");

        // ===== Cards grid =====
        cardsGrid = new GridPane();
        cardsGrid.setHgap(20);
        cardsGrid.setVgap(24);
        cardsGrid.setAlignment(Pos.TOP_CENTER);
        cardsGrid.setPadding(new Insets(8, 0, 0, 0));

        ScrollPane scroll = new ScrollPane(cardsGrid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // ===== Pagination =====
        HBox pager = new HBox(10);
        pager.setAlignment(Pos.CENTER);

        prevBtn = new Button("السابق");
        prevBtn.getStyleClass().add("secondary-button");
        prevBtn.setOnAction(e -> { if (currentPage > 1) { currentPage--; renderPage(); } });

        pageInfo = new Label("صفحة 1");
        pageInfo.getStyleClass().add("course-instructor");

        nextBtn = new Button("التالي");
        nextBtn.getStyleClass().add("secondary-button");
        nextBtn.setOnAction(e -> {
            int totalPages = (int) Math.ceil((double) filtered.size() / PAGE_SIZE);
            if (currentPage < totalPages) { currentPage++; renderPage(); }
        });

        pager.getChildren().addAll(prevBtn, pageInfo, nextBtn);

        // Assemble
        root.getChildren().addAll(header, searchBlock, filtersPanel, statusLabel, scroll, pager);

        // Load data
        loadAll();

        return root;
    }

    // ===== Filters panel =====
    private VBox buildFiltersPanel() {
        VBox panel = new VBox(16);
        panel.getStyleClass().add("course-card");
        panel.setPadding(new Insets(18));
        panel.setMaxWidth(920);

        // Title row
        HBox head = new HBox();
        head.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label("تصفية");
        t.getStyleClass().add("course-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button close = new Button("إغلاق ✕");
        close.getStyleClass().add("secondary-button");
        close.setOnAction(e -> toggleFiltersPanel());
        head.getChildren().addAll(t, sp, close);

        // Grid of filters
        GridPane filtersGrid = new GridPane();
        filtersGrid.setHgap(18);
        filtersGrid.setVgap(14);

        // السعر
        Label priceLbl = new Label("السعر");
        priceLbl.getStyleClass().add("course-instructor");
        priceGroup = new ToggleGroup();
        priceAll  = new RadioButton("جميع الكورسات");
        priceFree = new RadioButton("مجاني");
        pricePaid = new RadioButton("مدفوع");
        priceAll.setToggleGroup(priceGroup);
        priceFree.setToggleGroup(priceGroup);
        pricePaid.setToggleGroup(priceGroup);
        priceAll.setSelected(true);

        VBox priceBox = new VBox(6, priceAll, priceFree, pricePaid);

        // الموقع
        Label locLbl = new Label("الموقع");
        locLbl.getStyleClass().add("course-instructor");
        locationCombo = new ComboBox<>();
        locationCombo.getItems().addAll("جميع المواقع", "الرياض", "جدة", "الدمام", "أونلاين");
        locationCombo.setValue("جميع المواقع");
        locationCombo.getStyleClass().add("combo-box");

        filtersGrid.add(priceLbl, 0, 0);
        filtersGrid.add(priceBox, 0, 1);
        filtersGrid.add(locLbl, 1, 0);
        filtersGrid.add(locationCombo, 1, 1);

        // actions
        Button clear = new Button("مسح جميع الفلاتر");
        clear.getStyleClass().add("secondary-button");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setOnAction(e -> clearAllFilters());

        Button apply = new Button("تطبيق الفلاتر");
        apply.getStyleClass().add("primary-button");
        apply.setOnAction(e -> { currentPage = 1; refreshView(); });

        HBox actions = new HBox(10, apply, clear);
        actions.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(head, filtersGrid, actions);
        return panel;
    }

    private void toggleFiltersPanel() {
        boolean show = !filtersPanel.isVisible();
        filtersPanel.setVisible(show);
        filtersPanel.setManaged(show);
        filterToggleBtn.setText(show ? "تصفية ▴" : "تصفية ▾");
    }

    // ===== Data loading =====
    private void loadAll() {
        setStatus("جاري تحميل الكورسات…");
        showLoading(true);

        Task<List<Course>> task = new Task<>() {
            @Override
            protected List<Course> call() throws Exception {
                return apiService.getAllCourses().get();
            }

            @Override
            protected void succeeded() {
                showLoading(false);
                allCourses = getValue() != null ? getValue() : Collections.emptyList();
                setStatus("تم العثور على " + allCourses.size() + " كورس");
                currentPage = 1;
                refreshView();

                // بعد ما نحمّل الكورسات، نحمّل IDs المفضلة لتفعيل القلوب
                initFavoritesAsync();
            }

            @Override
            protected void failed() {
                showLoading(false);
                errorToGrid("فشل في تحميل الكورسات: " + getException().getMessage());
            }
        };
        EXECUTOR.submit(task);
    }

    private void onSearchClicked() {
        currentPage = 1;
        refreshView();
    }

    // ===== Filtering + Sorting + Paging =====
    private void refreshView() {
        // خذ snapshot للقيم (final داخل الـ stream)
        final String keyword = Optional.ofNullable(searchField.getText()).orElse("").trim().toLowerCase(Locale.ROOT);

        final String chosenLoc = locationCombo.getValue();
        final String locFilter = ("جميع المواقع".equals(chosenLoc) ? null : chosenLoc);

        final Boolean isFree =
                priceFree.isSelected() ? Boolean.TRUE :
                        pricePaid.isSelected() ? Boolean.FALSE : null;

        // filter
        filtered = allCourses.stream()
                .filter(c -> {
                    if (keyword.isEmpty()) return true;
                    String t = Optional.ofNullable(c.getTitle()).orElse("").toLowerCase(Locale.ROOT);
                    String d = Optional.ofNullable(c.getDescription()).orElse("").toLowerCase(Locale.ROOT);
                    String i = Optional.ofNullable(c.getInstructor()).orElse("").toLowerCase(Locale.ROOT);
                    return t.contains(keyword) || d.contains(keyword) || i.contains(keyword);
                })
                .filter(c -> {
                    if (locFilter == null) return true;
                    String courseLoc = c.getLocation();
                    if (courseLoc == null && ("أونلاين".equalsIgnoreCase(locFilter) || "online".equalsIgnoreCase(locFilter))) {
                        return true;
                    }
                    return eqIgnoreCase(courseLoc, locFilter) ||
                            ("أونلاين".equalsIgnoreCase(locFilter) && eqIgnoreCase(courseLoc, "online"));
                })
                .filter(c -> {
                    if (isFree == null) return true;
                    Boolean f = c.getIsFree();
                    return Boolean.TRUE.equals(isFree) ? Boolean.TRUE.equals(f) : Boolean.FALSE.equals(f);
                })
                .collect(Collectors.toList());

        // sort
        final String sortKey = sortCombo.getValue();
        Comparator<Course> comparator;
        switch (sortKey) {
            case "الأقل سعراً":
                comparator = Comparator.comparing(c -> nullSafePrice(c, 0.0));
                break;
            case "الأعلى سعراً":
                comparator = Comparator.comparing((Course c) -> nullSafePrice(c, 0.0)).reversed();
                break;
            case "الأطول مدة":
                comparator = Comparator.comparing((Course c) -> Optional.ofNullable(c.getDuration()).orElse(0)).reversed();
                break;
            default: // الأحدث (created_at desc)
                comparator = Comparator.comparing(
                        (Course c) -> Optional.ofNullable(c.getCreatedAt()).orElse(null),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed();
        }
        filtered.sort(comparator);

        // render
        renderPage();
        setStatus("تم العثور على " + filtered.size() + " كورس");
    }

    private double nullSafePrice(Course c, double def) {
        try {
            return c.getPrice() != null ? c.getPrice().doubleValue() : def;
        } catch (Exception e) {
            return def;
        }
    }

    private boolean eqIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private void renderPage() {
        cardsGrid.getChildren().clear();

        if (filtered.isEmpty()) {
            showEmpty();
            pageInfo.setText("لا نتائج");
            prevBtn.setDisable(true);
            nextBtn.setDisable(true);
            return;
        }

        int totalPages = (int) Math.ceil((double) filtered.size() / PAGE_SIZE);
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filtered.size());

        List<Course> page = filtered.subList(from, to);

        // 3 per row
        int colCount = 3;
        int r = 0, c = 0;
        for (Course course : page) {
            VBox card = buildCourseCard(course);
            cardsGrid.add(card, c, r);
            c++;
            if (c == colCount) { c = 0; r++; }
        }

        pageInfo.setText("صفحة " + currentPage + " / " + totalPages);
        prevBtn.setDisable(currentPage <= 1);
        nextBtn.setDisable(currentPage >= totalPages);
    }

    // ===== UI Helpers =====
    private void showLoading(boolean loading) {
        if (!loading) return;
        cardsGrid.getChildren().clear();

        VBox loadingCard = new VBox(12);
        loadingCard.setAlignment(Pos.CENTER);
        loadingCard.setPadding(new Insets(40));
        loadingCard.getStyleClass().add("course-card");
        loadingCard.setPrefWidth(360);

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(38, 38);
        Label txt = new Label("جاري التحميل…");
        txt.getStyleClass().add("course-instructor");
        loadingCard.getChildren().addAll(pi, txt);

        cardsGrid.add(loadingCard, 1, 0);
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("error-message", "success-message");
        statusLabel.getStyleClass().add("success-message");
    }

    private void errorToGrid(String message) {
        cardsGrid.getChildren().clear();

        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        box.getStyleClass().add("course-card");
        Label icon = new Label("⚠️");
        icon.getStyleClass().add("stats-number");
        Label title = new Label("حدث خطأ");
        title.getStyleClass().add("course-title");
        Label msg = new Label(message);
        msg.getStyleClass().add("error-message");
        msg.setWrapText(true);
        Button retry = new Button("إعادة المحاولة");
        retry.getStyleClass().add("secondary-button");
        retry.setOnAction(e -> loadAll());

        box.getChildren().addAll(icon, title, msg, retry);
        cardsGrid.add(box, 1, 0);
    }

    private void showEmpty() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        box.getStyleClass().add("course-card");

        Rectangle ph = new Rectangle(96, 96);
        ph.setFill(Color.web("#E2E8F0"));
        ph.setArcWidth(12); ph.setArcHeight(12);

        Label t = new Label("لا توجد نتائج مطابقة");
        t.getStyleClass().add("course-title");

        Label m = new Label("جرّب كلمات أخرى أو غيّر الفلاتر");
        m.getStyleClass().add("course-description");

        Button clear = new Button("مسح جميع الفلاتر");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearAllFilters());

        box.getChildren().addAll(ph, t, m, clear);
        cardsGrid.add(box, 1, 0);
    }

    private void clearAllFilters() {
        searchField.clear();
        locationCombo.setValue("جميع المواقع");
        priceAll.setSelected(true);
        sortCombo.setValue("الأحدث");
        currentPage = 1;
        refreshView();
    }

    // ===== Card =====
    private VBox buildCourseCard(Course course) {
        VBox card = new VBox();
        card.getStyleClass().add("course-card");
        card.setPrefWidth(360);
        card.setMaxWidth(360);
        card.setSpacing(0);

        // image + زر المفضلة (قلب)
        ImageView imageView = new ImageView();
        imageView.setFitWidth(360);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("course-image");
        loadCourseImage(imageView, course.getImageUrl());

        // غلاف عشان أركّب القلب في الزاوية
        StackPane imageWrap = new StackPane(imageView);

        ToggleButton favBtn = new ToggleButton();
        favBtn.setFocusTraversable(false);
        favBtn.setSelected(isFavorite(course.getId()));
        styleFavToggle(favBtn, favBtn.isSelected());
        favBtn.selectedProperty().addListener((obs, wasSel, isSel) -> styleFavToggle(favBtn, isSel));
        favBtn.setOnAction(e -> {
            e.consume(); // لا تفتح التفاصيل لما أضغط القلب
            onToggleFavorite(course, favBtn);
        });

        StackPane.setAlignment(favBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(favBtn, new Insets(10, 10, 0, 0));
        imageWrap.getChildren().add(favBtn);

        // content
        VBox content = new VBox(10);
        content.getStyleClass().add("course-content");
        content.setPadding(new Insets(16));

        Label title = new Label(Optional.ofNullable(course.getTitle()).orElse("Course"));
        title.getStyleClass().add("course-title");
        title.setWrapText(true);

        Label instructor = new Label("المدرب: " + Optional.ofNullable(course.getInstructor()).orElse("-"));
        instructor.getStyleClass().add("course-instructor");

        Label description = new Label(course.getShortDescription());
        description.getStyleClass().add("course-description");
        description.setWrapText(true);
        description.setPrefHeight(64);

        HBox bottom = new HBox(8);
        bottom.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(course.getPriceDisplay());
        price.getStyleClass().add(Boolean.TRUE.equals(course.getIsFree()) ? "course-free" : "course-price");
        Label location = new Label(course.getLocationDisplay());
        location.getStyleClass().add("course-instructor");
        bottom.getChildren().addAll(price, new Label("•"), location);

        Button details = new Button("عرض التفاصيل");
        details.getStyleClass().add("secondary-button");
        details.setOnAction(e -> mainController.loadCourseDetailsPage(course));

        content.getChildren().addAll(title, instructor, description, bottom, details);

        card.getChildren().addAll(imageWrap, content);

        // فتح التفاصيل عند الضغط على الكارت (ما عدا القلب)
        card.setOnMouseClicked(e -> mainController.loadCourseDetailsPage(course));
        return card;
    }

    // ===== Images =====
    private void loadCourseImage(ImageView imageView, String imageUrl) {
        String normalized = normalizeUrl(imageUrl);
        if (normalized == null) {
            setDefaultCourseImage(imageView);
            return;
        }

        Image cached = imageCache.get(normalized);
        if (cached != null) {
            imageView.setImage(cached);
            return;
        }

        try {
            Image image = new Image(normalized, 360, 200, false, true, true);
            imageView.setImage(image);

            image.errorProperty().addListener((obs, wasErr, isErr) -> {
                if (isErr) setDefaultCourseImage(imageView);
            });
            image.progressProperty().addListener((obs, ov, nv) -> {
                if (nv != null && nv.doubleValue() >= 1.0 && !image.isError()) {
                    imageCache.putIfAbsent(normalized, image);
                }
            });
        } catch (Exception e) {
            setDefaultCourseImage(imageView);
        }
    }

    private String normalizeUrl(String raw) {
        if (raw == null) return null;
        String candidate = raw.trim();
        if (candidate.isEmpty()) return null;

        candidate = candidate.replace('\\', '/');
        if (candidate.startsWith("//")) candidate = "https:" + candidate;

        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
            HttpUrl base = HttpUrl.parse(API_BASE);
            if (base == null) return null;
            HttpUrl resolved = base.resolve(candidate.startsWith("/") ? candidate : "/" + candidate);
            return resolved != null ? resolved.toString() : null;
        }
        HttpUrl url = HttpUrl.parse(candidate);
        if (url == null) return null;
        return url.newBuilder().build().toString();
    }

    private void setDefaultCourseImage(ImageView imageView) {
        try {
            Image def = new Image(getClass().getResourceAsStream("/images/default-course.png"));
            if (def != null && !def.isError()) { imageView.setImage(def); return; }
        } catch (Exception ignore) { }
        imageView.setImage(null);
        imageView.setStyle("-fx-background-color:#E2E8F0;");
    }

    /* ===================== Favorites helpers ===================== */

    private void initFavoritesAsync() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            favoriteIds.clear();
            return;
        }
        apiService.getFavorites()
                .thenAccept(list -> {
                    Set<Long> ids = new HashSet<>();
                    if (list != null) {
                        for (Course c : list) {
                            if (c != null && c.getId() != null) ids.add(c.getId());
                        }
                    }
                    Platform.runLater(() -> {
                        favoriteIds.clear();
                        favoriteIds.addAll(ids);
                        // نحدّث القلوب الحالية في الصفحة المعروضة
                        renderPage();
                    });
                })
                .exceptionally(ex -> null); // تجاهل الخطأ (القلب يبقى غير مفعّل)
    }

    private boolean isFavorite(Long id) {
        return id != null && favoriteIds.contains(id);
    }

    private void onToggleFavorite(Course course, ToggleButton btn) {
        if (course == null || course.getId() == null) return;

        // لو مو مسجل دخول
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            btn.setSelected(false);
            styleFavToggle(btn, false);
            new Alert(Alert.AlertType.INFORMATION, "سجّل الدخول لحفظ الكورسات في المفضلة.").showAndWait();
            return;
        }

        btn.setDisable(true);
        final boolean targetFav = !isFavorite(course.getId());

        CompletableFuture<Boolean> fut = targetFav
                ? apiService.addFavorite(course.getId())
                : apiService.removeFavorite(course.getId());

        fut.thenAccept(ok -> Platform.runLater(() -> {
            btn.setDisable(false);
            if (Boolean.TRUE.equals(ok)) {
                if (targetFav) favoriteIds.add(course.getId());
                else favoriteIds.remove(course.getId());
                btn.setSelected(targetFav);
                styleFavToggle(btn, targetFav);
            } else {
                // رجّع الحالة القديمة
                btn.setSelected(!targetFav);
                styleFavToggle(btn, !targetFav);
                new Alert(Alert.AlertType.ERROR, "تعذّر تحديث المفضلة.").showAndWait();
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                btn.setDisable(false);
                btn.setSelected(!targetFav);
                styleFavToggle(btn, !targetFav);
                new Alert(Alert.AlertType.ERROR, "خطأ في الاتصال بالمخدم.").showAndWait();
            });
            return null;
        });
    }

    private void styleFavToggle(ToggleButton btn, boolean selected) {
        // قلب بسيط داخل كبسولة بيضاء نصف شفافة
        String base = "-fx-background-radius:999; -fx-padding:6 10; -fx-font-weight:700;"
                + "-fx-background-color: rgba(255,255,255,0.92);"
                + "-fx-border-color: rgba(0,0,0,0.08); -fx-border-radius:999;";
        String txt = selected ? "♥" : "♡";
        String color = selected ? "#DC2626" : "#6B7280";
        btn.setText(txt);
        btn.setStyle(base + " -fx-text-fill:" + color + ";");
    }
}
