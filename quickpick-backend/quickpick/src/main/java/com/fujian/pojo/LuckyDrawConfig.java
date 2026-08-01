package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@TableName("lucky_draw_config")
public class LuckyDrawConfig {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String activityName;
    private Integer status;
    private LocalTime reserveStartTime;
    private LocalTime reserveEndTime;
    private LocalTime drawTime;
    private String prizeType;
    private BigDecimal prizeValue;
    private Integer maxWinnersPerDay;
    private String redeemWechat;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalTime getReserveStartTime() { return reserveStartTime; }
    public void setReserveStartTime(LocalTime reserveStartTime) { this.reserveStartTime = reserveStartTime; }
    public LocalTime getReserveEndTime() { return reserveEndTime; }
    public void setReserveEndTime(LocalTime reserveEndTime) { this.reserveEndTime = reserveEndTime; }
    public LocalTime getDrawTime() { return drawTime; }
    public void setDrawTime(LocalTime drawTime) { this.drawTime = drawTime; }
    public String getPrizeType() { return prizeType; }
    public void setPrizeType(String prizeType) { this.prizeType = prizeType; }
    public BigDecimal getPrizeValue() { return prizeValue; }
    public void setPrizeValue(BigDecimal prizeValue) { this.prizeValue = prizeValue; }
    public Integer getMaxWinnersPerDay() { return maxWinnersPerDay; }
    public void setMaxWinnersPerDay(Integer maxWinnersPerDay) { this.maxWinnersPerDay = maxWinnersPerDay; }
    public String getRedeemWechat() { return redeemWechat; }
    public void setRedeemWechat(String redeemWechat) { this.redeemWechat = redeemWechat; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
