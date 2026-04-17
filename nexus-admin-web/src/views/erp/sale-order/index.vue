<template>
  <div class="page-container">
    <NexusSearchCard>
      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="订单号">
          <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
            <el-option label="草稿" :value="0" />
            <el-option label="已出库" :value="1" />
            <el-option label="已取消" :value="-1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #actions>
        <div class="search-actions">
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="openAddDrawer">新增订单</el-button>
        </div>
      </template>
    </NexusSearchCard>

    <NexusTableCard
      v-model:current="queryParams.current"
      v-model:size="queryParams.size"
      :loading="loading"
      :total="total"
      @pagination-change="loadData"
    >
      <el-table :data="tableData" @row-click="openViewDrawer">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="customerName" label="客户" min-width="160" />
        <el-table-column prop="warehouseName" label="仓库" min-width="140" />
        <el-table-column label="总金额" width="130" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" effect="light">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-cell" @click.stop>
              <el-button type="primary" link @click="openViewDrawer(row)">查看详情</el-button>
              <el-button v-if="row.status === 0" type="primary" link @click="openEditDrawer(row)">编辑</el-button>
              <el-button v-if="row.status === 0" type="primary" link @click="confirmSubmitOutbound(row)">提交出库</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </NexusTableCard>
    <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />

    <el-drawer
