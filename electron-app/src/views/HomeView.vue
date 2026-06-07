<template>
  <div class="home">
    <div class="hero-card">
      <div class="hero-content">
        <div class="hero-icon">
          <el-icon :size="40"><Cpu /></el-icon>
        </div>
        <div class="hero-text">
          <h1>DLL VTable Analyzer</h1>
          <p>上传 DLL 文件，通过 PE 结构解析和 AI 分析，反向出其内部的虚表地址信息</p>
        </div>
        <el-button type="primary" size="large" class="start-btn" @click="$router.push('/analysis')">
          <el-icon><Search /></el-icon>
          开始分析
        </el-button>
      </div>
    </div>

    <div class="feature-grid">
      <div class="feature-item" v-for="(f, i) in features" :key="i">
        <div class="feature-icon" :style="{ background: f.bg }">
          <el-icon :size="24" :color="f.color"><component :is="f.icon" /></el-icon>
        </div>
        <h3>{{ f.title }}</h3>
        <p>{{ f.desc }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { animate, stagger } from "animejs";

const features = [
  { icon: "Document", color: "#10b981", bg: "rgba(16,185,129,0.1)", title: "PE 全面解析", desc: "解析 PE 头、段表、导出/导入表、调试信息、TLS 等全部结构" },
  { icon: "Search", color: "#f59e0b", bg: "rgba(245,158,11,0.1)", title: "多策略虚表检测", desc: "RTTI 引导、连续指针扫描、导出交叉引用三种策略互补" },
  { icon: "MagicStick", color: "#6366f1", bg: "rgba(99,102,241,0.1)", title: "AI 智能分析", desc: "DeepSeek / 小米 MiMo 大模型辅助确认虚表、排除误报" },
];

onMounted(() => {
  const hero = document.querySelector('.hero-card');
  if (hero) {
    animate(hero, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 20 }, { to: 0 }],
      scale: [{ from: 0.98 }, { to: 1 }],
      duration: 500,
      ease: 'outElastic(1, .75)',
    });
  }
  const items = document.querySelectorAll('.feature-item');
  if (items.length > 0) {
    animate(items, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 30 }, { to: 0 }],
      scale: [{ from: 0.95 }, { to: 1 }],
      duration: 500,
      delay: stagger(100, { start: 200 }),
      ease: 'outElastic(1, .75)',
    });
  }
});
</script>

<style scoped>
.home { max-width: 860px; margin: 0 auto; }

.hero-card {
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
  border-radius: var(--radius-xl);
  padding: 36px 40px;
  margin-bottom: 28px;
  color: #fff;
  opacity: 0;
}
.hero-content {
  display: flex;
  align-items: center;
  gap: 24px;
}
.hero-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  backdrop-filter: blur(8px);
}
.hero-text h1 {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
  letter-spacing: -0.5px;
}
.hero-text p {
  font-size: 14px;
  opacity: 0.85;
  line-height: 1.6;
}
.start-btn {
  margin-left: auto;
  flex-shrink: 0;
  border-radius: var(--radius-md) !important;
  background: rgba(255,255,255,0.2) !important;
  border: 1px solid rgba(255,255,255,0.3) !important;
  backdrop-filter: blur(8px);
  transition: all 0.3s var(--transition-smooth);
}
.start-btn:hover {
  background: rgba(255,255,255,0.35) !important;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.2);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.feature-item {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 28px 24px;
  text-align: center;
  opacity: 0;
  transition: all 0.3s var(--transition-smooth);
  cursor: default;
}
.feature-item:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
}
.feature-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}
.feature-item h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}
.feature-item p {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>