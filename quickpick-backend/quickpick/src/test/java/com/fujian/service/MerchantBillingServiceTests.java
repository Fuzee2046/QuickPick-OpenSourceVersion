package com.fujian.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MerchantBillingServiceTests {
    private final MerchantBillingService service = new MerchantBillingService(null);

    @Test
    void calculatesFreeOrdersAndPerOrderFee() {
        BigDecimal unit = new BigDecimal("0.30");
        assertEquals(new BigDecimal("0.00"), service.amount(0, 10, unit));
        assertEquals(new BigDecimal("0.00"), service.amount(10, 10, unit));
        assertEquals(new BigDecimal("0.30"), service.amount(11, 10, unit));
        assertEquals(new BigDecimal("57.00"), service.amount(200, 10, unit));
    }
}
