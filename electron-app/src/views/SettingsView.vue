<template>
  <div class="settings">
    <el-card>
      <template #header><span style="font-weight:600">DeepSeek API 设置</span></template>
      <el-form label-width="100px">
        <el-form-item label="API Key">
          <el-input
            v-model="apiKey"
            :type="showKey ? 'text' : 'password'"
            placeholder="输入你的 DeepSeek API Key"
          >
            <template #append>
              <el-button @click="showKey = !showKey">
                <el-icon><component :is="showKey ? 'Hide' : 'View'" /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="模型选择">
          <el-select v-model="model" style="width: 100%">
            <el-option
              v-for="m in models"
              :key="m.id"
              :label="m.name + ' - ' + m.description"
              :value="m.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
          <el-button @click="testConnection">测试连接</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getModels, healthCheck } from "@/api/client";
import { useSettingsStore } from "@/stores/settings";
import type { ModelInfo } from "@/types";

const store = useSettingsStore();
const apiKey = ref(store.apiKey);
const model = ref(store.selectedModel);
const showKey = ref(false);
const models = ref<ModelInfo[]>([]);

onMounted(async () => {
  try {
    models.value = await getModels();
  } catch {
    models.value = [
      { id: "deepseek-chat", name: "DeepSeek-V3", description: "通用对话，速度快" },
      { id: "deepseek-reasoner", name: "DeepSeek-R1", description: "深度推理，更准确" },
      { id: "deepseek-v4-flash", name: "DeepSeek-V4 Flash", description: "轻量快速，性价比高" },
      { id: "deepseek-v4-pro", name: "DeepSeek-V4 Pro", description: "专业版，能力最强" },
    ];
  }
});

function save() {
  store.setApiKey(apiKey.value);
  store.setModel(model.value);
  ElMessage.success("设置已保存");
}

async function testConnection() {
  const ok = await healthCheck();
  ElMessage[ok ? "success" : "error"](ok ? "服务器连接正常" : "无法连接服务器");
}
</script>

<style scoped>
.settings { max-width: 600px; }
</style>
