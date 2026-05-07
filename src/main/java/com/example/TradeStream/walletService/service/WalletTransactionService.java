package com.example.TradeStream.walletService.service;

import com.example.TradeStream.walletService.payload.WalletTransactionStatus;
import com.example.TradeStream.walletService.payload.WalletTransactionType;
import com.example.TradeStream.walletService.entity.Wallet;
import com.example.TradeStream.walletService.entity.WalletTransaction;
import com.example.TradeStream.userService.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface WalletTransactionService {
    WalletTransaction createTransaction(
            Wallet wallet,
            User user,
            WalletTransactionType type,
            WalletTransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String description,
            String referenceId
    );

    Page<WalletTransaction> getUserTransactions(Long userId, Pageable pageable);
}