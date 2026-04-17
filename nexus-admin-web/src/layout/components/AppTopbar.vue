<template>
  <header class="app-topbar">
    <div class="left">
      <div class="module-title">{{ moduleTitle || '工作台' }}</div>
    </div>

    <div class="right">
      <el-tooltip v-if="latestNotice" :content="latestNotice" placement="bottom" :show-after="400">
        <el-icon class="action-icon"><Bell /></el-icon>
      </el-tooltip>

      <el-dropdown v-if="shops.length > 0" @command="(v:number) => $emit('switch-shop', v)" trigger="click">
        <span class="shop-chip">
          <el-icon><Shop /></el-icon>
          {{ currentShopName || '选择店铺' }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="shop in shops"
              :key="shop.shopId"
              :command="shop.shopId"
            >
              {{ shop.shopName }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-dropdown trigger="click">
        <div class="user-chip">
          <el-avatar :size="30" class="avatar">{{ userInitial }}</el-avatar>
          <span class="user-name">{{ username || '-' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="$emit('refresh')">刷新页面</el-dropdown-item>
            <el-dropdown-item divided @click="$emit('logout')">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type ShopItem = { shopId: number; shopName: string }

const props = defineProps<{
  moduleTitle: string
  latestNotice?: string
  shops: ShopItem[]
  currentShopName?: string
  username?: string
}>()

defineEmits<{
  (e: 'switch-shop', shopId: number): void
  (e: 'refresh'): void
  (e: 'logout'): void
}>()

const userInitial = computed(() => (props.username?.[0] || '?').toUpperCase())
</script>

<style scoped>
.app-topbar {
  height: var(--header-height);
  flex-shrink: 0;
  border-bottom: 1px solid var(--header-border);
  background: var(--header-bg);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.left, .right { display: flex; align-items: center; gap: 12px; }
.module-title { font-size: 18px; font-weight: 700; letter-spacing: 0.1px; }
.action-icon { color: var(--text-secondary); cursor: pointer; }

.shop-chip, .user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 12px;
  border: 1px solid transparent;
  cursor: pointer;
}

.shop-chip:hover, .user-chip:hover {
  background: #f8faff;
  border-color: var(--border-color);
}
.user-name { font-size: 13px; font-weight: 600; }
.avatar { background: var(--color-primary); color: #fff; }
</style>
