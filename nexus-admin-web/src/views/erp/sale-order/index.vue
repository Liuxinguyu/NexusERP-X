<template>
  <div class="page-container">
    <div class="nexus-card search-card">
      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="订单号">
          <el-input
            v-model="filterOrderNo"
            placeholder="搜索订单号"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 160px">
            <el-option label="草稿" :value="0" />
            <el-option label="待审核 / 已出库" :value="1" />
            <el-option label="已审核" :value="2" />
            <el-option label="已拒绝" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="nexus-card table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        class="sale-order-table"
        row-class-name="sale-order-row"
        @row-click="onRowClick"
      >
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="customerName" label="客户" min-width="140" show-overflow-tooltip />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" effect="plain" size="small">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-cell" @click.stop>
              <el-button type="primary" link @click="openDrawer(row)">查看明细</el-button>
              <el-button
                v-if="row.status === SALE_ORDER_STATUS.DRAFT"
                type="primary"
                size="small"
                class="submit-out-btn"
                @click="confirmSubmitOutbound(row)"
              >
                提交出库
              </el-button>
              <template v-if="row.status === SALE_ORDER_STATUS.PENDING">
                <el-button type="success" link @click="handleApprove(row)">审核通过</el-button>
                <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
              </template>
              <el-button
                v-if="row.status === SALE_ORDER_STATUS.DRAFT || row.status === SALE_ORDER_STATUS.REJECTED"
                type="danger"
                link
                @click="handleDelete(row.id)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        class="pagination"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="handleSizeChange"
      />
    </div>

    <el-drawer
      v-model="drawerVisible"
      :size="440"
      direction="rtl"
      destroy-on-close
      class="order-detail-drawer"
      :show-close="true"
      @opened="onDrawerOpened"
    >
      <template #header>
        <div class="drawer-header-inner">
          <h2 class="drawer-title">订单详情</h2>
          <p v-if="activeOrder" class="drawer-sub">{{ activeOrder.orderNo }}</p>
        </div>
      </template>

      <div v-if="activeOrder" class="drawer-body">
        <dl class="detail-dl">
          <div class="detail-row">
            <dt>客户</dt>
            <dd>{{ activeOrder.customerName || '—' }}</dd>
          </div>
          <div class="detail-row">
            <dt>仓库 ID</dt>
            <dd>{{ activeOrder.warehouseId ?? '—' }}</dd>
          </div>
          <div class="detail-row">
            <dt>总金额</dt>
            <dd class="amount">¥{{ formatMoney(activeOrder.totalAmount) }}</dd>
          </div>
          <div class="detail-row">
            <dt>状态</dt>
            <dd>
              <el-tag :type="statusMeta(activeOrder.status).type" effect="plain" size="small">
                {{ statusMeta(activeOrder.status).label }}
              </el-tag>
            </dd>
          </div>
          <div v-if="activeOrder.createTime" class="detail-row">
            <dt>创建时间</dt>
            <dd>{{ activeOrder.createTime }}</dd>
          </div>
        </dl>

        <h3 class="items-heading">明细</h3>
        <el-table
          :data="detailItems"
          v-loading="itemsLoading"
          class="items-table"
          size="small"
          :show-header="true"
        >
          <el-table-column label="产品" min-width="140">
            <template #default="{ row }">
              {{ row.productName || `产品 #${row.productId}` }}
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="72" align="center" />
          <el-table-column label="单价" width="100" align="right">
            <template #default="{ row }">¥{{ formatMoney(row.unitPrice) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!itemsLoading && detailItems.length === 0" description="暂无明细" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { erpApi, type ErpSaleOrder } from '@/api/erp'
import { SALE_ORDER_STATUS } from '@/constants/status'
import RequestErrorState from '@/components/RequestErrorState.vue'

