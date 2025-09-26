package com.techway.coursemanagementdesktop.dto;

import com.techway.coursemanagementdesktop.model.User;

/**
 * Login Response DTO
 */
public class LoginResponse {
    private User user;
    private String token;

    public LoginResponse() {}

    public LoginResponse(User user, String token) {
        this.user = user;
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}