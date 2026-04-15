import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const activeModuleBase = ref('/dashboard')
  const activeTabPath = ref('/dashboard')

  function setActiveModuleBase(base: string) {
    activeModuleBase.value = base || '/dashboard'
  }

  function setActiveTabPath(path: string) {
    activeTabPath.value = path || '/dashboard'
  }

  return {
    activeModuleBase,
    activeTabPath,
    setActiveModuleBase,
    setActiveTabPath,
  }
})
