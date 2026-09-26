<template>
  <div class="panel admin-list">
    <div class="panel-header">
      <h2>🛠️ 管理员维护</h2>
      <p>维护节点、道路边与临时封闭信息，保存后访客路线实时生效</p>
    </div>
    <div class="panel-body">
      <div class="alert alert-info" style="margin-bottom:16px">
        当前：{{ nodes.length }} 个节点 · {{ edges.length }} 条道路 ·
        {{ activeClosures.length }} 段生效封闭
      </div>

      <!-- 节点 -->
      <section class="admin-section">
        <h3>
          节点管理
          <button class="btn btn-primary btn-sm" style="width:auto;padding:5px 12px"
                  @click="openNode(null)">＋ 新增节点</button>
        </h3>
        <div style="max-height:240px;overflow-y:auto">
          <table class="data">
            <thead><tr><th>编码</th><th>名称</th><th>类型</th><th>坐标</th><th>状态</th><th></th></tr></thead>
            <tbody>
              <tr v-for="n in nodes" :key="n.id">
                <td><code>{{ n.code }}</code></td>
                <td>{{ n.name }}</td>
                <td><span class="badge" :class="typeBadge(n.type)">{{ typeLabel(n.type) }}</span></td>
                <td>{{ n.x }},{{ n.y }}</td>
                <td>
                  <span class="badge" :class="n.active ? 'badge-green' : 'badge-gray'">
                    {{ n.active ? '启用' : '停用' }}
                  </span>
                </td>
                <td style="white-space:nowrap">
                  <button class="btn btn-ghost btn-sm" @click="openNode(n)">编辑</button>
                  <button class="btn btn-danger btn-sm" @click="removeNode(n)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 边 -->
      <section class="admin-section">
        <h3>
          道路（边）管理
          <button class="btn btn-primary btn-sm" style="width:auto;padding:5px 12px"
                  @click="openEdge(null)">＋ 新增道路</button>
        </h3>
        <div style="max-height:280px;overflow-y:auto">
          <table class="data">
            <thead><tr><th>编码</th><th>端点</th><th>长度</th><th>通行权限</th><th></th></tr></thead>
            <tbody>
              <tr v-for="e in edges" :key="e.id">
                <td><code>{{ e.code }}</code></td>
                <td>{{ nodeName(e.fromCode) }} ↔ {{ nodeName(e.toCode) }}</td>
                <td>{{ e.distanceMeters }}m</td>
                <td>
                  <span v-if="e.truckRoad" class="badge badge-amber">货车专用</span>
                  <span v-else>
                    <span v-if="e.walkAllowed" class="badge badge-green">步行</span>
                    <span v-if="e.shuttleAllowed" class="badge badge-blue" style="margin-left:3px">摆渡</span>
                  </span>
                  <span v-if="!e.active" class="badge badge-gray" style="margin-left:3px">停用</span>
                  <span v-if="closedSet.has(e.code)" class="badge badge-red" style="margin-left:3px">封闭中</span>
                </td>
                <td style="white-space:nowrap">
                  <button class="btn btn-ghost btn-sm" @click="openEdge(e)">编辑</button>
                  <button class="btn btn-danger btn-sm" @click="removeEdge(e)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 封闭信息 -->
      <section class="admin-section">
        <h3>
          临时封闭信息
          <button class="btn btn-primary btn-sm" style="width:auto;padding:5px 12px"
                  @click="openClosure(null)">＋ 新增封闭</button>
        </h3>
        <table class="data">
          <thead><tr><th>道路</th><th>原因</th><th>起止时间</th><th>状态</th><th></th></tr></thead>
          <tbody>
            <tr v-for="c in closures" :key="c.id">
              <td><code>{{ c.edgeCode }}</code></td>
              <td style="max-width:150px">{{ c.reason }}</td>
              <td style="font-size:11.5px;white-space:nowrap">
                {{ c.startAt ? fmt(c.startAt) : '即时' }}<br>～ {{ c.endAt ? fmt(c.endAt) : '长期' }}
              </td>
              <td><span class="badge" :class="c.enabled ? 'badge-red' : 'badge-gray'">
                {{ c.enabled ? '封闭中' : '已解除' }}
              </span></td>
              <td style="white-space:nowrap">
                <button class="btn btn-ghost btn-sm" @click="openClosure(c)">编辑</button>
                <button class="btn btn-danger btn-sm" @click="removeClosure(c)">删除</button>
              </td>
            </tr>
            <tr v-if="!closures.length"><td colspan="5" class="empty">暂无封闭信息</td></tr>
          </tbody>
        </table>
      </section>
    </div>

    <!-- 节点弹窗 -->
    <div class="modal-backdrop" v-if="nodeForm" @click.self="nodeForm = null">
      <div class="modal">
        <div class="modal-head">{{ nodeForm.id ? '编辑节点' : '新增节点' }}</div>
        <div class="modal-body">
          <div class="field"><label>编码（英文唯一标识）</label>
            <input v-model="nodeForm.code" :disabled="!!nodeForm.id" placeholder="如 GATE_N"/></div>
          <div class="field"><label>名称</label><input v-model="nodeForm.name" placeholder="如 北门岗"/></div>
          <div class="field"><label>类型</label>
            <select v-model="nodeForm.type">
              <option v-for="t in nodeTypes" :key="t.v" :value="t.v">{{ t.label }}</option>
            </select></div>
          <div style="display:flex;gap:10px">
            <div class="field" style="flex:1"><label>X 坐标</label><input type="number" v-model.number="nodeForm.x"/></div>
            <div class="field" style="flex:1"><label>Y 坐标</label><input type="number" v-model.number="nodeForm.y"/></div>
          </div>
          <label class="checkline"><input type="checkbox" v-model="nodeForm.active"/> 启用该节点</label>
        </div>
        <div class="modal-foot">
          <button class="btn btn-ghost" @click="nodeForm = null">取消</button>
          <button class="btn btn-primary" style="width:auto" @click="saveNode">保存</button>
        </div>
      </div>
    </div>

    <!-- 边弹窗 -->
    <div class="modal-backdrop" v-if="edgeForm" @click.self="edgeForm = null">
      <div class="modal">
        <div class="modal-head">{{ edgeForm.id ? '编辑道路' : '新增道路' }}</div>
        <div class="modal-body">
          <div class="field"><label>编码</label>
            <input v-model="edgeForm.code" :disabled="!!edgeForm.id" placeholder="如 E23"/></div>
          <div style="display:flex;gap:10px">
            <div class="field" style="flex:1"><label>端点 A</label>
              <select v-model="edgeForm.fromCode">
                <option v-for="n in nodes" :key="n.code" :value="n.code">{{ n.name }}</option>
              </select></div>
            <div class="field" style="flex:1"><label>端点 B</label>
              <select v-model="edgeForm.toCode">
                <option v-for="n in nodes" :key="n.code" :value="n.code">{{ n.name }}</option>
              </select></div>
          </div>
          <div class="field"><label>长度（米）</label>
            <input type="number" min="1" v-model.number="edgeForm.distanceMeters"/></div>
          <label class="checkline"><input type="checkbox" v-model="edgeForm.walkAllowed"
                  :disabled="edgeForm.truckRoad"/> 允许步行</label>
          <label class="checkline"><input type="checkbox" v-model="edgeForm.shuttleAllowed"
                  :disabled="edgeForm.truckRoad"/> 允许摆渡车</label>
          <label class="checkline"><input type="checkbox" v-model="edgeForm.truckRoad"/> 货车专用通道（访客路线避开）</label>
          <label class="checkline"><input type="checkbox" v-model="edgeForm.active"/> 道路启用</label>
        </div>
        <div class="modal-foot">
          <button class="btn btn-ghost" @click="edgeForm = null">取消</button>
          <button class="btn btn-primary" style="width:auto" @click="saveEdge">保存</button>
        </div>
      </div>
    </div>

    <!-- 封闭弹窗 -->
    <div class="modal-backdrop" v-if="closureForm" @click.self="closureForm = null">
      <div class="modal">
        <div class="modal-head">{{ closureForm.id ? '编辑封闭信息' : '新增临时封闭' }}</div>
        <div class="modal-body">
          <div class="field"><label>封闭道路</label>
            <select v-model="closureForm.edgeCode">
              <option v-for="e in edges" :key="e.code" :value="e.code">
                {{ e.code }} · {{ nodeName(e.fromCode) }} ↔ {{ nodeName(e.toCode) }}
              </option>
            </select></div>
          <div class="field"><label>封闭原因</label>
            <textarea rows="2" v-model="closureForm.reason" placeholder="如 燃气管道抢修"></textarea></div>
          <div class="field"><label>开始时间（可选，留空为即时）</label>
            <input type="datetime-local" v-model="closureForm.startAt"/></div>
          <div class="field"><label>结束时间（可选，留空为长期）</label>
            <input type="datetime-local" v-model="closureForm.endAt"/></div>
          <label class="checkline"><input type="checkbox" v-model="closureForm.enabled"/> 当前生效</label>
        </div>
        <div class="modal-foot">
          <button class="btn btn-ghost" @click="closureForm = null">取消</button>
          <button class="btn btn-primary" style="width:auto" @click="saveClosure">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { api } from '../api/client.js'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  closures: { type: Array, default: () => [] }
})
const emit = defineEmits(['changed', 'toast'])

