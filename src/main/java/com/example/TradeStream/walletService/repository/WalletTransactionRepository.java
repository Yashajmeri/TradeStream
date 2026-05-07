package com.example.TradeStream.walletService.repository;

import com.example.TradeStream.walletService.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Page<WalletTransaction> findByUser_IdOrderByTimestampDesc(Long userId, Pageable pageable);
    Page<WalletTransaction> findByWallet_IdOrderByTimestampDesc(Long walletId, Pageable pageable);
}