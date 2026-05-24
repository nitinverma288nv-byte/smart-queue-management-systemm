package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.entity.Token;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private HospitalBranchRepository hospitalBranchRepository;

    @Autowired
    private BankRepository bankRepository;

    @Autowired
    private BankBranchRepository bankBranchRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 14 Dynamic DB metrics calculation (no static fallbacks, no offsets)
        long totalUsers = userRepository.count();
        long totalHospitals = hospitalRepository.count();
        long totalHospitalBranches = hospitalBranchRepository.count();
        long totalBanks = bankRepository.count();
        long totalBankBranches = bankBranchRepository.count();
        long totalColleges = collegeRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalDoctors = doctorRepository.count();
        long totalCounters = counterRepository.count();
        long totalAppointments = appointmentRepository.count();
        long totalQueues = queueRepository.count();
        long totalTokens = tokenRepository.count();

        // activeQueues: count of distinct queues that are currently active with WAITING or SERVING tokens
        long activeQueues = tokenRepository.findAll().stream()
                .filter(t -> "WAITING".equalsIgnoreCase(t.getStatus()) || "SERVING".equalsIgnoreCase(t.getStatus()))
                .map(t -> t.getSectorType() + "_" + t.getReferenceId())
                .distinct()
                .count();

        // activeStaff: total registered active counter staff operators
        long activeStaff = staffRepository.count();

        stats.put("totalUsers", totalUsers);
        stats.put("totalHospitals", totalHospitals);
        stats.put("totalHospitalBranches", totalHospitalBranches);
        stats.put("totalBanks", totalBanks);
        stats.put("totalBankBranches", totalBankBranches);
        stats.put("totalColleges", totalColleges);
        stats.put("totalDepartments", totalDepartments);
        stats.put("totalDoctors", totalDoctors);
        stats.put("totalCounters", totalCounters);
        stats.put("totalAppointments", totalAppointments);
        stats.put("totalQueues", totalQueues);
        stats.put("totalTokens", totalTokens);
        stats.put("activeQueues", activeQueues);
        stats.put("activeStaff", activeStaff);

        // Core backward compatible values
        stats.put("hospitalCount", totalHospitals);
        stats.put("bankCount", totalBanks);
        stats.put("collegeCount", totalColleges);

        // Category distributions (hospital, bank, college)
        long hospitalTokens = tokenRepository.findAll().stream().filter(t -> "HOSPITAL".equalsIgnoreCase(t.getSectorType())).count();
        long bankTokens = tokenRepository.findAll().stream().filter(t -> "BANK".equalsIgnoreCase(t.getSectorType())).count();
        long collegeTokens = tokenRepository.findAll().stream().filter(t -> "COLLEGE".equalsIgnoreCase(t.getSectorType())).count();

        stats.put("hospitalTokens", hospitalTokens);
        stats.put("bankTokens", bankTokens);
        stats.put("collegeTokens", collegeTokens);

        // Real-time average wait time directly computed from completed tokens without fake fallbacks
        List<Token> completedTokens = tokenRepository.findAll().stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .collect(Collectors.toList());

        long averageWaitTime = 0;
        if (!completedTokens.isEmpty()) {
            long totalMinutes = 0;
            int validCount = 0;
            for (Token t : completedTokens) {
                if (t.getCreatedAt() != null && t.getUpdatedAt() != null) {
                    Duration duration = Duration.between(t.getCreatedAt(), t.getUpdatedAt());
                    totalMinutes += duration.toMinutes();
                    validCount++;
                }
            }
            if (validCount > 0) {
                averageWaitTime = totalMinutes / validCount;
            }
        }
        stats.put("averageWaitTime", averageWaitTime); // strictly dynamic wait time (no fallbacks to 5)

        return stats;
    }
}
