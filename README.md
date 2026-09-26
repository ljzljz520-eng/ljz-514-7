# 工业园访客路线系统

访客从门岗前往办公楼、样板车间或会议中心时，系统自动**避开货车通道与施工封闭区域**，
给出**步行**或**摆渡车**最优路线；管理员可维护园区节点、道路与临时封闭信息。

## 系统架构

```
┌────────────────────────────┐      HTTP/JSON       ┌─────────────────────────────┐
│  前端 Vue 3 + Vite          │  ─────────────────▶  │  后端 Spring Boot 3 (Java 17) │
│  · 访客导航页（SVG 园区图） │  /api/map            │  · MapController   访客 API   │
│  · 管理页（节点/边/封闭）   │  /api/route          │  · AdminController 管理 API   │
│                            │  /api/admin/**       │  · RouteService   路径规划    │
│                            │                      │  · GraphStore     内存图存储  │
└────────────────────────────┘                      └─────────────────────────────┘
```

## 核心规则

| 规则 | 说明 |
| --- | --- |
| 货车通道避让 | `TRUCK` 类型边对访客永远不可达 |
| 施工区域避让 | 生效中的 `Closure`（节点/边，按时间窗）从图中剔除 |
| 步行模式 | 仅使用步行道，按距离/步行速度计时的最短路 |
| 摆渡车模式 | 步行道 + 摆渡车道混合图，按**总耗时**最优；每次上车计 120s 候车，用 `(节点, 是否在车上)` 状态扩展 Dijkstra 精确建模 |
| 结果展示 | 路线按道路类型合并为「步行段 / 摆渡段」，含中文导航说明、距离与预计用时 |

## API 一览

**访客**
- `GET  /api/map` — 园区图（节点 + 边 + 当前生效封闭）
- `POST /api/route` — 路线规划，请求体 `{"fromId","toId","mode":"WALK"|"SHUTTLE"}`

**管理**
- `GET/POST/PUT/DELETE /api/admin/nodes[/{id}]` — 节点维护（删除节点级联删除关联边与封闭）
- `GET/POST/PUT/DELETE /api/admin/edges[/{id}]` — 道路维护（校验端点存在、禁止自环）
- `GET/POST/PUT/DELETE /api/admin/closures[/{id}]` — 临时封闭维护（校验时间窗与对象存在）

## 快速开始

```bash
# 后端（端口 8080）
cd backend && mvn spring-boot:run
# 或：mvn package -DskipTests && java -jar target/visitor-route-system-1.0.0.jar

# 前端（端口 5173，/api 自动代理到 8080）
cd frontend && npm install && npm run dev
```

访问 http://localhost:5173/ （访客导航）与 http://localhost:5173/admin （园区图管理）。

## 测试

```bash
cd backend && mvn test     # 8 个用例：货车避让、施工绕行、摆渡接驳、封闭生效/过期等
```

## 内置示例数据

启动时自动初始化：2 个门岗、办公楼、样板车间、会议中心、6 个路口、4 个摆渡站、
21 条步行道、4 条摆渡车道（环线）、5 条货车通道（外环），以及一条生效中的施工封闭
（`w04` 中心路口—东北路口，步行路线会自动绕行）。

> 数据保存在内存中，重启后重置为示例数据；生产环境可将 `GraphStore` 替换为数据库实现。
