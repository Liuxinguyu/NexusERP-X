<template>
  <header class="app-topbar">
    <div class="left">
      <el-button text class="collapse-btn" @click="$emit('toggle-sidebar')">
        <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
      </el-button>
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
  collapsed: boolean
  moduleTitle: string
  latestNotice?: string
  shops: ShopItem[]
  currentShopName?: string
  username?: string
}>()

defineEmits<{
  (e: 'toggle-sidebar'): void
  (e: 'switch-shop', shopId: number): void
  (e: 'refresh'): void
  (e: 'logout'): void
}>()

const userInitial = computed(() => (props.username?.[0] || '?').toUpperCase())
</script>

<style scoped>
.app-topbar {
  height: var(--header-height);
  border-bottom: 1px solid var(--header-border);
  background: var(--header-bg);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
}

.left, .right { display: flex; align-items: center; gap: 10px; }
.module-title { font-size: 18px; font-weight: 700; letter-spacing: 0.2px; }

.collapse-btn { color: var(--text-secondary); }
.action-icon { color: var(--text-secondary); cursor: pointer; }

.shop-chip, .user-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 12px;
  cursor: pointer;
}

.shop-chip:hover, .user-chip:hover { background: #eef2ff; }
.user-name { font-size: 13px; font-weight: 600; }
.avatar { background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%); color: #fff; }
</style>
