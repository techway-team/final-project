package com.techway.coursemanagementdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;



public class Enrollment {


    private Long id;


    private User user;


    private Course course;

    private LocalDateTime enrolledAt = LocalDateTime.now();

    private String status = "ACTIVE"; // ACTIVE, COMPLETED, CANCELLED

    private BigDecimal progress = BigDecimal.ZERO; // 0.00 to 100.00

    private BigDecimal coursePrice; // + getter/setter

    private Boolean isPaid = false;

    private Long courseId;


    // Default constructor
    public Enrollment() {}

    // Constructor
    public Enrollment(User user, Course course) {
        this.user = user;
        this.course = course;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getProgress() { return progress; }
    public void setProgress(BigDecimal progress) { this.progress = progress; }

    public BigDecimal getCoursePrice() {
        return coursePrice;
    }

    public void setCoursePrice(BigDecimal coursePrice) {
        this.coursePrice = coursePrice;
    }

    public Boolean getPaid() {
        return isPaid;
    }

    public void setPaid(Boolean paid) {
        isPaid = paid;
    }

    public void setCourseId(long courseId) {
        this.courseId=courseId;
    }
}