<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="登录时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="userAgent" label="User Agent" min-width="200" show-overflow-tooltip />
        <el-table-column prop="msg" label="登录信息" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="登录时间" width="180" />
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
import { ElMessage } from 'element-plus'
import { systemApi, type SysLoginLog } from '@/api/system'
import RequestErrorState from '@/components/RequestErrorState.vue'

const loading = ref(false)
const tableData = ref<SysLoginLog[]>([])
const errorMsg = ref('')
const total = ref(0)
const pagination = reactive({ page: 1, size: 10 })

const searchForm = reactive({
  dateRange: [] as string[],
})

async function fetchData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await systemApi.getLoginLogPage(pagination.page, pagination.size)
    let list = res.records ?? res.list ?? []
    // client-side time filter
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      const [start, end] = searchForm.dateRange
      list = list.filter(item => {
        const t = item.createTime
        return t >= start && t <= end
      })
    }
    tableData.value = list
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '登录日志加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.dateRange = []
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
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
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
