package com.fujian.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.mapper.OrderMapper;
import com.fujian.mapper.ShopMapper;
import com.fujian.mapper.UserMapper;
import com.fujian.pojo.Order;
import com.fujian.pojo.Shop;
import com.fujian.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class PickupReminderScheduler {
    private static final int SECOND_REMINDER_DELAY_MINUTES = 15;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WechatSubscribeMessageService wechatSubscribeMessageService;

    @Autowired
    private DistributedJobLockExecutor distributedJobLockExecutor;

    @Scheduled(cron = "0 * * * * ?")
    public void sendSecondPickupReminders() {
        distributedJobLockExecutor.execute("pickup-reminder", "global", this::sendSecondPickupRemindersLocked);
    }

    private void sendSecondPickupRemindersLocked() {
        LocalDateTime now = LocalDateTime.now();

        List<Order> pendingOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "pending")
                .eq(Order::getBizDate, LocalDate.now())
                .and(wrapper -> wrapper.isNull(Order::getSecondPickupReminderSent)
                        .or()
                        .eq(Order::getSecondPickupReminderSent, 0))
                .orderByAsc(Order::getUpdateTime));

        for (Order order : pendingOrders) {
            try {
                Shop shop = shopMapper.selectById(order.getShopId());
                if (shop == null || shop.getTasteSensitiveEnabled() == null || shop.getTasteSensitiveEnabled() != 1) {
                    continue;
                }

                LocalDateTime baseTime = buildPickupBaseTime(order);
                if (baseTime == null || now.isBefore(baseTime.plusMinutes(SECOND_REMINDER_DELAY_MINUTES))) {
                    continue;
                }

                User user = userMapper.selectById(order.getUserId());
                boolean sent = false;
                if (user != null && user.getOpenid() != null && !user.getOpenid().isEmpty()) {
                    String pickupTimeText = order.getPickupTime() != null
                            ? order.getPickupTime().toString().substring(0, 5)
                            : "未知时间";
                    sent = wechatSubscribeMessageService.sendPickupOvertimeReminder(
                            user.getOpenid(),
                            pickupTimeText,
                            shop.getName(),
                            shop.getAddress(),
                            order.getPickupCode(),
                            order.getId()
                    ).join();
                }

                if (sent) {
                    order.setSecondPickupReminderSent(1);
                    order.setSecondPickupReminderTime(now);
                    order.setUpdateTime(now);
                    orderMapper.updateById(order);
                }

                System.out.println("即时口感二次提醒处理完成，订单ID=" + order.getId() + "，发送结果=" + sent);
            } catch (Exception e) {
                System.out.println("处理二次取餐提醒失败，订单ID=" + order.getId() + "，原因=" + e.getMessage());
            }
        }
    }

    private LocalDateTime buildPickupBaseTime(Order order) {
        if (order == null || order.getBizDate() == null || order.getPickupTime() == null) {
            return null;
        }

        LocalDateTime scheduledPickupTime = LocalDateTime.of(order.getBizDate(), order.getPickupTime());
        if (order.getReadyTime() == null || !order.getReadyTime().isAfter(scheduledPickupTime)) {
            return scheduledPickupTime;
        }

        return order.getReadyTime();
    }
}
