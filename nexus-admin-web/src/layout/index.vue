<template>
  <div class="layout-root">
    <AppSidebar
      :menus="userStore.menus"
      :active-module="appStore.activeModule"
      @select-dashboard="handleSelectDashboard"
      @select-module="handleSelectModule"
    />

    <div class="layout-main">
      <AppTopbar
        :module-title="currentModuleTitle"
        :latest-notice="userStore.latestNotice"
        :shops="userStore.shops"
        :current-shop-name="userStore.currentShop?.shopName"
        :username="userStore.profile?.realName || userStore.profile?.username"
        @switch-shop="handleSwitchShop"
        @refresh="router.go(0)"
        @logout="userStore.logout()"
      />

      <SubTabs
        :menus="userStore.menus"
        :current-path="route.path"
        :current-module="appStore.activeModule"
        @navigate="navigateTo"
      />

      <main class="content-shell">
        <section class="content-surface">
          <router-view v-slot="{ Component }">
            <transition name="page-fade" mode="out-in">
              <component :is="Component" :key="route.fullPath" />
            </transition>
          </router-view>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import AppSidebar from './components/AppSidebar.vue'
import AppTopbar from './components/AppTopbar.vue'
import SubTabs from './components/SubTabs.vue'

const userStore = useUserStore()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()

function resolveModuleBase(path: string): string {
  const parts = path.split('/').filter(Boolean)
  if (!parts.length || parts[0] === 'dashboard') return '/dashboard'
  return `/${parts[0]}`
}

const currentModuleTitle = computed(() => {
  if (appStore.activeModule === '/dashboard') return '工作台'
  const matched = (userStore.menus || []).find((m) => {
    const root = (m.fullPath || m.path || '').split('/').filter(Boolean)[0]
    return `/${root}` === appStore.activeModule
  })
  return matched?.menuName || '业务中心'
})

function navigateTo(path: string) {
  if (!path) return
  router.push(path)
}

function handleSelectDashboard() {
  appStore.setActiveModule('/dashboard')
  router.push('/dashboard')
}

function handleSelectModule(base: string) {
  appStore.setActiveModule(base)
}

function syncFromRoute(path: string) {
  const mod = resolveModuleBase(path)
  appStore.setActiveModule(mod)
  appStore.setActiveTabPath(path)
}

watch(
  () => route.path,
  (path) => {
    syncFromRoute(path)
  },
  { immediate: true }
)

async function handleSwitchShop(shopId: number) {
  try {
    await userStore.switchShop(shopId)
    ElMessage.success('店铺切换成功')
  } catch {
    ElMessage.error('切换失败')
  }
}

onMounted(async () => {
  if (userStore.getToken() && !userStore.userInfo) {
    await userStore.fetchUserInfo()
    await userStore.fetchShops()
  }
})
</script>

<style scoped>
.layout-root {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--main-bg);
}

.layout-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-shell {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 24px;
}

.content-surface {
  min-height: 100%;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
