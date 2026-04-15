<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="queryForm" @submit.prevent>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:260px"
          />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="queryForm.title" placeholder="请输入标题" :clearable="true" style="width:180px" />
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
          <span>日程管理</span>
          <el-button type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon> 新增日程
          </el-button>
        </div>
      </template>
      <el-table :data="displayData" stripe v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column prop="location" label="地点" min-width="140" show-overflow-tooltip />
        <el-table-column label="提醒" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="reminderTagType(row.reminder)" size="small">
              {{ reminderLabel(row.reminder) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑日程' : '新增日程'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入日程标题" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="请选择开始时间"
            style="width:100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="请选择结束时间"
            style="width:100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入地点" />
        </el-form-item>
        <el-form-item label="提醒" prop="reminder">
          <el-select v-model="form.reminder" placeholder="请选择提醒时间" style="width:100%">
            <el-option label="不提醒" :value="0" />
            <el-option label="5分钟前" :value="5" />
            <el-option label="15分钟前" :value="15" />
            <el-option label="30分钟前" :value="30" />
            <el-option label="1小时前" :value="60" />
          </el-select>
        </el-form-item>
        <el-form-item label="参与人ID" prop="participantIds">
          <el-input v-model="form.participantIds" placeholder="请输入参与人用户ID，多个用逗号分隔" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入日程描述" />
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
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { oaApi, type OaSchedule } from '@/api/oa'

const queryForm = reactive({
  dateRange: [] as string[],
  title: '',
})

const allData = ref<any[]>([])
const loading = ref(false)

const pagination = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)

const form = reactive({
  title: '',
  startTime: '',
  endTime: '',
  location: '',
  reminder: 0,
  participantIds: '',
  description: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

function reminderLabel(reminder?: number) {
  const map: Record<number, string> = { 0: '不提醒', 5: '5分钟前', 15: '15分钟前', 30: '30分钟前', 60: '1小时前' }
  return map[reminder ?? 0] ?? '不提醒'
}

function reminderTagType(reminder?: number) {
  return reminder === 0 ? 'info' : 'warning'
}

const displayData = computed(() => {
  const start = (pagination.current - 1) * pagination.size
  const end = start + pagination.size
  return allData.value.slice(start, end)
})

function handlePageSizeChange() {
  pagination.current = 1
}

function handlePageChange() {
  // data already filtered via computed
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, {
    title: '',
    startTime: '',
    endTime: '',
    location: '',
    reminder: 0,
    participantIds: '',
    description: '',
  })
}

function openAddDialog() {
  isEdit.value = false
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, {
    title: row.title,
    startTime: row.startTime,
    endTime: row.endTime,
    location: row.location ?? '',
    reminder: row.reminder ?? 0,
    participantIds: row.participantIds ?? '',
    description: row.description ?? '',
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
      await oaApi.updateSchedule(currentId.value, form)
      ElMessage.success('编辑成功')
    } else {
      await oaApi.createSchedule(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {} finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该日程吗？', '提示', { type: 'warning' })
    await oaApi.deleteSchedule(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {}
    if (queryForm.dateRange && queryForm.dateRange.length === 2) {
      params.startDate = queryForm.dateRange[0]
      params.endDate = queryForm.dateRange[1]
    }
    const res = await oaApi.getSchedule(params.startDate, params.endDate)
    // filter by title on frontend
    let data = Array.isArray(res) ? res : []
    if (queryForm.title) {
      data = data.filter((item: any) => item.title?.includes(queryForm.title))
    }
    allData.value = data
    pagination.total = data.length
    pagination.current = 1
  } catch {
    allData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  queryForm.dateRange = []
  queryForm.title = ''
  loadData()
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
