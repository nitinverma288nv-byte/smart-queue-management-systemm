package com.smartqueue.app.controller;

import com.smartqueue.app.entity.*;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/export")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class ExportController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BankRepository bankRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AdminService adminService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String val = value.replace("\"", "\"\"");
        if (val.contains(",") || val.contains("\n") || val.contains("\"")) {
            return "\"" + val + "\"";
        }
        return val;
    }

    private ResponseEntity<String> buildCsvResponse(String csvContent, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }

    @GetMapping("/users")
    public ResponseEntity<String> exportUsers() {
        List<User> users = userRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("User ID,Username,Email,Full Name,Role,Created At\n");
        for (User u : users) {
            sb.append(u.getId()).append(",")
              .append(escapeCsv(u.getUsername())).append(",")
              .append(escapeCsv(u.getEmail())).append(",")
              .append(escapeCsv(u.getFullName())).append(",")
              .append(u.getRole().name()).append(",")
              .append(u.getCreatedAt() != null ? u.getCreatedAt().format(formatter) : "").append("\n");
        }
        return buildCsvResponse(sb.toString(), "users_export.csv");
    }

    @GetMapping("/appointments")
    public ResponseEntity<String> exportAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Appointment ID,User ID,Username,Sector Type,Reference ID,Service Name,Status,Created At\n");
        for (Appointment a : appointments) {
            sb.append(a.getId()).append(",")
              .append(a.getUser().getId()).append(",")
              .append(escapeCsv(a.getUser().getUsername())).append(",")
              .append(a.getSectorType()).append(",")
              .append(a.getReferenceId()).append(",")
              .append(escapeCsv(a.getServiceName())).append(",")
              .append(a.getStatus()).append(",")
              .append(a.getCreatedAt() != null ? a.getCreatedAt().format(formatter) : "").append("\n");
        }
        return buildCsvResponse(sb.toString(), "appointments_export.csv");
    }

    @GetMapping("/tokens")
    public ResponseEntity<String> exportTokens() {
        List<Token> tokens = tokenRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Token ID,Token Number,User ID,Username,Sector Type,Reference ID,Service Name,Priority,Status,Created At,Updated At\n");
        for (Token t : tokens) {
            sb.append(t.getId()).append(",")
              .append(escapeCsv(t.getTokenNumber())).append(",")
              .append(t.getUser().getId()).append(",")
              .append(escapeCsv(t.getUser().getUsername())).append(",")
              .append(t.getSectorType()).append(",")
              .append(t.getReferenceId()).append(",")
              .append(escapeCsv(t.getServiceName())).append(",")
              .append(t.getPriority()).append(",")
              .append(t.getStatus()).append(",")
              .append(t.getCreatedAt() != null ? t.getCreatedAt().format(formatter) : "").append(",")
              .append(t.getUpdatedAt() != null ? t.getUpdatedAt().format(formatter) : "").append("\n");
        }
        return buildCsvResponse(sb.toString(), "tokens_export.csv");
    }

    @GetMapping("/queues")
    public ResponseEntity<String> exportQueues() {
        List<Queue> queues = queueRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Queue ID,Sector Type,Reference ID,Last Token Number,Created At,Updated At\n");
        for (Queue q : queues) {
            sb.append(q.getId()).append(",")
              .append(q.getSectorType()).append(",")
              .append(q.getReferenceId()).append(",")
              .append(q.getLastTokenNumber()).append(",")
              .append(q.getCreatedAt() != null ? q.getCreatedAt().format(formatter) : "").append(",")
              .append(q.getUpdatedAt() != null ? q.getUpdatedAt().format(formatter) : "").append("\n");
        }
        return buildCsvResponse(sb.toString(), "queues_export.csv");
    }

    @GetMapping("/hospitals")
    public ResponseEntity<String> exportHospitals() {
        List<Hospital> hospitals = hospitalRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Hospital ID,Name,Logo URL\n");
        for (Hospital h : hospitals) {
            sb.append(h.getId()).append(",")
              .append(escapeCsv(h.getName())).append(",")
              .append(escapeCsv(h.getLogoUrl())).append("\n");
        }
        return buildCsvResponse(sb.toString(), "hospitals_export.csv");
    }

    @GetMapping("/banks")
    public ResponseEntity<String> exportBanks() {
        List<Bank> banks = bankRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Bank ID,Name,Logo URL\n");
        for (Bank b : banks) {
            sb.append(b.getId()).append(",")
              .append(escapeCsv(b.getName())).append(",")
              .append(escapeCsv(b.getLogoUrl())).append("\n");
        }
        return buildCsvResponse(sb.toString(), "banks_export.csv");
    }

    @GetMapping("/colleges")
    public ResponseEntity<String> exportColleges() {
        List<College> colleges = collegeRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("College ID,Name,Logo URL\n");
        for (College c : colleges) {
            sb.append(c.getId()).append(",")
              .append(escapeCsv(c.getName())).append(",")
              .append(escapeCsv(c.getLogoUrl())).append("\n");
        }
        return buildCsvResponse(sb.toString(), "colleges_export.csv");
    }

    @GetMapping("/notifications")
    public ResponseEntity<String> exportNotifications() {
        List<Notification> notifications = notificationRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Notification ID,User ID,Username,Message,Is Read,Created At\n");
        for (Notification n : notifications) {
            sb.append(n.getId()).append(",")
              .append(n.getUser().getId()).append(",")
              .append(escapeCsv(n.getUser().getUsername())).append(",")
              .append(escapeCsv(n.getMessage())).append(",")
              .append(n.getIsRead()).append(",")
              .append(n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : "").append("\n");
        }
        return buildCsvResponse(sb.toString(), "notifications_export.csv");
    }

    @GetMapping("/staff")
    public ResponseEntity<String> exportStaff() {
        List<Staff> staffList = staffRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Staff ID,User ID,Username,Full Name,Sector Type,Reference ID,Counter ID,Counter Name\n");
        for (Staff s : staffList) {
            sb.append(s.getId()).append(",")
              .append(s.getUser().getId()).append(",")
              .append(escapeCsv(s.getUser().getUsername())).append(",")
              .append(escapeCsv(s.getUser().getFullName())).append(",")
              .append(s.getSectorType()).append(",")
              .append(s.getReferenceId()).append(",")
              .append(s.getCounter() != null ? s.getCounter().getId() : "").append(",")
              .append(s.getCounter() != null ? escapeCsv(s.getCounter().getName()) : "").append("\n");
        }
        return buildCsvResponse(sb.toString(), "staff_export.csv");
    }

    @GetMapping("/analytics")
    public ResponseEntity<String> exportAnalytics() {
        Map<String, Object> stats = adminService.getDashboardStats();
        StringBuilder sb = new StringBuilder();
        sb.append("Metric Label,Value\n");
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            sb.append(escapeCsv(entry.getKey())).append(",")
              .append(escapeCsv(String.valueOf(entry.getValue()))).append("\n");
        }
        return buildCsvResponse(sb.toString(), "analytics_summary_export.csv");
    }

    @GetMapping("/payments")
    public ResponseEntity<String> exportPayments() {
        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtDesc();
        StringBuilder sb = new StringBuilder();
        sb.append("Payment ID,User ID,Username,Sector Type,Amount,Payment Method,Payment Status,Transaction ID,Created At\n");
        for (Payment p : payments) {
            sb.append(p.getId()).append(",")
              .append(p.getUser().getId()).append(",")
              .append(escapeCsv(p.getUser().getUsername())).append(",")
              .append(p.getSectorType()).append(",")
              .append(p.getAmount()).append(",")
              .append(p.getPaymentMethod()).append(",")
              .append(p.getPaymentStatus()).append(",")
              .append(escapeCsv(p.getTransactionId())).append(",")
              .append(p.getCreatedAt() != null ? p.getCreatedAt().format(formatter) : "").append("\n");
        }
        return buildCsvResponse(sb.toString(), "payments_export.csv");
    }
}
