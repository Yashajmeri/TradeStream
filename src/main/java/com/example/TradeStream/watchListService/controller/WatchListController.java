package com.example.TradeStream.watchListService.controller;


import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.cryptoCoinService.service.CoinService;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import com.example.TradeStream.watchListService.entity.WatchList;
import com.example.TradeStream.watchListService.service.WatchListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Watchlist", description = "Manage the authenticated user's coin watchlist")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WatchListController {
    private final WatchListService watchListService;
    private final UserService userService;
    private final CoinService coinService;

    @Operation(summary = "Get the user's watchlist — auto-creates one if it doesn't exist",
            responses = {
                @ApiResponse(responseCode = "200", description = "Watchlist returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/watchlist/user")
    public ResponseEntity<WatchList> getWatchListForCurrentUser(Authentication authentication) {
        User user = userService.getUserByUserName(authentication.getName());
        return ResponseEntity.ok(watchListService.getOrCreateWatchListByUser(user));
    }

    @Operation(summary = "Explicitly create a new watchlist for the authenticated user",
            responses = {
                @ApiResponse(responseCode = "200", description = "New watchlist created"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PostMapping("/watchlist/create")
    public ResponseEntity<WatchList> createWatchListForCurrentUser(Authentication authentication) {
        User user = userService.getUserByUserName(authentication.getName());
        return ResponseEntity.ok(watchListService.createWatchListForUser(user));
    }

    @Operation(summary = "Get any watchlist by ID (Admin only)",
            responses = {
                @ApiResponse(responseCode = "200", description = "Watchlist returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
                @ApiResponse(responseCode = "404", description = "Watchlist not found")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/watchlist/{watchListId}")
    public ResponseEntity<WatchList> getWatchListById(
            @Parameter(description = "Watchlist database ID") @PathVariable Long watchListId) {
        return ResponseEntity.ok(watchListService.findWatchListById(watchListId));
    }

    @Operation(summary = "Add a coin to the user's watchlist",
            responses = {
                @ApiResponse(responseCode = "200", description = "Updated watchlist returned"),
                @ApiResponse(responseCode = "404", description = "Coin not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PatchMapping("/watchlist/add/coin/{coinId}")
    public ResponseEntity<WatchList> addCoinToWatchList(Authentication authentication,
            @Parameter(description = "CoinGecko coin ID, e.g. bitcoin") @PathVariable String coinId) {
        User user = userService.getUserByUserName(authentication.getName());
        Coin coin = coinService.getCoinById(coinId);
        return ResponseEntity.ok(watchListService.addCoinToWatchList(user, coin));
    }

    @Operation(summary = "Remove a coin from the user's watchlist",
            responses = {
                @ApiResponse(responseCode = "200", description = "Updated watchlist returned"),
                @ApiResponse(responseCode = "404", description = "Coin not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PatchMapping("/watchlist/remove/coin/{coinId}")
    public ResponseEntity<WatchList> removeCoinFromWatchList(Authentication authentication,
            @Parameter(description = "CoinGecko coin ID, e.g. bitcoin") @PathVariable String coinId) {
        User user = userService.getUserByUserName(authentication.getName());
        Coin coin = coinService.getCoinById(coinId);
        return ResponseEntity.ok(watchListService.removeCoinFromWatchList(user, coin));
    }
}
