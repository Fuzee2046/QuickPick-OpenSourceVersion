package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.CategoryMapper;
import com.fujian.mapper.DishMapper;
import com.fujian.mapper.DishOptionGroupBindingMapper;
import com.fujian.mapper.DishOptionGroupMapper;
import com.fujian.mapper.DishOptionValueMapper;
import com.fujian.mapper.ShopBrothOptionMapper;
import com.fujian.mapper.ShopMapper;
import com.fujian.mapper.WeightIngredientMapper;
import com.fujian.pojo.ReservationRuleConfig;
import com.fujian.pojo.Category;
import com.fujian.pojo.Dish;
import com.fujian.pojo.DishOptionGroup;
import com.fujian.pojo.DishOptionGroupBinding;
import com.fujian.pojo.DishOptionValue;
import com.fujian.pojo.Shop;
import com.fujian.pojo.ShopBrothOption;
import com.fujian.pojo.WeightIngredient;
import com.fujian.service.ReservationRuleService;
import com.fujian.service.MerchantBillingService;
import com.fujian.service.ShopCatalogCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/client/shops")
public class ShopController {
    private static final LocalTime RESERVATION_OPEN_TIME = LocalTime.of(7, 30);

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishOptionGroupMapper dishOptionGroupMapper;

    @Autowired
    private DishOptionValueMapper dishOptionValueMapper;

    @Autowired
    private DishOptionGroupBindingMapper dishOptionGroupBindingMapper;

    @Autowired
    private WeightIngredientMapper weightIngredientMapper;

    @Autowired
    private ShopBrothOptionMapper shopBrothOptionMapper;

    @Autowired
    private ReservationRuleService reservationRuleService;

    @Autowired
    private MerchantBillingService merchantBillingService;

    @Autowired
    private ShopCatalogCacheService shopCatalogCacheService;

    @GetMapping
    public Result<List<Shop>> getShops(@RequestParam(required = false) Integer canteenId) {
        List<Shop> shops = canteenId == null
                ? shopCatalogCacheService.getShopList(this::loadVisibleShops)
                : loadVisibleShops(canteenId);

        // 动态检查营业状态
        shops.forEach(this::checkShopStatus);

        return Result.success(shops);
    }

    private List<Shop> loadVisibleShops() {
        return loadVisibleShops(null);
    }

