package com.techway.coursemanagementdesktop;


import com.itextpdf.kernel.colors.ColorConstants;
import com.techway.coursemanagementdesktop.model.Question;
import com.techway.coursemanagementdesktop.model.QuestionOption;
import com.techway.coursemanagementdesktop.model.Quiz;
import com.techway.coursemanagementdesktop.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class QuizView {

    private Stage stage;

    // Data
    private Quiz quiz;
    private int currentQuestionIndex = 0;
    private Map<Long, Long> selectedAnswers = new HashMap<>();
    private boolean quizCompleted = false;
    private QuizResult quizResult;
    private User user;

    // UI
    private BorderPane root;
    private VBox introPane;
    private VBox quizPane;
    private VBox resultPane;
    private Label questionLabel;
    private VBox optionsBox;
    private ProgressBar progressBar;
    private Label progressText;
    private Button nextButton;
    private Button prevButton;
    private Button finishButton;

    public QuizView(Stage stage, Quiz quiz, User user) {
        this.stage = stage;
        this.quiz = quiz;
        this.user=user;
        this.root = new BorderPane();
        initIntroPane();
        initQuizPane();
        initResultPane();

        // Start with intro
        root.setCenter(introPane);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("اختبار - " + quiz.getTitle());
        stage.show();
    }

    private void initIntroPane() {
        introPane = new VBox(15);
        introPane.setAlignment(Pos.CENTER);
        introPane.setPadding(new Insets(20));

        Label title = new Label(quiz.getTitle());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label desc = new Label(quiz.getDescription());
        desc.setWrapText(true);

        Button startBtn = new Button("ابدأ الاختبار");
        startBtn.setOnAction(e -> startQuiz());

        introPane.getChildren().addAll(title, desc, startBtn);
    }

    private void initQuizPane() {
        quizPane = new VBox(15);
        quizPane.setPadding(new Insets(20));

        // Progress
        progressText = new Label();
        progressBar = new ProgressBar(0);

        // Question
        questionLabel = new Label();
        questionLabel.setWrapText(true);
        questionLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        optionsBox = new VBox(10);

        HBox navButtons = new HBox(10);
        navButtons.setAlignment(Pos.CENTER_RIGHT);

        prevButton = new Button("السؤال السابق");
        nextButton = new Button("السؤال التالي");
        finishButton = new Button("إنهاء الاختبار");

        prevButton.setOnAction(e -> previousQuestion());
        nextButton.setOnAction(e -> nextQuestion());
        finishButton.setOnAction(e -> submitQuiz());

        navButtons.getChildren().addAll(prevButton, nextButton, finishButton);

        quizPane.getChildren().addAll(progressText, progressBar, questionLabel, optionsBox, navButtons);
    }

    private void initResultPane() {
        resultPane = new VBox(20);
        resultPane.setAlignment(Pos.CENTER);
        resultPane.setPadding(new Insets(20));
    }

    private void startQuiz() {
        currentQuestionIndex = 0;
        selectedAnswers.clear();
        quizCompleted = false;
        updateQuizUI();
        root.setCenter(quizPane);
    }

    private void updateQuizUI() {
        Question question = quiz.getQuestions().get(currentQuestionIndex);
        questionLabel.setText(question.getQuestionText());

        // Progress
        progressText.setText("السؤال " + (currentQuestionIndex + 1) + " من " + quiz.getQuestions().size());
        progressBar.setProgress((double) (currentQuestionIndex + 1) / quiz.getQuestions().size());

        // Options
        optionsBox.getChildren().clear();
        ToggleGroup group = new ToggleGroup();
        for (QuestionOption opt : question.getOptions()) {
            RadioButton rb = new RadioButton(opt.getOptionText());
            rb.setUserData(opt.getId());
            rb.setToggleGroup(group);
            if (Objects.equals(selectedAnswers.get(question.getId()), opt.getId())) {
                rb.setSelected(true);
            }
            rb.setOnAction(e -> selectedAnswers.put(question.getId(), (Long) rb.getUserData()));
            optionsBox.getChildren().add(rb);
        }

        // Buttons visibility
        prevButton.setDisable(currentQuestionIndex == 0);
        nextButton.setVisible(currentQuestionIndex < quiz.getQuestions().size() - 1);
        finishButton.setVisible(currentQuestionIndex == quiz.getQuestions().size() - 1);
    }

    private void nextQuestion() {
        if (currentQuestionIndex < quiz.getQuestions().size() - 1) {
            currentQuestionIndex++;
            updateQuizUI();
        }
    }

    private void previousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            updateQuizUI();
        }
    }

    private void submitQuiz() {
        int correct = 0;
        for (Question q : quiz.getQuestions()) {
            Long selected = selectedAnswers.get(q.getId());
            if (selected != null && q.getOptions().stream().anyMatch(o -> o.getId().equals(selected) && o.getIsCorrect())) {
                correct++;
            }
        }

        double score = ((double) correct / quiz.getQuestions().size()) * 100;
        boolean passed = score >= quiz.getPassingScore();

        quizResult = new QuizResult(score, correct, quiz.getQuestions().size(), passed);

        showResults();
    }

    private void showResults() {
        resultPane.getChildren().clear();

        Label resultTitle = new Label(quizResult.isPassed() ? "🎉 أحسنت! نجحت" : "❌ للأسف لم تنجح");
        resultTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label scoreLbl = new Label("النتيجة: " + Math.round(quizResult.getScore()) + "%");
        Label detailLbl = new Label("إجابات صحيحة: " + quizResult.getCorrectAnswers() + " من " + quizResult.getTotalQuestions());

        Button certBtn = new Button("تحميل الشهادة");
        certBtn.setDisable(!quizResult.isPassed());
        certBtn.setOnAction(e -> generateCertificate());

        Button retryBtn = new Button("إعادة المحاولة");
        retryBtn.setOnAction(e -> {
            startQuiz();
        });

        resultPane.getChildren().addAll(resultTitle, scoreLbl, detailLbl, certBtn, retryBtn);
        root.setCenter(resultPane);
    }

    private void generateCertificate() {
        try {
            String fileName = "Certificate_" + user.getName() + "_" + quiz.getTitle() + ".pdf";
            String dest = System.getProperty("user.home") + "/Desktop/" + fileName;

            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(dest);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

            // 🎨 إضافة إطار للصفحة
            float width = pdf.getDefaultPageSize().getWidth();
            float height = pdf.getDefaultPageSize().getHeight();

            com.itextpdf.kernel.pdf.canvas.PdfCanvas canvas = new com.itextpdf.kernel.pdf.canvas.PdfCanvas(pdf.addNewPage());
            canvas.setLineWidth(4f);
            canvas.setStrokeColor(com.itextpdf.kernel.colors.ColorConstants.BLUE);
            canvas.rectangle(30, 30, width - 60, height - 60);
            canvas.stroke();

            // 🖼️ (اختياري) إضافة شعار أعلى الشهادة
            // String logoPath = "src/main/resources/logo.png";
            // ImageData imageData = ImageDataFactory.create(logoPath);
            // Image logo = new Image(imageData).scaleToFit(120, 120).setFixedPosition(width/2 - 60, height - 150);
            // document.add(logo);

            // 🎓 العنوان
            com.itextpdf.layout.element.Paragraph title = new com.itextpdf.layout.element.Paragraph("شهادة إتمام")
                    .setFontSize(30)
                    .setBold()
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);

            // 📝 المحتوى
            String content = "تشهد منصة TechWay أن الطالب/ة:\n\n" +
                    user.getName() +
                    "\n\nقد أتم بنجاح اختبار الكورس:\n\n" +
                    quiz.getTitle() +
                    "\n\nبنتيجة: " + Math.round(quizResult.getScore()) + "%" +
                    "\n\nتاريخ الإصدار: " + java.time.LocalDate.now();

            com.itextpdf.layout.element.Paragraph body = new com.itextpdf.layout.element.Paragraph(content)
                    .setFontSize(18)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginTop(40);

            // ✍️ توقيع أو سطر إداري
            com.itextpdf.layout.element.Paragraph signature = new com.itextpdf.layout.element.Paragraph("\n\n_______________________\nمدير منصة TechWay")
                    .setFontSize(14)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
                    .setMarginTop(80);

            // ➕ إضافة العناصر للمستند
            document.add(title);
            document.add(new com.itextpdf.layout.element.Paragraph("\n")); // مسافة
            document.add(body);
            document.add(signature);

            document.close();

            // ✅ إشعار بالنجاح
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("تم إنشاء الشهادة");
            alert.setHeaderText("🎓 الشهادة جاهزة");
            alert.setContentText("تم حفظ الشهادة على سطح المكتب:\n" + dest);
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("خطأ");
            error.setHeaderText("فشل إنشاء الشهادة");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }


}
