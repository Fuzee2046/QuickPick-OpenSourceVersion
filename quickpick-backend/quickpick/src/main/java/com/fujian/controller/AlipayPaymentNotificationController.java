package com.fujian.controller;

import com.fujian.service.AlipayMerchantPaymentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/alipay")
public class AlipayPaymentNotificationController {
    private final AlipayMerchantPaymentService paymentService;

    public AlipayPaymentNotificationController(AlipayMerchantPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> notify(@RequestParam Map<String, String> params) {
        try {
            paymentService.handleNotification(params);
            return ResponseEntity.ok("success");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body("failure");
        }
    }
}
