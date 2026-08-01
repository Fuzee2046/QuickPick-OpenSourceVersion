package com.fujian.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "quickpick.redis.enabled=true",
        "quickpick.redis.cache-enabled=false",
        "quickpick.redis.idempotency-enabled=false",
        "quickpick.redis.scheduler-lock-enabled=false",
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=6399",
        "management.server.port=0"
})
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_FAILURE_TESTS", matches = "true")
class RedisStartupWithoutServerTests {

    @Test
    void applicationContextStartsBeforeRedisIsReachable() {
        assertNotNull(System.getProperty("java.version"));
    }
}
