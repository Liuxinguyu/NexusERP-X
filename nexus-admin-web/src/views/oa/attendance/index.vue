<template>
  <div class="attendance-container flex justify-center items-center h-full bg-slate-50 py-8">
    <div class="attendance-card w-full max-w-md bg-white rounded-3xl shadow-sm border border-slate-100 p-8 flex flex-col items-center relative overflow-hidden">

      <div class="header text-center mb-8 w-full z-10">
        <h2 class="text-xl font-bold text-slate-800">考勤打卡</h2>
        <p class="text-sm text-slate-500 mt-1">今日已工作: <span class="font-semibold text-indigo-600">6.5</span> 小时</p>
      </div>

      <div class="radar-zone relative w-64 h-64 flex justify-center items-center mb-10 z-10">
        <div class="fence absolute inset-0 rounded-full border-2 border-dashed border-indigo-200"></div>
        <button
          class="clock-btn relative w-44 h-44 rounded-full bg-indigo-600 text-white flex flex-col justify-center items-center shadow-lg transition-all active:scale-95 disabled:bg-slate-300 disabled:cursor-not-allowed"
          :class="{ 'is-pulsing': canClockIn }"
          :disabled="!canClockIn"
          @click="handleClockIn"
        >
          <span class="time text-4xl font-black tracking-tight mb-1">{{ currentTime }}</span>
          <span class="action font-medium">{{ clockInText }}</span>
          <div class="pulse-ring absolute inset-0 rounded-full border-4 border-indigo-400 opacity-0 pointer-events-none"></div>
        </button>
      </div>

      <div class="status-indicator text-center h-12 flex flex-col justify-center z-10">
        <div v-if="locating" class="text-slate-500 flex items-center justify-center gap-2">
          正在获取高精度位置...
        </div>
        <div v-else-if="!canClockIn" class="text-rose-500 font-medium bg-rose-50 px-4 py-1.5 rounded-full inline-block">
          ⚠️ 不在打卡范围内或网络异常
        </div>
        <div v-else class="text-emerald-500 font-medium bg-emerald-50 px-4 py-1.5 rounded-full inline-block">
          ✅ 已进入考勤范围 (内网IP)
        </div>
      </div>

      <div class="timeline mt-8 w-full border-t border-slate-100 pt-6 z-10">
        <h3 class="text-sm font-semibold text-slate-400 mb-4 px-2 uppercase tracking-wider">今日记录</h3>
        <div class="space-y-3 px-2">
          <div v-for="record in records" :key="record.id" class="flex items-center text-sm">
            <div class="w-2 h-2 rounded-full bg-slate-300 mr-3"></div>
            <span class="w-12 font-medium text-slate-700">{{ record.time }}</span>
            <span class="text-slate-500">{{ record.type }}</span>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { ElNotification } from 'element-plus';
import { oaApi } from '@/api/oa';

const currentTime = ref('00:00:00');
const clockInText = ref('上班打卡');
const locating = ref(true);
const canClockIn = ref(false);
const records = ref([{ id: 1, time: '09:00', type: '上班打卡 (正常)' }]);
let timer: any;

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false });
  }, 1000);

  // 模拟获取地理位置 (实际需调用 HTML5 navigator.geolocation)
  setTimeout(() => {
    locating.value = false;
    canClockIn.value = true; // 模拟进入打卡范围
  }, 1500);
});

onUnmounted(() => clearInterval(timer));

const handleClockIn = () => {
  ElNotification({
    title: '打卡成功',
    message: `打卡时间：${currentTime.value}`,
    type: 'success',
  });
  clockInText.value = '下班打卡';
  records.value.unshift({ id: Date.now(), time: currentTime.value.substring(0, 5), type: '下班打卡 (正常)' });
};
</script>

<style scoped>
.attendance-container { height: 100%; min-height: 600px; }
.is-pulsing .pulse-ring { animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite; }
@keyframes pulse {
  0% { transform: scale(1); opacity: 0.8; border-width: 4px; }
  100% { transform: scale(1.5); opacity: 0; border-width: 0px; }
}
</style>
