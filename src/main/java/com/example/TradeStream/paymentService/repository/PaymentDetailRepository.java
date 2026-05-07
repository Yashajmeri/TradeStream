package com.example.TradeStream.paymentService.repository;

import com.example.TradeStream.paymentService.entity.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentDetailRepository extends JpaRepository<PaymentDetails, Long> {
    List<PaymentDetails> findByUser_Id(Long userId);
}
