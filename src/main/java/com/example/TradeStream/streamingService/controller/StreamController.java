package com.example.TradeStream.streamingService.controller;

import com.example.TradeStream.streamingService.service.BinanceStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@Tag(name = "Streaming", description = "Binance WebSocket real-time kline/candle data — clients connect via WebSocket to /topic/stream/{symbol}")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final BinanceStreamService binanceStreamService;

    @Operation(summary = "Subscribe to a real-time kline stream",
            description = "Opens a Binance WebSocket connection for the given symbol. " +
                          "After subscribing, clients connect via STOMP to /topic/stream/{symbol}.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Subscribed — WebSocket topic returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe(
            @Parameter(description = "Binance trading pair, e.g. btcusdt") @RequestParam String symbol,
            @Parameter(description = "Kline interval: 1m, 3m, 5m, 15m, 30m, 1h, 4h, 1d") @RequestParam(defaultValue = "1m") String interval) {

        binanceStreamService.subscribe(symbol.toLowerCase(), interval);
        return ResponseEntity.ok(Map.of(
                "message", "Subscribed to " + symbol.toUpperCase() + " [" + interval + "]",
                "topic", "/topic/stream/" + symbol.toLowerCase()
        ));
    }

    @Operation(summary = "Unsubscribe from a real-time kline stream",
            responses = {
                @ApiResponse(responseCode = "200", description = "Unsubscribed successfully"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(
            @Parameter(description = "Binance trading pair to stop streaming, e.g. btcusdt") @RequestParam String symbol) {
        binanceStreamService.unsubscribe(symbol.toLowerCase());
        return ResponseEntity.ok(Map.of("message", "Unsubscribed from " + symbol.toUpperCase()));
    }

    @Operation(summary = "List all currently active WebSocket streams",
            responses = {
                @ApiResponse(responseCode = "200", description = "Set of active symbol streams returned"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/active")
    public ResponseEntity<Set<String>> activeStreams() {
        return ResponseEntity.ok(binanceStreamService.getActiveStreams());
    }
}
