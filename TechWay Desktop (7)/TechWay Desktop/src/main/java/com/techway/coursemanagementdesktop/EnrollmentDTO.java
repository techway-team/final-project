package com.techway.coursemanagementdesktop;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EnrollmentDTO {
    private Long id;
    private Long userId;
    private Long courseId;
    private String courseTitle;
    private String courseImage;
    private String enrolledAt; // <-- هنا التغيير الوحيد
    private String status;
    private BigDecimal progress;
    private boolean completed;
    private boolean isPaid;




    // ✅ Default constructor
    public EnrollmentDTO() {}

    public EnrollmentDTO(Long id, Long userId, Long courseId, String courseTitle, String courseImage,
                         String enrolledAt, String status, BigDecimal progress,boolean completed) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.courseImage = courseImage;
        this.enrolledAt = enrolledAt;
        this.status = status;
        this.progress = progress;
        this.completed=completed;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCourseImage() {
        return courseImage;
    }

    public void setCourseImage(String courseImage) {
        this.courseImage = courseImage;
    }

    public String getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(String enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getProgress() {
        return progress;
    }

    public void setProgress(BigDecimal progress) {
        this.progress = progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

}