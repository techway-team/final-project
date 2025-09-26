package com.courseapp.coursesystem.controller;

import com.courseapp.coursesystem.service.LessonProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lesson-progress")
@CrossOrigin(origins = "*")
public class LessonProgressController {

    @Autowired
    private LessonProgressService lessonProgressService;

    @PostMapping("/complete")
    public ResponseEntity<?> markLessonCompleted(@RequestBody Map<String, Long> payload) {
        Long enrollmentId = payload.get("enrollmentId");
        Long lessonId = payload.get("lessonId");

        if (enrollmentId == null || lessonId == null) {
            return ResponseEntity.badRequest().body("enrollmentId and lessonId must be provided");
        }

        lessonProgressService.markLessonAsCompleted(enrollmentId, lessonId);

        return ResponseEntity.ok("تم تحديث حالة الدرس");
    }

    @GetMapping("/by-enrollment/{enrollmentId}")
    public ResponseEntity<?> getLessonProgressByEnrollment(@PathVariable Long enrollmentId) {
        try {
            var progresses = lessonProgressService.getLessonProgressByEnrollmentId(enrollmentId);
            return ResponseEntity.ok(progresses);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("حدث خطأ: " + e.getMessage());
        }
    }


}
