package com.fujian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickpick.redis")
public class RedisFeatureProperties {
    private boolean enabled;
    private boolean cacheEnabled;
    private boolean idempotencyEnabled;
    private boolean schedulerLockEnabled;
    private String keyPrefix = "quickpick";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCacheEnabled() {
        return enabled && cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public boolean isIdempotencyEnabled() {
        return enabled && idempotencyEnabled;
    }

    public void setIdempotencyEnabled(boolean idempotencyEnabled) {
        this.idempotencyEnabled = idempotencyEnabled;
    }

    public boolean isSchedulerLockEnabled() {
        return enabled && schedulerLockEnabled;
    }

    public void setSchedulerLockEnabled(boolean schedulerLockEnabled) {
        this.schedulerLockEnabled = schedulerLockEnabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
