<template>
  <div class="settings-page" ref="settingsRef">
    <section class="settings-header">
      <div class="identity">
        <el-upload :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/*">
          <div class="avatar-wrap" :class="{ uploading: avatarUploading }">
            <el-avatar :size="72" :src="avatarSrc" class="avatar-clickable">
              {{ avatarInitial }}
            </el-avatar>
            <div class="avatar-overlay">
              <el-icon :size="20"><Camera /></el-icon>
            </div>
          </div>
        </el-upload>
        <div class="identity-copy">
          <h2>{{ profile.nickname || "未设置昵称" }}</h2>
          <p>{{ profile.email || "未绑定邮箱" }}</p>
          <div class="identity-tags">
            <el-tag size="small" type="success">{{ onlineLabel }}</el-tag>
            <el-tag size="small">{{ profile.gender ? genderLabel : "保密" }}</el-tag>
          </div>
        </div>
      </div>
      <div class="account-meta">
        <div class="meta-item">
          <span class="meta-label">账号 ID</span>
          <strong>{{ profile.id || "-" }}</strong>
        </div>
        <div class="meta-item">
          <span class="meta-label">注册时间</span>
          <strong>{{ createdAtText }}</strong>
        </div>
      </div>
    </section>

    <section class="settings-grid">
      <el-card class="settings-card profile-card">
        <template #header>
          <div class="card-title">
            <el-icon :size="18" color="var(--color-primary)"><User /></el-icon>
            <span>个人资料</span>
          </div>
        </template>
        <el-form label-width="86px" label-position="left">
          <el-form-item label="昵称">
            <el-input v-model="profile.nickname" maxlength="24" show-word-limit />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input :model-value="profile.email" disabled />
          </el-form-item>
          <el-form-item label="性别">
            <el-segmented v-model="profile.gender" :options="genderOptions" />
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker
              v-model="profile.birthday"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="个性签名">
            <el-input v-model="profile.bio" type="textarea" :rows="4" maxlength="200" show-word-limit resize="none" />
          </el-form-item>
          <el-form-item class="form-actions">
            <el-button type="primary" class="profile-save-btn" :loading="profileSaving" @click="saveProfile" round>
              保存资料
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="settings-card appearance-card">
        <template #header>
          <div class="card-title">
            <el-icon :size="18" color="var(--color-primary)"><Brush /></el-icon>
            <span>外观设置</span>
          </div>
        </template>
        <el-form label-width="104px" label-position="left">
          <el-form-item label="主题模式">
            <el-segmented v-model="themeValue" :options="themeOptions" @change="onThemeChange" />
          </el-form-item>
          <el-form-item label="侧边栏">
            <el-switch
              v-model="menuCollapsed"
              inline-prompt
              active-text="收起"
              inactive-text="展开"
              @change="onMenuCollapseChange"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="settings-card security-card">
        <template #header>
          <div class="card-title">
            <el-icon :size="18" color="var(--color-primary)"><Lock /></el-icon>
            <span>安全设置</span>
          </div>
        </template>
        <el-form label-width="86px" label-position="left">
          <el-form-item label="原密码">
            <el-input v-model="pwForm.oldPassword" type="password" show-password placeholder="输入原密码" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwForm.newPassword" type="password" show-password placeholder="至少 6 位" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="pwForm.confirmPassword" type="password" show-password placeholder="再次输入" />
          </el-form-item>
          <el-form-item class="form-actions">
            <el-button type="primary" class="password-save-btn" :loading="passwordSaving" @click="changePassword" round>
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="settings-card ai-card">
        <template #header>
          <div class="card-title">
            <el-icon :size="18" color="var(--color-primary)"><Setting /></el-icon>
            <span>AI 模型设置</span>
          </div>
        </template>
        <el-form label-width="96px" label-position="left">
          <el-form-item label="提供商">
            <el-select v-model="provider" style="width: 100%" @change="onProviderChange">
              <el-option v-for="p in providers" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型">
            <el-select v-model="model" style="width: 100%" filterable allow-create default-first-option>
              <el-option
                v-for="m in currentModels"
                :key="m.id"
                :label="`${m.name} - ${m.description}`"
                :value="m.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="API URL">
            <el-input v-model="apiUrl" placeholder="https://api.example.com/v1/chat/completions" clearable />
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
          <el-form-item class="form-actions">
            <el-button type="primary" class="ai-save-btn" @click="saveAi" round>保存设置</el-button>
            <el-button class="ai-test-btn" :loading="testingConnection" @click="testAiEndpoint" round>
              测试连接
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="settings-card embedding-card">
        <template #header>
          <div class="card-title">
            <el-icon :size="18" color="var(--color-primary)"><Connection /></el-icon>
            <span>RAG Embedding</span>
          </div>
        </template>
        <el-form label-width="96px" label-position="left">
          <el-form-item label="启用">
            <el-switch v-model="embeddingEnabled" />
          </el-form-item>
          <el-form-item label="接口链接">
            <el-input v-model="embeddingApiUrl" placeholder="https://api.example.com/v1/embeddings" clearable />
          </el-form-item>
          <el-form-item label="模型">
            <el-input v-model="embeddingModel" placeholder="text-embedding-3-small" clearable />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input v-model="embeddingApiKey" :type="showEmbeddingKey ? 'text' : 'password'" placeholder="输入 Embedding API Key">
              <template #append>
                <el-button @click="showEmbeddingKey = !showEmbeddingKey">
                  <el-icon><component :is="showEmbeddingKey ? 'Hide' : 'View'" /></el-icon>
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item class="form-actions">
            <el-button type="primary" class="embedding-save-btn" @click="saveEmbedding" round>保存 Embedding</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from "vue";
import { ElMessage } from "element-plus";
import { getProviders, healthCheck, resolveAssetUrl, testAiConnection } from "@/api/client";
import { useSettingsStore } from "@/stores/settings";
import { useAuthStore } from "@/stores/auth";
import { panelIn, softPulse, staggerIn } from "@/utils/motion";
import type { ProviderInfo } from "@/types";

const settingsStore = useSettingsStore();
const authStore = useAuthStore();
const settingsRef = ref<HTMLElement | null>(null);

const profile = reactive({
  id: authStore.user?.id || "",
  nickname: authStore.user?.nickname || "",
  email: authStore.user?.email || "",
  avatarUrl: authStore.user?.avatarUrl || "",
  bio: authStore.user?.bio || "",
  gender: authStore.user?.gender || "",
  birthday: authStore.user?.birthday || "",
  onlineStatus: authStore.user?.onlineStatus || "",
  createdAt: authStore.user?.createdAt || "",
});

const avatarUploading = ref(false);
const profileSaving = ref(false);
const passwordSaving = ref(false);
const testingConnection = ref(false);
const AVATAR_MAX_SIZE = 10 * 1024 * 1024;
const AVATAR_MAX_SIZE_LABEL = "10MB";

const avatarSrc = computed(() => resolveAssetUrl(profile.avatarUrl));
const avatarInitial = computed(() => (profile.nickname || "?").slice(0, 1));
const onlineLabel = computed(() => (profile.onlineStatus === "ONLINE" ? "在线" : "离线"));
const genderLabel = computed(() => {
  if (profile.gender === "male") return "男";
  if (profile.gender === "female") return "女";
  return "保密";
});
const createdAtText = computed(() => {
  if (!profile.createdAt) return "-";
  return new Date(profile.createdAt).toLocaleDateString("zh-CN");
});

const genderOptions = [
  { label: "保密", value: "" },
  { label: "男", value: "male" },
  { label: "女", value: "female" },
];

async function handleAvatarUpload(file: File) {
  if (!file.type.startsWith("image/")) {
    ElMessage.warning("请选择图片文件");
    return false;
  }
  if (file.size > AVATAR_MAX_SIZE) {
    ElMessage.warning(`头像不能超过 ${AVATAR_MAX_SIZE_LABEL}`);
    return false;
  }
  avatarUploading.value = true;
  try {
    const url = await authStore.uploadAvatar(file);
    profile.avatarUrl = url;
    await loadProfile();
    softPulse(settingsRef.value?.querySelector(".avatar-clickable"));
    ElMessage.success("头像已更新");
  } catch (e: any) {
    ElMessage.error(e.message || "头像上传失败");
  } finally {
    avatarUploading.value = false;
  }
  return false;
}

async function loadProfile() {
  const data = await authStore.fetchProfile();
  if (data) Object.assign(profile, data);
}

async function saveProfile() {
  if (!profile.nickname.trim()) {
    ElMessage.warning("昵称不能为空");
    return;
  }
  profileSaving.value = true;
  try {
    await authStore.updateProfile({
      nickname: profile.nickname.trim(),
      bio: profile.bio,
      gender: profile.gender,
      birthday: profile.birthday,
    });
    await loadProfile();
    softPulse(settingsRef.value?.querySelector(".profile-save-btn"));
    ElMessage.success("个人资料已保存");
  } catch (e: any) {
    ElMessage.error(e.message || "保存失败");
  } finally {
    profileSaving.value = false;
  }
}

const pwForm = reactive({ oldPassword: "", newPassword: "", confirmPassword: "" });

async function changePassword() {
  if (!pwForm.oldPassword || !pwForm.newPassword) {
    ElMessage.warning("请填写完整密码");
    return;
  }
  if (pwForm.newPassword.length < 6) {
    ElMessage.warning("新密码至少 6 位");
    return;
  }
  if (pwForm.newPassword !== pwForm.confirmPassword) {
    ElMessage.warning("两次密码不一致");
    return;
  }
  passwordSaving.value = true;
  try {
    await authStore.changePassword(pwForm.oldPassword, pwForm.newPassword);
    softPulse(settingsRef.value?.querySelector(".password-save-btn"));
    ElMessage.success("密码已修改");
    pwForm.oldPassword = "";
    pwForm.newPassword = "";
    pwForm.confirmPassword = "";
  } catch (e: any) {
    ElMessage.error(e.message || "修改失败");
  } finally {
    passwordSaving.value = false;
  }
}

const themeValue = ref(settingsStore.theme);
const themeOptions = [
  { label: "浅色", value: "light" },
  { label: "深色", value: "dark" },
];
const menuCollapsed = ref(settingsStore.menuCollapsed);

function onThemeChange(value: string | number | boolean) {
  settingsStore.setTheme(value === "dark" ? "dark" : "light");
  panelIn(settingsRef.value?.querySelector(".appearance-card"), { y: 6, scale: 0.998, duration: 260 });
}

function onMenuCollapseChange(value: string | number | boolean) {
  settingsStore.setMenuCollapsed(Boolean(value));
  softPulse(settingsRef.value?.querySelector(".appearance-card"));
}

const apiKey = ref(settingsStore.apiKey);
const provider = ref(settingsStore.selectedProvider);
const model = ref(settingsStore.selectedModel);
const apiUrl = ref(settingsStore.aiApiUrl);
const showKey = ref(false);
const embeddingEnabled = ref(settingsStore.embeddingEnabled);
const embeddingApiUrl = ref(settingsStore.embeddingApiUrl);
const embeddingApiKey = ref(settingsStore.embeddingApiKey);
const embeddingModel = ref(settingsStore.embeddingModel);
const showEmbeddingKey = ref(false);
const DEFAULT_PROVIDERS: ProviderInfo[] = [
  {
    id: "deepseek",
    name: "DeepSeek",
    models: [
      { id: "deepseek-v4-flash", name: "DeepSeek-V4 Flash", description: "官方推荐快速模型" },
      { id: "deepseek-v4-pro", name: "DeepSeek-V4 Pro", description: "官方推荐高能力模型" },
    ],
  },
  {
    id: "openai",
    name: "OpenAI",
    models: [
      { id: "gpt-5.5", name: "GPT-5.5", description: "OpenAI 旗舰通用模型" },
      { id: "gpt-5.4", name: "GPT-5.4", description: "高能力通用模型" },
      { id: "gpt-5.4-mini", name: "GPT-5.4 mini", description: "轻量高性价比模型" },
      { id: "gpt-5.4-nano", name: "GPT-5.4 nano", description: "低延迟小模型" },
    ],
  },
  {
    id: "anthropic",
    name: "Anthropic Claude",
    models: [
      { id: "claude-fable-5", name: "Claude Fable 5", description: "官方最新推理模型" },
      { id: "claude-opus-4-8", name: "Claude Opus 4.8", description: "复杂任务高能力模型" },
      { id: "claude-sonnet-4-6", name: "Claude Sonnet 4.6", description: "均衡通用模型" },
      { id: "claude-haiku-4-5", name: "Claude Haiku 4.5", description: "快速低成本模型" },
    ],
  },
  {
    id: "gemini",
    name: "Google Gemini",
    models: [
      { id: "gemini-3.5-flash", name: "Gemini 3.5 Flash", description: "Gemini 快速模型" },
      { id: "gemini-3.1-pro", name: "Gemini 3.1 Pro", description: "Gemini Pro 模型" },
      { id: "gemini-3-flash", name: "Gemini 3 Flash", description: "Gemini 低延迟模型" },
      { id: "gemini-2.5-pro", name: "Gemini 2.5 Pro", description: "Gemini 2.5 高能力模型" },
      { id: "gemini-2.5-flash", name: "Gemini 2.5 Flash", description: "Gemini 2.5 快速模型" },
      { id: "gemini-2.5-flash-lite", name: "Gemini 2.5 Flash-Lite", description: "Gemini 2.5 轻量模型" },
    ],
  },
  {
    id: "qwen",
    name: "阿里云百炼 / Qwen",
    models: [
      { id: "qwen3.7-max", name: "Qwen3.7 Max", description: "百炼最新旗舰模型" },
      { id: "qwen3.7-plus", name: "Qwen3.7 Plus", description: "高能力通用模型" },
      { id: "qwen3.6-plus", name: "Qwen3.6 Plus", description: "稳定通用模型" },
      { id: "qwen3.6-flash", name: "Qwen3.6 Flash", description: "快速低成本模型" },
      { id: "qwen3.5-plus", name: "Qwen3.5 Plus", description: "通用增强模型" },
      { id: "qwen3.5-flash", name: "Qwen3.5 Flash", description: "快速响应模型" },
      { id: "qwen-plus", name: "Qwen Plus", description: "百炼通用模型" },
      { id: "qwen-turbo", name: "Qwen Turbo", description: "百炼高吞吐模型" },
      { id: "qwen-long", name: "Qwen Long", description: "长上下文模型" },
      { id: "qwen3-coder-plus", name: "Qwen3 Coder Plus", description: "代码分析模型" },
      { id: "qwen3-coder-flash", name: "Qwen3 Coder Flash", description: "快速代码模型" },
      { id: "qwq-plus", name: "QwQ Plus", description: "推理模型" },
      { id: "mimo-v2.5-pro", name: "MiMo-V2.5 Pro", description: "百炼可调用的小米模型" },
      { id: "kimi-k2.6", name: "Kimi K2.6", description: "百炼可调用的 Kimi 模型" },
      { id: "glm-5.1", name: "GLM-5.1", description: "百炼可调用的智谱模型" },
    ],
  },
  {
    id: "xiaomi",
    name: "小米 MiMo",
    models: [
      { id: "MiMo-V2.5", name: "MiMo-V2.5", description: "小米主模型，通用对话" },
      { id: "mimo-v2.5-pro", name: "MiMo-V2.5 Pro", description: "专业版，能力更强" },
      { id: "MiMo-V2-Flash", name: "MiMo-V2 Flash", description: "轻量快速版" },
      { id: "MiMo-V2-Pro", name: "MiMo-V2 Pro (legacy)", description: "历史兼容模型" },
      { id: "MiMo-V2-Omni", name: "MiMo-V2 Omni (legacy)", description: "历史兼容多模态模型" },
    ],
  },
  {
    id: "zhipu",
    name: "智谱 GLM",
    models: [
      { id: "glm-5.1", name: "GLM-5.1", description: "智谱最新深度思考模型" },
      { id: "glm-5", name: "GLM-5", description: "智谱高能力通用模型" },
      { id: "glm-5-turbo", name: "GLM-5 Turbo", description: "快速模型" },
      { id: "glm-4.7", name: "GLM-4.7", description: "通用增强模型" },
      { id: "glm-4.7-flash", name: "GLM-4.7 Flash", description: "免费/快速模型" },
      { id: "glm-4.7-flashx", name: "GLM-4.7 FlashX", description: "高速模型" },
      { id: "glm-4.6", name: "GLM-4.6", description: "长上下文通用模型" },
      { id: "glm-4.5-air", name: "GLM-4.5 Air", description: "轻量通用模型" },
      { id: "glm-4.5-airx", name: "GLM-4.5 AirX", description: "增强轻量模型" },
      { id: "glm-4-long", name: "GLM-4 Long", description: "长上下文模型" },
    ],
  },
  {
    id: "moonshot",
    name: "Moonshot Kimi",
    models: [
      { id: "kimi-k2.6", name: "Kimi K2.6", description: "Moonshot 最新专家混合模型" },
      { id: "kimi-k2.5", name: "Kimi K2.5", description: "通用能力增强模型" },
      { id: "kimi-k2", name: "Kimi K2", description: "通用模型" },
      { id: "kimi-k2-thinking", name: "Kimi K2 Thinking", description: "深度思考模型" },
      { id: "moonshot-v1", name: "Moonshot V1", description: "经典稳定模型" },
    ],
  },
  {
    id: "xai",
    name: "xAI Grok",
    models: [
      { id: "grok-4.3", name: "Grok 4.3", description: "xAI 最新通用模型" },
      { id: "grok-4.3-latest", name: "Grok 4.3 Latest", description: "最新别名模型" },
      { id: "grok-4", name: "Grok 4", description: "上一代旗舰模型" },
      { id: "grok-4-fast", name: "Grok 4 Fast", description: "快速模型" },
      { id: "grok-code-fast", name: "Grok Code Fast", description: "代码快速模型" },
    ],
  },
  {
    id: "custom",
    name: "自定义厂商",
    apiUrl: "",
    apiFormat: "openai-chat",
    models: [
      { id: "custom-model", name: "自定义模型", description: "OpenAI-compatible relay model" },
    ],
  },
];
const DEFAULT_API_URLS: Record<string, string> = {
  deepseek: "https://api.deepseek.com/chat/completions",
  openai: "https://api.openai.com/v1/responses",
  anthropic: "https://api.anthropic.com/v1/messages",
  gemini: "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
  qwen: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
  xiaomi: "https://api.xiaomimimo.com/v1/chat/completions",
  zhipu: "https://open.bigmodel.cn/api/paas/v4/chat/completions",
  moonshot: "https://api.moonshot.cn/v1/chat/completions",
  xai: "https://api.x.ai/v1/chat/completions",
  custom: "",
};
const providers = ref<ProviderInfo[]>(DEFAULT_PROVIDERS);
const currentModels = computed(() => providers.value.find((p) => p.id === provider.value)?.models || []);
const currentProvider = computed(() => providers.value.find((p) => p.id === provider.value));

function providerDefaultApiUrl(item?: ProviderInfo) {
  return item?.apiUrl || DEFAULT_API_URLS[item?.id || ""] || "";
}

function onProviderChange(value: string) {
  const selected = providers.value.find((p) => p.id === value);
  if (selected?.models?.length) model.value = selected.models[0].id;
  apiUrl.value = providerDefaultApiUrl(selected);
}

function saveAi() {
  if (provider.value === "custom" && !apiUrl.value.trim()) {
    ElMessage.warning("请填写自定义接口链接");
    return;
  }
  if (provider.value === "custom" && !model.value.trim()) {
    ElMessage.warning("请填写自定义模型 ID");
    return;
  }
  settingsStore.setApiKey(apiKey.value);
  settingsStore.setProvider(provider.value);
  settingsStore.setModel(model.value.trim());
  settingsStore.setAiApiUrl(apiUrl.value.trim());
  softPulse(settingsRef.value?.querySelector(".ai-save-btn"));
  ElMessage.success("设置已保存");
}

function saveEmbedding() {
  if (embeddingEnabled.value && (!embeddingApiUrl.value.trim() || !embeddingApiKey.value.trim() || !embeddingModel.value.trim())) {
    ElMessage.warning("启用 Embedding 时需要填写接口链接、API Key 和模型");
    return;
  }
  settingsStore.setEmbeddingEnabled(Boolean(embeddingEnabled.value));
  settingsStore.setEmbeddingApiUrl(embeddingApiUrl.value.trim());
  settingsStore.setEmbeddingApiKey(embeddingApiKey.value.trim());
  settingsStore.setEmbeddingModel(embeddingModel.value.trim() || "text-embedding-3-small");
  softPulse(settingsRef.value?.querySelector(".embedding-save-btn"));
  ElMessage.success("Embedding 设置已保存");
}

function ensureProviderSelection() {
  if (!providers.value.length) return;
  let selected = providers.value.find((p) => p.id === provider.value);
  if (!selected) {
    selected = providers.value[0];
    provider.value = selected.id;
  }
  if (!selected.models.some((item) => item.id === model.value) && selected.models.length) {
    model.value = selected.models[0].id;
  }
  if (!apiUrl.value.trim()) {
    apiUrl.value = providerDefaultApiUrl(currentProvider.value);
  }
}

async function loadProviders() {
  try {
    const remoteProviders = await getProviders();
    providers.value = Array.isArray(remoteProviders) && remoteProviders.length ? remoteProviders : DEFAULT_PROVIDERS;
  } catch {
    providers.value = DEFAULT_PROVIDERS;
  }
  ensureProviderSelection();
}

async function testAiEndpoint() {
  testingConnection.value = true;
  try {
    if (apiKey.value.trim()) {
      await testAiConnection(apiKey.value.trim(), provider.value, model.value.trim(), apiUrl.value.trim());
      softPulse(settingsRef.value?.querySelector(".ai-test-btn"));
      ElMessage.success("AI 接口连接正常");
      return;
    }
    const ok = await healthCheck();
    softPulse(settingsRef.value?.querySelector(".ai-test-btn"));
    ElMessage[ok ? "success" : "error"](ok ? "服务器连接正常" : "无法连接服务器");
  } catch (e: any) {
    ElMessage.error(e.message || "AI 接口连接失败");
  } finally {
    testingConnection.value = false;
  }
}

async function testConnection() {
  testingConnection.value = true;
  try {
    const ok = await healthCheck();
    softPulse(settingsRef.value?.querySelector(".ai-test-btn"));
    ElMessage[ok ? "success" : "error"](ok ? "服务器连接正常" : "无法连接服务器");
  } finally {
    testingConnection.value = false;
  }
}

onMounted(async () => {
  animateSettingsPage();
  await loadProviders();
  try {
    await loadProfile();
    panelIn(settingsRef.value?.querySelector(".profile-card"), { y: 6, duration: 260 });
  } catch {}
});

function animateSettingsPage() {
  nextTick(() => {
    staggerIn(settingsRef.value?.querySelectorAll(".settings-card, .settings-header"), {
      y: 18,
      scale: 0.98,
      duration: 420,
      staggerDelay: 70,
    });
    staggerIn(settingsRef.value?.querySelectorAll(".el-form-item, .meta-item"), {
      x: -8,
      y: 0,
      scale: 1,
      duration: 280,
      delay: 140,
      staggerDelay: 24,
    });
  });
}
</script>

<style scoped>
.settings-page {
  width: min(1180px, 100%);
  min-height: calc(100vh - 112px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 22px 24px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  will-change: transform, opacity;
}

.identity {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.avatar-wrap {
  position: relative;
  width: 72px;
  height: 72px;
}

.avatar-wrap.uploading {
  opacity: 0.7;
  pointer-events: none;
}

.avatar-clickable {
  cursor: pointer;
  transition: box-shadow 0.25s var(--transition-smooth), transform 0.25s var(--transition-smooth);
}

.avatar-clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(74, 222, 128, 0.18);
}

.avatar-overlay {
  position: absolute;
  right: -3px;
  bottom: -3px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(15, 23, 42, 0.72);
  border-radius: 50%;
  border: 2px solid var(--color-surface);
  pointer-events: none;
  transition: transform 0.25s var(--transition-smooth), background 0.25s var(--transition-smooth);
}

.avatar-wrap:hover .avatar-overlay {
  transform: translateY(-2px);
  background: rgba(34, 197, 94, 0.86);
}

.identity-copy {
  min-width: 0;
}

.identity-copy h2 {
  margin: 0 0 4px;
  color: var(--color-text);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0;
}

.identity-copy p {
  margin: 0 0 10px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.identity-tags {
  display: flex;
  gap: 8px;
}

.account-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 12px;
}

.meta-item {
  min-width: 126px;
  padding: 12px 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.meta-label {
  display: block;
  margin-bottom: 6px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.meta-item strong {
  display: block;
  color: var(--color-text);
  font-size: 14px;
  font-weight: 600;
}

.settings-grid {
  display: grid;
  grid-template-columns: minmax(420px, 1.15fr) minmax(360px, 0.85fr);
  grid-template-areas:
    "profile appearance"
    "profile security"
    "ai ai"
    "embedding embedding";
  gap: 16px;
  align-items: stretch;
}

.profile-card {
  grid-area: profile;
}

.appearance-card {
  grid-area: appearance;
}

.security-card {
  grid-area: security;
}

.ai-card {
  grid-area: ai;
}

.embedding-card {
  grid-area: embedding;
}

.settings-card {
  height: 100%;
  will-change: transform, opacity;
  transition: box-shadow 0.25s var(--transition-smooth), transform 0.25s var(--transition-smooth);
}

.settings-card:hover {
  transform: translateY(-2px);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text);
  font-weight: 600;
  font-size: 15px;
}

.form-actions :deep(.el-form-item__content) {
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .settings-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .account-meta {
    width: 100%;
  }

  .settings-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "profile"
      "appearance"
      "security"
      "ai"
      "embedding";
  }
}

@media (max-width: 560px) {
  .identity {
    align-items: flex-start;
  }

  .account-meta {
    grid-template-columns: 1fr;
  }
}
</style>
