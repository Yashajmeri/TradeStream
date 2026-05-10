package com.example.TradeStream.paymentService;

import com.example.TradeStream.paymentService.entity.PaymentDetails;
import com.example.TradeStream.paymentService.repository.PaymentDetailRepository;
import com.example.TradeStream.paymentService.service.PaymentDetailServiceImpl;
import com.example.TradeStream.userService.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentDetailServiceImplTest {

    @Mock
    private PaymentDetailRepository paymentDetailRepository;

    @InjectMocks
    private PaymentDetailServiceImpl paymentDetailService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
    }

    // --- addPaymentDetails ---

    @Test
    void addPaymentDetails_savesWithCorrectFieldsAndReturns() {
        PaymentDetails saved = new PaymentDetails();
        saved.setId(1L);
        saved.setAccountNumber("123456789012");
        saved.setBankName("HDFC Bank");
        saved.setIfscCode("HDFC0001234");
        saved.setAccountHolderName("Test User");
        saved.setUser(user);
        when(paymentDetailRepository.save(any(PaymentDetails.class))).thenReturn(saved);

        PaymentDetails result = paymentDetailService.addPaymentDetails(
                "123456789012", "HDFC Bank", "HDFC0001234", "Test User", user);

        ArgumentCaptor<PaymentDetails> captor = ArgumentCaptor.forClass(PaymentDetails.class);
        verify(paymentDetailRepository).save(captor.capture());

        PaymentDetails captured = captor.getValue();
        assertThat(captured.getAccountNumber()).isEqualTo("123456789012");
        assertThat(captured.getBankName()).isEqualTo("HDFC Bank");
        assertThat(captured.getIfscCode()).isEqualTo("HDFC0001234");
        assertThat(captured.getAccountHolderName()).isEqualTo("Test User");
        assertThat(captured.getUser()).isEqualTo(user);

        assertThat(result.getId()).isEqualTo(1L);
    }

    // --- getPaymentDetailsByUserId ---

    @Test
    void getPaymentDetailsByUserId_whenFound_returnsList() {
        PaymentDetails details = new PaymentDetails();
        details.setId(1L);
        details.setUser(user);
        when(paymentDetailRepository.findByUser_Id(1L)).thenReturn(List.of(details));

        List<PaymentDetails> result = paymentDetailService.getPaymentDetailsByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getPaymentDetailsByUserId_whenMultipleExist_returnsAll() {
        PaymentDetails d1 = new PaymentDetails(); d1.setId(1L);
        PaymentDetails d2 = new PaymentDetails(); d2.setId(2L);
        when(paymentDetailRepository.findByUser_Id(1L)).thenReturn(List.of(d1, d2));

        List<PaymentDetails> result = paymentDetailService.getPaymentDetailsByUserId(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    void getPaymentDetailsByUserId_whenNoneFound_throwsRuntimeException() {
        when(paymentDetailRepository.findByUser_Id(99L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> paymentDetailService.getPaymentDetailsByUserId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }
}
