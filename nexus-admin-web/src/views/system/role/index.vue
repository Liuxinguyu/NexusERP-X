<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="角色名称">
          <el-input v-model="searchForm.roleName" placeholder="请输入角色名称" :clearable="true" style="width:200px" />
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
          <el-button type="primary" @click="openAddDialog">新增角色</el-button>
        </div>
      </template>

      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="shopId" label="店铺ID" width="120" align="center" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="dataScope" label="数据范围" width="120" align="center" />
        <el-table-column prop="delFlag" label="删除标记" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.delFlag === 0 ? 'success' : 'danger'" size="small">
              {{ row.delFlag === 0 ? '正常' : '已删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openMenuDialog(row)">分配菜单</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="500px"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" required>
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-input-number v-model="form.dataScope" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单弹窗 -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单" width="400px" destroy-on-close>
      <el-tree
        ref="menuTreeRef"
        :data="menuTreeData"
        :props="{ label: 'menuName', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuLoading" @click="handleAssignMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTree } from 'element-plus'
import { systemApi, type SysRole, type MenuNode } from '@/api/system'
import RequestErrorState from '@/components/RequestErrorState.vue'

const loading = ref(false)
const submitLoading = ref(false)
const menuLoading = ref(false)
const tableData = ref<SysRole[]>([])
const errorMsg = ref('')
const total = ref(0)
const pagination = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const menuDialogVisible = ref(false)
const isEdit = ref(false)
const currentRoleId = ref<number | null>(null)

const searchForm = reactive({ roleName: '' })

interface RoleForm {
  id?: number
  roleName: string
  roleCode: string
  dataScope: number
  remark: string
}

const form = reactive<RoleForm>({
  roleName: '',
  roleCode: '',
  dataScope: 0,
  remark: '',
})

const menuTreeData = ref<MenuNode[]>([])
const menuTreeRef = ref<InstanceType<typeof ElTree>>()

async function fetchData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await systemApi.getRolePage(pagination.page, pagination.size, searchForm.roleName || undefined)
    tableData.value = res.records ?? res.list ?? []
    total.value = res.total
  } catch {
    ElMessage.error('加载数据失败')
    errorMsg.value = '角色列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.roleName = ''
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, roleName: '', roleCode: '', dataScope: 0, remark: '' })
}

function openAddDialog() {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

function openEditDialog(row: SysRole) {
  isEdit.value = true
  form.id = row.id
  form.roleName = row.roleName
  form.roleCode = row.roleCode
  form.dataScope = row.dataScope
  form.remark = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.roleName || !form.roleCode) {
    ElMessage.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value && form.id) {
      await systemApi.updateRole(form.id, form)
      ElMessage.success('更新成功')
      dialogVisible.value = false
    } else {
      const id = await systemApi.createRole(form)
      ElMessage.success('新增成功')
      dialogVisible.value = false
      await nextTick()
      openMenuDialog({ ...form, id } as any)
    }
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: SysRole) {
  try {
    await ElMessageBox.confirm('确定删除该角色吗？', '提示', { type: 'warning' })
    await systemApi.updateRole(row.id, { delFlag: 1 } as any)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // cancelled
  }
}

async function openMenuDialog(row: SysRole) {
  currentRoleId.value = row.id
  menuDialogVisible.value = true
  menuLoading.value = true
  try {
    const [treeData, checkedIds] = await Promise.all([
      systemApi.getMenuTree(),
      systemApi.getRoleMenuIds(row.id),
    ])
    menuTreeData.value = treeData
    await nextTick()
    menuTreeRef.value?.setCheckedKeys(checkedIds)
  } catch {
    ElMessage.error('加载菜单数据失败')
  } finally {
    menuLoading.value = false
  }
}

async function handleAssignMenus() {
  if (!currentRoleId.value) return
  const checkedNodes = menuTreeRef.value?.getCheckedNodes() || []
  const halfCheckedNodes = menuTreeRef.value?.getHalfCheckedNodes() || []
  const allNodes = [...checkedNodes, ...halfCheckedNodes]
  const menuIds = allNodes.map(n => (n as MenuNode).id)
  try {
    await systemApi.assignRoleMenus(currentRoleId.value, menuIds)
    ElMessage.success('分配成功')
    menuDialogVisible.value = false
  } catch {
    ElMessage.error('分配失败')
  }
}

// init
fetchData()
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 12px; }
.table-card { margin-bottom: 12px; }
.toolbar { display: flex; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
