<template>
  <div class="page-container">
    <NexusSearchCard>
      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="请假类型">
          <el-select v-model="queryParams.leaveType" placeholder="全部类型" clearable filterable style="width: 220px">
            <el-option v-for="item in leaveTypeOptions" :key="String(item.value)" :label="item.label" :value="String(item.value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 180px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #actions>
        <div class="search-actions">
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreateDrawer">新增申请</el-button>
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
      <el-table :data="tableData" height="100%">
        <el-table-column label="请假类型" min-width="140">
          <template #default="{ row }">{{ getLeaveTypeLabel(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column label="起止时间" min-width="240">
          <template #default="{ row }">{{ formatLeaveRange(row) }}</template>
        </el-table-column>
        <el-table-column prop="leaveDays" label="天数" width="100" align="center" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :class="['status-tag', statusClass(row.status)]" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" min-width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="primary" link @click="handleSubmitLeave(row)">提交审批</el-button>
            <el-button v-if="row.status === 0" type="danger" link @click="handleDeleteLeave(row)">删除</el-button>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>
      </el-table>
    </NexusTableCard>

    <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />

    <el-drawer
:append-to-body="true"       v-model="drawerVisible"
      :size="600"
      direction="rtl"
      destroy-on-close
      class="leave-drawer"
      :show-close="true"
    >
      <template #header>
        <div>
          <div class="drawer-title">新增请假申请</div>
          <div class="drawer-subtitle">填写请假信息并提交审批流程</div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="leave-form">
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" placeholder="请选择请假类型" filterable>
            <el-option v-for="item in leaveTypeOptions" :key="String(item.value)" :label="item.label" :value="String(item.value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假日期" prop="dateRange">
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            range-separator="至"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="请假事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="5" placeholder="请输入请假事由" maxlength="200" show-word-limit />
        </el-form-item>

        <div v-if="computedLeaveDays > 0" class="leave-summary">预计请假 {{ computedLeaveDays }} 天</div>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleCreateLeave">提交申请</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { oaApi, type LeaveUpsertDTO, type OaLeaveRequest } from '@/api/oa'
import NexusSearchCard from '@/components/NexusSearchCard/index.vue'
import NexusTableCard from '@/components/NexusTableCard/index.vue'
import RequestErrorState from '@/components/RequestErrorState.vue'
import { useDict } from '@/hooks/useDict'

const LEAVE_TYPE_DICT = 'oa_leave_type'

const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const errorMsg = ref('')
const tableData = ref<OaLeaveRequest[]>([])
const drawerVisible = ref(false)
const formRef = ref<FormInstance>()

const queryParams = reactive({
  current: 1,
  size: 10,
  leaveType: '',
  status: undefined as number | undefined,
})

const form = reactive({
  leaveType: '',
  dateRange: [] as string[],
  reason: '',
})

const { dictMap } = useDict(LEAVE_TYPE_DICT)
const leaveTypeOptions = computed(() => dictMap[LEAVE_TYPE_DICT] || [])

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '审批中', value: 1 },
  { label: '已通过', value: 2 },
  { label: '已驳回', value: -1 },
]

const computedLeaveDays = computed(() => {
  const [start, end] = form.dateRange || []
  if (!start || !end) return 0
  const startTime = new Date(start).getTime()
  const endTime = new Date(end).getTime()
  if (Number.isNaN(startTime) || Number.isNaN(endTime) || endTime < startTime) return 0
  return Math.floor((endTime - startTime) / 86400000) + 1
})

const rules: FormRules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择请假日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假事由', trigger: 'blur' }],
}

function getLeaveTypeLabel(value?: string) {
  const matched = leaveTypeOptions.value.find((item) => item.value === value || String(item.value) === String(value || ''))
  return matched?.label || value || '—'
}

function getStartDate(row: OaLeaveRequest) {
  return row.startDate || row.startTime || ''
}

function getEndDate(row: OaLeaveRequest) {
  return row.endDate || row.endTime || ''
}

function formatLeaveRange(row: OaLeaveRequest) {
  const start = getStartDate(row)
  const end = getEndDate(row)
  if (start && end) return `${start} 至 ${end}`
  return start || end || '—'
}

function statusLabel(status?: number) {
  const map: Record<number, string> = {
    0: '草稿',
    1: '审批中',
    2: '已通过',
    [-1]: '已驳回',
  }
  return map[status ?? 99] || '未知'
}

function statusClass(status?: number) {
  const map: Record<number, string> = {
    0: 'status-draft',
    1: 'status-pending',
    2: 'status-approved',
    [-1]: 'status-rejected',
  }
  return map[status ?? 99] || 'status-default'
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, {
    leaveType: '',
    dateRange: [],
    reason: '',
  })
}

function openCreateDrawer() {
  resetForm()
  drawerVisible.value = true
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await oaApi.getLeavePage({
      current: queryParams.current,
      size: queryParams.size,
      status: queryParams.status,
    })
    const list = (res.records ?? res.list ?? []) as OaLeaveRequest[]
    tableData.value = list.filter((item) => !queryParams.leaveType || item.leaveType === queryParams.leaveType)
    total.value = res.total ?? tableData.value.length
    if (res.current != null) queryParams.current = Number(res.current)
    if (res.size != null) queryParams.size = Number(res.size)
  } catch {
    tableData.value = []
    errorMsg.value = '请假记录加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.leaveType = ''
  queryParams.status = undefined
  queryParams.current = 1
  loadData()
}

async function handleCreateLeave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!computedLeaveDays.value) {
    ElMessage.warning('请检查请假日期范围')
    return
  }
  saving.value = true
  try {
    const [startDate, endDate] = form.dateRange
    const payload: LeaveUpsertDTO = {
      leaveType: form.leaveType,
      startDate,
      endDate,
      leaveDays: computedLeaveDays.value,
      reason: form.reason.trim(),
    }
    await oaApi.createLeave(payload)
    ElMessage.success('请假申请已提交')
    drawerVisible.value = false
    loadData()
  } catch {
    ElMessage.error('提交申请失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmitLeave(row: OaLeaveRequest) {
  try {
    await ElMessageBox.confirm('确认提交该草稿进入审批流程吗？', '提示', { type: 'warning' })
    await oaApi.submitLeave(row.id)
    ElMessage.success('已提交审批')
    loadData()
  } catch {}
}

async function handleDeleteLeave(row: OaLeaveRequest) {
  try {
    await ElMessageBox.confirm('确认删除该请假草稿吗？', '提示', { type: 'warning' })
    await oaApi.deleteLeave(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

loadData()
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
.muted-text {
  color: #94a3b8;
}
.status-tag {
  border: none;
}
.status-draft {
  color: #64748b;
  background: #f1f5f9;
}
.status-pending {
  color: #4338ca;
  background: #eef2ff;
}
.status-approved {
  color: #15803d;
  background: #f0fdf4;
}
.status-rejected {
  color: #dc2626;
  background: #fef2f2;
}
.status-default {
  color: #64748b;
  background: #f8fafc;
}
.leave-drawer :deep(.el-drawer) {
  border-radius: 24px 0 0 24px;
}
.drawer-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}
.drawer-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}
.leave-form {
  padding-right: 4px;
}
.leave-summary {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  color: #0f172a;
  font-weight: 600;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
}
</style>
