package com.park.visitor.store;

import com.park.visitor.model.Closure;
import com.park.visitor.model.ParkEdge;
import com.park.visitor.model.ParkNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 园区图数据内存存储（节点 / 边 / 临时封闭）。
 * 生产环境可替换为数据库实现，本层接口保持不变。
 */
@Component
public class GraphStore {

    private final Map<String, ParkNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, ParkEdge> edges = new ConcurrentHashMap<>();
    private final Map<String, Closure> closures = new ConcurrentHashMap<>();

    // ---------------- 节点 ----------------

    public List<ParkNode> allNodes() {
        return nodes.values().stream()
                .sorted(Comparator.comparing(ParkNode::getId))
                .toList();
    }

    public Optional<ParkNode> node(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public ParkNode saveNode(ParkNode node) {
        nodes.put(node.getId(), node);
        return node;
    }

    public boolean deleteNode(String id) {
        if (nodes.remove(id) == null) {
            return false;
        }
        // 级联删除关联边与封闭
        edges.values().removeIf(e -> e.getFrom().equals(id) || e.getTo().equals(id));
        closures.values().removeIf(c -> c.getTargetType() == Closure.TargetType.NODE
                && c.getTargetId().equals(id));
        return true;
    }

    // ---------------- 边 ----------------

    public List<ParkEdge> allEdges() {
        return edges.values().stream()
                .sorted(Comparator.comparing(ParkEdge::getId))
                .toList();
    }

    public Optional<ParkEdge> edge(String id) {
        return Optional.ofNullable(edges.get(id));
    }

    public ParkEdge saveEdge(ParkEdge edge) {
        edges.put(edge.getId(), edge);
        return edge;
    }

    public boolean deleteEdge(String id) {
        if (edges.remove(id) == null) {
            return false;
        }
        closures.values().removeIf(c -> c.getTargetType() == Closure.TargetType.EDGE
                && c.getTargetId().equals(id));
        return true;
    }

    // ---------------- 封闭 ----------------

    public List<Closure> allClosures() {
        return closures.values().stream()
                .sorted(Comparator.comparing(Closure::getId))
                .toList();
    }

    public List<Closure> activeClosures() {
        LocalDateTime now = LocalDateTime.now();
        List<Closure> result = new ArrayList<>();
        for (Closure c : closures.values()) {
            if (c.isActiveAt(now)) {
                result.add(c);
            }
        }
        result.sort(Comparator.comparing(Closure::getId));
        return result;
    }

    public Optional<Closure> closure(String id) {
        return Optional.ofNullable(closures.get(id));
    }

    public Closure saveClosure(Closure closure) {
        closures.put(closure.getId(), closure);
        return closure;
    }

    public boolean deleteClosure(String id) {
        return closures.remove(id) != null;
    }

    /** 清空全部数据（仅用于测试/重置） */
    public void clear() {
        nodes.clear();
        edges.clear();
        closures.clear();
    }

    /** 便于调试的快照 */
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nodes", nodes.size());
        m.put("edges", edges.size());
        m.put("closures", closures.size());
        return m;
    }
}
