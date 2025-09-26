package com.courseapp.coursesystem.controller;

import com.courseapp.coursesystem.CourseAccessStatusDTO;
import com.courseapp.coursesystem.CourseDTO;
import com.courseapp.coursesystem.entity.Course;
import com.courseapp.coursesystem.entity.Enrollment;
import com.courseapp.coursesystem.entity.Lesson;
import com.courseapp.coursesystem.exception.ApiResponse;
import com.courseapp.coursesystem.exception.CourseNotFoundException;
import com.courseapp.coursesystem.repository.CourseRepository;
import com.courseapp.coursesystem.repository.EnrollmentRepository;
import com.courseapp.coursesystem.repository.LessonRepository;
import com.courseapp.coursesystem.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private  LessonRepository lessonRepo;

    @Autowired

    private CourseRepository courseRepository;

    private final EnrollmentRepository enrollmentRepository;

    public CourseController(CourseService courseService, LessonRepository lessonRepo, CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
        this.courseService = courseService;
        this.lessonRepo = lessonRepo;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    // GET /api/courses - جلب جميع الكورسات
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseDTO>>> getAllCourses() {
        List<CourseDTO> dtos = courseService.getAllCourses()
                .stream()
                .map(course -> new CourseDTO(
                        course.getId(),
                        course.getTitle(),
                        course.getDescription(),
                        course.getLocation(),
                        course.getDuration(),
                        course.getPrice(),
                        course.getIsFree(),
                        course.getInstructor(),
                        course.getImageUrl(),
                        course.getCreatedAt(),
                        course.getStatus(),
                        course.getFullAddress(),
                        course.getLatitude(),
                        course.getLongitude()

                ))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(dtos, "Courses retrieved successfully"));
    }
    // GET /api/courses/{id} - جلب كورس معين
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable Long id) {
        Optional<Course> course = courseService.getCourseById(id);

        if (course.isPresent()) {
            ApiResponse<Course> response = new ApiResponse<>(course.get(), "Course found successfully");
            return ResponseEntity.ok(response);
        } else {
            throw new CourseNotFoundException(id);
        }
    }

    // POST /api/courses - إضافة كورس جديد
    @PostMapping
    public ResponseEntity<ApiResponse<Course>> createCourse(@RequestBody Course course) {
        Course createdCourse = courseService.createCourse(course);
        ApiResponse<Course> response = new ApiResponse<>(createdCourse, "Course created successfully");
        return ResponseEntity.ok(response);
    }

    // PUT /api/courses/{id} - تحديث كورس موجود
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        Course updatedCourse = courseService.updateCourse(id, course);
        ApiResponse<Course> response = new ApiResponse<>(updatedCourse, "Course updated successfully");
        return ResponseEntity.ok(response);
    }

    // DELETE /api/courses/{id} - حذف كورس
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        ApiResponse<Void> response = new ApiResponse<>("Course deleted successfully");
        return ResponseEntity.ok(response);
    }

    // GET /api/courses/search - البحث في الكورسات
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Course>>> searchCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean isFree) {

        List<Course> courses;
        String message;

        if (keyword != null && !keyword.trim().isEmpty()) {
            courses = courseService.searchCourses(keyword);
            message = "Search results for: " + keyword;
        } else if (location != null && !location.trim().isEmpty()) {
            courses = courseService.getCoursesByLocation(location);
            message = "Courses in: " + location;
        } else if (isFree != null) {
            courses = courseService.getCoursesByType(isFree);
            message = isFree ? "Free courses" : "Paid courses";
        } else {
            courses = courseService.getAllCourses();
            message = "All courses";
        }

        ApiResponse<List<Course>> response = new ApiResponse<>(courses, message);
        return ResponseEntity.ok(response);
    }

    // GET /api/courses/free - جلب الكورسات المجانية فقط
    @GetMapping("/free")
    public ResponseEntity<ApiResponse<List<Course>>> getFreeCourses() {
        List<Course> freeCourses = courseService.getCoursesByType(true);
        ApiResponse<List<Course>> response = new ApiResponse<>(freeCourses, "Free courses retrieved successfully");
        return ResponseEntity.ok(response);
    }

    // GET /api/courses/paid - جلب الكورسات المدفوعة فقط
    @GetMapping("/paid")
    public ResponseEntity<ApiResponse<List<Course>>> getPaidCourses() {
        List<Course> paidCourses = courseService.getCoursesByType(false);
        ApiResponse<List<Course>> response = new ApiResponse<>(paidCourses, "Paid courses retrieved successfully");
        return ResponseEntity.ok(response);
    }

    // إضافة درس إلى كورس معين
    @PostMapping("/{courseId}/lessons")
    public ResponseEntity<Lesson> addLesson(@PathVariable Long courseId, @RequestBody Lesson lesson) {
        Course course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
        lesson.setCourse(course);
        Lesson saved = lessonRepo.save(lesson);
        return ResponseEntity.ok(saved);
    }
    @PostMapping("/with-lessons")
    public ResponseEntity<Course> createCourseWithLessons(@RequestBody Course course) {
        // الربط بين الدروس والكورس يتم داخل setLessons
        Course savedCourse = courseRepository.save(course);
        return ResponseEntity.ok(savedCourse);
    }

    // ✅ GET /api/courses/{courseId}/lessons - جلب دروس الكورس
    @GetMapping("/{courseId}/lessons")
    public ResponseEntity<List<Lesson>> getLessonsByCourseId(@PathVariable Long courseId) {
        List<Lesson> lessons = lessonRepo.findByCourseIdOrderByOrderIndexAsc(courseId);
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/{courseId}/user/{userId}/status")
    public CourseAccessStatusDTO getCourseStatus(@PathVariable Long courseId, @PathVariable Long userId) {
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndUserId(courseId, userId);

        boolean enrolled = enrollment != null;
        boolean hasPaid = (enrollment != null) && Boolean.TRUE.equals(enrollment.getIsPaid());

        return new CourseAccessStatusDTO(enrolled, hasPaid);
    }
}