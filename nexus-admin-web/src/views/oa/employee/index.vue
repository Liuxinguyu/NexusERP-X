<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="queryForm" @submit.prevent>
        <el-form-item label="姓名">
          <el-input v-model="queryForm.name" placeholder="请输入姓名" :clearable="true" style="width:180px" />
        </el-form-item>
        <el-form-item label="工号">
          <el-input v-model="queryForm.empNo" placeholder="请输入工号" :clearable="true" style="width:180px" />
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
          <span>员工列表</span>
          <el-button type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon> 新增员工
          </el-button>
        </div>
      </template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="empNo" label="工号" width="120" />
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="dept" label="部门" min-width="120" />
        <el-table-column prop="position" label="岗位" min-width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="hireDate" label="入职日期" width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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
      :title="isEdit ? '编辑员工' : '新增员工'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="工号" prop="empNo">
          <el-input v-model="form.empNo" placeholder="请输入工号" />
        </el-form-item>
        <el-form-item label="部门" prop="dept">
          <el-input v-model="form.dept" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="岗位" prop="position">
          <el-input v-model="form.position" placeholder="请输入岗位" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="入职日期" prop="hireDate">
          <el-date-picker
            v-model="form.hireDate"
            type="date"
            placeholder="请选择入职日期"
            style="width:100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在职</el-radio>
            <el-radio :value="0">离职</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { oaApi, type OaEmployee } from '@/api/oa'
import RequestErrorState from '@/components/RequestErrorState.vue'

// 查询表单
const queryForm = reactive({ name: '', empNo: '' })

// 表格
const tableData = ref<OaEmployee[]>([])
const loading = ref(false)
const errorMsg = ref('')

// 分页
const pagination = reactive({ current: 1, size: 10, total: 0 })

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)

const form = reactive({
  name: '',
  empNo: '',
  dept: '',
  position: '',
  phone: '',
  hireDate: '',
  status: 1,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  empNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  dept: [{ required: true, message: '请输入部门', trigger: 'blur' }],
  position: [{ required: true, message: '请输入岗位', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, { name: '', empNo: '', dept: '', position: '', phone: '', hireDate: '', status: 1 })
}

function openAddDialog() {
  isEdit.value = false
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: OaEmployee) {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, {
    name: row.name,
    empNo: row.empNo,
    dept: row.dept,
    position: row.position,
    phone: row.phone,
    hireDate: row.hireDate,
    status: row.status,
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
      await oaApi.updateEmployee(currentId.value, form)
      ElMessage.success('编辑成功')
    } else {
      await oaApi.createEmployee(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: OaEmployee) {
  try {
    await ElMessageBox.confirm('确认删除该员工吗？', '提示', { type: 'warning' })
    await oaApi.deleteEmployee(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

async function handleToggleStatus(row: OaEmployee) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${action}该员工吗？`, '提示', { type: 'warning' })
    await oaApi.updateEmployee(row.id, { ...row, status: newStatus })
    ElMessage.success(`${action}成功`)
    loadData()
  } catch {}
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await oaApi.getEmployeePage(
      pagination.current,
      pagination.size,
      queryForm.name || undefined,
      queryForm.empNo || undefined
    )
    tableData.value = res.records || []
    pagination.total = res.total
  } catch {
    tableData.value = []
    errorMsg.value = '员工列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  queryForm.name = ''
  queryForm.empNo = ''
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
