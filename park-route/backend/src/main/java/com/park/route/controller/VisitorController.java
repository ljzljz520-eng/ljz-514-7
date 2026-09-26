package com.park.route.controller;

import com.park.route.dto.GraphResponse;
import com.park.route.dto.RouteRequest;
import com.park.route.dto.RouteResponse;
import com.park.route.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class VisitorController {

    private final RouteService routeService;

    public VisitorController(RouteService routeService) {
        this.routeService = routeService;
    }

    /** 地图全量数据：节点、道路、生效/全部封闭信息 */
    @GetMapping("/graph")
    public GraphResponse graph() {
        return routeService.graph();
    }

    /** 访客路线规划：步行 / 摆渡车，自动避开货车通道与施工封闭 */
    @PostMapping("/route")
    public RouteResponse route(@Valid @RequestBody RouteRequest request) {
        return routeService.route(request);
    }
}