:append-to-body="true"       v-model="drawerVisible"
      :size="800"
      direction="rtl"
      destroy-on-close
      class="order-detail-drawer"
      :show-close="true"
      @opened="onDrawerOpened"
    >
      <template #header>
        <div class="drawer-header-inner">
          <h2 class="drawer-title">{{ drawerTitle }}</h2>
          <p v-if="activeOrder?.orderNo" class="drawer-sub">{{ activeOrder.orderNo }}</p>
        </div>
      </template>

      <div class="drawer-body">
        <template v-if="drawerType === 'view'">
          <el-descriptions :column="2" border class="order-overview">
            <el-descriptions-item label="客户">{{ activeOrder?.customerName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="仓库">{{ activeOrder?.warehouseName || activeOrder?.warehouseId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="总金额">¥{{ formatMoney(activeOrder?.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusMeta(activeOrder?.status).type" effect="light">{{ statusMeta(activeOrder?.status).label }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-table :data="detailItems" v-loading="itemsLoading" class="items-table">
            <el-table-column label="产品" min-width="180">
              <template #default="{ row }">{{ row.productName || `产品 #${row.productId}` }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="90" align="center" />
            <el-table-column label="单价" width="140" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column label="小计" width="140" align="right">
              <template #default="{ row }">¥{{ formatMoney(calcLineAmount(row)) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!itemsLoading && detailItems.length === 0" description="暂无明细数据" />
        </template>

        <template v-else>
          <el-form ref="formRef" :model="formModel" :rules="formRules" label-position="top" class="order-form">
            <div class="form-grid">
              <el-form-item label="订单号预览">
                <el-input :model-value="orderNoPreview" readonly />
              </el-form-item>
              <el-form-item label="客户" prop="customerId">
                <el-select v-model="formModel.customerId" placeholder="请选择客户" filterable>
                  <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="仓库" prop="warehouseId">
                <el-select v-model="formModel.warehouseId" placeholder="请选择仓库" filterable>
                  <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.warehouseName" :value="item.id" />
                </el-select>
              </el-form-item>
            </div>
          </el-form>

          <div class="items-toolbar">
            <el-button type="default" plain class="ghost-add-btn" @click="handleAddItem">+ 添加明细</el-button>
          </div>
          <el-table ref="editTableRef" :data="formItems" class="items-table edit-table">
            <el-table-column label="排序" width="86" align="center">
              <template #default="{ $index }">
                <div class="sort-cell drag-handle" title="拖拽排序">
                  <el-button text :disabled="$index === 0" @click="moveItemUp($index)">↑</el-button>
                  <el-button text :disabled="$index === formItems.length - 1" @click="moveItemDown($index)">↓</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="产品" min-width="220">
              <template #default="{ row, $index }">
                <el-select v-model="row.productId" filterable placeholder="请选择产品" class="inline-input" @change="onProductChange($index)">
                  <el-option v-for="item in productOptions" :key="item.id" :label="item.productName" :value="item.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="1" :controls="false" class="inline-input" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="150">
              <template #default="{ row }">
                <el-input v-model="row.unitPriceInput" class="inline-input" @keyup.enter="handleEnterOnLine" />
              </template>
            </el-table-column>
            <el-table-column label="小计金额" width="150" align="right">
              <template #default="{ row }">¥{{ formatMoney(calcEditableLineAmount(row)) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link @click="handleRemoveItem($index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <div v-if="drawerType !== 'view'" class="total-amount">订单总金额: ¥{{ formatMoney(totalAmount) }}</div>
          <div class="footer-actions">
            <el-button @click="drawerVisible = false">取消</el-button>
            <el-button v-if="drawerType !== 'view'" type="primary" :loading="saving" @click="handleSaveDraft">保存草稿</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { erpApi, type ErpSaleOrder, type SaleOrderItem } from '@/api/erp'
import NexusSearchCard from '@/components/NexusSearchCard/index.vue'
import NexusTableCard from '@/components/NexusTableCard/index.vue'
import RequestErrorState from '@/components/RequestErrorState.vue'
import Sortable, { type SortableEvent } from 'sortablejs'

const tableData = ref<ErpSaleOrder[]>([])
const loading = ref(false)
const total = ref(0)
const queryParams = reactive({
  current: 1,
  size: 10,
  orderNo: '',
  status: undefined as -1 | 0 | 1 | undefined,
})
const errorMsg = ref('')

const drawerVisible = ref(false)
const drawerType = ref<'view' | 'add' | 'edit'>('view')
const activeOrder = ref<ErpSaleOrder | null>(null)
const detailItems = ref<SaleOrderItem[]>([])
const itemsLoading = ref(false)
const saving = ref(false)

const formRef = ref<FormInstance>()
const editTableRef = ref()
const customerOptions = ref<Array<{ id: number; name: string }>>([])
const warehouseOptions = ref<Array<{ id: number; warehouseName: string }>>([])
const productOptions = ref<Array<{ id: number; productName: string; price?: number }>>([])

const formModel = reactive({
  id: undefined as number | undefined,
  customerId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  status: 0 as 0 | 1 | -1,
})

type EditableItem = {
  id?: number
  productId?: number
  productName?: string
  quantity: number
  unitPriceInput: string
}

const formItems = ref<EditableItem[]>([])
let sortableInstance: Sortable | null = null

const formRules: FormRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
}

const drawerTitle = computed(() => {
  if (drawerType.value === 'add') return '新增销售订单'
  if (drawerType.value === 'edit') return '编辑销售订单'
  return '订单详情'
})

const totalAmount = computed(() => formItems.value.reduce((sum, item) => sum + calcEditableLineAmount(item), 0))
const orderNoPreview = computed(() => {
  if (drawerType.value === 'edit' && activeOrder.value?.orderNo) return activeOrder.value.orderNo
  const date = new Date()
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const t = String(date.getTime()).slice(-6)
  return `SO-${y}${m}${d}-${t}`
})

function formatMoney(v: unknown) {
  if (v === undefined || v === null) return '0.00'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

function statusMeta(status: -1 | 0 | 1 | undefined) {
  switch (status) {
    case -1:
      return { label: '已取消', type: 'danger' as const }
    case 0:
      return { label: '草稿', type: 'info' as const }
    case 1:
      return { label: '已出库', type: 'success' as const }
    default:
      return { label: String(status ?? '—'), type: 'info' as const }
  }
}

function calcLineAmount(row: SaleOrderItem) {
  return Number(row.quantity || 0) * Number(row.unitPrice || 0)
}

function calcEditableLineAmount(row: EditableItem) {
  return Number(row.quantity || 0) * Number(row.unitPriceInput || 0)
}

function resetForm() {
  Object.assign(formModel, { id: undefined, customerId: undefined, warehouseId: undefined, status: 0 })
  formItems.value = []
}

async function ensureBaseOptions() {
  if (!customerOptions.value.length) {
    const customerRes = await erpApi.getCustomerPage({ current: 1, size: 200 })
    customerOptions.value = customerRes.records ?? customerRes.list ?? []
  }
  if (!warehouseOptions.value.length) {
    const warehouseRes = await erpApi.getWarehousePage({ current: 1, size: 200 })
    warehouseOptions.value = warehouseRes.records ?? warehouseRes.list ?? []
  }
  if (!productOptions.value.length) {
    const productRes = await erpApi.getProductPage({ current: 1, size: 500 })
    productOptions.value = (productRes.records ?? productRes.list ?? []).map((item) => ({
      id: item.id,
      productName: item.productName,
      price: (item as any).price ?? 0,
    }))
  }
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getSaleOrderPage({
      current: queryParams.current,
      size: queryParams.size,
      status: queryParams.status,
      orderNo: queryParams.orderNo.trim() || undefined,
    })
    tableData.value = (res.records ?? res.list ?? []) as ErpSaleOrder[]
    total.value = res.total ?? 0
    if (res.current != null) queryParams.current = Number(res.current)
    if (res.size != null) queryParams.size = Number(res.size)
  } catch {
    ElMessage.error('加载失败')
    errorMsg.value = '销售订单列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.orderNo = ''
  queryParams.status = undefined
  queryParams.current = 1
  loadData()
}

function openViewDrawer(row: ErpSaleOrder) {
  drawerType.value = 'view'
  activeOrder.value = row
  drawerVisible.value = true
}

async function openAddDrawer() {
  drawerType.value = 'add'
  activeOrder.value = null
  resetForm()
  await ensureBaseOptions()
  handleAddItem()
  drawerVisible.value = true
}

async function openEditDrawer(row: ErpSaleOrder) {
  drawerType.value = 'edit'
  activeOrder.value = row
  await ensureBaseOptions()
  const [detail, items] = await Promise.all([
    erpApi.getSaleOrderDetail(row.id).catch(() => row),
    erpApi.getSaleOrderItems(row.id),
  ])
  Object.assign(formModel, {
    id: row.id,
    customerId: detail.customerId,
    warehouseId: detail.warehouseId,
    status: 0,
  })
  formItems.value = items.map((item) => ({
    id: item.id,
    productId: item.productId,
    productName: item.productName,
    quantity: Number(item.quantity || 1),
    unitPriceInput: String(item.unitPrice ?? 0),
  }))
  drawerVisible.value = true
}

async function onDrawerOpened() {
  if (drawerType.value !== 'view' || !activeOrder.value?.id) return
  itemsLoading.value = true
  try {
    const [detail, items] = await Promise.all([
      erpApi.getSaleOrderDetail(activeOrder.value.id).catch(() => activeOrder.value as ErpSaleOrder),
      erpApi.getSaleOrderItems(activeOrder.value.id),
    ])
    activeOrder.value = detail
    detailItems.value = items
  } catch {
    detailItems.value = []
    ElMessage.error('加载明细失败')
  } finally {
    itemsLoading.value = false
  }
}

function handleAddItem() {
  formItems.value.push({ quantity: 1, unitPriceInput: '0' })
}

async function initSortable() {
  await nextTick()
  if (drawerType.value === 'view' || !drawerVisible.value) return
  const tableEl = editTableRef.value?.$el as HTMLElement | undefined
  const tbody = tableEl?.querySelector('.el-table__body-wrapper tbody') as HTMLElement | null
  if (!tbody || sortableInstance) return
  sortableInstance = Sortable.create(tbody, {
    animation: 150,
    handle: '.drag-handle',
    ghostClass: 'drag-ghost',
    onEnd(evt: SortableEvent) {
      const { oldIndex, newIndex } = evt
      if (oldIndex == null || newIndex == null || oldIndex === newIndex) return
      const list = [...formItems.value]
      const [moved] = list.splice(oldIndex, 1)
      list.splice(newIndex, 0, moved)
      formItems.value = list
    },
  })
}

function destroySortable() {
  if (sortableInstance) {
    sortableInstance.destroy()
    sortableInstance = null
  }
}

function moveItemUp(index: number) {
  if (index <= 0) return
  const list = [...formItems.value]
  const [curr] = list.splice(index, 1)
  list.splice(index - 1, 0, curr)
  formItems.value = list
}

function moveItemDown(index: number) {
  if (index >= formItems.value.length - 1) return
  const list = [...formItems.value]
  const [curr] = list.splice(index, 1)
  list.splice(index + 1, 0, curr)
  formItems.value = list
}

function handleRemoveItem(index: number) {
  formItems.value.splice(index, 1)
}

function handleEnterOnLine() {
  handleAddItem()
}

function onProductChange(index: number) {
  const row = formItems.value[index]
  const product = productOptions.value.find((item) => item.id === row.productId)
  row.productName = product?.productName
  row.unitPriceInput = String(product?.price ?? 0)
}

function buildRequestItems() {
  return formItems.value.map((item) => ({
    id: item.id,
    productId: item.productId,
    quantity: Number(item.quantity || 0),
    unitPrice: Number(item.unitPriceInput || 0),
  }))
}

async function handleSaveDraft() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!formItems.value.length) {
    ElMessage.warning('请至少添加一条明细')
    return
  }
  const hasInvalidItem = formItems.value.some((item) => !item.productId || Number(item.quantity) <= 0)
  if (hasInvalidItem) {
    ElMessage.warning('请确保明细产品已选择且数量大于 0')
    return
  }

  saving.value = true
  try {
    const payload = {
      id: formModel.id,
      customerId: formModel.customerId!,
      warehouseId: formModel.warehouseId!,
      status: 0,
      totalAmount: totalAmount.value,
      items: buildRequestItems(),
    }
    if (drawerType.value === 'edit' && formModel.id) {
      await erpApi.updateSaleOrder(formModel.id, payload)
      ElMessage.success('草稿更新成功')
    } else {
      await erpApi.addSaleOrder(payload)
      ElMessage.success('草稿创建成功')
    }
    drawerVisible.value = false
    loadData()
  } catch {
    ElMessage.error('保存草稿失败')
  } finally {
    saving.value = false
  }
}

async function confirmSubmitOutbound(row: ErpSaleOrder) {
  try {
    await ElMessageBox.confirm('确认提交出库吗？此操作将扣减库存并生成应收账款。', '提交出库', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await erpApi.submitDraftSaleOrder(row.id)
    ElMessage.success('提交成功')
    loadData()
  } catch {
    /* cancel */
  }
}

onMounted(() => loadData())

watch(
  () => [drawerVisible.value, drawerType.value],
  async () => {
    if (drawerVisible.value && drawerType.value !== 'view') {
      await initSortable()
      return
    }
    destroySortable()
  }
)

onBeforeUnmount(() => {
  destroySortable()
})
</script>

<style scoped>
.search-form {
  margin: 0;
}

.search-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.drawer-header-inner {
  padding: 4px 0 0;
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
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-form {
  margin-bottom: 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.items-toolbar {
  display: flex;
  justify-content: flex-start;
}

.ghost-add-btn {
  border-style: dashed;
}

.sort-cell {
  display: inline-flex;
  gap: 2px;
  cursor: grab;
}

.sort-cell:active {
  cursor: grabbing;
}

.edit-table :deep(.drag-ghost td) {
  background: #f8fafc !important;
}

.drawer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.footer-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.total-amount {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.order-overview :deep(.el-descriptions__body) {
  border-radius: 14px;
  overflow: hidden;
}

.edit-table :deep(.el-table .cell) {
  padding-left: 4px;
  padding-right: 4px;
}

.inline-input :deep(.el-input__wrapper),
.inline-input :deep(.el-select__wrapper),
.inline-input :deep(.el-input-number__wrapper) {
  box-shadow: 0 0 0 1px transparent inset !important;
  background: #fff;
}

.inline-input :deep(.el-input__wrapper:hover),
.inline-input :deep(.el-select__wrapper:hover),
.inline-input :deep(.el-input-number__wrapper:hover) {
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
}

.inline-input :deep(.el-input__wrapper.is-focus),
.inline-input :deep(.el-select__wrapper.is-focused),
.inline-input :deep(.el-input-number__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #6366f1 inset !important;
}

.items-table :deep(.el-table td.el-table__cell),
.items-table :deep(.el-table th.el-table__cell) {
  border-right: none !important;
}

.items-table :deep(.el-table::before),
.items-table :deep(.el-table--border::after) {
  display: none !important;
}

.items-table :deep(.el-table tr td.el-table__cell) {
  border-bottom: 1px solid #f1f5f9 !important;
}

.items-table :deep(.el-table th.el-table__cell) {
  color: #64748b;
  font-weight: 600;
}
</style>

<style>
.order-detail-drawer .el-drawer__header {
  margin-bottom: 0;
  padding: 28px 28px 8px;
  border-bottom: none !important;
}

.order-detail-drawer .el-drawer {
  border-top-left-radius: 20px;
  border-bottom-left-radius: 20px;
}

.order-detail-drawer .el-drawer__body {
  padding: 12px 28px 24px;
}

.order-detail-drawer .el-drawer__footer {
  padding: 12px 28px 20px;
  border-top: 1px solid #f1f5f9;
}
</style>
