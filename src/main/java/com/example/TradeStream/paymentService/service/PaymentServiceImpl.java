package com.example.TradeStream.paymentService.service;

import com.example.TradeStream.paymentService.entity.PaymentOrder;
import com.example.TradeStream.paymentService.payload.PaymentMethod;
import com.example.TradeStream.paymentService.payload.PaymentOrderStatus;
import com.example.TradeStream.paymentService.payload.PaymentResponse;
import com.example.TradeStream.paymentService.repository.PaymentOrderRepository;
import com.example.TradeStream.userService.entity.User;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentOrderRepository paymentOrderRepository;
    @Value("${stripe.api.secret}")
    private String stripeSecretKey;
    @Value("${razorpay.api.key}")
    private String razorpayKeyId;
    @Value("${razorpay.api.secret}")
    private String razorpayKeySecret;
    @Override
    public PaymentOrder createPaymentOrder(User user, BigDecimal amount, PaymentMethod paymentMethod) {
         PaymentOrder paymentOrder = new PaymentOrder();
            paymentOrder.setUser(user);
            paymentOrder.setAmount(amount);
            paymentOrder.setPaymentMethod(paymentMethod);
            paymentOrder.setStatus(PaymentOrderStatus.PENDING);
            paymentOrder.setCreditedToWallet(false);

        return paymentOrderRepository.save(paymentOrder);
    }

    @Transactional(readOnly = true)
    @Override
    public PaymentOrder getPaymentOrderById(Long paymentId) {
        return paymentOrderRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment order not found with id: " + paymentId));
    }

    @Override
    public Boolean processPaymentOrder(PaymentOrder paymentOrder, String paymentId) throws RazorpayException {
        if (paymentOrder.getStatus() == PaymentOrderStatus.SUCCESS) {
            return true;
        }
        if (paymentOrder.getStatus() == PaymentOrderStatus.FAILED) {
            return false;
        }
        if (paymentOrder.getPaymentMethod() == PaymentMethod.RAZORPAY) {
            return verifyRazorpayPayment(paymentOrder, paymentId);
        }
        if (paymentOrder.getPaymentMethod() == PaymentMethod.STRIPE) {
            return verifyStripePayment(paymentOrder, paymentId);
        }
        return false;
    }

    @Override
    public PaymentResponse createRazorPayOrder(User user, BigDecimal amount, Long paymentOrderId) throws RazorpayException {
        BigDecimal amountInCents = amount.multiply(BigDecimal.valueOf(100));
                try {
                    RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                    JSONObject paymentLinkRequest = new JSONObject();
                    paymentLinkRequest.put("amount", amountInCents.doubleValue());
                    paymentLinkRequest.put("currency", "INR");
                    //create the Json object for customer details
                    JSONObject customerDetails = new JSONObject();
                    customerDetails.put("name", user.getFullName());
                    customerDetails.put("email", user.getEmail());
                    paymentLinkRequest.put("customer", customerDetails);
                    // Set the notification preferences
                    paymentLinkRequest.put("notify", new JSONObject().put("email", true));
                    // Set the reminder preferences
                    paymentLinkRequest.put("reminder_enable", true);
                    // Set the callback URL for payment completion
                    paymentLinkRequest.put("callback_url", "http://localhost:3000/wallet?paymentOrderId=" + paymentOrderId);
                    paymentLinkRequest.put("callback_method", "get");
                    // Create the payment link
                    com.razorpay.PaymentLink paymentLink = razorpayClient.paymentLink.create(paymentLinkRequest);
                    String paymentLinkUrl = paymentLink.get("short_url");
                    return new PaymentResponse(paymentLinkUrl, paymentOrderId);

                } catch (RazorpayException e) {
                    throw new RazorpayException(e.getMessage());
                }

    }

    @Override
    public PaymentResponse createStripePayOrder(User user, BigDecimal amount, Long orderId) {
        try {
            Stripe.apiKey = stripeSecretKey;

            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:3000/wallet?paymentOrderId=" + orderId + "&paymentId={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:3000/payment/cancel")
                    .setCustomerEmail(user.getEmail())
                    .putMetadata("paymentOrderId", String.valueOf(orderId))
                    .putMetadata("userId", String.valueOf(user.getId()))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Wallet Top Up")
                                                                    .setDescription("TradeStream payment order #" + orderId)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            return new PaymentResponse(session.getUrl(), orderId);
        } catch (ArithmeticException | StripeException e) {
            throw new RuntimeException("Failed to create Stripe payment session", e);
        }
    }

    @Override
    public PaymentOrder markPaymentOrderAsCredited(PaymentOrder paymentOrder, String providerPaymentId) {
        paymentOrder.setCreditedToWallet(true);
        if (providerPaymentId != null && !providerPaymentId.isBlank()) {
            paymentOrder.setProviderPaymentId(providerPaymentId);
        }
        return paymentOrderRepository.save(paymentOrder);
    }

    private Boolean verifyRazorpayPayment(PaymentOrder paymentOrder, String paymentId) throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        Payment payment = razorpayClient.payments.fetch(paymentId);
        Integer amount = payment.get("amount");
        boolean validPayment = "captured".equals(payment.get("status"))
                && amount.equals(paymentOrder.getAmount().multiply(BigDecimal.valueOf(100)).intValue());

        paymentOrder.setProviderPaymentId(paymentId);
        paymentOrder.setStatus(validPayment ? PaymentOrderStatus.SUCCESS : PaymentOrderStatus.FAILED);
        paymentOrderRepository.save(paymentOrder);
        return validPayment;
    }

    private Boolean verifyStripePayment(PaymentOrder paymentOrder, String paymentId) {
        try {
            Stripe.apiKey = stripeSecretKey;
            Session session = Session.retrieve(paymentId);
            Long amountTotal = session.getAmountTotal();
            String paymentStatus = session.getPaymentStatus();
            String paymentOrderId = session.getMetadata() == null ? null : session.getMetadata().get("paymentOrderId");
            String userId = session.getMetadata() == null ? null : session.getMetadata().get("userId");

            boolean validPayment = "paid".equalsIgnoreCase(paymentStatus)
                    && amountTotal != null
                    && amountTotal.equals(paymentOrder.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact())
                    && String.valueOf(paymentOrder.getId()).equals(paymentOrderId)
                    && paymentOrder.getUser() != null
                    && String.valueOf(paymentOrder.getUser().getId()).equals(userId);

            paymentOrder.setProviderPaymentId(paymentId);
            paymentOrder.setStatus(validPayment ? PaymentOrderStatus.SUCCESS : PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(paymentOrder);
            return validPayment;
        } catch (ArithmeticException | StripeException e) {
            throw new RuntimeException("Failed to verify Stripe payment session", e);
        }
    }
}
