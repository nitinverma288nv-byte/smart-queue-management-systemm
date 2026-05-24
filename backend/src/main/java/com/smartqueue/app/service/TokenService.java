package com.smartqueue.app.service;

import com.smartqueue.app.entity.Token;
import java.util.List;

public interface TokenService {
    Token generateToken(Long userId, String sectorType, Long branchId, Long counterId, String serviceName, Long appointmentId, String priority);
    List<Token> getTokensByUserId(Long userId);
    Token updateTokenStatus(Long tokenId, String status);
    Token callNextToken(Long counterId);
    Token skipToken(Long tokenId);
    Token completeToken(Long tokenId);
    void cancelToken(Long tokenId);
    List<Token> getActiveQueueTokens(Long counterId);
    Token callSpecificToken(Long tokenId, Long counterId);
}
