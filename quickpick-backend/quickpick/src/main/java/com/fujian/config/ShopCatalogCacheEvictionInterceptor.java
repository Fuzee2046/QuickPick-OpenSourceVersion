package com.fujian.config;

import com.fujian.service.ShopCatalogCacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class ShopCatalogCacheEvictionInterceptor implements HandlerInterceptor {
    private static final String SHOP_ID_ATTRIBUTE = ShopCatalogCacheEvictionInterceptor.class.getName() + ".shopId";
    private static final Set<String> MERCHANT_CATALOG_PATHS = Set.of(
            "/api/merchant/shop",
            "/api/merchant/categories",
            "/api/merchant/dishes",
            "/api/merchant/dish-option-groups",
            "/api/merchant/weight-ingredients",
            "/api/merchant/broth-options"
    );

    private final ShopCatalogCacheService cacheService;

    public ShopCatalogCacheEvictionInterceptor(ShopCatalogCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isReadOnly(request)) {
            return true;
        }
        String uri = request.getRequestURI();
        if (isMerchantCatalogMutation(uri)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Long shopId) {
                request.setAttribute(SHOP_ID_ATTRIBUTE, shopId);
            }
        } else if (uri.startsWith("/api/admin/shops/")) {
            Long shopId = parseAdminShopId(uri);
            if (shopId != null) {
                request.setAttribute(SHOP_ID_ATTRIBUTE, shopId);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null || isReadOnly(request)) {
            return;
        }
        Object shopId = request.getAttribute(SHOP_ID_ATTRIBUTE);
        if (shopId instanceof Long id) {
            cacheService.evictShop(id);
        }
        if (request.getRequestURI().equals("/api/admin/config/reservation")) {
            cacheService.evictReservationRule();
        }
    }

    private boolean isMerchantCatalogMutation(String uri) {
        return MERCHANT_CATALOG_PATHS.stream().anyMatch(path -> uri.equals(path) || uri.startsWith(path + "/"));
    }

    private boolean isReadOnly(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod());
    }

    private Long parseAdminShopId(String uri) {
        String[] parts = uri.split("/");
        if (parts.length < 5) {
            return null;
        }
        try {
            return Long.valueOf(parts[4]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
