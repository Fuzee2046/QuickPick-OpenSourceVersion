package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.DishMapper;
import com.fujian.mapper.CategoryMapper;
import com.fujian.mapper.DishOptionGroupBindingMapper;
import com.fujian.mapper.DishOptionGroupMapper;
import com.fujian.mapper.DishOptionValueMapper;
import com.fujian.pojo.Dish;
import com.fujian.pojo.Category;
import com.fujian.pojo.DishOptionGroup;
import com.fujian.pojo.DishOptionGroupBinding;
import com.fujian.pojo.DishOptionValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchant/dishes")
public class MerchantDishController {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private DishOptionGroupMapper dishOptionGroupMapper;

    @Autowired
    private DishOptionValueMapper dishOptionValueMapper;

    @Autowired
    private DishOptionGroupBindingMapper dishOptionGroupBindingMapper;

    private Long getCurrentShopId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static class DishOptionBindingRequest {
        private Long optionGroupId;
        private Integer required;
        private Integer sort;

        public Long getOptionGroupId() { return optionGroupId; }
        public void setOptionGroupId(Long optionGroupId) { this.optionGroupId = optionGroupId; }
        public Integer getRequired() { return required; }
        public void setRequired(Integer required) { this.required = required; }
        public Integer getSort() { return sort; }
        public void setSort(Integer sort) { this.sort = sort; }
    }

