package com.techway.coursemanagementdesktop.model;


public class LessonProgress {
    private Long id;
    private boolean completed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
