package com.fujian.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("lucky_draw_winners")
public class LuckyDrawWinner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate drawDate;
    private Long userId;
    private String prizeType;
    private BigDecimal prizeValue;
    private Integer status;
    private LocalDateTime drawTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDrawDate() { return drawDate; }
    public void setDrawDate(LocalDate drawDate) { this.drawDate = drawDate; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPrizeType() { return prizeType; }
    public void setPrizeType(String prizeType) { this.prizeType = prizeType; }
    public BigDecimal getPrizeValue() { return prizeValue; }
    public void setPrizeValue(BigDecimal prizeValue) { this.prizeValue = prizeValue; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getDrawTime() { return drawTime; }
    public void setDrawTime(LocalDateTime drawTime) { this.drawTime = drawTime; }
}
