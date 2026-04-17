<template>
  <div class="dashboard-root">
    <!-- ── Bento Grid ─────────────────────────────────────────── -->
    <div class="bento">

      <!-- Row 1: Clock-in (4col) + Core Metrics (8col) ─────── -->

      <!-- 打卡模块 -->
      <section class="card card-clock">
        <div class="clock-header">
          <span class="clock-date">{{ todayDate }}</span>
          <span class="clock-week">{{ weekDay }}</span>
        </div>
        <div class="clock-time">{{ currentTime }}</div>
        <div class="clock-status">
          <el-icon class="status-dot" :class="clocked ? 'dot-success' : 'dot-idle'">
            <CircleCheckFilled v-if="clocked" />
            <Timer v-else />
          </el-icon>
          <span>{{ clocked ? '已打卡' : '等待打卡…' }}</span>
        </div>
        <el-button
          class="clock-btn"
          :type="clocked ? 'default' : 'primary'"
          :disabled="clocked"
          size="large"
          @click="handleClockIn"
        >
          {{ clocked ? '今日已打卡' : '立即打卡' }}
        </el-button>
      </section>

      <!-- 核心数据 -->
      <section class="card card-metrics">
        <h2 class="section-label">今日概览</h2>
        <div class="metrics-grid">
          <div class="metric-block" @click="router.push('/erp/sale-order')">
            <p class="metric-icon-wrap"><el-icon class="mi" color="#165DFF"><Tickets /></el-icon></p>
            <p class="metric-val">{{ formatMoney(summary.todaySaleAmount) }}</p>
            <p class="metric-lbl">今日销售额</p>
          </div>
          <div class="metric-block" @click="router.push('/oa/task')">
            <p class="metric-icon-wrap"><el-icon class="mi" color="#722ED1"><Stamp /></el-icon></p>
            <p class="metric-val">{{ summary.pendingApprovalCount }}</p>
            <p class="metric-lbl">待审批</p>
          </div>
          <div class="metric-block warn" @click="router.push('/erp/stock')">
            <p class="metric-icon-wrap"><el-icon class="mi" color="#F53F3F"><WarningFilled /></el-icon></p>
            <p class="metric-val">{{ summary.stockAlarmCount }}</p>
            <p class="metric-lbl">库存预警</p>
          </div>
          <div class="metric-block" @click="router.push('/erp/customer')">
            <p class="metric-icon-wrap"><el-icon class="mi" color="#0FC6C2"><User /></el-icon></p>
            <p class="metric-val">{{ summary.customerCount }}</p>
            <p class="metric-lbl">客户数</p>
          </div>
          <div class="metric-block" @click="router.push('/erp/supplier')">
            <p class="metric-icon-wrap"><el-icon class="mi" color="#F7BA1E"><OfficeBuilding /></el-icon></p>
            <p class="metric-val">{{ summary.supplierCount }}</p>
            <p class="metric-lbl">供应商数</p>
          </div>
        </div>
      </section>

      <!-- Row 2: 待办列表 (7col) + 快捷入口 (5col) ──────────── -->

      <!-- 待办列表 -->
      <section class="card card-todo">
        <div class="card-head">
          <h2 class="section-label">我的待办</h2>
          <el-button link type="primary" @click="router.push('/oa/task')">查看全部</el-button>
        </div>
        <div v-if="todoLoading" class="todo-skeleton">
          <el-skeleton v-for="i in 4" :key="i" :rows="1" animated />
        </div>
        <ul v-else-if="todoList.length" class="todo-list">
          <li v-for="item in todoList" :key="item.id" class="todo-item">
            <div class="todo-info">
              <span class="todo-title">{{ item.title }}</span>
              <el-tag size="small" :type="tagType(item.status)">{{ statusLabel(item.status) }}</el-tag>
            </div>
            <el-button size="small" type="primary" plain @click="router.push('/oa/task')">去处理</el-button>
          </li>
        </ul>
        <div v-else class="todo-empty">
          <el-icon :size="32" color="#c0c4cc"><SuccessFilled /></el-icon>
          <p>暂无待办事项</p>
        </div>
      </section>

      <!-- 快捷入口 -->
      <section class="card card-quick">
        <h2 class="section-label">快捷入口</h2>
        <div class="quick-grid">
          <button
            v-for="item in quickMenus"
            :key="item.path"
            class="quick-tile"
            @click="router.push(item.path)"
          >
            <el-icon :size="22" class="qi"><component :is="item.icon || 'Document'" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>
        <!-- 固定快捷项 -->
        <div class="quick-fixed">
          <el-button class="qf-btn" type="primary" plain @click="router.push('/oa/leave')">
            <el-icon><Memo /></el-icon>发起请假
          </el-button>
          <el-button class="qf-btn" type="success" plain @click="router.push('/erp/sale-order')">
            <el-icon><DocumentAdd /></el-icon>新建订单
          </el-button>
        </div>
      </section>

    </div>

    <!-- ── Bottom Stats Strip ────────────────────────────────── -->
    <div class="stats-strip">
      <div class="card stat-chip">
        <span class="stat-label">当月采购</span>
        <strong>¥{{ formatMoney(summary.monthlyPurchaseAmount) }}</strong>
      </div>
      <div class="card stat-chip">
        <span class="stat-label">当前店铺</span>
        <strong class="shop-name">{{ userStore.currentShop?.shopName || '—' }}</strong>
      </div>
      <div class="card stat-chip">
        <span class="stat-label">登录账号</span>
        <strong>{{ userStore.profile?.username || '—' }}</strong>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  CircleCheckFilled, Timer, Tickets, Stamp, WarningFilled,
  User, OfficeBuilding, SuccessFilled, DocumentAdd, Memo,
} from '@element-plus/icons-vue'
import { oaApi } from '@/api/oa'
import { systemApi, type DashboardSummary, type MenuNode } from '@/api/system'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// ── Clock ──────────────────────────────────────────────────────
const now = ref(new Date())
let clockTimer: ReturnType<typeof setInterval>

