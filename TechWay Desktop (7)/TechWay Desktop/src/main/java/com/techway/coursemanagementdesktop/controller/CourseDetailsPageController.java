package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.CertificateDTO;
import com.techway.coursemanagementdesktop.PaymentFormHD;
import com.techway.coursemanagementdesktop.PaymentUI;
import com.techway.coursemanagementdesktop.model.*;


import com.techway.coursemanagementdesktop.service.ApiService;
import com.techway.coursemanagementdesktop.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import okhttp3.HttpUrl;

import java.awt.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.time.format.DateTimeFormatter;

public class CourseDetailsPageController {

    private static final String API_BASE = "http://localhost:8080";
    private final MainController mainController;
    private final SessionManager sessionManager;

    /** كاش بسيط للصور */
    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private Long enrollmentId;
    @FXML
    private VBox lessonsContainer;

    private Course currentCourse;

    private VBox lessonsListBox; // نخزنها كـ global variable

    @FXML
    private Button startQuizButton;


    private Quiz currentQuiz;
    private Long currentAttemptId;
    private int currentIndex = 0;

    @FXML private VBox quizContent;
    @FXML private Button submitButton;

    // بيانات الكويز
    private List<Question> questions;

    private List<ToggleGroup> questionToggleGroups = new ArrayList<>();

    // واجهة الكويز


    private Label quizTitleLabel = new Label();
    private Long quizId;
    private VBox quizSectionContainer;

    private Label statusLabel;
    private VBox quizBox;

    // ===== إضافة المتغيرات الجديدة =====

    private Button btnStartQuiz;
    private Button btnViewCertificate;
    private VBox quizSection;
    private VBox certificateSection;

    // إضافة متغيرات لتتبع التقدم
    private ProgressBar courseProgressBar;
    private Label progressLabel;
    private List<LessonProgress> userProgress = new ArrayList<>();
    private Long currentEnrollmentId;


    private Map<Long, List<LessonProgress>> courseProgressMap = new HashMap<>();
    private Map<Long, Long> courseEnrollmentMap = new HashMap<>();
    private final Set<Long> favoriteIds = ConcurrentHashMap.newKeySet();




    public CourseDetailsPageController(MainController mainController, SessionManager sessionManager,Course currentCourse) {
        this.mainController = mainController;
        this.sessionManager = sessionManager;
        this.currentCourse = currentCourse;

    }

    /* =============================== UI =============================== */

    /** يبني صفحة التفاصيل */
    public VBox createCourseDetailsPage(Course course) {

        Long userId = SessionManager.getInstance().getCurrentUserId();

        VBox root = new VBox(16);
        root.getStyleClass().add("details-page");
        root.setPadding(new Insets(24));

        // رجوع
        Hyperlink back = new Hyperlink("⬅ العودة");
        back.getStyleClass().add("back-link");
        back.setOnAction(e -> mainController.navigateToCourses());
        root.getChildren().add(back);

        // شبكة عمودين
        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(24);
        ColumnConstraints left = new ColumnConstraints();
        left.setPrefWidth(320);
        left.setMinWidth(280);
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(left, right);

        /* ===== العمود الأيمن: هيدر وصورة وميتا وأقسام ===== */
        VBox rightCol = new VBox(16);

        // هيرو
        StackPane hero = new StackPane();
        hero.getStyleClass().add("details-hero");
        hero.setMinHeight(260);
        hero.setPrefHeight(260);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        hero.layoutBoundsProperty().addListener((obs, o, b) -> {
            clip.setWidth(b.getWidth());
            clip.setHeight(b.getHeight());
        });
        hero.setClip(clip);

        ImageView heroImage = new ImageView();
        heroImage.setPreserveRatio(false);
        heroImage.setSmooth(true);
        heroImage.fitWidthProperty().bind(hero.widthProperty());
        heroImage.fitHeightProperty().bind(hero.heightProperty());
        loadCourseImage(heroImage, getString(course, "getImageUrl"));

        boolean isFree = Boolean.TRUE.equals(getBoolean(course, "getIsFree"));
        Label badge = new Label(isFree ? "مجاني" : "مدفوع");
        badge.getStyleClass().add("chip");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(12));

        hero.getChildren().addAll(heroImage, badge);

        // العنوان
        String titleStr = or(
                getString(course, "getTitle"),
                "Course Title"
        );
        Label title = new Label(titleStr);
        title.getStyleClass().add("details-title");
        title.setWrapText(true);

        // ميتا
        HBox meta = new HBox(18);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(
                metaItem("⏱", durationText(course)),
                metaItem("📍", or(getString(course, "getLocationDisplay"), "—")),
                metaItem("👤", or(getString(course, "getInstructor"), "—"))
        );

        rightCol.getChildren().addAll(hero, title, meta);

        // وصف
        rightCol.getChildren().add(sectionCard(
                "وصف الكورس",
                or(
                        getString(course, "getLongDescription"),
                        getString(course, "getDescription"),
                        getString(course, "getShortDescription"),
                        "لا توجد بيانات متاحة حالياً."
                )
        ));

        // ما ستتعلمه
        rightCol.getChildren().add(listSection("ما ستتعلمه:", learningList(course)));

        // المتطلبات
        rightCol.getChildren().add(listSection("المتطلبات:", requirementsList(course)));

        // المحتوى
        rightCol.getChildren().add(syllabusSection("محتوى الكورس:", syllabusList(course)));
        rightCol.getChildren().add(lessonsSection(course, enrollmentId));
        VBox quizAndCertificatesSection = createQuizAndCertificatesSection(course);
        rightCol.getChildren().add(quizAndCertificatesSection);

        Button startQuizButton = new Button("ابدأ الكويز");
        startQuizButton.getStyleClass().add("primary-button");
        startQuizButton.setMaxWidth(Double.MAX_VALUE);
        startQuizButton.setVisible(false); // نخفيه مبدئيًا

        Button viewCertificateButton = new Button("🎓 عرض الشهادة");
        viewCertificateButton.getStyleClass().add("primary-button");
        viewCertificateButton.setMaxWidth(Double.MAX_VALUE);
        viewCertificateButton.setVisible(false); // نخفيه مبدئيًا

        rightCol.getChildren().add(viewCertificateButton);



        rightCol.getChildren().add(startQuizButton);


// 👇 بعدها تتحقق هل يوجد كويز للكورس
        Long courseId = course.getId();











        /* ===== العمود الأيسر: بطاقة التسجيل + معلومات سريعة ===== */
        VBox leftCol = new VBox(16);

        VBox purchase = new VBox(12);
        purchase.getStyleClass().add("purchase-card");
        purchase.setPadding(new Insets(16));

        Label price = new Label(priceText(course));
        price.getStyleClass().add("price-amount");



        Button enroll = new Button();
        enroll.getStyleClass().add("primary-button");
        enroll.setMaxWidth(Double.MAX_VALUE);

        Button payButton = new Button("سجل وادفع الآن");
        payButton.getStyleClass().add("primary-button");
        payButton.setMaxWidth(Double.MAX_VALUE);
        payButton.setVisible(false);
        payButton.setDisable(true);

