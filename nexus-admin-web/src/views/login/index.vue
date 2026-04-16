<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="login-header">
        <h2>NexusERP-X</h2>
        <p v-if="step === 1">Welcome back. Please enter your details.</p>
        <p v-else>Select your workspace to continue.</p>
      </div>

      <transition name="fade-slide" mode="out-in">
        <el-form
          v-if="step === 1"
          key="step1"
          ref="accountFormRef"
          :model="accountForm"
          :rules="accountRules"
          label-position="top"
          @submit.prevent="handleNextStep"
        >
          <el-form-item label="Username" prop="username">
            <el-input v-model="accountForm.username" placeholder="Enter username" @keyup.enter="handleNextStep" />
          </el-form-item>
          <el-form-item label="Password" prop="password">
            <el-input
              v-model="accountForm.password"
              type="password"
              show-password
              placeholder="Enter password"
              @keyup.enter="handleNextStep"
            />
          </el-form-item>
          <el-form-item label="Captcha" prop="captcha">
            <el-input v-model="accountForm.captcha" placeholder="Enter captcha (optional)" @keyup.enter="handleNextStep" />
          </el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleNextStep">下一步</el-button>
        </el-form>

        <div v-else key="step2" class="step2-container">
          <el-tree-select
            v-model="selectedShopId"
            :data="userStore.shopTree"
            :props="{ label: 'shopName', children: 'children' }"
            node-key="id"
            check-strictly
            placeholder="Search workspace..."
            class="nexus-tree-select"
          />
          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleEnterSystem">进入系统</el-button>
          <el-button class="back-btn" @click="handleBack">返回重新输入账号</el-button>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const step = ref<1 | 2>(1)
const loading = ref(false)
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
    step.value = 2
  } catch (error: any) {
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

function handleBack() {
  step.value = 1
  selectedShopId.value = undefined
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8fafc;
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 24px;
  padding: 48px 40px;
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.login-header h2 {
  font-weight: 800;
  font-size: 28px;
  letter-spacing: -0.02em;
  margin-bottom: 8px;
  color: #0f172a;
}

.login-header p {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 32px;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  height: 44px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: none !important;
}

.nexus-tree-select {
  width: 100%;
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  margin-top: 8px;
}

.back-btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  margin-top: 12px;
}

.step2-container {
  display: flex;
  flex-direction: column;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
