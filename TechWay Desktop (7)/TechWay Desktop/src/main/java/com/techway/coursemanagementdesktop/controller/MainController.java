package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.model.Quiz;
import com.techway.coursemanagementdesktop.model.QuizAttempt;
import com.techway.coursemanagementdesktop.model.User;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import com.techway.coursemanagementdesktop.util.ViewRouter;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.techway.coursemanagementdesktop.TechWayApplication.getPrimaryStage;


public class MainController {

    /* ====== إعداد للتحكم بحجم الهيرو ====== */
    /** عدّل النسبة للي تبغيه: 0.55 = 55% من ارتفاع النافذة، 0.65 أكبر، 0.70 ضخم */
    private static final double HERO_HEIGHT_RATIO = 0.75;

    /* ====== FXML refs ====== */
    @FXML private BorderPane mainContainer;
    @FXML private VBox headerSection;
    @FXML private HBox navigationBar;
    @FXML private Label appTitle;
    @FXML private Label userWelcome;

    // Auth buttons
    @FXML private Button loginButton;
    @FXML private Button profileButton;
    @FXML private Button logoutButton;

    // Admin button (أضف زر أدمن في الهيدر بالـ FXML: fx:id="adminButton")
    @FXML private Button adminButton;

    // Nav buttons
    @FXML private Button homeButton;
    @FXML private Button coursesButton;
    @FXML private Button aboutButton;
    @FXML private Button contactButton;
    @FXML private Button mapButton;
    private MapController mapController;

    // Hero
    @FXML private StackPane heroSection;
    @FXML private ImageView heroImage;
    @FXML private Rectangle heroOverlay;

    // Hero CTAs
    @FXML private Button heroExploreButton; // "ابدأ الآن"
    @FXML private Button heroAboutButton;   // "من نحن"

    // Scroll / content
    @FXML private ScrollPane contentArea;
    @FXML private VBox mainContent;
    @FXML private VBox contentSlot;

    /* ====== Services/State ====== */
    private SessionManager sessionManager;
    private ApiService apiService;

    /* ====== Sub-controllers ====== */
    private CoursesPageController coursesPageController;
    private LoginPageController loginPageController;
    private RegisterPageController registerPageController;
    private ProfilePageController profilePageController;
    private CourseDetailsPageController courseDetailsPageController;
    private AboutPageController aboutPageController;
    private ContactPageController contactPageController;

    @FXML private FlowPane coursesGrid;

    /* حفظ القالب الأصلي للصفحة الرئيسية (الموجود في FXML) */
    private List<Node> homeTemplateChildren;

    /* للسماح بالوصول السريع لتبديل المحتوى من أي كنترولر */
    private static MainController INSTANCE;

    @FXML private Button certificatesButton; // أضف هذا للـ FXML
    private QuizPageController quizPageController;
    private QuizResultsPageController quizResultsPageController;
    private CertificatePageController certificatePageController;

    /* ====== Init ====== */
    @FXML
    public void initialize() {
        INSTANCE = this;

        sessionManager = SessionManager.getInstance();
        apiService = ApiService.getInstance();

        if (mainContent != null) {
            homeTemplateChildren = new ArrayList<>(mainContent.getChildren());
        }

        setupUI();
        setupBindings();
        setupHeroBindings();   // ربط الهيرو
        setupHeroButtons();    // أزرار الهيرو

        loadHomePage(); // الافتراضي
    }

    private void setupUI() {
        if (headerSection != null && !headerSection.getStyleClass().contains("header-section")) {
            headerSection.getStyleClass().add("header-section");
        }
        if (appTitle != null) {
            if (!appTitle.getStyleClass().contains("header-title")) {
                appTitle.getStyleClass().add("header-title");
            }
            appTitle.setText(" ");
        }

        setupNavigationButtons();

        // مهم: يوجد هذا الميثود في هذا الملف — استدعِه هنا
        updateUIForAuthenticationState();

        if (contentArea != null) contentArea.setFitToWidth(true);

        // إعداد زر الأدمن إن وُجد في الـ FXML
        if (adminButton != null) {
            adminButton.setOnAction(e -> onAdminClicked());
            boolean isAdmin = sessionManager.isAdmin();
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
        }
    }

