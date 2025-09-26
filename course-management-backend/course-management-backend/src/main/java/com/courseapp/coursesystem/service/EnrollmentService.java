package com.courseapp.coursesystem.service;

import com.courseapp.coursesystem.entity.Course;
import com.courseapp.coursesystem.entity.Enrollment;
import com.courseapp.coursesystem.entity.User;
import com.courseapp.coursesystem.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface  EnrollmentService {

    List<Enrollment> getEnrollmentsByUserId(Long userId);
    Enrollment enrollUserInCourse(User user, Course course);

    Enrollment enrollUserInCourse(Long userId, Long courseId);

    void deleteEnrollmentById(Long enrollmentId);

    void markLessonComplete(Long enrollmentId, Long lessonId);

    boolean isUserEnrolled(Long userId, Long courseId);

    Enrollment markPaid(Long userId, Long courseId, String paymentReference);

    Enrollment findById(Long id);

    void save(Enrollment enrollment);
}