    private List<Shop> loadVisibleShops(Integer canteenId) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<Shop>()
                .eq(Shop::getVisible, 1)
                .orderByAsc(Shop::getDisplaySort)
                .orderByAsc(Shop::getId);
        if (canteenId != null) {
            queryWrapper.eq(Shop::getCanteenId, canteenId);
        }
        return shopMapper.selectList(queryWrapper);
    }

    @GetMapping("/{shopId}")
    public Result<Shop> getShopDetail(@PathVariable Long shopId) {
        Shop shop = shopCatalogCacheService.getShop(shopId, () -> loadVisibleShop(shopId));
        if (shop == null || shop.getVisible() == null || shop.getVisible() != 1) {
            return Result.error("店铺不存在或已下线");
        }
        checkShopStatus(shop);
        return Result.success(shop);
    }

    @GetMapping("/reservation-rule-config")
    public Result<ReservationRuleConfig> getReservationRuleConfig() {
        return Result.success(reservationRuleService.getCurrentConfig());
    }

    /**
     * 根据当前时间检查店铺状态
     * status 仅表示商户是否手动暂停接单
     * currentlyOpen 表示当前时间是否处于营业时段
     */
    private void checkShopStatus(Shop shop) {
        boolean billingAvailable = merchantBillingService.isOrderingAllowed(shop.getId());
        shop.setBillingServiceAvailable(billingAvailable);
        if (!billingAvailable) {
            shop.setCurrentlyOpen(false);
            shop.setDisplayStatus("service_paused");
            shop.setDisplayStatusText("商户服务暂停，暂时无法下单");
            return;
        }
        if (shop.getStatus() == null || shop.getStatus() == 0) {
            shop.setCurrentlyOpen(false);
            shop.setDisplayStatus("paused");
            shop.setDisplayStatusText("暂停接单");
            return;
        }

        java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        boolean isOpen = false;

        // 检查第一时间段
        if (isTimeInRange(now, shop.getOpenTime1(), shop.getCloseTime1())) {
            isOpen = true;
        }

        // 检查第二时间段
        if (!isOpen && isTimeInRange(now, shop.getOpenTime2(), shop.getCloseTime2())) {
            isOpen = true;
        }

        shop.setCurrentlyOpen(isOpen);

        if (isOpen) {
            shop.setDisplayStatus("open");
            shop.setDisplayStatusText("营业中");
            return;
        }

        if (now.isBefore(RESERVATION_OPEN_TIME)) {
            shop.setDisplayStatus("closed");
            shop.setDisplayStatusText("休息中");
            return;
        }

        if (hasUpcomingBusinessSlot(now, shop)) {
            shop.setDisplayStatus("reservable");
            shop.setDisplayStatusText("可预约");
            return;
        }

        shop.setDisplayStatus("closed");
        shop.setDisplayStatusText("休息中");
    }

    private boolean hasUpcomingBusinessSlot(java.time.LocalTime now, Shop shop) {
        return isUpcomingSlot(now, shop.getOpenTime1(), shop.getCloseTime1())
                || isUpcomingSlot(now, shop.getOpenTime2(), shop.getCloseTime2());
    }

    private boolean isUpcomingSlot(java.time.LocalTime now, java.time.LocalTime start, java.time.LocalTime end) {
        if (start == null || end == null) {
            return false;
        }

        if (start.isAfter(end)) {
            return now.isBefore(start);
        }

        return now.isBefore(start);
    }

    private boolean isTimeInRange(java.time.LocalTime now, java.time.LocalTime start, java.time.LocalTime end) {
        if (start == null || end == null) {
            return false;
        }
        if (start.isAfter(end)) {
            // 跨越午夜，例如 22:00 - 02:00
            return !now.isBefore(start) || !now.isAfter(end);
        } else {
            // 当天内，例如 09:00 - 21:00
            return !now.isBefore(start) && !now.isAfter(end);
        }
    }

    @GetMapping("/{shopId}/dishes")
    public Result<List<Map<String, Object>>> getShopDishes(@PathVariable Long shopId) {
        Shop shop = shopCatalogCacheService.getShop(shopId, () -> loadVisibleShop(shopId));
        if (shop == null || shop.getVisible() == null || shop.getVisible() != 1) {
            return Result.error("店铺不存在或已下线");
        }

        return Result.success(shopCatalogCacheService.getMenu(shopId, () -> loadShopDishes(shopId)));
    }

    private List<Map<String, Object>> loadShopDishes(Long shopId) {
        // 获取分类
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getShopId, shopId)
                .orderByAsc(Category::getSort));

        // 获取菜品
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getShopId, shopId)
                .in(Dish::getStatus, 0, 1) // 0:下架, 1:上架
                .orderByAsc(Dish::getSort));
        Map<Long, List<Map<String, Object>>> optionMap = buildDishOptionMap(shopId, dishes);

        // 按分类分组
        List<Map<String, Object>> result = categories.stream().map(category -> {
            List<Map<String, Object>> categoryDishes = dishes.stream()
                    .filter(dish -> dish.getCategoryId().equals(category.getId()))
                    .map(dish -> {
                        Map<String, Object> dishMap = new LinkedHashMap<>();
                        dishMap.put("id", dish.getId());
                        dishMap.put("shopId", dish.getShopId());
                        dishMap.put("categoryId", dish.getCategoryId());
                        dishMap.put("name", dish.getName());
                        dishMap.put("price", dish.getPrice());
                        dishMap.put("optionEnabled", dish.getOptionEnabled() != null && dish.getOptionEnabled() == 1 ? 1 : 0);
                        dishMap.put("image", dish.getImage());
                        dishMap.put("status", dish.getStatus());
                        dishMap.put("sort", dish.getSort());
                        List<Map<String, Object>> optionGroups = optionMap.getOrDefault(dish.getId(), new ArrayList<>());
                        dishMap.put("optionGroups", optionGroups);
                        dishMap.put("optionSummary", optionGroups.stream()
                                .map(group -> String.valueOf(group.get("name")))
                                .collect(Collectors.joining(" / ")));
                        return dishMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("categoryName", category.getName());
            map.put("categoryId", category.getId());
            map.put("dishes", categoryDishes);
            return map;
        }).collect(Collectors.toList());

        return result;
    }

    private Map<Long, List<Map<String, Object>>> buildDishOptionMap(Long shopId, List<Dish> dishes) {
        Map<Long, List<Map<String, Object>>> result = new LinkedHashMap<>();
        List<Long> dishIds = dishes.stream()
                .filter(dish -> dish.getOptionEnabled() != null && dish.getOptionEnabled() == 1)
                .map(Dish::getId)
                .collect(Collectors.toList());
        if (dishIds.isEmpty()) {
            return result;
        }

        List<DishOptionGroupBinding> bindings = dishOptionGroupBindingMapper.selectList(new LambdaQueryWrapper<DishOptionGroupBinding>()
                .in(DishOptionGroupBinding::getDishId, dishIds)
                .orderByAsc(DishOptionGroupBinding::getSort)
                .orderByAsc(DishOptionGroupBinding::getId));
        if (bindings.isEmpty()) {
            return result;
        }

        List<Long> groupIds = bindings.stream().map(DishOptionGroupBinding::getOptionGroupId).distinct().collect(Collectors.toList());
        Map<Long, DishOptionGroup> groupMap = dishOptionGroupMapper.selectList(new LambdaQueryWrapper<DishOptionGroup>()
                .eq(DishOptionGroup::getShopId, shopId)
                .in(DishOptionGroup::getId, groupIds))
                .stream()
                .collect(Collectors.toMap(DishOptionGroup::getId, item -> item));
        Map<Long, List<DishOptionValue>> valueMap = dishOptionValueMapper.selectList(new LambdaQueryWrapper<DishOptionValue>()
                .in(DishOptionValue::getOptionGroupId, groupIds)
                .orderByAsc(DishOptionValue::getSort)
                .orderByAsc(DishOptionValue::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        DishOptionValue::getOptionGroupId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (DishOptionGroupBinding binding : bindings) {
            DishOptionGroup group = groupMap.get(binding.getOptionGroupId());
            if (group == null) {
                continue;
            }

            Map<String, Object> groupItem = new LinkedHashMap<>();
            groupItem.put("optionGroupId", group.getId());
            groupItem.put("name", group.getName());
            groupItem.put("selectType", group.getSelectType());
            groupItem.put("required", binding.getRequired() == null ? 1 : binding.getRequired());
            groupItem.put("sort", binding.getSort());

            List<Map<String, Object>> values = new ArrayList<>();
            for (DishOptionValue value : valueMap.getOrDefault(group.getId(), new ArrayList<>())) {
                Map<String, Object> valueItem = new LinkedHashMap<>();
                valueItem.put("optionValueId", value.getId());
                valueItem.put("name", value.getName());
                valueItem.put("extraPrice", value.getExtraPrice());
                valueItem.put("isDefault", value.getIsDefault() == null ? 0 : value.getIsDefault());
                valueItem.put("sort", value.getSort());
                values.add(valueItem);
            }
            groupItem.put("values", values);
            result.computeIfAbsent(binding.getDishId(), key -> new ArrayList<>()).add(groupItem);
        }

        return result;
    }

    @GetMapping("/{shopId}/weight-selection-config")
    public Result<Map<String, Object>> getWeightSelectionConfig(@PathVariable Long shopId) {
        Shop shop = shopCatalogCacheService.getShop(shopId, () -> loadVisibleShop(shopId));
        if (shop == null || shop.getVisible() == null || shop.getVisible() != 1) {
            return Result.error("店铺不存在或已下线");
        }
        if (!"weight_selection".equals(shop.getShopMode())) {
            return Result.error("当前店铺不是自选称重店");
        }

        return Result.success(shopCatalogCacheService.getWeightMenu(shopId, () -> loadWeightSelectionConfig(shopId)));
    }

    private Map<String, Object> loadWeightSelectionConfig(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getShopId, shopId)
                .orderByAsc(Category::getSort));
        List<WeightIngredient> ingredients = weightIngredientMapper.selectList(new LambdaQueryWrapper<WeightIngredient>()
                .eq(WeightIngredient::getShopId, shopId)
                .eq(WeightIngredient::getStatus, 1)
                .orderByAsc(WeightIngredient::getSort));
        List<ShopBrothOption> brothOptions = shopBrothOptionMapper.selectList(new LambdaQueryWrapper<ShopBrothOption>()
                .eq(ShopBrothOption::getShopId, shopId)
                .eq(ShopBrothOption::getStatus, 1)
                .orderByAsc(ShopBrothOption::getSort));

        List<Map<String, Object>> categoryBlocks = categories.stream().map(category -> {
            List<WeightIngredient> categoryIngredients = ingredients.stream()
                    .filter(ingredient -> ingredient.getCategoryId() != null && ingredient.getCategoryId().equals(category.getId()))
                    .collect(Collectors.toList());

            Map<String, Object> block = new java.util.HashMap<>();
            block.put("categoryId", category.getId());
            block.put("categoryName", category.getName());
            block.put("ingredients", categoryIngredients);
            return block;
        }).filter(block -> {
            List<?> categoryIngredients = (List<?>) block.get("ingredients");
            return categoryIngredients != null && !categoryIngredients.isEmpty();
        }).collect(Collectors.toList());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("shopId", shop.getId());
        result.put("shopMode", shop.getShopMode());
        result.put("weightPricePer500g", shop.getWeightPricePer500g());
        result.put("minimumOrderWeightG", shop.getMinimumOrderWeightG());
        result.put("brothOptions", brothOptions);
        result.put("categories", categoryBlocks);
        return result;
    }

    private Shop loadVisibleShop(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || shop.getVisible() == null || shop.getVisible() != 1) {
            return null;
        }
        return shop;
    }
}
