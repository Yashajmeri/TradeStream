package com.example.TradeStream.orderService.entity;

import com.example.TradeStream.orderService.payload.OrderStatus;
import com.example.TradeStream.orderService.payload.OrderType;
import com.example.TradeStream.userService.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
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
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User is required for an order")
    @ManyToOne
    private User user;

    @NotNull(message = "Order type (BUY/SELL) is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @NotNull(message = "Order amount is required")
    @DecimalMin(value = "0.00000001", message = "Order amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Order timestamp is required")
    private LocalDateTime timestamp;

    @NotNull(message = "Order status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Version
    private Long version;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "order")
    private OrderItem orderItem;
}
