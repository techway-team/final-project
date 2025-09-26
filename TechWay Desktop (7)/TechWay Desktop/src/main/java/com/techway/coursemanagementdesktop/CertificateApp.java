package com.techway.coursemanagementdesktop;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CertificateApp{

    private TextField userIdField = new TextField();
    private TextField courseIdField = new TextField();
    private TextArea resultArea = new TextArea();

    private final String BASE_URL = "http://localhost:8080/api/certificates";  // غيّر حسب الباكند


    public void start(Stage stage) {
        Label userIdLabel = new Label("User ID:");
        Label courseIdLabel = new Label("Course ID:");

        Button btnGetCertificate = new Button("Get Certificate Info");
        btnGetCertificate.setOnAction(e -> getCertificate());

        VBox root = new VBox(10, userIdLabel, userIdField, courseIdLabel, courseIdField, btnGetCertificate, resultArea);
        root.setPadding(new Insets(15));

        resultArea.setPrefHeight(200);

        Scene scene = new Scene(root, 400, 350);
        stage.setScene(scene);
        stage.setTitle("Certificate Viewer");
        stage.show();
    }

    private void getCertificate() {
        String userId = userIdField.getText().trim();
        String courseId = courseIdField.getText().trim();

        if (userId.isEmpty() || courseId.isEmpty()) {
            resultArea.setText("Please enter both User ID and Course ID.");
            return;
        }

        String url = BASE_URL + "/user/" + userId + "/course/" + courseId;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        resultArea.setText("Loading...");

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    // لتبسيط، نعرض الرد كنص خام JSON
                    javafx.application.Platform.runLater(() -> resultArea.setText(response));
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> resultArea.setText("Error: " + ex.getMessage()));
                    return null;
                });
    }


}
