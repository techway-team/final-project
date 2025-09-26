package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.model.*;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller لصفحة حل الكويز - متكامل مع MainController
 */
public class QuizPageController {

    private MainController mainController;
    private ApiService apiService;
    private SessionManager sessionManager;

    // Quiz data
    private Quiz currentQuiz;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private QuizAttempt currentAttempt;
    private Map<Long, Long> selectedAnswers = new HashMap<>();
    private Timeline timer;
    private LocalDateTime quizStartTime;
    private Long courseId;
    private Long userId;

    // UI Components
    private VBox mainContainer;
    private Label lblQuizTitle;
    private Label lblQuestionCounter;
    private Label lblTimeRemaining;
    private ProgressBar progressBar;
    private Label lblQuestionText;
    private VBox optionsContainer;
    private Button btnPrevious;
    private Button btnNext;
    private Button btnSubmitQuiz;
    private Button btnSaveAnswer;

    public QuizPageController() {
        this.mainController = mainController;
        this.apiService = ApiService.getInstance();
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * تهيئة الكويز وبدء المحاولة
     */
    public void initData(Long courseId, Long userId, MainController controller) {
        this.courseId = courseId;
        this.userId = userId;
        this.mainController = controller;

        loadAndStartQuiz();
    }

    /**
     * إنشاء واجهة الكويز
     */
    public VBox createQuizPage() {
        mainContainer = new VBox(0);
        mainContainer.getStyleClass().add("quiz-container");

        // Header section
        VBox headerSection = createQuizHeader();

        // Content section
        ScrollPane contentSection = createQuizContent();

        // Footer section
        HBox footerSection = createQuizFooter();

        mainContainer.getChildren().addAll(headerSection, contentSection, footerSection);

        return mainContainer;
    }

    private VBox createQuizHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(25));
        header.setStyle("-fx-background-color: #2c3e50;");

        // عنوان الكويز
        lblQuizTitle = new Label("عنوان الكويز");
        lblQuizTitle.setTextFill(Color.WHITE);
        lblQuizTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        // شريط المعلومات
        HBox infoBar = new HBox(30);
        infoBar.setAlignment(Pos.CENTER_LEFT);

        lblQuestionCounter = new Label("السؤال 1 من 10");
        lblQuestionCounter.setTextFill(Color.WHITE);
        lblQuestionCounter.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        lblTimeRemaining = new Label("الوقت المتبقي: 30:00");
        lblTimeRemaining.setTextFill(Color.web("#F39C12"));
        lblTimeRemaining.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblTimeRemaining.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        infoBar.getChildren().addAll(lblQuestionCounter, lblTimeRemaining, spacer, progressBar);

