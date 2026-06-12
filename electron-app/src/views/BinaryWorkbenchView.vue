<template>
  <div class="binary-workbench">
    <section class="workspace-head">
      <div class="title-block">
        <div class="title-icon">
          <el-icon :size="28"><Cpu /></el-icon>
        </div>
        <div>
          <h1>二进制分析工作台</h1>
          <p>统一完成 PE / 虚表分析、字符串提取和十六进制查看。</p>
        </div>
      </div>
      <div class="head-actions">
        <el-tag v-if="selectedFile" type="success" round>{{ fileExtension }}</el-tag>
        <el-tag v-else type="info" round>等待文件</el-tag>
      </div>
    </section>

    <el-card class="file-panel" shadow="never">
      <div
        class="drop-zone"
        :class="{ 'is-dragover': isDragover }"
        @click="openFilePicker"
        @dragover.prevent="isDragover = true"
        @dragleave="isDragover = false"
        @drop.prevent="onDrop"
      >
        <div class="upload-icon">
          <el-icon :size="34"><UploadFilled /></el-icon>
        </div>
        <div class="upload-copy">
          <strong>拖拽二进制文件到此处，或点击选择</strong>
          <span>支持 .dll、.exe、.sys、.ocx、.bin、.dat 等文件</span>
        </div>
        <input ref="fileInputRef" type="file" class="file-input" accept=".dll,.exe,.sys,.ocx,.bin,.dat" @change="onFileChange" />
      </div>

      <div v-if="selectedFile" class="file-strip">
        <div class="file-main">
          <el-icon><Document /></el-icon>
          <div>
            <div class="file-name">{{ selectedFile.name }}</div>
            <div class="file-meta">
              {{ formatSize(selectedFile.size) }}
              <span v-if="selectedFile.lastModified"> · {{ formatDate(selectedFile.lastModified) }}</span>
            </div>
          </div>
        </div>
        <div class="file-actions">
          <el-button size="small" @click="clearFile">
            <el-icon><Close /></el-icon>
            移除
          </el-button>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" class="tool-tabs">
      <el-tab-pane name="pe">
        <template #label>
          <span class="tab-label"><el-icon><Search /></el-icon> PE / 虚表</span>
        </template>

        <div class="tool-action-bar">
          <div>
            <h2>PE 结构与虚表检测</h2>
            <p>解析 PE 基础信息、段表、导入导出表，并检测潜在虚表地址。</p>
          </div>
          <div class="action-buttons">
            <el-button type="primary" :disabled="!selectedFile" :loading="analysisLoading" @click="runAnalysis">
              <el-icon><VideoPlay /></el-icon>
              开始分析
            </el-button>
            <el-button :disabled="!analysisResult || !selectedFile" @click="downloadReport('json')">
              <el-icon><Download /></el-icon>
              JSON
            </el-button>
            <el-button :disabled="!analysisResult || !selectedFile" @click="downloadReport('html')">
              <el-icon><Download /></el-icon>
              HTML
            </el-button>
          </div>
        </div>

        <AnalysisProgress v-if="analysisLoading" :percent="analysisPercent" />
        <el-empty v-else-if="!analysisResult" class="empty-state" description="选择文件后开始 PE / 虚表分析" />
        <template v-else>
          <div class="summary-grid">
            <div class="metric">
              <span class="metric-value">{{ analysisResult.peInfo.numberOfSections }}</span>
              <span class="metric-label">段数量</span>
            </div>
            <div class="metric">
              <span class="metric-value">{{ analysisResult.peInfo.importCount }}</span>
              <span class="metric-label">导入 DLL</span>
            </div>
            <div class="metric">
              <span class="metric-value">{{ analysisResult.peInfo.exportCount }}</span>
              <span class="metric-label">导出项</span>
            </div>
            <div class="metric">
              <span class="metric-value">{{ analysisResult.vtables.length }}</span>
              <span class="metric-label">虚表候选</span>
            </div>
            <div class="metric purple">
              <span class="metric-value">{{ worldCandidateCount }}</span>
              <span class="metric-label">世界数据候选</span>
            </div>
          </div>
          <el-card v-if="analysisResult.worldAnalysis" class="world-card" shadow="never">
            <template #header>
              <div class="card-title">
                <el-icon><Aim /></el-icon>
                <span>世界数组优先分析</span>
                <el-tag v-if="worldCandidateCount" type="success" size="small" round>{{ worldCandidateCount }} 个候选</el-tag>
              </div>
            </template>

            <p class="world-summary">{{ analysisResult.worldAnalysis.summary }}</p>
            <div v-if="analysisResult.worldAnalysis.priorityHints?.length" class="hint-list">
              <div v-for="hint in analysisResult.worldAnalysis.priorityHints" :key="hint" class="hint-item">
                <el-icon><Flag /></el-icon>
                <span>{{ hint }}</span>
              </div>
            </div>

            <el-table
              v-if="analysisResult.worldAnalysis.worldArrayCandidates?.length"
              :data="analysisResult.worldAnalysis.worldArrayCandidates"
              stripe
              size="small"
              class="world-table"
            >
              <el-table-column label="候选" min-width="170">
                <template #default="{ row }">
                  <div class="candidate-name">{{ row.name || row.kind || '-' }}</div>
                  <div class="candidate-sub">{{ row.kind || '-' }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="rva" label="RVA" width="120" />
              <el-table-column prop="va" label="VA" width="150" />
              <el-table-column prop="sectionName" label="段" width="90" />
              <el-table-column label="置信度" width="110">
                <template #default="{ row }">
                  <el-progress :percentage="confidencePercent(row.confidence)" :stroke-width="6" :show-text="false" />
                  <span class="confidence-text">{{ confidencePercent(row.confidence) }}%</span>
                </template>
              </el-table-column>
              <el-table-column prop="pointerCount" label="指针数" width="85" />
              <el-table-column label="证据" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ compactEvidence(row.evidence, row.relatedStrings) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="未定位到稳定世界数组候选" />

            <el-collapse v-if="analysisResult.worldAnalysis.relatedData?.length" class="related-collapse">
              <el-collapse-item :title="`相关证据 (${analysisResult.worldAnalysis.relatedData.length})`" name="related">
                <el-table :data="analysisResult.worldAnalysis.relatedData.slice(0, 40)" stripe size="small" max-height="280">
                  <el-table-column prop="kind" label="类型" width="120" />
                  <el-table-column prop="name" label="名称" width="150" />
                  <el-table-column prop="rva" label="RVA" width="120" />
                  <el-table-column prop="sectionName" label="段" width="90" />
                  <el-table-column prop="value" label="值" show-overflow-tooltip />
                  <el-table-column prop="note" label="说明" show-overflow-tooltip />
                </el-table>
              </el-collapse-item>
            </el-collapse>
          </el-card>
          <PeInfoPanel :pe-info="analysisResult.peInfo" />
          <VtableResult :vtables="analysisResult.vtables" :ai-summary="analysisResult.aiSummary || ''" />
        </template>
      </el-tab-pane>

      <el-tab-pane name="strings">
        <template #label>
          <span class="tab-label"><el-icon><Tickets /></el-icon> 字符串</span>
        </template>

        <div class="tool-action-bar">
          <div>
            <h2>字符串提取</h2>
            <p>提取 ASCII / UTF-16LE 字符串，支持长度、编码和关键词过滤。</p>
          </div>
          <el-button type="primary" :disabled="!selectedFile" :loading="stringsLoading" @click="runStringExtract">
            <el-icon><Search /></el-icon>
            提取字符串
          </el-button>
        </div>

        <div class="option-grid">
          <label class="option-field">
            <span>最小长度</span>
            <el-input-number v-model="stringMinLength" :min="2" :max="256" controls-position="right" />
          </label>
          <label class="option-field">
            <span>编码</span>
            <el-select v-model="stringEncoding">
              <el-option label="全部" value="all" />
              <el-option label="ASCII" value="ascii" />
              <el-option label="UTF-16LE" value="unicode" />
            </el-select>
          </label>
          <label class="option-field wide">
            <span>关键词</span>
            <el-input v-model="stringKeyword" placeholder="可选，按内容过滤" clearable />
          </label>
        </div>

        <el-empty v-if="!stringsResult" class="empty-state" description="选择文件后提取字符串" />
        <template v-else>
          <div class="summary-grid">
            <div class="metric">
              <span class="metric-value">{{ stringsResult.totalCount }}</span>
              <span class="metric-label">总计</span>
            </div>
            <div class="metric blue">
              <span class="metric-value">{{ stringsResult.asciiCount }}</span>
              <span class="metric-label">ASCII</span>
            </div>
            <div class="metric amber">
              <span class="metric-value">{{ stringsResult.unicodeCount }}</span>
              <span class="metric-label">UTF-16LE</span>
            </div>
          </div>

          <el-card class="result-card" shadow="never">
            <el-table :data="pagedStrings" stripe size="small" max-height="520">
              <el-table-column label="偏移" width="130">
                <template #default="{ row }">0x{{ formatOffset(row.offset) }}</template>
              </el-table-column>
              <el-table-column prop="encoding" label="编码" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.encoding === 'ascii' ? '' : 'warning'" size="small">{{ row.encoding }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="length" label="长度" width="80" />
              <el-table-column prop="value" label="内容" show-overflow-tooltip />
            </el-table>
            <el-pagination
              v-if="stringsResult.totalCount > stringPageSize"
              v-model:current-page="stringCurrentPage"
              :page-size="stringPageSize"
              :total="stringsResult.totalCount"
              layout="prev, pager, next, total"
              class="pager"
            />
          </el-card>
        </template>
      </el-tab-pane>

      <el-tab-pane name="hex">
        <template #label>
          <span class="tab-label"><el-icon><Grid /></el-icon> Hex</span>
        </template>

        <div class="tool-action-bar">
          <div>
            <h2>十六进制查看</h2>
            <p>按偏移和长度查看 Hex Dump，也可以定位十六进制字节序列。</p>
          </div>
          <el-button type="primary" :disabled="!selectedFile" :loading="hexLoading" @click="runHexDump">
            <el-icon><View /></el-icon>
            查看 Hex
          </el-button>
        </div>

        <div class="option-grid">
          <label class="option-field">
            <span>起始偏移</span>
            <el-input v-model="hexOffset" placeholder="0 或 0x1000" />
          </label>
          <label class="option-field">
            <span>读取长度</span>
            <el-input v-model="hexLength" placeholder="0 表示自动" />
          </label>
          <label class="option-field wide">
            <span>搜索 Hex</span>
            <el-input v-model="hexSearch" placeholder="例如 4D 5A" clearable />
          </label>
        </div>

        <el-empty v-if="!hexResult" class="empty-state" description="选择文件后查看十六进制内容" />
        <template v-else>
          <div class="hex-info">
            <span>总大小：<b>{{ formatSize(hexResult.totalBytes) }}</b></span>
            <span>显示范围：<b>0x{{ formatOffset(hexResult.startOffset) }}</b> - <b>0x{{ formatOffset(hexResult.endOffset) }}</b></span>
            <span>行数：<b>{{ hexResult.totalLines }}</b></span>
          </div>

          <el-card v-if="hexResult.lines.length" class="hex-card" shadow="never">
            <div class="hex-scroll">
              <div class="hex-header">
                <span class="col-offset">Offset</span>
                <span class="col-hex">00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F</span>
                <span class="col-ascii">ASCII</span>
              </div>
              <div v-for="line in hexResult.lines" :key="line.offset" class="hex-line">
                <span class="col-offset">{{ formatOffset(line.offset) }}</span>
                <span class="col-hex" v-html="highlightHex(line.hex)" />
                <span class="col-ascii">{{ line.ascii }}</span>
              </div>
            </div>
          </el-card>
          <el-card v-else class="result-card" shadow="never">
            <el-empty description="未找到匹配数据" />
          </el-card>
        </template>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { animate, stagger } from "animejs";
import {
  analyzeDll,
  extractStrings,
  getExportUrl,
  hexDump,
  readApiErrorMessage,
} from "@/api/client";
import { useSettingsStore } from "@/stores/settings";
import type {
  AnalysisResult,
  BinaryStringExtractResult,
  BinaryStringEntry,
  HexDumpResult,
} from "@/types";
import AnalysisProgress from "@/components/AnalysisProgress.vue";
import PeInfoPanel from "@/components/PeInfoPanel.vue";
import VtableResult from "@/components/VtableResult.vue";

type ToolTab = "pe" | "strings" | "hex";
type StringEncoding = "all" | "ascii" | "unicode";

const tabs: ToolTab[] = ["pe", "strings", "hex"];
const route = useRoute();
const router = useRouter();
const settingsStore = useSettingsStore();

const fileInputRef = ref<HTMLInputElement | null>(null);
const selectedFile = ref<File | null>(null);
const isDragover = ref(false);
const activeTab = ref<ToolTab>(tabFromQuery() || "pe");

const analysisLoading = ref(false);
const analysisPercent = ref(0);
const analysisResult = ref<AnalysisResult | null>(null);

const stringsLoading = ref(false);
const stringMinLength = ref(4);
const stringEncoding = ref<StringEncoding>("all");
const stringKeyword = ref("");
const stringsResult = ref<BinaryStringExtractResult | null>(null);
const stringCurrentPage = ref(1);
const stringPageSize = 100;

const hexLoading = ref(false);
const hexOffset = ref("0");
const hexLength = ref("0");
const hexSearch = ref("");
const hexResult = ref<HexDumpResult | null>(null);

const worldCandidateCount = computed(() => analysisResult.value?.worldAnalysis?.worldArrayCandidates?.length || 0);

const fileExtension = computed(() => {
  if (!selectedFile.value?.name.includes(".")) return "二进制文件";
  return selectedFile.value.name.split(".").pop()?.toUpperCase() || "二进制文件";
});

const pagedStrings = computed<BinaryStringEntry[]>(() => {
  if (!stringsResult.value) return [];
  const start = (stringCurrentPage.value - 1) * stringPageSize;
  return stringsResult.value.strings.slice(start, start + stringPageSize);
});

watch(
  () => [route.query.tool, route.query.tab],
  () => {
    const nextTab = tabFromQuery();
    if (nextTab && nextTab !== activeTab.value) {
      activeTab.value = nextTab;
    }
  }
);

watch(activeTab, (tab) => {
  const current = tabFromQuery();
  if (current === tab) return;
  const query: Record<string, string> = {};
  Object.entries(route.query).forEach(([key, value]) => {
    if (key !== "tool" && key !== "tab" && typeof value === "string") {
      query[key] = value;
    }
  });
  query.tool = tab;
  router.replace({ path: "/analysis", query });
});

onMounted(() => {
  animateEntry();
});

function tabFromQuery(): ToolTab | null {
  const raw = route.query.tool ?? route.query.tab;
  const value = Array.isArray(raw) ? raw[0] : raw;
  return tabs.includes(value as ToolTab) ? (value as ToolTab) : null;
}

function openFilePicker() {
  fileInputRef.value?.click();
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file) setSelectedFile(file);
  input.value = "";
}

