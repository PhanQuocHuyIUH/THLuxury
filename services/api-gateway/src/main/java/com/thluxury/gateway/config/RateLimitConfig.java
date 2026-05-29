package com.thluxury.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate-limit key: ưu tiên userId (đã được JwtAuthGlobalFilter gắn vào X-User-Id),
 * fallback về IP cho GUEST. Dùng bởi RequestRateLimiter filter trong application.yml.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            var addr = exchange.getRequest().getRemoteAddress();
            String ip = (addr != null && addr.getAddress() != null)
                    ? addr.getAddress().getHostAddress() : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
