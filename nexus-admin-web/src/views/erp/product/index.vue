<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="erp-tabs">
      <!-- 产品分类 -->
      <el-tab-pane label="产品分类" name="category">
        <el-card class="table-card" shadow="never">
          <template #header>
            <div class="toolbar">
              <el-button type="primary" :icon="Plus" @click="openCategoryDialog()">新增分类</el-button>
            </div>
          </template>
        <el-table :data="categoryList" stripe v-loading="categoryLoading" row-key="id">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="分类名称" />
          <el-table-column prop="parentId" label="上级ID" width="100" />
          <el-table-column prop="sortOrder" label="排序" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="openCategoryDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" link @click="handleDeleteCategory(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        </el-card>

        <!-- 分类对话框 -->
        <el-dialog v-model="categoryDialogVisible" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="500px" destroy-on-close>
          <el-form :model="categoryForm" label-width="80px">
            <el-form-item label="分类名称" required>
              <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
            </el-form-item>
            <el-form-item label="上级ID">
              <el-input-number v-model="categoryForm.parentId" :min="0" placeholder="上级分类ID，0表示顶级" style="width:100%" />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="categoryForm.sortOrder" :min="0" style="width:100%" />
            </el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="categoryForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="categoryDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="categorySaving" @click="handleSaveCategory">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 产品信息 -->
      <el-tab-pane label="产品信息" name="product">
        <el-card class="search-card" shadow="never">
          <div class="toolbar">
            <el-input v-model="productSearch" placeholder="按产品名称搜索" :clearable="true" style="width:240px;margin-right:8px" />
            <el-button type="primary" :icon="Plus" @click="openProductDialog()">新增产品</el-button>
          </div>
        </el-card>
        <el-card class="table-card" shadow="never">
        <el-table :data="productList" stripe v-loading="productLoading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="productCode" label="产品编码" width="140" />
          <el-table-column prop="productName" label="产品名称" />
          <el-table-column prop="categoryId" label="分类ID" width="100" />
          <el-table-column prop="specModel" label="规格型号" />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="price" label="单价" width="100" align="right">
            <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="stockQty" label="库存" width="100" align="center" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="openProductDialog(row)">编辑</el-button>
              <el-button size="small" type="warning" link @click="toggleProductStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" link @click="handleDeleteProduct(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="productPage"
          v-model:page-size="productSize"
          :total="productTotal"
          :page-sizes="[10,20,50]"
          layout="total,sizes,prev,pager,next"
          @current-change="loadProducts"
          @size-change="loadProducts"
          style="margin-top:16px"
        />
        </el-card>

        <!-- 产品对话框 -->
        <el-dialog v-model="productDialogVisible" :title="productForm.id ? '编辑产品' : '新增产品'" width="600px" destroy-on-close>
          <el-form :model="productForm" label-width="100px">
            <el-form-item label="产品编码" required>
              <el-input v-model="productForm.productCode" placeholder="请输入产品编码" />
            </el-form-item>
            <el-form-item label="产品名称" required>
              <el-input v-model="productForm.productName" placeholder="请输入产品名称" />
            </el-form-item>
            <el-form-item label="分类ID">
              <el-input-number v-model="productForm.categoryId" :min="0" style="width:100%" />
            </el-form-item>
            <el-form-item label="规格型号">
              <el-input v-model="productForm.specModel" placeholder="请输入规格型号" />
            </el-form-item>
            <el-form-item label="单位">
              <el-input v-model="productForm.unit" placeholder="如：个/箱/件" style="width:200px" />
            </el-form-item>
            <el-form-item label="单价">
              <el-input-number v-model="productForm.price" :min="0" :precision="2" style="width:200px" />
            </el-form-item>
            <el-form-item label="库存数量">
              <el-input-number v-model="productForm.stockQty" :min="0" style="width:200px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="productForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="productDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="productSaving" @click="handleSaveProduct">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { erpApi } from '@/api/erp'

const activeTab = ref('category')

