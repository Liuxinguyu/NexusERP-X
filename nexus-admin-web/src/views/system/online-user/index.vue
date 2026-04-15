<template>
  <div class="page-container">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-button type="primary" @click="handleRefresh">刷新</el-button>
        <el-button
          type="danger"
          :disabled="selectedRows.length === 0"
          @click="handleKickBatch"
        >
          强制下线{{ selectedRows.length > 0 ? ` (${selectedRows.length})` : '' }}
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table
        ref="tableRef"
        :data="tableData"
        stripe
        border
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="shopId" label="店铺ID" width="100" align="center" />
        <el-table-column prop="shopName" label="店铺名称" />
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="loginTime" label="登录时间" width="180" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTable } from 'element-plus'
import { systemApi, type OnlineUser } from '@/api/system'
import RequestErrorState from '@/components/RequestErrorState.vue'

const loading = ref(false)
const tableData = ref<OnlineUser[]>([])
const errorMsg = ref('')
const total = ref(0)
const pagination = reactive({ page: 1, size: 10 })
const selectedRows = ref<OnlineUser[]>([])
const tableRef = ref<InstanceType<typeof ElTable>>()

async function fetchData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await systemApi.getOnlineUserPage(pagination.page, pagination.size)
    tableData.value = res.records ?? []
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '在线用户列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleRefresh() {
  pagination.page = 1
  selectedRows.value = []
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function handleSelectionChange(rows: OnlineUser[]) {
  selectedRows.value = rows
}

async function handleKickBatch() {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定强制下线 ${selectedRows.value.length} 位在线用户吗？`,
      '提示',
      { type: 'warning' }
    )
    for (const row of selectedRows.value) {
      try {
        await systemApi.kickUser(row.userId)
      } catch {
        // continue with next
      }
    }
    ElMessage.success('强制下线完成')
    selectedRows.value = []
    tableRef.value?.clearSelection()
    fetchData()
  } catch {
    // cancelled
  }
}

// init
fetchData()
</script>

<style scoped>
.page-container { padding: 16px; }
.toolbar-card { margin-bottom: 12px; }
.toolbar { display: flex; align-items: center; gap: 12px; }
.table-card { margin-bottom: 12px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
