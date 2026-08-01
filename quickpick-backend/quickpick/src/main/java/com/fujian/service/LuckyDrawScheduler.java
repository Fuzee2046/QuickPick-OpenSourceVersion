package com.fujian.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.mapper.LuckyDrawConfigMapper;
import com.fujian.mapper.LuckyDrawReservationMapper;
import com.fujian.mapper.LuckyDrawWinnerMapper;
import com.fujian.mapper.UserMapper;
import com.fujian.pojo.LuckyDrawConfig;
import com.fujian.pojo.LuckyDrawReservation;
import com.fujian.pojo.LuckyDrawWinner;
import com.fujian.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Component
public class LuckyDrawScheduler {

    @Autowired
    private LuckyDrawConfigMapper luckyDrawConfigMapper;

    @Autowired
    private LuckyDrawReservationMapper luckyDrawReservationMapper;

    @Autowired
    private LuckyDrawWinnerMapper luckyDrawWinnerMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WechatSubscribeMessageService wechatSubscribeMessageService;

    @Autowired
    private DistributedJobLockExecutor distributedJobLockExecutor;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Scheduled(cron = "0 * * * * *")
    public void executeDraw() {
        LocalDate today = LocalDate.now();
        distributedJobLockExecutor.execute("lucky-draw", today.toString(),
                () -> transactionTemplate.executeWithoutResult(status -> executeDrawLocked(today)));
    }

    private void executeDrawLocked(LocalDate today) {
        LuckyDrawConfig config = luckyDrawConfigMapper.selectOne(new LambdaQueryWrapper<LuckyDrawConfig>()
                .eq(LuckyDrawConfig::getStatus, 1)
                .orderByDesc(LuckyDrawConfig::getId)
                .last("limit 1"));
        if (config == null) {
            return;
        }

        LocalTime now = LocalTime.now();
        if (now.isBefore(config.getDrawTime())) {
            return;
        }

        long drawnCount = luckyDrawWinnerMapper.selectCount(new LambdaQueryWrapper<LuckyDrawWinner>()
                .eq(LuckyDrawWinner::getDrawDate, today));
        if (drawnCount > 0) {
            return;
        }

        List<LuckyDrawReservation> reservations = luckyDrawReservationMapper.selectList(new LambdaQueryWrapper<LuckyDrawReservation>()
                .eq(LuckyDrawReservation::getReserveDate, today));
        if (reservations.isEmpty()) {
            return;
        }

        int winnerCount = Math.min(config.getMaxWinnersPerDay(), reservations.size());
        Collections.shuffle(reservations);

        for (int i = 0; i < winnerCount; i++) {
            LuckyDrawReservation reservation = reservations.get(i);
            LuckyDrawWinner winner = new LuckyDrawWinner();
            winner.setDrawDate(today);
            winner.setUserId(reservation.getUserId());
            winner.setPrizeType(config.getPrizeType());
            winner.setPrizeValue(config.getPrizeValue());
            winner.setStatus(0);
            winner.setDrawTime(LocalDateTime.now());
            luckyDrawWinnerMapper.insert(winner);

            // 发送微信订阅消息通知中奖用户
            try {
                User user = userMapper.selectById(reservation.getUserId());
                if (user != null && user.getOpenid() != null && !user.getOpenid().isEmpty()) {
                    boolean sendResult = wechatSubscribeMessageService.sendLuckyDrawWinnerNotification(
                            user.getOpenid(), config.getActivityName());
                    System.out.println("中奖通知发送结果: " + (sendResult ? "成功" : "失败") + 
                                     " | 用户ID: " + reservation.getUserId() + 
                                     " | OpenID: " + user.getOpenid());
                } else {
                    System.out.println("⚠️ 无法发送中奖通知: 用户不存在或OpenID为空 | 用户ID: " + reservation.getUserId());
                }
            } catch (Exception e) {
                System.out.println("❌ 发送中奖通知异常: " + e.getMessage());
                e.printStackTrace();
                // 通知发送失败不影响开奖流程，继续处理下一个中奖用户
            }
        }
    }
}