const pad = (n: number) => String(n).padStart(2, '0')
const currentTime = computed(() => {
  const d = now.value
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})
const todayDate = computed(() => {
  const d = now.value
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
})
const weekDay = computed(() => ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.value.getDay()])

// ── Clock-in ────────────────────────────────────────────────────
const clocked = ref(false)

async function handleClockIn() {
  try {
    await oaApi.checkIn('in')
    clocked.value = true
    ElMessage.success('打卡成功')
  } catch {
    ElMessage.error('打卡失败，请稍后重试')
  }
}

// ── Summary ────────────────────────────────────────────────────
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

// ── Todo ───────────────────────────────────────────────────────
const todoLoading = ref(false)
const todoList = ref<any[]>([])

async function loadTodo() {
  todoLoading.value = true
  try {
    const resp = await oaApi.getMyApprove(1, 5, 0)
    todoList.value = (resp?.records || []).slice(0, 5)
  } catch {
    todoList.value = []
  } finally {
    todoLoading.value = false
  }
}

function tagType(status: number) {
  return status === 0 ? 'warning' : status === 1 ? 'success' : 'info'
}
function statusLabel(status: number) {
  return status === 0 ? '待审批' : status === 1 ? '已通过' : '已驳回'
}

// ── Quick Menus ───────────────────────────────────────────────
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

// ── Mount ──────────────────────────────────────────────────────
onMounted(async () => {
  clockTimer = setInterval(() => { now.value = new Date() }, 1000)
  try {
    const data = await systemApi.getDashboard()
    Object.assign(summary, data)
  } catch { /* silent */ }
  loadTodo()
})

onUnmounted(() => clearInterval(clockTimer))
</script>

