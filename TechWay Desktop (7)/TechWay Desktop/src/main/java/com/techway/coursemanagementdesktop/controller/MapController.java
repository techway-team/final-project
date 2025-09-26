package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MapController {

    private final MainController mainController;
    private final ApiService apiService;
    private final SessionManager sessionManager;
    private WebView webView;
    private WebEngine webEngine;
    private boolean mapReady = false;
    private Long focusCourseId = null;

    public MapController(MainController mainController, ApiService apiService, SessionManager sessionManager) {
        this.mainController = mainController;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
    }

    public VBox createMapPage() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_CENTER);

        // Header
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);

        Label title = new Label("خريطة الكورسات");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label("اعثر على الكورسات القريبة منك");
        subtitle.getStyleClass().add("course-instructor");

        header.getChildren().addAll(title, subtitle);

        // Controls
        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(16));

        Button backBtn = new Button("العودة للكورسات");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> mainController.navigateToCourses());

        Button refreshBtn = new Button("تحديث الخريطة");
        refreshBtn.getStyleClass().add("primary-button");
        refreshBtn.setOnAction(e -> loadCoursesOnMap());

        Button myLocationBtn = new Button("موقعي الحالي");
        myLocationBtn.getStyleClass().add("secondary-button");
        myLocationBtn.setOnAction(e -> getCurrentLocation());

        Button openInBrowserBtn = new Button("فتح في المتصفح");
        openInBrowserBtn.getStyleClass().add("secondary-button");
        openInBrowserBtn.setOnAction(e -> openMapInBrowser());

        controls.getChildren().addAll(backBtn, refreshBtn, myLocationBtn, openInBrowserBtn);

        // Map WebView
        webView = new WebView();
        webView.setPrefHeight(600);
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Enable console logging
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapReady = true;
                // Add bridge for JavaScript to Java communication
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaApp", new JavaScriptBridge());

                // Load courses after map is ready
                Platform.runLater(this::loadCoursesOnMap);
            }
        });

        // Load map HTML inside WebView
        try {
            webEngine.loadContent(loadMapHTMLAsString());
        } catch (IOException e) {
            mainController.showError("فشل في تحميل ملف الخريطة: " + e.getMessage());
        }

        root.getChildren().addAll(header, controls, webView);
        return root;
    }

    private String loadMapHTMLAsString() throws IOException {
        InputStream is = getClass().getResourceAsStream("/static/courses-map.html");
        if (is == null) throw new FileNotFoundException("ملف الخريطة غير موجود");
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }


    private void loadCoursesOnMap() {
        System.out.println("DEBUG: loadCoursesOnMap called, mapReady=" + mapReady);

        if (!mapReady) {
            System.out.println("DEBUG: map not ready yet, returning.");
            return;
        }

        apiService.getAllCourses().thenAccept(courses -> {
            Platform.runLater(() -> {
                if (courses == null || courses.isEmpty()) {
                    System.out.println("DEBUG: No courses received from API");
                } else {
                    System.out.println("DEBUG: Received " + courses.size() + " courses from API");
                }

                // Clear existing markers
                webEngine.executeScript("clearMarkers();");

                for (Course course : courses) {
                    System.out.println("DEBUG: checking course id=" + course.getId() +
                            " title=" + course.getTitle() +
                            " lat=" + course.getLatitude() +
                            " lng=" + course.getLongitude());

                    if (course.hasCoordinates()) {
                        System.out.println("DEBUG: course has valid coordinates, adding to map");
                        addCourseToMap(course);
                    } else {
                        System.out.println("DEBUG: skipped course " + course.getId() + " (no valid coordinates)");
                    }
                }

                // Focus on specific course if requested
                if (focusCourseId != null) {
                    System.out.println("DEBUG: focusing on course id=" + focusCourseId);
                    webEngine.executeScript("focusOnCourse(" + focusCourseId + ");");
                    focusCourseId = null;
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                System.out.println("DEBUG: exception occurred while fetching courses: " + ex.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "فشل في تحميل الكورسات: " + ex.getMessage());
                alert.showAndWait();
            });
            return null;
        });
    }


    private void addCourseToMap(Course course) {
        String script = String.format(
                "addCourseMarker({" +
                        "id: %d, " +
                        "title: '%s', " +
                        "instructor: '%s', " +
                        "location: '%s', " +
                        "fullAddress: '%s', " +
                        "locationType: '%s', " +
                        "locationTypeDisplay: '%s', " +
                        "price: %s, " +
                        "isFree: %s, " +
                        "latitude: %f, " +
                        "longitude: %f" +
                        "});",
                course.getId(),
                escapeSingleQuotes(course.getTitle()),
                escapeSingleQuotes(course.getInstructor()),
                escapeSingleQuotes(course.getLocation()),
                escapeSingleQuotes(course.getFullAddress()),
                escapeSingleQuotes(course.getLocationType()),
                escapeSingleQuotes(course.getLocationTypeDisplay()),
                course.getPrice(),
                course.getIsFree(),
                course.getLatitude(),
                course.getLongitude()
        );

        // 👇 هنا اطبع عشان تشيك بنفسك
        System.out.println("Generated JS: " + script);

        webEngine.executeScript(script);
    }


    // استدعاء JS للحصول على الموقع
    private void getCurrentLocation() {
        if (mapReady) {
            webEngine.executeScript("getCurrentLocation();");
        }
    }

    // استدعاء من JS لإرسال الإحداثيات إلى JavaFX
    public void onLocationFound(double lat, double lng) {
        Platform.runLater(() -> {
            System.out.println("Latitude: " + lat + ", Longitude: " + lng);
            // يمكن هنا تحديث Marker أو أي عنصر على الخريطة
        });
    }

    public void focusOnCourse(Long courseId) {
        if (mapReady && courseId != null) {
            webEngine.executeScript("focusOnCourse(" + courseId + ");");
        } else {
            focusCourseId = courseId;
        }
    }

    private String escapeSingleQuotes(String text) {
        if (text == null) return "";
        return text.replace("'", "\\'").replace("\"", "\\\"");
    }

    // فتح الخريطة في المتصفح
    // فتح الخريطة في المتصفح مع الكورسات
    private void openMapInBrowser() {
        apiService.getAllCourses().thenAccept(courses -> {
            Platform.runLater(() -> {
                try {
                    // جهز سكربت لإضافة الكورسات
                    StringBuilder markersScript = new StringBuilder();
                    for (Course course : courses) {
                        if (course.hasCoordinates()) {
                            String script = String.format(
                                    "addCourseMarker({" +
                                            "id: %d, " +
                                            "title: '%s', " +
                                            "instructor: '%s', " +
                                            "location: '%s', " +
                                            "fullAddress: '%s', " +
                                            "locationType: '%s', " +
                                            "locationTypeDisplay: '%s', " +
                                            "price: %s, " +
                                            "isFree: %s, " +
                                            "latitude: %f, " +
                                            "longitude: %f" +
                                            "});\n",
                                    course.getId(),
                                    escapeSingleQuotes(course.getTitle()),
                                    escapeSingleQuotes(course.getInstructor()),
                                    escapeSingleQuotes(course.getLocation()),
                                    escapeSingleQuotes(course.getFullAddress()),
                                    escapeSingleQuotes(course.getLocationType()),
                                    escapeSingleQuotes(course.getLocationTypeDisplay()),
                                    course.getPrice(),
                                    course.getIsFree(),
                                    course.getLatitude(),
                                    course.getLongitude()
                            );
                            markersScript.append(script);
                        }
                    }

                    // ادمج السكربت مع HTML
                    String htmlContent = loadMapHTMLAsString().replace("</script>", markersScript.toString() + "\n</script>");

                    // خزنه في ملف مؤقت
                    File tempFile = File.createTempFile("map", ".html");
                    try (FileWriter writer = new FileWriter(tempFile)) {
                        writer.write(htmlContent);
                    }

                    // افتحه بالمتصفح
                    Desktop.getDesktop().browse(tempFile.toURI());

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "فشل في فتح الخريطة في المتصفح: " + ex.getMessage());
                    alert.showAndWait();
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "فشل في تحميل الكورسات: " + ex.getMessage());
                alert.showAndWait();
            });
            return null;
        });
    }




    // Bridge class
    public class JavaScriptBridge {
        public void openCourseDetails(int courseId) {
            Platform.runLater(() -> {
                apiService.getCourseById((long) courseId).thenAccept(course -> {
                    Platform.runLater(() -> {
                        mainController.loadCourseDetailsPage(course);
                    });
                }).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR,
                                "فشل في تحميل تفاصيل الكورس");
                        alert.showAndWait();
                    });
                    return null;
                });
            });
        }
    }


}
