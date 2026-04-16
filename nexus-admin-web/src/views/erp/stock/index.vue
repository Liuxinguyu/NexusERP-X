<template>
  <div class="page-container">
    <NexusSearchCard>
      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="产品名称">
          <el-input v-model="queryParams.productName" placeholder="请输入产品名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="所属仓库">
          <el-select v-model="queryParams.warehouseId" placeholder="请选择仓库" clearable filterable style="width: 220px">
            <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.warehouseName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #actions>
        <div class="search-actions">
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
        </div>
      </template>
    </NexusSearchCard>

    <NexusTableCard v-model:current="queryParams.current" v-model:size="queryParams.size" :loading="loading" :total="total" @pagination-change="loadData">
      <el-table :data="displayData" height="100%">
        <el-table-column label="产品图片" width="100" align="center">
          <template #default>
            <div class="image-placeholder">—</div>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="产品名称" min-width="180" />
        <el-table-column label="规格型号" min-width="140">
          <template #default>
            <span class="muted-text">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="所属仓库" min-width="160" />
        <el-table-column label="当前库存" width="180">
          <template #default="{ row }">
            <div class="qty-cell">
              <span :class="['qty-value', { danger: row.lowStock }]">{{ row.qty ?? row.quantity ?? 0 }}</span>
              <el-tag v-if="row.lowStock" type="danger" effect="light" size="small">低库存</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单位" width="100">
          <template #default>
            <span class="muted-text">—</span>
          </template>
        </el-table-column>
      </el-table>
    </NexusTableCard>

    <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />
    <div v-if="!loading" class="page-tip">产品名称筛选基于当前页已加载数据。</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { erpApi, type ErpStock, type ErpWarehouse } from '@/api/erp'
import NexusSearchCard from '@/components/NexusSearchCard/index.vue'
import NexusTableCard from '@/components/NexusTableCard/index.vue'
import RequestErrorState from '@/components/RequestErrorState.vue'

type StockTableRow = ErpStock & {
  lowStock: boolean
}

type StockAlarmRow = {
  productId?: number
  warehouseName?: string
  currentQty?: number
  minStock?: number
}

const loading = ref(false)
const total = ref(0)
const errorMsg = ref('')
const rawTableData = ref<ErpStock[]>([])
const warehouseOptions = ref<ErpWarehouse[]>([])
const alarmMap = ref(new Map<string, boolean>())

const queryParams = reactive({
  current: 1,
  size: 10,
  productName: '',
  warehouseId: undefined as number | undefined,
})

const displayData = computed<StockTableRow[]>(() => {
  const keyword = queryParams.productName.trim().toLowerCase()
  return rawTableData.value
    .filter((item) => !keyword || item.productName.toLowerCase().includes(keyword))
    .map((item) => ({
      ...item,
      lowStock: alarmMap.value.get(buildAlarmKey(item.productId, item.warehouseName)) ?? false,
    }))
})

function buildAlarmKey(productId?: number, warehouseName?: string) {
  return `${productId ?? ''}::${warehouseName ?? ''}`
}

async function loadWarehouseOptions() {
  const res = await erpApi.getWarehousePage({ current: 1, size: 1000 })
  warehouseOptions.value = (res.records ?? res.list ?? []) as ErpWarehouse[]
}

async function loadAlarmMap() {
  try {
    const res = await erpApi.getStockAlarm()
    const list = Array.isArray(res) ? res : Array.isArray(res?.records) ? res.records : []
    const nextMap = new Map<string, boolean>()
    ;(list as StockAlarmRow[]).forEach((item) => {
      const currentQty = Number(item.currentQty ?? 0)
      const minStock = Number(item.minStock ?? 0)
      if (item.productId != null && item.warehouseName && currentQty <= minStock) {
        nextMap.set(buildAlarmKey(item.productId, item.warehouseName), true)
      }
    })
    alarmMap.value = nextMap
  } catch {
    alarmMap.value = new Map<string, boolean>()
  }
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getStockPage({
      current: queryParams.current,
      size: queryParams.size,
      warehouseId: queryParams.warehouseId,
    })
    rawTableData.value = (res.records ?? res.list ?? []) as ErpStock[]
    total.value = res.total ?? 0
    if (res.current != null) queryParams.current = Number(res.current)
    if (res.size != null) queryParams.size = Number(res.size)
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '库存列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.productName = ''
  queryParams.warehouseId = undefined
  queryParams.current = 1
  loadData()
}

onMounted(async () => {
  await Promise.all([loadWarehouseOptions(), loadAlarmMap()])
  loadData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 0;
}
.search-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.image-placeholder {
  width: 40px;
  height: 40px;
  margin: 0 auto;
  border-radius: 12px;
  background: #f8fafc;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
}
.qty-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.qty-value {
  font-weight: 600;
  color: #0f172a;
}
.qty-value.danger {
  color: #dc2626;
}
.muted-text {
  color: #94a3b8;
}
.page-tip {
  margin-top: 12px;
  color: #94a3b8;
  font-size: 12px;
}
.page-container :deep(.el-table) {
  height: 100%;
}
.page-container :deep(.el-table th.el-table__cell) {
  color: #64748b;
  font-weight: 600;
}
.page-container :deep(.el-table td.el-table__cell),
.page-container :deep(.el-table th.el-table__cell) {
  border-right: none !important;
}
.page-container :deep(.el-table--border::after),
.page-container :deep(.el-table--group::after),
.page-container :deep(.el-table::before) {
  display: none;
}
.page-container :deep(.el-table tr td.el-table__cell) {
  border-bottom: 1px solid #f1f5f9;
}
</style>
