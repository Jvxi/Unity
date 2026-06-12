<template>
  <el-container class="app-shell">
    <el-aside :width="isCollapsed ? '72px' : '220px'" class="sidebar" ref="sidebarRef">
      <div class="logo" ref="logoRef">
        <div class="logo-icon">
          <img src="@/assets/cat-logo.png" alt="猫爪工具" class="logo-img" />
        </div>
        <transition name="fade-slide">
          <span v-show="!isCollapsed" class="logo-text">猫爪工具</span>
        </transition>
      </div>

      <nav class="menu-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="menu-item"
          :class="{ active: isMenuActive(item.path), 'collapsed-item': isCollapsed }"
        >
          <div class="menu-icon">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </div>
          <transition name="fade-slide">
            <span v-show="!isCollapsed" class="menu-label">{{ item.label }}</span>
          </transition>
          <div v-if="isMenuActive(item.path) && !isCollapsed" class="active-dot"></div>
          <el-badge v-if="item.path === '/chat' && chatStore.unreadTotal > 0" :value="chatStore.unreadTotal" :max="99" class="chat-badge" />
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
      <el-header class="header">
        <h2 class="page-title">{{ pageTitle }}</h2>
        <div class="header-right">
          <template v-if="authStore.isLoggedIn">
            <el-dropdown>
              <span class="user-info">
                <el-avatar :size="32" :src="userAvatarSrc" />
                <span class="username">{{ authStore.user?.nickname }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/settings')">设置</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
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
import { useRoute, useRouter } from "vue-router";
import { useSettingsStore } from "@/stores/settings";
import { useAuthStore } from "@/stores/auth";
import { useChatStore } from "@/stores/chat";
import { animate } from "animejs";
import { panelIn, slideFlash, softPulse, staggerIn } from "@/utils/motion";
import { ElMessage } from "element-plus";
import catLogo from "@/assets/cat-logo.png";
import { resolveAssetUrl } from "@/api/client";

const route = useRoute();
const router = useRouter();
const store = useSettingsStore();
const authStore = useAuthStore();
const chatStore = useChatStore();

const isCollapsed = computed(() => store.menuCollapsed);
const currentRoute = computed(() => route.path);
const userAvatarSrc = computed(() => resolveAssetUrl(authStore.user?.avatarUrl) || catLogo);

const sidebarRef = ref<HTMLElement | null>(null);
const logoRef = ref<HTMLElement | null>(null);
const glowRef = ref<HTMLElement | null>(null);

const menuItems = [
  { path: "/", icon: "HomeFilled", label: "首页" },
  { path: "/analysis", icon: "Cpu", label: "二进制工作台" },
  { path: "/history", icon: "Clock", label: "历史记录" },
  { path: "/chat", icon: "ChatDotRound", label: "聊天" },
  { path: "/novels", icon: "EditPen", label: "小说助手" },
  { path: "/settings", icon: "Setting", label: "设置" },
];

const isMenuActive = (path: string) => currentRoute.value === path;

const toggleCollapse = () => {
  const newState = !store.menuCollapsed;
  store.setMenuCollapsed(newState);
  animateMenuItems();
};

const animateMenuItems = () => {
  const items = document.querySelectorAll(".menu-item");
  if (items.length > 0) {
    staggerIn(items, {
      x: isCollapsed.value ? -8 : 8,
      y: 0,
      scale: 1,
      duration: 320,
      staggerDelay: 34,
      ease: "out(3)",
    });
  }
};

const animatePageEnter = () => {
  nextTick(() => {
    const cards = document.querySelectorAll(".main-content .el-card");
    if (cards.length > 0) {
      staggerIn(cards, {
        y: 20,
        scale: 0.98,
        duration: 400,
        delay: 60,
        staggerDelay: 70,
        ease: "out(3)",
      });
    }
    panelIn(document.querySelector(".header"), { y: -6, duration: 240 });
  });
};

const animateGlow = () => {
  if (glowRef.value) {
    animate(glowRef.value, {
      top: ["0%", "100%"],
      opacity: [0, 0.3, 0],
      duration: 5000,
      loop: true,
      ease: "inOutSine",
    });
  }
};

watch(() => route.path, animatePageEnter);
watch(() => chatStore.unreadTotal, (count, oldCount) => {
  if (count > oldCount) {
    softPulse(document.querySelector(".chat-badge"), 1.12);
  }
});

onMounted(() => {
  animateGlow();
  animatePageEnter();
  if (logoRef.value) {
    animate(logoRef.value, {
      opacity: [{ from: 0 }, { to: 1 }],
      translateY: [{ from: -12 }, { to: 0 }],
      duration: 450,
      ease: "outElastic(1, .75)",
    });
  }
});

const pageTitle = computed(() => {
  switch (route.path) {
    case "/": return "首页";
    case "/analysis": return "二进制工作台";
    case "/strings":
    case "/hex": return "二进制工作台";
    case "/history": return "历史记录";
    case "/chat": return "聊天";
    case "/novels": return "小说助手";
    case "/settings": return "设置";
    default: return "猫爪工具";
  }
});

watch(pageTitle, () => {
  nextTick(() => slideFlash(document.querySelector(".header .page-title"), -6));
});

const handleLogout = () => {
  chatStore.disconnect();
  authStore.logout();
  ElMessage.success("已退出登录");
  router.push("/");
};
</script>

<style scoped>
.app-shell {
  height: 100vh;
  padding: 12px;
  gap: 12px;
}

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
  flex-shrink: 0;
  overflow: hidden;
}

