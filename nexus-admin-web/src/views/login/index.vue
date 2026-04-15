<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <el-icon class="logo-icon"><Monitor /></el-icon>
          <span class="logo-text">NexusERP</span>
        </div>
        <h1 class="brand-title">企业级管理系统</h1>
        <p class="brand-desc">集成 ERP / OA / 薪资管理的一体化企业云平台</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>多模块业务集成</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>灵活权限体系</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>实时数据看板</span>
          </div>
        </div>
      </div>
      <div class="brand-footer">
        <span>© {{ new Date().getFullYear() }} NexusERP. All rights reserved.</span>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-form-area">
      <div class="login-card">
        <div class="login-card-header">
          <h2>登录账户</h2>
          <p>请输入您的账号信息</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @submit.prevent="handleLogin"
          size="large"
        >
          <!-- 用户名 -->
          <el-form-item prop="username">
            <label class="form-label">用户名</label>
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
              autocomplete="username"
            />
          </el-form-item>

          <!-- 密码 -->
          <el-form-item prop="password">
            <label class="form-label">密码</label>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item prop="tenantId">
            <label class="form-label">租户</label>
            <el-select v-model="form.tenantId" placeholder="请选择租户">
              <el-option
                v-for="item in tenantOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <!-- 验证码（后端启用时才显示） -->
          <el-form-item v-if="captchaVisible" prop="captchaCode">
            <label class="form-label">验证码</label>
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                :prefix-icon="CircleCheck"
                clearable
                @keyup.enter="handleLogin"
                style="flex: 1"
              />
              <el-image
                :src="captchaImg"
                alt="验证码"
                fit="contain"
                class="captcha-img"
                @click="refreshCaptcha"
              />
            </div>
          </el-form-item>

          <!-- 错误提示 -->
          <div v-if="errorMsg" class="error-message">
            <el-icon><CircleClose /></el-icon>
            {{ errorMsg }}
          </div>

          <!-- 登录按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 底部提示 -->
        <div class="login-footer">
          <span>默认账号：admin / admin</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, CircleCheck, CircleClose, Check, Monitor } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')
const captchaVisible = ref(false)

const captchaUuid = ref('')
const captchaImg = ref('')

const form = reactive({
  username: '',
  password: '',
  tenantId: 1,
  captchaCode: '',
  captchaKey: '',
})

const tenantOptions = [
  { label: '默认租户（ID: 1）', value: 1 },
]

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  tenantId: [{ required: true, message: '请选择租户', trigger: 'change' }],
}

async function refreshCaptcha() {
  try {
    const result = await authApi.getCaptchaImage()
    captchaUuid.value = result.uuid
    captchaImg.value = result.img
    captchaVisible.value = true
    ;(window as any).__NEXUS_CAPTCHA_UUID__ = result.uuid
  } catch {
    captchaVisible.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})

async function handleLogin() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    form.captchaKey = captchaVisible.value ? captchaUuid.value : ''
    await userStore.loginAction({
      username: form.username.trim(),
      password: form.password,
      tenantId: form.tenantId,
      captcha: captchaVisible.value ? form.captchaCode : '',
      captchaKey: captchaVisible.value ? form.captchaKey : '',
    })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: any) {
    errorMsg.value = e.message || '登录失败，请检查用户名、密码和验证码'
    if (String(errorMsg.value).includes('验证码')) {
      captchaVisible.value = true
      refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ---- Layout ---- */
.login-page {
  min-height: 100vh;
  display: flex;
}

/* ---- Left Brand Panel ---- */
.login-brand {
  flex: 1;
  background: var(--color-primary);
  background-image: radial-gradient(ellipse at 30% 20%, #3d8fc7 0%, var(--color-primary) 60%);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 60px 56px;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 600px;
  height: 600px;
  background: rgba(255,255,255,0.04);
  border-radius: 50%;
}

.login-brand::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 400px;
  height: 400px;
  background: rgba(255,255,255,0.03);
  border-radius: 50%;
}

.brand-content { position: relative; z-index: 1; }

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 48px;
}

.logo-icon { font-size: 32px; color: #fff; }
.logo-text { font-size: 24px; font-weight: 700; color: #fff; letter-spacing: 1px; }

.brand-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  line-height: 1.2;
}

.brand-desc {
  font-size: 15px;
  color: rgba(255,255,255,0.75);
  margin-bottom: 48px;
  line-height: 1.6;
}

.brand-features { display: flex; flex-direction: column; gap: 14px; }

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: rgba(255,255,255,0.9);
}

.feature-item .el-icon {
  color: rgba(255,255,255,0.7);
  font-size: 14px;
}

.brand-footer {
  font-size: 12px;
  color: rgba(255,255,255,0.4);
  position: relative;
  z-index: 1;
}

/* ---- Right Form Panel ---- */
.login-form-area {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--page-bg);
  padding: 40px 48px;
  flex-shrink: 0;
}

.login-card {
  width: 100%;
  max-width: 380px;
}

.login-card-header {
  margin-bottom: 32px;
}

.login-card-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.login-card-header p {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---- Form ---- */
.login-form { margin-top: 0; }

.form-label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-img {
  width: 120px;
  height: 38px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color var(--transition-fast);
}

.captcha-img:hover { border-color: var(--color-primary); }

.login-btn {
  width: 100%;
  height: 42px;
  font-size: var(--font-size-base);
  font-weight: 600;
  letter-spacing: 2px;
  background: var(--color-primary);
  border-color: var(--color-primary);
  border-radius: var(--border-radius);
  margin-top: 8px;
}

.login-btn:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary-light);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: var(--color-danger-bg);
  border: 1px solid #f5c6cb;
  border-radius: var(--border-radius);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  margin-bottom: 4px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 12px;
  color: var(--text-muted);
}

/* Responsive */
@media (max-width: 900px) {
  .login-brand { display: none; }
  .login-form-area { width: 100%; }
}
</style>
