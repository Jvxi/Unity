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
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.06);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08);
  --shadow-lg: 0 8px 32px rgba(0,0,0,0.12);
  --color-primary: #6366f1;
  --color-primary-light: #818cf8;
  --color-primary-bg: rgba(99,102,241,0.08);
  --color-bg: #f8f9fc;
  --color-surface: #ffffff;
  --color-sidebar: #1e1e2e;
  --color-sidebar-hover: rgba(255,255,255,0.06);
  --color-text: #1e293b;
  --color-text-secondary: #64748b;
  --color-border: rgba(0,0,0,0.06);
  --transition-smooth: cubic-bezier(0.4, 0, 0.2, 1);
  --transition-bounce: cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 全局 Element Plus 圆角覆盖 */
.el-card { border-radius: var(--radius-md) !important; border: none !important; box-shadow: var(--shadow-sm) !important; }
.el-card:hover { box-shadow: var(--shadow-md) !important; }
.el-button { border-radius: var(--radius-sm) !important; }
.el-input__wrapper { border-radius: var(--radius-sm) !important; }
.el-select .el-input__wrapper { border-radius: var(--radius-sm) !important; }
.el-table { border-radius: var(--radius-md) !important; overflow: hidden; }
.el-tag { border-radius: 20px !important; }
.el-descriptions { border-radius: var(--radius-md) !important; overflow: hidden; }
.el-collapse-item__header { border-radius: var(--radius-sm) !important; }
.el-upload-dragger { border-radius: var(--radius-lg) !important; }
</style>