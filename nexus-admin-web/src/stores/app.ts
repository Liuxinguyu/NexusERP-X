import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 一级模块路径前缀，如 /dashboard、/erp、/system */
export const useAppStore = defineStore('app', () => {
  const activeModule = ref('/dashboard')
  const activeTabPath = ref('/dashboard')

  function setActiveModule(base: string) {
    activeModule.value = base || '/dashboard'
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
