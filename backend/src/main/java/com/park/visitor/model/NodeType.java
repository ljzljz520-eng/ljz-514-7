package com.park.visitor.model;

/** 节点类型 */
public enum NodeType {
    GATE("门岗"),
    OFFICE("办公楼"),
    WORKSHOP("样板车间"),
    CONFERENCE("会议中心"),
    JUNCTION("路口"),
    SHUTTLE_STOP("摆渡车站"),
    FACILITY("其他设施");

    private final String label;

    NodeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
