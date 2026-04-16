<template>
  <section class="nexus-card nexus-search-card">
    <div class="search-left">
      <slot />
    </div>
    <div v-if="$slots.actions" class="search-actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<style scoped>
.nexus-card {
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--ring-subtle);
}

.nexus-search-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
}

.search-left {
  flex: 1;
  min-width: 0;
}

.search-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
</style>
