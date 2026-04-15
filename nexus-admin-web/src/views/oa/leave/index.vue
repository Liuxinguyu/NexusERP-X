<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="queryForm" @submit.prevent>
        <el-form-item label="审批状态">
          <el-select v-model="queryForm.status" placeholder="全部" :clearable="true" style="width:150px">
            <el-option label="全部" :value="''" />
            <el-option label="待审批" :value="0" />
            <el-option label="已批准" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>请假记录</span>
          <el-button type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon> 新增请假
          </el-button>
        </div>
      </template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="leaveType" label="请假类型" width="120" />
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column prop="leaveDays" label="天数" width="80" align="center" />
        <el-table-column prop="reason" label="请假事由" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetailDialog(row)">查看详情</el-button>
            <template v-if="row.status === 0">
              <el-button link type="success" size="small" @click="handleApprove(row)">批准</el-button>
              <el-button link type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="请假详情" width="600px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="请假类型">{{ detailRow?.leaveType }}</el-descriptions-item>
        <el-descriptions-item label="天数">{{ detailRow?.leaveDays }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detailRow?.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detailRow?.endTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailRow?.status)" size="small">
            {{ statusLabel(detailRow?.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detailRow?.createTime }}</el-descriptions-item>
        <el-descriptions-item label="请假事由" :span="2">{{ detailRow?.reason }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增弹窗 -->
    <el-dialog v-model="addVisible" title="新增请假" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" placeholder="请选择请假类型" style="width:100%">
            <el-option label="事假" value="事假" />
            <el-option label="病假" value="病假" />
            <el-option label="年假" value="年假" />
            <el-option label="婚假" value="婚假" />
            <el-option label="产假" value="产假" />
            <el-option label="陪产假" value="陪产假" />
            <el-option label="丧假" value="丧假" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="date"
            placeholder="请选择开始日期"
            style="width:100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="date"
            placeholder="请选择结束日期"
            style="width:100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="请假事由" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入请假事由"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { oaApi, type OaLeaveRequest } from '@/api/oa'

const queryForm = reactive({ status: undefined as number | undefined })

const tableData = ref<OaLeaveRequest[]>([])
const loading = ref(false)

const pagination = reactive({ current: 1, size: 10, total: 0 })

const detailVisible = ref(false)
const detailRow = ref<OaLeaveRequest | null>(null)
const addVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({ leaveType: '', startTime: '', endTime: '', reason: '' })
const rules: FormRules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假事由', trigger: 'blur' }],
}

function statusLabel(status?: number) {
  const map: Record<number, string> = { 0: '待审批', 1: '已批准', 2: '已拒绝' }
  return map[status ?? -1] ?? '未知'
}

function statusTagType(status?: number) {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger' }
  return (map[status ?? -1] ?? 'info') as any
}

function openDetailDialog(row: OaLeaveRequest) {
  detailRow.value = row
  detailVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, { leaveType: '', startTime: '', endTime: '', reason: '' })
}

function openAddDialog() {
  resetForm()
  addVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await oaApi.createLeaveRequest(form)
    ElMessage.success('提交成功')
    addVisible.value = false
    loadData()
  } catch {} finally {
    submitLoading.value = false
  }
}

async function handleApprove(row: OaLeaveRequest) {
  try {
    await ElMessageBox.confirm('确认批准该请假申请吗？', '提示', { type: 'warning' })
    await oaApi.approveLeaveRequest(row.id, true, '同意')
    ElMessage.success('已批准')
    loadData()
  } catch {}
}

async function handleReject(row: OaLeaveRequest) {
  try {
    await ElMessageBox.confirm('确认拒绝该请假申请吗？', '提示', { type: 'warning' })
    await oaApi.approveLeaveRequest(row.id, false, '不同意')
    ElMessage.success('已拒绝')
    loadData()
  } catch {}
}

async function loadData() {
  loading.value = true
  try {
    const res = await oaApi.getLeaveRequestPage(pagination.current, pagination.size, queryForm.status)
    tableData.value = res.records || []
    pagination.total = res.total
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  queryForm.status = undefined
  handleSearch()
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 12px; }
.table-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
