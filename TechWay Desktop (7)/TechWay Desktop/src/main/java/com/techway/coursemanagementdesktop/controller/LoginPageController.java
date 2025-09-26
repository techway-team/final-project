package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.dto.LoginResponse;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class LoginPageController {

    private final MainController mainController;
    private final ApiService apiService;
    private final SessionManager sessionManager;

    // ========== نفس أسماء الحقول الأصلية ==========
    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button registerButton;
    private Label statusLabel;
    private ProgressIndicator loadingIndicator;
    private Hyperlink forgotPasswordLink;

    // وضع المدير (جديد)
    private final BooleanProperty adminMode = new SimpleBooleanProperty(false);

    // ========== فالديشن ==========
    private static final Pattern EMAIL_RE =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final PseudoClass PC_ERROR = PseudoClass.getPseudoClass("error");

    public LoginPageController(MainController mainController, ApiService apiService, SessionManager sessionManager) {
        this.mainController = mainController;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
    }

    public VBox createLoginPage() {
        // صفحة متمركزة بخلفية خفيفة
        VBox page = new VBox();
        page.getStyleClass().add("auth-page");
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(32, 16, 48, 16));

        // ===== الهيدر =====
        Label icon = new Label("📚");
        icon.getStyleClass().add("auth-icon");

        Label title = new Label("مرحباً بعودتك");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("سجل دخولك للوصول لحسابك");
        subtitle.getStyleClass().add("auth-subtitle");

        VBox header = new VBox(6, icon, title, subtitle);
        header.setAlignment(Pos.CENTER);

        // ===== سويتش وضع الدخول (عادي / مدير) =====
        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton normalBtn = new ToggleButton("تسجيل عادي");
        ToggleButton adminBtn  = new ToggleButton("تسجيل مدير");


        normalBtn.setToggleGroup(modeGroup);
        adminBtn.setToggleGroup(modeGroup);
        normalBtn.setSelected(true);

        normalBtn.getStyleClass().addAll("seg-button");
        adminBtn.getStyleClass().addAll("seg-button");

        HBox modeSwitch = new HBox(8, normalBtn, adminBtn);
        modeSwitch.setAlignment(Pos.CENTER);

        // اربط الحالة
        modeGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            boolean isAdmin = nv == adminBtn;
            adminMode.set(isAdmin);
        });

        // ===== بانر تنبيه عند وضع المدير =====
        Label adminBanner = new Label("تسجيل دخول كمدير: يحتاج صلاحيات خاصة");
        adminBanner.getStyleClass().addAll("warning-banner");
        adminBanner.setVisible(false);
        adminBanner.setManaged(false);
        adminMode.addListener((o, a, b) -> {
            adminBanner.setVisible(b);
            adminBanner.setManaged(b);
        });

        // ===== الحقول =====
        emailField = new TextField();
        emailField.setPromptText("example@email.com");
        emailField.getStyleClass().add("auth-field");

        Label emailErr = errorLabel();

        passwordField = new PasswordField();
        passwordField.setPromptText("••••••");
        passwordField.getStyleClass().add("auth-field");

        Label passErr = errorLabel();

        // فالديشن كسول
        Runnable validateEmail = attachLazyValidator(emailField, emailErr,
                v -> v != null && EMAIL_RE.matcher(v.trim()).matches(),
                "يرجى إدخال بريد إلكتروني صحيح");

        Runnable validatePass = attachLazyValidator(passwordField, passErr,
                v -> v != null && v.trim().length() >= 6,
                "كلمة المرور 6 أحرف على الأقل");

        // ===== زر الدخول =====
        loginButton = new Button("تسجيل الدخول");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        // غيّر نص الزر حسب وضع المدير
        adminMode.addListener((o, a, b) -> loginButton.setText(b ? "دخول كمدير" : "تسجيل الدخول"));

        // محمل + حالة
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(26, 26);

        statusLabel = new Label();
        statusLabel.getStyleClass().add("auth-status");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        HBox statusBox = new HBox(10, loadingIndicator, statusLabel);
        statusBox.setAlignment(Pos.CENTER);

        // روابط
        forgotPasswordLink = new Hyperlink("نسيت كلمة المرور؟");
        forgotPasswordLink.getStyleClass().add("auth-link");
        forgotPasswordLink.setOnAction(e -> showForgotPasswordDialog());

        registerButton = new Button("إنشاء حساب");
        registerButton.getStyleClass().add("link-button");
        registerButton.setOnAction(e -> mainController.navigateToRegister());

        HBox links = new HBox(16, forgotPasswordLink, registerButton);
        links.setAlignment(Pos.CENTER);
        links.getStyleClass().add("auth-links");

        // ===== بطاقة تجربة للمطور (تظهر في وضع المدير) =====
        Label devTitle   = new Label();
        Label devEmail   = new Label();
        Label devPass    = new Label();
        Label devNotice  = new Label();
        devTitle.getStyleClass().add("auth-subtitle");

        VBox devCard = new VBox(4, devTitle, devEmail, devPass, devNotice);
        devCard.getStyleClass().add("dev-card");
        devCard.setVisible(false);
        devCard.setManaged(false);
        adminMode.addListener((o, a, b) -> {
            devCard.setVisible(b);
            devCard.setManaged(b);
        });

        // ===== الفورم =====
        VBox form = new VBox(
                adminBanner,
                labelSmall("البريد الإلكتروني *"), emailField, emailErr,
                labelSmall("كلمة المرور *"), passwordField, passErr
        );
        form.setSpacing(8);
        form.setFillWidth(true);

        Region divider = new Region();
        divider.getStyleClass().add("auth-divider");
        HBox.setHgrow(divider, Priority.ALWAYS);

        VBox card = new VBox(18, header, modeSwitch, form, loginButton, statusBox, devCard, divider, links);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20));
        card.setMaxWidth(520);

        // أحداث الإرسال
        Runnable submit = () -> {
            boolean okEmail = EMAIL_RE.matcher(value(emailField)).matches();
            boolean okPass  = value(passwordField).length() >= 6;

            if (!okEmail) validateEmail.run();
            if (!okPass)  validatePass.run();
            if (!okEmail || !okPass) return;

            performLogin(value(emailField), value(passwordField), adminMode.get());
        };
        passwordField.setOnAction(e -> submit.run());
        loginButton.setOnAction(e -> submit.run());

        // Hover لطيف على الزر
        loginButton.setOnMouseEntered(e -> loginButton.getStyleClass().add("hover"));
        loginButton.setOnMouseExited (e -> loginButton.getStyleClass().remove("hover"));

        page.getChildren().add(card);
        return page;
    }

    // ====== منطق النداء للسيرفر (ApiService + SessionManager) ======
    private void performLogin(String email, String password, boolean adminModeAtSubmit) {
        showLoading(true);
        clearStatus();

        Task<LoginResponse> loginTask = new Task<>() {
            @Override
            protected LoginResponse call() throws Exception {
                CompletableFuture<LoginResponse> f = apiService.login(email, password);
                return f.get(); // نعمل بلوك هنا (داخل خيط الخلفية)
            }

            @Override
            protected void succeeded() {
                showLoading(false);
                LoginResponse response = getValue();

                if (response != null && response.getUser() != null) {
                    // خزّن المستخدم + التوكن
                    sessionManager.login(response.getUser(), response.getToken());

                    // لو وضع المدير مفعّل، نتحقق من الدور
                    if (adminModeAtSubmit && !sessionManager.isAdmin()) {
                        SessionManager.logout();
                        showError("هذا الحساب ليس مديرًا. فعّل تسجيل عادي أو استخدم حساب مدير.");
                        return;
                    }

                    showSuccess("تم تسجيل الدخول بنجاح!");
                    PauseTransition pause = new PauseTransition(Duration.millis(500));
                    pause.setOnFinished(ev -> mainController.onLoginSuccess());
                    pause.play();
                } else {
                    showError("فشل تسجيل الدخول. حاول مرة أخرى.");
                }
            }

            @Override
            protected void failed() {
                showLoading(false);
                Throwable ex = getException();
                String msg = "فشل تسجيل الدخول";
                if (ex != null) {
                    String em = ex.getMessage() == null ? "" : ex.getMessage();
                    String emLower = em.toLowerCase();
                    if (em.contains("Invalid email or password")) {
                        msg = "بيانات الدخول غير صحيحة";
                    } else if (emLower.contains("connection") || emLower.contains("connect") || emLower.contains("timeout")) {
                        msg = "تعذر الاتصال. تحقق من الشبكة.";
                    } else {
                        msg = msg + ": " + em;
                    }
                }
                showError(msg);
            }
        };

        Thread t = new Thread(loginTask, "login-task");
        t.setDaemon(true);
        t.start();
    }

    // ====== Helpers ======
    private static String value(TextInputControl c) {
        String v = c.getText();
        return v == null ? "" : v.trim();
    }

    private Label errorLabel() {
        Label l = new Label();
        l.getStyleClass().add("field-error");
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    private Label labelSmall(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("auth-subtitle");
        return l;
    }

    /** فالديشن كسول: لا يلوّن أحمر إلا بعد كتابة/blur أو عند submit */
    private Runnable attachLazyValidator(TextInputControl field, Label err,
                                         java.util.function.Predicate<String> rule, String ruleMsg) {
        final boolean[] dirty = {false};
        Runnable update = () -> {
            String v = value(field);
            boolean hasText = !v.isEmpty();
            boolean ok = rule.test(v);
            boolean show = dirty[0] && hasText && !ok;
            field.pseudoClassStateChanged(PC_ERROR, show);
            err.setText(show ? ruleMsg : "");
            err.setVisible(show);
            err.setManaged(show);
        };
        field.textProperty().addListener((o, ov, nv) -> { dirty[0] = true; update.run(); });
        field.focusedProperty().addListener((o, was, now) -> { if (!now) { dirty[0] = true; update.run(); } });
        return () -> { // عند الإرسال: إن خطأ أو فاضي
            String v = value(field);
            boolean ok = rule.test(v);
            boolean show = !ok;
            err.setText(v.isEmpty() ? "هذا الحقل مطلوب" : ruleMsg);
            err.setVisible(show);
            err.setManaged(show);
            field.pseudoClassStateChanged(PC_ERROR, show);
        };
    }

    private void showForgotPasswordDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("استعادة كلمة المرور");
        alert.setHeaderText("إعادة تعيين كلمة المرور");
        alert.setContentText(
                "لإعادة تعيين كلمة المرور تواصل معنا:\n\n" +
                        "📧 support@techway.com\n" +
                        "📞 +966 123 456 789\n\n" +
                        "سنرسل لك التعليمات على بريدك الإلكتروني."
        );
        alert.showAndWait();
    }

    private void showLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        if (loginButton != null)   loginButton.setDisable(loading);
        if (registerButton != null)registerButton.setDisable(loading);
        if (emailField != null)    emailField.setDisable(loading);
        if (passwordField != null) passwordField.setDisable(loading);
    }

    private void showError(String message) {
        statusLabel.getStyleClass().removeAll("status-success");
        statusLabel.getStyleClass().add("status-error");
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        statusLabel.getStyleClass().removeAll("status-error");
        statusLabel.getStyleClass().add("status-success");
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
    }
}
