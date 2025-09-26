package com.techway.coursemanagementdesktop.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public final class ViewRouter {
    private ViewRouter() {}
    public static Parent load(String path) throws java.io.IOException {
        FXMLLoader fxml = new FXMLLoader(ViewRouter.class.getResource(path));
        return fxml.load();
    }
}
