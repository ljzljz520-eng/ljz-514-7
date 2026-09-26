# 工业园访客路线系统

访客从门岗前往办公楼、样板车间或会议中心时，系统**自动避开货车通道和施工（临时封闭）区域**，
给出**步行**或**摆渡车**推荐路线；管理员可在管理端维护**节点、道路边和临时封闭信息**，
封闭信息保存后访客路线实时绕行。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.2、Spring Web、Spring Data JPA、Bean Validation、H2 内存库 |
| 前端 | Vue 3（Composition API）、Vite 5、原生 SVG 地图（无需地图密钥） |
| 算法 | 无向加权图 + Dijkstra（边权=道路长度米），按出行方式/封闭/货车通道动态过滤 |

## 目录结构

```
park-route/
├── backend/                     # Spring Boot 后端（端口 8080）
│   └── src/main/java/com/park/route/
│       ├── model/               # MapNode / MapEdge / Closure / 枚举
│       ├── repositories/        # JPA Repository
│       ├── service/             # RouteService：建图 + Dijkstra + 避让
│       ├── controller/          # 访客 API + 管理员 API
│       ├── dto/                 # 路线请求/响应记录
│       ├── exception/           # 统一异常处理
│       └── init/DataInitializer.java  # 园区示例数据
└── frontend/                    # Vue 3 前端（端口 5173，/api 代理到 8080）
    └── src/
        ├── App.vue              # 访客端 / 管理员端 双标签
        └── components/
            ├── MapView.vue      # SVG 园区地图（路线动画、施工/货区标注）
            ├── VisitorPanel.vue # 路线查询与分段指引
            └── AdminPanel.vue   # 节点/边/封闭 CRUD
```

## 快速开始

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
# 服务地址 http://localhost:8080
```

首次启动自动写入示例数据：3 个门岗、办公楼 A/B、样板车间、会议中心、5 个摆渡站、
东侧货运走廊（货车专用）、三期施工区，以及 4 条默认封闭信息。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
# 打开 http://localhost:5173
```

生产构建：`npm run build`（产物在 `frontend/dist`，Nginx 部署时将 `/api` 反代到 8080）。

## 路线规划规则

- **步行**：只走 `walkAllowed=true` 的道路；所有 `truckRoad=true` 的货车通道一律排除。
- **摆渡车**：只走 `shuttleAllowed=true` 的摆渡环路与站点联络道。
- **临时封闭**：`enabled=true` 且当前时间落在 `[startAt, endAt]`（空值视为不限）的封闭边
  对两种方式都排除；边仍显示在地图上（红色虚线）便于访客识别。
- 无向边双向通行；Dijkstra 以道路长度（米）为权。
- ETA：步行 80 m/min，摆渡车 240 m/min（含停靠）。
- 当封闭导致某目的地某种方式不可达时，返回 `found=false` 与提示（不会引导访客走货车道）。

示例：东门岗→样板车间，货运走廊近 115 米，但属货车通道，步行路线自动改走北侧访客环路。

## API 一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/graph` | 全量节点/边/封闭（绘图 + 管理端） |
| POST | `/api/route` | 路线规划，body：`{ "from","to","mode":"WALK|SHUTTLE" }` |
| GET/POST | `/api/admin/nodes` | 节点列表/新增 |
| PUT/DELETE | `/api/admin/nodes/{id}` | 修改/删除（被边引用时拒绝删除） |
| GET/POST | `/api/admin/edges` | 道路列表/新增（校验端点、通行方式互斥等） |
| PUT/DELETE | `/api/admin/edges/{id}` | 修改/删除 |
| GET/POST | `/api/admin/closures` | 封闭列表/新增 |
| PUT/DELETE | `/api/admin/closures/{id}` | 修改/删除（解除封闭后道路立即恢复） |

## 测试

```bash
cd backend && mvn test
```

覆盖：避货车通道、封闭边绕行、摆渡车仅走摆渡路网、全封闭不可达、未来封闭不生效。
