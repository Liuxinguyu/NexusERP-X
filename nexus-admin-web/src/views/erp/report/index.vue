<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <el-card class="query-card" shadow="never">
      <el-row :gutter="16" align="middle">
        <el-col :span="4">
          <el-select v-model="queryYear" style="width:100%">
            <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="queryMonth" style="width:100%">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" :loading="monthlyLoading" @click="loadMonthly">查询</el-button>
          <el-button @click="loadAll">刷新全部</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 月度汇总 -->
    <el-row :gutter="16" class="section-row" v-loading="monthlyLoading">
      <el-col :span="24">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header"><span>{{ queryYear }}年{{ queryMonth }}月 销售汇总</span></div>
          </template>
          <div v-if="monthlyData && Object.keys(monthlyData).length > 0">
            <el-descriptions :column="4" border size="small">
              <el-descriptions-item label="销售订单数">{{ monthlyData.orderCount ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="销售总额">
                <span class="text-primary text-lg">¥{{ Number(monthlyData.saleAmount ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="销售毛利">
                <span class="text-success">¥{{ Number(monthlyData.profitAmount ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="客户数">{{ monthlyData.customerCount ?? 0 }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 销售趋势 + 产品排名 -->
    <el-row :gutter="16" class="section-row">
      <!-- 销售趋势 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="section-card" v-loading="trendLoading">
          <template #header>
            <div class="card-header">
              <span>{{ queryYear }}年 销售趋势</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
          <el-table :data="trendData" stripe size="small" :show-header="trendData.length === 0">
            <el-table-column prop="month" label="月份" width="120" />
            <el-table-column prop="orderCount" label="订单数" width="120" align="center" />
            <el-table-column label="销售金额" width="160" align="right">
              <template #default="{ row }">¥{{ (row.saleAmount ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</template>
            </el-table-column>
            <el-table-column label="毛利" width="160" align="right">
              <template #default="{ row }">¥{{ (row.profitAmount ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="trendData.length === 0 && !trendLoading" description="暂无趋势数据" />
        </el-card>
      </el-col>

      <!-- 产品排名 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="section-card" v-loading="rankLoading">
          <template #header>
            <div class="card-header">
              <span>产品销售排名 TOP10</span>
            </div>
          </template>
          <div ref="rankChartRef" class="chart-container-sm"></div>
          <el-table :data="productRankData" stripe size="small" :show-header="productRankData.length === 0">
            <el-table-column type="index" label="排名" width="50" align="center" />
            <el-table-column prop="productName" label="产品名称" min-width="120" />
            <el-table-column prop="saleQuantity" label="销量" width="80" align="center">
              <template #default="{ row }"><span class="text-primary">{{ row.saleQuantity ?? 0 }}</span></template>
            </el-table-column>
            <el-table-column label="销售额" width="110" align="right">
              <template #default="{ row }">¥{{ (row.saleAmount ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 0 }) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="productRankData.length === 0 && !rankLoading" description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 库存汇总 -->
    <el-row :gutter="16" class="section-row">
      <el-col :span="24">
        <el-card shadow="never" class="section-card" v-loading="stockLoading">
          <template #header>
            <div class="card-header">
              <span>库存汇总</span>
            </div>
          </template>
          <div v-if="stockSummary && Object.keys(stockSummary).length > 0">
            <el-descriptions :column="4" border size="small">
              <el-descriptions-item label="产品种类">{{ stockSummary.productCount ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="仓库数量">{{ stockSummary.warehouseCount ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="库存总量">{{ stockSummary.totalQty ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="库存总值">¥{{ Number(stockSummary.totalValue ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</el-descriptions-item>
              <el-descriptions-item label="预警产品数">
                <el-tag type="danger" size="small">{{ stockSummary.alarmCount ?? 0 }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="库存周转天数">{{ stockSummary.turnoverDays ?? 0 }}天</el-descriptions-item>
            </el-descriptions>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { erpApi } from '@/api/erp'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'

const queryYear = ref(new Date().getFullYear())
const queryMonth = ref(new Date().getMonth() + 1)
const yearOptions = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i)

const monthlyData = ref<any>({})
const trendData = ref<any[]>([])
const productRankData = ref<any[]>([])
const stockSummary = ref<any>({})

const monthlyLoading = ref(false)
const trendLoading = ref(false)
const rankLoading = ref(false)
const stockLoading = ref(false)

const trendChartRef = ref<HTMLDivElement>()
const rankChartRef = ref<HTMLDivElement>()
let trendChart: ECharts | null = null
let rankChart: ECharts | null = null

function buildTrendChart(data: any[]) {
  if (!trendChartRef.value) return
  if (!trendChart) { trendChart = echarts.init(trendChartRef.value) }
  const months = data.map(d => d.month)
  const amounts = data.map(d => Number(d.saleAmount) || 0)
  const profits = data.map(d => Number(d.profitAmount) || 0)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '毛利'], top: 0, textStyle: { fontSize: 11 } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '30%', containLabel: true },
    xAxis: { type: 'category', data: months, axisLabel: { fontSize: 11, color: '#666' }, axisLine: { lineStyle: { color: '#e6e6e6' } } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#666', formatter: (v: number) => v >= 10000 ? `${(v / 10000).toFixed(0)}万` : String(v) } },
    series: [
      { name: '销售额', data: amounts, type: 'line', smooth: true, lineStyle: { width: 2 }, itemStyle: { color: '#5470c6' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(84,112,198,0.2)' }, { offset: 1, color: 'rgba(84,112,198,0.02)' }]) } },
      { name: '毛利', data: profits, type: 'line', smooth: true, lineStyle: { width: 2 }, itemStyle: { color: '#67c23a' } },
    ],
  })
}

function buildRankChart(data: any[]) {
  if (!rankChartRef.value) return
  if (!rankChart) { rankChart = echarts.init(rankChartRef.value) }
  const sorted = [...data].slice(0, 8)
  const names = sorted.map(d => d.productName)
  const values = sorted.map(d => d.saleQuantity || 0)
  rankChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '8%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value', axisLabel: { fontSize: 10, color: '#666' } },
    yAxis: { type: 'category', data: names, axisLabel: { fontSize: 10, color: '#666' } },
    series: [{
      data: values,
      type: 'bar',
      barWidth: '55%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }]),
        borderRadius: [0, 4, 4, 0],
      },
      label: { show: true, position: 'right', fontSize: 10, color: '#666', formatter: '{c}' },
    }],
  })
}

async function loadMonthly() {
  monthlyLoading.value = true
  try {
    monthlyData.value = await erpApi.getSalesMonthly(queryYear.value, queryMonth.value)
  } catch {
    monthlyData.value = {}
    ElMessage.error('加载月度汇总失败')
  } finally {
    monthlyLoading.value = false
  }
}

async function loadTrend() {
  trendLoading.value = true
  try {
    trendData.value = await erpApi.getSalesTrend(queryYear.value)
    buildTrendChart(trendData.value)
  } catch {
    trendData.value = []
    ElMessage.error('加载趋势数据失败')
  } finally {
    trendLoading.value = false
  }
}

async function loadProductRank() {
  rankLoading.value = true
  try {
    productRankData.value = await erpApi.getProductRank(10, queryYear.value, queryMonth.value)
    buildRankChart(productRankData.value)
  } catch {
    productRankData.value = []
    ElMessage.error('加载产品排名失败')
  } finally {
    rankLoading.value = false
  }
}

async function loadStockSummary() {
  stockLoading.value = true
  try {
    stockSummary.value = await erpApi.getStockSummary()
  } catch {
    stockSummary.value = {}
    ElMessage.error('加载库存汇总失败')
  } finally {
    stockLoading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadMonthly(), loadTrend(), loadProductRank(), loadStockSummary()])
}

function resizeCharts() {
  trendChart?.resize()
  rankChart?.resize()
}

onMounted(async () => {
  await nextTick()
  await loadAll()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  rankChart?.dispose()
})
</script>

<style scoped>
.page-container { padding: 0; }
.query-card { margin-bottom: 16px; }
.section-row { margin-bottom: 16px; }
.section-card { border-radius: var(--card-radius); }
.card-header { font-weight: 600; color: #303133; font-size: 14px; }
.chart-container { width: 100%; height: 220px; margin-bottom: 8px; }
.chart-container-sm { width: 100%; height: 200px; margin-bottom: 8px; }
.text-primary { color: var(--color-primary); font-weight: 600; }
.text-success { color: var(--color-success); font-weight: 600; }
.text-lg { font-size: 15px; }
</style>
