package com.smartqueue.app.controller;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.Payment;
import com.smartqueue.app.entity.User;
import com.smartqueue.app.repository.PaymentRepository;
import com.smartqueue.app.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<Payment>> initiatePayment(
            @RequestParam Long userId,
            @RequestParam String sectorType,
            @RequestParam Double amount,
            @RequestParam String paymentMethod) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        Payment payment = Payment.builder()
                .user(user)
                .sectorType(sectorType.toUpperCase())
                .amount(amount)
                .paymentMethod(paymentMethod.toUpperCase())
                .paymentStatus("PAID") // Mark paid on success simulation
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        Payment saved = paymentRepository.save(payment);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment successful!", saved));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentHistory(@PathVariable Long userId) {
        List<Payment> history = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment history fetched successfully!", history));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Payment>>> getAllPaymentHistory() {
        List<Payment> history = paymentRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(new ApiResponse<>(true, "All payment history fetched successfully!", history));
    }

    @GetMapping("/receipt/{paymentId}")
    public void downloadReceipt(@PathVariable Long paymentId, HttpServletResponse response) throws IOException {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found!"));

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=receipt_" + payment.getTransactionId() + ".pdf";
        response.setHeader(headerKey, headerValue);

        Document document = new Document(PageSize.A5);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        
        // Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        titleFont.setSize(18);
        Paragraph title = new Paragraph("SMART QUEUE SYSTEM RECEIPT", titleFont);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(title);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph("----------------------------------------------------------------------------"));
        document.add(new Paragraph(" "));

        // Body
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA);
        regularFont.setSize(12);

        document.add(new Paragraph("Transaction ID: " + payment.getTransactionId(), regularFont));
        document.add(new Paragraph("User: " + payment.getUser().getFullName() + " (" + payment.getUser().getUsername() + ")", regularFont));
        document.add(new Paragraph("Sector: " + payment.getSectorType(), regularFont));
        document.add(new Paragraph("Amount Paid: INR " + payment.getAmount(), regularFont));
        document.add(new Paragraph("Method: " + payment.getPaymentMethod(), regularFont));
        document.add(new Paragraph("Status: " + payment.getPaymentStatus(), regularFont));
        document.add(new Paragraph("Date: " + payment.getCreatedAt(), regularFont));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("----------------------------------------------------------------------------"));
        document.add(new Paragraph(" "));
        
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE);
        footerFont.setSize(10);
        Paragraph footer = new Paragraph("Thank you for using the Smart Queue Management System!", footerFont);
        footer.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(footer);

        document.close();
    }
}
