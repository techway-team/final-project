package com.courseapp.coursesystem.service;

import com.courseapp.coursesystem.entity.Enrollment;
import com.courseapp.coursesystem.entity.Lesson;
import com.courseapp.coursesystem.entity.LessonProgress;
import com.courseapp.coursesystem.repository.EnrollmentRepository;
import com.courseapp.coursesystem.repository.LessonProgressRepository;
import com.courseapp.coursesystem.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LessonProgressService {

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    // تهيئة تقدم الدروس عند التسجيل في كورس جديد
    @Transactional
    public void initializeLessonProgressForEnrollment(Enrollment enrollment) {
        List<Lesson> lessons = lessonRepository.findByCourse(enrollment.getCourse());
        List<LessonProgress> progresses = lessons.stream()
                .map(lesson -> {
                    LessonProgress lp = new LessonProgress(enrollment, lesson);
                    lp.setCompleted(false);
                    return lp;
                })
                .collect(Collectors.toList());
        lessonProgressRepository.saveAll(progresses);
    }

    // تحديث تقدم درس محدد
    @Transactional
    public void markLessonAsCompleted(Long enrollmentId, Long lessonId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        LessonProgress progress = lessonProgressRepository.findByEnrollmentAndLesson(enrollment, lesson)
                .orElseThrow(() -> new RuntimeException("LessonProgress not found"));

        if (!progress.isCompleted()) {
            progress.setCompleted(true);
            // لا داعي هنا ل save()
            // JPA سيقوم بحفظ التغيير تلقائياً عند انتهاء الدالة (عند commit)
            updateEnrollmentProgress(enrollment);
        }
    }


    // تحديث نسبة التقدم وحالة التسجيل
    @Transactional
    public void updateEnrollmentProgress(Enrollment enrollment) {
        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollment(enrollment);

        long completedCount = progresses.stream().filter(LessonProgress::isCompleted).count();
        long totalLessons = progresses.size();

        double progressValue = totalLessons == 0 ? 0 : ((double) completedCount / totalLessons) * 100;

        enrollment.setProgress(BigDecimal.valueOf(progressValue));
        if (progressValue >= 100.0) {
            enrollment.setStatus("COMPLETED");
        } else {
            enrollment.setStatus("IN_PROGRESS");
        }

        enrollmentRepository.save(enrollment);
    }

    public List<LessonProgress> getLessonProgressByEnrollmentId(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        return lessonProgressRepository.findByEnrollment(enrollment);
    }

}
