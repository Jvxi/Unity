<div align="center">

# 🔍 Unity - DLL VTable Analyzer

**DLL 逆向分析工具 · PE 结构解析 · AI 智能分析 · 虚表地址提取**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/)
[![Vue.js](https://img.shields.io/badge/Vue.js-3-brightgreen.svg)](https://vuejs.org/)
[![Electron](https://img.shields.io/badge/Electron-30-lightgrey.svg)](https://www.electronjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.6-blue.svg)](https://element-plus.org/)

<br/>

[![GitHub stars](https://img.shields.io/github/stars/Jvxi/Unity?style=social)](https://github.com/Jvxi/Unity/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/Jvxi/Unity?style=social)](https://github.com/Jvxi/Unity/network/members)
[![GitHub issues](https://img.shields.io/github/issues/Jvxi/Unity)](https://github.com/Jvxi/Unity/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/Jvxi/Unity)](https://github.com/Jvxi/Unity/pulls)

</div>

---

## ✨ 功能特性

<table>
<tr>
<td width="50%">

### 📦 PE 全面解析
- DOS/PE 头、段表完整解析
- 16 个 Data Directory 全覆盖
- 导出/导入表详细提取
- 调试信息（PDB 路径、GUID）
- TLS 回调、数字签名检测

</td>
<td width="50%">

### 🎯 多策略虚表检测
- **RTTI 引导**：扫描 Complete Object Locator
- **指针扫描**：连续代码指针数组检测
- **导出引用**：导出符号交叉引用分析
- 自动 MSVC 类名 demangle

</td>
</tr>
<tr>
<td width="50%">

### 🤖 AI 智能分析
- 支持 **DeepSeek** 和 **小米 MiMo** 双平台
- 模型商自由切换
- 自动确认真实虚表
- 推测类名和函数用途

</td>
<td width="50%">

### 🖥️ 桌面客户端
- Electron + Vue 3 + TypeScript
- 左侧可折叠菜单栏
- 拖拽上传 DLL 文件
- 虚表结果可展开详情

</td>
</tr>
</table>

---

## 🚀 快速开始

### 环境要求

![JDK](https://img.shields.io/badge/JDK-21+-orange)
![Node.js](https://img.shields.io/badge/Node.js-18+-green)
![Maven](https://img.shields.io/badge/Maven-3.9+-blue)

### 1. 启动后端

```bash
cd spring-server
mvn spring-boot:run
```

> 后端默认运行在 `http://localhost:8080`

### 2. 启动前端

```bash
cd electron-app
npm install
npm run dev
```

### 3. 使用流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  配置 AI    │───▶│  上传 DLL   │───▶│  PE 解析    │───▶│  查看结果   │
│  API Key    │    │  拖拽/选择  │    │  + AI 分析  │    │  虚表详情   │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

---

## 🤖 支持的 AI 模型

### DeepSeek

| 模型 | 说明 | 状态 |
|------|------|------|
| `deepseek-v4-flash` | 轻量快速，性价比高 | ✅ 推荐 |
| `deepseek-v4-pro` | 专业版，能力最强 | ✅ 可用 |
| `deepseek-chat` | V3 通用对话 | ⚠️ 2026/07/24 弃用 |
| `deepseek-reasoner` | R1 深度推理 | ⚠️ 2026/07/24 弃用 |

### 小米 MiMo

| 模型 | 说明 | 状态 |
|------|------|------|
| `MiMo-V2.5` | 主模型，通用对话 | ✅ 推荐 |
| `mimo-v2.5-pro` | 专业版 | ✅ 可用 |
| `MiMo-V2-Flash` | 轻量快速版 | ✅ 可用 |
| `MiMo-V2-Pro` | 多模态版 | ⚠️ 2026/06/30 弃用 |
| `MiMo-V2-Omni` | 全能版 | ⚠️ 2026/06/30 弃用 |

---

## 📡 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/health` | 健康检查 |
| `GET` | `/api/providers` | 获取 AI 提供商和模型列表 |
| `POST` | `/api/analyze` | 上传 DLL 文件进行分析 |

<details>
<summary>📋 POST /api/analyze 详细参数</summary>

**请求**：`multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | File | ✅ | DLL 文件 |
| `apiKey` | String | ❌ | AI API Key |
| `provider` | String | ❌ | AI 提供商（默认 `deepseek`） |
| `model` | String | ❌ | 模型 ID |

**响应示例**：
```json
{
  "success": true,
  "data": {
    "peInfo": {
      "fileName": "example.dll",
      "machine": "AMD64",
      "imageBase": "0x180000000",
      "sections": [...],
      "exports": [...],
      "imports": [...]
    },
    "vtables": [
      {
        "rva": "0x82040",
        "va": "0x180082040",
        "functionCount": 5,
        "detectionMethod": "RTTI",
        "rttiTypeName": "MyClass",
        "functions": [...]
      }
    ],
    "aiSummary": "该 DLL 包含 2 个确认虚表..."
  }
}
```

</details>

---

## 🏗️ 项目结构

```
Unity/
├── spring-server/                    # 🔧 Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/jvxi/unity/
│       ├── controller/               # REST API 入口
│       ├── service/
│       │   ├── pe/                   # PE 解析器（手写二进制解析）
│       │   ├── VtableDetectorService # 虚表检测（三策略）
│       │   └── DeepSeekService       # AI 集成（多提供商）
│       └── model/                    # 数据模型
│
├── electron-app/                     # 🖥️ Electron + Vue 3 前端
│   ├── package.json
│   ├── electron/main.ts             # Electron 主进程
│   └── src/
│       ├── layout/MainLayout.vue    # 可折叠菜单布局
│       ├── views/                   # 页面视图
│       ├── components/              # 通用组件
│       ├── api/client.ts            # API 调用封装
│       ├── stores/settings.ts       # 状态管理
│       └── types/index.ts           # TypeScript 类型
│
├── LICENSE                          # MIT License
└── README.md
```

---

## 🔍 PE 解析覆盖范围

<details>
<summary>📂 完整解析清单</summary>

**文件头层**
- DOS Header（e_magic, e_lfanew）
- PE Signature
- COFF File Header（Machine, NumberOfSections, TimeDateStamp, Characteristics）

**Optional Header**
- Magic（PE32/PE32+）、ImageBase、SectionAlignment、FileAlignment
- Subsystem（WINDOWS_GUI/WINDOWS_CUI/NATIVE 等）
- DLL Characteristics（ASLR, DEP, CFG, High Entropy VA 等安全标志）

**16 个 Data Directory**
- [0] Export Table → 导出函数（名称、序号、RVA、Forwarder）
- [1] Import Table → 导入 DLL 列表 + 函数名/Hint/Thunk RVA
- [2] Resource Table
- [3] Exception Table
- [4] Certificate Table → 数字签名检测
- [5] Base Relocation Table
- [6] Debug Directory → CodeView PDB 路径/GUID/Age
- [7] Architecture
- [8] Global Ptr
- [9] TLS Table → TLS 回调地址列表
- [10] Load Config → Security Cookie, Guard CF
- [11] Bound Import
- [12] IAT
- [13] Delay Import
- [14] CLR Runtime Header
- [15] Reserved

**段表**
- 所有 Section：Name, VirtualAddress, VirtualSize, RawDataPtr, RawDataSize, Characteristics

</details>

---

## 📊 技术栈

<div align="center">

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Vue.js](https://img.shields.io/badge/Vue.js-35495E?style=for-the-badge&logo=vuedotjs&logoColor=4FC08D)
![TypeScript](https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white)
![Electron](https://img.shields.io/badge/Electron-2B2E3A?style=for-the-badge&logo=electron&logoColor=9FEAF9)
![Element Plus](https://img.shields.io/badge/Element_Plus-409EFF?style=for-the-badge&logo=element&logoColor=white)

</div>

---

## 📄 License

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐ Star 支持一下！**

![Star History](https://api.star-history.com/svg?repos=Jvxi/Unity&type=Date)

</div>