<template>
  <el-card>
    <template #header>
      <span style="font-weight:600">虚表分析结果 ({{ vtables.length }} 个)</span>
    </template>

    <el-empty v-if="!vtables.length" description="未检测到虚表" />

    <el-table v-else :data="vtables" stripe border size="small">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div style="padding:8px 16px">
            <h4 style="margin-bottom:8px">虚函数列表 ({{ row.functionCount }} 个)</h4>
            <el-table :data="row.functions" size="small" stripe>
              <el-table-column prop="index" label="#" width="50" />
              <el-table-column prop="rva" label="RVA" width="140" />
              <el-table-column prop="va" label="VA" width="200" />
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="#" type="index" width="50" />
      <el-table-column prop="rva" label="虚表 RVA" width="140" />
      <el-table-column prop="va" label="虚表 VA" width="200" />
      <el-table-column prop="functionCount" label="函数数" width="80" />
      <el-table-column prop="detectionMethod" label="检测方法" width="120">
        <template #default="{ row }">
          <el-tag :type="methodTagType(row.detectionMethod)" size="small">{{ row.detectionMethod }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="rttiTypeName" label="RTTI 类型名" width="160" />
      <el-table-column prop="relatedSymbol" label="关联符号" width="140" />
    </el-table>

    <el-card v-if="aiSummary" style="margin-top:16px" shadow="never">
      <template #header><span style="font-weight:600">AI 分析摘要</span></template>
      <pre style="white-space:pre-wrap;font-size:13px;line-height:1.8;color:#303133">{{ aiSummary }}</pre>
    </el-card>
  </el-card>
</template>

<script setup lang="ts">
import type { VtableInfo } from "@/types";

defineProps<{ vtables: VtableInfo[]; aiSummary: string }>();

function methodTagType(method: string): string {
  switch (method) {
    case "RTTI": return "success";
    case "POINTER_SCAN": return "warning";
    case "EXPORT_REF": return "info";
    default: return "";
  }
}
</script>