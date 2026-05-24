package com.smartqueue.app.repository;

import com.smartqueue.app.entity.ResetToken;
import com.smartqueue.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {
    Optional<ResetToken> findByToken(String token);
    Optional<ResetToken> findByUser(User user);
    void deleteByUser(User user);
}
