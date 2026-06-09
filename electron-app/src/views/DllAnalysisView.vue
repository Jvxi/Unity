<template>
  <div class="analysis">
    <FileUploader :loading="loading" @upload="handleUpload" />
    <AnalysisProgress v-if="loading" :percent="uploadPercent" />
    <template v-if="result">
      <!-- 导出按钮栏 -->
      <el-card class="export-bar" shadow="never">
        <div class="export-row">
          <span class="export-label">导出报告</span>
          <el-button size="small" @click="doExport('json')">
            <el-icon><Download /></el-icon> JSON
          </el-button>
          <el-button size="small" @click="doExport('html')">
            <el-icon><Download /></el-icon> HTML
          </el-button>
        </div>
      </el-card>
      <PeInfoPanel :pe-info="result.peInfo" />
      <VtableResult :vtables="result.vtables" :ai-summary="result.aiSummary" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from "vue";
import { ElMessage } from "element-plus";
import { animate, stagger } from "animejs";
import { analyzeDll, getExportUrl } from "@/api/client";
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
const uploadedFile = ref<File | null>(null);

async function handleUpload(file: File) {
  loading.value = true;
  uploadPercent.value = 0;
  result.value = null;
  uploadedFile.value = file;
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

function doExport(format: "json" | "html") {
  if (!uploadedFile.value) return;
  const url = getExportUrl(format);
  const fd = new FormData();
  fd.append("file", uploadedFile.value);
  if (store.apiKey) {
    fd.append("apiKey", store.apiKey);
    fd.append("provider", store.selectedProvider);
    fd.append("model", store.selectedModel);
  }

  const token = localStorage.getItem("token");
  fetch(url, {
    method: "POST",
    body: fd,
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })
    .then((res) => {
      if (!res.ok) throw new Error("导出失败");
      return res.blob();
    })
    .then((blob) => {
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = format === "json" ? "report.json" : "report.html";
      a.click();
      URL.revokeObjectURL(a.href);
      ElMessage.success("导出成功");
    })
    .catch((e) => ElMessage.error(e.message || "导出失败"));
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
.export-bar { border-radius: var(--radius-lg); }
.export-row { display: flex; align-items: center; gap: 12px; }
.export-label { font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }
</style>
