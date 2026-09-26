<template>
  <div class="map-wrap">
    <svg class="map-svg" viewBox="0 0 1000 720" preserveAspectRatio="xMidYMid meet"
         @mousemove="onMove" @mouseleave="hover = null">
      <!-- 园区底图 -->
      <defs>
        <pattern id="hatch" patternUnits="userSpaceOnUse" width="10" height="10"
                 patternTransform="rotate(45)">
          <rect width="10" height="10" fill="#f6d6d6"/>
          <line x1="0" y1="0" x2="0" y2="10" stroke="#dc3b3b" stroke-width="2.5" opacity="0.55"/>
        </pattern>
        <linearGradient id="grass" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#eaf3e2"/>
          <stop offset="100%" stop-color="#dfe9d4"/>
        </linearGradient>
        <filter id="routeGlow" x="-30%" y="-30%" width="160%" height="160%">
          <feDropShadow dx="0" dy="0" stdDeviation="3" flood-color="#ffb020" flood-opacity="0.9"/>
        </filter>
      </defs>

      <rect x="0" y="0" width="1000" height="720" fill="url(#grass)"/>
      <rect x="14" y="14" width="972" height="692" rx="18" fill="none"
            stroke="#b9cd9f" stroke-width="3" stroke-dasharray="10 8" opacity="0.8"/>
      <text x="30" y="44" font-size="15" fill="#7d9465" font-weight="700">智汇工业园 · 总平面图</text>

      <!-- 区域底块 -->
      <g opacity="0.55">
        <rect :x="nodeByCode['CONS']?.x - 78" :y="nodeByCode['CONS']?.y - 58" width="156" height="116"
              rx="10" fill="url(#hatch)" stroke="#dc3b3b" stroke-width="2" v-if="nodeByCode['CONS']"/>
        <rect :x="nodeByCode['LOGISTICS']?.x - 92" :y="nodeByCode['LOGISTICS']?.y - 60"
              width="184" height="120" rx="10"
              fill="#f7e7c8" stroke="#b45309" stroke-width="2" stroke-dasharray="8 6"
              v-if="nodeByCode['LOGISTICS']"/>
        <text :x="(nodeByCode['LOGISTICS']?.x ?? 0)" :y="(nodeByCode['LOGISTICS']?.y ?? 0) + 82"
              font-size="12" fill="#8a4b08" text-anchor="middle" font-weight="600"
              v-if="nodeByCode['LOGISTICS']">货运作业区 · 行人禁入</text>
      </g>

      <!-- 道路层 -->
      <g>
        <line v-for="e in edges" :key="e.code"
              :x1="nodeByCode[e.fromCode]?.x" :y1="nodeByCode[e.fromCode]?.y"
              :x2="nodeByCode[e.toCode]?.x" :y2="nodeByCode[e.toCode]?.y"
              :stroke="edgeColor(e)" :stroke-width="edgeWidth(e)"
              :stroke-dasharray="edgeDash(e)"
              stroke-linecap="round" opacity="0.95"
              :class="{ edge: true, inactive: !e.active }"
              @click="$emit('edgeClick', e)" style="cursor:pointer"/>
      </g>

      <!-- 规划路线层 -->
      <g v-if="route && route.found">
        <polyline :points="routePoints" fill="none"
                  :stroke="routeMode === 'WALK' ? '#12a365' : '#1a6dd9'"
                  stroke-width="9" stroke-linecap="round" stroke-linejoin="round"
                  opacity="0.28"/>
        <polyline :points="routePoints" fill="none"
                  :stroke="routeMode === 'WALK' ? '#0e8a55' : '#1559b3'"
                  stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"
                  filter="url(#routeGlow)">
          <animate attributeName="stroke-dashoffset" from="24" to="0" dur="0.9s"
                   repeatCount="indefinite"/>
          <animate attributeName="stroke-dasharray" values="14 10;16 8;14 10" dur="1.4s"
                   repeatCount="indefinite"/>
        </polyline>
      </g>

      <!-- 节点层 -->
      <g v-for="n in nodes" :key="n.code" class="node-group"
         :opacity="n.active ? 1 : 0.45"
         @click="$emit('nodeClick', n)"
         @mouseenter="hover = n" @mouseleave="hover = null"
         style="cursor:pointer">
        <g :transform="`translate(${n.x},${n.y})`">
          <!-- 起终点高亮 -->
          <circle r="17" fill="#fff7e0" stroke="#f59e0b" stroke-width="2.5"
                  v-if="isEndpoint(n.code)">
            <animate attributeName="r" values="15;19;15" dur="1.6s" repeatCount="indefinite"/>
          </circle>

          <circle r="11" fill="#fff" stroke="#1a6dd9" stroke-width="3"
                  v-if="n.type === 'STOP'"/>
          <rect x="-12" y="-12" width="24" height="24" rx="5" fill="#0f3f86"
                v-else-if="n.type === 'GATE'"/>
          <rect x="-13" y="-13" width="26" height="26" rx="7" fill="#12a365"
                v-else-if="n.type === 'BUILDING'"/>
          <polygon points="0,-14 13,8 -13,8" fill="#7c3aed"
                v-else-if="n.type === 'WORKSHOP'"/>
          <rect x="-13" y="-11" width="26" height="22" rx="11" fill="#0ea5e9"
                v-else-if="n.type === 'CONFERENCE'"/>
          <rect x="-11" y="-11" width="22" height="22" rx="4" fill="#dc3b3b"
                v-else-if="n.type === 'CONSTRUCTION'"/>
          <rect x="-11" y="-11" width="22" height="22" rx="3" fill="#b45309"
                v-else-if="n.type === 'LOGISTICS'"/>
          <circle r="7" fill="#94a3b8"
                v-else/>

          <text text-anchor="middle" y="4" font-size="11" fill="#fff" font-weight="700"
                v-if="n.type === 'GATE'">岗</text>
          <text text-anchor="middle" y="4" font-size="11" fill="#fff" font-weight="700"
                v-else-if="n.type === 'CONSTRUCTION'">!</text>
          <circle r="3.5" fill="#1a6dd9" v-else-if="n.type === 'STOP'"/>

          <text text-anchor="middle" :y="labelY(n)" font-size="12.5"
                :fill="isEndpoint(n.code) ? '#b45309' : '#23364d'"
                font-weight="600"
                :style="{ paintOrder: 'stroke', stroke: '#f3f7ef', strokeWidth: 3 }">
            {{ n.name }}
          </text>
        </g>
      </g>

      <!-- hover 提示 -->
      <g v-if="hover" :transform="`translate(${hoverTip.x},${hoverTip.y})`" pointer-events="none">
        <rect x="0" y="0" :width="hoverTip.w" height="26" rx="7" fill="#1f2a37" opacity="0.92"/>
        <text x="10" y="17" font-size="12" fill="#fff" font-weight="600">{{ hover.name }}（{{ typeLabel(hover.type) }}）</text>
      </g>
    </svg>

    <!-- 图例 -->
    <div class="legend">
      <div class="item"><span class="swatch" style="background:#9aa7b8"></span>步行/摆渡共用道</div>
      <div class="item"><span class="swatch" style="background:#1a6dd9;height:5px"></span>摆渡车可通行</div>
      <div class="item"><span class="swatch" style="background:#b45309;opacity:.8"></span>货车通道（访客避开）</div>
      <div class="item"><span class="swatch" style="background:#dc3b3b"></span>临时封闭路段</div>
      <div class="item"><span class="swatch" style="background:#0e8a55;height:5px"></span>推荐访客路线</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  closures: { type: Array, default: () => [] },
  route: { type: Object, default: null },
  routeMode: { type: String, default: 'WALK' }
})
defineEmits(['nodeClick', 'edgeClick'])

