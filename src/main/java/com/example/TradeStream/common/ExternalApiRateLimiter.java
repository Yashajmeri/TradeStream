package com.example.TradeStream.common;

import com.example.TradeStream.cryptoCoinService.exception.APIException;
import com.example.TradeStream.marketService.exception.MarketDataApiException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Global rate limiter for outbound calls to third-party APIs.
 *
 * Free-tier quotas:
 *   CoinGecko   → 30 calls / minute
 *   Twelve Data → 8  calls / minute
 *
 * Both buckets are shared across all in-flight requests (global, not per-user).
 */
@Component
public class ExternalApiRateLimiter {

    private final Bucket coinGeckoBucket = Bucket.builder()
            .addLimit(Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1))))
            .build();

    private final Bucket twelveDataBucket = Bucket.builder()
            .addLimit(Bandwidth.classic(8, Refill.greedy(8, Duration.ofMinutes(1))))
            .build();

    /**
     * Call before every CoinGecko HTTP request.
     * Throws APIException (mapped to 400) if the quota is exhausted.
     */
    public void acquireCoinGecko() {
        if (!coinGeckoBucket.tryConsume(1)) {
            throw new APIException(
                    "CoinGecko rate limit reached. Please wait a moment and try again.");
        }
    }

    /**
     * Call before every Twelve Data HTTP request.
     * Throws MarketDataApiException (mapped to 400) if the quota is exhausted.
     */
    public void acquireTwelveData() {
        if (!twelveDataBucket.tryConsume(1)) {
            throw new MarketDataApiException(
                    "Market data rate limit reached. Please wait a moment and try again.");
        }
    }
}
