import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type LoginReq, type LoginResp, type ShopItem } from '@/api/auth'
import { systemApi, type UserProfile, type MenuNode } from '@/api/system'
import router from '@/router'

// 注意：Vite 的 import.meta.glob key 是以 /src 开头的真实路径，不会按 alias "@/" 返回
const viewModules = import.meta.glob('/src/views/**/index.vue')
const fallbackModule = import.meta.glob('/src/views/route-fallback/index.vue')

type RouteFallbackDiag = {
  menuName: string
  fullPath: string
  component: string
  expectedViewKey: string
}

function normalizeMenuComponentToViewKey(component: string): string {
  // 后端常见格式：views/system/user/index 或 views/erp/product/index
  const c = String(component || '').trim().replace(/^\/+/, '')
  if (!c) return '/src/views/route-fallback/index.vue'
  if (c.startsWith('views/')) {
    // views/**/index -> /src/views/**/index.vue
    return `/src/${c}.vue`
  }
  // 兼容：system/user 或 system/user/index
  if (c.endsWith('/index')) return `/src/views/${c}.vue`
  return `/src/views/${c}/index.vue`
}

export const useUserStore = defineStore('user', () => {
  // token 初始为 null，首次访问时从 localStorage 读取（防止页面刷新后 token 丢失）
  const token = ref<string | null>(null)
  const profile = ref<UserProfile | null>(null)
  const menus = ref<MenuNode[]>([])
  const latestNotice = ref<string>('')
  const shops = ref<ShopItem[]>([])
  const currentShop = ref<ShopItem | null>(null)

  const isLoggedIn = computed(() => !!getToken())

  function getToken(): string | null {
    if (!token.value) {
      token.value = localStorage.getItem('nexus_token')
    }
    return token.value
  }

  async function login(req: LoginReq) {
    const resp = await authApi.login(req)
    token.value = resp.accessToken
    localStorage.setItem('nexus_token', resp.accessToken)
    await fetchUserInfo()
    await fetchShops()
    return resp
  }

  async function fetchUserInfo() {
    const data = await systemApi.getUserInfo()
    profile.value = data.profile
    menus.value = data.menus
    latestNotice.value = data.latestNoticeTitle
    addAccessibleRoutes()
  }

  async function fetchShops() {
    shops.value = await authApi.getShops()
    if (profile.value?.currentShopId) {
      currentShop.value = shops.value.find(s => s.shopId === profile.value!.currentShopId) ?? null
    }
  }

  async function switchShop(shopId: number) {
    const resp = await authApi.switchShop(shopId)
    token.value = resp.accessToken
    localStorage.setItem('nexus_token', resp.accessToken)
    await fetchUserInfo()
    await fetchShops()
  }

  async function logout() {
    try { await authApi.logout() } catch {}
    token.value = null
    profile.value = null
    menus.value = []
    shops.value = []
    currentShop.value = null
    localStorage.removeItem('nexus_token')
    router.push('/login')
  }

  function buildFlatRoutes(menuNodes: MenuNode[]): any[] {
    const flat: any[] = []
    for (const node of menuNodes) {
      const name = '__dyn_' + node.menuName
      if (node.component) {
        // fullPath 为空时给一个稳定兜底，避免 addRoute 时报错
        const fullPath = node.fullPath || (node.path ? (node.path.startsWith('/') ? node.path : `/${node.path}`) : `/menu-${node.id}`)
        // 优先用后端 component 字段定位视图文件，避免用 fullPath 推导导致层级错位（如 /system/erp/...）
        const viewKey = normalizeMenuComponentToViewKey(node.component)
        const hasView = !!viewModules[viewKey]
        const importer = viewModules[viewKey] || fallbackModule['/src/views/route-fallback/index.vue']
        const fallbackDiag: RouteFallbackDiag | undefined = hasView
          ? undefined
          : {
              menuName: node.menuName,
              fullPath,
              component: String(node.component),
              expectedViewKey: viewKey,
            }
        flat.push({
          path: fullPath, // '/system/user' — 完整绝对路径
          name,
          component: importer,
          meta: {
            title: node.menuName,
            icon: node.icon,
            perms: node.perms,
            ...(fallbackDiag ? { __fallback: fallbackDiag } : {}),
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

  function reportMenuRouteMismatches(menuNodes: MenuNode[]) {
    if (!import.meta.env.DEV) return
    const missing: Array<{ menu: string; fullPath: string; component: string; expectedView: string }> = []
    const walk = (nodes: MenuNode[]) => {
      for (const node of nodes) {
        if (node.component) {
          const viewKey = normalizeMenuComponentToViewKey(node.component)
          if (!viewModules[viewKey]) {
            const fullPath = node.fullPath || (node.path ? (node.path.startsWith('/') ? node.path : `/${node.path}`) : `/menu-${node.id}`)
            missing.push({ menu: node.menuName, fullPath, component: String(node.component), expectedView: viewKey })
          }
        }
        if (node.children?.length) walk(node.children)
      }
    }
    walk(menuNodes)
    if (missing.length) {
      console.groupCollapsed(`[route-check] 发现 ${missing.length} 个菜单未匹配前端视图`)
      console.table(missing)
      console.groupEnd()
    }
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
    reportMenuRouteMismatches(menus.value)
    for (const r of flatRoutes) {
      router.addRoute(r)
    }
  }

  return {
    token,           // 暴露原始 ref（写操作）
    getToken,        // 暴露读取方法（读操作：自动同步 localStorage）
    profile, menus, latestNotice, shops, currentShop,
    isLoggedIn,
    login, fetchUserInfo, fetchShops, switchShop, logout,
    addAccessibleRoutes,
  }
})
