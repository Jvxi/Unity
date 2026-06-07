<template>
  <el-container style="height: 100vh; padding: 12px; gap: 12px;">
    <el-aside :width="isCollapsed ? '72px' : '220px'" class="sidebar" ref="sidebarRef">
      <div class="logo" ref="logoRef">
        <div class="logo-icon">
          <el-icon :size="22"><Cpu /></el-icon>
        </div>
        <transition name="fade-slide">
          <span v-show="!isCollapsed" class="logo-text">VTable Analyzer</span>
        </transition>
      </div>

      <nav class="menu-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="menu-item"
          :class="{ active: currentRoute === item.path, 'collapsed-item': isCollapsed }"
        >
          <div class="menu-icon">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </div>
          <transition name="fade-slide">
            <span v-show="!isCollapsed" class="menu-label">{{ item.label }}</span>
          </transition>
          <div v-if="currentRoute === item.path && !isCollapsed" class="active-dot"></div>
        </router-link>
      </nav>

      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon :size="14">
          <component :is="isCollapsed ? 'ArrowRight' : 'ArrowLeft'" />
        </el-icon>
      </div>

      <div class="sidebar-glow" ref="glowRef"></div>
    </el-aside>

    <el-container class="main-area">
      <el-header class="header" ref="headerRef">
        <h2 class="page-title">{{ pageTitle }}</h2>
      </el-header>
      <el-main class="main-content">
        <router-view v-slot="{ Component, route: r }">
          <transition name="page-slide" mode="out-in">
            <component :is="Component" :key="r.path" />
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
import { animate, stagger } from "animejs";

const route = useRoute();
const store = useSettingsStore();

const isCollapsed = computed(() => store.menuCollapsed);
const currentRoute = computed(() => route.path);

const sidebarRef = ref<HTMLElement | null>(null);
const logoRef = ref<HTMLElement | null>(null);
const headerRef = ref<HTMLElement | null>(null);
const glowRef = ref<HTMLElement | null>(null);

const menuItems = [
  { path: "/", icon: "HomeFilled", label: "首页" },
  { path: "/analysis", icon: "Search", label: "DLL 分析" },
  { path: "/settings", icon: "Setting", label: "设置" },
];

const toggleCollapse = () => {
  const newState = !store.menuCollapsed;
  store.setMenuCollapsed(newState);
  animateMenuItems();
};

const animateMenuItems = () => {
  const items = document.querySelectorAll('.menu-item');
  if (items.length > 0) {
    animate(items, {
      opacity: [{ from: 0.6 }, { to: 1 }],
      translateX: isCollapsed.value ? [{ from: -8 }, { to: 0 }] : [{ from: 8 }, { to: 0 }],
      duration: 350,
      delay: stagger(40),
      ease: 'outElastic(1, .85)',
    });
  }
};

const animatePageEnter = () => {
  nextTick(() => {
    const cards = document.querySelectorAll('.main-content .el-card');
    if (cards.length > 0) {
      animate(cards, {
        opacity: [{ from: 0 }, { to: 1 }],
        translateY: [{ from: 20 }, { to: 0 }],
        scale: [{ from: 0.98 }, { to: 1 }],
        duration: 400,
        delay: stagger(70, { start: 60 }),
        ease: 'outElastic(1, .85)',
      });
    }
  });
};

const animateGlow = () => {
  if (glowRef.value) {
    animate(glowRef.value, {
      top: ['0%', '100%'],
      opacity: [0, 0.3, 0],
      duration: 5000,
      loop: true,
      ease: 'inOutSine',
    });
  }
};

watch(() => route.path, animatePageEnter);

onMounted(() => {
  animateGlow();
  animatePageEnter();
  if (logoRef.value) {
    animate(logoRef.value, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: -12 }, { to: 0 }],
      duration: 450,
      ease: 'outElastic(1, .75)',
    });
  }
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
  background: var(--color-sidebar);
  display: flex;
  flex-direction: column;
  transition: width 0.35s var(--transition-smooth);
  overflow: hidden;
  position: relative;
  border-radius: var(--radius-xl);
  flex-shrink: 0;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  position: relative;
  z-index: 2;
}
.logo-icon {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #052e16;
  flex-shrink: 0;
}
.logo-text {
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  letter-spacing: 0.2px;
}

.menu-nav {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  position: relative;
  z-index: 2;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  justify-content: flex-start;
  border-radius: var(--radius-md);
  color: #6b7280;
  text-decoration: none;
  transition: all 0.25s var(--transition-smooth);
  position: relative;
  overflow: hidden;
}
.menu-item:hover {
  color: #d1d5db;
  background: var(--color-sidebar-hover);
}
.menu-item.active {
  color: var(--color-primary);
  background: rgba(74,222,128,0.08);
  border-left: 3px solid var(--color-primary);
  padding-left: 11px;
}
.menu-icon {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  transition: all 0.25s var(--transition-smooth);
}
.menu-item.active .menu-icon {
  background: rgba(74,222,128,0.15);
  color: var(--color-primary);
}
.menu-label {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}
.active-dot {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
}

.collapse-btn {
  margin: 8px 10px 12px;
  padding: 10px;
  text-align: center;
  cursor: pointer;
  color: #4b5563;
  border-radius: var(--radius-sm);
  transition: all 0.25s var(--transition-smooth);
  position: relative;
  z-index: 2;
}
.collapse-btn:hover {
  color: #d1d5db;
  background: var(--color-sidebar-hover);
}

.sidebar-glow {
  position: absolute;
  left: 0;
  width: 100%;
  height: 80px;
  background: linear-gradient(180deg, transparent, rgba(74,222,128,0.04), transparent);
  pointer-events: none;
  z-index: 1;
}

.main-area {
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: var(--color-bg);
}
.header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 28px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.2px;
}
.main-content {
  background: var(--color-bg);
  padding: 20px;
  overflow-y: auto;
}

/* 路由过渡 */
.page-slide-enter-active { transition: all 0.3s var(--transition-smooth); }
.page-slide-leave-active { transition: all 0.18s var(--transition-smooth); }
.page-slide-enter-from { opacity: 0; transform: translateY(14px) scale(0.99); }
.page-slide-leave-to { opacity: 0; transform: translateY(-6px) scale(0.99); }

/* 文字淡入 */
.fade-slide-enter-active { transition: all 0.25s var(--transition-smooth); }
.fade-slide-leave-active { transition: all 0.15s var(--transition-smooth); }
.fade-slide-enter-from { opacity: 0; transform: translateX(-6px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(6px); }

.menu-item.active.collapsed-item {
  background: rgba(74,222,128,0.12);
  border-left: none;
  padding-left: 14px;
  justify-content: center;
}
</style>