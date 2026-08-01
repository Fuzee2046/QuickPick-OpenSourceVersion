package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;
    private String phone;
    private String name;
    private Integer noShowCount;
    private String penaltyStatus;
    private LocalDateTime penaltyEndTime;
    private String penaltyReason;
    private String lastNoShowOrderId;
    private String frozenContactNote;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // Manual getters for compatibility
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getOpenid() {
        return openid;
    }
    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getNoShowCount() {
        return noShowCount;
    }
    public void setNoShowCount(Integer noShowCount) {
        this.noShowCount = noShowCount;
    }

    public String getPenaltyStatus() {
        return penaltyStatus;
    }
    public void setPenaltyStatus(String penaltyStatus) {
        this.penaltyStatus = penaltyStatus;
    }

    public LocalDateTime getPenaltyEndTime() {
        return penaltyEndTime;
    }
    public void setPenaltyEndTime(LocalDateTime penaltyEndTime) {
        this.penaltyEndTime = penaltyEndTime;
    }

    public String getPenaltyReason() {
        return penaltyReason;
    }
    public void setPenaltyReason(String penaltyReason) {
        this.penaltyReason = penaltyReason;
    }

    public String getLastNoShowOrderId() {
        return lastNoShowOrderId;
    }
    public void setLastNoShowOrderId(String lastNoShowOrderId) {
        this.lastNoShowOrderId = lastNoShowOrderId;
    }

    public String getFrozenContactNote() {
        return frozenContactNote;
    }
    public void setFrozenContactNote(String frozenContactNote) {
        this.frozenContactNote = frozenContactNote;
    }
}
