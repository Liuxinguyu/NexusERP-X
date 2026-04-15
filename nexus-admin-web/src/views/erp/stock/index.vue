<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <el-input v-model="filterProductId" placeholder="产品ID" :clearable="true" style="width:140px;margin-right:8px" type="number" @keyup.enter="handleSearch" />
        <el-input v-model="filterWarehouseId" placeholder="仓库ID" :clearable="true" style="width:140px;margin-right:8px" type="number" @keyup.enter="handleSearch" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      </div>
    </el-card>
    <el-card class="table-card" shadow="never">
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="productId" label="产品ID" width="100" />
      <el-table-column prop="productName" label="产品名称" />
      <el-table-column prop="warehouseId" label="仓库ID" width="100" />
      <el-table-column prop="warehouseName" label="仓库名称" />
      <el-table-column label="库存数量" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.qty < 10 ? 'danger' : row.qty < 50 ? 'warning' : 'success'" size="small">
            {{ row.qty }}
          </el-tag>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { erpApi } from '@/api/erp'
import RequestErrorState from '@/components/RequestErrorState.vue'

const tableData = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterProductId = ref<number | undefined>()
const filterWarehouseId = ref<number | undefined>()
const errorMsg = ref('')

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await erpApi.getStockPage(page.value, size.value, filterProductId.value, filterWarehouseId.value)
    tableData.value = res.records ?? res.list ?? []
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '库存列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
</style>
