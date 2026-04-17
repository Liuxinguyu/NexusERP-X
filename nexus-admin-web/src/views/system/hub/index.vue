<template>
  <div class="page-container hub-container">
    <div class="hub-header mb-10">
      <h1 class="text-3xl font-extrabold text-slate-900 tracking-tight">系统设置控制中心</h1>
      <p class="text-slate-500 mt-2 text-lg">统一管理企业身份、组织架构与系统底层基座。</p>
    </div>

    <div class="hub-grid">

      <div class="hub-section">
        <h2 class="section-title">👤 身份与权限</h2>
        <div class="card-grid">
          <div class="hub-card" @click="go('/system/user')">
            <div class="icon-box bg-blue-50 text-blue-600"><el-icon><User /></el-icon></div>
            <div class="content">
              <h3>用户管理</h3>
              <p>管理系统登录账号与密码配置</p>
            </div>
          </div>
          <div class="hub-card" @click="go('/system/role')">
            <div class="icon-box bg-indigo-50 text-indigo-600"><el-icon><Key /></el-icon></div>
            <div class="content">
              <h3>角色权限</h3>
              <p>配置角色并分配功能菜单权限</p>
            </div>
          </div>
        </div>
      </div>

      <div class="hub-section">
        <h2 class="section-title">🏢 组织架构</h2>
        <div class="card-grid">
          <div class="hub-card" @click="go('/system/org')">
            <div class="icon-box bg-emerald-50 text-emerald-600"><el-icon><OfficeBuilding /></el-icon></div>
            <div class="content">
              <h3>部门管理</h3>
              <p>维护企业内部组织架构树</p>
            </div>
          </div>
          <div class="hub-card" @click="go('/system/shop')">
            <div class="icon-box bg-teal-50 text-teal-600"><el-icon><Shop /></el-icon></div>
            <div class="content">
              <h3>店铺管理</h3>
              <p>配置大区与门店树形结构</p>
            </div>
          </div>
        </div>
      </div>

      <div class="hub-section">
        <h2 class="section-title">🛠️ 系统基座</h2>
        <div class="card-grid">
          <div class="hub-card" @click="go('/system/menu')">
            <div class="icon-box bg-slate-100 text-slate-600"><el-icon><Menu /></el-icon></div>
            <div class="content">
              <h3>菜单配置</h3>
              <p>系统左侧导航栏与路由绑定</p>
            </div>
          </div>
          <div class="hub-card" @click="go('/system/dict')">
            <div class="icon-box bg-slate-100 text-slate-600"><el-icon><Collection /></el-icon></div>
            <div class="content">
              <h3>字典管理</h3>
              <p>系统各类状态与下拉框枚举配置</p>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { User, Key, OfficeBuilding, Shop, Menu, Collection } from '@element-plus/icons-vue'

const router = useRouter()
const go = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.hub-container {
  max-width: 1200px;
  margin: 0 auto;
}

.hub-grid {
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.hub-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: flex-start;
  gap: 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.hub-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01);
  border-color: #e2e8f0;
}

.icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.content h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 4px;
}

.content p {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}
</style>
