<div align="center">

# 猫爪工具 - Cat Paw Tool

**二进制文件分析助手 · PE 结构解析 · AI 智能分析 · 虚表地址提取**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/)
[![Vue.js](https://img.shields.io/badge/Vue.js-3-brightgreen.svg)](https://vuejs.org/)
[![Tauri](https://img.shields.io/badge/Tauri-2-blue.svg)](https://tauri.app/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)

<br/>

[![GitHub stars](https://img.shields.io/github/stars/Jvxi/Unity?style=social)](https://github.com/Jvxi/Unity/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/Jvxi/Unity?style=social)](https://github.com/Jvxi/Unity/network/members)
[![GitHub issues](https://img.shields.io/github/issues/Jvxi/Unity)](https://github.com/Jvxi/Unity/issues)

</div>

---

## 功能特性

### DLL 分析
- **PE 全面解析**：DOS/PE 头、段表、16 个 Data Directory 全覆盖
- **多策略虚表检测**：RTTI 引导、连续指针扫描、导出交叉引用三种策略互补
- **AI 智能分析**：支持 DeepSeek 和小米 MiMo 大模型辅助确认虚表、排除误报
- **报告导出**：一键导出 JSON / HTML 格式分析报告

### 工具集
- **字符串提取**：从二进制文件中提取 ASCII/Unicode 字符串，支持最小长度、编码过滤、关键词搜索
- **十六进制查看器**：文件十六进制 Dump 查看，支持偏移量/长度/搜索

### 用户系统
- **注册 / 登录**：邮箱验证码注册，昵称或邮箱登录
- **路由守卫**：未登录自动跳转登录页，已登录自动进入首页
- **历史记录**：分析历史自动保存和查看

### 桌面客户端
- **Tauri 2** 轻量桌面应用（约 9 MB）
- 分栏式登录/注册页面，磨砂玻璃效果
- 可折叠侧边栏导航
- NSIS 安装包分发

---

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- Maven 3.9+
- Rust（仅打包桌面端需要）

### 启动后端

```bash
cd spring-server
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`

### 启动前端

```bash
cd electron-app
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`

### 打包桌面程序

```bash
cd electron-app
npm run tauri build
```

---

## 支持的 AI 模型

### DeepSeek

| 模型 | 说明 | 状态 |
|------|------|------|
| `deepseek-v4-flash` | 轻量快速，性价比高 | 推荐 |
| `deepseek-v4-pro` | 专业版，能力最强 | 可用 |

### 小米 MiMo

| 模型 | 说明 | 状态 |
|------|------|------|
| `MiMo-V2.5` | 主模型，通用对话 | 推荐 |
| `mimo-v2.5-pro` | 专业版 | 可用 |
| `MiMo-V2-Flash` | 轻量快速版 | 可用 |

---

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/health` | 健康检查 |
| `GET` | `/api/providers` | 获取 AI 提供商和模型列表 |
| `POST` | `/api/analyze` | 上传 DLL 文件进行分析 |
| `POST` | `/api/tools/strings` | 字符串提取 |
| `POST` | `/api/tools/hex` | 十六进制查看 |
| `GET` | `/api/tools/export/json` | 导出 JSON 报告 |
| `GET` | `/api/tools/export/html` | 导出 HTML 报告 |
| `POST` | `/auth/login` | 用户登录 |
| `POST` | `/auth/register` | 用户注册 |
| `POST` | `/auth/send-code` | 发送邮箱验证码 |

---

## 项目结构

```
Unity/
├── spring-server/                    # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/jvxi/unity/
│       ├── controller/               # REST API
│       ├── service/
│       │   ├── pe/                   # PE 解析器
│       │   ├── VtableDetectorService # 虚表检测
│       │   ├── DeepSeekService       # AI 集成
│       │   ├── StringExtractService  # 字符串提取
│       │   ├── HexViewService        # 十六进制查看
│       │   └── ReportExportService   # 报告导出
│       └── model/                    # 数据模型
│
├── electron-app/                     # Tauri + Vue 3 前端
│   ├── package.json
│   ├── src-tauri/                    # Tauri Rust 源码
│   │   ├── Cargo.toml
│   │   └── tauri.conf.json
│   └── src/
│       ├── layout/MainLayout.vue     # 可折叠菜单布局
│       ├── views/                    # 页面视图
│       ├── components/               # 通用组件
│       ├── api/client.ts             # API 调用封装
│       ├── stores/                   # 状态管理
│       └── router/index.ts           # 路由配置
│
├── LICENSE                           # MIT License
└── README.md
```

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21 / Spring Boot 3.2 / Maven |
| 前端 | Vue 3 / TypeScript / Element Plus / Vite |
| 桌面 | Tauri 2 / Rust |
| AI | DeepSeek / 小米 MiMo |

---

## License

[MIT License](LICENSE)