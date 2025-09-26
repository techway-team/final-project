package com.techway.coursemanagementdesktop.controller.admin;

import com.techway.coursemanagementdesktop.CourseDTO;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;

public class CourseDialogs {

    public static class CourseFormData {
        public String title, description, location, instructor, status, imageUrl;
        public BigDecimal price;
        public Integer duration;

        public CourseFormData(String title, String description, String instructor, String duration, String location, BigDecimal price, boolean isFree, String imageUrl) {
        }

        public CourseFormData() {

        }
    }

    public static CourseFormData showCourseDialog(CourseDTO existing) {
        Dialog<CourseFormData> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "إضافة كورس" : "تعديل كورس");
        ButtonType save = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField title = new TextField();
        TextField instructor = new TextField();
        TextField location = new TextField();
        TextField status = new TextField();
        TextField imageUrl = new TextField();
        TextField price = new TextField();
        TextField duration = new TextField();
        TextArea desc = new TextArea();

        if (existing != null) {
            title.setText(existing.getTitle());
            instructor.setText(existing.getInstructor());
            location.setText(existing.getLocation());
            status.setText(existing.getStatus());
            imageUrl.setText(existing.getImageUrl());
            price.setText(existing.getPrice() == null ? "" : existing.getPrice().stripTrailingZeros().toPlainString());
            duration.setText(existing.getDuration() == null ? "" : String.valueOf(existing.getDuration()));
            desc.setText(existing.getDescription());
        }

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));
        gp.addRow(0, new Label("العنوان"), title);
        gp.addRow(1, new Label("المدرّس"), instructor);
        gp.addRow(2, new Label("المكان"), location);
        gp.addRow(3, new Label("الحالة"), status);
        gp.addRow(4, new Label("رابط الصورة"), imageUrl);
        gp.addRow(5, new Label("السعر"), price);
        gp.addRow(6, new Label("المدّة (ساعات)"), duration);
        gp.addRow(7, new Label("الوصف"), desc);
        dialog.getDialogPane().setContent(gp);

        // فالديشن بسيط قبل الحفظ
        Node saveBtn = dialog.getDialogPane().lookupButton(save);
        saveBtn.setDisable(true);
        title.textProperty().addListener((o, ov, nv) -> saveBtn.setDisable(nv == null || nv.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn == save) {
                CourseFormData d = new CourseFormData();
                d.title = title.getText().trim();
                d.instructor = instructor.getText().trim();
                d.location = location.getText().trim();
                d.status = status.getText().trim();
                d.imageUrl = imageUrl.getText().trim();
                d.price = parseDecimal(price.getText().trim());
                d.duration = parseInt(duration.getText().trim());
                d.description = desc.getText().trim();
                return d;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private static BigDecimal parseDecimal(String s){
        try { return new BigDecimal(s); } catch(Exception e){ return BigDecimal.ZERO; }
    }
    private static Integer parseInt(String s){
        try { return Integer.parseInt(s); } catch(Exception e){ return null; }
    }
}
