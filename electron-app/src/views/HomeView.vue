<template>
  <div class="home">
    <el-card class="welcome-card" ref="welcomeCardRef">
      <template #header>
        <div class="card-header">
          <el-icon :size="32" color="#409eff"><Cpu /></el-icon>
          <span>DLL VTable Analyzer</span>
        </div>
      </template>
      <p class="desc">上传 DLL 文件，通过 PE 结构解析和 AI 分析，反向出其内部的虚表（vtable）地址信息。</p>
      <el-button type="primary" size="large" @click="$router.push('/analysis')" class="start-btn">
        <el-icon><Search /></el-icon>
        开始分析
      </el-button>
    </el-card>

    <el-row :gutter="20" class="features">
      <el-col :span="8">
        <el-card class="feature-card" ref="card1Ref">
          <el-icon :size="28" color="#67c23a"><Document /></el-icon>
          <h3>PE 全面解析</h3>
          <p>解析 PE 头、段表、导出/导入表、调试信息、TLS 等全部结构</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="feature-card" ref="card2Ref">
          <el-icon :size="28" color="#e6a23c"><Search /></el-icon>
          <h3>多策略虚表检测</h3>
          <p>RTTI 引导、连续指针扫描、导出交叉引用三种策略互补</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="feature-card" ref="card3Ref">
          <el-icon :size="28" color="#f56c6c"><MagicStick /></el-icon>
          <h3>AI 智能分析</h3>
          <p>DeepSeek / 小米 MiMo 大模型辅助确认虚表、排除误报、推测类名</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { animate, stagger } from "animejs";

const welcomeCardRef = ref<HTMLElement | null>(null);
const card1Ref = ref<HTMLElement | null>(null);
const card2Ref = ref<HTMLElement | null>(null);
const card3Ref = ref<HTMLElement | null>(null);

onMounted(() => {
  // 欢迎卡片入场动画
  const welcomeCard = document.querySelector('.welcome-card');
  if (welcomeCard) {
    animate(welcomeCard, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 40 }, { to: 0 }],
      scale: [{ from: 0.95 }, { to: 1 }],
      duration: 600,
      ease: 'outElastic(1, .6)',
    });
  }

  // 功能卡片交错入场
  const featureCards = document.querySelectorAll('.feature-card');
  if (featureCards.length > 0) {
    animate(featureCards, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: 50 }, { to: 0 }],
      scale: [{ from: 0.9 }, { to: 1 }],
      duration: 600,
      delay: stagger(150, { start: 300 }),
      ease: 'outElastic(1, .6)',
    });
  }

  // 开始按钮脉冲动画
  const startBtn = document.querySelector('.start-btn');
  if (startBtn) {
    animate(startBtn, {
      scale: [{ from: 1 }, { to: 1.05 }, { to: 1 }],
      duration: 2000,
      loop: true,
      ease: 'inOutQuad',
    });
  }

  // 卡片悬停动画
  featureCards.forEach(card => {
    card.addEventListener('mouseenter', () => {
      animate(card, {
        translateY: -8,
        boxShadow: '0 12px 24px rgba(0,0,0,0.15)',
        duration: 300,
        ease: 'outElastic(1, .8)',
      });
    });
    card.addEventListener('mouseleave', () => {
      animate(card, {
        translateY: 0,
        boxShadow: '0 2px 12px rgba(0,0,0,0.1)',
        duration: 300,
        ease: 'outElastic(1, .8)',
      });
    });
  });
});
</script>

<style scoped>
.home { max-width: 900px; margin: 0 auto; }
.welcome-card {
  margin-bottom: 24px;
  opacity: 0;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: bold;
}
.desc {
  color: #606266;
  font-size: 15px;
  margin-bottom: 20px;
  line-height: 1.8;
}
.start-btn {
  transition: all 0.3s ease;
}
.start-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}
.features .el-card {
  text-align: center;
  padding: 20px;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}
.features .el-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.15);
}
.features h3 { margin: 12px 0 8px; font-size: 16px; }
.features p { color: #909399; font-size: 13px; }
</style>