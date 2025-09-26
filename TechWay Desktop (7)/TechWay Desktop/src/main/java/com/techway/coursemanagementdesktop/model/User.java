package com.techway.coursemanagementdesktop.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * User Model - matches backend User entity
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private Long id;
    private String name;
    private String email;
    private String role;

    // للحالة الإدارية (ACTIVE / DISABLED / ...). مطلوبة لجدول الأدمن.
    private String status;

    @JsonProperty("createdAt")
    @JsonAlias({"created_at"})
    private LocalDateTime createdAt;

    public User() {}

    // Constructor ملائم للتسجيل/التجارب
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.role = "USER";
    }

    // ===== Getters / Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ===== Utilities =====
    public boolean isAdmin() {
        return role != null && "ADMIN".equalsIgnoreCase(role);
    }

    public String getDisplayName() {
        return (name != null && !name.isBlank()) ? name : email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(status);
    }

}
