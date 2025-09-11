package com.devluanmarcene.NextBusRealTimeTracker.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.devluanmarcene.NextBusRealTimeTracker.helpers.HttpHelper;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class IpRateLimiterConfig extends OncePerRequestFilter {

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().equals("/api/bustrack/routes");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = HttpHelper.getIp(request);

        RateLimiter rateLimiter = limiters.computeIfAbsent(clientIp, ip -> {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(1)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            return RateLimiter.of(ip, config);
        });

        try {
            RateLimiter.decorateRunnable(rateLimiter, () -> {
            }).run();
            filterChain.doFilter(request, response);
        } catch (RequestNotPermitted ex) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many request, wait at least 5 seconds to try again.");
        }
    }

}
