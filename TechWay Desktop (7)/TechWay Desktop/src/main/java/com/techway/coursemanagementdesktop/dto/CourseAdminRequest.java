package com.techway.coursemanagementdesktop.dto;


public class CourseAdminRequest {
    private String title;
    private String instructor;
    private String status;
    private String duration;
    private String description;
    private String imageUrl;
    private Double price;
    private Boolean isFree;

    public CourseAdminRequest() {
    }

    public CourseAdminRequest(String title, Double price, String status) {
        this.title = title;
        this.price = price;
        this.status = status;
        this.isFree = price == null || price <= 0.0;
    }

    public CourseAdminRequest(String title, String instructor, String status, String duration, String description, String imageUrl, Double price, Boolean isFree) {
        this.title = title;
        this.instructor = instructor;
        this.status = status;
        this.duration = duration;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.isFree = isFree;
    }
    // Getters and setters (اختياري إذا تحتاج تستخدمه مع مكتبات مثل Jackson)

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
        this.isFree = price == null || price <= 0.0;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public void setIsFree(Boolean isFree) {
        this.isFree = isFree;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getFree() {
        return isFree;
    }

    public void setFree(Boolean free) {
        isFree = free;
    }
}
