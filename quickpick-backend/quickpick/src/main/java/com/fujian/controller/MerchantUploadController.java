package com.fujian.controller;

import com.fujian.common.Result;
import com.fujian.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/upload")
public class MerchantUploadController {

    @Autowired
    private CosService cosService;

    @PostMapping("/dish-image")
    public Result<Map<String, String>> uploadDishImage(@RequestParam("file") MultipartFile file) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }
        try {
            return Result.success(buildUploadResult(file, "dishes", shopId, "dish"));
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/broth-image")
    public Result<Map<String, String>> uploadBrothImage(@RequestParam("file") MultipartFile file) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }
        try {
            return Result.success(buildUploadResult(file, "broths", shopId, "broth"));
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/price-evidence-image")
    public Result<Map<String, String>> uploadPriceEvidenceImage(@RequestParam("file") MultipartFile file) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }
        try {
            return Result.success(buildUploadResult(file, "price-evidence", shopId, "price_evidence"));
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    private Long getCurrentMerchantShopId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getAuthorities() == null) {
                return null;
            }
            boolean isMerchant = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_MERCHANT"::equals);
            if (!isMerchant) {
                return null;
            }
            return (Long) authentication.getPrincipal();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> buildUploadResult(MultipartFile file, String folder, Long shopId, String filePrefix) throws Exception {
        validateImage(file);
        String url = cosService.upload(file, folder, shopId, filePrefix);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return data;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只能上传图片文件");
        }
    }
}
