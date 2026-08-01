package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("order_item_options")
public class OrderItemOption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderItemId;
    private Long optionGroupId;
    private Long optionValueId;
    private String groupName;
    private String valueName;
    private BigDecimal extraPrice;
    private Integer sort;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Long getOptionGroupId() { return optionGroupId; }
    public void setOptionGroupId(Long optionGroupId) { this.optionGroupId = optionGroupId; }
    public Long getOptionValueId() { return optionValueId; }
    public void setOptionValueId(Long optionValueId) { this.optionValueId = optionValueId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getValueName() { return valueName; }
    public void setValueName(String valueName) { this.valueName = valueName; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
