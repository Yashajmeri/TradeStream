package com.example.TradeStream.streamingService.controller;

import com.example.TradeStream.streamingService.service.BinanceStreamService;
import io.swagger.v3.oas.annotations.Operation;
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

    /**
     * Subscribe to a coin's real-time kline stream.
     * Frontend then connects via WebSocket and subscribes to /topic/stream/{symbol}
     *
     * POST /api/stream/subscribe?symbol=btcusdt&interval=1m
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1m") String interval) {

        binanceStreamService.subscribe(symbol.toLowerCase(), interval);
        return ResponseEntity.ok(Map.of(
                "message", "Subscribed to " + symbol.toUpperCase() + " [" + interval + "]",
                "topic", "/topic/stream/" + symbol.toLowerCase()
        ));
    }

    /**
     * Stop streaming a coin.
     *
     * DELETE /api/stream/unsubscribe?symbol=btcusdt
     */
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestParam String symbol) {
        binanceStreamService.unsubscribe(symbol.toLowerCase());
        return ResponseEntity.ok(Map.of("message", "Unsubscribed from " + symbol.toUpperCase()));
    }

    /**
     * List all currently active streams.
     *
     * GET /api/stream/active
     */
    @GetMapping("/active")
    public ResponseEntity<Set<String>> activeStreams() {
        return ResponseEntity.ok(binanceStreamService.getActiveStreams());
    }
}