    public static class ReorderRequest {
        private Long categoryId;
        private List<Long> orderedIds;

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public List<Long> getOrderedIds() { return orderedIds; }
        public void setOrderedIds(List<Long> orderedIds) { this.orderedIds = orderedIds; }
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long categoryId) {
        Long shopId = getCurrentShopId();
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getShopId, shopId);
        if (categoryId != null) {
            queryWrapper.eq(Dish::getCategoryId, categoryId);
        }
        queryWrapper.orderByAsc(Dish::getSort).orderByAsc(Dish::getId);
        List<Dish> dishes = dishMapper.selectList(queryWrapper);
        return Result.success(buildMerchantDishList(dishes, shopId, false));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Dish dish) {
        Long shopId = getCurrentShopId();
        dish.setShopId(shopId);
        dish.setSort(nextSortForCategory(shopId, dish.getCategoryId()));
        dish.setCreateTime(LocalDateTime.now());
        dish.setUpdateTime(LocalDateTime.now());
        dish.setStatus(1); // 默认上架
        dish.setOptionEnabled(dish.getOptionEnabled() != null && dish.getOptionEnabled() == 1 ? 1 : 0);
        dishMapper.insert(dish);
        return Result.success(buildMerchantDishDetail(dish.getId(), getCurrentShopId()));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Dish dish) {
        Long shopId = getCurrentShopId();
        Dish existingDish = dishMapper.selectById(id);
        if (existingDish == null || !existingDish.getShopId().equals(shopId)) {
            return Result.error("菜品不存在");
        }

        dish.setId(id);
        dish.setShopId(shopId);
        boolean categoryChanged = dish.getCategoryId() != null
                && !dish.getCategoryId().equals(existingDish.getCategoryId());
        dish.setSort(categoryChanged
                ? nextSortForCategory(shopId, dish.getCategoryId())
                : existingDish.getSort());
        dish.setUpdateTime(LocalDateTime.now());
        dish.setOptionEnabled(dish.getOptionEnabled() != null && dish.getOptionEnabled() == 1 ? 1 : 0);
        dishMapper.updateById(dish);
        return Result.success(buildMerchantDishDetail(id, shopId));
    }

    @PutMapping("/reorder")
    @Transactional
    public Result<String> reorder(@RequestBody ReorderRequest request) {
        Long shopId = getCurrentShopId();
        String validationError = validateReorderRequest(request, shopId);
        if (validationError != null) {
            return Result.error(validationError);
        }

        for (int index = 0; index < request.getOrderedIds().size(); index++) {
            Dish update = new Dish();
            update.setId(request.getOrderedIds().get(index));
            update.setSort(index + 1);
            update.setUpdateTime(LocalDateTime.now());
            if (dishMapper.updateById(update) != 1) {
                throw new IllegalStateException("菜品顺序保存失败");
            }
        }
        return Result.success("排序保存成功");
    }

    private String validateReorderRequest(ReorderRequest request, Long shopId) {
        if (request == null || request.getCategoryId() == null || request.getCategoryId() <= 0) {
            return "请选择需要排序的分类";
        }
        List<Long> orderedIds = request.getOrderedIds();
        if (orderedIds == null || orderedIds.isEmpty() || orderedIds.stream().anyMatch(id -> id == null || id <= 0)) {
            return "排序数据不能为空";
        }
        Set<Long> uniqueIds = new HashSet<>(orderedIds);
        if (uniqueIds.size() != orderedIds.size()) {
            return "排序数据包含重复菜品";
        }
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || !shopId.equals(category.getShopId())) {
            return "分类不存在";
        }
        List<Dish> categoryDishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getShopId, shopId)
                .eq(Dish::getCategoryId, request.getCategoryId()));
        Set<Long> existingIds = categoryDishes.stream().map(Dish::getId).collect(Collectors.toSet());
        if (!existingIds.equals(uniqueIds)) {
            return "菜品列表已发生变化，请刷新后重试";
        }
        return null;
    }

    private int nextSortForCategory(Long shopId, Long categoryId) {
        if (categoryId == null) return 1;
        Dish lastDish = dishMapper.selectOne(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getShopId, shopId)
                .eq(Dish::getCategoryId, categoryId)
                .orderByDesc(Dish::getSort)
                .orderByDesc(Dish::getId)
                .last("LIMIT 1"));
        return lastDish == null || lastDish.getSort() == null ? 1 : lastDish.getSort() + 1;
    }

    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody Dish dishStatus) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !dish.getShopId().equals(getCurrentShopId())) {
            return Result.error("菜品不存在");
        }
        dish.setStatus(dishStatus.getStatus());
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<String> delete(@PathVariable Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !dish.getShopId().equals(getCurrentShopId())) {
            return Result.error("菜品不存在");
        }
        dishOptionGroupBindingMapper.delete(new LambdaQueryWrapper<DishOptionGroupBinding>()
                .eq(DishOptionGroupBinding::getDishId, id));
        // 这里采用物理删除，实际项目中建议逻辑删除
        dishMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/{id}/option-bindings")
    public Result<List<Map<String, Object>>> getOptionBindings(@PathVariable Long id) {
        Long shopId = getCurrentShopId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !shopId.equals(dish.getShopId())) {
            return Result.error("菜品不存在");
        }
        return Result.success(buildDishBindingOptions(id, shopId));
    }

    @PutMapping("/{id}/option-bindings")
    @Transactional
    public Result<List<Map<String, Object>>> saveOptionBindings(
            @PathVariable Long id,
            @RequestBody List<DishOptionBindingRequest> bindings
    ) {
        Long shopId = getCurrentShopId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !shopId.equals(dish.getShopId())) {
            return Result.error("菜品不存在");
        }

        List<DishOptionBindingRequest> safeBindings = bindings == null ? new ArrayList<>() : bindings;
        if (!safeBindings.isEmpty()) {
            List<Long> groupIds = safeBindings.stream()
                    .map(DishOptionBindingRequest::getOptionGroupId)
                    .filter(groupId -> groupId != null && groupId > 0)
                    .distinct()
                    .collect(Collectors.toList());
            if (groupIds.size() != safeBindings.size()) {
                return Result.error("规格组参数不合法");
            }
            Long groupCount = dishOptionGroupMapper.selectCount(new LambdaQueryWrapper<DishOptionGroup>()
                    .eq(DishOptionGroup::getShopId, shopId)
                    .in(DishOptionGroup::getId, groupIds));
            if (groupCount == null || groupCount.intValue() != groupIds.size()) {
                return Result.error("部分规格组不存在");
            }
        }

        dishOptionGroupBindingMapper.delete(new LambdaQueryWrapper<DishOptionGroupBinding>()
                .eq(DishOptionGroupBinding::getDishId, id));

        int fallbackSort = 1;
        for (DishOptionBindingRequest bindingRequest : safeBindings) {
            DishOptionGroupBinding binding = new DishOptionGroupBinding();
            binding.setDishId(id);
            binding.setOptionGroupId(bindingRequest.getOptionGroupId());
            binding.setRequired(bindingRequest.getRequired() != null && bindingRequest.getRequired() == 0 ? 0 : 1);
            binding.setSort(bindingRequest.getSort() == null ? fallbackSort : bindingRequest.getSort());
            binding.setCreateTime(LocalDateTime.now());
            binding.setUpdateTime(LocalDateTime.now());
            dishOptionGroupBindingMapper.insert(binding);
            fallbackSort += 1;
        }

        Dish updateDish = new Dish();
        updateDish.setId(id);
        updateDish.setOptionEnabled(safeBindings.isEmpty() ? 0 : 1);
        updateDish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(updateDish);

        return Result.success(buildDishBindingOptions(id, shopId));
    }

    private List<Map<String, Object>> buildMerchantDishList(List<Dish> dishes, Long shopId) {
        return buildMerchantDishList(dishes, shopId, true);
    }

    private List<Map<String, Object>> buildMerchantDishList(List<Dish> dishes, Long shopId, boolean includeValues) {
        List<Long> dishIds = dishes.stream().map(Dish::getId).collect(Collectors.toList());
        Map<Long, List<Map<String, Object>>> bindingMap = buildBindingSummaryMap(dishIds, shopId, includeValues);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Dish dish : dishes) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", dish.getId());
            map.put("shopId", dish.getShopId());
            map.put("categoryId", dish.getCategoryId());
            map.put("name", dish.getName());
            map.put("price", dish.getPrice());
            map.put("optionEnabled", dish.getOptionEnabled());
            map.put("image", dish.getImage());
            map.put("status", dish.getStatus());
            map.put("sort", dish.getSort());
            map.put("createTime", dish.getCreateTime());
            map.put("updateTime", dish.getUpdateTime());
            List<Map<String, Object>> bindings = bindingMap.getOrDefault(dish.getId(), new ArrayList<>());
            map.put("optionBindings", bindings);
            map.put("optionSummary", buildBindingSummaryText(bindings));
            result.add(map);
        }
        return result;
    }

    private Map<String, Object> buildMerchantDishDetail(Long dishId, Long shopId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null || !shopId.equals(dish.getShopId())) {
            return new LinkedHashMap<>();
        }
        return buildMerchantDishList(java.util.Arrays.asList(dish), shopId, true).get(0);
    }

    private Map<Long, List<Map<String, Object>>> buildBindingSummaryMap(List<Long> dishIds, Long shopId) {
        return buildBindingSummaryMap(dishIds, shopId, true);
    }

    private Map<Long, List<Map<String, Object>>> buildBindingSummaryMap(List<Long> dishIds, Long shopId, boolean includeValues) {
        Map<Long, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if (dishIds == null || dishIds.isEmpty()) {
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
        List<DishOptionGroup> groups = dishOptionGroupMapper.selectList(new LambdaQueryWrapper<DishOptionGroup>()
                .eq(DishOptionGroup::getShopId, shopId)
                .in(DishOptionGroup::getId, groupIds));
        Map<Long, DishOptionGroup> groupMap = groups.stream().collect(Collectors.toMap(DishOptionGroup::getId, item -> item));

        Map<Long, List<DishOptionValue>> valueMap = new LinkedHashMap<>();
        if (includeValues) {
            List<DishOptionValue> values = dishOptionValueMapper.selectList(new LambdaQueryWrapper<DishOptionValue>()
                    .in(DishOptionValue::getOptionGroupId, groupIds)
                    .orderByAsc(DishOptionValue::getSort)
                    .orderByAsc(DishOptionValue::getId));
            valueMap = values.stream().collect(Collectors.groupingBy(
                    DishOptionValue::getOptionGroupId,
                    LinkedHashMap::new,
                    Collectors.toList()
            ));
        }

        for (DishOptionGroupBinding binding : bindings) {
            DishOptionGroup group = groupMap.get(binding.getOptionGroupId());
            if (group == null) {
                continue;
            }
            Map<String, Object> bindingMap = new LinkedHashMap<>();
            bindingMap.put("id", binding.getId());
            bindingMap.put("dishId", binding.getDishId());
            bindingMap.put("optionGroupId", binding.getOptionGroupId());
            bindingMap.put("required", binding.getRequired());
            bindingMap.put("sort", binding.getSort());
            bindingMap.put("groupName", group.getName());
            bindingMap.put("selectType", group.getSelectType());

            if (includeValues) {
                List<Map<String, Object>> valueList = new ArrayList<>();
                for (DishOptionValue value : valueMap.getOrDefault(group.getId(), new ArrayList<>())) {
                    Map<String, Object> valueItem = new LinkedHashMap<>();
                    valueItem.put("id", value.getId());
                    valueItem.put("name", value.getName());
                    valueItem.put("extraPrice", value.getExtraPrice() == null ? BigDecimal.ZERO : value.getExtraPrice());
                    valueItem.put("isDefault", value.getIsDefault());
                    valueItem.put("sort", value.getSort());
                    valueList.add(valueItem);
                }
                bindingMap.put("values", valueList);
            }
            result.computeIfAbsent(binding.getDishId(), key -> new ArrayList<>()).add(bindingMap);
        }
        return result;
    }

    private List<Map<String, Object>> buildDishBindingOptions(Long dishId, Long shopId) {
        return buildBindingSummaryMap(java.util.Arrays.asList(dishId), shopId, true).getOrDefault(dishId, new ArrayList<>());
    }

    private String buildBindingSummaryText(List<Map<String, Object>> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return "";
        }
        return bindings.stream()
                .map(binding -> {
                    String name = String.valueOf(binding.getOrDefault("groupName", ""));
                    Object required = binding.get("required");
                    return Integer.valueOf(0).equals(required) ? name + "(可不选)" : name;
                })
                .collect(Collectors.joining(" / "));
    }
}
