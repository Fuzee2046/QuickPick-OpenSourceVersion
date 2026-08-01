package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@TableName("orders")
public class Order {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long shopId;
    private LocalDate bizDate;
    private Long userId; // Replaced studentOpenid
    @TableField("client_request_id")
    private String clientRequestId;
    private BigDecimal totalAmount;
    private String pickupCode;
    private String status; // pending, making, pending, completed, cancelled
    private String cancelReason;
    private LocalDateTime cancelTime;
    private Integer needPack; // 0: dine-in, 1: take-away
    private String remark;
    private String orderMode;
    private String pricingStatus;
    private Integer estimatedWeightG;
    private BigDecimal estimatedAmount;
    private Integer finalWeightG;
    private BigDecimal finalAmount;
    private LocalDateTime priceConfirmTime;
    private String priceEvidenceImage;
    private Long brothOptionId;
    private String brothName;
    private BigDecimal brothExtraPrice;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime pickupTime;
    
    private LocalDateTime readyTime;
    private LocalDateTime completedTime;
    private LocalDateTime closedTime;
    private Integer secondPickupReminderSent;
    private LocalDateTime secondPickupReminderTime;
    private Integer pickupOvertimeMinutes;
    private String pickupOvertimeNote;

    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public LocalDate getBizDate() { return bizDate; }
    public void setBizDate(LocalDate bizDate) { this.bizDate = bizDate; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getNeedPack() { return needPack; }
    public void setNeedPack(Integer needPack) { this.needPack = needPack; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOrderMode() { return orderMode; }
    public void setOrderMode(String orderMode) { this.orderMode = orderMode; }
    public String getPricingStatus() { return pricingStatus; }
    public void setPricingStatus(String pricingStatus) { this.pricingStatus = pricingStatus; }
    public Integer getEstimatedWeightG() { return estimatedWeightG; }
    public void setEstimatedWeightG(Integer estimatedWeightG) { this.estimatedWeightG = estimatedWeightG; }
    public BigDecimal getEstimatedAmount() { return estimatedAmount; }
    public void setEstimatedAmount(BigDecimal estimatedAmount) { this.estimatedAmount = estimatedAmount; }
    public Integer getFinalWeightG() { return finalWeightG; }
    public void setFinalWeightG(Integer finalWeightG) { this.finalWeightG = finalWeightG; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public LocalDateTime getPriceConfirmTime() { return priceConfirmTime; }
    public void setPriceConfirmTime(LocalDateTime priceConfirmTime) { this.priceConfirmTime = priceConfirmTime; }
    public String getPriceEvidenceImage() { return priceEvidenceImage; }
    public void setPriceEvidenceImage(String priceEvidenceImage) { this.priceEvidenceImage = priceEvidenceImage; }
    public Long getBrothOptionId() { return brothOptionId; }
    public void setBrothOptionId(Long brothOptionId) { this.brothOptionId = brothOptionId; }
    public String getBrothName() { return brothName; }
    public void setBrothName(String brothName) { this.brothName = brothName; }
    public BigDecimal getBrothExtraPrice() { return brothExtraPrice; }
    public void setBrothExtraPrice(BigDecimal brothExtraPrice) { this.brothExtraPrice = brothExtraPrice; }
    public LocalTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalTime pickupTime) { this.pickupTime = pickupTime; }
    public LocalDateTime getReadyTime() { return readyTime; }
    public void setReadyTime(LocalDateTime readyTime) { this.readyTime = readyTime; }
    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
    public LocalDateTime getClosedTime() { return closedTime; }
    public void setClosedTime(LocalDateTime closedTime) { this.closedTime = closedTime; }
    public Integer getSecondPickupReminderSent() { return secondPickupReminderSent; }
    public void setSecondPickupReminderSent(Integer secondPickupReminderSent) { this.secondPickupReminderSent = secondPickupReminderSent; }
    public LocalDateTime getSecondPickupReminderTime() { return secondPickupReminderTime; }
    public void setSecondPickupReminderTime(LocalDateTime secondPickupReminderTime) { this.secondPickupReminderTime = secondPickupReminderTime; }
    public Integer getPickupOvertimeMinutes() { return pickupOvertimeMinutes; }
    public void setPickupOvertimeMinutes(Integer pickupOvertimeMinutes) { this.pickupOvertimeMinutes = pickupOvertimeMinutes; }
    public String getPickupOvertimeNote() { return pickupOvertimeNote; }
    public void setPickupOvertimeNote(String pickupOvertimeNote) { this.pickupOvertimeNote = pickupOvertimeNote; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCancelTime() { return cancelTime; }
    public void setCancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; }
}
