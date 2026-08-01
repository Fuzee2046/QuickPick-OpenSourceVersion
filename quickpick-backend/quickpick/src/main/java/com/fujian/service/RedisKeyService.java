package com.fujian.service;

import com.fujian.config.RedisFeatureProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class RedisKeyService {
    private final String namespace;

    public RedisKeyService(RedisFeatureProperties properties, Environment environment) {
        String profile = Arrays.stream(environment.getActiveProfiles()).findFirst().orElse("default");
        this.namespace = properties.getKeyPrefix() + ":" + profile + ":v6";
    }

    public String key(String suffix) {
        return namespace + ":" + suffix;
    }
}
