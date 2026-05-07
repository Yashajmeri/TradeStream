package com.example.TradeStream.walletService.entity;

import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.payload.WalletTransactionStatus;
import com.example.TradeStream.walletService.payload.WalletTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Wallet is required for a transaction")
    @ManyToOne
    private Wallet wallet;

    @NotNull(message = "User is required for a transaction")
    @ManyToOne
    private User user;

    @NotNull(message = "Transaction type (DEPOSIT/WITHDRAWAL/TRANSFER) is required")
    @Enumerated(EnumType.STRING)
    private WalletTransactionType type;

    @NotNull(message = "Transaction status is required")
    @Enumerated(EnumType.STRING)
    private WalletTransactionStatus status;

    @NotNull(message = "Transaction timestamp is required")
    private LocalDateTime timestamp;

    @NotNull(message = "Transaction amount is required")
    private BigDecimal amount;

    @NotNull(message = "Balance before transaction is required")
    private BigDecimal balanceBefore;

    @NotNull(message = "Balance after transaction is required")
    private BigDecimal balanceAfter;

    @NotBlank(message = "Reference ID (order ID/transfer ID) is required")
    @Size(max = 255, message = "Reference ID must not exceed 255 characters")
    private String referenceId;

    @NotBlank(message = "Reference type (ORDER/WITHDRAWAL/DEPOSIT/TRANSFER) is required")
    @Size(max = 50, message = "Reference type must not exceed 50 characters")
    private String referenceType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}