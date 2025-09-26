package com.techway.coursemanagementdesktop.controller;


import com.techway.coursemanagementdesktop.model.*;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Controller لواجهة حل الكويز
 */
public class QuizTakingController implements Initializable {

    @FXML private Label lblQuizTitle;
    @FXML private Label lblQuizDescription;
    @FXML private Label lblQuestionCounter;
    @FXML private Label lblTimeRemaining;
    @FXML private ProgressBar progressBar;

    @FXML private VBox questionContainer;
    @FXML private Label lblQuestionText;
    @FXML private VBox optionsContainer;
    @FXML private TextArea txtExplanation;

    @FXML private Button btnPrevious;
    @FXML private Button btnNext;
    @FXML private Button btnSubmitQuiz;
    @FXML private Button btnSaveAnswer;

    // Data
    private Quiz currentQuiz;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private QuizAttempt currentAttempt;
    private Map<Long, Long> selectedAnswers = new HashMap<>(); // questionId -> selectedOptionId
    private Timeline timer;
    private LocalDateTime quizStartTime;

    // Services
    private ApiService apiService;
    private Long courseId;
    private Long userId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.apiService = ApiService.getInstance();
        this.userId = SessionManager.getInstance().getCurrentUserId();

        setupUI();
    }

    private void setupUI() {
        // إعداد الأزرار
        btnPrevious.setOnAction(e -> previousQuestion());
        btnNext.setOnAction(e -> nextQuestion());
        btnSubmitQuiz.setOnAction(e -> submitQuiz());
        btnSaveAnswer.setOnAction(e -> saveCurrentAnswer());

        // إخفاء زر الإرسال في البداية
        btnSubmitQuiz.setVisible(false);

        // إعداد النص التوضيحي
        txtExplanation.setVisible(false);
        txtExplanation.setEditable(false);
        txtExplanation.setWrapText(true);
        txtExplanation.setPrefHeight(100);
    }

    /**
     * بدء الكويز لكورس معين
     */
    public void startQuiz(Long courseId) {
        this.courseId = courseId;

        // إظهار شاشة التحميل
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
                        showErrorAlert("خطأ في بدء الكويز", ex.getMessage());
                        showLoadingState(false);
                    });
                    return null;
                });
    }

    private CompletableFuture<QuizAttempt> startQuizAttempt(Long quizId) {
        return apiService.startQuizAttempt(quizId, userId);
    }

    private void setupQuiz() {
        // إعداد معلومات الكويز
        lblQuizTitle.setText(currentQuiz.getTitle());
        lblQuizDescription.setText(currentQuiz.getDescription());

        // إعداد الأسئلة
        this.questions = currentQuiz.getQuestionsForAttempt(); // قد تكون مخلوطة

        if (questions.isEmpty()) {
            showErrorAlert("خطأ", "لا توجد أسئلة في هذا الكويز");
            return;
        }

        // بدء العداد الزمني إذا كان هناك حد زمني
        if (currentQuiz.hasTimeLimit()) {
            startTimer();
        } else {
            lblTimeRemaining.setVisible(false);
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

        // إخفاء الشرح
        txtExplanation.setVisible(false);
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

            // تحديد الإجابة المحفوظة مسبقاً
            Long savedAnswer = selectedAnswers.get(question.getId());
            if (savedAnswer != null && savedAnswer.equals(option.getId())) {
                radioButton.setSelected(true);
            }

            optionsContainer.getChildren().add(radioButton);
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

    private Long getSelectedOptionId() {
        return optionsContainer.getChildren().stream()
                .filter(node -> node instanceof RadioButton)
                .map(node -> (RadioButton) node)
                .filter(RadioButton::isSelected)
                .map(rb -> (Long) rb.getUserData())
                .findFirst()
                .orElse(null);
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

    private void saveCurrentAnswerSilently() {
        Question currentQuestion = questions.get(currentQuestionIndex);
        Long selectedOptionId = getSelectedOptionId();

        if (selectedOptionId != null) {
            selectedAnswers.put(currentQuestion.getId(), selectedOptionId);
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
                            showLoadingState(false);
                            showQuizResults(resultMap);
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            showLoadingState(false);
                            showErrorAlert("خطأ في إرسال الكويز", ex.getMessage());
                        });
                        return null;
                    });
        }
    }

    private void showQuizResults(Map<String, Object> resultMap) {
        stopTimer();

        Boolean passed = (Boolean) resultMap.get("passed");
        Double score = (Double) resultMap.get("score");
        Boolean certificateGenerated = (Boolean) resultMap.get("certificateGenerated");

        // إنشاء نافذة النتائج
        Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
        resultAlert.setTitle("نتائج الكويز");

        String message = String.format(
                "تم إنهاء الكويز بنجاح!\n\n" +
                        "النتيجة: %.1f%%\n" +
                        "الحالة: %s\n" +
                        "النسبة المطلوبة للنجاح: %s\n\n",
                score,
                passed ? "نجح" : "لم ينجح",
                currentQuiz.getPassingScoreDisplay()
        );

        if (certificateGenerated) {
            message += "🎉 تم إصدار شهادة إتمام الكورس!\n" +
                    "يمكنك تحميلها من قائمة شهاداتي.";
        }

        resultAlert.setHeaderText(passed ? "تهانينا!" : "حاول مرة أخرى");
        resultAlert.setContentText(message);

        // تلوين النتيجة
        if (passed) {
            resultAlert.getDialogPane().setStyle("-fx-background-color: #E8F5E8;");
        }

        resultAlert.showAndWait();

        // العودة لصفحة الكورس
        // TODO: إضافة navigation logic
    }

    private void startTimer() {
        if (currentQuiz.getTimeLimitMinutes() == null) return;

        quizStartTime = LocalDateTime.now();

        // إنشاء Timeline للعداد
        // استخدم المتغير timer الموجود في الكلاس بدل إنشاء جديد
        timer = new Timeline(
                new KeyFrame(
                        javafx.util.Duration.seconds(1),
                        e -> updateTimeDisplay()
                )
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
                        showErrorAlert("خطأ في الإرسال التلقائي", ex.getMessage());
                    });
                    return null;
                });
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    // Utility Methods
    private void showLoadingState(boolean loading) {
        // TODO: إضافة شاشة تحميل
        questionContainer.setDisable(loading);
        btnSubmitQuiz.setDisable(loading);
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessMessage(String message) {
        // TODO: إضافة Notification system
        System.out.println("Success: " + message);
    }

    private void showErrorMessage(String message) {
        // TODO: إضافة Notification system
        System.err.println("Error: " + message);
    }

    private void showWarningMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("تنبيه");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}