package com.fujian.service;

import com.fujian.mapper.ShopDailyCounterMapper;
import com.fujian.pojo.ShopDailyCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class OrderIdGeneratorService {

    @Autowired
    private ShopDailyCounterMapper shopDailyCounterMapper;

    public static class GeneratedOrderNumbers {
        private final String orderId;
        private final String pickupCode;
        private final LocalDate bizDate;

        public GeneratedOrderNumbers(String orderId, String pickupCode, LocalDate bizDate) {
            this.orderId = orderId;
            this.pickupCode = pickupCode;
            this.bizDate = bizDate;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getPickupCode() {
            return pickupCode;
        }

        public LocalDate getBizDate() {
            return bizDate;
        }
    }

    public String getShopCode(Long shopId) {
        if (shopId == null || shopId <= 0) {
            return "S";
        }

        long n = shopId;
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + n % 26));
            n /= 26;
        }
        return sb.toString();
    }

    public GeneratedOrderNumbers generateOrderNumbers(Long shopId, LocalDate bizDate) {
        String shopCode = getShopCode(shopId);
        String date = bizDate.format(DateTimeFormatter.ofPattern("yyMMdd"));

        shopDailyCounterMapper.ensureDailyCounterRow(shopId, bizDate);
        ShopDailyCounter counter = shopDailyCounterMapper.selectForUpdate(shopId, bizDate);
        if (counter == null) {
            throw new IllegalStateException("未找到店铺当日计数记录");
        }

        int nextOrderSeq = counter.getOrderSeq() + 1;
        int nextPickupSeq = counter.getPickupSeq() + 1;

        counter.setOrderSeq(nextOrderSeq);
        counter.setPickupSeq(nextPickupSeq);
        shopDailyCounterMapper.updateById(counter);

        String orderId = shopCode + date + String.format("%04d", nextOrderSeq);
        String pickupCode = shopCode + String.format("%03d", nextPickupSeq);
        return new GeneratedOrderNumbers(orderId, pickupCode, bizDate);
    }
}
