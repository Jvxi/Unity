<template>
  <el-card class="vtable-card">
    <template #header>
      <div class="card-header">
        <el-icon :size="18" color="var(--color-primary)"><List /></el-icon>
        <span class="header-title">虚表分析结果</span>
        <el-tag v-if="vtables.length" type="success" size="small" round>{{ vtables.length }} 个</el-tag>
      </div>
    </template>

    <el-empty v-if="!vtables.length" description="未检测到虚表" />

    <el-table v-else :data="vtables" stripe size="small" class="vtable-table">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <div class="expand-header">
              <el-icon :size="14" color="var(--color-primary)"><Document /></el-icon>
              <span>虚函数列表 ({{ row.functionCount }} 个)</span>
            </div>
            <el-table :data="row.functions" size="small" stripe class="func-table">
              <el-table-column prop="index" label="#" width="50" />
              <el-table-column prop="rva" label="RVA" width="140" />
              <el-table-column prop="va" label="VA" />
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="#" type="index" width="50" />
      <el-table-column prop="rva" label="虚表 RVA" width="140">
        <template #default="{ row }">
          <span class="mono">{{ row.rva }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="va" label="虚表 VA" width="200">
        <template #default="{ row }">
          <span class="mono">{{ row.va }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="functionCount" label="函数数" width="80" align="center" />
      <el-table-column prop="detectionMethod" label="检测方法" width="120">
        <template #default="{ row }">
          <el-tag :type="methodTagType(row.detectionMethod)" size="small" round>{{ methodLabel(row.detectionMethod) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="rttiTypeName" label="RTTI 类型名" width="160">
        <template #default="{ row }">
          <span class="mono">{{ row.rttiTypeName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="relatedSymbol" label="关联符号" width="140" />
    </el-table>

    <div v-if="aiSummary" class="ai-summary">
      <div class="summary-header">
        <el-icon :size="16" color="var(--color-primary)"><MagicStick /></el-icon>
        <span>AI 分析摘要</span>
      </div>
      <pre class="summary-text">{{ aiSummary }}</pre>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { VtableInfo } from "@/types";

defineProps<{ vtables: VtableInfo[]; aiSummary: string }>();

function methodTagType(method: string): string {
  switch (method) {
    case "RTTI": return "success";
    case "POINTER_SCAN": return "warning";
    case "EXPORT_REF": return "";
    default: return "info";
  }
}

function methodLabel(method: string): string {
  switch (method) {
    case "RTTI": return "RTTI";
    case "POINTER_SCAN": return "指针扫描";
    case "EXPORT_REF": return "导出引用";
    default: return method;
  }
}
</script>

<style scoped>
.vtable-card {
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text);
}

/* 表格主题覆盖 */
.vtable-table {
  border-radius: var(--radius-md);
  overflow: hidden;
}

/* 等宽字体 */
.mono {
  font-family: "SF Mono", "Cascadia Code", "Consolas", monospace;
  font-size: 12.5px;
  letter-spacing: 0.3px;
}

/* 展开行 */
.expand-content {
  padding: 12px 20px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  margin: 4px 8px;
}
.expand-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text);
}
.func-table {
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

/* AI 摘要区 */
.ai-summary {
  margin-top: 16px;
  padding: 16px 20px;
  background: var(--color-primary-bg);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-primary);
}
.summary-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text);
}
.summary-text {
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.8;
  color: var(--color-text);
  font-family: inherit;
  margin: 0;
}
</style>