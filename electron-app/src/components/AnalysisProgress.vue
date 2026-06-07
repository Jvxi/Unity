<template>
  <el-card>
    <div class="progress">
      <el-icon class="spin" :size="20"><Loading /></el-icon>
      <span>正在分析中... {{ percent > 0 ? percent + '%' : '' }}</span>
    </div>
    <el-progress v-if="percent > 0" :percentage="percent" :stroke-width="8" style="margin-top:8px" />
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ percent: number }>();
</script>

<style scoped>
.progress { display: flex; align-items: center; gap: 8px; color: #409eff; font-size: 14px; }
.spin { animation: rotate 1s linear infinite; }
@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>
