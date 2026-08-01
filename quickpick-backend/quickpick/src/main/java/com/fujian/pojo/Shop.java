package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@TableName("shops")
public class Shop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String address;
    private String coverImage;
    private String logoImage;
    private String contactPhone;
    @JsonIgnore
    private String password;
    private LocalDateTime pwdUpdatedAt;
    private Integer status; // 1: 营业中, 0: 休息中
    private Integer visible; // 1: 客户端展示, 0: 客户端隐藏
    @TableField("display_sort")
    private Integer displaySort;
    private Integer tasteSensitiveEnabled;
    private Integer peakLimitEnabled;
    private String shopMode;
    @TableField("weight_price_per_500g")
    private java.math.BigDecimal weightPricePer500g;
    private Integer minimumOrderWeightG;
    @TableField(exist = false)
    private Boolean currentlyOpen;
    @TableField(exist = false)
    private String displayStatus;
    @TableField(exist = false)
    private String displayStatusText;
    @TableField(exist = false)
    private Boolean billingServiceAvailable;
    private Integer canteenId;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime1;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime1;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime2;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime2;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getLogoImage() { return logoImage; }
    public void setLogoImage(String logoImage) { this.logoImage = logoImage; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDateTime getPwdUpdatedAt() { return pwdUpdatedAt; }
    public void setPwdUpdatedAt(LocalDateTime pwdUpdatedAt) { this.pwdUpdatedAt = pwdUpdatedAt; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getVisible() { return visible; }
    public void setVisible(Integer visible) { this.visible = visible; }
    public Integer getDisplaySort() { return displaySort; }
    public void setDisplaySort(Integer displaySort) { this.displaySort = displaySort; }
    public Integer getTasteSensitiveEnabled() { return tasteSensitiveEnabled; }
    public void setTasteSensitiveEnabled(Integer tasteSensitiveEnabled) { this.tasteSensitiveEnabled = tasteSensitiveEnabled; }
    public Integer getPeakLimitEnabled() { return peakLimitEnabled; }
    public void setPeakLimitEnabled(Integer peakLimitEnabled) { this.peakLimitEnabled = peakLimitEnabled; }
    public String getShopMode() { return shopMode; }
    public void setShopMode(String shopMode) { this.shopMode = shopMode; }
    public java.math.BigDecimal getWeightPricePer500g() { return weightPricePer500g; }
    public void setWeightPricePer500g(java.math.BigDecimal weightPricePer500g) { this.weightPricePer500g = weightPricePer500g; }
    public Integer getMinimumOrderWeightG() { return minimumOrderWeightG; }
    public void setMinimumOrderWeightG(Integer minimumOrderWeightG) { this.minimumOrderWeightG = minimumOrderWeightG; }
    public Boolean getCurrentlyOpen() { return currentlyOpen; }
    public void setCurrentlyOpen(Boolean currentlyOpen) { this.currentlyOpen = currentlyOpen; }
    public String getDisplayStatus() { return displayStatus; }
    public void setDisplayStatus(String displayStatus) { this.displayStatus = displayStatus; }
    public String getDisplayStatusText() { return displayStatusText; }
    public void setDisplayStatusText(String displayStatusText) { this.displayStatusText = displayStatusText; }
    public Boolean getBillingServiceAvailable() { return billingServiceAvailable; }
    public void setBillingServiceAvailable(Boolean billingServiceAvailable) { this.billingServiceAvailable = billingServiceAvailable; }
    public Integer getCanteenId() { return canteenId; }
    public void setCanteenId(Integer canteenId) { this.canteenId = canteenId; }
    public LocalTime getOpenTime1() { return openTime1; }
    public void setOpenTime1(LocalTime openTime1) { this.openTime1 = openTime1; }
    public LocalTime getCloseTime1() { return closeTime1; }
    public void setCloseTime1(LocalTime closeTime1) { this.closeTime1 = closeTime1; }
    public LocalTime getOpenTime2() { return openTime2; }
    public void setOpenTime2(LocalTime openTime2) { this.openTime2 = openTime2; }
    public LocalTime getCloseTime2() { return closeTime2; }
    public void setCloseTime2(LocalTime closeTime2) { this.closeTime2 = closeTime2; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
