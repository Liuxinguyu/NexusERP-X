<template>
  <div class="page-container">
    <NexusCard>
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="handleAddRoot">新增分类</el-button>
      </div>
      <el-table :data="categoryTree" row-key="id" :tree-props="{ children: 'children' }" :default-expand-all="false" v-loading="loading">
        <el-table-column prop="name" label="分类名称" min-width="220" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleAddChild(row)">新增下级</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </NexusCard>

    <el-dialog v-model="dialogVisible" width="560px" destroy-on-close class="nexus-dialog">
      <template #header>
        <span class="dialog-title">{{ form.id ? '编辑分类' : '新增分类' }}</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="上级ID" prop="parentId">
          <el-input-number v-model="form.parentId" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { erpApi, type ErpProductCategory, type ProductCategoryUpsertDTO } from '@/api/erp'
import NexusCard from '@/components/NexusCard/index.vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const categoryTree = ref<ErpProductCategory[]>([])

const form = reactive<ProductCategoryUpsertDTO>({
  id: undefined,
  name: '',
  parentId: 0,
  sortOrder: 0,
  status: 1,
})
const formRef = ref<FormInstance>()

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    categoryTree.value = await erpApi.getProductCategoryTree()
  } catch {
    ElMessage.error('加载分类树失败')
  } finally {
    loading.value = false
  }
}

function handleAddRoot() {
  Object.assign(form, { id: undefined, name: '', parentId: 0, sortOrder: 0, status: 1 })
  dialogVisible.value = true
}

function handleAddChild(row: ErpProductCategory) {
  Object.assign(form, { id: undefined, name: '', parentId: row.id, sortOrder: 0, status: 1 })
  dialogVisible.value = true
}

function handleEdit(row: ErpProductCategory) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    parentId: row.parentId,
    sortOrder: row.sortOrder ?? 0,
    status: row.status,
  })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await erpApi.updateProductCategory(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await erpApi.addProductCategory(form)
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
    await ElMessageBox.confirm('确认删除该分类？', '提示', { type: 'warning' })
    await erpApi.deleteCategory(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.page-container :deep(.el-table .el-table__indent) {
  position: relative;
}
.page-container :deep(.el-table .el-table__indent::after) {
  content: '';
  position: absolute;
  left: 8px;
  top: 0;
  bottom: 0;
  border-left: 1px solid #eef2f7;
}
.nexus-dialog :deep(.el-dialog) {
  border-radius: 24px;
}
.dialog-title {
  font-size: 16px;
  font-weight: 700;
}
</style>
