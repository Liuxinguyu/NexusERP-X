<template>
  <div class="page-container">
    <NexusSearchCard>
      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="产品名称">
          <el-input v-model="queryParams.productName" placeholder="请输入产品名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-tree-select v-model="queryParams.categoryId" :data="categoryTree" node-key="id" check-strictly clearable filterable :props="{ label: 'name', children: 'children' }" style="width: 260px" placeholder="请选择分类" />
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
        <el-table-column prop="productCode" label="产品编码" width="140" />
        <el-table-column prop="productName" label="产品名称" min-width="180" />
        <el-table-column label="分类名称" min-width="160">
          <template #default="{ row }">{{ getCategoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="100" />
        <el-table-column prop="specModel" label="规格" min-width="160" show-overflow-tooltip />
        <el-table-column prop="minStock" label="最小库存" width="110" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </NexusTableCard>

    <el-dialog v-model="dialogVisible" width="620px" destroy-on-close class="nexus-dialog">
      <template #header>
        <span class="dialog-title">{{ form.id ? '编辑产品' : '新增产品' }}</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-tree-select v-model="form.categoryId" :data="categoryTree" node-key="id" check-strictly filterable :props="{ label: 'name', children: 'children' }" placeholder="请选择分类" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="请输入单位" />
        </el-form-item>
        <el-form-item label="规格" prop="specModel">
          <el-input v-model="form.specModel" placeholder="请输入规格" />
        </el-form-item>
        <el-form-item label="最小库存" prop="minStock">
          <el-input-number v-model="form.minStock" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" class="desc-textarea" placeholder="请输入产品描述" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { erpApi, type ErpProductCategory, type ErpProductInfo, type ProductUpsertDTO } from '@/api/erp'
import NexusSearchCard from '@/components/NexusSearchCard/index.vue'
import NexusTableCard from '@/components/NexusTableCard/index.vue'

const tableData = ref<ErpProductInfo[]>([])
const categoryTree = ref<ErpProductCategory[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const total = ref(0)

const queryParams = reactive({
  current: 1,
  size: 10,
  productName: '',
  categoryId: undefined as number | undefined,
})

const form = reactive<ProductUpsertDTO>({
  id: undefined,
  productCode: '',
  productName: '',
  categoryId: 0,
  unit: '',
  specModel: '',
  minStock: 0,
  description: '',
  status: 1,
})
const formRef = ref<FormInstance>()

const rules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const categoryNameMap = computed(() => {
  const map = new Map<number, string>()
  const loop = (nodes: ErpProductCategory[]) => {
    nodes.forEach((node) => {
      map.set(node.id, node.name)
      if (node.children?.length) loop(node.children)
    })
  }
  loop(categoryTree.value)
  return map
})

function getCategoryName(categoryId: number) {
  return categoryNameMap.value.get(categoryId) || '-'
}

async function loadCategoryTree() {
  try {
    categoryTree.value = await erpApi.getProductCategoryTree()
  } catch {
    ElMessage.error('加载分类失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await erpApi.getProductPage({
      current: queryParams.current,
      size: queryParams.size,
      categoryId: queryParams.categoryId,
      productName: queryParams.productName.trim() || undefined,
    })
    tableData.value = res.records ?? res.list ?? []
    total.value = res.total ?? 0
    if (res.current != null) queryParams.current = Number(res.current)
    if (res.size != null) queryParams.size = Number(res.size)
  } catch {
    ElMessage.error('加载产品失败')
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.current = 1
  queryParams.productName = ''
  queryParams.categoryId = undefined
  loadData()
}

function handleAdd() {
  Object.assign(form, { id: undefined, productCode: '', productName: '', categoryId: 0, unit: '', specModel: '', minStock: 0, description: '', status: 1 })
  dialogVisible.value = true
}

async function handleEdit(row: ErpProductInfo) {
  Object.assign(form, {
    id: row.id,
    productCode: row.productCode,
    productName: row.productName,
    categoryId: row.categoryId,
    unit: row.unit,
    specModel: row.specModel || '',
    minStock: (row as any).minStock || 0,
    description: (row as any).description || '',
    status: row.status,
  })
  try {
    const detail = await erpApi.getProductDetail(row.id)
    Object.assign(form, {
      specModel: detail.specModel || '',
      description: (detail as any).description || '',
      minStock: (detail as any).minStock || form.minStock || 0,
    })
  } catch {}
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await erpApi.updateProduct(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await erpApi.addProduct(form)
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

onMounted(async () => {
  await loadCategoryTree()
  await loadData()
})
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
.nexus-dialog :deep(.el-dialog) {
  border-radius: 24px;
}
.dialog-title {
  font-size: 16px;
  font-weight: 700;
}
.desc-textarea :deep(.el-textarea__inner) {
  resize: none;
  min-height: 120px !important;
}
.page-container :deep(.el-input__wrapper.is-focus),
.page-container :deep(.el-textarea__inner:focus),
.page-container :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #6366f1 inset !important;
  background: #fff !important;
}
</style>
