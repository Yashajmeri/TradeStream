package com.example.TradeStream.paymentService.service;

import com.example.TradeStream.paymentService.entity.PaymentDetails;
import com.example.TradeStream.userService.entity.User;

import java.util.List;

public interface PaymentDetailService {
    PaymentDetails addPaymentDetails(String accountNumber,
                                     String bankName,
                                     String ifscCode,
                                     String accountHolderName,
                                     User user);
      List<PaymentDetails> getPaymentDetailsByUserId(Long userId);
}
