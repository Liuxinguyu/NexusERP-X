<template>
  <div class="page-container">
    <NexusSearchCard>
      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="订单状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
            <el-option label="草稿" :value="PURCHASE_ORDER_STATUS.DRAFT" />
            <el-option label="待审核" :value="PURCHASE_ORDER_STATUS.PENDING" />
            <el-option label="已通过" :value="PURCHASE_ORDER_STATUS.APPROVED" />
            <el-option label="已拒绝" :value="PURCHASE_ORDER_STATUS.REJECTED" />
            <el-option label="已入库" :value="PURCHASE_ORDER_STATUS.INBOUNDED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #actions>
        <div class="search-actions">
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="openAddDrawer">新增采购单</el-button>
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
        <el-table-column prop="supplierName" label="供应商" min-width="160">
          <template #default="{ row }">{{ row.supplierName || `供应商 #${row.supplierId}` }}</template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" min-width="140">
          <template #default="{ row }">{{ row.warehouseName || `仓库 #${row.warehouseId}` }}</template>
        </el-table-column>
        <el-table-column label="总金额" width="130" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" effect="light">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-cell" @click.stop>
              <el-button type="primary" link @click="openViewDrawer(row)">查看详情</el-button>
              <el-button v-if="canEdit(row.status)" type="primary" link @click="openEditDrawer(row)">编辑</el-button>
              <el-button v-if="row.status === PURCHASE_ORDER_STATUS.DRAFT" type="primary" link @click="handleSubmit(row)">提交</el-button>
              <el-button v-if="row.status === PURCHASE_ORDER_STATUS.APPROVED" type="primary" link @click="handleConfirmInbound(row)">确认入库</el-button>
              <el-button v-if="row.status === PURCHASE_ORDER_STATUS.PENDING" type="success" link @click="handleApprove(row)">审核</el-button>
              <el-button v-if="row.status === PURCHASE_ORDER_STATUS.PENDING" type="danger" link @click="handleReject(row)">拒绝</el-button>
              <el-button v-if="canEdit(row.status)" type="danger" link @click="handleDelete(row.id)">删除</el-button>
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
      class="purchase-order-drawer"
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
            <el-descriptions-item label="供应商">{{ activeOrder?.supplierName || `供应商 #${activeOrder?.supplierId ?? '—'}` }}</el-descriptions-item>
            <el-descriptions-item label="仓库">{{ activeOrder?.warehouseName || `仓库 #${activeOrder?.warehouseId ?? '—'}` }}</el-descriptions-item>
            <el-descriptions-item label="总金额">¥{{ formatMoney(activeOrder?.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusMeta(activeOrder?.status).type" effect="light">{{ statusMeta(activeOrder?.status).label }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ activeOrder?.remark || '—' }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="detailItems" v-loading="itemsLoading" class="items-table">
            <el-table-column label="产品" min-width="180">
              <template #default="{ row }">{{ row.productName || `产品 #${row.productId}` }}</template>
            </el-table-column>
            <el-table-column label="数量" width="100" align="center">
              <template #default="{ row }">{{ getItemQuantity(row) }}</template>
            </el-table-column>
            <el-table-column label="单价" width="140" align="right">
              <template #default="{ row }">¥{{ formatMoney(getItemUnitPrice(row)) }}</template>
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
              <el-form-item label="供应商" prop="supplierId">
                <el-select v-model="formModel.supplierId" placeholder="请选择供应商" filterable>
                  <el-option v-for="item in supplierOptions" :key="item.id" :label="item.supplierName" :value="item.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="仓库" prop="warehouseId">
                <el-select v-model="formModel.warehouseId" placeholder="请选择仓库" filterable>
                  <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.warehouseName" :value="item.id" />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item label="备注">
              <el-input v-model="formModel.remark" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入备注" />
            </el-form-item>
          </el-form>

          <div class="items-toolbar">
            <el-button type="default" plain class="ghost-add-btn" @click="handleAddItem">+ 添加明细</el-button>
          </div>
          <el-table :data="formItems" class="items-table edit-table">
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
          <div v-if="drawerType !== 'view'" class="total-amount">采购总金额: ¥{{ formatMoney(totalAmount) }}</div>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { erpApi, type ErpProductInfo, type ErpPurchaseOrder, type ErpSupplier, type ErpWarehouse, type PurchaseOrderItem } from '@/api/erp'
import { PURCHASE_ORDER_STATUS } from '@/constants/status'
import NexusSearchCard from '@/components/NexusSearchCard/index.vue'
import NexusTableCard from '@/components/NexusTableCard/index.vue'
import RequestErrorState from '@/components/RequestErrorState.vue'

const tableData = ref<ErpPurchaseOrder[]>([])
const loading = ref(false)
const total = ref(0)
const errorMsg = ref('')

const queryParams = reactive({
  current: 1,
  size: 10,
  status: undefined as number | undefined,
})

const drawerVisible = ref(false)
const drawerType = ref<'view' | 'add' | 'edit'>('view')
const activeOrder = ref<ErpPurchaseOrder | null>(null)
const detailItems = ref<PurchaseOrderItem[]>([])
const itemsLoading = ref(false)
const saving = ref(false)

const supplierOptions = ref<ErpSupplier[]>([])
const warehouseOptions = ref<ErpWarehouse[]>([])
const productOptions = ref<Array<Pick<ErpProductInfo, 'id' | 'productName' | 'price'>>>([])

const formRef = ref<FormInstance>()
const formModel = reactive({
  id: undefined as number | undefined,
  supplierId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  remark: '',
  status: PURCHASE_ORDER_STATUS.DRAFT as number,
})

type EditableItem = {
  id?: number
  productId?: number
  productName?: string
  quantity: number
  unitPriceInput: string
}

const formItems = ref<EditableItem[]>([])

const formRules: FormRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
}

const drawerTitle = computed(() => {
  if (drawerType.value === 'add') return '新增采购单'
  if (drawerType.value === 'edit') return '编辑采购单'
  return '采购单详情'
})

const totalAmount = computed(() => formItems.value.reduce((sum, item) => sum + calcEditableLineAmount(item), 0))
const orderNoPreview = computed(() => {
  if (drawerType.value === 'edit' && activeOrder.value?.orderNo) return activeOrder.value.orderNo
  const date = new Date()
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const t = String(date.getTime()).slice(-6)
  return `PO-${y}${m}${d}-${t}`
})

function formatMoney(v: unknown) {
  if (v === undefined || v === null) return '0.00'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

function statusMeta(status: number | undefined) {
  switch (status) {
    case PURCHASE_ORDER_STATUS.DRAFT:
      return { label: '草稿', type: 'info' as const }
    case PURCHASE_ORDER_STATUS.PENDING:
      return { label: '待审核', type: 'warning' as const }
    case PURCHASE_ORDER_STATUS.APPROVED:
      return { label: '已通过', type: 'success' as const }
    case PURCHASE_ORDER_STATUS.REJECTED:
      return { label: '已拒绝', type: 'danger' as const }
    case PURCHASE_ORDER_STATUS.INBOUNDED:
      return { label: '已入库', type: 'success' as const }
    default:
      return { label: String(status ?? '—'), type: 'info' as const }
  }
}

function canEdit(status: number) {
  return status === PURCHASE_ORDER_STATUS.DRAFT || status === PURCHASE_ORDER_STATUS.REJECTED
}

function getItemQuantity(item: PurchaseOrderItem) {
  return Number(item.quantity ?? item.qty ?? 0)
}

function getItemUnitPrice(item: PurchaseOrderItem) {
  return Number(item.unitPrice ?? item.price ?? 0)
}

function calcLineAmount(item: PurchaseOrderItem) {
  return getItemQuantity(item) * getItemUnitPrice(item)
}

function calcEditableLineAmount(item: EditableItem) {
  return Number(item.quantity || 0) * Number(item.unitPriceInput || 0)
}

function resetForm() {
  Object.assign(formModel, {
    id: undefined,
    supplierId: undefined,
    warehouseId: undefined,
    remark: '',
    status: PURCHASE_ORDER_STATUS.DRAFT,
  })
  formItems.value = []
}

async function ensureBaseOptions() {
  if (!supplierOptions.value.length) {
    const supplierRes = await erpApi.getSupplierPage({ current: 1, size: 200 })
    supplierOptions.value = (supplierRes.records ?? supplierRes.list ?? []) as ErpSupplier[]
  }
  if (!warehouseOptions.value.length) {
    const warehouseRes = await erpApi.getWarehousePage({ current: 1, size: 200 })
    warehouseOptions.value = (warehouseRes.records ?? warehouseRes.list ?? []) as ErpWarehouse[]
  }
  if (!productOptions.value.length) {
    const productRes = await erpApi.getProductPage({ current: 1, size: 500 })
    productOptions.value = (productRes.records ?? productRes.list ?? []).map((item) => ({
      id: item.id,
      productName: item.productName,
      price: item.price ?? 0,
    }))
  }
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getPurchaseOrderPage({
      current: queryParams.current,
      size: queryParams.size,
      status: queryParams.status,
    })
    tableData.value = (res.records ?? res.list ?? []) as ErpPurchaseOrder[]
    total.value = res.total ?? 0
    if (res.current != null) queryParams.current = Number(res.current)
    if (res.size != null) queryParams.size = Number(res.size)
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '采购单列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.status = undefined
  queryParams.current = 1
  loadData()
}

function openViewDrawer(row: ErpPurchaseOrder) {
  drawerType.value = 'view'
  activeOrder.value = row
  detailItems.value = []
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

async function openEditDrawer(row: ErpPurchaseOrder) {
  drawerType.value = 'edit'
  activeOrder.value = row
  await ensureBaseOptions()
  const [detail, items] = await Promise.all([
    erpApi.getPurchaseOrderDetail(row.id).catch(() => row),
    erpApi.getPurchaseOrderItems(row.id),
  ])
  Object.assign(formModel, {
    id: row.id,
    supplierId: detail.supplierId,
    warehouseId: detail.warehouseId,
    remark: detail.remark || '',
    status: detail.status,
  })
  formItems.value = items.map((item) => ({
    id: item.id,
    productId: item.productId,
    productName: item.productName,
    quantity: Math.max(1, getItemQuantity(item)),
    unitPriceInput: String(getItemUnitPrice(item)),
  }))
  if (!formItems.value.length) handleAddItem()
  drawerVisible.value = true
}

async function onDrawerOpened() {
  if (drawerType.value !== 'view' || !activeOrder.value?.id) return
  itemsLoading.value = true
  try {
    const [detail, items] = await Promise.all([
      erpApi.getPurchaseOrderDetail(activeOrder.value.id).catch(() => activeOrder.value as ErpPurchaseOrder),
      erpApi.getPurchaseOrderItems(activeOrder.value.id),
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
    productId: item.productId!,
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
  const hasInvalidItem = formItems.value.some(
    (item) => !item.productId || Number(item.quantity) <= 0 || Number(item.unitPriceInput) < 0
  )
  if (hasInvalidItem) {
    ElMessage.warning('请确保明细产品已选择、数量大于 0 且单价不小于 0')
    return
  }

  saving.value = true
  try {
    const payload = {
      id: formModel.id,
      supplierId: formModel.supplierId!,
      warehouseId: formModel.warehouseId!,
      remark: formModel.remark.trim() || undefined,
      status: PURCHASE_ORDER_STATUS.DRAFT,
      totalAmount: totalAmount.value,
      items: buildRequestItems(),
    }
    if (drawerType.value === 'edit' && formModel.id) {
      await erpApi.updatePurchaseOrder(formModel.id, payload)
      ElMessage.success('草稿更新成功')
    } else {
      await erpApi.createPurchaseOrder(payload)
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

async function handleConfirmInbound(row: ErpPurchaseOrder) {
  try {
    await ElMessageBox.confirm('确认该采购单已入库？', '确认入库', { type: 'info' })
    await erpApi.confirmInbound(row.id)
    ElMessage.success('入库确认成功')
    loadData()
  } catch {
    /* cancel */
  }
}

async function handleSubmit(row: ErpPurchaseOrder) {
  try {
    await ElMessageBox.confirm('确认提交该采购单进入审批？', '提交确认', { type: 'info' })
    await erpApi.submitPurchaseOrder(row.id)
    ElMessage.success('提交成功')
    loadData()
  } catch {
    /* cancel */
  }
}

async function handleApprove(row: ErpPurchaseOrder) {
  try {
    await ElMessageBox.confirm('确认审核通过该订单？', '审核确认', { type: 'success' })
    await erpApi.approvePurchaseOrder(row.id)
    ElMessage.success('审核通过')
    loadData()
  } catch {
    /* cancel */
  }
}

async function handleReject(row: ErpPurchaseOrder) {
  try {
    await ElMessageBox.confirm('确认拒绝该订单？', '拒绝确认', { type: 'warning' })
    await erpApi.rejectPurchaseOrder(row.id)
    ElMessage.success('已拒绝')
    loadData()
  } catch {
    /* cancel */
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该订单？', '提示', { type: 'warning' })
    await erpApi.deletePurchaseOrder(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    /* cancel */
  }
}

onMounted(() => loadData())
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
  flex-wrap: wrap;
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
.purchase-order-drawer .el-drawer__header {
  margin-bottom: 0;
  padding: 28px 28px 8px;
  border-bottom: none !important;
}

.purchase-order-drawer .el-drawer {
  border-top-left-radius: 20px;
  border-bottom-left-radius: 20px;
}

.purchase-order-drawer .el-drawer__body {
  padding: 12px 28px 24px;
}

.purchase-order-drawer .el-drawer__footer {
  padding: 12px 28px 20px;
  border-top: 1px solid #f1f5f9;
}
</style>
