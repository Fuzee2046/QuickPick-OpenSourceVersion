package com.fujian.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.net.URI;
import java.util.*;

@Service
public class AlipayMerchantPaymentService {
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";
    private final JdbcTemplate jdbc;
    private final MerchantBillingService billingService;
    private final boolean enabled;
    private final String appId;
    private final String sellerId;
    private final String alipayPublicKey;
    private final String notifyUrl;
    private final String returnUrl;
    private final AlipayClient client;

    public AlipayMerchantPaymentService(JdbcTemplate jdbc, MerchantBillingService billingService,
            @Value("${alipay.pay.enabled:false}") boolean enabled,
            @Value("${alipay.pay.app-id:}") String appId,
            @Value("${alipay.pay.seller-id:}") String sellerId,
            @Value("${alipay.pay.merchant-private-key:}") String merchantPrivateKey,
            @Value("${alipay.pay.public-key:}") String alipayPublicKey,
            @Value("${alipay.pay.notify-url:}") String notifyUrl,
            @Value("${alipay.pay.return-url:}") String returnUrl,
            @Value("${alipay.pay.gateway:https://openapi.alipay.com/gateway.do}") String gateway) {
        this.jdbc = jdbc;
        this.billingService = billingService;
        this.enabled = enabled;
        this.appId = appId;
        this.sellerId = sellerId;
        this.alipayPublicKey = alipayPublicKey;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
        if (enabled) {
            if (List.of(appId, sellerId, merchantPrivateKey, alipayPublicKey, notifyUrl, returnUrl, gateway).stream().anyMatch(String::isBlank))
                throw new IllegalStateException("alipay.pay.enabled=true 时必须配置完整支付宝环境变量");
            validateHttpUrl("ALIPAY_NOTIFY_URL", notifyUrl);
            validateHttpUrl("ALIPAY_RETURN_URL", returnUrl);
            this.client = new DefaultAlipayClient(gateway, appId, merchantPrivateKey, "json", CHARSET, alipayPublicKey, SIGN_TYPE);
        } else {
            this.client = null;
        }
    }

    @Transactional
    public Map<String, Object> createPayment(long shopId, long billId) {
        ensureEnabled();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT b.*,s.name shop_name FROM merchant_bills b JOIN shops s ON s.id=b.shop_id
                WHERE b.id=? AND b.shop_id=? FOR UPDATE
                """, billId, shopId);
        if (rows.isEmpty()) throw new IllegalArgumentException("账单不存在");
        Map<String, Object> bill = rows.get(0);
        if (!"unpaid".equals(bill.get("status"))) throw new IllegalStateException("账单当前无需支付");
        BigDecimal amount = MerchantBillingService.asDecimal(bill.get("payable_amount"));
        if (amount.signum() <= 0) throw new IllegalStateException("账单金额为零，无需支付");
        Map<String, Object> plan = billingService.currentPlan(((java.sql.Date) bill.get("billing_month")).toLocalDate());
        if (plan == null || !MerchantBillingService.asBoolean(plan.get("alipay_wap_enabled")))
            throw new IllegalStateException("支付宝手机网站支付未开启");

        String outTradeNo = "QP" + System.currentTimeMillis() + String.format("%05d", shopId % 100000);
        LocalDateTime expireTime = LocalDateTime.now(MerchantBillingService.ZONE).plusMinutes(15);
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(outTradeNo);
        model.setTotalAmount(amount.toPlainString());
        model.setSubject("食刻快取运营成本费-" + bill.get("bill_no"));
        model.setBody("商户平台运营成本费，不包含学生餐费");
        model.setProductCode("QUICK_WAP_WAY");
        model.setTimeoutExpress("15m");

        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setBizModel(model);
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl + (returnUrl.contains("?") ? "&" : "?") + "outTradeNo=" + outTradeNo);
        try {
            String formHtml = client.pageExecute(request).getBody();
            jdbc.update("""
                    INSERT INTO merchant_payment_transactions
                    (out_trade_no,bill_id,shop_id,channel,amount,status,prepay_url,expire_time)
                    VALUES (?,?,?,?,?,'paying',NULL,?)
                    """, outTradeNo, billId, shopId, "ALIPAY_WAP", amount, Timestamp.valueOf(expireTime));
            return Map.of("outTradeNo", outTradeNo, "channel", "ALIPAY_WAP", "formHtml", formHtml,
                    "expireTime", expireTime, "amount", amount);
        } catch (AlipayApiException ex) {
            throw new IllegalStateException("支付宝下单失败，请稍后重试");
        }
    }

    @Transactional
    public void handleNotification(Map<String, String> params) {
        ensureEnabled();
        boolean signatureValid;
        try {
            signatureValid = AlipaySignature.rsaCheckV1(params, alipayPublicKey, CHARSET, SIGN_TYPE);
        } catch (AlipayApiException ex) {
            throw new SecurityException("支付宝通知验签异常", ex);
        }
        if (!signatureValid) throw new SecurityException("支付宝通知验签失败");
        if (!appId.equals(params.get("app_id"))) throw new SecurityException("支付宝应用ID不匹配");
        if (!sellerId.equals(params.get("seller_id"))) throw new SecurityException("支付宝收款账号不匹配");
        String tradeStatus = params.get("trade_status");
        if (!Set.of("TRADE_SUCCESS", "TRADE_FINISHED").contains(tradeStatus)) return;
        String outTradeNo = params.get("out_trade_no");
        List<Map<String, Object>> payments = jdbc.queryForList("""
                SELECT p.*,b.status bill_status FROM merchant_payment_transactions p
                JOIN merchant_bills b ON b.id=p.bill_id WHERE p.out_trade_no=? FOR UPDATE
                """, outTradeNo);
        if (payments.isEmpty()) throw new SecurityException("支付流水不存在");
        Map<String, Object> payment = payments.get(0);
        BigDecimal expected = MerchantBillingService.asDecimal(payment.get("amount"));
        BigDecimal actual = new BigDecimal(params.getOrDefault("total_amount", "-1"));
        if (actual.compareTo(expected) != 0) throw new SecurityException("支付宝支付金额不匹配");
        if ("success".equals(payment.get("status"))) return;
        billingService.markPaid(((Number) payment.get("bill_id")).longValue(), outTradeNo,
                params.get("trade_no"), LocalDateTime.now(MerchantBillingService.ZONE));
    }

    public Map<String, Object> paymentStatus(long shopId, String outTradeNo) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.out_trade_no,p.channel,p.amount,p.status,p.expire_time,p.success_time,b.id bill_id,b.status bill_status
                FROM merchant_payment_transactions p JOIN merchant_bills b ON b.id=p.bill_id
                WHERE p.out_trade_no=? AND p.shop_id=?
                """, outTradeNo, shopId);
        if (rows.isEmpty()) throw new IllegalArgumentException("支付流水不存在");
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> syncPayment(String outTradeNo) {
        ensureEnabled();
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM merchant_payment_transactions WHERE out_trade_no=? FOR UPDATE", outTradeNo);
        if (rows.isEmpty()) throw new IllegalArgumentException("支付流水不存在");
        return syncPaymentRow(outTradeNo, rows.get(0));
    }

