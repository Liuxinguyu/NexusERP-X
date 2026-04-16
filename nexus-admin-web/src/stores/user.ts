import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, login, getUserShops, switchShop as switchShopApi, type LoginReq, type ShopTreeVO, type CurrentUserInfo, type UserMenuNode } from '@/api/auth'
import router from '@/router'

// 唯一视图源：所有本地页面都从该 glob 结果中解析
const viewModules = import.meta.glob('/src/views/**/index.vue')
const FALLBACK_VIEW_KEY = '/src/views/route-fallback/index.vue'
type ShopItem = { shopId: number; shopName: string }

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
  const token = ref<string>('')
  const userInfo = ref<CurrentUserInfo | null>(null)
  const menus = ref<UserMenuNode[]>([])
  const permissions = ref<string[]>([])
  const currentShopId = ref<number | null>(null)
  const shopTree = ref<ShopTreeVO[]>([])
  const shops = ref<ShopItem[]>([])
  const latestNotice = ref<string>('')

  const isLoggedIn = computed(() => !!getToken())
  const profile = computed(() => userInfo.value as any)
  const isShopConfirmed = computed(() => !!userInfo.value && currentShopId.value !== null)
  const currentShop = computed(() => shops.value.find((item) => item.shopId === currentShopId.value) || null)

  function getToken(): string | null {
    if (!token.value) {
      token.value = localStorage.getItem('nexus_token') || ''
    }
    return token.value || null
  }

  function unwrapToken(payload: unknown): string {
    if (typeof payload === 'string') return payload
    if (payload && typeof payload === 'object') {
      const candidate = payload as { data?: unknown; accessToken?: unknown }
      if (typeof candidate.data === 'string') return candidate.data
      if (typeof candidate.accessToken === 'string') return candidate.accessToken
    }
    return ''
  }

  // Action 1 - loginAccount(data: LoginReq)
  async function loginAccount(req: LoginReq) {
    const resp = await login(req)
    const nextToken = unwrapToken(resp)
    if (!nextToken) throw new Error('登录失败：未获取到 token')
    token.value = nextToken
    localStorage.setItem('nexus_token', nextToken)
    userInfo.value = null
    menus.value = []
    permissions.value = []
    currentShopId.value = null
    shopTree.value = []
    return nextToken
  }

  async function fetchProfileAndAcl() {
    const data = await authApi.getCurrentUserInfo()
    userInfo.value = data
    menus.value = data.menus || []
    permissions.value = data.permissions || []
    currentShopId.value = data.shopId || null
    addAccessibleRoutes()
  }

  // Action 2 - fetchAuthorizedShops()
  async function fetchAuthorizedShops() {
    const treeResp = await getUserShops()
    const tree = Array.isArray(treeResp)
      ? treeResp
      : (((treeResp as unknown as { data?: ShopTreeVO[] })?.data || []) as ShopTreeVO[])
    shopTree.value = tree || []
    const nextShops: ShopItem[] = []
    const flat = (nodes: ShopTreeVO[]) => {
      for (const node of nodes) {
        nextShops.push({ shopId: node.id, shopName: node.shopName })
        if (node.children?.length) flat(node.children)
      }
    }
    flat(shopTree.value)
    shops.value = nextShops
  }

  // Action 3 - enterSystem(shopId: number)
  async function enterSystem(shopId: number) {
    const resp = await switchShopApi(shopId)
    const nextToken = unwrapToken(resp)
    if (!nextToken) throw new Error('切换店铺失败：未获取到正式 token')
    token.value = nextToken
    localStorage.setItem('nexus_token', nextToken)
    currentShopId.value = shopId
    await fetchProfileAndAcl()
    router.push('/')
  }

  async function fetchUserInfo() {
    await fetchProfileAndAcl()
  }

  async function fetchUserShops() {
    await fetchAuthorizedShops()
  }

  async function confirmShopEntry(shopId: number, redirectPath = '/') {
    await enterSystem(shopId)
    if (redirectPath && redirectPath !== '/') router.push(redirectPath)
  }

  async function switchShop(shopId: number) {
    await enterSystem(shopId)
  }

  async function logout() {
    try { await authApi.logout() } catch {}
    token.value = ''
    userInfo.value = null
    menus.value = []
    permissions.value = []
    currentShopId.value = null
    shopTree.value = []
    shops.value = []
    localStorage.removeItem('nexus_token')
    router.push('/login')
  }

  function buildFlatRoutes(menuNodes: UserMenuNode[], modulePath = ''): any[] {
    const flat: any[] = []
    for (const node of menuNodes) {
      const name = '__dyn_' + node.menuName
      const currentModulePath =
        modulePath || (node.fullPath || node.path || '').split('/').filter(Boolean)[0] || 'dashboard'
      const normalizedModulePath = currentModulePath.startsWith('/') ? currentModulePath : `/${currentModulePath}`
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
          // 作为 Layout 的 children 挂载，避免进入独立页面时丢失侧边栏
          path: fullPath.replace(/^\/+/, '') || `menu-${node.id}`,
          name,
          component: importer,
          meta: {
            title: node.menuName,
            icon: node.icon,
            perms: node.perms,
            modulePath: normalizedModulePath,
          },
        })
      }
      if (node.children?.length) {
        // 递归处理子菜单
        flat.push(...buildFlatRoutes(node.children, normalizedModulePath))
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
    // 全部注册到 Layout 子路由下，统一走布局壳
    const flatRoutes = buildFlatRoutes(menus.value)
    for (const r of flatRoutes) {
      router.addRoute('Layout', r)
    }
  }

  return {
    token,           // 暴露原始 ref（写操作）
    getToken,        // 暴露读取方法（读操作：自动同步 localStorage）
    userInfo, currentShopId, menus, permissions,
    profile, latestNotice, shopTree, shops, currentShop,
    isLoggedIn, isShopConfirmed,
    loginAccount, fetchAuthorizedShops, enterSystem,
    loginAction: loginAccount, login: loginAccount, fetchUserInfo, fetchUserShops, fetchShops: fetchUserShops, confirmShopEntry, switchShop, logout,
    addAccessibleRoutes,
  }
})
