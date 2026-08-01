package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.DishOptionGroupBindingMapper;
import com.fujian.mapper.DishOptionGroupMapper;
import com.fujian.mapper.DishOptionValueMapper;
import com.fujian.pojo.DishOptionGroup;
import com.fujian.pojo.DishOptionValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchant/dish-option-groups")
public class MerchantDishOptionController {

    @Autowired
    private DishOptionGroupMapper dishOptionGroupMapper;

    @Autowired
    private DishOptionValueMapper dishOptionValueMapper;

    @Autowired
    private DishOptionGroupBindingMapper dishOptionGroupBindingMapper;

    private Long getCurrentShopId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static class DishOptionValuePayload {
        private Long id;
        private String name;
        private BigDecimal extraPrice;
        private Integer isDefault;
        private Integer sort;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getExtraPrice() { return extraPrice; }
        public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }
        public Integer getIsDefault() { return isDefault; }
        public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
        public Integer getSort() { return sort; }
        public void setSort(Integer sort) { this.sort = sort; }
    }

    public static class DishOptionGroupPayload {
        private String name;
        private String selectType;
        private List<DishOptionValuePayload> values;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSelectType() { return selectType; }
        public void setSelectType(String selectType) { this.selectType = selectType; }
        public List<DishOptionValuePayload> getValues() { return values; }
        public void setValues(List<DishOptionValuePayload> values) { this.values = values; }
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        Long shopId = getCurrentShopId();
        List<DishOptionGroup> groups = dishOptionGroupMapper.selectList(new LambdaQueryWrapper<DishOptionGroup>()
                .eq(DishOptionGroup::getShopId, shopId)
                .orderByDesc(DishOptionGroup::getUpdateTime)
                .orderByDesc(DishOptionGroup::getId));

        List<Long> groupIds = groups.stream().map(DishOptionGroup::getId).collect(Collectors.toList());
        Map<Long, List<DishOptionValue>> valueMap = new LinkedHashMap<>();
        if (!groupIds.isEmpty()) {
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

        List<Map<String, Object>> result = new ArrayList<>();
        for (DishOptionGroup group : groups) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", group.getId());
            map.put("shopId", group.getShopId());
            map.put("name", group.getName());
            map.put("selectType", group.getSelectType());
            map.put("createTime", group.getCreateTime());
            map.put("updateTime", group.getUpdateTime());

            List<Map<String, Object>> values = new ArrayList<>();
            for (DishOptionValue value : valueMap.getOrDefault(group.getId(), new ArrayList<>())) {
                Map<String, Object> valueMapItem = new LinkedHashMap<>();
                valueMapItem.put("id", value.getId());
                valueMapItem.put("optionGroupId", value.getOptionGroupId());
                valueMapItem.put("name", value.getName());
                valueMapItem.put("extraPrice", value.getExtraPrice());
                valueMapItem.put("isDefault", value.getIsDefault());
                valueMapItem.put("sort", value.getSort());
                values.add(valueMapItem);
            }
            map.put("values", values);
            result.add(map);
        }
        return Result.success(result);
    }

    @PostMapping
    @Transactional
    public Result<Map<String, Object>> create(@RequestBody DishOptionGroupPayload payload) {
        Long shopId = getCurrentShopId();
        String validationMessage = validateGroupPayload(payload);
        if (validationMessage != null) {
            return Result.error(validationMessage);
        }

        DishOptionGroup group = new DishOptionGroup();
        group.setShopId(shopId);
        group.setName(payload.getName().trim());
        group.setSelectType(normalizeSelectType(payload.getSelectType()));
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        dishOptionGroupMapper.insert(group);
        replaceGroupValues(group.getId(), payload.getValues());
        return Result.success(buildGroupDetail(group.getId(), shopId));
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody DishOptionGroupPayload payload) {
        Long shopId = getCurrentShopId();
        DishOptionGroup existingGroup = dishOptionGroupMapper.selectById(id);
        if (existingGroup == null || !shopId.equals(existingGroup.getShopId())) {
            return Result.error("规格组不存在");
        }
        String validationMessage = validateGroupPayload(payload);
        if (validationMessage != null) {
            return Result.error(validationMessage);
        }

        existingGroup.setName(payload.getName().trim());
        existingGroup.setSelectType(normalizeSelectType(payload.getSelectType()));
        existingGroup.setUpdateTime(LocalDateTime.now());
        dishOptionGroupMapper.updateById(existingGroup);
        replaceGroupValues(existingGroup.getId(), payload.getValues());
        return Result.success(buildGroupDetail(existingGroup.getId(), shopId));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<String> delete(@PathVariable Long id) {
        Long shopId = getCurrentShopId();
        DishOptionGroup existingGroup = dishOptionGroupMapper.selectById(id);
        if (existingGroup == null || !shopId.equals(existingGroup.getShopId())) {
            return Result.error("规格组不存在");
        }
        dishOptionGroupMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/{id}/binding-count")
    public Result<Map<String, Object>> getBindingCount(@PathVariable Long id) {
        Long shopId = getCurrentShopId();
        DishOptionGroup group = dishOptionGroupMapper.selectById(id);
        if (group == null || !shopId.equals(group.getShopId())) {
            return Result.error("规格组不存在");
        }
        Long count = dishOptionGroupBindingMapper.selectCount(new LambdaQueryWrapper<com.fujian.pojo.DishOptionGroupBinding>()
                .eq(com.fujian.pojo.DishOptionGroupBinding::getOptionGroupId, id));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", count == null ? 0 : count);
        return Result.success(map);
    }

    private String validateGroupPayload(DishOptionGroupPayload payload) {
        if (payload == null) {
            return "规格组参数不能为空";
        }
        if (payload.getName() == null || payload.getName().trim().isEmpty()) {
            return "请填写规格组名称";
        }
        if (payload.getValues() == null || payload.getValues().isEmpty()) {
            return "请至少添加一个规格值";
        }
        Map<String, Boolean> valueNameMap = new LinkedHashMap<>();
        for (DishOptionValuePayload value : payload.getValues()) {
            if (value == null || value.getName() == null || value.getName().trim().isEmpty()) {
                return "规格值名称不能为空";
            }
            String normalizedName = value.getName().trim();
            if (valueNameMap.containsKey(normalizedName)) {
                return "同一规格组内不能有重复的规格值名称";
            }
            valueNameMap.put(normalizedName, true);
        }
        return null;
    }

    private String normalizeSelectType(String selectType) {
        if (selectType == null || selectType.trim().isEmpty()) {
            return "single";
        }
        return selectType.trim();
    }

    private void replaceGroupValues(Long groupId, List<DishOptionValuePayload> values) {
        dishOptionValueMapper.delete(new LambdaQueryWrapper<DishOptionValue>()
                .eq(DishOptionValue::getOptionGroupId, groupId));

        int fallbackSort = 1;
        for (DishOptionValuePayload valuePayload : values) {
            DishOptionValue value = new DishOptionValue();
            value.setOptionGroupId(groupId);
            value.setName(valuePayload.getName().trim());
            value.setExtraPrice(valuePayload.getExtraPrice() == null ? BigDecimal.ZERO : valuePayload.getExtraPrice());
            value.setIsDefault(valuePayload.getIsDefault() != null && valuePayload.getIsDefault() == 1 ? 1 : 0);
            value.setSort(valuePayload.getSort() == null ? fallbackSort : valuePayload.getSort());
            value.setCreateTime(LocalDateTime.now());
            value.setUpdateTime(LocalDateTime.now());
            dishOptionValueMapper.insert(value);
            fallbackSort += 1;
        }
    }

    private Map<String, Object> buildGroupDetail(Long groupId, Long shopId) {
        DishOptionGroup group = dishOptionGroupMapper.selectOne(new LambdaQueryWrapper<DishOptionGroup>()
                .eq(DishOptionGroup::getId, groupId)
                .eq(DishOptionGroup::getShopId, shopId)
                .last("LIMIT 1"));
        List<DishOptionValue> values = dishOptionValueMapper.selectList(new LambdaQueryWrapper<DishOptionValue>()
                .eq(DishOptionValue::getOptionGroupId, groupId)
                .orderByAsc(DishOptionValue::getSort)
                .orderByAsc(DishOptionValue::getId));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", group.getId());
        map.put("shopId", group.getShopId());
        map.put("name", group.getName());
        map.put("selectType", group.getSelectType());
        map.put("createTime", group.getCreateTime());
        map.put("updateTime", group.getUpdateTime());

        List<Map<String, Object>> valueList = new ArrayList<>();
        for (DishOptionValue value : values) {
            Map<String, Object> valueItem = new LinkedHashMap<>();
            valueItem.put("id", value.getId());
            valueItem.put("optionGroupId", value.getOptionGroupId());
            valueItem.put("name", value.getName());
            valueItem.put("extraPrice", value.getExtraPrice());
            valueItem.put("isDefault", value.getIsDefault());
            valueItem.put("sort", value.getSort());
            valueList.add(valueItem);
        }
        map.put("values", valueList);
        return map;
    }
}
