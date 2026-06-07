<template>
  <div class="settings">
    <el-card>
      <template #header>
        <div style="display:flex;align-items:center;gap:8px">
          <el-icon :size="18" color="var(--color-primary)"><Setting /></el-icon>
          <span style="font-weight:600;font-size:15px">AI 模型设置</span>
        </div>
      </template>
      <el-form label-width="90px" label-position="left">
        <el-form-item label="AI 提供商">
          <el-select v-model="provider" style="width: 100%" @change="onProviderChange">
            <el-option v-for="p in providers" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型选择">
          <el-select v-model="model" style="width: 100%">
            <el-option v-for="m in currentModels" :key="m.id" :label="m.name + ' - ' + m.description" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="apiKey" :type="showKey ? 'text' : 'password'" placeholder="输入 API Key">
            <template #append>
              <el-button @click="showKey = !showKey">
                <el-icon><component :is="showKey ? 'Hide' : 'View'" /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save" round>保存设置</el-button>
          <el-button @click="testConnection" round>测试连接</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getProviders, healthCheck } from "@/api/client";
import { useSettingsStore } from "@/stores/settings";
import type { ProviderInfo } from "@/types";

const store = useSettingsStore();
const apiKey = ref(store.apiKey);
const provider = ref(store.selectedProvider);
const model = ref(store.selectedModel);
const showKey = ref(false);
const providers = ref<ProviderInfo[]>([]);

const currentModels = computed(() => {
  const p = providers.value.find(p => p.id === provider.value);
  return p?.models || [];
});

onMounted(async () => {
  try {
    providers.value = await getProviders();
  } catch {
    providers.value = [
      { id: "deepseek", name: "DeepSeek", models: [
        { id: "deepseek-v4-flash", name: "DeepSeek-V4 Flash", description: "轻量快速" },
        { id: "deepseek-v4-pro", name: "DeepSeek-V4 Pro", description: "专业版" },
      ]},
      { id: "xiaomi", name: "小米 MiMo", models: [
        { id: "MiMo-V2.5", name: "MiMo-V2.5", description: "小米主模型" },
        { id: "mimo-v2.5-pro", name: "MiMo-V2.5 Pro", description: "专业版" },
      ]},
    ];
  }
});

function onProviderChange(val: string) {
  const p = providers.value.find(p => p.id === val);
  if (p?.models?.length) model.value = p.models[0].id;
}

function save() {
  store.setApiKey(apiKey.value);
  store.setProvider(provider.value);
  store.setModel(model.value);
  ElMessage.success("设置已保存");
}

async function testConnection() {
  const ok = await healthCheck();
  ElMessage[ok ? "success" : "error"](ok ? "服务器连接正常" : "无法连接服务器");
}
</script>

<style scoped>
.settings { max-width: 560px; }
</style>