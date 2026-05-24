package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.entity.Counter;
import com.smartqueue.app.entity.Staff;
import com.smartqueue.app.entity.Token;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.StaffRepository;
import com.smartqueue.app.repository.TokenRepository;
import com.smartqueue.app.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StaffServiceImpl implements StaffService {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private com.smartqueue.app.repository.HospitalBranchRepository hospitalBranchRepository;

    @Autowired
    private com.smartqueue.app.repository.BankBranchRepository bankBranchRepository;

    @Autowired
    private com.smartqueue.app.repository.DepartmentRepository departmentRepository;

    @Override
    public Staff getStaffByUserId(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user ID: " + userId));

        if ("HOSPITAL".equalsIgnoreCase(staff.getSectorType())) {
            hospitalBranchRepository.findById(staff.getReferenceId()).ifPresent(branch -> {
                staff.setBranchName(branch.getName());
                if (branch.getHospital() != null) {
                    staff.setOrganizationName(branch.getHospital().getName());
                }
            });
        } else if ("BANK".equalsIgnoreCase(staff.getSectorType())) {
            bankBranchRepository.findById(staff.getReferenceId()).ifPresent(branch -> {
                staff.setBranchName(branch.getName());
                if (branch.getBank() != null) {
                    staff.setOrganizationName(branch.getBank().getName());
                }
            });
        } else if ("COLLEGE".equalsIgnoreCase(staff.getSectorType())) {
            departmentRepository.findById(staff.getReferenceId()).ifPresent(dept -> {
                staff.setBranchName(dept.getName());
                if (dept.getCollege() != null) {
                    staff.setOrganizationName(dept.getCollege().getName());
                }
            });
        }

        return staff;
    }

    @Override
    public Token getCurrentlyServingTokenByUserId(Long userId) {
        Staff staff = getStaffByUserId(userId);
        Counter counter = staff.getCounter();
        if (counter == null) {
            throw new ResourceNotFoundException("Assigned counter not found for staff user ID: " + userId);
        }
        List<Token> servingTokens = tokenRepository.findByCounterIdAndStatus(counter.getId(), "SERVING");
        return servingTokens.isEmpty() ? null : servingTokens.get(0);
    }

    @Override
    public List<Token> getWaitingTokensByUserId(Long userId) {
        Staff staff = getStaffByUserId(userId);
        return tokenRepository.findBySectorTypeAndReferenceIdAndStatus(
                staff.getSectorType().toUpperCase(), staff.getReferenceId(), "WAITING");
    }
}
