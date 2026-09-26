package com.park.visitor.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 园区道路边 */
public class ParkEdge {

    @NotBlank(message = "边ID不能为空")
    private String id;

    @NotBlank(message = "起点节点不能为空")
    private String from;

    @NotBlank(message = "终点节点不能为空")
    private String to;

    @NotNull(message = "道路类型不能为空")
    private EdgeType type;

    /** 长度（米）。<=0 时由系统按节点坐标自动计算 */
    private double distance;

    /** 是否双向通行，默认 true */
    private boolean bidirectional = true;

    public ParkEdge() {
    }

    public ParkEdge(String id, String from, String to, EdgeType type, double distance, boolean bidirectional) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.type = type;
        this.distance = distance;
        this.bidirectional = bidirectional;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public EdgeType getType() {
        return type;
    }

    public void setType(EdgeType type) {
        this.type = type;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    public void setBidirectional(boolean bidirectional) {
        this.bidirectional = bidirectional;
    }
}
