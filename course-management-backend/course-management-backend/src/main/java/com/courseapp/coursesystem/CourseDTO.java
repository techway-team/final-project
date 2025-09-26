package com.courseapp.coursesystem;

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
    private String fullAddress;

    // ✅ ضيفي الحقول الجديدة
    private Double latitude;
    private Double longitude;

    public CourseDTO() {}

    public CourseDTO(Long id, String title, String description, String location, Integer duration, BigDecimal price, Boolean isFree, String instructor, String imageUrl, LocalDateTime createdAt, String status, String fullAddress, Double latitude, Double longitude) {
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
        this.fullAddress = fullAddress;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    // getters & setters ...
}