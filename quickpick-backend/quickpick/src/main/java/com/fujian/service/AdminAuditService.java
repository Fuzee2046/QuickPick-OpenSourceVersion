package com.fujian.service;

import com.fujian.mapper.AdminOperationLogMapper;
import com.fujian.pojo.AdminOperationLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {
    private final AdminOperationLogMapper logMapper;

    public AdminAuditService(AdminOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public void record(Long adminId, String operation, String targetType, String targetId,
                       String reason, HttpServletRequest request) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setOperationType(operation);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setReason(reason);
        String forwarded = request.getHeader("X-Forwarded-For");
        log.setIpAddress(forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim());
        logMapper.insert(log);
    }
}
