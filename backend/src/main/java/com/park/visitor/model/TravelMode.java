package com.park.visitor.model;

/** 出行方式 */
public enum TravelMode {
    WALK("步行"),
    SHUTTLE("摆渡车");

    private final String label;

    TravelMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
