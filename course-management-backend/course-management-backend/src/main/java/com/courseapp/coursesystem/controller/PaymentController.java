package com.courseapp.coursesystem.controller;


import com.courseapp.coursesystem.entity.Enrollment;
import com.courseapp.coursesystem.entity.Payment;
import com.courseapp.coursesystem.exception.ApiResponse;
import com.courseapp.coursesystem.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/has-paid")
    public ResponseEntity<Boolean> hasUserPaid(
            @RequestParam Long userId,
            @RequestParam Long courseId) {
        boolean paid = paymentService.hasUserPaid(userId, courseId);
        return ResponseEntity.ok(paid);
    }

    @PostMapping("/make-payment")
    public ResponseEntity<?> makePayment(
            @RequestParam Long userId,
            @RequestParam Long courseId) {
        Payment payment = paymentService.createPayment(userId, courseId);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/markPaid/{enrollmentId}")
    public ResponseEntity<?> markPaid(@PathVariable Long enrollmentId) {
        try {
            Enrollment enrollment = paymentService.markEnrollmentAsPaid(enrollmentId);
            if (enrollment == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Enrollment not found",
                        "data", null
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Marked as paid",
                    "data", enrollment
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "data", null
            ));
        }
    }



}





