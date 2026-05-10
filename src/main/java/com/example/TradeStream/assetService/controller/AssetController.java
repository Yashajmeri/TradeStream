package com.example.TradeStream.assetService.controller;

import com.example.TradeStream.assetService.entity.Asset;
import com.example.TradeStream.assetService.service.AssetService;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Assets", description = "User crypto holdings — auto-managed by the order system")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AssetController {
    private final AssetService assetService;
    private final UserService userService;

    @Operation(summary = "Get any asset by ID (Admin only)",
            responses = {
                @ApiResponse(responseCode = "200", description = "Asset returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
                @ApiResponse(responseCode = "404", description = "Asset not found")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/assets/{assetId}")
    public ResponseEntity<Asset> getAssetById(
            @Parameter(description = "Asset database ID") @PathVariable Long assetId) {
        return ResponseEntity.ok(assetService.getAssetById(assetId));
    }

    @Operation(summary = "Get the user's holding for a specific coin",
            responses = {
                @ApiResponse(responseCode = "200", description = "Asset returned"),
                @ApiResponse(responseCode = "404", description = "Holding not found for this coin"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/assets/coin/{coinId}/user")
    public ResponseEntity<Asset> getAssetByUserIdAndCoinId(Authentication authentication,
            @Parameter(description = "CoinGecko coin ID, e.g. bitcoin") @PathVariable String coinId) {
        User user = userService.getUserByUserName(authentication.getName());
        return ResponseEntity.ok(assetService.findAssetByUserIdAndCoinId(user.getId(), coinId));
    }

    @Operation(summary = "Get all crypto assets held by the authenticated user",
            responses = {
                @ApiResponse(responseCode = "200", description = "Asset list returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/assets")
    public ResponseEntity<List<Asset>> getAssetsByUserId(Authentication authentication) {
        User user = userService.getUserByUserName(authentication.getName());
        return ResponseEntity.ok(assetService.getAssetsByUserId(user.getId()));
    }
}