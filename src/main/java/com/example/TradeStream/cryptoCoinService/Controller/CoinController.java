package com.example.TradeStream.cryptoCoinService.Controller;

import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.cryptoCoinService.service.CoinService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Crypto Coins", description = "CoinGecko market data — prices, charts, search, trending")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coins")
public class CoinController {
    private final CoinService coinService;

    @Operation(summary = "Get all coins (paginated)", description = "Returns page N of coins ordered by market cap rank (50 coins per page).",
            responses = {
                @ApiResponse(responseCode = "200", description = "Coin list returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping
    ResponseEntity<List<Coin>> getAllCoins(
            @Parameter(description = "Page number, starting from 1") @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(coinService.getAllCoins(page));
    }

    @Operation(summary = "Get market chart data for a coin",
            description = "Returns price, market cap, and volume history for the given number of days.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Chart data returned"),
                @ApiResponse(responseCode = "404", description = "Coin not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/{coinId}/chart")
    ResponseEntity<JsonNode> getMarketChart(
            @Parameter(description = "CoinGecko coin ID, e.g. bitcoin") @PathVariable String coinId,
            @Parameter(description = "Number of days of history to return (1, 7, 14, 30, 90, 180, 365)") @RequestParam(defaultValue = "7") int days) {
        JsonNode response = coinService.getMarketChart(coinId, days);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get coin by ID",
            responses = {
                @ApiResponse(responseCode = "200", description = "Coin returned"),
                @ApiResponse(responseCode = "404", description = "Coin not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/{coinId}")
    public ResponseEntity<Coin> getCoinById(
            @Parameter(description = "CoinGecko coin ID, e.g. bitcoin") @PathVariable String coinId) {
        return ResponseEntity.ok(coinService.getCoinById(coinId));
    }

    @Operation(summary = "Get detailed coin info by ID",
            description = "Returns the full CoinGecko details payload including links, description, and community data.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Coin details returned"),
                @ApiResponse(responseCode = "404", description = "Coin not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("details/{coinId}")
    public ResponseEntity<JsonNode> getCoinDetailsById(
            @Parameter(description = "CoinGecko coin ID, e.g. bitcoin") @PathVariable String coinId) {
        return ResponseEntity.ok(coinService.getCoinDetails(coinId));
    }

    @Operation(summary = "Search coins by name or symbol",
            responses = {
                @ApiResponse(responseCode = "200", description = "Search results returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/search")
    public ResponseEntity<JsonNode> searchCoins(
            @Parameter(description = "Search keyword, e.g. eth or ethereum") @RequestParam String query) {
        return ResponseEntity.ok(coinService.searchCoins(query));
    }

    @Operation(summary = "Get top 50 coins by market cap rank",
            responses = {
                @ApiResponse(responseCode = "200", description = "Top 50 coins returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/top50")
    public ResponseEntity<JsonNode> getTop50CoinsByMarketCapRank() {
        return ResponseEntity.ok(coinService.getTop50CoinsByMarketCapRank());
    }

    @Operation(summary = "Get currently trending coins",
            description = "Returns coins trending on CoinGecko in the last 24 hours.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Trending coins returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/trending")
    public ResponseEntity<JsonNode> getTrendingCoins() {
        return ResponseEntity.ok(coinService.getTrendingCoins());
    }

}
