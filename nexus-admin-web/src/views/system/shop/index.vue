<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="店铺名称">
          <el-input v-model="searchForm.shopName" placeholder="请输入店铺名称" :clearable="true" style="width:200px" />
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
          <el-button type="primary" @click="openAddDialog">新增店铺</el-button>
        </div>
      </template>

      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orgId" label="组织ID" width="120" align="center" />
        <el-table-column prop="shopName" label="店铺名称" />
        <el-table-column prop="shopType" label="店铺类型" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              :type="row.status === 0 ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 0 ? '停用' : '启用' }}
            </el-button>
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

    <!-- 新增/编辑店铺弹窗 -->
    <el-dialog
:append-to-body="true"       v-model="dialogVisible"
      :title="isEdit ? '编辑店铺' : '新增店铺'"
      width="500px"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="店铺名称" required>
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="组织ID" required>
          <el-input-number v-model="form.orgId" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="店铺类型" required>
          <el-input-number v-model="form.shopType" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态" required>
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
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
import { systemApi, type ShopVO } from '@/api/system'
import RequestErrorState from '@/components/RequestErrorState.vue'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<ShopVO[]>([])
const errorMsg = ref('')
const total = ref(0)
const pagination = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const isEdit = ref(false)

const searchForm = reactive({ shopName: '' })

interface ShopForm {
  id?: number
  shopName: string
  orgId: number
  shopType: number
  status: number
}

const form = reactive<ShopForm>({
  shopName: '',
  orgId: 0,
  shopType: 0,
  status: 0,
})

async function fetchData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await systemApi.getShopPage(pagination.page, pagination.size, searchForm.shopName || undefined)
    tableData.value = res.records ?? res.list ?? []
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '店铺列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.shopName = ''
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, shopName: '', orgId: 0, shopType: 0, status: 0 })
}

function openAddDialog() {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

function openEditDialog(row: ShopVO) {
  isEdit.value = true
  form.id = row.id
  form.shopName = row.shopName
  form.orgId = row.orgId
  form.shopType = row.shopType
  form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.shopName || form.orgId === undefined) {
    ElMessage.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value && form.id) {
      await systemApi.updateShop(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await systemApi.createShop(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function toggleStatus(row: ShopVO) {
  const newStatus = row.status === 0 ? 1 : 0
  try {
    await ElMessageBox.confirm(`确定${newStatus === 0 ? '启用' : '停用'}该店铺吗？`, '提示', { type: 'warning' })
    await systemApi.updateShopStatus(row.id, newStatus)
    ElMessage.success('操作成功')
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
