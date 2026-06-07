<template>
  <el-card>
    <el-upload
      class="uploader"
      drag
      :auto-upload="false"
      :show-file-list="false"
      accept=".dll"
      :on-change="onFileChange"
      v-loading="loading"
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div class="upload-text">拖拽 DLL 文件到此处，或 <em>点击上传</em></div>
      <div class="upload-hint">仅支持 .dll 文件</div>
    </el-upload>
    <div v-if="file" class="file-info">
      <el-tag type="success">{{ file.name }}</el-tag>
      <span class="file-size">{{ formatSize(file.size) }}</span>
      <el-button type="primary" @click="emit('upload', file)" :loading="loading" style="margin-left:12px">
        开始分析
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { UploadFile } from "element-plus";

defineProps<{ loading: boolean }>();
const emit = defineEmits<{ upload: [file: File] }>();

const file = ref<File | null>(null);

function onFileChange(uploadFile: UploadFile) {
  file.value = uploadFile.raw;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(2) + " MB";
}
</script>

<style scoped>
.uploader { width: 100%; }
.upload-icon { font-size: 48px; color: #c0c4cc; }
.upload-text { color: #606266; font-size: 14px; margin-top: 8px; }
.upload-text em { color: #409eff; font-style: normal; }
.upload-hint { color: #909399; font-size: 12px; margin-top: 4px; }
.file-info { margin-top: 12px; display: flex; align-items: center; }
.file-size { color: #909399; font-size: 13px; margin-left: 8px; }
</style>
