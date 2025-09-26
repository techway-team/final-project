package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.model.User;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.regex.Pattern;

public class RegisterPageController {

    private final MainController mainController;
    private final ApiService apiService;
    private final SessionManager sessionManager;

    // ==== نفس أسماء المتغيّرات الأصلية (لا تلمس) ====
    private TextField nameField;
    private TextField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Button registerButton;
    private Button loginButton;
    private Label statusLabel;
    private ProgressIndicator loadingIndicator;
    private CheckBox termsCheckBox;

    // ==== فالديشن ====
    private static final Pattern EMAIL_RE =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final PseudoClass PC_ERROR = PseudoClass.getPseudoClass("error");

    public RegisterPageController(MainController mainController, ApiService apiService, SessionManager sessionManager) {
        this.mainController = mainController;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
    }

    public VBox createRegisterPage() {
        // صفحة بخلفية هادئة
        VBox page = new VBox();
        page.getStyleClass().add("auth-page");
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(32, 16, 48, 16));

        // ===== بطاقة التسجيل (نفس روح صفحة الدخول) =====
        Label icon = new Label("📖");
        icon.getStyleClass().add("auth-icon");

        Label title = new Label("إنشاء حساب جديد");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("انضم إلى مجتمع TechWay وابدأ رحلتك");
        subtitle.getStyleClass().add("auth-subtitle");

        VBox header = new VBox(6, icon, title, subtitle);
        header.setAlignment(Pos.CENTER);

        // الحقول + رسائل الأخطاء (مخفية افتراضياً)
        nameField = new TextField();
        nameField.setPromptText("اسمك الكامل");
        nameField.getStyleClass().add("auth-field");
        Label nameErr = errorLabel();

        emailField = new TextField();
        emailField.setPromptText("example@email.com");
        emailField.getStyleClass().add("auth-field");
        Label emailErr = errorLabel();

        passwordField = new PasswordField();
        passwordField.setPromptText("كلمة المرور (6 أحرف على الأقل)");
        passwordField.getStyleClass().add("auth-field");
        Label passErr = errorLabel();

        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("تأكيد كلمة المرور");
        confirmPasswordField.getStyleClass().add("auth-field");
        Label confirmErr = errorLabel();

        // فالديشن كسول (لا يلوّن أحمر إلا بعد تفاعل/submit)
        Runnable vName = attachLazyValidator(nameField, nameErr,
                v -> v != null && v.trim().length() >= 3,
                "الاسم يجب أن يكون 3 أحرف على الأقل");

        Runnable vEmail = attachLazyValidator(emailField, emailErr,
                v -> v != null && EMAIL_RE.matcher(v.trim()).matches(),
                "يرجى إدخال بريد إلكتروني صحيح");

        Runnable vPass = attachLazyValidator(passwordField, passErr,
                v -> v != null && v.trim().length() >= 6,
                "كلمة المرور 6 أحرف على الأقل");

        Runnable vConfirm = attachLazyValidator(confirmPasswordField, confirmErr,
                v -> v != null && v.equals(value(passwordField)),
                "كلمتا المرور غير متطابقتين");

        // صفّ كلمتَي المرور جنب بعض
        VBox leftPassCol = new VBox(6,
                labelSmall("كلمة المرور *"), passwordField, passErr);
        VBox rightConfirmCol = new VBox(6,
                labelSmall("تأكيد كلمة المرور *"), confirmPasswordField, confirmErr);
        HBox passwordsRow = new HBox(12, leftPassCol, rightConfirmCol);
        passwordsRow.setFillHeight(true);

        // الموافقة على الشروط
        termsCheckBox = new CheckBox("أوافق على شروط الخدمة وسياسة الخصوصية");
        termsCheckBox.getStyleClass().add("auth-checkbox");