        Long[] currentEnrollmentId = new Long[1]; // لتخزين enrollmentId

// زر التسجيل الافتراضي
        enroll.setText("جاري التحقق...");
        enroll.setDisable(true);

// ==================== handler لفتح فورم الدفع (نستخدمه في كل الحالات) ====================
        EventHandler<ActionEvent> openPaymentHandler = ev -> {
            Stage paymentStage = new Stage();
            paymentStage.initModality(Modality.APPLICATION_MODAL);

            PaymentFormHD paymentForm = new PaymentFormHD();

            VBox form = paymentForm.build(() -> {
                // هذا الكود يُنفَّذ عندما يضغط المستخدم "تأكيد الدفع" داخل الفورم
                // نغير حالة زر الدفع ونبدأ عملية الـ API
                payButton.setDisable(true);
                String previousText = payButton.getText();
                payButton.setText("جاري الدفع...");

                if (currentEnrollmentId[0] == null) {
                    Platform.runLater(() -> {
                        paymentStage.close();
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                "حدث خطأ: لم يتم العثور على معرف التسجيل.",
                                ButtonType.OK);
                        alert.setHeaderText("فشل في الدفع");
                        alert.showAndWait();

                        payButton.setDisable(false);
                        payButton.setText(previousText);
                    });
                    return;
                }

                ApiService.getInstance().markPaid(currentEnrollmentId[0])
                        .thenAccept(paidEnrollment -> Platform.runLater(() -> {
                            // نجاح الدفع
                            paymentStage.close();
                            StackPane rootPane = new StackPane();
                            PaymentUI paymentUI = new PaymentUI(rootPane);
                            paymentUI.showToast("✅ تم الدفع وتفعيل دخولك للكورس: " + course.getTitle());


                            // بعد ما يقفل التنبيه نقفل نافذة الدفع
                            paymentStage.close();

                            // تغيير الزر ليصبح دخول للدروس
                            payButton.setText("ادخل لمشاهدة الدروس");
                            payButton.setDisable(false);
                            payButton.setOnAction(ev2 -> mainController.openCourseContent(course));

                            // تحديث واجهة المستخدم حسب الحاجة
                            refreshUIAfterEnrollment();
                        }))
                        .exceptionally(ex -> {
                            // حسب المنطق السابق: في حالة أي خطأ نعاملها كنجاح (لتفادي رسائل json غير مرغوب بها)
                            Platform.runLater(() -> {
                                paymentStage.close();
                                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                                        "تم الدفع وتفعيل دخولك للكورس: " + course.getTitle(),
                                        ButtonType.OK);
                                alert.setHeaderText("نجاح الدفع ✅");
                                alert.showAndWait();

                                payButton.setText("ادخل لمشاهدة الدروس");
                                payButton.setDisable(false);
                                payButton.setOnAction(ev2 -> mainController.openCourseContent(course));

                                refreshUIAfterEnrollment();
                            });
                            return null;
                        });
            });

            Scene paymentScene = new Scene(new StackPane(form), 520, 420);
            paymentStage.setScene(paymentScene);
            paymentStage.setTitle("نموذج الدفع");
            paymentStage.showAndWait();
        };
// رابطنا الـ handler بزر الدفع
        payButton.setOnAction(openPaymentHandler);
// =======================================================================================

        if (userId == null) {
            enroll.setText(isFree ? "التسجيل المجاني" : "التسجيل في الكورس");
            enroll.setDisable(false);
            enroll.setOnAction(e -> mainController.navigateToLogin());
        } else {
            ApiService.getInstance().getEnrollmentDetails(userId, courseId)
                    .thenAccept(enrollment -> {
                        Platform.runLater(() -> {
                            boolean isRegistered = enrollment != null;
                            boolean hasPaid = enrollment != null && Boolean.TRUE.equals(enrollment.isPaid());

                            if (isRegistered) currentEnrollmentId[0] = enrollment.getId();

                            if (!isRegistered) {
                                enroll.setText(isFree ? "سجل الآن مجاناً" : "سجل في الكورس");
                                enroll.setDisable(false);
                                enroll.setOnAction(e -> {
                                    enroll.setDisable(true);
                                    enroll.setText("جاري التسجيل...");
                                    ApiService.getInstance().enrollUser(userId, courseId)
                                            .thenAccept(enrolledCourse -> Platform.runLater(() -> {
                                                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                                                        "تم تسجيلك في الكورس بنجاح " + course.getTitle(),
                                                        ButtonType.OK);
                                                alert.setHeaderText("نجاح التسجيل ✅");
                                                alert.showAndWait();
                                                currentEnrollmentId[0] = enrolledCourse.getId();
                                                if (isFree) {
                                                    enroll.setText("ادخل الكورس");
                                                    enroll.setDisable(false);
                                                    enroll.setOnAction(ev -> mainController.openCourseContent(course));
                                                } else {
                                                    enroll.setVisible(false);
                                                    payButton.setVisible(true);
                                                    payButton.setDisable(false);
                                                    // payButton بالفعل مرتبط بـ openPaymentHandler أعلاه
                                                }
                                            }))
                                            .exceptionally(ex -> {
                                                Platform.runLater(() -> {
                                                    // تجاهل أي رسالة خطأ
                                                    enroll.setDisable(false);
                                                    enroll.setText(isFree ? "سجل الآن مجاناً" : "سجل في الكورس");
                                                });
                                                return null;
                                            });
                                });
                                payButton.setVisible(false);
                            } else {
                                if (isFree || hasPaid) {
                                    enroll.setText("ادخل الكورس");
                                    enroll.setDisable(false);
                                    enroll.setOnAction(e -> mainController.openCourseContent(course));
                                    payButton.setVisible(false);
                                } else {
                                    enroll.setVisible(false);
                                    payButton.setVisible(true);
                                    payButton.setDisable(false);
                                    // payButton already wired to openPaymentHandler
                                    // إذا أردت إعادة ضبط handler هنا أيضاً يمكن وضع:
                                    // payButton.setOnAction(openPaymentHandler);
                                }
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            // تجاهل أي خطأ في التحقق من التسجيل
                            enroll.setText(isFree ? "سجل الآن مجاناً" : "سجل في الكورس");
                            enroll.setDisable(false);
                            payButton.setVisible(false);
                        });
                        return null;
                    });
        }



        HBox quick = new HBox(8);
        quick.setAlignment(Pos.CENTER);
        Button save = new Button("♡ حفظ");
        save.getStyleClass().add("ghost-button");
        save.setOnAction(e -> {
            if (course == null || course.getId() == null) return;

            if (SessionManager.getInstance() == null || !SessionManager.getInstance().isLoggedIn()) {
                new Alert(Alert.AlertType.INFORMATION, "سجّل الدخول لحفظ الكورسات في المفضلة.").showAndWait();
                return;
            }

            save.setDisable(true);
            final boolean targetFav = !isFavorite(course.getId());

            CompletableFuture<Boolean> fut = targetFav
                    ? ApiService.getInstance().addFavorite(course.getId())
                    : ApiService.getInstance().removeFavorite(course.getId());

            fut.thenAccept(ok -> Platform.runLater(() -> {
                save.setDisable(false);
                if (Boolean.TRUE.equals(ok)) {
                    if (targetFav) favoriteIds.add(course.getId());
                    else favoriteIds.remove(course.getId());

                    // تغيير النص ولون الخلفية مباشرة حسب الحالة
                    save.setText(targetFav ? "❤️ محفوظ" : "♡ حفظ");
                    if (targetFav) {
                        save.setStyle("-fx-background-color: linear-gradient(to right, #8B5CF6, #7C3AED); -fx-text-fill: white; -fx-font-weight: bold;");
                    } else {
                        save.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151; -fx-font-weight: normal;");
                    }
                } else {
                    save.setText(!targetFav ? "❤️ محفوظ" : "♡ حفظ");
                    save.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151; -fx-font-weight: normal;");
                    new Alert(Alert.AlertType.ERROR, "تعذّر تحديث المفضلة.").showAndWait();
                }
            })).exceptionally(ex -> {
                Platform.runLater(() -> {
                    save.setDisable(false);
                    save.setText(!targetFav ? "❤️ محفوظ" : "♡ حفظ");
                    save.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151; -fx-font-weight: normal;");
                    new Alert(Alert.AlertType.ERROR, "خطأ في الاتصال بالمخدم.").showAndWait();
                });
                return null;
            });
        });




        Button share = new Button("↗ مشاركة");
        share.getStyleClass().add("ghost-button");
        share.setOnAction(e -> {
            String courseUrl = "http://localhost:8080/courses/" + course.getId();
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(courseUrl);
            clipboard.setContent(content);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "تم نسخ رابط الكورس!");
            alert.showAndWait();
        });

        HBox.setHgrow(save, Priority.ALWAYS);
        HBox.setHgrow(share, Priority.ALWAYS);
        save.setMaxWidth(Double.MAX_VALUE);
        share.setMaxWidth(Double.MAX_VALUE);
        quick.getChildren().addAll(share, save);


        purchase.getChildren().addAll(price, enroll, payButton, quick);

        VBox facts = new VBox(8);
        facts.getStyleClass().add("info-card");
        facts.setPadding(new Insets(12));
        facts.getChildren().addAll(
                factRow("المدة", durationText(course)),
                factRow("الموقع", or(getString(course, "getLocationDisplay"), "—")),
                factRow("حالة الكورس", or(getString(course, "getStatus"), "—"))
        );

        leftCol.getChildren().addAll(purchase, facts);



        // ضع في الشبكة
        grid.add(leftCol, 0, 0);
        grid.add(rightCol, 1, 0);

        root.getChildren().add(grid);
        return root;
    }




    // إضافة قسم الكويز والشهادات


