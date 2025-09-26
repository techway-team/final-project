package com.techway.coursemanagementdesktop;

public class CourseStatus {
    private boolean enrolled;
    private boolean hasPaid;

    public CourseStatus(boolean enrolled, boolean hasPaid) {
        this.enrolled = enrolled;
        this.hasPaid = hasPaid;
    }

    public boolean isEnrolled() {
        return enrolled;
    }

    public boolean hasPaid() {
        return hasPaid;
    }
}
