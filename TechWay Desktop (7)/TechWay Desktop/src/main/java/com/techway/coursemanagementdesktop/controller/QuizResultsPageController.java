package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.CertificateDTO;
import com.techway.coursemanagementdesktop.model.*;
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

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller لصفحة نتائج الكويز - متكامل مع MainController
 */
public class QuizResultsPageController {

    private MainController mainController;
    private ApiService apiService;
    private SessionManager sessionManager;

    // Data
    private Quiz currentQuiz;
    private QuizAttempt currentAttempt;
    private Long courseId;
    private Map<String, Object> resultData;
    private CertificateDTO certificate;

    public QuizResultsPageController(MainController mainController) {
        this.mainController = mainController;
        this.apiService = ApiService.getInstance();
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * إنشاء صفحة النتائج
     */
    public VBox createResultsPage(Quiz quiz, QuizAttempt attempt, Long courseId, Map<String, Object> resultData) {
        this.currentQuiz = quiz;
        this.currentAttempt = attempt;
        this.courseId = courseId;
        this.resultData = resultData;

        VBox mainContainer = new VBox(0);
        mainContainer.getStyleClass().add("results-container");

        // Header section مع النتيجة
        VBox headerSection = createResultsHeader();

        // Content section مع التفاصيل
        ScrollPane contentSection = createResultsContent();

        // Footer section مع الأزرار
        HBox footerSection = createResultsFooter();

        mainContainer.getChildren().addAll(headerSection, contentSection, footerSection);

        // التحقق من وجود شهادة
        checkForCertificate();

        return mainContainer;
    }

    private void checkForCertificate() {
        if (sessionManager.getCurrentUser() == null || courseId == null) return;

        apiService.getUserCourseCertificate(sessionManager.getCurrentUser().getId(), courseId)
                .thenAccept(optCert -> {
                    optCert.ifPresent(c -> this.certificate = c);
                });
    }


    private HBox createResultsFooter() {
        HBox footer = new HBox(15);
        footer.setPadding(new Insets(20));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #ecf0f1;");

        // زر الرجوع للدورة
        Button backButton = new Button("🔙 رجوع إلى الدورة");
        backButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
        backButton.setOnAction(e -> mainController.loadCourseDetails(courseId));

        footer.getChildren().add(backButton);

        // زر إعادة المحاولة إذا لم ينجح
        Boolean passed = (Boolean) resultData.get("passed");
        if (passed != null && !passed) {
            Button retryButton = new Button("🔁 إعادة المحاولة");
            retryButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
            retryButton.setOnAction(e -> mainController.loadQuizPage(courseId));
            footer.getChildren().add(retryButton);
        }

        // زر عرض الشهادة إذا متاحة
        if (passed != null && passed && certificate != null) {
            Button viewCertButton = new Button("🏆 عرض الشهادة");
            viewCertButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
            viewCertButton.setOnAction(e -> downloadCertificate());
            footer.getChildren().add(viewCertButton);
        }

        return footer;
    }


    private VBox createResultsHeader() {
        VBox header = new VBox(20);
        header.setPadding(new Insets(30));

        // تحديد لون الخلفية بناء على النجاح/الفشل
        Boolean passed = (Boolean) resultData.get("passed");
        Double score = (Double) resultData.get("score");

        String backgroundColor = passed ? "#27ae60" : "#e74c3c";
        header.setStyle("-fx-background-color: " + backgroundColor + ";");

        // أيقونة النتيجة
        Label resultIcon = new Label(passed ? "🎉" : "😞");
        resultIcon.setFont(Font.font(48));

        // عنوان النتيجة
        Label resultTitle = new Label(passed ? "تهانينا!" : "حاول مرة أخرى");
        resultTitle.setTextFill(Color.WHITE);
        resultTitle.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        // عنوان الكويز
        Label quizTitle = new Label(currentQuiz.getTitle());
        quizTitle.setTextFill(Color.WHITE);
        quizTitle.setFont(Font.font("Arial", FontWeight.NORMAL, 18));

        // النتيجة الرئيسية
        Label mainScore = new Label(String.format("%.1f%%", score));
        mainScore.setTextFill(Color.WHITE);
        mainScore.setFont(Font.font("Arial", FontWeight.BOLD, 48));

        // النسبة المطلوبة
        Label requiredScore = new Label("النسبة المطلوبة للنجاح: " + currentQuiz.getPassingScoreDisplay());
        requiredScore.setTextFill(Color.web("#ECF0F1"));
        requiredScore.setFont(Font.font("Arial", 14));

        VBox centerBox = new VBox(10);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(resultIcon, resultTitle, quizTitle, mainScore, requiredScore);

        header.getChildren().add(centerBox);

        return header;
    }

    private ScrollPane createResultsContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f8f9fa;");

        VBox contentContainer = new VBox(25);
        contentContainer.setPadding(new Insets(30));

        // إحصائيات المحاولة
        VBox statsCard = createStatsCard();

        // تفاصيل الأسئلة
        VBox questionsCard = createQuestionsDetailsCard();

        // معلومات الشهادة (إن وُجدت)
        VBox certificateCard = createCertificateCard();

        contentContainer.getChildren().addAll(statsCard, questionsCard, certificateCard);
        scrollPane.setContent(contentContainer);

        return scrollPane;
    }

