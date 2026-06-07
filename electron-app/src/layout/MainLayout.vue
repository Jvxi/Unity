<template>
  <el-container style="height: 100vh">
    <el-aside :width="isCollapsed ? '64px' : '200px'" class="sidebar">
      <div class="logo">
        <el-icon :size="28"><Cpu /></el-icon>
        <span v-show="!isCollapsed" class="logo-text">VTable Analyzer</span>
      </div>

      <el-menu
        :default-active="currentRoute"
        :collapse="isCollapsed"
        router
        background-color="#1d1e2c"
        text-color="#a3a6b4"
        active-text-color="#409eff"
        :collapse-transition="true"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-menu-item index="/analysis">
          <el-icon><Search /></el-icon>
          <template #title>DLL 分析</template>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>设置</template>
        </el-menu-item>
      </el-menu>

      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon>
          <component :is="isCollapsed ? 'ArrowRight' : 'ArrowLeft'" />
        </el-icon>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="page-title">{{ pageTitle }}</span>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useSettingsStore } from "@/stores/settings";

const route = useRoute();
const store = useSettingsStore();

const isCollapsed = computed(() => store.menuCollapsed);
const currentRoute = computed(() => route.path);

const toggleCollapse = () => store.setMenuCollapsed(!store.menuCollapsed);

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
  background: #1d1e2c;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden;
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
  border-bottom: 1px solid #2d2e3e;
}
.logo-text { white-space: nowrap; }
.collapse-btn {
  margin-top: auto;
  padding: 12px;
  text-align: center;
  cursor: pointer;
  color: #a3a6b4;
  border-top: 1px solid #2d2e3e;
}
.collapse-btn:hover { color: #409eff; background: #2d2e3e; }
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
</style>