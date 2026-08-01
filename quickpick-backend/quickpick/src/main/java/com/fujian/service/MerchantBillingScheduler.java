package com.fujian.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class MerchantBillingScheduler {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final MerchantBillingService billingService;
    private final DistributedJobLockExecutor distributedJobLockExecutor;

    public MerchantBillingScheduler(MerchantBillingService billingService,
                                    DistributedJobLockExecutor distributedJobLockExecutor) {
        this.billingService = billingService;
        this.distributedJobLockExecutor = distributedJobLockExecutor;
    }

    @Scheduled(cron = "0 5 0 1-3 * *", zone = "Asia/Shanghai")
    public void generateMonthlyBills() {
        LocalDate billingMonth = LocalDate.now(ZONE).withDayOfMonth(1).minusMonths(1);
        String scope = billingMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
        distributedJobLockExecutor.execute("merchant-billing", scope, billingService::generatePreviousMonth);
    }
}
