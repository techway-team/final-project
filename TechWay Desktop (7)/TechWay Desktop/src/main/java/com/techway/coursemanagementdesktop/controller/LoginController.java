package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.dto.LoginResponse;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for Login Dialog
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Hyperlink forgotPasswordLink;

    private ApiService apiService;
    private SessionManager sessionManager;
    private Runnable onLoginSuccess;

    @FXML
    public void initialize() {
        apiService = ApiService.getInstance();
        sessionManager = SessionManager.getInstance();

        setupUI();
        setupValidation();
        setupEventHandlers();
    }

    private void setupUI() {
        // Initially hide loading indicator
        loadingIndicator.setVisible(false);
        statusLabel.setText("");

        // Set placeholder text
        emailField.setPromptText("البريد الإلكتروني");
        passwordField.setPromptText("كلمة المرور");

        // Style buttons
        loginButton.getStyleClass().add("primary-button");
        registerButton.getStyleClass().add("secondary-button");

        // Set button text
        loginButton.setText("تسجيل الدخول");
        registerButton.setText("إنشاء حساب جديد");
        forgotPasswordLink.setText("نسيت كلمة المرور؟");
    }

    private void setupValidation() {
        // Real-time validation
        emailField.textProperty().addListener((obs, oldText, newText) -> {
            clearStatus();
            validateForm();
        });

        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            clearStatus();
            validateForm();
        });
    }

    private void setupEventHandlers() {
        loginButton.setOnAction(e -> performLogin());
        registerButton.setOnAction(e -> showRegisterDialog());
        forgotPasswordLink.setOnAction(e -> showForgotPasswordDialog());

        // Allow Enter key to submit
        passwordField.setOnAction(e -> {
            if (isFormValid()) {
                performLogin();
            }
        });
    }

    private void validateForm() {
        boolean isValid = isFormValid();
        loginButton.setDisable(!isValid);
    }

    private boolean isFormValid() {
        return !emailField.getText().trim().isEmpty() &&
                !passwordField.getText().trim().isEmpty() &&
                isValidEmail(emailField.getText().trim());
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void performLogin() {
        if (!isFormValid()) {
            showError("يرجى إدخال بريد إلكتروني صالح وكلمة المرور");
            return;
        }

        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        showLoading(true);
        clearStatus();

        Task<LoginResponse> loginTask = new Task<LoginResponse>() {
            @Override
            protected LoginResponse call() throws Exception {
                return apiService.login(email, password).get();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    showLoading(false);
                    LoginResponse response = getValue();

                    if (response != null && response.getUser() != null) {
                        // Login successful
                        sessionManager.login(response.getUser(), response.getToken());
                        showSuccess("تم تسجيل الدخول بنجاح!");

                        // Close dialog after brief delay
                        Platform.runLater(() -> {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {}

                            if (onLoginSuccess != null) {
                                onLoginSuccess.run();
                            }
                        });
                    } else {
                        showError("فشل في تسجيل الدخول. يرجى المحاولة مرة أخرى.");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showLoading(false);
                    Throwable exception = getException();
                    String errorMessage = "فشل في تسجيل الدخول";

                    if (exception != null) {
                        String exceptionMessage = exception.getMessage();
                        if (exceptionMessage.contains("Invalid email or password")) {
                            errorMessage = "البريد الإلكتروني أو كلمة المرور غير صحيح";
                        } else if (exceptionMessage.contains("connection")) {
                            errorMessage = "فشل في الاتصال بالخادم. يرجى التحقق من الاتصال بالإنترنت";
                        } else {
                            errorMessage += ": " + exceptionMessage;
                        }
                    }

                    showError(errorMessage);
                });
            }
        };

        new Thread(loginTask).start();
    }

    private void showRegisterDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("إنشاء حساب جديد");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 450, 600));

            // Apply styling
            stage.getScene().getStylesheets().add(getClass().getResource("/css/main-styles.css").toExternalForm());

            RegisterController controller = loader.getController();
            controller.setOnRegistrationSuccess(() -> {
                stage.close();
                showSuccess("تم إنشاء الحساب بنجاح! يمكنك الآن تسجيل الدخول");
            });

            stage.showAndWait();
        } catch (IOException e) {
            showError("فشل في فتح نافذة التسجيل: " + e.getMessage());
        }
    }

    private void showForgotPasswordDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("استعادة كلمة المرور");
        alert.setHeaderText("استعادة كلمة المرور");
        alert.setContentText(
                "لاستعادة كلمة المرور، يرجى التواصل معنا على:\n\n" +
                        "📧 البريد الإلكتروني: support@techway.com\n" +
                        "📞 الهاتف: +966123456789\n\n" +
                        "سيتم إرسال تعليمات استعادة كلمة المرور إلى بريدك الإلكتروني."
        );

        // Apply styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/main-styles.css").toExternalForm());

        alert.showAndWait();
    }

    private void showLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loginButton.setDisable(loading);
        registerButton.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-message", "error-message");
        statusLabel.getStyleClass().add("error-message");
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-message", "error-message");
        statusLabel.getStyleClass().add("success-message");
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll("success-message", "error-message");
    }

    // Callback setter
    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }
}