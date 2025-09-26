package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.CertificateDTO;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controller لصفحة عرض الشهادات - متكامل مع MainController
 */
public class CertificatePageController {

    private MainController mainController;
    private ApiService apiService;
    private SessionManager sessionManager;

    // UI Components
    private VBox mainContainer;
    private TextField txtSearchCertificate;
    private VBox certificatesContainer;
    private Label lblNoCertificates;
    private ProgressIndicator loadingIndicator;

    public CertificatePageController(MainController mainController) {
        this.mainController = mainController;
        this.apiService = ApiService.getInstance();
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * إنشاء صفحة الشهادات
     */
    public VBox createCertificatesPage() {
        mainContainer = new VBox(0);
        mainContainer.getStyleClass().add("certificates-container");

        // Header section
        VBox headerSection = createCertificatesHeader();

        // Content section
        ScrollPane contentSection = createCertificatesContent();

        // Footer section
        HBox footerSection = createCertificatesFooter();

        mainContainer.getChildren().addAll(headerSection, contentSection, footerSection);

        // تحميل الشهادات
        loadUserCertificates();

        return mainContainer;
    }

    private VBox createCertificatesHeader() {
        VBox header = new VBox(20);
        header.setPadding(new Insets(25));
        header.setStyle("-fx-background-color: #2c3e50;");

        // العنوان الرئيسي
        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label titleIcon = new Label("🏆");
        titleIcon.setFont(Font.font(28));

        Label titleLabel = new Label("شهاداتي");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 26));

        titleBox.getChildren().addAll(titleIcon, titleLabel);

        // شريط البحث والأدوات
        HBox searchBox = new HBox(15);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("البحث عن شهادة:");
        searchLabel.setTextFill(Color.WHITE);
        searchLabel.setFont(Font.font("Arial", 14));

        txtSearchCertificate = new TextField();
        txtSearchCertificate.setPromptText("أدخل رقم الشهادة...");
        txtSearchCertificate.setPrefWidth(300);
        txtSearchCertificate.setStyle("-fx-padding: 8; -fx-font-size: 14;");

        Button btnSearch = new Button("بحث");
        btnSearch.getStyleClass().add("danger-button");
        btnSearch.setOnAction(e -> searchCertificate());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = new Button("تحديث");
        btnRefresh.getStyleClass().add("success-button");
        btnRefresh.setOnAction(e -> loadUserCertificates());

        searchBox.getChildren().addAll(searchLabel, txtSearchCertificate, btnSearch, spacer, btnRefresh);

