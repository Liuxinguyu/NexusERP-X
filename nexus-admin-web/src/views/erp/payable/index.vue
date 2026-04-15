<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <el-input v-model="filterSupplierId" placeholder="供应商ID" :clearable="true" style="width:160px;margin-right:8px" type="number" @keyup.enter="handleSearch" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      </div>
    </el-card>
    <el-card class="table-card" shadow="never">
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="payableNo" label="应付单号" width="180" />
      <el-table-column prop="supplierId" label="供应商ID" width="100" />
      <el-table-column prop="supplierName" label="供应商名称" />
      <el-table-column label="总金额" width="120" align="right">
        <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="已付金额" width="120" align="right">
        <template #default="{ row }">¥{{ row.paidAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="待付金额" width="120" align="right">
        <template #default="{ row }">¥{{ row.pendingAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="付款进度" width="180">
        <template #default="{ row }">
          <el-progress
            :percentage="row.totalAmount > 0 ? Math.round((row.paidAmount / row.totalAmount) * 100) : 0"
            :color="progressColor(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="warning" size="small">待付款</el-tag>
          <el-tag v-else-if="row.status === 2" type="success" size="small">已结清</el-tag>
          <el-tag v-else-if="row.status === 3" type="danger" size="small">逾期</el-tag>
          <el-tag v-else size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="info" link @click="viewRecords(row)">查看明细</el-button>
          <el-button size="small" type="primary" link @click="openRecordDialog(row)">登记付款</el-button>
          <el-button size="small" type="danger" link @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />
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

    <!-- 查看明细对话框 -->
    <el-dialog v-model="detailVisible" title="付款明细" width="600px" destroy-on-close>
      <el-table :data="recordList" stripe size="small">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="paidAmount" label="付款金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.paidAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <el-empty v-if="recordList.length === 0" description="暂无付款记录" />
    </el-dialog>

    <!-- 登记付款对话框 -->
    <el-dialog v-model="recordDialogVisible" title="登记付款" width="500px" destroy-on-close>
      <el-form :model="recordForm" label-width="100px">
        <el-form-item label="付款金额" required>
          <el-input-number v-model="recordForm.paidAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="recordForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="recordSaving" @click="handleRecordPayment">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { erpApi } from '@/api/erp'
import RequestErrorState from '@/components/RequestErrorState.vue'

const tableData = ref<any[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const recordDialogVisible = ref(false)
const recordSaving = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterSupplierId = ref<number | undefined>()
const recordList = ref<any[]>([])
const currentRecordId = ref<number>()
const errorMsg = ref('')

const recordForm = reactive({
  paidAmount: 0,
  remark: '',
})

function progressColor(row: any): string {
  const pct = row.totalAmount > 0 ? (row.paidAmount / row.totalAmount) * 100 : 0
  if (pct >= 100) return '#67c23a'
  if (pct >= 50) return '#e6a23c'
  return '#f56c6c'
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getPayablePage(page.value, size.value, {
      supplierId: filterSupplierId.value,
    })
    tableData.value = (res.records ?? res.list ?? [])
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '应付列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

async function viewRecords(row: any) {
  detailVisible.value = true
  try {
    recordList.value = await erpApi.getPayableRecords(row.id)
  } catch {
    recordList.value = []
  }
}

function openRecordDialog(row: any) {
  currentRecordId.value = row.id
  Object.assign(recordForm, { paidAmount: 0, remark: '' })
  recordDialogVisible.value = true
}

async function handleRecordPayment() {
  if (!recordForm.paidAmount) {
    ElMessage.warning('请输入付款金额')
    return
  }
  recordSaving.value = true
  try {
    await erpApi.recordPayment(currentRecordId.value!, recordForm)
    ElMessage.success('登记成功')
    recordDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('登记失败')
  } finally {
    recordSaving.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该应付记录?', '提示', { type: 'warning' })
    await erpApi.deletePayable(id)
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
