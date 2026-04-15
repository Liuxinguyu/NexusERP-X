<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <el-input v-model="searchName" placeholder="按仓库名称搜索" :clearable="true" style="width:240px;margin-right:8px" @keyup.enter="handleSearch" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增仓库</el-button>
      </div>
    </el-card>
    <el-card class="table-card" shadow="never">
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="warehouseCode" label="仓库编码" width="140" />
      <el-table-column prop="warehouseName" label="仓库名称" />
      <el-table-column prop="managerName" label="管理员" width="120" />
      <el-table-column prop="contactInfo" label="联系方式" width="140" />
      <el-table-column prop="address" label="地址" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="warning" link @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑仓库' : '新增仓库'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="仓库编码" required>
          <el-input v-model="form.warehouseCode" placeholder="请输入仓库编码" />
        </el-form-item>
        <el-form-item label="仓库名称" required>
          <el-input v-model="form.warehouseName" placeholder="请输入仓库名称" />
        </el-form-item>
        <el-form-item label="管理员">
          <el-input v-model="form.managerName" placeholder="请输入管理员姓名" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.contactInfo" placeholder="请输入联系方式" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入仓库地址" />
        </el-form-item>
        <el-form-item label="状态">
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
const errorMsg = ref('')

const form = reactive({
  id: undefined as number | undefined,
  warehouseCode: '',
  warehouseName: '',
  managerName: '',
  contactInfo: '',
  address: '',
  status: 1,
})

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getWarehousePage(page.value, size.value, searchName.value || undefined)
    tableData.value = res.records ?? res.list ?? []
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '仓库列表加载失败'
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
      warehouseCode: row.warehouseCode,
      warehouseName: row.warehouseName,
      managerName: row.managerName,
      contactInfo: row.contactInfo,
      address: row.address,
      status: row.status,
    })
  } else {
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
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.warehouseCode || !form.warehouseName) {
    ElMessage.warning('请填写仓库编码和名称')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await erpApi.updateWarehouse(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await erpApi.createWarehouse(form)
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

async function toggleStatus(row: any) {
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
.page-container { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
</style>
