package com.techway.coursemanagementdesktop.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Quiz {
    private Long id;
    private String title;
    private String description;
    private Double passingScore;
    private Integer timeLimitMinutes;
    private Integer maxAttempts;
    private Boolean shuffleQuestions;
    private LocalDateTime createdAt;

    private List<Question> questions;
    private Course course;

    // Constructors
    public Quiz() {}

    public Quiz(Long id, String title, String description, double passingScore, List<Question> questions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.passingScore = passingScore;
        this.questions = questions;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public Double getPassingScore() { return passingScore != null ? passingScore : 70.0; }
    public void setPassingScore(Double passingScore) { this.passingScore = passingScore; }

    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

    public Boolean getShuffleQuestions() { return shuffleQuestions != null ? shuffleQuestions : false; }
    public void setShuffleQuestions(Boolean shuffleQuestions) { this.shuffleQuestions = shuffleQuestions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Question> getQuestions() {
        return questions != null ? questions : new ArrayList<>();
    }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    // Helper Methods للواجهة

    /**
     * يعطي عدد الأسئلة في الكويز
     */
    public int getQuestionCount() {
        return getQuestions().size();
    }

    /**
     * يتحقق من وجود حد زمني
     */
    public boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }

    /**
     * يتحقق من وجود حد للمحاولات
     */
    public boolean hasMaxAttempts() {
        return maxAttempts != null && maxAttempts > 0;
    }

    /**
     * يعطي نص وصفي للوقت المحدد
     */
    public String getTimeLimitDisplay() {
        if (!hasTimeLimit()) return "لا يوجد حد زمني";

        int minutes = getTimeLimitMinutes();
        if (minutes < 60) {
            return minutes + " دقيقة";
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + " ساعة";
            } else {
                return hours + " ساعة و " + remainingMinutes + " دقيقة";
            }
        }
    }

    /**
     * يعطي نص وصفي لعدد المحاولات المسموحة
     */
    public String getMaxAttemptsDisplay() {
        if (!hasMaxAttempts()) return "محاولات لا نهائية";
        return getMaxAttempts() + " محاولات";
    }

    /**
     * يعطي نص وصفي لدرجة النجاح
     */
    public String getPassingScoreDisplay() {
        return String.format("%.0f%%", getPassingScore());
    }

    /**
     * يحضر الأسئلة مع إمكانية الخلط
     */
    public List<Question> getQuestionsForAttempt() {
        List<Question> questionsToUse = new ArrayList<>(getQuestions());

        if (Boolean.TRUE.equals(getShuffleQuestions())) {
            Collections.shuffle(questionsToUse);
        }

        return questionsToUse;
    }

    /**
     * يتحقق من صحة الكويز (وجود أسئلة)
     */
    public boolean isValid() {
        return getQuestionCount() > 0 &&
                getPassingScore() != null && getPassingScore() > 0 && getPassingScore() <= 100;
    }

    /**
     * يعطي ملخص سريع عن الكويز
     */
    public String getSummary() {
        return String.format("%d سؤال | النجاح: %s | %s",
                getQuestionCount(),
                getPassingScoreDisplay(),
                getTimeLimitDisplay());
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", questionCount=" + getQuestionCount() +
                ", passingScore=" + passingScore +
                ", timeLimitMinutes=" + timeLimitMinutes +
                '}';
    }
}