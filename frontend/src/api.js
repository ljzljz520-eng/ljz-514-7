/** 后端 API 封装 */
async function request(url, options = {}) {
  const resp = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })
  if (!resp.ok) {
    let message = `请求失败（${resp.status}）`
    try {
      const body = await resp.json()
      if (body.message) message = body.message
    } catch { /* ignore */ }
    throw new Error(message)
  }
  return resp.json()
}

export const api = {
  // 访客
  getMap: () => request('/api/map'),
  planRoute: (fromId, toId, mode) =>
    request('/api/route', { method: 'POST', body: JSON.stringify({ fromId, toId, mode }) }),

  // 管理：节点
  listNodes: () => request('/api/admin/nodes'),
  createNode: (node) => request('/api/admin/nodes', { method: 'POST', body: JSON.stringify(node) }),
  updateNode: (id, node) => request(`/api/admin/nodes/${id}`, { method: 'PUT', body: JSON.stringify(node) }),
  deleteNode: (id) => request(`/api/admin/nodes/${id}`, { method: 'DELETE' }),

  // 管理：边
  listEdges: () => request('/api/admin/edges'),
  createEdge: (edge) => request('/api/admin/edges', { method: 'POST', body: JSON.stringify(edge) }),
  updateEdge: (id, edge) => request(`/api/admin/edges/${id}`, { method: 'PUT', body: JSON.stringify(edge) }),
  deleteEdge: (id) => request(`/api/admin/edges/${id}`, { method: 'DELETE' }),

  // 管理：封闭
  listClosures: () => request('/api/admin/closures'),
  createClosure: (c) => request('/api/admin/closures', { method: 'POST', body: JSON.stringify(c) }),
  updateClosure: (id, c) => request(`/api/admin/closures/${id}`, { method: 'PUT', body: JSON.stringify(c) }),
  deleteClosure: (id) => request(`/api/admin/closures/${id}`, { method: 'DELETE' })
}
