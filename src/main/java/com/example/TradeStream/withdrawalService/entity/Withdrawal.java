package com.example.TradeStream.withdrawalService.entity;

import com.example.TradeStream.userService.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Withdrawal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Withdrawal status is required")
    @Enumerated(EnumType.STRING)
    WithdrawalStatus status;

    @NotNull(message = "Withdrawal amount is required")
    @DecimalMin(value = "0.01", message = "Withdrawal amount must be at least 0.01")
    private BigDecimal amount;

    @NotNull(message = "User is required for a withdrawal")
    @ManyToOne
    User user;

    @NotNull(message = "Withdrawal timestamp is required")
    private LocalDateTime timestamp;
}
