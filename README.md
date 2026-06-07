# Unity - DLL VTable Analyzer

DLL 逆向分析工具，通过 PE 结构解析和 AI 智能分析，反向出 DLL 文件内部的虚表（vtable）地址信息。

## 功能特性

- **全面 PE 解析**：解析 DOS/PE 头、段表、Data Directory（16 个）、导出/导入表、调试信息、TLS 回调等
- **多策略虚表检测**：
  - RTTI Complete Object Locator 扫描
  - 连续代码指针数组扫描
  - 导出数据符号交叉引用
- **AI 智能分析**：支持多个 AI 提供商，辅助确认虚表、排除误报、推测类名
- **桌面客户端**：Electron + Vue 3 + Element Plus，左侧可折叠菜单栏

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端 | Java 21 + Spring Boot 3.2 |
| 前端 | Electron + Vue 3 + TypeScript + Element Plus |
| PE 解析 | 手写二进制解析（ByteBuffer） |
| AI 集成 | DeepSeek / 小米 MiMo（OpenAI 兼容格式） |

## 支持的 AI 模型

### DeepSeek

| 模型 ID | 说明 |
|---------|------|
| `deepseek-v4-flash` | 轻量快速，性价比高（推荐） |
| `deepseek-v4-pro` | 专业版，能力最强 |
| `deepseek-chat` | V3 legacy（将于 2026/07/24 弃用） |
| `deepseek-reasoner` | R1 legacy（将于 2026/07/24 弃用） |

### 小米 MiMo

| 模型 ID | 说明 |
|---------|------|
| `MiMo-V2.5` | 主模型，通用对话 |
| `mimo-v2.5-pro` | 专业版 |
| `MiMo-V2-Flash` | 轻量快速版 |
| `MiMo-V2-Pro` | legacy（将于 2026/06/30 弃用） |
| `MiMo-V2-Omni` | 多模态 legacy（将于 2026/06/30 弃用） |

## 项目结构

```
Unity/
├── spring-server/                # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/jvxi/unity/
│       ├── controller/           # REST API
│       ├── service/              # 业务逻辑
│       │   ├── pe/               # PE 解析器
│       │   ├── VtableDetectorService.java
│       │   └── DeepSeekService.java
│       └── model/                # 数据模型
│
├── electron-app/                 # Electron + Vue 3 前端
│   ├── package.json
│   ├── electron/main.ts          # Electron 主进程
│   └── src/
│       ├── layout/               # 布局组件（可折叠菜单）
│       ├── views/                # 页面视图
│       ├── components/           # 通用组件
│       ├── api/                  # API 调用
│       ├── stores/               # 状态管理
│       └── types/                # TypeScript 类型
│
└── README.md
```

## 快速开始

### 环境要求

- JDK 21
- Node.js 18+
- Maven 3.9+

### 启动后端

```bash
cd spring-server
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 启动前端

```bash
cd electron-app
npm install
npm run dev
```

### 使用流程

1. 启动后端和前端
2. 在设置页面配置 AI 提供商和 API Key
3. 进入"DLL 分析"页面，拖拽或选择 DLL 文件
4. 点击"开始分析"，等待 PE 解析和 AI 分析完成
5. 查看虚表分析结果

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/health` | 健康检查 |
| `GET` | `/api/providers` | 获取 AI 提供商和模型列表 |
| `POST` | `/api/analyze` | 上传 DLL 文件进行分析 |

### POST /api/analyze

**请求**：`multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | File | 是 | DLL 文件 |
| `apiKey` | String | 否 | AI API Key |
| `provider` | String | 否 | AI 提供商（默认 `deepseek`） |
| `model` | String | 否 | 模型 ID |

**响应**：

```json
{
  "success": true,
  "data": {
    "peInfo": { ... },
    "vtables": [
      {
        "rva": "0x82040",
        "va": "0x180082040",
        "functionCount": 5,
        "detectionMethod": "RTTI",
        "rttiTypeName": "MyClass",
        "functions": [ ... ]
      }
    ],
    "aiSummary": "该 DLL 包含 2 个确认虚表..."
  }
}
```

## PE 解析覆盖范围

- DOS Header、PE Signature、COFF File Header
- Optional Header（ImageBase、Subsystem、DLL Characteristics 安全标志）
- 16 个 Data Directory（Export/Import/Resource/Exception/Certificate/Relocation/Debug/TLS/LoadConfig/IAT/DelayImport/CLR 等）
- 全部 Section Header（RVA、大小、属性标志）
- 导出函数列表（名称、序号、RVA、Forwarder 检测）
- 导入函数列表（来源 DLL、函数名、Hint）
- 调试目录（CodeView PDB 路径、GUID、Age）
- TLS 回调地址
- 数字签名检测（Authenticode）

## License

MIT