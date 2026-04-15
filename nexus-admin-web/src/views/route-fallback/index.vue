<template>
  <div class="route-fallback">
    <el-result icon="warning" title="页面加载失败" sub-title="菜单对应页面不存在或暂不可用">
      <template #extra>
        <div class="actions">
          <el-button type="primary" @click="goDashboard">返回工作台</el-button>
          <el-button :disabled="!expectedViewKey" @click="copyExpectedViewKey">
            复制 expectedViewKey
          </el-button>
        </div>
      </template>
    </el-result>

    <el-card class="diag" shadow="never">
      <template #header>
        <div class="diag-header">
          <span>诊断信息</span>
          <el-tag v-if="expectedViewKey" type="warning" effect="light">缺失视图</el-tag>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="菜单名 (menuName)">
          <span class="mono">{{ menuName || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="访问路径 (fullPath)">
          <span class="mono">{{ fullPath || route.fullPath || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="后端 component">
          <span class="mono">{{ backendComponent || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="expectedViewKey">
          <div class="expected">
            <span class="mono">{{ expectedViewKey || '-' }}</span>
            <el-button
              size="small"
              text
              :disabled="!expectedViewKey"
              @click="copyExpectedViewKey"
            >
              复制
            </el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

type RouteFallbackDiag = {
  menuName?: string
  fullPath?: string
  component?: string
  expectedViewKey?: string
}

const route = useRoute()
const router = useRouter()

const diag = computed<RouteFallbackDiag>(() => {
  const metaDiag = (route.meta as any)?.__fallback as RouteFallbackDiag | undefined
  const q = route.query as Record<string, any>
  const queryDiag: RouteFallbackDiag = {
    menuName: typeof q.menuName === 'string' ? q.menuName : undefined,
    fullPath: typeof q.fullPath === 'string' ? q.fullPath : undefined,
    component: typeof q.component === 'string' ? q.component : undefined,
    expectedViewKey: typeof q.expectedViewKey === 'string' ? q.expectedViewKey : undefined,
  }
  return { ...(queryDiag || {}), ...(metaDiag || {}) }
})

const menuName = computed(() => diag.value.menuName)
const fullPath = computed(() => diag.value.fullPath)
const backendComponent = computed(() => diag.value.component)
const expectedViewKey = computed(() => diag.value.expectedViewKey)

function goDashboard() {
  router.push('/dashboard')
}

async function copyExpectedViewKey() {
  const text = expectedViewKey.value
  if (!text) return

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      ElMessage.success('已复制 expectedViewKey')
      return
    }
  } catch {
    // fallthrough
  }

  try {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.setAttribute('readonly', 'true')
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.top = '0'
    document.body.appendChild(textarea)
    textarea.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(textarea)
    if (ok) ElMessage.success('已复制 expectedViewKey')
    else ElMessage.error('复制失败，请手动复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.route-fallback {
  padding: 28px;
  max-width: 920px;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.diag {
  margin-top: 18px;
}

.diag-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  word-break: break-all;
}

.expected {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>