const hover = ref(null)
const mouse = ref({ x: 0, y: 0 })

const nodeByCode = computed(() => {
  const m = {}
  for (const n of props.nodes) m[n.code] = n
  return m
})

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

function edgeColor(e) {
  if (!e.active || closedSet.value.has(e.code)) return '#dc3b3b'
  if (e.truckRoad) return '#b45309'
  return '#94a3b8'
}
function edgeWidth(e) {
  if (closedSet.value.has(e.code)) return 4
  if (e.truckRoad) return 6
  return e.shuttleAllowed ? 7 : 4
}
function edgeDash(e) {
  if (!e.active) return '3 6'
  if (closedSet.value.has(e.code)) return '8 6'
  if (e.truckRoad) return '10 6'
  return undefined
}

const routePoints = computed(() => {
  if (!props.route || !props.route.found) return ''
  return props.route.nodePath.map(code => {
    const n = nodeByCode.value[code]
    return n ? `${n.x},${n.y}` : ''
  }).filter(Boolean).join(' ')
})

function isEndpoint(code) {
  return props.route?.found &&
      (props.route.fromCode === code || props.route.toCode === code)
}

function labelY(n) {
  if (n.type === 'BUILDING' || n.type === 'WORKSHOP') return -22
  if (n.type === 'CONSTRUCTION') return 32
  if (n.y > 560) return -20
  return 28
}

function typeLabel(t) {
  return {
    GATE: '门岗', BUILDING: '办公楼', WORKSHOP: '样板车间',
    CONFERENCE: '会议中心', STOP: '摆渡车站', JUNCTION: '路口',
    CONSTRUCTION: '施工区', LOGISTICS: '物流区'
  }[t] || t
}

const hoverTip = computed(() => {
  if (!hover.value) return { x: 0, y: 0, w: 0 }
  const w = hover.value.name.length * 13 + typeLabel(hover.value.type).length * 13 + 34
  let x = mouse.value.x + 14
  let y = mouse.value.y - 34
  if (x + w > 990) x = mouse.value.x - w - 10
  if (y < 8) y = mouse.value.y + 16
  return { x, y, w }
})

function onMove(ev) {
  const svg = ev.currentTarget
  const rect = svg.getBoundingClientRect()
  mouse.value = {
    x: (ev.clientX - rect.left) / rect.width * 1000,
    y: (ev.clientY - rect.top) / rect.height * 720
  }
}
</script>
