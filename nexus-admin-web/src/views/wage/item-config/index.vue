<template>
  <div class="wage-item-config-page">
    <!-- 搜索过滤 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="薪资项名称">
          <el-input v-model="searchForm.itemName" placeholder="请输入薪资项名称" :clearable="true" style="width: 200px" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="searchForm.category" placeholder="请选择类别" :clearable="true" style="width: 160px">
            <el-option label="基本工资" :value="1" />
            <el-option label="绩效" :value="2" />
            <el-option label="补贴" :value="3" />
            <el-option label="奖金" :value="4" />
            <el-option label="扣款" :value="5" />
            <el-option label="社保" :value="6" />
            <el-option label="公积金" :value="7" />
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
          <span>薪资项配置列表</span>
          <el-button type="primary" :icon="Plus" @click="openAddDialog">新增薪资项</el-button>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading" height="100%">
        <el-table-column prop="itemName" label="薪资项名称" min-width="140" />
        <el-table-column prop="itemKind" label="类别" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="categoryTagType(row.itemKind)" size="small">
              {{ categoryLabel(row.itemKind) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="calcType" label="计算方式" width="100" align="center">
          <template #default="{ row }">
            {{ calcTypeLabel(row.calcType) }}
          </template>
        </el-table-column>
        <el-table-column prop="defaultAmount" label="默认值" width="110" align="right">
          <template #default="{ row }">
            {{ formatMoney(row.defaultAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="isDeduction" label="是否扣款" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isDeduction === 1 ? 'danger' : 'success'" size="small">
              {{ row.isDeduction === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
:append-to-body="true"       v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      destroy-on-close
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="薪资项名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="请输入薪资项名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="类别" prop="itemKind">
          <el-select v-model="form.itemKind" placeholder="请选择类别" style="width: 100%">
            <el-option label="基本工资" :value="1" />
            <el-option label="绩效" :value="2" />
            <el-option label="补贴" :value="3" />
            <el-option label="奖金" :value="4" />
            <el-option label="扣款" :value="5" />
            <el-option label="社保" :value="6" />
            <el-option label="公积金" :value="7" />
          </el-select>
        </el-form-item>
        <el-form-item label="计算方式" prop="calcType">
          <el-select v-model="form.calcType" placeholder="请选择计算方式" style="width: 100%">
            <el-option label="固定" :value="1" />
            <el-option label="按比例" :value="2" />
            <el-option label="公式" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认值" prop="defaultAmount">
          <el-input-number v-model="form.defaultAmount" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="是否扣款" prop="isDeduction">
          <el-radio-group v-model="form.isDeduction">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
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
import { wageApi, type WageItemConfig } from '@/api/wage'
import RequestErrorState from '@/components/RequestErrorState.vue'

// --- Search & Table ---
const loading = ref(false)
const tableData = ref<WageItemConfig[]>([])
const errorMsg = ref('')
const searchForm = reactive({
  itemName: '',
  category: undefined as number | undefined,
})
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
})

// --- Dialog ---
const dialogVisible = ref(false)
const dialogTitle = ref('新增薪资项')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  itemName: '',
  itemKind: 1,
  calcType: 1,
  defaultAmount: 0,
  isDeduction: 0,
  sortOrder: 0,
  status: 1,
})

const formRules: FormRules = {
  itemName: [{ required: true, message: '请输入薪资项名称', trigger: 'blur' }],
  itemKind: [{ required: true, message: '请选择类别', trigger: 'change' }],
  calcType: [{ required: true, message: '请选择计算方式', trigger: 'change' }],
}

// --- Helpers ---
const categoryLabel = (val: number) => {
  const map: Record<number, string> = { 1: '基本工资', 2: '绩效', 3: '补贴', 4: '奖金', 5: '扣款', 6: '社保', 7: '公积金' }
  return map[val] ?? '未知'
}

const categoryTagType = (val: number) => {
  const map: Record<number, string> = { 1: '', 2: 'warning', 3: 'success', 4: 'danger', 5: 'info', 6: '', 7: '' }
  return (map[val] ?? '') as any
}

const calcTypeLabel = (val: number) => {
  const map: Record<number, string> = { 1: '固定', 2: '按比例', 3: '公式' }
  return map[val] ?? '未知'
}

function formatMoney(val: number) {
  if (val == null) return '0.00'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// --- Data Loading ---
async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      itemName: searchForm.itemName || undefined,
      category: searchForm.category || undefined,
    }
    const res = await wageApi.getItemConfigPage(pagination.current, pagination.size, params)
    const page = res as any
    tableData.value = page.records ?? page.list ?? []
    pagination.total = page.total ?? 0
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '薪资项配置加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.itemName = ''
  searchForm.category = undefined
  pagination.current = 1
  loadData()
}

// --- Status Toggle ---
async function handleToggleStatus(row: WageItemConfig) {
  try {
    await wageApi.updateItemConfig(row.id, { status: row.status })
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

// --- Add / Edit ---
function openAddDialog() {
  dialogTitle.value = '新增薪资项'
  editingId.value = null
  dialogVisible.value = true
}

function openEditDialog(row: WageItemConfig) {
  dialogTitle.value = '编辑薪资项'
  editingId.value = row.id
  Object.assign(form, {
    itemName: row.itemName,
    itemKind: row.itemKind,
    calcType: row.calcType,
    defaultAmount: row.defaultAmount,
    isDeduction: row.isDeduction,
    sortOrder: row.sortOrder,
    status: row.status,
  })
  dialogVisible.value = true
}

function handleDialogClosed() {
  formRef.value?.resetFields()
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
    if (editingId.value !== null) {
      await wageApi.updateItemConfig(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await wageApi.createItemConfig({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(editingId.value !== null ? '更新失败' : '新增失败')
  } finally {
    submitLoading.value = false
  }
}

// --- Delete ---
async function handleDelete(row: WageItemConfig) {
  try {
    await ElMessageBox.confirm('确定要删除该薪资项吗？', '提示', { type: 'warning' })
    await wageApi.deleteItemConfig(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.wage-item-config-page {
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
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #303133;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
