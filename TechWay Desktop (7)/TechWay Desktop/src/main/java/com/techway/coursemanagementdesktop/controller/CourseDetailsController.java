package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;

import java.util.Map;

/**
 * Controller for Course Details Dialog
 */
public class CourseDetailsController {

    @FXML private ImageView courseImage;
    @FXML private Label courseTitle;
    @FXML private Label courseInstructor;
    @FXML private Label coursePrice;
    @FXML private Label courseLocation;
    @FXML private Label courseDuration;
    @FXML private TextArea courseDescription;
    @FXML private Button enrollButton;
    @FXML private Button addToFavoritesButton;
    @FXML private VBox courseInfoContainer;
    @FXML private ScrollPane scrollPane;

    @FXML
    private ListView<Map<String, Object>> lessonList;
    @FXML
    private Pane mediaPane;
    private MediaView mediaView;

    private Course course;
    private SessionManager sessionManager;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        setupUI();
        setupBindings();
        initializeLessonList(); // ⬅️ هنا

    }

    private void setupUI() {
        // Style buttons
        enrollButton.getStyleClass().add("primary-button");
        addToFavoritesButton.getStyleClass().add("secondary-button");

        // Set button text
        enrollButton.setText("سجل في الكورس");
        addToFavoritesButton.setText("إضافة للمفضلة");

        // Make description read-only
        courseDescription.setEditable(false);
        courseDescription.setWrapText(true);

        // Configure scroll pane
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    private void setupBindings() {
        // Bind buttons to login state
        sessionManager.loggedInProperty().addListener((obs, wasLoggedIn, isLoggedIn) -> {
            updateButtonsState();
        });

        // Button actions
        enrollButton.setOnAction(e -> enrollInCourse());
        addToFavoritesButton.setOnAction(e -> addToFavorites());
    }

    public void setCourse(Course course) {
        this.course = course;
        displayCourseDetails();
        updateButtonsState();
    }

    private void displayCourseDetails() {
        if (course == null) return;

        // Set course image
        loadCourseImage();

        // Set basic information
        courseTitle.setText(course.getTitle());
        courseTitle.getStyleClass().add("course-title");

        courseInstructor.setText("المدرب: " + course.getInstructor());
        courseInstructor.getStyleClass().add("course-instructor");

        coursePrice.setText(course.getPriceDisplay());
        coursePrice.getStyleClass().add(course.getIsFree() ? "course-free" : "course-price");

        courseLocation.setText("الموقع: " + course.getLocationDisplay());
        courseLocation.getStyleClass().add("course-instructor");

        courseDuration.setText("المدة: " + course.getDuration() + " ساعة");
        courseDuration.getStyleClass().add("course-instructor");

        // Set description
        courseDescription.setText(course.getDescription() != null ? course.getDescription() : "لا يوجد وصف متاح");

        // Add additional course information
        addCourseFeatures();
    }

    private void loadCourseImage() {
        if (course.getImageUrl() != null && !course.getImageUrl().trim().isEmpty()) {
            try {
                Image image = new Image(course.getImageUrl(), true);
                courseImage.setImage(image);
            } catch (Exception e) {
                setDefaultCourseImage();
            }
        } else {
            setDefaultCourseImage();
        }

        // Set image properties
        courseImage.setFitWidth(400);
        courseImage.setFitHeight(250);
        courseImage.setPreserveRatio(false);
    }

    private void setDefaultCourseImage() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-course-large.png"));
            courseImage.setImage(defaultImage);
        } catch (Exception e) {
            // Image not found, keep empty
            System.out.println("Default course image not found");
        }
    }

    private void addCourseFeatures() {
        // Add a features section
        Label featuresTitle = new Label("ما ستتعلمه:");
        featuresTitle.getStyleClass().add("course-title");
        featuresTitle.setPadding(new Insets(20, 0, 10, 0));

        VBox featuresBox = new VBox(5);
        featuresBox.getChildren().addAll(
                createFeatureItem("✓ مهارات عملية ومطلوبة في سوق العمل"),
                createFeatureItem("✓ مشاريع تطبيقية لبناء ملف أعمال قوي"),
                createFeatureItem("✓ دعم فني ومتابعة مستمرة من المدرب"),
                createFeatureItem("✓ شهادة معتمدة عند إكمال الكورس"),
                createFeatureItem("✓ الوصول الدائم للمحتوى والتحديثات")
        );

        courseInfoContainer.getChildren().addAll(featuresTitle, featuresBox);

        // Add requirements section
        Label requirementsTitle = new Label("المتطلبات:");
        requirementsTitle.getStyleClass().add("course-title");
        requirementsTitle.setPadding(new Insets(20, 0, 10, 0));

        VBox requirementsBox = new VBox(5);
        requirementsBox.getChildren().addAll(
                createFeatureItem("• جهاز كمبيوتر أو لابتوب"),
                createFeatureItem("• اتصال إنترنت مستقر"),
                createFeatureItem("• الرغبة في التعلم والتطوير"),
                createFeatureItem("• خصص وقت كافي للممارسة والتطبيق")
        );

        courseInfoContainer.getChildren().addAll(requirementsTitle, requirementsBox);
    }

    private Label createFeatureItem(String text) {
        Label item = new Label(text);
        item.getStyleClass().add("course-description");
        item.setPadding(new Insets(2, 0, 2, 20));
        item.setWrapText(true);
        return item;
    }

    private void updateButtonsState() {
        boolean isLoggedIn = sessionManager.isLoggedIn();

        if (!isLoggedIn) {
            enrollButton.setText("سجل دخول للتسجيل");
            addToFavoritesButton.setText("سجل دخول لإضافة للمفضلة");
        } else {
            enrollButton.setText("سجل في الكورس");
            addToFavoritesButton.setText("إضافة للمفضلة");
        }
    }

    private void enrollInCourse() {
        if (!sessionManager.isLoggedIn()) {
            showLoginRequiredAlert();
            return;
        }

        Long userId = sessionManager.getCurrentUserId();
        if (userId == null || course == null) {
            return;
        }

        // استدعاء ApiService للتسجيل
        ApiService apiService = ApiService.getInstance();
        apiService.enrollUser(userId, course.getId())
                .thenAccept(enrollment -> {
                    Platform.runLater(() -> {
                        if (enrollment != null) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("التسجيل في الكورس");
                            alert.setHeaderText("تم التسجيل بنجاح ✅");
                            alert.setContentText("تم تسجيلك في كورس \""
                                    + enrollment.getCourse().getTitle() + "\" بنجاح.");
                            alert.getDialogPane().getStylesheets()
                                    .add(getClass().getResource("/css/main-styles.css").toExternalForm());

                            alert.showAndWait();
                            enrollButton.setText("مسجل ✓");
                            enrollButton.setDisable(true);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("خطأ في الاتصال");
                        alert.setHeaderText("فشل التسجيل");
                        alert.setContentText("حدث خطأ: " +
                                (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                        alert.getDialogPane().getStylesheets()
                                .add(getClass().getResource("/css/main-styles.css").toExternalForm());
                        alert.showAndWait();
                    });
                    return null;
                });
    }

        private void addToFavorites() {
        if (!sessionManager.isLoggedIn()) {
            showLoginRequiredAlert();
            return;
        }

        // TODO: Implement add to favorites logic
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("إضافة للمفضلة");
        alert.setHeaderText("تمت الإضافة بنجاح!");
        alert.setContentText("تم إضافة كورس \"" + course.getTitle() + "\" لقائمة المفضلة.\n\n" +
                "يمكنك الوصول لكورساتك المفضلة من الملف الشخصي.");

        // Apply styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/main-styles.css").toExternalForm());

        alert.showAndWait();

        // Update button text
        addToFavoritesButton.setText("تمت الإضافة ✓");
        addToFavoritesButton.setDisable(true);
    }

    private void showLoginRequiredAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("تسجيل الدخول مطلوب");
        alert.setHeaderText("يجب تسجيل الدخول أولاً");
        alert.setContentText("لاستخدام هذه الميزة، يجب عليك تسجيل الدخول أو إنشاء حساب جديد.");

        // Apply styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/main-styles.css").toExternalForm());

        alert.showAndWait();
    }

    private void initializeLessonList() {
        lessonList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String videoUrl = (String) newV.get("videoUrl");
                playVideo(videoUrl);
            }
        });

        // عرض العناوين فقط
        lessonList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (String) item.get("title"));
            }
        });

        // تحميل الدروس للكورس الحالي
        if (course != null) {
            ApiService.getInstance().getLessonsForCourse(course.getId()).thenAccept(lessons -> {
                Platform.runLater(() -> {
                    lessonList.getItems().setAll(lessons);
                });
            });
        }
    }

    private void playVideo(String url) {
        if (url == null || url.isBlank()) {
            showError("رابط الفيديو غير متوفر.");
            return;
        }

        mediaPane.getChildren().clear();

        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            // عرض YouTube عبر WebView
            javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
            webView.setPrefSize(500, 300);

            String videoId = extractYouTubeVideoId(url);
            if (videoId != null) {
                String embedUrl = "https://www.youtube.com/embed/" + videoId + "?autoplay=1";
                webView.getEngine().load(embedUrl);
                mediaPane.getChildren().add(webView);
            } else {
                showError("رابط YouTube غير صالح.");
            }

        } else {
            try {
                javafx.scene.media.Media media = new javafx.scene.media.Media(url);
                javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
                mediaView = new javafx.scene.media.MediaView(player);
                mediaView.setFitWidth(500);
                mediaView.setFitHeight(300);
                mediaView.setPreserveRatio(true);

                mediaPane.getChildren().add(mediaView);
                player.play();

            } catch (Exception e) {
                e.printStackTrace();
                showError("فشل تشغيل الفيديو: " + e.getMessage());
            }
        }
    }


    private String extractYouTubeVideoId(String url) {
        try {
            if (url.contains("youtube.com")) {
                java.net.URI uri = new java.net.URI(url);
                String query = uri.getQuery();
                for (String param : query.split("&")) {
                    if (param.startsWith("v=")) {
                        return param.substring(2);
                    }
                }
            } else if (url.contains("youtu.be")) {
                return url.substring(url.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

}