<template>
  <div class="page-container dict-container">
    <div class="dict-layout">
      <!-- 左侧：字典类型 -->
      <div class="dict-types-card">
        <div class="border-b border-slate-100 p-4">
          <h3 class="font-bold text-slate-800">字典类型</h3>
        </div>
        <div class="types-list" v-loading="typesLoading">
          <div 
            v-for="item in typesList" 
            :key="item.id"
            class="type-item"
            :class="{ active: activeType === item.dictType }"
            @click="handleTypeClick(item.dictType)"
          >
            <div class="type-name font-medium">{{ item.dictName }}</div>
            <div class="type-code text-xs text-slate-500 mt-1">{{ item.dictType }}</div>
          </div>
          <el-empty v-if="!typesList.length && !typesLoading" description="暂无字典数据" :image-size="60" />
        </div>
      </div>

      <!-- 右侧：字典明细 -->
      <div class="dict-data-card table-card">
        <div class="border-b border-slate-100 p-4 flex justify-between">
          <h3 class="font-bold text-slate-800 flex items-center gap-2">
            字典数据
            <el-tag v-if="activeType" size="small" type="info">{{ activeType }}</el-tag>
          </h3>
        </div>
        <div class="p-4 flex-1">
          <el-table :data="itemsList" v-loading="itemsLoading" height="100%">
            <el-table-column prop="dictLabel" label="字典标签" min-width="120" />
            <el-table-column prop="dictValue" label="字典键值" min-width="120" />
            <el-table-column prop="dictSort" label="排序" width="80" align="center" />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { systemApi } from '@/api/system'

const typesLoading = ref(false)
const itemsLoading = ref(false)

const typesList = ref<any[]>([])
const itemsList = ref<any[]>([])
const activeType = ref<string>('')

async function loadTypes() {
  typesLoading.value = true
  try {
    const res = await systemApi.getDictTypes()
    typesList.value = res || []
    if (typesList.value.length > 0) {
      handleTypeClick(typesList.value[0].dictType)
    }
  } catch (e) {
    console.error(e)
  } finally {
    typesLoading.value = false
  }
}

async function loadItems(dictType: string) {
  if (!dictType) return
  itemsLoading.value = true
  try {
    const res = await systemApi.getDictItems(dictType)
    itemsList.value = res || []
  } catch (e) {
    console.error(e)
  } finally {
    itemsLoading.value = false
  }
}

function handleTypeClick(dictType: string) {
  activeType.value = dictType
  loadItems(dictType)
}

onMounted(() => {
  loadTypes()
})
</script>

<style scoped>
.dict-container {
  height: 100%;
}

.dict-layout {
  display: flex;
  gap: 24px;
  height: 100%;
  min-height: 0;
}

.dict-types-card {
  width: 280px;
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: var(--ring-subtle);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.types-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-item {
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.type-item:hover {
  background: var(--slate-50);
}

.type-item.active {
  background: var(--color-primary-soft);
  border-color: rgba(79, 70, 229, 0.2);
}

.type-item.active .type-name {
  color: var(--color-primary);
  font-weight: 600;
}

.type-item.active .type-code {
  color: rgba(79, 70, 229, 0.7);
}

.dict-data-card {
  flex: 1;
}

:deep(.el-table__inner-wrapper) {
  height: 100% !important;
}
</style>
