package com.courseapp.coursesystem.service;



import com.courseapp.coursesystem.entity.Course;
import com.courseapp.coursesystem.entity.Enrollment;
import com.courseapp.coursesystem.entity.Payment;
import com.courseapp.coursesystem.entity.User;
import com.courseapp.coursesystem.repository.CourseRepository;
import com.courseapp.coursesystem.repository.EnrollmentRepository;
import com.courseapp.coursesystem.repository.PaymentRepository;
import com.courseapp.coursesystem.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.stripe.model.checkout.Session;


import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;


    public PaymentService(PaymentRepository paymentRepository, EnrollmentRepository enrollmentRepository) {
        this.paymentRepository = paymentRepository;
        this.enrollmentRepository = enrollmentRepository;

    }

    public boolean hasUserPaid(Long userId, Long courseId) {
        return paymentRepository.existsByUserIdAndCourseIdAndPaidTrue(userId, courseId);
    }

    public Payment createPayment(Long userId, Long courseId) {
        // إذا لم يكن هناك دفع سابق، نسوي سجل جديد
        // أو تحديث السجل إذا موجود

        Optional<Payment> existing = paymentRepository.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            Payment p = existing.get();
            p.setPaid(true);
            return paymentRepository.save(p);
        } else {
            Payment p = new Payment();
            p.setUserId(userId);
            p.setCourseId(courseId);
            p.setPaid(true);
            return paymentRepository.save(p);
        }
    }

    public Enrollment markEnrollmentAsPaid(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        enrollment.setPaid(true);
        return enrollmentRepository.save(enrollment); // ← هنا يتحدث is_paid في DB
    }

}

