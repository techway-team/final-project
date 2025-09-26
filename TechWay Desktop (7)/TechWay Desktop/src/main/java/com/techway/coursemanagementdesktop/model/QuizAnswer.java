package com.techway.coursemanagementdesktop.model;

import com.techway.coursemanagementdesktop.CertificateDTO;

import java.time.LocalDateTime;

public class QuizAnswer {

    private Long id;
    private Long quizAttemptId;   // بدل QuizAttempt object نستخدم ID فقط
    private Long questionId;      // بدل Question object نستخدم ID فقط
    private Long selectedOptionId; // بدل QuestionOption object نستخدم ID فقط
    private Boolean isCorrect = false;
    private LocalDateTime answeredAt;

    public QuizAnswer() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuizAttemptId() {
        return quizAttemptId;
    }

    public void setQuizAttemptId(Long quizAttemptId) {
        this.quizAttemptId = quizAttemptId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Long selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }


}
