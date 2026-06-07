<template>
  <div class="analysis">
    <FileUploader :loading="loading" @upload="handleUpload" />

    <AnalysisProgress v-if="loading" :percent="uploadPercent" />

    <template v-if="result">
      <PeInfoPanel :pe-info="result.peInfo" />
      <VtableResult :vtables="result.vtables" :ai-summary="result.aiSummary" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from "vue";
import { ElMessage } from "element-plus";
import { animate, stagger } from "animejs";
import { analyzeDll } from "@/api/client";
import { useSettingsStore } from "@/stores/settings";
import type { AnalysisResult } from "@/types";
import FileUploader from "@/components/FileUploader.vue";
import PeInfoPanel from "@/components/PeInfoPanel.vue";
import VtableResult from "@/components/VtableResult.vue";
import AnalysisProgress from "@/components/AnalysisProgress.vue";

const store = useSettingsStore();
const loading = ref(false);
const uploadPercent = ref(0);
const result = ref<AnalysisResult | null>(null);

async function handleUpload(file: File) {
  loading.value = true;
  uploadPercent.value = 0;
  result.value = null;
  try {
    result.value = await analyzeDll(file, store.apiKey, store.selectedProvider, store.selectedModel, (p) => {
      uploadPercent.value = p;
    });
    ElMessage.success("分析完成");
    nextTick(() => animateResult());
  } catch (e: any) {
    ElMessage.error(e.message || "分析失败");
  } finally {
    loading.value = false;
  }
}

function animateResult() {
  // PE 信息面板入场
  const pePanel = document.querySelector('.analysis .el-card:first-of-type');
  if (pePanel) {
    animate(pePanel, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 30 }, { to: 0 }],
      scale: [{ from: 0.95 }, { to: 1 }],
      duration: 500,
      ease: 'outElastic(1, .7)',
    });
  }

  // 虚表结果表格入场
  const vtableCards = document.querySelectorAll('.analysis .el-card');
  if (vtableCards.length > 1) {
    animate(Array.from(vtableCards).slice(1), {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 40 }, { to: 0 }],
      duration: 600,
      delay: stagger(200, { start: 300 }),
      ease: 'outElastic(1, .7)',
    });
  }
}
</script>

<style scoped>
.analysis { display: flex; flex-direction: column; gap: 20px; }
</style>