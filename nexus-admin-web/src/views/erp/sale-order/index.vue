<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <el-input v-model="filterOrderNo" placeholder="按订单号搜索" :clearable="true" style="width:200px;margin-right:8px" @keyup.enter="handleSearch" />
        <el-select v-model="filterStatus" placeholder="订单状态" :clearable="true" style="width:160px;margin-right:8px">
          <el-option label="全部" :value="''" />
          <el-option label="待审核" :value="1" />
          <el-option label="已通过" :value="2" />
          <el-option label="已拒绝" :value="3" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增销售单</el-button>
      </div>
    </el-card>
    <el-card class="table-card" shadow="never">
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="customerId" label="客户ID" width="100" />
      <el-table-column prop="customerName" label="客户名称" />
      <el-table-column prop="warehouseId" label="仓库ID" width="100" />
      <el-table-column label="总金额" width="120" align="right">
        <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === SALE_ORDER_STATUS.DRAFT" size="small">草稿</el-tag>
          <el-tag v-else-if="row.status === SALE_ORDER_STATUS.PENDING" type="warning" size="small">待审核</el-tag>
          <el-tag v-else-if="row.status === SALE_ORDER_STATUS.APPROVED" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="row.status === SALE_ORDER_STATUS.REJECTED" type="danger" size="small">已拒绝</el-tag>
          <el-tag v-else size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="info" link @click="viewItems(row)">查看明细</el-button>
          <el-button v-if="row.status === SALE_ORDER_STATUS.DRAFT" size="small" type="primary" link @click="handleSubmit(row)">提交</el-button>
          <el-button v-if="row.status === SALE_ORDER_STATUS.PENDING" size="small" type="success" link @click="handleApprove(row)">审核</el-button>
          <el-button v-if="row.status === SALE_ORDER_STATUS.PENDING" size="small" type="danger" link @click="handleReject(row)">拒绝</el-button>
          <el-button v-if="row.status === SALE_ORDER_STATUS.DRAFT || row.status === SALE_ORDER_STATUS.REJECTED" size="small" type="danger" link @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10,20,50]"
      layout="total,sizes,prev,pager,next"
      @current-change="loadData"
      @size-change="loadData"
      style="margin-top:16px"
    />
    </el-card>

    <!-- 新增销售单对话框 -->
    <el-dialog v-model="dialogVisible" title="新增销售单" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="客户ID" required>
          <el-input-number v-model="form.customerId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="仓库ID" required>
          <el-input-number v-model="form.warehouseId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="总金额">
          <el-input-number v-model="form.totalAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="SALE_ORDER_STATUS.DRAFT">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看明细对话框 -->
    <el-dialog v-model="detailVisible" title="订单明细" width="600px" destroy-on-close>
      <el-table :data="orderItems" stripe size="small">
        <el-table-column prop="productId" label="产品ID" width="100" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column prop="qty" label="数量" width="100" align="center" />
        <el-table-column label="单价" width="120" align="right">
          <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="120" align="right">
          <template #default="{ row }">¥{{ ((row.qty ?? 0) * (row.price ?? 0)).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="orderItems.length === 0" description="暂无明细" />
    </el-dialog>
    <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { erpApi } from '@/api/erp'
import { SALE_ORDER_STATUS } from '@/constants/status'
import RequestErrorState from '@/components/RequestErrorState.vue'

const tableData = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterOrderNo = ref('')
const filterStatus = ref<number | undefined>()
const orderItems = ref<any[]>([])
const errorMsg = ref('')

const form = reactive({
  customerId: 0,
  warehouseId: 0,
  totalAmount: 0,
  status: SALE_ORDER_STATUS.DRAFT,
})

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getSaleOrderPage(page.value, size.value, filterStatus.value, filterOrderNo.value || undefined)
    tableData.value = res.records ?? res.list ?? []
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '销售单列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function openDialog() {
  Object.assign(form, { customerId: 0, warehouseId: 0, totalAmount: 0, status: SALE_ORDER_STATUS.DRAFT })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.customerId || !form.warehouseId) {
    ElMessage.warning('请填写客户ID和仓库ID')
    return
  }
  saving.value = true
  try {
    await erpApi.createSaleOrder(form)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('创建失败')
  } finally {
    saving.value = false
  }
}

async function viewItems(row: any) {
  detailVisible.value = true
  try {
    orderItems.value = await erpApi.getSaleOrderItems(row.id)
  } catch {
    orderItems.value = []
    ElMessage.error('加载明细失败')
  }
}

async function handleApprove(row: any) {
  try {
    await ElMessageBox.confirm('确认审核通过该订单?', '审核确认', { type: 'success' })
    await erpApi.approveSaleOrder(row.id)
    ElMessage.success('审核通过')
    loadData()
  } catch {}
}

async function handleSubmit(row: any) {
  try {
    await ElMessageBox.confirm('确认提交该订单进入审批?', '提交确认', { type: 'info' })
    await erpApi.submitDraftSaleOrder(row.id)
    ElMessage.success('提交成功')
    loadData()
  } catch {}
}

async function handleReject(row: any) {
  try {
    await ElMessageBox.confirm('确认拒绝该订单?', '拒绝确认', { type: 'warning' })
    await erpApi.rejectSaleOrder(row.id)
    ElMessage.success('已拒绝')
    loadData()
  } catch {}
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该订单?', '提示', { type: 'warning' })
    await erpApi.deleteSaleOrder(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
</style>
