package com.courseapp.coursesystem;

import com.courseapp.coursesystem.entity.Certificate;

import java.time.LocalDateTime;

public class CertificateDTO {
    private Long id;
    private String certificateNumber;
    private Long userId;
    private Long courseId;
    private Double finalScore;
    private Double quizScore;
    private LocalDateTime issuedAt;
    private LocalDateTime completionDate;
    private String status;
    private Boolean valid;

    public CertificateDTO(Certificate cert) {
        this.id = cert.getId();
        this.certificateNumber = cert.getCertificateNumber();
        this.userId = cert.getUser().getId();     // ← استخراج ID من العلاقة
        this.courseId = cert.getCourse().getId(); // ← استخراج ID من العلاقة
        this.finalScore = cert.getFinalScore();
        this.quizScore = cert.getQuizScore();
        this.issuedAt = cert.getIssuedAt();
        this.completionDate = cert.getCompletionDate();
        this.status = cert.getStatus().name();
        this.valid = cert.isValid();
    }

    public Long getId() {
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

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDateTime completionDate) {
        this.completionDate = completionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }
}
