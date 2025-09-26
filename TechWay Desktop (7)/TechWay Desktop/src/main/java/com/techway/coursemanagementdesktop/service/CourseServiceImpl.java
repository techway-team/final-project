package com.techway.coursemanagementdesktop.service;

import com.techway.coursemanagementdesktop.exception.CourseNotFoundException;
import com.techway.coursemanagementdesktop.exception.ValidationException;
import com.techway.coursemanagementdesktop.model.Course;
import com.techway.coursemanagementdesktop.repository.CourseRepository;

import java.util.List;
import java.util.Optional;

public class CourseServiceImpl implements CourseService {

    private CourseRepository courseRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public Optional<Course> getCourseById(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("Course ID must be a positive number");
        }
        return courseRepository.findById(id);
    }

    @Override
    public Course createCourse(Course course) {
        // التحقق من البيانات الأساسية
        validateCourse(course);

        // إذا الكورس مجاني، نخلي السعر = 0
        if (Boolean.TRUE.equals(course.getIsFree())) {
            course.setPrice(java.math.BigDecimal.ZERO);
        }

        // التحقق من السعر
        if (Boolean.FALSE.equals(course.getIsFree()) &&
                (course.getPrice() == null || course.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            throw new ValidationException("Paid courses must have a price greater than 0");
        }

        return courseRepository.save(course);
    }

    @Override
    public Course updateCourse(Long id, Course course) throws CourseNotFoundException {
        if (id == null || id <= 0) {
            throw new ValidationException("Course ID must be a positive number");
        }

        Optional<Course> existingCourse = courseRepository.findById(id);

        if (existingCourse.isEmpty()) {
            throw new CourseNotFoundException(id);
        }

        // التحقق من البيانات الجديدة
        validateCourse(course);

        Course courseToUpdate = existingCourse.get();

        // تحديث البيانات
        courseToUpdate.setTitle(course.getTitle());
        courseToUpdate.setDescription(course.getDescription());
        courseToUpdate.setLocation(course.getLocation());
        courseToUpdate.setDuration(course.getDuration());
        courseToUpdate.setPrice(course.getPrice());
        courseToUpdate.setIsFree(course.getIsFree());
        courseToUpdate.setInstructor(course.getInstructor());
        courseToUpdate.setImageUrl(course.getImageUrl()); // ← الإضافة الجديدة

        // إذا الكورس مجاني، نخلي السعر = 0
        if (Boolean.TRUE.equals(course.getIsFree())) {
            courseToUpdate.setPrice(java.math.BigDecimal.ZERO);
        }

        return courseRepository.save(courseToUpdate);
    }

    @Override
    public void deleteCourse(Long id) throws CourseNotFoundException {
        if (id == null || id <= 0) {
            throw new ValidationException("Course ID must be a positive number");
        }

        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }

        courseRepository.deleteById(id);
    }

    @Override
    public List<Course> searchCourses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new ValidationException("Search keyword cannot be empty");
        }

        return courseRepository.findByTitleContainingIgnoreCase(keyword.trim());
    }

    @Override
    public List<Course> getCoursesByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new ValidationException("Location cannot be empty");
        }

        return courseRepository.findByLocationContainingIgnoreCase(location.trim());
    }

    @Override
    public List<Course> getCoursesByType(Boolean isFree) {
        if (isFree == null) {
            throw new ValidationException("Course type (free/paid) must be specified");
        }

        return courseRepository.findByIsFree(isFree);
    }

    @Override
    public List<Course> searchAndFilterCourses(String keyword, String location, Boolean isFree) {
        // للحين نرجع جميع الكورسات - يمكن تحسينه لاحقاً
        return courseRepository.findAll();
    }

    // method مساعدة للتحقق من صحة البيانات
    private void validateCourse(Course course) {
        if (course == null) {
            throw new ValidationException("Course data is required");
        }

        if (course.getTitle() == null || course.getTitle().trim().isEmpty()) {
            throw new ValidationException("Course title is required");
        }

        if (course.getTitle().length() > 200) {
            throw new ValidationException("Course title cannot exceed 200 characters");
        }

        if (course.getInstructor() == null || course.getInstructor().trim().isEmpty()) {
            throw new ValidationException("Instructor name is required");
        }

        if (course.getInstructor().length() > 100) {
            throw new ValidationException("Instructor name cannot exceed 100 characters");
        }

        if (course.getDuration() != null && course.getDuration() <= 0) {
            throw new ValidationException("Course duration must be greater than 0");
        }

        if (course.getLocation() != null && course.getLocation().length() > 100) {
            throw new ValidationException("Location cannot exceed 100 characters");
        }
    }
}