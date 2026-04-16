<template>
  <div class="page-container">
    <NexusSearchCard>
      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="仓库名称">
          <el-input v-model="queryParams.warehouseName" placeholder="请输入仓库名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
      </el-form>
      <template #actions>
        <div class="search-actions">
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        </div>
      </template>
    </NexusSearchCard>

    <NexusTableCard v-model:current="queryParams.current" v-model:size="queryParams.size" :loading="loading" :total="total" @pagination-change="loadData">
      <el-table :data="tableData" height="100%">
        <el-table-column prop="warehouseCode" label="仓库编码" min-width="160" />
        <el-table-column prop="warehouseName" label="仓库名称" min-width="180" />
        <el-table-column prop="managerName" label="负责人" width="140" />
        <el-table-column prop="contactInfo" label="联系电话" width="180" />
        <el-table-column prop="address" label="详细地址" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </NexusTableCard>

    <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />

    <el-dialog v-model="dialogVisible" width="620px" destroy-on-close class="nexus-dialog">
      <template #header>
        <span class="dialog-title">{{ form.id ? '编辑仓库' : '新增仓库' }}</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="仓库编码" prop="warehouseCode">
            <el-input v-model="form.warehouseCode" placeholder="请输入仓库编码" />
          </el-form-item>
          <el-form-item label="仓库名称" prop="warehouseName">
            <el-input v-model="form.warehouseName" placeholder="请输入仓库名称" />
          </el-form-item>
          <el-form-item label="负责人" prop="managerName">
            <el-input v-model="form.managerName" placeholder="请输入负责人姓名" />
          </el-form-item>
          <el-form-item label="联系电话" prop="contactInfo">
            <el-input v-model="form.contactInfo" placeholder="请输入联系电话" />
          </el-form-item>
        </div>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="3" placeholder="请输入详细地址" />
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
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { erpApi, type ErpWarehouse, type WarehouseUpsertDTO } from '@/api/erp'
import NexusSearchCard from '@/components/NexusSearchCard/index.vue'
import NexusTableCard from '@/components/NexusTableCard/index.vue'
import RequestErrorState from '@/components/RequestErrorState.vue'

const tableData = ref<ErpWarehouse[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const total = ref(0)
const errorMsg = ref('')

const queryParams = reactive({
  current: 1,
  size: 10,
  warehouseName: '',
})

const form = reactive<WarehouseUpsertDTO>({
  id: undefined,
  warehouseCode: '',
  warehouseName: '',
  managerName: '',
  contactInfo: '',
  address: '',
  status: 1,
})
const formRef = ref<FormInstance>()
const rules: FormRules<WarehouseUpsertDTO> = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    warehouseCode: '',
    warehouseName: '',
    managerName: '',
    contactInfo: '',
    address: '',
    status: 1,
  })
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getWarehousePage({
      current: queryParams.current,
      size: queryParams.size,
      warehouseName: queryParams.warehouseName.trim() || undefined,
    })
    tableData.value = (res.records ?? res.list ?? []) as ErpWarehouse[]
    total.value = res.total ?? 0
    if (res.current != null) queryParams.current = Number(res.current)
    if (res.size != null) queryParams.size = Number(res.size)
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '仓库列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.warehouseName = ''
  queryParams.current = 1
  loadData()
}

function handleAdd() {
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: ErpWarehouse) {
  Object.assign(form, {
    id: row.id,
    warehouseCode: row.warehouseCode,
    warehouseName: row.warehouseName,
    managerName: row.managerName || '',
    contactInfo: row.contactInfo || '',
    address: row.address || '',
    status: row.status,
  })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await erpApi.updateWarehouse(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await erpApi.addWarehouse(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该仓库?', '提示', { type: 'warning' })
    await erpApi.deleteWarehouse(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

async function toggleStatus(row: ErpWarehouse) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await erpApi.updateWarehouse(row.id, { ...row, status: newStatus })
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => loadData())
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
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
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
.nexus-dialog :deep(.el-dialog) {
  border-radius: 24px;
}
.dialog-title {
  font-size: 16px;
  font-weight: 700;
}
</style>
