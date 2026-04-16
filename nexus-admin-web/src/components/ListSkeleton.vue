<template>
  <div class="list-skeleton" :style="{ '--skeleton-rows': rows }">
    <div class="skeleton-head" :style="{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }">
      <span v-for="i in columns" :key="`head-${i}`" class="skeleton-head-cell shimmer" />
    </div>
    <div class="skeleton-body">
      <span
        v-for="i in rows"
        :key="`row-${i}`"
        class="skeleton-row shimmer"
        :style="{ width: `${92 - (i % 3) * 8}%` }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    rows?: number
    columns?: number
  }>(),
  {
    rows: 7,
    columns: 6,
  }
)
</script>

<style scoped>
.list-skeleton {
  height: 100%;
  min-height: 260px;
  border-radius: var(--radius-sm);
  padding: 10px 8px;
}

.skeleton-head {
  display: grid;
  gap: 16px;
  margin-bottom: 14px;
}

.skeleton-head-cell {
  height: 14px;
  border-radius: 999px;
  background: #eef2f7;
}

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-row {
  height: 12px;
  border-radius: 999px;
  background: #eef2f7;
}

.shimmer {
  position: relative;
  overflow: hidden;
}

.shimmer::after {
  content: '';
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.65), rgba(255, 255, 255, 0));
  animation: skeleton-shimmer 1.4s infinite;
}

@keyframes skeleton-shimmer {
  to {
    transform: translateX(100%);
  }
}
</style>
