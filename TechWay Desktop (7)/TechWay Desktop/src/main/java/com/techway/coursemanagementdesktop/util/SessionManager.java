package com.techway.coursemanagementdesktop.util;

import com.techway.coursemanagementdesktop.model.User;
import javafx.beans.property.*;

/**
 * Session Manager for handling user authentication state
 */
public class SessionManager {

    private static SessionManager instance;

    private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>();
    private final BooleanProperty loggedIn = new SimpleBooleanProperty(false);
    private final StringProperty role = new SimpleStringProperty();

    private String authToken;

    private SessionManager() {}

    public static void initialize() {
        if (instance == null) {
            instance = new SessionManager();
        }
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    /* ========================= AUTH FLOW ========================= */

    /** يضبط المستخدم والتوكن ويحدّث حالة الدخول. */
    public void login(User user, String token) {
        setUser(user);
        setToken(token);
        loggedIn.set(user != null);
        if (user != null) {
            System.out.println("User logged in: " + safe(user.getName()) + " (" + safe(user.getRole()) + ")");
        }
    }

    /** يصفّر الجلسة بالكامل. */
    public static void logout() {
        if (instance != null) {
            instance.setUser(null);
            instance.setToken(null);
            instance.loggedIn.set(false);
            System.out.println("User logged out");
        }
    }

    /* ========================= Getters / Setters ========================= */

    public User getCurrentUser() {
        return currentUser.get();
    }

    public void setUser(User user) {
        this.currentUser.set(user);
        // مزامنة الدور مع المستخدم
        if (user != null) {
            setRole(user.getRole());
            loggedIn.set(true);
        } else {
            setRole(null);
            loggedIn.set(false);
        }
    }

    public ObjectProperty<User> currentUserProperty() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return loggedIn.get();
    }

    public BooleanProperty loggedInProperty() {
        return loggedIn;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setToken(String token) {
        this.authToken = token;
    }

    /* ========================= Role helpers ========================= */

    public String getRole() {
        return role.get();
    }

    public void setRole(String newRole) {
        role.set(newRole);
    }

    public StringProperty roleProperty() {
        return role;
    }

    public boolean isAdmin() {
        String r = getRole();
        return r != null && "ADMIN".equalsIgnoreCase(r);
    }

    /* ========================= Convenience ========================= */

    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
