package com.example.TradeStream.marketService.controller;

import com.example.TradeStream.marketService.dto.CandleResponse;
import com.example.TradeStream.marketService.dto.MarketStatusResponse;
import com.example.TradeStream.marketService.dto.QuoteResponse;
import com.example.TradeStream.marketService.dto.StockResponse;
import com.example.TradeStream.marketService.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
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

    // GET /api/market/stocks
    @GetMapping("/stocks")
    public ResponseEntity<List<StockResponse>> getAllStocks() {
        return ResponseEntity.ok(marketService.getAllStocks());
    }

    // GET /api/market/stocks/{symbol}
    @GetMapping("/stocks/{symbol}")
    public ResponseEntity<StockResponse> getStock(
            @PathVariable String symbol) {
        return ResponseEntity.ok(marketService.getStock(symbol));
    }

    // GET /api/market/stocks/search?query=apple
    @GetMapping("/stocks/search")
    public ResponseEntity<List<StockResponse>> searchStocks(
            @RequestParam String query) {
        return ResponseEntity.ok(marketService.searchStocks(query));
    }

    // GET /api/market/stocks/{symbol}/quote
    @GetMapping("/stocks/{symbol}/quote")
    public ResponseEntity<QuoteResponse> getQuote(
            @PathVariable String symbol) {
        return ResponseEntity.ok(marketService.getQuote(symbol));
    }

    // GET /api/market/stocks/{symbol}/candles?resolution=D&from=...&to=...
    @GetMapping("/stocks/{symbol}/candles")
    public ResponseEntity<CandleResponse> getCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "D") String resolution,
            @RequestParam long from,
            @RequestParam long to) {
        return ResponseEntity.ok(
                marketService.getCandles(symbol, resolution, from, to));
    }

    // GET /api/market/status
    @GetMapping("/status")
    public ResponseEntity<MarketStatusResponse> getMarketStatus() {
        return ResponseEntity.ok(marketService.getMarketStatus());
    }

    // POST /api/admin/market/stocks?symbol=TSLA
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/stocks")
    public ResponseEntity<StockResponse> addStock(@RequestParam String symbol) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketService.addStock(symbol));
    }
}
