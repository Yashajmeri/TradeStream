package com.example.TradeStream.orderService.repository;

import com.example.TradeStream.orderService.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
