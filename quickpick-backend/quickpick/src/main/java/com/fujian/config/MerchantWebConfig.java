package com.fujian.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MerchantWebConfig implements WebMvcConfigurer {
    private final ShopCatalogCacheEvictionInterceptor cacheEvictionInterceptor;

    public MerchantWebConfig(ShopCatalogCacheEvictionInterceptor cacheEvictionInterceptor) {
        this.cacheEvictionInterceptor = cacheEvictionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cacheEvictionInterceptor)
                .addPathPatterns("/api/merchant/**", "/api/admin/shops/**", "/api/admin/config/reservation");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /uploads/** to the local file system directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("classpath:/static/uploads/");
    }
}
