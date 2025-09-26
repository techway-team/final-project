package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.EnrollmentDTO;
import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.model.LessonProgress;
import com.techway.coursemanagementdesktop.model.User;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ProfilePageController {

    private final MainController mainController;
    private final ApiService apiService;         // لاستخدام نداءات المفضلة
    private final SessionManager sessionManager; // مصدر بيانات المستخدم الحالي

    public ProfilePageController(MainController mainController,
                                 ApiService apiService,
                                 SessionManager sessionManager) {
        this.mainController = mainController;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
    }

    // عناصر يتم تحديثها ديناميكياً
    private Label nameValue;
    private Label emailValue;

    // مراجع تبويب "المفضلة"
    private VBox favoritesCard;                 // الحاوية الأساسية لقسم المفضلة
    private Label favoritesHeaderLabel;         // "المفضلة (N)"
    private FlowPane favoritesGrid;             // شبكة الكروت
    private VBox favoritesEmpty;                // حالة فارغة
    private VBox favoritesError;                // حالة خطأ
    private ProgressIndicator favoritesLoading; // حالة تحميل


    // مراجع تبويب "كورساتي"
    private VBox myCoursesCard;
    private Label myCoursesHeaderLabel;
    private FlowPane myCoursesGrid;
    private VBox myCoursesEmpty;
    private VBox myCoursesError;
    private ProgressIndicator myCoursesLoading;

    /** يبني صفحة الملف الشخصي */
    public VBox createProfilePage() {
        User user = (sessionManager != null) ? sessionManager.getCurrentUser() : null;

        String displayName = (user != null && user.getName() != null && !user.getName().isBlank())
                ? user.getName() : "User";
        String email = (user != null && user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail() : "—";

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color:#F8FAFC;");

        // زر رجوع
        Button back = new Button("← الرجوع للرئيسية");
        back.setOnAction(e -> mainController.navigateToHome());
        back.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748B;-fx-font-size:13px;");
        HBox backRow = new HBox(back);
        backRow.setAlignment(Pos.CENTER_LEFT);
        backRow.setMaxWidth(1080);

        // ترويسة
        Node headerCard = buildHeaderCard(displayName, email);

        // إحصائيات (أرقام شكلية الآن)
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setMaxWidth(1080);

// مبدئيًا نعرض القيم صفر
        statsRow.getChildren().addAll(
                statCard("20", "يوم في المنصة"),
                statCard("0", "كورس مكتمل"),
                statCard("0", "كورس محفوظ")
        );

// بعدين نحدثها من الـ API
        loadStatsRow(statsRow);

        // تبويبات + محتوى
        ToggleGroup tabs = new ToggleGroup();
        ToggleButton tabOverview = makeTab("نظرة عامة", tabs, true);
        ToggleButton tabFavs     = makeTab("المفضلة",  tabs, false);
        ToggleButton tabMyCourses = makeTab("كورساتي", tabs, false);
        ToggleButton tabSettings = makeTab("الإعدادات", tabs, false);

        HBox tabsBar = new HBox(8, tabOverview, tabFavs,tabMyCourses, tabSettings);
        tabsBar.setAlignment(Pos.CENTER_LEFT);
        tabsBar.setMaxWidth(1080);
        tabsBar.setPadding(new Insets(0, 0, 4, 0));

        StackPane content = new StackPane();
        content.setMaxWidth(1080);

        Node overviewContent  = buildOverviewContent();
        Node favoritesContent = buildFavoritesContent(); // ديناميكي
        Node settingsContent  = buildSettingsContent(displayName, email);
        Node myCoursesContent = buildMyCoursesContent();
        content.getChildren().addAll(overviewContent, favoritesContent, myCoursesContent, settingsContent);

        favoritesContent.setVisible(false);
        myCoursesContent.setVisible(false);
        settingsContent.setVisible(false);

        tabs.selectedToggleProperty().addListener((obs, o, n) -> {
            boolean ov = tabs.getSelectedToggle() == tabOverview;
            boolean fv = tabs.getSelectedToggle() == tabFavs;
            boolean mc = tabs.getSelectedToggle() == tabMyCourses;

            overviewContent.setVisible(ov);
            favoritesContent.setVisible(fv);
            myCoursesContent.setVisible(mc);
            settingsContent.setVisible(!ov && !fv && !mc);

            if (fv) {
                refreshFavorites();
            }
            if (mc) {
                refreshMyCourses();
            }
        });

        // فوتر بسيط
        Node footer = buildFooter();

        root.getChildren().addAll(backRow, headerCard, statsRow, tabsBar, content, footer);
        return root;
    }

    /* ------------------------------- UI Builders ------------------------------- */

    private Node buildHeaderCard(String displayName, String email) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setMaxWidth(1080);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(14), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(Color.web("#E2E8F0"),
                BorderStrokeStyle.SOLID, new CornerRadii(14), new BorderWidths(1))));

        // أفاتار أول حرف
        Circle avatar = new Circle(32, Color.web("#EEF2FF"));
        Label initial = new Label(getInitial(displayName));
        initial.setTextFill(Color.web("#4F46E5"));
        initial.setFont(Font.font(18));
        StackPane avatarBox = new StackPane(avatar, initial);
        avatarBox.setPrefSize(64, 64);

        // معلومات
        nameValue = new Label(displayName);
        nameValue.setStyle("-fx-font-size:18px;-fx-font-weight:700;-fx-text-fill:#0F172A;");
        emailValue = new Label(email);
        emailValue.setStyle("-fx-text-fill:#475569;-fx-font-size:13px;");
        Label joined = new Label("انضم حديثاً");
        joined.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");

        VBox info = new VBox(4, nameValue, emailValue, joined);
        info.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button action = new Button("عرض عام");
        action.setStyle(primaryGhost());

        card.getChildren().addAll(avatarBox, info, spacer, action);
        return card;
    }

    private Node buildMyCoursesContent() {
        myCoursesCard = sectionCard();

        myCoursesHeaderLabel = new Label("كورساتي");
        myCoursesHeaderLabel.setStyle(sectionTitle());

        // حالة تحميل
        myCoursesLoading = new ProgressIndicator();
        myCoursesLoading.setPrefSize(38, 38);

        // الشبكة
        myCoursesGrid = new FlowPane();
        myCoursesGrid.setHgap(16);
        myCoursesGrid.setVgap(16);
        myCoursesGrid.setPadding(new Insets(8));
        myCoursesGrid.setPrefWrapLength(1000);

        // حالة فارغة
        myCoursesEmpty = new VBox(6);
        myCoursesEmpty.setAlignment(Pos.CENTER);
        myCoursesEmpty.setPadding(new Insets(32));
        Label icon = new Label("📚");
        icon.setStyle("-fx-font-size:36px;-fx-text-fill:#CBD5E1;");
        Label h = new Label("لا توجد كورسات مسجّل فيها");
        h.setStyle("-fx-font-size:14px;-fx-text-fill:#0F172A;-fx-font-weight:700;");
        myCoursesEmpty.getChildren().addAll(icon, h);

        // حالة خطأ
        myCoursesError = new VBox(8);
        myCoursesError.setAlignment(Pos.CENTER);
        myCoursesError.setPadding(new Insets(24));
        Label err = new Label("تعذّر تحميل الكورسات");
        err.setStyle("-fx-text-fill:#DC2626;-fx-font-weight:700;");
        Button retry = new Button("إعادة المحاولة");
        retry.setStyle(primaryGhost());
        retry.setOnAction(e -> refreshMyCourses());
        myCoursesError.getChildren().addAll(err, retry);

        myCoursesCard.getChildren().addAll(myCoursesHeaderLabel, myCoursesLoading);
        return myCoursesCard;
    }


    private void loadStatsRow(HBox statsRow) {
        Long userId = sessionManager.getCurrentUserId();

        ApiService.getInstance().getUserEnrollments(userId)
                .thenAccept(enrollments -> {
                    // هنا نحتاج نجيب تقدم كل تسجيل (Enrollment) من السيرفر
                    // جمع كل CompletableFuture لجلب تقدم كل enrollment
                    List<CompletableFuture<Double>> progressFutures = enrollments.stream()
                            .map(enrollment ->
                                    ApiService.getInstance().getLessonProgressByEnrollmentId(enrollment.getId())
                                            .thenApply(progressList -> {
                                                long completedCount = progressList.stream()
                                                        .filter(LessonProgress::isCompleted)
                                                        .count();
                                                return progressList.isEmpty() ? 0.0 : (completedCount * 100.0) / progressList.size();
                                            })
                            )
                            .collect(Collectors.toList());

                    // ننتظر جميع الـ futures تكتمل
                    CompletableFuture.allOf(progressFutures.toArray(new CompletableFuture[0]))
                            .thenAccept(v -> {
                                // بعد ما تكتمل، نحسب المتوسط ونحسب الدورات المكتملة بناءً على النسبة
                                double totalProgress = 0;
                                int completedCount = 0;
                                int total = enrollments.size();

                                for (int i = 0; i < total; i++) {
                                    double progress = progressFutures.get(i).join();
                                    totalProgress += progress;
                                    if (progress >= 100.0) {
                                        completedCount++;
                                    }
                                }

                                double avgProgress = total > 0 ? totalProgress / total : 0;

                                int finalCompletedCount = completedCount;
                                Platform.runLater(() -> {
                                    statsRow.getChildren().clear();
                                    statsRow.getChildren().addAll(
                                            statCard("20", "يوم في المنصة"),
                                            statCard(String.valueOf(finalCompletedCount), "كورس مكتمل"),
                                            statCard(String.format("%.1f%%", avgProgress), "متوسط التقدم")
                                    );
                                });
                            })
                            .exceptionally(ex -> {
                                Platform.runLater(() -> {
                                    statsRow.getChildren().clear();
                                    statsRow.getChildren().add(new Label("فشل تحميل الإحصائيات ❌"));
                                });
                                return null;
                            });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statsRow.getChildren().clear();
                        statsRow.getChildren().add(new Label("فشل تحميل الإحصائيات ❌"));
                    });
                    return null;
                });
    }




    private String pluralize(String word, int count) {
        if (word.equals("كورس محفوظ")) {
            if (count == 0) return "لا يوجد كورسات محفوظة";
            if (count == 1) return "كورس محفوظ واحد";
            return count + " كورسات محفوظة";
        } else if (word.equals("completed")) {
            if (count == 0) return "لا يوجد كورسات مكتملة";
            if (count == 1) return "كورس مكتمل واحد";
            return count + " كورسات مكتملة";
        }
        return count + " " + word;
    }


    private Node statCard(String number, String label) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(14), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(Color.web("#E2E8F0"),
                BorderStrokeStyle.SOLID, new CornerRadii(14), new BorderWidths(1))));
        card.setPrefWidth(350);

        Label num = new Label(number);
        num.setStyle("-fx-font-size:22px;-fx-font-weight:800;-fx-text-fill:#111827;");
        Label cap = new Label(label);
        cap.setStyle("-fx-text-fill:#6B7280;-fx-font-size:12px;");

        card.getChildren().addAll(num, cap);
        return card;
    }

    private ToggleButton makeTab(String text, ToggleGroup group, boolean selected) {
        ToggleButton t = new ToggleButton(text);
        t.setToggleGroup(group);
        t.setSelected(selected);
        t.setStyle(tabStyle(selected));
        t.selectedProperty().addListener((obs, was, isSel) -> t.setStyle(tabStyle(isSel)));
        return t;
    }

    private String tabStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color:#EEF2FF;-fx-text-fill:#4F46E5;" +
                    "-fx-font-weight:700;-fx-padding:8 14;-fx-background-radius:10;" +
                    "-fx-border-color:#C7D2FE;-fx-border-radius:10;";
        }
        return "-fx-background-color:transparent;-fx-text-fill:#475569;" +
                "-fx-padding:8 14;-fx-background-radius:10;" +
                "-fx-border-color:transparent;-fx-border-radius:10;";
    }

    private Node buildOverviewContent() {
        VBox card = sectionCard();
        Label t = new Label("نظرة عامة");
        t.setStyle(sectionTitle());
        Label p = new Label("هنا ستظهر لمحات سريعة عن نشاطك على المنصة.");
        p.setStyle("-fx-text-fill:#64748B;-fx-font-size:13px;");
        card.getChildren().addAll(t, p);
        return card;
    }

    private Node loadFavorites() {
        Long userId = SessionManager.getInstance().getCurrentUserId();

        if (userId == null) {
            return new Label("الرجاء تسجيل الدخول أولاً");
        }

        TilePane tilePane = new TilePane();
        tilePane.setPadding(new Insets(20));
        tilePane.setHgap(20);
        tilePane.setVgap(20);
        tilePane.setPrefColumns(3); // كم كارد في كل صف

        ApiService.getInstance().getFavoritesByUserId(userId).thenAccept(favorites -> {
            Platform.runLater(() -> {
                tilePane.getChildren().clear();
                if (favorites == null || favorites.isEmpty()) {
                    tilePane.getChildren().add(new Label("لا يوجد كورسات مفضلة"));
                } else {
                    for (Course course : favorites) {
                        VBox card = new VBox(10);
                        card.setAlignment(Pos.TOP_CENTER);
                        card.setPadding(new Insets(10));
                        card.setPrefSize(220, 250);
                        card.setStyle(
                                "-fx-background-color: #ffffff;" +
                                        "-fx-background-radius: 12;" +
                                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 4);"
                        );

                        // صورة (افتراضية إذا ما عندك URL)
                        ImageView imageView;
                        try {
                            imageView = new ImageView(new Image(
                                    course.getImageUrl() != null ? course.getImageUrl() :
                                            "https://via.placeholder.com/220x120.png",
                                    200, 120, true, true));
                        } catch (Exception e) {
                            imageView = new ImageView(new Image("https://via.placeholder.com/220x120.png", 200, 120, true, true));
                        }

                        Label title = new Label(course.getTitle());
                        title.getStyleClass().add("course-title");
                        Label price = new Label(course.getPrice() + " ريال");
                        price.setStyle("-fx-text-fill: #6c63ff; -fx-font-size: 13px;");

                        Button removeBtn = new Button("إزالة من المفضلة");
                        removeBtn.setStyle(
                                "-fx-background-color: #ff4d4f;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-background-radius: 8;"
                        );

                        card.getChildren().addAll(imageView, title, price, removeBtn);
                        tilePane.getChildren().add(card);
                    }
                }
            });
        });

        ScrollPane scrollPane = new ScrollPane(tilePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    /** يبني تبويب المفضلة (هيكل + حالات) */
    private Node buildFavoritesContent() {
        favoritesCard = sectionCard();

        favoritesHeaderLabel = new Label("المفضلة");
        favoritesHeaderLabel.setStyle(sectionTitle());

        // حالة تحميل
        favoritesLoading = new ProgressIndicator();
        favoritesLoading.setPrefSize(38, 38);

        // الشبكة
        favoritesGrid = new FlowPane();
        favoritesGrid.setHgap(16);
        favoritesGrid.setVgap(16);
        favoritesGrid.setPadding(new Insets(8));
        favoritesGrid.setPrefWrapLength(1000); // يلف تلقائياً حسب العرض

        // حالة فارغة
        favoritesEmpty = new VBox(6);
        favoritesEmpty.setAlignment(Pos.CENTER);
        favoritesEmpty.setPadding(new Insets(32));
        Label icon = new Label("♡");
        icon.setStyle("-fx-font-size:36px;-fx-text-fill:#CBD5E1;");
        Label h = new Label("لا توجد كورسات محفوظة");
        h.setStyle("-fx-font-size:14px;-fx-text-fill:#0F172A;-fx-font-weight:700;");
        Label d = new Label("ابدأ بحفظ الكورسات التي تهمّك");
        d.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
        favoritesEmpty.getChildren().addAll(icon, h, d);

        // حالة خطأ
        favoritesError = new VBox(8);
        favoritesError.setAlignment(Pos.CENTER);
        favoritesError.setPadding(new Insets(24));
        Label err = new Label("تعذّر تحميل المفضّلة");
        err.setStyle("-fx-text-fill:#DC2626;-fx-font-weight:700;");
        Button retryFav = new Button("إعادة المحاولة");
        retryFav.setStyle(primaryGhost());
        retryFav.setOnAction(e -> refreshFavorites());
        favoritesError.getChildren().addAll(err, retryFav);

        // البداية: عنوان + تحميل
        favoritesCard.getChildren().addAll(favoritesHeaderLabel, favoritesLoading);
        return favoritesCard;
    }

    private Node buildSettingsContent(String displayName, String email) {
        VBox card = sectionCard();
        Label t = new Label("إعدادات الحساب");
        t.setStyle(sectionTitle());

        TextField nameField = new TextField(displayName);
        nameField.setPromptText("الاسم");
        nameField.setStyle(fieldStyle());

        TextField emailField = new TextField(email);
        emailField.setPromptText("البريد الإلكتروني");
        emailField.setStyle(fieldStyle());

        PasswordField newPass = new PasswordField();
        newPass.setPromptText("كلمة المرور الجديدة (اختياري)");
        newPass.setStyle(fieldStyle());

        VBox fields = new VBox(10, nameField, emailField, newPass);

        Button save = new Button("حفظ التغييرات");
        save.setStyle(primarySolid());
        save.setOnAction(e -> {
            String newName  = nameField.getText().trim().isEmpty() ? displayName : nameField.getText().trim();
            String newEmail = emailField.getText().trim().isEmpty() ? email : emailField.getText().trim();
            nameValue.setText(newName);
            emailValue.setText(newEmail);
            new Alert(Alert.AlertType.INFORMATION, "تم حفظ التغييرات بنجاح.").showAndWait();
        });

        card.getChildren().addAll(t, fields, save);
        return card;
    }

    private Node buildFooter() {
        HBox foot = new HBox();
        foot.setAlignment(Pos.CENTER);
        foot.setPadding(new Insets(24, 16, 8, 16));
        foot.setMaxWidth(1080);
        foot.setBackground(new Background(new BackgroundFill(Color.web("#0B1220"), new CornerRadii(14), Insets.EMPTY)));

        VBox col = new VBox(6);
        col.setAlignment(Pos.CENTER);
        Label title = new Label("اتصل بنا");
        title.setStyle("-fx-text-fill:#F8FAFC;-fx-font-weight:700;");
        Label mail = new Label("info@TechWay.com  •  +966 123 50 966");
        mail.setStyle("-fx-text-fill:#CBD5E1;-fx-font-size:12px;");
        col.getChildren().addAll(title, mail);

        foot.getChildren().add(col);
        return foot;
    }

    /* ------------------------------- Favorites Logic ------------------------------- */

    /** تحميل وتحديث تبويب المفضلة */
    private void refreshFavorites() {
        // لو المستخدم غير مسجّل
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            showFavoritesLoggedOut();
            return;
        }

        // حالة التحميل
        favoritesCard.getChildren().setAll(favoritesHeaderLabel, favoritesLoading);

        CompletableFuture<List<Course>> fut = apiService.getFavorites();
        fut.thenAccept(courses -> Platform.runLater(() -> updateFavoritesUI(courses)))
                .exceptionally(ex -> {
                    Platform.runLater(this::showFavoritesError);
                    return null;
                });
    }

    private void refreshMyCourses() {

        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            VBox loggedOut = new VBox(10);
            loggedOut.setAlignment(Pos.CENTER);
            loggedOut.setPadding(new Insets(28));
            Label msg = new Label("سجّل الدخول لعرض كورساتك");
            msg.setStyle("-fx-font-size:13.5px;-fx-text-fill:#475569;");
            Button goLogin = new Button("تسجيل الدخول");
            goLogin.setStyle(primarySolid());
            goLogin.setOnAction(e -> mainController.navigateToLogin());

            myCoursesCard.getChildren().setAll(myCoursesHeaderLabel, loggedOut);
            return;
        }

        myCoursesCard.getChildren().setAll(myCoursesHeaderLabel, myCoursesLoading);

        Long userId = sessionManager.getCurrentUserId();

        CompletableFuture<List<EnrollmentDTO>> fut = apiService.getUserEnrollments(userId);
        fut.thenAccept(enrollments -> Platform.runLater(() -> {
            updateMyCoursesUI(enrollments);  // مرر قائمة التسجيلات مباشرة
        })).exceptionally(ex -> {
            Platform.runLater(this::showMyCoursesError);
            return null;
        });
    }



    private void showMyCoursesError() {
        myCoursesCard.getChildren().setAll(myCoursesHeaderLabel, myCoursesError);
    }

    private void updateMyCoursesUI(List<EnrollmentDTO> enrollments) {
        int count = (enrollments == null) ? 0 : enrollments.size();
        myCoursesHeaderLabel.setText("كورساتي (" + count + ")");

        if (count == 0) {
            myCoursesCard.getChildren().setAll(myCoursesHeaderLabel, myCoursesEmpty);
            return;
        }

        myCoursesGrid.getChildren().clear();
        for (EnrollmentDTO enrollment : enrollments) {
            myCoursesGrid.getChildren().add(buildEnrollmentCourseCard(enrollment, () -> {
                refreshMyCourses();  // إعادة تحميل الكورسات بعد الحذف
            }));
        }

        myCoursesCard.getChildren().setAll(myCoursesHeaderLabel, myCoursesGrid);
    }



    private void showFavoritesLoggedOut() {
        VBox loggedOut = new VBox(10);
        loggedOut.setAlignment(Pos.CENTER);
        loggedOut.setPadding(new Insets(28));
        Label msg = new Label("سجّل الدخول لعرض كورساتك المفضّلة");
        msg.setStyle("-fx-font-size:13.5px;-fx-text-fill:#475569;");
        Button goLogin = new Button("تسجيل الدخول");
        goLogin.setStyle(primarySolid());
        goLogin.setOnAction(e -> mainController.navigateToLogin());

        favoritesHeaderLabel.setText("المفضلة");
        loggedOut.getChildren().addAll(msg, goLogin);
        favoritesCard.getChildren().setAll(favoritesHeaderLabel, loggedOut);
    }

    private void showFavoritesError() {
        favoritesHeaderLabel.setText("المفضلة");
        favoritesCard.getChildren().setAll(favoritesHeaderLabel, favoritesError);
    }

    private void updateFavoritesUI(List<Course> courses) {
        int count = (courses == null) ? 0 : courses.size();
        favoritesHeaderLabel.setText("المفضلة (" + count + ")");

        if (count == 0) {
            favoritesCard.getChildren().setAll(favoritesHeaderLabel, favoritesEmpty);
            return;
        }

        favoritesGrid.getChildren().clear();
        for (Course c : new ArrayList<>(courses)) {
            favoritesGrid.getChildren().add(buildCourseCard(c, () -> {
                // إزالة الكرت محلياً وتحديث العداد
                favoritesGrid.getChildren().removeIf(node -> node.getUserData() == c);
                int newCount = favoritesGrid.getChildren().size();
                favoritesHeaderLabel.setText("المفضلة (" + newCount + ")");
                if (newCount == 0) {
                    favoritesCard.getChildren().setAll(favoritesHeaderLabel, favoritesEmpty);
                }
            }));
        }
        favoritesCard.getChildren().setAll(favoritesHeaderLabel, favoritesGrid);
    }

    /** يبني كرت كورس داخل المفضلة */
    private Node buildCourseCard(Course course, Runnable onRemoved) {
        VBox card = new VBox(10);
        card.setUserData(course);
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(14), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(Color.web("#E2E8F0"),
                BorderStrokeStyle.SOLID, new CornerRadii(14), new BorderWidths(1))));

        // صورة
        String img = (course.getImageUrl() != null && !course.getImageUrl().isBlank())
                ? course.getImageUrl()
                : "https://via.placeholder.com/260x150.png?text=Course";
        ImageView imageView = new ImageView(new Image(img, 260, 150, true, true));
        imageView.setFitWidth(260);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // عنوان + وصف مختصر
        Label title = new Label(safe(course.getTitle()));
        title.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#111827;");
        Label desc = new Label(safe(course.getDescription()));
        desc.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
        desc.setWrapText(true);
        desc.setMaxWidth(236);

        // السعر/مجاني
        String priceText;
        if (course.getPrice() != null && course.getPrice().doubleValue() > 0.0) {
            priceText = String.format("%.2f ر.س", course.getPrice().doubleValue());
        } else {
            priceText = "مجاني";
        }
        Label price = new Label(priceText);
        price.setStyle("-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:#111827;");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button details = new Button("تفاصيل");
        details.setStyle(primaryGhost());
        details.setOnAction(e -> mainController.loadCourseDetailsPage(course));

        Button remove = new Button("إزالة");
        remove.setStyle("-fx-background-color:transparent;-fx-text-fill:#DC2626;-fx-font-weight:700;");
        remove.setOnAction(e -> {
            // نداء حذف من المفضلة ثم تحديث الواجهة
            CompletableFuture<Boolean> fut = apiService.removeFavorite(course.getId());
            fut.thenAccept(ok -> Platform.runLater(() -> {
                        if (Boolean.TRUE.equals(ok)) {
                            onRemoved.run();
                        } else {
                            new Alert(Alert.AlertType.ERROR, "تعذّرت إزالة الكورس من المفضلة.").showAndWait();
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() ->
                                new Alert(Alert.AlertType.ERROR, "خطأ في الاتصال أثناء إزالة المفضلة.").showAndWait());
                        return null;
                    });
        });

        actions.getChildren().addAll(details, remove);

        card.getChildren().addAll(imageView, title, desc, price, actions);
        return card;
    }


    private Node buildEnrollmentCourseCard(EnrollmentDTO enrollment, Runnable onRemoved) {
        Course c = new Course();
        c.setId(enrollment.getCourseId());
        c.setTitle(enrollment.getCourseTitle());
        c.setImageUrl(enrollment.getCourseImage());

        VBox card = new VBox(10);
        card.setUserData(enrollment.getCourseId());
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(14), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(Color.web("#E2E8F0"),
                BorderStrokeStyle.SOLID, new CornerRadii(14), new BorderWidths(1))));

        // صورة الكورس
        ImageView imageView = new ImageView(new Image(c.getImageUrl(), 260, 150, true, true));
        imageView.setFitWidth(260);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // عنوان الكورس
        Label title = new Label(c.getTitle());
        title.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#111827;");

        // السعر
        String priceText = (c.getPrice() != null && c.getPrice().doubleValue() > 0)
                ? String.format("%.2f ر.س", c.getPrice().doubleValue())
                : "مجاني";
        Label priceLabel = new Label("السعر: " + priceText);
        priceLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;");

        // الحالة
        Label statusLabel = new Label("الحالة: " + enrollment.getStatus());
        statusLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;");

        // زر التفاصيل
        Button details = new Button("تفاصيل");
        details.setStyle(primaryGhost());
        details.setOnAction(e -> mainController.loadCourseDetailsPage(c));

        // زر الإزالة
        Button remove = new Button("إزالة");
        remove.setStyle("-fx-background-color:transparent;-fx-text-fill:#DC2626;-fx-font-weight:700;");
        remove.setOnAction(e -> {
            CompletableFuture<Boolean> fut = apiService.deleteEnrollment(enrollment.getId());
            fut.thenAccept(ok -> Platform.runLater(() -> {
                if (Boolean.TRUE.equals(ok)) {
                    onRemoved.run();
                } else {
                    new Alert(Alert.AlertType.ERROR, "تعذر إزالة التسجيل.").showAndWait();
                }
            })).exceptionally(ex -> {
                Platform.runLater(() ->
                        new Alert(Alert.AlertType.ERROR, "خطأ في الاتصال أثناء إزالة التسجيل.").showAndWait());
                return null;
            });
        });

        // شريط الأزرار
        HBox actions = new HBox(10, details, remove);
        actions.setAlignment(Pos.CENTER_LEFT);

        // تجميع كل العناصر في الكرت
        card.getChildren().addAll(imageView, title, priceLabel, statusLabel, actions);
        return card;
    }





    /* ------------------------------- Helpers/Styles ------------------------------- */

    private VBox sectionCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxWidth(1080);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(14), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(Color.web("#E2E8F0"),
                BorderStrokeStyle.SOLID, new CornerRadii(14), new BorderWidths(1))));
        return card;
    }

    private String sectionTitle() {
        return "-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:#0F172A;";
    }

    private String fieldStyle() {
        return "-fx-background-color:#F8FAFC; -fx-border-color:#E2E8F0; -fx-border-radius:10;" +
                "-fx-background-radius:10; -fx-padding:12; -fx-font-size:13px;";
    }

    private String primarySolid() {
        return "-fx-background-color:linear-gradient(to right,#7C3AED,#8B5CF6);" +
                "-fx-text-fill:white;-fx-font-weight:700;-fx-padding:10 16;" +
                "-fx-background-radius:10;";
    }

    private String primaryGhost() {
        return "-fx-background-color:#EEF2FF;-fx-text-fill:#4F46E5;-fx-font-weight:700;" +
                "-fx-padding:8 12;-fx-background-radius:10;-fx-border-color:#C7D2FE;" +
                "-fx-border-radius:10;";
    }

    private String getInitial(String name) {
        String n = (name == null) ? "" : name.trim();
        return n.isEmpty() ? "U" : n.substring(0, 1).toUpperCase();
    }



    private String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
