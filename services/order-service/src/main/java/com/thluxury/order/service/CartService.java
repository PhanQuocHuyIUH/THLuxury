package com.thluxury.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cart lưu trong Redis Hash `cart:{userId}` field = productId, value = qty (string).
 * GUEST cart sống ở FE localStorage; chỉ khi login mới sync lên đây.
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final StringRedisTemplate redis;
    private final long ttlSeconds;

    public CartService(StringRedisTemplate redis,
                       @Value("${thluxury.order.cart-ttl-seconds:1209600}") long ttlSeconds) {
        this.redis = redis;
        this.ttlSeconds = ttlSeconds;
    }

    private String key(UUID userId) { return "cart:" + userId; }

    public Map<UUID, Integer> get(UUID userId) {
        HashOperations<String, Object, Object> hops = redis.opsForHash();
        Map<Object, Object> raw = hops.entries(key(userId));
        Map<UUID, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> e : raw.entrySet()) {
            try {
                out.put(UUID.fromString(e.getKey().toString()),
                        Integer.parseInt(e.getValue().toString()));
            } catch (Exception ignore) { /* skip malformed */ }
        }
        return out;
    }

    public void setItem(UUID userId, UUID productId, int qty) {
        if (qty <= 0) {
            removeItem(userId, productId);
            return;
        }
        redis.opsForHash().put(key(userId), productId.toString(), String.valueOf(qty));
        redis.expire(key(userId), Duration.ofSeconds(ttlSeconds));
    }

    public void removeItem(UUID userId, UUID productId) {
        redis.opsForHash().delete(key(userId), productId.toString());
    }

    public void clear(UUID userId) { redis.delete(key(userId)); }

    /** Merge cart từ FE (GUEST → login). Cộng dồn số lượng. */
    public Map<UUID, Integer> sync(UUID userId, Map<UUID, Integer> incoming) {
        if (incoming == null) incoming = new HashMap<>();
        Map<UUID, Integer> existing = get(userId);
        for (Map.Entry<UUID, Integer> e : incoming.entrySet()) {
            int q = Math.max(1, e.getValue() == null ? 0 : e.getValue());
            existing.merge(e.getKey(), q, Integer::sum);
        }
        // persist
        if (existing.isEmpty()) {
            clear(userId);
            return existing;
        }
        Map<String, String> raw = new LinkedHashMap<>();
        existing.forEach((k, v) -> raw.put(k.toString(), String.valueOf(v)));
        redis.opsForHash().putAll(key(userId), new LinkedHashMap<>(raw));
        redis.expire(key(userId), Duration.ofSeconds(ttlSeconds));
        return existing;
    }
}
