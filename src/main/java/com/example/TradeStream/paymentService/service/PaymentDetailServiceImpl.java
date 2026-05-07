package com.example.TradeStream.paymentService.service;

import com.example.TradeStream.paymentService.entity.PaymentDetails;
import com.example.TradeStream.paymentService.repository.PaymentDetailRepository;
import com.example.TradeStream.userService.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentDetailServiceImpl implements PaymentDetailService {
  private final PaymentDetailRepository paymentDetailRepository;
    @Override
    public PaymentDetails addPaymentDetails(String accountNumber, String bankName, String ifscCode, String accountHolderName, User user) {
        PaymentDetails paymentDetails = new PaymentDetails();
        paymentDetails.setAccountNumber(accountNumber);
        paymentDetails.setBankName(bankName);
        paymentDetails.setIfscCode(ifscCode);
        paymentDetails.setAccountHolderName(accountHolderName);
        paymentDetails.setUser(user);
        return paymentDetailRepository.save(paymentDetails);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PaymentDetails> getPaymentDetailsByUserId(Long userId) {
        List<PaymentDetails> usersPaymentDetails = paymentDetailRepository.findByUser_Id(userId);
        if(usersPaymentDetails.isEmpty()){
            throw new RuntimeException("No payment details found for user with id: " + userId);
        }
        return usersPaymentDetails;

    }
}
