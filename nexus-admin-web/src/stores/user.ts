import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, confirmShop as confirmShopApi, switchShop as switchShopApi } from '@/api/auth'
import type {
  LoginReq,
  LoginResponse,
  PreAuthLoginResponse,
  ShopItem,
  UserMenuNode,
  CurrentUserInfo,
} from '@/api/auth'
import router from '@/router'

const TOKEN_KEY = 'nexus_token'
const CURRENT_SHOP_KEY = 'nexus_current_shop_id'
const SHOP_STEP_KEY = 'nexus_shop_step_ready'
const PRE_AUTH_TOKEN_KEY = 'nexus_pre_auth_token'
const PENDING_SHOPS_KEY = 'nexus_pending_shops'

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
  // 正式登录态
  const token = ref<string>('')
  const userInfo = ref<CurrentUserInfo | null>(null)
  const menus = ref<UserMenuNode[]>([])
  const permissions = ref<string[]>([])
  const currentShopId = ref<number | null>(null)
  const shops = ref<ShopItem[]>([])
  const latestNotice = ref<string>('')
  const profile = computed(() => userInfo.value?.profile ?? null)
  const currentShop = computed(() => shops.value.find((s) => s.shopId === getCurrentShopIdFromLs()) ?? null)
  const isLoggedIn = computed(() => !!getToken())

  // ── 预登录态（仅存在于选店铺阶段）───────────────────────────
  function getPreAuthToken(): string | null {
    return sessionStorage.getItem(PRE_AUTH_TOKEN_KEY)
  }

  function setPreAuthData(preAuthToken: string, shopsList: ShopItem[]) {
    sessionStorage.setItem(PRE_AUTH_TOKEN_KEY, preAuthToken)
    sessionStorage.setItem(PRE_AUTH_TOKEN_KEY, preAuthToken)
    try {
      sessionStorage.setItem(PENDING_SHOPS_KEY, JSON.stringify(shopsList))
    } catch { /* ignore */ }
  }

  function clearPreAuthData() {
    sessionStorage.removeItem(PRE_AUTH_TOKEN_KEY)
    sessionStorage.removeItem(PENDING_SHOPS_KEY)
    sessionStorage.removeItem(SHOP_STEP_KEY)
  }

  function getPendingShops(): ShopItem[] {
    try {
      const raw = sessionStorage.getItem(PENDING_SHOPS_KEY)
      if (!raw) return []
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }

  // ── 正式 JWT ────────────────────────────────────────────────
  function getToken(): string | null {
    if (!token.value) token.value = localStorage.getItem(TOKEN_KEY) || ''
    return token.value || null
  }

  function setToken(t: string) {
    token.value = t
    localStorage.setItem(TOKEN_KEY, t)
  }

  function getCurrentShopIdFromLs(): number | null {
    if (currentShopId.value !== null) return currentShopId.value
    const raw = localStorage.getItem(CURRENT_SHOP_KEY)
    if (!raw || raw === 'null' || raw === 'undefined') return null
    const parsed = Number(raw)
    if (!Number.isFinite(parsed)) return null
    currentShopId.value = parsed
    return parsed
  }

  function setCurrentShopId(shopId: number | null) {
    currentShopId.value = shopId
    if (shopId === null) {
      localStorage.removeItem(CURRENT_SHOP_KEY)
    } else {
      localStorage.setItem(CURRENT_SHOP_KEY, String(shopId))
    }
  }

  function isShopStepReady() {
    return sessionStorage.getItem(SHOP_STEP_KEY) === '1'
  }

  function setShopStepReady(ready: boolean) {
    if (ready) sessionStorage.setItem(SHOP_STEP_KEY, '1')
    else sessionStorage.removeItem(SHOP_STEP_KEY)
  }

  // ── Step 1 预登录 ──────────────────────────────────────────
  async function preAuthLogin(req: LoginReq): Promise<PreAuthLoginResponse> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const resp = await (authApi.loginPreAuth(req) as any) as PreAuthLoginResponse
    setPreAuthData(resp.preAuthToken, resp.shops ?? [])
    setShopStepReady(true)
    return resp
  }

  // ── 读取预登录店铺（写 shopTree）────────────────────────────
  function loadPendingShopsIntoTree() {
    const list = getPendingShops()
    const tree = buildShopTree(list)
    ;(shops as any).value = list
    ;(shopTree as any).value = tree
  }

  // 后端返回的是扁平 shopItem[]，转成 el-tree-select 需要的树形
  function buildShopTree(list: ShopItem[]): any[] {
    const map = new Map<number, any>()
    const roots: any[] = []
    for (const s of list) {
      map.set(s.shopId, { id: s.shopId, shopName: s.shopName, children: [] })
    }
    for (const s of list) {
      const node = map.get(s.shopId)!
      // ShopItem 无 parentId，视为平铺，均为根节点
      roots.push(node)
    }
    return roots
  }

  // ── Step 2 确认店铺 ─────────────────────────────────────────
  async function confirmShopEntry(shopId: number): Promise<void> {
    const preAuthToken = getPreAuthToken()
    if (!preAuthToken) throw new Error('预登录态已失效，请重新登录')

    const resp: LoginResponse = await confirmShopApi({ preAuthToken, shopId })

    setToken(resp.accessToken)
    setCurrentShopId(resp.currentShopId)
    clearPreAuthData()
    userInfo.value = null
    menus.value = []
    permissions.value = []
    shops.value = []
    await fetchProfileAndAcl()
  }

  // ── 已登录后切换店铺 ────────────────────────────────────────
  async function switchShop(shopId: number) {
    const resp: LoginResponse = await switchShopApi(shopId)
    setToken(resp.accessToken)
    setCurrentShopId(resp.currentShopId)
    userInfo.value = null
    menus.value = []
    await fetchProfileAndAcl()
  }

  // ── 获取用户信息并构建路由 ──────────────────────────────────
  async function fetchProfileAndAcl() {
    const data = await (authApi.getCurrentUserInfo() as any) as CurrentUserInfo
    userInfo.value = data
    // 后端返回 { profile, menus, latestNoticeTitle }
    menus.value = data.menus ?? []
    latestNotice.value = data.latestNoticeTitle ?? ''
    setCurrentShopId(data.profile?.currentShopId ?? null)
    addAccessibleRoutes()
  }

  function fetchUserInfo() { return fetchProfileAndAcl() }
  function fetchUserShops() { /* 已登录后的店铺从 profile.accessibleShopIds 读，不再单独拉 */ }

  // ── 登出 ───────────────────────────────────────────────────
  async function logout() {
    try { await authApi.logout() } catch { /* ignore */ }
    token.value = ''
    userInfo.value = null
    menus.value = []
    permissions.value = []
    setCurrentShopId(null)
    clearPreAuthData()
    shops.value = []
    localStorage.removeItem(TOKEN_KEY)
    router.push('/login')
  }

  // ── 动态路由 ────────────────────────────────────────────────
  function buildFlatRoutes(menuNodes: UserMenuNode[], modulePath = ''): any[] {
    const flat: any[] = []
    for (const node of menuNodes) {
      const name = '__dyn_' + node.menuName
      const seg = (node.fullPath || node.path || '').split('/').filter(Boolean)[0] || 'dashboard'
      const normalizedModulePath = seg.startsWith('/') ? seg : `/${seg}`
      if (node.component) {
        const fullPath = node.fullPath ||
          (node.path ? (node.path.startsWith('/') ? node.path : `/${node.path}`) : `/menu-${node.id}`)
        const viewKey = normalizeMenuComponentToViewKey(node.component)
        const importer = viewModules[viewKey] || viewModules[FALLBACK_VIEW_KEY]
        flat.push({
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
        flat.push(...buildFlatRoutes(node.children, normalizedModulePath))
      }
    }
    return flat
  }

  function addAccessibleRoutes() {
    const existingRoutes = router.getRoutes()
    for (const r of existingRoutes) {
      if (r.name && String(r.name).startsWith('__dyn_')) {
        try { router.removeRoute(r.name) } catch { /* ignore */ }
      }
    }
    for (const r of buildFlatRoutes(menus.value)) {
      router.addRoute('Layout', r)
    }
  }

  // shopTree 仅供 el-tree-select 渲染
  const shopTree = ref<any[]>([])

  return {
    token, userInfo, menus, permissions, currentShopId, shops, shopTree,
    latestNotice, profile, currentShop, isLoggedIn,
    getToken, getCurrentShopId: getCurrentShopIdFromLs,
    isShopStepReady, setShopStepReady,
    getPreAuthToken, getPendingShops, clearPreAuthData,
    preAuthLogin, loadPendingShopsIntoTree, confirmShopEntry,
    switchShop, fetchProfileAndAcl, fetchUserInfo, fetchUserShops, logout,
    addAccessibleRoutes,
    // 兼容已有调用
    loginAccount: preAuthLogin,
    login: preAuthLogin,
    enterSystem: confirmShopEntry,
    fetchAuthorizedShops: loadPendingShopsIntoTree,
    fetchShops: fetchUserShops,
  }
})
