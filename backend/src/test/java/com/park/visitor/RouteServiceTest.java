package com.park.visitor;

import com.park.visitor.config.DataSeeder;
import com.park.visitor.model.Closure;
import com.park.visitor.model.EdgeType;
import com.park.visitor.model.RouteResult;
import com.park.visitor.model.RouteSegment;
import com.park.visitor.model.TravelMode;
import com.park.visitor.service.RouteService;
import com.park.visitor.store.GraphStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteServiceTest {

    private GraphStore store;
    private RouteService routeService;

    @BeforeEach
    void setUp() throws Exception {
        store = new GraphStore();
        new DataSeeder(store).run(null);
        routeService = new RouteService(store);
    }

    @Test
    void walkingRouteAvoidsTruckLanes() {
        RouteResult r = routeService.plan("gate-west", "office", TravelMode.WALK);
        assertTrue(r.found());
        // 路径中不允许出现货运节点
        assertFalse(r.pathNodeIds().stream().anyMatch(id -> id.startsWith("t")));
        // 所有分段均为步行
        assertTrue(r.segments().stream().allMatch(s -> s.type() == EdgeType.WALK));
    }

    @Test
    void walkingRouteDetoursAroundConstruction() {
        // w04 (j3-j4) 施工封闭中，步行路线不得经过该边
        RouteResult r = routeService.plan("gate-west", "office", TravelMode.WALK);
        assertTrue(r.found());
        for (int i = 0; i + 1 < r.pathNodeIds().size(); i++) {
            String a = r.pathNodeIds().get(i);
            String b = r.pathNodeIds().get(i + 1);
            assertFalse((a.equals("j3") && b.equals("j4")) || (a.equals("j4") && b.equals("j3")),
                    "路线不应经过施工封闭的 w04");
        }
    }

    @Test
    void shuttleRouteUsesShuttleLoop() {
        RouteResult r = routeService.plan("gate-west", "office", TravelMode.SHUTTLE);
        assertTrue(r.found());
        assertTrue(r.segments().stream().anyMatch(s -> s.type() == EdgeType.SHUTTLE),
                "摆渡模式应包含摆渡段");
        // 摆渡应比纯步行快
        RouteResult walk = routeService.plan("gate-west", "office", TravelMode.WALK);
        assertTrue(r.totalDurationSeconds() < walk.totalDurationSeconds());
    }

    @Test
    void closedNodeBlocksRoute() {
        store.saveClosure(new Closure("cx", Closure.TargetType.NODE, "office",
                "消防演练", LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2), true));
        RouteResult r = routeService.plan("gate-west", "office", TravelMode.WALK);
        assertFalse(r.found());
    }

    @Test
    void expiredClosureDoesNotApply() {
        store.saveClosure(new Closure("cx", Closure.TargetType.EDGE, "w03",
                "已结束的施工", LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1), true));
        // w03 (j1-j3) 上的封闭已过期，gate-west → office 的最短步行路线应经过 j3
        RouteResult r = routeService.plan("gate-west", "office", TravelMode.WALK);
        assertTrue(r.found());
        assertTrue(r.pathNodeIds().contains("j3"), "过期封闭不应生效");
    }

    @Test
    void unreachableWhenFullyCutOff() {
        // 封闭会议中心周边所有步行边后不可达
        LocalDateTime now = LocalDateTime.now();
        int i = 0;
        for (String edgeId : new String[]{"w07", "w13", "w18"}) {
            store.saveClosure(new Closure("cz" + (i++), Closure.TargetType.EDGE, edgeId,
                    "测试封闭", now.minusHours(1), now.plusHours(1), true));
        }
        RouteResult r = routeService.plan("gate-west", "conf-center", TravelMode.WALK);
        assertFalse(r.found());
    }

    @Test
    void sameStartAndEnd() {
        RouteResult r = routeService.plan("office", "office", TravelMode.WALK);
        assertTrue(r.found());
        assertEquals(0, r.totalDistanceMeters());
    }

    @Test
    void shuttleSegmentsHaveInstructions() {
        RouteResult r = routeService.plan("gate-west", "workshop", TravelMode.SHUTTLE);
        assertTrue(r.found());
        for (RouteSegment s : r.segments()) {
            assertFalse(s.instruction().isBlank());
            assertFalse(s.points().isEmpty());
        }
    }
}
