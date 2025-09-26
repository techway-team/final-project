package com.techway.coursemanagementdesktop.dto;

public class LessonAccessResult {
    private boolean accessGranted;
    private String message;

    public LessonAccessResult(boolean accessGranted, String message) {
        this.accessGranted = accessGranted;
        this.message = message;
    }

    public boolean isAccessGranted() {
        return accessGranted;
    }

    public String getMessage() {
        return message;
    }
}

