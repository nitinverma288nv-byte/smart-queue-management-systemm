package com.smartqueue.app.repository;

import com.smartqueue.app.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollegeRepository extends JpaRepository<College, Long> {
}