.logo-img {
  width: 28px;
  height: 28px;
  object-fit: cover;
}

.logo-text {
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  letter-spacing: 0;
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

.chat-badge {
  position: absolute;
  right: 8px;
  top: 4px;
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
  min-width: 0;
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: var(--color-bg);
}

.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: var(--color-text);
}

.main-content {
  min-width: 0;
  background: var(--color-bg);
  padding: 20px;
  overflow: auto;
}

.page-slide-enter-active { transition: all 0.3s var(--transition-smooth); }
.page-slide-leave-active { transition: all 0.18s var(--transition-smooth); }
.page-slide-enter-from { opacity: 0; transform: translateY(14px) scale(0.99); }
.page-slide-leave-to { opacity: 0; transform: translateY(-6px) scale(0.99); }

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

@media (max-width: 720px) {
  .app-shell {
    flex-direction: column;
    height: 100vh;
    padding: 8px;
    gap: 8px;
  }

  .sidebar {
    width: 100% !important;
    height: auto;
    min-height: 52px;
    flex-direction: row;
    align-items: center;
    border-radius: var(--radius-lg);
  }

  .logo {
    width: 48px;
    height: 52px;
    padding: 0 8px;
    border-bottom: 0;
    border-right: 1px solid rgba(255,255,255,0.05);
    flex-shrink: 0;
  }

  .logo-icon {
    width: 32px;
    height: 32px;
  }

  .logo-text,
  .active-dot,
  .collapse-btn,
  .sidebar-glow {
    display: none;
  }

  .menu-nav {
    min-width: 0;
    flex: 1;
    flex-direction: row;
    overflow-x: auto;
    overflow-y: hidden;
    padding: 7px 8px;
    gap: 4px;
    scrollbar-width: none;
  }

  .menu-nav::-webkit-scrollbar {
    display: none;
  }

  .menu-item,
  .menu-item.active,
  .menu-item.active.collapsed-item {
    flex: 0 0 auto;
    justify-content: center;
    gap: 6px;
    padding: 8px 10px;
    border-left: 0;
    min-width: 42px;
  }

  .menu-icon {
    width: 28px;
    height: 28px;
  }

  .menu-label {
    display: none;
  }

  .chat-badge {
    right: 0;
    top: 0;
  }

  .main-area {
    flex: 1;
    min-height: 0;
    border-radius: var(--radius-lg);
  }

  .header {
    height: 48px;
    padding: 0 14px;
  }

  .page-title {
    font-size: 15px;
  }

  .username {
    display: none;
  }

  .main-content {
    padding: 12px;
  }
}
</style>
