package com.fujian.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fujian.config.RedisFeatureProperties;
import com.fujian.pojo.ReservationRuleConfig;
import com.fujian.pojo.Shop;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class ShopCatalogCacheService {
    private static final Logger log = LoggerFactory.getLogger(ShopCatalogCacheService.class);
    private static final String NULL_VALUE = "__QUICKPICK_NULL__";
    private static final Duration NULL_TTL = Duration.ofSeconds(30);
    private static final Duration CATALOG_TTL = Duration.ofDays(1);
    private static final Duration CATALOG_TTL_JITTER = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<RedissonClient> redissonProvider;
    private final RedisFeatureProperties properties;
    private final RedisKeyService keyService;
    private final MeterRegistry meterRegistry;

    public ShopCatalogCacheService(StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper,
                                   ObjectProvider<RedissonClient> redissonProvider,
                                   RedisFeatureProperties properties,
                                   RedisKeyService keyService,
                                   MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redissonProvider = redissonProvider;
        this.properties = properties;
        this.keyService = keyService;
        this.meterRegistry = meterRegistry;
    }

    public Shop getShop(Long shopId, Supplier<Shop> loader) {
        return readThrough(
                "shop",
                keyService.key("cache:shop:" + shopId),
                objectMapper.getTypeFactory().constructType(Shop.class),
                CATALOG_TTL,
                CATALOG_TTL_JITTER,
                loader
        );
    }

    public List<Shop> getShopList(Supplier<List<Shop>> loader) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, Shop.class);
        return readThrough("shop-list", keyService.key("cache:shop-list:all"), type,
                CATALOG_TTL, CATALOG_TTL_JITTER, loader);
    }

    public List<Map<String, Object>> getMenu(Long shopId, Supplier<List<Map<String, Object>>> loader) {
        JavaType type = objectMapper.getTypeFactory().constructType(new TypeReference<List<Map<String, Object>>>() { });
        return readThrough("menu", keyService.key("cache:menu:" + shopId), type,
                CATALOG_TTL, CATALOG_TTL_JITTER, loader);
    }

    public Map<String, Object> getWeightMenu(Long shopId, Supplier<Map<String, Object>> loader) {
        JavaType type = objectMapper.getTypeFactory().constructType(new TypeReference<Map<String, Object>>() { });
        return readThrough("weight-menu", keyService.key("cache:weight-menu:" + shopId), type,
                CATALOG_TTL, CATALOG_TTL_JITTER, loader);
    }

    public ReservationRuleConfig getReservationRule(Supplier<ReservationRuleConfig> loader) {
        return readThrough("reservation-rule", keyService.key("cache:reservation-rule"),
                objectMapper.getTypeFactory().constructType(ReservationRuleConfig.class),
                CATALOG_TTL, CATALOG_TTL_JITTER, loader);
    }

    public void evictShop(Long shopId) {
        if (shopId == null || !properties.isCacheEnabled()) {
            return;
        }
        deleteKeys(keyService.key("cache:shop:" + shopId), keyService.key("cache:menu:" + shopId),
                keyService.key("cache:weight-menu:" + shopId), keyService.key("cache:shop-list:all"));
    }

    public void evictMenu(Long shopId) {
        if (shopId == null || !properties.isCacheEnabled()) {
            return;
        }
        deleteKeys(keyService.key("cache:menu:" + shopId), keyService.key("cache:weight-menu:" + shopId));
    }

    public void evictReservationRule() {
        if (properties.isCacheEnabled()) {
            deleteKeys(keyService.key("cache:reservation-rule"));
        }
    }

    private <T> T readThrough(String cacheName, String key, JavaType type,
                              Duration baseTtl, Duration jitter, Supplier<T> loader) {
        if (!properties.isCacheEnabled()) {
            return loader.get();
        }

        try {
            CacheLookup<T> firstLookup = lookup(cacheName, key, type);
            if (firstLookup.found()) {
                return firstLookup.value();
            }
        } catch (RedisCacheAccessException redisFailure) {
            return fallback(cacheName, loader, redisFailure);
        }

        RedissonClient redisson = redissonProvider.getIfAvailable();
        if (redisson == null) {
            record(cacheName, "fallback");
            return loader.get();
        }

        RLock lock = null;
        boolean acquired = false;
        try {
            lock = redisson.getLock(keyService.key("lock:cache-rebuild:" + Integer.toHexString(key.hashCode())));
            acquired = lock.tryLock(200, TimeUnit.MILLISECONDS);
            if (!acquired) {
                CacheLookup<T> retryLookup = lookup(cacheName, key, type);
                return retryLookup.found() ? retryLookup.value() : loader.get();
            }

            CacheLookup<T> secondLookup = lookup(cacheName, key, type);
            if (secondLookup.found()) {
                return secondLookup.value();
            }

            long start = System.nanoTime();
            T loaded = loader.get();
            meterRegistry.timer("quickpick.cache.load", "cache", cacheName)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            try {
                write(key, loaded, baseTtl, jitter);
            } catch (Exception redisFailure) {
                record(cacheName, "fallback");
                log.warn("Redis cache write failed, cache={}, returning database result: {}",
                        cacheName, redisFailure.getMessage());
            }
            return loaded;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            record(cacheName, "fallback");
            return loader.get();
        } catch (RedisCacheAccessException redisFailure) {
            return fallback(cacheName, loader, redisFailure);
        } catch (org.redisson.client.RedisException redisFailure) {
            return fallback(cacheName, loader, redisFailure);
        } finally {
            if (acquired && lock != null) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception unlockFailure) {
                    meterRegistry.counter("quickpick.redis.failures", "operation", "cache-unlock").increment();
                    log.warn("Redis cache lock release failed, cache={}: {}", cacheName, unlockFailure.getMessage());
                }
            }
        }
    }

    private <T> T fallback(String cacheName, Supplier<T> loader, Exception redisFailure) {
        record(cacheName, "fallback");
        log.warn("Redis cache operation failed, cache={}, fallback to database: {}",
                cacheName, redisFailure.getMessage());
        return loader.get();
    }

    private <T> CacheLookup<T> lookup(String cacheName, String key, JavaType type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                record(cacheName, "miss");
                return CacheLookup.miss();
            }
            if (NULL_VALUE.equals(json)) {
                record(cacheName, "null-hit");
                return CacheLookup.hit(null);
            }
            record(cacheName, "hit");
            return CacheLookup.hit(objectMapper.readValue(json, type));
        } catch (Exception redisFailure) {
            throw new RedisCacheAccessException(redisFailure);
        }
    }

    private void write(String key, Object value, Duration baseTtl, Duration jitter) throws Exception {
        if (value == null) {
            redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_TTL);
            return;
        }
        long jitterSeconds = Math.max(0, jitter.toSeconds());
        long ttlSeconds = baseTtl.toSeconds() + (jitterSeconds == 0
                ? 0
                : ThreadLocalRandom.current().nextLong(jitterSeconds + 1));
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), Duration.ofSeconds(ttlSeconds));
    }

    private void deleteKeys(String... keys) {
        try {
            redisTemplate.delete(List.of(keys));
        } catch (Exception redisFailure) {
            meterRegistry.counter("quickpick.redis.failures", "operation", "cache-evict").increment();
            log.warn("Redis cache eviction failed: {}", redisFailure.getMessage());
        }
    }

    private void record(String cacheName, String result) {
        meterRegistry.counter("quickpick.cache.requests", "cache", cacheName, "result", result).increment();
    }

    private record CacheLookup<T>(boolean found, T value) {
        private static <T> CacheLookup<T> hit(T value) {
            return new CacheLookup<>(true, value);
        }

        private static <T> CacheLookup<T> miss() {
            return new CacheLookup<>(false, null);
        }
    }

    private static class RedisCacheAccessException extends RuntimeException {
        RedisCacheAccessException(Throwable cause) {
            super(cause);
        }
    }
}