// ===== إضافة Methods الجديدة =====

    /**
     * إنشاء قسم الكويزات والشهادات
     */
    private VBox createQuizAndCertificatesSection(Course course) {
        VBox section = new VBox(20);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;");

        // عنوان القسم
        Label sectionTitle = new Label("الكويزات والشهادات");
        sectionTitle.getStyleClass().add("course-title");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        sectionTitle.setTextFill(Color.web("#2c3e50"));

        // قسم الكويز
        quizSection = createQuizSection(course);

        // قسم الشهادة
        certificateSection = createCertificateSection(course);

        section.getChildren().addAll(sectionTitle, quizSection, certificateSection);

        // تحميل معلومات الكويز والشهادة
        loadQuizAndCertificateInfo(course);

        return section;
    }

    private boolean isFavorite(Long id) { return id != null && favoriteIds.contains(id); }
    /**
     * إنشاء قسم الكويز
     */
    private VBox createQuizSection(Course course) {
        VBox quizBox = new VBox(15);
        quizBox.setPadding(new Insets(20));
        quizBox.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #3498db; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;");

        // أيقونة ومعلومات الكويز
        HBox quizHeader = new HBox(15);
        quizHeader.setAlignment(Pos.CENTER_LEFT);

        Label quizIcon = new Label("📝");
        quizIcon.setFont(Font.font(24));

        VBox quizInfo = new VBox(5);

        Label quizTitle = new Label("كويز الكورس");
        quizTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        quizTitle.setTextFill(Color.web("#2c3e50"));

        Label quizDescription = new Label("اختبر معلوماتك واحصل على شهادة إتمام");
        quizDescription.setFont(Font.font("Arial", 12));
        quizDescription.setTextFill(Color.web("#7f8c8d"));

        quizInfo.getChildren().addAll(quizTitle, quizDescription);
        quizHeader.getChildren().addAll(quizIcon, quizInfo);

        // زر بدء الكويز
        btnStartQuiz = new Button("ابدأ الكويز");
        btnStartQuiz.getStyleClass().add("primary-button");
        btnStartQuiz.setVisible(false); // مخفي في البداية
        btnStartQuiz.setOnAction(e -> startQuiz(course));

        // معلومات إضافية عن الكويز
        Label quizStatus = new Label("جاري تحميل معلومات الكويز...");
        quizStatus.setFont(Font.font("Arial", 12));
        quizStatus.setTextFill(Color.web("#95a5a6"));

        quizBox.getChildren().addAll(quizHeader, quizStatus, btnStartQuiz);
        return quizBox;
    }

    /**
     * إنشاء قسم الشهادة
     */
    private VBox createCertificateSection(Course course) {
        VBox certBox = new VBox(15);
        certBox.setPadding(new Insets(20));
        certBox.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #27ae60; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;");
        certBox.setVisible(false); // مخفي في البداية

        // أيقونة ومعلومات الشهادة
        HBox certHeader = new HBox(15);
        certHeader.setAlignment(Pos.CENTER_LEFT);

        Label certIcon = new Label("🏆");
        certIcon.setFont(Font.font(24));

        VBox certInfo = new VBox(5);

        Label certTitle = new Label("شهادة الإتمام");
        certTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        certTitle.setTextFill(Color.web("#2c3e50"));

        Label certDescription = new Label("تهانينا! لقد حصلت على شهادة إتمام هذا الكورس");
        certDescription.setFont(Font.font("Arial", 12));
        certDescription.setTextFill(Color.web("#27ae60"));

        certInfo.getChildren().addAll(certTitle, certDescription);
        certHeader.getChildren().addAll(certIcon, certInfo);

        // أزرار الشهادة
        HBox certButtons = new HBox(10);

        btnViewCertificate = new Button("عرض الشهادة");
        btnViewCertificate.getStyleClass().add("success-button");
        btnViewCertificate.setOnAction(e -> viewCertificate(course));

        Button btnDownloadCert = new Button("تحميل PDF");
        btnDownloadCert.getStyleClass().add("secondary-button");
        btnDownloadCert.setOnAction(e -> downloadCertificate(course));

        certButtons.getChildren().addAll(btnViewCertificate, btnDownloadCert);

        certBox.getChildren().addAll(certHeader, certButtons);
        return certBox;
    }

    /**
     * تحميل معلومات الكويز والشهادة
     */
    private void loadQuizAndCertificateInfo(Course course) {
        // تحقق من وجود كويز
        checkForQuiz(course);

        // تحقق من وجود شهادة
        checkForCertificate(course);
    }

    /**
     * التحقق من وجود كويز للكورس
     */
    private void checkForQuiz(Course course) {
        ApiService.getInstance().getQuizByCourseId(course.getId())
                .thenAccept(quiz -> {
                    Platform.runLater(() -> {
                        updateQuizSection(quiz);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        updateQuizSection(null);
                    });
                    return null;
                });
    }

    /**
     * تحديث قسم الكويز بناء على النتائج
     */
    private void updateQuizSection(Quiz quiz) {
        // البحث عن label الحالة في قسم الكويز
        Label statusLabel = findQuizStatusLabel();

        if (quiz != null) {
            btnStartQuiz.setVisible(true);
            if (statusLabel != null) {
                statusLabel.setText(String.format("الكويز متاح - %s | النسبة المطلوبة: %s",
                        quiz.getSummary(),
                        quiz.getPassingScoreDisplay()));
                statusLabel.setTextFill(Color.web("#27ae60"));
            }
        } else {
            btnStartQuiz.setVisible(false);
            if (statusLabel != null) {
                statusLabel.setText("لا يوجد كويز متاح لهذا الكورس حالياً");
                statusLabel.setTextFill(Color.web("#e74c3c"));
            }
        }
    }

    /**
     * التحقق من وجود شهادة للمستخدم
     */
    private void checkForCertificate(Course course) {
        if (!sessionManager.isLoggedIn()) {
            return;
        }

        Long userId = sessionManager.getCurrentUserId();
        ApiService.getInstance().getUserCourseCertificate(userId, course.getId())
                .thenAccept(optionalCert -> {
                    Platform.runLater(() -> {
                        if (optionalCert.isPresent()) {
                            showCertificateSection(optionalCert.get());
                        }
                    });
                })
                .exceptionally(ex -> {
                    // لا توجد شهادة - هذا طبيعي
                    return null;
                });
    }

    /**
     * إظهار قسم الشهادة
     */
    private void showCertificateSection(CertificateDTO certificate) {
        certificateSection.setVisible(true);

        // تحديث معلومات الشهادة
        VBox certInfo = (VBox) ((HBox) certificateSection.getChildren().get(0)).getChildren().get(1);
        Label certDescription = (Label) certInfo.getChildren().get(1);

        certDescription.setText(String.format("رقم الشهادة: %s | تاريخ الإصدار: %s",
                certificate.getCertificateNumber(),
                String.format(String.valueOf(DateTimeFormatter.ofPattern(String.valueOf(DateTimeFormatter.ofPattern("dd/MM/yyyy")))))));
    }

    /**
     * بدء الكويز
     */
    private void startQuiz(Course course) {
        if (!sessionManager.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("تسجيل الدخول مطلوب");
            alert.setHeaderText("يرجى تسجيل الدخول أولاً");
            alert.setContentText("تحتاج إلى تسجيل الدخول لحل الكويز والحصول على الشهادة.");
            alert.showAndWait();

            mainController.navigateToLogin();
            return;
        }

        // بدء الكويز عبر MainController
        mainController.openQuizPage(course.getId());
    }

    /**
     * عرض الشهادة
     */
    private void viewCertificate(Course course) {
        mainController.openCertificatesPage();
    }

    /**
     * تحميل الشهادة
     */
    private void downloadCertificate(Course course) {
        if (!sessionManager.isLoggedIn()) {
            return;
        }

        Long userId = sessionManager.getCurrentUserId();
        ApiService.getInstance().getUserCourseCertificate(userId, course.getId())
                .thenAccept(optionalCert -> {
                    if (optionalCert.isPresent()) {
                        String userName = sessionManager.getCurrentUser().getName();
                        ApiService.getInstance().downloadCertificatePdf(optionalCert.get().getId(), userName)
                                .thenAccept(filePath -> {
                                    Platform.runLater(() -> {
                                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("تحميل الشهادة");
                                        alert.setHeaderText("تم تحميل الشهادة بنجاح");
                                        alert.setContentText("تم حفظ الشهادة في: " + filePath);
                                        alert.showAndWait();
                                    });
                                })
                                .exceptionally(ex -> {
                                    Platform.runLater(() -> {
                                        Alert alert = new Alert(Alert.AlertType.ERROR);
                                        alert.setTitle("خطأ في التحميل");
                                        alert.setHeaderText("فشل تحميل الشهادة");
                                        alert.setContentText(ex.getMessage());
                                        alert.showAndWait();
                                    });
                                    return null;
                                });
                    }
                });
    }

    // Helper methods
    private Label findQuizStatusLabel() {
        try {
            VBox quizBox = quizSection;
            return (Label) quizBox.getChildren().get(1); // الـ status label
        } catch (Exception e) {
            return null;
        }
    }






    private void loadLessons(Course course, Long enrollmentId) {
        lessonsListBox.getChildren().clear();

        ApiService.getInstance().getLessonsForCourse(course.getId())
                .thenAccept(lessons -> {
                    Platform.runLater(() -> {
                        int index = 0;
                        for (Map<String, Object> map : lessons) {
                            Lesson lesson = new Lesson();
                            lesson.setTitle((String) map.get("title"));
                            lesson.setVideoUrl((String) map.get("videoUrl"));
                            lesson.setId(((Number) map.get("id")).longValue());

                            lessonsListBox.getChildren().add(createLessonCard(lesson, enrollmentId, index));
                            index++;
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Label error = new Label("فشل تحميل الدروس ❌");
                        lessonsListBox.getChildren().add(error);
                    });
                    return null;
                });
    }




    private HBox createLessonCard(Lesson lesson, Long enrollmentId, int index) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);

        card.setMinHeight(60);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(300); // تأكد من وجود مساحة كافية

        Label titleLabel = new Label(lesson.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusIcon = new Label();
        statusIcon.setStyle("-fx-font-size: 18px;");
        statusIcon.setPadding(new Insets(0, 10, 0, 10)); // مساحة حول العلامة

        if (index == 0) {
            card.setStyle("-fx-background-color: #a9d18e; -fx-background-radius: 8;");
            statusIcon.setText("▶");
            statusIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");
            titleLabel.setTextFill(Color.WHITE);

            card.setOnMouseClicked(e -> {
                openVideo(lesson.getVideoUrl());
                if (enrollmentId != null) {
                    ApiService.getInstance().markLessonAsCompleted(enrollmentId, lesson.getId());
                }
            });

        } else {
            card.setStyle("-fx-background-color: #fff8dc; -fx-border-color: #ffcc00; -fx-border-width: 1; -fx-background-radius: 8;");
            titleLabel.setTextFill(Color.BLACK);
            statusIcon.setText("🔒");

            Long userId = sessionManager.getCurrentUserId();
            ApiService.getInstance().checkLessonAccess(lesson.getId(), userId)
                    .thenAccept(result -> {
                        Platform.runLater(() -> {
                            if (result.isAccessGranted()) {
                                card.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 8;");
                                titleLabel.setTextFill(Color.WHITE);
                                statusIcon.setText("▶");
                                statusIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

                                card.setOnMouseClicked(e -> {
                                    openVideo(lesson.getVideoUrl());
                                    ApiService.getInstance().markLessonAsCompleted(enrollmentId, lesson.getId());
                                });

                            } else {
                                statusIcon.setText("🔒");
                                statusIcon.setStyle("-fx-font-size: 18px;");
                                card.setOnMouseClicked(e -> {
                                    Alert alert = new Alert(Alert.AlertType.WARNING);
                                    alert.setHeaderText("🚫 يجب التسجيل والدفع للوصول لهذا الكورس");
                                    alert.showAndWait();
                                });
                            }
                        });
                    });
        }

        card.getChildren().addAll(titleLabel, spacer, statusIcon);
        return card;
    }










    private void openVideo(String url) {
        if (url == null || url.isBlank()) {
            mainController.showError("رابط الفيديو غير صالح.");
            return;
        }

        Platform.runLater(() -> {
            if (url.contains("youtube.com") || url.contains("youtu.be")) {
                // YouTube link
                String videoId = extractYoutubeVideoId(url);
                if (videoId == null) {
                    mainController.showError("تعذر استخراج معرف الفيديو من رابط YouTube.");
                    return;
                }

                Stage stage = new Stage();
                stage.setTitle("تشغيل الفيديو - YouTube");

                WebView webView = new WebView();
                WebEngine webEngine = webView.getEngine();

                String content = buildYoutubeEmbedHtml(videoId);
                webEngine.loadContent(content);

                Scene scene = new Scene(webView, 800, 480);
                stage.setScene(scene);
                stage.show();
            } else {
                // أي رابط فيديو عادي (mp4، m3u8، رابط مباشر...)
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    mainController.showError("تعذر فتح رابط الفيديو في المتصفح.");
                }
            }
        });
    }

    private String buildYoutubeEmbedHtml(String videoId) {
        return """
        <html>
        <head>
            <style>
                body, html {
                    margin: 0;
                    padding: 0;
                    height: 100%%;
                    background-color: black;
                }
                iframe {
                    width: 100%%;
                    height: 100%%;
                    border: none;
                }
            </style>
        </head>
        <body>
            <iframe 
                src="https://www.youtube.com/embed/%s?autoplay=1&rel=0&modestbranding=1&controls=1" 
                allow="autoplay; encrypted-media" 
                allowfullscreen>
            </iframe>
        </body>
        </html>
        """.formatted(videoId);
    }


    // استخراج معرف الفيديو من رابط YouTube
    private String extractYoutubeVideoId(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return null;

            if (host.contains("youtu.be")) {
                return uri.getPath().substring(1);  // بعد /
            } else if (host.contains("youtube.com")) {
                String query = uri.getQuery();
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("v=")) {
                            return param.substring(2);
                        }
                    }
                }

                // ممكن يكون الرابط بصيغة /embed/{id}
                String path = uri.getPath();
                if (path.startsWith("/embed/")) {
                    return path.substring(7);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }





    /* =============================== Helpers: UI blocks =============================== */

    private Node metaItem(String icon, String text) {
        HBox box = new HBox(6);
        Label i = new Label(icon);
        Label t = new Label(text);
        i.getStyleClass().add("meta-icon");
        t.getStyleClass().add("meta-text");
        box.getChildren().addAll(i, t);
        return box;
    }

    private VBox sectionCard(String title, String bodyText) {
        VBox card = new VBox(10);
        card.getStyleClass().add("section-card");
        card.setPadding(new Insets(16));
        Label t = new Label(title);
        t.getStyleClass().add("section-title");
        Label b = new Label(bodyText);
        b.setWrapText(true);
        b.getStyleClass().add("section-body");
        card.getChildren().addAll(t, b);
        return card;
    }

    private VBox listSection(String title, List<String> items) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");
        card.setPadding(new Insets(16));
        Label t = new Label(title);
        t.getStyleClass().add("section-title");

        VBox list = new VBox(6);
        for (String s : items) {
            HBox row = new HBox(8);
            Label dot = new Label("•");
            dot.getStyleClass().add("bullet");
            Label txt = new Label(s);
            txt.setWrapText(true);
            txt.getStyleClass().add("section-body");
            row.getChildren().addAll(dot, txt);
            list.getChildren().add(row);
        }
        card.getChildren().addAll(t, list);
        return card;
    }

    private VBox syllabusSection(String title, List<String> modules) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");

        Label t = new Label(title);
        t.getStyleClass().add("section-title");

        Accordion acc = new Accordion();

        int idx = 1;
        for (String m : modules) {
            VBox content = new VBox();
            content.getStyleClass().add("sylla-content");

            Label l = new Label(m);
            l.setWrapText(true);
            content.getChildren().add(l);

            TitledPane tp = new TitledPane("الوحدة " + idx++, content);
            tp.getStyleClass().add("sylla-titled-pane");

            acc.getPanes().add(tp);
        }
        if (!acc.getPanes().isEmpty()) acc.setExpandedPane(acc.getPanes().get(0));

        card.getChildren().addAll(t, acc);
        return card;
    }




    private HBox factRow(String key, String val) {
        HBox row = new HBox(8);
        Label k = new Label(key + ":");
        k.getStyleClass().add("fact-key");
        Label v = new Label(val);
        v.getStyleClass().add("fact-val");
        row.getChildren().addAll(k, v);
        return row;
    }

    /* =============================== Text builders (safe) =============================== */

    private String priceText(Course c) {
        if (c == null) return "—";
        Boolean free = getBoolean(c, "getIsFree");
        if (Boolean.TRUE.equals(free)) return "مجاني";
        String pDisp = getString(c, "getPriceDisplay");
        if (pDisp != null && !pDisp.isBlank()) return pDisp;
        Number pNum = getNumber(c, "getPrice");
        return pNum != null ? pNum + " ريال" : "—";
    }

    private String ratingText(Course c) {
        Number r = getNumber(c, "getRating");
        Number cnt = getNumber(c, "getRatingCount");
        if (r == null && cnt == null) return "—";
        String rr = r != null ? String.format(Locale.US, "%.1f", r.doubleValue()) : "—";
        String cc = cnt != null ? "(" + cnt.intValue() + " تقييم)" : "";
        return rr + " " + cc;
    }

    private String durationText(Course c) {
        Number h = getNumber(c, "getDuration");
        return h != null ? (h.intValue() + " ساعة") : "—";
    }


    private List<String> learningList(Course c) {
        List<String> out = getStringList(c, "getLearningOutcomes");
        if (out == null || out.isEmpty()) {
            out = new ArrayList<>();
            out.add("مهارات عملية مطلوبة في سوق العمل.");
            out.add("مشروع تطبيقي لبناء أعمال قوية.");
            out.add("أساسيات ثم مفاهيم متقدمة بشكل تدريجي.");
        }
        return out;
    }

    private List<String> requirementsList(Course c) {
        List<String> out = getStringList(c, "getRequirements");
        if (out == null || out.isEmpty()) {
            out = new ArrayList<>();
            out.add("جهاز كمبيوتر واتصال إنترنت مستقر.");
            out.add("الرغبة في التعلم والممارسة.");
        }
        return out;
    }

    private List<String> syllabusList(Course c) {
        String syl = getString(c, "getSyllabus");
        List<String> out = new ArrayList<>();
        if (syl != null && !syl.isBlank()) {
            String[] lines = syl.split("\\r?\\n");
            for (String ln : lines) {
                String s = ln.trim();
                if (!s.isBlank()) out.add(s);
            }
        }
        if (out.isEmpty()) {
            out.add("مقدمة وأساسيات الموضوع.");
            out.add("الأدوات والمتطلبات المطلوبة.");
            out.add("التطبيق العملي والأمثلة.");
        }
        return out;
    }

    /* =============================== Images =============================== */

    private void loadCourseImage(ImageView imageView, String imageUrl) {
        String normalized = normalizeUrl(imageUrl);
        if (normalized == null) {
            setDefaultCourseImage(imageView);
            return;
        }
        Image cached = imageCache.get(normalized);
        if (cached != null) { imageView.setImage(cached); return; }

        try {
            Image img = new Image(normalized, 1280, 720, false, true, true);
            imageView.setImage(img);
            img.errorProperty().addListener((o, w, e) -> { if (e) setDefaultCourseImage(imageView); });
            img.progressProperty().addListener((o, a, b) -> {
                if (b != null && b.doubleValue() >= 1.0 && !img.isError()) imageCache.putIfAbsent(normalized, img);
            });
        } catch (Exception ex) {
            setDefaultCourseImage(imageView);
        }
    }

    private String normalizeUrl(String raw) {
        if (raw == null) return null;
        String candidate = raw.trim();
        if (candidate.isEmpty()) return null;
        candidate = candidate.replace('\\', '/');
        if (candidate.startsWith("//")) candidate = "https:" + candidate;

        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
            HttpUrl base = HttpUrl.parse(API_BASE);
            if (base == null) return null;
            HttpUrl resolved = base.resolve(candidate.startsWith("/") ? candidate : "/" + candidate);
            return resolved != null ? resolved.toString() : null;
        }
        HttpUrl url = HttpUrl.parse(candidate);
        if (url == null) return null;
        return url.newBuilder().build().toString();
    }

    private void setDefaultCourseImage(ImageView iv) {
        try {
            Image fallback = new Image(getClass().getResourceAsStream("/images/default-course.png"));
            if (fallback != null && !fallback.isError()) {
                iv.setImage(fallback);
                return;
            }
        } catch (Exception ignore) {}
        iv.setImage(null);
    }

    /* =============================== Reflection helpers =============================== */

    private String or(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private Method findMethod(Object obj, String name) {
        if (obj == null || name == null) return null;
        try {
            return obj.getClass().getMethod(name);
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(Object obj, String... methodNames) {
        for (String name : methodNames) {
            Method m = findMethod(obj, name);
            if (m != null) {
                try {
                    Object v = m.invoke(obj);
                    if (v != null) return String.valueOf(v);
                } catch (Exception ignore) {}
            }
        }
        return null;
    }

    private Boolean getBoolean(Object obj, String methodName) {
        Method m = findMethod(obj, methodName);
        if (m == null) return null;
        try {
            Object v = m.invoke(obj);
            return (v instanceof Boolean) ? (Boolean) v : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Number getNumber(Object obj, String methodName) {
        Method m = findMethod(obj, methodName);
        if (m == null) return null;
        try {
            Object v = m.invoke(obj);
            return (v instanceof Number) ? (Number) v : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Object obj, String methodName) {
        Method m = findMethod(obj, methodName);
        if (m == null) return null;
        try {
            Object v = m.invoke(obj);
            if (v instanceof List<?>) {
                List<?> raw = (List<?>) v;
                List<String> out = new ArrayList<>(raw.size());
                for (Object o : raw) if (o != null) out.add(String.valueOf(o));
                return out;
            }
        } catch (Exception ignore) {}
        return null;
    }


    private boolean checkIfUserIsEnrolled(Long courseId, Long userId) {
        // تنادي خدمة الـ API لمعرفة إذا هذا المستخدم مسجل في الكورس
        try {
            return ApiService.getInstance().isUserEnrolled(courseId, userId)
                    .join(); // أو await أو thenAccept حسب كيف تستخدم futures
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean checkIfUserHasPaid(Long userId, Long courseId) {
        try {
            return ApiService.getInstance().hasUserPaid(userId, courseId).get();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


    }

    // دالة موحدة للتحديث بعد التسجيل/الدفع
    private void refreshUIAfterEnrollment() {
        if (currentCourse == null) return; // لو ما فيه كورس محدد

        // نجيب أحدث Enrollment من السيرفر
        ApiService.getInstance().getEnrollmentDetails(
                sessionManager.getCurrentUserId(),
                currentCourse.getId()
        ).thenAccept(enrollment -> {
            Platform.runLater(() -> {
                if (enrollment != null) {
                    this.enrollmentId = enrollment.getId();
                    if (lessonsListBox != null) {
                        loadLessons(currentCourse, enrollmentId); // تحديث قائمة الدروس
                    }
                }

                // تحديث الصفحة نفسها (زر الاشتراك، العنوان .. إلخ)
                mainController.loadCourseDetailsPage(currentCourse);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                System.out.println("فشل تحديث بيانات enrollment: " + ex.getMessage());
            });
            return null;
        });
    }

    @FXML
    private void startQuiz() {
        if (currentQuiz == null) return;

        Long userId = SessionManager.getInstance().getCurrentUserId();

        ApiService.getInstance().startQuizAttempt(currentQuiz.getId(), userId)
                .thenAccept(attempt -> {
                    currentAttemptId = attempt.getId();
                    currentIndex = 0;
                    Platform.runLater(this::showQuestion);
                });
    }


    private void showQuestion() {
        quizContent.getChildren().clear();

        if (currentIndex >= currentQuiz.getQuestions().size()) {
            // خلص الكويز
            ApiService.getInstance().completeQuizAttempt(currentAttemptId)
                    .thenAccept(result -> Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setHeaderText("✅ النتيجة");
                        alert.setContentText("درجتك: " + result.get("score") + "/" + result.get("total"));
                        alert.showAndWait();
                    }));
            return;
        }

        var question = currentQuiz.getQuestions().get(currentIndex);

        Label qLabel = new Label((currentIndex+1) + ". " + question.getQuestionText());
        ToggleGroup group = new ToggleGroup();
        VBox optionsBox = new VBox(8);

        for (var option : question.getOptions()) {
            RadioButton rb = new RadioButton(option.getOptionText());
            rb.setUserData(option.getId());
            rb.setToggleGroup(group);
            optionsBox.getChildren().add(rb);
        }

        Button nextBtn = new Button(
                currentIndex == currentQuiz.getQuestions().size()-1 ? "إنهاء" : "التالي"
        );
        nextBtn.getStyleClass().add("quiz-button");

        nextBtn.setOnAction(e -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            if (selected != null) {
                Long selectedOptionId = (Long) selected.getUserData();
                ApiService.getInstance()
                        .submitAnswer(currentAttemptId, question.getId(), selectedOptionId)
                        .thenAccept(updated -> Platform.runLater(() -> {
                            currentIndex++;
                            showQuestion();
                        }));
            }
        });

        quizContent.getChildren().addAll(qLabel, optionsBox, nextBtn);
    }



    private HBox createQuizCard(Quiz quiz, Long userId) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #e0f7fa; -fx-background-radius: 8;");
        card.setMinHeight(70);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(300);

        VBox quizInfo = new VBox(5);
        Label quizTitle = new Label("📝 " + quiz.getTitle());
        quizTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label description = new Label(quiz.getDescription());
        description.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        quizInfo.getChildren().addAll(quizTitle, description);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button startBtn = new Button("ابدأ الكويز");
        startBtn.getStyleClass().add("quiz-button");

        // ✅ استدعاء showQuizUI مباشرة بدون نافذة جديدة
        startBtn.setOnAction(e -> showQuizUI(quiz.getId(), userId));


        card.getChildren().addAll(quizInfo, spacer, startBtn);
        return card;
    }


    private void startQuizAttemptUI(Long quizId, Long userId) {
        ApiService api = ApiService.getInstance();

        // بدء محاولة الكويز
        api.startQuizAttempt(quizId, userId)
                .thenCombine(api.getQuizByCourseId(quizId), (attempt, quiz) -> Map.of("attempt", attempt, "quiz", quiz))
                .thenAccept(data -> {
                    Platform.runLater(() -> {
                        QuizAttempt attempt = (QuizAttempt) data.get("attempt");
                        Quiz quiz = (Quiz) data.get("quiz");

                        // حفظ بيانات المحاولة
                        currentAttemptId = attempt.getId();
                        questions = quiz.getQuestions();

                        quizContent.getChildren().clear();
                        questionToggleGroups.clear();

                        // عرض كل سؤال
                        for (Question q : questions) {
                            VBox questionBox = new VBox(8);
                            questionBox.setPadding(new Insets(10));
                            questionBox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-color: #f9f9f9; -fx-background-radius: 6;");

                            Label questionLabel = new Label(q.getQuestionText());
                            questionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                            ToggleGroup group = new ToggleGroup();
                            VBox optionsBox = new VBox(5);

                            for (QuestionOption option : q.getOptions()) {
                                RadioButton radio = new RadioButton(option.getOptionText());
                                radio.setToggleGroup(group);
                                radio.setUserData(option.getId());
                                optionsBox.getChildren().add(radio);
                            }

                            questionToggleGroups.add(group);
                            questionBox.getChildren().addAll(questionLabel, optionsBox);
                            quizContent.getChildren().add(questionBox);
                        }

                        // إظهار القسم إذا كان مخفي
                        quizSection.setVisible(true);
                        quizSection.setManaged(true);

                        // إضافة زر الإرسال إذا لم يكن موجودًا
                        if (!quizSection.getChildren().contains(submitButton)) {
                            quizSection.getChildren().add(submitButton);
                        }

                        // ربط الحدث
                        submitButton.setOnAction(e -> onSubmit());

                        statusLabel.setText("يرجى حل الأسئلة ثم الضغط على إرسال.");
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        ex.printStackTrace();
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setHeaderText("فشل تحميل الكويز");
                        alert.setContentText(ex.getCause() != null ? ex.getCause().getMessage() : "حدث خطأ.");
                        alert.showAndWait();
                    });
                    return null;
                });
    }






    private void showQuizResultUI(Long attemptId, Map<String, Object> result) {
        VBox resultBox = new VBox(15);
        resultBox.setPadding(new Insets(20));
        resultBox.setAlignment(Pos.CENTER);

        // نظف المحتوى الحالي وأضف الـ VBox
        quizContent.getChildren().clear();
        quizContent.getChildren().add(resultBox);

        // الحصول على بيانات النتيجة مباشرة من الوسيط result
        Number scoreNumber = (Number) result.getOrDefault("score", 0.0);
        double score = scoreNumber.doubleValue();
        boolean passed = (Boolean) result.getOrDefault("passed", false);

        Label titleLabel = new Label("تم إنهاء الكويز!");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label scoreLabel = new Label(String.format("الدرجة: %.1f%%", score));
        scoreLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label passedLabel = new Label(passed ? "✔ ناجح" : "❌ لم تنجح");
        passedLabel.setStyle(passed ? "-fx-text-fill: green; -fx-font-size: 20px;" : "-fx-text-fill: red; -fx-font-size: 20px;");

        Button closeBtn = new Button("إغلاق");
        closeBtn.setOnAction(e -> {
            // يمكنك إعادة تحميل الأسئلة أو إخفاء القسم هنا
            quizContent.getChildren().clear();
            // مثال: quizSection.setVisible(false);
        });

        resultBox.getChildren().setAll(titleLabel, scoreLabel, passedLabel, closeBtn);
    }


    public VBox getQuizBox() {
        return quizBox;
    }

    public void setQuizBox(VBox quizBox) {
        this.quizBox = quizBox;
    }

    private void showQuizUI(Long courseId, Long userId) {
        quizSectionContainer = new VBox(15);
        quizSectionContainer.setPadding(new Insets(16));
        quizSectionContainer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-radius: 6;");

        quizBox = new VBox(20);

        statusLabel = new Label("📌 سيتم تحميل الأسئلة...");

        statusLabel.setStyle("-fx-text-fill: #555;");

        quizSectionContainer.getChildren().clear();

        quizBox.getChildren().clear();
        quizBox.setPadding(new Insets(20));
        quizBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-width: 1;");
        questionToggleGroups.clear();

        ApiService.getInstance().getQuizByCourseId(courseId).thenAccept(quiz -> {
            Platform.runLater(() -> {
                if (quiz == null || quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
                    statusLabel.setText("لا يوجد كويز متاح لهذه الدورة.");
                    quizSectionContainer.getChildren().add(statusLabel);
                    return;
                }

                this.questions = quiz.getQuestions();
                this.quizId = quiz.getId();

                quizTitleLabel.setText(quiz.getTitle());
                quizTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
                quizBox.getChildren().add(quizTitleLabel);

                quizBox.getChildren().add(statusLabel);

                // زر بدء الكويز
                startQuizButton.setOnAction(e -> {
                    onStartQuiz(userId); // يبدأ الكويز في نفس الصفحة
                });

                quizBox.getChildren().add(startQuizButton);

                // زر الإرسال (يظهر لاحقًا)
                submitButton.setOnAction(e -> onSubmit());
                submitButton.setDisable(true);

                quizBox.getChildren().add(submitButton);

                quizSectionContainer.getChildren().add(quizBox);

                statusLabel.setText("اضغط 'ابدأ الكويز' للمتابعة.");
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> statusLabel.setText("فشل في تحميل الكويز: " + ex.getMessage()));
            return null;
        });
    }

    private void onStartQuiz(Long userId) {
        statusLabel.setText("جاري بدء الكويز...");
        ApiService.getInstance().startQuizAttempt(quizId, userId)
                .thenAccept(attempt -> {
                    currentAttemptId = attempt.getId();
                    Platform.runLater(() -> {
                        statusLabel.setText("تم بدء الكويز، بالتوفيق!");
                        startQuizButton.setDisable(true);
                        submitButton.setDisable(false);
                        displayQuestions(); // عرض الأسئلة
                    });
                }).exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("فشل بدء الكويز: " + ex.getMessage()));
                    return null;
                });
    }

    private void displayQuestions() {
        // نزيل الأسئلة السابقة إن وجدت
        // نحتفظ فقط بعنوان الكويز، الرسالة، وأزرار البدء والإرسال
        quizBox.getChildren().removeIf(node -> node instanceof VBox); // إزالة كل صناديق الأسئلة
        questionToggleGroups.clear();

        for (Question question : questions) {
            VBox questionBox = new VBox(10);
            questionBox.setPadding(new Insets(10));
            questionBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5; -fx-background-color: #ffffff; -fx-background-radius: 5;");

            // نص السؤال
            Label questionLabel = new Label(question.getQuestionText());
            questionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            questionLabel.setWrapText(true);

            // خيارات السؤال
            VBox optionsBox = new VBox(5);
            ToggleGroup group = new ToggleGroup();

            for (QuestionOption option : question.getOptions()) {
                RadioButton optionRadio = new RadioButton(option.getOptionText());
                optionRadio.setToggleGroup(group);
                optionRadio.setUserData(option.getId()); // نستخدم ID الخيار
                optionRadio.setWrapText(true);
                optionsBox.getChildren().add(optionRadio);
            }

            questionToggleGroups.add(group); // نضيف المجموعة إلى القائمة لمراجعة الإجابات لاحقًا

            questionBox.getChildren().addAll(questionLabel, optionsBox);
            quizBox.getChildren().add(questionBox);
        }
    }




    private void onSubmit() {
        if (currentAttemptId == null) {
            statusLabel.setText("يرجى بدء الكويز أولاً.");
            return;
        }

        submitButton.setDisable(true);
        statusLabel.setText("جاري إرسال الإجابات...");

        CompletableFuture<?>[] futures = new CompletableFuture[questions.size()];
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            ToggleGroup group = questionToggleGroups.get(i);
            RadioButton selected = (RadioButton) group.getSelectedToggle();

            if (selected == null) {
                statusLabel.setText("يرجى الإجابة على جميع الأسئلة.");
                submitButton.setDisable(false);
                return;
            }

            Long selectedOptionId = (Long) selected.getUserData();
            futures[i] = ApiService.getInstance().submitAnswer(currentAttemptId, q.getId(), selectedOptionId);
        }

        CompletableFuture.allOf(futures)
                .thenCompose(v -> ApiService.getInstance().completeQuizAttempt(currentAttemptId))
                .thenAccept(result -> {
                    Platform.runLater(() -> {
                        Number scoreNumber = (Number) result.getOrDefault("score", 0.0);
                        boolean passed = (Boolean) result.getOrDefault("passed", false);
                        double score = scoreNumber.doubleValue();

                        statusLabel.setText(String.format("تم إنهاء الكويز! الدرجة: %.1f%% - %s",
                                score, passed ? "✔ ناجح" : "❌ لم تنجح"));
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("فشل إرسال الكويز: " + ex.getMessage());
                        submitButton.setDisable(false);
                    });
                    return null;
                });
    }

    /**
     * إنشاء قسم الدروس مع تتبع التقدم
     */
    private VBox lessonsSection(Course course, Long enrollmentId) {
        this.currentEnrollmentId = enrollmentId;

        VBox box = new VBox(12);
        box.getStyleClass().add("section-card");
        box.setPadding(new Insets(16));

        // Header مع عنوان وشريط التقدم
        VBox headerBox = new VBox(8);

        Label title = new Label("محتوى المسار");
        title.getStyleClass().add("section-title");

        // شريط التقدم
        HBox progressBox = createProgressSection();

        headerBox.getChildren().addAll(title, progressBox);

        lessonsListBox = new VBox(12);

        box.getChildren().addAll(headerBox, lessonsListBox);

        // تحميل الدروس والتقدم
        loadLessonsWithProgress(course, enrollmentId);

        return box;
    }

    /**
     * إنشاء قسم التقدم
     */
    private HBox createProgressSection() {
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        Label progressText = new Label("التقدم:");
        progressText.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        courseProgressBar = new ProgressBar(0);
        courseProgressBar.setPrefWidth(200);
        courseProgressBar.setStyle("-fx-accent: #4CAF50;");

        progressLabel = new Label("0%");
        progressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        progressBox.getChildren().addAll(progressText, courseProgressBar, progressLabel);

        return progressBox;
    }

    /**
     * تحميل الدروس مع تتبع التقدم
     */
    private void loadLessonsWithProgress(Course course, Long enrollmentId) {
        lessonsListBox.getChildren().clear();

        // تحميل الدروس أولاً
        ApiService.getInstance().getLessonsForCourse(course.getId())
                .thenAccept(lessons -> {
                    Platform.runLater(() -> {
                        // تحميل تقدم المستخدم
                        if (enrollmentId != null) {
                            loadUserProgress(enrollmentId, lessons);
                        } else {
                            displayLessonsOnly(lessons, null);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Label error = new Label("فشل تحميل الدروس");
                        lessonsListBox.getChildren().add(error);
                    });
                    return null;
                });
    }

    /**
     * تحميل تقدم المستخدم
     */
    private void loadUserProgress(Long enrollmentId, List<Map<String, Object>> lessons) {
        ApiService.getInstance().getLessonProgressByEnrollmentId(enrollmentId)
                .thenAccept(progressList -> {
                    Platform.runLater(() -> {
                        // حفظ التقدم حسب الكورس
                        courseProgressMap.put(currentCourse.getId(), progressList);
                        courseEnrollmentMap.put(currentCourse.getId(), enrollmentId);

                        updateProgressDisplay(progressList);
                        displayLessonsWithProgress(lessons, progressList);
                    });
                });
    }

    private List<LessonProgress> getCurrentCourseProgress() {
        return courseProgressMap.getOrDefault(currentCourse.getId(), new ArrayList<>());
    }

    /**
     * تحديث عرض التقدم
     */
    private void updateProgressDisplay(List<LessonProgress> progressList) {
        double progressPercentage = ApiService.getInstance().calculateProgressPercentage(userProgress);

        Platform.runLater(() -> {
            courseProgressBar.setProgress(progressPercentage / 100.0);
            progressLabel.setText(String.format("%.0f%%", progressPercentage));

            // تغيير اللون حسب النسبة
            if (progressPercentage == 100) {
                progressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #4CAF50;");
                courseProgressBar.setStyle("-fx-accent: #4CAF50;");
            } else if (progressPercentage >= 50) {
                progressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #FF9800;");
                courseProgressBar.setStyle("-fx-accent: #FF9800;");
            } else {
                progressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #F44336;");
                courseProgressBar.setStyle("-fx-accent: #F44336;");
            }
        });
    }

    /**
     * عرض الدروس مع التقدم
     */
    private void displayLessonsWithProgress(List<Map<String, Object>> lessons, List<LessonProgress> progressList) {
        lessonsListBox.getChildren().clear();

        // إنشاء خريطة للوصول السريع للتقدم
        Map<Long, LessonProgress> progressMap = new HashMap<>();
        if (progressList != null) {
            for (LessonProgress progress : progressList) {
                progressMap.put(progress.getId(), progress);
            }
        }

        int index = 0;
        for (Map<String, Object> map : lessons) {
            Lesson lesson = new Lesson();
            lesson.setTitle((String) map.get("title"));
            lesson.setVideoUrl((String) map.get("videoUrl"));
            lesson.setId(((Number) map.get("id")).longValue());

            LessonProgress progress = progressMap.get(lesson.getId());

            HBox lessonCard = createEnhancedLessonCard(lesson, progress, index);
            lessonsListBox.getChildren().add(lessonCard);
            index++;
        }
    }

    /**
     * عرض الدروس بدون تقدم
     */
    private void displayLessonsOnly(List<Map<String, Object>> lessons, Object unused) {
        lessonsListBox.getChildren().clear();

        int index = 0;
        for (Map<String, Object> map : lessons) {
            Lesson lesson = new Lesson();
            lesson.setTitle((String) map.get("title"));
            lesson.setVideoUrl((String) map.get("videoUrl"));
            lesson.setId(((Number) map.get("id")).longValue());

            HBox lessonCard = createBasicLessonCard(lesson, index);
            lessonsListBox.getChildren().add(lessonCard);
            index++;
        }
    }

    /**
     * إنشاء كارت درس محسن مع التقدم
     */
    private HBox createEnhancedLessonCard(Lesson lesson, LessonProgress progress, int index) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(60);

        Label titleLabel = new Label(lesson.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusIcon = new Label();
        statusIcon.setStyle("-fx-font-size: 18px;");
        statusIcon.setPadding(new Insets(0, 10, 0, 10));

        // تحديد حالة الدرس
        boolean isCompleted = progress != null && progress.isCompleted();
        boolean isAccessible = index == 0 || isUserEnrolledAndPaid() || hasCompletedPreviousLesson(index);

        if (isCompleted) {
            // درس مكتمل
            card.setStyle("-fx-background-color: #E8F5E8; -fx-background-radius: 8; -fx-border-color: #4CAF50; -fx-border-width: 1;");
            statusIcon.setText("✓");
            statusIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            titleLabel.setTextFill(Color.web("#2E7D32"));

            card.setOnMouseClicked(e -> {
                openVideo(lesson.getVideoUrl());
                // لا نحتاج تسجيل مرة أخرى
            });

        } else if (isAccessible) {
            // درس متاح للمشاهدة
            card.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 8; -fx-border-color: #2196F3; -fx-border-width: 1;");
            statusIcon.setText("▶");
            statusIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: #1976D2;");
            titleLabel.setTextFill(Color.web("#1565C0"));

            card.setOnMouseClicked(e -> {
                openVideo(lesson.getVideoUrl());

                // تسجيل الدرس كمكتمل
                if (currentEnrollmentId != null) {
                    markLessonCompleted(lesson.getId());
                }
            });

        } else {
            // درس مقفل
            card.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 8; -fx-border-color: #FF9800; -fx-border-width: 1;");
            statusIcon.setText("🔒");
            titleLabel.setTextFill(Color.web("#E65100"));

            card.setOnMouseClicked(e -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("درس مقفل");
                alert.setContentText("يجب إكمال الدرس السابق أو الاشتراك في الكورس للوصول لهذا الدرس.");
                alert.showAndWait();
            });
        }

        // إضافة مؤشر التقدم للدرس الحالي
        if (isAccessible && !isCompleted) {
            ProgressIndicator lessonProgress = new ProgressIndicator(-1);
            lessonProgress.setPrefSize(20, 20);
            lessonProgress.setVisible(false);
            card.getChildren().add(lessonProgress);
        }

        card.getChildren().addAll(titleLabel, spacer, statusIcon);

        // إضافة Tooltip
        Tooltip tooltip = new Tooltip();
        if (isCompleted) {
            tooltip.setText("تم إكمال هذا الدرس");
        } else if (isAccessible) {
            tooltip.setText("اضغط للمشاهدة");
        } else {
            tooltip.setText("مقفل - يجب إكمال الدروس السابقة");
        }
        Tooltip.install(card, tooltip);

        return card;
    }

    /**
     * إنشاء كارت درس أساسي (بدون تقدم)
     */
    private HBox createBasicLessonCard(Lesson lesson, int index) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(60);

        Label titleLabel = new Label(lesson.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusIcon = new Label();
        statusIcon.setStyle("-fx-font-size: 18px;");
        statusIcon.setPadding(new Insets(0, 10, 0, 10));

        if (index == 0) {
            // الدرس الأول مجاني دائماً
            card.setStyle("-fx-background-color: #E8F5E8; -fx-background-radius: 8;");
            statusIcon.setText("▶");
            statusIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: #4CAF50;");
            titleLabel.setTextFill(Color.web("#2E7D32"));

            card.setOnMouseClicked(e -> openVideo(lesson.getVideoUrl()));

        } else {
            // باقي الدروس مقفلة
            card.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 8;");
            statusIcon.setText("🔒");
            titleLabel.setTextFill(Color.web("#C62828"));

            card.setOnMouseClicked(e -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("يجب الاشتراك في الكورس");
                alert.setContentText("للوصول لهذا الدرس، يرجى التسجيل في الكورس أولاً.");
                alert.showAndWait();
            });
        }

        card.getChildren().addAll(titleLabel, spacer, statusIcon);
        return card;
    }

    /**
     * تسجيل درس كمكتمل
     */
    private void markLessonCompleted(Long lessonId) {
        ApiService.getInstance().markLessonAsCompleted(currentEnrollmentId, lessonId)
                .thenAccept(v -> {
                    // إعادة تحميل التقدم
                    refreshProgressAfterCompletion();
                })
                .exceptionally(ex -> {
                    System.err.println("فشل في تسجيل الدرس كمكتمل: " + ex.getMessage());
                    return null;
                });
    }

    /**
     * تحديث التقدم بعد إكمال درس
     */
    private void refreshProgressAfterCompletion() {
        if (currentEnrollmentId != null) {
            ApiService.getInstance().getLessonProgressByEnrollmentId(currentEnrollmentId)
                    .thenAccept(progressList -> {
                        Platform.runLater(() -> {
                            this.userProgress = progressList;
                            updateProgressDisplay(progressList);

                            // تحديث عرض الدروس
                            ApiService.getInstance().getLessonsForCourse(currentCourse.getId())
                                    .thenAccept(lessons -> {
                                        Platform.runLater(() -> {
                                            displayLessonsWithProgress(lessons, progressList);
                                        });
                                    });
                        });
                    });
        }
    }

    /**
     * فحص إذا كان المستخدم مسجل ودفع
     */
    private boolean isUserEnrolledAndPaid() {
        // يجب التحقق من حالة التسجيل والدفع
        return currentEnrollmentId != null;
    }

    /**
     * فحص إذا تم إكمال الدرس السابق
     */
    private boolean hasCompletedPreviousLesson(int currentIndex) {
        if (currentIndex == 0) return true; // الدرس الأول

        // البحث عن الدرس السابق في قائمة التقدم
        for (LessonProgress progress : userProgress) {
            // نحتاج للتأكد من أن هذا الدرس السابق مكتمل
            // هذا يتطلب معرفة ترتيب الدروس
        }

        return false; // مؤقتاً
    }

}

