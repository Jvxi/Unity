<div align="center">

# 猫爪工具 - Cat Paw Tool

**二进制文件分析助手 · PE 结构解析 · AI 智能分析 · 虚表地址提取**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/)
[![Vue.js](https://img.shields.io/badge/Vue.js-3-brightgreen.svg)](https://vuejs.org/)
[![Tauri](https://img.shields.io/badge/Tauri-2-blue.svg)](https://tauri.app/)

[![GitHub stars](https://img.shields.io/github/stars/Jvxi/Unity?style=social)](https://github.com/Jvxi/Unity/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/Jvxi/Unity?style=social)](https://github.com/Jvxi/Unity/network/members)
[![GitHub issues](https://img.shields.io/github/issues/Jvxi/Unity)](https://github.com/Jvxi/Unity/issues)

</div>

---

## 下载安装

前往 [Releases](https://github.com/Jvxi/Unity/releases) 下载最新版本的 Windows 安装包。

> 当前版本：**v0.2.0** · 支持 Windows x64 · 提供安装包与 ZIP 绿色包

---

## 功能特性

### DLL 分析
- **PE 全面解析** — DOS/PE 头、段表、16 个 Data Directory 全覆盖
- **多策略虚表检测** — RTTI 引导、连续指针扫描、导出交叉引用三种策略互补
- **AI 智能分析** — 支持 DeepSeek 和小米 MiMo 大模型辅助确认虚表、排除误报
- **报告导出** — 一键导出 JSON / HTML 格式分析报告

### 工具集
- **字符串提取** — 从二进制文件中提取 ASCII/Unicode 字符串，支持最小长度、编码过滤、关键词搜索
- **十六进制查看器** — 文件十六进制 Dump，支持偏移量/长度/搜索

### 用户系统
- **注册 / 登录** — 邮箱验证码注册，昵称或邮箱登录
- **JWT 认证** — 无状态会话，Spring Security + JWT Token
- **路由守卫** — 未登录自动跳转登录页
- **历史记录** — 分析历史自动保存和查看
- **个人设置** — 头像上传、资料编辑、密码修改、主题和侧边栏设置

### 即时聊天
- **私聊 / 群聊** — 支持会话列表、未读数、群成员管理
- **文件与图片消息** — 上传后通过静态资源地址访问
- **消息搜索与撤回** — 支持当前会话检索和 2 分钟内撤回

### 桌面客户端
- **Tauri 2** 轻量桌面应用
- 分栏式登录/注册页面，磨砂玻璃效果
- 可折叠侧边栏导航
- NSIS / MSI 安装包和 ZIP 绿色包分发

---

## 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | 后端运行 |
| Node.js | 18+ | 前端构建 |
| Maven | 3.9+ | 后端构建 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.0+ | 验证码缓存 |
| Rust | 最新 | 仅打包桌面端需要 |

### 启动后端

复制或编写本地私有配置，生产环境建议使用环境变量注入敏感项：

```bash
cd spring-server
# Windows PowerShell 示例
$env:CAT_TOOL_DB_PASSWORD="your-db-password"
$env:CAT_TOOL_MAIL_PASSWORD="your-mail-auth-code"
$env:CAT_TOOL_JWT_SECRET="base64-encoded-256-bit-secret"
```

```bash
cd spring-server
mvn spring-boot:run
```

后端默认运行在 `http://localhost:38765`

### 启动前端

```bash
cd electron-app
npm install
cp .env.example .env
npm run dev
```

前端默认运行在 `http://localhost:5173`

### 打包桌面程序

```bash
cd electron-app
npm run tauri build
```

### 发布打包

```bash
# 后端 Jar
cd spring-server
mvn clean package -DskipTests

# 前端静态资源
cd ../electron-app
npm run build

# 桌面安装包
npm run tauri build
```

发布产物会整理到 `artifacts/v0.2.0/`，包括后端 Jar、前端 ZIP、桌面安装包和桌面 ZIP。

---

## 支持的 AI 模型

| 提供商 | 模型 | 说明 |
|--------|------|------|
| DeepSeek | `deepseek-v4-flash` | 轻量快速，推荐日常使用 |
| DeepSeek | `deepseek-v4-pro` | 专业版，能力最强 |
| 小米 MiMo | `MiMo-V2.5` | 主模型，通用对话 |
| 小米 MiMo | `mimo-v2.5-pro` | 专业版 |
| 小米 MiMo | `MiMo-V2-Flash` | 轻量快速版 |

---

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/health` | 健康检查 |
| `GET` | `/api/providers` | 获取 AI 提供商和模型列表 |
| `POST` | `/api/analyze` | 上传 DLL 文件进行分析 |
| `POST` | `/api/tools/strings` | 字符串提取 |
| `POST` | `/api/tools/hex` | 十六进制查看 |
| `POST` | `/api/tools/export/json` | 导出 JSON 报告 |
| `POST` | `/api/tools/export/html` | 导出 HTML 报告 |
| `POST` | `/api/auth/login` | 用户登录 |
| `POST` | `/api/auth/register` | 用户注册 |
| `POST` | `/api/auth/send-code` | 发送邮箱验证码 |
| `POST` | `/api/user/avatar` | 上传头像 |
| `GET` | `/api/chat/sessions` | 聊天会话 |
| `POST` | `/api/chat/upload` | 聊天文件上传 |

---

## 项目结构

```
Unity/
├── spring-server/                    # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/resources/application.yml # 安全默认配置，敏感项读取环境变量
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
│   └── src/
│       ├── views/                    # 页面视图
│       ├── components/               # 通用组件
│       ├── api/client.ts             # API 调用封装
│       ├── stores/                   # Pinia 状态管理
│       └── router/index.ts           # Vue Router 路由
│
├── LICENSE                           # MIT License
└── README.md
```

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21 / Spring Boot 3.2 / Spring Security / JWT / Maven |
| 前端 | Vue 3 / TypeScript / Element Plus / Vite |
| 桌面 | Tauri 2 / Rust |
| 数据库 | MySQL 8 / Redis |
| AI | DeepSeek / 小米 MiMo |

---

## 配置安全

以下文件不会推送到仓库：`.env`、`electron-app/.env.*`、`spring-server/src/main/resources/application-local.yml`、日志、上传文件和打包产物。

后端敏感配置通过环境变量读取：

| 环境变量 | 说明 |
|----------|------|
| `CAT_TOOL_DB_URL` | MySQL 连接地址 |
| `CAT_TOOL_DB_USERNAME` / `CAT_TOOL_DB_PASSWORD` | 数据库账号密码 |
| `CAT_TOOL_MAIL_USERNAME` / `CAT_TOOL_MAIL_PASSWORD` | SMTP 账号和授权码 |
| `CAT_TOOL_JWT_SECRET` | JWT Base64 密钥 |
| `CAT_TOOL_UPLOAD_DIR` | 上传文件目录 |

---

## License

[MIT License](LICENSE)
