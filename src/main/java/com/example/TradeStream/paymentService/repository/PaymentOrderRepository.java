package com.example.TradeStream.paymentService.repository;

import com.example.TradeStream.paymentService.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    List<PaymentOrder> findByUser_Id(Long userId);
}
