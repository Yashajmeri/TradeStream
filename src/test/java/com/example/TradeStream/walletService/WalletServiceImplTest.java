package com.example.TradeStream.walletService;

import com.example.TradeStream.orderService.entity.Order;
import com.example.TradeStream.orderService.payload.OrderType;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.entity.Wallet;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.walletService.payload.WalletTransactionStatus;
import com.example.TradeStream.walletService.payload.WalletTransactionType;
import com.example.TradeStream.walletService.repository.WalletRepository;
import com.example.TradeStream.walletService.service.WalletServiceImpl;
import com.example.TradeStream.walletService.service.WalletTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionService walletTransactionService;

    @InjectMocks
    private WalletServiceImpl walletService;

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
        wallet.setBalance(new BigDecimal("1000.00"));
    }

    // --- getWalletByUser ---

    @Test
    void getWalletByUser_whenWalletExists_returnsExistingWallet() {
        when(walletRepository.findByUser_Id(1L)).thenReturn(wallet);

        Wallet result = walletService.getWalletByUser(user);

        assertThat(result).isEqualTo(wallet);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void getWalletByUser_whenWalletNotFound_createsNewWallet() {
        when(walletRepository.findByUser_Id(1L)).thenReturn(null);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.getWalletByUser(user);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(walletRepository).save(any(Wallet.class));
    }

    // --- addFunds ---

    @Test
    void addFunds_increasesBalanceCorrectly() {
        BigDecimal amount = new BigDecimal("500.00");
        when(walletRepository.save(wallet)).thenReturn(wallet);

        Wallet result = walletService.addFunds(wallet, amount);

        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void addFunds_createsAddFundsTransaction() {
        BigDecimal amount = new BigDecimal("500.00");
        when(walletRepository.save(wallet)).thenReturn(wallet);

        walletService.addFunds(wallet, amount);

        verify(walletTransactionService).createTransaction(
                eq(wallet), eq(user),
                eq(WalletTransactionType.ADD_FUNDS),
                eq(WalletTransactionStatus.SUCCESS),
                eq(amount),
                eq(new BigDecimal("1000.00")),
                eq(new BigDecimal("1500.00")),
                anyString(), isNull()
        );
    }

    // --- findWalletById ---

    @Test
    void findWalletById_whenFound_returnsWallet() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.findWalletById(1L);

        assertThat(result).isEqualTo(wallet);
    }

    @Test
    void findWalletById_whenNotFound_throwsAPIException() {
        when(walletRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.findWalletById(99L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("99");
    }

    // --- walletToWalletTransfer ---

    @Test
    void walletToWalletTransfer_success_debitsSenderAndCreditsReceiver() {
        User receiver = new User();
        receiver.setId(2L);
        Wallet receiverWallet = new Wallet();
        receiverWallet.setId(2L);
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(new BigDecimal("200.00"));

        BigDecimal amount = new BigDecimal("300.00");
        when(walletRepository.findByUser_Id(1L)).thenReturn(wallet);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.walletToWalletTransfer(user, receiverWallet, amount);

        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void walletToWalletTransfer_createsTransferTransactions() {
        User receiver = new User();
        receiver.setId(2L);
        Wallet receiverWallet = new Wallet();
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(new BigDecimal("200.00"));

        when(walletRepository.findByUser_Id(1L)).thenReturn(wallet);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.walletToWalletTransfer(user, receiverWallet, new BigDecimal("100.00"));

        verify(walletTransactionService).createTransaction(
                any(), any(), eq(WalletTransactionType.TRANSFER_SENT),
                eq(WalletTransactionStatus.SUCCESS), any(), any(), any(), anyString(), anyString()
        );
        verify(walletTransactionService).createTransaction(
                any(), any(), eq(WalletTransactionType.TRANSFER_RECEIVED),
                eq(WalletTransactionStatus.SUCCESS), any(), any(), any(), anyString(), anyString()
        );
    }

    @Test
    void walletToWalletTransfer_insufficientFunds_throwsAPIException() {
        User receiver = new User();
        Wallet receiverWallet = new Wallet();
        receiverWallet.setUser(receiver);

        when(walletRepository.findByUser_Id(1L)).thenReturn(wallet);

        assertThatThrownBy(() -> walletService.walletToWalletTransfer(user, receiverWallet, new BigDecimal("5000.00")))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void walletToWalletTransfer_zeroAmount_throwsAPIException() {
        User receiver = new User();
        Wallet receiverWallet = new Wallet();
        receiverWallet.setUser(receiver);

        assertThatThrownBy(() -> walletService.walletToWalletTransfer(user, receiverWallet, BigDecimal.ZERO))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void walletToWalletTransfer_negativeAmount_throwsAPIException() {
        User receiver = new User();
        Wallet receiverWallet = new Wallet();
        receiverWallet.setUser(receiver);

        assertThatThrownBy(() -> walletService.walletToWalletTransfer(user, receiverWallet, new BigDecimal("-50.00")))
                .isInstanceOf(APIException.class);
    }

    // --- doOrderPayment ---

    @Test
    void doOrderPayment_buyOrder_deductsBalanceFromWallet() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderType(OrderType.BUY);
        order.setAmount(new BigDecimal("400.00"));

        when(walletRepository.findByUser_Id(1L)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);

        Wallet result = walletService.doOrderPayment(user, order);

        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void doOrderPayment_buyOrder_createsBuyAssetTransaction() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderType(OrderType.BUY);
        order.setAmount(new BigDecimal("400.00"));

        when(walletRepository.findByUser_Id(1L)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);

        walletService.doOrderPayment(user, order);

        verify(walletTransactionService).createTransaction(
                any(), any(), eq(WalletTransactionType.BUY_ASSET),
                eq(WalletTransactionStatus.SUCCESS), any(), any(), any(), anyString(), anyString()
        );
    }

    @Test
    void doOrderPayment_buyOrder_insufficientFunds_throwsAPIException() {
        Order order = new Order();
        order.setOrderType(OrderType.BUY);
        order.setAmount(new BigDecimal("9999.00"));

        when(walletRepository.findByUser_Id(1L)).thenReturn(wallet);

        assertThatThrownBy(() -> walletService.doOrderPayment(user, order))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void doOrderPayment_sellOrder_creditsBalanceToWallet() {
        Order order = new Order();
        order.setId(11L);
        order.setOrderType(OrderType.SELL);
        order.setAmount(new BigDecimal("250.00"));

        when(walletRepository.findByUser_Id(1L)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);

        Wallet result = walletService.doOrderPayment(user, order);

        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("1250.00"));
    }

    @Test
    void doOrderPayment_sellOrder_createsSellAssetTransaction() {
        Order order = new Order();
        order.setId(11L);
        order.setOrderType(OrderType.SELL);
        order.setAmount(new BigDecimal("250.00"));

        when(walletRepository.findByUser_Id(1L)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);

        walletService.doOrderPayment(user, order);

        verify(walletTransactionService).createTransaction(
                any(), any(), eq(WalletTransactionType.SELL_ASSET),
                eq(WalletTransactionStatus.SUCCESS), any(), any(), any(), anyString(), anyString()
        );
    }
}
