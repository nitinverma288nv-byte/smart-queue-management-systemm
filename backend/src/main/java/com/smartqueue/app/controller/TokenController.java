package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.*;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/token")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175", "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178"})
public class TokenController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private com.smartqueue.app.service.EmailService emailService;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private HospitalBranchRepository hospitalBranchRepository;

    @Autowired
    private BankBranchRepository bankBranchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Token>> generateToken(
            @RequestParam Long userId,
            @RequestParam String sectorType,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long counterId,
            @RequestParam(required = false) Long referenceId,
            @RequestParam String serviceName,
            @RequestParam(required = false) Long appointmentId,
            @RequestParam(required = false, defaultValue = "REGULAR") String priority) {
        if ("HOSPITAL".equalsIgnoreCase(sectorType) && referenceId != null) {
            Doctor doctor = doctorRepository.findById(referenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found for legacy hospital token request."));
            branchId = doctor.getBranch().getId();
        } else if (branchId == null) {
            branchId = referenceId;
        }

        if (counterId == null) {
            counterId = inferCounterId(sectorType, branchId, referenceId, serviceName, priority);
        }
        Token token = tokenService.generateToken(userId, sectorType, branchId, counterId, serviceName, appointmentId, priority);
        populateTokenDetails(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token generated successfully!", token));
    }

    private Long inferCounterId(String sectorType, Long branchId, Long referenceId, String serviceName, String priority) {
        sectorType = sectorType.toUpperCase();
        String normalizedService = serviceName == null ? "" : serviceName.toLowerCase();
        if (branchId == null && referenceId != null) {
            branchId = referenceId;
        }

        if (sectorType.equals("HOSPITAL")) {
            if (referenceId != null) {
                Doctor doctor = doctorRepository.findById(referenceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Doctor not found for legacy hospital token request."));
                branchId = doctor.getBranch().getId();
            }
            if ("EMERGENCY".equalsIgnoreCase(priority) || normalizedService.contains("emergency")) {
                return findCounterByType("HOSPITAL", branchId, "Emergency Counter");
            }
            if (normalizedService.contains("bill") || normalizedService.contains("payment") || normalizedService.contains("billing")) {
                return findCounterByType("HOSPITAL", branchId, "Billing Counter");
            }
            return findCounterByType("HOSPITAL", branchId, "OPD Counter");
        }

        if (sectorType.equals("BANK")) {
            if (normalizedService.contains("loan")) {
                return findCounterByType("BANK", branchId, "Loan Desk");
            }
            if (normalizedService.contains("kyc") || normalizedService.contains("card")) {
                return findCounterByType("BANK", branchId, "KYC Desk");
            }
            return findCounterByType("BANK", branchId, "Cash Counter");
        }

        if (sectorType.equals("COLLEGE")) {
            if (normalizedService.contains("scholarship")) {
                return findCounterByType("COLLEGE", branchId, "Scholarship Desk");
            }
            if (normalizedService.contains("admission")) {
                return findCounterByType("COLLEGE", branchId, "Admission Desk");
            }
            return findCounterByType("COLLEGE", branchId, "Fees Counter");
        }

        throw new ResourceNotFoundException("Unable to infer counter for sector type: " + sectorType);
    }

    private Long findCounterByType(String sectorType, Long branchId, String counterType) {
        List<Counter> counters = counterRepository.findByTypeAndReferenceId(sectorType, branchId);
        if (counters.isEmpty()) {
            throw new ResourceNotFoundException("No active counters found for branch " + branchId);
        }
        return counters.stream()
                .filter(counter -> counterType.equalsIgnoreCase(counter.getCounterType()))
                .findFirst()
                .map(Counter::getId)
                .orElse(counters.get(0).getId());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Token>>> getUserTokens(@PathVariable Long userId) {
        List<Token> tokens = tokenService.getTokensByUserId(userId);
        if (tokens != null) {
            tokens.forEach(this::populateTokenDetails);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Tokens fetched successfully!", tokens));
    }

    @PostMapping("/{tokenId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelToken(@PathVariable Long tokenId) {
        tokenService.cancelToken(tokenId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token cancelled successfully!", null));
    }

    @GetMapping("/{tokenId}/position")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTokenPosition(@PathVariable Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found!"));

        Map<String, Object> data = new HashMap<>();
        data.put("tokenNumber", token.getTokenNumber());
        data.put("status", token.getStatus());

        if (!token.getStatus().equals("WAITING")) {
            data.put("peopleAhead", 0);
            data.put("waitingTime", 0);
            data.put("servingToken", token.getStatus().equals("SERVING") ? token.getTokenNumber() : "None");
            return ResponseEntity.ok(new ApiResponse<>(true, "Position fetched successfully!", data));
        }

        // Count people ahead
        List<Token> waitingTokens = tokenRepository.findBySectorTypeAndBranchIdAndCounterIdAndStatus(
                token.getSectorType(), token.getBranchId(), token.getCounterId(), "WAITING");

        // Sort just like callNextToken: EMERGENCY (3) first, then SENIOR (2), then REGULAR (1), then by ID (FIFO)
        List<Token> sortedWaiting = waitingTokens.stream()
                .sorted((t1, t2) -> {
                    java.util.function.Function<String, Integer> getWeight = p -> {
                        if (p == null) return 1;
                        String up = p.toUpperCase();
                        if (up.equals("EMERGENCY")) return 3;
                        if (up.equals("SENIOR") || up.equals("SENIOR_CITIZEN")) return 2;
                        return 1;
                    };
                    int w1 = getWeight.apply(t1.getPriority());
                    int w2 = getWeight.apply(t2.getPriority());
                    if (w1 != w2) {
                        return Integer.compare(w2, w1);
                    }
                    return t1.getId().compareTo(t2.getId());
                })
                .collect(Collectors.toList());

        int position = sortedWaiting.indexOf(token);
        int peopleAhead = Math.max(0, position);

        int serviceTime = token.getSectorType().equals("HOSPITAL") ? 15 : (token.getSectorType().equals("BANK") ? 8 : 10);
        int waitingTime = peopleAhead * serviceTime;

        Queue queue = queueRepository.findBySectorTypeAndBranchIdAndCounterId(token.getSectorType(), token.getBranchId(), token.getCounterId()).orElse(null);
        String servingToken = (queue != null && queue.getCurrentServingToken() != null) ? queue.getCurrentServingToken().getTokenNumber() : "None";

        data.put("peopleAhead", peopleAhead);
        data.put("waitingTime", waitingTime);
        data.put("servingToken", servingToken);

        return ResponseEntity.ok(new ApiResponse<>(true, "Position fetched successfully!", data));
    }

    private void populateTokenDetails(Token token) {
        if (token == null) return;
        
        if ("HOSPITAL".equalsIgnoreCase(token.getSectorType())) {
            if (token.getBranchId() != null) {
                hospitalBranchRepository.findById(token.getBranchId()).ifPresent(branch -> {
                    String hospName = branch.getHospital() != null ? branch.getHospital().getName() : "";
                    token.setHospitalName(hospName + " (" + branch.getName() + ")");
                });
            }
            if (token.getAppointment() != null && token.getAppointment().getReferenceId() != null) {
                doctorRepository.findById(token.getAppointment().getReferenceId()).ifPresent(doctor -> {
                    token.setDoctorName(doctor.getName() + " (" + doctor.getSpecialization() + ")");
                });
            }
        } else if ("BANK".equalsIgnoreCase(token.getSectorType())) {
            if (token.getBranchId() != null) {
                bankBranchRepository.findById(token.getBranchId()).ifPresent(branch -> {
                    String bankName = branch.getBank() != null ? branch.getBank().getName() : "";
                    token.setHospitalName(bankName + " (" + branch.getName() + ")");
                });
            }
        } else if ("COLLEGE".equalsIgnoreCase(token.getSectorType())) {
            if (token.getBranchId() != null) {
                departmentRepository.findById(token.getBranchId()).ifPresent(dept -> {
                    String collegeName = dept.getCollege() != null ? dept.getCollege().getName() : "";
                    token.setHospitalName(collegeName + " (" + dept.getName() + ")");
                });
            }
        }
    }

    @GetMapping("/{tokenId}/receipt")
    public void downloadReceipt(@PathVariable Long tokenId, jakarta.servlet.http.HttpServletResponse response) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found!"));
        populateTokenDetails(token);

        response.setContentType("application/pdf");
        response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=token_receipt_" + token.getTokenNumber() + ".pdf");

        try {
            // A4 page with clean margins
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 36, 36, 36, 36);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());

            document.open();

            // Colors
            java.awt.Color primaryColor = new java.awt.Color(37, 99, 235);    // Royal Blue #2563EB
            java.awt.Color secondaryColor = new java.awt.Color(30, 41, 59);  // Slate Blue #1E293B
            java.awt.Color iceBlue = new java.awt.Color(239, 246, 255);       // Light Ice Blue #EFF6FF
            java.awt.Color lightGray = new java.awt.Color(248, 250, 252);     // Soft gray #F8FAFC
            java.awt.Color darkGray = new java.awt.Color(100, 116, 139);      // Slate text #64748B
            java.awt.Color charcoal = new java.awt.Color(51, 65, 85);         // Body text #334155
            
            // Priority colors
            java.awt.Color priorityColor;
            String priorityStr = token.getPriority() != null ? token.getPriority().toUpperCase() : "REGULAR";
            if ("EMERGENCY".equals(priorityStr)) {
                priorityColor = new java.awt.Color(220, 38, 38); // Red
            } else if ("SENIOR".equals(priorityStr) || "SENIOR_CITIZEN".equals(priorityStr)) {
                priorityColor = new java.awt.Color(217, 119, 6); // Amber
            } else {
                priorityColor = new java.awt.Color(3, 105, 161); // Light Blue
            }

            // Fonts
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
            com.lowagie.text.Font subtitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, new java.awt.Color(191, 219, 254));
            com.lowagie.text.Font sectionTitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, secondaryColor);
            com.lowagie.text.Font bodyFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, charcoal);
            com.lowagie.text.Font bodyBoldFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, secondaryColor);
            com.lowagie.text.Font labelFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, darkGray);
            com.lowagie.text.Font supportEmailFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, primaryColor);

            // Generate structured dynamic Transaction ID
            String transactionId = "TXN-" + String.format("%08X", (token.getId() != null ? token.getId() : 1) * 78619);

            // -----------------------------------------------------------------
            // Main Outer Card Container Table
            // -----------------------------------------------------------------
            com.lowagie.text.pdf.PdfPTable containerTable = new com.lowagie.text.pdf.PdfPTable(1);
            containerTable.setWidthPercentage(100);
            
            com.lowagie.text.pdf.PdfPCell containerCell = new com.lowagie.text.pdf.PdfPCell();
            containerCell.setBorderColor(new java.awt.Color(203, 213, 225)); // slate-300
            containerCell.setBorderWidth(1.5f);
            containerCell.setPadding(20f);
            containerCell.setBackgroundColor(java.awt.Color.WHITE);
            
            // -----------------------------------------------------------------
            // 1. TICKET HEADER BANNER
            // -----------------------------------------------------------------
            com.lowagie.text.pdf.PdfPTable headerBanner = new com.lowagie.text.pdf.PdfPTable(1);
            headerBanner.setWidthPercentage(100);
            
            com.lowagie.text.pdf.PdfPCell bannerCell = new com.lowagie.text.pdf.PdfPCell();
            bannerCell.setBackgroundColor(primaryColor);
            bannerCell.setPadding(12f);
            bannerCell.setBorder(com.lowagie.text.pdf.PdfPCell.NO_BORDER);
            bannerCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            
            com.lowagie.text.Paragraph p1 = new com.lowagie.text.Paragraph("SMART QUEUE MANAGEMENT SYSTEM", titleFont);
            p1.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            bannerCell.addElement(p1);
            
            com.lowagie.text.Paragraph p2 = new com.lowagie.text.Paragraph("OFFICIAL VIRTUAL QUEUE TICKET PASS", subtitleFont);
            p2.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            bannerCell.addElement(p2);
            
            headerBanner.addCell(bannerCell);
            containerCell.addElement(headerBanner);
            
            // Spacing
            com.lowagie.text.Paragraph spacer = new com.lowagie.text.Paragraph("\n");
            spacer.setLeading(8f);
            containerCell.addElement(spacer);

            // -----------------------------------------------------------------
            // 2. CENTRAL TICKET NUMBER BOX
            // -----------------------------------------------------------------
            com.lowagie.text.pdf.PdfPTable ticketBoxTable = new com.lowagie.text.pdf.PdfPTable(1);
            ticketBoxTable.setWidthPercentage(90);
            
            com.lowagie.text.pdf.PdfPCell ticketBoxCell = new com.lowagie.text.pdf.PdfPCell();
            ticketBoxCell.setBackgroundColor(iceBlue);
            ticketBoxCell.setBorderColor(primaryColor);
            ticketBoxCell.setBorderWidth(1.5f);
            ticketBoxCell.setPadding(15f);
            ticketBoxCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            
            com.lowagie.text.Font tLabelFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, darkGray);
            com.lowagie.text.Paragraph tLabel = new com.lowagie.text.Paragraph("YOUR TICKET NUMBER", tLabelFont);
            tLabel.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            ticketBoxCell.addElement(tLabel);
            
            com.lowagie.text.Font tNumFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 36, com.lowagie.text.Font.BOLD, primaryColor);
            com.lowagie.text.Paragraph tNum = new com.lowagie.text.Paragraph(token.getTokenNumber(), tNumFont);
            tNum.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            tNum.setLeading(38f);
            ticketBoxCell.addElement(tNum);
            
            com.lowagie.text.Font pBadgeFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, priorityColor);
            com.lowagie.text.Paragraph pBadge = new com.lowagie.text.Paragraph(priorityStr + " QUEUE ACCESS", pBadgeFont);
            pBadge.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            pBadge.setSpacingBefore(5f);
            ticketBoxCell.addElement(pBadge);
            
            ticketBoxTable.addCell(ticketBoxCell);
            containerCell.addElement(ticketBoxTable);
            containerCell.addElement(spacer);

            // -----------------------------------------------------------------
            // 3. TICKET PARAMETERS GRID
            // -----------------------------------------------------------------
            com.lowagie.text.pdf.PdfPTable gridTable = new com.lowagie.text.pdf.PdfPTable(2);
            gridTable.setWidthPercentage(100);
            gridTable.setWidths(new float[]{1.2f, 2.0f});
            gridTable.setSpacingBefore(10f);
            gridTable.setSpacingAfter(10f);
            
            // Helper lambda-like block to add grid rows
            addGridRow(gridTable, "Transaction ID", transactionId, labelFont, bodyBoldFont, lightGray, false);
            addGridRow(gridTable, "User / Full Name", token.getUser() != null ? token.getUser().getFullName() + " (" + token.getUser().getUsername() + ")" : "Guest User", labelFont, bodyFont, java.awt.Color.WHITE, true);
            addGridRow(gridTable, "Sector / Facility", token.getSectorType(), labelFont, bodyBoldFont, lightGray, true);
            
            String locName = token.getHospitalName() != null ? token.getHospitalName() : "Main Branch";
            addGridRow(gridTable, "Branch Location", locName, labelFont, bodyFont, java.awt.Color.WHITE, true);
            addGridRow(gridTable, "Service Booked", token.getServiceName(), labelFont, bodyBoldFont, lightGray, true);
            
            String specialist = token.getDoctorName() != null ? token.getDoctorName() : "Assigned Service Desk Counter";
            addGridRow(gridTable, "Specialist / Desk", specialist, labelFont, bodyFont, java.awt.Color.WHITE, true);
            
            String slotTiming = "Direct Entry Queue";
            if (token.getAppointment() != null && token.getAppointment().getSlot() != null) {
                Slot slot = token.getAppointment().getSlot();
                slotTiming = slot.getStartTime() + " - " + slot.getEndTime() + " on " + slot.getDate();
            }
            addGridRow(gridTable, "Scheduled Slot", slotTiming, labelFont, bodyBoldFont, lightGray, true);
            
            String issueDate = token.getCreatedAt() != null ? token.getCreatedAt().toString().replace("T", " ").substring(0, 19) : "Live Generated";
            addGridRow(gridTable, "Issued Date & Time", issueDate, labelFont, bodyFont, java.awt.Color.WHITE, true);
            addGridRow(gridTable, "Current Ticket Status", token.getStatus(), labelFont, bodyBoldFont, lightGray, true);
            
            containerCell.addElement(gridTable);

            // -----------------------------------------------------------------
            // 4. DYNAMIC DECORATIVE BARCODE
            // -----------------------------------------------------------------
            com.lowagie.text.pdf.PdfPTable barcodeTable = new com.lowagie.text.pdf.PdfPTable(39);
            barcodeTable.setWidthPercentage(70);
            barcodeTable.setSpacingBefore(12f);
            barcodeTable.setSpacingAfter(10f);
            for (int i = 0; i < 39; i++) {
                com.lowagie.text.pdf.PdfPCell bCell = new com.lowagie.text.pdf.PdfPCell();
                bCell.setFixedHeight(18f);
                bCell.setBorder(com.lowagie.text.pdf.PdfPCell.NO_BORDER);
                if (i % 2 == 0) {
                    bCell.setBackgroundColor(java.awt.Color.BLACK);
                } else {
                    bCell.setBackgroundColor(java.awt.Color.WHITE);
                }
                barcodeTable.addCell(bCell);
            }
            containerCell.addElement(barcodeTable);

            // Tear-off dashed divider line
            com.lowagie.text.Paragraph tearOff = new com.lowagie.text.Paragraph("------------------------------------------ TEAR HERE ------------------------------------------", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL, darkGray));
            tearOff.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            tearOff.setSpacingBefore(5f);
            tearOff.setSpacingAfter(8f);
            containerCell.addElement(tearOff);

            // -----------------------------------------------------------------
            // 5. IMPORTANT INSTRUCTIONS
            // -----------------------------------------------------------------
            com.lowagie.text.Paragraph guideHeader = new com.lowagie.text.Paragraph("IMPORTANT INSTRUCTIONS / DIRECTIONS:", sectionTitleFont);
            guideHeader.setSpacingAfter(5f);
            containerCell.addElement(guideHeader);
            
            com.lowagie.text.Paragraph rule1 = new com.lowagie.text.Paragraph("1. Please present this dynamic ticket pass PDF or your Ticket Number at the counter desk.", bodyFont);
            com.lowagie.text.Paragraph rule2 = new com.lowagie.text.Paragraph("2. Arrive at least 10 minutes prior to your scheduled slot timing to ensure smooth coordination.", bodyFont);
            com.lowagie.text.Paragraph rule3 = new com.lowagie.text.Paragraph("3. Real-time updates and active queue progress can be monitored from your digital account dashboard.", bodyFont);
            containerCell.addElement(rule1);
            containerCell.addElement(rule2);
            containerCell.addElement(rule3);
            
            containerCell.addElement(spacer);

            // -----------------------------------------------------------------
            // 6. PREMIUM IT HELP & SUPPORT EMAIL BLOCK (AS REQUESTED)
            // -----------------------------------------------------------------
            com.lowagie.text.pdf.PdfPTable supportTable = new com.lowagie.text.pdf.PdfPTable(1);
            supportTable.setWidthPercentage(100);
            
            com.lowagie.text.pdf.PdfPCell supportCell = new com.lowagie.text.pdf.PdfPCell();
            supportCell.setBackgroundColor(lightGray);
            supportCell.setBorderColor(new java.awt.Color(226, 232, 240)); // border-slate-200
            supportCell.setBorderWidth(1.0f);
            supportCell.setPadding(10f);
            supportCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            
            com.lowagie.text.Paragraph supText = new com.lowagie.text.Paragraph("Need technical assistance, slot reschedule, or campus assistance?", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, charcoal));
            supText.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            supportCell.addElement(supText);
            
            com.lowagie.text.Paragraph supEmail = new com.lowagie.text.Paragraph("📧 Operations Helpdesk: nitinverma288nv@gmail.com", supportEmailFont);
            supEmail.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            supEmail.setSpacingBefore(2f);
            supportCell.addElement(supEmail);
            
            supportTable.addCell(supportCell);
            containerCell.addElement(supportTable);

            // Add main card cell to container table
            containerTable.addCell(containerCell);
            document.add(containerTable);

            // Footer thanks
            com.lowagie.text.Paragraph thanks = new com.lowagie.text.Paragraph("\nThank you for using the Smart Queue Management System! (No physical stamp or queue slip required)", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.ITALIC, darkGray));
            thanks.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(thanks);

            document.close();
        } catch (Exception e) {
            System.err.println("❌ Error generating professional ticket PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addGridRow(com.lowagie.text.pdf.PdfPTable table, String label, String value, com.lowagie.text.Font labelF, com.lowagie.text.Font valF, java.awt.Color bgColor, boolean drawTopBorder) {
        com.lowagie.text.pdf.PdfPCell cell1 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(label, labelF));
        cell1.setBackgroundColor(bgColor);
        cell1.setPadding(6f);
        cell1.setBorder(com.lowagie.text.pdf.PdfPCell.NO_BORDER);
        if (drawTopBorder) {
            cell1.setBorderColor(new java.awt.Color(241, 245, 249));
            cell1.setBorderWidthTop(1.0f);
        }
        table.addCell(cell1);
        
        com.lowagie.text.pdf.PdfPCell cell2 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(value, valF));
        cell2.setBackgroundColor(bgColor);
        cell2.setPadding(6f);
        cell2.setBorder(com.lowagie.text.pdf.PdfPCell.NO_BORDER);
        if (drawTopBorder) {
            cell2.setBorderColor(new java.awt.Color(241, 245, 249));
            cell2.setBorderWidthTop(1.0f);
        }
        table.addCell(cell2);
    }

    @PostMapping("/{tokenId}/reschedule")
    public ResponseEntity<ApiResponse<Token>> rescheduleToken(
            @PathVariable Long tokenId,
            @RequestParam Long newSlotId) {
        
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found!"));
        
        if (token.getAppointment() == null) {
            throw new com.smartqueue.app.exception.BadRequestException("Direct counter tokens cannot be rescheduled. Only appointments can be rescheduled.");
        }
        
        Appointment appointment = token.getAppointment();
        Slot oldSlot = appointment.getSlot();
        
        Slot newSlot = slotRepository.findById(newSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("New Slot not found!"));
        
        if (newSlot.getBookedTokens() >= newSlot.getMaxTokens()) {
            throw new com.smartqueue.app.exception.BadRequestException("The requested slot is already fully booked!");
        }
        
        // Update old slot
        if (oldSlot.getBookedTokens() > 0) {
            oldSlot.setBookedTokens(oldSlot.getBookedTokens() - 1);
            slotRepository.save(oldSlot);
        }
        
        // Update new slot
        newSlot.setBookedTokens(newSlot.getBookedTokens() + 1);
        slotRepository.save(newSlot);
        
        // Update appointment
        appointment.setSlot(newSlot);
        appointmentRepository.save(appointment);
        
        // Notify user about rescheduling
        notificationRepository.save(Notification.builder()
                .user(token.getUser())
                .message("Your token " + token.getTokenNumber() + " rescheduled successfully to slot: " + newSlot.getStartTime() + " - " + newSlot.getEndTime())
                .isRead(false)
                .build());
        
        if (emailService != null) {
            emailService.sendAppointmentTimingEmail(token.getUser().getEmail(), token.getUser().getFullName(), token.getServiceName(), newSlot.getStartTime() + " - " + newSlot.getEndTime(), token.getSectorType());
        }
        
        populateTokenDetails(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token rescheduled successfully!", token));
    }
}
