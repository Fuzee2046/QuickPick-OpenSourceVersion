package com.fujian.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MerchantBillingService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate FIRST_BILLING_MONTH = LocalDate.of(2026, 8, 1);
    private final JdbcTemplate jdbc;

    public MerchantBillingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> currentPlan(LocalDate month) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT * FROM merchant_billing_plans
                WHERE effective_month<=? ORDER BY effective_month DESC LIMIT 1
                """, Date.valueOf(month.withDayOfMonth(1)));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> overviewPlan(LocalDate month) {
        Map<String, Object> plan = currentPlan(month);
        if (plan != null) return plan;
        List<Map<String, Object>> upcoming = jdbc.queryForList("""
                SELECT * FROM merchant_billing_plans
                WHERE effective_month>? ORDER BY effective_month ASC LIMIT 1
                """, Date.valueOf(month.withDayOfMonth(1)));
        return upcoming.isEmpty() ? null : upcoming.get(0);
    }

    public boolean isOrderingAllowed(Long shopId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM merchant_bills
                WHERE shop_id=? AND status='unpaid' AND due_time<?
                """, Long.class, shopId, Timestamp.valueOf(LocalDateTime.now(ZONE)));
        return count == null || count == 0;
    }

    public Map<String, Object> overview(Long shopId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate month = today.withDayOfMonth(1);
        Map<String, Object> plan = overviewPlan(month);
        int completed = countCompletedOrders(shopId, month, month.plusMonths(1));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceAvailable", isOrderingAllowed(shopId));
        result.put("billingMonth", month);
        result.put("completedOrderCount", completed);
        boolean planActive = plan != null
                && !asLocalDate(plan.get("effective_month")).isAfter(month)
                && asBoolean(plan.get("billing_enabled"));
        if (plan == null) {
            result.put("billingEnabled", false);
            result.put("billingConfiguredEnabled", false);
            result.put("planActive", false);
            result.put("alipayWapEnabled", false);
            result.put("planEffectiveMonth", null);
            result.put("freeOrderCount", 0);
            result.put("unitPrice", BigDecimal.ZERO.setScale(2));
            result.put("estimatedAmount", BigDecimal.ZERO.setScale(2));
        } else {
            int free = asInt(plan.get("free_order_count"));
            BigDecimal unit = asDecimal(plan.get("unit_price"));
            result.put("billingEnabled", planActive);
            result.put("billingConfiguredEnabled", asBoolean(plan.get("billing_enabled")));
            result.put("planActive", planActive);
            result.put("alipayWapEnabled", asBoolean(plan.get("alipay_wap_enabled")));
            result.put("planEffectiveMonth", plan.get("effective_month"));
            result.put("freeOrderCount", free);
            result.put("unitPrice", unit);
            result.put("estimatedAmount", planActive ? amount(completed, free, unit) : BigDecimal.ZERO.setScale(2));
        }
        List<Map<String, Object>> overdue = jdbc.queryForList("""
                SELECT id,bill_no,billing_month,payable_amount,due_time,status
                FROM merchant_bills WHERE shop_id=? AND status='unpaid' AND due_time<? ORDER BY billing_month
                """, shopId, Timestamp.valueOf(LocalDateTime.now(ZONE)));
        result.put("overdueBills", overdue);
        result.put("paymentReminder", paymentReminder(shopId, today));
        return result;
    }

    private Map<String, Object> paymentReminder(Long shopId, LocalDate today) {
        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList("""
                    SELECT b.id,b.billing_month,b.payable_amount,b.due_time,b.status,
                           b.billable_order_count, r.last_dismiss_date
                    FROM merchant_bills b
                    LEFT JOIN merchant_billing_reminders r ON r.bill_id=b.id AND r.shop_id=b.shop_id
                    WHERE b.shop_id=? AND b.status='unpaid'
                    ORDER BY b.billing_month ASC,b.due_time ASC LIMIT 1
                    """, shopId);
        } catch (DataAccessException ex) {
            // 兼容已部署但尚未执行 5.0-2 增量 SQL 的旧数据库；账单主流程不能被提醒表阻断。
            String message = String.valueOf(ex.getMostSpecificCause().getMessage());
            if (!message.contains("merchant_billing_reminders")) throw ex;
            System.err.println("merchant_billing_reminders is unavailable; skip reminder lookup: " + message);
            return null;
        }
        if (rows.isEmpty()) return null;
        Map<String, Object> bill = rows.get(0);
        Object dismissed = bill.get("last_dismiss_date");
        boolean shouldShow = dismissed == null || !today.equals(asLocalDate(dismissed));
        Map<String, Object> reminder = new LinkedHashMap<>();
        reminder.put("billId", ((Number) bill.get("id")).longValue());
        reminder.put("billingMonth", bill.get("billing_month"));
        reminder.put("amount", bill.get("payable_amount"));
        reminder.put("dueTime", bill.get("due_time"));
        reminder.put("billableOrderCount", bill.get("billable_order_count"));
        reminder.put("overdue", bill.get("due_time") instanceof Timestamp due
                && due.toLocalDateTime().isBefore(LocalDateTime.now(ZONE)));
        reminder.put("shouldShow", shouldShow);
        return reminder;
    }

    @Transactional
    public void dismissReminder(Long shopId, long billId) {
        LocalDate today = LocalDate.now(ZONE);
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM merchant_bills WHERE id=? AND shop_id=? AND status='unpaid'
                """, Long.class, billId, shopId);
        if (count == null || count == 0) throw new IllegalArgumentException("账单不存在或无需提醒");
        jdbc.update("""
                INSERT INTO merchant_billing_reminders(shop_id,bill_id,last_dismiss_date)
                VALUES (?,?,?)
                ON DUPLICATE KEY UPDATE last_dismiss_date=VALUES(last_dismiss_date)
                """, shopId, billId, Date.valueOf(today));
    }

    private LocalDate asLocalDate(Object value) {
        if (value instanceof Date date) return date.toLocalDate();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        return value instanceof LocalDate date ? date : LocalDate.parse(String.valueOf(value));
    }

    public Map<String, Object> listBills(Long shopId, int page, int pageSize) {
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        int safePage = Math.max(page, 1);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM merchant_bills WHERE shop_id=?", Long.class, shopId);
        List<Map<String, Object>> records = jdbc.queryForList("""
                SELECT id,bill_no,billing_month,completed_order_count,free_order_count,billable_order_count,
                       unit_price,original_amount,adjustment_amount,payable_amount,status,due_time,paid_time,create_time
                FROM merchant_bills WHERE shop_id=? ORDER BY billing_month DESC LIMIT ? OFFSET ?
                """, shopId, safeSize, (safePage - 1) * safeSize);
        return Map.of("records", records, "total", total == null ? 0 : total, "page", safePage, "pageSize", safeSize);
    }

    @Transactional
    public int generateMonth(LocalDate billingMonth) {
        LocalDate month = billingMonth.withDayOfMonth(1);
        if (month.isBefore(FIRST_BILLING_MONTH) || !month.isBefore(LocalDate.now(ZONE).withDayOfMonth(1))) return 0;
        Map<String, Object> plan = currentPlan(month);
        if (plan == null || !asBoolean(plan.get("billing_enabled"))) return 0;
        int free = asInt(plan.get("free_order_count"));
        int grace = asInt(plan.get("grace_days"));
        BigDecimal unit = asDecimal(plan.get("unit_price"));
        long planId = ((Number) plan.get("id")).longValue();
        int created = 0;
        for (Map<String, Object> shop : jdbc.queryForList("SELECT id FROM shops")) {
            long shopId = ((Number) shop.get("id")).longValue();
            int completed = countCompletedOrders(shopId, month, month.plusMonths(1));
            int billable = Math.max(completed - free, 0);
            BigDecimal total = amount(completed, free, unit);
            String billNo = "B" + month.format(DateTimeFormatter.ofPattern("yyyyMM")) + String.format("%08d", shopId);
            LocalDateTime due = month.plusMonths(1).plusDays(grace - 1L).atTime(23, 59, 59);
            try {
                jdbc.update("""
                        INSERT INTO merchant_bills
                        (bill_no,shop_id,billing_month,period_start,period_end,completed_order_count,free_order_count,
                         billable_order_count,unit_price,original_amount,adjustment_amount,payable_amount,status,due_time,plan_id)
                        VALUES (?,?,?,?,?,?,?,?,?,?,0,?,?,?,?)
                        """, billNo, shopId, Date.valueOf(month), Timestamp.valueOf(month.atStartOfDay()),
                        Timestamp.valueOf(month.plusMonths(1).atStartOfDay()), completed, free, billable, unit, total, total,
                        total.signum() == 0 ? "waived" : "unpaid", Timestamp.valueOf(due), planId);
                created++;
            } catch (DuplicateKeyException ignored) {
                // Idempotent monthly generation.
            }
        }
        return created;
    }

    @Transactional
    public int generatePreviousMonth() {
        return generateMonth(LocalDate.now(ZONE).withDayOfMonth(1).minusMonths(1));
    }

    public int countCompletedOrders(Long shopId, LocalDate start, LocalDate end) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM orders
                WHERE shop_id=? AND status='completed' AND completed_time>=? AND completed_time<?
                """, Integer.class, shopId, Timestamp.valueOf(start.atStartOfDay()), Timestamp.valueOf(end.atStartOfDay()));
        return count == null ? 0 : count;
    }

    public BigDecimal amount(int completed, int free, BigDecimal unit) {
        return unit.multiply(BigDecimal.valueOf(Math.max(completed - free, 0))).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void markPaid(long billId, String outTradeNo, String transactionId, LocalDateTime successTime) {
        int updated = jdbc.update("""
                UPDATE merchant_payment_transactions SET status='success',provider_trade_no=?,success_time=?
                WHERE out_trade_no=? AND status<>'success'
                """, transactionId, Timestamp.valueOf(successTime), outTradeNo);
        if (updated > 0) {
            jdbc.update("UPDATE merchant_bills SET status='paid',paid_time=? WHERE id=? AND status='unpaid'",
                    Timestamp.valueOf(successTime), billId);
        }
    }

    public static boolean asBoolean(Object value) { return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() == 1; }
    public static int asInt(Object value) { return value == null ? 0 : ((Number) value).intValue(); }
    public static BigDecimal asDecimal(Object value) { return value instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(value)); }
}
