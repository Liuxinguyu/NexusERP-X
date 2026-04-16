import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserMenuNode } from '@/api/auth'

/** 一级模块路径前缀，如 /dashboard、/erp、/system */
export const useAppStore = defineStore('app', () => {
  const activeModule = ref('/dashboard')
  const activeTabPath = ref('/dashboard')

  function normalizePath(path?: string): string {
    if (!path) return ''
    return path.startsWith('/') ? path : `/${path}`
  }

  function findFirstMenuPath(nodes: UserMenuNode[] | undefined): string {
    for (const node of nodes || []) {
      // 后端约定 menuType=M 表示菜单，且带可访问组件时可作为起始页
      if (node.menuType === 'M' && node.component) {
        const p = normalizePath(node.fullPath || node.path)
        if (p) return p
      }
      const child = findFirstMenuPath(node.children)
      if (child) return child
    }
    return ''
  }

  function setActiveModule(base: string, menus?: UserMenuNode[]) {
    activeModule.value = base || '/dashboard'
    if (!menus?.length) return

    const targetRoot = menus.find((m) => {
      const seg = (m.fullPath || m.path || '').split('/').filter(Boolean)[0] || ''
      return normalizePath(seg) === activeModule.value
    })
    if (!targetRoot) return

    const nextPath = findFirstMenuPath([targetRoot])
    if (nextPath) {
      activeTabPath.value = nextPath
    }
  }

  function setActiveTabPath(path: string) {
    activeTabPath.value = path || '/dashboard'
  }

  return {
    activeModule,
    activeTabPath,
    setActiveModule,
    setActiveTabPath,
  }
})
