package com.fujian.service;

import com.fujian.pojo.Shop;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "quickpick.redis.enabled=true",
        "quickpick.redis.cache-enabled=true",
        "quickpick.redis.idempotency-enabled=true",
        "quickpick.redis.scheduler-lock-enabled=true",
        "management.server.port=0"
})
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION_TESTS", matches = "true")
class RedisInfrastructureIntegrationTests {

    @Autowired
    private ShopCatalogCacheService cacheService;

    @Autowired
    private OrderSubmissionIdempotencyService idempotencyService;

    @Autowired
    private DistributedJobLockExecutor jobLockExecutor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanTestKeys() {
        Set<String> keys = redisTemplate.keys("quickpick:dev:v6:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void cacheShouldLoadOnceAndThenHitRedis() {
        long shopId = System.nanoTime();
        AtomicInteger loads = new AtomicInteger();
        Shop first = cacheService.getShop(shopId, () -> createShop(shopId, loads.incrementAndGet()));
        Shop second = cacheService.getShop(shopId, () -> createShop(shopId, loads.incrementAndGet()));

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(1, loads.get());
        assertEquals(first.getName(), second.getName());
    }

    @Test
    void idempotencyShouldMoveFromNewToProcessingAndSuccess() {
        String requestId = UUID.randomUUID().toString();
        OrderSubmissionIdempotencyService.Claim first = idempotencyService.claim(90001L, requestId);
        OrderSubmissionIdempotencyService.Claim second = idempotencyService.claim(90001L, requestId);

        assertEquals(OrderSubmissionIdempotencyService.ClaimState.NEW, first.state());
        assertEquals(OrderSubmissionIdempotencyService.ClaimState.PROCESSING, second.state());

        AtomicReference<String> orderId = new AtomicReference<>("TEST-ORDER-001");
        idempotencyService.bindToTransaction(first, orderId);

        OrderSubmissionIdempotencyService.Claim third = idempotencyService.claim(90001L, requestId);
        assertEquals(OrderSubmissionIdempotencyService.ClaimState.SUCCESS, third.state());
        assertEquals("TEST-ORDER-001", third.orderId());
    }

    @Test
    void distributedJobLockShouldAllowOnlyOneConcurrentExecutor() throws Exception {
        String scope = UUID.randomUUID().toString();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> jobLockExecutor.execute("integration-test", scope, () -> {
                entered.countDown();
                try {
                    release.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertTrue(entered.await(2, TimeUnit.SECONDS));

            Future<Boolean> second = executor.submit(() -> jobLockExecutor.execute("integration-test", scope, () -> { }));
            assertFalse(second.get(2, TimeUnit.SECONDS));
            release.countDown();
            assertTrue(first.get(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private Shop createShop(long shopId, int loadNumber) {
        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setName("Redis test shop " + loadNumber);
        shop.setVisible(1);
        return shop;
    }
}
