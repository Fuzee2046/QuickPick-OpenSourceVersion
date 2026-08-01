package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.JwtTokenUtil;
import com.fujian.common.PasswordUtil;
import com.fujian.common.Result;
import com.fujian.mapper.AdminUserMapper;
import com.fujian.pojo.AdminUser;
import lombok.Data;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminUserMapper adminUserMapper;
    private final JwtTokenUtil jwtTokenUtil;

    public AdminAuthController(AdminUserMapper adminUserMapper, JwtTokenUtil jwtTokenUtil) {
        this.adminUserMapper = adminUserMapper;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class PasswordRequest {
        private String oldPassword;
        private String newPassword;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return Result.error("请输入账号和密码");
        }
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.getUsername().trim()));
        if (admin == null || admin.getStatus() == null || admin.getStatus() != 1
                || !PasswordUtil.matches(request.getPassword(), admin.getPassword())) {
            return Result.error("账号或密码错误");
        }
        admin.setLastLoginTime(LocalDateTime.now());
        adminUserMapper.updateById(admin);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", jwtTokenUtil.generateAdminToken(admin.getId(), admin.getUsername(), admin.getDisplayName()));
        data.put("role", "admin");
        data.put("adminId", admin.getId());
        data.put("displayName", admin.getDisplayName());
        data.put("requirePasswordChange", Integer.valueOf(1).equals(admin.getMustChangePassword()));
        return Result.success(data);
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        AdminUser admin = adminUserMapper.selectById(currentAdminId());
        if (admin == null) return Result.error("管理员不存在");
        return Result.success(Map.of("id", admin.getId(), "username", admin.getUsername(),
                "displayName", admin.getDisplayName(), "lastLoginTime", admin.getLastLoginTime()));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody PasswordRequest request) {
        AdminUser admin = adminUserMapper.selectById(currentAdminId());
        if (admin == null || !PasswordUtil.matches(request.getOldPassword(), admin.getPassword())) {
            return Result.error("原密码错误");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 10) {
            return Result.error("新密码至少需要10位");
        }
        admin.setPassword(PasswordUtil.encrypt(request.getNewPassword()));
        admin.setMustChangePassword(0);
        adminUserMapper.updateById(admin);
        return Result.success(null);
    }

    private Long currentAdminId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
