package com.example.TradeStream.paymentService.entity;

import com.example.TradeStream.paymentService.payload.PaymentMethod;
import com.example.TradeStream.paymentService.payload.PaymentOrderStatus;
import com.example.TradeStream.userService.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be at least 0.01")
    private BigDecimal amount;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    private PaymentOrderStatus status;

    @NotNull(message = "Payment method (STRIPE/RAZORPAY) is required")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Size(max = 255, message = "Provider payment ID must not exceed 255 characters")
    private String providerPaymentId;

    private boolean creditedToWallet;

    @NotNull(message = "User is required for a payment order")
    @ManyToOne
    User user;
}
