package com.example.TradeStream.streamingService.service;

import com.example.TradeStream.streamingService.dto.CandleUpdate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceStreamService {

    private static final String BINANCE_WS_BASE = "wss://stream.binance.com:9443/ws/";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // symbol -> active WebSocket connection
    private final Map<String, WebSocket> activeStreams = new ConcurrentHashMap<>();

    /**
     * Subscribe to Binance kline (candle) stream for a symbol and interval.
     * Data is broadcast to /topic/stream/{symbol}
     *
     * @param symbol   e.g. "btcusdt"
     * @param interval e.g. "1m", "5m", "1h", "1d"
     */
    public void subscribe(String symbol, String interval) {
        String key = symbol.toLowerCase();
        if (activeStreams.containsKey(key)) {
            log.info("Already streaming {}", key);
            return;
        }

        String streamName = key + "@kline_" + interval;
        URI uri = URI.create(BINANCE_WS_BASE + streamName);

        httpClient.newWebSocketBuilder()
                .buildAsync(uri, new BinanceListener(key, interval))
                .thenAccept(ws -> {
                    activeStreams.put(key, ws);
                    log.info("Subscribed to Binance stream: {}", streamName);
                })
                .exceptionally(ex -> {
                    log.error("Failed to connect to Binance stream {}: {}", streamName, ex.getMessage());
                    return null;
                });
    }

    /**
     * Stop streaming for a symbol.
     */
    public void unsubscribe(String symbol) {
        String key = symbol.toLowerCase();
        WebSocket ws = activeStreams.remove(key);
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "Unsubscribed").thenRun(() ->
                    log.info("Unsubscribed from stream: {}", key));
        }
    }

    /**
     * Returns currently active stream symbols.
     */
    public Set<String> getActiveStreams() {
        return activeStreams.keySet();
    }

    // -------------------------------------------------------------------------
    // Inner listener — handles messages from Binance
    // -------------------------------------------------------------------------
    private class BinanceListener implements WebSocket.Listener {

        private final String symbol;
        private final String interval;
        private final StringBuilder buffer = new StringBuilder();

        BinanceListener(String symbol, String interval) {
            this.symbol = symbol;
            this.interval = interval;
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                handleMessage(buffer.toString());
                buffer.setLength(0);
            }
            ws.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.info("Binance stream closed for {}: {} {}", symbol, statusCode, reason);
            activeStreams.remove(symbol);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.error("Binance stream error for {}: {}", symbol, error.getMessage());
            activeStreams.remove(symbol);
        }

        private void handleMessage(String raw) {
            try {
                JsonNode root = objectMapper.readTree(raw);
                JsonNode k = root.path("k");
                if (k.isMissingNode()) return;

                CandleUpdate candle = CandleUpdate.builder()
                        .symbol(symbol.toUpperCase())
                        .interval(interval)
                        .openTime(k.path("t").asLong())
                        .closeTime(k.path("T").asLong())
                        .open(k.path("o").asText())
                        .high(k.path("h").asText())
                        .low(k.path("l").asText())
                        .close(k.path("c").asText())
                        .volume(k.path("v").asText())
                        .isClosed(k.path("x").asBoolean())
                        .build();

                messagingTemplate.convertAndSend("/topic/stream/" + symbol, candle);
            } catch (Exception e) {
                log.error("Failed to parse Binance kline message: {}", e.getMessage());
            }
        }
    }
}