    private VBox createCertificateCard() {
        VBox card = new VBox(20);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        Label cardTitle = new Label("الشهادة");
        cardTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        cardTitle.setTextFill(Color.web("#2c3e50"));

        VBox contentBox = new VBox(10);

        if (certificate != null) {
            // ✅ الشهادة متوفرة
            Label certInfo = new Label(String.format("رقم الشهادة: %s | تاريخ الإصدار: %s",
                    certificate.getCertificateNumber(),
                    certificate.getIssuedAt().format(String.valueOf(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            certInfo.setFont(Font.font("Arial", 13));
            certInfo.setTextFill(Color.web("#16a085"));

            Button downloadBtn = new Button("📄 تحميل الشهادة PDF");
            downloadBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
            downloadBtn.setOnAction(e -> downloadCertificate());

            contentBox.getChildren().addAll(certInfo, downloadBtn);
        } else {
            // ❌ لا توجد شهادة
            Label noCertLabel = new Label("لم يتم إصدار شهادة لهذه المحاولة.");
            noCertLabel.setFont(Font.font("Arial", 13));
            noCertLabel.setTextFill(Color.web("#e74c3c"));
            contentBox.getChildren().add(noCertLabel);
        }

        card.getChildren().addAll(cardTitle, contentBox);
        return card;
    }

    private void downloadCertificate() {
        if (certificate == null) return;

        Platform.runLater(() -> {
            try {
                File pdf = CertificateGenerator.generate(
                        sessionManager.getCurrentUser(),
                        apiService.getCourseById(courseId).join(),
                        certificate
                );

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("تم التحميل");
                alert.setHeaderText("🎉 تم تحميل الشهادة بنجاح!");
                alert.setContentText("تم حفظ الشهادة في: \n" + pdf.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("خطأ");
                alert.setHeaderText("❌ فشل في تحميل الشهادة");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        });
    }


    private VBox createStatsCard() {
        VBox card = new VBox(20);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        // عنوان الكارت
        Label cardTitle = new Label("إحصائيات المحاولة");
        cardTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        cardTitle.setTextFill(Color.web("#2c3e50"));

        // الإحصائيات في شبكة
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(30);
        statsGrid.setVgap(15);

        // رقم المحاولة
        addStatRow(statsGrid, 0, "رقم المحاولة:", String.valueOf(currentAttempt.getAttemptNumber()));

        // الإجابات الصحيحة
        addStatRow(statsGrid, 1, "الإجابات الصحيحة:", currentAttempt.getCorrectAnswersDisplay());

        // النتيجة النهائية
        Double score = (Double) resultData.get("score");
        addStatRow(statsGrid, 2, "النتيجة النهائية:", String.format("%.1f%%", score));

        // الوقت المستغرق
        addStatRow(statsGrid, 3, "الوقت المستغرق:", currentAttempt.getTimeTakenDisplay());

        // تاريخ الانتهاء
        addStatRow(statsGrid, 4, "تاريخ الانتهاء:", currentAttempt.getCompletedAtDisplay());

        // النتيجة النهائية
        Boolean passed = (Boolean) resultData.get("passed");
        Label statusLabel = new Label(passed ? "نجح" : "لم ينجح");
        statusLabel.setTextFill(passed ? Color.GREEN : Color.RED);
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        addStatRow(statsGrid, 5, "الحالة:", statusLabel);

        card.getChildren().addAll(cardTitle, statsGrid);
        return card;
    }

    private void addStatRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        labelNode.setTextFill(Color.web("#34495e"));

        Label valueNode = new Label(value);
        valueNode.setFont(Font.font("Arial", 12));
        valueNode.setTextFill(Color.web("#2c3e50"));

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private void addStatRow(GridPane grid, int row, String label, Label valueNode) {
        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        labelNode.setTextFill(Color.web("#34495e"));

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private VBox createQuestionsDetailsCard() {
        VBox card = new VBox(20);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        Label cardTitle = new Label("تفاصيل الأسئلة");
        cardTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        cardTitle.setTextFill(Color.web("#2c3e50"));

        VBox questionsContainer = new VBox(15);

        for (Question question : currentQuiz.getQuestions()) {
            VBox questionBox = new VBox(10);
            questionBox.setStyle("-fx-background-color: #f0f3f5; -fx-padding: 15; -fx-background-radius: 8;");

            Label questionLabel = new Label("السؤال: " + question.getQuestionText());
            questionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            questionLabel.setWrapText(true);
            questionLabel.setTextFill(Color.web("#34495e"));

            // الإجابة الصحيحة
            Optional<QuestionOption> correctOption = question.getOptions().stream()
                    .filter(QuestionOption::getIsCorrect)
                    .findFirst();

            // إجابة المستخدم
            QuizAnswer userAnswer = currentAttempt.getAnswers().stream()
                    .filter(a -> Objects.equals(a.getQuestionId(), question.getId()))
                    .findFirst()
                    .orElse(null);

            String userAnswerText = "لم يُجب";
            String correctAnswerText = correctOption.map(QuestionOption::getOptionText).orElse("غير متوفر");

            boolean isCorrect = false;

            if (userAnswer != null) {
                Optional<QuestionOption> selectedOption = question.getOptions().stream()
                        .filter(opt -> Objects.equals(opt.getId(), userAnswer.getSelectedOptionId()))
                        .findFirst();

                if (selectedOption.isPresent()) {
                    userAnswerText = selectedOption.get().getOptionText();
                    isCorrect = selectedOption.get().getIsCorrect();
                }
            }

            Label userAnswerLabel = new Label("إجابتك: " + userAnswerText + (isCorrect ? " ✅" : " ❌"));
            userAnswerLabel.setFont(Font.font("Arial", 13));
            userAnswerLabel.setTextFill(isCorrect ? Color.GREEN : Color.RED);

            Label correctAnswerLabel = new Label("الإجابة الصحيحة: " + correctAnswerText);
            correctAnswerLabel.setFont(Font.font("Arial", 13));
            correctAnswerLabel.setTextFill(Color.web("#2980b9"));

            questionBox.getChildren().addAll(questionLabel, userAnswerLabel, correctAnswerLabel);
            questionsContainer.getChildren().add(questionBox);
        }

        card.getChildren().addAll(cardTitle, questionsContainer);
        return card;
    }
}
