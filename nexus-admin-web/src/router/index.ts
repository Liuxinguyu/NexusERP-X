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
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled' },
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

// 每次路由导航前确保用户信息已加载（刷新页面场景）
router.beforeEach(async (to) => {
  if (to.path === '/login') return
  const userStore = useUserStore()
  // 直接读 localStorage，不依赖 store.token ref（logout 后 ref 为 null，但 localStorage 仍有值）
  const storedToken = localStorage.getItem('nexus_token')
  if (!storedToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!userStore.profile) {
    try {
      await userStore.fetchUserInfo()
      await userStore.fetchShops()
      console.log('[guard] fetchUserInfo OK, menus:', userStore.menus.length)
    } catch (e: any) {
      console.log('[guard] fetchUserInfo FAILED:', e?.message || e)
      return { path: '/login' }
    }
  }
})

router.onError((err) => {
  console.error('[router]', err)
})

export default router
