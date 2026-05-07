package com.example.TradeStream.walletService.service;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.entity.Wallet;
import com.example.TradeStream.walletService.entity.WalletTransaction;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.walletService.payload.WalletTransactionStatus;
import com.example.TradeStream.walletService.payload.WalletTransactionType;
import com.example.TradeStream.walletService.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;


    @Override
    public WalletTransaction createTransaction(Wallet wallet, User user, WalletTransactionType type,
                                               WalletTransactionStatus status,
                                               BigDecimal amount, BigDecimal balanceBefore,
                                               BigDecimal balanceAfter, String description,
                                               String referenceId) {
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .user(user)
                .type(type)
                .status(status)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .referenceId(referenceId)
                .timestamp(LocalDateTime.now())
                .build();

        return walletTransactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<WalletTransaction> getUserTransactions(Long userId, Pageable pageable) {
        return walletTransactionRepository.findByUser_IdOrderByTimestampDesc(userId, pageable);
    }
}
