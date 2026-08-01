package com.fujian.controller;

import com.fujian.common.Result;
import com.fujian.service.CosService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/upload")
public class AdminUploadController {
    private final CosService cosService;

    public AdminUploadController(CosService cosService) {
        this.cosService = cosService;
    }

    @PostMapping("/shop-image")
    public Result<Map<String, String>> uploadShopImage(@RequestParam("file") MultipartFile file,
                                                        @RequestParam String type) {
        if (!Set.of("logo", "cover").contains(type)) return Result.error("图片类型不合法");
        if (file == null || file.isEmpty()) return Result.error("上传文件不能为空");
        if (file.getSize() > 5 * 1024 * 1024) return Result.error("文件大小不能超过5MB");
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) return Result.error("只能上传图片文件");
        try {
            Long adminId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String url = cosService.upload(file, "shops/" + type, adminId, "shop_" + type);
            return Result.success(Map.of("url", url));
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}
