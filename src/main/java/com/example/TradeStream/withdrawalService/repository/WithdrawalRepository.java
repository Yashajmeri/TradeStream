package com.example.TradeStream.withdrawalService.repository;

import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.withdrawalService.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByUser_Id(Long userId);
}