function onDrop(event: DragEvent) {
  isDragover.value = false;
  const file = event.dataTransfer?.files?.[0];
  if (file) setSelectedFile(file);
}

function setSelectedFile(file: File) {
  selectedFile.value = file;
  analysisResult.value = null;
  stringsResult.value = null;
  hexResult.value = null;
  stringCurrentPage.value = 1;
  ElMessage.success(`已选择：${file.name}`);
}

function clearFile() {
  selectedFile.value = null;
  analysisResult.value = null;
  stringsResult.value = null;
  hexResult.value = null;
}

async function runAnalysis() {
  if (!selectedFile.value) return warnSelectFile();
  analysisLoading.value = true;
  analysisPercent.value = 0;
  analysisResult.value = null;
  try {
    analysisResult.value = await analyzeDll(
      selectedFile.value,
      settingsStore.apiKey,
      settingsStore.selectedProvider,
      settingsStore.selectedModel,
      settingsStore.aiApiUrl,
      (percent) => {
        analysisPercent.value = percent;
      }
    );
    ElMessage.success("PE / 虚表分析完成");
    animateResults();
  } catch (error) {
    ElMessage.error(readApiErrorMessage(error, "分析失败"));
  } finally {
    analysisLoading.value = false;
  }
}

async function runStringExtract() {
  if (!selectedFile.value) return warnSelectFile();
  stringsLoading.value = true;
  try {
    stringsResult.value = await extractStrings(
      selectedFile.value,
      stringMinLength.value,
      stringEncoding.value,
      stringKeyword.value.trim() || undefined
    );
    stringCurrentPage.value = 1;
    ElMessage.success(`字符串提取完成，共 ${stringsResult.value.totalCount} 条`);
    animateResults();
  } catch (error) {
    ElMessage.error(readApiErrorMessage(error, "提取失败"));
  } finally {
    stringsLoading.value = false;
  }
}

