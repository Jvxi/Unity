<template>
  <el-card class="progress-card">
    <div class="progress-inner">
      <div class="spinner"></div>
      <span class="progress-text">正在分析中{{ percent > 0 ? '...' : '' }}</span>
      <span v-if="percent > 0" class="progress-pct">{{ percent }}%</span>
    </div>
    <el-progress v-if="percent > 0" :percentage="percent" :stroke-width="6" :show-text="false"
      color="var(--color-primary)" style="margin-top:12px" />
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ percent: number }>();
</script>

<style scoped>
.progress-card { background: var(--color-primary-bg); }
.progress-inner {
  display: flex;
  align-items: center;
  gap: 10px;
}
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(74,222,128,0.2);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.progress-text { color: var(--color-primary-dark); font-size: 14px; font-weight: 500; }
.progress-pct { color: var(--color-text-secondary); font-size: 13px; margin-left: auto; }
</style>