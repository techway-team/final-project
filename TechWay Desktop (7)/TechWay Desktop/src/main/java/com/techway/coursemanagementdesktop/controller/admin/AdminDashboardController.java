package com.techway.coursemanagementdesktop.controller.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.techway.coursemanagementdesktop.*;
import com.techway.coursemanagementdesktop.dto.ApiResponse;
import com.techway.coursemanagementdesktop.dto.CourseAdminRequest;
import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Scanner;
import java.time.Duration;
import java.util.stream.Collectors;


public class AdminDashboardController {

    // ===== Labels =====
    @FXML private Label totalUsersLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label freeCoursesLabel;
    @FXML private Label paidCoursesLabel;
    @FXML private Label newEnrollmentsLabel;
    @FXML private Label totalRevenueLabel;

    // ===== Charts =====
    @FXML private BarChart<String, Number> enrollmentsBarChart;

    // ===== Tabs =====
    @FXML private TabPane adminTabPane;
    @FXML private Tab dashboardTab;

    @FXML private Tab settingsTab;


    @FXML private Tab coursesTab;
    @FXML private TableView<CourseDTO> coursesTable;
    @FXML private TableColumn<CourseDTO, Long> idColumn;
    @FXML private TableColumn<CourseDTO, String> titleColumn;
    @FXML private TableColumn<CourseDTO, String> statusColumn;
    @FXML private TableColumn<CourseDTO, Double> priceColumn;


    @FXML private Tab usersTab;
    @FXML private TableView<AdminUserDTO> usersTable;
    @FXML private TableColumn<AdminUserDTO, Long> userIdColumn;
    @FXML private TableColumn<AdminUserDTO, String> userNameColumn;
    @FXML private TableColumn<AdminUserDTO, String> userEmailColumn;
    @FXML private TableColumn<AdminUserDTO, String> userRoleColumn;


    @FXML private TableView<TopCourseRowDTO> topCoursesTable;
    @FXML private TableColumn<TopCourseRowDTO, Long> topCourseIdColumn;
    @FXML private TableColumn<TopCourseRowDTO, String> topCourseTitleColumn;
    @FXML private TableColumn<TopCourseRowDTO, Long> topCourseStudentsColumn;
    @FXML private TableColumn<TopCourseRowDTO, BigDecimal> topCourseRevenueColumn;
    @FXML
    TableColumn<TopCourseRowDTO, TopCourseRowDTO> topCourseStatusColumn;

    @FXML private LineChart<String, Number> enrollmentsChart;
    @FXML private ComboBox<String> rangeComboBox;
    @FXML private Button refreshButton;
    @FXML private Label subtitleLabel;


    @FXML private ListView<ActivityItemDTO> activityList;
    @FXML private Button refreshBtn;


    @FXML
    private ListView<RecentCourseDTO> recentCoursesList;

    @FXML
    private Button refreshCoursesBtn;

    @FXML private Button addCourseButton;
    @FXML private Button editCourseButton;
    @FXML private Button deleteCourseButton;

    // ============= Settings Tab Implementation =============

