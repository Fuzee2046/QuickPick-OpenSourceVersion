package com.fujian.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.mapper.ReservationRuleConfigMapper;
import com.fujian.pojo.ReservationRuleConfig;
import com.fujian.pojo.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class ReservationRuleService {
    private static final int DEFAULT_OFF_PEAK_MINUTES = 15;
    private static final int DEFAULT_PEAK_MINUTES = 25;

    @Autowired
    private ReservationRuleConfigMapper reservationRuleConfigMapper;

    @Autowired
    private ShopCatalogCacheService shopCatalogCacheService;

    public ReservationRuleConfig getCurrentConfig() {
        return shopCatalogCacheService.getReservationRule(this::loadCurrentConfig);
    }

    private ReservationRuleConfig loadCurrentConfig() {
        ReservationRuleConfig config = reservationRuleConfigMapper.selectOne(
                new LambdaQueryWrapper<ReservationRuleConfig>().orderByDesc(ReservationRuleConfig::getId).last("LIMIT 1")
        );

        if (config == null) {
            config = new ReservationRuleConfig();
            config.setOffPeakMinMinutes(DEFAULT_OFF_PEAK_MINUTES);
            config.setPeakMinMinutes(DEFAULT_PEAK_MINUTES);
            config.setWorkdayOnly(1);
        }

        if (config.getOffPeakMinMinutes() == null || config.getOffPeakMinMinutes() <= 0) {
            config.setOffPeakMinMinutes(DEFAULT_OFF_PEAK_MINUTES);
        }
        if (config.getPeakMinMinutes() == null || config.getPeakMinMinutes() <= 0) {
            config.setPeakMinMinutes(DEFAULT_PEAK_MINUTES);
        }

        return config;
    }

    public int getRequiredAdvanceMinutes(Shop shop, LocalDate date, LocalTime pickupTime) {
        ReservationRuleConfig config = getCurrentConfig();
        if (shop == null || pickupTime == null) {
            return config.getOffPeakMinMinutes();
        }

        if (!isPeakLimitEnabled(shop)) {
            return config.getOffPeakMinMinutes();
        }

        if (Boolean.TRUE.equals(isWorkdayOnly(config)) && !isWorkday(date)) {
            return config.getOffPeakMinMinutes();
        }

        if (isInPeakWindow(pickupTime, config.getLunchPeakStart(), config.getLunchPeakEnd())
                || isInPeakWindow(pickupTime, config.getDinnerPeakStart(), config.getDinnerPeakEnd())) {
            return config.getPeakMinMinutes();
        }

        return config.getOffPeakMinMinutes();
    }

    public boolean isPeakLimitEnabled(Shop shop) {
        return shop != null && shop.getPeakLimitEnabled() != null && shop.getPeakLimitEnabled() == 1;
    }

    private Boolean isWorkdayOnly(ReservationRuleConfig config) {
        return config != null && config.getWorkdayOnly() != null && config.getWorkdayOnly() == 1;
    }

    private boolean isWorkday(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        DayOfWeek day = targetDate.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    private boolean isInPeakWindow(LocalTime target, LocalTime start, LocalTime end) {
        if (target == null || start == null || end == null) {
            return false;
        }
        return !target.isBefore(start) && !target.isAfter(end);
    }
}
