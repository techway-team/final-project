package com.courseapp.coursesystem.repository;

import com.courseapp.coursesystem.entity.Enrollment;
import com.courseapp.coursesystem.entity.User;
import com.courseapp.coursesystem.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUser(User user);
    List<Enrollment> findByUserId(Long userId);
    List<Enrollment> findByCourse(Course course);
    List<Enrollment> findByCourseId(Long courseId);
    boolean existsByUserAndCourse(User user, Course course);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
    List<Enrollment> findByStatus(String status);
    List<Enrollment> findByUserAndStatus(User user, String status);
    long countByCourseId(Long courseId);
    List<Enrollment> findByUserIdAndStatus(Long userId, String status);
    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course WHERE e.user.id = :userId")
    List<Enrollment> findEnrollmentsWithCourse(@Param("userId") Long userId);

    Enrollment findByCourseIdAndUserId(Long courseId, Long userId);
    Optional<Enrollment> findById(Long id);
}