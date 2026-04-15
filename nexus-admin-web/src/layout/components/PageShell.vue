<template>
  <div class="page-shell">
    <div class="inner">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.page-shell {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
}

.inner {
  max-width: 1360px;
  margin: 0 auto;
}
</style>
