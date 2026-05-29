package com.thluxury.identity.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Store refresh-token state in Redis as a single key per jti.
 *   refresh:{jti}  -> userId  (TTL = remaining lifetime)
 * Revocation = DEL key.
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redis;

    public RefreshTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void store(String jti, UUID userId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) return;
        redis.opsForValue().set(KEY_PREFIX + jti, userId.toString(), ttl);
    }

    public boolean isActive(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
    }

    public void revoke(String jti) {
        redis.delete(KEY_PREFIX + jti);
    }
}
