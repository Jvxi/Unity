<template>
  <div class="home">
    <section class="workspace-hero">
      <div class="hero-copy">
        <div class="eyebrow">Cat Paw Tool</div>
        <h1>猫爪工具控制台</h1>
        <p>把二进制分析、小说创作、聊天协作和本地 AI 设置放在同一个工作入口里。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="go('/analysis')">
          <el-icon><Cpu /></el-icon>
          二进制工作台
        </el-button>
        <el-button size="large" @click="go('/novels')">
          <el-icon><EditPen /></el-icon>
          小说助手
        </el-button>
      </div>
    </section>

    <section class="status-grid">
      <div v-for="item in statusItems" :key="item.label" class="status-item">
        <span class="status-value">{{ item.value }}</span>
        <span class="status-label">{{ item.label }}</span>
      </div>
    </section>

    <section class="module-grid">
      <button v-for="item in modules" :key="item.path" class="module-tile" type="button" @click="go(item.path)">
        <span class="module-icon" :style="{ background: item.bg, color: item.color }">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </span>
        <span class="module-main">
          <strong>{{ item.title }}</strong>
          <small>{{ item.desc }}</small>
        </span>
        <el-icon class="module-arrow"><ArrowRight /></el-icon>
      </button>
    </section>

    <section class="release-panel">
      <div>
        <h2>当前重点</h2>
        <p>0.2.x 系列聚焦“创作工作台 + 二进制逆向工作台”的稳定交付。</p>
      </div>
      <ul>
        <li v-for="item in releaseNotes" :key="item">{{ item }}</li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { animate, stagger } from "animejs";

const router = useRouter();

const go = (path: string) => {
  router.push(path);
};

const statusItems = [
  { value: "4", label: "核心工作区" },
  { value: "AI", label: "本地密钥设置" },
  { value: "JSON", label: "版本化工程" },
  { value: "SSE", label: "流式生成" },
];

const modules = [
  {
    path: "/analysis",
    icon: "Cpu",
    color: "#16a34a",
    bg: "rgba(22, 163, 74, 0.11)",
    title: "二进制分析工作台",
    desc: "PE 解析、虚表检测、世界数组优先分析、字符串和 Hex 查看。",
  },
  {
    path: "/novels",
    icon: "EditPen",
    color: "#2563eb",
    bg: "rgba(37, 99, 235, 0.11)",
    title: "小说助手",
    desc: "开书、设定、章节写作、审查、记忆、RAG 和追读力闭环。",
  },
  {
    path: "/chat",
    icon: "ChatDotRound",
    color: "#9333ea",
    bg: "rgba(147, 51, 234, 0.1)",
    title: "即时聊天",
    desc: "私聊、群聊、文件图片消息、搜索和短时间撤回。",
  },
  {
    path: "/settings",
    icon: "Setting",
    color: "#d97706",
    bg: "rgba(217, 119, 6, 0.12)",
    title: "设置中心",
    desc: "管理资料、头像、密码、主题和侧边栏偏好。",
  },
];

const releaseNotes = [
  "AI Key 和模型设置只保存在前端本地，后端临时使用后不落库。",
  "服务器真实配置不提交仓库，Release 不上传服务器运行 JAR。",
  "二进制报告导出已包含世界数组、对象数组和名称池相关证据。",
];

onMounted(() => {
  animate(".workspace-hero", {
    opacity: [{ from: 0 }, { to: 1 }],
    translateY: [{ from: 14 }, { to: 0 }],
    duration: 380,
    ease: "out(3)",
  });

  animate(".status-item, .module-tile, .release-panel", {
    opacity: [{ from: 0 }, { to: 1 }],
    translateY: [{ from: 18 }, { to: 0 }],
    duration: 360,
    delay: stagger(45, { start: 120 }),
    ease: "out(3)",
  });
});
</script>

<style scoped>
.home {
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workspace-hero,
.status-item,
.module-tile,
.release-panel {
  opacity: 0;
}

.workspace-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 30px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(20, 83, 45, 0.18), rgba(30, 41, 59, 0.04)),
    var(--color-surface);
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  margin-bottom: 8px;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0 0 8px;
  color: var(--color-text);
  font-size: 28px;
  line-height: 1.2;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 620px;
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 18px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.status-value {
  color: var(--color-text);
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}

.status-label {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.module-tile {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: inherit;
  padding: 18px;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 20px;
  gap: 14px;
  align-items: center;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s var(--transition-smooth), box-shadow 0.2s var(--transition-smooth), border-color 0.2s var(--transition-smooth);
}

.module-tile:hover {
  transform: translateY(-2px);
  border-color: rgba(74, 222, 128, 0.35);
  box-shadow: var(--shadow-md);
}

.module-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.module-main {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.module-main strong {
  color: var(--color-text);
  font-size: 15px;
  line-height: 1.3;
}

.module-main small {
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.module-arrow {
  color: var(--color-text-secondary);
}

.release-panel {
  display: grid;
  grid-template-columns: minmax(180px, 280px) minmax(0, 1fr);
  gap: 18px;
  padding: 20px;
  border-radius: var(--radius-md);
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.release-panel h2 {
  margin: 0 0 8px;
  color: var(--color-text);
  font-size: 16px;
  letter-spacing: 0;
}

.release-panel p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.release-panel ul {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.75;
}

@media (max-width: 900px) {
  .workspace-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .status-grid,
  .module-grid,
  .release-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .workspace-hero {
    padding: 22px;
  }

  .hero-copy h1 {
    font-size: 24px;
  }

  .module-tile {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .module-arrow {
    display: none;
  }
}
</style>
