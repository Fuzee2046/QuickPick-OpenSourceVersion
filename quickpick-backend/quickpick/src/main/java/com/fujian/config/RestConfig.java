package com.fujian.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestConfig {

    /**
     * 创建RestTemplate Bean，用于调用REST API
     * RestTemplate是Spring提供的同步HTTP客户端，用于访问REST服务
     * 配置支持text/plain和application/json响应类型
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 获取现有的消息转换器
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        
        // 添加StringHttpMessageConverter，支持text/plain响应
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        List<MediaType> stringMediaTypes = new ArrayList<>();
        stringMediaTypes.add(MediaType.TEXT_PLAIN);
        stringMediaTypes.add(MediaType.TEXT_HTML);
        stringMediaTypes.add(MediaType.APPLICATION_JSON); // 也支持JSON
        stringConverter.setSupportedMediaTypes(stringMediaTypes);
        messageConverters.add(stringConverter);
        
        // 添加JSON转换器
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        messageConverters.add(jsonConverter);
        
        restTemplate.setMessageConverters(messageConverters);
        
        return restTemplate;
    }
}