<style scoped>
/* ── Root ─────────────────────────────────────────────────── */
.dashboard-root {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ── Bento Grid ───────────────────────────────────────────── */
.bento {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  grid-auto-rows: minmax(160px, auto);
  gap: 20px;
}

/* ── Card base ─────────────────────────────────────────────── */
.card {
  background: var(--card-bg);
  border-radius: 24px;
  border: 1px solid var(--border-color-soft);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.section-label {
  margin: 0 0 16px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

/* ── Clock-in card (4 col) ─────────────────────────────────── */
.card-clock {
  grid-column: span 4;
  grid-row: span 2;
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: linear-gradient(145deg, var(--color-primary-soft) 0%, var(--card-bg) 60%);
}

.clock-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.clock-date {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.clock-week {
  font-size: 12px;
  color: var(--text-muted);
  background: var(--bg-color);
  border-radius: 8px;
  padding: 2px 8px;
}

.clock-time {
  font-size: 52px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: var(--text-primary);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.clock-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.status-dot {
  font-size: 16px;
}
.dot-success { color: #00b42a; }
.dot-idle   { color: var(--text-muted); }

.clock-btn {
  margin-top: auto;
  width: 100%;
  height: 48px;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

/* ── Metrics card (8 col) ──────────────────────────────────── */
.card-metrics {
  grid-column: span 8;
  grid-row: span 1;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0;
}

.metric-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 4px;
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}
.metric-block:hover {
  background: var(--bg-color);
  transform: translateY(-2px);
}
.metric-block.warn .metric-val { color: var(--color-danger); }

.metric-icon-wrap {
  margin: 0;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: var(--bg-color);
  display: flex;
  align-items: center;
  justify-content: center;
}
.mi { font-size: 22px; }

.metric-val {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--text-primary);
  line-height: 1;
}

.metric-lbl {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

/* ── Todo card (7 col) ─────────────────────────────────────── */
.card-todo {
  grid-column: span 7;
  grid-row: span 2;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.card-head .section-label { margin-bottom: 0; }

.todo-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border-color-soft);
  transition: background 0.15s;
}
.todo-item:last-child { border-bottom: none; }
.todo-item:hover { background: var(--bg-color); margin: 0 -12px; padding: 14px 12px; border-radius: 12px; }

.todo-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.todo-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.todo-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px 0;
  color: var(--text-muted);
  font-size: 13px;
}

/* ── Quick entry card (5 col) ──────────────────────────────── */
.card-quick {
  grid-column: span 5;
  grid-row: span 2;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  flex: 1;
}

.quick-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: 1px solid var(--border-color-soft);
  border-radius: 16px;
  background: var(--bg-color);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s, box-shadow 0.2s, color 0.2s;
}
.quick-tile:hover {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(22, 93, 255, 0.12);
}
.qi { font-size: 22px; }

.quick-fixed {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
.qf-btn {
  flex: 1;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  height: 40px;
}

/* ── Bottom stats strip ────────────────────────────────────── */
.stats-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-chip {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 24px;
  border-radius: 20px;
}

.stat-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.stat-chip strong {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.shop-name {
  word-break: break-all;
  font-size: 17px !important;
}

/* ── Responsive ─────────────────────────────────────────────── */
@media (max-width: 1100px) {
  .card-clock   { grid-column: span 12; grid-row: auto; min-height: 200px; }
  .card-metrics { grid-column: span 12; }
  .metrics-grid { grid-template-columns: repeat(3, 1fr); }
  .card-todo    { grid-column: span 12; }
  .card-quick   { grid-column: span 12; }
}

@media (max-width: 640px) {
  .bento { grid-template-columns: 1fr; }
  .card-clock, .card-metrics, .card-todo, .card-quick { grid-column: span 1; }
  .metrics-grid { grid-template-columns: repeat(2, 1fr); }
  .stats-strip { grid-template-columns: 1fr; }
}
</style>
