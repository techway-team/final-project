package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.CertificateDTO;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Controller لواجهة عرض الشهادات
 */
public class CertificateViewerController implements Initializable {

    @FXML private TextField txtSearchCertificate;
    @FXML private Button btnSearchCertificate;
    @FXML private Button btnRefreshCertificates;

    @FXML private ScrollPane certificatesScrollPane;
    @FXML private VBox certificatesContainer;
    @FXML private Label lblNoCertificates;
    @FXML private ProgressIndicator loadingIndicator;

    // Services
    private ApiService apiService;
    private Long userId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.apiService = ApiService.getInstance();
        this.userId = SessionManager.getInstance().getCurrentUserId();

        setupUI();
        loadUserCertificates();
    }

    private void setupUI() {
        // إعداد البحث
        btnSearchCertificate.setOnAction(e -> searchCertificate());
        txtSearchCertificate.setOnAction(e -> searchCertificate());

        // إعداد التحديث
        btnRefreshCertificates.setOnAction(e -> loadUserCertificates());

        // إعداد الحاوية
        certificatesContainer.setSpacing(15);
        certificatesContainer.setPadding(new Insets(20));

        // إخفاء المؤشرات في البداية
        lblNoCertificates.setVisible(false);
        loadingIndicator.setVisible(false);
    }

    /**
     * تحميل جميع شهادات المستخدم
     */
    private void loadUserCertificates() {
        showLoadingState(true);

        apiService.getUserCertificates(userId)
                .thenAccept(certificates -> {
                    Platform.runLater(() -> {
                        showLoadingState(false);
                        if (certificates == null || certificates.isEmpty()) {
                            showNoCertificatesState();
                        } else {
                            displayCertificates(certificates);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoadingState(false);
                        showErrorAlert("خطأ في تحميل الشهادات", ex.getMessage());
                    });
                    return null;
                });
    }


    /**
     * طريقة بديلة لتحميل الشهادات (مؤقت حتى يتم إضافة API)
     */
    private void loadCertificatesAlternative() {
        showLoadingState(false);
        showNoCertificatesState();
        // TODO: تحديث هذا عندما يصبح getUserCertificates متاح في API
    }

    /**
     * عرض قائمة الشهادات
     */
    private void displayCertificates(List<CertificateDTO> certificates) {
        certificatesContainer.getChildren().clear();
        lblNoCertificates.setVisible(false);

        for (CertificateDTO cert : certificates) {
            VBox certCard = createCertificateCard(cert);
            certificatesContainer.getChildren().add(certCard);
        }
    }

    /**
     * إنشاء كارت شهادة
     */
    private VBox createCertificateCard(CertificateDTO certificate) {
        VBox card = new VBox(10);
        card.getStyleClass().add("certificate-card");
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Header مع أيقونة الشهادة
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // أيقونة الشهادة
        ImageView certIcon = new ImageView();
        try {
            certIcon.setImage(new Image(getClass().getResourceAsStream("/icons/certificate.png")));
            certIcon.setFitWidth(48);
            certIcon.setFitHeight(48);
        } catch (Exception e) {
            // إذا لم تكن الأيقونة متاحة، استخدم نص بديل
            Label iconLabel = new Label("🏆");
            iconLabel.setFont(Font.font(24));
            header.getChildren().add(iconLabel);
        }

        VBox certInfo = new VBox(5);

        // عنوان الشهادة
        Label titleLabel = new Label("شهادة إتمام الكورس");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        // رقم الشهادة
        Label certNumberLabel = new Label("رقم الشهادة: " + certificate.getCertificateNumber());
        certNumberLabel.setFont(Font.font("Arial", 12));
        certNumberLabel.setTextFill(Color.web("#7f8c8d"));

        certInfo.getChildren().addAll(titleLabel, certNumberLabel);
        header.getChildren().addAll(certIcon, certInfo);

        // معلومات الشهادة
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(8);

        // تاريخ الإصدار
        Label issueDateLabel = new Label("تاريخ الإصدار:");
        issueDateLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label issueDateValue = new Label(certificate.getIssuedAt() != null ?
                certificate.getIssuedAt().format(String.valueOf(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) : "غير محدد");

        // النتيجة النهائية
        Label scoreLabel = new Label("النتيجة النهائية:");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label scoreValue = new Label(certificate.getFinalScore() != null ?
                String.format("%.1f%%", certificate.getFinalScore()) : "غير محدد");

        // نتيجة الكويز
        Label quizScoreLabel = new Label("نتيجة الكويز:");
        quizScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label quizScoreValue = new Label(certificate.getQuizScore() != null ?
                String.format("%.1f%%", certificate.getQuizScore()) : "غير محدد");

        // حالة الشهادة
        Label statusLabel = new Label("الحالة:");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label statusValue = new Label(getStatusDisplayText(certificate.getStatus()));
        statusValue.setTextFill(getStatusColor(certificate.getStatus()));
        statusValue.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        detailsGrid.add(issueDateLabel, 0, 0);
        detailsGrid.add(issueDateValue, 1, 0);
        detailsGrid.add(scoreLabel, 0, 1);
        detailsGrid.add(scoreValue, 1, 1);
        detailsGrid.add(quizScoreLabel, 0, 2);
        detailsGrid.add(quizScoreValue, 1, 2);
        detailsGrid.add(statusLabel, 0, 3);
        detailsGrid.add(statusValue, 1, 3);

        // الأزرار
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        Button btnView = new Button("عرض الشهادة");
        btnView.getStyleClass().add("btn-primary");
        btnView.setStyle("-fx-background-color: #3498db; " +
                "-fx-text-fill: white; " +
                "-fx-padding: 8 16; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5;");
        btnView.setOnAction(e -> viewCertificate(certificate));

        Button btnDownload = new Button("تحميل PDF");
        btnDownload.getStyleClass().add("btn-secondary");
        btnDownload.setStyle("-fx-background-color: #27ae60; " +
                "-fx-text-fill: white; " +
                "-fx-padding: 8 16; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5;");
        btnDownload.setOnAction(e -> downloadCertificate(certificate));

        Button btnVerify = new Button("التحقق من الشهادة");
        btnVerify.getStyleClass().add("btn-outline");
        btnVerify.setStyle("-fx-background-color: transparent; " +
                "-fx-text-fill: #9b59b6; " +
                "-fx-border-color: #9b59b6; " +
                "-fx-border-width: 1; " +
                "-fx-padding: 8 16; " +
                "-fx-border-radius: 5;");
        btnVerify.setOnAction(e -> verifyCertificate(certificate));

        buttonsBox.getChildren().addAll(btnView, btnDownload, btnVerify);

        card.getChildren().addAll(header, detailsGrid, buttonsBox);
        return card;
    }

    /**
     * البحث عن شهادة برقم معين
     */
    private void searchCertificate() {
        String certificateNumber = txtSearchCertificate.getText().trim();
        if (certificateNumber.isEmpty()) {
            showWarningAlert("تنبيه", "يرجى إدخال رقم الشهادة للبحث");
            return;
        }

        showLoadingState(true);

        // استخدام API للتحقق من الشهادة
        CompletableFuture.supplyAsync(() -> {
            // هنا نستخدم verify API للبحث
            return apiService.getUserCourseCertificate(userId, null); // نحتاج تعديل هذا
        }).thenAccept(result -> {
            Platform.runLater(() -> {
                showLoadingState(false);
                // عرض نتائج البحث
                // TODO: تحديث هذا بناء على API response
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showLoadingState(false);
                showErrorAlert("خطأ في البحث", "لم يتم العثور على شهادة برقم: " + certificateNumber);
            });
            return null;
        });
    }

    /**
     * عرض الشهادة
     */
    private void viewCertificate(CertificateDTO certificate) {
        // فتح الشهادة في المتصفح
        apiService.openCertificateInBrowser(certificate.getId());
    }

    /**
     * تحميل الشهادة كـ PDF
     */
    private void downloadCertificate(CertificateDTO certificate) {
        showLoadingState(true);

        // تحميل PDF
        apiService.downloadCertificatePdf(certificate.getId(), "المستخدم")
                .thenAccept(filePath -> {
                    Platform.runLater(() -> {
                        showLoadingState(false);
                        showSuccessMessage("تم تحميل الشهادة بنجاح إلى: " + filePath);

                        // فتح مجلد التحميل
                        try {
                            Desktop.getDesktop().open(filePath.getParent().toFile());
                        } catch (IOException e) {
                            System.err.println("فشل فتح مجلد التحميل: " + e.getMessage());
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoadingState(false);
                        showErrorAlert("خطأ في التحميل", "فشل تحميل الشهادة: " + ex.getMessage());
                    });
                    return null;
                });
    }

    /**
     * التحقق من صحة الشهادة
     */
    private void verifyCertificate(CertificateDTO certificate) {
        // إنشاء نافذة التحقق
        Alert verifyAlert = new Alert(Alert.AlertType.INFORMATION);
        verifyAlert.setTitle("التحقق من الشهادة");
        verifyAlert.setHeaderText("معلومات التحقق");

        String verifyInfo = String.format(
                "رقم الشهادة: %s\n" +
                        "تاريخ الإصدار: %s\n" +
                        "الحالة: %s\n" +
                        "صالحة: %s\n\n" +
                        "يمكنك مشاركة رقم الشهادة مع الآخرين للتحقق من صحتها.",
                certificate.getCertificateNumber(),
                certificate.getIssuedAt() != null ?
                        certificate.getIssuedAt().format(String.valueOf(DateTimeFormatter.ofPattern(String.valueOf(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))))) : "غير محدد",
                getStatusDisplayText(certificate.getStatus()),
                certificate.isValid() ? "نعم" : "لا"
        );

        verifyAlert.setContentText(verifyInfo);
        verifyAlert.showAndWait();
    }

    // Utility Methods
    private String getStatusDisplayText(String status) {
        if (status == null) return "غير محدد";
        switch (status.toLowerCase()) {
            case "active": return "فعالة";
            case "revoked": return "ملغاة";
            case "expired": return "منتهية الصلاحية";
            default: return status;
        }
    }

    private Color getStatusColor(String status) {
        if (status == null) return Color.GRAY;
        switch (status.toLowerCase()) {
            case "active": return Color.GREEN;
            case "revoked": return Color.RED;
            case "expired": return Color.ORANGE;
            default: return Color.GRAY;
        }
    }

    private void showLoadingState(boolean loading) {
        loadingIndicator.setVisible(loading);
        certificatesContainer.setDisable(loading);
    }

    private void showNoCertificatesState() {
        certificatesContainer.getChildren().clear();
        lblNoCertificates.setVisible(true);
        lblNoCertificates.setText("لا توجد شهادات حتى الآن.\nأكمل الكورسات واجتز الكويزات للحصول على الشهادات!");
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("نجح");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}