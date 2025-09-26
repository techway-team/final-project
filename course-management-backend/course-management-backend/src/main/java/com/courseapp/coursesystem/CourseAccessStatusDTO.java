package com.courseapp.coursesystem;


public class CourseAccessStatusDTO {
    private boolean enrolled;
    private boolean hasPaid;

    public CourseAccessStatusDTO(boolean enrolled, boolean hasPaid) {
        this.enrolled = enrolled;
        this.hasPaid = hasPaid;
    }

    public boolean isEnrolled() {
        return enrolled;
    }

    public boolean isHasPaid() {
        return hasPaid;
    }

    public void setEnrolled(boolean enrolled) {
        this.enrolled = enrolled;
    }

    public void setHasPaid(boolean hasPaid) {
        this.hasPaid = hasPaid;
    }
}