<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="客户名称">
          <el-input
            v-model="queryForm.name"
            placeholder="按客户名称搜索"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input
            v-model="queryForm.contactPhone"
            placeholder="按联系电话搜索"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="openDialog()">新增客户</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="客户名称" min-width="160" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="info" effect="plain">停用</el-tag>
            <el-tag v-else-if="row.status === 1" type="success" effect="plain">正常</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.level === 'VIP'" type="danger" size="small">VIP</el-tag>
            <el-tag v-else-if="row.level === '重要'" type="warning" size="small">重要</el-tag>
            <el-tag v-else type="info" size="small">普通</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="信用额度" width="120" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.creditLimit) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="handleSizeChange"
        class="pagination"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑客户' : '新增客户'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="客户名称" required>
          <el-input v-model="form.name" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="客户等级">
          <el-select v-model="form.level" placeholder="请选择等级" style="width: 100%">
            <el-option label="普通" value="普通" />
            <el-option label="重要" value="重要" />
            <el-option label="VIP" value="VIP" />
          </el-select>
        </el-form-item>
        <el-form-item label="信用额度">
          <el-input-number v-model="form.creditLimit" :min="0" :precision="2" style="width: 100%" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { erpApi, type ErpCustomer } from '@/api/erp'
import RequestErrorState from '@/components/RequestErrorState.vue'

const tableData = ref<ErpCustomer[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const errorMsg = ref('')

const queryForm = reactive({
  name: '',
  contactPhone: '',
})

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  contactName: '',
  contactPhone: '',
  level: '普通',
  creditLimit: 0,
})

function formatMoney(v: number | undefined | null) {
  if (v === undefined || v === null || Number.isNaN(Number(v))) return '0.00'
  return Number(v).toFixed(2)
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getCustomerPage(
      page.value,
      size.value,
      queryForm.name.trim() || undefined,
      queryForm.contactPhone.trim() || undefined
    )
    tableData.value = (res.records ?? res.list ?? []) as ErpCustomer[]
    total.value = res.total ?? 0
    if (res.current != null) page.value = Number(res.current)
    if (res.size != null) size.value = Number(res.size)
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '客户列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  page.value = 1
  loadData()
}

function resetQuery() {
  queryForm.name = ''
  queryForm.contactPhone = ''
  page.value = 1
  loadData()
}

function handleSizeChange() {
  page.value = 1
  loadData()
}

function openDialog(row?: ErpCustomer) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      contactName: row.contactName,
      contactPhone: row.contactPhone,
      level: row.level,
      creditLimit: row.creditLimit,
    })
  } else {
    Object.assign(form, { id: undefined, name: '', contactName: '', contactPhone: '', level: '普通', creditLimit: 0 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name) {
    ElMessage.warning('请输入客户名称')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await erpApi.updateCustomer(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await erpApi.createCustomer(form)
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
.page-container {
  padding: 16px;
}
.search-form {
  margin-bottom: 0;
}
.pagination {
  margin-top: 16px;
}
.muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
