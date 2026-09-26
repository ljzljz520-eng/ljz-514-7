package com.park.route.init;

import com.park.route.model.*;
import com.park.route.repositories.ClosureRepository;
import com.park.route.repositories.MapEdgeRepository;
import com.park.route.repositories.MapNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 初始化工业园示例地图：
 *  - 3 个门岗、办公楼A/B、样板车间、会议中心
 *  - 5 个摆渡车站点构成摆渡环路
 *  - 东侧货运走廊（货车专用，访客禁行）
 *  - 西南施工区 + 西北环路施工封闭
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final MapNodeRepository nodeRepo;
    private final MapEdgeRepository edgeRepo;
    private final ClosureRepository closureRepo;

    public DataInitializer(MapNodeRepository nodeRepo, MapEdgeRepository edgeRepo,
                           ClosureRepository closureRepo) {
        this.nodeRepo = nodeRepo;
        this.edgeRepo = edgeRepo;
        this.closureRepo = closureRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (nodeRepo.count() > 0) {
            return;
        }

        // ---------------- 节点 ----------------
        node(n("GATE_W",  "西门岗",        NodeType.GATE,         70, 300));
        node(n("GATE_S",  "南门岗（主入口）", NodeType.GATE,       500, 650));
        node(n("GATE_E",  "东门岗",        NodeType.GATE,        930, 300));

        node(n("STOP_W",  "西区摆渡站",     NodeType.STOP,        230, 300));
        node(n("STOP_NW", "西北摆渡站",     NodeType.STOP,        320, 170));
        node(n("STOP_C",  "中心广场摆渡站",  NodeType.STOP,        500, 320));
        node(n("STOP_NE", "东北摆渡站",     NodeType.STOP,        680, 170));
        node(n("STOP_E",  "东区摆渡站",     NodeType.STOP,        770, 300));

        node(n("OFFICE_A",   "办公楼A座",   NodeType.BUILDING,    400, 100));
        node(n("OFFICE_B",   "办公楼B座",   NodeType.BUILDING,    600, 100));
        node(n("WORKSHOP",   "样板车间",    NodeType.WORKSHOP,    880, 240));
        node(n("CONFERENCE", "会议中心",    NodeType.CONFERENCE,  870, 530));

        node(n("CONS",       "三期施工区",  NodeType.CONSTRUCTION, 350, 460));
        node(n("LOGISTICS",  "物流仓储中心", NodeType.LOGISTICS,   830, 460));
        node(n("TRUCK_GATE", "货运门",      NodeType.GATE,        940, 600));

        // ---------------- 道路 ----------------
        // 门岗 → 站点（步行/摆渡）
        edge(e("E01", "GATE_W", "STOP_W",  160, true,  true,  false));
        edge(e("E02", "GATE_S", "STOP_C",  330, true,  true,  false));
        edge(e("E03", "GATE_E", "STOP_E",  160, true,  true,  false));

        // 摆渡环路（步行/摆渡）
        edge(e("E04", "STOP_W", "STOP_NW", 160, true,  true,  false));
        edge(e("E05", "STOP_NW","STOP_C",  230, true,  true,  false));
        edge(e("E06", "STOP_C", "STOP_NE", 230, true,  true,  false));
        edge(e("E07", "STOP_NE","STOP_E",  160, true,  true,  false));
        edge(e("E08", "STOP_W", "STOP_C",  270, true,  true,  false));
        edge(e("E09", "STOP_C", "STOP_E",  270, true,  true,  false));

        // 目的地联络道（步行/摆渡）
        edge(e("E10", "STOP_NW","OFFICE_A",   110, true, true, false));
        edge(e("E11", "STOP_NE","OFFICE_B",   110, true, true, false));
        edge(e("E12", "STOP_NE","WORKSHOP",   210, true, true, false));
        edge(e("E13", "STOP_E", "CONFERENCE", 250, true, true, false));

        // 步行便道（步行专用，访客抄近路）
        edge(e("E14", "OFFICE_A","OFFICE_B",  200, true, false, false));
        edge(e("E18", "OFFICE_B","WORKSHOP",  315, true, false, false));
        edge(e("E15", "STOP_C", "CONS",       206, true, false, false));
        edge(e("E16", "CONS",   "STOP_W",     200, true, false, false));
        edge(e("E17", "CONS",   "GATE_S",     242, true, false, false));

        // 东侧货运走廊（货车专用，访客路线一律避开）
        edge(e("E19", "GATE_E",    "LOGISTICS",   189, false, false, true));
        edge(e("E20", "LOGISTICS", "TRUCK_GATE",  178, false, false, true));
        edge(e("E21", "LOGISTICS", "CONFERENCE",   81, false, false, true));
        edge(e("E22", "LOGISTICS", "WORKSHOP",    226, false, false, true));

        // ---------------- 默认封闭信息 ----------------
        closureRepo.save(new Closure("E05", "西北环路路面改造施工，车辆与行人请绕行中心广场",
                true, LocalDateTime.of(2026, 9, 24, 0, 0),
                LocalDateTime.of(2026, 10, 10, 23, 59)));
        closureRepo.save(new Closure("E15", "三期施工区围挡封闭，禁止通行",
                true, LocalDateTime.of(2026, 9, 1, 0, 0), null));
        closureRepo.save(new Closure("E16", "三期施工区围挡封闭，禁止通行",
                true, LocalDateTime.of(2026, 9, 1, 0, 0), null));
        closureRepo.save(new Closure("E17", "三期施工区围挡封闭，禁止通行",
                true, LocalDateTime.of(2026, 9, 1, 0, 0), null));

        log.info("工业园示例数据初始化完成：{} 个节点，{} 条道路，{} 条封闭信息",
                nodeRepo.count(), edgeRepo.count(), closureRepo.count());
    }

    private static MapNode n(String code, String name, NodeType type, int x, int y) {
        return new MapNode(code, name, type, x, y);
    }

    private static MapEdge e(String code, String from, String to, int meters,
                             boolean walk, boolean shuttle, boolean truck) {
        return new MapEdge(code, from, to, meters, walk, shuttle, truck);
    }

    private void node(MapNode n) { nodeRepo.save(n); }
    private void edge(MapEdge e) { edgeRepo.save(e); }
}
