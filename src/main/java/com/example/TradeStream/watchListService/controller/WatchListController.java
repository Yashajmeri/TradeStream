package com.example.TradeStream.watchListService.controller;


import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.cryptoCoinService.service.CoinService;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import com.example.TradeStream.watchListService.entity.WatchList;
import com.example.TradeStream.watchListService.service.WatchListService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Get the user's watchlist — auto-creates one if it doesn't exist")
    @GetMapping("/watchlist/user")
    public ResponseEntity<WatchList> getWatchListForCurrentUser(Authentication authentication) {
        User user = userService.getUserByUserName(authentication.getName());
        return ResponseEntity.ok(watchListService.getOrCreateWatchListByUser(user));
    }

    // USER: explicitly create a new watchlist
    @PostMapping("/watchlist/create")
    public ResponseEntity<WatchList> createWatchListForCurrentUser(Authentication authentication) {
        User user = userService.getUserByUserName(authentication.getName());
        return ResponseEntity.ok(watchListService.createWatchListForUser(user));
    }

    // ADMIN: look up any user's watchlist by its database ID
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/watchlist/{watchListId}")
    public ResponseEntity<WatchList> getWatchListById(@PathVariable Long watchListId) {
        return ResponseEntity.ok(watchListService.findWatchListById(watchListId));
    }

    @Operation(summary = "Add a coin to the user's watchlist")
    @PatchMapping("/watchlist/add/coin/{coinId}")
    public ResponseEntity<WatchList> addCoinToWatchList(Authentication authentication,
                                                        @PathVariable String coinId) {
        User user = userService.getUserByUserName(authentication.getName());
        Coin coin = coinService.getCoinById(coinId);
        return ResponseEntity.ok(watchListService.addCoinToWatchList(user, coin));
    }

    @Operation(summary = "Remove a coin from the user's watchlist")
    @PatchMapping("/watchlist/remove/coin/{coinId}")
    public ResponseEntity<WatchList> removeCoinFromWatchList(Authentication authentication,
                                                             @PathVariable String coinId) {
        User user = userService.getUserByUserName(authentication.getName());
        Coin coin = coinService.getCoinById(coinId);
        return ResponseEntity.ok(watchListService.removeCoinFromWatchList(user, coin));
    }
}
