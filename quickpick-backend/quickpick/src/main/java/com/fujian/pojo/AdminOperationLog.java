package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_operation_logs")
public class AdminOperationLog {
    private Long id;
    private Long adminId;
    private String operationType;
    private String targetType;
    private String targetId;
    private String reason;
    private String ipAddress;
    private LocalDateTime createTime;
}
