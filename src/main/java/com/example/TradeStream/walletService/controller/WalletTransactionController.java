package com.example.TradeStream.walletService.controller;


import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import com.example.TradeStream.walletService.entity.WalletTransaction;
import com.example.TradeStream.walletService.service.WalletTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transactions", description = "Paginated wallet transaction history")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class WalletTransactionController {
    private final WalletTransactionService walletTransactionService;
    private final UserService userService;

    @Operation(summary = "Get paginated wallet transaction history for the authenticated user",
            responses = {
                @ApiResponse(responseCode = "200", description = "Transaction page returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/api/transactions")
    public ResponseEntity<Page<WalletTransaction>> getUserTransactions(
            Authentication authentication,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        User user = userService.getUserByUserName(authentication.getName());
        return ResponseEntity.ok(
                walletTransactionService.getUserTransactions(user.getId(), PageRequest.of(page, size)));
    }
}
