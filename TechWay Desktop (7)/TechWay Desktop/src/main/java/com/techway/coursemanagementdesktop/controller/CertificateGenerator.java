package com.techway.coursemanagementdesktop.controller;

import com.techway.coursemanagementdesktop.CertificateDTO;
import com.techway.coursemanagementdesktop.model.User;
import com.techway.coursemanagementdesktop.model.Course;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class CertificateGenerator {

    public static File generate(User user, Course course, CertificateDTO cert) throws IOException {
        // إنشاء PDF جديد
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        // استخدام خط عربي (تأكد أن عندك خط TTF مثل Amiri أو Arial Unicode)
        PDFont font = PDType0Font.load(document, new File("fonts/Amiri-Regular.ttf"));

        contentStream.beginText();
        contentStream.setFont(font, 22);
        contentStream.newLineAtOffset(150, 700);
        contentStream.showText("شهادة إتمام");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(font, 16);
        contentStream.newLineAtOffset(100, 630);
        contentStream.showText("تشهد منصة TechWay أن الطالب/ة: " + user.getName());
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(font, 16);
        contentStream.newLineAtOffset(100, 600);
        contentStream.showText("قد أتم بنجاح دورة: " + course.getTitle());
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(font, 14);
        contentStream.newLineAtOffset(100, 570);
        contentStream.showText("تاريخ الإصدار: " + cert.getIssuedAt().format(String.valueOf(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        contentStream.endText();

        contentStream.close();

        // حفظ في سطح المكتب
        String home = System.getProperty("user.home");
        File outputFile = new File(home + "/Desktop/certificate_" + user.getId() + ".pdf");
        document.save(outputFile);
        document.close();

        return outputFile;
    }
}
