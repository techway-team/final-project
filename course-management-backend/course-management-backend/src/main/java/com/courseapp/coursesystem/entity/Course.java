package com.courseapp.coursesystem.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "courses")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String location;

    private Integer duration; // in hours

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_free")
    private Boolean isFree = false;

    @Column(length = 100)
    private String instructor;

    // ✨ إضافة جديدة - رابط الصورة
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Relationships
    @JsonIgnore

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();


    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Review> reviews = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_categories",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @JsonIgnore
    private Set<Category> categories = new HashSet<>();

    private String status; // ACTIVE, INACTIVE, DELETED



    // ===== إضافة حقول الإحداثيات للخرائط =====
    @JsonProperty("latitude")
    @JsonAlias({"lat"})
    private Double latitude;

    @JsonProperty("longitude")
    @JsonAlias({"lng", "lon"})
    private Double longitude;

    // عنوان دقيق للموقع (اختياري)
    @JsonProperty("fullAddress")
    @JsonAlias({"full_address", "address"})
    private String fullAddress;

    // نوع الموقع (حضوري/أونلاين/مختلط)
    @JsonProperty("locationType")
    @JsonAlias({"location_type"})
    private String locationType;


    // Default constructor
    public Course() {}

    // Constructor
    public Course(String title, String description, String location,
                  Integer duration, BigDecimal price, Boolean isFree, String instructor) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.duration = duration;
        this.price = price;
        this.isFree = isFree;
        this.instructor = instructor;
    }

    // ✨ Constructor محدث مع imageUrl
    public Course(String title, String description, String location,
                  Integer duration, BigDecimal price, Boolean isFree, String instructor, String imageUrl) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.duration = duration;
        this.price = price;
        this.isFree = isFree;
        this.instructor = instructor;
        this.imageUrl = imageUrl;
    }



    public Course(Long id, String title, String description, String location, Integer duration, BigDecimal price, Boolean isFree, String instructor, String imageUrl, LocalDateTime createdAt, List<Enrollment> enrollments, Set<Review> reviews, Set<Category> categories, String status) {
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
        this.enrollments = enrollments;
        this.reviews = reviews;
        this.categories = categories;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getIsFree() { return isFree; }
    public void setIsFree(Boolean isFree) { this.isFree = isFree; }

    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }

    // ✨ Getter و Setter للصورة
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Enrollment> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<Enrollment> enrollments) {
        this.enrollments = enrollments;
    }

    public Set<Review> getReviews() { return reviews; }
    public void setReviews(Set<Review> reviews) { this.reviews = reviews; }

    public Set<Category> getCategories() { return categories; }
    public void setCategories(Set<Category> categories) { this.categories = categories; }

    public Boolean getFree() {
        return isFree;
    }

    public void setFree(Boolean free) {
        isFree = free;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ===== Getters / Setters للحقول الجديدة =====
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    // ===== مساعدات عرض الأصلية =====
    public String getPriceDisplay() {
        if (Boolean.TRUE.equals(getIsFree())) return "مجاني";
        return getPrice().stripTrailingZeros().toPlainString() + " ريال";
    }

    public String getDurationDisplay() {
        if (duration == null) return "غير محدد";
        return duration + " ساعة";
    }

    public String getLocationDisplay() {
        String loc = getLocation();
        return (loc == null || loc.isBlank()) ? "Online" : loc;
    }

    public String getShortDescription() {
        String d = getDescription();
        if (d == null) return "";
        return d.length() > 100 ? d.substring(0, 100) + "..." : d;
    }


    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", instructor='" + instructor + '\'' +
                ", price=" + price +
                ", isFree=" + isFree +
                ", status=" + status +
                ", imageUrl=" + imageUrl +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", locationType='" + locationType + '\'' +
                '}';
    }
}