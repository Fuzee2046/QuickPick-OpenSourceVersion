package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fujian.common.JwtTokenUtil;
import com.fujian.common.PasswordUtil;
import com.fujian.common.Result;
import com.fujian.mapper.ShopMapper;
import com.fujian.pojo.Shop;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/auth")
public class MerchantAuthController {

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Data
    public static class LoginRequest {
        private String identifier; // 店铺ID或手机号
        private String password;
        
        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
        
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (request.getIdentifier() == null || request.getPassword() == null) {
            return Result.error("参数不完整");
        }

        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getId, request.getIdentifier())
                .or()
                .eq(Shop::getContactPhone, request.getIdentifier()));

        if (shop == null) {
            return Result.error("商户不存在");
        }

        if (!PasswordUtil.matches(request.getPassword(), shop.getPassword())) {
            return Result.error("密码错误");
        }

        // Generate Token
        String token = jwtTokenUtil.generateToken(shop.getId(), shop.getName());

        // Check if password change is required
        // Condition: pwdUpdatedAt is null implies it's initial password or never changed
        boolean requirePasswordChange = shop.getPwdUpdatedAt() == null;

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("shopId", shop.getId());
        data.put("shopName", shop.getName());
        data.put("requirePasswordChange", requirePasswordChange);

        return Result.success(data);
    }

    @PostMapping("/password")
    public Result<String> changePassword(@RequestBody ChangePasswordRequest request) {
        Long shopId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return Result.error("商户不存在");
        }

        if (!PasswordUtil.matches(request.getOldPassword(), shop.getPassword())) {
            return Result.error("原密码错误");
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
             return Result.error("新密码长度不能少于6位");
        }

        String encodedNewPassword = PasswordUtil.encrypt(request.getNewPassword());
        
        shop.setPassword(encodedNewPassword);
        shop.setPwdUpdatedAt(LocalDateTime.now());
        
        shopMapper.updateById(shop);

        return Result.success("密码修改成功，请重新登录");
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile() {
        Long shopId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return Result.error("商户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("shopId", shop.getId());
        data.put("shopName", shop.getName());
        data.put("contactPhone", shop.getContactPhone());
        data.put("address", shop.getAddress());
        data.put("logoImage", shop.getLogoImage());
        data.put("status", shop.getStatus());
        data.put("openTime1", shop.getOpenTime1());
        data.put("closeTime1", shop.getCloseTime1());
        data.put("openTime2", shop.getOpenTime2());
        data.put("closeTime2", shop.getCloseTime2());
        data.put("peakLimitEnabled", shop.getPeakLimitEnabled());
        // Don't return password!
        return Result.success(data);
    }
}
