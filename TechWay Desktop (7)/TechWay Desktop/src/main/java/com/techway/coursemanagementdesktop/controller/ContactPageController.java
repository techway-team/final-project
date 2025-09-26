package com.techway.coursemanagementdesktop.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class ContactPageController {

    private static final double CONTENT_WIDTH = 1100;
    private final MainController mainController;

    public ContactPageController(MainController mainController) {
        this.mainController = mainController;
    }

    public VBox createContactPage() {
        VBox root = new VBox(28);
        root.setPadding(new Insets(36));
        root.setAlignment(Pos.TOP_CENTER);
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setStyle("-fx-font-family: 'Tajawal Medium';");


        Font font = Font.loadFont(getClass().getResourceAsStream("/fonts/Tajawal-Medium.ttf"), 14);
        System.out.println(font);  // هل يرجع null؟

        // CSS مخصص للاتصال
        try {
            root.getStylesheets().add(
                    getClass().getResource("/css/contact-styles.css").toExternalForm()
            );
        } catch (Exception ignore) {}

        VBox container = new VBox(32);
        container.setMaxWidth(CONTENT_WIDTH);

        // Hero محسن مع أيقونات متحركة
        VBox hero = createEnhancedHero();

        // Grid: يسار (النموذج) — يمين (معلومات التواصل + FAQ + ميزات إضافية)
        GridPane grid = new GridPane();
        grid.setHgap(28);
        grid.setVgap(24);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(55); // النموذج
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(45); // المعلومات
        grid.getColumnConstraints().addAll(c1, c2);

        // النموذج المحسن
        VBox formCard = createEnhancedForm();

        // العمود الأيمن المحسن
        VBox rightCol = createEnhancedRightColumn();

        grid.add(formCard, 0, 0);
        grid.add(rightCol, 1, 0);

        // إضافة قسم الخريطة والإحصائيات
        VBox statsSection = createStatsSection();

        container.getChildren().addAll(hero, grid, statsSection);
        root.getChildren().add(container);
        return root;
    }

    private VBox createEnhancedHero() {
        VBox hero = new VBox(16);
        hero.setAlignment(Pos.TOP_CENTER);

        // أيقونة متحركة
        Label iconLabel = new Label("📞");
        iconLabel.setStyle("-fx-font-size: 48px;");

        // تأثير تحريك بسيط
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.seconds(0), e -> iconLabel.setScaleX(1.0)),
                new KeyFrame(Duration.seconds(0.5), e -> iconLabel.setScaleX(1.1)),
                new KeyFrame(Duration.seconds(1), e -> iconLabel.setScaleX(1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        Label title = new Label("تواصل معنا");
        title.getStyleClass().add("contact-hero-title");
        title.setStyle("-fx-font-family: 'Tajawal-Medium';");

        Label subtitle = new Label("نحن هنا لمساعدتك في رحلتك التعليمية. تواصل معنا وسنرد عليك في أقرب وقت ممكن.");
        subtitle.getStyleClass().add("contact-hero-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(CONTENT_WIDTH - 430);

        // شريط معلومات سريع
        HBox quickInfo = new HBox(24);
        quickInfo.setAlignment(Pos.CENTER);
        quickInfo.getChildren().addAll(
                createQuickInfoItem("⚡", "رد سريع", "خلال 24 ساعة"),
                createQuickInfoItem("💬", "دعم مجاني", "استشارة تعليمية"),
                createQuickInfoItem("🎯", "حلول مخصصة", "لاحتياجاتك")
        );

        hero.getChildren().addAll(iconLabel, title, subtitle, quickInfo);
        return hero;
    }

    private VBox createQuickInfoItem(String icon, String title, String desc) {
        VBox item = new VBox(4);
        item.setAlignment(Pos.CENTER);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111827;");

        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6B7280;");

        item.getChildren().addAll(iconLabel, titleLabel, descLabel);
        return item;
    }

    private VBox createEnhancedForm() {
        VBox formCard = new VBox(20);
        formCard.getStyleClass().add("contact-card");
        formCard.setPadding(new Insets(28));

        // Header محسن للنموذج
        HBox formHeader = new HBox(12);
        formHeader.setAlignment(Pos.TOP_LEFT);

        VBox headerText = new VBox(4);
        Label formTitle = new Label("أرسل لنا رسالة");
        formTitle.getStyleClass().add("section-title");
        headerText.setAlignment(Pos.TOP_LEFT);




        Label formSubtitle = new Label("سنقوم بالرد عليك في أقرب وقت ممكن");
        formSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        headerText.getChildren().addAll(formTitle, formSubtitle);

        Label formIcon = new Label("✉️");
        formIcon.setStyle("-fx-font-size: 24px;");

        formHeader.getChildren().addAll(headerText, formIcon);

        // نموذج محسن
        GridPane formGrid = new GridPane();
        formGrid.setHgap(16);
        formGrid.setVgap(16);
        ColumnConstraints f1 = new ColumnConstraints(); f1.setPercentWidth(50);
        ColumnConstraints f2 = new ColumnConstraints(); f2.setPercentWidth(50);
        formGrid.getColumnConstraints().addAll(f1, f2);

        // الحقول
        TextField nameField = new TextField();
        nameField.setPromptText("الاسم الكامل");
        nameField.getStyleClass().add("input");
        addLabeledField(formGrid, "الاسم *", nameField, 0, 0);

        TextField emailField = new TextField();
        emailField.setPromptText("your@email.com");
        emailField.getStyleClass().add("input");
        addLabeledField(formGrid, "البريد الإلكتروني *", emailField, 1, 0);

        TextField phoneField = new TextField();
        phoneField.setPromptText("رقم الهاتف (اختياري)");
        phoneField.getStyleClass().add("input");
        addLabeledField(formGrid, "رقم الهاتف", phoneField, 0, 1);

        ComboBox<String> subjectCombo = new ComboBox<>();
        subjectCombo.getItems().addAll(
                "استفسار عام",
                "طلب كورس مخصص",
                "دعم فني",
                "اقتراح",
                "شكوى",
                "أخرى"
        );
        subjectCombo.setPromptText("اختر نوع الاستفسار");
        subjectCombo.getStyleClass().add("input");
        addLabeledCombo(formGrid, "نوع الاستفسار *", subjectCombo, 1, 1);

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("اكتب رسالتك هنا... (وضح استفسارك بالتفصيل لنتمكن من مساعدتك بشكل أفضل)");
        messageArea.setPrefRowCount(5);
        messageArea.getStyleClass().add("textarea");
        addLabeledArea(formGrid, "الرسالة *", messageArea, 0, 2, 2);

        // خيارات إضافية
        CheckBox newsletterCheck = new CheckBox("أريد الحصول على النشرة الإخبارية");
        newsletterCheck.setStyle("-fx-font-size: 12px;");

        // أزرار محسنة
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button sendBtn = new Button("إرسال الرسالة 📨");
        sendBtn.getStyleClass().addAll("primary-button");
        sendBtn.setPrefWidth(180);

        Button clearBtn = new Button("مسح الحقول");
        clearBtn.getStyleClass().addAll("secondary-button");
        clearBtn.setPrefWidth(120);

        // Actions
        sendBtn.setOnAction(e -> handleSendMessage(nameField, emailField, phoneField, subjectCombo, messageArea, newsletterCheck));
        clearBtn.setOnAction(e -> clearForm(nameField, emailField, phoneField, subjectCombo, messageArea, newsletterCheck));

        buttonBox.getChildren().addAll(sendBtn, clearBtn);

        formCard.getChildren().addAll(formHeader, formGrid, newsletterCheck, buttonBox);

        // قص محسن
        Rectangle clip = new Rectangle();
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        formCard.setClip(clip);
        formCard.layoutBoundsProperty().addListener((obs, o, n) -> {
            clip.setWidth(n.getWidth());
            clip.setHeight(n.getHeight());
        });

        return formCard;
    }

    private VBox createEnhancedRightColumn() {
        VBox rightCol = new VBox(20);

        // معلومات الاتصال محسنة
        VBox infoCard = new VBox(20);
        infoCard.getStyleClass().add("contact-card");
        infoCard.setPadding(new Insets(28));
        infoCard.setAlignment(Pos.CENTER);



        Label infoTitle = new Label("طرق التواصل");
        infoTitle.getStyleClass().add("section-title");

        VBox contactMethods = new VBox(16);
        contactMethods.setAlignment(Pos.CENTER);

        contactMethods.getChildren().addAll(
                buildEnhancedInfoItem("📧", "البريد الإلكتروني",
                        "info@TechWay.com", "للاستفسارات العامة"),
                buildEnhancedInfoItem("💬", "الدعم الفني",
                        "support@TechWay.com", "للمساعدة التقنية"),
                buildEnhancedInfoItem("📞", "الهاتف",
                        "4567 123 50 966+", "خدمة العملاء"),
                buildEnhancedInfoItem("📱", "واتساب",
                        "7890 456 11 966+", "تواصل سريع"),
                buildEnhancedInfoItem("📍", "العنوان",
                        "الرياض، حي الملك فهد", "المقر الرئيسي"),
                buildEnhancedInfoItem("⏰", "ساعات العمل",
                        "الأحد-الخميس: 9:00-18:00", "توقيت السعودية")
        );

        infoCard.getChildren().addAll(infoTitle, contactMethods);

        // وسائل التواصل الاجتماعي
        VBox socialCard = createSocialMediaSection();

        // FAQ محسن
        VBox faqCard = createEnhancedFAQ();

        rightCol.getChildren().addAll(infoCard, socialCard, faqCard);
        return rightCol;
    }

    private VBox createSocialMediaSection() {
        VBox socialCard = new VBox(16);
        socialCard.getStyleClass().add("contact-card");
        socialCard.setPadding(new Insets(24));

        Label socialTitle = new Label("تابعنا على وسائل التواصل");
        socialTitle.getStyleClass().add("section-title");

        HBox socialButtons = new HBox(12);
        socialButtons.setAlignment(Pos.CENTER);

        Button twitterBtn = createSocialButton("🐦", "تويتر", "#1DA1F2");
        Button linkedinBtn = createSocialButton("💼", "لينكدإن", "#0077B5");
        Button instagramBtn = createSocialButton("📷", "إنستغرام", "#E4405F");
        Button youtubeBtn = createSocialButton("📺", "يوتيوب", "#FF0000");

        socialButtons.getChildren().addAll(twitterBtn, linkedinBtn, instagramBtn, youtubeBtn);

        socialCard.getChildren().addAll(socialTitle, socialButtons);
        return socialCard;
    }

    private Button createSocialButton(String icon, String platform, String color) {
        Button btn = new Button(icon);
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; " +
                        "-fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40; " +
                        "-fx-font-size: 16px; -fx-cursor: hand;", color
        ));
        btn.setTooltip(new Tooltip(platform));
        btn.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION,
                "سيتم توجيهك إلى صفحة " + platform + " الخاصة بنا"));
        return btn;
    }

    private VBox createEnhancedFAQ() {
        VBox faqCard = new VBox(18);
        faqCard.getStyleClass().add("contact-card");
        faqCard.setPadding(new Insets(24));

        Label faqTitle = new Label("أسئلة شائعة");
        faqTitle.getStyleClass().add("section-title");

        Accordion faqAccordion = new Accordion();

        faqAccordion.getPanes().addAll(
                createFaqPane("كم يستغرق الرد على الاستفسارات؟",
                        "نرد على جميع الاستفسارات خلال 24 ساعة كحد أقصى في أيام العمل. للاستفسارات العاجلة يمكنكم التواصل عبر الواتساب."),

                createFaqPane("هل يمكنكم مساعدتي في اختيار الكورسات المناسبة؟",
                        "بالطبع! فريقنا التعليمي يقدم استشارات مجانية لمساعدتك في اختيار المسار التعليمي المناسب لأهدافك ومستواك الحالي."),

                createFaqPane("هل تقدمون دعم فني للطلاب؟",
                        "نعم، نقدم دعم فني شامل لجميع الطلاب المسجلين في كورساتنا عبر البريد الإلكتروني والهاتف."),

                createFaqPane("هل يمكن طلب كورسات مخصصة؟",
                        "نعم، نقدم كورسات مخصصة للشركات والمؤسسات حسب احتياجاتها التدريبية الخاصة.")
        );

        // فتح أول سؤال افتراضياً
        if (!faqAccordion.getPanes().isEmpty()) {
            faqAccordion.setExpandedPane(faqAccordion.getPanes().get(0));
        }

        faqCard.getChildren().addAll(faqTitle, faqAccordion);
        return faqCard;
    }

    private TitledPane createFaqPane(String question, String answer) {
        TitledPane pane = new TitledPane();
        pane.setText(question);

        Label answerLabel = new Label(answer);
        answerLabel.setWrapText(true);
        answerLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-padding: 12 16;" +
                        "-fx-background-color: white;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;"
        );

        pane.setContent(answerLabel);
        pane.setExpanded(false);

        // ستايل العنوان
        pane.setStyle(
                "-fx-background-color: #EDE7F6;" +   // موف فاتح
                        "-fx-border-color: #D1C4E9;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 10 14;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: #111827;"
        );

        // تغيير الخلفية عند التوسيع
        pane.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
            if (isNowExpanded) {
                pane.setStyle(
                        "-fx-background-color: white;" +   // أبيض عند الفتح
                                "-fx-border-color: #7C3AED;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 12;" +
                                "-fx-background-radius: 12;" +
                                "-fx-padding: 10 14;" +
                                "-fx-font-size: 14px;" +
                                "-fx-text-fill: #111827;"
                );
            } else {
                pane.setStyle(
                        "-fx-background-color: #EDE7F6;" + // موف فاتح عند الإغلاق
                                "-fx-border-color: #D1C4E9;" +
                                "-fx-border-radius: 12;" +
                                "-fx-background-radius: 12;" +
                                "-fx-padding: 10 14;" +
                                "-fx-font-size: 14px;" +
                                "-fx-text-fill: #111827;"
                );
            }
        });

        return pane;
    }


    private VBox createStatsSection() {
        VBox statsSection = new VBox(16);
        statsSection.setAlignment(Pos.CENTER);

        Label statsTitle = new Label("نحن في خدمتكم");
        statsTitle.getStyleClass().add("section-title");

        HBox statsBox = new HBox(48);
        statsBox.setAlignment(Pos.CENTER);

        statsBox.getChildren().addAll(
                createStatItem("⚡", "24 ساعة", "متوسط وقت الرد"),
                createStatItem("😊", "98%", "رضا العملاء"),
                createStatItem("🎓", "5000+", "طالب راضي"),
                createStatItem("📚", "50+", "كورس متاح")
        );

        statsSection.getChildren().addAll(statsTitle, statsBox);
        return statsSection;
    }

    private VBox createStatItem(String icon, String number, String label) {
        VBox item = new VBox(8);
        item.setAlignment(Pos.CENTER);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32px;");

        Label numberLabel = new Label(number);
        numberLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #7C3AED;");

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        item.getChildren().addAll(iconLabel, numberLabel, labelText);
        return item;
    }

    private HBox buildEnhancedInfoItem(String emoji, String title, String text, String subtitle) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER);


        VBox content = new VBox(6);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("info-title");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("info-text");
        textLabel.setStyle("-fx-font-weight: bold;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        content.getChildren().addAll(titleLabel, textLabel, subtitleLabel);

        Label icon = new Label(emoji);
        icon.getStyleClass().add("icon-badge");

        row.getChildren().setAll(icon, content);
        return row;
    }

    private void handleSendMessage(TextField nameField, TextField emailField, TextField phoneField,
                                   ComboBox<String> subjectCombo, TextArea messageArea, CheckBox newsletterCheck) {

        // التحقق من الحقول المطلوبة
        if (nameField.getText().trim().isEmpty()) {
            showFieldError(nameField, "يرجى إدخال الاسم");
            return;
        }

        if (emailField.getText().trim().isEmpty() || !isValidEmail(emailField.getText())) {
            showFieldError(emailField, "يرجى إدخال بريد إلكتروني صحيح");
            return;
        }

        if (subjectCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "يرجى اختيار نوع الاستفسار");
            return;
        }

        if (messageArea.getText().trim().isEmpty()) {
            showFieldError(messageArea, "يرجى كتابة رسالتك");
            return;
        }

        // رسالة نجاح محسنة
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("تم إرسال الرسالة بنجاح");
        successAlert.setHeaderText("شكراً لتواصلك معنا!");
        successAlert.setContentText(String.format(
                "تم إرسال رسالتك بنجاح يا %s.\n" +
                        "سنقوم بالرد على بريدك الإلكتروني خلال 24 ساعة.\n" +
                        "رقم المرجع: #%06d",
                nameField.getText(),
                (int)(Math.random() * 999999)
        ));
        successAlert.showAndWait();

        // مسح النموذج
        clearForm(nameField, emailField, phoneField, subjectCombo, messageArea, newsletterCheck);
    }

    private void clearForm(TextField nameField, TextField emailField, TextField phoneField,
                           ComboBox<String> subjectCombo, TextArea messageArea, CheckBox newsletterCheck) {
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        subjectCombo.setValue(null);
        messageArea.clear();
        newsletterCheck.setSelected(false);
    }

    private void showFieldError(Control field, String message) {
        field.setStyle(field.getStyle() + "; -fx-border-color: #EF4444;");

        // إزالة اللون الأحمر بعد 3 ثواني
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            field.setStyle(field.getStyle().replace("; -fx-border-color: #EF4444", ""));
        }));
        timeline.play();

        showAlert(Alert.AlertType.WARNING, message);
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // Helper methods للنموذج
    private void addLabeledField(GridPane grid, String label, TextField field, int col, int row) {
        addLabeledField(grid, label, field, col, row, 1);
    }

    private void addLabeledField(GridPane grid, String label, TextField field, int col, int row, int colspan) {
        VBox box = new VBox(8);
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        box.getChildren().addAll(l, field);
        GridPane.setColumnIndex(box, col);
        GridPane.setRowIndex(box, row);
        GridPane.setColumnSpan(box, colspan);
        grid.getChildren().add(box);
    }

    private void addLabeledCombo(GridPane grid, String label, ComboBox<String> combo, int col, int row) {
        VBox box = new VBox(8);
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        box.getChildren().addAll(l, combo);
        GridPane.setColumnIndex(box, col);
        GridPane.setRowIndex(box, row);
        grid.getChildren().add(box);
    }

    private void addLabeledArea(GridPane grid, String label, TextArea area, int col, int row, int colspan) {
        VBox box = new VBox(8);
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        box.getChildren().addAll(l, area);
        GridPane.setColumnIndex(box, col);
        GridPane.setRowIndex(box, row);
        GridPane.setColumnSpan(box, colspan);
        grid.getChildren().add(box);
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.setTitle("TechWay - اتصل بنا");
        alert.showAndWait();
    }
}