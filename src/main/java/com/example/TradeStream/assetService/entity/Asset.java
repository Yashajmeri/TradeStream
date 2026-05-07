package com.example.TradeStream.assetService.entity;

import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.userService.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @PositiveOrZero(message = "Asset quantity cannot be negative")
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "Buy price cannot be negative")
    private BigDecimal buyPrice;

    @NotNull(message = "Coin is required for an asset")
    @ManyToOne
    private Coin coin;

    @NotNull(message = "User is required for an asset")
    @ManyToOne
    private User user;

    @Version
    private Long version;
}
