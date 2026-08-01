package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.LuckyDrawConfigMapper;
import com.fujian.mapper.LuckyDrawReservationMapper;
import com.fujian.mapper.LuckyDrawWinnerMapper;
import com.fujian.pojo.LuckyDrawConfig;
import com.fujian.pojo.LuckyDrawReservation;
import com.fujian.pojo.LuckyDrawWinner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/client/lucky-draw")
public class LuckyDrawController {

    @Autowired
    private LuckyDrawConfigMapper luckyDrawConfigMapper;

    @Autowired
    private LuckyDrawReservationMapper luckyDrawReservationMapper;

    @Autowired
    private LuckyDrawWinnerMapper luckyDrawWinnerMapper;

    @GetMapping("/info")
    public Result<Map<String, Object>> getTodayLuckyDrawInfo() {
        LuckyDrawConfig config = getActiveConfig();
        if (config == null) {
            return Result.error("活动未开启");
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalTime now = LocalTime.now();
        Long userId = getCurrentUserId();

        long reservationCount = luckyDrawReservationMapper.selectCount(new LambdaQueryWrapper<LuckyDrawReservation>()
                .eq(LuckyDrawReservation::getReserveDate, today));

        boolean userReserved = false;
        LuckyDrawWinner myWinnerRecord = null;
        LuckyDrawWinner yesterdayWinnerRecord = null;
        if (userId != null) {
            userReserved = luckyDrawReservationMapper.selectCount(new LambdaQueryWrapper<LuckyDrawReservation>()
                    .eq(LuckyDrawReservation::getReserveDate, today)
                    .eq(LuckyDrawReservation::getUserId, userId)) > 0;
            myWinnerRecord = luckyDrawWinnerMapper.selectOne(new LambdaQueryWrapper<LuckyDrawWinner>()
                    .eq(LuckyDrawWinner::getDrawDate, today)
                    .eq(LuckyDrawWinner::getUserId, userId)
                    .last("limit 1"));
            yesterdayWinnerRecord = luckyDrawWinnerMapper.selectOne(new LambdaQueryWrapper<LuckyDrawWinner>()
                    .eq(LuckyDrawWinner::getDrawDate, yesterday)
                    .eq(LuckyDrawWinner::getUserId, userId)
                    .last("limit 1"));
        }

        List<LuckyDrawWinner> winnerList = luckyDrawWinnerMapper.selectList(new LambdaQueryWrapper<LuckyDrawWinner>()
                .eq(LuckyDrawWinner::getDrawDate, today));
        List<LuckyDrawReservation> reservationList = luckyDrawReservationMapper.selectList(new LambdaQueryWrapper<LuckyDrawReservation>()
                .eq(LuckyDrawReservation::getReserveDate, today)
                .orderByAsc(LuckyDrawReservation::getCreateTime, LuckyDrawReservation::getId));

        Set<Long> winnerUserIds = new HashSet<>();
        for (LuckyDrawWinner winner : winnerList) {
            winnerUserIds.add(winner.getUserId());
        }

        List<Map<String, Object>> reservationDots = new ArrayList<>();
        boolean bubbleAssigned = false;
        for (LuckyDrawReservation reservation : reservationList) {
            boolean isWinner = winnerUserIds.contains(reservation.getUserId());
            boolean isMine = userId != null && userId.equals(reservation.getUserId());
            boolean showBubble = isWinner && !bubbleAssigned;
            if (showBubble) {
                bubbleAssigned = true;
            }

            Map<String, Object> dot = new HashMap<>();
            dot.put("id", reservation.getId());
            dot.put("isWinner", isWinner);
            dot.put("isMine", isMine);
            dot.put("showBubble", showBubble);
            reservationDots.add(dot);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("activityName", config.getActivityName());
        data.put("status", config.getStatus());
        data.put("reserveStartTime", formatTime(config.getReserveStartTime()));
        data.put("reserveEndTime", formatTime(config.getReserveEndTime()));
        data.put("drawTime", formatTime(config.getDrawTime()));
        data.put("prizeType", config.getPrizeType());
        data.put("prizeValue", config.getPrizeValue());
        data.put("maxWinnersPerDay", config.getMaxWinnersPerDay());
        data.put("description", config.getDescription());
        data.put("drawDate", today.toString());
        data.put("currentTime", formatTime(now));
        data.put("reservationCount", reservationCount);
        data.put("userReserved", userReserved);
        data.put("canReserve", canReserve(now, config) && !userReserved);
        data.put("phase", calculatePhase(now, config, winnerList.isEmpty()));
        data.put("drawn", !winnerList.isEmpty());
        data.put("winnerCount", winnerList.size());
        data.put("reservationDots", reservationDots);
        data.put("isWinner", myWinnerRecord != null);
        data.put("isYesterdayWinner", yesterdayWinnerRecord != null);

        if (myWinnerRecord != null) {
            Map<String, Object> winnerInfo = new HashMap<>();
            winnerInfo.put("prizeType", myWinnerRecord.getPrizeType());
            winnerInfo.put("prizeValue", myWinnerRecord.getPrizeValue());
            winnerInfo.put("status", myWinnerRecord.getStatus());
            winnerInfo.put("drawTime", myWinnerRecord.getDrawTime());
            winnerInfo.put("redeemWechat", config.getRedeemWechat());
            data.put("myWinnerInfo", winnerInfo);
        }
        if (yesterdayWinnerRecord != null) {
            Map<String, Object> yesterdayWinnerInfo = new HashMap<>();
            yesterdayWinnerInfo.put("prizeType", yesterdayWinnerRecord.getPrizeType());
            yesterdayWinnerInfo.put("prizeValue", yesterdayWinnerRecord.getPrizeValue());
            yesterdayWinnerInfo.put("status", yesterdayWinnerRecord.getStatus());
            yesterdayWinnerInfo.put("drawTime", yesterdayWinnerRecord.getDrawTime());
            yesterdayWinnerInfo.put("drawDate", yesterday.toString());
            yesterdayWinnerInfo.put("redeemWechat", config.getRedeemWechat());
            data.put("yesterdayWinnerInfo", yesterdayWinnerInfo);
        }

        return Result.success(data);
    }

    @PostMapping("/reserve")
    public Result<String> reserveTodayLuckyDraw() {
        LuckyDrawConfig config = getActiveConfig();
        if (config == null) {
            return Result.error("活动未开启");
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        if (!canReserve(now, config)) {
            return Result.error("当前不在预约时间内");
        }

        long exists = luckyDrawReservationMapper.selectCount(new LambdaQueryWrapper<LuckyDrawReservation>()
                .eq(LuckyDrawReservation::getUserId, userId)
                .eq(LuckyDrawReservation::getReserveDate, today));
        if (exists > 0) {
            return Result.error("今日已预约，请勿重复提交");
        }

        LuckyDrawReservation reservation = new LuckyDrawReservation();
        reservation.setUserId(userId);
        reservation.setReserveDate(today);
        luckyDrawReservationMapper.insert(reservation);
        return Result.success("预约成功，祝你好运");
    }

    private LuckyDrawConfig getActiveConfig() {
        return luckyDrawConfigMapper.selectOne(new LambdaQueryWrapper<LuckyDrawConfig>()
                .eq(LuckyDrawConfig::getStatus, 1)
                .orderByDesc(LuckyDrawConfig::getId)
                .last("limit 1"));
    }

    private Long getCurrentUserId() {
        try {
            return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean canReserve(LocalTime now, LuckyDrawConfig config) {
        return !now.isBefore(config.getReserveStartTime()) && !now.isAfter(config.getReserveEndTime());
    }

    private String calculatePhase(LocalTime now, LuckyDrawConfig config, boolean noWinnerYet) {
        if (now.isBefore(config.getReserveStartTime())) {
            return "not_started";
        }
        if (!now.isAfter(config.getReserveEndTime())) {
            return "reserving";
        }
        if (now.isBefore(config.getDrawTime()) || (noWinnerYet && !now.isBefore(config.getDrawTime()))) {
            return "waiting_draw";
        }
        return "drawn";
    }

    private String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
