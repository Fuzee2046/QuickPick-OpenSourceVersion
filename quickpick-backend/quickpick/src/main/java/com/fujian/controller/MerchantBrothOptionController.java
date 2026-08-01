package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.ShopBrothOptionMapper;
import com.fujian.pojo.ShopBrothOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/merchant/broth-options")
public class MerchantBrothOptionController {

    @Autowired
    private ShopBrothOptionMapper shopBrothOptionMapper;

    private Long getCurrentShopId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public Result<List<ShopBrothOption>> list() {
        List<ShopBrothOption> options = shopBrothOptionMapper.selectList(new LambdaQueryWrapper<ShopBrothOption>()
                .eq(ShopBrothOption::getShopId, getCurrentShopId())
                .orderByAsc(ShopBrothOption::getSort));
        return Result.success(options);
    }

    @PostMapping
    public Result<String> create(@RequestBody ShopBrothOption option) {
        option.setShopId(getCurrentShopId());
        option.setCreateTime(LocalDateTime.now());
        option.setStatus(option.getStatus() == null ? 1 : option.getStatus());
        shopBrothOptionMapper.insert(option);
        return Result.success("创建成功");
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody ShopBrothOption option) {
        ShopBrothOption existing = shopBrothOptionMapper.selectById(id);
        if (existing == null || !existing.getShopId().equals(getCurrentShopId())) {
            return Result.error("汤底选项不存在");
        }
        option.setId(id);
        option.setShopId(getCurrentShopId());
        option.setUpdateTime(LocalDateTime.now());
        shopBrothOptionMapper.updateById(option);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        ShopBrothOption existing = shopBrothOptionMapper.selectById(id);
        if (existing == null || !existing.getShopId().equals(getCurrentShopId())) {
            return Result.error("汤底选项不存在");
        }
        shopBrothOptionMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
