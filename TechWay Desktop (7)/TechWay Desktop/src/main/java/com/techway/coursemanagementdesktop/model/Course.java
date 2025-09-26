package com.techway.coursemanagementdesktop.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Course Model - matches backend Course entity
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Course {

    private Long id;
    private String title;
    private String description;
    private String location;

    // من الباك احتمال يجي رقم ساعات
    private Integer duration;

    private BigDecimal price;

    @JsonProperty("isFree")
    @JsonAlias({"is_free", "free", "freeCourse"})
    private Boolean isFree;

    private String instructor;

    // حالة الكورس (مطلوبة للأدمن: Published/Draft/Inactive ... الخ)
    private String status;

    // نقبل كل الصيغ الشائعة للصورة
    @JsonProperty("imageUrl")
    @JsonAlias({"image_url", "image-url", "image"})
    private String imageUrl;

    @JsonProperty("createdAt")
    @JsonAlias({"created_at"})
    private LocalDateTime createdAt;

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

    public Course() {
        this.isFree = false;
        this.price = BigDecimal.ZERO;
        this.locationType = "ONLINE"; // افتراضي أونلاين
    }

    // ===== Getters / Setters الأصلية =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public BigDecimal getPrice() { return price != null ? price : BigDecimal.ZERO; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getIsFree() { return isFree != null ? isFree : Boolean.FALSE; }
    public void setIsFree(Boolean isFree) { this.isFree = isFree; }

    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

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

    // ===== مساعدات الخرائط الجديدة =====

    /**
     * يتحقق من وجود إحداثيات صالحة للعرض على الخريطة
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    /**
     * يتحقق من أن الكورس حضوري وليس أونلاين
     */
    public boolean isPhysicalLocation() {
        return locationType != null &&
                !"ONLINE".equalsIgnoreCase(locationType) &&
                !"أونلاين".equals(locationType);
    }

    /**
     * يعطي العنوان الكامل أو الموقع العادي
     */
    public String getDisplayAddress() {
        if (fullAddress != null && !fullAddress.isBlank()) {
            return fullAddress;
        }
        return getLocationDisplay();
    }

    /**
     * نص وصفي لنوع الموقع
     */
    public String getLocationTypeDisplay() {
        if (locationType == null) return "غير محدد";

        switch (locationType.toUpperCase()) {
            case "ONLINE": return "أونلاين";
            case "PHYSICAL": return "حضوري";
            case "HYBRID": return "مختلط";
            default: return locationType;
        }
    }

    /**
     * يعطي رمز emoji حسب نوع الموقع
     */
    public String getLocationIcon() {
        if (locationType == null) return "❓";

        switch (locationType.toUpperCase()) {
            case "ONLINE": return "💻";
            case "PHYSICAL": return "🏢";
            case "HYBRID": return "🔄";
            default: return "📍";
        }
    }

    /**
     * يحسب المسافة التقريبية من موقع معين (بالكيلومتر)
     */
    public double getDistanceFrom(double fromLat, double fromLng) {
        if (!hasCoordinates()) return Double.MAX_VALUE;

        double R = 6371; // نصف قطر الأرض بالكيلومتر
        double latDistance = Math.toRadians(fromLat - latitude);
        double lonDistance = Math.toRadians(fromLng - longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(fromLat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * نص وصفي للمسافة
     */
    public String getDistanceDisplay(double fromLat, double fromLng) {
        if (!hasCoordinates()) return "غير محدد";

        double distance = getDistanceFrom(fromLat, fromLng);
        if (distance < 1) {
            return String.format("%.0f متر", distance * 1000);
        } else {
            return String.format("%.1f كم", distance);
        }
    }

    /**
     * يتحقق من أن الكورس قريب من موقع معين (ضمن نطاق محدد)
     */
    public boolean isNearby(double fromLat, double fromLng, double maxDistanceKm) {
        if (!hasCoordinates()) return false;
        return getDistanceFrom(fromLat, fromLng) <= maxDistanceKm;
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