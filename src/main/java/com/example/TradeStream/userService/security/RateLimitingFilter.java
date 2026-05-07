package com.example.TradeStream.userService.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiting filter using the token-bucket algorithm (Bucket4j).
 *
 * Rules (all per client IP):
 *   /auth/signin      →  5  requests / 1  minute   (brute-force protection)
 *   /auth/signup      →  3  requests / 10 minutes  (registration spam)
 *   /api/payment/**   →  5  requests / 1  minute   (payment abuse)
 *   /api/orders/**    →  10 requests / 1  minute   (order spam)
 *   /api/**  (rest)   →  100 requests / 1 minute   (general API)
 *
 * NOTE: This uses an in-memory ConcurrentHashMap.
 * In production with multiple instances, replace with Bucket4j + Redis:
 *   https://github.com/bucket4j/bucket4j#bucket4j-redis
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    // key = "clientIp:bucketType"
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String bucketType = resolveBucketType(uri);
        String key = getClientIp(request) + ":" + bucketType;

        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(bucketType));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            sendRateLimitExceededResponse(response, bucketType);
        }
    }

    // ── Bucket type resolution ────────────────────────────────────────────────

    private String resolveBucketType(String uri) {
        if (uri.startsWith("/auth/signin"))   return "auth_signin";
        if (uri.startsWith("/auth/signup"))   return "auth_signup";
        if (uri.startsWith("/api/payment"))   return "api_payment";
        if (uri.startsWith("/api/orders"))    return "api_orders";
        return "api_general";
    }

    private Bucket createBucket(String type) {
        return switch (type) {
            case "auth_signin"  -> Bucket.builder()
                    .addLimit(Bandwidth.classic(5,  Refill.greedy(5,  Duration.ofMinutes(1))))
                    .build();
            case "auth_signup"  -> Bucket.builder()
                    .addLimit(Bandwidth.classic(3,  Refill.greedy(3,  Duration.ofMinutes(10))))
                    .build();
            case "api_payment"  -> Bucket.builder()
                    .addLimit(Bandwidth.classic(5,  Refill.greedy(5,  Duration.ofMinutes(1))))
                    .build();
            case "api_orders"   -> Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                    .build();
            default             -> Bucket.builder()
                    .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                    .build();
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain a chain: "client, proxy1, proxy2"
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response,
                                               String bucketType) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = objectMapper.writeValueAsString(Map.of(
                "message", "Too many requests. Please slow down and try again shortly.",
                "success", false
        ));
        response.getWriter().write(body);
    }
}
