package com.courseapp.coursesystem.controller;

import com.courseapp.coursesystem.EnrollmentDTO;
import com.courseapp.coursesystem.EnrollmentIdDTO;
import com.courseapp.coursesystem.entity.Enrollment;
import com.courseapp.coursesystem.entity.Course;
import com.courseapp.coursesystem.entity.User;
import com.courseapp.coursesystem.exception.ApiResponse;
import com.courseapp.coursesystem.repository.EnrollmentRepository;
import com.courseapp.coursesystem.service.EnrollmentService;
import com.courseapp.coursesystem.service.CourseService;
import com.courseapp.coursesystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = "*")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private  EnrollmentRepository enrollmentRepository;



    // ================================
    // 1. جلب جميع الكورسات المسجلة لمستخدم
    // ================================
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getUserEnrollments(@PathVariable Long userId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByUserId(userId);

        List<EnrollmentDTO> dtoList = enrollments.stream().map(e ->
                new EnrollmentDTO(
                        e.getId(),
                        e.getUser().getId(),
                        e.getCourse().getId(),
                        e.getCourse().getTitle(),
                        e.getCourse().getImageUrl(),
                        e.getEnrolledAt(),
                        e.getStatus(),
                        e.getProgress(),
                        "COMPLETED".equalsIgnoreCase(e.getStatus())
                )
        ).toList();

        return ResponseEntity.ok(new ApiResponse<>(dtoList, "User enrollments retrieved successfully"));
    }


    // ================================
    // 2. تسجيل مستخدم في كورس
    // ================================
    @PostMapping
    public EnrollmentDTO enrollUser(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        Long courseId = payload.get("courseId");

        Enrollment e = enrollmentService.enrollUserInCourse(userId, courseId);

        return new EnrollmentDTO(
                e.getId(),
                e.getUser().getId(),
                e.getCourse().getId(),
                e.getCourse().getTitle(),
                e.getCourse().getImageUrl(),
                e.getEnrolledAt(),
                e.getStatus(),
                e.getProgress(),
                "COMPLETED".equalsIgnoreCase(e.getStatus())
        );
    }

    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<?> unenrollUser(@PathVariable Long enrollmentId) {
        enrollmentService.deleteEnrollmentById(enrollmentId);
        return ResponseEntity.ok(new ApiResponse<>(null, "تم إلغاء التسجيل بنجاح"));
    }

    @PostMapping("/{enrollmentId}/lessons/{lessonId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeLesson(
            @PathVariable Long enrollmentId,
            @PathVariable Long lessonId
    ) {
        enrollmentService.markLessonComplete(enrollmentId, lessonId);
        return ResponseEntity.ok(new ApiResponse<>(null, "تم تحديث التقدم بنجاح"));
    }


    @GetMapping("/is-enrolled")
    public ResponseEntity<Boolean> isUserEnrolled(
            @RequestParam Long userId,
            @RequestParam Long courseId
    ) {
        boolean enrolled = enrollmentService.isUserEnrolled(userId, courseId);
        return ResponseEntity.ok(enrolled);
    }

    @GetMapping("/user/{userId}/course/{courseId}/id")
    public ResponseEntity<EnrollmentIdDTO> getEnrollmentId(
            @PathVariable Long userId,
            @PathVariable Long courseId) {

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        if (enrollmentOpt.isPresent()) {
            return ResponseEntity.ok(new EnrollmentIdDTO(enrollmentOpt.get().getId()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/mark-paid")
    public ResponseEntity<Enrollment> markPaid(
            @RequestBody Map<String, String> payload) {

        Long userId = Long.valueOf(payload.get("userId"));
        Long courseId = Long.valueOf(payload.get("courseId"));
        String paymentReference = payload.get("paymentReference");

        Enrollment enrollment = enrollmentService.markPaid(userId, courseId, paymentReference);

        return ResponseEntity.ok(enrollment);
    }

    @GetMapping("/user/{userId}/course/{courseId}")
    public ResponseEntity<?> getEnrollmentDetails(@PathVariable Long userId, @PathVariable Long courseId) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);

        if (enrollmentOpt.isPresent()) {
            Enrollment e = enrollmentOpt.get();

            EnrollmentDTO dto = new EnrollmentDTO(
                    e.getId(),
                    e.getUser().getId(),
                    e.getCourse().getId(),
                    e.getCourse().getTitle(),
                    e.getCourse().getImageUrl(),
                    e.getEnrolledAt(),
                    e.getStatus(),
                    e.getProgress(),
                    "COMPLETED".equalsIgnoreCase(e.getStatus())
            );

            return ResponseEntity.ok(dto);
        }  else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Enrollment not found"));
        }
    }

    @PostMapping("/{id}/pay")
    public String payForEnrollment(@PathVariable Long id, Model model) {
        Enrollment enrollment = enrollmentService.findById(id);
        if (enrollment != null) {
            enrollment.setPaid(true); // تحديث حالة الدفع
            enrollmentService.save(enrollment);
            model.addAttribute("message", "تم الدفع بنجاح");
        } else {
            model.addAttribute("message", "لم يتم العثور على التسجيل");
        }
        return "paymentResult"; // صفحة تأكيد الدفع
    }


}