async function runHexDump() {
  if (!selectedFile.value) return warnSelectFile();
  const offset = parsePositiveNumber(hexOffset.value, "起始偏移");
  const length = parsePositiveNumber(hexLength.value, "读取长度");
  if (offset === null || length === null) return;

  const search = normalizeHexSearch(hexSearch.value);
  if (search === null) return;

  hexLoading.value = true;
  try {
    hexResult.value = await hexDump(selectedFile.value, offset, length, search || undefined);
    ElMessage.success(`Hex 加载完成，${hexResult.value.totalLines} 行`);
    animateResults();
  } catch (error) {
    ElMessage.error(readApiErrorMessage(error, "加载失败"));
  } finally {
    hexLoading.value = false;
  }
}

function warnSelectFile() {
  ElMessage.warning("请先选择文件");
}

function parsePositiveNumber(value: string, label: string): number | null {
  const text = value.trim();
  if (!text) return 0;
  const parsed = /^0x/i.test(text) ? Number.parseInt(text.slice(2), 16) : Number.parseInt(text, 10);
  if (!Number.isFinite(parsed) || parsed < 0) {
    ElMessage.warning(`${label}必须是非负数字`);
    return null;
  }
  return parsed;
}

function normalizeHexSearch(value: string): string | null {
  const normalized = value.replace(/\s+/g, "").toUpperCase();
  if (!normalized) return "";
  if (!/^[0-9A-F]+$/.test(normalized) || normalized.length % 2 !== 0) {
    ElMessage.warning("搜索 Hex 必须是偶数个十六进制字符");
    return null;
  }
  return normalized;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
}

