package com.example.TradeStream.walletService.controller;

import com.example.TradeStream.orderService.entity.Order;
import com.example.TradeStream.orderService.service.OrderService;
import com.example.TradeStream.paymentService.entity.PaymentOrder;
import com.example.TradeStream.paymentService.service.PaymentService;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import com.example.TradeStream.walletService.entity.Wallet;
import com.example.TradeStream.walletService.entity.WalletTransaction;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.walletService.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Wallet", description = "Wallet balance, transfers, and deposit management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wallet")
public class WalletController {
    private final WalletService walletService;
    private final UserService userService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    @Operation(summary = "Get the authenticated user's wallet")
    @GetMapping
    public ResponseEntity<Wallet> getUserWallet(Authentication authentication) {
         String username = authentication.getName();
            User user = userService.getUserByUserName(username);
        Wallet wallet = walletService.getWalletByUser(user);
        return ResponseEntity.ok(wallet);
    }

    @Operation(summary = "Transfer funds to another wallet by wallet ID")
    @PutMapping("/{walletId}/transfer")
public ResponseEntity<Wallet> walletToTransfer(Authentication authentication,
                                               @PathVariable Long walletId,
                                               @Valid @RequestBody WalletTransaction walletTransaction) {
    String senderUsername = authentication.getName();
    User sender = userService.getUserByUserName(senderUsername);
    Wallet recieverWallet = walletService.findWalletById(walletId);
    Wallet updatedWallet = walletService.walletToWalletTransfer(sender, recieverWallet, walletTransaction.getAmount());
    return ResponseEntity.ok(updatedWallet);

}
    @Operation(summary = "Pay for a pending order from the wallet")
    @PutMapping("/order/{orderId}/pay")
public ResponseEntity<Wallet> doOrderPayment(Authentication authentication,
                                             @PathVariable Long orderId) {
    String senderUsername = authentication.getName();
    User sender = userService.getUserByUserName(senderUsername);
    Order order = orderService.getOrderById(orderId);
    Wallet updatedWallet = walletService.doOrderPayment(sender, order);
    return ResponseEntity.ok(updatedWallet);
    }
    @Operation(summary = "Credit wallet after successful Stripe/Razorpay payment verification")
    @PutMapping("/deposit")
    public ResponseEntity<Wallet> addMoneyToWallet (Authentication authentication,@RequestParam Long paymentOrderId,
                                                    @RequestParam String paymentId) throws Exception {
        String senderUsername = authentication.getName();
        User user = userService.getUserByUserName(senderUsername);
        Wallet userWallet = walletService.getWalletByUser(user);
        PaymentOrder paymentOrder = paymentService.getPaymentOrderById(paymentOrderId);

        if (paymentOrder.getUser() == null || !paymentOrder.getUser().getId().equals(user.getId())) {
            throw new APIException("Payment order does not belong to the authenticated user");
        }

        if (paymentOrder.isCreditedToWallet()) {
            return ResponseEntity.ok(userWallet);
        }

        Boolean paymentProcessed = paymentService.processPaymentOrder(paymentOrder, paymentId);

        if (!Boolean.TRUE.equals(paymentProcessed)) {
            throw new APIException("Payment verification failed");
        }

        Wallet updatedWallet = walletService.addFunds(userWallet, paymentOrder.getAmount());
        paymentService.markPaymentOrderAsCredited(paymentOrder, paymentId);
        return ResponseEntity.ok(updatedWallet);
    }



}
