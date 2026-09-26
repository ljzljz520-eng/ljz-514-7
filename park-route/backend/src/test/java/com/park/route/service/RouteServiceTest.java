package com.park.route.service;

import com.park.route.dto.RouteRequest;
import com.park.route.dto.RouteResponse;
import com.park.route.model.*;
import com.park.route.repositories.ClosureRepository;
import com.park.route.repositories.MapEdgeRepository;
import com.park.route.repositories.MapNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock MapNodeRepository nodeRepo;
    @Mock MapEdgeRepository edgeRepo;
    @Mock ClosureRepository closureRepo;

    private RouteService service;

    /**
     * 图：
     *   G --(步行近道 100, 封闭)--> W(车间)
     *   G --(货车通道 120, truck)--> W(车间)
     *   G --(访客道路 300, walk+shuttle)--> C --> W(100, walk+shuttle)
     */
    @BeforeEach
    void setup() {
        when(nodeRepo.findAll()).thenReturn(List.of(
                new MapNode("G", "门岗", NodeType.GATE, 0, 0),
                new MapNode("W", "车间", NodeType.WORKSHOP, 10, 0),
                new MapNode("C", "中心站", NodeType.STOP, 5, 5)
        ));
        when(edgeRepo.findAll()).thenReturn(List.of(
                new MapEdge("NEAR", "G", "W", 100, true, false, false),
                new MapEdge("TRUCK", "G", "W", 120, false, false, true),
                new MapEdge("E1", "G", "C", 300, true, true, false),
                new MapEdge("E2", "C", "W", 100, true, true, false)
        ));
        when(closureRepo.findAll()).thenReturn(List.of());
        service = new RouteService(nodeRepo, edgeRepo, closureRepo);
    }

    @Test
    void walk_avoids_truck_road_and_takes_visitor_path() {
        RouteResponse r = service.route(new RouteRequest("G", "W", TravelMode.WALK));
        // 无封闭时：近道 100 可用，应为最短
        assertThat(r.found()).isTrue();
        assertThat(r.nodePath()).containsExactly("G", "W");
        assertThat(r.steps()).noneMatch(s -> s.truckRoad());
    }

    @Test
    void walk_bypasses_closed_edge_even_if_shorter() {
        when(closureRepo.findAll()).thenReturn(List.of(
                new Closure("NEAR", "施工封闭", true, null, null)));
        service = new RouteService(nodeRepo, edgeRepo, closureRepo);

        RouteResponse r = service.route(new RouteRequest("G", "W", TravelMode.WALK));
        assertThat(r.found()).isTrue();
        // 近道封闭、货车道禁行，只能绕中心站 400m
        assertThat(r.nodePath()).containsExactly("G", "C", "W");
        assertThat(r.totalMeters()).isEqualTo(400);
        assertThat(r.warnings().toString()).contains("1 段道路临时封闭");
    }

    @Test
    void shuttle_only_uses_shuttle_edges() {
        RouteResponse r = service.route(new RouteRequest("G", "W", TravelMode.SHUTTLE));
        assertThat(r.found()).isTrue();
        // NEAR 步行专用、TRUCK 货车专用，都不能走
        assertThat(r.nodePath()).containsExactly("G", "C", "W");
    }

    @Test
    void no_route_when_all_visitor_paths_closed() {
        when(closureRepo.findAll()).thenReturn(List.of(
                new Closure("NEAR", "a", true, null, null),
                new Closure("E1", "b", true, null, null),
                new Closure("E2", "c", true, null, null)));
        service = new RouteService(nodeRepo, edgeRepo, closureRepo);

        RouteResponse r = service.route(new RouteRequest("G", "W", TravelMode.WALK));
        assertThat(r.found()).isFalse();
        assertThat(r.message()).contains("没有可通行");
    }

    @Test
    void future_closure_does_not_take_effect() {
        when(closureRepo.findAll()).thenReturn(List.of(
                new Closure("NEAR", "未来才封", true,
                        java.time.LocalDateTime.now().plusDays(1),
                        java.time.LocalDateTime.now().plusDays(2))));
        service = new RouteService(nodeRepo, edgeRepo, closureRepo);

        RouteResponse r = service.route(new RouteRequest("G", "W", TravelMode.WALK));
        assertThat(r.found()).isTrue();
        assertThat(r.nodePath()).containsExactly("G", "W");
    }
}
