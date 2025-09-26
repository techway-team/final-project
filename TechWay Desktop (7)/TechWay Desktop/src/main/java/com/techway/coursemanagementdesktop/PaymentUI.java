package com.techway.coursemanagementdesktop;

import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class PaymentUI {

    private final StackPane root;

    public PaymentUI(StackPane root) {
        this.root = root;
    }

    public void showToast(String message) {
        Label toast = new Label(message);
        toast.setStyle(
                "-fx-background-color: #4CAF50;" +   // أخضر
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;"
        );

        root.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2));

        fadeIn.setOnFinished(e -> fadeOut.play());
        fadeOut.setOnFinished(e -> root.getChildren().remove(toast));

        fadeIn.play();
    }
}