function formatOffset(value: number): string {
  return Math.max(0, value).toString(16).toUpperCase().padStart(8, "0");
}

function formatDate(timestamp: number): string {
  return new Date(timestamp).toLocaleString();
}

function highlightHex(hex: string): string {
  return hex.replace(/([0-9A-F]{2})/g, '<span class="hex-byte">$1</span>');
}

function confidencePercent(value?: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.max(0, Math.min(100, Math.round((value || 0) * 100)));
}

function compactEvidence(evidence?: string[], relatedStrings?: string[]): string {
  const parts = [...(evidence || []), ...(relatedStrings || []).map((item) => `相关字符串: ${item}`)];
  return parts.length ? parts.join(" | ") : "-";
}

async function downloadReport(format: "json" | "html") {
  if (!selectedFile.value) return warnSelectFile();
  try {
    const formData = new FormData();
    formData.append("file", selectedFile.value);
    if (settingsStore.apiKey) {
      formData.append("apiKey", settingsStore.apiKey);
      formData.append("provider", settingsStore.selectedProvider);
      formData.append("model", settingsStore.selectedModel);
      if (settingsStore.aiApiUrl) formData.append("apiUrl", settingsStore.aiApiUrl);
    }

    const token = localStorage.getItem("token");
    const response = await fetch(getExportUrl(format), {
      method: "POST",
      body: formData,
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    });
    if (!response.ok) throw new Error("导出失败");

    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = format === "json" ? "binary-analysis-report.json" : "binary-analysis-report.html";
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    ElMessage.success("导出成功");
  } catch (error) {
    ElMessage.error(readApiErrorMessage(error, "导出失败"));
  }
}

