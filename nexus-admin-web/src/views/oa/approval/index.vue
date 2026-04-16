<template>
  <div class="page-container">
    <div class="approval-tabs">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待我处理" name="pending" />
        <el-tab-pane label="我已处理" name="processed" />
      </el-tabs>
    </div>

    <NexusTableCard
      v-model:current="queryParams.current"
      v-model:size="queryParams.size"
      :loading="loading"
      :total="total"
      @pagination-change="loadData"
    >
      <el-table :data="tableData" height="100%">
        <el-table-column prop="bizType" label="业务类型" width="140" />
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="applicantUserName" label="申请人" width="140" />
        <el-table-column prop="createTime" label="申请时间" min-width="180" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="activeTab === 'pending'">
              <el-button type="primary" link @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" link @click="handleReject(row)">驳回</el-button>
            </template>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>
      </el-table>
    </NexusTableCard>

    <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { oaApi, type OaApprovalTask } from '@/api/oa'
import NexusTableCard from '@/components/NexusTableCard/index.vue'
import RequestErrorState from '@/components/RequestErrorState.vue'

const activeTab = ref<'pending' | 'processed'>('pending')
const loading = ref(false)
const total = ref(0)
const errorMsg = ref('')
const tableData = ref<OaApprovalTask[]>([])

const queryParams = reactive({
  current: 1,
  size: 10,
})

function statusLabel(status?: number) {
  const map: Record<number, string> = {
    0: '待处理',
    1: '已通过',
    2: '已驳回',
  }
  return map[status ?? 99] || '未知'
}

function statusTagType(status?: number): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<number, 'warning' | 'success' | 'danger' | 'info'> = {
    0: 'warning',
    1: 'success',
    2: 'danger',
  }
  return map[status ?? 99] || 'info'
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await oaApi.getApprovalPage(
      {
        current: queryParams.current,
        size: queryParams.size,
      },
      activeTab.value
    )
    tableData.value = (res.records ?? res.list ?? []) as OaApprovalTask[]
    total.value = res.total ?? 0
    if (res.current != null) queryParams.current = Number(res.current)
    if (res.size != null) queryParams.size = Number(res.size)
  } catch {
    tableData.value = []
    errorMsg.value = '审批任务列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  queryParams.current = 1
  loadData()
}

async function handleApprove(row: OaApprovalTask) {
  try {
    await ElMessageBox.confirm('确认通过该审批任务吗？', '提示', { type: 'warning' })
    await oaApi.approveTask(row.taskId ?? row.id, { approved: true, opinion: '同意' })
    ElMessage.success('审批已通过')
    loadData()
  } catch {}
}

async function handleReject(row: OaApprovalTask) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回申请', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPlaceholder: '请填写驳回原因',
      inputValidator: (input) => (input?.trim() ? true : '请输入驳回原因'),
    })
    await oaApi.approveTask(row.taskId ?? row.id, { approved: false, opinion: value.trim() })
    ElMessage.success('已驳回')
    loadData()
  } catch {}
}

loadData()
</script>

<style scoped>
.approval-tabs {
  margin-bottom: 8px;
}
.approval-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
.approval-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}
.muted-text {
  color: #94a3b8;
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