    @Transactional
    public Map<String, Object> syncPaymentForShop(long shopId, String outTradeNo) {
        ensureEnabled();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT * FROM merchant_payment_transactions
                WHERE out_trade_no=? AND shop_id=? FOR UPDATE
                """, outTradeNo, shopId);
        if (rows.isEmpty()) throw new IllegalArgumentException("支付流水不存在");
        return syncPaymentRow(outTradeNo, rows.get(0));
    }

    private Map<String, Object> syncPaymentRow(String outTradeNo, Map<String, Object> payment) {
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizModel(model);
        try {
            AlipayTradeQueryResponse response = client.execute(request);
            if (!response.isSuccess()) {
                return Map.of("outTradeNo", outTradeNo, "tradeState", "WAITING", "subCode",
                        response.getSubCode() == null ? "" : response.getSubCode());
            }
            if (Set.of("TRADE_SUCCESS", "TRADE_FINISHED").contains(response.getTradeStatus())) {
                BigDecimal expected = MerchantBillingService.asDecimal(payment.get("amount"));
                if (response.getTotalAmount() == null || new BigDecimal(response.getTotalAmount()).compareTo(expected) != 0)
                    throw new SecurityException("支付宝支付金额不匹配");
                billingService.markPaid(((Number) payment.get("bill_id")).longValue(), outTradeNo,
                        response.getTradeNo(), LocalDateTime.now(MerchantBillingService.ZONE));
            }
            return Map.of("outTradeNo", outTradeNo, "tradeState", response.getTradeStatus());
        } catch (AlipayApiException ex) {
            throw new IllegalStateException("支付宝交易查询失败");
        }
    }

    private void validateHttpUrl(String name, String value) {
        try {
            URI uri = URI.create(value);
            if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null)
                throw new IllegalArgumentException();
        } catch (RuntimeException ex) {
            throw new IllegalStateException(name + "必须是完整的http或https URL，不能把变量名写进变量值");
        }
    }

    private void ensureEnabled() { if (!enabled) throw new IllegalStateException("支付宝支付尚未完成生产配置"); }
}