function animateEntry() {
  const panels = document.querySelectorAll(".binary-workbench .file-panel, .binary-workbench .tool-tabs");
  if (!panels.length) return;
  animate(panels, {
    opacity: [{ from: 0 }, { to: 1 }],
    translateY: [{ from: 16 }, { to: 0 }],
    duration: 360,
    delay: stagger(80),
    ease: "out(3)",
  });
}

function animateResults() {
  nextTick(() => {
    const nodes = document.querySelectorAll(".binary-workbench .summary-grid, .binary-workbench .result-card, .binary-workbench .peinfo-card, .binary-workbench .vtable-card, .binary-workbench .hex-card");
    if (!nodes.length) return;
    animate(nodes, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 14 }, { to: 0 }],
      duration: 320,
      delay: stagger(55),
      ease: "out(3)",
    });
  });
}
</script>

<style scoped>
.binary-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1180px;
  margin: 0 auto;
}

.workspace-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.title-block {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.title-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  background: #18202a;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.title-block h1 {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 4px;
  letter-spacing: 0;
}

.title-block p {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.file-panel,
.tool-tabs {
  opacity: 0;
}

.drop-zone {
  min-height: 128px;
  border: 1px dashed rgba(74, 222, 128, 0.35);
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, rgba(74, 222, 128, 0.07), rgba(59, 130, 246, 0.04));
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  cursor: pointer;
  transition: border-color 0.2s var(--transition-smooth), background 0.2s var(--transition-smooth);
}

.drop-zone:hover,
.drop-zone.is-dragover {
  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(74, 222, 128, 0.12), rgba(59, 130, 246, 0.06));
}