        header.getChildren().addAll(titleBox, searchBox);
        return header;
    }

    private ScrollPane createCertificatesContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #ecf0f1;");

        StackPane contentStack = new StackPane();

        // حاوية الشهادات
        certificatesContainer = new VBox(20);
        certificatesContainer.setPadding(new Insets(30));

        // رسالة عدم وجود شهادات
        VBox noCertsContainer = new VBox(20);
        noCertsContainer.setAlignment(Pos.CENTER);

        Label noCertsIcon = new Label("📜");
        noCertsIcon.setFont(Font.font(64));
        noCertsIcon.setTextFill(Color.web("#7f8c8d"));

        lblNoCertificates = new Label("لا توجد شهادات حتى الآن");
        lblNoCertificates.setFont(Font.font("Arial", 18));
        lblNoCertificates.setTextFill(Color.web("#7f8c8d"));

        Label noCertsSubtext = new Label("أكمل الكورسات واجتز الكويزات للحصول على الشهادات!");
        noCertsSubtext.setFont(Font.font("Arial", 14));
        noCertsSubtext.setTextFill(Color.web("#95a5a6"));
        noCertsSubtext.setWrapText(true);

        noCertsContainer.getChildren().addAll(noCertsIcon, lblNoCertificates, noCertsSubtext);
        noCertsContainer.setVisible(false);

        // مؤشر التحميل
        VBox loadingContainer = new VBox(15);
        loadingContainer.setAlignment(Pos.CENTER);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(50, 50);

        Label loadingLabel = new Label("جاري تحميل الشهادات...");
        loadingLabel.setFont(Font.font("Arial", 14));
        loadingLabel.setTextFill(Color.web("#7f8c8d"));

        loadingContainer.getChildren().addAll(loadingIndicator, loadingLabel);
        loadingContainer.setVisible(false);

        contentStack.getChildren().addAll(certificatesContainer, noCertsContainer, loadingContainer);
        scrollPane.setContent(contentStack);

        return scrollPane;
    }

    private HBox createCertificatesFooter() {
        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(15));
        footer.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");

        Label tipLabel = new Label("💡 نصيحة: يمكنك مشاركة رقم الشهادة مع الآخرين للتحقق من صحتها");
        tipLabel.setFont(Font.font("Arial", 12));
        tipLabel.setTextFill(Color.web("#7f8c8d"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label("إجمالي الشهادات: 0");
        countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        countLabel.setTextFill(Color.web("#34495e"));

        Button btnBackToCourses = new Button("العودة للكورسات");
        btnBackToCourses.getStyleClass().add("secondary-button");
        btnBackToCourses.setOnAction(e -> mainController.navigateToCourses());

        footer.getChildren().addAll(tipLabel, spacer, countLabel, btnBackToCourses);

        return footer;
    }



    /**
     * عرض قائمة الشهادات
     */
    private void displayCertificates(List<CertificateDTO> certificates) {
        certificatesContainer.getChildren().clear();

        for (CertificateDTO cert : certificates) {
            VBox certCard = createCertificateCard(cert);
            certificatesContainer.getChildren().add(certCard);
        }

        // تحديث عداد الشهادات
        updateCertificatesCount(certificates.size());
    }

    /**
     * إنشاء كارت شهادة
     */
    private VBox createCertificateCard(CertificateDTO certificate) {
        VBox card = new VBox(10);
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

        Label certIcon = new Label("🏆");
        certIcon.setFont(Font.font(24));

        VBox certInfo = new VBox(5);

        Label titleLabel = new Label("شهادة إتمام الكورس");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        Label certNumberLabel = new Label("رقم الشهادة: " + certificate.getCertificateNumber());
        certNumberLabel.setFont(Font.font("Arial", 12));
        certNumberLabel.setTextFill(Color.web("#7f8c8d"));

        certInfo.getChildren().addAll(titleLabel, certNumberLabel);
        header.getChildren().addAll(certIcon, certInfo);

        // معلومات الشهادة
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(8);

        addDetailRow(detailsGrid, 0, "تاريخ الإصدار:",
                certificate.getIssuedAt() != null && !certificate.getIssuedAt().isEmpty() ?
                        LocalDateTime.parse(certificate.getIssuedAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"))
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) :
                        "غير محدد");

        addDetailRow(detailsGrid, 1, "النتيجة النهائية:",
                certificate.getFinalScore() != null ?
                        String.format("%.1f%%", certificate.getFinalScore()) : "غير محدد");

        addDetailRow(detailsGrid, 2, "نتيجة الكويز:",
                certificate.getQuizScore() != null ?
                        String.format("%.1f%%", certificate.getQuizScore()) : "غير محدد");

        Label statusLabel = new Label(getStatusDisplayText(certificate.getStatus()));
        statusLabel.setTextFill(getStatusColor(certificate.getStatus()));
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        addDetailRow(detailsGrid, 3, "الحالة:", statusLabel);

        // الأزرار
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        Button btnView = new Button("عرض الشهادة");
        btnView.getStyleClass().add("primary-button");
        btnView.setOnAction(e -> viewCertificate(certificate));

        Button btnDownload = new Button("تحميل PDF");
        btnDownload.getStyleClass().add("success-button");
        btnDownload.setOnAction(e -> downloadCertificate(certificate));

        Button btnVerify = new Button("التحقق من الشهادة");
        btnVerify.getStyleClass().add("secondary-button");
        btnVerify.setOnAction(e -> verifyCertificate(certificate));

        buttonsBox.getChildren().addAll(btnView, btnDownload, btnVerify);

        card.getChildren().addAll(header, detailsGrid, buttonsBox);
        return card;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Label valueNode = new Label(value);
        valueNode.setFont(Font.font("Arial", 12));

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private void addDetailRow(GridPane grid, int row, String label, Label valueNode) {
        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    /**
     * البحث عن شهادة برقم معين
     */
    private void searchCertificate() {
        String certificateNumber = txtSearchCertificate.getText().trim();
        if (certificateNumber.isEmpty()) {
            showWarningMessage("يرجى إدخال رقم الشهادة للبحث");
            return;
        }

        showLoadingState(true);

        // استخدام API للبحث عن الشهادة
        apiService.getCertificateByNumber(certificateNumber)
                .thenAccept(optionalCert -> {
                    Platform.runLater(() -> {
                        showLoadingState(false);
                        if (optionalCert.isPresent()) {
                            // عرض الشهادة الموجودة
                            displaySingleCertificate(optionalCert.get());
                        } else {
                            showNoCertificatesState();
                            showWarningMessage("لم يتم العثور على شهادة برقم: " + certificateNumber);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoadingState(false);
                        showErrorMessage("خطأ في البحث: " + ex.getMessage());
                    });
                    return null;
                });
    }

    /**
     * تحميل شهادات المستخدم - الآن يستخدم API الحقيقي
     */
    private void loadUserCertificates() {
        showLoadingState(true);

        Long userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            showLoadingState(false);
            showNoCertificatesState();
            return;
        }

        // استخدام API الجديد لجلب الشهادات
        apiService.getUserCertificates(userId)
                .thenAccept(certificates -> {
                    Platform.runLater(() -> {
                        showLoadingState(false);
                        if (certificates == null || certificates.isEmpty()) {
                            showNoCertificatesState();
                        } else {
                            displayCertificates(certificates);
                            updateCertificatesCount(certificates.size());
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoadingState(false);
                        showErrorMessage("فشل في تحميل الشهادات: " + ex.getMessage());
                        showNoCertificatesState();
                    });
                    return null;
                });
    }

    /**
     * عرض شهادة واحدة (من البحث)
     */
    private void displaySingleCertificate(CertificateDTO certificate) {
        certificatesContainer.getChildren().clear();

        VBox certCard = createCertificateCard(certificate);
        certificatesContainer.getChildren().add(certCard);

        updateCertificatesCount(1);
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
        String userName = sessionManager.getCurrentUser().getName();

        apiService.downloadCertificatePdf(certificate.getId(), userName)
                .thenAccept(filePath -> {
                    Platform.runLater(() -> {
                        showSuccessMessage("تم تحميل الشهادة بنجاح إلى: " + filePath);

                        // محاولة فتح مجلد التحميل
                        try {
                            java.awt.Desktop.getDesktop().open(filePath.getParent().toFile());
                        } catch (Exception e) {
                            System.err.println("فشل فتح مجلد التحميل: " + e.getMessage());
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showErrorMessage("فشل تحميل الشهادة: " + ex.getMessage());
                    });
                    return null;
                });
    }

    /**
     * التحقق من صحة الشهادة
     */
    private void verifyCertificate(CertificateDTO certificate) {
        apiService.verifyCertificate(certificate.getCertificateNumber())
                .thenAccept(verificationResult -> {
                    Platform.runLater(() -> {
                        showVerificationDialog(certificate, verificationResult);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showErrorMessage("فشل في التحقق من الشهادة: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void showVerificationDialog(CertificateDTO certificate, Map<String, Object> verificationResult) {
        Alert verifyAlert = new Alert(Alert.AlertType.INFORMATION);
        verifyAlert.setTitle("التحقق من الشهادة");
        verifyAlert.setHeaderText("معلومات التحقق");

        Boolean isValid = (Boolean) verificationResult.get("valid");
        String message = (String) verificationResult.get("message");

        String verifyInfo = String.format(
                "رقم الشهادة: %s\n" +
                        "تاريخ الإصدار: %s\n" +
                        "الحالة: %s\n" +
                        "صالحة: %s\n\n" +
                        "%s\n\n" +
                        "يمكنك مشاركة رقم الشهادة مع الآخرين للتحقق من صحتها.",
                certificate.getCertificateNumber(),
                certificate.getIssuedAt() != null ?
                        certificate.getIssuedAt().format(String.valueOf(DateTimeFormatter.ofPattern(String.valueOf(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))))) : "غير محدد",
                getStatusDisplayText(certificate.getStatus()),
                isValid ? "نعم" : "لا",
                message != null ? message : ""
        );

        verifyAlert.setContentText(verifyInfo);
        verifyAlert.showAndWait();
    }

    // Helper methods
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
        // البحث عن مؤشر التحميل وإظهاره/إخفاءه
        loadingIndicator.setVisible(loading);

        // تعطيل/تمكين واجهة المستخدم
        txtSearchCertificate.setDisable(loading);
    }

    private void showNoCertificatesState() {
        certificatesContainer.getChildren().clear();
        lblNoCertificates.setVisible(true);
        updateCertificatesCount(0);
    }

    private void updateCertificatesCount(int count) {
        // البحث عن label العدد وتحديثه
        Label countLabel = findCertificatesCountLabel();
        if (countLabel != null) {
            countLabel.setText("إجمالي الشهادات: " + count);
        }
    }

    private Label findCertificatesCountLabel() {
        // يمكن تحسين هذا بحفظ reference مباشر
        // لكن للبساطة نبحث في الـ footer
        try {
            HBox footer = (HBox) mainContainer.getChildren().get(2);
            return (Label) footer.getChildren().get(2);
        } catch (Exception e) {
            return null;
        }
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("نجح");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("خطأ");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarningMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("تنبيه");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}