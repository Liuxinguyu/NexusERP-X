<template>
  <div class="login-page">
    <div class="login-bg" aria-hidden="true" />

    <div class="login-shell">
      <div class="login-card">
        <header class="login-header">
          <h1 class="brand">NexusERP-X</h1>
          <p class="tagline">Welcome back. Please enter your details.</p>
        </header>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              clearable
              autocomplete="username"
              class="field-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
              class="field-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item label="租户" prop="tenantId">
            <el-select v-model="form.tenantId" placeholder="请选择租户" class="field-input field-select">
              <el-option
                v-for="item in tenantOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="captchaVisible" label="验证码" prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                placeholder="验证码"
                clearable
                class="field-input captcha-input"
                @keyup.enter="handleLogin"
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

          <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

          <el-form-item class="submit-wrap">
            <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin">
              {{ loading ? '登录中…' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <p class="login-footnote">默认账号：admin / admin</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
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

const tenantOptions = [{ label: '默认租户（ID: 1）', value: 1 }]

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
    ;(window as unknown as { __NEXUS_CAPTCHA_UUID__?: string }).__NEXUS_CAPTCHA_UUID__ = result.uuid
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
  } catch (e: unknown) {
    const err = e as { message?: string }
    errorMsg.value = err.message || '登录失败，请检查用户名、密码和验证码'
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
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 32px 20px;
}

.login-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  background: linear-gradient(165deg, #f8fafc 0%, #f1f5f9 45%, #eef2f7 100%);
}

.login-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.09) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.09) 1px, transparent 1px);
  background-size: 48px 48px;
  opacity: 0.5;
  pointer-events: none;
}

.login-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
}

.login-card {
  width: 100%;
  background: #fff;
  border-radius: 24px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.05);
  padding: 48px 40px;
}

.login-header {
  margin-bottom: 32px;
}

.brand {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: var(--text-primary);
  line-height: 1.2;
}

.tagline {
  margin: 10px 0 0;
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
}

.login-form :deep(.el-form-item__label) {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 6px !important;
  padding: 0 !important;
  line-height: 1.3;
}

.field-input :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 12px !important;
}

.field-select {
  width: 100%;
}

.field-select :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 12px !important;
}

.captcha-row {
  display: flex;
  align-items: stretch;
  gap: 10px;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-img {
  width: 112px;
  height: 44px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  flex-shrink: 0;
  overflow: hidden;
  background: #fff;
}

.error-banner {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 12px;
  font-size: 13px;
  color: var(--color-danger);
  background: rgba(220, 38, 38, 0.06);
  border: 1px solid rgba(220, 38, 38, 0.15);
}

.submit-wrap {
  margin-bottom: 0;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 700;
  border-radius: 12px !important;
  letter-spacing: 0.02em;
}

.login-footnote {
  margin: 24px 0 0;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
