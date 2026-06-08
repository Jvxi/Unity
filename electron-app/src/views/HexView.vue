<template>
  <div class="hex-view">
    <!-- 上传区域 -->
    <el-card class="upload-card" shadow="never">
      <div class="upload-header">
        <el-icon :size="24" color="#4ade80"><Grid /></el-icon>
        <div>
          <h3>十六进制查看器</h3>
          <p class="hint">以 Hex Dump 形式查看二进制文件内容</p>
        </div>
      </div>
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="onFileChange"
        :on-exceed="() => ElMessage.warning('请先移除已选文件')"
        accept=".dll,.exe,.sys,.ocx,.bin,.dat"
      >
        <el-icon :size="40"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
      </el-upload>

      <div class="options-row">
        <span class="opt-label">偏移</span>
        <el-input v-model.number="offset" size="small" style="width: 120px" placeholder="0" />
        <span class="opt-label">长度</span>
        <el-input v-model.number="length" size="small" style="width: 120px" placeholder="0=自动" />
        <el-input v-model="searchHex" placeholder="搜索 Hex (如 4D5A)" size="small" style="width: 200px" clearable />
        <el-button type="primary" :loading="loading" @click="doDump">
          <el-icon><View /></el-icon> 查看
        </el-button>
      </div>
    </el-card>

    <!-- 统计信息 -->
    <el-card v-if="hexResult" class="info-card" shadow="never">
      <div class="info-row">
        <span>总大小: <b>{{ formatSize(hexResult.totalBytes) }}</b></span>
        <span>显示范围: <b>0x{{ hexResult.startOffset.toString(16).toUpperCase() }}</b> - <b>0x{{ hexResult.endOffset.toString(16).toUpperCase() }}</b></span>
        <span>行数: <b>{{ hexResult.totalLines }}</b></span>
      </div>
    </el-card>

    <!-- Hex Dump -->
    <el-card v-if="hexResult && hexResult.lines.length > 0" class="hex-card" shadow="never">
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

    <el-card v-if="hexResult && hexResult.lines.length === 0" shadow="never" class="empty-card">
      <el-empty description="未找到匹配数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import http from "@/api/client";

interface HexLine {
  offset: number;
  hex: string;
  ascii: string;
}
interface HexResult {
  lines: HexLine[];
  totalBytes: number;
  startOffset: number;
  endOffset: number;
  totalLines: number;
}

const file = ref<File | null>(null);
const offset = ref(0);
const length = ref(0);
const searchHex = ref("");
const loading = ref(false);
const hexResult = ref<HexResult | null>(null);

function onFileChange(f: any) {
  file.value = f.raw;
  hexResult.value = null;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / 1048576).toFixed(1) + " MB";
}

function formatOffset(val: number): string {
  return val.toString(16).toUpperCase().padStart(8, '0');
}

function highlightHex(hex: string): string {
  // 将每两个字符分组，保留空格
  return hex.replace(/([0-9A-F]{2})/g, '<span class="hex-byte">$1</span>');
}

async function doDump() {
  if (!file.value) {
    ElMessage.warning("请先选择文件");
    return;
  }
  loading.value = true;
  try {
    const fd = new FormData();
    fd.append("file", file.value);
    fd.append("offset", String(offset.value || 0));
    fd.append("length", String(length.value || 0));
    if (searchHex.value) fd.append("search", searchHex.value);
    const res = await http.post("/api/tools/hex", fd, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    if (res.data.success) {
      hexResult.value = res.data.data;
      ElMessage.success(`加载完成，${res.data.data.totalLines} 行`);
    } else {
      ElMessage.error(res.data.error || "加载失败");
    }
  } catch (e: any) {
    ElMessage.error(e.message || "请求失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.hex-view { display: flex; flex-direction: column; gap: 16px; max-width: 1000px; }
.upload-card { border-radius: var(--radius-lg); }
.upload-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.upload-header h3 { font-size: 16px; font-weight: 600; color: var(--color-text); margin: 0; }
.hint { font-size: 12px; color: var(--color-text-secondary); margin: 2px 0 0; }
.options-row { display: flex; align-items: center; gap: 12px; margin-top: 16px; flex-wrap: wrap; }
.opt-label { font-size: 13px; color: var(--color-text-secondary); }

.info-card { border-radius: var(--radius-lg); }
.info-row { display: flex; gap: 24px; font-size: 13px; color: var(--color-text-secondary); }
.info-row b { color: var(--color-text); }

.hex-card { border-radius: var(--radius-lg); padding: 0; }
.hex-scroll { max-height: 520px; overflow-y: auto; font-family: 'Cascadia Code', 'Consolas', 'Courier New', monospace; font-size: 13px; line-height: 1.7; }
.hex-header { display: flex; padding: 8px 16px; background: #22232a; color: #6b7280; font-weight: 600; position: sticky; top: 0; z-index: 1; border-bottom: 1px solid #2d2e33; }
.hex-line { display: flex; padding: 1px 16px; }
.hex-line:nth-child(odd) { background: rgba(255,255,255,0.02); }
.hex-line:hover { background: rgba(74,222,128,0.06); }
.col-offset { width: 90px; flex-shrink: 0; color: #6b7280; }
.col-hex { flex: 1; color: #d1d5db; letter-spacing: 0.5px; }
.col-ascii { width: 180px; flex-shrink: 0; color: #4ade80; white-space: pre; }
.empty-card { border-radius: var(--radius-lg); }

:deep(.hex-byte) { color: #93c5fd; }
</style>
