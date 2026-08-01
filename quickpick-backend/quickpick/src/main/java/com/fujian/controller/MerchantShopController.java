package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.ShopMapper;
import com.fujian.pojo.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/shop")
public class MerchantShopController {

    @Autowired
    private ShopMapper shopMapper;

    private Long getCurrentShopId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public Result<Shop> getCurrentShop() {
        Long shopId = getCurrentShopId();
        Shop shop = shopMapper.selectById(shopId);
        return Result.success(shop);
    }

    @PutMapping("/status")
    public Result<String> updateStatus(@RequestBody Map<String, Integer> params) {
        Long shopId = getCurrentShopId();
        Integer status = params.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("状态值无效");
        }

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setStatus(status);
        shopMapper.updateById(shop);

        return Result.success("状态更新成功");
    }

    @PutMapping("/business-hours")
    public Result<String> updateBusinessHours(@RequestBody Shop params) {
        Long shopId = getCurrentShopId();

        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shopId)
                // 使用 set 显式更新字段，确保 null 也能正确写回数据库
                .set(Shop::getOpenTime1, params.getOpenTime1())
                .set(Shop::getCloseTime1, params.getCloseTime1())
                .set(Shop::getOpenTime2, params.getOpenTime2())
                .set(Shop::getCloseTime2, params.getCloseTime2());

        shopMapper.update(null, updateWrapper);
        return Result.success("营业时间更新成功");
    }

    @PutMapping("/peak-limit")
    public Result<String> updatePeakLimit(@RequestBody Map<String, Integer> params) {
        Long shopId = getCurrentShopId();
        Integer peakLimitEnabled = params.get("peakLimitEnabled");
        if (peakLimitEnabled == null || (peakLimitEnabled != 0 && peakLimitEnabled != 1)) {
            return Result.error("高峰限制开关参数无效");
        }

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setPeakLimitEnabled(peakLimitEnabled);
        shopMapper.updateById(shop);

        return Result.success(peakLimitEnabled == 1 ? "已开启高峰预约限制" : "已关闭高峰预约限制");
    }

    @PutMapping("/weight-config")
    public Result<String> updateWeightConfig(@RequestBody Shop params) {
        Long shopId = getCurrentShopId();
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return Result.error("店铺不存在");
        }

        String shopMode = params.getShopMode();
        if (shopMode == null || (!"fixed_dish".equals(shopMode) && !"weight_selection".equals(shopMode))) {
            return Result.error("店铺模式参数无效");
        }
        if ("weight_selection".equals(shopMode)) {
            if (params.getWeightPricePer500g() == null || params.getWeightPricePer500g().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return Result.error("请输入有效的每500g价格");
            }
            if (params.getMinimumOrderWeightG() == null || params.getMinimumOrderWeightG() < 0) {
                return Result.error("最低下单重量参数无效");
            }
        }

        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .set(Shop::getShopMode, shopMode)
                .set(Shop::getWeightPricePer500g, "weight_selection".equals(shopMode) ? params.getWeightPricePer500g() : null)
                .set(Shop::getMinimumOrderWeightG, "weight_selection".equals(shopMode) ? params.getMinimumOrderWeightG() : 0);

        shopMapper.update(null, updateWrapper);
        return Result.success("fixed_dish".equals(shopMode) ? "已切换为固定菜品店" : "已保存自选称重店配置");
    }
}
