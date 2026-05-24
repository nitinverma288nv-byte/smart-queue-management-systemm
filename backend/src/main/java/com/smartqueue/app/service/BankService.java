package com.smartqueue.app.service;

import com.smartqueue.app.entity.Bank;
import com.smartqueue.app.entity.BankBranch;
import com.smartqueue.app.entity.Counter;
import com.smartqueue.app.entity.Slot;
import com.smartqueue.app.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface BankService {
    List<Bank> getAllBanks();
    List<BankBranch> getBranchesByBank(Long bankId);
    List<Counter> getCountersByBranch(Long branchId);
    List<Slot> getSlotsByCounterAndDate(Long counterId, LocalDate date);
    Appointment bookAppointment(Long userId, Long slotId, Long branchId, String serviceName);
    void cancelAppointment(Long appointmentId);
}