const tableData = ref<ErpSaleOrder[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterOrderNo = ref('')
const filterStatus = ref<number | undefined>()
const errorMsg = ref('')

const drawerVisible = ref(false)
const activeOrder = ref<ErpSaleOrder | null>(null)
const detailItems = ref<any[]>([])
const itemsLoading = ref(false)

function formatMoney(v: unknown) {
  if (v === undefined || v === null) return '0.00'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

/** 展示规范：-1 已取消，0 草稿，1 已出库（与一步出库单一致；分步流程下 1 亦为待审核） */
function statusMeta(status: number | undefined) {
  switch (status) {
    case -1:
      return { label: '已取消', type: 'danger' as const }
    case 0:
      return { label: '草稿', type: 'info' as const }
    case 1:
      return { label: '已出库', type: 'success' as const }
    case 2:
      return { label: '已审核', type: 'success' as const }
    default:
      return { label: String(status ?? '—'), type: 'info' as const }
  }
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getSaleOrderPage(
      page.value,
      size.value,
      filterStatus.value,
      filterOrderNo.value.trim() || undefined
    )
    tableData.value = (res.records ?? res.list ?? []) as ErpSaleOrder[]
    total.value = res.total ?? 0
    if (res.current != null) page.value = Number(res.current)
    if (res.size != null) size.value = Number(res.size)
  } catch {
    ElMessage.error('加载失败')
    errorMsg.value = '销售订单列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  page.value = 1
  loadData()
}

function resetQuery() {
  filterOrderNo.value = ''
  filterStatus.value = undefined
  page.value = 1
  loadData()
}

function handleSizeChange() {
  page.value = 1
  loadData()
}

function openDrawer(row: ErpSaleOrder) {
  activeOrder.value = row
  drawerVisible.value = true
}

function onRowClick(row: ErpSaleOrder) {
  openDrawer(row)
}

async function onDrawerOpened() {
  if (!activeOrder.value?.id) return
  itemsLoading.value = true
  try {
    detailItems.value = await erpApi.getSaleOrderItems(activeOrder.value.id)
  } catch {
    detailItems.value = []
    ElMessage.error('加载明细失败')
  } finally {
    itemsLoading.value = false
  }
}

async function confirmSubmitOutbound(row: ErpSaleOrder) {
  try {
    await ElMessageBox.confirm('确认提交出库？提交后将进入下一流程。', '提交出库', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
      showClose: true,
      customClass: 'minimal-msgbox',
    })
    await erpApi.submitDraftSaleOrder(row.id)
    ElMessage.success('提交成功')
    loadData()
    if (activeOrder.value?.id === row.id) {
      const next = tableData.value.find((r) => r.id === row.id)
      if (next) activeOrder.value = next
    }
  } catch {
    /* cancel */
  }
}

async function handleApprove(row: ErpSaleOrder) {
  try {
    await ElMessageBox.confirm('确认审核通过？通过后将扣减库存。', '审核', { type: 'success' })
    await erpApi.approveSaleOrder(row.id)
    ElMessage.success('已通过')
    loadData()
  } catch {
    /* cancel */
  }
}

async function handleReject(row: ErpSaleOrder) {
  try {
    await ElMessageBox.confirm('确认拒绝该订单？', '拒绝', { type: 'warning' })
    await erpApi.rejectSaleOrder(row.id)
    ElMessage.success('已拒绝')
    loadData()
  } catch {
    /* cancel */
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该订单？', '提示', { type: 'warning' })
    await erpApi.deleteSaleOrder(id)
    ElMessage.success('已删除')
    drawerVisible.value = false
    loadData()
  } catch {
    /* cancel */
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container {
  padding: 16px;
}

.nexus-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 24px;
  margin-bottom: 16px;
  border: 1px solid var(--border-color-soft);
}

.search-form {
  margin: 0;
}

.table-card {
  padding-bottom: 16px;
}

.sale-order-table {
  width: 100%;
}

.sale-order-row {
  cursor: pointer;
}

.action-cell {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.submit-out-btn {
  border-radius: 12px !important;
  font-weight: 600;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.drawer-header-inner {
  padding: 4px 0 8px;
}

.drawer-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.drawer-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.drawer-body {
  padding-top: 8px;
}

.detail-dl {
  margin: 0;
  padding: 0 0 8px;
}

.detail-row {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color-soft);
  font-size: 14px;
}

.detail-row dt {
  margin: 0;
  color: var(--text-muted);
  font-weight: 600;
}

.detail-row dd {
  margin: 0;
  color: var(--text-primary);
}

.detail-row .amount {
  font-weight: 800;
  font-size: 16px;
}

.items-heading {
  margin: 20px 0 12px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.02em;
}

.items-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.items-table :deep(.el-table__cell) {
  border-bottom: 1px solid var(--border-color-soft) !important;
}
</style>

<style>
.order-detail-drawer .el-drawer__header {
  margin-bottom: 0;
  padding: 24px 24px 0;
  border-bottom: none !important;
}

.order-detail-drawer .el-drawer__body {
  padding: 8px 24px 32px;
}

.minimal-msgbox.el-message-box {
  border-radius: 16px;
  padding-bottom: 12px;
}
</style>
