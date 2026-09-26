<template>
  <header class="topbar">
    <div class="logo">
      <span class="badge">🏭</span>
      <span>工业园访客路线系统</span>
    </div>
    <nav>
      <RouterLink to="/" exact-active-class="active">访客导航</RouterLink>
      <RouterLink to="/admin" active-class="active">园区图管理</RouterLink>
    </nav>
    <div class="spacer"></div>
    <span class="hint">货车通道与施工区域已自动避让</span>
  </header>
  <RouterView />
</template>

<script setup>
</script>
