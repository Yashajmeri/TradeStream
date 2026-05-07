package com.example.TradeStream.orderService.entity;


import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "Buy price cannot be negative")
    private BigDecimal buyPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Sell price cannot be negative")
    private BigDecimal sellPrice;

    @NotNull(message = "Coin is required for an order item")
    @ManyToOne
    private Coin coin;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
