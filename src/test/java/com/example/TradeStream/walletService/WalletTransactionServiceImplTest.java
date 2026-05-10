package com.example.TradeStream.walletService;

import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.entity.Wallet;
import com.example.TradeStream.walletService.entity.WalletTransaction;
import com.example.TradeStream.walletService.payload.WalletTransactionStatus;
import com.example.TradeStream.walletService.payload.WalletTransactionType;
import com.example.TradeStream.walletService.repository.WalletTransactionRepository;
import com.example.TradeStream.walletService.service.WalletTransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletTransactionServiceImplTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private WalletTransactionServiceImpl walletTransactionService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("500.00"));
    }

    // --- createTransaction ---

    @Test
    void createTransaction_buildsTransactionWithCorrectFields() {
        WalletTransaction saved = WalletTransaction.builder().id(1L).build();
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenReturn(saved);

        walletTransactionService.createTransaction(
                wallet, user,
                WalletTransactionType.BUY_ASSET,
                WalletTransactionStatus.SUCCESS,
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                new BigDecimal("400.00"),
                "Bought BTC", "order-42"
        );

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());

        WalletTransaction captured = captor.getValue();
        assertThat(captured.getWallet()).isEqualTo(wallet);
        assertThat(captured.getUser()).isEqualTo(user);
        assertThat(captured.getType()).isEqualTo(WalletTransactionType.BUY_ASSET);
        assertThat(captured.getStatus()).isEqualTo(WalletTransactionStatus.SUCCESS);
        assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(captured.getBalanceBefore()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(captured.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(captured.getDescription()).isEqualTo("Bought BTC");
        assertThat(captured.getReferenceId()).isEqualTo("order-42");
        assertThat(captured.getTimestamp()).isNotNull();
    }

    @Test
    void createTransaction_returnsPersistedTransaction() {
        WalletTransaction saved = WalletTransaction.builder().id(99L).build();
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenReturn(saved);

        WalletTransaction result = walletTransactionService.createTransaction(
                wallet, user,
                WalletTransactionType.ADD_FUNDS,
                WalletTransactionStatus.SUCCESS,
                new BigDecimal("200.00"),
                new BigDecimal("300.00"),
                new BigDecimal("500.00"),
                "Deposit", null
        );

        assertThat(result.getId()).isEqualTo(99L);
    }

    // --- getUserTransactions ---

    @Test
    void getUserTransactions_delegatesToRepositoryAndReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        WalletTransaction tx = WalletTransaction.builder().id(1L).build();
        Page<WalletTransaction> page = new PageImpl<>(List.of(tx));
        when(walletTransactionRepository.findByUser_IdOrderByTimestampDesc(1L, pageable)).thenReturn(page);

        Page<WalletTransaction> result = walletTransactionService.getUserTransactions(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        verify(walletTransactionRepository).findByUser_IdOrderByTimestampDesc(1L, pageable);
    }

    @Test
    void getUserTransactions_emptyHistory_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(walletTransactionRepository.findByUser_IdOrderByTimestampDesc(99L, pageable))
                .thenReturn(Page.empty());

        Page<WalletTransaction> result = walletTransactionService.getUserTransactions(99L, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
