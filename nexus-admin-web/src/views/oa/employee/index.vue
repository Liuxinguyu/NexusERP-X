<template>
  <div class="page-container employee-page">
    <aside class="org-panel">
      <div class="panel-header">
        <h3>组织架构</h3>
      </div>
      <div class="org-tree-wrap" v-loading="orgLoading">
        <el-tree
          :data="orgTree"
          node-key="id"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          :props="{ label: 'orgName', children: 'children' }"
          @node-click="handleOrgSelect"
        >
          <template #default="{ data }">
            <div class="org-node">
              <span>{{ data.orgName }}</span>
              <span v-if="data.userCount != null" class="org-count">{{ data.userCount }}</span>
            </div>
          </template>
        </el-tree>
      </div>
    </aside>

    <section class="content-panel">
      <NexusSearchCard>
        <el-form :inline="true" class="search-form" @submit.prevent>
          <el-form-item label="姓名">
            <el-input v-model="queryParams.name" placeholder="请输入姓名" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="工号">
            <el-input v-model="queryParams.empNo" placeholder="请输入工号" clearable @keyup.enter="handleQuery" />
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

      <NexusTableCard
        v-model:current="queryParams.current"
        v-model:size="queryParams.size"
        :loading="loading"
        :total="total"
        @pagination-change="loadData"
      >
        <el-table :data="tableData" height="100%">
          <el-table-column prop="empNo" label="工号" width="140" />
          <el-table-column prop="name" label="姓名" min-width="120" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column label="归属部门" min-width="160">
            <template #default="{ row }">{{ row.orgName || row.dept || '—' }}</template>
          </el-table-column>
          <el-table-column prop="hireDate" label="入职日期" width="130" />
          <el-table-column prop="position" label="职位" min-width="140" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
                {{ row.status === 1 ? '在职' : '离职' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </NexusTableCard>

      <RequestErrorState v-if="errorMsg && !loading" :description="errorMsg" @retry="loadData" />
    </section>

    <el-dialog :append-to-body="true" v-model="dialogVisible" width="680px" destroy-on-close class="employee-dialog">
      <template #header>
        <span class="dialog-title">{{ form.id ? '编辑员工' : '新增员工' }}</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="工号" prop="empNo">
            <el-input v-model="form.empNo" placeholder="请输入工号" />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="职位" prop="position">
            <el-input v-model="form.position" placeholder="请输入职位" />
          </el-form-item>
          <el-form-item label="归属组织" prop="orgId">
            <el-select v-model="form.orgId" placeholder="请选择组织" filterable>
              <el-option v-for="item in orgOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="关联系统账号">
            <el-select v-model="form.userId" placeholder="请选择系统账号" filterable clearable @change="handleUserChange">
              <el-option v-for="item in userOptions" :key="item.id" :label="userOptionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="入职日期">
            <el-date-picker
              v-model="form.hireDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择入职日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">在职</el-radio>
              <el-radio :value="0">离职</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { oaApi, type EmployeeDetail, type EmployeeUpsertDTO, type OaEmployee } from '@/api/oa'
import { systemApi, type OrgTreeNode, type SysUser } from '@/api/system'
import NexusSearchCard from '@/components/NexusSearchCard/index.vue'
import NexusTableCard from '@/components/NexusTableCard/index.vue'
import RequestErrorState from '@/components/RequestErrorState.vue'

const loading = ref(false)
const orgLoading = ref(false)
const saving = ref(false)
const total = ref(0)
const errorMsg = ref('')
const tableData = ref<OaEmployee[]>([])
const orgTree = ref<OrgTreeNode[]>([])
const userOptions = ref<SysUser[]>([])
const selectedOrgId = ref<number | undefined>()

const queryParams = reactive({
  current: 1,
  size: 10,
  name: '',
  empNo: '',
})

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<EmployeeUpsertDTO & { id?: number }>({
  id: undefined,
  empNo: '',
  name: '',
  phone: '',
  position: '',
  hireDate: '',
  status: 1,
  dept: '',
  orgId: undefined,
  orgName: '',
  userId: undefined,
  userName: '',
  directLeaderUserId: undefined,
})

const rules: FormRules<EmployeeUpsertDTO> = {
  empNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  orgId: [{ required: true, message: '请选择归属组织', trigger: 'change' }],
}

function flattenOrgTree(nodes: OrgTreeNode[]): OrgTreeNode[] {
  return nodes.flatMap((node: OrgTreeNode) => [node, ...(node.children ? flattenOrgTree(node.children) : [])])
}

const orgOptions = ref<Array<{ id: number; label: string }>>([])

function resetForm() {
  Object.assign(form, {
    id: undefined,
    empNo: '',
    name: '',
    phone: '',
    position: '',
    hireDate: '',
    status: 1,
    dept: '',
    orgId: selectedOrgId.value,
    orgName: '',
    userId: undefined,
    userName: '',
    directLeaderUserId: undefined,
  })
}

function userOptionLabel(user: SysUser) {
  return `${user.realName || user.username}（${user.username}）`
}

function syncOrgMeta() {
  const org = orgOptions.value.find((item) => item.id === form.orgId)
  form.orgName = org?.label || ''
  form.dept = org?.label || ''
}

function handleUserChange(userId?: number) {
  const user = userOptions.value.find((item) => item.id === userId)
  form.userName = user?.realName || user?.username || ''
}

async function loadOrgTree() {
  orgLoading.value = true
  try {
    const tree = await systemApi.getOrgTree()
    orgTree.value = tree
    orgOptions.value = flattenOrgTree(tree).map((item: OrgTreeNode) => ({ id: item.id, label: item.orgName }))
  } catch {
    ElMessage.error('加载组织树失败')
  } finally {
    orgLoading.value = false
  }
}

async function loadUserOptions() {
  try {
    const res = await systemApi.getUserPage(1, 500)
    userOptions.value = (res.records ?? res.list ?? []) as SysUser[]
  } catch {
    userOptions.value = []
  }
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await oaApi.getEmployeePage({
      current: queryParams.current,
      size: queryParams.size,
      name: queryParams.name.trim() || undefined,
      empNo: queryParams.empNo.trim() || undefined,
      orgId: selectedOrgId.value,
    })
    tableData.value = (res.records ?? res.list ?? []) as OaEmployee[]
    total.value = res.total ?? 0
    if (res.current != null) queryParams.current = Number(res.current)
    if (res.size != null) queryParams.size = Number(res.size)
  } catch {
    tableData.value = []
    errorMsg.value = '员工列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.name = ''
  queryParams.empNo = ''
  queryParams.current = 1
  loadData()
}

function handleOrgSelect(node: OrgTreeNode) {
  selectedOrgId.value = node.id
  queryParams.current = 1
  loadData()
}

async function handleAdd() {
  resetForm()
  await loadUserOptions()
  dialogVisible.value = true
}

async function handleEdit(row: OaEmployee) {
  await loadUserOptions()
  try {
    const detail = (await oaApi.getEmployeeDetail(row.id)) as EmployeeDetail
    Object.assign(form, {
      id: detail.id,
      empNo: detail.empNo,
      name: detail.name,
      phone: detail.phone,
      position: detail.position || '',
      hireDate: detail.hireDate || '',
      status: detail.status,
      dept: detail.dept || '',
      orgId: detail.orgId,
      orgName: detail.orgName || detail.dept || '',
      userId: detail.userId,
      userName: detail.userName || '',
      directLeaderUserId: detail.directLeaderUserId,
    })
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载员工详情失败')
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  syncOrgMeta()
  saving.value = true
  try {
    const payload: EmployeeUpsertDTO = {
      empNo: form.empNo,
      name: form.name,
      phone: form.phone,
      position: form.position,
      hireDate: form.hireDate,
      status: form.status,
      dept: form.dept,
      orgId: form.orgId,
      orgName: form.orgName,
      userId: form.userId,
      userName: form.userName,
      directLeaderUserId: form.directLeaderUserId,
    }
    if (form.id) {
      await oaApi.updateEmployee(form.id, payload)
      ElMessage.success('更新成功')
    } else {
      await oaApi.addEmployee(payload)
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
  await Promise.all([loadOrgTree(), loadUserOptions()])
  loadData()
})
</script>

<style scoped>
.employee-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  height: 100%;
}

.org-panel,
.content-panel {
  min-height: 0;
  overflow: hidden;
}

.org-panel {
  border: 1px solid #eef2f7;
  border-radius: 20px;
  background: #fff;
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

.panel-header {
  margin-bottom: 12px;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.org-tree-wrap {
  flex: 1;
  min-height: 320px;
  overflow: auto;
}

.org-node {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.org-count {
  color: #94a3b8;
  font-size: 12px;
}

.search-form {
  margin-bottom: 0;
}

.search-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.content-panel :deep(.el-table) {
  height: 100%;
}

.content-panel :deep(.el-table th.el-table__cell) {
  color: #64748b;
  font-weight: 600;
}

.content-panel :deep(.el-table td.el-table__cell),
.content-panel :deep(.el-table th.el-table__cell) {
  border-right: none !important;
}

.content-panel :deep(.el-table--border::after),
.content-panel :deep(.el-table--group::after),
.content-panel :deep(.el-table::before) {
  display: none;
}

.content-panel :deep(.el-table tr td.el-table__cell) {
  border-bottom: 1px solid #f1f5f9;
}

.employee-dialog :deep(.el-dialog) {
  border-radius: 24px;
}

.dialog-title {
  font-size: 16px;
  font-weight: 700;
}
</style>
