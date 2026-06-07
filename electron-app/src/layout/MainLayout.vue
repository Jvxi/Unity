<template>
  <el-container style="height: 100vh">
    <el-aside :width="isCollapsed ? '64px' : '200px'" class="sidebar" ref="sidebarRef">
      <div class="logo" ref="logoRef">
        <el-icon :size="28"><Cpu /></el-icon>
        <span v-show="!isCollapsed" class="logo-text">VTable Analyzer</span>
      </div>

      <el-menu
        :default-active="currentRoute"
        :collapse="isCollapsed"
        router
        background-color="transparent"
        text-color="#a3a6b4"
        active-text-color="#409eff"
        :collapse-transition="false"
        ref="menuRef"
      >
        <el-menu-item index="/" ref="menuItem1">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-menu-item index="/analysis" ref="menuItem2">
          <el-icon><Search /></el-icon>
          <template #title>DLL 分析</template>
        </el-menu-item>
        <el-menu-item index="/settings" ref="menuItem3">
          <el-icon><Setting /></el-icon>
          <template #title>设置</template>
        </el-menu-item>
      </el-menu>

      <div class="collapse-btn" @click="toggleCollapse" ref="collapseBtnRef">
        <el-icon>
          <component :is="isCollapsed ? 'ArrowRight' : 'ArrowLeft'" />
        </el-icon>
      </div>

      <!-- 装饰性流动光效 -->
      <div class="sidebar-glow" ref="glowRef"></div>
    </el-aside>

    <el-container>
      <el-header class="header" ref="headerRef">
        <span class="page-title">{{ pageTitle }}</span>
      </el-header>
      <el-main class="main-content">
        <router-view v-slot="{ Component, route }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch, nextTick } from "vue";
import { useRoute } from "vue-router";
import { useSettingsStore } from "@/stores/settings";
import { animate, stagger, utils } from "animejs";

const route = useRoute();
const store = useSettingsStore();

const isCollapsed = computed(() => store.menuCollapsed);
const currentRoute = computed(() => route.path);

const sidebarRef = ref<HTMLElement | null>(null);
const logoRef = ref<HTMLElement | null>(null);
const headerRef = ref<HTMLElement | null>(null);
const glowRef = ref<HTMLElement | null>(null);
const collapseBtnRef = ref<HTMLElement | null>(null);

const toggleCollapse = () => {
  const newState = !store.menuCollapsed;
  store.setMenuCollapsed(newState);
  animateSidebar(newState);
};

const animateSidebar = (collapsed: boolean) => {
  // 菜单项交错动画
  const menuItems = document.querySelectorAll('.el-menu-item');
  if (menuItems.length > 0) {
    animate(menuItems, {
      opacity: [{ from: 0.5 }, { to: 1 }],
      translateX: collapsed ? [{ from: -10 }, { to: 0 }] : [{ from: 10 }, { to: 0 }],
      duration: 400,
      delay: stagger(50),
      ease: 'outElastic(1, .8)',
    });
  }

  // Logo 动画
  if (logoRef.value) {
    animate(logoRef.value, {
      scale: [{ from: 0.9 }, { to: 1 }],
      duration: 300,
      ease: 'outBack(1.7)',
    });
  }
};

// 页面入场动画
const animatePageEnter = () => {
  nextTick(() => {
    const mainContent = document.querySelector('.main-content');
    if (mainContent) {
      const cards = mainContent.querySelectorAll('.el-card');
      if (cards.length > 0) {
        animate(cards, {
          opacity: [{ from: 0 }, { to: 1 }],
          translateY: [{ from: 30 }, { to: 0 }],
          scale: [{ from: 0.95 }, { to: 1 }],
          duration: 500,
          delay: stagger(100, { start: 100 }),
          ease: 'outElastic(1, .8)',
        });
      }
    }
  });
};

// Logo 入场动画
const animateLogo = () => {
  if (logoRef.value) {
    animate(logoRef.value, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: -20 }, { to: 0 }],
      duration: 600,
      ease: 'outElastic(1, .6)',
    });
  }
};

// 头部入场动画
const animateHeader = () => {
  if (headerRef.value) {
    animate(headerRef.value, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateX: [{ from: -30 }, { to: 0 }],
      duration: 500,
      delay: 200,
      ease: 'outElastic(1, .8)',
    });
  }
};

// 菜单项入场动画
const animateMenuItems = () => {
  const menuItems = document.querySelectorAll('.el-menu-item');
  if (menuItems.length > 0) {
    animate(menuItems, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateX: [{ from: -30 }, { to: 0 }],
      duration: 500,
      delay: stagger(100, { start: 300 }),
      ease: 'outElastic(1, .8)',
    });
  }
};

// 流动光效动画
const animateGlow = () => {
  if (glowRef.value) {
    animate(glowRef.value, {
      top: ['0%', '100%'],
      opacity: [{ from: 0 }, { to: 0.3 }, { to: 0 }],
      duration: 3000,
      loop: true,
      ease: 'inOutQuad',
    });
  }
};

// 折叠按钮悬停动画
const animateCollapseBtn = () => {
  if (collapseBtnRef.value) {
    collapseBtnRef.value.addEventListener('mouseenter', () => {
      animate(collapseBtnRef.value!, {
        scale: 1.1,
        duration: 200,
        ease: 'outElastic(1, .8)',
      });
    });
    collapseBtnRef.value.addEventListener('mouseleave', () => {
      animate(collapseBtnRef.value!, {
        scale: 1,
        duration: 200,
        ease: 'outElastic(1, .8)',
      });
    });
  }
};

// 监听路由变化
watch(() => route.path, () => {
  animatePageEnter();
});

onMounted(() => {
  animateLogo();
  animateHeader();
  animateMenuItems();
  animateGlow();
  animateCollapseBtn();
  animatePageEnter();
});

const pageTitle = computed(() => {
  switch (route.path) {
    case "/": return "首页";
    case "/analysis": return "DLL 分析";
    case "/settings": return "设置";
    default: return "Unity";
  }
});
</script>

<style scoped>
.sidebar {
  background: linear-gradient(180deg, #1a1b2e 0%, #1d1e2c 50%, #1a1b2e 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #409eff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid rgba(64, 158, 255, 0.1);
  position: relative;
  z-index: 2;
}
.logo-text { white-space: nowrap; }
.collapse-btn {
  margin-top: auto;
  padding: 12px;
  text-align: center;
  cursor: pointer;
  color: #a3a6b4;
  border-top: 1px solid rgba(64, 158, 255, 0.1);
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
}
.collapse-btn:hover {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

/* 流动光效 */
.sidebar-glow {
  position: absolute;
  left: 0;
  width: 100%;
  height: 60px;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(64, 158, 255, 0.05) 30%,
    rgba(64, 158, 255, 0.1) 50%,
    rgba(64, 158, 255, 0.05) 70%,
    transparent 100%);
  pointer-events: none;
  z-index: 1;
}

/* 菜单项动画增强 */
.sidebar :deep(.el-menu) {
  border-right: none;
  background: transparent !important;
}
.sidebar :deep(.el-menu-item) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}
.sidebar :deep(.el-menu-item)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: #409eff;
  transform: scaleY(0);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.sidebar :deep(.el-menu-item.is-active)::before {
  transform: scaleY(1);
}
.sidebar :deep(.el-menu-item:hover) {
  background: rgba(64, 158, 255, 0.08) !important;
}

.header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.page-title { font-size: 18px; font-weight: 600; color: #303133; }
.main-content { background: #f5f7fa; padding: 20px; overflow-y: auto; }

/* 页面切换动画 */
.page-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.page-fade-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.98);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
}
</style>