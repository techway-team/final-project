package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PaymentPageController {
    private Course course;
    private Runnable onPaymentSuccess;

    public PaymentPageController(Course course, Runnable onPaymentSuccess) {
        this.course = course;
        this.onPaymentSuccess = onPaymentSuccess;
    }

    public VBox createPaymentPage(Course course, SessionManager sessionManager) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        Label title = new Label("دفع الكورس: " + course.getTitle());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label priceLabel = new Label("السعر: " + course.getPriceDisplay());
        priceLabel.setStyle("-fx-font-size: 16px;");

        Button payButton = new Button("ادفع الآن");
        payButton.getStyleClass().add("primary-button");
        payButton.setOnAction(e -> {
            Long userId = sessionManager.getCurrentUserId();
            Long courseId = course.getId();

            ApiService.getInstance().makePayment(userId, courseId)
                    .thenAccept(paymentResult -> {
                        Platform.runLater(() -> {
                            if (paymentResult.isSuccess()) {
                                Alert a = new Alert(Alert.AlertType.INFORMATION,
                                        "تم الدفع بنجاح! يمكنك الآن الوصول إلى جميع الدروس.",
                                        ButtonType.OK);
                                a.showAndWait();

                                if (onPaymentSuccess != null) {
                                    onPaymentSuccess.run();
                                }

                                // إغلاق نافذة الدفع
                                Stage stage = (Stage) payButton.getScene().getWindow();
                                stage.close();

                            } else {
                                Alert a = new Alert(Alert.AlertType.ERROR,
                                        "فشل الدفع: " + paymentResult.getErrorMessage(),
                                        ButtonType.OK);
                                a.showAndWait();
                            }
                        });
                    });
        });

        root.getChildren().addAll(title, priceLabel, payButton);
        return root;
    }
}
