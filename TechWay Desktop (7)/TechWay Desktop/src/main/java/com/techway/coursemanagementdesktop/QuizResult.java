package com.techway.coursemanagementdesktop;

public class QuizResult {
    private double score;
    private int correctAnswers;
    private int totalQuestions;
    private boolean passed;

    public QuizResult(double score, int correctAnswers, int totalQuestions, boolean passed) {
        this.score = score;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.passed = passed;
    }

    // Getters
    public double getScore() { return score; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getTotalQuestions() { return totalQuestions; }
    public boolean isPassed() { return passed; }
}