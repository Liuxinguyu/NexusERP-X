<template>
  <div class="layout-wrapper">
    <AppTopbar
      :collapsed="appStore.sidebarCollapsed"
      :module-title="currentModuleTitle"
      :latest-notice="userStore.latestNotice"
      :shops="userStore.shops"
      :current-shop-name="userStore.currentShop?.shopName"
      :username="userStore.profile?.realName || userStore.profile?.username"
      @toggle-sidebar="appStore.toggleSidebar()"
      @switch-shop="handleSwitchShop"
      @refresh="router.go(0)"
      @logout="userStore.logout()"
    />

    <div class="body-wrapper">
      <AppSidebar
        :menus="userStore.menus"
        :collapsed="appStore.sidebarCollapsed"
        :current-path="route.path"
        @navigate="navigateTo"
      />

      <main class="content">
        <SubTabs
          :menus="userStore.menus"
          :current-path="route.path"
          :current-module-base="currentModuleBase"
          @navigate="navigateTo"
        />

        <PageShell>
          <router-view v-slot="{ Component }">
            <keep-alive><component :is="Component" /></keep-alive>
          </router-view>
        </PageShell>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import AppSidebar from './components/AppSidebar.vue'
import AppTopbar from './components/AppTopbar.vue'
import SubTabs from './components/SubTabs.vue'
import PageShell from './components/PageShell.vue'

const userStore = useUserStore()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()

const currentModuleBase = computed(() => {
  const parts = route.path.split('/').filter(Boolean)
  if (!parts.length || parts[0] === 'dashboard') return '/dashboard'
  return `/${parts[0]}`
})

const currentModuleTitle = computed(() => {
  if (currentModuleBase.value === '/dashboard') return '工作台'
  for (const m of userStore.menus) {
    const p = `/${(m.fullPath || m.path || '').split('/').filter(Boolean)[0] || ''}`
    if (p === currentModuleBase.value) return m.menuName
  }
  return '业务中心'
})

function navigateTo(path: string) {
  if (!path) return
  router.push(path)
}

async function handleSwitchShop(shopId: number) {
  try {
    await userStore.switchShop(shopId)
    ElMessage.success('店铺切换成功')
  } catch {
    ElMessage.error('切换失败')
  }
}

onMounted(async () => {
  if (userStore.getToken() && !userStore.profile) {
    await userStore.fetchUserInfo()
    await userStore.fetchShops()
  }
})
</script>

<style scoped>
.layout-wrapper {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.body-wrapper {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
</style>
