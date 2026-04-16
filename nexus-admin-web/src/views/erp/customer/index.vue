<template>
  <div class="page-container">
    <NexusSearchCard>
      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="客户名称">
          <el-input v-model="queryParams.name" placeholder="请输入客户名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="queryParams.contactName" placeholder="请输入联系人" clearable @keyup.enter="handleQuery" />
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
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="contactName" label="联系人" width="140" />
        <el-table-column prop="contactPhone" label="电话" width="160" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </NexusTableCard>

    <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />

    <el-dialog v-model="dialogVisible" width="560px" destroy-on-close class="nexus-dialog">
      <template #header>
        <span class="dialog-title">{{ form.id ? '编辑客户' : '新增客户' }}</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="客户名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
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
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { erpApi, type CustomerUpsertDTO, type ErpCustomer } from '@/api/erp'
import NexusSearchCard from '@/components/NexusSearchCard/index.vue'
import NexusTableCard from '@/components/NexusTableCard/index.vue'
import RequestErrorState from '@/components/RequestErrorState.vue'
import { useDict } from '@/hooks/useDict'

const tableData = ref<ErpCustomer[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const total = ref(0)
const errorMsg = ref('')

const queryParams = reactive({
  current: 1,
  size: 10,
  name: '',
  contactName: '',
})

const form = reactive<CustomerUpsertDTO>({
  id: undefined as number | undefined,
  name: '',
  contactName: '',
  contactPhone: '',
  address: '',
  status: 1,
})
const formRef = ref<FormInstance>()

const { dictMap } = useDict('erp_status')
const statusOptions = computed(() => dictMap.erp_status || [])
const rules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
}

function getStatusLabel(status: 0 | 1) {
  const matched = statusOptions.value.find((item) => Number(item.value) === status || item.value === String(status))
  return matched?.label || (status === 1 ? '启用' : '停用')
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getCustomerPage({
      current: queryParams.current,
      size: queryParams.size,
      name: queryParams.name.trim() || undefined,
      contactName: queryParams.contactName.trim() || undefined,
    })
    tableData.value = (res.records ?? res.list ?? []) as ErpCustomer[]
    total.value = res.total ?? 0
    if (res.current != null) queryParams.current = Number(res.current)
    if (res.size != null) queryParams.size = Number(res.size)
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '客户列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.name = ''
  queryParams.contactName = ''
  queryParams.current = 1
  loadData()
}

function handleAdd() {
  Object.assign(form, { id: undefined, name: '', contactName: '', contactPhone: '', address: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row: ErpCustomer) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      contactName: row.contactName,
      contactPhone: row.contactPhone,
      address: row.address || '',
      status: row.status,
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await erpApi.updateCustomer(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await erpApi.addCustomer(form)
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
