package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.CertificateDTO;
import com.techway.coursemanagementdesktop.model.*;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.controller.CertificateGenerator;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Controller لواجهة عرض نتائج الكويز المفصلة
 */
public class QuizResultsController implements Initializable {

    @FXML private Label lblQuizTitle;
    @FXML private Label lblAttemptInfo;
    @FXML private Label lblFinalScore;
    @FXML private Label lblPassStatus;
    @FXML private Label lblCorrectAnswers;
    @FXML private Label lblTimeTaken;
    @FXML private Label lblAttemptDate;

    @FXML private ProgressBar scoreProgressBar;
    @FXML private VBox resultsContainer;
    @FXML private ScrollPane questionsScrollPane;
    @FXML private VBox questionsContainer;

    @FXML private Button btnTryAgain;
    @FXML private Button btnViewCertificate;
    @FXML private Button btnBackToCourse;
    @FXML private Button btnShowAllAttempts;

    @FXML private VBox certificateSection;
    @FXML private Label lblCertificateStatus;

    // Data
    private Quiz currentQuiz;
    private QuizAttempt currentAttempt;
    private List<Question> questions;
    private Map<Long, Long> userAnswers; // questionId -> selectedOptionId
    private CertificateDTO certificate;
    private Long courseId;

    // Services
    private ApiService apiService;
    private Long userId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.apiService = ApiService.getInstance();
        this.userId = SessionManager.getInstance().getCurrentUserId();

