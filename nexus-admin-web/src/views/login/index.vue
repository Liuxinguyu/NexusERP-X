<template>
  <div class="login-page">
    <div class="login-shell">
      <section class="login-card">
        <div class="login-header">
          <span class="login-kicker">NexusERP-X · 企业管理平台</span>
          <h1>NexusERP-X</h1>
          <p v-if="step === 1">请输入账号信息进行登录</p>
          <p v-else>请选择要进入的店铺</p>
        </div>

        <div class="login-stepbar" aria-label="登录步骤">
          <div class="step-item" :class="{ active: step === 1, done: step === 2 }">
            <span class="step-index">1</span>
            <div>
              <strong>账号登录</strong>
              <small>校验账号信息</small>
            </div>
          </div>
          <div class="step-divider" />
          <div class="step-item" :class="{ active: step === 2 }">
            <span class="step-index">2</span>
            <div>
              <strong>选择店铺</strong>
              <small>进入业务工作台</small>
            </div>
          </div>
        </div>

        <transition name="fade-slide" mode="out-in">
          <el-form
            v-if="step === 1"
            key="step1"
            ref="accountFormRef"
            :model="accountForm"
            :rules="accountRules"
            label-position="top"
            class="login-form"
            @submit.prevent="handleNextStep"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="accountForm.username" placeholder="请输入用户名" @keyup.enter="handleNextStep" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="accountForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
                @keyup.enter="handleNextStep"
              />
            </el-form-item>
            <el-form-item v-if="captchaVisible" label="验证码" prop="captcha" class="captcha-form-item">
              <div class="captcha-row">
                <el-input
                  v-model="accountForm.captcha"
                  placeholder="请输入验证码"
                  :disabled="captchaLoading"
                  @keyup.enter="handleNextStep"
                />
                <button
                  type="button"
                  class="captcha-image-button"
                  :disabled="captchaLoading"
                  @click="loadCaptcha(true)"
                >
                  <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-image" />
                  <span v-else class="captcha-image-placeholder">加载中</span>
                </button>
              </div>
              <button type="button" class="captcha-refresh" :disabled="captchaLoading" @click="loadCaptcha(true)">
                {{ captchaLoading ? '正在刷新验证码...' : '看不清？换一张' }}
              </button>
            </el-form-item>
            <el-button type="primary" :loading="loading" class="submit-btn" @click="handleNextStep">下一步</el-button>
          </el-form>

          <div v-else key="step2" class="step2-container">
            <div class="shop-panel">
              <div class="shop-tip">
                <strong>已完成账号校验</strong>
                <span>请选择本次要进入的店铺后继续</span>
              </div>
              <el-select
                v-model="selectedShopId"
                placeholder="请选择店铺"
                class="nexus-tree-select"
                filterable
                clearable
              >
                <el-option
                  v-for="item in shopOptions"
                  :key="item.id"
                  :label="item.label"
                  :value="item.id"
                />
              </el-select>
              <div v-if="!hasShops" class="shop-empty">
                当前账号未分配可访问店铺，请联系管理员
              </div>
            </div>
            <el-button type="primary" :loading="loading" class="submit-btn" :disabled="!hasShops" @click="handleEnterSystem">
              进入系统
            </el-button>
            <el-button class="back-btn" @click="handleBack">返回上一步</el-button>
          </div>
        </transition>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const step = ref<1 | 2>(1)
const loading = ref(false)
const captchaVisible = ref(false)
const captchaLoading = ref(false)
const captchaImage = ref('')
const selectedShopId = ref<number>()
const accountFormRef = ref<FormInstance>()

const accountForm = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaKey: '',
})

const accountRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const hasShops = computed(() => userStore.shopTree.length > 0)
const shopOptions = computed(() => {
  const result: Array<{ id: number; label: string }> = []
  const walk = (nodes: Array<{ id: number; shopName: string; children?: any[] }>, prefix = '') => {
    for (const node of nodes || []) {
      const label = prefix ? `${prefix} / ${node.shopName}` : node.shopName
      result.push({ id: node.id, label })
      if (node.children?.length) walk(node.children, label)
    }
  }
  walk(userStore.shopTree as Array<{ id: number; shopName: string; children?: any[] }>)
  return result
})

function normalizeCaptchaImage(img: string) {
  if (!img) return ''
  const value = img.trim()
  return value.startsWith('data:image') ? value : `data:image/png;base64,${value}`
}

async function loadCaptcha(forceRefresh = false) {
  if (step.value !== 1 && !forceRefresh) return
  captchaLoading.value = true
  try {
    const data = await authApi.getCaptchaImage()
    if (!data?.uuid || !data?.img) {
      captchaVisible.value = false
      captchaImage.value = ''
      accountForm.captcha = ''
      accountForm.captchaKey = ''
      return
    }
    captchaVisible.value = true
    captchaImage.value = normalizeCaptchaImage(data.img)
    accountForm.captchaKey = data.uuid
    if (forceRefresh) accountForm.captcha = ''
  } catch {
    captchaVisible.value = false
    captchaImage.value = ''
    accountForm.captcha = ''
    accountForm.captchaKey = ''
  } finally {
    captchaLoading.value = false
  }
}

