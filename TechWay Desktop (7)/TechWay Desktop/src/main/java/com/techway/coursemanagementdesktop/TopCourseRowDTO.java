package com.techway.coursemanagementdesktop;
import java.math.BigDecimal;

public class TopCourseRowDTO {
    private Long courseId;
    private String title;
    private long students;
    private BigDecimal revenue;
    private String status;

    public TopCourseRowDTO(Long courseId, String title, long students, BigDecimal revenue, String status) {
        this.courseId = courseId;
        this.title = title;
        this.students = students;
        this.revenue = revenue;
        this.status = status;
    }

    // Getters & Setters
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getStudents() { return students; }
    public void setStudents(long students) { this.students = students; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}