    private void setupNavigationButtons() {
        String navCls = "nav-button";
        if (homeButton != null)    homeButton.getStyleClass().add(navCls);
        if (coursesButton != null) coursesButton.getStyleClass().add(navCls);
        if (aboutButton != null)   aboutButton.getStyleClass().add(navCls);
        if (contactButton != null) contactButton.getStyleClass().add(navCls);
        if (mapButton != null)     mapButton.getStyleClass().add(navCls);

        if (homeButton != null)    { homeButton.setText("الرئيسية");  homeButton.setOnAction(e -> loadHomePage()); }
        if (coursesButton != null) { coursesButton.setText("الكورسات"); coursesButton.setOnAction(e -> loadCoursesPage()); }
        if (mapButton != null)     { mapButton.setText("الخريطة");     mapButton.setOnAction(e -> loadMapPage()); }
        if (aboutButton != null)   { aboutButton.setText("من نحن");    aboutButton.setOnAction(e -> loadAboutPage()); }
        if (contactButton != null) { contactButton.setText("اتصل بنا"); contactButton.setOnAction(e -> loadContactPage()); }

        // ===== في setupNavigationButtons() أضف هذا =====

        if (certificatesButton != null) {
            certificatesButton.getStyleClass().add(navCls);
            certificatesButton.setText("شهاداتي");
            certificatesButton.setOnAction(e -> loadCertificatesPage());

            // إظهار الزر فقط للمستخدمين المسجلين
            boolean isLoggedIn = sessionManager.isLoggedIn();
            certificatesButton.setVisible(isLoggedIn);
            certificatesButton.setManaged(isLoggedIn);
        }
    }

    private void setupBindings() {
        sessionManager.loggedInProperty().addListener((obs, wasLoggedIn, isLoggedIn) ->
                Platform.runLater(this::updateUIForAuthenticationState));

        sessionManager.currentUserProperty().addListener((obs, oldUser, newUser) ->
                Platform.runLater(() -> updateUserWelcome(newUser)));

        if (loginButton != null)   loginButton.setOnAction(e -> loadLoginPage());
        if (profileButton != null) profileButton.setOnAction(e -> loadProfilePage());
        if (logoutButton != null)  logoutButton.setOnAction(e -> performLogout());

        // لو تغير الدور بعد تسجيل الدخول، حدث زر الأدمن
        sessionManager.roleProperty().addListener((obs, oldRole, newRole) -> Platform.runLater(() -> {
            if (adminButton != null) {
                boolean isAdmin = sessionManager.isAdmin();
                adminButton.setVisible(isAdmin);
                adminButton.setManaged(isAdmin);
            }
        }));
    }

    /** أزرار الهيرو */
    private void setupHeroButtons() {
        if (heroExploreButton != null) {
            heroExploreButton.setOnAction(e -> loadCoursesPage()); // ابدأ الآن -> الكورسات
        }
        if (heroAboutButton != null) {
            heroAboutButton.setOnAction(e -> loadAboutPage());     // من نحن -> صفحة من نحن
        }
    }

    /* ====== ربط الهيرو بشكل ثابت كنسبة من ارتفاع النافذة ====== */

    /** يربط ارتفاع الهيرو مباشرة بنسبة من ارتفاع النافذة */
    private void bindHeroHeightsToScene() {
        if (heroSection == null || heroSection.getScene() == null) return;

        // ألغِ أي ربط قديم ثم اربط من جديد
        heroSection.minHeightProperty().unbind();
        heroSection.prefHeightProperty().unbind();

        heroSection.minHeightProperty().bind(heroSection.getScene().heightProperty().multiply(HERO_HEIGHT_RATIO));
        heroSection.prefHeightProperty().bind(heroSection.getScene().heightProperty().multiply(HERO_HEIGHT_RATIO));
    }

    /** تأكيد جاهزية الهيرو بعد إعادة إدراجه */
    private void ensureHeroReady() {
        if (heroSection == null) return;
        bindHeroHeightsToScene();  // مهم جداً بعد استرجاع الصفحة
        heroSection.applyCss();
        heroSection.requestLayout();
    }

