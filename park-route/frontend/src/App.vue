<template>
  <header class="app-header">
    <div class="brand">
      <span class="logo">🏭</span>
      <div>
        工业园访客路线系统
        <div style="font-size:11px;font-weight:400;opacity:.85">
          访客路线自动避让货车通道与施工区域
        </div>
      </div>
    </div>
    <nav class="tabs">
      <button class="tab" :class="{ active: tab === 'visitor' }" @click="tab = 'visitor'">
        🚶 访客端
      </button>
      <button class="tab" :class="{ active: tab === 'admin' }" @click="switchAdmin">
        🛠️ 管理员端
      </button>
    </nav>
  </header>

  <div v-if="loading" class="loading-box" style="height:60vh">
    <div class="spin"></div>
    <div>正在加载园区地图…</div>
  </div>

  <div v-else-if="loadError" class="loading-box" style="height:60vh">
    <div class="alert alert-error" style="max-width:420px">
      地图加载失败：{{ loadError }}<br/>请确认后端服务已启动（端口 8080）。
    </div>
    <button class="btn btn-primary" style="width:auto" @click="loadGraph">重试</button>
  </div>

  <div v-else :class="tab === 'visitor' ? 'layout' : 'admin-layout'">
    <template v-if="tab === 'visitor'">
      <VisitorPanel ref="visitorRef" :nodes="graph.nodes" @planned="onPlanned"/>
      <MapView :nodes="graph.nodes" :edges="graph.edges" :closures="graph.closures"
               :route="route" :route-mode="routeMode"
               @node-click="onMapNodeClick"/>
    </template>
    <template v-else>
      <MapView :nodes="graph.nodes" :edges="graph.edges" :closures="graph.closures"
               :route="null" @edge-click="onMapEdgeClick"/>
      <AdminPanel :nodes="graph.nodes" :edges="graph.edges" :closures="graph.closures"
                  @changed="loadGraph" @toast="showToast"/>
    </template>
  </div>

  <transition name="fade">
    <div v-if="toast" class="toast" :class="toast.kind">{{ toast.msg }}</div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from './api/client.js'
import MapView from './components/MapView.vue'
import VisitorPanel from './components/VisitorPanel.vue'
import AdminPanel from './components/AdminPanel.vue'

const tab = ref('visitor')
const graph = ref({ nodes: [], edges: [], closures: [] })
const loading = ref(true)
const loadError = ref('')
const route = ref(null)
const routeMode = ref('WALK')
const visitorRef = ref(null)
const toast = ref(null)

let toastTimer = null
function showToast(t) {
  toast.value = t
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = null), 2600)
}

async function loadGraph() {
  loading.value = true
  loadError.value = ''
  try {
    graph.value = await api.graph()
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}

function onPlanned(res) {
  route.value = res
  routeMode.value = res.mode
}

function onMapNodeClick(n) {
  if (tab.value === 'visitor' && visitorRef.value) {
    visitorRef.value.onMapNodeClick(n)
  }
}

function onMapEdgeClick() {
  // 点击地图道路时切到管理端定位（简单提示）
  showToast({ msg: '可在右侧管理列表中编辑该道路或设置封闭', kind: 'success' })
}

function switchAdmin() {
  tab.value = 'admin'
  route.value = null
}

onMounted(loadGraph)
</script>
