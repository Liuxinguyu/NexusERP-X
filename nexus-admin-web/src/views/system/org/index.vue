<template>
  <div class="page-container">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-button type="primary" @click="openAddDialog({ id: 0, parentId: 0 })">新增组织</el-button>
      </div>
    </el-card>

    <el-card class="tree-card">
      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="{ label: 'orgName', children: 'children' }"
        node-key="id"
        default-expand-all
        lazy
        :load="loadNode"
      >
        <template #default="{ node, data }">
          <span class="tree-node-wrap">
            <span class="node-label">{{ data.orgName }}</span>
            <span class="node-actions">
              <el-button type="primary" link size="small" @click.stop="openAddDialog(data)">新增子级</el-button>
              <el-button type="primary" link size="small" @click.stop="openEditDialog(data)">编辑</el-button>
              <el-button type="danger" link size="small" @click.stop="handleDelete(data)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </el-card>

    <!-- 新增/编辑组织弹窗 -->
    <el-dialog
:append-to-body="true"       v-model="dialogVisible"
      :title="isEdit ? '编辑组织' : '新增组织'"
      width="500px"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="上级组织">
          <el-input :value="parentOrgName" disabled />
        </el-form-item>
        <el-form-item label="组织名称" required>
          <el-input v-model="form.orgName" placeholder="请输入组织名称" />
        </el-form-item>
        <el-form-item label="组织编码" required>
          <el-input v-model="form.orgCode" placeholder="请输入组织编码" />
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
import { ref, reactive, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTree } from 'element-plus'
import { systemApi, type OrgTreeNode } from '@/api/system'

const loading = ref(false)
const submitLoading = ref(false)
const treeData = ref<OrgTreeNode[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const dialogVisible = ref(false)
const isEdit = ref(false)
const parentOrgName = ref('')

interface OrgForm {
  id?: number
  parentId: number
  orgName: string
  orgCode: string
}

const form = reactive<OrgForm>({
  parentId: 0,
  orgName: '',
  orgCode: '',
})

async function initTree() {
  try {
    treeData.value = await systemApi.getOrgTree()
  } catch {
    ElMessage.error('加载组织树失败')
  }
}

async function loadNode(node: any, resolve: (data: OrgTreeNode[]) => void) {
  try {
    const data = await systemApi.getOrgTreeLazy(node.data.id)
    resolve(data)
  } catch {
    resolve([])
  }
}

function resetForm() {
  Object.assign(form, { id: undefined, parentId: 0, orgName: '', orgCode: '' })
}

function openAddDialog(data: Partial<OrgTreeNode> & { id: number }) {
  resetForm()
  isEdit.value = false
  form.parentId = data.id
  parentOrgName.value = data.orgName || '根组织'
  dialogVisible.value = true
}

function openEditDialog(data: OrgTreeNode) {
  isEdit.value = true
  form.id = data.id
  form.parentId = data.parentId
  form.orgName = data.orgName
  form.orgCode = data.orgCode
  // find parent name
  parentOrgName.value = data.parentId === 0 ? '根组织' : (treeData.value.find(n => n.id === data.parentId)?.orgName || '')
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.orgName || !form.orgCode) {
    ElMessage.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value && form.id) {
      await systemApi.updateOrg(form)
      ElMessage.success('更新成功')
    } else {
      await systemApi.createOrg(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await nextTick()
    initTree()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(data: OrgTreeNode) {
  try {
    await ElMessageBox.confirm('确定删除该组织吗？', '提示', { type: 'warning' })
    await systemApi.deleteOrg(data.id)
    ElMessage.success('删除成功')
    initTree()
  } catch {
    // cancelled
  }
}

// init
initTree()
</script>

<style scoped>
.page-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.toolbar-card { flex-shrink: 0; margin-bottom: 12px; }
.tree-card { flex: 1; min-height: 0; overflow: auto; }
.toolbar { display: flex; align-items: center; }
.tree-node-wrap { flex: 1; display: flex; align-items: center; justify-content: space-between; width: 100%; }
.node-label { font-size: 14px; }
.node-actions { display: flex; gap: 8px; }
</style>
