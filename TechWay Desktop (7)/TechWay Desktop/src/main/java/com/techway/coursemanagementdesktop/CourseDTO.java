package com.techway.coursemanagementdesktop;

import com.techway.coursemanagementdesktop.model.Course;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CourseDTO {
    private Long id;
    private String title;
    private String description;
    private String location;
    private Integer duration;
    private BigDecimal price;
    private Boolean isFree;
    private String instructor;
    private String imageUrl;
    private LocalDateTime createdAt;
    private String status;

    public CourseDTO() {}

    public CourseDTO(Long id, String title, String description, String location,
                     Integer duration, BigDecimal price, Boolean isFree,
                     String instructor, String imageUrl, LocalDateTime createdAt, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.duration = duration;
        this.price = price;
        this.isFree = isFree;
        this.instructor = instructor;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static CourseDTO fromCourse(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setPrice(course.getPrice());
        dto.setDescription(course.getDescription());
        dto.setLocation(course.getLocation());
        dto.setDuration(course.getDuration());
        dto.setFree(course.getIsFree());
        dto.setInstructor(course.getInstructor());
        dto.setImageUrl(course.getImageUrl());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setStatus(course.getStatus());

        // انسخ باقي الخصائص اللي تحتاجها
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getFree() {
        return isFree;
    }

    public void setFree(Boolean free) {
        isFree = free;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // getters & setters ...
}