package com.fujian.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RedisFeatureProperties.class)
public class RedisInfrastructureConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "quickpick.redis", name = "enabled", havingValue = "true")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        config.setLazyInitialization(true);
        String scheme = redisProperties.getSsl().isEnabled() ? "rediss" : "redis";
        Duration connectTimeout = redisProperties.getConnectTimeout() == null
                ? Duration.ofSeconds(5)
                : redisProperties.getConnectTimeout();
        Duration commandTimeout = redisProperties.getTimeout() == null
                ? Duration.ofSeconds(2)
                : redisProperties.getTimeout();
        SingleServerConfig server = config.useSingleServer()
                .setAddress(scheme + "://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase())
                .setConnectTimeout((int) connectTimeout.toMillis())
                .setTimeout((int) commandTimeout.toMillis());

        if (StringUtils.hasText(redisProperties.getUsername())) {
            server.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            server.setPassword(redisProperties.getPassword());
        }
        return Redisson.create(config);
    }
}
