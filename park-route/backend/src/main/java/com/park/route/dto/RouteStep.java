package com.park.route.dto;

/** 单段指引 */
public record RouteStep(
        String edgeCode,
        String fromCode,
        String toCode,
        String fromName,
        String toName,
        Integer distanceMeters,
        boolean shuttleRoad,
        boolean truckRoad,
        boolean closedBypassed,
        String instruction
) {}
