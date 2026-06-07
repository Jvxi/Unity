<template>
  <MainLayout />
</template>

<script setup lang="ts">
import MainLayout from "./layout/MainLayout.vue";
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; font-family: "SF Pro Display", -apple-system, "Microsoft YaHei", sans-serif; }

:root {
  --radius-sm: 8px;
  --radius-md: 14px;
  --radius-lg: 20px;
  --radius-xl: 28px;
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.05);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.07);
  --shadow-lg: 0 8px 32px rgba(0,0,0,0.1);
  --color-primary: #4ade80;
  --color-primary-light: #86efac;
  --color-primary-dark: #22c55e;
  --color-primary-bg: rgba(74,222,128,0.08);
  --color-bg: #f4f5f7;
  --color-surface: #ffffff;
  --color-sidebar: #1e1f24;
  --color-sidebar-light: #2a2b32;
  --color-sidebar-hover: rgba(255,255,255,0.05);
  --color-text: #1e293b;
  --color-text-secondary: #94a3b8;
  --color-border: rgba(0,0,0,0.06);
  --transition-smooth: cubic-bezier(0.4, 0, 0.2, 1);
  --transition-bounce: cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* Element Plus border-radius override */
.el-card { border-radius: var(--radius-md) !important; border: none !important; box-shadow: var(--shadow-sm) !important; }
.el-card:hover { box-shadow: var(--shadow-md) !important; }
.el-button { border-radius: var(--radius-sm) !important; }
.el-button--primary { background: var(--color-primary) !important; border-color: var(--color-primary) !important; color: #052e16 !important; font-weight: 600; }
.el-button--primary:hover { background: var(--color-primary-dark) !important; border-color: var(--color-primary-dark) !important; }
.el-input__wrapper { border-radius: var(--radius-sm) !important; }
.el-select .el-input__wrapper { border-radius: var(--radius-sm) !important; }
.el-table { border-radius: var(--radius-md) !important; overflow: hidden; }
.el-tag { border-radius: 20px !important; }
.el-tag--success { --el-tag-bg-color: rgba(74,222,128,0.12) !important; --el-tag-text-color: #16a34a !important; --el-tag-border-color: transparent !important; }
.el-upload-dragger { border-radius: var(--radius-lg) !important; }
.el-menu { border-right: none !important; }
.el-progress-bar__outer { border-radius: 10px !important; }
.el-progress-bar__inner { border-radius: 10px !important; }
</style>
