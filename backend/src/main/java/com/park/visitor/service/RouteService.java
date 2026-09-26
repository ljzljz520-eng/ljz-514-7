package com.park.visitor.service;

import com.park.visitor.model.Closure;
import com.park.visitor.model.EdgeType;
import com.park.visitor.model.ParkEdge;
import com.park.visitor.model.ParkNode;
import com.park.visitor.model.RouteResult;
import com.park.visitor.model.RouteSegment;
import com.park.visitor.model.TravelMode;
import com.park.visitor.store.GraphStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 访客路线规划服务。
 *
 * <p>规则：
 * <ul>
 *   <li>货车通道（TRUCK）对访客永远不可用；</li>
 *   <li>生效中的临时封闭（施工区域等）从图中剔除；</li>
 *   <li>步行模式仅使用步行道；摆渡模式使用步行道 + 摆渡车道，按总耗时最优；</li>
 *   <li>摆渡车每次上车计一次候车时间，通过 (节点, 是否在车上) 状态扩展建模。</li>
 * </ul>
 */
@Service
public class RouteService {

    /** 步行速度 m/s（约 5 km/h） */
    private static final double WALK_SPEED = 1.4;
    /** 摆渡车速度 m/s（约 18 km/h） */
    private static final double SHUTTLE_SPEED = 5.0;
    /** 每次乘坐摆渡车的候车时间（秒） */
    private static final double BOARDING_WAIT_SECONDS = 120;

    private final GraphStore store;

    public RouteService(GraphStore store) {
        this.store = store;
    }

    /** Dijkstra 状态：所在节点 + 是否正在摆渡车上 */
    private record State(String nodeId, boolean onShuttle) {
    }

    private record Adj(ParkEdge edge, double length) {
    }

    public RouteResult plan(String fromId, String toId, TravelMode mode) {
        ParkNode from = store.node(fromId).orElse(null);
        ParkNode to = store.node(toId).orElse(null);
        if (from == null) {
            return RouteResult.notFound("起点不存在：" + fromId, mode);
        }
        if (to == null) {
            return RouteResult.notFound("终点不存在：" + toId, mode);
        }

        // 生效中的封闭
        LocalDateTime now = LocalDateTime.now();
        Set<String> closedNodes = new HashSet<>();
        Set<String> closedEdges = new HashSet<>();
        for (Closure c : store.allClosures()) {
            if (c.isActiveAt(now)) {
                if (c.getTargetType() == Closure.TargetType.NODE) {
                    closedNodes.add(c.getTargetId());
                } else {
                    closedEdges.add(c.getTargetId());
                }
            }
        }

        if (closedNodes.contains(fromId)) {
            return RouteResult.notFound("起点「" + from.getName() + "」当前处于封闭区域", mode);
        }
        if (closedNodes.contains(toId)) {
            return RouteResult.notFound("终点「" + to.getName() + "」当前处于封闭区域", mode);
        }
        if (fromId.equals(toId)) {
            return new RouteResult(true, "您已到达目的地", mode, 0, 0,
                    List.of(fromId), List.of());
        }

        // 建图（剔除货车通道与封闭对象）
        Map<String, List<Adj>> adj = new HashMap<>();
        for (ParkEdge e : store.allEdges()) {
            if (e.getType() == EdgeType.TRUCK) {
                continue; // 访客避开货车通道
            }
            if (mode == TravelMode.WALK && e.getType() != EdgeType.WALK) {
                continue; // 步行模式仅步行道
            }
            if (closedEdges.contains(e.getId())
                    || closedNodes.contains(e.getFrom())
                    || closedNodes.contains(e.getTo())) {
                continue; // 施工封闭
            }
            double len = edgeLength(e);
            adj.computeIfAbsent(e.getFrom(), k -> new ArrayList<>()).add(new Adj(e, len));
            if (e.isBidirectional()) {
                adj.computeIfAbsent(e.getTo(), k -> new ArrayList<>()).add(new Adj(e, len));
            }
        }

        // Dijkstra
        Map<State, Double> dist = new HashMap<>();
        Map<State, State> prevState = new HashMap<>();
        Map<State, ParkEdge> prevEdge = new HashMap<>();
        PriorityQueue<long[]> queue = new PriorityQueue<>(java.util.Comparator.comparingLong(a -> a[0]));
        // 用 double 键的优先队列
        PriorityQueue<Entry> pq = new PriorityQueue<>(java.util.Comparator.comparingDouble(Entry::cost));

        State start = new State(fromId, false);
        dist.put(start, 0.0);
        pq.add(new Entry(0.0, start));

        while (!pq.isEmpty()) {
            Entry top = pq.poll();
            State s = top.state();
            if (top.cost() > dist.getOrDefault(s, Double.MAX_VALUE) + 1e-9) {
                continue;
            }
            for (Adj a : adj.getOrDefault(s.nodeId(), List.of())) {
                ParkEdge e = a.edge();
                String nextNode = e.getFrom().equals(s.nodeId()) ? e.getTo() : e.getFrom();
                boolean shuttle = e.getType() == EdgeType.SHUTTLE;
                double travel = a.length() / (shuttle ? SHUTTLE_SPEED : WALK_SPEED);
                // 乘摆渡车且当前不在车上时，计一次候车时间
                double wait = shuttle && !s.onShuttle() ? BOARDING_WAIT_SECONDS : 0;
                State next = new State(nextNode, shuttle);
                double cost = top.cost() + travel + wait;
                if (cost + 1e-9 < dist.getOrDefault(next, Double.MAX_VALUE)) {
                    dist.put(next, cost);
                    prevState.put(next, s);
                    prevEdge.put(next, e);
                    pq.add(new Entry(cost, next));
                }
            }
        }

        // 终点：步行到达或在摆渡站下车均可
        State best = null;
        double bestCost = Double.MAX_VALUE;
        for (boolean onShuttle : new boolean[]{false, true}) {
            State cand = new State(toId, onShuttle);
            double c = dist.getOrDefault(cand, Double.MAX_VALUE);
            if (c < bestCost) {
                bestCost = c;
                best = cand;
            }
        }
        if (best == null) {
            return RouteResult.notFound(
                    "未找到从「" + from.getName() + "」到「" + to.getName() + "」的可达路线，可能受施工封闭影响", mode);
        }

        // 回溯路径
        Deque<String> nodePath = new ArrayDeque<>();
        Deque<ParkEdge> edgePath = new ArrayDeque<>();
        State cur = best;
        nodePath.addFirst(cur.nodeId());
        while (prevState.containsKey(cur)) {
            ParkEdge e = prevEdge.get(cur);
            State p = prevState.get(cur);
            edgePath.addFirst(e);
            nodePath.addFirst(p.nodeId());
            cur = p;
        }

        List<RouteSegment> segments = buildSegments(new ArrayList<>(nodePath), new ArrayList<>(edgePath));
        double totalDist = segments.stream().mapToDouble(RouteSegment::distanceMeters).sum();
        double totalTime = segments.stream().mapToDouble(RouteSegment::durationSeconds).sum();

        String msg = mode == TravelMode.WALK ? "步行路线规划成功" : "摆渡车路线规划成功（含步行接驳与候车时间）";
        return new RouteResult(true, msg, mode, totalDist, totalTime,
                new ArrayList<>(nodePath), segments);
    }

