package com.fujian.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReservationRuleConfig {
    private Long id;
    private Integer offPeakMinMinutes;
    private Integer peakMinMinutes;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime lunchPeakStart;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime lunchPeakEnd;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime dinnerPeakStart;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime dinnerPeakEnd;
    private Integer workdayOnly;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getOffPeakMinMinutes() { return offPeakMinMinutes; }
    public void setOffPeakMinMinutes(Integer offPeakMinMinutes) { this.offPeakMinMinutes = offPeakMinMinutes; }
    public Integer getPeakMinMinutes() { return peakMinMinutes; }
    public void setPeakMinMinutes(Integer peakMinMinutes) { this.peakMinMinutes = peakMinMinutes; }
    public LocalTime getLunchPeakStart() { return lunchPeakStart; }
    public void setLunchPeakStart(LocalTime lunchPeakStart) { this.lunchPeakStart = lunchPeakStart; }
    public LocalTime getLunchPeakEnd() { return lunchPeakEnd; }
    public void setLunchPeakEnd(LocalTime lunchPeakEnd) { this.lunchPeakEnd = lunchPeakEnd; }
    public LocalTime getDinnerPeakStart() { return dinnerPeakStart; }
    public void setDinnerPeakStart(LocalTime dinnerPeakStart) { this.dinnerPeakStart = dinnerPeakStart; }
    public LocalTime getDinnerPeakEnd() { return dinnerPeakEnd; }
    public void setDinnerPeakEnd(LocalTime dinnerPeakEnd) { this.dinnerPeakEnd = dinnerPeakEnd; }
    public Integer getWorkdayOnly() { return workdayOnly; }
    public void setWorkdayOnly(Integer workdayOnly) { this.workdayOnly = workdayOnly; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
