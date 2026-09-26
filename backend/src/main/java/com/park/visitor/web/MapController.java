package com.park.visitor.web;

import com.park.visitor.model.MapData;
import com.park.visitor.model.RouteRequest;
import com.park.visitor.model.RouteResult;
import com.park.visitor.service.RouteService;
import com.park.visitor.store.GraphStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 访客端 API：园区地图与路线规划 */
@RestController
@RequestMapping("/api")
public class MapController {

    private final GraphStore store;
    private final RouteService routeService;

    public MapController(GraphStore store, RouteService routeService) {
        this.store = store;
        this.routeService = routeService;
    }

    /** 园区地图：节点 + 道路 + 当前生效的封闭 */
    @GetMapping("/map")
    public MapData map() {
        return new MapData(store.allNodes(), store.allEdges(), store.activeClosures());
    }

    /** 路线规划 */
    @PostMapping("/route")
    public RouteResult route(@Valid @RequestBody RouteRequest request) {
        return routeService.plan(request.fromId(), request.toId(), request.mode());
    }
}
