<template>
  <aside class="app-sidebar" :class="{ collapsed }">
    <div class="brand">
      <div class="logo">N</div>
      <div v-if="!collapsed" class="brand-text">
        <div class="title">NexusERP</div>
        <div class="sub">Enterprise Suite</div>
      </div>
    </div>

    <div class="nav">
      <button
        class="nav-item"
        :class="{ active: currentPath === '/dashboard' }"
        @click="$emit('navigate', '/dashboard')"
      >
        <el-icon><HomeFilled /></el-icon>
        <span v-if="!collapsed">工作台</span>
      </button>

      <button
        v-for="module in modules"
        :key="module.base"
        class="nav-item"
        :class="{ active: isModuleActive(module.base) }"
        @click="$emit('navigate', module.firstPath)"
      >
        <el-icon><component :is="module.icon || 'Menu'" /></el-icon>
        <span v-if="!collapsed">{{ module.label }}</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MenuNode } from '@/api/system'

const props = defineProps<{
  menus: MenuNode[]
  currentPath: string
  collapsed: boolean
}>()

defineEmits<{
  (e: 'navigate', path: string): void
}>()

type ModuleItem = { base: string; label: string; icon: string; firstPath: string }

function normalizePath(path?: string): string {
  if (!path) return ''
  return path.startsWith('/') ? path : `/${path}`
}

function findFirstLeafPath(node: MenuNode): string {
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

function isModuleActive(base: string): boolean {
  return props.currentPath === base || props.currentPath.startsWith(`${base}/`)
}
</script>

<style scoped>
.app-sidebar {
  width: var(--sidebar-width);
  border-right: 1px solid var(--sidebar-border);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal);
}

.app-sidebar.collapsed { width: var(--sidebar-collapsed-width); }

.brand {
  height: var(--header-height);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid var(--sidebar-border);
}

.logo {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.title { font-weight: 700; }
.sub { font-size: 11px; color: var(--text-muted); text-transform: uppercase; }

.nav { padding: 12px 10px; display: flex; flex-direction: column; gap: 8px; }

.nav-item {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--sidebar-text-muted);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all var(--transition-fast);
}

.nav-item:hover {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-text);
}

.nav-item.active {
  background: var(--sidebar-active-bg);
  color: var(--sidebar-active-text);
  box-shadow: var(--shadow-sm);
}

.collapsed .nav-item { justify-content: center; padding: 12px 6px; }
</style>
