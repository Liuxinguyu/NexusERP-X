<template>
  <div class="page-container dashboard-container">
    <div class="bento-grid">

      <div class="nexus-card bento-item span-4 flex-center pulse-border">
        <div class="punch-info">
          <div class="time">{{ currentTime }}</div>
          <p class="status text-slate-400 text-sm mt-1">📍 IP/GPS 定位中...</p>
        </div>
        <el-button type="primary" size="large" class="punch-btn mt-6 w-full" round>立即打卡</el-button>
      </div>

      <div class="nexus-card bento-item span-8 hero-stats">
        <div class="stat-block">
          <p class="label">今日销售额</p>
          <h2 class="value text-indigo-600">¥ 124,500</h2>
        </div>
        <div class="stat-block">
          <p class="label">待办审批</p>
          <h2 class="value text-amber-500">12</h2>
        </div>
        <div class="stat-block">
          <p class="label">异常库存</p>
          <h2 class="value text-rose-500">3</h2>
        </div>
        <div class="stat-block">
          <p class="label">在线员工</p>
          <h2 class="value text-emerald-500">45</h2>
        </div>
      </div>

      <div class="nexus-card bento-item span-7 row-span-2 scrollable-card">
        <div class="card-header flex justify-between items-center mb-4">
          <h3 class="font-bold text-lg">待办审批</h3>
          <el-button link type="primary">查看全部</el-button>
        </div>
        <div class="list-wrapper">
          <div v-for="i in 4" :key="i" class="list-item flex justify-between items-center py-3 border-b border-slate-100 last:border-0">
            <div class="info">
              <p class="font-medium text-slate-800">张三的请假申请</p>
              <p class="text-sm text-slate-500">2026-04-18 10:00 提交</p>
            </div>
            <el-button size="small" type="primary" plain>去处理</el-button>
          </div>
        </div>
      </div>

      <div class="nexus-card bento-item span-5 quick-actions">
        <h3 class="font-bold text-lg mb-4">快捷操作</h3>
        <div class="action-grid grid grid-cols-2 gap-4">
          <div class="action-btn">发起请假</div>
          <div class="action-btn">新建销售单</div>
          <div class="action-btn">新增客户</div>
          <div class="action-btn">库存盘点</div>
        </div>
      </div>

      <div class="nexus-card bento-item span-5 chart-placeholder flex-center bg-slate-50">
        <p class="text-slate-400">📈 销售趋势图表预留区</p>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const currentTime = ref('')
let timer: ReturnType<typeof setInterval>

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }, 1000)
})

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}

/* --- Bento Grid 核心 --- */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 24px;
  grid-auto-rows: minmax(140px, auto);
}

.bento-item {
  background: #fff;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 10px 30px -10px rgba(0,0,0,0.03);
  border: 1px solid rgba(0,0,0,0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.bento-item:hover {
  box-shadow: 0 20px 40px -10px rgba(0,0,0,0.08);
}

/* 跨列跨行工具类 */
.span-4 { grid-column: span 4; }
.span-5 { grid-column: span 5; }
.span-7 { grid-column: span 7; }
.span-8 { grid-column: span 8; }
.row-span-2 { grid-row: span 2; }

/* --- 内部区块细化 --- */
.flex-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.hero-stats {
  display: flex;
  justify-content: space-around;
  align-items: center;
}
.stat-block { text-align: center; }
.stat-block .label { font-size: 14px; color: #64748b; margin-bottom: 8px; }
.stat-block .value { font-size: 36px; font-weight: 800; line-height: 1; letter-spacing: -0.02em; }

.time { font-size: 42px; font-weight: 800; color: #0f172a; letter-spacing: -0.05em; line-height: 1;}

.scrollable-card {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.list-wrapper {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

.action-btn {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  font-weight: 500;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
}
.action-btn:hover {
  background: #e0e7ff;
  color: #4f46e5;
}
</style>
