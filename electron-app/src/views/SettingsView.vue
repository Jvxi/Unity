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
            <el-select v-model="model" style="width: 100%">
              <el-option
                v-for="m in currentModels"
                :key="m.id"
                :label="`${m.name} - ${m.description}`"
                :value="m.id"
              />
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
          <el-form-item class="form-actions">
            <el-button type="primary" class="ai-save-btn" @click="saveAi" round>保存设置</el-button>
            <el-button class="ai-test-btn" :loading="testingConnection" @click="testConnection" round>
              测试连接
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from "vue";
import { ElMessage } from "element-plus";
import { getProviders, healthCheck, resolveAssetUrl } from "@/api/client";
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
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning("头像不能超过 2MB");
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
const showKey = ref(false);
const providers = ref<ProviderInfo[]>([]);
const currentModels = computed(() => providers.value.find((p) => p.id === provider.value)?.models || []);

function onProviderChange(value: string) {
  const selected = providers.value.find((p) => p.id === value);
  if (selected?.models?.length) model.value = selected.models[0].id;
}

function saveAi() {
  settingsStore.setApiKey(apiKey.value);
  settingsStore.setProvider(provider.value);
  settingsStore.setModel(model.value);
  softPulse(settingsRef.value?.querySelector(".ai-save-btn"));
  ElMessage.success("设置已保存");
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
  try {
    providers.value = await getProviders();
    if (!currentModels.value.some((item) => item.id === model.value) && currentModels.value.length) {
      model.value = currentModels.value[0].id;
    }
  } catch {
    providers.value = [
      { id: "deepseek", name: "DeepSeek", models: [{ id: "deepseek-v4-flash", name: "DeepSeek-V4 Flash", description: "轻量快速" }, { id: "deepseek-v4-pro", name: "DeepSeek-V4 Pro", description: "专业模型" }] },
      { id: "xiaomi", name: "小米 MiMo", models: [{ id: "MiMo-V2.5", name: "MiMo-V2.5", description: "小米模型" }] },
    ];
  }
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
    "ai ai";
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
      "ai";
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
