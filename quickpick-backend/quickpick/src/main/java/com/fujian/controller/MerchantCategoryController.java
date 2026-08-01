package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.CategoryMapper;
import com.fujian.mapper.DishMapper;
import com.fujian.mapper.WeightIngredientMapper;
import com.fujian.pojo.Category;
import com.fujian.pojo.Dish;
import com.fujian.pojo.WeightIngredient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchant/categories")
public class MerchantCategoryController {

    public static class ReorderRequest {
        private List<Long> orderedIds;

        public List<Long> getOrderedIds() { return orderedIds; }
        public void setOrderedIds(List<Long> orderedIds) { this.orderedIds = orderedIds; }
    }

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private WeightIngredientMapper weightIngredientMapper;

    private Long getCurrentShopId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public Result<List<Category>> list() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getShopId, getCurrentShopId())
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        return Result.success(categories);
    }

    @PostMapping
    public Result<String> create(@RequestBody Category category) {
        Long shopId = getCurrentShopId();
        category.setShopId(shopId);
        category.setSort(nextCategorySort(shopId));
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return Result.success("创建成功");
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody Category category) {
        Long shopId = getCurrentShopId();
        Category existingCategory = categoryMapper.selectById(id);
        if (existingCategory == null || !existingCategory.getShopId().equals(shopId)) {
            return Result.error("分类不存在");
        }

        category.setId(id);
        category.setShopId(shopId);
        category.setSort(existingCategory.getSort());
        categoryMapper.updateById(category);
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
            Category update = new Category();
            update.setId(request.getOrderedIds().get(index));
            update.setSort(index + 1);
            if (categoryMapper.updateById(update) != 1) {
                throw new IllegalStateException("分类顺序保存失败");
            }
        }
        return Result.success("排序保存成功");
    }

    private String validateReorderRequest(ReorderRequest request, Long shopId) {
        if (request == null || request.getOrderedIds() == null || request.getOrderedIds().isEmpty()) {
            return "排序数据不能为空";
        }
        List<Long> orderedIds = request.getOrderedIds();
        if (orderedIds.stream().anyMatch(id -> id == null || id <= 0)) {
            return "排序数据不合法";
        }
        Set<Long> uniqueIds = new HashSet<>(orderedIds);
        if (uniqueIds.size() != orderedIds.size()) {
            return "排序数据包含重复分类";
        }
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getShopId, shopId));
        Set<Long> existingIds = categories.stream().map(Category::getId).collect(Collectors.toSet());
        if (!existingIds.equals(uniqueIds)) {
            return "分类列表已发生变化，请刷新后重试";
        }
        return null;
    }

    private int nextCategorySort(Long shopId) {
        Category lastCategory = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getShopId, shopId)
                .orderByDesc(Category::getSort)
                .orderByDesc(Category::getId)
                .last("LIMIT 1"));
        return lastCategory == null || lastCategory.getSort() == null ? 1 : lastCategory.getSort() + 1;
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long shopId = getCurrentShopId();
        Category category = categoryMapper.selectById(id);
        if (category == null || !category.getShopId().equals(shopId)) {
            return Result.error("分类不存在");
        }

        // 检查是否有菜品
        Long dishCount = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getCategoryId, id)
                .eq(Dish::getShopId, shopId));
        if (dishCount > 0) {
            return Result.error("该分类下有菜品，不能删除");
        }

        Long ingredientCount = weightIngredientMapper.selectCount(new LambdaQueryWrapper<WeightIngredient>()
                .eq(WeightIngredient::getCategoryId, id)
                .eq(WeightIngredient::getShopId, shopId));
        if (ingredientCount > 0) {
            return Result.error("该分类下有自选食材，不能删除");
        }
        categoryMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
