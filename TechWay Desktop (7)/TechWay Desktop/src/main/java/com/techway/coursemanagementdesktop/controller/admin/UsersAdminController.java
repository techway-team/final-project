package com.techway.coursemanagementdesktop.controller.admin;

import com.techway.coursemanagementdesktop.model.User;
import com.techway.coursemanagementdesktop.service.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Optional;

public class UsersAdminController {

    @FXML private TableView<User> table;
    @FXML private TableColumn<User, Long> idCol;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TextField searchField;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        Platform.runLater(() -> {
            table.getScene().getStylesheets().add(
                    getClass().getResource("C:\\Users\\rh.aljasser\\Downloads\\TechWay Desktop (7)\\TechWay Desktop\\src\\main\\resources\\css\\admin-styles.css").toExternalForm()
            );
        });


        refresh(null);
    }

    /* ========================= Data ========================= */

    private void refresh(String q) {
        setLoading(true);
        ApiService.getInstance().adminListUsers(q)
                .thenAccept((List<User> users) ->
                        Platform.runLater(() -> {
                            table.getItems().setAll(users);
                            setLoading(false);
                        }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        err("فشل تحميل المستخدمين: " + safe(ex.getMessage()));
                    });
                    return null;
                });
    }

    /* ========================= UI Actions ========================= */

    @FXML private void onSearch() { refresh(value(searchField)); }

    @FXML private void onPromote() { roleOp("ADMIN"); }

    @FXML private void onDemote()  { roleOp("USER"); }

    private void roleOp(String role) {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { err("اختَر مستخدم"); return; }

        setLoading(true);
        ApiService.getInstance().adminSetRole(u.getId(), role)
                .thenAccept(ok -> Platform.runLater(() -> {
                    setLoading(false);
                    if (Boolean.TRUE.equals(ok)) refresh(null);
                    else err("فشل تغيير الدور");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        err("خطأ أثناء تغيير الدور: " + safe(ex.getMessage()));
                    });
                    return null;
                });
    }

    @FXML
    private void onDisable() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { err("اختَر مستخدم"); return; }

        setLoading(true);
        ApiService.getInstance().adminDisableUser(u.getId())
                .thenAccept(ok -> Platform.runLater(() -> {
                    setLoading(false);
                    if (Boolean.TRUE.equals(ok)) refresh(null);
                    else err("فشل التعطيل");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        err("خطأ أثناء التعطيل: " + safe(ex.getMessage()));
                    });
                    return null;
                });
    }

    @FXML
    private void onDelete() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { err("اختَر مستخدم"); return; }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "حذف المستخدم #" + u.getId() + "؟");
        Optional<ButtonType> res = a.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        setLoading(true);
        ApiService.getInstance().adminDeleteUser(u.getId())
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

    /* ========================= Helpers ========================= */

    private void setLoading(boolean loading) {
        if (table != null) table.setDisable(loading);
        if (searchField != null) searchField.setDisable(loading);
    }

    private void err(String m) {
        new Alert(Alert.AlertType.ERROR, m).showAndWait();
    }

    private static String value(TextField f) {
        return f == null || f.getText() == null ? "" : f.getText().trim();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
