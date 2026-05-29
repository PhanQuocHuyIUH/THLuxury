package com.thluxury.catalog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Cache-Aside cho product list & detail trong Redis.
 *   cat:list:{hash(query)}  -> JSON danh sách
 *   cat:detail:{id}         -> JSON chi tiết
 * TTL: 10 phút.
 */
@Component
public class ProductCache {

    private static final Logger log = LoggerFactory.getLogger(ProductCache.class);

    public  static final String LIST_PREFIX   = "cat:list:";
    public  static final String DETAIL_PREFIX = "cat:detail:";
    public  static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    public ProductCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    public void put(String key, String json) {
        redis.opsForValue().set(key, json, TTL);
    }

    public void invalidateDetail(UUID productId) {
        redis.delete(DETAIL_PREFIX + productId);
    }

    public void invalidateAllLists() {
        Set<String> keys = redis.keys(LIST_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
            log.debug("Invalidated {} list cache entries", keys.size());
        }
    }
}