.upload-icon {
  width: 58px;
  height: 58px;
  border-radius: var(--radius-md);
  background: rgba(15, 23, 42, 0.08);
  color: var(--color-primary-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.upload-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.upload-copy strong {
  color: var(--color-text);
  font-size: 15px;
}

.upload-copy span {
  color: var(--color-text-secondary);
  font-size: 13px;
}

.file-input {
  display: none;
}

.file-strip {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background: var(--color-bg);
}

.file-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.file-name {
  color: var(--color-text);
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 560px;
}

.file-meta {
  color: var(--color-text-secondary);
  font-size: 12px;
  margin-top: 2px;
}

.file-actions {
  flex-shrink: 0;
}

.tool-tabs {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 4px 16px 18px;
  box-shadow: var(--shadow-sm);
}

:deep(.tool-tabs .el-tabs__header) {
  margin-bottom: 14px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tool-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 6px 0 16px;
}

.tool-action-bar h2 {
  margin: 0 0 4px;
  font-size: 16px;
  color: var(--color-text);
  letter-spacing: 0;
}

.tool-action-bar p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric {
  border-radius: var(--radius-md);
  background: rgba(74, 222, 128, 0.08);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.metric.blue {
  background: rgba(59, 130, 246, 0.09);
}

.metric.amber {
  background: rgba(245, 158, 11, 0.12);
}

.metric.purple {
  background: rgba(124, 58, 237, 0.1);
}

.metric-value {
  font-size: 24px;
  line-height: 1.15;
  color: var(--color-text);
  font-weight: 700;
}

.metric-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.option-grid {
  display: grid;
  grid-template-columns: minmax(160px, 220px) minmax(160px, 220px) minmax(240px, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.option-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.option-field span {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.result-card {
  overflow: hidden;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.empty-state {
  background: var(--color-bg);
  border-radius: var(--radius-md);
  min-height: 220px;
}

.world-card {
  margin-bottom: 16px;
  overflow: hidden;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text);
  font-size: 15px;
  font-weight: 600;
}

.world-summary {
  margin: 0 0 12px;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.hint-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.hint-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 9px 11px;
  border-radius: var(--radius-sm);
  background: rgba(74, 222, 128, 0.08);
  color: var(--color-text);
  font-size: 13px;
  line-height: 1.6;
}

.world-table {
  margin-top: 8px;
}

.candidate-name {
  color: var(--color-text);
  font-weight: 600;
}

.candidate-sub {
  margin-top: 2px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.confidence-text {
  display: inline-block;
  margin-top: 4px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.related-collapse {
  margin-top: 12px;
  border: none;
}

.hex-info {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  padding: 12px 14px;
  margin-bottom: 12px;
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 13px;
}

.hex-info b {
  color: var(--color-text);
}

.hex-card {
  overflow: hidden;
}

.hex-scroll {
  max-height: 560px;
  overflow: auto;
  font-family: "Cascadia Code", "Consolas", "Courier New", monospace;
  font-size: 13px;
  line-height: 1.7;
}

.hex-header,
.hex-line {
  display: grid;
  grid-template-columns: 96px minmax(520px, 1fr) 180px;
  gap: 12px;
  align-items: center;
  padding: 2px 16px;
}

.hex-header {
  position: sticky;
  top: 0;
  z-index: 1;
  padding-top: 8px;
  padding-bottom: 8px;
  background: #22232a;
  color: #94a3b8;
  font-weight: 600;
}

.hex-line:nth-child(odd) {
  background: rgba(15, 23, 42, 0.03);
}

.hex-line:hover {
  background: rgba(74, 222, 128, 0.08);
}

.col-offset {
  color: #64748b;
}

.col-hex {
  color: #d1d5db;
  letter-spacing: 0;
  white-space: pre;
}

.col-ascii {
  color: var(--color-primary-dark);
  white-space: pre;
}

:deep(.hex-byte) {
  color: #93c5fd;
}

@media (max-width: 900px) {
  .workspace-head,
  .tool-action-bar,
  .file-strip {
    align-items: stretch;
    flex-direction: column;
  }

  .head-actions,
  .action-buttons {
    justify-content: flex-start;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .option-grid {
    grid-template-columns: 1fr;
  }

  .file-name {
    max-width: 100%;
  }

  .hex-header,
  .hex-line {
    grid-template-columns: 88px minmax(500px, 1fr) 140px;
  }
}

@media (max-width: 640px) {
  .drop-zone {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .tool-tabs {
    padding-left: 10px;
    padding-right: 10px;
  }
}
</style>
