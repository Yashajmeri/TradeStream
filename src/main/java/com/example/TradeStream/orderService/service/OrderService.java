package com.example.TradeStream.orderService.service;

import com.example.TradeStream.orderService.entity.Order;
import com.example.TradeStream.orderService.entity.OrderItem;
import com.example.TradeStream.orderService.payload.OrderType;
import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.userService.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface OrderService {
    Order createOrder(User user, OrderItem orderItem, OrderType orderType);

    Order getOrderById(Long orderId);

    Page<Order> getOrdersByUser(Long userId, Pageable pageable);

    Order processOrder(User user, BigDecimal quantity, Coin coin, OrderType orderType);
}
