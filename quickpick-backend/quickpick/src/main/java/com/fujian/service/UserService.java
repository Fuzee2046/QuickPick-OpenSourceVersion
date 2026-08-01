package com.fujian.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fujian.common.Result;
import com.fujian.pojo.User;

import java.util.Map;

public interface UserService extends IService<User> {
    Result<Map<String, Object>> login(String code, String phoneCode, String name);
}
