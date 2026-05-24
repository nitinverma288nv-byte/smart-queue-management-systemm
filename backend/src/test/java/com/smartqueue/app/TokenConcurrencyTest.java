package com.smartqueue.app;

import com.smartqueue.app.entity.Counter;
import com.smartqueue.app.entity.Token;
import com.smartqueue.app.entity.User;
import com.smartqueue.app.entity.Role;
import com.smartqueue.app.repository.CounterRepository;
import com.smartqueue.app.repository.TokenRepository;
import com.smartqueue.app.repository.UserRepository;
import com.smartqueue.app.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class TokenConcurrencyTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testConcurrentTokenGeneration() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        Set<String> tokenNumbers = ConcurrentHashMap.newKeySet();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        Optional<Counter> hospitalCounterOpt = counterRepository.findBySectorType("HOSPITAL").stream().findFirst();
        if (hospitalCounterOpt.isEmpty()) {
            throw new IllegalStateException("No HOSPITAL counter seeded for testing");
        }
        Counter hospitalCounter = hospitalCounterOpt.get();

        User testUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_USER || u.getRole() == Role.ROLE_ADMIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No test user found"));
        final long userId = testUser.getId();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    Token token = tokenService.generateToken(userId, "HOSPITAL", hospitalCounter.getBranchId(), hospitalCounter.getId(), "OPD", null, "REGULAR");
                    tokenNumbers.add(token.getTokenNumber());
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // Print exceptions if any
        for (Exception e : exceptions) {
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }

        // Verify that no exceptions occurred and all 10 tokens are unique
        assertEquals(0, exceptions.size(), "Should have no exceptions during token generation");
        assertEquals(threadCount, tokenNumbers.size(), "All generated token numbers should be unique");
    }

    @Test
    public void testSectorQueueIsolationBetweenHospitalAndBank() {
        Counter hospitalCounter = counterRepository.findBySectorType("HOSPITAL").stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No HOSPITAL counter seeded for testing"));
        Counter bankCounter = counterRepository.findBySectorType("BANK").stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No BANK counter seeded for testing"));

        User testUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_USER || u.getRole() == Role.ROLE_ADMIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No test user found"));
        final long userId = testUser.getId();
        Token hospitalToken = tokenService.generateToken(userId, "HOSPITAL", hospitalCounter.getBranchId(), hospitalCounter.getId(), "OPD", null, "REGULAR");
        Token bankToken = tokenService.generateToken(userId, "BANK", bankCounter.getBranchId(), bankCounter.getId(), "Cash", null, "REGULAR");

        List<Token> hospitalQueueTokens = tokenRepository.findBySectorTypeAndBranchIdAndCounterId("HOSPITAL", hospitalCounter.getBranchId(), hospitalCounter.getId());
        List<Token> bankQueueTokens = tokenRepository.findBySectorTypeAndBranchIdAndCounterId("BANK", bankCounter.getBranchId(), bankCounter.getId());

        assertTrue(hospitalQueueTokens.stream().anyMatch(t -> t.getId().equals(hospitalToken.getId())), "Hospital queue should contain its own token");
        assertFalse(hospitalQueueTokens.stream().anyMatch(t -> t.getId().equals(bankToken.getId())), "Hospital queue should never contain bank token");

        assertTrue(bankQueueTokens.stream().anyMatch(t -> t.getId().equals(bankToken.getId())), "Bank queue should contain its own token");
        assertFalse(bankQueueTokens.stream().anyMatch(t -> t.getId().equals(hospitalToken.getId())), "Bank queue should never contain hospital token");
    }
}
