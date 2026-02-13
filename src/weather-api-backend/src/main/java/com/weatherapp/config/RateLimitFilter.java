package com.weatherapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory rate limiter that tracks requests per IP per minute.
 * Returns 429 Too Many Requests when the limit is exceeded.
 * This is a bonus feature for the assessment.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        RateLimitBucket bucket = buckets.computeIfAbsent(clientIp, k -> new RateLimitBucket());

        if (bucket.tryConsume(requestsPerMinute)) {
            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(requestsPerMinute - bucket.getCount()));
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "timestamp": "%s",
                    "status": 429,
                    "error": "Too Many Requests",
                    "errorCode": "RATE_LIMIT_EXCEEDED",
                    "message": "Rate limit exceeded. Maximum %d requests per minute."
                }
                """.formatted(java.time.Instant.now(), requestsPerMinute));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Simple sliding-window-ish bucket per IP */
    private static class RateLimitBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        public boolean tryConsume(int limit) {
            long now = System.currentTimeMillis();
            long start = windowStart.get();

            // Reset window every minute
            if (now - start > 60_000) {
                windowStart.set(now);
                count.set(0);
            }

            return count.incrementAndGet() <= limit;
        }

        public int getCount() {
            return count.get();
        }
    }
}
