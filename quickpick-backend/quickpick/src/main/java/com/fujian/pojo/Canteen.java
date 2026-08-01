package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("canteens")
public class Canteen {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer sortOrder;
    private Integer status; // 1: 启用, 0: 停用
    
    @TableField(exist = false)
    private Long shopCount;

    // Manual getters for compatibility
    public Integer getStatus() {
        return status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getShopCount() {
        return shopCount;
    }

    public void setShopCount(Long shopCount) {
        this.shopCount = shopCount;
    }
}
