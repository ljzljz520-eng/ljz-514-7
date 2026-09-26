package com.park.route.controller;

import com.park.route.exception.ApiException;
import com.park.route.model.*;
import com.park.route.repositories.ClosureRepository;
import com.park.route.repositories.MapEdgeRepository;
import com.park.route.repositories.MapNodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 管理员维护：节点、边、临时封闭 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final MapNodeRepository nodeRepo;
    private final MapEdgeRepository edgeRepo;
    private final ClosureRepository closureRepo;

    public AdminController(MapNodeRepository nodeRepo, MapEdgeRepository edgeRepo,
                           ClosureRepository closureRepo) {
        this.nodeRepo = nodeRepo;
        this.edgeRepo = edgeRepo;
        this.closureRepo = closureRepo;
    }

    // ---------- 节点 ----------
    @GetMapping("/nodes")
    public List<MapNode> nodes() { return nodeRepo.findAll(); }

    @PostMapping("/nodes")
    public MapNode createNode(@RequestBody MapNode node) {
        if (node.getCode() == null || node.getCode().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "节点编码不能为空");
        if (nodeRepo.existsByCode(node.getCode()))
            throw new ApiException(HttpStatus.CONFLICT, "节点编码已存在: " + node.getCode());
        return nodeRepo.save(node);
    }

    @PutMapping("/nodes/{id}")
    public MapNode updateNode(@PathVariable Long id, @RequestBody MapNode body) {
        MapNode node = nodeRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "节点不存在"));
        node.setName(body.getName());
        node.setType(body.getType());
        node.setX(body.getX());
        node.setY(body.getY());
        node.setActive(body.getActive() != null ? body.getActive() : true);
        return nodeRepo.save(node);
    }

    @DeleteMapping("/nodes/{id}")
    public void deleteNode(@PathVariable Long id) {
        MapNode node = nodeRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "节点不存在"));
        boolean referenced = edgeRepo.findAll().stream().anyMatch(e ->
                e.getFromCode().equals(node.getCode()) || e.getToCode().equals(node.getCode()));
        if (referenced)
            throw new ApiException(HttpStatus.BAD_REQUEST, "该节点仍被道路引用，请先删除相关道路或停用节点。");
        nodeRepo.delete(node);
    }

    // ---------- 边 ----------
    @GetMapping("/edges")
    public List<MapEdge> edges() { return edgeRepo.findAll(); }

    @PostMapping("/edges")
    public MapEdge createEdge(@RequestBody MapEdge edge) {
        if (edge.getCode() == null || edge.getCode().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "道路编码不能为空");
        if (edgeRepo.existsByCode(edge.getCode()))
            throw new ApiException(HttpStatus.CONFLICT, "道路编码已存在: " + edge.getCode());
        validateEndpoints(edge.getFromCode(), edge.getToCode());
        normalizeFlags(edge);
        return edgeRepo.save(edge);
    }

    @PutMapping("/edges/{id}")
    public MapEdge updateEdge(@PathVariable Long id, @RequestBody MapEdge body) {
        MapEdge edge = edgeRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "道路不存在"));
        validateEndpoints(body.getFromCode(), body.getToCode());
        edge.setFromCode(body.getFromCode());
        edge.setToCode(body.getToCode());
        edge.setDistanceMeters(body.getDistanceMeters());
        edge.setWalkAllowed(body.getWalkAllowed());
        edge.setShuttleAllowed(body.getShuttleAllowed());
        edge.setTruckRoad(body.getTruckRoad());
        edge.setActive(body.getActive() != null ? body.getActive() : true);
        normalizeFlags(edge);
        return edgeRepo.save(edge);
    }

    @DeleteMapping("/edges/{id}")
    public void deleteEdge(@PathVariable Long id) {
        if (!edgeRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "道路不存在");
        edgeRepo.deleteById(id);
    }

    private void validateEndpoints(String from, String to) {
        if (from == null || to == null || from.isBlank() || to.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "道路必须指定两个端点");
        if (from.equals(to))
            throw new ApiException(HttpStatus.BAD_REQUEST, "道路两个端点不能相同");
        if (!nodeRepo.existsByCode(from) || !nodeRepo.existsByCode(to))
            throw new ApiException(HttpStatus.BAD_REQUEST, "道路端点节点不存在");
    }

    private void normalizeFlags(MapEdge edge) {
        if (edge.getDistanceMeters() == null || edge.getDistanceMeters() <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "道路长度必须为正数（米）");
        edge.setWalkAllowed(Boolean.TRUE.equals(edge.getWalkAllowed()));
        edge.setShuttleAllowed(Boolean.TRUE.equals(edge.getShuttleAllowed()));
        edge.setTruckRoad(Boolean.TRUE.equals(edge.getTruckRoad()));
        if (edge.getTruckRoad() && (edge.getWalkAllowed() || edge.getShuttleAllowed()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "货车通道不可同时允许步行或摆渡车通行");
        if (!edge.getWalkAllowed() && !edge.getShuttleAllowed() && !edge.getTruckRoad())
            throw new ApiException(HttpStatus.BAD_REQUEST, "道路至少需要一种通行方式");
    }

    // ---------- 临时封闭 ----------
    @GetMapping("/closures")
    public List<Closure> closures() { return closureRepo.findAll(); }

    @PostMapping("/closures")
    public Closure createClosure(@RequestBody Closure closure) {
        if (closure.getEdgeCode() == null || closure.getEdgeCode().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "必须指定封闭的道路");
        if (closure.getReason() == null || closure.getReason().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "封闭原因不能为空");
        if (!edgeRepo.existsByCode(closure.getEdgeCode()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "封闭的道路不存在: " + closure.getEdgeCode());
        if (closure.getEnabled() == null) closure.setEnabled(true);
        return closureRepo.save(closure);
    }

    @PutMapping("/closures/{id}")
    public Closure updateClosure(@PathVariable Long id, @RequestBody Closure body) {
        Closure closure = closureRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "封闭记录不存在"));
        closure.setEdgeCode(body.getEdgeCode());
        closure.setReason(body.getReason());
        closure.setEnabled(body.getEnabled() != null ? body.getEnabled() : true);
        closure.setStartAt(body.getStartAt());
        closure.setEndAt(body.getEndAt());
        return closureRepo.save(closure);
    }

    @DeleteMapping("/closures/{id}")
    public void deleteClosure(@PathVariable Long id) {
        if (!closureRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "封闭记录不存在");
        closureRepo.deleteById(id);
    }
}
