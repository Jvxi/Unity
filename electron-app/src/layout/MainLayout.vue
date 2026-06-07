<template>
  <el-container style="height: 100vh">
    <el-aside :width="isCollapsed ? '72px' : '220px'" class="sidebar" ref="sidebarRef">
      <div class="logo" ref="logoRef">
        <div class="logo-icon">
          <el-icon :size="24"><Cpu /></el-icon>
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
          :class="{ active: currentRoute === item.path }"
        >
          <div class="menu-icon">
            <el-icon :size="20"><component :is="item.icon" /></el-icon>
          </div>
          <transition name="fade-slide">
            <span v-show="!isCollapsed" class="menu-label">{{ item.label }}</span>
          </transition>
          <div v-if="currentRoute === item.path" class="active-indicator"></div>
        </router-link>
      </nav>

      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon :size="16">
          <component :is="isCollapsed ? 'ArrowRight' : 'ArrowLeft'" />
        </el-icon>
      </div>

      <div class="sidebar-glow" ref="glowRef"></div>
    </el-aside>

    <el-container>
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
        translateY: [{ from: 24 }, { to: 0 }],
        scale: [{ from: 0.97 }, { to: 1 }],
        duration: 450,
        delay: stagger(80, { start: 80 }),
        ease: 'outElastic(1, .85)',
      });
    }
  });
};

const animateGlow = () => {
  if (glowRef.value) {
    animate(glowRef.value, {
      top: ['0%', '100%'],
      opacity: [0, 0.4, 0],
      duration: 4000,
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
      translateY: [{ from: -16 }, { to: 0 }],
      duration: 500,
      ease: 'outElastic(1, .7)',
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
  background: linear-gradient(180deg, #1a1a2e 0%, #16162a 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.35s var(--transition-smooth);
  overflow: hidden;
  position: relative;
  border-radius: 0 var(--radius-xl) var(--radius-xl) 0;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  position: relative;
  z-index: 2;
}
.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.logo-text {
  color: #e2e8f0;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  letter-spacing: 0.3px;
}

.menu-nav {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
  z-index: 2;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.25s var(--transition-smooth);
  position: relative;
  overflow: hidden;
}
.menu-item:hover {
  color: #e2e8f0;
  background: var(--color-sidebar-hover);
}
.menu-item.active {
  color: #fff;
  background: rgba(99,102,241,0.15);
}
.menu-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  transition: all 0.25s var(--transition-smooth);
}
.menu-item.active .menu-icon {
  background: var(--color-primary);
  color: #fff;
}
.menu-label {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}
.active-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: var(--color-primary);
}

.collapse-btn {
  margin: 8px 10px 12px;
  padding: 10px;
  text-align: center;
  cursor: pointer;
  color: #64748b;
  border-radius: var(--radius-sm);
  transition: all 0.25s var(--transition-smooth);
  position: relative;
  z-index: 2;
}
.collapse-btn:hover {
  color: #e2e8f0;
  background: var(--color-sidebar-hover);
}

.sidebar-glow {
  position: absolute;
  left: 0;
  width: 100%;
  height: 80px;
  background: linear-gradient(180deg, transparent, rgba(99,102,241,0.06), transparent);
  pointer-events: none;
  z-index: 1;
}

.header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 28px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.page-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.3px;
}
.main-content {
  background: var(--color-bg);
  padding: 24px;
  overflow-y: auto;
}

/* 路由过渡 */
.page-slide-enter-active { transition: all 0.35s var(--transition-smooth); }
.page-slide-leave-active { transition: all 0.2s var(--transition-smooth); }
.page-slide-enter-from { opacity: 0; transform: translateY(16px) scale(0.99); }
.page-slide-leave-to { opacity: 0; transform: translateY(-8px) scale(0.99); }

/* 文字淡入滑出 */
.fade-slide-enter-active { transition: all 0.25s var(--transition-smooth); }
.fade-slide-leave-active { transition: all 0.15s var(--transition-smooth); }
.fade-slide-enter-from { opacity: 0; transform: translateX(-8px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(8px); }
</style>