<template>
  <div class="overview">
    <section class="hero">
      <div class="hero-left">
        <p class="kicker">System Online • NexusERP</p>
        <h1>你好，{{ userStore.profile?.realName || userStore.profile?.username || '管理员' }}</h1>
        <p class="desc">今天也保持高效协作，关键业务指标如下。</p>
      </div>
      <div class="hero-right">
        <el-tag type="success" effect="light">当前店铺：{{ userStore.currentShop?.shopName || '未选择' }}</el-tag>
      </div>
    </section>

    <section class="bento">
      <el-card class="bento-big" shadow="never">
        <template #header>核心指标</template>
        <div class="metrics">
          <div class="metric">
            <div class="label">今日销售额</div>
            <div class="value">¥{{ formatMoney(summary.todaySaleAmount) }}</div>
          </div>
          <div class="metric">
            <div class="label">本月采购额</div>
            <div class="value">¥{{ formatMoney(summary.monthlyPurchaseAmount) }}</div>
          </div>
          <div class="metric">
            <div class="label">库存预警</div>
            <div class="value" :class="{ danger: summary.stockAlarmCount > 0 }">{{ summary.stockAlarmCount }}</div>
          </div>
          <div class="metric">
            <div class="label">待审批</div>
            <div class="value">{{ summary.pendingApprovalCount }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="bento-side" shadow="never">
        <template #header>快捷入口</template>
        <div class="quick-list">
          <button
            v-for="item in quickMenus"
            :key="item.path"
            class="quick-item"
            @click="router.push(item.path)"
          >
            <el-icon><component :is="item.icon || 'Document'" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>客户与供应商</template>
        <div class="mini-stats">
          <div><strong>{{ summary.customerCount }}</strong><span>客户</span></div>
          <div><strong>{{ summary.supplierCount }}</strong><span>供应商</span></div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>系统状态</template>
        <div class="status-grid">
          <div class="status ok">Gateway 8080</div>
          <div class="status ok">System 8081</div>
          <div class="status ok">ERP 8082</div>
          <div class="status ok">Auth 8085</div>
        </div>
      </el-card>
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
    // keep fallback values
  }
})
</script>

<style scoped>
.overview { display: flex; flex-direction: column; gap: 16px; }

.hero {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kicker { color: var(--text-muted); font-size: 12px; margin-bottom: 4px; }
h1 { font-size: 30px; margin: 0 0 6px; }
.desc { color: var(--text-secondary); margin: 0; }

.bento {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.bento > :nth-child(3),
.bento > :nth-child(4) {
  grid-column: span 1;
}

.bento-big { min-height: 220px; }
.bento-side { min-height: 220px; }

.metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.metric { background: #f8fafc; border-radius: var(--radius-sm); padding: 14px; }
.metric .label { color: var(--text-secondary); font-size: 12px; margin-bottom: 4px; }
.metric .value { font-size: 24px; font-weight: 800; }
.metric .value.danger { color: var(--color-danger); }

.quick-list { display: grid; grid-template-columns: 1fr; gap: 8px; }
.quick-item {
  border: 1px solid var(--border-color);
  background: #fff;
  border-radius: 12px;
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  text-align: left;
}
.quick-item:hover { background: #eef2ff; border-color: #c7d2fe; }

.mini-stats { display: flex; gap: 16px; }
.mini-stats > div { background: #f8fafc; border-radius: 12px; padding: 12px 14px; min-width: 120px; }
.mini-stats strong { display: block; font-size: 24px; }
.mini-stats span { color: var(--text-secondary); font-size: 12px; }

.status-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.status { border-radius: 10px; padding: 10px 12px; font-size: 12px; font-weight: 700; }
.status.ok { background: #dcfce7; color: #166534; }

@media (max-width: 1100px) {
  .bento { grid-template-columns: 1fr; }
}
</style>
