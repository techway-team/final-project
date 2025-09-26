package com.techway.coursemanagementdesktop.model;

public class QuestionOption {

    private Long id;
    private String optionText;
    private Boolean isCorrect;
    private Integer optionIndex;

    // Constructors (اختياري)
    public QuestionOption() {}

    public QuestionOption(Long id, String optionText, Boolean isCorrect, Integer optionIndex) {
        this.id = id;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
        this.optionIndex = optionIndex;
    }

    public QuestionOption(Long id, String optionText, Boolean isCorrect) {
        this.id = id;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
    }


    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Integer getOptionIndex() {
        return optionIndex;
    }

    public void setOptionIndex(Integer optionIndex) {
        this.optionIndex = optionIndex;
    }
}
