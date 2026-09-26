package com.park.visitor.model;

import java.util.List;

/** 访客地图数据：节点 + 边 + 当前生效的封闭 */
public record MapData(
        List<ParkNode> nodes,
        List<ParkEdge> edges,
        List<Closure> activeClosures) {
}
