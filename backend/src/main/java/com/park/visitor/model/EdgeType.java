package com.park.visitor.model;

/** 边（道路）类型 */
public enum EdgeType {
    WALK("步行道"),
    SHUTTLE("摆渡车道"),
    TRUCK("货车通道");

    private final String label;

    EdgeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
