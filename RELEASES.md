# Releases

## v0.2.0 - 2026-06-09

### 新增
- 增加个人设置能力：头像上传、资料编辑、密码修改、主题和侧边栏偏好。
- 增加即时聊天能力：私聊、群聊、会话列表、未读数、文件/图片消息、消息搜索和短时间撤回。
- 增加桌面端图标资源更新，统一应用、安装包和平台图标展示。

### 打包产物
- 后端：`cat-tool-server-0.2.0.jar`
- 后端 ZIP：`cat-tool-server-0.2.0.zip`
- 前端静态资源 ZIP：`cat-tool-web-0.2.0.zip`
- 桌面端安装包：NSIS `.exe` 和 MSI `.msi`
- 桌面端绿色 ZIP：`cat-tool-desktop-0.2.0-windows-x64.zip`

### 安全
- 移除前端本地 `.env` 文件的 Git 跟踪，并提供 `electron-app/.env.example`。
- 后端敏感配置改为通过环境变量读取，包括数据库密码、邮箱授权码和 JWT 密钥。
- `.gitignore` 排除本地环境变量、上传目录、日志、构建目录和发布产物。

### 迁移说明
- 运行后端前必须配置 `CAT_TOOL_JWT_SECRET`，否则服务会拒绝生成或解析 JWT。
- 生产环境建议通过 `CAT_TOOL_DB_*`、`CAT_TOOL_MAIL_*`、`CAT_TOOL_REDIS_*` 和 `CAT_TOOL_UPLOAD_DIR` 注入私有配置。
