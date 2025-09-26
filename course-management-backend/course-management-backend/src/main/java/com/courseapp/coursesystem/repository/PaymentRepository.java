package com.courseapp.coursesystem.repository;

import com.courseapp.coursesystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // نستعمل هذا للتأكد إذا المستخدم دفع الكورس
    boolean existsByUserIdAndCourseIdAndPaidTrue(Long userId, Long courseId);

    // ممكن دالة لإحضار الدفع إذا تحتاج بياناته
    Optional<Payment> findByUserIdAndCourseId(Long userId, Long courseId);
}