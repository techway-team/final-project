package com.techway.coursemanagementdesktop.controller.admin;

import com.techway.coursemanagementdesktop.CourseDTO;
import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.service.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class CoursesAdminController {

    @FXML private TableView<Course> table;
    @FXML private TableColumn<Course, Long> idCol;
    @FXML private TableColumn<Course, String> titleCol;
    @FXML private TableColumn<Course, String> instructorCol;
    @FXML private TableColumn<Course, BigDecimal> priceCol;
    @FXML private TableColumn<Course, String> locationCol;
    @FXML private TableColumn<Course, String> statusCol;
    @FXML private TextField searchField;
    @FXML private TableColumn<Course, Boolean> isFreeCol;


    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        instructorCol.setCellValueFactory(new PropertyValueFactory<>("instructor"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        isFreeCol.setCellValueFactory(new PropertyValueFactory<>("isFree"));

        // نخليها تعرض "نعم" أو "لا" بدل true/false
        isFreeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(val ? "نعم" : "لا");
                }
            }
        });

        // عرض السعر كنص
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty ? null : (val == null ? "0" : val.stripTrailingZeros().toPlainString()));
            }
        });



        // Double-click للتعديل
        table.setRowFactory(tv -> {
            TableRow<Course> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) onEdit();
            });
            return row;
        });



        // قائمة يمين: تعديل/حذف
        MenuItem edit = new MenuItem("تعديل");
        edit.setOnAction(e -> onEdit());
        MenuItem del = new MenuItem("حذف");
        del.setOnAction(e -> onDelete());
        table.setContextMenu(new ContextMenu(edit, del));

        refresh(null);


    }

    private void refresh(String q) {
        setLoading(true);
        ApiService.getInstance().adminListCourses(q)
                .thenAccept((List<Course> list) ->
                        Platform.runLater(() -> {
                            table.getItems().setAll(list);
                            setLoading(false);
                        }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        err("فشل تحميل الكورسات: " + safe(ex.getMessage()));
                    });
                    return null;
                });
    }

    @FXML private void onSearch() { refresh(value(searchField)); }

    @FXML
    private void onAdd() {
        CourseDialogs.CourseFormData data = CourseDialogs.showCourseDialog(null);
        if (data == null) return;

        setLoading(true);
        ApiService.getInstance().adminCreateCourse(data)
                .thenAccept(ok -> Platform.runLater(() -> {
                    setLoading(false);
                    if (Boolean.TRUE.equals(ok)) refresh(null);
                    else err("فشل إنشاء الكورس");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        err("خطأ أثناء الإنشاء: " + safe(ex.getMessage()));
                    });
                    return null;
                });
    }

    @FXML
    private void onEdit() {
        Course c = table.getSelectionModel().getSelectedItem();
        if (c == null) { err("اختَر كورس"); return; }

        CourseDialogs.CourseFormData data = CourseDialogs.showCourseDialog(CourseDTO.fromCourse(c));
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
        Course c = table.getSelectionModel().getSelectedItem();
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

    /* Helpers */
    private void setLoading(boolean loading) {
        if (table != null) table.setDisable(loading);
        if (searchField != null) searchField.setDisable(loading);
    }
    private void err(String m) { new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
    private static String value(TextField f) { return f == null || f.getText() == null ? "" : f.getText().trim(); }
    private static String safe(String s) { return s == null ? "" : s; }
}
