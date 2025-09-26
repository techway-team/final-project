package com.techway.coursemanagementdesktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class PaymentFormHD {

    private TextField cardNameField;
    private TextField cardNumberField;
    private TextField expiryField;
    private TextField cvvField;

    // رسائل الخطأ
    private Label cardNameError;
    private Label cardNumberError;
    private Label expiryError;
    private Label cvvError;

    public VBox build(Runnable onPaymentSuccess) {
        VBox card = new VBox(18);
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(420);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;"
        );
        card.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.15)));

        // الاسم على البطاقة
        VBox cardNameBox = createInputField("الاسم على البطاقة");
        cardNameField = new TextField();
        styleTextField(cardNameField);
        cardNameError = createErrorLabel();
        cardNameBox.getChildren().addAll(cardNameField, cardNameError);

        // رقم البطاقة
        VBox cardNumberBox = createInputField("رقم البطاقة");
        cardNumberField = new TextField();
        cardNumberField.setPromptText("XXXX XXXX XXXX XXXX");
        styleTextField(cardNumberField);
        cardNumberError = createErrorLabel();
        cardNumberBox.getChildren().addAll(cardNumberField, cardNumberError);

        // تاريخ الانتهاء + CVV
        HBox bottomRow = new HBox(15);

        VBox expiryBox = createInputField("تاريخ الانتهاء");
        expiryField = new TextField();
        expiryField.setPromptText("MM/YY");
        styleTextField(expiryField);
        expiryError = createErrorLabel();
        expiryBox.getChildren().addAll(expiryField, expiryError);

        VBox cvvBox = createInputField("CVV");
        cvvField = new TextField();
        cvvField.setPromptText("***");
        styleTextField(cvvField);
        cvvError = createErrorLabel();
        cvvBox.getChildren().addAll(cvvField, cvvError);

        bottomRow.getChildren().addAll(expiryBox, cvvBox);

        // زر الدفع
        Button submitBtn = new Button("تأكيد الدفع");
        submitBtn.setFont(Font.font(16));
        submitBtn.setTextFill(Color.WHITE);
        submitBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #8B5CF6, #7C3AED);" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 12 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-weight: bold;"
        );

        submitBtn.setOnAction(e -> {
            if (validateFields()) {
                if (onPaymentSuccess != null) {
                    onPaymentSuccess.run();
                }
            }
        });

        card.getChildren().addAll(cardNameBox, cardNumberBox, bottomRow, submitBtn);
        return card;
    }

    private VBox createInputField(String labelText) {
        VBox box = new VBox(6);
        Label label = new Label(labelText);
        label.setFont(Font.font(14));
        label.setTextFill(Color.web("#374151"));
        box.getChildren().add(label);  // إضافة الـ Label للصندوق

        return box;
    }

    private Label createErrorLabel() {
        Label error = new Label();
        error.setTextFill(Color.web("#DC2626")); // أحمر
        error.setFont(Font.font(11));
        error.setVisible(false);
        return error;
    }

    private void styleTextField(TextField field) {
        field.setStyle(
                "-fx-background-color: #F9FAFB;" +
                        "-fx-border-color: #C4B5FD;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-font-size: 13px;"
        );
    }

    private boolean validateFields() {
        boolean valid = true;

        // الاسم
        if (cardNameField.getText().trim().isEmpty()) {
            setError(cardNameField, cardNameError, "الاسم مطلوب");
            valid = false;
        } else {
            setValid(cardNameField, cardNameError);
        }

        // رقم البطاقة
        if (!cardNumberField.getText().matches("\\d{16}")) {
            setError(cardNumberField, cardNumberError, "رقم البطاقة يجب أن يكون 16 رقمًا");
            valid = false;
        } else {
            setValid(cardNumberField, cardNumberError);
        }

        // تاريخ الانتهاء
        if (!expiryField.getText().matches("(0[1-9]|1[0-2])/\\d{2}")) {
            setError(expiryField, expiryError, "استخدم الصيغة MM/YY");
            valid = false;
        } else {
            setValid(expiryField, expiryError);
        }

        // CVV
        if (!cvvField.getText().matches("\\d{3}")) {
            setError(cvvField, cvvError, "الـ CVV يتكون من 3 أرقام");
            valid = false;
        } else {
            setValid(cvvField, cvvError);
        }

        return valid;
    }

    private void setError(TextField field, Label errorLabel, String message) {
        field.setStyle(
                "-fx-background-color: #FEE2E2;" +
                        "-fx-border-color: #DC2626;" +   // أحمر
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-font-size: 13px;"
        );
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void setValid(TextField field, Label errorLabel) {
        field.setStyle(
                "-fx-background-color: #F9FAFB;" +
                        "-fx-border-color: #8B5CF6;" +   // موف
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-font-size: 13px;"
        );
        errorLabel.setVisible(false);
    }
}
