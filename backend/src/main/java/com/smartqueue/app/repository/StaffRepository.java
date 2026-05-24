package com.smartqueue.app.repository;

import com.smartqueue.app.entity.Staff;
import com.smartqueue.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByUser(User user);
    Optional<Staff> findByUserId(Long userId);
}
