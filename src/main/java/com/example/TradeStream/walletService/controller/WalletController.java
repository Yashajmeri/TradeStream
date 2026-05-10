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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "Get the authenticated user's wallet",
            responses = {
                @ApiResponse(responseCode = "200", description = "Wallet returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping
    public ResponseEntity<Wallet> getUserWallet(Authentication authentication) {
         String username = authentication.getName();
            User user = userService.getUserByUserName(username);
        Wallet wallet = walletService.getWalletByUser(user);
        return ResponseEntity.ok(wallet);
    }

    @Operation(summary = "Transfer funds to another wallet by wallet ID",
            responses = {
                @ApiResponse(responseCode = "200", description = "Transfer successful — updated sender wallet returned"),
                @ApiResponse(responseCode = "400", description = "Insufficient balance"),
                @ApiResponse(responseCode = "404", description = "Recipient wallet not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PutMapping("/{walletId}/transfer")
public ResponseEntity<Wallet> walletToTransfer(Authentication authentication,
                                               @Parameter(description = "Recipient wallet ID") @PathVariable Long walletId,
                                               @Valid @RequestBody WalletTransaction walletTransaction) {
    String senderUsername = authentication.getName();
    User sender = userService.getUserByUserName(senderUsername);
    Wallet recieverWallet = walletService.findWalletById(walletId);
    Wallet updatedWallet = walletService.walletToWalletTransfer(sender, recieverWallet, walletTransaction.getAmount());
    return ResponseEntity.ok(updatedWallet);

}
    @Operation(summary = "Pay for a pending order from the wallet",
            responses = {
                @ApiResponse(responseCode = "200", description = "Payment successful — updated wallet returned"),
                @ApiResponse(responseCode = "400", description = "Insufficient balance or order not payable"),
                @ApiResponse(responseCode = "403", description = "Order does not belong to the authenticated user"),
                @ApiResponse(responseCode = "404", description = "Order not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PutMapping("/order/{orderId}/pay")
public ResponseEntity<Wallet> doOrderPayment(Authentication authentication,
                                             @Parameter(description = "Order database ID") @PathVariable Long orderId) {
    String senderUsername = authentication.getName();
    User sender = userService.getUserByUserName(senderUsername);
    Order order = orderService.getOrderById(orderId);
    Wallet updatedWallet = walletService.doOrderPayment(sender, order);
    return ResponseEntity.ok(updatedWallet);
    }
    @Operation(summary = "Credit wallet after successful Stripe/Razorpay payment verification",
            description = "Call this endpoint with the paymentOrderId and the paymentId returned by the payment provider " +
                          "to verify and apply the top-up. Idempotent — repeated calls with the same IDs return the current balance without double-crediting.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Wallet credited — updated wallet returned"),
                @ApiResponse(responseCode = "400", description = "Payment verification failed"),
                @ApiResponse(responseCode = "403", description = "Payment order does not belong to the authenticated user"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PutMapping("/deposit")
    public ResponseEntity<Wallet> addMoneyToWallet(Authentication authentication,
            @Parameter(description = "Payment order ID returned when the order was created") @RequestParam Long paymentOrderId,
            @Parameter(description = "Payment ID returned by Stripe or Razorpay after the user completes payment") @RequestParam String paymentId) throws Exception {
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
