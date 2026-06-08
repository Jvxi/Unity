<template>
  <div class="auth-container">
    <div class="bg-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>
    <div class="auth-card">
      <div class="card-glow"></div>
      <div class="auth-header">
        <div class="logo-wrapper">
          <img src="@/assets/cat-logo.png" alt="猫爪工具" class="auth-logo" />
          <div class="logo-ring"></div>
        </div>
        <h1>猫爪工具</h1>
        <p class="subtitle">创建新账号</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" size="large" prefix-icon="User" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large" prefix-icon="Message" />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div class="code-input">
            <el-input v-model="form.code" placeholder="请输入验证码" size="large" prefix-icon="Key" />
            <el-button size="large" :disabled="codeCooldown > 0" @click="sendCode" class="code-btn">
              {{ codeCooldown > 0 ? `${codeCooldown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" prefix-icon="Lock" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" size="large" prefix-icon="Lock" show-password />
        </el-form-item>

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
    <div class="brand-footer">
      <span>Cat Paw Tool v0.1</span>
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
  nickname: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
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
        if (value !== form.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
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
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0f1117;
  position: relative;
  overflow: hidden;
}

.bg-orbs { position: absolute; inset: 0; pointer-events: none; }
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}
.orb-1 {
  width: 400px; height: 400px;
  background: #4ade80;
  top: -10%; left: -5%;
  animation: float1 12s ease-in-out infinite;
}
.orb-2 {
  width: 300px; height: 300px;
  background: #22d3ee;
  bottom: -8%; right: -3%;
  animation: float2 15s ease-in-out infinite;
}
.orb-3 {
  width: 250px; height: 250px;
  background: #a78bfa;
  top: 50%; left: 60%;
  animation: float3 18s ease-in-out infinite;
}
@keyframes float1 { 0%,100% { transform: translate(0,0); } 50% { transform: translate(60px,40px); } }
@keyframes float2 { 0%,100% { transform: translate(0,0); } 50% { transform: translate(-50px,-30px); } }
@keyframes float3 { 0%,100% { transform: translate(0,0); } 50% { transform: translate(-40px,50px); } }

.auth-card {
  position: relative;
  background: rgba(255,255,255,0.03);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 20px;
  padding: 40px 40px 32px;
  width: 440px;
  box-shadow: 0 24px 80px rgba(0,0,0,0.4);
  z-index: 1;
  animation: cardIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.card-glow {
  position: absolute;
  top: -1px; left: 50%; transform: translateX(-50%);
  width: 60%; height: 2px;
  background: linear-gradient(90deg, transparent, #4ade80, transparent);
  border-radius: 2px;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}
.logo-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 14px;
}
.auth-logo {
  width: 68px; height: 68px;
  border-radius: 16px;
  object-fit: cover;
  position: relative;
  z-index: 1;
}
.logo-ring {
  position: absolute;
  inset: -4px;
  border-radius: 20px;
  background: linear-gradient(135deg, #4ade80, #22d3ee);
  opacity: 0.5;
  filter: blur(8px);
  z-index: 0;
  animation: pulse 3s ease-in-out infinite;
}
@keyframes pulse { 0%,100% { opacity: 0.3; } 50% { opacity: 0.6; } }

.auth-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: #f1f5f9;
  margin-bottom: 4px;
  letter-spacing: -0.3px;
}
.subtitle {
  color: #64748b;
  font-size: 13px;
  font-weight: 400;
}

.auth-form { margin-bottom: 16px; }
.auth-form :deep(.el-form-item__label) {
  color: #94a3b8 !important;
  font-size: 13px;
  font-weight: 500;
}
.auth-form :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.04) !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
  box-shadow: none !important;
  border-radius: 10px !important;
  height: 44px;
  transition: border-color 0.25s;
}
.auth-form :deep(.el-input__wrapper:hover),
.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(74,222,128,0.4) !important;
}
.auth-form :deep(.el-input__inner) {
  color: #e2e8f0;
}
.auth-form :deep(.el-input__inner::placeholder) {
  color: #475569;
}
.auth-form :deep(.el-input__prefix .el-icon) {
  color: #64748b;
}

.code-input {
  display: flex;
  gap: 10px;
  width: 100%;
}
.code-input .el-input { flex: 1; }
.code-btn {
  flex-shrink: 0;
  border-radius: 10px !important;
  background: rgba(255,255,255,0.06) !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
  color: #4ade80 !important;
  font-weight: 500;
  transition: all 0.25s;
}
.code-btn:hover:not(:disabled) {
  background: rgba(74,222,128,0.12) !important;
  border-color: rgba(74,222,128,0.3) !important;
}
.code-btn:disabled {
  color: #475569 !important;
  border-color: rgba(255,255,255,0.05) !important;
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
  box-shadow: 0 4px 20px rgba(74,222,128,0.25);
}
.auth-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 28px rgba(74,222,128,0.35) !important;
}
.auth-btn:active {
  transform: translateY(0);
}

.auth-footer {
  text-align: center;
  color: #64748b;
  font-size: 13px;
}
.auth-footer a {
  color: #4ade80;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 500;
  transition: color 0.2s;
}
.auth-footer a:hover {
  color: #86efac;
}

.brand-footer {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  color: #334155;
  font-size: 11px;
  letter-spacing: 0.5px;
}
</style>