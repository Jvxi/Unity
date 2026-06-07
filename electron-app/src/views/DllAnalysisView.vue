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
import { ref, nextTick } from "vue";
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
  const cards = document.querySelectorAll('.analysis .el-card');
  if (cards.length > 0) {
    animate(cards, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 24 }, { to: 0 }],
      scale: [{ from: 0.97 }, { to: 1 }],
      duration: 450,
      delay: stagger(100, { start: 100 }),
      ease: 'outElastic(1, .8)',
    });
  }
}
</script>

<style scoped>
.analysis { display: flex; flex-direction: column; gap: 20px; }
</style>