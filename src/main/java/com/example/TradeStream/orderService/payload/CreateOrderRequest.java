package com.example.TradeStream.orderService.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Coin ID is required")
    private String coinId;

    @NotNull(message = "Order type (BUY/SELL) is required")
    private OrderType orderType;

    @Positive(message = "Quantity must be greater than zero")
    private double quantity;
}
