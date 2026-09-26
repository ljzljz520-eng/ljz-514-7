<template>
  <svg class="park-map" :viewBox="`0 0 ${W} ${H}`" @click="onSvgClick">
    <defs>
      <pattern id="truckHatch" width="8" height="8" patternTransform="rotate(45)" patternUnits="userSpaceOnUse">
        <rect width="8" height="8" fill="#fdf6e3" />
        <line x1="0" y1="0" x2="0" y2="8" stroke="#d97706" stroke-width="2.5" />
      </pattern>
    </defs>

    <!-- 园区背景装饰 -->
    <rect x="0" y="0" :width="W" :height="H" fill="#eef3ea" />
    <rect x="16" y="16" :width="W - 32" :height="H - 32" rx="14" fill="none" stroke="#cbd5e1" stroke-width="2" stroke-dasharray="10 7" />

    <!-- 道路 -->
    <g v-for="e in edges" :key="e.id">
      <template v-if="nodeOf(e.from) && nodeOf(e.to)">
        <!-- 货车通道：警示底色 + 斜纹 -->
        <template v-if="e.type === 'TRUCK'">
          <line :x1="nodeOf(e.from).x" :y1="nodeOf(e.from).y" :x2="nodeOf(e.to).x" :y2="nodeOf(e.to).y"
                stroke="#f59e0b" stroke-width="9" opacity="0.35" stroke-linecap="round" />
          <line :x1="nodeOf(e.from).x" :y1="nodeOf(e.from).y" :x2="nodeOf(e.to).x" :y2="nodeOf(e.to).y"
                stroke="#d97706" stroke-width="3.5" stroke-dasharray="9 6" stroke-linecap="round" />
        </template>
        <template v-else-if="e.type === 'SHUTTLE'">
          <line :x1="nodeOf(e.from).x" :y1="nodeOf(e.from).y" :x2="nodeOf(e.to).x" :y2="nodeOf(e.to).y"
                :stroke="isClosed(e.id, 'EDGE') ? '#dc2626' : '#2563eb'"
                stroke-width="4" stroke-dasharray="12 7" stroke-linecap="round"
                :opacity="isClosed(e.id, 'EDGE') ? 0.85 : 0.55" />
        </template>
        <template v-else>
          <line :x1="nodeOf(e.from).x" :y1="nodeOf(e.from).y" :x2="nodeOf(e.to).x" :y2="nodeOf(e.to).y"
                :stroke="isClosed(e.id, 'EDGE') ? '#dc2626' : '#a8b6c4'"
                :stroke-width="isClosed(e.id, 'EDGE') ? 4.5 : 4" stroke-linecap="round"
                :stroke-dasharray="isClosed(e.id, 'EDGE') ? '6 5' : 'none'" />
        </template>
        <!-- 封闭标记 -->
        <g v-if="isClosed(e.id, 'EDGE')" :transform="`translate(${(nodeOf(e.from).x + nodeOf(e.to).x) / 2}, ${(nodeOf(e.from).y + nodeOf(e.to).y) / 2})`">
          <circle r="10" fill="#dc2626" stroke="#fff" stroke-width="2" />
          <text y="4" text-anchor="middle" font-size="11" fill="#fff">🚧</text>
        </g>
      </template>
    </g>

    <!-- 规划路线高亮 -->
    <g v-if="segments && segments.length">
      <template v-for="(seg, i) in segments" :key="i">
        <polyline :points="polyline(seg.points)" fill="none"
                  :stroke="seg.type === 'SHUTTLE' ? '#2563eb' : '#16a34a'"
                  stroke-width="11" stroke-linecap="round" stroke-linejoin="round" opacity="0.28" />
        <polyline :points="polyline(seg.points)" fill="none"
                  :stroke="seg.type === 'SHUTTLE' ? '#2563eb' : '#16a34a'"
                  stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"
                  :stroke-dasharray="seg.type === 'SHUTTLE' ? '14 8' : 'none'"
                  class="route-anim" />
      </template>
    </g>

    <!-- 节点 -->
    <g v-for="n in nodes" :key="n.id"
       :transform="`translate(${n.x}, ${n.y})`"
       :style="{ cursor: clickable ? 'pointer' : 'default' }"
       @click.stop="emit('node-click', n)">
      <!-- 起终点光环 -->
      <circle v-if="n.id === selectedFrom || n.id === selectedTo" r="20"
              :fill="n.id === selectedFrom ? 'rgba(22,163,74,.25)' : 'rgba(220,38,38,.25)'">
        <animate attributeName="r" values="17;23;17" dur="1.8s" repeatCount="indefinite" />
      </circle>

      <!-- 门岗：绿色方块 -->
      <g v-if="n.type === 'GATE'">
        <rect x="-11" y="-11" width="22" height="22" rx="5" fill="#16a34a" stroke="#fff" stroke-width="2.5" />
        <text y="4.5" text-anchor="middle" font-size="12" fill="#fff">门</text>
      </g>
      <!-- 办公楼 -->
      <g v-else-if="n.type === 'OFFICE'">
        <rect x="-13" y="-13" width="26" height="26" rx="5" fill="#2563eb" stroke="#fff" stroke-width="2.5" />
        <text y="5" text-anchor="middle" font-size="13" fill="#fff">办</text>
      </g>
      <!-- 样板车间 -->
      <g v-else-if="n.type === 'WORKSHOP'">
        <rect x="-13" y="-13" width="26" height="26" rx="5" fill="#7c3aed" stroke="#fff" stroke-width="2.5" />
        <text y="5" text-anchor="middle" font-size="13" fill="#fff">车</text>
      </g>
      <!-- 会议中心 -->
      <g v-else-if="n.type === 'CONFERENCE'">
        <rect x="-13" y="-13" width="26" height="26" rx="5" fill="#0d9488" stroke="#fff" stroke-width="2.5" />
        <text y="5" text-anchor="middle" font-size="13" fill="#fff">会</text>
      </g>
      <!-- 摆渡站 -->
      <g v-else-if="n.type === 'SHUTTLE_STOP'">
        <circle r="9" fill="#fff" stroke="#2563eb" stroke-width="3" />
        <text y="3.5" text-anchor="middle" font-size="9.5" font-weight="700" fill="#2563eb">摆</text>
      </g>
      <!-- 货运/其他设施 -->
      <g v-else-if="n.type === 'FACILITY'">
        <rect x="-7" y="-7" width="14" height="14" rx="3" fill="#d97706" stroke="#fff" stroke-width="2" />
      </g>
      <!-- 路口 -->
      <circle v-else r="5" fill="#94a3b8" stroke="#fff" stroke-width="2" />

      <!-- 封闭节点标记 -->
      <g v-if="isClosed(n.id, 'NODE')">
        <circle r="16" fill="none" stroke="#dc2626" stroke-width="2.5" stroke-dasharray="4 3" />
        <text y="-18" text-anchor="middle" font-size="13">🚧</text>
      </g>

      <!-- 名称 -->
      <text :y="labelY(n)" text-anchor="middle" font-size="12" fill="#334155"
            font-weight="600" style="paint-order: stroke; stroke: #fff; stroke-width: 3.5px;">
        {{ n.name }}
      </text>
    </g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const W = 1000
