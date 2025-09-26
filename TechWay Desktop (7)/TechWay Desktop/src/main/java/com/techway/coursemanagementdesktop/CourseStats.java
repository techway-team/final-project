package com.techway.coursemanagementdesktop;


import java.math.BigDecimal;
import java.util.Map;

public class CourseStats {

    private int totalCourses;
    private int freeCourses;
    private int paidCourses;
    private BigDecimal averagePrice;
    private Map<String, Integer> locationDistribution;

    // Getters and Setters

    public int getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(int totalCourses) {
        this.totalCourses = totalCourses;
    }

    public int getFreeCourses() {
        return freeCourses;
    }

    public void setFreeCourses(int freeCourses) {
        this.freeCourses = freeCourses;
    }

    public int getPaidCourses() {
        return paidCourses;
    }

    public void setPaidCourses(int paidCourses) {
        this.paidCourses = paidCourses;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public Map<String, Integer> getLocationDistribution() {
        return locationDistribution;
    }

    public void setLocationDistribution(Map<String, Integer> locationDistribution) {
        this.locationDistribution = locationDistribution;
    }
}
