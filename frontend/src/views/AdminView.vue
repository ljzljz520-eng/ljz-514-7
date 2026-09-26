<template>
  <div class="page" style="flex-direction: column;">
    <div v-if="toast" class="toast" :class="toastType">{{ toast }}</div>

    <div class="admin-grid">
      <!-- 左：地图预览 + 数据表 -->
      <div class="admin-main">
        <div class="map-wrap" style="flex: none; height: 420px;">
          <div class="map-toolbar">
            <span class="title">园区图预览</span>
            <span style="font-size:12px;color:var(--ink-2)">
              {{ tab === 'nodes' ? '点击地图可拾取坐标' : '实时反映节点 / 道路 / 封闭变更' }}
            </span>
            <div class="legend">
              <span><i class="walk"></i>步行道</span>
              <span><i class="shuttle"></i>摆渡车道</span>
              <span><i class="truck"></i>货车通道</span>
              <span><i class="closed"></i>封闭中</span>
            </div>
          </div>
          <div class="map-svg-wrap">
            <ParkMap :nodes="nodes" :edges="edges" :active-closures="activeClosures"
                     clickable @node-click="onNodeClick" @map-click="onMapClick" />
          </div>
        </div>

        <div class="card" style="flex:1;">
          <div class="tabs">
            <button :class="{ active: tab === 'nodes' }" @click="tab = 'nodes'">节点（{{ nodes.length }}）</button>
            <button :class="{ active: tab === 'edges' }" @click="tab = 'edges'">道路（{{ edges.length }}）</button>
            <button :class="{ active: tab === 'closures' }" @click="tab = 'closures'">临时封闭（{{ closures.length }}）</button>
          </div>

          <!-- 节点表 -->
          <div v-if="tab === 'nodes'" class="table-scroll">
            <table class="data">
              <thead>
                <tr><th>ID</th><th>名称</th><th>类型</th><th>坐标</th><th>访客目的地</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="n in nodes" :key="n.id">
                  <td><code>{{ n.id }}</code></td>
                  <td>{{ n.name }}</td>
                  <td>{{ nodeTypeLabel(n.type) }}</td>
                  <td>({{ Math.round(n.x) }}, {{ Math.round(n.y) }})</td>
                  <td>{{ n.visitorDestination ? '✅' : '—' }}</td>
                  <td>
                    <button class="btn btn-sm btn-ghost" @click="editNode(n)">编辑</button>
                    <button class="btn btn-sm btn-danger" @click="removeNode(n)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 边表 -->
          <div v-if="tab === 'edges'" class="table-scroll">
            <table class="data">
              <thead>
                <tr><th>ID</th><th>起点</th><th>终点</th><th>类型</th><th>长度</th><th>双向</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="e in edges" :key="e.id">
                  <td><code>{{ e.id }}</code></td>
                  <td>{{ nameOf(e.from) }}</td>
                  <td>{{ nameOf(e.to) }}</td>
                  <td><span class="tag" :class="e.type.toLowerCase()">{{ edgeTypeLabel(e.type) }}</span></td>
                  <td>{{ e.distance > 0 ? e.distance + ' m' : '自动' }}</td>
                  <td>{{ e.bidirectional ? '是' : '否' }}</td>
                  <td>
                    <button class="btn btn-sm btn-ghost" @click="editEdge(e)">编辑</button>
                    <button class="btn btn-sm btn-danger" @click="removeEdge(e)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 封闭表 -->
          <div v-if="tab === 'closures'" class="table-scroll">
            <table class="data">
              <thead>
                <tr><th>ID</th><th>对象</th><th>原因</th><th>时间范围</th><th>状态</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="c in closures" :key="c.id">
                  <td><code>{{ c.id }}</code></td>
                  <td>
                    <span class="tag" :class="c.targetType.toLowerCase()">{{ c.targetType === 'NODE' ? '节点' : '道路' }}</span>
                    {{ targetName(c) }}
                  </td>
                  <td>{{ c.reason }}</td>
                  <td style="white-space:nowrap">{{ fmtTime(c.startTime) }} ~ {{ fmtTime(c.endTime) }}</td>
                  <td>
                    <span class="tag" :class="closureStatus(c).cls">{{ closureStatus(c).text }}</span>
                  </td>
                  <td>
                    <button class="btn btn-sm btn-ghost" @click="editClosure(c)">编辑</button>
                    <button class="btn btn-sm btn-danger" @click="removeClosure(c)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 右：编辑表单 -->
      <div class="admin-form">
        <!-- 节点表单 -->
        <div v-if="tab === 'nodes'" class="card">
          <h3><span class="dot"></span>{{ nodeForm._editing ? '编辑节点' : '新增节点' }}</h3>
          <div class="field">
            <label>节点 ID（唯一，字母/数字/连字符）</label>
            <input v-model.trim="nodeForm.id" :disabled="nodeForm._editing" placeholder="如 j7" />
          </div>
          <div class="field">
            <label>名称</label>
            <input v-model.trim="nodeForm.name" placeholder="如 路口·东" />
          </div>
          <div class="field">
            <label>类型</label>
            <select v-model="nodeForm.type">
              <option v-for="t in nodeTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>
          <div class="field-row">
            <div class="field">
              <label>X 坐标（米）</label>
              <input v-model.number="nodeForm.x" type="number" />
            </div>
            <div class="field">
              <label>Y 坐标（米）</label>
              <input v-model.number="nodeForm.y" type="number" />
            </div>
          </div>
          <div class="field">
            <label style="display:flex;align-items:center;gap:8px;color:var(--ink)">
              <input type="checkbox" v-model="nodeForm.visitorDestination" style="width:auto" />
              作为访客可选目的地（门岗 / 办公楼等）
            </label>
          </div>
          <button class="btn btn-primary" @click="saveNode">{{ nodeForm._editing ? '保存修改' : '创建节点' }}</button>
          <button v-if="nodeForm._editing" class="btn btn-ghost" style="width:100%;margin-top:8px" @click="resetNodeForm">取消编辑</button>
        </div>

        <!-- 边表单 -->
        <div v-if="tab === 'edges'" class="card">
          <h3><span class="dot"></span>{{ edgeForm._editing ? '编辑道路' : '新增道路' }}</h3>
          <div class="field">
            <label>道路 ID</label>
            <input v-model.trim="edgeForm.id" :disabled="edgeForm._editing" placeholder="如 w22" />
          </div>
          <div class="field-row">
            <div class="field">
              <label>起点节点</label>
              <select v-model="edgeForm.from">
                <option value="" disabled>选择</option>
                <option v-for="n in nodes" :key="n.id" :value="n.id">{{ n.name }}</option>
              </select>
            </div>
            <div class="field">
              <label>终点节点</label>
              <select v-model="edgeForm.to">
                <option value="" disabled>选择</option>
                <option v-for="n in nodes" :key="n.id" :value="n.id">{{ n.name }}</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label>道路类型</label>
            <select v-model="edgeForm.type">
              <option value="WALK">步行道</option>
              <option value="SHUTTLE">摆渡车道</option>
              <option value="TRUCK">货车通道（访客不可见）</option>
            </select>
          </div>
          <div class="field">
            <label>长度（米，0 = 按坐标自动计算）</label>
            <input v-model.number="edgeForm.distance" type="number" min="0" />
          </div>
          <div class="field">
            <label style="display:flex;align-items:center;gap:8px;color:var(--ink)">
              <input type="checkbox" v-model="edgeForm.bidirectional" style="width:auto" />
              双向通行
            </label>
          </div>
          <button class="btn btn-primary" @click="saveEdge">{{ edgeForm._editing ? '保存修改' : '创建道路' }}</button>
          <button v-if="edgeForm._editing" class="btn btn-ghost" style="width:100%;margin-top:8px" @click="resetEdgeForm">取消编辑</button>
        </div>

        <!-- 封闭表单 -->
        <div v-if="tab === 'closures'" class="card">
          <h3><span class="dot" style="background:var(--red)"></span>{{ closureForm._editing ? '编辑封闭' : '新增临时封闭' }}</h3>
          <div class="field">
            <label>封闭 ID</label>
            <input v-model.trim="closureForm.id" :disabled="closureForm._editing" placeholder="如 c3" />
          </div>
          <div class="field">
            <label>封闭对象类型</label>
            <select v-model="closureForm.targetType">
              <option value="NODE">节点（施工区域）</option>
              <option value="EDGE">道路</option>
            </select>
          </div>
          <div class="field">
            <label>封闭对象</label>
            <select v-model="closureForm.targetId">
              <option value="" disabled>选择</option>
              <option v-if="closureForm.targetType === 'NODE'" v-for="n in nodes" :key="n.id" :value="n.id">{{ n.name }}</option>
              <option v-if="closureForm.targetType === 'EDGE'" v-for="e in edges" :key="e.id" :value="e.id">
                {{ e.id }}：{{ nameOf(e.from) }} — {{ nameOf(e.to) }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>封闭原因</label>
            <input v-model.trim="closureForm.reason" placeholder="如 道路施工：管线改造" />
          </div>
          <div class="field">
            <label>开始时间</label>
            <input v-model="closureForm.startTime" type="datetime-local" />
          </div>
          <div class="field">
            <label>结束时间</label>
            <input v-model="closureForm.endTime" type="datetime-local" />
          </div>
          <div class="field">
            <label style="display:flex;align-items:center;gap:8px;color:var(--ink)">
              <input type="checkbox" v-model="closureForm.enabled" style="width:auto" />
              启用（停用后不参与路线计算）
            </label>
          </div>
          <button class="btn btn-primary" @click="saveClosure">{{ closureForm._editing ? '保存修改' : '创建封闭' }}</button>
          <button v-if="closureForm._editing" class="btn btn-ghost" style="width:100%;margin-top:8px" @click="resetClosureForm">取消编辑</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import ParkMap from '../components/ParkMap.vue'
import { api } from '../api'

const tab = ref('nodes')
const nodes = ref([])
const edges = ref([])
const closures = ref([])
const toast = ref('')
const toastType = ref('ok')

const nodeTypes = [
  { value: 'GATE', label: '门岗' },
  { value: 'OFFICE', label: '办公楼' },
  { value: 'WORKSHOP', label: '样板车间' },
  { value: 'CONFERENCE', label: '会议中心' },
  { value: 'JUNCTION', label: '路口' },
  { value: 'SHUTTLE_STOP', label: '摆渡车站' },
  { value: 'FACILITY', label: '其他设施' }
]

const blankNode = () => ({ id: '', name: '', type: 'JUNCTION', x: 0, y: 0, visitorDestination: false, _editing: false })
const blankEdge = () => ({ id: '', from: '', to: '', type: 'WALK', distance: 0, bidirectional: true, _editing: false })
const blankClosure = () => {
  const now = new Date()
  const pad = (v) => String(v).padStart(2, '0')
  const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
  const end = new Date(now.getTime() + 24 * 3600 * 1000)
  return { id: '', targetType: 'EDGE', targetId: '', reason: '', startTime: fmt(now), endTime: fmt(end), enabled: true, _editing: false }
}

const nodeForm = ref(blankNode())
const edgeForm = ref(blankEdge())
const closureForm = ref(blankClosure())

const nameMap = computed(() => new Map(nodes.value.map(n => [n.id, n.name])))
const nameOf = (id) => nameMap.value.get(id) ?? id

const activeClosures = computed(() => {
  const now = new Date()
  return closures.value.filter(c =>
    c.enabled && new Date(c.startTime) <= now && now <= new Date(c.endTime))
})

function targetName(c) {
  if (c.targetType === 'NODE') return nameOf(c.targetId)
  const e = edges.value.find(x => x.id === c.targetId)
  return e ? `${nameOf(e.from)} — ${nameOf(e.to)}` : c.targetId
}

function closureStatus(c) {
  if (!c.enabled) return { text: '已停用', cls: 'inactive' }
  const now = new Date()
  if (now < new Date(c.startTime)) return { text: '未开始', cls: 'inactive' }
  if (now > new Date(c.endTime)) return { text: '已结束', cls: 'inactive' }
  return { text: '生效中', cls: 'active' }
}

const nodeTypeLabel = (t) => nodeTypes.find(x => x.value === t)?.label ?? t
const edgeTypeLabel = (t) => ({ WALK: '步行道', SHUTTLE: '摆渡车道', TRUCK: '货车通道' }[t] ?? t)
const fmtTime = (iso) => iso ? iso.replace('T', ' ').slice(0, 16) : ''

function showToast(msg, ok = true) {
  toast.value = msg
  toastType.value = ok ? 'ok' : 'err'
  setTimeout(() => (toast.value = ''), 2600)
}

async function refresh() {
  const data = await api.getMap()
  nodes.value = data.nodes
  edges.value = data.edges
  closures.value = await api.listClosures()
}

// ---------- 节点 ----------
function onMapClick(p) {
  if (tab.value !== 'nodes') return
  nodeForm.value.x = p.x
  nodeForm.value.y = p.y
}
function onNodeClick(n) {
  if (tab.value === 'nodes') editNode(n)
}
function editNode(n) {
  nodeForm.value = { ...n, _editing: true }
}
function resetNodeForm() { nodeForm.value = blankNode() }
async function saveNode() {
  const f = nodeForm.value
  const payload = { id: f.id, name: f.name, type: f.type, x: f.x, y: f.y, visitorDestination: f.visitorDestination }
  try {
    if (f._editing) {
      await api.updateNode(f.id, payload)
      showToast('节点已更新')
    } else {
      await api.createNode(payload)
      showToast('节点已创建')
    }
    resetNodeForm()
    await refresh()
  } catch (e) { showToast(e.message, false) }
}
async function removeNode(n) {
  if (!confirm(`删除节点「${n.name}」？关联的道路与封闭将一并删除。`)) return
  try {
    await api.deleteNode(n.id)
    showToast('节点已删除')
    await refresh()
  } catch (e) { showToast(e.message, false) }
}

// ---------- 边 ----------
function editEdge(e) { edgeForm.value = { ...e, _editing: true } }
function resetEdgeForm() { edgeForm.value = blankEdge() }
async function saveEdge() {
  const f = edgeForm.value
  const payload = { id: f.id, from: f.from, to: f.to, type: f.type, distance: f.distance, bidirectional: f.bidirectional }
  try {
    if (f._editing) {
      await api.updateEdge(f.id, payload)
      showToast('道路已更新')
    } else {
      await api.createEdge(payload)
      showToast('道路已创建')
    }
    resetEdgeForm()
    await refresh()
  } catch (e) { showToast(e.message, false) }
}
async function removeEdge(e) {
  if (!confirm(`删除道路 ${e.id}（${nameOf(e.from)} — ${nameOf(e.to)}）？`)) return
  try {
    await api.deleteEdge(e.id)
    showToast('道路已删除')
    await refresh()
  } catch (e) { showToast(e.message, false) }
}

// ---------- 封闭 ----------
function editClosure(c) {
  closureForm.value = { ...c, startTime: c.startTime.slice(0, 16), endTime: c.endTime.slice(0, 16), _editing: true }
}
function resetClosureForm() { closureForm.value = blankClosure() }
async function saveClosure() {
  const f = closureForm.value
  const payload = {
    id: f.id, targetType: f.targetType, targetId: f.targetId, reason: f.reason,
    startTime: f.startTime, endTime: f.endTime, enabled: f.enabled
  }
  try {
    if (f._editing) {
      await api.updateClosure(f.id, payload)
      showToast('封闭已更新')
    } else {
      await api.createClosure(payload)
      showToast('封闭已创建')
    }
    resetClosureForm()
    await refresh()
  } catch (e) { showToast(e.message, false) }
}
async function removeClosure(c) {
  if (!confirm(`删除封闭记录 ${c.id}？`)) return
  try {
    await api.deleteClosure(c.id)
    showToast('封闭已删除')
    await refresh()
  } catch (e) { showToast(e.message, false) }
}

onMounted(async () => {
  try {
    await refresh()
  } catch (e) {
    showToast('无法连接后端服务：' + e.message, false)
  }
})
</script>
