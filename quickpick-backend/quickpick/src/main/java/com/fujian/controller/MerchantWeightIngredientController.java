package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.WeightIngredientMapper;
import com.fujian.mapper.CategoryMapper;
import com.fujian.pojo.Category;
import com.fujian.pojo.WeightIngredient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchant/weight-ingredients")
public class MerchantWeightIngredientController {

    @Autowired
    private WeightIngredientMapper weightIngredientMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    public static class ReorderRequest {
        private Long categoryId;
        private List<Long> orderedIds;

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public List<Long> getOrderedIds() { return orderedIds; }
        public void setOrderedIds(List<Long> orderedIds) { this.orderedIds = orderedIds; }
    }

    private Long getCurrentShopId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public Result<List<WeightIngredient>> list(@RequestParam(required = false) Long categoryId) {
        LambdaQueryWrapper<WeightIngredient> queryWrapper = new LambdaQueryWrapper<WeightIngredient>()
                .eq(WeightIngredient::getShopId, getCurrentShopId());
        if (categoryId != null) {
            queryWrapper.eq(WeightIngredient::getCategoryId, categoryId);
        }
        queryWrapper.orderByAsc(WeightIngredient::getSort).orderByAsc(WeightIngredient::getId);
        return Result.success(weightIngredientMapper.selectList(queryWrapper));
    }

    @PostMapping
    public Result<String> create(@RequestBody WeightIngredient ingredient) {
        Long shopId = getCurrentShopId();
        ingredient.setShopId(shopId);
        ingredient.setSort(nextSortForCategory(shopId, ingredient.getCategoryId()));
        ingredient.setCreateTime(LocalDateTime.now());
        ingredient.setStatus(ingredient.getStatus() == null ? 1 : ingredient.getStatus());
        weightIngredientMapper.insert(ingredient);
        return Result.success("创建成功");
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody WeightIngredient ingredient) {
        WeightIngredient existing = weightIngredientMapper.selectById(id);
        if (existing == null || !existing.getShopId().equals(getCurrentShopId())) {
            return Result.error("食材不存在");
        }
        ingredient.setId(id);
        Long shopId = getCurrentShopId();
        ingredient.setShopId(shopId);
        boolean categoryChanged = ingredient.getCategoryId() != null
                && !ingredient.getCategoryId().equals(existing.getCategoryId());
        ingredient.setSort(categoryChanged
                ? nextSortForCategory(shopId, ingredient.getCategoryId())
                : existing.getSort());
        ingredient.setUpdateTime(LocalDateTime.now());
        weightIngredientMapper.updateById(ingredient);
        return Result.success("更新成功");
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
            WeightIngredient update = new WeightIngredient();
            update.setId(request.getOrderedIds().get(index));
            update.setSort(index + 1);
            update.setUpdateTime(LocalDateTime.now());
            if (weightIngredientMapper.updateById(update) != 1) {
                throw new IllegalStateException("食材顺序保存失败");
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
            return "排序数据包含重复食材";
        }
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || !shopId.equals(category.getShopId())) {
            return "分类不存在";
        }
        List<WeightIngredient> categoryItems = weightIngredientMapper.selectList(new LambdaQueryWrapper<WeightIngredient>()
                .eq(WeightIngredient::getShopId, shopId)
                .eq(WeightIngredient::getCategoryId, request.getCategoryId()));
        Set<Long> existingIds = categoryItems.stream().map(WeightIngredient::getId).collect(Collectors.toSet());
        if (!existingIds.equals(uniqueIds)) {
            return "食材列表已发生变化，请刷新后重试";
        }
        return null;
    }

    private int nextSortForCategory(Long shopId, Long categoryId) {
        if (categoryId == null) return 1;
        WeightIngredient lastItem = weightIngredientMapper.selectOne(new LambdaQueryWrapper<WeightIngredient>()
                .eq(WeightIngredient::getShopId, shopId)
                .eq(WeightIngredient::getCategoryId, categoryId)
                .orderByDesc(WeightIngredient::getSort)
                .orderByDesc(WeightIngredient::getId)
                .last("LIMIT 1"));
        return lastItem == null || lastItem.getSort() == null ? 1 : lastItem.getSort() + 1;
    }

    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody WeightIngredient ingredientStatus) {
        WeightIngredient ingredient = weightIngredientMapper.selectById(id);
        if (ingredient == null || !ingredient.getShopId().equals(getCurrentShopId())) {
            return Result.error("食材不存在");
        }
        ingredient.setStatus(ingredientStatus.getStatus());
        ingredient.setUpdateTime(LocalDateTime.now());
        weightIngredientMapper.updateById(ingredient);
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        WeightIngredient existing = weightIngredientMapper.selectById(id);
        if (existing == null || !existing.getShopId().equals(getCurrentShopId())) {
            return Result.error("食材不存在");
        }
        weightIngredientMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
