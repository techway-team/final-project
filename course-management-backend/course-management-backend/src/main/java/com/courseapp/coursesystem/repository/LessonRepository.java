package com.courseapp.coursesystem.repository;

import com.courseapp.coursesystem.entity.Course;
import com.courseapp.coursesystem.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourseId(Long courseId);
    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    List<Lesson> findByCourse(Course course);

    int countByCourse(Course course);

}
