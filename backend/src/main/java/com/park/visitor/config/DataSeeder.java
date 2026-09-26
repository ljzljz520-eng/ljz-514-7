package com.park.visitor.config;

import com.park.visitor.model.Closure;
import com.park.visitor.model.EdgeType;
import com.park.visitor.model.NodeType;
import com.park.visitor.model.ParkEdge;
import com.park.visitor.model.ParkNode;
import com.park.visitor.store.GraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时初始化示例园区图：
 * 两个门岗、办公楼、样板车间、会议中心，步行路网 + 摆渡车环线 + 货车外环。
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final GraphStore store;

    public DataSeeder(GraphStore store) {
        this.store = store;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!store.allNodes().isEmpty()) {
            return;
        }

        // ---------------- 节点（坐标单位：米，原点为园区西北角） ----------------
        seedNode("gate-west", "西门岗", NodeType.GATE, 70, 320, true);
        seedNode("gate-north", "北门岗", NodeType.GATE, 480, 60, true);
        seedNode("office", "办公楼", NodeType.OFFICE, 760, 150, true);
        seedNode("workshop", "样板车间", NodeType.WORKSHOP, 800, 480, true);
        seedNode("conf-center", "会议中心", NodeType.CONFERENCE, 430, 540, true);

        seedNode("j1", "路口·西", NodeType.JUNCTION, 240, 320, false);
        seedNode("j2", "路口·西南", NodeType.JUNCTION, 240, 480, false);
        seedNode("j3", "路口·中心", NodeType.JUNCTION, 480, 320, false);
        seedNode("j4", "路口·东北", NodeType.JUNCTION, 620, 200, false);
        seedNode("j5", "路口·东南", NodeType.JUNCTION, 620, 420, false);
        seedNode("j6", "路口·中南", NodeType.JUNCTION, 430, 420, false);

        seedNode("s-west", "摆渡站·西门", NodeType.SHUTTLE_STOP, 120, 320, false);
        seedNode("s-office", "摆渡站·办公楼", NodeType.SHUTTLE_STOP, 700, 190, false);
        seedNode("s-conf", "摆渡站·会议中心", NodeType.SHUTTLE_STOP, 470, 500, false);
        seedNode("s-workshop", "摆渡站·车间", NodeType.SHUTTLE_STOP, 730, 450, false);

        seedNode("t1", "货运路口·西南", NodeType.FACILITY, 70, 600, false);
        seedNode("t2", "货运路口·东南", NodeType.FACILITY, 900, 600, false);
        seedNode("t3", "货运路口·东北", NodeType.FACILITY, 940, 100, false);

        // ---------------- 步行道 ----------------
        seedEdge("w01", "gate-west", "s-west", EdgeType.WALK);
        seedEdge("w02", "s-west", "j1", EdgeType.WALK);
        seedEdge("w03", "j1", "j3", EdgeType.WALK);
        seedEdge("w04", "j3", "j4", EdgeType.WALK);
        seedEdge("w05", "j4", "office", EdgeType.WALK);
        seedEdge("w06", "j3", "j6", EdgeType.WALK);
        seedEdge("w07", "j6", "conf-center", EdgeType.WALK);
        seedEdge("w08", "j6", "j5", EdgeType.WALK);
        seedEdge("w09", "j5", "workshop", EdgeType.WALK);
        seedEdge("w10", "j4", "j5", EdgeType.WALK);
        seedEdge("w11", "j1", "j2", EdgeType.WALK);
        seedEdge("w12", "j2", "j6", EdgeType.WALK);
        seedEdge("w13", "j2", "conf-center", EdgeType.WALK);
        seedEdge("w14", "gate-north", "j4", EdgeType.WALK);
        seedEdge("w15", "gate-north", "j3", EdgeType.WALK);
        seedEdge("w16", "s-office", "office", EdgeType.WALK);
        seedEdge("w17", "s-office", "j4", EdgeType.WALK);
        seedEdge("w18", "s-conf", "conf-center", EdgeType.WALK);
        seedEdge("w19", "s-conf", "j6", EdgeType.WALK);
        seedEdge("w20", "s-workshop", "workshop", EdgeType.WALK);
        seedEdge("w21", "s-workshop", "j5", EdgeType.WALK);

        // ---------------- 摆渡车环线 ----------------
        seedEdge("sh1", "s-west", "s-office", EdgeType.SHUTTLE);
        seedEdge("sh2", "s-office", "s-workshop", EdgeType.SHUTTLE);
        seedEdge("sh3", "s-workshop", "s-conf", EdgeType.SHUTTLE);
        seedEdge("sh4", "s-conf", "s-west", EdgeType.SHUTTLE);

        // ---------------- 货车通道（访客不可用） ----------------
        seedEdge("tk1", "gate-west", "t1", EdgeType.TRUCK);
        seedEdge("tk2", "t1", "t2", EdgeType.TRUCK);
        seedEdge("tk3", "t2", "t3", EdgeType.TRUCK);
        seedEdge("tk4", "t3", "gate-north", EdgeType.TRUCK);
        seedEdge("tk5", "t2", "workshop", EdgeType.TRUCK);

        // ---------------- 临时封闭示例 ----------------
        LocalDateTime now = LocalDateTime.now();
        store.saveClosure(new Closure("c1", Closure.TargetType.EDGE, "w04",
                "道路施工：地下管线改造", now.minusDays(1), now.plusDays(7), true));
        store.saveClosure(new Closure("c2", Closure.TargetType.EDGE, "w10",
                "路面养护（计划）", now.plusDays(3), now.plusDays(5), true));

        log.info("示例园区数据初始化完成：{}", store.stats());
    }

    private void seedNode(String id, String name, NodeType type, double x, double y, boolean dest) {
        store.saveNode(new ParkNode(id, name, type, x, y, dest));
    }

    private void seedEdge(String id, String from, String to, EdgeType type) {
        // distance=0 表示按节点坐标自动计算
        store.saveEdge(new ParkEdge(id, from, to, type, 0, true));
    }
}
