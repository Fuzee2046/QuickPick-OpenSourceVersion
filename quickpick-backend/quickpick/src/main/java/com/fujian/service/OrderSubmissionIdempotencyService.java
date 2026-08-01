package com.fujian.service;

import com.fujian.config.RedisFeatureProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrderSubmissionIdempotencyService {
    private static final Logger log = LoggerFactory.getLogger(OrderSubmissionIdempotencyService.class);
    private static final Duration PROCESSING_TTL = Duration.ofSeconds(30);
    private static final Duration SUCCESS_TTL = Duration.ofHours(24);

    private static final DefaultRedisScript<String> CLAIM_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current then
                redis.call('PSETEX', KEYS[1], ARGV[1], 'P:' .. ARGV[2])
                return 'NEW|' .. ARGV[2]
            end
            if string.sub(current, 1, 2) == 'S:' then
                return 'SUCCESS|' .. string.sub(current, 3)
            end
            return 'PROCESSING|'
            """, String.class);

    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current == 'P:' .. ARGV[1] then
                redis.call('PSETEX', KEYS[1], ARGV[2], 'S:' .. ARGV[3])
                return 1
            end
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current == 'P:' .. ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisFeatureProperties properties;
    private final RedisKeyService keyService;
    private final MeterRegistry meterRegistry;

    public OrderSubmissionIdempotencyService(StringRedisTemplate redisTemplate,
                                             RedisFeatureProperties properties,
                                             RedisKeyService keyService,
                                             MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.keyService = keyService;
        this.meterRegistry = meterRegistry;
    }

    public Claim claim(Long userId, String clientRequestId) {
        if (!properties.isIdempotencyEnabled() || userId == null || clientRequestId == null) {
            return Claim.bypass();
        }
        String key = keyService.key("idempotency:order:" + userId + ":" + digest(clientRequestId));
        String token = UUID.randomUUID().toString();
        try {
            String result = redisTemplate.execute(CLAIM_SCRIPT, List.of(key),
                    String.valueOf(PROCESSING_TTL.toMillis()), token);
            Claim claim = parseClaim(key, token, result);
            record(claim.state().metricValue);
            return claim;
        } catch (Exception redisFailure) {
            record("fallback");
            log.warn("Redis order idempotency claim failed, fallback to MySQL: {}", redisFailure.getMessage());
            return Claim.bypass();
        }
    }

    public String awaitCompletedOrderId(Claim claim, Duration timeout) {
        if (claim == null || claim.state() != ClaimState.PROCESSING) {
            return claim == null ? null : claim.orderId();
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                String value = redisTemplate.opsForValue().get(claim.key());
                if (value != null && value.startsWith("S:")) {
                    return value.substring(2);
                }
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception redisFailure) {
                return null;
            }
        }
        return null;
    }

    public void bindToTransaction(Claim claim, AtomicReference<String> committedOrderId) {
        if (claim == null || claim.state() != ClaimState.NEW) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            String orderId = committedOrderId.get();
            if (orderId == null) {
                release(claim);
            } else {
                complete(claim, orderId);
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String orderId = committedOrderId.get();
                if (orderId != null) {
                    complete(claim, orderId);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED || committedOrderId.get() == null) {
                    release(claim);
                }
            }
        });
    }

    public void discardStaleSuccess(Claim claim) {
        if (claim == null || claim.key() == null) {
            return;
        }
        try {
            redisTemplate.delete(claim.key());
        } catch (Exception ignored) {
            // MySQL remains the source of truth when cleanup fails.
        }
    }

    private void complete(Claim claim, String orderId) {
        try {
            redisTemplate.execute(COMPLETE_SCRIPT, List.of(claim.key()), claim.token(),
                    String.valueOf(SUCCESS_TTL.toMillis()), orderId);
        } catch (Exception redisFailure) {
            meterRegistry.counter("quickpick.redis.failures", "operation", "idempotency-complete").increment();
            log.warn("Redis order idempotency completion failed: {}", redisFailure.getMessage());
        }
    }

    private void release(Claim claim) {
        try {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(claim.key()), claim.token());
        } catch (Exception redisFailure) {
            meterRegistry.counter("quickpick.redis.failures", "operation", "idempotency-release").increment();
        }
    }

    private Claim parseClaim(String key, String token, String result) {
        if (result == null) {
            return Claim.bypass();
        }
        int separator = result.indexOf('|');
        String state = separator < 0 ? result : result.substring(0, separator);
        String value = separator < 0 ? "" : result.substring(separator + 1);
        return switch (state) {
            case "NEW" -> new Claim(ClaimState.NEW, key, token, null);
            case "SUCCESS" -> new Claim(ClaimState.SUCCESS, key, null, value);
            case "PROCESSING" -> new Claim(ClaimState.PROCESSING, key, null, null);
            default -> Claim.bypass();
        };
    }

    private String digest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private void record(String result) {
        meterRegistry.counter("quickpick.idempotency.requests", "result", result).increment();
    }

    public enum ClaimState {
        BYPASS("bypass"), NEW("new"), PROCESSING("processing"), SUCCESS("success");

        private final String metricValue;

        ClaimState(String metricValue) {
            this.metricValue = metricValue;
        }
    }

    public record Claim(ClaimState state, String key, String token, String orderId) {
        private static Claim bypass() {
            return new Claim(ClaimState.BYPASS, null, null, null);
        }
    }
}
