package com.courseapp.coursesystem.controller;


import com.courseapp.coursesystem.entity.Lesson;
import com.courseapp.coursesystem.service.LessonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lessons")
@CrossOrigin(origins = "*")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lesson> updateLesson(@PathVariable Long id, @RequestBody Lesson updatedLesson) {
        Lesson saved = lessonService.updateLesson(id, updatedLesson);
        return ResponseEntity.ok(saved);
    }

    // ✅ API يتحقق من صلاحية المستخدم ثم يرجع الدرس
    @GetMapping("/{lessonId}/user/{userId}")
    public ResponseEntity<Lesson> getLessonWithAccess(
            @PathVariable Long lessonId,
            @PathVariable Long userId
    ) {
        Lesson lesson = lessonService.getAccessibleLesson(lessonId, userId);
        return ResponseEntity.ok(lesson);
    }


    
}
