<template>
  <aside class="app-sidebar">
    <div class="brand">
      <div class="logo">N</div>
      <div class="brand-text">
        <div class="title">NexusERP</div>
        <div class="sub">Linear Style Console</div>
      </div>
    </div>

    <div class="nav">
      <button
        class="nav-item"
        :class="{ active: currentPath === '/dashboard' }"
        @click="$emit('navigate', '/dashboard')"
      >
        <el-icon><HomeFilled /></el-icon>
        <span>工作台</span>
      </button>

      <button
        v-for="module in modules"
        :key="module.base"
        class="nav-item"
        :class="{ active: module.base === activeModuleBase }"
        @click="$emit('navigate', module.firstPath)"
      >
        <el-icon><component :is="module.icon || 'Menu'" /></el-icon>
        <span>{{ module.label }}</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { UserMenuNode } from '@/api/auth'

const props = defineProps<{
  menus: UserMenuNode[]
  currentPath: string
  activeModuleBase: string
}>()

defineEmits<{
  (e: 'navigate', path: string): void
}>()

type ModuleItem = { base: string; label: string; icon: string; firstPath: string }

function normalizePath(path?: string): string {
  if (!path) return ''
  return path.startsWith('/') ? path : `/${path}`
}

function findFirstLeafPath(node: UserMenuNode): string {
  if (node.component) return normalizePath(node.fullPath || node.path) || '/dashboard'
  for (const c of node.children || []) {
    const leaf = findFirstLeafPath(c)
    if (leaf) return leaf
  }
  return normalizePath(node.fullPath || node.path) || '/dashboard'
}

const modules = computed<ModuleItem[]>(() => {
  const out: ModuleItem[] = []
  for (const m of props.menus || []) {
    const base = normalizePath((m.fullPath || m.path || '').split('/').filter(Boolean)[0] || '')
    if (!base || base === '/dashboard') continue
    out.push({
      base,
      label: m.menuName,
      icon: m.icon || 'Menu',
      firstPath: findFirstLeafPath(m),
    })
  }
  return out
})
</script>

<style scoped>
.app-sidebar {
  width: var(--sidebar-width);
  border-right: 1px solid var(--sidebar-border);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  gap: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 4px;
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
}

.title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.sub {
  font-size: 12px;
  color: var(--text-muted);
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-item {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--sidebar-text-muted);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 14px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  transition: background-color var(--transition-fast), color var(--transition-fast);
}

.nav-item:hover {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-text);
}

.nav-item.active {
  background: var(--sidebar-active-bg);
  color: var(--sidebar-active-text);
  font-weight: 700;
}
</style>