        // زر التسجيل + حالة/محمل
        registerButton = new Button("إنشاء حساب");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);

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

        // رابط “عندك حساب؟”
        loginButton = new Button("تسجيل الدخول");
        loginButton.getStyleClass().add("link-button");
        loginButton.setOnAction(e -> mainController.navigateToLogin());

        HBox loginRow = new HBox(8, new Label("لديك حساب بالفعل؟"), loginButton);
        loginRow.setAlignment(Pos.CENTER);
        loginRow.getStyleClass().add("auth-links");

        // divider بسيط
        Region divider = new Region();
        divider.getStyleClass().add("auth-divider");
        HBox.setHgrow(divider, Priority.ALWAYS);

        // تجميع النموذج
        VBox form = new VBox(8,
                labelSmall("الاسم الكامل *"), nameField, nameErr,
                labelSmall("البريد الإلكتروني *"), emailField, emailErr,
                passwordsRow,
                termsCheckBox
        );
        form.setFillWidth(true);

        VBox card = new VBox(18, header, form, registerButton, statusBox, divider, loginRow);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20));
        card.setMaxWidth(520);

        // أحداث الإرسال
        Runnable submit = () -> {
            boolean ok =
                    value(nameField).length() >= 3 &&
                            EMAIL_RE.matcher(value(emailField)).matches() &&
                            value(passwordField).length() >= 6 &&
                            value(confirmPasswordField).equals(value(passwordField)) &&
                            termsCheckBox.isSelected();

            // فعّل الفالديشن عند الحاجة
            if (value(nameField).isEmpty() || value(nameField).length() < 3) vName.run();
            if (!EMAIL_RE.matcher(value(emailField)).matches()) vEmail.run();
            if (value(passwordField).length() < 6) vPass.run();
            if (!value(confirmPasswordField).equals(value(passwordField))) vConfirm.run();
            if (!termsCheckBox.isSelected()) {
                showError("يجب الموافقة على الشروط قبل إنشاء الحساب");
            } else {
                clearStatus();
            }

            if (!ok) return;

            performRegistration();   // نفس منطقك الأصلي
        };
        confirmPasswordField.setOnAction(e -> submit.run());
        registerButton.setOnAction(e -> submit.run());

        // Hover لطيف
        registerButton.setOnMouseEntered(e -> registerButton.getStyleClass().add("hover"));
        registerButton.setOnMouseExited (e -> registerButton.getStyleClass().remove("hover"));

        // ترتيب الحقول عند Enter
        nameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());

        VBox pageRoot = new VBox(card);
        pageRoot.setAlignment(Pos.TOP_CENTER);
        return pageRoot;
    }

    // ================== منطق التسجيل (بدون أي تغيير وظيفي) ==================
    private void performRegistration() {
        final String name = value(nameField);
        final String email = value(emailField);
        final String password = value(passwordField);

        showLoading(true);
        clearStatus();

        Task<User> registrationTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                // كما هو: ApiService يرجع CompletableFuture<User>
                return apiService.register(name, email, password).get();
            }

            @Override
            protected void succeeded() {
                showLoading(false);
                User user = getValue();
                if (user != null) {
                    showSuccess("تم إنشاء الحساب بنجاح! يمكنك الآن تسجيل الدخول");
                    Platform.runLater(() -> {
                        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                        mainController.onRegistrationSuccess();
                    });
                } else {
                    showError("فشل في إنشاء الحساب. يرجى المحاولة مرة أخرى.");
                }
            }

            @Override
            protected void failed() {
                showLoading(false);
                Throwable exception = getException();
                String errorMessage = "فشل في إنشاء الحساب";
                if (exception != null) {
                    String em = exception.getMessage() == null ? "" : exception.getMessage();
                    if (em.contains("Email already exists")) {
                        errorMessage = "البريد الإلكتروني مسجّل مسبقاً";
                    } else if (em.toLowerCase().contains("connection")) {
                        errorMessage = "تعذر الاتصال بالخادم. تحقق من الشبكة.";
                    } else {
                        errorMessage = errorMessage + ": " + em;
                    }
                }
                showError(errorMessage);
            }
        };

        new Thread(registrationTask, "register-task").start();
    }

    // ================== Helpers / UI ==================
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

    /** فالديشن كسول: يُظهر الخطأ بعد الكتابة/الblur أو عند submit */
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
        return () -> { // عند الإرسال
            String v = value(field);
            boolean ok = rule.test(v);
            boolean show = !ok;
            err.setText(v.isEmpty() ? "هذا الحقل مطلوب" : ruleMsg);
            err.setVisible(show);
            err.setManaged(show);
            field.pseudoClassStateChanged(PC_ERROR, show);
        };
    }

    private void showLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        registerButton.setDisable(loading);
        loginButton.setDisable(loading);
        nameField.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        termsCheckBox.setDisable(loading);
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