const H = 640

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  activeClosures: { type: Array, default: () => [] },
  segments: { type: Array, default: () => [] },
  selectedFrom: { type: String, default: '' },
  selectedTo: { type: String, default: '' },
  clickable: { type: Boolean, default: false }
})

const emit = defineEmits(['node-click', 'map-click'])

const nodeMap = computed(() => new Map(props.nodes.map(n => [n.id, n])))
const nodeOf = (id) => nodeMap.value.get(id)

const closedSet = computed(() => {
  const s = new Set()
  for (const c of props.activeClosures) s.add(`${c.targetType}:${c.targetId}`)
  return s
})
const isClosed = (id, type) => closedSet.value.has(`${type}:${id}`)

const polyline = (pts) => pts.map(p => `${p[0]},${p[1]}`).join(' ')

const labelY = (n) => {
  if (['GATE', 'OFFICE', 'WORKSHOP', 'CONFERENCE'].includes(n.type)) return 30
  if (n.type === 'SHUTTLE_STOP') return 24
  return 20
}

function onSvgClick(e) {
  if (!props.clickable) return
  const svg = e.currentTarget
  const rect = svg.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * W
  const y = ((e.clientY - rect.top) / rect.height) * H
  emit('map-click', { x: Math.round(x), y: Math.round(y) })
}
</script>

<style scoped>
.route-anim {
  animation: dashmove 1.2s linear infinite;
}
@keyframes dashmove {
  to { stroke-dashoffset: -22; }
}
</style>
