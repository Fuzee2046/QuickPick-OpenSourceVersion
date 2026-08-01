package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_users")
public class AdminUser {
    private Long id;
    private String username;
    private String password;
    private String displayName;
    private Integer status;
    private Integer mustChangePassword;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
