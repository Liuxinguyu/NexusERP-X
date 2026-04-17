<template>
  <div class="hub-root">

    <!-- ── Hero Header ──────────────────────────────────────── -->
    <header class="hub-hero">
      <div class="hub-hero-inner">
        <div class="hub-hero-text">
          <h1 class="hub-title">系统设置控制中心</h1>
          <p class="hub-sub">统一管理身份权限、组织架构与系统基础配置，保障业务安全运转。</p>
        </div>
        <div class="hub-hero-badge">
          <el-icon><Setting /></el-icon>
          <span>{{ userStore.profile?.username || '管理员' }}</span>
        </div>
      </div>
    </header>

    <!-- ── Hub Sections ─────────────────────────────────────── -->
    <div class="hub-body">

      <!-- 身份与权限 -->
      <section class="hub-section">
        <div class="hub-section-head">
          <el-icon class="hs-icon" color="#165DFF"><Key /></el-icon>
          <h2 class="hs-title">身份与权限</h2>
          <p class="hs-desc">管理账户、角色与菜单权限的分层授权体系。</p>
        </div>
        <div class="hub-cards">
          <div
            v-for="card in identityCards"
            :key="card.path"
            class="hub-card"
            @click="router.push(card.path)"
          >
            <div class="hub-card-icon" :style="{ background: card.bg }">
              <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
            </div>
            <div class="hub-card-body">
              <h3 class="hub-card-title">{{ card.title }}</h3>
              <p class="hub-card-desc">{{ card.desc }}</p>
            </div>
            <el-icon class="hub-card-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>

      <!-- 组织架构 -->
      <section class="hub-section">
        <div class="hub-section-head">
          <el-icon class="hs-icon" color="#722ED1"><OfficeBuilding /></el-icon>
          <h2 class="hs-title">组织架构</h2>
          <p class="hs-desc">维护部门组织树与店铺网点，支持多门店统一管控。</p>
        </div>
        <div class="hub-cards">
          <div
            v-for="card in orgCards"
            :key="card.path"
            class="hub-card"
            @click="router.push(card.path)"
          >
            <div class="hub-card-icon" :style="{ background: card.bg }">
              <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
            </div>
            <div class="hub-card-body">
              <h3 class="hub-card-title">{{ card.title }}</h3>
              <p class="hub-card-desc">{{ card.desc }}</p>
            </div>
            <el-icon class="hub-card-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>

      <!-- 开发者与基座 -->
      <section class="hub-section">
        <div class="hub-section-head">
          <el-icon class="hs-icon" color="#0FC6C2"><Cpu /></el-icon>
          <h2 class="hs-title">开发者与基座</h2>
          <p class="hs-desc">系统字典、菜单路由、日志审计与消息公告等基础设施。</p>
        </div>
        <div class="hub-cards hub-cards--3">
          <div
            v-for="card in devCards"
            :key="card.path"
            class="hub-card"
            @click="router.push(card.path)"
          >
            <div class="hub-card-icon" :style="{ background: card.bg }">
              <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
            </div>
            <div class="hub-card-body">
              <h3 class="hub-card-title">{{ card.title }}</h3>
              <p class="hub-card-desc">{{ card.desc }}</p>
            </div>
            <el-icon class="hub-card-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>

    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  ArrowRight, Setting, Key, User, Stamp, OfficeBuilding, Shop,
  Tickets, Monitor, Bell, Cpu,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const identityCards = [
  {
    path: '/system/user',
    title: '用户管理',
    desc: '员工账户的增删改查、状态控制与密码重置',
    icon: 'User',
    bg: 'rgba(22,93,255,0.08)',
    color: '#165DFF',
  },
  {
    path: '/system/role',
    title: '角色权限',
    desc: '定义角色、配置菜单与数据权限边界',
    icon: 'Stamp',
    bg: 'rgba(114,46,209,0.08)',
    color: '#722ED1',
  },
]

const orgCards = [
  {
    path: '/system/org',
    title: '机构管理',
    desc: '树形组织架构，支持多层级部门与归属关系',
    icon: 'OfficeBuilding',
    bg: 'rgba(0,198,182,0.08)',
    color: '#0FC6C2',
  },
  {
    path: '/system/shop',
    title: '店铺管理',
    desc: '门店网点管理，支持多店铺数据隔离与切换',
    icon: 'Shop',
    bg: 'rgba(247,186,30,0.08)',
    color: '#F7BA1E',
  },
]

const devCards = [
  {
    path: '/system/online-user',
    title: '在线用户',
    desc: '实时会话监控与强制下线',
    icon: 'Monitor',
    bg: 'rgba(22,93,255,0.08)',
    color: '#165DFF',
  },
  {
    path: '/system/login-log',
    title: '登录日志',
    desc: '登录审计轨迹与异常IP追踪',
    icon: 'Tickets',
    bg: 'rgba(114,46,209,0.08)',
    color: '#722ED1',
  },
  {
    path: '/system/notice',
    title: '通知公告',
    desc: '系统公告发布、置顶与过期管理',
    icon: 'Bell',
    bg: 'rgba(245,63,63,0.08)',
    color: '#F53F3F',
  },
]
</script>

<style scoped>
/* ── Root ───────────────────────────────────────────────────── */
.hub-root {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 40px;
}

/* ── Hero ───────────────────────────────────────────────────── */
.hub-hero {
  background: linear-gradient(135deg, #EFF6FF 0%, #F5F3FF 100%);
  border-radius: 28px;
  border: 1px solid rgba(22, 93, 255, 0.1);
  padding: 36px 40px;
}

.hub-hero-inner {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.hub-title {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--text-primary);
  line-height: 1.15;
}

.hub-sub {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  max-width: 480px;
}

.hub-hero-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: #fff;
  border-radius: 40px;
  border: 1px solid var(--border-color-soft);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  flex-shrink: 0;
}

/* ── Hub body ──────────────────────────────────────────────── */
.hub-body {
  display: flex;
  flex-direction: column;
  gap: 48px;
}

/* ── Section ───────────────────────────────────────────────── */
.hub-section-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.hs-icon {
  font-size: 28px;
  padding: 6px;
  background: var(--card-bg);
  border-radius: 12px;
  border: 1px solid var(--border-color-soft);
}

.hs-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.hs-desc {
  margin: 0 0 0 auto;
  font-size: 13px;
  color: var(--text-muted);
  max-width: 320px;
  text-align: right;
  line-height: 1.5;
}

/* ── Hub Cards ─────────────────────────────────────────────── */
.hub-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.hub-cards--3 {
  grid-template-columns: repeat(3, 1fr);
}

.hub-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: var(--card-bg);
  border-radius: 20px;
  border: 1px solid var(--border-color-soft);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.hub-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: var(--color-primary);
}

.hub-card-icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hub-card-body {
  flex: 1;
  min-width: 0;
}

.hub-card-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.hub-card-desc {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hub-card-arrow {
  font-size: 16px;
  color: var(--text-muted);
  flex-shrink: 0;
  transition: color 0.2s, transform 0.2s;
}

.hub-card:hover .hub-card-arrow {
  color: var(--color-primary);
  transform: translateX(3px);
}

/* ── Responsive ─────────────────────────────────────────────── */
@media (max-width: 900px) {
  .hub-cards { grid-template-columns: 1fr; }
  .hub-cards--3 { grid-template-columns: 1fr; }
  .hub-hero-inner { flex-direction: column; }
  .hs-desc { text-align: left; margin-left: 0; }
}
</style>
