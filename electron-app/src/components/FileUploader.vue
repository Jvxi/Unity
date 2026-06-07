<template>
  <el-card class="upload-card">
    <div class="upload-zone" :class="{ 'is-dragover': isDragover }"
         @dragover.prevent="isDragover = true"
         @dragleave="isDragover = false"
         @drop.prevent="onDrop"
         @click="triggerInput">
      <div class="upload-inner">
        <div class="upload-icon-wrap">
          <el-icon :size="32" color="var(--color-primary)"><UploadFilled /></el-icon>
        </div>
        <div class="upload-text">拖拽 DLL 文件到此处，或 <em>点击选择</em></div>
        <div class="upload-hint">仅支持 .dll 文件</div>
      </div>
      <input ref="fileInput" type="file" accept=".dll" style="display:none" @change="onFileChange" />
    </div>
    <div v-if="file" class="file-info">
      <el-tag type="success" effect="light" round>{{ file.name }}</el-tag>
      <span class="file-size">{{ formatSize(file.size) }}</span>
      <el-button type="primary" round :loading="loading" @click.stop="emit('upload', file)" style="margin-left:auto">
        <el-icon><VideoPlay /></el-icon>
        开始分析
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from "vue";

defineProps<{ loading: boolean }>();
const emit = defineEmits<{ upload: [file: File] }>();

const file = ref<File | null>(null);
const isDragover = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);

function triggerInput() { fileInput.value?.click(); }
function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement;
  if (input.files?.[0]) file.value = input.files[0];
}
function onDrop(e: DragEvent) {
  isDragover.value = false;
  if (e.dataTransfer?.files?.[0]) file.value = e.dataTransfer.files[0];
}
function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / 1048576).toFixed(2) + " MB";
}
</script>

<style scoped>
.upload-card { overflow: hidden; }
.upload-zone {
  border: 2px dashed rgba(74,222,128,0.25);
  border-radius: var(--radius-lg);
  padding: 36px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s var(--transition-smooth);
  background: var(--color-primary-bg);
}
.upload-zone:hover, .upload-zone.is-dragover {
  border-color: var(--color-primary);
  background: rgba(74,222,128,0.06);
}
.upload-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  background: rgba(74,222,128,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}
.upload-text { color: var(--color-text); font-size: 14px; }
.upload-text em { color: var(--color-primary-dark); font-style: normal; font-weight: 600; }
.upload-hint { color: var(--color-text-secondary); font-size: 12px; margin-top: 4px; }
.file-info {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.file-size { color: var(--color-text-secondary); font-size: 13px; }
</style>