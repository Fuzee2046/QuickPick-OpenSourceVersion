package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("order_weight_items")
public class OrderWeightItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private Long ingredientId;
    private Integer quantity;
    private Integer referenceWeightG;
    private Integer estimatedWeightG;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getReferenceWeightG() { return referenceWeightG; }
    public void setReferenceWeightG(Integer referenceWeightG) { this.referenceWeightG = referenceWeightG; }
    public Integer getEstimatedWeightG() { return estimatedWeightG; }
    public void setEstimatedWeightG(Integer estimatedWeightG) { this.estimatedWeightG = estimatedWeightG; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
