package com.techway.coursemanagementdesktop;

import com.techway.coursemanagementdesktop.CertificateDTO;
import com.techway.coursemanagementdesktop.service.ApiService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class CertificateGeneratorView extends VBox {

    private Label lblTitle = new Label("شهادة إتمام الكورس");
    private Label lblUserName = new Label();
    private Label lblCourseTitle = new Label();
    private Label lblCompletionDate = new Label();
    private Label lblFinalScore = new Label();
    private Label lblQuizScore = new Label();
    private Label lblCertificateNumber = new Label();
    private Label lblIssuedAt = new Label();
    private Label lblStatus = new Label();

    private Button btnRefresh = new Button("تحديث الشهادة");

    private CertificateDTO certificate;

    public CertificateGeneratorView(Long userId, Long courseId) {
        buildUI();
        btnRefresh.setOnAction(e -> loadCertificate(userId, courseId));
        loadCertificate(userId, courseId);
    }

    public CertificateGeneratorView() {

    }

    private void buildUI() {
        this.setSpacing(10);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.CENTER);

        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        lblStatus.setStyle("-fx-text-fill: red;");

        this.getChildren().addAll(
                lblTitle,
                lblCertificateNumber,
                lblUserName,
                lblCourseTitle,
                lblCompletionDate,
                lblFinalScore,
                lblQuizScore,
                lblIssuedAt,
                btnRefresh,
                lblStatus
        );
    }

    private void loadCertificate(Long userId, Long courseId) {
        lblStatus.setText("جاري تحميل بيانات الشهادة...");
        ApiService.getInstance().getUserCourseCertificate(userId, courseId)
                .thenAccept(optCert -> {
                    Platform.runLater(() -> {
                        if (optCert.isPresent()) {
                            certificate = optCert.get();
                            lblStatus.setText("");

                            lblCertificateNumber.setText("رقم الشهادة: " + certificate.getCertificateNumber());
                            lblUserName.setText("المستخدم: " + certificate.getUserName());
                            lblCourseTitle.setText("اسم الدورة: " + certificate.getCourseTitle());
                            lblCompletionDate.setText("تاريخ الإكمال: " + certificate.getCompletionDate());
                            lblFinalScore.setText("النتيجة النهائية: " + certificate.getFinalScore());
                            lblQuizScore.setText("نتيجة الكويز: " + certificate.getQuizScore());
                            lblIssuedAt.setText("تاريخ الإصدار: " + certificate.getIssuedAt());
                        } else {
                            lblStatus.setText("⚠️ لا توجد شهادة لهذا المستخدم أو الكورس.");
                        }
                    });
                }).exceptionally(ex -> {
                    Platform.runLater(() -> lblStatus.setText("حدث خطأ: " + ex.getMessage()));
                    return null;
                });
    }

    // داخل CertificateGenerator.java
    public void showInStage() {
        Stage stage = new Stage();
        stage.setTitle("شهادة إتمام الكورس");
        stage.setScene(new Scene(this, 600, 600));
        stage.show();
    }

    public void setCertificate(CertificateDTO certificate) {
        this.certificate = certificate;
    }
}