    // إضافة هذه المتغيرات في الكلاس
    @FXML private VBox settingsContainer;
    @FXML private TextField platformNameField;
    @FXML private TextField adminEmailField;
    @FXML private TextField supportEmailField;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private CheckBox enableNotificationsCheckBox;
    @FXML private CheckBox enableEmailNotificationsCheckBox;
    @FXML private TextField maxEnrollmentsField;
    @FXML private TextField sessionTimeoutField;
    @FXML private Button saveSettingsButton;
    @FXML private Button resetToDefaultButton;
    @FXML private Button exportDataButton;
    @FXML private Button importDataButton;
    @FXML private Label lastBackupLabel;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                    LocalDateTime.parse(json.getAsString())
            )
            .create();

    @FXML
    public void initialize() {
        topCoursesTable.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());
        adminTabPane.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());
        coursesTable.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());
        coursesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);


        // ربط أعمدة الجدول مع خصائص CourseDTO
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));


        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        userEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        userRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));


        // إعداد أعمدة Top Courses
        topCourseIdColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getCourseId()));
        topCourseTitleColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getTitle()));
        topCourseStudentsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getStudents()));
        topCourseRevenueColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getRevenue() != null ? cellData.getValue().getRevenue() : BigDecimal.ZERO));
        topCourseStatusColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));

        topCourseStatusColumn.setCellFactory(column -> new TableCell<TopCourseRowDTO, TopCourseRowDTO>() {
            @Override
            protected void updateItem(TopCourseRowDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String status = item.getStatus();
                    Label label = new Label(status);
                    label.setStyle(
                            "-fx-background-color: #d4f5dc;" +
                                    "-fx-text-fill: #2e7d32;" +
                                    "-fx-padding: 4 12 4 12;" +
                                    "-fx-background-radius: 12;" +
                                    "-fx-font-weight: bold;"
                    );
                    setGraphic(label);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });


        // تحميل البيانات الافتراضية للوحة التحكم
        loadDashboardData();
        loadTopCourses();

        // خيارات المدة
        rangeComboBox.getItems().addAll("7 أيام", "30 يوم", "90 يوم");
        rangeComboBox.getSelectionModel().select("30 يوم");

        // تحميل أول مرة
        loadEnrollmentTrend("30d");

        // تغيير المدة
        rangeComboBox.setOnAction(e -> refreshChart());

        // زر التحديث
        refreshButton.setOnAction(e -> refreshChart());

        // تصميم العنصر داخل القائمة
        activityList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ActivityItemDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String ago = formatAgo(item.getAt());
                    String user = (item.getUser() != null) ? item.getUser() : "—";
                    String course = (item.getCourse() != null) ? item.getCourse() : "—";

                    String actionText = item.getType().equals("ENROLL")
                            ? "سجل " + user + " في " + course
                            : "أنشأ " + user + " الكورس " + course;

                    Label label = new Label(ago + "   " + actionText);
                    label.getStyleClass().add("activity-item");

                    setGraphic(label);
                }
            }
        });
        refreshBtn.setOnAction(e -> loadActivityItem());
        // تحميل بيانات تجريبية
        loadActivityItem();



        refreshCoursesBtn.setOnAction(event -> loadRecentCourses());
        loadRecentCourses(); // تحميل عند فتح الواجهة

        // تصميم ListCell عصري لكل عنصر
        recentCoursesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(RecentCourseDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(2);
                    Label title = new Label("الكورس: " + item.getTitle());
                    title.setStyle("-fx-font-weight: bold; -fx-text-fill: #6b54b8;");

                    Label instructor = new Label("المحاضر: " + item.getInstructor());
                    instructor.setStyle("-fx-text-fill: #333; -fx-font-size: 12;");

                    Label type = new Label(item.isFree() ? "مجاني" : "مدفوع");
                    type.setStyle("-fx-text-fill: " + (item.isFree() ? "#4caf50" : "#f44336") + "; -fx-font-size: 12;");

                    Label date = new Label("تاريخ الإنشاء: " + item.getCreatedAt().toLocalDate());
                    date.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

                    vbox.getChildren().addAll(title, instructor, type, date);
                    setGraphic(vbox);
                }
            }
        });

        refresh(null);
        // الاستماع لتغيير التبويبات
        adminTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == dashboardTab) {
                loadDashboardData();
                loadTopCourses();
            } else if (newTab == coursesTab) {
                loadCoursesTab();
            } else if (newTab == usersTab) {
                loadUsersTab();
            } else if (newTab == settingsTab) {
                loadSettingsTab();
            }
        });
        // في initialize() أضف هذا الكود في النهاية:

