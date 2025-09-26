package com.courseapp.coursesystem.repository;

import com.courseapp.coursesystem.entity.Course;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // للبحث بالموقع
    List<Course> findByLocationContainingIgnoreCase(String location);

    // للبحث بنوع الكورس (مجاني/مدفوع)
    List<Course> findByIsFree(Boolean isFree);

    // للبحث بالعنوان
    List<Course> findByTitleContainingIgnoreCase(String title);

    // للبحث بالمدرب
    List<Course> findByInstructorContainingIgnoreCase(String instructor);

    List<Course> findAllByOrderByCreatedAtDesc(PageRequest pageable);


}