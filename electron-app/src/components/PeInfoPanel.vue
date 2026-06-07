<template>
  <el-card class="peinfo-card">
    <template #header>
      <div class="card-header">
        <el-icon :size="18" color="var(--color-primary)"><Files /></el-icon>
        <span class="header-title">PE 文件信息</span>
        <el-tag v-for="src in peInfo.parserSources" :key="src" size="small" type="info" round class="parser-tag">{{ src }}</el-tag>
      </div>
    </template>

    <el-descriptions :column="3" border size="small" class="pe-descriptions">
      <el-descriptions-item label="文件名">{{ peInfo.fileName }}</el-descriptions-item>
      <el-descriptions-item label="架构">{{ peInfo.machine }} ({{ peInfo.magic }})</el-descriptions-item>
      <el-descriptions-item label="子系统">{{ peInfo.subsystem }}</el-descriptions-item>
      <el-descriptions-item label="ImageBase">
        <span class="mono">0x{{ peInfo.imageBase.toString(16).toUpperCase() }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="段数">{{ peInfo.numberOfSections }}</el-descriptions-item>
      <el-descriptions-item label="CheckSum">
        <span class="mono">0x{{ peInfo.checkSum.toString(16).toUpperCase() }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="导出函数">{{ peInfo.exportCount }}</el-descriptions-item>
      <el-descriptions-item label="导入函数">{{ peInfo.importCount }}</el-descriptions-item>
      <el-descriptions-item label="签名">
        <el-tag :type="peInfo.hasCertificate ? 'success' : 'info'" size="small" round>
          {{ peInfo.hasCertificate ? '有' : '无' }}
        </el-tag>
      </el-descriptions-item>
    </el-descriptions>

    <div v-if="peInfo.dllCharacteristics?.length" class="security-flags">
      <span class="flags-label">安全标志:</span>
      <el-tag v-for="ch in peInfo.dllCharacteristics" :key="ch" size="small" type="warning" round class="flag-tag">{{ ch }}</el-tag>
    </div>

    <el-collapse class="pe-collapse">
      <el-collapse-item title="段表 (Sections)" name="sections">
        <el-table :data="peInfo.sections" size="small" stripe class="section-table">
          <el-table-column prop="name" label="段名" width="100">
            <template #default="{ row }">
              <span class="mono">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="rva" label="RVA" width="120">
            <template #default="{ row }">
              <span class="mono">{{ row.rva }}</span>
            </template>
          </el-table-column>
          <el-table-column label="VirtualSize" width="120">
            <template #default="{ row }">
              <span class="mono">0x{{ row.virtualSize.toString(16) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="RawSize" width="120">
            <template #default="{ row }">
              <span class="mono">0x{{ row.rawSize.toString(16) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="characteristics" label="属性" />
        </el-table>
      </el-collapse-item>

      <el-collapse-item :title="'导出函数 (' + peInfo.exportCount + ')'" name="exports" v-if="peInfo.exports?.length">
        <el-table :data="peInfo.exports.slice(0, 100)" size="small" stripe max-height="300" class="export-table">
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="ordinal" label="序号" width="80" align="center" />
          <el-table-column prop="rva" label="RVA" width="120">
            <template #default="{ row }">
              <span class="mono">{{ row.rva }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Forwarder" width="80">
            <template #default="{ row }">{{ row.forwarder ? 'Yes' : '' }}</template>
          </el-table-column>
        </el-table>
      </el-collapse-item>

      <el-collapse-item :title="'导入函数 (' + peInfo.importCount + ')'" name="imports" v-if="peInfo.imports?.length">
        <div v-for="imp in peInfo.imports" :key="imp.dllName" class="import-item">
          <el-tag type="warning" size="small" round>{{ imp.dllName }}</el-tag>
          <span class="import-count">{{ imp.functions?.length || 0 }} 个函数</span>
        </div>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<script setup lang="ts">
import type { PeInfo } from "@/types";
defineProps<{ peInfo: PeInfo }>();
</script>

<style scoped>
.peinfo-card {
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
.parser-tag {
  margin-left: 4px;
}

/* 等宽字体 */
.mono {
  font-family: "SF Mono", "Cascadia Code", "Consolas", monospace;
  font-size: 12.5px;
  letter-spacing: 0.3px;
}

/* 描述列表主题覆盖 */
.pe-descriptions {
  border-radius: var(--radius-sm);
  overflow: hidden;
}

/* 安全标志区 */
.security-flags {
  margin-top: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.flags-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-right: 2px;
}
.flag-tag {
  margin: 0;
}

/* 折叠面板 */
.pe-collapse {
  margin-top: 16px;
  border: none;
}

/* 内嵌表格 */
.section-table,
.export-table {
  border-radius: var(--radius-sm);
  overflow: hidden;
}

/* 导入项 */
.import-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.import-count {
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>