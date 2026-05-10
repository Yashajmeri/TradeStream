package com.example.TradeStream.withdrawalService.controller;

import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import com.example.TradeStream.withdrawalService.entity.Withdrawal;
import com.example.TradeStream.withdrawalService.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Withdrawals", description = "Withdrawal requests and admin approval workflow")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final UserService userService;

    @Operation(summary = "Request a withdrawal",
            description = "Deducts the amount from the wallet immediately. Status starts as PENDING until an admin approves.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Withdrawal request created"),
                @ApiResponse(responseCode = "400", description = "Insufficient wallet balance"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PostMapping("/withdrawals/{amount}")
    public ResponseEntity<Withdrawal> requestWithdrawal(
            @Parameter(description = "Amount to withdraw in USD") @PathVariable BigDecimal amount,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUserName(username);

        Withdrawal withdrawal = withdrawalService.requestWithdrawal(user, amount.longValue());

        return ResponseEntity.ok(withdrawal);
    }

    @Operation(summary = "Approve or reject a pending withdrawal (Admin only)",
            description = "Approval completes the withdrawal. Rejection refunds the amount back to the wallet.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Withdrawal processed"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
                @ApiResponse(responseCode = "404", description = "Withdrawal not found")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/withdrawals/{withdrawalId}/process/{accept}")
    public ResponseEntity<Withdrawal> processWithdrawal(
            @Parameter(description = "Withdrawal database ID") @PathVariable Long withdrawalId,
            @Parameter(description = "true to approve, false to reject") @PathVariable boolean accept) {
        Withdrawal processedWithdrawal = withdrawalService.processWithdrawal(withdrawalId, accept);
        return ResponseEntity.ok(processedWithdrawal);
    }

    @Operation(summary = "Get withdrawal history for the authenticated user",
            responses = {
                @ApiResponse(responseCode = "200", description = "Withdrawal history returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/withdrawals/history")
    public ResponseEntity<List<Withdrawal>> getUserWithdrawalHistory(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUserName(username);

        List<Withdrawal> withdrawals = withdrawalService.getUserWithdrawalHistory(user);

        return ResponseEntity.ok(withdrawals);
    }

    @Operation(summary = "List all withdrawals with pagination (Admin only)",
            responses = {
                @ApiResponse(responseCode = "200", description = "Withdrawal page returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/withdrawals")
    public ResponseEntity<Page<Withdrawal>> getAllWithdrawals(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by("timestamp").ascending()
                : Sort.by("timestamp").descending();
        return ResponseEntity.ok(withdrawalService.getAllWithdrawals(PageRequest.of(page, size, sort)));
    }
}