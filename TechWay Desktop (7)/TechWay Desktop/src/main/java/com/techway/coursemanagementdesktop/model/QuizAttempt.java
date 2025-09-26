package com.techway.coursemanagementdesktop.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QuizAttempt {

    private Long id;
    private Long userId;
    private Long quizId;
    private Integer attemptNumber = 1;
    private Double scorePercentage;
    private Integer correctAnswers = 0;
    private Integer totalQuestions = 0;
    private Boolean isPassed = false;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Boolean isCompleted = false;
    private Integer timeTakenMinutes;

    private List<QuizAnswer> answers = new ArrayList<>();

    // Constructors
    public QuizAttempt() {}

    public QuizAttempt(Long userId, Long quizId, Integer attemptNumber) {
        this.userId = userId;
        this.quizId = quizId;
        this.attemptNumber = attemptNumber;
        this.startedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }

    public Integer getAttemptNumber() {
        return attemptNumber != null ? attemptNumber : 1;
    }
    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Double getScorePercentage() { return scorePercentage; }
    public void setScorePercentage(Double scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public Integer getCorrectAnswers() {
        return correctAnswers != null ? correctAnswers : 0;
    }
    public void setCorrectAnswers(Integer correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public Integer getTotalQuestions() {
        return totalQuestions != null ? totalQuestions : 0;
    }
    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Boolean getIsPassed() {
        return isPassed != null ? isPassed : false;
    }
    public void setIsPassed(Boolean isPassed) { this.isPassed = isPassed; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Boolean getIsCompleted() {
        return isCompleted != null ? isCompleted : false;
    }
    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public Integer getTimeTakenMinutes() { return timeTakenMinutes; }
    public void setTimeTakenMinutes(Integer timeTakenMinutes) {
        this.timeTakenMinutes = timeTakenMinutes;
    }

    public List<QuizAnswer> getAnswers() {
        return answers != null ? answers : new ArrayList<>();
    }
    public void setAnswers(List<QuizAnswer> answers) {
        this.answers = answers != null ? answers : new ArrayList<>();
    }

    // Helper Methods للواجهة

    /**
     * يحسب النسبة المئوية للنجاح
     */
    public String getScoreDisplay() {
        if (scorePercentage == null) return "لم يكتمل";
        return String.format("%.1f%%", scorePercentage);
    }

    /**
     * يعطي نص حالة النجاح/الفشل
     */
    public String getPassStatusDisplay() {
        if (!getIsCompleted()) return "لم يكتمل";
        return getIsPassed() ? "نجح" : "لم ينجح";
    }

    /**
     * يعطي لون حالة النجاح للواجهة
     */
    public String getPassStatusColor() {
        if (!getIsCompleted()) return "#FFA500"; // برتقالي للغير مكتمل
        return getIsPassed() ? "#4CAF50" : "#F44336"; // أخضر للنجاح، أحمر للفشل
    }

    /**
     * يعطي نسبة الإجابات الصحيحة
     */
    public String getCorrectAnswersDisplay() {
        return String.format("%d من %d", getCorrectAnswers(), getTotalQuestions());
    }

    /**
     * يحسب الوقت المستغرق في تنسيق مقروء
     */
    public String getTimeTakenDisplay() {
        if (timeTakenMinutes == null) return "غير محدد";

        if (timeTakenMinutes < 60) {
            return timeTakenMinutes + " دقيقة";
        } else {
            int hours = timeTakenMinutes / 60;
            int minutes = timeTakenMinutes % 60;
            if (minutes == 0) {
                return hours + " ساعة";
            } else {
                return hours + " ساعة و " + minutes + " دقيقة";
            }
        }
    }

    /**
     * يحسب الوقت المستغرق بالدقائق من البداية والنهاية
     */
    public void calculateTimeTaken() {
        if (startedAt != null && completedAt != null) {
            Duration duration = Duration.between(startedAt, completedAt);
            this.timeTakenMinutes = (int) duration.toMinutes();
        }
    }

    /**
     * يعطي تاريخ ووقت البدء بتنسيق مقروء
     */
    public String getStartedAtDisplay() {
        if (startedAt == null) return "غير محدد";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return startedAt.format(formatter);
    }

    /**
     * يعطي تاريخ ووقت الانتهاء بتنسيق مقروء
     */
    public String getCompletedAtDisplay() {
        if (completedAt == null) return "لم يكتمل";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return completedAt.format(formatter);
    }

    /**
     * يحسب الوقت المتبقي بناء على حد زمني معين
     */
    public long getRemainingMinutes(Integer timeLimitMinutes) {
        if (startedAt == null || timeLimitMinutes == null || getIsCompleted()) {
            return 0;
        }

        Duration elapsed = Duration.between(startedAt, LocalDateTime.now());
        long elapsedMinutes = elapsed.toMinutes();
        return Math.max(0, timeLimitMinutes - elapsedMinutes);
    }

    /**
     * يتحقق من انتهاء الوقت المحدد
     */
    public boolean isTimeExpired(Integer timeLimitMinutes) {
        return getRemainingMinutes(timeLimitMinutes) <= 0;
    }

    /**
     * يعطي نسبة التقدم في الإجابة
     */
    public double getProgressPercentage() {
        if (getTotalQuestions() == 0) return 0.0;
        return (getAnswers().size() * 100.0) / getTotalQuestions();
    }

    /**
     * يتحقق من أن جميع الأسئلة تم الإجابة عليها
     */
    public boolean isAllQuestionsAnswered() {
        return getAnswers().size() >= getTotalQuestions();
    }

    /**
     * يعطي ملخص سريع للمحاولة
     */
    public String getSummary() {
        if (!getIsCompleted()) {
            return String.format("المحاولة %d - جارية", getAttemptNumber());
        } else {
            return String.format("المحاولة %d - %s (%s)",
                    getAttemptNumber(),
                    getPassStatusDisplay(),
                    getScoreDisplay());
        }
    }

    @Override
    public String toString() {
        return "QuizAttempt{" +
                "id=" + id +
                ", userId=" + userId +
                ", quizId=" + quizId +
                ", attemptNumber=" + attemptNumber +
                ", scorePercentage=" + scorePercentage +
                ", isPassed=" + isPassed +
                ", isCompleted=" + isCompleted +
                '}';
    }
}