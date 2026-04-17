<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" :clearable="true" style="width:150px">
            <el-option label="全部" :value="''" />
            <el-option label="已发布" :value="0" />
            <el-option label="草稿" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="toolbar">
          <el-button type="primary" @click="openAddDialog">发布公告</el-button>
        </div>
      </template>

      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="noticeType" label="类型" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">
              {{ row.status === 0 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="180" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="row.status !== 0" type="success" link size="small" @click="handlePublish(row)">发布</el-button>
          </template>
        </el-table-column>
      </el-table>
      <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="fetchData" />

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :current-page="pagination.page"
          :page-size="pagination.size"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 发布/编辑公告弹窗 -->
    <el-dialog
:append-to-body="true"       v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '发布公告'"
      width="600px"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.noticeType" placeholder="请选择类型" style="width:100%">
            <el-option label="系统通知" value="system" />
            <el-option label="业务通知" value="business" />
            <el-option label="活动通知" value="activity" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="选择过期时间"
            style="width:100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemApi, type SysNotice } from '@/api/system'
import RequestErrorState from '@/components/RequestErrorState.vue'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<SysNotice[]>([])
const errorMsg = ref('')
const total = ref(0)
const pagination = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const isEdit = ref(false)

const searchForm = reactive({ status: undefined as number | undefined })

interface NoticeForm {
  id?: number
  title: string
  content: string
  noticeType: string
  expireTime: string
}

const form = reactive<NoticeForm>({
  title: '',
  content: '',
  noticeType: 'system',
  expireTime: '',
})

async function fetchData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await systemApi.getNoticePage(pagination.page, pagination.size)
    let list = res.records ?? res.list ?? []
    if (searchForm.status !== undefined) {
      list = list.filter(item => item.status === searchForm.status)
    }
    tableData.value = list
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '公告列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.status = undefined
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, title: '', content: '', noticeType: 'system', expireTime: '' })
}

function openAddDialog() {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

function openEditDialog(row: SysNotice) {
  isEdit.value = true
  form.id = row.id
  form.title = row.title
  form.content = row.content
  form.noticeType = row.noticeType
  form.expireTime = row.expireTime || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.title || !form.content || !form.noticeType) {
    ElMessage.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value && form.id) {
      await systemApi.updateNotice(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await systemApi.createNotice(form)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handlePublish(row: SysNotice) {
  try {
    await ElMessageBox.confirm('确定发布该公告吗？', '提示', { type: 'warning' })
    await systemApi.publishNotice(row.id)
    ElMessage.success('发布成功')
    fetchData()
  } catch {
    // cancelled
  }
}

// init
fetchData()
</script>

<style scoped>
.page-container {
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
.toolbar { display: flex; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
