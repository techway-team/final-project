package com.techway.coursemanagementdesktop;

import com.techway.coursemanagementdesktop.model.Question;
import com.techway.coursemanagementdesktop.model.QuestionOption;
import com.techway.coursemanagementdesktop.model.Quiz;
import com.techway.coursemanagementdesktop.model.User;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        // خيارات
        QuestionOption o1 = new QuestionOption(1L, "الرياض", true);
        QuestionOption o2 = new QuestionOption(2L, "جدة", false);
        QuestionOption o3 = new QuestionOption(3L, "مكة", false);
        Question q1 = new Question(1L, "ما هي عاصمة السعودية؟", Arrays.asList(o1, o2, o3));

        QuestionOption o4 = new QuestionOption(4L, "JavaFX", true);
        QuestionOption o5 = new QuestionOption(5L, "Vue.js", false);
        QuestionOption o6 = new QuestionOption(6L, "Spring", false);
        Question q2 = new Question(2L, "أي تقنية تستخدم في بناء واجهات سطح المكتب بالجافا؟", Arrays.asList(o4, o5, o6));

        List<Question> questions = Arrays.asList(q1, q2);

        // الكويز
        Quiz quiz = new Quiz(1L, "اختبار تجريبي", "هذا اختبار للتجربة فقط.", 50, questions);

        // عرض واجهة الكويز
        new QuizView(stage, quiz,new User());
    }

    public static void main(String[] args) {
        launch();
    }
}