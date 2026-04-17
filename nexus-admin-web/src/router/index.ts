import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled' },
      },
      {
        path: 'system/hub',
        name: 'SystemHub',
        component: () => import('@/views/system/hub/index.vue'),
        meta: { title: '系统设置控制中心' },
      },
      {
        path: 'erp/product-category',
        name: 'ErpProductCategoryLocal',
        component: () => import('@/views/erp/product-category/index.vue'),
        meta: { title: '产品分类' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const token = userStore.getToken()
  const shopId = userStore.getCurrentShopId()
  const preAuthToken = userStore.getPreAuthToken()

  // 业务页：必须有正式 JWT + 已选店铺
  if (to.path !== '/login') {
    if (!token || shopId === null) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    return
  }

  // 在登录页：
  // 有正式 JWT + 店铺 → 已登录，直接回首页
  if (token && shopId !== null) {
    return { path: '/' }
  }

  // 有预登录票据且想进选店页 → 允许
  if (preAuthToken && to.query.step === 'shop') return

  // 其他情况 → 留在登录页第一步
})

router.onError((err) => {
  console.error('[router]', err)
})

export default router
