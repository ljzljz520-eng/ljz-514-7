package com.park.visitor.model;

import java.util.List;

/** 路线规划结果 */
public record RouteResult(
        boolean found,
        String message,
        TravelMode mode,
        double totalDistanceMeters,
        double totalDurationSeconds,
        List<String> pathNodeIds,
        List<RouteSegment> segments) {

    public static RouteResult notFound(String message, TravelMode mode) {
        return new RouteResult(false, message, mode, 0, 0, List.of(), List.of());
    }
}
