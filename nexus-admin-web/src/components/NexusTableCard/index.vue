<template>
  <section class="nexus-card nexus-table-card">
    <div class="table-body">
      <ListSkeleton v-if="loading" />
      <slot v-else />
    </div>
    <el-pagination
      v-model:current-page="currentModel"
      v-model:page-size="sizeModel"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ListSkeleton from '@/components/ListSkeleton.vue'

const props = defineProps<{
  loading: boolean
  total: number
  current: number
  size: number
}>()

const emit = defineEmits<{
  (e: 'update:current', value: number): void
  (e: 'update:size', value: number): void
  (e: 'pagination-change'): void
}>()

const currentModel = computed({
  get: () => props.current,
  set: (value: number) => emit('update:current', value),
})

const sizeModel = computed({
  get: () => props.size,
  set: (value: number) => emit('update:size', value),
})

function handleCurrentChange() {
  emit('pagination-change')
}

function handleSizeChange() {
  emit('update:current', 1)
  emit('pagination-change')
}
</script>

<style scoped>
.nexus-card {
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--ring-subtle);
}

.nexus-table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.table-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
