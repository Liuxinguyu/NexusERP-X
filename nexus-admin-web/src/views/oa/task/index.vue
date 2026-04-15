<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="queryForm" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" :clearable="true" style="width:150px">
            <el-option label="全部" :value="''" />
            <el-option label="待处理" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 标签页 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>任务管理</span>
          <el-button type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon> 新增任务
          </el-button>
        </div>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="我的任务" name="mine" />
        <el-tab-pane label="全部任务" name="all" />
      </el-tabs>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="taskNo" label="任务编号" width="140" />
        <el-table-column prop="title" label="任务标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="assigneeUserName" label="负责人" width="120" />
        <el-table-column label="优先级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small">
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="截止日期" width="120" />
        <el-table-column prop="progress" label="进度%" width="80" align="center">
          <template #default="{ row }">{{ row.progress ?? 0 }}%</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑任务' : '新增任务'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="任务描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="负责人ID" prop="assigneeUserId">
          <el-input-number v-model="form.assigneeUserId" :min="1" placeholder="请输入负责人用户ID" style="width:100%" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" placeholder="请选择优先级" style="width:100%">
            <el-option label="高" :value="0" />
            <el-option label="中" :value="1" />
            <el-option label="低" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期" prop="dueDate">
          <el-date-picker
            v-model="form.dueDate"
            type="date"
            placeholder="请选择截止日期"
            style="width:100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { oaApi, type OaTask } from '@/api/oa'
import { useUserStore } from '@/stores/user'
import RequestErrorState from '@/components/RequestErrorState.vue'

const userStore = useUserStore()
const activeTab = ref('mine')

const queryForm = reactive({ status: undefined as number | undefined })

const tableData = ref<OaTask[]>([])
const loading = ref(false)
const errorMsg = ref('')

const pagination = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)

const form = reactive({
  title: '',
  description: '',
  assigneeUserId: undefined as number | undefined,
  priority: 1,
  dueDate: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  assigneeUserId: [{ required: true, message: '请输入负责人用户ID', trigger: 'blur' }],
}

function priorityLabel(priority?: number) {
  const map: Record<number, string> = { 0: '高', 1: '中', 2: '低' }
  return map[priority ?? -1] ?? '未知'
}

function priorityTagType(priority?: number) {
  const map: Record<number, string> = { 0: 'danger', 1: 'warning', 2: 'info' }
  return (map[priority ?? -1] ?? 'info') as any
}

function statusLabel(status?: number) {
  const map: Record<number, string> = { 0: '待处理', 1: '进行中', 2: '已完成', 3: '已取消' }
  return map[status ?? -1] ?? '未知'
}

function statusTagType(status?: number) {
  const map: Record<number, string> = { 0: 'info', 1: 'primary', 2: 'success', 3: 'warning' }
  return (map[status ?? -1] ?? 'info') as any
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, { title: '', description: '', assigneeUserId: undefined, priority: 1, dueDate: '' })
}

function openAddDialog() {
  isEdit.value = false
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: OaTask) {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, {
    title: row.title,
    description: row.description,
    assigneeUserId: row.assigneeUserId,
    priority: row.priority,
    dueDate: row.dueDate ?? '',
  })
  dialogVisible.value = true
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
    if (isEdit.value && currentId.value !== null) {
      await oaApi.updateTask(currentId.value, form)
      ElMessage.success('编辑成功')
    } else {
      await oaApi.createTask(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {} finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: OaTask) {
  try {
    await ElMessageBox.confirm('确认删除该任务吗？', '提示', { type: 'warning' })
    await oaApi.deleteTask(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await oaApi.getTaskPage(
      pagination.current,
      pagination.size,
      queryForm.status,
      activeTab.value === 'mine' ? userStore.profile?.userId : undefined
    )
    tableData.value = res.records || res.list || []
    pagination.total = res.total
  } catch {
    tableData.value = []
    errorMsg.value = '任务列表加载失败'
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

watch(activeTab, () => {
  pagination.current = 1
  loadData()
})

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 12px; }
.table-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
