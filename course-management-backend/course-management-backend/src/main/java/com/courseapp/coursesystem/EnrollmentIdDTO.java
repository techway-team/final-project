package com.courseapp.coursesystem;

public class EnrollmentIdDTO {
    private Long enrollmentId;

    public EnrollmentIdDTO(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }
}
