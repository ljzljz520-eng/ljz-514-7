package com.park.route.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 临时封闭信息（管理员维护，寻路时实时避让） */
@Entity
@Table(name = "closure")
public class Closure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "edge_code", nullable = false, length = 32)
    private String edgeCode;

    @Column(nullable = false, length = 200)
    private String reason;

    /** 是否启用（可临时解除封闭而不删除记录） */
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    public Closure() {}

    public Closure(String edgeCode, String reason, Boolean enabled,
                   LocalDateTime startAt, LocalDateTime endAt) {
        this.edgeCode = edgeCode;
        this.reason = reason;
        this.enabled = enabled;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    /** 当前时刻该封闭是否生效 */
    public boolean effectiveAt(LocalDateTime now) {
        if (!Boolean.TRUE.equals(enabled)) return false;
        if (startAt != null && now.isBefore(startAt)) return false;
        if (endAt != null && now.isAfter(endAt)) return false;
        return true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEdgeCode() { return edgeCode; }
    public void setEdgeCode(String edgeCode) { this.edgeCode = edgeCode; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
}
