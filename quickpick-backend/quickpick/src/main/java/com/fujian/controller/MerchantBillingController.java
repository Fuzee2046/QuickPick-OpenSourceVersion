package com.fujian.controller;

import com.fujian.common.Result;
import com.fujian.service.MerchantBillingService;
import com.fujian.service.AlipayMerchantPaymentService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant/billing")
public class MerchantBillingController {
    private final MerchantBillingService billingService;
    private final AlipayMerchantPaymentService paymentService;

    public MerchantBillingController(MerchantBillingService billingService, AlipayMerchantPaymentService paymentService) {
        this.billingService = billingService;
        this.paymentService = paymentService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() { return Result.success(billingService.overview(shopId())); }

    @GetMapping("/bills")
    public Result<Map<String, Object>> bills(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(billingService.listBills(shopId(), page, pageSize));
    }

    @PostMapping("/reminders/{billId}/dismiss")
    public Result<Void> dismissReminder(@PathVariable long billId) {
        billingService.dismissReminder(shopId(), billId);
        return Result.success(null);
    }

    @PostMapping("/bills/{billId}/pay")
    public Result<Map<String, Object>> pay(@PathVariable long billId) {
        try { return Result.success(paymentService.createPayment(shopId(), billId)); }
        catch (RuntimeException ex) { return Result.error(ex.getMessage()); }
    }

    @GetMapping("/payments/{outTradeNo}")
    public Result<Map<String, Object>> payment(@PathVariable String outTradeNo) {
        try { return Result.success(paymentService.paymentStatus(shopId(), outTradeNo)); }
        catch (RuntimeException ex) { return Result.error(ex.getMessage()); }
    }

    @PostMapping("/payments/{outTradeNo}/sync")
    public Result<Map<String, Object>> syncPayment(@PathVariable String outTradeNo) {
        try { return Result.success(paymentService.syncPaymentForShop(shopId(), outTradeNo)); }
        catch (RuntimeException ex) { return Result.error(ex.getMessage()); }
    }

    private Long shopId() { return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
}
