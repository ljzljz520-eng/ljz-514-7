<template>
  <div class="panel">
    <div class="panel-header">
      <h2>🚶 访客路线指引</h2>
      <p>选择门岗与目的地，系统自动避开货车通道和施工区域</p>
    </div>
    <div class="panel-body">
      <div class="field">
        <label>出行方式</label>
        <div class="seg">
          <button :class="{ 'active-walk': mode === 'WALK' }" @click="mode = 'WALK'">🚶 步行</button>
          <button :class="{ 'active-shuttle': mode === 'SHUTTLE' }" @click="mode = 'SHUTTLE'">🚐 摆渡车</button>
        </div>
      </div>

      <div class="field">
        <label>出发门岗</label>
        <select v-model="from">
          <option v-for="g in gates" :key="g.code" :value="g.code">{{ g.name }}</option>
        </select>
      </div>

      <div class="field">
        <label>目的地</label>
        <select v-model="to">
          <option v-for="d in destinations" :key="d.code" :value="d.code">
            {{ d.name }}（{{ typeLabel(d.type) }}）
          </option>
        </select>
      </div>

      <div style="display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap">
        <button class="btn btn-ghost btn-sm" v-for="q in quick" :key="q.label"
                @click="applyQuick(q)">⚡ {{ q.label }}</button>
      </div>

      <button class="btn btn-primary" @click="plan" :disabled="loading">
        <span v-if="loading" class="spin"></span>
        {{ loading ? '正在规划…' : '开始规划路线' }}
      </button>

      <div v-if="error" class="alert alert-error" style="margin-top:14px">{{ error }}</div>

      <template v-if="route">
        <div v-if="!route.found" class="alert alert-error" style="margin-top:14px">
          ⚠️ {{ route.message }}
        </div>

        <template v-else>
          <div class="route-summary" style="margin-top:16px">
            <div class="stat">
              <div class="k">总路程</div>
              <div class="v">{{ route.totalMeters }}<span class="unit"> 米</span></div>
            </div>
            <div class="stat">
              <div class="k">预计用时</div>
              <div class="v">{{ route.estimatedMinutes }}<span class="unit"> 分钟</span></div>
            </div>
          </div>

          <div v-for="(w, i) in route.warnings" :key="i" class="alert"
               :class="i === 0 ? 'alert-success' : 'alert-warn'">
            {{ w }}
          </div>

          <h3 style="font-size:13.5px;margin:16px 0 8px">🧭 分段指引</h3>
          <ol class="steps">
            <li v-for="(s, i) in route.steps" :key="i">
              <div><strong>{{ s.instruction }}</strong></div>
              <div class="meta">
                {{ s.fromName }} → {{ s.toName }} · {{ s.distanceMeters }} 米
                <span v-if="s.shuttleRoad" class="badge badge-blue" style="margin-left:4px">摆渡道路</span>
              </div>
            </li>
          </ol>
        </template>
      </template>

      <div v-else-if="!loading" class="empty" style="margin-top:20px">
        地图上点击节点也可快速选择
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { api } from '../api/client.js'

const props = defineProps({
  nodes: { type: Array, default: () => [] }
})
const emit = defineEmits(['planned'])

const mode = ref('WALK')
const from = ref('GATE_S')
const to = ref('OFFICE_A')
const route = ref(null)
const loading = ref(false)
const error = ref('')

const gates = computed(() => props.nodes.filter(n => n.type === 'GATE' && n.active))
const destinations = computed(() => props.nodes.filter(n =>
    ['BUILDING', 'WORKSHOP', 'CONFERENCE'].includes(n.type) && n.active))

watch(gates, list => {
  if (!list.find(g => g.code === from.value) && list[0]) from.value = list[0].code
}, { immediate: true })
watch(destinations, list => {
  if (!list.find(d => d.code === to.value) && list[0]) to.value = list[0].code
}, { immediate: true })

const quick = [
  { label: '南门→办公楼A', from: 'GATE_S', to: 'OFFICE_A', mode: 'WALK' },
  { label: '东门→样板车间', from: 'GATE_E', to: 'WORKSHOP', mode: 'WALK' },
  { label: '西门→会议中心', from: 'GATE_W', to: 'CONFERENCE', mode: 'SHUTTLE' },
  { label: '南门→办公楼B·摆渡', from: 'GATE_S', to: 'OFFICE_B', mode: 'SHUTTLE' }
]

function typeLabel(t) {
  return { GATE: '门岗', BUILDING: '办公楼', WORKSHOP: '样板车间', CONFERENCE: '会议中心' }[t]
}

function applyQuick(q) {
  from.value = q.from
  to.value = q.to
  mode.value = q.mode
  plan()
}

async function plan() {
  loading.value = true
  error.value = ''
  route.value = null
  try {
    const res = await api.route({ from: from.value, to: to.value, mode: mode.value })
    route.value = res
    emit('planned', res)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function onMapNodeClick(n) {
  if (n.type === 'GATE') {
    from.value = n.code
  } else if (['BUILDING', 'WORKSHOP', 'CONFERENCE'].includes(n.type)) {
    to.value = n.code
  }
}

defineExpose({ onMapNodeClick, replan: plan })
</script>
