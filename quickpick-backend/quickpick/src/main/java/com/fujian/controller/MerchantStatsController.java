package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.OrderMapper;
import com.fujian.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/stats")
public class MerchantStatsController {

    @Autowired
    private OrderMapper orderMapper;

    private Long getCurrentShopId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/today")
    public Result<Map<String, Object>> getTodayStats() {
        Long shopId = getCurrentShopId();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(today, LocalTime.MAX);

        // 1. 待制作订单数 (making) - 统计所有当前处于制作中的订单，不限时间
        Long makingOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getBizDate, today)
                .eq(Order::getStatus, "making"));

        // 2. 待取餐订单数 (pending) - 统计所有当前处于待取餐的订单，不限时间
        Long pendingOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getBizDate, today)
                .eq(Order::getStatus, "pending"));

        // 3. 今日完成订单数 (completed)
        Long completedOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getStatus, "completed")
                .between(Order::getCreateTime, startOfDay, endOfDay));

        // 4. 今日总收入
        List<Order> todayCompletedOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getStatus, "completed")
                .between(Order::getCreateTime, startOfDay, endOfDay));
        
        BigDecimal totalRevenue = todayCompletedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("makingOrders", makingOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("completedOrders", completedOrders);
        stats.put("totalRevenue", totalRevenue);

        return Result.success(stats);
    }
}
