package com.techway.coursemanagementdesktop.model;

public class QuizAttemptView {
    private double scorePercentage;
    private boolean passed;

    public QuizAttemptView(double scorePercentage, boolean passed) {
        this.scorePercentage = scorePercentage;
        this.passed = passed;
    }

    public double getScorePercentage() {
        return scorePercentage;
    }

    public boolean isPassed() {
        return passed;
    }
}
