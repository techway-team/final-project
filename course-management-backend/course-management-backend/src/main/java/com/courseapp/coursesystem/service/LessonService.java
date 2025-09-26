package com.courseapp.coursesystem.service;

import com.courseapp.coursesystem.entity.Course;
import com.courseapp.coursesystem.entity.Enrollment;
import com.courseapp.coursesystem.entity.Lesson;
import com.courseapp.coursesystem.exception.UnauthorizedException;
import com.courseapp.coursesystem.repository.EnrollmentRepository;
import com.courseapp.coursesystem.repository.LessonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final  EnrollmentRepository enrollmentRepository;

    public LessonService(LessonRepository lessonRepository, EnrollmentRepository enrollmentRepository) {
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository=enrollmentRepository;
    }

    public List<Lesson> getAllLessons() {
        return lessonRepository.findAll();
    }

    public Optional<Lesson> getLessonById(Long id) {
        return lessonRepository.findById(id);
    }

    public Lesson saveLesson(Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    public void deleteLesson(Long id) {
        lessonRepository.deleteById(id);
    }

    public List<Lesson> getLessonsByCourseId(Long courseId) {
        return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    public int countLessonsByCourse(Course course) {
        return lessonRepository.countByCourse(course);
    }


    // Optional: update lesson if exists
    public Lesson updateLesson(Long id, Lesson updatedLesson) {
        return lessonRepository.findById(id).map(lesson -> {
            lesson.setTitle(updatedLesson.getTitle());
            lesson.setVideoUrl(updatedLesson.getVideoUrl());
            lesson.setOrderIndex(updatedLesson.getOrderIndex());
            lesson.setCourse(updatedLesson.getCourse());
            lesson.setIsPreview(updatedLesson.getIsPreview()); // ✅ هذا السطر مهم
            return lessonRepository.save(lesson);
        }).orElseThrow(() -> new RuntimeException("Lesson not found with id " + id));
    }


    public Lesson getAccessibleLesson(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        Course course = lesson.getCourse();

        // درس مفتوح للجميع أو كورس مجاني
        if (Boolean.TRUE.equals(lesson.getIsPreview()) || Boolean.TRUE.equals(course.getIsFree())) {
            return lesson;
        }

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, course.getId())
                .orElseThrow(() -> new UnauthorizedException("You must enroll to access this lesson"));

        if (Boolean.TRUE.equals(enrollment.getIsPaid())) {
            return lesson;
        }

        throw new UnauthorizedException("You must pay to access this lesson");
    }

}
