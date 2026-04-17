<template>
  <div v-if="tabs.length" class="sub-tabs-wrap">
    <div ref="tabsRef" class="sub-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.path"
        type="button"
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
import { computed, ref, watch, nextTick } from 'vue'
import type { UserMenuNode } from '@/api/auth'

const props = defineProps<{
  menus: UserMenuNode[]
  currentPath: string
  currentModule: string
}>()

defineEmits<{
  (e: 'navigate', path: string): void
}>()

type TabItem = { path: string; label: string; sort: number }

/** 模块 → 枢纽页路由映射（点击父节点时跳转的概览页） */
const moduleHubMap: Record<string, { path: string; label: string }> = {
  '/system': { path: '/system/hub', label: '概览' },
  '/erp':    { path: '/erp/hub',    label: '概览' },
  '/oa':     { path: '/oa/hub',     label: '概览' },
}

function normalize(path: string): string {
  return path.startsWith('/') ? path : `/${path}`
}

function findFirstLeafPath(node: UserMenuNode): string {
  if (node.component) return normalize(node.fullPath || node.path) || ''
  for (const c of node.children || []) {
    const p = findFirstLeafPath(c)
    if (p) return p
  }
  return normalize(node.fullPath || node.path) || ''
}

/** 仅展示当前一级模块下的「二级」菜单：根节点的直接子节点，每个 Tab 对应一条可访问路由 */
function collectSecondLevelTabs(root: UserMenuNode, moduleBase: string): TabItem[] {
  const hub = moduleHubMap[moduleBase]
  const out: TabItem[] = hub ? [{ path: hub.path, label: hub.label, sort: -1 }] : []
  for (const child of root.children || []) {
    const path = child.component ? normalize(child.fullPath || child.path) : findFirstLeafPath(child)
    if (!path) continue
    out.push({ path, label: child.menuName, sort: child.sort ?? 0 })
  }
  return out
}

/** 无二级节点时：收集该模块下所有可路由叶子（兼容旧菜单扁平结构） */
function collectLeafTabs(nodes: UserMenuNode[] | undefined, out: TabItem[]) {
  for (const n of nodes || []) {
    if (n.component) {
      const p = normalize(n.fullPath || n.path)
      if (p) out.push({ path: p, label: n.menuName, sort: n.sort || 0 })
    }
    if (n.children?.length) collectLeafTabs(n.children, out)
  }
}

const tabs = computed<TabItem[]>(() => {
  if (!props.currentModule || props.currentModule === '/dashboard') return []
  for (const root of props.menus || []) {
    const rootBase = normalize((root.fullPath || root.path || '').split('/').filter(Boolean)[0] || '')
    if (rootBase === props.currentModule) {
      const second = collectSecondLevelTabs(root, rootBase)
      if (second.length) return second
      if (root.component) {
        const p = normalize(root.fullPath || root.path)
        if (p) return [{ path: p, label: root.menuName, sort: root.sort || 0 }]
      }
      const fallback: TabItem[] = []
      collectLeafTabs(root.children, fallback)
      const dedup = new Map<string, TabItem>()
      for (const t of fallback) dedup.set(t.path, t)
      return Array.from(dedup.values()).sort((a, b) => a.sort - b.sort)
    }
  }
  return []
})

function isActive(path: string): boolean {
  return props.currentPath === path || props.currentPath.startsWith(`${path}/`)
}

const tabsRef = ref<HTMLElement | null>(null)

watch(
  () => props.currentPath,
  async () => {
    await nextTick()
    const wrap = tabsRef.value
    if (!wrap) return
    const active = wrap.querySelector('.tab-btn.active') as HTMLElement | null
    if (!active) return
    const left = active.offsetLeft - 24
    wrap.scrollTo({ left, behavior: 'smooth' })
  },
  { immediate: true }
)
</script>

<style scoped>
.sub-tabs-wrap {
  background: rgba(255, 255, 255, 0.82);
  padding: 0 24px;
  border-bottom: none;
}

.sub-tabs {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
}

.sub-tabs::-webkit-scrollbar {
  display: none;
}

.tab-btn {
  position: relative;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  border-radius: 0;
  padding: 12px 14px 14px;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.tab-btn:hover {
  color: var(--color-primary);
}

.tab-btn.active {
  color: var(--color-primary);
  font-weight: 700;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 0;
  height: 2px;
  border-radius: 2px;
  background: var(--color-primary);
}
</style>
