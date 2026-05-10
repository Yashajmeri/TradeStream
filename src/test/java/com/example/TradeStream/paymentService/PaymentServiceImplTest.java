package com.example.TradeStream.paymentService;

import com.example.TradeStream.paymentService.entity.PaymentOrder;
import com.example.TradeStream.paymentService.payload.PaymentMethod;
import com.example.TradeStream.paymentService.payload.PaymentOrderStatus;
import com.example.TradeStream.paymentService.repository.PaymentOrderRepository;
import com.example.TradeStream.paymentService.service.PaymentServiceImpl;
import com.example.TradeStream.userService.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
    }

    // --- createPaymentOrder ---

    @Test
    void createPaymentOrder_setsAllFieldsCorrectlyAndPersists() {
        when(paymentOrderRepository.save(any(PaymentOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentOrder result = paymentService.createPaymentOrder(
                user, new BigDecimal("250.00"), PaymentMethod.STRIPE);

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderRepository).save(captor.capture());

        PaymentOrder captured = captor.getValue();
        assertThat(captured.getUser()).isEqualTo(user);
        assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(captured.getPaymentMethod()).isEqualTo(PaymentMethod.STRIPE);
        assertThat(captured.getStatus()).isEqualTo(PaymentOrderStatus.PENDING);
        assertThat(captured.isCreditedToWallet()).isFalse();
    }

    @Test
    void createPaymentOrder_razorpayMethod_persistsWithRazorpayMethod() {
        when(paymentOrderRepository.save(any(PaymentOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentOrder result = paymentService.createPaymentOrder(
                user, new BigDecimal("100.00"), PaymentMethod.RAZORPAY);

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.RAZORPAY);
    }

    // --- getPaymentOrderById ---

    @Test
    void getPaymentOrderById_whenFound_returnsPaymentOrder() {
        PaymentOrder order = new PaymentOrder();
        order.setId(1L);
        order.setStatus(PaymentOrderStatus.PENDING);
        when(paymentOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        PaymentOrder result = paymentService.getPaymentOrderById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(PaymentOrderStatus.PENDING);
    }

    @Test
    void getPaymentOrderById_whenNotFound_throwsRuntimeException() {
        when(paymentOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentOrderById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // --- processPaymentOrder ---

    @Test
    void processPaymentOrder_whenAlreadySuccess_returnsTrueWithoutApiCall() throws Exception {
        PaymentOrder order = new PaymentOrder();
        order.setStatus(PaymentOrderStatus.SUCCESS);

        Boolean result = paymentService.processPaymentOrder(order, "any-id");

        assertThat(result).isTrue();
        verify(paymentOrderRepository, never()).save(any());
    }

    @Test
    void processPaymentOrder_whenAlreadyFailed_returnsFalseWithoutApiCall() throws Exception {
        PaymentOrder order = new PaymentOrder();
        order.setStatus(PaymentOrderStatus.FAILED);

        Boolean result = paymentService.processPaymentOrder(order, "any-id");

        assertThat(result).isFalse();
        verify(paymentOrderRepository, never()).save(any());
    }

    // --- markPaymentOrderAsCredited ---

    @Test
    void markPaymentOrderAsCredited_setsFieldsAndPersists() {
        PaymentOrder order = new PaymentOrder();
        order.setId(1L);
        order.setCreditedToWallet(false);
        when(paymentOrderRepository.save(order)).thenReturn(order);

        PaymentOrder result = paymentService.markPaymentOrderAsCredited(order, "stripe-session-xyz");

        assertThat(result.isCreditedToWallet()).isTrue();
        assertThat(result.getProviderPaymentId()).isEqualTo("stripe-session-xyz");
        verify(paymentOrderRepository).save(order);
    }

    @Test
    void markPaymentOrderAsCredited_nullProviderPaymentId_doesNotOverwriteId() {
        PaymentOrder order = new PaymentOrder();
        order.setId(2L);
        order.setProviderPaymentId(null);
        when(paymentOrderRepository.save(order)).thenReturn(order);

        PaymentOrder result = paymentService.markPaymentOrderAsCredited(order, null);

        assertThat(result.isCreditedToWallet()).isTrue();
        assertThat(result.getProviderPaymentId()).isNull();
    }

    @Test
    void markPaymentOrderAsCredited_blankProviderPaymentId_doesNotOverwriteId() {
        PaymentOrder order = new PaymentOrder();
        order.setId(3L);
        order.setProviderPaymentId(null);
        when(paymentOrderRepository.save(order)).thenReturn(order);

        PaymentOrder result = paymentService.markPaymentOrderAsCredited(order, "   ");

        assertThat(result.isCreditedToWallet()).isTrue();
        assertThat(result.getProviderPaymentId()).isNull();
    }
}
