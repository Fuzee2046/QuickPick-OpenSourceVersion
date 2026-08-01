package com.fujian.controller;

import com.fujian.common.Result;
import com.fujian.service.AdminAuditService;
import com.fujian.service.MerchantBillingService;
import com.fujian.service.AlipayMerchantPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/billing")
public class AdminBillingController {
    private final JdbcTemplate jdbc;
    private final MerchantBillingService billingService;
    private final AdminAuditService auditService;
    private final AlipayMerchantPaymentService paymentService;

    public AdminBillingController(JdbcTemplate jdbc, MerchantBillingService billingService, AdminAuditService auditService,
                                  AlipayMerchantPaymentService paymentService) {
        this.jdbc = jdbc;
        this.billingService = billingService;
        this.auditService = auditService;
        this.paymentService = paymentService;
    }

    @GetMapping("/plans")
    public Result<List<Map<String, Object>>> plans() {
        return Result.success(jdbc.queryForList("SELECT * FROM merchant_billing_plans ORDER BY effective_month DESC"));
    }

    @PutMapping("/plans/{id}")
    public Result<Void> updatePlan(@PathVariable long id, @RequestBody PlanRequest body, HttpServletRequest request) {
        if (body.getFreeOrderCount() == null || body.getFreeOrderCount() < 0 || body.getUnitPrice() == null || body.getUnitPrice().signum() < 0)
            return Result.error("免费订单数和单价不能为负数");
        if (body.getGraceDays() == null || body.getGraceDays() < 1 || body.getGraceDays() > 31)
            return Result.error("支付期限必须为1至31天");
        int updated = jdbc.update("""
                UPDATE merchant_billing_plans
                SET billing_enabled=?,free_order_count=?,unit_price=?,grace_days=?,alipay_wap_enabled=?
                WHERE id=?
                """, flag(body.getBillingEnabled()), body.getFreeOrderCount(), body.getUnitPrice(),
                body.getGraceDays(), flag(body.getAlipayWapEnabled()), id);
        if (updated == 0) return Result.error("计费方案不存在");
        auditService.record(adminId(), "UPDATE_BILLING_PLAN", "billing_plan", String.valueOf(id), "修改当前运营成本费方案", request);
        return Result.success(null);
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam(required = false) String billingMonth) {
        List<Object> args = new ArrayList<>();
        String monthWhere = "";
        if (billingMonth != null && !billingMonth.isBlank()) {
            monthWhere = " WHERE b.billing_month=?";
            args.add(Date.valueOf(LocalDate.parse(billingMonth).withDayOfMonth(1)));
        }
        Map<String, Object> totals = jdbc.queryForMap("""
                SELECT COUNT(*) bill_count,
                       COALESCE(SUM(b.original_amount),0) total_billed,
                       COALESCE(SUM(CASE WHEN b.status='paid' THEN b.payable_amount ELSE 0 END),0) total_received,
                       COALESCE(SUM(CASE WHEN b.status='unpaid' THEN b.payable_amount ELSE 0 END),0) pending_amount,
                       COALESCE(SUM(CASE WHEN b.status IN ('waived','void') THEN b.original_amount
                                         ELSE ABS(b.adjustment_amount) END),0) discounted_amount,
                       SUM(CASE WHEN b.status='paid' THEN 1 ELSE 0 END) paid_count,
                       SUM(CASE WHEN b.status='unpaid' THEN 1 ELSE 0 END) unpaid_count,
                       SUM(CASE WHEN b.status='unpaid' AND b.due_time<NOW() THEN 1 ELSE 0 END) overdue_count
                FROM merchant_bills b
                """ + monthWhere, args.toArray());
        List<Map<String, Object>> shopIncome = jdbc.queryForList("""
                SELECT b.shop_id,s.name shop_name,COUNT(*) bill_count,
                       COALESCE(SUM(b.original_amount),0) billed_amount,
                       COALESCE(SUM(CASE WHEN b.status='paid' THEN b.payable_amount ELSE 0 END),0) received_amount,
                       COALESCE(SUM(CASE WHEN b.status='unpaid' THEN b.payable_amount ELSE 0 END),0) pending_amount,
                       COALESCE(SUM(ABS(b.adjustment_amount)),0) discounted_amount
                FROM merchant_bills b JOIN shops s ON s.id=b.shop_id
                """ + monthWhere + " GROUP BY b.shop_id,s.name ORDER BY received_amount DESC,billed_amount DESC", args.toArray());
        List<Map<String, Object>> paymentChannels = jdbc.queryForList("""
                SELECT p.channel,p.status,COUNT(*) transaction_count,COALESCE(SUM(p.amount),0) amount
                FROM merchant_payment_transactions p
                JOIN merchant_bills b ON b.id=p.bill_id
                """ + monthWhere + " GROUP BY p.channel,p.status ORDER BY p.channel,p.status", args.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totals", totals);
        result.put("shopIncome", shopIncome);
        result.put("paymentChannels", paymentChannels);
        result.put("months", jdbc.queryForList("SELECT DISTINCT billing_month FROM merchant_bills ORDER BY billing_month DESC"));
        return Result.success(result);
    }

    @PostMapping("/plans")
    public Result<Void> createPlan(@RequestBody PlanRequest body, HttpServletRequest request) {
        LocalDate month = body.getEffectiveMonth() == null ? null : body.getEffectiveMonth().withDayOfMonth(1);
        if (month == null || !month.isAfter(LocalDate.now(MerchantBillingService.ZONE).withDayOfMonth(1)))
            return Result.error("计费方案只能从未来月份生效");
        if (body.getFreeOrderCount() == null || body.getFreeOrderCount() < 0 || body.getUnitPrice() == null || body.getUnitPrice().signum() < 0)
            return Result.error("免费订单数和单价不能为负数");
        if (body.getGraceDays() == null || body.getGraceDays() < 1 || body.getGraceDays() > 31)
            return Result.error("宽限天数必须为1至31天");
        jdbc.update("""
                INSERT INTO merchant_billing_plans
                (effective_month,billing_enabled,free_order_count,unit_price,grace_days,alipay_wap_enabled)
                VALUES (?,?,?,?,?,?)
                """, Date.valueOf(month), flag(body.getBillingEnabled()), body.getFreeOrderCount(), body.getUnitPrice(),
                body.getGraceDays(), flag(body.getAlipayWapEnabled()));
        auditService.record(adminId(), "CREATE_BILLING_PLAN", "billing_plan", month.toString(), "新增未来计费方案", request);
        return Result.success(null);
    }

    @GetMapping("/bills")
    public Result<Map<String, Object>> bills(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String status,
            @RequestParam(required = false) Long shopId, @RequestParam(required = false) String billingMonth,
            @RequestParam(required = false) String keyword) {
        int size = Math.min(Math.max(pageSize, 1), 100), current = Math.max(page, 1);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status != null && !status.isBlank()) { where.append(" AND b.status=?"); args.add(status); }
        if (shopId != null) { where.append(" AND b.shop_id=?"); args.add(shopId); }
        if (billingMonth != null && !billingMonth.isBlank()) { where.append(" AND b.billing_month=?"); args.add(Date.valueOf(LocalDate.parse(billingMonth).withDayOfMonth(1))); }
        if (keyword != null && !keyword.isBlank()) { where.append(" AND (b.bill_no LIKE ? OR s.name LIKE ?)"); String value = "%" + keyword.trim() + "%"; args.add(value); args.add(value); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM merchant_bills b JOIN shops s ON s.id=b.shop_id" + where, Long.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args); queryArgs.add(size); queryArgs.add((current - 1) * size);
        List<Map<String, Object>> records = jdbc.queryForList("""
                SELECT b.*,s.name shop_name FROM merchant_bills b JOIN shops s ON s.id=b.shop_id
                """ + where + " ORDER BY b.billing_month DESC,b.shop_id LIMIT ? OFFSET ?", queryArgs.toArray());
        return Result.success(Map.of("records", records, "total", total == null ? 0 : total, "page", current, "pageSize", size));
    }

    @GetMapping("/bills/{id}")
    public Result<Map<String, Object>> bill(@PathVariable long id) {
        List<Map<String, Object>> bills = jdbc.queryForList("SELECT b.*,s.name shop_name FROM merchant_bills b JOIN shops s ON s.id=b.shop_id WHERE b.id=?", id);
        if (bills.isEmpty()) return Result.error("账单不存在");
        Map<String, Object> result = new LinkedHashMap<>(bills.get(0));
        result.put("payments", jdbc.queryForList("SELECT * FROM merchant_payment_transactions WHERE bill_id=? ORDER BY create_time DESC", id));
        result.put("adjustments", jdbc.queryForList("SELECT a.*,u.display_name admin_name FROM merchant_bill_adjustments a JOIN admin_users u ON u.id=a.admin_id WHERE a.bill_id=? ORDER BY a.create_time DESC", id));
        return Result.success(result);
    }

    @PostMapping("/generate")
    public Result<Map<String, Integer>> generate(@RequestBody GenerateRequest body, HttpServletRequest request) {
        if (body.getBillingMonth() == null) return Result.error("请选择账期");
        int created = billingService.generateMonth(body.getBillingMonth());
        auditService.record(adminId(), "GENERATE_MERCHANT_BILLS", "billing_month", body.getBillingMonth().toString(), "补生成账单：" + created + "张", request);
        return Result.success(Map.of("created", created));
    }

    @PostMapping("/bills/{id}/adjust")
    @Transactional
    public Result<Void> adjust(@PathVariable long id, @RequestBody AdjustmentRequest body, HttpServletRequest request) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM merchant_bills WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) return Result.error("账单不存在");
        Map<String, Object> bill = rows.get(0);
        if (!"unpaid".equals(bill.get("status"))) return Result.error("只有未支付账单可以调整");
        if (body.getReason() == null || body.getReason().isBlank()) return Result.error("请填写调整原因");
        String type = body.getType() == null ? "" : body.getType().toLowerCase(Locale.ROOT);
        BigDecimal original = MerchantBillingService.asDecimal(bill.get("original_amount"));
        BigDecimal currentAdjustment = MerchantBillingService.asDecimal(bill.get("adjustment_amount"));
        BigDecimal currentPayable = MerchantBillingService.asDecimal(bill.get("payable_amount"));
        BigDecimal adjustment;
        String status = "unpaid";
        if ("reduce".equals(type)) {
            BigDecimal reduce = body.getAmount() == null ? BigDecimal.ZERO : body.getAmount();
            if (reduce.signum() <= 0 || reduce.compareTo(currentPayable) > 0) return Result.error("减免金额必须大于0且不能超过当前应付金额");
            adjustment = currentAdjustment.subtract(reduce);
            if (original.add(adjustment).signum() == 0) status = "waived";
        } else if ("waive".equals(type)) {
            adjustment = original.negate(); status = "waived";
        } else if ("void".equals(type)) {
            adjustment = original.negate(); status = "void";
        } else return Result.error("不支持的调整类型");
        BigDecimal payable = original.add(adjustment);
        jdbc.update("UPDATE merchant_bills SET adjustment_amount=?,payable_amount=?,status=? WHERE id=?", adjustment, payable, status, id);
        jdbc.update("INSERT INTO merchant_bill_adjustments (bill_id,admin_id,adjustment_type,amount,reason) VALUES (?,?,?,?,?)",
                id, adminId(), type, adjustment.abs(), body.getReason().trim());
        auditService.record(adminId(), "ADJUST_MERCHANT_BILL", "merchant_bill", String.valueOf(id), body.getReason().trim(), request);
        return Result.success(null);
    }

    @PostMapping("/payments/{outTradeNo}/sync")
    public Result<Map<String, Object>> syncPayment(@PathVariable String outTradeNo, HttpServletRequest request) {
        try {
            Map<String, Object> result = paymentService.syncPayment(outTradeNo);
            auditService.record(adminId(), "SYNC_ALIPAY_PAYMENT", "payment", outTradeNo, "主动查询支付宝交易状态", request);
            return Result.success(result);
        } catch (RuntimeException ex) {
            return Result.error(ex.getMessage());
        }
    }

    private Long adminId() { return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
    private int flag(Boolean value) { return Boolean.TRUE.equals(value) ? 1 : 0; }
    @Data public static class PlanRequest { private LocalDate effectiveMonth; private Boolean billingEnabled; private Integer freeOrderCount; private BigDecimal unitPrice; private Integer graceDays; private Boolean alipayWapEnabled; }
    @Data public static class GenerateRequest { private LocalDate billingMonth; }
    @Data public static class AdjustmentRequest { private String type; private BigDecimal amount; private String reason; }
}
