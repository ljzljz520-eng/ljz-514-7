package com.park.route.service;

import com.park.route.dto.*;
import com.park.route.exception.ApiException;
import com.park.route.model.*;
import com.park.route.repositories.ClosureRepository;
import com.park.route.repositories.MapEdgeRepository;
import com.park.route.repositories.MapNodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RouteService {

    /** 步行速度 80 m/min（约 4.8 km/h） */
    private static final int WALK_SPEED_M_PER_MIN = 80;
    /** 摆渡车均速（含站点停靠）约 240 m/min */
    private static final int SHUTTLE_SPEED_M_PER_MIN = 240;

    private final MapNodeRepository nodeRepo;
    private final MapEdgeRepository edgeRepo;
    private final ClosureRepository closureRepo;

    public RouteService(MapNodeRepository nodeRepo, MapEdgeRepository edgeRepo,
                        ClosureRepository closureRepo) {
        this.nodeRepo = nodeRepo;
        this.edgeRepo = edgeRepo;
        this.closureRepo = closureRepo;
    }

    public GraphResponse graph() {
        return new GraphResponse(nodeRepo.findAll(), edgeRepo.findAll(), closureRepo.findAll());
    }

    public RouteResponse route(RouteRequest req) {
        Map<String, MapNode> nodes = new HashMap<>();
        nodeRepo.findAll().forEach(n -> nodes.put(n.getCode(), n));

        MapNode from = requireNode(nodes, req.from(), "起点");
        MapNode to = requireNode(nodes, req.to(), "终点");
        if (from.getCode().equals(to.getCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "起点和终点不能相同");
        }

        // 当前生效的封闭边
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> closedEdges = new HashMap<>();
        for (Closure c : closureRepo.findAll()) {
            if (c.effectiveAt(now)) {
                closedEdges.put(c.getEdgeCode(), c.getReason());
            }
        }

        // 邻接表：无向边双向加入，过滤失效边、货车通道、封闭边
        Map<String, List<EdgeView>> graph = new HashMap<>();
        int truckSkipped = 0;
        int closedSkipped = 0;
        for (MapEdge e : edgeRepo.findAll()) {
            if (!Boolean.TRUE.equals(e.getActive())) continue;
            if (!nodes.containsKey(e.getFromCode()) || !nodes.containsKey(e.getToCode())) continue;
            MapNode a = nodes.get(e.getFromCode());
            MapNode b = nodes.get(e.getToCode());
            if (!Boolean.TRUE.equals(a.getActive()) || !Boolean.TRUE.equals(b.getActive())) continue;

            boolean closed = closedEdges.containsKey(e.getCode());
            boolean modeOk = req.mode() == TravelMode.WALK
                    ? Boolean.TRUE.equals(e.getWalkAllowed())
                    : Boolean.TRUE.equals(e.getShuttleAllowed());
            boolean truck = Boolean.TRUE.equals(e.getTruckRoad());

            if (closed) closedSkipped++;
            if (truck) truckSkipped++;
            if (closed || truck || !modeOk) continue;

            EdgeView view = new EdgeView(e);
            graph.computeIfAbsent(e.getFromCode(), k -> new ArrayList<>()).add(view);
            graph.computeIfAbsent(e.getToCode(), k -> new ArrayList<>())
                    .add(new EdgeView(e.getCode(), e.getToCode(), e.getFromCode(), e.getDistanceMeters(),
                            e.getWalkAllowed(), e.getShuttleAllowed(), e.getTruckRoad()));
        }

        // Dijkstra（权重为米）
        Map<String, Long> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(
                Comparator.comparingLong(c -> dist.getOrDefault(c, Long.MAX_VALUE)));
        dist.put(from.getCode(), 0L);
        pq.add(from.getCode());

        while (!pq.isEmpty()) {
            String cur = pq.poll();
            long curDist = dist.get(cur);
            if (cur.equals(to.getCode())) break;
            for (EdgeView v : graph.getOrDefault(cur, List.of())) {
                long nd = curDist + v.distanceMeters();
                if (nd < dist.getOrDefault(v.to(), Long.MAX_VALUE)) {
                    dist.put(v.to(), nd);
                    prev.put(v.to(), cur);
                    pq.add(v.to());
                }
            }
        }

        if (!dist.containsKey(to.getCode())) {
            return RouteResponse.notFound(from.getName(), to.getName(),
                    req.mode().name(), closedSkipped > 0, truckSkipped > 0);
        }

        // 回溯路径
        LinkedList<String> path = new LinkedList<>();
        String cur = to.getCode();
        while (cur != null) {
            path.addFirst(cur);
            if (cur.equals(from.getCode())) break;
            cur = prev.get(cur);
        }

        // 收集边视图
        List<EdgeView> used = new ArrayList<>();
        int total = 0;
        for (int i = 0; i + 1 < path.size(); i++) {
            String a = path.get(i);
            String b = path.get(i + 1);
            EdgeView v = graph.get(a).stream().filter(x -> x.to().equals(b)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("路径边缺失"));
            used.add(v);
            total += v.distanceMeters();
        }

        // 生成指引
        List<RouteStep> steps = new ArrayList<>();
        for (int i = 0; i < used.size(); i++) {
            EdgeView v = used.get(i);
            MapNode a = nodes.get(v.from());
            MapNode b = nodes.get(v.to());
            String instruction = buildInstruction(req.mode(), a, b, i, used.size());
            steps.add(new RouteStep(v.code(), v.from(), v.to(), a.getName(), b.getName(),
                    v.distanceMeters(), v.shuttleAllowed(), v.truckRoad(),
                    closedEdges.containsKey(v.code()), instruction));
        }

        List<String> polyline = new ArrayList<>();
        for (String code : path) polyline.add(code);

        List<String> warnings = new ArrayList<>();
        if (req.mode() == TravelMode.WALK) {
            warnings.add("步行路线已全程避开货车通道。");
        } else {
            warnings.add("摆渡车路线仅走园区摆渡环路，已避开货车通道。");
            warnings.add("摆渡车约 10-15 分钟一班，请在站点候车并听从调度。");
        }
        if (closedSkipped > 0) {
            warnings.add("当前有 " + closedSkipped + " 段道路临时封闭，路线已自动绕行。");
            Set<String> reasons = new LinkedHashSet<>();
            for (Map.Entry<String, String> en : closedEdges.entrySet()) {
                reasons.add("· " + edgeName(en.getKey(), nodes, edgeRepo) + "：" + en.getValue());
            }
            warnings.add(String.join("\n", reasons));
        }

        int speed = req.mode() == TravelMode.WALK ? WALK_SPEED_M_PER_MIN : SHUTTLE_SPEED_M_PER_MIN;
        int minutes = Math.max(1, (int) Math.round(total / (double) speed));

        return new RouteResponse(true, req.mode().name(), from.getCode(), from.getName(),
                to.getCode(), to.getName(), total, minutes, path, polyline, steps, warnings, null);
    }

    private MapNode requireNode(Map<String, MapNode> nodes, String code, String label) {
        MapNode n = nodes.get(code);
        if (n == null) throw new ApiException(HttpStatus.BAD_REQUEST, label + "节点不存在: " + code);
        if (!Boolean.TRUE.equals(n.getActive()))
            throw new ApiException(HttpStatus.BAD_REQUEST, label + "节点已停用: " + n.getName());
        return n;
    }

    private String edgeName(String edgeCode, Map<String, MapNode> nodes, MapEdgeRepository edges) {
        return edges.findByCode(edgeCode)
                .map(e -> {
                    MapNode a = nodes.get(e.getFromCode());
                    MapNode b = nodes.get(e.getToCode());
                    return (a != null ? a.getName() : e.getFromCode()) + " ↔ "
                            + (b != null ? b.getName() : e.getToCode());
                })
                .orElse(edgeCode);
    }

    private String buildInstruction(TravelMode mode, MapNode a, MapNode b, int index, int total) {
        String action = mode == TravelMode.WALK ? "步行" : "乘坐摆渡车";
        if (index == 0) {
            return "从【" + a.getName() + "】出发，" + action + "约 " + describeArrival(b);
        }
        if (index == total - 1) {
            return "到达【" + b.getName() + "】，结束行程。";
        }
        return "经【" + a.getName() + "】继续" + action + "前往" + describeArrival(b);
    }

    private String describeArrival(MapNode b) {
        String suffix = switch (b.getType()) {
            case GATE -> "门岗";
            case BUILDING -> "办公楼";
            case WORKSHOP -> "样板车间";
            case CONFERENCE -> "会议中心";
            case STOP -> "摆渡车站";
            case CONSTRUCTION -> "施工区（请勿进入）";
            case LOGISTICS -> "物流区";
            case JUNCTION -> "路口";
        };
        return "【" + b.getName() + "】（" + suffix + "）方向";
    }

    /** 内部边视图（带方向） */
    private record EdgeView(String code, String from, String to, int distanceMeters,
                            boolean walkAllowed, boolean shuttleAllowed, boolean truckRoad) {
        EdgeView(MapEdge e) {
            this(e.getCode(), e.getFromCode(), e.getToCode(), e.getDistanceMeters(),
                    Boolean.TRUE.equals(e.getWalkAllowed()),
                    Boolean.TRUE.equals(e.getShuttleAllowed()),
                    Boolean.TRUE.equals(e.getTruckRoad()));
        }
    }
}
