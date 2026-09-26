<template>
  <div class="page">
    <!-- 地图 -->
    <div class="map-wrap">
      <div class="map-toolbar">
        <span class="title">园区导览图</span>
        <div class="legend">
          <span><i class="route"></i>步行路线</span>
          <span><i class="shuttle"></i>摆渡车</span>
          <span><i class="walk"></i>步行道</span>
          <span><i class="truck"></i>货车通道（已避让）</span>
          <span><i class="closed"></i>施工封闭</span>
        </div>
      </div>
      <div class="map-svg-wrap">
        <ParkMap
          :nodes="nodes" :edges="edges" :active-closures="activeClosures"
          :segments="route?.segments ?? []"
          :selected-from="fromId" :selected-to="toId"
          clickable @node-click="onNodeClick" />
      </div>
    </div>

    <!-- 查询面板 -->
    <div class="side">
      <div v-if="activeClosures.length" class="closure-banner">
        <b>⚠️ 当前施工 / 管制通知（{{ activeClosures.length }}）</b>
        <div v-for="c in activeClosures" :key="c.id">
          · {{ c.reason }}（{{ targetName(c) }}，至 {{ fmtTime(c.endTime) }}）
        </div>
      </div>

      <div class="card">
        <h3><span class="dot"></span>路线查询</h3>
        <div class="field">
          <label>起点（点击地图节点可快速选择）</label>
          <select v-model="fromId">
            <option value="" disabled>请选择起点</option>
            <option v-for="n in destinations" :key="n.id" :value="n.id">{{ n.name }}</option>
          </select>
        </div>
        <div class="field">
          <label>目的地</label>
          <select v-model="toId">
            <option value="" disabled>请选择目的地</option>
            <option v-for="n in destinations" :key="n.id" :value="n.id">{{ n.name }}</option>
          </select>
        </div>
        <div class="field">
          <label>出行方式</label>
          <div class="mode-switch">
            <button :class="{ active: mode === 'WALK' }" @click="mode = 'WALK'">🚶 步行</button>
            <button :class="{ active: mode === 'SHUTTLE' }" @click="mode = 'SHUTTLE'">🚌 摆渡车</button>
          </div>
        </div>
        <button class="btn btn-primary" :disabled="!fromId || !toId || loading" @click="query">
          {{ loading ? '规划中…' : '规划路线' }}
        </button>
      </div>

      <div v-if="error" class="error-box">⚠️ {{ error }}</div>

      <div v-if="route && route.found" class="card">
        <h3><span class="dot" style="background: var(--green)"></span>推荐路线</h3>
        <div class="summary">
          <div class="stat">
            <div class="v">{{ fmtDist(route.totalDistanceMeters) }}</div>
            <div class="k">总距离</div>
          </div>
          <div class="stat">
            <div class="v">{{ fmtDur(route.totalDurationSeconds) }}</div>
            <div class="k">预计用时</div>
          </div>
          <div class="stat">
            <div class="v">{{ route.segments.length }}</div>
            <div class="k">行程段数</div>
          </div>
        </div>
        <div class="segments">
          <div v-for="(seg, i) in route.segments" :key="i"
               class="seg" :class="seg.type === 'SHUTTLE' ? 'shuttle' : 'walk'">
            <div class="rail">
              <div class="ico">{{ seg.type === 'SHUTTLE' ? '🚌' : '🚶' }}</div>
              <div v-if="i < route.segments.length - 1" class="line"></div>
            </div>
            <div class="body">
              <div class="text">{{ seg.instruction }}</div>
              <div class="meta">{{ fmtDist(seg.distanceMeters) }} · 约 {{ fmtDur(seg.durationSeconds) }}</div>
            </div>
          </div>
          <div v-if="!route.segments.length" class="empty-tip">{{ route.message }}</div>
        </div>
      </div>

      <div v-else-if="route && !route.found" class="error-box">⚠️ {{ route.message }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import ParkMap from '../components/ParkMap.vue'
import { api } from '../api'

const nodes = ref([])
const edges = ref([])
const activeClosures = ref([])

const fromId = ref('')
const toId = ref('')
const mode = ref('WALK')
const loading = ref(false)
const route = ref(null)
const error = ref('')

// 起终点可选：门岗 + 对访客开放的目的地
const destinations = computed(() =>
  nodes.value.filter(n => n.visitorDestination || n.type === 'GATE')
)

const nodeName = computed(() => new Map(nodes.value.map(n => [n.id, n.name])))
const edgeName = computed(() => {
  const m = new Map()
  for (const e of edges.value) {
    const a = nodeName.value.get(e.from) ?? e.from
    const b = nodeName.value.get(e.to) ?? e.to
    m.set(e.id, `${a} — ${b}`)
  }
  return m
})

function targetName(c) {
  return c.targetType === 'NODE'
    ? (nodeName.value.get(c.targetId) ?? c.targetId)
    : (edgeName.value.get(c.targetId) ?? c.targetId)
}

// 点击地图：先设起点，再设终点，之后替换终点
let pickCount = 0
function onNodeClick(n) {
  if (!destinations.value.some(d => d.id === n.id)) return
  if (pickCount % 2 === 0 || !fromId.value) {
    fromId.value = n.id
  } else {
    toId.value = n.id
  }
  pickCount++
}

async function loadMap() {
  const data = await api.getMap()
  nodes.value = data.nodes
  edges.value = data.edges
  activeClosures.value = data.activeClosures
}

async function query() {
  loading.value = true
  error.value = ''
  route.value = null
  try {
    route.value = await api.planRoute(fromId.value, toId.value, mode.value)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

const fmtDist = (m) => m >= 1000 ? `${(m / 1000).toFixed(2)} km` : `${Math.round(m)} m`
const fmtDur = (s) => {
  const min = Math.round(s / 60)
  return min >= 1 ? `${min} 分钟` : `${Math.round(s)} 秒`
}
const fmtTime = (iso) => iso ? iso.replace('T', ' ').slice(5, 16) : ''

onMounted(async () => {
  try {
    await loadMap()
    // 默认：西门岗 → 办公楼
    fromId.value = 'gate-west'
    toId.value = 'office'
    await query()
  } catch (e) {
    error.value = '无法连接后端服务：' + e.message
  }
})
</script>
