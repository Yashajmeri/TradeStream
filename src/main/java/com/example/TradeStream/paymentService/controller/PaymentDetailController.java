package com.example.TradeStream.paymentService.controller;
import com.example.TradeStream.paymentService.entity.PaymentDetails;
import com.example.TradeStream.paymentService.payload.PaymentDetailsRequest;
import com.example.TradeStream.paymentService.service.PaymentDetailService;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payment Details", description = "Saved bank account details for withdrawals")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PaymentDetailController {
private final PaymentDetailService paymentDetailService;
private final UserService userService;

    @Operation(summary = "Save bank account details for the authenticated user",
            responses = {
                @ApiResponse(responseCode = "201", description = "Payment details saved"),
                @ApiResponse(responseCode = "400", description = "Invalid request body"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PostMapping("/payment-details")
public ResponseEntity<PaymentDetails > addPaymentDetails(Authentication authentication
        , @Valid @RequestBody PaymentDetailsRequest request) {
    User user = userService.getUserByUserName(authentication.getName());
    PaymentDetails paymentDetails = paymentDetailService.addPaymentDetails(
            request.getAccountNumber(),
            request.getBankName(),
            request.getIfscCode(),
            request.getAccountHolderName(), user);
    return new ResponseEntity<>(paymentDetails, HttpStatus.CREATED) ;}

    @Operation(summary = "List all saved payment details for the authenticated user",
            responses = {
                @ApiResponse(responseCode = "200", description = "Payment details list returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/payment-details")
    public ResponseEntity<List<PaymentDetails>> getPaymentDetailsForCurrentUser(Authentication authentication) {
        User user = userService.getUserByUserName(authentication.getName());
        List<PaymentDetails> paymentDetailsList = paymentDetailService.getPaymentDetailsByUserId(user.getId());
        return ResponseEntity.ok(paymentDetailsList);
}

}
