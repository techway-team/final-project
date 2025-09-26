package com.courseapp.coursesystem.service;

import com.courseapp.coursesystem.entity.*;
import com.courseapp.coursesystem.repository.*;
import com.courseapp.coursesystem.service.EnrollmentService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Override
    public List<Enrollment> getEnrollmentsByUserId(Long userId) {
        return enrollmentRepository.findByUserId(userId);
    }

    @Override
    public Enrollment enrollUserInCourse(User user, Course course) {
        // يمكنك استدعاء الطريقة الأسهل من هنا أو لا
        return enrollUserInCourse(user.getId(), course.getId());
    }

    @Transactional
    public Enrollment enrollUserInCourse(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setStatus("Active");
        enrollment.setProgress(BigDecimal.ZERO);

        enrollment = enrollmentRepository.save(enrollment);

        // إنشاء سجلات تقدم الدروس
        List<Lesson> lessons = lessonRepository.findByCourse(course);
        for (Lesson lesson : lessons) {
            LessonProgress lp = new LessonProgress(enrollment, lesson);
            lp.setCompleted(false);
            lessonProgressRepository.save(lp);
        }

        return enrollment;
    }

    @Override
    public void deleteEnrollmentById(Long enrollmentId) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new RuntimeException("Enrollment with ID " + enrollmentId + " not found.");
        }
        enrollmentRepository.deleteById(enrollmentId);
    }

    @Transactional
    public void markLessonComplete(Long enrollmentId, Long lessonId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentAndLesson(enrollment, lesson)
                .orElseThrow(() -> new RuntimeException("LessonProgress not found"));

        if (!progress.isCompleted()) {
            progress.setCompleted(true);
            lessonProgressRepository.save(progress);
            updateEnrollmentProgress(enrollment);
        }
    }

    @Transactional
    public void updateEnrollmentProgress(Enrollment enrollment) {
        int completedLessons = (int) lessonProgressRepository.findByEnrollment(enrollment)
                .stream()
                .filter(LessonProgress::isCompleted)
                .count();

        int totalLessons = lessonRepository.countByCourse(enrollment.getCourse());

        BigDecimal percentage = totalLessons == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf((double) completedLessons / totalLessons * 100);

        enrollment.setProgress(percentage);

        if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            enrollment.setStatus("COMPLETED");
        } else {
            enrollment.setStatus("IN_PROGRESS");
        }

        enrollmentRepository.save(enrollment);
    }

    public boolean isUserEnrolled(Long userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    public Enrollment markPaid(Long userId, Long courseId, String paymentReference) {
        Optional<Enrollment> optionalEnrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        Enrollment e = optionalEnrollment.orElse(null);

        if (e == null) {
            // لو وصل تأكيد دفع بدون تسجيل مسبق، ننشئ Enrollment ثم نعلّمه مدفوع
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new EntityNotFoundException("Course not found"));
            e = enrollUserInCourse(user, course);
        }
        if (!e.getIsPaid()) {
            e.setIsPaid(true);
            e.setPaymentDate(LocalDateTime.now());  // لاحظ أنك تستخدم paymentDate وليس paidAt كما في المثال السابق
        }

        if (paymentReference != null && !paymentReference.isBlank()) {
            e.setPaymentReference(paymentReference);
        }
        return enrollmentRepository.save(e);
    }

    public Enrollment findById(Long id) {
        return enrollmentRepository.findById(id)
                .orElse(null);
    }

    public void save(Enrollment enrollment) {
        enrollmentRepository.save(enrollment);
    }

}
