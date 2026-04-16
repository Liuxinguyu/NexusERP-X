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
        path: 'erp/product-category',
        name: 'ErpProductCategoryLocal',
        component: () => import('@/views/erp/product-category/index.vue'),
        meta: { title: '产品分类' },
      },
      // 其他所有业务路由由 userStore.addAccessibleRoutes() 动态注册
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
  const storedToken = userStore.getToken() || localStorage.getItem('nexus_token')

  if (to.path === '/login') {
    if (!storedToken) return
    try {
      if (!userStore.isShopConfirmed) {
        await userStore.fetchUserInfo()
        await userStore.fetchUserShops()
      }
      return { path: '/' }
    } catch {
      try {
        if (!userStore.shopTree.length) await userStore.fetchUserShops()
      } catch {
        localStorage.removeItem('nexus_token')
      }
      if (to.query.step === 'shop') return
      return { path: '/login', query: { step: 'shop', redirect: String(to.query.redirect || '/') } }
    }
  }

  if (!storedToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!userStore.isShopConfirmed) {
    try {
      await userStore.fetchUserInfo()
      await userStore.fetchUserShops()
    } catch {
      try {
        if (!userStore.shopTree.length) await userStore.fetchUserShops()
      } catch {
        localStorage.removeItem('nexus_token')
        return { path: '/login', query: { redirect: to.fullPath } }
      }
      return { path: '/login', query: { step: 'shop', redirect: to.fullPath } }
    }
  }
})

router.onError((err) => {
  console.error('[router]', err)
})

export default router
