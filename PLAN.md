## 猫爪工具 - 综合性二进制分析助手

### 一、项目概述
将现有工具扩展为综合性二进制分析工具，命名为"猫爪工具"，使用可爱的小猫图标。

### 二、当前后端完成度: 70%

**已完成:**
- PE结构解析 (完整)
- VTable检测 (三种策略)
- AI集成 (DeepSeek、MiMo)
- REST API (health、providers、analyze)

**待完善:**
- 无本地存储
- 无历史记录
- 无批量分析
- 无报告导出

### 三、新增功能

#### 1. 历史记录系统
- SQLite存储分析记录
- 搜索、筛选、详情查看

#### 2. 字符串提取
- ASCII/Unicode字符串提取
- 按长度/类型过滤

#### 3. 十六进制查看器
- 分段高亮显示
- 搜索功能

#### 4. 报告导出
- PDF/HTML/JSON格式

### 四、界面改造

路由结构:
`
/           → 首页 (概览+快速操作)
/analysis   → DLL分析 (现有)
/strings    → 字符串提取
/hex        → 十六进制查看
/history    → 历史记录
/settings   → 设置
`

侧边栏: 🐱 猫爪工具 logo

### 五、技术建议

**保持现有技术栈 (Java + Vue)**
- 已完成70%，重写成本高
- Spring Boot生态成熟
- Vue 3 + Element Plus组件丰富
- 通过添加Java库扩展功能即可

新增依赖:
- SQLite: sqlite-jdbc
- PDF: OpenPDF
