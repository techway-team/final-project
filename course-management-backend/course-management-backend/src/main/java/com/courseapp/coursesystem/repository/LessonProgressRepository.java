package com.courseapp.coursesystem.repository;

import com.courseapp.coursesystem.entity.Enrollment;
import com.courseapp.coursesystem.entity.Lesson;
import com.courseapp.coursesystem.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByEnrollmentAndLesson(Enrollment enrollment, Lesson lesson);

    List<LessonProgress> findByEnrollment(Enrollment enrollment);
    boolean existsByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);
}
