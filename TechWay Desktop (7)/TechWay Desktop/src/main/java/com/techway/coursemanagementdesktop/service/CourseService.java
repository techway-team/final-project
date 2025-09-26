package com.techway.coursemanagementdesktop.service;


import com.techway.coursemanagementdesktop.exception.CourseNotFoundException;
import com.techway.coursemanagementdesktop.model.Course;

import java.util.List;
import java.util.Optional;

public interface CourseService {

    // جلب جميع الكورسات
    List<Course> getAllCourses();

    // جلب كورس معين بالـ ID
    Optional<Course> getCourseById(Long id);

    // إضافة كورس جديد
    Course createCourse(Course course);

    // تحديث كورس موجود
    Course updateCourse(Long id, Course course) throws CourseNotFoundException;

    // حذف كورس
    void deleteCourse(Long id) throws CourseNotFoundException;

    // البحث في الكورسات
    List<Course> searchCourses(String keyword);

    // فلترة حسب الموقع
    List<Course> getCoursesByLocation(String location);

    // فلترة حسب نوع الكورس (مجاني/مدفوع)
    List<Course> getCoursesByType(Boolean isFree);

    // البحث والفلترة معاً
    List<Course> searchAndFilterCourses(String keyword, String location, Boolean isFree);
}