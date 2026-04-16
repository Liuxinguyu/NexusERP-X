<template>
  <section class="nexus-card">
    <slot />
  </section>
</template>

<style scoped>
.nexus-card {
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--ring-subtle);
  padding: 16px;
}
</style>
