package com.smartqueue.app.service;

import com.smartqueue.app.entity.Staff;
import com.smartqueue.app.entity.Token;
import java.util.List;

public interface StaffService {
    Staff getStaffByUserId(Long userId);
    Token getCurrentlyServingTokenByUserId(Long userId);
    List<Token> getWaitingTokensByUserId(Long userId);
}