async function handleNextStep() {
  if (!accountFormRef.value) return
  try {
    await accountFormRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.loginAccount({
      username: accountForm.username.trim(),
      password: accountForm.password,
      captcha: accountForm.captcha || undefined,
      captchaKey: accountForm.captchaKey || undefined,
    })
    await userStore.fetchAuthorizedShops()
    selectedShopId.value = undefined
    step.value = 2
  } catch (error: any) {
    accountForm.captcha = ''
    if (captchaVisible.value) await loadCaptcha(true)
    ElMessage.error(error?.message || '账号验证失败')
  } finally {
    loading.value = false
  }
}

async function handleEnterSystem() {
  if (!selectedShopId.value) {
    ElMessage.warning('请选择店铺')
    return
  }
  loading.value = true
  try {
    await userStore.enterSystem(selectedShopId.value)
  } catch (error: any) {
    ElMessage.error(error?.message || '进入系统失败')
  } finally {
    loading.value = false
  }
}

async function handleBack() {
  step.value = 1
  selectedShopId.value = undefined
  await loadCaptcha(true)
}

onMounted(() => {
  loadCaptcha()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 32px 20px;
  background:
    radial-gradient(circle at top left, rgba(79, 70, 229, 0.1), transparent 32%),
    radial-gradient(circle at bottom right, rgba(148, 163, 184, 0.12), transparent 28%),
    linear-gradient(180deg, var(--slate-50) 0%, #ffffff 100%);
}

.login-shell {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: min(100%, 460px);
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid var(--border-color-soft);
  border-radius: 28px;
  padding: 32px;
  box-shadow: var(--shadow-lg);
  backdrop-filter: blur(18px);
}

.login-header {
  margin-bottom: 24px;
}

.login-kicker {
  display: inline-flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
}

.login-header h1 {
  margin: 0 0 10px;
  font-size: clamp(28px, 5vw, 34px);
  font-weight: 800;
  letter-spacing: -0.04em;
  color: var(--text-primary);
}

.login-header p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-muted);
}

.login-stepbar {
  display: flex;
  align-items: stretch;
  gap: 12px;
  margin-bottom: 28px;
}

.step-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  background: var(--slate-50);
  border: 1px solid transparent;
  transition: all var(--transition-normal);
}

.step-item strong,
.step-item small {
  display: block;
}

.step-item strong {
  margin-bottom: 4px;
  font-size: 14px;
  color: var(--text-secondary);
}

.step-item small {
  font-size: 12px;
  color: var(--text-muted);
}

.step-item.active {
  background: linear-gradient(135deg, var(--color-primary-soft) 0%, #ffffff 100%);
  border-color: rgba(79, 70, 229, 0.12);
}

.step-item.done .step-index,
.step-item.active .step-index {
  background: var(--color-primary);
  color: #fff;
}

.step-item.active strong {
  color: var(--text-primary);
}

.step-index {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
  border: 1px solid var(--border-color-soft);
}

.step-divider {
  width: 1px;
  background: var(--border-color-soft);
  border-radius: 999px;
}

.login-form,
.step2-container {
  display: flex;
  flex-direction: column;
}

.login-form :deep(.el-form-item),
.step2-container {
  gap: 0;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
  min-height: 48px;
  border-radius: 14px;
  box-shadow: none !important;
  border: 1px solid var(--border-color-soft);
  background: rgba(248, 250, 252, 0.72);
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px rgba(79, 70, 229, 0.18) !important;
  border-color: rgba(79, 70, 229, 0.28);
}

.captcha-form-item {
  margin-bottom: 8px;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 124px;
  gap: 12px;
}

.captcha-image-button {
  height: 48px;
  padding: 0;
  border: 1px solid var(--border-color-soft);
  border-radius: 14px;
  background: linear-gradient(135deg, #ffffff 0%, var(--slate-50) 100%);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.captcha-image-button:hover:not(:disabled) {
  border-color: rgba(79, 70, 229, 0.24);
}

.captcha-image-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.captcha-image,
.captcha-image-placeholder {
  width: 100%;
  height: 100%;
}

.captcha-image {
  display: block;
  object-fit: cover;
}

.captcha-image-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-muted);
}

.captcha-refresh {
  margin-top: 10px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  align-self: flex-start;
  cursor: pointer;
}

.captcha-refresh:disabled {
  cursor: not-allowed;
  color: var(--text-muted);
}

.shop-panel {
  margin-bottom: 16px;
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.72) 0%, rgba(248, 250, 252, 0.9) 100%);
  border: 1px solid var(--border-color-soft);
}

.shop-tip {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 14px;
}

.shop-tip strong {
  font-size: 14px;
  color: var(--text-primary);
}

.shop-tip span,
.shop-empty {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
}

.nexus-tree-select {
  width: 100%;
}

.shop-empty {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px dashed var(--border-color);
}

.submit-btn,
.back-btn {
  width: 100%;
  height: 46px;
  border-radius: 14px;
}

.submit-btn {
  margin-top: 8px;
}

.back-btn {
  margin-top: 12px;
  border-color: var(--border-color-soft);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 640px) {
  .login-page {
    padding: 20px 14px;
  }

  .login-shell {
    min-height: calc(100vh - 40px);
  }

  .login-card {
    padding: 24px 20px;
    border-radius: 24px;
  }

  .login-stepbar {
    flex-direction: column;
  }

  .step-divider {
    display: none;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }
}
</style>