        header.getChildren().addAll(lblQuizTitle, infoBar);
        return header;
    }

    private ScrollPane createQuizContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #ecf0f1;");

        VBox contentContainer = new VBox(20);
        contentContainer.setPadding(new Insets(30));

        // كارت السؤال
        VBox questionCard = new VBox(15);
        questionCard.setPadding(new Insets(25));
        questionCard.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        // نص السؤال
        lblQuestionText = new Label("نص السؤال سيظهر هنا...");
        lblQuestionText.setWrapText(true);
        lblQuestionText.setFont(Font.font("Arial", 16));

        // حاوية الخيارات
        optionsContainer = new VBox(12);
        optionsContainer.setPadding(new Insets(10, 0, 0, 0));

        questionCard.getChildren().addAll(lblQuestionText, optionsContainer);
        contentContainer.getChildren().add(questionCard);

        scrollPane.setContent(contentContainer);
        return scrollPane;
    }

    private HBox createQuizFooter() {
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20));
        footer.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");

        // أزرار التنقل
        btnPrevious = new Button("السابق");
        btnPrevious.getStyleClass().add("secondary-button");
        btnPrevious.setOnAction(e -> previousQuestion());

        btnSaveAnswer = new Button("حفظ الإجابة");
        btnSaveAnswer.getStyleClass().add("warning-button");
        btnSaveAnswer.setOnAction(e -> saveCurrentAnswer());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNext = new Button("التالي");
        btnNext.getStyleClass().add("primary-button");
        btnNext.setOnAction(e -> nextQuestion());

        btnSubmitQuiz = new Button("إرسال الكويز");
        btnSubmitQuiz.getStyleClass().add("success-button");
        btnSubmitQuiz.setVisible(false);
        btnSubmitQuiz.setOnAction(e -> submitQuiz());

        // زر العودة
        Button btnBack = new Button("العودة للكورس");
        btnBack.getStyleClass().add("secondary-button");
        btnBack.setOnAction(e -> goBackToCourse());

        footer.getChildren().addAll(btnBack, btnPrevious, btnSaveAnswer, spacer, btnNext, btnSubmitQuiz);

        return footer;
    }

    private void loadAndStartQuiz() {
        showLoadingState(true);

        // جلب الكويز من الخادم
        apiService.getQuizByCourseId(courseId)
                .thenCompose(quiz -> {
                    if (quiz == null) {
                        throw new RuntimeException("لا يوجد كويز متاح لهذا الكورس");
                    }
                    this.currentQuiz = quiz;
                    return startQuizAttempt(quiz.getId());
                })
                .thenAccept(attempt -> {
                    Platform.runLater(() -> {
                        this.currentAttempt = attempt;
                        setupQuiz();
                        showLoadingState(false);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showErrorAndGoBack("خطأ في بدء الكويز: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void setupQuiz() {
        // إعداد معلومات الكويز
        lblQuizTitle.setText(currentQuiz.getTitle());

        // إعداد الأسئلة
        this.questions = currentQuiz.getQuestionsForAttempt();

        if (questions.isEmpty()) {
            showErrorAndGoBack("لا توجد أسئلة في هذا الكويز");
            return;
        }

        // بدء العداد الزمني إذا كان هناك حد زمني
        if (currentQuiz.hasTimeLimit()) {
            startTimer();
            lblTimeRemaining.setVisible(true);
        }

        // إظهار السؤال الأول
        currentQuestionIndex = 0;
        showQuestion(currentQuestionIndex);
        updateProgress();
    }

    private void showQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;

        Question question = questions.get(index);

        // تحديث عداد الأسئلة
        lblQuestionCounter.setText(String.format("السؤال %d من %d",
                index + 1, questions.size()));

        // إظهار نص السؤال
        lblQuestionText.setText(question.getQuestionText());

        // إظهار الخيارات
        displayQuestionOptions(question);

        // تحديث حالة الأزرار
        updateNavigationButtons();
    }

    private void displayQuestionOptions(Question question) {
        optionsContainer.getChildren().clear();

        List<QuestionOption> options = question.getOptions();
        ToggleGroup toggleGroup = new ToggleGroup();

        for (QuestionOption option : options) {
            RadioButton radioButton = new RadioButton(option.getOptionText());
            radioButton.setToggleGroup(toggleGroup);
            radioButton.setUserData(option.getId());
            radioButton.setWrapText(true);
            radioButton.setPadding(new Insets(8, 0, 8, 0));
            radioButton.getStyleClass().add("quiz-option");

            // تحديد الإجابة المحفوظة مسبقاً
            Long savedAnswer = selectedAnswers.get(question.getId());
            if (savedAnswer != null && savedAnswer.equals(option.getId())) {
                radioButton.setSelected(true);
            }

            optionsContainer.getChildren().add(radioButton);
        }
    }

    private void previousQuestion() {
        if (currentQuestionIndex > 0) {
            saveCurrentAnswerSilently();
            currentQuestionIndex--;
            showQuestion(currentQuestionIndex);
            updateProgress();
        }
    }

    private void nextQuestion() {
        if (currentQuestionIndex < questions.size() - 1) {
            saveCurrentAnswerSilently();
            currentQuestionIndex++;
            showQuestion(currentQuestionIndex);
            updateProgress();
        }
    }

    private void saveCurrentAnswer() {
        Question currentQuestion = questions.get(currentQuestionIndex);
        Long selectedOptionId = getSelectedOptionId();

        if (selectedOptionId != null) {
            selectedAnswers.put(currentQuestion.getId(), selectedOptionId);

            // إرسال الإجابة للخادم
            apiService.submitAnswer(currentAttempt.getId(),
                            currentQuestion.getId(),
                            selectedOptionId)
                    .thenAccept(updatedAttempt -> {
                        Platform.runLater(() -> {
                            this.currentAttempt = updatedAttempt;
                            showSuccessMessage("تم حفظ الإجابة");
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            showErrorMessage("فشل حفظ الإجابة: " + ex.getMessage());
                        });
                        return null;
                    });
        } else {
            showWarningMessage("يرجى اختيار إجابة أولاً");
        }
    }

    private void saveCurrentAnswerSilently() {
        Question currentQuestion = questions.get(currentQuestionIndex);
        Long selectedOptionId = getSelectedOptionId();

        if (selectedOptionId != null) {
            selectedAnswers.put(currentQuestion.getId(), selectedOptionId);
        }
    }

    private Long getSelectedOptionId() {
        return optionsContainer.getChildren().stream()
                .filter(node -> node instanceof RadioButton)
                .map(node -> (RadioButton) node)
                .filter(RadioButton::isSelected)
                .map(rb -> (Long) rb.getUserData())
                .findFirst()
                .orElse(null);
    }

    private void submitQuiz() {
        // تأكيد الإرسال
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("تأكيد الإرسال");
        confirmAlert.setHeaderText("هل أنت متأكد من إرسال الكويز؟");
        confirmAlert.setContentText("لن تتمكن من تعديل إجاباتك بعد الإرسال.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            // حفظ الإجابة الحالية
            saveCurrentAnswerSilently();

            // إرسال الكويز
            showLoadingState(true);

            apiService.completeQuizAttempt(currentAttempt.getId())
                    .thenAccept(resultMap -> {
                        Platform.runLater(() -> {
                            stopTimer();
                            showQuizResults(resultMap);
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            showLoadingState(false);
                            showErrorMessage("خطأ في إرسال الكويز: " + ex.getMessage());
                        });
                        return null;
                    });
        }
    }

    private void showQuizResults(Map<String, Object> resultMap) {
        // انتقل إلى صفحة النتائج عبر MainController
        QuizResultsPageController resultsController = new QuizResultsPageController(mainController);
        VBox resultsContent = resultsController.createResultsPage(currentQuiz, currentAttempt, courseId, resultMap);

        mainController.setContent(resultsContent);
        mainController.updateStatus("نتائج الكويز - " + currentQuiz.getTitle());
    }

    private void startTimer() {
        if (currentQuiz.getTimeLimitMinutes() == null) return;

        quizStartTime = LocalDateTime.now();

        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateTimeDisplay())
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void updateTimeDisplay() {
        if (quizStartTime == null) return;

        long remainingMinutes = currentAttempt.getRemainingMinutes(
                currentQuiz.getTimeLimitMinutes()
        );

        if (remainingMinutes <= 0) {
            // انتهى الوقت - إرسال تلقائي
            Platform.runLater(() -> {
                stopTimer();
                autoSubmitQuiz();
            });
            return;
        }

        // تحديث العرض
        String timeText = String.format("%02d:%02d",
                remainingMinutes / 60,
                remainingMinutes % 60);

        Platform.runLater(() -> {
            lblTimeRemaining.setText("الوقت المتبقي: " + timeText);

            // تغيير اللون عند اقتراب انتهاء الوقت
            if (remainingMinutes <= 5) {
                lblTimeRemaining.setTextFill(Color.RED);
            } else if (remainingMinutes <= 10) {
                lblTimeRemaining.setTextFill(Color.ORANGE);
            }
        });
    }

    private void autoSubmitQuiz() {
        showWarningMessage("انتهى الوقت المحدد! سيتم إرسال الكويز تلقائياً.");

        saveCurrentAnswerSilently();

        apiService.completeQuizAttempt(currentAttempt.getId())
                .thenAccept(resultMap -> {
                    Platform.runLater(() -> showQuizResults(resultMap));
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showErrorAndGoBack("خطأ في الإرسال التلقائي: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void updateNavigationButtons() {
        btnPrevious.setDisable(currentQuestionIndex == 0);
        btnNext.setDisable(currentQuestionIndex == questions.size() - 1);

        // إظهار زر الإرسال في السؤال الأخير
        boolean isLastQuestion = currentQuestionIndex == questions.size() - 1;
        btnSubmitQuiz.setVisible(isLastQuestion);
        btnNext.setVisible(!isLastQuestion);
    }

    private void updateProgress() {
        double progress = (double) (currentQuestionIndex + 1) / questions.size();
        progressBar.setProgress(progress);
    }

    private void goBackToCourse() {
        if (currentAttempt != null && !currentAttempt.getIsCompleted()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("تأكيد الخروج");
            confirmAlert.setHeaderText("هل أنت متأكد من الخروج من الكويز؟");
            confirmAlert.setContentText("سيتم فقدان التقدم الحالي إذا لم تقم بحفظ الإجابات.");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                stopTimer();
                // العودة للكورس
                mainController.navigateToCourses();
            }
        } else {
            // العودة للكورس مباشرة
            mainController.navigateToCourses();
        }
    }

    // Helper methods للـ API calls
    private java.util.concurrent.CompletableFuture<QuizAttempt> startQuizAttempt(Long quizId) {
        return apiService.startQuizAttempt(quizId, userId);
    }

    // UI Helper methods
    private void showLoadingState(boolean loading) {
        if (loading) {
            mainController.showLoadingInMainArea(true);
        }
    }

    private void showSuccessMessage(String message) {
        // يمكن إضافة notification system لاحقاً
        System.out.println("Success: " + message);
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
        alert.show();
    }

    private void showErrorAndGoBack(String message) {
        showErrorMessage(message);
        mainController.navigateToCourses();
    }
}