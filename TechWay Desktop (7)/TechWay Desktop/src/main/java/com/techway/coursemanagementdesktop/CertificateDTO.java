package com.techway.coursemanagementdesktop;

import java.time.LocalDate;

public class CertificateDTO {
    private Long id;
    private String certificateNumber;
    private String userName;
    private String courseTitle;
    private String completionDate; // أو LocalDate أو نوع بيانات يتوافق مع النص
    private Double finalScore;
    private Double quizScore;

    private String issuedAt;
    private String status;
    private boolean valid;


    // Getters and Setters

    public Long getId() { return id;}
    public void setId(Long id) { this.id = id; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getCompletionDate() { return completionDate; }
    public void setCompletionDate(String completionDate) { this.completionDate = completionDate; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public Double getQuizScore() { return quizScore; }
    public void setQuizScore(Double quizScore) { this.quizScore = quizScore; }

    public String getIssuedAt() { return issuedAt; }
    public void setIssuedAt(String issuedAt) { this.issuedAt = issuedAt; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}