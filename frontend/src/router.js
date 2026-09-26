import { createRouter, createWebHistory } from 'vue-router'
import VisitorView from './views/VisitorView.vue'
import AdminView from './views/AdminView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'visitor', component: VisitorView, meta: { title: '访客导航' } },
    { path: '/admin', name: 'admin', component: AdminView, meta: { title: '园区图管理' } }
  ]
})
