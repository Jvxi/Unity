<template>
  <div class="strings-view">
    <!-- 上传区域 -->
    <el-card class="upload-card" shadow="never">
      <div class="upload-header">
        <el-icon :size="24" color="#4ade80"><Document /></el-icon>
        <div>
          <h3>字符串提取</h3>
          <p class="hint">从二进制文件中提取 ASCII / Unicode 字符串</p>
        </div>
      </div>
      <el-upload
        ref="uploadRef"
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
        <el-input-number v-model="minLength" :min="2" :max="256" size="small" />
        <span class="opt-label">最小长度</span>
        <el-select v-model="encoding" size="small" style="width: 140px">
          <el-option label="全部" value="all" />
          <el-option label="ASCII" value="ascii" />
          <el-option label="Unicode" value="unicode" />
        </el-select>
        <el-input v-model="keyword" placeholder="关键词过滤 (可选)" size="small" style="width: 200px" clearable />
        <el-button type="primary" :loading="loading" @click="doExtract">
          <el-icon><Search /></el-icon> 提取
        </el-button>
      </div>
    </el-card>

    <!-- 结果统计 -->
    <el-card v-if="result" class="stat-card" shadow="never">
      <div class="stat-row">
        <div class="stat-item">
          <span class="stat-num">{{ result.totalCount }}</span>
          <span class="stat-label">总计</span>
        </div>
        <div class="stat-item">
          <span class="stat-num ascii">{{ result.asciiCount }}</span>
          <span class="stat-label">ASCII</span>
        </div>
        <div class="stat-item">
          <span class="stat-num unicode">{{ result.unicodeCount }}</span>
          <span class="stat-label">Unicode</span>
        </div>
      </div>
    </el-card>

    <!-- 结果表格 -->
    <el-card v-if="result" class="table-card" shadow="never">
      <el-table :data="pagedData" stripe size="small" max-height="520">
        <el-table-column label="偏移" width="130">
          <template #default="{ row }">0x{{ row.offset.toString(16).toUpperCase() }}</template>
        </el-table-column>
        <el-table-column prop="encoding" label="编码" width="90">
          <template #default="{ row }">
            <el-tag :type="row.encoding === 'ascii' ? '' : 'warning'" size="small">{{ row.encoding }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="length" label="长度" width="70" />
        <el-table-column prop="value" label="内容" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-if="result.totalCount > pageSize"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="result.totalCount"
        layout="prev, pager, next, total"
        style="margin-top: 12px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { ElMessage } from "element-plus";
import http from "@/api/client";

interface StringEntry {
  offset: number;
  value: string;
  encoding: string;
  length: number;
}
interface ExtractResult {
  strings: StringEntry[];
  totalCount: number;
  asciiCount: number;
  unicodeCount: number;
}

const file = ref<File | null>(null);
const minLength = ref(4);
const encoding = ref("all");
const keyword = ref("");
const loading = ref(false);
const result = ref<ExtractResult | null>(null);
const currentPage = ref(1);
const pageSize = 100;

function onFileChange(f: any) {
  file.value = f.raw;
  result.value = null;
}

const pagedData = computed(() => {
  if (!result.value) return [];
  const start = (currentPage.value - 1) * pageSize;
  return result.value.strings.slice(start, start + pageSize);
});

async function doExtract() {
  if (!file.value) {
    ElMessage.warning("请先选择文件");
    return;
  }
  loading.value = true;
  try {
    const fd = new FormData();
    fd.append("file", file.value);
    fd.append("minLength", String(minLength.value));
    fd.append("encoding", encoding.value);
    if (keyword.value) fd.append("keyword", keyword.value);
    const res = await http.post("/api/tools/strings", fd, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    if (res.data.success) {
      result.value = res.data.data;
      currentPage.value = 1;
      ElMessage.success(`提取完成，共 ${res.data.data.totalCount} 条`);
    } else {
      ElMessage.error(res.data.error || "提取失败");
    }
  } catch (e: any) {
    ElMessage.error(e.message || "请求失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.strings-view { display: flex; flex-direction: column; gap: 16px; max-width: 960px; }
.upload-card { border-radius: var(--radius-lg); }
.upload-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.upload-header h3 { font-size: 16px; font-weight: 600; color: var(--color-text); margin: 0; }
.hint { font-size: 12px; color: var(--color-text-secondary); margin: 2px 0 0; }
.options-row { display: flex; align-items: center; gap: 12px; margin-top: 16px; flex-wrap: wrap; }
.opt-label { font-size: 13px; color: var(--color-text-secondary); }
.stat-card { border-radius: var(--radius-lg); }
.stat-row { display: flex; gap: 32px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 28px; font-weight: 700; color: var(--color-primary); }
.stat-num.ascii { color: #3b82f6; }
.stat-num.unicode { color: #f59e0b; }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.table-card { border-radius: var(--radius-lg); }
</style>