// ---- Category ----
const categoryList = ref<any[]>([])
const categoryLoading = ref(false)
const categoryDialogVisible = ref(false)
const categorySaving = ref(false)
const categoryForm = reactive({
  id: undefined as number | undefined,
  name: '',
  parentId: 0,
  sortOrder: 0,
  status: 1,
})

async function loadCategories() {
  categoryLoading.value = true
  try {
    categoryList.value = await erpApi.getCategoryList()
  } catch {
    ElMessage.error('加载分类失败')
  } finally {
    categoryLoading.value = false
  }
}

function openCategoryDialog(row?: any) {
  if (row) {
    Object.assign(categoryForm, {
      id: row.id,
      name: row.name,
      parentId: row.parentId,
      sortOrder: row.sortOrder ?? 0,
      status: row.status,
    })
  } else {
    Object.assign(categoryForm, { id: undefined, name: '', parentId: 0, sortOrder: 0, status: 1 })
  }
  categoryDialogVisible.value = true
}

async function handleSaveCategory() {
  if (!categoryForm.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  categorySaving.value = true
  try {
    if (categoryForm.id) {
      await erpApi.updateCategory(categoryForm.id, categoryForm)
      ElMessage.success('更新成功')
    } else {
      await erpApi.createCategory(categoryForm)
      ElMessage.success('创建成功')
    }
    categoryDialogVisible.value = false
    loadCategories()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    categorySaving.value = false
  }
}

async function handleDeleteCategory(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该分类?', '提示', { type: 'warning' })
    await erpApi.deleteCategory(id)
    ElMessage.success('删除成功')
    loadCategories()
  } catch {}
}

// ---- Product ----
const productList = ref<any[]>([])
const productLoading = ref(false)
const productDialogVisible = ref(false)
const productSaving = ref(false)
const productSearch = ref('')
const productPage = ref(1)
const productSize = ref(10)
const productTotal = ref(0)
const productForm = reactive({
  id: undefined as number | undefined,
  productCode: '',
  productName: '',
  categoryId: 0,
  specModel: '',
  unit: '',
  price: 0,
  stockQty: 0,
  status: 1,
})

async function loadProducts() {
  productLoading.value = true
  try {
    const res = await erpApi.getProductInfoPage(productPage.value, productSize.value, undefined, productSearch.value)
    productList.value = res.records ?? res.list ?? []
    productTotal.value = res.total
  } catch {
    ElMessage.error('加载产品失败')
  } finally {
    productLoading.value = false
  }
}

function openProductDialog(row?: any) {
  if (row) {
    Object.assign(productForm, {
      id: row.id,
      productCode: row.productCode,
      productName: row.productName,
      categoryId: row.categoryId,
      specModel: row.specModel,
      unit: row.unit,
      price: row.price,
      stockQty: row.stockQty,
      status: row.status,
    })
  } else {
    Object.assign(productForm, {
      id: undefined,
      productCode: '',
      productName: '',
      categoryId: 0,
      specModel: '',
      unit: '',
      price: 0,
      stockQty: 0,
      status: 1,
    })
  }
  productDialogVisible.value = true
}

async function handleSaveProduct() {
  if (!productForm.productCode || !productForm.productName) {
    ElMessage.warning('请填写产品编码和名称')
    return
  }
  productSaving.value = true
  try {
    if (productForm.id) {
      await erpApi.updateProductInfo(productForm.id, productForm)
      ElMessage.success('更新成功')
    } else {
      await erpApi.createProductInfo(productForm)
      ElMessage.success('创建成功')
    }
    productDialogVisible.value = false
    loadProducts()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    productSaving.value = false
  }
}

async function handleDeleteProduct(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该产品?', '提示', { type: 'warning' })
    await erpApi.deleteProductInfo(id)
    ElMessage.success('删除成功')
    loadProducts()
  } catch {}
}

async function toggleProductStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await erpApi.updateProductStatus(row.id, newStatus)
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadProducts()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadCategories()
  loadProducts()
})
</script>

<style scoped>
.page-container { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.erp-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
</style>
