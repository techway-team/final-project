package com.techway.coursemanagementdesktop;

import com.techway.coursemanagementdesktop.controller.MainController;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * TechWay Desktop Application
 * Course Management System - Desktop Version
 */
public class TechWayApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Initialize API Service
        ApiService.initialize("http://localhost:8080");

        // Initialize Session Manager
        SessionManager.initialize();

        Font.loadFont(
                getClass().getResourceAsStream("/fonts/Rubik-MediumItalic.ttf"),
                14 // الحجم الافتراضي للتحميل
        );

        // Load the main scene
        loadMainScene();

        // Configure the stage
        configurePrimaryStage();

        // Show the application
        primaryStage.show();
        Font.loadFont(getClass().getResourceAsStream("/fonts/NotoSansArabic-Medium.ttf"), 14);

    }

    private void loadMainScene() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                TechWayApplication.class.getResource("/fxml/main.fxml")
        );


        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);

        // Load CSS styles (آمن بدون رمي NPE لو ملف ناقص)
        safeAddStylesheet(scene, "/css/main-styles.css");
        safeAddStylesheet(scene, "/css/main-styles.v2.css");
        safeAddStylesheet(scene, "/css/header-compact.css"); // الهيدر المضغوط
        safeAddStylesheet(scene, "fxml/admin/css/admin-style.css");
        // << ستايل لوحة الأدمن


        primaryStage.setScene(scene);

        // لا تحتاج تنادي initialize يدويًا؛ FXMLLoader يستدعي @FXML initialize تلقائيًا
        // لو مصرّ تبقيه، احذف التعليق تحت (لكن غالبًا يسبب تكرار ربط/Listeners)
        // MainController controller = fxmlLoader.getController();
        // controller.initialize();
    }

    /** يضيف ستايل شيت إذا موجود؛ يطبع تحذير لو غير موجود بدل ما يطيح التطبيق */
    private void safeAddStylesheet(Scene scene, String path) {
        URL url = getClass().getResource(path);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            System.err.println("WARN stylesheet not found: " + path);
        }
    }

    private void configurePrimaryStage() {
        primaryStage.setTitle("TechWay");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);

        // Set application icon (آمن)
        try (InputStream is = getClass().getResourceAsStream("/icons/app-icon.png")) {
            if (is != null) {
                primaryStage.getIcons().add(new Image(is));
            } else {
                System.out.println("Could not load application icon: /icons/app-icon.png");
            }
        } catch (Exception e) {
            System.out.println("Could not load application icon: " + e.getMessage());
        }

        // Handle close request
        primaryStage.setOnCloseRequest(e -> {
            try {
                SessionManager.logout();
            } finally {
                Platform.exit();
                System.exit(0);
            }
        });
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}
