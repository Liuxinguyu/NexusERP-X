<template>
  <div class="request-error-state">
    <el-empty :description="description">
      <el-button type="primary" @click="$emit('retry')">重试</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ description?: string }>(), {
  description: '数据加载失败',
})

defineEmits<{ (e: 'retry'): void }>()
</script>

<style scoped>
.request-error-state {
  padding: 16px 0;
}
</style>

