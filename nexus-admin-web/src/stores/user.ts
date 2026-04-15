import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type LoginReq, type ShopItem, type CurrentUserInfo, type UserMenuNode } from '@/api/auth'
import router from '@/router'

// 唯一视图源：所有本地页面都从该 glob 结果中解析
const viewModules = import.meta.glob('/src/views/**/index.vue')
const FALLBACK_VIEW_KEY = '/src/views/route-fallback/index.vue'

function normalizeMenuComponentToViewKey(component: string): string {
  const normalized = String(component || '').trim().replace(/^\/+/, '').replace(/\.vue$/, '')
  if (!normalized) return FALLBACK_VIEW_KEY
  if (normalized.startsWith('src/views/')) return `/${normalized}.vue`
  if (normalized.startsWith('views/')) return `/src/${normalized}.vue`
  if (normalized.startsWith('/src/views/')) return `${normalized}.vue`
  if (normalized.endsWith('/index')) return `/src/views/${normalized}.vue`
  return `/src/views/${normalized}/index.vue`
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(null)
  const userInfo = ref<CurrentUserInfo | null>(null)
  const menus = ref<UserMenuNode[]>([])
  const permissions = ref<string[]>([])
  const currentShopId = ref<number | null>(null)
  const accessibleShopIds = ref<number[]>([])
  const shops = ref<ShopItem[]>([])
  const latestNotice = ref<string>('')

  const isLoggedIn = computed(() => !!getToken())
  const profile = computed(() => userInfo.value as any)
  const currentShop = computed(() => {
    if (!currentShopId.value) return null
    return shops.value.find((s) => s.shopId === currentShopId.value) ?? null
  })

  function getToken(): string | null {
    if (!token.value) {
      token.value = localStorage.getItem('nexus_token')
    }
    return token.value
  }

  async function loginAction(req: LoginReq) {
    const resp = await authApi.login(req)
    token.value = resp.accessToken
    localStorage.setItem('nexus_token', resp.accessToken)
    currentShopId.value = resp.currentShopId
    accessibleShopIds.value = resp.accessibleShopIds || []
    await fetchUserInfo()
    await fetchShops()
    return resp
  }

  async function fetchUserInfo() {
    const data = await authApi.getCurrentUserInfo()
    userInfo.value = data
    menus.value = data.menus || []
    permissions.value = data.permissions || []
    currentShopId.value = data.shopId || null
    accessibleShopIds.value = []
    addAccessibleRoutes()
  }

  async function fetchShops() {
    shops.value = await authApi.getShops()
    if (!currentShopId.value && shops.value.length) {
      currentShopId.value = shops.value[0].shopId
    }
  }

  async function switchShop(shopId: number) {
    const resp = await authApi.switchShop(shopId)
    token.value = resp.accessToken
    localStorage.setItem('nexus_token', resp.accessToken)
    currentShopId.value = resp.currentShopId
    accessibleShopIds.value = resp.accessibleShopIds || []
    await fetchUserInfo()
    await fetchShops()
  }

  async function logout() {
    try { await authApi.logout() } catch {}
    token.value = null
    userInfo.value = null
    menus.value = []
    permissions.value = []
    currentShopId.value = null
    accessibleShopIds.value = []
    shops.value = []
    localStorage.removeItem('nexus_token')
    router.push('/login')
  }

  function buildFlatRoutes(menuNodes: UserMenuNode[]): any[] {
    const flat: any[] = []
    for (const node of menuNodes) {
      const name = '__dyn_' + node.menuName
      if (node.component) {
        // fullPath 为空时给一个稳定兜底，避免 addRoute 时报错
        const fullPath = node.fullPath || (node.path ? (node.path.startsWith('/') ? node.path : `/${node.path}`) : `/menu-${node.id}`)
        const viewKey = normalizeMenuComponentToViewKey(node.component)
        const hasView = !!viewModules[viewKey]
        if (!hasView) {
          console.warn(
            `[Router] 视图映射失败: 菜单名称为 ${node.menuName}, 预期的 component 路径为 ${viewKey}`
          )
        }
        const importer = viewModules[viewKey] || viewModules[FALLBACK_VIEW_KEY]
        flat.push({
          path: fullPath, // '/system/user' — 完整绝对路径
          name,
          component: importer,
          meta: {
            title: node.menuName,
            icon: node.icon,
            perms: node.perms,
          },
        })
      }
      if (node.children?.length) {
        // 递归处理子菜单
        flat.push(...buildFlatRoutes(node.children))
      }
    }
    return flat
  }

  function addAccessibleRoutes() {
    // 清除旧的动态路由
    const existingRoutes = router.getRoutes()
    for (const r of existingRoutes) {
      if (r.name && String(r.name).startsWith('__dyn_')) {
        try { router.removeRoute(r.name) } catch { /* ignore */ }
      }
    }
    // 全部平铺注册（每个路由用完整路径，如 /system/user）
    const flatRoutes = buildFlatRoutes(menus.value)
    for (const r of flatRoutes) {
      router.addRoute(r)
    }
  }

  return {
    token,           // 暴露原始 ref（写操作）
    getToken,        // 暴露读取方法（读操作：自动同步 localStorage）
    userInfo, currentShopId, accessibleShopIds, menus, permissions,
    profile, latestNotice, shops, currentShop,
    isLoggedIn,
    loginAction, login: loginAction, fetchUserInfo, fetchShops, switchShop, logout,
    addAccessibleRoutes,
  }
})
