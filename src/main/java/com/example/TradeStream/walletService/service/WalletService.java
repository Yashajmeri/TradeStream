package com.example.TradeStream.walletService.service;

import com.example.TradeStream.orderService.entity.Order;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.entity.Wallet;

import java.math.BigDecimal;

public interface WalletService {
      Wallet getWalletByUser(User user);
      Wallet addFunds(Wallet wallet, BigDecimal amount);
      Wallet findWalletById(Long walletId);
      Wallet walletToWalletTransfer(User user, Wallet receiverWallet, BigDecimal amount);
      Wallet doOrderPayment(User user, Order order);
}
