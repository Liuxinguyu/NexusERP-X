<template>
  <aside class="app-sidebar">
    <div class="brand">
      <div class="logo">N</div>
    </div>

    <nav class="nav" aria-label="一级模块">
      <el-tooltip content="工作台" placement="right" :show-after="300">
        <button
          type="button"
          class="nav-item"
          :class="{ active: activeModule === '/dashboard' }"
          @click="onDashboard"
        >
          <el-icon :size="22"><HomeFilled /></el-icon>
        </button>
      </el-tooltip>

      <el-tooltip
        v-for="module in modules"
        :key="module.base"
        :content="module.label"
        placement="right"
        :show-after="300"
      >
        <button
          type="button"
          class="nav-item"
          :class="{ active: module.base === activeModule }"
          @click="handleModuleClick(module.base)"
        >
          <el-icon :size="22"><component :is="module.icon || 'Menu'" /></el-icon>
        </button>
      </el-tooltip>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { UserMenuNode } from '@/api/auth'
import { useAppStore } from '@/stores/app'

const props = defineProps<{
  menus: UserMenuNode[]
  activeModule: string
}>()

const emit = defineEmits<{ (e: 'select-dashboard'): void }>()
const appStore = useAppStore()
const router = useRouter()

function normalizePath(path?: string): string {
  if (!path) return ''
  return path.startsWith('/') ? path : `/${path}`
}

function onDashboard() {
  emit('select-dashboard')
}

function handleParentClick(path: string) {
  router.push(path)
}

function handleModuleClick(base: string) {
  appStore.setActiveModule(base, props.menus)
  const hubPath = moduleHubMap[base]
  if (hubPath) {
    router.push(hubPath)
  } else if (appStore.activeTabPath) {
    router.push(appStore.activeTabPath)
  }
}

const moduleHubMap: Record<string, string> = {
  '/system': '/system/hub',
  '/erp':    '/erp/hub',
  '/oa':     '/oa/hub',
}

type ModuleItem = { base: string; label: string; icon: string }

const modules = computed<ModuleItem[]>(() => {
  const out: ModuleItem[] = []
  for (const m of props.menus || []) {
    const seg = (m.fullPath || m.path || '').split('/').filter(Boolean)[0] || ''
    const base = normalizePath(seg)
    if (!base || base === '/dashboard') continue
    out.push({
      base,
      label: m.menuName,
      icon: m.icon || 'Menu',
    })
  }
  return out
})
</script>

<style scoped>
.app-sidebar {
  width: var(--sidebar-width);
  height: 100%;
  flex-shrink: 0;
  border-right: 1px solid var(--sidebar-border);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0 24px;
  gap: 20px;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 16px;
}

.nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
  width: 100%;
}

.nav-item {
  width: 48px;
  height: 48px;
  border: none;
  background: transparent;
  color: var(--sidebar-text-muted);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast);
}

.nav-item:hover {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-text);
}

.nav-item.active {
  background: var(--sidebar-active-bg);
  color: var(--sidebar-active-text);
}
</style>
