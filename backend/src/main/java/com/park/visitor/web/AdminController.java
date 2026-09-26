package com.park.visitor.web;

import com.park.visitor.model.Closure;
import com.park.visitor.model.ParkEdge;
import com.park.visitor.model.ParkNode;
import com.park.visitor.store.GraphStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** 管理端 API：节点 / 边 / 临时封闭的维护 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final GraphStore store;

    public AdminController(GraphStore store) {
        this.store = store;
    }

    // ---------------- 节点 ----------------

    @GetMapping("/nodes")
    public List<ParkNode> nodes() {
        return store.allNodes();
    }

    @PostMapping("/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public ParkNode createNode(@Valid @RequestBody ParkNode node) {
        if (store.node(node.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "节点ID已存在：" + node.getId());
        }
        return store.saveNode(node);
    }

    @PutMapping("/nodes/{id}")
    public ParkNode updateNode(@PathVariable String id, @Valid @RequestBody ParkNode node) {
        if (store.node(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "节点不存在：" + id);
        }
        node.setId(id);
        return store.saveNode(node);
    }

    @DeleteMapping("/nodes/{id}")
    public Map<String, Object> deleteNode(@PathVariable String id) {
        if (!store.deleteNode(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "节点不存在：" + id);
        }
        return Map.of("deleted", id);
    }

    // ---------------- 边 ----------------

    @GetMapping("/edges")
    public List<ParkEdge> edges() {
        return store.allEdges();
    }

    @PostMapping("/edges")
    @ResponseStatus(HttpStatus.CREATED)
    public ParkEdge createEdge(@Valid @RequestBody ParkEdge edge) {
        validateEdge(edge);
        if (store.edge(edge.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "边ID已存在：" + edge.getId());
        }
        return store.saveEdge(edge);
    }

    @PutMapping("/edges/{id}")
    public ParkEdge updateEdge(@PathVariable String id, @Valid @RequestBody ParkEdge edge) {
        if (store.edge(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "边不存在：" + id);
        }
        edge.setId(id);
        validateEdge(edge);
        return store.saveEdge(edge);
    }

    @DeleteMapping("/edges/{id}")
    public Map<String, Object> deleteEdge(@PathVariable String id) {
        if (!store.deleteEdge(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "边不存在：" + id);
        }
        return Map.of("deleted", id);
    }

    private void validateEdge(ParkEdge edge) {
        if (edge.getFrom().equals(edge.getTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "边的起点和终点不能相同");
        }
        if (store.node(edge.getFrom()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "起点节点不存在：" + edge.getFrom());
        }
        if (store.node(edge.getTo()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "终点节点不存在：" + edge.getTo());
        }
    }

    // ---------------- 临时封闭 ----------------

    @GetMapping("/closures")
    public List<Closure> closures() {
        return store.allClosures();
    }

    @PostMapping("/closures")
    @ResponseStatus(HttpStatus.CREATED)
    public Closure createClosure(@Valid @RequestBody Closure closure) {
        validateClosure(closure);
        if (store.closure(closure.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "封闭ID已存在：" + closure.getId());
        }
        return store.saveClosure(closure);
    }

    @PutMapping("/closures/{id}")
    public Closure updateClosure(@PathVariable String id, @Valid @RequestBody Closure closure) {
        if (store.closure(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "封闭记录不存在：" + id);
        }
        closure.setId(id);
        validateClosure(closure);
        return store.saveClosure(closure);
    }

    @DeleteMapping("/closures/{id}")
    public Map<String, Object> deleteClosure(@PathVariable String id) {
        if (!store.deleteClosure(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "封闭记录不存在：" + id);
        }
        return Map.of("deleted", id);
    }

    private void validateClosure(Closure closure) {
        if (closure.getEndTime().isBefore(closure.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间不能早于开始时间");
        }
        boolean exists = switch (closure.getTargetType()) {
            case NODE -> store.node(closure.getTargetId()).isPresent();
            case EDGE -> store.edge(closure.getTargetId()).isPresent();
        };
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "封闭对象不存在：" + closure.getTargetId());
        }
    }
}
