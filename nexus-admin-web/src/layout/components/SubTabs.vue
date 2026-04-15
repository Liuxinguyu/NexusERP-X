<template>
  <div v-if="tabs.length" class="sub-tabs-wrap">
    <div class="sub-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.path"
        class="tab-btn"
        :class="{ active: isActive(tab.path) }"
        @click="$emit('navigate', tab.path)"
      >
        {{ tab.label }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MenuNode } from '@/api/system'

const props = defineProps<{
  menus: MenuNode[]
  currentPath: string
  currentModuleBase: string
}>()

defineEmits<{
  (e: 'navigate', path: string): void
}>()

type TabItem = { path: string; label: string; sort: number }

function normalize(path: string): string {
  return path.startsWith('/') ? path : `/${path}`
}

function collectLeaf(nodes: MenuNode[] | undefined, out: TabItem[]) {
  for (const n of nodes || []) {
    if (n.component) {
      const p = normalize(n.fullPath || n.path)
      out.push({ path: p, label: n.menuName, sort: n.sort || 0 })
    }
    if (n.children?.length) collectLeaf(n.children, out)
  }
}

const tabs = computed<TabItem[]>(() => {
  if (!props.currentModuleBase || props.currentModuleBase === '/dashboard') return []
  const out: TabItem[] = []
  for (const root of props.menus || []) {
    const rootBase = normalize((root.fullPath || root.path || '').split('/').filter(Boolean)[0] || '')
    if (rootBase === props.currentModuleBase) {
      collectLeaf([root], out)
      break
    }
  }
  const dedup = new Map<string, TabItem>()
  for (const t of out) dedup.set(t.path, t)
  return Array.from(dedup.values()).sort((a, b) => a.sort - b.sort)
})

function isActive(path: string): boolean {
  return props.currentPath === path || props.currentPath.startsWith(`${path}/`)
}
</script>

<style scoped>
.sub-tabs-wrap {
  border-bottom: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.72);
}

.sub-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 10px 16px;
}

.tab-btn {
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  border-radius: 999px;
  padding: 8px 14px;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.tab-btn:hover {
  background: #eef2ff;
  color: var(--color-primary);
}

.tab-btn.active {
  background: #fff;
  border-color: #c7d2fe;
  color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}
</style>
