<template>
  <div class="auth-page">
    <div class="bg-pattern"></div>
    <div class="brand-panel">
      <div class="brand-content">
        <div class="brand-logo">
          <img src="@/assets/cat-logo.png" alt="猫爪工具" />
        </div>
        <h1 class="brand-title">猫爪工具</h1>
        <p class="brand-desc">加入我们，开始使用专业的二进制分析工具链</p>
        <div class="steps-cards">
          <div class="step-card">
            <div class="step-num">1</div>
            <div class="step-info">
              <h3>创建账号</h3>
              <p>填写基本信息完成注册</p>
            </div>
          </div>
          <div class="step-card">
            <div class="step-num">2</div>
            <div class="step-info">
              <h3>上传文件</h3>
              <p>支持 DLL / EXE 等 PE 文件</p>
            </div>
          </div>
          <div class="step-card">
            <div class="step-num">3</div>
            <div class="step-info">
              <h3>获取分析</h3>
              <p>AI 辅助生成完整分析报告</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="form-panel">
      <div class="form-wrapper">
        <div class="form-header">
          <h2>创建账号</h2>
          <p>注册后即可使用全部分析功能</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
          <div class="form-row">
            <el-form-item label="昵称" prop="nickname" class="form-col">
              <el-input v-model="form.nickname" placeholder="请输入昵称" size="large" prefix-icon="User" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email" class="form-col">
              <el-input v-model="form.email" placeholder="请输入邮箱" size="large" prefix-icon="Message" />
            </el-form-item>
          </div>
          <el-form-item label="验证码" prop="code">
            <div class="code-input">
              <el-input v-model="form.code" placeholder="请输入验证码" size="large" prefix-icon="Key" />
              <el-button size="large" :disabled="codeCooldown > 0" @click="sendCode" class="code-btn">
                {{ codeCooldown > 0 ? codeCooldown + 's' : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <div class="form-row">
            <el-form-item label="密码" prop="password" class="form-col">
              <el-input v-model="form.password" type="password" placeholder="至少6位" size="large" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword" class="form-col">
              <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" size="large" prefix-icon="Lock" show-password />
            </el-form-item>
          </div>
          <el-form-item>
            <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleRegister">
              注册
            </el-button>
          </el-form-item>
        </el-form>
        <div class="auth-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
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
const codeCooldown = ref(0)

const form = reactive({
  nickname: '', email: '', code: '', password: '', confirmPassword: ''
})

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== form.password) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const sendCode = async () => {
  try {
    await authStore.sendCode(form.email, 'register')
    ElMessage.success('验证码已发送')
    codeCooldown.value = 60
    const timer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  }
}

const handleRegister = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    await authStore.register(form.nickname, form.email, form.password, form.code)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (error: any) {
    ElMessage.error(error.message || '注册失败')
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
  flex: 0 0 40%;
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
  max-width: 380px;
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

.steps-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.step-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(255,255,255,0.6);
  border: 1px solid rgba(0,0,0,0.06);
  border-radius: 12px;
  transition: all 0.3s;
}
.step-card:hover {
  background: rgba(255,255,255,0.85);
  border-color: rgba(0,0,0,0.1);
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}
.step-num {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: rgba(74,222,128,0.12);
  color: #16a34a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  flex-shrink: 0;
}
.step-info h3 {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
}
.step-info p {
  font-size: 12px;
  color: #94a3b8;
}

.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 48px;
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  z-index: 1;
}
.form-wrapper {
  width: 100%;
  max-width: 520px;
  animation: fadeUp 0.7s 0.15s cubic-bezier(0.16,1,0.3,1) both;
}
.form-header {
  margin-bottom: 28px;
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

.form-row {
  display: flex;
  gap: 16px;
}
.form-col { flex: 1; min-width: 0; }

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

.code-input {
  display: flex;
  gap: 10px;
  width: 100%;
}
.code-input .el-input { flex: 1; }
.code-btn {
  flex-shrink: 0;
  border-radius: 10px !important;
  background: rgba(240,253,244,0.7) !important;
  border: 1px solid rgba(187,247,208,0.6) !important;
  color: #16a34a !important;
  font-weight: 500;
  backdrop-filter: blur(8px);
  transition: all 0.25s;
}
.code-btn:hover:not(:disabled) {
  background: rgba(220,252,231,0.8) !important;
  border-color: rgba(134,239,172,0.8) !important;
}
.code-btn:disabled {
  color: #94a3b8 !important;
  background: rgba(248,250,252,0.6) !important;
  border-color: rgba(226,232,240,0.6) !important;
}

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
