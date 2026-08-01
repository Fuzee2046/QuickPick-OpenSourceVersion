package com.fujian.service;

import com.fujian.config.RedisFeatureProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class DistributedJobLockExecutor {
    private static final Logger log = LoggerFactory.getLogger(DistributedJobLockExecutor.class);

    private final RedisFeatureProperties properties;
    private final ObjectProvider<RedissonClient> redissonProvider;
    private final RedisKeyService keyService;
    private final MeterRegistry meterRegistry;

    public DistributedJobLockExecutor(RedisFeatureProperties properties,
                                      ObjectProvider<RedissonClient> redissonProvider,
                                      RedisKeyService keyService,
                                      MeterRegistry meterRegistry) {
        this.properties = properties;
        this.redissonProvider = redissonProvider;
        this.keyService = keyService;
        this.meterRegistry = meterRegistry;
    }

    public boolean execute(String jobName, String scope, Runnable task) {
        if (!properties.isSchedulerLockEnabled()) {
            task.run();
            return true;
        }
        RedissonClient redisson = redissonProvider.getIfAvailable();
        if (redisson == null) {
            record(jobName, "error");
            return false;
        }

        RLock lock = redisson.getLock(keyService.key("lock:scheduler:" + jobName + ":" + scope));
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, TimeUnit.SECONDS);
            if (!acquired) {
                record(jobName, "skipped");
                return false;
            }
            record(jobName, "acquired");
            task.run();
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            record(jobName, "error");
            return false;
        } catch (Exception redisFailure) {
            record(jobName, "error");
            log.warn("Distributed job lock failed, job={}, scope={}: {}", jobName, scope, redisFailure.getMessage());
            return false;
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void record(String jobName, String result) {
        meterRegistry.counter("quickpick.scheduler.lock", "job", jobName, "result", result).increment();
    }
}
