<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <el-input v-model="searchName" placeholder="按客户名称搜索" :clearable="true" style="width:200px;margin-right:8px" @keyup.enter="handleSearch" />
        <el-input v-model="searchPhone" placeholder="按联系电话搜索" :clearable="true" style="width:200px;margin-right:8px" @keyup.enter="handleSearch" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增客户</el-button>
      </div>
    </el-card>
    <el-card class="table-card" shadow="never">
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="客户名称" />
      <el-table-column prop="contactName" label="联系人" width="120" />
      <el-table-column prop="contactPhone" label="联系电话" width="140" />
      <el-table-column label="等级" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.level === 'VIP'" type="danger" size="small">VIP</el-tag>
          <el-tag v-else-if="row.level === '重要'" type="warning" size="small">重要</el-tag>
          <el-tag v-else type="info" size="small">普通</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="信用额度" width="120" align="right">
        <template #default="{ row }">¥{{ row.creditLimit?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" link @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10,20,50]"
      layout="total,sizes,prev,pager,next"
      @current-change="loadData"
      @size-change="loadData"
      style="margin-top:16px"
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
          <el-select v-model="form.level" placeholder="请选择等级" style="width:100%">
            <el-option label="普通" value="普通" />
            <el-option label="重要" value="重要" />
            <el-option label="VIP" value="VIP" />
          </el-select>
        </el-form-item>
        <el-form-item label="信用额度">
          <el-input-number v-model="form.creditLimit" :min="0" :precision="2" style="width:100%" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { erpApi } from '@/api/erp'
import RequestErrorState from '@/components/RequestErrorState.vue'

const tableData = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchName = ref('')
const searchPhone = ref('')
const errorMsg = ref('')

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  contactName: '',
  contactPhone: '',
  level: '普通',
  creditLimit: 0,
})

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getCustomerPage(page.value, size.value, searchName.value || undefined, searchPhone.value || undefined)
    tableData.value = res.records ?? res.list ?? []
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '客户列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function openDialog(row?: any) {
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

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该客户?', '提示', { type: 'warning' })
    // Customer delete API not in erpApi, using generic approach
    await fetch(`/api/erp/customers/${id}`, { method: 'DELETE' })
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
</style>
