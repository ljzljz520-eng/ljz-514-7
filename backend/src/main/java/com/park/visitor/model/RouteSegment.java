package com.park.visitor.model;

import java.util.List;

/**
 * 路线分段：连续的同类型道路合并为一段（步行段 / 摆渡段）
 */
public record RouteSegment(
        EdgeType type,
        String instruction,
        List<String> nodeIds,
        List<double[]> points,
        double distanceMeters,
        double durationSeconds) {
}
