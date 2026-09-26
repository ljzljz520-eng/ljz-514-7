const BASE = '/api'

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })
  if (!res.ok) {
    let msg = `请求失败 (${res.status})`
    try {
      const data = await res.json()
      if (data.message) msg = data.message
    } catch (_) { /* ignore */ }
    throw new Error(msg)
  }
  if (res.status === 204) return null
  return res.json()
}

export const api = {
  graph: () => request('/graph'),
  route: (body) => request('/route', { method: 'POST', body: JSON.stringify(body) }),

  listNodes: () => request('/admin/nodes'),
  createNode: (n) => request('/admin/nodes', { method: 'POST', body: JSON.stringify(n) }),
  updateNode: (id, n) => request(`/admin/nodes/${id}`, { method: 'PUT', body: JSON.stringify(n) }),
  deleteNode: (id) => request(`/admin/nodes/${id}`, { method: 'DELETE' }),

  listEdges: () => request('/admin/edges'),
  createEdge: (e) => request('/admin/edges', { method: 'POST', body: JSON.stringify(e) }),
  updateEdge: (id, e) => request(`/admin/edges/${id}`, { method: 'PUT', body: JSON.stringify(e) }),
  deleteEdge: (id) => request(`/admin/edges/${id}`, { method: 'DELETE' }),

  listClosures: () => request('/admin/closures'),
  createClosure: (c) => request('/admin/closures', { method: 'POST', body: JSON.stringify(c) }),
  updateClosure: (id, c) => request(`/admin/closures/${id}`, { method: 'PUT', body: JSON.stringify(c) }),
  deleteClosure: (id) => request(`/admin/closures/${id}`, { method: 'DELETE' })
}
