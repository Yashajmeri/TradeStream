package com.example.TradeStream.withdrawalService.service;

import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.withdrawalService.entity.Withdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WithdrawalService {
    Page<Withdrawal> getAllWithdrawals(Pageable pageable);
    Withdrawal requestWithdrawal(User user, Long amount);
    Withdrawal processWithdrawal(Long withdrawalId, boolean accept);
    List<Withdrawal> getUserWithdrawalHistory(User user);
}
