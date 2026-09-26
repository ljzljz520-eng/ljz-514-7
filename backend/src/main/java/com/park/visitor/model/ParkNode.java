package com.park.visitor.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 园区节点：门岗、建筑、路口、摆渡站等 */
public class ParkNode {

    @NotBlank(message = "节点ID不能为空")
    private String id;

    @NotBlank(message = "节点名称不能为空")
    private String name;

    @NotNull(message = "节点类型不能为空")
    private NodeType type;

    /** 地图坐标（米，原点为园区西北角） */
    private double x;
    private double y;

    /** 是否对访客开放为目的地的兴趣点 */
    private boolean visitorDestination;

    public ParkNode() {
    }

    public ParkNode(String id, String name, NodeType type, double x, double y, boolean visitorDestination) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.visitorDestination = visitorDestination;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isVisitorDestination() {
        return visitorDestination;
    }

    public void setVisitorDestination(boolean visitorDestination) {
        this.visitorDestination = visitorDestination;
    }
}
