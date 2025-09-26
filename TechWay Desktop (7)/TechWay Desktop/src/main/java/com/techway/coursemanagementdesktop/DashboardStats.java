package com.techway.coursemanagementdesktop;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardStats {

    private int totalCourses;
    private int paidCourses;
    private int freeCourses;
    private int totalUsers;
    private BigDecimal averagePrice;


    private Map<String, Number> performanceData; // مثلاً: {"يناير": 10, "فبراير": 5}
    private List<TopCourse> topCourses; // أكثر الكورسات شهرة أو تسجيلًا

    // Getters & Setters

    public int getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(int totalCourses) {
        this.totalCourses = totalCourses;
    }

    public int getPaidCourses() {
        return paidCourses;
    }

    public void setPaidCourses(int paidCourses) {
        this.paidCourses = paidCourses;
    }

    public int getFreeCourses() {
        return freeCourses;
    }

    public void setFreeCourses(int freeCourses) {
        this.freeCourses = freeCourses;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Map<String, Number> getPerformanceData() {
        return performanceData;
    }

    public void setPerformanceData(Map<String, Number> performanceData) {
        this.performanceData = performanceData;
    }

    public List<TopCourse> getTopCourses() {
        return topCourses;
    }

    public void setTopCourses(List<TopCourse> topCourses) {
        this.topCourses = topCourses;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }
}
