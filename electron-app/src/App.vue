<template>
  <router-view />
</template>

<script setup lang="ts">
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

/* Dark theme */
[data-theme="dark"] {
  --color-bg: #121318;
  --color-surface: #1a1b23;
  --color-sidebar: #0f1015;
  --color-sidebar-light: #1e1f28;
  --color-sidebar-hover: rgba(255,255,255,0.06);
  --color-text: #e2e8f0;
  --color-text-secondary: #64748b;
  --color-border: rgba(255,255,255,0.08);
  --color-primary-bg: rgba(74,222,128,0.1);
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.2);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.25);
  --shadow-lg: 0 8px 32px rgba(0,0,0,0.3);
}

/* Element Plus overrides */
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

/* Dark theme element overrides */
[data-theme="dark"] .el-card { background: var(--color-surface) !important; color: var(--color-text) !important; }
[data-theme="dark"] .el-input__wrapper { background: var(--color-sidebar-light) !important; box-shadow: 0 0 0 1px var(--color-border) inset !important; }
[data-theme="dark"] .el-input__inner { color: var(--color-text) !important; }
[data-theme="dark"] .el-textarea__inner { background: var(--color-sidebar-light) !important; color: var(--color-text) !important; }
[data-theme="dark"] .el-dialog { background: var(--color-surface) !important; }
[data-theme="dark"] .el-dialog__title { color: var(--color-text) !important; }
[data-theme="dark"] .el-form-item__label { color: var(--color-text) !important; }
[data-theme="dark"] .el-select-dropdown { background: var(--color-surface) !important; }
[data-theme="dark"] .el-select-dropdown__item { color: var(--color-text) !important; }
[data-theme="dark"] .el-select-dropdown__item.hover { background: var(--color-sidebar-light) !important; }
[data-theme="dark"] .el-segmented { background: var(--color-sidebar-light) !important; }
[data-theme="dark"] .el-segmented__item { color: var(--color-text-secondary) !important; }
[data-theme="dark"] .el-segmented__item.is-active { color: var(--color-text) !important; background: var(--color-surface) !important; }
[data-theme="dark"] .el-tabs__item { color: var(--color-text-secondary) !important; }
[data-theme="dark"] .el-tabs__item.is-active { color: var(--color-primary) !important; }
[data-theme="dark"] .el-radio__label { color: var(--color-text) !important; }
[data-theme="dark"] .el-dropdown-menu { background: var(--color-surface) !important; }
[data-theme="dark"] .el-dropdown-menu__item { color: var(--color-text) !important; }
[data-theme="dark"] .el-badge__content { background: var(--color-primary) !important; color: #052e16 !important; }
[data-theme="dark"] .el-empty__description p { color: var(--color-text-secondary) !important; }
[data-theme="dark"] .el-header { background: var(--color-surface) !important; border-color: var(--color-border) !important; }
[data-theme="dark"] .el-message-box { background: var(--color-surface) !important; }
[data-theme="dark"] .el-message-box__title { color: var(--color-text) !important; }
[data-theme="dark"] .el-message-box__message { color: var(--color-text-secondary) !important; }
[data-theme="dark"] .el-upload-dragger { background: var(--color-sidebar-light) !important; }
</style>