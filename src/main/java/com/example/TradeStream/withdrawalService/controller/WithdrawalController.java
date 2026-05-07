package com.example.TradeStream.withdrawalService.controller;

import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import com.example.TradeStream.withdrawalService.entity.Withdrawal;
import com.example.TradeStream.withdrawalService.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Request a withdrawal — deducts amount from wallet immediately, status starts as PENDING")
    @PostMapping("/withdrawals/{amount}")
    public ResponseEntity<Withdrawal> requestWithdrawal(@PathVariable BigDecimal amount,
                                                        Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUserName(username);

        Withdrawal withdrawal = withdrawalService.requestWithdrawal(user, amount.longValue());

        return ResponseEntity.ok(withdrawal);
    }

    @Operation(summary = "Approve or reject a pending withdrawal (Admin only) — rejection refunds the wallet")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/withdrawals/{withdrawalId}/process/{accept}")
    public ResponseEntity<Withdrawal> processWithdrawal(@PathVariable Long withdrawalId,
                                                        @PathVariable boolean accept) {
        Withdrawal processedWithdrawal = withdrawalService.processWithdrawal(withdrawalId, accept);
        return ResponseEntity.ok(processedWithdrawal);
    }

    @Operation(summary = "Get withdrawal history for the authenticated user")
    @GetMapping("/withdrawals/history")
    public ResponseEntity<List<Withdrawal>> getUserWithdrawalHistory(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUserName(username);

        List<Withdrawal> withdrawals = withdrawalService.getUserWithdrawalHistory(user);

        return ResponseEntity.ok(withdrawals);
    }

    @Operation(summary = "List all withdrawals with pagination (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/withdrawals")
    public ResponseEntity<Page<Withdrawal>> getAllWithdrawals(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by("timestamp").ascending()
                : Sort.by("timestamp").descending();
        return ResponseEntity.ok(withdrawalService.getAllWithdrawals(PageRequest.of(page, size, sort)));
    }
}