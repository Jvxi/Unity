<template>
  <el-card>
    <template #header>
      <span style="font-weight:600">PE 文件信息</span>
      <el-tag v-for="src in peInfo.parserSources" :key="src" size="small" style="margin-left:8px">{{ src }}</el-tag>
    </template>

    <el-descriptions :column="3" border size="small">
      <el-descriptions-item label="文件名">{{ peInfo.fileName }}</el-descriptions-item>
      <el-descriptions-item label="架构">{{ peInfo.machine }} ({{ peInfo.magic }})</el-descriptions-item>
      <el-descriptions-item label="子系统">{{ peInfo.subsystem }}</el-descriptions-item>
      <el-descriptions-item label="ImageBase">0x{{ peInfo.imageBase.toString(16).toUpperCase() }}</el-descriptions-item>
      <el-descriptions-item label="段数">{{ peInfo.numberOfSections }}</el-descriptions-item>
      <el-descriptions-item label="CheckSum">0x{{ peInfo.checkSum.toString(16).toUpperCase() }}</el-descriptions-item>
      <el-descriptions-item label="导出函数">{{ peInfo.exportCount }}</el-descriptions-item>
      <el-descriptions-item label="导入函数">{{ peInfo.importCount }}</el-descriptions-item>
      <el-descriptions-item label="签名">{{ peInfo.hasCertificate ? '有' : '无' }}</el-descriptions-item>
    </el-descriptions>

    <div v-if="peInfo.dllCharacteristics?.length" style="margin-top:12px">
      <span style="font-size:13px;color:#606266;margin-right:8px">安全标志:</span>
      <el-tag v-for="ch in peInfo.dllCharacteristics" :key="ch" size="small" type="info" style="margin:2px">{{ ch }}</el-tag>
    </div>

    <el-collapse style="margin-top:16px">
      <el-collapse-item title="段表 (Sections)" name="sections">
        <el-table :data="peInfo.sections" size="small" stripe>
          <el-table-column prop="name" label="段名" width="100" />
          <el-table-column prop="rva" label="RVA" width="120" />
          <el-table-column label="VirtualSize" width="120">
            <template #default="{ row }">0x{{ row.virtualSize.toString(16) }}</template>
          </el-table-column>
          <el-table-column label="RawSize" width="120">
            <template #default="{ row }">0x{{ row.rawSize.toString(16) }}</template>
          </el-table-column>
          <el-table-column prop="characteristics" label="属性" />
        </el-table>
      </el-collapse-item>

      <el-collapse-item :title="'导出函数 (' + peInfo.exportCount + ')'" name="exports" v-if="peInfo.exports?.length">
        <el-table :data="peInfo.exports.slice(0, 100)" size="small" stripe max-height="300">
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="ordinal" label="序号" width="80" />
          <el-table-column prop="rva" label="RVA" width="120" />
          <el-table-column label="Forwarder" width="80">
            <template #default="{ row }">{{ row.forwarder ? 'Yes' : '' }}</template>
          </el-table-column>
        </el-table>
      </el-collapse-item>

      <el-collapse-item :title="'导入函数 (' + peInfo.importCount + ')'" name="imports" v-if="peInfo.imports?.length">
        <div v-for="imp in peInfo.imports" :key="imp.dllName" style="margin-bottom:8px">
          <el-tag type="warning" size="small">{{ imp.dllName }}</el-tag>
          <span style="font-size:12px;color:#909399;margin-left:8px">{{ imp.functions?.length || 0 }} 个函数</span>
        </div>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<script setup lang="ts">
import type { PeInfo } from "@/types";
defineProps<{ peInfo: PeInfo }>();
</script>