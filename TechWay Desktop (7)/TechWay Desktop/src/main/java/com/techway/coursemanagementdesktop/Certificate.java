package com.techway.coursemanagementdesktop;

import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.model.User;

import java.time.LocalDateTime;

public class Certificate {
    private Long id;
    private String certificateNumber;
    private Long userId;
    private Long courseId;
    private Double finalScore;
    private Double quizScore;
    private String createdAt;
    private LocalDateTime completionDate;

    private User user;      // المستخدم
    private Course course;  // الدورة



    public  Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
    }

    public Double getQuizScore() {
        return quizScore;
    }

    public void setQuizScore(Double quizScore) {
        this.quizScore = quizScore;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }


    // getters & setters
    public User getUser() { return user; }
    public Course getCourse() { return course; }
    public LocalDateTime getCompletionDate() { return completionDate; }


}
