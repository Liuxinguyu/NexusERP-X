<template>
  <div class="monthly-slip-page">
    <!-- 搜索过滤 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="年月">
          <el-date-picker
            v-model="searchForm.yearMonth"
            type="month"
            placeholder="请选择年月"
            value-format="YYYY-MM"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="员工姓名">
          <el-input v-model="searchForm.employeeName" placeholder="请输入员工姓名" :clearable="true" style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" :clearable="true" style="width: 140px">
            <el-option label="待计算" :value="0" />
            <el-option label="待确认" :value="1" />
            <el-option label="已发放" :value="2" />
            <el-option label="已撤销" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" :icon="DocumentAdd" @click="openGenerateDialog">生成工资单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" stripe v-loading="loading" height="100%">
        <el-table-column prop="slipNo" label="工资单号" min-width="150" />
        <el-table-column prop="employeeName" label="员工姓名" width="120" />
        <el-table-column prop="yearMonth" label="年月" width="100" align="center" />
        <el-table-column prop="basicWage" label="基本工资" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.basicWage) }}</template>
        </el-table-column>
        <el-table-column prop="performanceWage" label="绩效工资" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.performanceWage) }}</template>
        </el-table-column>
        <el-table-column prop="allowances" label="补贴合计" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.allowances) }}</template>
        </el-table-column>
        <el-table-column prop="deductions" label="扣款合计" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.deductions) }}</template>
        </el-table-column>
        <el-table-column prop="socialSecurity" label="社保" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.socialSecurity) }}</template>
        </el-table-column>
        <el-table-column prop="housingFund" label="公积金" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.housingFund) }}</template>
        </el-table-column>
        <el-table-column prop="taxAmount" label="个税" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.taxAmount) }}</template>
        </el-table-column>
        <el-table-column prop="netWage" label="实发工资" width="120" align="right">
          <template #default="{ row }">
            <span class="net-wage">{{ formatMoney(row.netWage) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetailDialog(row)">查看明细</el-button>
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />

      <el-pagination
        class="pagination"
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 查看明细弹窗 -->
    <el-dialog :append-to-body="true" v-model="detailVisible" title="工资单明细" width="700px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工资单号">{{ detailRow?.slipNo }}</el-descriptions-item>
        <el-descriptions-item label="员工姓名">{{ detailRow?.employeeName }}</el-descriptions-item>
        <el-descriptions-item label="年月">{{ detailRow?.yearMonth }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailRow?.status)" size="small">{{ statusLabel(detailRow?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="基本工资">{{ formatMoney(detailRow?.basicWage) }}</el-descriptions-item>
        <el-descriptions-item label="绩效工资">{{ formatMoney(detailRow?.performanceWage) }}</el-descriptions-item>
        <el-descriptions-item label="补贴合计">{{ formatMoney(detailRow?.allowances) }}</el-descriptions-item>
        <el-descriptions-item label="扣款合计">{{ formatMoney(detailRow?.deductions) }}</el-descriptions-item>
        <el-descriptions-item label="社保">{{ formatMoney(detailRow?.socialSecurity) }}</el-descriptions-item>
        <el-descriptions-item label="公积金">{{ formatMoney(detailRow?.housingFund) }}</el-descriptions-item>
        <el-descriptions-item label="个税">{{ formatMoney(detailRow?.taxAmount) }}</el-descriptions-item>
        <el-descriptions-item label="实发工资">
          <span class="net-wage">{{ formatMoney(detailRow?.netWage) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRow?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog :append-to-body="true" v-model="editVisible" title="编辑工资单" width="640px" destroy-on-close @closed="handleEditClosed">
      <el-form ref="formRef" :model="editForm" :rules="editFormRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="基本工资" prop="basicWage">
              <el-input-number v-model="editForm.basicWage" :precision="2" :step="100" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绩效工资" prop="performanceWage">
              <el-input-number v-model="editForm.performanceWage" :precision="2" :step="100" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="补贴合计" prop="allowances">
              <el-input-number v-model="editForm.allowances" :precision="2" :step="50" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="扣款合计" prop="deductions">
              <el-input-number v-model="editForm.deductions" :precision="2" :step="50" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="社保" prop="socialSecurity">
              <el-input-number v-model="editForm.socialSecurity" :precision="2" :step="50" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公积金" prop="housingFund">
              <el-input-number v-model="editForm.housingFund" :precision="2" :step="50" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="个税" prop="taxAmount">
              <el-input-number v-model="editForm.taxAmount" :precision="2" :step="50" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 生成工资单弹窗 -->
    <el-dialog :append-to-body="true" v-model="generateVisible" title="生成工资单" width="420px" destroy-on-close>
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateFormRules" label-width="80px">
        <el-form-item label="年份" prop="year">
          <el-input-number v-model="generateForm.year" :min="2000" :max="2100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月份" prop="month">
          <el-input-number v-model="generateForm.month" :min="1" :max="12" style="width: 100%" />
        </el-form-item>
        <el-form-item label="员工ID" prop="employeeId">
          <el-input-number v-model="generateForm.employeeId" :min="1" style="width: 100%" placeholder="不填则生成全部" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { DocumentAdd } from '@element-plus/icons-vue'
import { wageApi } from '@/api/wage'
import RequestErrorState from '@/components/RequestErrorState.vue'

interface WageSlipRow {
  id: number
  slipNo: string
  employeeName: string
  yearMonth: string
  basicWage: number
  performanceWage: number
  allowances: number
  deductions: number
  socialSecurity: number
  housingFund: number
  taxAmount: number
  netWage: number
  status: number
  createTime?: string
}

// --- Search & Table ---
const loading = ref(false)
const tableData = ref<WageSlipRow[]>([])
const errorMsg = ref('')
const searchForm = reactive({
  yearMonth: '',
  employeeName: '',
  status: undefined as number | undefined,
})
const pagination = reactive({ current: 1, size: 10, total: 0 })

// --- Detail ---
const detailVisible = ref(false)
const detailRow = ref<WageSlipRow | null>(null)

// --- Edit ---
const editVisible = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editingId = ref<number | null>(null)
const editForm = reactive({
  basicWage: 0,
  performanceWage: 0,
  allowances: 0,
  deductions: 0,
  socialSecurity: 0,
  housingFund: 0,
  taxAmount: 0,
})

const editFormRules: FormRules = {}

// --- Generate ---
const generateVisible = ref(false)
const generateFormRef = ref<FormInstance>()
const generateLoading = ref(false)
const generateForm = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  employeeId: undefined as number | undefined,
})
const generateFormRules: FormRules = {}

// --- Helpers ---
function formatMoney(val: number | undefined) {
  if (val == null) return '0.00'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const statusLabel = (val: number | undefined) => {
  const map: Record<number, string> = { 0: '待计算', 1: '待确认', 2: '已发放', 3: '已撤销' }
  return val !== undefined ? (map[val] ?? '未知') : '未知'
}

const statusTagType = (val: number | undefined) => {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return (val !== undefined ? (map[val] ?? '') : '') as any
}

// --- Data Loading ---
async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      yearMonth: searchForm.yearMonth || undefined,
      employeeName: searchForm.employeeName || undefined,
      status: searchForm.status,
    }
    const res = await wageApi.getMonthlySlipPage(pagination.current, pagination.size, params)
    const page = (res as any)
    // Map backend fields to view field names
    tableData.value = (page.records ?? page.list ?? []).map((r: any) => ({
      ...r,
      slipNo: r.slipNo ?? '',
      employeeName: r.employeeName ?? r.name ?? '',
      basicWage: r.basicWage ?? r.baseSalary ?? 0,
      performanceWage: r.performanceWage ?? 0,
      allowances: r.allowances ?? r.subsidyTotal ?? 0,
      deductions: r.deductions ?? r.deductionTotal ?? 0,
      netWage: r.netWage ?? r.netPay ?? 0,
    }))
    pagination.total = page.total ?? 0
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '工资单列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.yearMonth = ''
  searchForm.employeeName = ''
  searchForm.status = undefined
  pagination.current = 1
  loadData()
}

// --- Detail ---
function openDetailDialog(row: WageSlipRow) {
  detailRow.value = { ...row }
  detailVisible.value = true
}

// --- Edit ---
function openEditDialog(row: WageSlipRow) {
  editingId.value = row.id
  Object.assign(editForm, {
    basicWage: row.basicWage ?? 0,
    performanceWage: row.performanceWage ?? 0,
    allowances: row.allowances ?? 0,
    deductions: row.deductions ?? 0,
    socialSecurity: row.socialSecurity ?? 0,
    housingFund: row.housingFund ?? 0,
    taxAmount: row.taxAmount ?? 0,
  })
  editVisible.value = true
}

function handleEditClosed() {
  formRef.value?.resetFields()
}

async function handleEditSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    // Recalculate net wage
    const netWage =
      (editForm.basicWage ?? 0) +
      (editForm.performanceWage ?? 0) +
      (editForm.allowances ?? 0) -
      (editForm.deductions ?? 0) -
      (editForm.socialSecurity ?? 0) -
      (editForm.housingFund ?? 0) -
      (editForm.taxAmount ?? 0)
    await wageApi.adjustSlip(editingId.value!, { ...editForm, netWage })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

// --- Delete ---
async function handleDelete(row: WageSlipRow) {
  try {
    await ElMessageBox.confirm('确定要删除该工资单吗？', '提示', { type: 'warning' })
    await wageApi.deleteMonthlySlip(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// --- Generate ---
function openGenerateDialog() {
  generateFormRef.value?.resetFields()
  generateForm.year = new Date().getFullYear()
  generateForm.month = new Date().getMonth() + 1
  generateForm.employeeId = undefined
  generateVisible.value = true
}

async function handleGenerate() {
  generateLoading.value = true
  try {
    const yearMonth = `${generateForm.year}-${String(generateForm.month).padStart(2, '0')}`
    await wageApi.generateMonthly(yearMonth, generateForm.employeeId ? [generateForm.employeeId] : undefined)
    ElMessage.success('生成成功')
    generateVisible.value = false
    loadData()
  } catch {
    ElMessage.error('生成失败')
  } finally {
    generateLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.monthly-slip-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
  gap: 12px;
}
.search-card { flex-shrink: 0; }
.table-card {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.net-wage { color: #f56c6c; font-weight: 600; }
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
