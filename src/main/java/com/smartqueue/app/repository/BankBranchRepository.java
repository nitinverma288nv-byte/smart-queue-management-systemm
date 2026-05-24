package com.smartqueue.app.repository;

import com.smartqueue.app.entity.BankBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BankBranchRepository extends JpaRepository<BankBranch, Long> {
    List<BankBranch> findByBankId(Long bankId);
}