    private record Entry(double cost, State state) {
    }

    /** 将边序列按类型合并为步行段 / 摆渡段，并生成导航说明 */
    private List<RouteSegment> buildSegments(List<String> nodePath, List<ParkEdge> edgePath) {
        List<RouteSegment> segments = new ArrayList<>();
        int i = 0;
        while (i < edgePath.size()) {
            EdgeType type = edgePath.get(i).getType();
            List<String> segNodes = new ArrayList<>();
            List<double[]> segPoints = new ArrayList<>();
            double segDist = 0;
            double segTime = 0;
            segNodes.add(nodePath.get(i));
            segPoints.add(pointOf(nodePath.get(i)));
            int j = i;
            while (j < edgePath.size() && edgePath.get(j).getType() == type) {
                ParkEdge e = edgePath.get(j);
                double len = edgeLength(e);
                segDist += len;
                segTime += len / (type == EdgeType.SHUTTLE ? SHUTTLE_SPEED : WALK_SPEED);
                segNodes.add(nodePath.get(j + 1));
                segPoints.add(pointOf(nodePath.get(j + 1)));
                j++;
            }
            if (type == EdgeType.SHUTTLE) {
                segTime += BOARDING_WAIT_SECONDS;
            }
            String fromName = nameOf(segNodes.get(0));
            String toName = nameOf(segNodes.get(segNodes.size() - 1));
            String instruction;
            if (type == EdgeType.SHUTTLE) {
                int stops = segNodes.size() - 1;
                instruction = "在「" + fromName + "」乘坐摆渡车，途经 " + stops + " 站，到「" + toName + "」下车";
            } else {
                instruction = "从「" + fromName + "」步行至「" + toName + "」";
            }
            segments.add(new RouteSegment(type, instruction, segNodes, segPoints,
                    Math.round(segDist * 10) / 10.0, Math.round(segTime)));
            i = j;
        }
        return segments;
    }

    private double[] pointOf(String nodeId) {
        ParkNode n = store.node(nodeId).orElseThrow();
        return new double[]{n.getX(), n.getY()};
    }

    private String nameOf(String nodeId) {
        return store.node(nodeId).map(ParkNode::getName).orElse(nodeId);
    }

    /** 边长：优先使用管理员设定值，否则按节点坐标欧氏距离计算 */
    public double edgeLength(ParkEdge e) {
        if (e.getDistance() > 0) {
            return e.getDistance();
        }
        ParkNode a = store.node(e.getFrom()).orElse(null);
        ParkNode b = store.node(e.getTo()).orElse(null);
        if (a == null || b == null) {
            return 0;
        }
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }
}
