package com.techway.coursemanagementdesktop.model;

public class AdminOverview {
    private long totalUsers, totalCourses, freeCourses, paidCourses;

    public AdminOverview(long totalUsers, long totalCourses, long freeCourses, long paidCourses) {
        this.totalUsers = totalUsers;
        this.totalCourses = totalCourses;
        this.freeCourses = freeCourses;
        this.paidCourses = paidCourses;
    }

    public long getTotalUsers() { return totalUsers; }
    public long getTotalCourses() { return totalCourses; }
    public long getFreeCourses() { return freeCourses; }
    public long getPaidCourses() { return paidCourses; }
}