    /** إعداد ربط الصورة/الطبقة والحجم */
    private void setupHeroBindings() {
        if (heroSection == null) return;

        // فكّ أي قيود قديمة جايه من الـFXML
        heroSection.minHeightProperty().unbind();
        heroSection.prefHeightProperty().unbind();
        heroSection.maxHeightProperty().unbind();

        heroSection.setMinHeight(Region.USE_COMPUTED_SIZE);
        heroSection.setPrefHeight(Region.USE_COMPUTED_SIZE);
        heroSection.setMaxHeight(Double.MAX_VALUE); // السماح بالتمدد

        // أربط عند تغيّر الـScene
        heroSection.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) bindHeroHeightsToScene();
        });

        // ولو الـScene موجود فعلاً الآن (عادة عند الرجوع)، اربط فوراً
        Platform.runLater(this::bindHeroHeightsToScene);

        // الصورة تملأ الهيرو
        if (heroImage != null) {
            heroImage.setPreserveRatio(false);
            heroImage.fitWidthProperty().bind(heroSection.widthProperty());
            heroImage.fitHeightProperty().bind(heroSection.heightProperty());
        }

        // طبقة التلوين تغطي الهيرو
        if (heroOverlay != null) {
            heroOverlay.widthProperty().bind(heroSection.widthProperty());
            heroOverlay.heightProperty().bind(heroSection.heightProperty());
        }

        // قصّ الحواف
        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        clip.widthProperty().bind(heroSection.widthProperty());
        clip.heightProperty().bind(heroSection.heightProperty());
        heroSection.setClip(clip);
    }

    /* ====== Auth UI ====== */

    private void updateUIForAuthenticationState() {
        boolean isLoggedIn = sessionManager.isLoggedIn();

        if (loginButton != null)   { loginButton.setVisible(!isLoggedIn); loginButton.setManaged(!isLoggedIn); }
        if (profileButton != null) { profileButton.setVisible(isLoggedIn); profileButton.setManaged(isLoggedIn); }
        if (logoutButton != null)  { logoutButton.setVisible(isLoggedIn);  logoutButton.setManaged(isLoggedIn);  }
        if (userWelcome != null)   { userWelcome.setVisible(isLoggedIn);   userWelcome.setManaged(isLoggedIn);   }
        // ===== في updateUIForAuthenticationState() أضف هذا =====

// إظهار/إخفاء زر الشهادات حسب حالة تسجيل الدخول
        if (certificatesButton != null) {
            certificatesButton.setVisible(isLoggedIn);
            certificatesButton.setManaged(isLoggedIn);
        }

        if (adminButton != null) {
            boolean isAdmin = sessionManager.isAdmin();
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
        }

        if (isLoggedIn) updateUserWelcome(sessionManager.getCurrentUser());
    }

    private void updateUserWelcome(User user) {
        if (user != null && userWelcome != null) {
            if (!userWelcome.getStyleClass().contains("header-subtitle")) {
                userWelcome.getStyleClass().add("header-subtitle");
            }
            userWelcome.setText("مرحباً، " + user.getName());
        }
    }

    /* ========================= NAV / CONTENT ========================= */

    private void setMainContent(Node content) {
        if (content == null) return;

        content.setOpacity(0);

        if (contentSlot != null) {
            contentSlot.getChildren().setAll(content);
        } else if (mainContent != null) {
            mainContent.getChildren().setAll(content);
        }

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), content);
        fadeIn.setToValue(1);
        fadeIn.play();

        if (contentArea != null) {
            Platform.runLater(() -> contentArea.setVvalue(0));
        }
    }

    private void restoreHomeTemplate() {
        if (mainContent != null && homeTemplateChildren != null && !homeTemplateChildren.isEmpty()) {
            mainContent.getChildren().setAll(homeTemplateChildren);
        }
        // تأكيد الربط بعد الاسترجاع (هذا كان سبب تغيّر الارتفاع عند الرجوع)
        setupHeroBindings();
        setupHeroButtons();
        Platform.runLater(this::ensureHeroReady);
    }

    private void setActiveNavButton(Button activeButton) {
        clearActiveNavButton();
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void clearActiveNavButton() {
        if (homeButton != null)    homeButton.getStyleClass().remove("active");
        if (coursesButton != null) coursesButton.getStyleClass().remove("active");
        if (aboutButton != null)   aboutButton.getStyleClass().remove("active");
        if (contactButton != null) contactButton.getStyleClass().remove("active");
        if (profileButton != null) profileButton.getStyleClass().remove("active");
        if (adminButton != null)   adminButton.getStyleClass().remove("active");
        // ===== في clearActiveNavButton() أضف هذا =====

        if (certificatesButton != null) certificatesButton.getStyleClass().remove("active");
    }

    /* ========================= PAGE LOADERS ========================= */

    private void loadHomePage() {
        try {
            restoreHomeTemplate();
            setActiveNavButton(homeButton);
            updateStatus("مرحباً بك في TechWay");
        } catch (Exception e) {
            showError("فشل في تحميل الصفحة الرئيسية: " + e.getMessage());
        }
    }

    private void loadCoursesPage() {
        try {
            if (coursesPageController == null) {
                coursesPageController = new CoursesPageController(this, apiService, sessionManager);
            }
            VBox coursesContent = coursesPageController.createCoursesPage();
            setMainContent(coursesContent);
            setActiveNavButton(coursesButton);
        } catch (Exception e) {
            showError("فشل في تحميل صفحة الكورسات: " + e.getMessage());
        }
    }

    // Map page loader - جديد
    private void loadMapPage() {
        try {
            if (mapController == null) {
                mapController = new MapController(this, apiService, sessionManager);
            }
            VBox mapContent = mapController.createMapPage();
            setMainContent(mapContent);
            setActiveNavButton(mapButton);
            updateStatus("خريطة الكورسات التفاعلية");
        } catch (Exception e) {
            showError("فشل في تحميل الخريطة: " + e.getMessage());
        }
    }
    private void loadLoginPage() {
        try {
            if (loginPageController == null) {
                loginPageController = new LoginPageController(this, apiService, sessionManager);
            }
            VBox loginContent = loginPageController.createLoginPage();
            setMainContent(loginContent);
            clearActiveNavButton();
            updateStatus("تسجيل الدخول");
        } catch (Exception e) {
            showError("فشل في تحميل صفحة تسجيل الدخول: " + e.getMessage());
        }
    }

    private void loadRegisterPage() {
        try {
            if (registerPageController == null) {
                registerPageController = new RegisterPageController(this, apiService, sessionManager);
            }
            VBox registerContent = registerPageController.createRegisterPage();
            setMainContent(registerContent);
            clearActiveNavButton();
            updateStatus("إنشاء حساب جديد");
        } catch (Exception e) {
            showError("فشل في تحميل صفحة التسجيل: " + e.getMessage());
        }
    }

    private void loadProfilePage() {
        try {
            if (profilePageController == null) {
                profilePageController = new ProfilePageController(this, apiService, sessionManager);
            }
            VBox profileContent = profilePageController.createProfilePage();
            setMainContent(profileContent);
            clearActiveNavButton();
            updateStatus("الملف الشخصي");
        } catch (Exception e) {
            showError("فشل في تحميل الملف الشخصي: " + e.getMessage());
        }
    }

    private void loadAboutPage() {
        try {
            if (aboutPageController == null) {
                aboutPageController = new AboutPageController(this);
            }
            VBox aboutContent = aboutPageController.createAboutPage();
            setMainContent(aboutContent);
            setActiveNavButton(aboutButton);
            updateStatus("معلومات عن TechWay");
        } catch (Exception e) {
            showError("فشل في تحميل صفحة من نحن: " + e.getMessage());
        }
    }

    private void loadContactPage() {
        try {
            if (contactPageController == null) {
                contactPageController = new ContactPageController(this);
            }
            VBox contactContent = contactPageController.createContactPage();
            setMainContent(contactContent);
            setActiveNavButton(contactButton);
            updateStatus("تواصل معنا");
        } catch (Exception e) {
            showError("فشل في تحميل صفحة اتصل بنا: " + e.getMessage());
        }
    }

    /* ========================= COURSE DETAILS ========================= */

    public void loadCourseDetailsPage(Course course) {
        try {
            if (courseDetailsPageController == null) {
                courseDetailsPageController = new CourseDetailsPageController(this, sessionManager,course);
            }
            VBox courseDetailsContent = courseDetailsPageController.createCourseDetailsPage(course);
            setMainContent(courseDetailsContent);
            clearActiveNavButton();
            updateStatus("تفاصيل الكورس - " + course.getTitle());
        } catch (Exception e) {
            showError("فشل في تحميل تفاصيل الكورس: " + e.getMessage());
        }
    }

    public void loadCourseDetails(Long courseId) {
        apiService.getCourseById(courseId).thenAccept(course -> {
            Platform.runLater(() -> {
                if (course != null) {
                    loadCourseDetailsPage(course);
                } else {
                    showError("لم يتم العثور على الكورس.");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> showError("فشل تحميل الكورس: " + ex.getMessage()));
            return null;
        });
    }


    /* ========================= ADMIN ========================= */

    /** استدعاء عند الضغط على زر الأدمن في الهيدر */
    @FXML
    private void onAdminClicked() {
        if (!sessionManager.isAdmin()) {
            new Alert(Alert.AlertType.ERROR, "هذه المنطقة للأدمن فقط").showAndWait();
            return;
        }
        try {
            Node adminRoot = ViewRouter.load("/fxml/admin/AdminDashboard.fxml");
            setContent(adminRoot); // نفس النافذة
            clearActiveNavButton();
            if (adminButton != null) adminButton.getStyleClass().add("active");
            updateStatus("لوحة التحكم - أدمن");
        } catch (Exception ex) {
            showError("تعذر تحميل لوحة الأدمن: " + ex.getMessage());
        }
    }

    /** يتيح لأي كنترولر تبديل المحتوى داخل نفس النافذة (static helper) */
    public static void setCenterStatic(Node node) {
        if (INSTANCE != null) {
            INSTANCE.setContent(node);
        }
    }

    /** public للوصول من الكنترولرز الأخرى */
    public void setContent(Node node) {
        setMainContent(node);
    }

    /* ========================= Status / Errors ========================= */

    public void showLoadingInMainArea(boolean loading) {
        if (!loading) return;
        VBox loadingContent = new VBox(20);
        loadingContent.setAlignment(Pos.CENTER);
        loadingContent.setPadding(new Insets(100));

        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(50, 50);

        Label loadingLabel = new Label("جاري التحميل...");
        loadingLabel.getStyleClass().add("course-instructor");

        loadingContent.getChildren().addAll(loadingIndicator, loadingLabel);
        setMainContent(loadingContent);
    }

    public void updateStatus(String message) {
        System.out.println("Status: " + message);
    }

    public void showError(String message) {
        VBox errorContent = new VBox(20);
        errorContent.setAlignment(Pos.CENTER);
        errorContent.setPadding(new Insets(100));

        Label errorIcon = new Label("⚠️");
        errorIcon.getStyleClass().add("stats-number");

        Label errorLabel = new Label("حدث خطأ");
        errorLabel.getStyleClass().add("course-title");

        Label errorMessage = new Label(message);
        errorMessage.getStyleClass().add("error-message");
        errorMessage.setWrapText(true);

        Button retryButton = new Button("إعادة المحاولة");
        retryButton.getStyleClass().add("secondary-button");
        retryButton.setOnAction(e -> loadHomePage());

        errorContent.getChildren().addAll(errorIcon, errorLabel, errorMessage, retryButton);
        setMainContent(errorContent);

        System.err.println("Error: " + message);
    }

    /* ========================= Public navigation helpers ========================= */

    public void onLoginSuccess() {
        updateStatus("مرحباً " + sessionManager.getCurrentUser().getName() + "! تم تسجيل الدخول بنجاح");
        loadHomePage();
    }

    public void onRegistrationSuccess() {
        updateStatus("تم إنشاء الحساب بنجاح! يمكنك الآن تسجيل الدخول");
        loadLoginPage();
    }

    public void navigateToRegister() { loadRegisterPage(); }
    public void navigateToLogin()    { loadLoginPage(); }
    public void navigateToHome()     { loadHomePage(); }
    public void navigateToCourses()  { loadCoursesPage(); }

    private void performLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تسجيل الخروج");
        alert.setHeaderText("هل أنت متأكد من تسجيل الخروج؟");
        alert.setContentText("سيتم إنهاء جلستك الحالية وستحتاج لتسجيل الدخول مرة أخرى للوصول للميزات المتقدمة.");

        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/css/main-styles.css").toExternalForm());
        } catch (Exception ignore) { /* لو ما حصل الملف ما نكسر */ }

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (okButton != null)   okButton.setText("تسجيل الخروج");
        if (cancelButton != null) cancelButton.setText("إلغاء");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SessionManager.logout();
                updateStatus("تم تسجيل الخروج بنجاح. نراك قريباً!");
                loadHomePage();
            }
        });
    }

    public void cleanup() {
        System.out.println("MainController cleanup completed");
        if (quizPageController != null) {
            // تنظيف موارد الكويز
        }
        if (quizResultsPageController != null) {
            // تنظيف موارد النتائج
        }
        if (certificatePageController != null) {
            // تنظيف موارد الشهادات
        }
    }

    // (قديمة؛ تُستخدم في جزء غير ظاهر – أبقيتها كما هي)
    public void showCoursesPage() {
        if (contentSlot == null || coursesGrid == null) return;

        // شاشة تحميل بسيطة
        contentSlot.getChildren().clear();
        coursesGrid.getChildren().clear();
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(60));
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(50, 50);
        loadingBox.getChildren().addAll(pi, new Label("جاري تحميل الكورسات..."));
        contentSlot.getChildren().add(loadingBox);

        ApiService.getInstance().getAllCourses()
                .thenAccept(courses -> Platform.runLater(() -> {
                    contentSlot.getChildren().clear();
                    coursesGrid.getChildren().clear();

                    if (courses == null || courses.isEmpty()) {
                        Label empty = new Label("لا يوجد كورسات حالياً");
                        empty.getStyleClass().add("auth-subtitle");
                        VBox emptyBox = new VBox(12, new Label("📭"), empty);
                        emptyBox.setAlignment(Pos.CENTER);
                        emptyBox.setPadding(new Insets(60));
                        contentSlot.getChildren().add(emptyBox);
                        return;
                    }

                    for (Course course : courses) {
                        String title = course.getTitle();
                        String description = course.getDescription();
                        String imageUrl = course.getImageUrl();

                        ImageView imageView;
                        if (imageUrl != null && !imageUrl.isBlank()) {
                            imageView = new ImageView(new Image(imageUrl, 220, 140, true, true));
                        } else {
                            imageView = new ImageView(new Image(
                                    "https://via.placeholder.com/220x140.png?text=Course+Image",
                                    220, 140, true, true));
                        }

                        Label titleLabel = new Label(title);
                        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                        Label descLabel = new Label(description);
                        descLabel.setWrapText(true);
                        descLabel.setStyle("-fx-text-fill: #555;");

                        Button detailsBtn = new Button("عرض التفاصيل");
                        detailsBtn.getStyleClass().add("primary-button");
                        detailsBtn.setOnAction(e -> loadCourseDetailsPage(course));

                        VBox card = new VBox(10, imageView, titleLabel, descLabel, detailsBtn);
                        card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 5, 0, 2, 2);");
                        card.setPrefWidth(220);

                        coursesGrid.getChildren().add(card);
                    }

                    contentSlot.getChildren().add(coursesGrid);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("فشل تحميل الكورسات: " + (ex.getMessage() == null ? "" : ex.getMessage())));
                    return null;
                });
    }

    public void navigateToPaymentPage(Course course, Runnable onPaymentSuccess) {
        PaymentPageController paymentController = new PaymentPageController(course, onPaymentSuccess);
        Scene paymentScene = new Scene(paymentController.createPaymentPage(course, sessionManager), 600, 400);
        Stage stage = new Stage();
        stage.setTitle("صفحة الدفع");
        stage.setScene(paymentScene);
        stage.show();
    }

    public void openCourseContent(Course course) {
        loadCourseDetailsPage(course);
    }






    // ===== إضافة Methods الجديدة =====

    /**
     * تحميل صفحة الشهادات
     */
    private void loadCertificatesPage() {
        try {
            if (!sessionManager.isLoggedIn()) {
                loadLoginPage();
                return;
            }

            if (certificatePageController == null) {
                certificatePageController = new CertificatePageController(this);
            }
            VBox certificatesContent = certificatePageController.createCertificatesPage();
            setMainContent(certificatesContent);
            setActiveNavButton(certificatesButton);
            updateStatus("شهاداتي");
        } catch (Exception e) {
            showError("فشل في تحميل صفحة الشهادات: " + e.getMessage());
        }
    }

    /**
     * فتح صفحة الكويز للكورس (محدث)
     */
    public void openQuizPage(Long courseId) {
        try {
            if (!sessionManager.isLoggedIn()) {
                new Alert(Alert.AlertType.WARNING, "يرجى تسجيل الدخول أولاً لحل الكويز").showAndWait();
                loadLoginPage();
                return;
            }

            Long userId = sessionManager.getCurrentUserId();

            if (quizPageController == null) {
                quizPageController = new QuizPageController();
            }


            quizPageController.initData(courseId, userId, this);
            VBox quizContent = quizPageController.createQuizPage();
            setMainContent(quizContent);
            clearActiveNavButton();
            updateStatus("حل الكويز");

        } catch (Exception e) {
            showError("فشل في تحميل صفحة الكويز: " + e.getMessage());
        }
    }

    /**
     * عرض نتائج الكويز (جديد)
     */
    public void showQuizResults(Quiz quiz, QuizAttempt attempt, Long courseId, Map<String, Object> resultData) {
        try {
            if (quizResultsPageController == null) {
                quizResultsPageController = new QuizResultsPageController(this);
            }

            VBox resultsContent = quizResultsPageController.createResultsPage(quiz, attempt, courseId, resultData);
            setMainContent(resultsContent);
            clearActiveNavButton();
            updateStatus("نتائج الكويز - " + quiz.getTitle());

        } catch (Exception e) {
            showError("فشل في عرض نتائج الكويز: " + e.getMessage());
        }
    }

    /**
     * فتح صفحة الشهادات من أي مكان
     */
    public void openCertificatesPage() {
        loadCertificatesPage();
    }

    /**
     * التحقق من وجود كويز للكورس وإظهار زر البدء
     */
    public void checkCourseQuiz(Long courseId, Button quizButton) {
        apiService.getQuizByCourseId(courseId)
                .thenAccept(quiz -> {
                    Platform.runLater(() -> {
                        if (quiz != null) {
                            quizButton.setVisible(true);
                            quizButton.setDisable(false);
                            quizButton.setText("ابدأ الكويز");
                            quizButton.setOnAction(e -> openQuizPage(courseId));
                        } else {
                            quizButton.setVisible(false);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        quizButton.setVisible(false);
                    });
                    return null;
                });
    }

    /**
     * التحقق من وجود شهادة للمستخدم في الكورس
     */
    public void checkUserCertificate(Long courseId, Button certificateButton) {
        if (!sessionManager.isLoggedIn()) {
            certificateButton.setVisible(false);
            return;
        }

        Long userId = sessionManager.getCurrentUserId();
        apiService.getUserCourseCertificate(userId, courseId)
                .thenAccept(optionalCert -> {
                    Platform.runLater(() -> {
                        if (optionalCert.isPresent()) {
                            certificateButton.setVisible(true);
                            certificateButton.setDisable(false);
                            certificateButton.setText("عرض الشهادة");
                            certificateButton.setOnAction(e -> {
                                // فتح الشهادة في المتصفح
                                apiService.openCertificateInBrowser(optionalCert.get().getId());
                            });
                        } else {
                            certificateButton.setVisible(false);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        certificateButton.setVisible(false);
                    });
                    return null;
                });
    }



    public void loadQuizPage(Long courseId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/quiz_taking_view.fxml"));
            VBox quizPage = loader.load();

            QuizPageController controller = loader.getController();
            controller.initData(courseId, sessionManager.getCurrentUser().getId(), this);

            setMainContent(quizPage); // دالة لتغيير محتوى الصفحة الرئيسية
            updateStatus("الاختبار - الدورة رقم " + courseId);
            clearActiveNavButton(); // إن وجدت
        } catch (IOException e) {
            showError("فشل في تحميل صفحة الاختبار: " + e.getMessage());
        }
    }





}
