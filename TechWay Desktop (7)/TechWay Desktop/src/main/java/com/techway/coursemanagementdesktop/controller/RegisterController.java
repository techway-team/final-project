package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.model.User;
import com.techway.coursemanagementdesktop.service.ApiService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for Registration Dialog
 */
public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private CheckBox termsCheckBox;

    private ApiService apiService;
    private Runnable onRegistrationSuccess;

    @FXML
    public void initialize() {
        apiService = ApiService.getInstance();

        setupUI();
        setupValidation();
        setupEventHandlers();
    }

    private void setupUI() {
        // Initially hide loading indicator
        loadingIndicator.setVisible(false);
        statusLabel.setText("");

        // Set placeholder text
        nameField.setPromptText("الاسم الكامل");
        emailField.setPromptText("البريد الإلكتروني");
        passwordField.setPromptText("كلمة المرور (6 أحرف على الأقل)");
        confirmPasswordField.setPromptText("تأكيد كلمة المرور");

        // Style buttons
        registerButton.getStyleClass().add("primary-button");
        backToLoginButton.getStyleClass().add("secondary-button");

        // Set button text
        registerButton.setText("إنشاء الحساب");
        backToLoginButton.setText("العودة لتسجيل الدخول");

        termsCheckBox.setText("أوافق على شروط الخدمة وسياسة الخصوصية");

        // Initially disable register button
        registerButton.setDisable(true);
    }

    private void setupValidation() {
        // Real-time validation
        nameField.textProperty().addListener((obs, oldText, newText) -> {
            clearStatus();
            validateForm();
        });

        emailField.textProperty().addListener((obs, oldText, newText) -> {
            clearStatus();
            validateForm();
        });

        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            clearStatus();
            validateForm();
        });

        confirmPasswordField.textProperty().addListener((obs, oldText, newText) -> {
            clearStatus();
            validateForm();
        });

        termsCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            validateForm();
        });
    }

    private void setupEventHandlers() {
        registerButton.setOnAction(e -> performRegistration());
        backToLoginButton.setOnAction(e -> closeDialog());

        // Allow Enter key to submit
        confirmPasswordField.setOnAction(e -> {
            if (isFormValid()) {
                performRegistration();
            }
        });
    }

    private void validateForm() {
        boolean isValid = isFormValid();
        registerButton.setDisable(!isValid);

        // Show real-time validation feedback
        if (!nameField.getText().trim().isEmpty() && nameField.getText().trim().length() < 2) {
            nameField.getStyleClass().add("error-field");
        } else {
            nameField.getStyleClass().remove("error-field");
        }

        if (!emailField.getText().trim().isEmpty() && !isValidEmail(emailField.getText().trim())) {
            emailField.getStyleClass().add("error-field");
        } else {
            emailField.getStyleClass().remove("error-field");
        }

        if (!passwordField.getText().isEmpty() && passwordField.getText().length() < 6) {
            passwordField.getStyleClass().add("error-field");
        } else {
            passwordField.getStyleClass().remove("error-field");
        }

        if (!confirmPasswordField.getText().isEmpty() &&
                !confirmPasswordField.getText().equals(passwordField.getText())) {
            confirmPasswordField.getStyleClass().add("error-field");
        } else {
            confirmPasswordField.getStyleClass().remove("error-field");
        }
    }

    private boolean isFormValid() {
        return !nameField.getText().trim().isEmpty() &&
                nameField.getText().trim().length() >= 2 &&
                !emailField.getText().trim().isEmpty() &&
                isValidEmail(emailField.getText().trim()) &&
                !passwordField.getText().isEmpty() &&
                passwordField.getText().length() >= 6 &&
                passwordField.getText().equals(confirmPasswordField.getText()) &&
                termsCheckBox.isSelected();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void performRegistration() {
        if (!isFormValid()) {
            showError("يرجى التأكد من صحة جميع البيانات المدخلة");
            return;
        }

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        showLoading(true);
        clearStatus();

        Task<User> registrationTask = new Task<User>() {
            @Override
            protected User call() throws Exception {
                return apiService.register(name, email, password).get();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    showLoading(false);
                    User user = getValue();

                    if (user != null) {
                        // Registration successful
                        showSuccess("تم إنشاء الحساب بنجاح! يمكنك الآن تسجيل الدخول");

                        // Close dialog after brief delay
                        Platform.runLater(() -> {
                            try {
                                Thread.sleep(1500);
                            } catch (InterruptedException ignored) {}

                            if (onRegistrationSuccess != null) {
                                onRegistrationSuccess.run();
                            }
                        });
                    } else {
                        showError("فشل في إنشاء الحساب. يرجى المحاولة مرة أخرى.");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showLoading(false);
                    Throwable exception = getException();
                    String errorMessage = "فشل في إنشاء الحساب";

                    if (exception != null) {
                        String exceptionMessage = exception.getMessage();
                        if (exceptionMessage.contains("Email already exists")) {
                            errorMessage = "البريد الإلكتروني مسجل مسبقاً. يرجى استخدام بريد إلكتروني آخر";
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

        new Thread(registrationTask).start();
    }

    private void closeDialog() {
        // Get current stage and close it
        registerButton.getScene().getWindow().hide();
    }

    private void showLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        registerButton.setDisable(loading);
        backToLoginButton.setDisable(loading);
        nameField.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        termsCheckBox.setDisable(loading);
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
    public void setOnRegistrationSuccess(Runnable callback) {
        this.onRegistrationSuccess = callback;
    }
}