const nodeTypes = [
  { v: 'GATE', label: '门岗' },
  { v: 'BUILDING', label: '办公楼' },
  { v: 'WORKSHOP', label: '样板车间' },
  { v: 'CONFERENCE', label: '会议中心' },
  { v: 'STOP', label: '摆渡车站' },
  { v: 'JUNCTION', label: '道路路口' },
  { v: 'CONSTRUCTION', label: '施工区域' },
  { v: 'LOGISTICS', label: '物流/货运区' }
]

const nodeForm = ref(null)
const edgeForm = ref(null)
const closureForm = ref(null)

const nodeMap = computed(() => Object.fromEntries(props.nodes.map(n => [n.code, n])))
const closedSet = computed(() => {
  const now = Date.now()
  const set = new Set()
  for (const c of props.closures) {
    if (!c.enabled) continue
    if (c.startAt && now < new Date(c.startAt).getTime()) continue
    if (c.endAt && now > new Date(c.endAt).getTime()) continue
    set.add(c.edgeCode)
  }
  return set
})
const activeClosures = computed(() => props.closures.filter(c => closedSet.value.has(c.edgeCode)))

function nodeName(code) { return nodeMap.value[code]?.name || code }
function typeLabel(t) { return nodeTypes.find(x => x.v === t)?.label || t }
function typeBadge(t) {
  return { GATE: 'badge-blue', BUILDING: 'badge-green', WORKSHOP: 'badge-amber',
    CONFERENCE: 'badge-blue', STOP: 'badge-green', CONSTRUCTION: 'badge-red',
    LOGISTICS: 'badge-amber', JUNCTION: 'badge-gray' }[t] || 'badge-gray'
}
function fmt(t) { return (t || '').replace('T', ' ').slice(0, 16) }

