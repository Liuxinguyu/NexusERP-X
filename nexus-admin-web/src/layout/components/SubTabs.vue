<template>
  <div v-if="tabs.length" class="sub-tabs-wrap">
    <div ref="tabsRef" class="sub-tabs">
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
import { computed, ref, watch, nextTick } from 'vue'
import type { UserMenuNode } from '@/api/auth'

const props = defineProps<{
  menus: UserMenuNode[]
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

function collectLeaf(nodes: UserMenuNode[] | undefined, out: TabItem[]) {
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
  border-bottom: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.82);
  padding: 8px 24px;
}

.sub-tabs {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  scrollbar-width: none;
}

.sub-tabs::-webkit-scrollbar {
  display: none;
}

.tab-btn {
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  border-radius: 10px;
  padding: 10px 14px;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast);
}

.tab-btn:hover {
  background: #f5f7ff;
  color: var(--color-primary);
}

.tab-btn.active {
  background: #fff;
  border-color: #d7dcff;
  color: var(--color-primary);
  font-weight: 700;
}
</style>
