package com.park.visitor.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** 临时封闭信息（施工、管制等），可针对节点或边 */
public class Closure {

    @NotBlank(message = "封闭ID不能为空")
    private String id;

    /** 封闭对象类型：NODE / EDGE */
    @NotNull(message = "封闭对象类型不能为空")
    private TargetType targetType;

    @NotBlank(message = "封闭对象ID不能为空")
    private String targetId;

    @NotBlank(message = "封闭原因不能为空")
    private String reason;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 是否启用（管理员可临时停用而不删除） */
    private boolean enabled = true;

    public enum TargetType {
        NODE, EDGE
    }

    public Closure() {
    }

    public Closure(String id, TargetType targetType, String targetId, String reason,
                   LocalDateTime startTime, LocalDateTime endTime, boolean enabled) {
        this.id = id;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.startTime = startTime;
        this.endTime = endTime;
        this.enabled = enabled;
    }

    /** 当前时刻是否生效 */
    public boolean isActiveAt(LocalDateTime time) {
        return enabled && !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
