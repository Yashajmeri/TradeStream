package com.example.TradeStream.paymentService.service;

import com.example.TradeStream.paymentService.entity.PaymentOrder;
import com.example.TradeStream.paymentService.payload.PaymentMethod;
import com.example.TradeStream.paymentService.payload.PaymentResponse;
import com.example.TradeStream.userService.entity.User;
import com.razorpay.RazorpayException;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentOrder createPaymentOrder(User user, BigDecimal amount, PaymentMethod paymentMethod);
    PaymentOrder getPaymentOrderById(Long paymentId);
    Boolean processPaymentOrder(PaymentOrder paymentOrder , String paymentId) throws RazorpayException;
    PaymentResponse createRazorPayOrder(User user, BigDecimal amount, Long paymentOrderId) throws RazorpayException;
    PaymentResponse createStripePayOrder(User user, BigDecimal amount,Long orderId);
    PaymentOrder markPaymentOrderAsCredited(PaymentOrder paymentOrder, String providerPaymentId);
}
