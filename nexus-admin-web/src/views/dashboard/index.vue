<template>
  <div class="dashboard">
    <section class="bento-grid">
      <div class="nexus-card hero-card">
        <div class="hero-inner">
          <div class="hero-copy">
            <p class="kicker">NexusERP-X · 工作台</p>
            <h1 class="welcome">
              你好，{{ userStore.profile?.realName || userStore.profile?.username || '管理员' }}
            </h1>
            <p class="sub">今日业务概览与关键指标。</p>
          </div>
          <div class="hero-metric">
            <span class="metric-label">今日销售额</span>
            <p class="metric-value">¥{{ formatMoney(summary.todaySaleAmount) }}</p>
            <span class="metric-hint">本月采购 ¥{{ formatMoney(summary.monthlyPurchaseAmount) }}</span>
          </div>
        </div>
      </div>

      <div class="nexus-card quick-card">
        <h2 class="card-title">快捷操作</h2>
        <div class="quick-grid">
          <button
            v-for="item in quickMenus"
            :key="item.path"
            type="button"
            class="quick-tile"
            @click="router.push(item.path)"
          >
            <el-icon :size="20"><component :is="item.icon || 'Document'" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>

      <div class="nexus-card chart-card">
        <h2 class="card-title">销售趋势</h2>
        <div class="chart-placeholder">
          <el-skeleton :rows="4" animated />
          <p class="placeholder-hint">图表数据接入后可在此展示趋势曲线</p>
        </div>
      </div>

      <div class="nexus-card chart-card">
        <h2 class="card-title">采购与库存</h2>
        <div class="chart-placeholder">
          <el-skeleton :rows="4" animated />
          <p class="placeholder-hint">采购、库存对比占位，可对接 getPurchaseChart / 库存汇总</p>
        </div>
      </div>
    </section>

    <section class="stats-strip">
      <div class="nexus-card stat-chip">
        <span class="stat-label">待审批</span>
        <strong>{{ summary.pendingApprovalCount }}</strong>
      </div>
      <div class="nexus-card stat-chip">
        <span class="stat-label">库存预警</span>
        <strong :class="{ warn: summary.stockAlarmCount > 0 }">{{ summary.stockAlarmCount }}</strong>
      </div>
      <div class="nexus-card stat-chip">
        <span class="stat-label">客户数</span>
        <strong>{{ summary.customerCount }}</strong>
      </div>
      <div class="nexus-card stat-chip">
        <span class="stat-label">供应商数</span>
        <strong>{{ summary.supplierCount }}</strong>
      </div>
      <div class="nexus-card stat-chip shop-chip">
        <span class="stat-label">当前店铺</span>
        <strong class="shop-name">{{ userStore.currentShop?.shopName || '未选择' }}</strong>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { systemApi, type DashboardSummary, type MenuNode } from '@/api/system'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const summary = reactive<DashboardSummary>({
  todaySaleAmount: 0,
  monthlyPurchaseAmount: 0,
  customerCount: 0,
  supplierCount: 0,
  pendingApprovalCount: 0,
  stockAlarmCount: 0,
})

function formatMoney(v: number) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function collectLeaf(nodes: MenuNode[] | undefined, out: Array<{ path: string; label: string; icon: string }>) {
  for (const n of nodes || []) {
    if (n.component) {
      const path = (n.fullPath || n.path || '').startsWith('/') ? (n.fullPath || n.path) : `/${n.fullPath || n.path}`
      out.push({ path, label: n.menuName, icon: n.icon })
    }
    if (n.children?.length) collectLeaf(n.children, out)
  }
}

const quickMenus = computed(() => {
  const out: Array<{ path: string; label: string; icon: string }> = []
  collectLeaf(userStore.menus, out)
  return out.slice(0, 8)
})

onMounted(async () => {
  try {
    const data = await systemApi.getDashboard()
    Object.assign(summary, data)
  } catch {
    /* 静默降级，保留 0 */
  }
})
</script>

<style scoped>
.dashboard {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.bento-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 24px;
  grid-auto-rows: minmax(120px, auto);
}

.nexus-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 24px;
  border: 1px solid var(--border-color-soft);
  box-shadow: none;
}

.hero-card {
  grid-column: span 8;
  grid-row: span 2;
  min-height: 0;
}

.quick-card {
  grid-column: span 4;
  grid-row: span 2;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chart-card {
  grid-column: span 6;
  min-height: 200px;
}

.hero-inner {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  min-height: 220px;
  gap: 24px;
}

@media (min-width: 900px) {
  .hero-inner {
    flex-direction: row;
    align-items: flex-end;
  }
}

.kicker {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  margin: 0 0 8px;
}

.welcome {
  margin: 0 0 8px;
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--text-primary);
  line-height: 1.15;
}

.sub {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.hero-metric {
  flex-shrink: 0;
  padding: 20px 24px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary-soft) 0%, #fff 100%);
  border: 1px solid var(--border-color-soft);
  min-width: min(100%, 280px);
}

.metric-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.metric-value {
  margin: 0;
  font-size: 36px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--color-primary);
  line-height: 1;
}

.metric-hint {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

.card-title {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-secondary);
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  flex: 1;
}

.quick-tile {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 14px 16px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--slate-50);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.quick-tile:hover {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.chart-placeholder {
  position: relative;
}

.placeholder-hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

.stats-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.stat-chip {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 20px;
}

.stat-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}

.stat-chip strong {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-primary);
}

.stat-chip strong.warn {
  color: var(--color-danger);
}

.shop-chip .shop-name {
  font-size: 15px;
  font-weight: 700;
  word-break: break-all;
}

@media (max-width: 1024px) {
  .hero-card,
  .quick-card {
    grid-column: span 12;
    grid-row: auto;
  }

  .chart-card {
    grid-column: span 12;
  }
}
</style>
