package com.fujian.controller;

import com.fujian.common.Result;
import com.fujian.service.UserService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Data
    public static class StudentLoginRequest {
        private String code;      // 微信登录 code
        private String phoneCode; // 手机号获取 code
        private String name;      // 学生姓名

        // Manual getters for compatibility
        public String getCode() {
            return code;
        }

        public String getPhoneCode() {
            return phoneCode;
        }

        public String getName() {
            return name;
        }
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody StudentLoginRequest request) {
        return userService.login(request.getCode(), request.getPhoneCode(), request.getName());
    }
    
    // 保留旧接口兼容性，但建议前端迁移到 /login
    public static class LoginRequest {
        private String code;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody LoginRequest request) {
        // 简单适配，没有手机号和姓名
        return userService.login(request.getCode(), null, null);
    }
}
