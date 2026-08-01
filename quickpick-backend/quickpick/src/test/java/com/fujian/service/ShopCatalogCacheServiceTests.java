package com.fujian.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fujian.config.RedisFeatureProperties;
import com.fujian.pojo.Shop;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopCatalogCacheServiceTests {

    @Test
    void readFailureShouldFallbackToLoaderOnce() {
        TestContext context = new TestContext();
        when(context.values.get(anyString())).thenThrow(new IllegalStateException("Redis unavailable"));
        Shop expected = shop(101L);
        AtomicInteger loads = new AtomicInteger();

        Shop actual = context.service.getShop(101L, () -> {
            loads.incrementAndGet();
            return expected;
        });

        assertSame(expected, actual);
        assertEquals(1, loads.get());
    }

    @Test
    void writeFailureShouldReturnLoadedValueWithoutReloading() throws Exception {
        TestContext context = new TestContext();
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(context.redissonProvider.getIfAvailable()).thenReturn(redisson);
        when(redisson.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(200, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(context.values.get(anyString())).thenReturn(null);
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(context.values).set(anyString(), anyString(), any());
        Shop expected = shop(102L);
        AtomicInteger loads = new AtomicInteger();

        Shop actual = context.service.getShop(102L, () -> {
            loads.incrementAndGet();
            return expected;
        });

        assertSame(expected, actual);
        assertEquals(1, loads.get());
        verify(lock).unlock();
    }

    private static Shop shop(long id) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName("Fallback shop");
        shop.setVisible(1);
        return shop;
    }

    private static class TestContext {
        private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        private final ValueOperations<String, String> values = mock(ValueOperations.class);
        @SuppressWarnings("unchecked")
        private final ObjectProvider<RedissonClient> redissonProvider = mock(ObjectProvider.class);
        private final ShopCatalogCacheService service;

        private TestContext() {
            RedisFeatureProperties properties = new RedisFeatureProperties();
            properties.setEnabled(true);
            properties.setCacheEnabled(true);
            RedisKeyService keyService = mock(RedisKeyService.class);
            when(keyService.key(anyString())).thenAnswer(invocation -> "quickpick:test:v6:" + invocation.getArgument(0));
            when(redisTemplate.opsForValue()).thenReturn(values);
            service = new ShopCatalogCacheService(redisTemplate, new ObjectMapper(), redissonProvider,
                    properties, keyService, new SimpleMeterRegistry());
        }
    }
}
