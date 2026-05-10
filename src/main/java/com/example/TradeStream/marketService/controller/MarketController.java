package com.example.TradeStream.marketService.controller;

import com.example.TradeStream.marketService.dto.CandleResponse;
import com.example.TradeStream.marketService.dto.MarketStatusResponse;
import com.example.TradeStream.marketService.dto.QuoteResponse;
import com.example.TradeStream.marketService.dto.StockResponse;
import com.example.TradeStream.marketService.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Market Data", description = "Stock quotes, candlestick data, and market status via Twelve Data API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @Operation(summary = "Get all stocks", description = "Returns the full list of tracked stocks",
            responses = {
                @ApiResponse(responseCode = "200", description = "List of stocks returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/stocks")
    public ResponseEntity<List<StockResponse>> getAllStocks() {
        return ResponseEntity.ok(marketService.getAllStocks());
    }

    @Operation(summary = "Get stock by symbol",
            responses = {
                @ApiResponse(responseCode = "200", description = "Stock found"),
                @ApiResponse(responseCode = "404", description = "Symbol not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/stocks/{symbol}")
    public ResponseEntity<StockResponse> getStock(
            @Parameter(description = "Stock ticker symbol, e.g. AAPL") @PathVariable String symbol) {
        return ResponseEntity.ok(marketService.getStock(symbol));
    }

    @Operation(summary = "Search stocks by name or symbol",
            responses = {
                @ApiResponse(responseCode = "200", description = "Matching stocks returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/stocks/search")
    public ResponseEntity<List<StockResponse>> searchStocks(
            @Parameter(description = "Search keyword, e.g. apple") @RequestParam String query) {
        return ResponseEntity.ok(marketService.searchStocks(query));
    }

    @Operation(summary = "Get real-time quote for a stock",
            responses = {
                @ApiResponse(responseCode = "200", description = "Quote returned"),
                @ApiResponse(responseCode = "404", description = "Symbol not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/stocks/{symbol}/quote")
    public ResponseEntity<QuoteResponse> getQuote(
            @Parameter(description = "Stock ticker symbol") @PathVariable String symbol) {
        return ResponseEntity.ok(marketService.getQuote(symbol));
    }

    @Operation(summary = "Get candlestick (OHLCV) data for a stock",
            description = "Returns OHLCV candles for the given symbol and time range. " +
                          "`from` and `to` are Unix timestamps (seconds). " +
                          "`resolution` supports: 1, 5, 15, 30, 60 (minutes), D, W, M.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Candle data returned"),
                @ApiResponse(responseCode = "404", description = "Symbol not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/stocks/{symbol}/candles")
    public ResponseEntity<CandleResponse> getCandles(
            @Parameter(description = "Stock ticker symbol") @PathVariable String symbol,
            @Parameter(description = "Candle resolution: 1, 5, 15, 30, 60, D, W, M") @RequestParam(defaultValue = "D") String resolution,
            @Parameter(description = "Start time as Unix timestamp (seconds)") @RequestParam long from,
            @Parameter(description = "End time as Unix timestamp (seconds)") @RequestParam long to) {
        return ResponseEntity.ok(
                marketService.getCandles(symbol, resolution, from, to));
    }

    @Operation(summary = "Get current market open/close status",
            responses = {
                @ApiResponse(responseCode = "200", description = "Market status returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/status")
    public ResponseEntity<MarketStatusResponse> getMarketStatus() {
        return ResponseEntity.ok(marketService.getMarketStatus());
    }

    @Operation(summary = "Add a stock to the platform (Admin only)",
            description = "Fetches stock metadata from Twelve Data and persists it. Requires ADMIN role.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Stock added successfully"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/stocks")
    public ResponseEntity<StockResponse> addStock(
            @Parameter(description = "Stock ticker symbol to add, e.g. TSLA") @RequestParam String symbol) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketService.addStock(symbol));
    }
}
