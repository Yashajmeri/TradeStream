package com.example.TradeStream.withdrawalService;

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
import com.example.TradeStream.withdrawalService.service.WithdrawalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceImplTest {

    @Mock private WithdrawalRepository withdrawalRepository;
    @Mock private WalletService walletService;
    @Mock private WalletTransactionService walletTransactionService;
    @Mock private WalletRepository walletRepository;

    @InjectMocks
    private WithdrawalServiceImpl withdrawalService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("password123");

        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("5000.00"));
    }

    // --- getAllWithdrawals ---

    @Test
    void getAllWithdrawals_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(1L);
        when(withdrawalRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(withdrawal)));

        Page<Withdrawal> result = withdrawalService.getAllWithdrawals(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // --- requestWithdrawal ---

    @Test
    void requestWithdrawal_success_deductsBalanceAndCreatesPendingWithdrawal() {
        Long amount = 1000L;
        when(walletService.getWalletByUser(user)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(inv -> {
            Withdrawal w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });

        Withdrawal result = withdrawalService.requestWithdrawal(user, amount);

        assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.PENDING);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("4000.00"));
    }

    @Test
    void requestWithdrawal_createsWithdrawalTransaction() {
        when(walletService.getWalletByUser(user)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(inv -> {
            Withdrawal w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });

        withdrawalService.requestWithdrawal(user, 500L);

        verify(walletTransactionService).createTransaction(
                any(), any(), eq(WalletTransactionType.WITHDRAWAL),
                eq(WalletTransactionStatus.PENDING), any(), any(), any(), anyString(), anyString()
        );
    }

    @Test
    void requestWithdrawal_insufficientBalance_throwsAPIException() {
        when(walletService.getWalletByUser(user)).thenReturn(wallet);

        assertThatThrownBy(() -> withdrawalService.requestWithdrawal(user, 9999L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void requestWithdrawal_nullAmount_throwsAPIException() {
        assertThatThrownBy(() -> withdrawalService.requestWithdrawal(user, null))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void requestWithdrawal_zeroAmount_throwsAPIException() {
        assertThatThrownBy(() -> withdrawalService.requestWithdrawal(user, 0L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void requestWithdrawal_negativeAmount_throwsAPIException() {
        assertThatThrownBy(() -> withdrawalService.requestWithdrawal(user, -100L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("greater than zero");
    }

    // --- processWithdrawal ---

    @Test
    void processWithdrawal_accept_setsStatusToSuccess() {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(1L);
        withdrawal.setUser(user);
        withdrawal.setAmount(new BigDecimal("500.00"));
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setTimestamp(LocalDateTime.now());

        when(withdrawalRepository.findById(1L)).thenReturn(Optional.of(withdrawal));
        when(withdrawalRepository.save(withdrawal)).thenReturn(withdrawal);

        Withdrawal result = withdrawalService.processWithdrawal(1L, true);

        assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.SUCCESS);
        verify(walletService, never()).getWalletByUser(any());
    }

    @Test
    void processWithdrawal_reject_refundsWalletAndSetsStatusToFailed() {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(1L);
        withdrawal.setUser(user);
        withdrawal.setAmount(new BigDecimal("500.00"));
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setTimestamp(LocalDateTime.now());

        when(withdrawalRepository.findById(1L)).thenReturn(Optional.of(withdrawal));
        when(walletService.getWalletByUser(user)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(withdrawalRepository.save(withdrawal)).thenReturn(withdrawal);

        Withdrawal result = withdrawalService.processWithdrawal(1L, false);

        assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.FAILED);
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("5500.00"));
    }

    @Test
    void processWithdrawal_reject_createsRefundTransaction() {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(1L);
        withdrawal.setUser(user);
        withdrawal.setAmount(new BigDecimal("500.00"));
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setTimestamp(LocalDateTime.now());

        when(withdrawalRepository.findById(1L)).thenReturn(Optional.of(withdrawal));
        when(walletService.getWalletByUser(user)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(withdrawalRepository.save(withdrawal)).thenReturn(withdrawal);

        withdrawalService.processWithdrawal(1L, false);

        verify(walletTransactionService).createTransaction(
                any(), any(), eq(WalletTransactionType.WITHDRAWAL_REFUND),
                eq(WalletTransactionStatus.SUCCESS), any(), any(), any(), anyString(), anyString()
        );
    }

    @Test
    void processWithdrawal_alreadyProcessed_throwsAPIException() {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(1L);
        withdrawal.setStatus(WithdrawalStatus.SUCCESS);

        when(withdrawalRepository.findById(1L)).thenReturn(Optional.of(withdrawal));

        assertThatThrownBy(() -> withdrawalService.processWithdrawal(1L, true))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("already processed");
    }

    @Test
    void processWithdrawal_withdrawalNotFound_throwsResourceNotFoundException() {
        when(withdrawalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalService.processWithdrawal(99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getUserWithdrawalHistory ---

    @Test
    void getUserWithdrawalHistory_returnsUserWithdrawals() {
        Withdrawal w1 = new Withdrawal();
        w1.setId(1L);
        Withdrawal w2 = new Withdrawal();
        w2.setId(2L);
        when(withdrawalRepository.findByUser_Id(1L)).thenReturn(List.of(w1, w2));

        List<Withdrawal> result = withdrawalService.getUserWithdrawalHistory(user);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(w1, w2);
    }

    @Test
    void getUserWithdrawalHistory_noWithdrawals_returnsEmptyList() {
        when(withdrawalRepository.findByUser_Id(1L)).thenReturn(List.of());

        List<Withdrawal> result = withdrawalService.getUserWithdrawalHistory(user);

        assertThat(result).isEmpty();
    }
}
