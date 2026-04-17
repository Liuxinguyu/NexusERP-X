<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="queryForm" @submit.prevent>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:260px"
          />
        </el-form-item>
        <el-form-item label="员工姓名">
          <el-input v-model="queryForm.userName" placeholder="请输入员工姓名" :clearable="true" style="width:160px" />
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
        <div class="card-header"><span>考勤记录</span></div>
      </template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="userName" label="员工姓名" min-width="120" />
        <el-table-column prop="checkDate" label="考勤日期" width="120" />
        <el-table-column label="签到时间" width="120">
          <template #default="{ row }">{{ row.checkInTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="签退时间" width="120">
          <template #default="{ row }">{{ row.checkOutTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="工作时长(分钟)" width="120" align="center">
          <template #default="{ row }">{{ row.workMinutes || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { oaApi, type OaAttendanceRecord } from '@/api/oa'
import RequestErrorState from '@/components/RequestErrorState.vue'

const queryForm = reactive({
  dateRange: [] as string[],
  userName: '',
})

const tableData = ref<any[]>([])
const loading = ref(false)
const errorMsg = ref('')

const pagination = reactive({ current: 1, size: 10, total: 0 })

function statusLabel(status?: number) {
  const map: Record<number, string> = { 0: '正常', 1: '迟到', 2: '早退', 3: '缺勤' }
  return map[status ?? -1] ?? '未知'
}

function statusTagType(status?: number) {
  const map: Record<number, string> = { 0: 'success', 1: 'warning', 2: 'warning', 3: 'danger' }
  return (map[status ?? -1] ?? 'info') as any
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const params: any = {}
    if (queryForm.dateRange && queryForm.dateRange.length === 2) {
      params.startDate = queryForm.dateRange[0]
      params.endDate = queryForm.dateRange[1]
    }
    if (queryForm.userName) {
      params.userName = queryForm.userName
    }
    const res = await oaApi.getAttendanceRecords(pagination.current, pagination.size, params)
    tableData.value = res.records || res.list || []
    pagination.total = res.total
  } catch {
    tableData.value = []
    errorMsg.value = '考勤记录加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  queryForm.dateRange = []
  queryForm.userName = ''
  handleSearch()
}

onMounted(() => loadData())
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
  border-radius: 8px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
