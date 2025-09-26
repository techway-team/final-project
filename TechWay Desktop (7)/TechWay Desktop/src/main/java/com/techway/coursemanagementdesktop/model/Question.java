package com.techway.coursemanagementdesktop.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Question {

    private Long id;
    private String questionText;
    private String questionType;  // MULTIPLE_CHOICE, TRUE_FALSE, SINGLE_CHOICE
    private Integer orderIndex;
    private Integer points;
    private String explanation;
    private LocalDateTime createdAt;

    private List<QuestionOption> options = new ArrayList<>();

    // Constructors
    public Question() {}

    public Question(Long id, String questionText, List<QuestionOption> options) {
        this.id = id;
        this.questionText = questionText;
        this.options = (options != null) ? options : new ArrayList<>();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestionText() {
        return questionText != null ? questionText : "";
    }
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType != null ? questionType : "SINGLE_CHOICE";
    }
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Integer getOrderIndex() {
        return orderIndex != null ? orderIndex : 1;
    }
    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Integer getPoints() {
        return points != null ? points : 1;
    }
    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<QuestionOption> getOptions() {
        return options != null ? options : new ArrayList<>();
    }
    public void setOptions(List<QuestionOption> options) {
        this.options = (options != null) ? options : new ArrayList<>();
    }

    // Helper Methods للواجهة

    /**
     * يتحقق من وجود خيارات للسؤال
     */
    public boolean hasOptions() {
        return !getOptions().isEmpty();
    }

    /**
     * يعطي عدد الخيارات المتاحة
     */
    public int getOptionCount() {
        return getOptions().size();
    }

    /**
     * يحصل على الإجابة الصحيحة
     */
    public QuestionOption getCorrectOption() {
        return getOptions().stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .findFirst()
                .orElse(null);
    }

    /**
     * يتحقق من وجود إجابة صحيحة محددة
     */
    public boolean hasCorrectAnswer() {
        return getCorrectOption() != null;
    }

    /**
     * يعطي نص وصفي لنوع السؤال
     */
    public String getQuestionTypeDisplay() {
        switch (getQuestionType().toUpperCase()) {
            case "MULTIPLE_CHOICE": return "اختيار متعدد";
            case "TRUE_FALSE": return "صح أم خطأ";
            case "SINGLE_CHOICE": return "اختيار واحد";
            default: return "نوع غير محدد";
        }
    }

    /**
     * يتحقق من صحة السؤال (وجود نص وخيارات وإجابة صحيحة)
     */
    public boolean isValid() {
        return getQuestionText() != null && !getQuestionText().trim().isEmpty() &&
                hasOptions() && hasCorrectAnswer();
    }

    /**
     * يعطي ملخص سريع عن السؤال
     */
    public String getSummary() {
        return String.format("السؤال %d: %d خيارات | %d نقطة",
                getOrderIndex(),
                getOptionCount(),
                getPoints());
    }

    /**
     * يتحقق من أن السؤال هو صح/خطأ
     */
    public boolean isTrueFalse() {
        return "TRUE_FALSE".equals(getQuestionType()) || getOptionCount() == 2;
    }

    /**
     * إضافة خيار جديد للسؤال
     */
    public void addOption(String optionText, boolean isCorrect) {
        QuestionOption option = new QuestionOption();
        option.setOptionText(optionText);
        option.setIsCorrect(isCorrect);
        option.setOptionIndex(getOptions().size() + 1);
        getOptions().add(option);
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", questionText='" + questionText + '\'' +
                ", questionType='" + questionType + '\'' +
                ", orderIndex=" + orderIndex +
                ", points=" + points +
                ", optionCount=" + getOptionCount() +
                '}';
    }
}