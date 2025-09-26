package com.techway.coursemanagementdesktop.controller;


import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class VideoPlayerDialog {

    public static void show(String videoUrl) {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("مشاهدة الدرس");
            stage.setMinWidth(800);
            stage.setMinHeight(500);
            stage.initStyle(StageStyle.DECORATED);

            BorderPane root = new BorderPane();
            root.setPadding(new Insets(12));

            // زر الإغلاق
            Button closeButton = new Button("إغلاق ✖");
            closeButton.setOnAction(e -> stage.close());
            HBox topBar = new HBox(closeButton);
            topBar.setPadding(new Insets(0, 0, 10, 0));
            root.setTop(topBar);

            if (isMp4(videoUrl)) {
                try {
                    Media media = new Media(videoUrl);
                    MediaPlayer player = new MediaPlayer(media);
                    MediaView mediaView = new MediaView(player);

                    mediaView.setPreserveRatio(true);
                    mediaView.setFitWidth(780);
                    mediaView.setFitHeight(440);

                    root.setCenter(mediaView);
                    player.play();
                } catch (Exception e) {
                    root.setCenter(new javafx.scene.control.Label("فشل تشغيل الفيديو."));
                }

            } else if (isYouTube(videoUrl)) {
                WebView webView = new WebView();
                webView.setPrefSize(780, 440);

                String embedUrl = convertToEmbedUrl(videoUrl);

                String content = "<html><body style='margin:0;padding:0;'>" +
                        "<iframe width='100%' height='100%' " +
                        "src='" + embedUrl + "' " +
                        "frameborder='0' allowfullscreen></iframe>" +
                        "</body></html>";

                webView.getEngine().loadContent(content, "text/html");

                root.setCenter(webView);
            } else {
                root.setCenter(new javafx.scene.control.Label("نوع فيديو غير مدعوم."));
            }

            Scene scene = new Scene(root, 800, 500);
            stage.setScene(scene);
            stage.showAndWait();
        });
    }

    private static boolean isMp4(String url) {
        return url != null && url.toLowerCase().endsWith(".mp4");
    }

    private static boolean isYouTube(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }

    private static String convertToEmbedUrl(String url) {
        if (url.contains("youtube.com/watch?v=")) {
            String videoId = url.substring(url.indexOf("v=") + 2);
            int amp = videoId.indexOf("&");
            if (amp != -1) videoId = videoId.substring(0, amp);
            return "https://www.youtube.com/embed/" + videoId;
        } else if (url.contains("youtu.be/")) {
            String videoId = url.substring(url.lastIndexOf("/") + 1);
            return "https://www.youtube.com/embed/" + videoId;
        }
        return url; // fallback
    }
}