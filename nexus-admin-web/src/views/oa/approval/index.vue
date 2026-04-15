<template>
  <div class="page-container">
    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header"><span>审批中心</span></div>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="待我审批" name="pending" />
        <el-tab-pane label="我已审批" name="processed" />
      </el-tabs>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="bizType" label="业务类型" width="120" />
        <el-table-column prop="bizId" label="业务ID" width="100" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="applicantUserName" label="申请人" width="120" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="activeTab === 'pending'">
              <el-button link type="primary" size="small" @click="openApproveDialog(row)">
                审批
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />
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

    <!-- 审批弹窗 -->
    <el-dialog v-model="dialogVisible" title="审批" width="520px" destroy-on-close>
      <el-form :model="approveForm" label-width="90px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :value="true">批准</el-radio>
            <el-radio :value="false">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="approveForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入审批意见（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { oaApi, type OaApprovalTask } from '@/api/oa'
import { APPROVAL_TASK_STATUS } from '@/constants/status'
import RequestErrorState from '@/components/RequestErrorState.vue'

const activeTab = ref('pending')

const tableData = ref<OaApprovalTask[]>([])
const loading = ref(false)
const errorMsg = ref('')

const pagination = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const submitLoading = ref(false)
const currentRow = ref<OaApprovalTask | null>(null)

const approveForm = reactive({ approved: true as boolean, comment: '' })

function statusLabel(status?: number) {
  const map: Record<number, string> = {
    [APPROVAL_TASK_STATUS.PENDING]: '待审批',
    [APPROVAL_TASK_STATUS.APPROVED]: '已批准',
    [APPROVAL_TASK_STATUS.REJECTED]: '已拒绝',
  }
  return map[status ?? -1] ?? '未知'
}

function statusTagType(status?: number): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<number, 'warning' | 'success' | 'danger' | 'info'> = {
    [APPROVAL_TASK_STATUS.PENDING]: 'warning',
    [APPROVAL_TASK_STATUS.APPROVED]: 'success',
    [APPROVAL_TASK_STATUS.REJECTED]: 'danger',
  }
  return map[status ?? -1] ?? 'info'
}

function openApproveDialog(row: OaApprovalTask) {
  currentRow.value = row
  approveForm.approved = true
  approveForm.comment = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!currentRow.value) return
  submitLoading.value = true
  try {
    if (approveForm.approved) {
      await oaApi.approveTask(currentRow.value.id, approveForm.comment)
      ElMessage.success('审批已通过')
    } else {
      await oaApi.rejectTask(currentRow.value.id, approveForm.comment)
      ElMessage.success('已拒绝')
    }
    dialogVisible.value = false
    loadData()
  } catch {} finally {
    submitLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = activeTab.value === 'pending'
      ? await oaApi.getMyApprove(pagination.current, pagination.size)
      : await oaApi.getMyApply(pagination.current, pagination.size)
    tableData.value = res.records || res.list || []
    pagination.total = res.total
  } catch {
    tableData.value = []
    errorMsg.value = '审批任务列表加载失败'
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => {
  pagination.current = 1
  loadData()
})

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.table-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
