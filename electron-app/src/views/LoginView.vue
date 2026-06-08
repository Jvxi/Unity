<template>
  <div class="auth-page">
    <div class="bg-pattern"></div>
    <div class="brand-panel">
      <div class="brand-content">
        <div class="brand-logo">
          <img src="@/assets/cat-logo.png" alt="猫爪工具" />
        </div>
        <h1 class="brand-title">猫爪工具</h1>
        <p class="brand-desc">专业的二进制文件分析助手，集 PE 解析、虚表检测、AI 智能分析于一体</p>
        <div class="feature-cards">
          <div class="feat-card">
            <div class="feat-icon" style="background:rgba(74,222,128,0.1);color:#16a34a">
              <el-icon :size="20"><Search /></el-icon>
            </div>
            <div class="feat-info">
              <h3>PE 深度解析</h3>
              <p>全面解析 PE 结构，提取虚表地址</p>
            </div>
          </div>
          <div class="feat-card">
            <div class="feat-icon" style="background:rgba(6,182,212,0.1);color:#0891b2">
              <el-icon :size="20"><MagicStick /></el-icon>
            </div>
            <div class="feat-info">
              <h3>AI 辅助分析</h3>
              <p>大模型智能确认虚表，排除误报</p>
            </div>
          </div>
          <div class="feat-card">
            <div class="feat-icon" style="background:rgba(139,92,246,0.1);color:#7c3aed">
              <el-icon :size="20"><Document /></el-icon>
            </div>
            <div class="feat-info">
              <h3>报告导出</h3>
              <p>一键导出 JSON / HTML 分析报告</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="form-panel">
      <div class="form-wrapper">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>登录您的账号以继续使用</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
          <el-form-item label="昵称或邮箱" prop="login">
            <el-input v-model="form.login" placeholder="请输入昵称或邮箱" size="large" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleLogin">
              登录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="auth-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ login: '', password: '' })
const rules = {
  login: [{ required: true, message: '请输入昵称或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    await authStore.login(form.login, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  background: #f0f2f5;
  position: relative;
}

.bg-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(74,222,128,0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(56,189,248,0.06) 0%, transparent 40%),
    radial-gradient(circle at 60% 80%, rgba(139,92,246,0.05) 0%, transparent 40%);
  pointer-events: none;
  z-index: 0;
}

.brand-panel {
  flex: 0 0 45%;
  background: linear-gradient(160deg, #e8f5e9 0%, #f1f8f2 40%, #e0f2f1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 48px;
  position: relative;
  overflow: hidden;
  border-right: 1px solid rgba(0,0,0,0.05);
  z-index: 1;
}
.brand-panel::before {
  content: '';
  position: absolute;
  top: -120px; right: -120px;
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(74,222,128,0.15) 0%, transparent 70%);
  pointer-events: none;
}
.brand-panel::after {
  content: '';
  position: absolute;
  bottom: -100px; left: -80px;
  width: 350px; height: 350px;
  background: radial-gradient(circle, rgba(56,189,248,0.1) 0%, transparent 70%);
  pointer-events: none;
}

.brand-content {
  max-width: 400px;
  position: relative;
  z-index: 1;
  animation: fadeUp 0.7s cubic-bezier(0.16,1,0.3,1) both;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.brand-logo {
  width: 64px; height: 64px;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.brand-logo img { width: 100%; height: 100%; object-fit: cover; }

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 10px;
  letter-spacing: -0.5px;
}
.brand-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
  margin-bottom: 36px;
}

.feature-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.feat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(255,255,255,0.6);
  border: 1px solid rgba(0,0,0,0.06);
  border-radius: 12px;
  transition: all 0.3s;
}
.feat-card:hover {
  background: rgba(255,255,255,0.85);
  border-color: rgba(0,0,0,0.1);
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}
.feat-icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.feat-info h3 {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
}
.feat-info p {
  font-size: 12px;
  color: #94a3b8;
}

.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 48px;
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  z-index: 1;
}
.form-wrapper {
  width: 100%;
  max-width: 400px;
  animation: fadeUp 0.7s 0.15s cubic-bezier(0.16,1,0.3,1) both;
}
.form-header {
  margin-bottom: 32px;
}
.form-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 6px;
}
.form-header p {
  font-size: 14px;
  color: #94a3b8;
}

.auth-form :deep(.el-form-item__label) {
  color: #475569 !important;
  font-size: 13px;
  font-weight: 500;
}
.auth-form :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.6) !important;
  border: 1px solid rgba(0,0,0,0.08) !important;
  box-shadow: none !important;
  border-radius: 10px !important;
  height: 44px;
  backdrop-filter: blur(8px);
  transition: border-color 0.25s, background 0.25s;
}
.auth-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(0,0,0,0.12) !important;
}
.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: #4ade80 !important;
  background: rgba(255,255,255,0.8) !important;
}
.auth-form :deep(.el-input__inner) { color: #1e293b; }
.auth-form :deep(.el-input__inner::placeholder) { color: #94a3b8; }
.auth-form :deep(.el-input__prefix .el-icon) { color: #94a3b8; }

.auth-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px !important;
  background: linear-gradient(135deg, #4ade80, #22c55e) !important;
  border: none !important;
  color: #052e16 !important;
  letter-spacing: 0.5px;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(74,222,128,0.25);
}
.auth-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(74,222,128,0.35) !important;
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  color: #94a3b8;
  font-size: 13px;
}
.auth-footer a {
  color: #16a34a;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 500;
}
.auth-footer a:hover { color: #22c55e; }

@media (max-width: 900px) {
  .brand-panel { display: none; }
  .form-panel { padding: 40px 24px; }
}
</style>
