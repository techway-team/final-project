package com.techway.coursemanagementdesktop.repository;

import com.techway.coursemanagementdesktop.model.Course;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // للبحث بالموقع
    List<Course> findByLocationContainingIgnoreCase(String location);

    // للبحث بنوع الكورس (مجاني/مدفوع)
    List<Course> findByIsFree(Boolean isFree);

    // للبحث بالعنوان
    List<Course> findByTitleContainingIgnoreCase(String title);

    // للبحث بالمدرب
    List<Course> findByInstructorContainingIgnoreCase(String instructor);



    boolean existsById(Long id);

    void deleteById(Long id);

    List<Course> findAll();
}