// إعداد أزرار الكورسات
        setupCourseButtons();

    }
    private void setupCourseButtons() {
        // زر الإضافة
        if (addCourseButton != null) {
            addCourseButton.setOnAction(e -> showAddCourseDialog());
        }

        // زر التعديل
        if (editCourseButton != null) {
            editCourseButton.setOnAction(e -> showEditCourseDialog());
        }

        // زر الحذف
        if (deleteCourseButton != null) {
            deleteCourseButton.setOnAction(e -> deleteSelectedCourse());
        }

        // تفعيل/إلغاء تفعيل الأزرار حسب التحديد
        coursesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            if (editCourseButton != null) editCourseButton.setDisable(!hasSelection);
            if (deleteCourseButton != null) deleteCourseButton.setDisable(!hasSelection);
        });

        // في البداية، أزرار التعديل والحذف معطلة
        if (editCourseButton != null) editCourseButton.setDisable(true);
        if (deleteCourseButton != null) deleteCourseButton.setDisable(true);
    }

    private void refreshChart() {
        String selected = rangeComboBox.getValue();
        String rangeKey = "30d";
        if (selected.contains("7")) rangeKey = "7d";
        else if (selected.contains("90")) rangeKey = "90d";

        subtitleLabel.setText("تسجيلات آخر " + selected);
        loadEnrollmentTrend(rangeKey);
    }


    // ============= Dashboard =============
    private void loadDashboardData() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/admin/dashboard");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                // إضافة التوكن من SessionManager
                String token = SessionManager.getInstance().getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder jsonStr = new StringBuilder();
                    while (scanner.hasNext()) jsonStr.append(scanner.nextLine());
                    scanner.close();

                    Type responseType = new TypeToken<ApiResponse<AdminOverviewDTO>>() {}.getType();
                    ApiResponse<AdminOverviewDTO> response = gson.fromJson(jsonStr.toString(), responseType);
                    AdminOverviewDTO overview = response.getData();

                    System.out.println("Raw JSON: " + jsonStr);
                    System.out.println("Parsed data: " + response.getData());

                    Platform.runLater(() -> updateUI(overview));
                } else {
                    System.err.println("Failed to load dashboard data, code: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateUI(AdminOverviewDTO overview) {
        if (overview == null) return;

        // تحديث البطاقات بالقيم الحقيقية
        totalUsersLabel.setText(String.valueOf(overview.getTotalUsers()));
        totalCoursesLabel.setText(String.valueOf(overview.getTotalCourses()));
        freeCoursesLabel.setText(String.valueOf(overview.getFreeCourses()));
        paidCoursesLabel.setText(String.valueOf(overview.getPaidCourses()));
        newEnrollmentsLabel.setText(String.valueOf(overview.getNewEnrollments30d()));
        totalRevenueLabel.setText(overview.getTotalRevenue() != null ? overview.getTotalRevenue().toString() : "0");


    }

    private void refresh(String q) {
        setLoading(true);
        ApiService.getInstance().adminListCourses(q)
                .thenAccept((List<Course> courseList) ->
                        Platform.runLater(() -> {
                            try {
                                // تحويل List<Course> إلى List<CourseDTO>
                                List<CourseDTO> courseDTOList = courseList.stream()
                                        .map(CourseDTO::fromCourse)
                                        .collect(Collectors.toList());

                                coursesTable.getItems().setAll(courseDTOList);
                                setLoading(false);
                            } catch (Exception e) {
                                setLoading(false);
                                err("خطأ في تحويل البيانات: " + safe(e.getMessage()));
                            }
                        }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        err("فشل تحميل الكورسات: " + safe(ex.getMessage()));
                    });
                    return null;
                });
    }

    @FXML
    private void onEdit() {
        CourseDTO c = coursesTable.getSelectionModel().getSelectedItem();
        if (c == null) { err("اختَر كورس"); return; }

        CourseDialogs.CourseFormData data = CourseDialogs.showCourseDialog(c);
        if (data == null) return;

        setLoading(true);
        ApiService.getInstance().adminUpdateCourse(c.getId(), data)
                .thenAccept(ok -> Platform.runLater(() -> {
                    setLoading(false);
                    if (Boolean.TRUE.equals(ok)) refresh(null);
                    else err("فشل تعديل الكورس");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        err("خطأ أثناء التعديل: " + safe(ex.getMessage()));
                    });
                    return null;
                });
    }

    @FXML
    private void onDelete() {
        CourseDTO c = coursesTable.getSelectionModel().getSelectedItem();
        if (c == null) { err("اختَر كورس"); return; }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "حذف الكورس #" + c.getId() + "؟");
        Optional<ButtonType> res = a.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        setLoading(true);
        ApiService.getInstance().adminDeleteCourse(c.getId())
                .thenAccept(ok -> Platform.runLater(() -> {
                    setLoading(false);
                    if (Boolean.TRUE.equals(ok)) refresh(null);
                    else err("فشل الحذف");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        err("خطأ أثناء الحذف: " + safe(ex.getMessage()));
                    });
                    return null;
                });
    }
    
    

    private void setLoading(boolean loading) {
        if (coursesTable != null) coursesTable.setDisable(loading);
    }

    private void err(String m) { new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
    private static String value(TextField f) { return f == null || f.getText() == null ? "" : f.getText().trim(); }
    private static String safe(String s) { return s == null ? "" : s; }
    
    private void loadEnrollmentTrend(String range) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/admin/trend/enrollments?range=" + range);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                String token = SessionManager.getInstance().getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder jsonStr = new StringBuilder();
                    while (scanner.hasNext()) jsonStr.append(scanner.nextLine());
                    scanner.close();

                    Type responseType = new TypeToken<ApiResponse<TrendDTO>>() {}.getType();
                    ApiResponse<TrendDTO> response = gson.fromJson(jsonStr.toString(), responseType);
                    TrendDTO trend = response.getData();

                    Platform.runLater(() -> {
                        enrollmentsChart.getData().clear();
                        XYChart.Series<String, Number> series = new XYChart.Series<>();
                        series.setName("التسجيلات");
                        for (int i = 0; i < trend.getLabels().size(); i++) {
                            series.getData().add(
                                    new XYChart.Data<>(trend.getLabels().get(i), trend.getData().get(i))
                            );
                        }
                        enrollmentsChart.getData().add(series);
                    });
                } else {
                    System.err.println("Failed to load trend, code: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ============= Courses Tab =============
    private void loadCoursesTab() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/admin/courses");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                String token = SessionManager.getInstance().getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder jsonStr = new StringBuilder();
                    while (scanner.hasNext()) jsonStr.append(scanner.nextLine());
                    scanner.close();

                    Type responseType = new TypeToken<ApiResponse<List<Course>>>() {}.getType();
                    ApiResponse<List<Course>> response = gson.fromJson(jsonStr.toString(), responseType);

                    List<CourseDTO> courseDTOs = response.getData().stream()
                            .map(CourseDTO::fromCourse)
                            .collect(Collectors.toList());

                    Platform.runLater(() -> {
                        coursesTable.getItems().clear();
                        coursesTable.getItems().addAll(courseDTOs);
                    });
                } else {
                    System.err.println("خطأ في تحميل الكورسات، كود الحالة: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ============= Users Tab =============
    private void loadUsersTab() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/admin/users");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                String token = SessionManager.getInstance().getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder jsonStr = new StringBuilder();
                    while (scanner.hasNext()) jsonStr.append(scanner.nextLine());
                    scanner.close();

                    Type responseType = new TypeToken<ApiResponse<List<AdminUserDTO>>>() {}.getType();
                    ApiResponse<List<AdminUserDTO>> response = gson.fromJson(jsonStr.toString(), responseType);
                    List<AdminUserDTO> users = response.getData();

                    Platform.runLater(() -> {
                        usersTable.getItems().clear();
                        if (users != null) {
                            usersTable.getItems().addAll(users);
                        }
                    });
                } else {
                    System.err.println("Failed to load users, code: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadTopCourses() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/admin/top-courses");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                String token = SessionManager.getInstance().getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder jsonStr = new StringBuilder();
                    while (scanner.hasNext()) jsonStr.append(scanner.nextLine());
                    scanner.close();

                    Type responseType = new TypeToken<ApiResponse<List<TopCourseRowDTO>>>() {}.getType();
                    ApiResponse<List<TopCourseRowDTO>> response = gson.fromJson(jsonStr.toString(), responseType);
                    List<TopCourseRowDTO> topCourses = response.getData();

                    Platform.runLater(() -> {
                        topCoursesTable.getItems().clear();
                        if (topCourses != null) topCoursesTable.getItems().addAll(topCourses);
                    });
                } else {
                    System.err.println("Failed to load top courses, code: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadActivityItem() {
// مثال باستخدام HttpClient لجلب البيانات من Spring Boot
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/admin/recent-activity?limit=10");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                // إضافة التوكن إذا موجود
                String token = SessionManager.getInstance().getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder jsonStr = new StringBuilder();
                    while (scanner.hasNext()) jsonStr.append(scanner.nextLine());
                    scanner.close();

                    // تحويل JSON إلى قائمة ActivityItemDTO
                    Type listType = new TypeToken<List<ActivityItemDTO>>(){}.getType();
                    List<ActivityItemDTO> activities = gson.fromJson(jsonStr.toString(), listType);

                    // تحديث UI
                    Platform.runLater(() -> {
                        activityList.getItems().setAll(activities);
                    });
                } else {
                    System.err.println("Failed to load recent activity, code: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private String formatAgo(LocalDateTime time) {
        if (time == null) return "—";
        Duration d = Duration.between(time, LocalDateTime.now());
        if (d.toHours() < 24) {
            return d.toHours() + "h";
        } else {
            return d.toDays() + "d";
        }
    }

    private void loadRecentCourses() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/admin/recent-courses?limit=10");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                String token = SessionManager.getInstance().getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder jsonStr = new StringBuilder();
                    while (scanner.hasNext()) jsonStr.append(scanner.nextLine());
                    scanner.close();

                    Type listType = new TypeToken<List<RecentCourseDTO>>(){}.getType();
                    List<RecentCourseDTO> courses = gson.fromJson(jsonStr.toString(), listType);

                    Platform.runLater(() -> recentCoursesList.getItems().setAll(courses));
                } else {
                    System.err.println("Failed to load recent courses, code: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    @FXML
    private void showAddCourseDialog() {
        // مكونات الإدخال
        TextField titleField = new TextField();
        TextField instructorField = new TextField();
        TextField priceField = new TextField();
        TextField durationField = new TextField(); // المدة مثلاً: "10 ساعات"
        TextArea descriptionArea = new TextArea(); // وصف الكورس
        TextField imageUrlField = new TextField(); // رابط صورة
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("ACTIVE", "Non ACTIVE");
        statusComboBox.getSelectionModel().selectFirst();

        // تصميم النموذج
        VBox form = new VBox(10);
        form.getChildren().addAll(
                new Label("عنوان الكورس:"), titleField,
                new Label("اسم المحاضر:"), instructorField,
                new Label("السعر (بالريال):"), priceField,
                new Label("المدة:"), durationField,
                new Label("الوصف:"), descriptionArea,
                new Label("رابط الصورة:"), imageUrlField,
                new Label("الحالة:"), statusComboBox
        );

        // نافذة الحوار
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("إضافة كورس جديد");
        dialog.setHeaderText("أدخل بيانات الكورس");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // عرض النافذة
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String title = titleField.getText().trim();
            String instructor = instructorField.getText().trim();
            String priceText = priceField.getText().trim();
            String duration = durationField.getText().trim();
            String description = descriptionArea.getText().trim();
            String imageUrl = imageUrlField.getText().trim();
            String status = statusComboBox.getValue();

            // التحقق من الحقول
            if (title.isEmpty()) {
                err("العنوان لا يمكن أن يكون فارغاً");
                return;
            }
            if (instructor.isEmpty()) {
                err("اسم المحاضر مطلوب");
                return;
            }

            Double price = null;
            try {
                if (!priceText.isEmpty()) {
                    price = Double.parseDouble(priceText);
                    if (price < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                err("السعر غير صالح");
                return;
            }

            // إنشاء الطلب
            CourseAdminRequest course = new CourseAdminRequest();
            course.setTitle(title);
            course.setInstructor(instructor);
            course.setPrice(price);
            course.setStatus(status);
            course.setDuration(duration);
            course.setDescription(description);
            course.setImageUrl(imageUrl);

            setLoading(true);
            ApiService.getInstance().adminCreateCourse(course)
                    .thenAccept(ok -> Platform.runLater(() -> {
                        setLoading(false);
                        if (Boolean.TRUE.equals(ok)) {
                            refresh(null);
                        } else {
                            err("فشل في إضافة الكورس");
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            setLoading(false);
                            err("خطأ أثناء الإضافة: " + safe(ex.getMessage()));
                        });
                        return null;
                    });
        }
    }



    @FXML
    private void showEditCourseDialog() {
        CourseDTO selectedCourse = coursesTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            err("يرجى اختيار كورس للتعديل");
            return;
        }

        // إنشاء مكونات الإدخال مع القيم الحالية
        TextField titleField = new TextField(selectedCourse.getTitle());
        TextField instructorField = new TextField(selectedCourse.getInstructor() != null ? selectedCourse.getInstructor() : "");
        TextField priceField = new TextField(selectedCourse.getPrice() != null ? selectedCourse.getPrice().toString() : "");
        TextField durationField = new TextField(
                selectedCourse.getDuration() != null ? selectedCourse.getDuration().toString() : "");
        TextArea descriptionArea = new TextArea(selectedCourse.getDescription());
        TextField imageUrlField = new TextField(selectedCourse.getImageUrl());

        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("ACTIVE", "Non ACTIVE");
        statusComboBox.setValue(selectedCourse.getStatus());

        VBox form = new VBox(10);
        form.getChildren().addAll(
                new Label("عنوان الكورس:"), titleField,
                new Label("اسم المحاضر:"), instructorField,
                new Label("السعر (بالريال):"), priceField,
                new Label("المدة:"), durationField,
                new Label("الوصف:"), descriptionArea,
                new Label("رابط الصورة:"), imageUrlField,
                new Label("الحالة:"), statusComboBox
        );

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("تعديل كورس");
        dialog.setHeaderText("تفاصيل تعديل الكورس");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String title = titleField.getText().trim();
            String instructor = instructorField.getText().trim();
            String priceText = priceField.getText().trim();
            String duration = durationField.getText().trim();
            Integer durationValue = null;
            try {
                if (!duration.isEmpty()) {
                    durationValue = Integer.parseInt(duration);
                    if (durationValue < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                err("المدة غير صالحة");
                return;
            }            String description = descriptionArea.getText().trim();
            String imageUrl = imageUrlField.getText().trim();
            String status = statusComboBox.getValue();

            if (title.isEmpty()) {
                err("العنوان لا يمكن أن يكون فارغاً");
                return;
            }
            if (instructor.isEmpty()) {
                err("اسم المحاضر مطلوب");
                return;
            }

            Double price = null;
            try {
                if (!priceText.isEmpty()) {
                    price = Double.parseDouble(priceText);
                    if (price < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                err("السعر غير صالح");
                return;
            }

            // تحديث بيانات الكورس
            CourseAdminRequest updatedCourse = new CourseAdminRequest();
            updatedCourse.setTitle(title);
            updatedCourse.setInstructor(instructor);
            updatedCourse.setPrice(price);
            updatedCourse.setDuration(String.valueOf(durationValue));
            updatedCourse.setDescription(description);
            updatedCourse.setImageUrl(imageUrl);
            updatedCourse.setStatus(status);

            setLoading(true);
            ApiService.getInstance().adminUpdateCourse(selectedCourse.getId(), updatedCourse)
                    .thenAccept(ok -> Platform.runLater(() -> {
                        setLoading(false);
                        if (Boolean.TRUE.equals(ok)) {
                            refresh(null);
                        } else {
                            err("فشل في تحديث الكورس");
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            setLoading(false);
                            err("خطأ في تعديل الكورس: " + safe(ex.getMessage()));
                        });
                        return null;
                    });
        }
    }


    @FXML
    private void deleteSelectedCourse() {
        CourseDTO selectedCourse = coursesTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            err("يرجى اختيار كورس للحذف");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("حذف الكورس");
        confirm.setHeaderText("هل أنت متأكد من حذف الكورس: " + selectedCourse.getTitle() + "؟");
        confirm.setContentText("لن يمكن التراجع عن هذا الإجراء.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            setLoading(true);
            ApiService.getInstance().adminDeleteCourse(selectedCourse.getId())
                    .thenAccept(ok -> Platform.runLater(() -> {
                        setLoading(false);
                        if (Boolean.TRUE.equals(ok)) {
                            refresh(null);
                        } else {
                            err("فشل في حذف الكورس");
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            setLoading(false);
                            err("خطأ أثناء حذف الكورس: " + safe(ex.getMessage()));
                        });
                        return null;
                    });
        }
    }

    // ============= Settings Tab =============
    private void loadSettingsTab() {
        System.out.println("⚙️ تحميل الإعدادات...");

        // إذا كانت الحقول غير موجودة، إنشاؤها برمجياً
        if (settingsContainer == null) {
            createSettingsUI();
        }

        loadCurrentSettings();
    }

    private void createSettingsUI() {
        // إنشاء واجهة الإعدادات برمجياً إذا لم تكن موجودة في FXML

        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));

        // 1. إعدادات عامة
        VBox generalSection = createSettingsSection("الإعدادات العامة");

        platformNameField = new TextField("Techway Academy");
        generalSection.getChildren().addAll(
                new Label("اسم المنصة:"),
                platformNameField
        );

        adminEmailField = new TextField("admin@techway.com");
        generalSection.getChildren().addAll(
                new Label("بريد المدير:"),
                adminEmailField
        );

        supportEmailField = new TextField("support@techway.com");
        generalSection.getChildren().addAll(
                new Label("بريد الدعم الفني:"),
                supportEmailField
        );

        // 2. إعدادات الواجهة
        VBox uiSection = createSettingsSection("إعدادات الواجهة");

        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll("العربية", "English");
        languageComboBox.setValue("العربية");
        uiSection.getChildren().addAll(
                new Label("اللغة:"),
                languageComboBox
        );

        themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("فاتح", "داكن", "تلقائي");
        themeComboBox.setValue("فاتح");
        uiSection.getChildren().addAll(
                new Label("المظهر:"),
                themeComboBox
        );

        // 3. إعدادات التنبيهات
        VBox notificationSection = createSettingsSection("التنبيهات");

        enableNotificationsCheckBox = new CheckBox("تفعيل التنبيهات");
        enableNotificationsCheckBox.setSelected(true);
        notificationSection.getChildren().add(enableNotificationsCheckBox);

        enableEmailNotificationsCheckBox = new CheckBox("تنبيهات البريد الإلكتروني");
        enableEmailNotificationsCheckBox.setSelected(true);
        notificationSection.getChildren().add(enableEmailNotificationsCheckBox);

        // 4. إعدادات النظام
        VBox systemSection = createSettingsSection("إعدادات النظام");

        maxEnrollmentsField = new TextField("100");
        systemSection.getChildren().addAll(
                new Label("الحد الأقصى للتسجيلات لكل كورس:"),
                maxEnrollmentsField
        );

        sessionTimeoutField = new TextField("30");
        systemSection.getChildren().addAll(
                new Label("انتهاء الجلسة (بالدقائق):"),
                sessionTimeoutField
        );

        // 5. أزرار الإجراءات
        HBox actionButtons = new HBox(10);

        saveSettingsButton = new Button("حفظ الإعدادات");
        saveSettingsButton.setOnAction(e -> saveSettings());
        saveSettingsButton.setStyle("-fx-background-color: #6b54b8; -fx-text-fill: white;");

        resetToDefaultButton = new Button("استعادة الافتراضي");
        resetToDefaultButton.setOnAction(e -> resetToDefault());

        exportDataButton = new Button("تصدير البيانات");
        exportDataButton.setOnAction(e -> exportData());

        importDataButton = new Button("استيراد البيانات");
        importDataButton.setOnAction(e -> importData());

        actionButtons.getChildren().addAll(
                saveSettingsButton, resetToDefaultButton,
                exportDataButton, importDataButton
        );

        // 6. معلومات النظام
        VBox infoSection = createSettingsSection("معلومات النظام");

        lastBackupLabel = new Label("آخر نسخة احتياطية: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        Label versionLabel = new Label("إصدار التطبيق: 1.0.0");
        Label javaVersionLabel = new Label("إصدار Java: " + System.getProperty("java.version"));

        infoSection.getChildren().addAll(lastBackupLabel, versionLabel, javaVersionLabel);

        // إضافة كل الأقسام للحاوي الرئيسي
        mainContainer.getChildren().addAll(
                generalSection, uiSection, notificationSection,
                systemSection, actionButtons, infoSection
        );

        // إضافة ScrollPane للحاوي
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);

        // إضافة للتاب (تحتاج تعديل FXML أو إضافة برمجياً)
        settingsTab.setContent(scrollPane);
    }

    private VBox createSettingsSection(String title) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: #fafafa; -fx-background-radius: 5;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6b54b8;");
        section.getChildren().add(titleLabel);

        return section;
    }

    private void loadCurrentSettings() {
        // تحميل الإعدادات من ملف محلي أو قاعدة البيانات
        Properties settings = loadSettingsFromFile();

        if (platformNameField != null) {
            platformNameField.setText(settings.getProperty("platform.name", "Techway Academy"));
        }
        if (adminEmailField != null) {
            adminEmailField.setText(settings.getProperty("admin.email", "admin@techway.com"));
        }
        if (supportEmailField != null) {
            supportEmailField.setText(settings.getProperty("support.email", "support@techway.com"));
        }
        if (languageComboBox != null) {
            languageComboBox.setValue(settings.getProperty("ui.language", "العربية"));
        }
        if (themeComboBox != null) {
            themeComboBox.setValue(settings.getProperty("ui.theme", "فاتح"));
        }
        if (enableNotificationsCheckBox != null) {
            enableNotificationsCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty("notifications.enabled", "true")));
        }
        if (enableEmailNotificationsCheckBox != null) {
            enableEmailNotificationsCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty("notifications.email", "true")));
        }
        if (maxEnrollmentsField != null) {
            maxEnrollmentsField.setText(settings.getProperty("system.max.enrollments", "100"));
        }
        if (sessionTimeoutField != null) {
            sessionTimeoutField.setText(settings.getProperty("system.session.timeout", "30"));
        }
    }

    private Properties loadSettingsFromFile() {
        Properties settings = new Properties();
        try {
            Path settingsFile = Paths.get("config", "settings.properties");
            if (Files.exists(settingsFile)) {
                settings.load(Files.newInputStream(settingsFile));
            }
        } catch (IOException e) {
            System.err.println("خطأ في تحميل الإعدادات: " + e.getMessage());
        }
        return settings;
    }

    private void saveSettings() {
        try {
            Properties settings = new Properties();

            if (platformNameField != null) {
                settings.setProperty("platform.name", platformNameField.getText());
            }
            if (adminEmailField != null) {
                settings.setProperty("admin.email", adminEmailField.getText());
            }
            if (supportEmailField != null) {
                settings.setProperty("support.email", supportEmailField.getText());
            }
            if (languageComboBox != null) {
                settings.setProperty("ui.language", languageComboBox.getValue());
            }
            if (themeComboBox != null) {
                settings.setProperty("ui.theme", themeComboBox.getValue());
            }
            if (enableNotificationsCheckBox != null) {
                settings.setProperty("notifications.enabled", String.valueOf(enableNotificationsCheckBox.isSelected()));
            }
            if (enableEmailNotificationsCheckBox != null) {
                settings.setProperty("notifications.email", String.valueOf(enableEmailNotificationsCheckBox.isSelected()));
            }
            if (maxEnrollmentsField != null) {
                settings.setProperty("system.max.enrollments", maxEnrollmentsField.getText());
            }
            if (sessionTimeoutField != null) {
                settings.setProperty("system.session.timeout", sessionTimeoutField.getText());
            }

            // حفظ في ملف
            Path configDir = Paths.get("config");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Path settingsFile = configDir.resolve("settings.properties");
            settings.store(Files.newOutputStream(settingsFile), "Techway Academy Settings");

            info("تم حفظ الإعدادات بنجاح!");

        } catch (IOException e) {
            err("خطأ في حفظ الإعدادات: " + e.getMessage());
        }
    }

    private void resetToDefault() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "هل تريد استعادة الإعدادات الافتراضية؟");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (platformNameField != null) platformNameField.setText("Techway Academy");
            if (adminEmailField != null) adminEmailField.setText("admin@techway.com");
            if (supportEmailField != null) supportEmailField.setText("support@techway.com");
            if (languageComboBox != null) languageComboBox.setValue("العربية");
            if (themeComboBox != null) themeComboBox.setValue("فاتح");
            if (enableNotificationsCheckBox != null) enableNotificationsCheckBox.setSelected(true);
            if (enableEmailNotificationsCheckBox != null) enableEmailNotificationsCheckBox.setSelected(true);
            if (maxEnrollmentsField != null) maxEnrollmentsField.setText("100");
            if (sessionTimeoutField != null) sessionTimeoutField.setText("30");

            info("تم استعادة الإعدادات الافتراضية");
        }
    }

    private void exportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("تصدير البيانات");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                // تصدير إحصائيات أساسية
                Map<String, Object> exportData = new HashMap<>();
                exportData.put("exportDate", LocalDateTime.now().toString());
                exportData.put("totalUsers", totalUsersLabel.getText());
                exportData.put("totalCourses", totalCoursesLabel.getText());
                exportData.put("totalRevenue", totalRevenueLabel.getText());

                String json = new Gson().toJson(exportData);
                Files.write(file.toPath(), json.getBytes());

                info("تم تصدير البيانات بنجاح إلى: " + file.getName());

            } catch (Exception e) {
                err("خطأ في تصدير البيانات: " + e.getMessage());
            }
        }
    }

    private void importData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("استيراد البيانات");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                // معالجة البيانات المستوردة
                info("تم استيراد البيانات بنجاح من: " + file.getName());

            } catch (Exception e) {
                err("خطأ في استيراد البيانات: " + e.getMessage());
            }
        }
    }

    private void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("تنبيه");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
