<div align="center">

# 猫爪工具 - Cat Paw Tool

**二进制分析工作台 · 小说创作助手 · AI 增强分析 · Tauri 桌面客户端**

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

## 下载

前往 [Releases](https://github.com/Jvxi/Unity/releases) 下载最新版本。

> 当前版本：**v0.2.0**。Release 可提供后端开源包和 Windows 桌面安装包；服务器运行 JAR 需要在本地写入真实配置后自行构建，不作为 GitHub Release 附件发布。

---

## 功能特性

### 二进制分析工作台

- **统一文件入口**：上传一次二进制文件，在同一页面切换 PE / 虚表、字符串和 Hex 工具。
- **PE 全面解析**：解析 DOS/PE 头、段表、Data Directory、导入导出、调试信息、TLS 等结构。
- **多策略虚表检测**：支持 RTTI 引导、连续指针扫描、导出交叉引用等策略。
- **世界数组优先分析**：优先识别 `GWorld`、`UWorld`、`WorldContext`、`GUObjectArray`、`GNames`、`FNamePool`、Actor/Level 相关全局数据。
- **AI 增强分析**：AI 提示词会先分析世界数组、对象数组、名称池，再分析虚表候选和误报。
- **字符串与 Hex**：支持 ASCII / UTF-16LE 字符串提取、关键词过滤、Hex Dump、偏移量和字节序列搜索。
- **报告导出**：JSON / HTML 报告包含 PE 信息、虚表候选、世界数组候选、相关证据和 AI 摘要。

### 小说助手

- **书库与章节工作台**：创建、切换、删除书籍，统一管理项目 JSON、章节、大纲、角色和伏笔。
- **开书流程**：支持 8 问灵感开书、15 问开书问卷、大纲方案生成。
- **写作引擎**：章节生成、续写、重新生成、全文审查、一键修复。
- **知识增强**：支持记忆包、RAG 检索、上下文组装、写作指导和可选 embedding 设置。
- **质量闭环**：合规检查、章节摘要、追读力评分、记忆沉淀、提交记录、故事事件和债务追踪。
- **安全边界**：AI Key、接口地址和模型选择只来自前端本地设置，后端临时接收使用，不落库、不写日志。

### 用户、聊天和桌面端

- **用户系统**：邮箱验证码注册、昵称或邮箱登录、JWT 认证、路由守卫、历史记录和个人设置。
- **即时聊天**：支持私聊、群聊、会话列表、未读数、文件/图片消息、搜索和短时间撤回。
- **Tauri 2 桌面端**：Vue 3 + Element Plus 前端，支持 Windows NSIS / MSI 安装包构建。

---

## 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | 后端运行与构建 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.0+ | 验证码缓存 |
| Rust | 最新稳定版 | 仅打包桌面端需要 |

### 后端

仓库中的后端配置只能保存安全模板值。真实数据库密码、邮箱授权码、JWT 密钥、服务器地址和部署路径不要提交到 GitHub。

```bash
cd spring-server
mvn spring-boot:run
```

后端默认监听 `http://localhost:38765`。

### 前端

```bash
cd electron-app
npm install
cp .env.example .env
npm run dev
```

前端默认监听 `http://localhost:5173`。

### 桌面端打包

```bash
cd electron-app
npm run tauri build
```

### 后端 JAR 打包

```bash
cd spring-server
mvn clean package -DskipTests
```

生成的 `spring-server/target/cat-tool-0.2.0.jar` 用于服务器部署。该 JAR 可能包含本地写入的真实运行配置，不要提交仓库，不要上传到 GitHub Release。

---

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/health` | 健康检查 |
| `GET` | `/api/providers` | 获取 AI 提供商和模型列表 |
| `POST` | `/api/analyze` | 上传二进制文件进行 PE / 虚表 / 世界数组分析 |
| `POST` | `/api/tools/strings` | 字符串提取 |
| `POST` | `/api/tools/hex` | 十六进制查看 |
| `POST` | `/api/tools/export/json` | 导出 JSON 报告 |
| `POST` | `/api/tools/export/html` | 导出 HTML 报告 |
| `POST` | `/api/auth/login` | 用户登录 |
| `POST` | `/api/auth/register` | 用户注册 |
| `POST` | `/api/auth/send-code` | 发送邮箱验证码 |
| `GET` | `/api/novels/library` | 小说书库 |
| `PUT` | `/api/novels/project` | 保存小说工程 |
| `POST` | `/api/novels/chapters/{chapterId}/generate` | 章节生成 |
| `POST` | `/api/novels/rag/{bookId}/search` | 小说 RAG 搜索 |
| `GET` | `/api/chat/sessions` | 聊天会话 |
| `POST` | `/api/chat/upload` | 聊天文件上传 |

---

## 项目结构

```text
Unity/
├── spring-server/                    # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/jvxi/unity/
│       ├── controller/               # REST API
│       ├── model/                    # 数据模型
│       ├── novel/                    # 小说助手模块
│       └── service/                  # PE、虚表、AI、报告、工具服务
├── electron-app/                     # Tauri + Vue 3 前端
│   ├── package.json
│   ├── src-tauri/                    # Tauri Rust 配置与资源
│   └── src/
│       ├── api/                      # API 调用封装
│       ├── components/               # 通用组件
│       ├── stores/                   # Pinia 状态
│       ├── router/                   # Vue Router
│       └── views/                    # 页面视图
├── RELEASES.md
├── LICENSE
└── README.md
```

---

## 配置安全

以下内容不得提交或上传到 Release：

- 真实服务器地址、部署路径、公网 IP。
- 数据库密码、Redis 密码、邮箱授权码、JWT 密钥、AI Key。
- `electron-app/.env`、本地 `application-local.yml`、上传目录、日志目录、构建产物。
- 服务器运行 JAR、带真实配置的压缩包。

当前仓库通过 `.gitignore` 和本地 `skip-worktree` 保护真实配置。提交前仍应检查 `git diff --cached`，确认没有敏感信息。

---

## License

[MIT License](LICENSE)
