package com.example.TradeStream.paymentService.controller;


import com.example.TradeStream.paymentService.entity.PaymentOrder;
import com.example.TradeStream.paymentService.payload.PaymentResponse;
import com.example.TradeStream.paymentService.service.PaymentService;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments", description = "Create Stripe and Razorpay payment orders for wallet top-ups")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final UserService userService;

    @Operation(summary = "Create a payment order",
            description = "Accepted paymentMethod values: STRIPE, RAZORPAY. Returns a payment URL to redirect the user.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Payment order created — redirect URL returned"),
                @ApiResponse(responseCode = "400", description = "Unsupported payment method"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PostMapping ("/payment/{paymentMethod}/amount/{amount}")
    public ResponseEntity<PaymentResponse>  paymentHandler(
            @Parameter(description = "Payment provider: STRIPE or RAZORPAY") @PathVariable String paymentMethod,
            @Parameter(description = "Amount to top up in USD") @PathVariable String amount,
            Authentication authentication) throws Exception {
        User user = userService.getUserByUserName(authentication.getName());
        PaymentOrder paymentOrder = paymentService.createPaymentOrder(user,  java.math.BigDecimal.valueOf(Double.parseDouble(amount)), com.example.TradeStream.paymentService.payload.PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        PaymentResponse paymentResponse;
        if(paymentMethod.equalsIgnoreCase("razorpay")) {
            paymentResponse = paymentService.createRazorPayOrder(user, java.math.BigDecimal.valueOf(Double.parseDouble(amount)), paymentOrder.getId());
        } else if(paymentMethod.equalsIgnoreCase("stripe")) {
            paymentResponse = paymentService.createStripePayOrder(user, java.math.BigDecimal.valueOf(Double.parseDouble(amount)), paymentOrder.getId());
        } else {
            throw new com.example.TradeStream.walletService.exception.APIException("Unsupported payment method. Accepted values: STRIPE, RAZORPAY");
        }
return new ResponseEntity<>(paymentResponse, HttpStatus.CREATED);
    }

}
