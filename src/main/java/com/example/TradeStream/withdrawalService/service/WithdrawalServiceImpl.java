package com.example.TradeStream.withdrawalService.service;

import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.entity.Wallet;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.walletService.payload.WalletTransactionStatus;
import com.example.TradeStream.walletService.payload.WalletTransactionType;
import com.example.TradeStream.walletService.repository.WalletRepository;
import com.example.TradeStream.walletService.service.WalletService;
import com.example.TradeStream.walletService.service.WalletTransactionService;
import com.example.TradeStream.withdrawalService.entity.Withdrawal;
import com.example.TradeStream.withdrawalService.entity.WithdrawalStatus;
import com.example.TradeStream.withdrawalService.exception.ResourceNotFoundException;
import com.example.TradeStream.withdrawalService.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;



@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService{
    private final WithdrawalRepository withdrawalRepository;
    private final WalletService walletService;
    private final WalletTransactionService walletTransactionService;
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<Withdrawal> getAllWithdrawals(Pageable pageable) {
        return withdrawalRepository.findAll(pageable);
    }

    @Override
    public Withdrawal requestWithdrawal(User user, Long amount) {
        if (amount == null || amount <= 0) {
            throw new APIException("Withdrawal amount must be greater than zero");
        }

        BigDecimal withdrawalAmount = BigDecimal.valueOf(amount);
        Wallet wallet = walletService.getWalletByUser(user);

        if (wallet.getBalance().compareTo(withdrawalAmount) < 0) {
            throw new APIException("Insufficient balance for withdrawal");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(withdrawalAmount);

        wallet.setBalance(balanceAfter);
        Wallet savedWallet = walletRepository.save(wallet);

        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUser(user);
        withdrawal.setAmount(withdrawalAmount);
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setTimestamp(LocalDateTime.now());

        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        walletTransactionService.createTransaction(
                savedWallet,
                user,
                WalletTransactionType.WITHDRAWAL,
                WalletTransactionStatus.PENDING,
                withdrawalAmount,
                balanceBefore,
                balanceAfter,
                "Withdrawal request created and amount reserved",
                String.valueOf(savedWithdrawal.getId())
        );

        return savedWithdrawal;

    }

    @Override
    public Withdrawal processWithdrawal(Long withdrawalId, boolean accept) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("withdrawal", "id", withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new APIException("Withdrawal already processed");
        }

        withdrawal.setTimestamp(LocalDateTime.now());

        if (accept) {
            withdrawal.setStatus(WithdrawalStatus.SUCCESS);
            return withdrawalRepository.save(withdrawal);
        } else {
            Wallet wallet = walletService.getWalletByUser(withdrawal.getUser());

            BigDecimal balanceBefore = wallet.getBalance();
            BigDecimal balanceAfter = balanceBefore.add(withdrawal.getAmount());

            wallet.setBalance(balanceAfter);
            Wallet savedWallet = walletRepository.save(wallet);

            walletTransactionService.createTransaction(
                    savedWallet,
                    withdrawal.getUser(),
                    WalletTransactionType.WITHDRAWAL_REFUND,
                    WalletTransactionStatus.SUCCESS,
                    withdrawal.getAmount(),
                    balanceBefore,
                    balanceAfter,
                    "Withdrawal rejected, amount refunded to wallet",
                    String.valueOf(withdrawal.getId())
            );

            withdrawal.setStatus(WithdrawalStatus.FAILED);
            return withdrawalRepository.save(withdrawal);
        }
    }
    @Transactional(readOnly = true)
    @Override
    public List<Withdrawal> getUserWithdrawalHistory(User user) {
        return withdrawalRepository.findByUser_Id(user.getId());
    }
}
