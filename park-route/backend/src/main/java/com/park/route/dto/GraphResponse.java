package com.park.route.dto;

import com.park.route.model.Closure;
import com.park.route.model.MapEdge;
import com.park.route.model.MapNode;

import java.util.List;

/** 地图全量数据（访客端绘图 + 管理员端展示） */
public record GraphResponse(List<MapNode> nodes, List<MapEdge> edges, List<Closure> closures) {}
