package com.park.route.dto;

import java.util.List;

public record RouteResponse(
        boolean found,
        String mode,
        String fromCode,
        String fromName,
        String toCode,
        String toName,
        Integer totalMeters,
        Integer estimatedMinutes,
        List<String> nodePath,
        List<String> polyline,
        List<RouteStep> steps,
        List<String> warnings,
        String message
) {
    public static RouteResponse notFound(String fromName, String toName, String mode,
                                         boolean closuresActive, boolean truckFiltered) {
        StringBuilder msg = new StringBuilder("当前没有可通行的");
        msg.append("SHUTTLE".equals(mode) ? "摆渡车" : "步行").append("路线");
        if (closuresActive) msg.append("，存在临时封闭路段");
        if (truckFiltered) msg.append("，已按规定避开货车通道");
        msg.append("，请联系门岗或管理员。");
        return new RouteResponse(false, mode, null, fromName, null, toName,
                null, null, List.of(), List.of(), List.of(), List.of(), msg.toString());
    }
}
