package com.courseapp.coursesystem.service;

import com.courseapp.coursesystem.entity.*;
import com.courseapp.coursesystem.exception.CertificateAlreadyExistsException;
import com.courseapp.coursesystem.exception.ValidationException;
import com.courseapp.coursesystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

// PDFBox 3.x
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

@Service
@Transactional
public class CertificateService {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    // توليد شهادة تلقائياً
    public Certificate generateCertificate(Long userId, Long courseId, Double finalScore, Double quizScore) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ValidationException("Course not found"));

        if (certificateRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new CertificateAlreadyExistsException("Certificate already exists for this user and course");
        }


        String certificateNumber = Certificate.generateCertificateNumber(userId, courseId);

        Certificate certificate = new Certificate(user, course, certificateNumber, finalScore, quizScore);
        certificate.setCompletionDate(LocalDateTime.now());
        certificate.setIssuedAt(LocalDateTime.now());

        return certificateRepository.save(certificate);
    }

    // جلب شهادة المستخدم للكورس
    public Optional<Certificate> getUserCourseCertificate(Long userId, Long courseId) {
        return certificateRepository.findByUserIdAndCourseId(userId, courseId);
    }

    // جلب شهادات المستخدم
    public List<Certificate> getUserCertificates(Long userId) {
        return certificateRepository.findByUserIdOrderByIssuedAtDesc(userId);
    }

    // جلب شهادة برقمها
    public Optional<Certificate> getCertificateByNumber(String certificateNumber) {
        return certificateRepository.findByCertificateNumber(certificateNumber);
    }

    // التحقق من صحة الشهادة
    public boolean verifyCertificate(String certificateNumber) {
        Optional<Certificate> certificate = certificateRepository.findByCertificateNumber(certificateNumber);
        return certificate.isPresent() && certificate.get().isValid();
    }


    // إلغاء شهادة
    public void revokeCertificate(Long certificateId, String reason) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ValidationException("Certificate not found"));
        certificate.setStatus(CertificateStatus.REVOKED);
        certificate.setMetadata("Revoked: " + reason + " at " + LocalDateTime.now());
        certificateRepository.save(certificate);
    }

    public byte[] generateCertificatePDF(Long certificateId) {
        Certificate c = certificateRepository.findByIdWithUserAndCourse(certificateId)
                .orElseThrow(() -> new ValidationException("Certificate not found"));
        if (!c.isValid()) throw new ValidationException("Certificate is not valid");

        String studentName = c.getUser() != null ? c.getUser().getName() : "Student";
        String courseTitle = c.getCourse() != null ? c.getCourse().getTitle() : "Course";
        double finalScore = c.getFinalScore() != null ? c.getFinalScore() : 0.0;
        double quizScore  = c.getQuizScore() != null ? c.getQuizScore() : 0.0;
        String certNum    = c.getCertificateNumber() != null ? c.getCertificateNumber() : String.valueOf(c.getId());
        LocalDateTime when = c.getCompletionDate() != null ? c.getCompletionDate() : LocalDateTime.now();
        String dateStr = when.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // تحميل الخط
            PDType0Font font;
            try (InputStream is = getClass().getResourceAsStream("/fonts/NotoNaskhArabic-VariableFont_wght.ttf")) {
                if (is == null) throw new IllegalStateException("Font not found");
                font = PDType0Font.load(doc, is, true);
            }

            // الصفحة أفقية
            PDPage page = new PDPage(PDRectangle.LETTER);
            page.setRotation(90);
            doc.addPage(page);

            float pageWidth  = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // 1. الخلفية البيضاء
                cs.setNonStrokingColor(Color.WHITE);
                cs.addRect(0, 0, pageWidth, pageHeight);
                cs.fill();

                // 2. الأشكال الهندسية الجانبية - اليسار
                // مربع كبير موف غامق
                cs.setNonStrokingColor(Color.decode("#6B46C1"));
                cs.addRect(0, 0, 120, 200);
                cs.fill();

                // مربع متوسط موف فاتح
                cs.setNonStrokingColor(Color.decode("#A78BFA"));
                cs.addRect(80, 150, 80, 120);
                cs.fill();

                // إطار وردي فاتح
                cs.setStrokingColor(Color.decode("#F3E8FF"));
                cs.setLineWidth(3);
                cs.addRect(60, 130, 100, 100);
                cs.stroke();

                // 3. الأشكال الهندسية الجانبية - اليمين
                // مربع كبير موف غامق
                cs.setNonStrokingColor(Color.decode("#6B46C1"));
                cs.addRect(pageWidth - 120, pageHeight - 200, 120, 200);
                cs.fill();

                // مربع متوسط موف فاتح
                cs.setNonStrokingColor(Color.decode("#A78BFA"));
                cs.addRect(pageWidth - 160, pageHeight - 270, 80, 120);
                cs.fill();

                // إطار وردي فاتح
                cs.setStrokingColor(Color.decode("#F3E8FF"));
                cs.setLineWidth(3);
                cs.addRect(pageWidth - 160, pageHeight - 230, 100, 100);
                cs.stroke();

                // 4. الدائرة في أعلى اليسار
                cs.setNonStrokingColor(Color.decode("#6B46C1"));
                // رسم دائرة باستخدام مربع (تقريبي)
                for (int i = 0; i < 360; i += 10) {
                    double x = 80 + 25 * Math.cos(Math.toRadians(i));
                    double y = pageHeight - 50 + 25 * Math.sin(Math.toRadians(i));
                    cs.addRect((float)x, (float)y, 2, 2);
                }
                cs.fill();

                // النص داخل الدائرة - techway
                cs.setNonStrokingColor(Color.WHITE);
                cs.setFont(font, 10);
                cs.beginText();
                cs.newLineAtOffset(58, pageHeight - 55);
                cs.showText("techway");
                cs.endText();

                // 5. العنوان الرئيسي
                cs.setNonStrokingColor(Color.BLACK);
                cs.setFont(font, 36);
                cs.beginText();
                cs.newLineAtOffset(pageWidth/2 - 140, pageHeight - 80);
                cs.showText("CERTIFICATE");
                cs.endText();

                cs.setFont(font, 14);
                cs.beginText();
                cs.newLineAtOffset(pageWidth/2 - 60, pageHeight - 110);
                cs.showText("OF ACHIEVEMENT");
                cs.endText();

                cs.setFont(font, 16);
                cs.beginText();
                cs.newLineAtOffset(pageWidth/2 - 80, pageHeight - 160);
                cs.showText("This certificate awarded to:");
                cs.endText();

                // 7. اسم الطالب بخط مميز
                cs.setNonStrokingColor(Color.decode("#6B46C1"));
                cs.setFont(font, 32);
                cs.beginText();
                cs.newLineAtOffset(pageWidth/2 - (studentName.length() * 8), pageHeight - 210);
                cs.showText(studentName);
                cs.endText();

                // خط تحت الاسم
                cs.setStrokingColor(Color.decode("#6B46C1"));
                cs.setLineWidth(2);
                cs.moveTo(pageWidth/2 - 150, pageHeight - 220);
                cs.lineTo(pageWidth/2 + 150, pageHeight - 220);
                cs.stroke();

                // 8. وصف الدورة
                cs.setNonStrokingColor(Color.BLACK);
                cs.setFont(font, 14);
                cs.beginText();
                cs.newLineAtOffset(pageWidth/2 - 200, pageHeight - 260);
                cs.showText("has successfully completed the course: " + courseTitle);
                cs.endText();

                cs.beginText();
                cs.newLineAtOffset(pageWidth/2 - 200, pageHeight - 280);
                cs.showText("with Final Score: " + String.format("%.1f%%", finalScore) +
                        " and Quiz Score: " + String.format("%.1f%%", quizScore));
                cs.endText();

                // 9. التاريخ في الأسفل اليسار
                cs.setFont(font, 14);
                cs.beginText();
                cs.newLineAtOffset(180, 100);
                cs.showText("Date");
                cs.endText();

                // خط تحت التاريخ
                cs.setStrokingColor(Color.BLACK);
                cs.setLineWidth(1);
                cs.moveTo(180, 80);
                cs.lineTo(280, 80);
                cs.stroke();

                cs.beginText();
                cs.newLineAtOffset(200, 60);
                cs.showText(dateStr);
                cs.endText();

                // 10. التوقيع Techway في الأسفل اليمين
                cs.setNonStrokingColor(Color.decode("#6B46C1"));
                cs.setFont(font, 20);
                cs.beginText();
                cs.newLineAtOffset(pageWidth - 300, 120);
                cs.showText("Techway");
                cs.endText();

                // خط تحت التوقيع
                cs.setStrokingColor(Color.BLACK);
                cs.setLineWidth(1);
                cs.moveTo(pageWidth - 300, 100);
                cs.lineTo(pageWidth - 180, 100);
                cs.stroke();

                cs.setNonStrokingColor(Color.BLACK);
                cs.setFont(font, 12);
                cs.beginText();
                cs.newLineAtOffset(pageWidth - 280, 80);
                cs.showText("Techway Academy");
                cs.endText();

                cs.beginText();
                cs.newLineAtOffset(pageWidth - 280, 65);
                cs.showText("Education Platform");
                cs.endText();

                // 11. رقم الشهادة في الأسفل
                cs.setFont(font, 10);
                cs.beginText();
                cs.newLineAtOffset(pageWidth/2 - 50, 30);
                cs.showText("Certificate Number: " + certNum);
                cs.endText();
            }

            doc.save(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }


    // إحصائيات الشهادات لكورس
    public Object[] getCourseStatistics(Long courseId) {
        return certificateRepository.getCourseStatistics(courseId);
    }

    // حذف شهادات كورس (عند حذف الكورس)
    public void deleteCertificatesByCourse(Long courseId) {
        certificateRepository.deleteByCourseId(courseId);
    }

}

