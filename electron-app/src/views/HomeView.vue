<template>
  <div class="home">
    <div class="hero-card">
      <div class="hero-content">
        <div class="hero-icon">
          <el-icon :size="36"><Cpu /></el-icon>
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
          <el-icon :size="22" :color="f.color"><component :is="f.icon" /></el-icon>
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
  { icon: "Document", color: "#22c55e", bg: "rgba(34,197,94,0.1)", title: "PE 全面解析", desc: "解析 PE 头、段表、导出导入表、调试信息、TLS 等全部结构" },
  { icon: "Search", color: "#f59e0b", bg: "rgba(245,158,11,0.1)", title: "多策略虚表检测", desc: "RTTI 引导、连续指针扫描、导出交叉引用三种策略互补" },
  { icon: "MagicStick", color: "#6b7280", bg: "rgba(107,114,128,0.1)", title: "AI 智能分析", desc: "DeepSeek / 小米 MiMo 大模型辅助确认虚表、排除误报" },
];

onMounted(() => {
  const hero = document.querySelector('.hero-card');
  if (hero) {
    animate(hero, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 16 }, { to: 0 }],
      duration: 450,
      ease: 'outElastic(1, .8)',
    });
  }
  const items = document.querySelectorAll('.feature-item');
  if (items.length > 0) {
    animate(items, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 24 }, { to: 0 }],
      scale: [{ from: 0.96 }, { to: 1 }],
      duration: 450,
      delay: stagger(90, { start: 180 }),
      ease: 'outElastic(1, .8)',
    });
  }
});
</script>

<style scoped>
.home { max-width: 860px; margin: 0 auto; }

.hero-card {
  background: linear-gradient(135deg, #1e1f24 0%, #2a2b32 100%);
  border-radius: var(--radius-xl);
  padding: 32px 36px;
  margin-bottom: 24px;
  color: #fff;
  opacity: 0;
  position: relative;
  overflow: hidden;
}
.hero-card::after {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(74,222,128,0.08) 0%, transparent 70%);
  pointer-events: none;
}
.hero-content {
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
  z-index: 1;
}
.hero-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  background: rgba(74,222,128,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  flex-shrink: 0;
}
.hero-text h1 {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: -0.4px;
}
.hero-text p {
  font-size: 13px;
  opacity: 0.7;
  line-height: 1.6;
}
.start-btn {
  margin-left: auto;
  flex-shrink: 0;
  border-radius: var(--radius-md) !important;
  background: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  color: #052e16 !important;
  font-weight: 600;
  transition: all 0.3s var(--transition-smooth);
}
.start-btn:hover {
  background: var(--color-primary-dark) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(74,222,128,0.3);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.feature-item {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 24px 20px;
  text-align: center;
  opacity: 0;
  transition: all 0.3s var(--transition-smooth);
  cursor: default;
}
.feature-item:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
}
.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}
.feature-item h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
}
.feature-item p {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>