        setupUI();
    }

    private void setupUI() {
        // إعداد الأزرار
        btnTryAgain.setOnAction(e -> retryQuiz());
        btnViewCertificate.setOnAction(e -> viewCertificate());
        btnBackToCourse.setOnAction(e -> backToCourse());
        btnShowAllAttempts.setOnAction(e -> showAllAttempts());

        // إعداد الحاويات
        questionsContainer.setSpacing(15);
        questionsContainer.setPadding(new Insets(20));

        // إخفاء قسم الشهادة في البداية
        certificateSection.setVisible(false);
        btnViewCertificate.setVisible(false);
    }

    /**
     * عرض نتائج محاولة كويز معينة
     */
    public void showResults(Quiz quiz, QuizAttempt attempt, Long courseId) {
        this.currentQuiz = quiz;
        this.currentAttempt = attempt;
        this.courseId = courseId;
        this.questions = quiz.getQuestions();

        // استخراج الإجابات من المحاولة
        extractUserAnswers(attempt);

        // عرض المعلومات الأساسية
        displayBasicInfo();

        // عرض الأسئلة والإجابات
        displayDetailedResults();

        // تحديث حالة الأزرار
        updateButtonStates();

        // التحقق من الشهادة
        checkForCertificate();
    }

    private void extractUserAnswers(QuizAttempt attempt) {
        this.userAnswers = new HashMap<>();
        if (attempt.getAnswers() != null) {
            for (QuizAnswer answer : attempt.getAnswers()) {
                if (answer.getSelectedOptionId() != null) {
                    userAnswers.put(answer.getQuestionId(),
                            answer.getSelectedOptionId());
                }
            }
        }
    }


    private void displayBasicInfo() {
        // عنوان الكويز
        lblQuizTitle.setText(currentQuiz.getTitle());

        // معلومات المحاولة
        lblAttemptInfo.setText(String.format("المحاولة رقم %d",
                currentAttempt.getAttemptNumber()));

        // النتيجة النهائية
        double score = currentAttempt.getScorePercentage() != null ?
                currentAttempt.getScorePercentage() : 0.0;
        lblFinalScore.setText(String.format("%.1f%%", score));
        lblFinalScore.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        // شريط التقدم
        scoreProgressBar.setProgress(score / 100.0);

        // حالة النجاح/الفشل
        boolean passed = currentAttempt.getIsPassed();
        lblPassStatus.setText(passed ? "نجح" : "لم ينجح");
        lblPassStatus.setTextFill(passed ? Color.GREEN : Color.RED);
        lblPassStatus.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        // عدد الإجابات الصحيحة
        lblCorrectAnswers.setText(currentAttempt.getCorrectAnswersDisplay());

        // الوقت المستغرق
        lblTimeTaken.setText(currentAttempt.getTimeTakenDisplay());

        // تاريخ المحاولة
        lblAttemptDate.setText(currentAttempt.getCompletedAtDisplay());

        // تلوين الخلفية حسب النجاح/الفشل
        resultsContainer.setStyle(passed ?
                "-fx-background-color: #E8F5E8;" :
                "-fx-background-color: #FFE8E8;"
        );
    }

    private void displayDetailedResults() {
        questionsContainer.getChildren().clear();

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            VBox questionCard = createQuestionResultCard(question, i + 1);
            questionsContainer.getChildren().add(questionCard);
        }
    }

    private VBox createQuestionResultCard(Question question, int questionNumber) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #ddd; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;");

        // رأس السؤال
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label questionNumLabel = new Label("السؤال " + questionNumber);
        questionNumLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        questionNumLabel.setTextFill(Color.web("#2c3e50"));

        // نقاط السؤال
        Label pointsLabel = new Label("(" + question.getPoints() + " نقاط)");
        pointsLabel.setFont(Font.font("Arial", 12));
        pointsLabel.setTextFill(Color.web("#7f8c8d"));

        header.getChildren().addAll(questionNumLabel, pointsLabel);

        // نص السؤال
        Label questionTextLabel = new Label(question.getQuestionText());
        questionTextLabel.setWrapText(true);
        questionTextLabel.setFont(Font.font("Arial", 13));
        questionTextLabel.setPadding(new Insets(5, 0, 10, 0));

        // الخيارات مع النتائج
        VBox optionsBox = new VBox(5);
        Long userSelectedId = userAnswers.get(question.getId());
        QuestionOption correctOption = question.getCorrectOption();

        for (QuestionOption option : question.getOptions()) {
            HBox optionBox = createOptionResultBox(option, userSelectedId, correctOption);
            optionsBox.getChildren().add(optionBox);
        }

        // الشرح إذا كان متوفر
        if (question.getExplanation() != null && !question.getExplanation().trim().isEmpty()) {
            Label explanationHeader = new Label("الشرح:");
            explanationHeader.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            explanationHeader.setTextFill(Color.web("#34495e"));

            Label explanationLabel = new Label(question.getExplanation());
            explanationLabel.setWrapText(true);
            explanationLabel.setFont(Font.font("Arial", 12));
            explanationLabel.setTextFill(Color.web("#5d6d7e"));
            explanationLabel.setPadding(new Insets(5, 0, 0, 15));

            card.getChildren().addAll(header, questionTextLabel, optionsBox,
                    explanationHeader, explanationLabel);
        } else {
            card.getChildren().addAll(header, questionTextLabel, optionsBox);
        }

        return card;
    }

    private HBox createOptionResultBox(QuestionOption option, Long userSelectedId,
                                       QuestionOption correctOption) {
        HBox optionBox = new HBox(10);
        optionBox.setAlignment(Pos.CENTER_LEFT);
        optionBox.setPadding(new Insets(5));

        boolean isUserSelection = Objects.equals(option.getId(), userSelectedId);
        boolean isCorrectAnswer = Objects.equals(option.getId(), correctOption.getId());

        // أيقونة الحالة
        Label statusIcon = new Label();
        statusIcon.setFont(Font.font(16));

        // تلوين الخلفية والأيقونة
        String backgroundColor;
        String textColor = "#2c3e50";

        if (isCorrectAnswer && isUserSelection) {
            // إجابة صحيحة ومختارة
            statusIcon.setText("✓");
            statusIcon.setTextFill(Color.GREEN);
            backgroundColor = "#D5F4E6";
        } else if (isCorrectAnswer && !isUserSelection) {
            // إجابة صحيحة لكن غير مختارة
            statusIcon.setText("○");
            statusIcon.setTextFill(Color.GREEN);
            backgroundColor = "#E8F5E8";
        } else if (!isCorrectAnswer && isUserSelection) {
            // إجابة خاطئة ومختارة
            statusIcon.setText("✗");
            statusIcon.setTextFill(Color.RED);
            backgroundColor = "#FFE8E8";
            textColor = "#c0392b";
        } else {
            // إجابة عادية
            statusIcon.setText("○");
            statusIcon.setTextFill(Color.GRAY);
            backgroundColor = "white";
        }

        optionBox.setStyle("-fx-background-color: " + backgroundColor + "; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5;");

        // نص الخيار
        Label optionText = new Label(option.getOptionText());
        optionText.setWrapText(true);
        optionText.setFont(Font.font("Arial", 12));
        optionText.setTextFill(Color.web(textColor));

        // إضافة مؤشر الاختيار
        if (isUserSelection) {
            optionText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        }

        optionBox.getChildren().addAll(statusIcon, optionText);
        return optionBox;
    }

    private void updateButtonStates() {
        // زر المحاولة مرة أخرى
        boolean canRetry = currentQuiz.getMaxAttempts() == null ||
                currentAttempt.getAttemptNumber() < currentQuiz.getMaxAttempts();
        btnTryAgain.setDisable(!canRetry);

        if (!canRetry) {
            btnTryAgain.setText("تم استنفاد المحاولات");
        }
    }

    private void checkForCertificate() {
        if (currentAttempt.getIsPassed()) {
            // البحث عن شهادة للمستخدم في هذا الكورس
            apiService.getUserCourseCertificate(userId, courseId)
                    .thenAccept(optionalCert -> {
                        Platform.runLater(() -> {
                            if (optionalCert.isPresent()) {
                                this.certificate = optionalCert.get();
                                showCertificateSection();
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        // لا توجد شهادة أو خطأ
                        return null;
                    });
        }
    }

    private void showCertificateSection() {
        certificateSection.setVisible(true);
        btnViewCertificate.setVisible(true);

        lblCertificateStatus.setText("تهانينا! تم إصدار شهادة إتمام الكورس");
        lblCertificateStatus.setTextFill(Color.web("#27ae60"));
        lblCertificateStatus.setFont(Font.font("Arial", FontWeight.BOLD, 14));
    }

    private void retryQuiz() {
        // التأكد من إمكانية إعادة المحاولة
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("إعادة المحاولة");
        confirmAlert.setHeaderText("هل تريد بدء محاولة جديدة؟");
        confirmAlert.setContentText("ستبدأ كويز جديد من البداية.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // العودة لواجهة الكويز لبدء محاولة جديدة
            // TODO: Navigation back to quiz taking interface
            startNewQuizAttempt();
        }
    }

    private void startNewQuizAttempt() {
        // بدء محاولة جديدة
        try {
            // TODO: Navigate to QuizTakingController and call startQuiz(courseId)
            System.out.println("Starting new quiz attempt for course: " + courseId);
        } catch (Exception e) {
            showErrorAlert("خطأ", "فشل في بدء محاولة جديدة: " + e.getMessage());
        }
    }

    private void viewCertificate() {
        if (certificate != null) {
            try {
                // اجلب المستخدم و الكورس من Session أو ApiService
                User user = SessionManager.getInstance().getCurrentUser();
                Course course = apiService.getCourseById(courseId).join(); // تأكد أنها async أو sync حسب تصميمك

                File pdf = CertificateGenerator.generate(user, course, certificate);

                showSuccessAlert("تم إنشاء الشهادة", "تم حفظ الشهادة في: " + pdf.getAbsolutePath());

                // فتح الملف مباشرة
                java.awt.Desktop.getDesktop().open(pdf);

            } catch (Exception e) {
                showErrorAlert("خطأ", "فشل في إنشاء أو فتح الشهادة: " + e.getMessage());
            }
        } else {
            showErrorAlert("خطأ", "لم يتم العثور على الشهادة");
        }
    }


    private void backToCourse() {
        // العودة لصفحة الكورس
        // TODO: Navigate back to course details page
        System.out.println("Navigating back to course: " + courseId);
    }

    private void showAllAttempts() {
        // عرض جميع محاولات المستخدم لهذا الكويز
        showLoadingDialog("جاري تحميل المحاولات...");

        apiService.getBestAttempt(currentQuiz.getId(), userId) // أو create new method للحصول على جميع المحاولات
                .thenAccept(bestAttempt -> {
                    Platform.runLater(() -> {
                        hideLoadingDialog();
                        showAttemptsDialog(bestAttempt);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        hideLoadingDialog();
                        showErrorAlert("خطأ", "فشل في تحميل المحاولات: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void showAttemptsDialog(QuizAttempt bestAttempt) {
        Alert attemptsAlert = new Alert(Alert.AlertType.INFORMATION);
        attemptsAlert.setTitle("محاولاتك في الكويز");
        attemptsAlert.setHeaderText("أفضل محاولة");

        String attemptsInfo = String.format(
                "أفضل نتيجة: %.1f%%\n" +
                        "المحاولة رقم: %d\n" +
                        "تاريخ المحاولة: %s\n" +
                        "الحالة: %s\n\n" +
                        "المحاولات المسموحة: %s",
                bestAttempt.getScorePercentage(),
                bestAttempt.getAttemptNumber(),
                bestAttempt.getCompletedAtDisplay(),
                bestAttempt.getPassStatusDisplay(),
                currentQuiz.getMaxAttemptsDisplay()
        );

        attemptsAlert.setContentText(attemptsInfo);
        attemptsAlert.showAndWait();
    }

    // Utility Methods
    private void showLoadingDialog(String message) {
        // TODO: إظهار dialog للتحميل
        System.out.println("Loading: " + message);
    }

    private void hideLoadingDialog() {
        // TODO: إخفاء dialog التحميل
        System.out.println("Loading completed");
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void initData(Long quizId, Long userId) {
    }
}