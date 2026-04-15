<template>
  <div class="route-fallback">
    <div class="fallback-card">
      <h2>页面正在开发中或视图文件丢失</h2>
      <p class="desc">当前访问路径：</p>
      <p class="path">{{ route.fullPath }}</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.route-fallback {
  min-height: calc(100vh - 140px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.fallback-card {
  width: min(640px, 100%);
  padding: 40px 28px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-sm);
  text-align: center;
}

.fallback-card h2 {
  margin: 0 0 14px;
  font-size: 24px;
  color: var(--text-primary);
}

.desc {
  margin: 0;
  color: var(--text-secondary);
}

.path {
  margin: 8px 0 22px;
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  word-break: break-all;
}
</style>