function toast(msg, kind = 'success') { emit('toast', { msg, kind }) }

// ---------- 节点 ----------
function openNode(n) {
  nodeForm.value = n
    ? { ...n }
    : { code: '', name: '', type: 'JUNCTION', x: 500, y: 360, active: true }
}
async function saveNode() {
  try {
    const f = nodeForm.value
    if (f.id) await api.updateNode(f.id, f)
    else await api.createNode(f)
    nodeForm.value = null
    emit('changed')
    toast('节点已保存')
  } catch (e) { toast(e.message, 'error') }
}
async function removeNode(n) {
  if (!confirm(`确认删除节点「${n.name}」？`)) return
  try {
    await api.deleteNode(n.id)
    emit('changed')
    toast('节点已删除')
  } catch (e) { toast(e.message, 'error') }
}

// ---------- 边 ----------
function openEdge(e) {
  if (e) {
    edgeForm.value = { ...e }
  } else {
    edgeForm.value = {
      code: '', fromCode: props.nodes[0]?.code, toCode: props.nodes[1]?.code,
      distanceMeters: 100, walkAllowed: true, shuttleAllowed: false,
      truckRoad: false, active: true
    }
  }
}
async function saveEdge() {
  try {
    const f = edgeForm.value
    if (f.truckRoad) { f.walkAllowed = false; f.shuttleAllowed = false }
    if (f.id) await api.updateEdge(f.id, f)
    else await api.createEdge(f)
    edgeForm.value = null
    emit('changed')
    toast('道路已保存')
  } catch (e) { toast(e.message, 'error') }
}
async function removeEdge(e) {
  if (!confirm(`确认删除道路「${e.code}」？`)) return
  try {
    await api.deleteEdge(e.id)
    emit('changed')
    toast('道路已删除')
  } catch (e) { toast(e.message, 'error') }
}

// ---------- 封闭 ----------
function openClosure(c) {
  if (c) {
    closureForm.value = { ...c }
  } else {
    closureForm.value = {
      edgeCode: props.edges[0]?.code, reason: '', enabled: true,
      startAt: '', endAt: ''
    }
  }
}
async function saveClosure() {
  try {
    const f = { ...closureForm.value }
    f.startAt = f.startAt || null
    f.endAt = f.endAt || null
    if (!f.reason?.trim()) throw new Error('请填写封闭原因')
    if (f.id) await api.updateClosure(f.id, f)
    else await api.createClosure(f)
    closureForm.value = null
    emit('changed')
    toast('封闭信息已保存，访客路线将实时绕行')
  } catch (e) { toast(e.message, 'error') }
}
async function removeClosure(c) {
  if (!confirm('确认删除该封闭信息？删除后道路恢复通行。')) return
  try {
    await api.deleteClosure(c.id)
    emit('changed')
    toast('封闭信息已删除，道路恢复通行')
  } catch (e) { toast(e.message, 'error') }
}
</script>
