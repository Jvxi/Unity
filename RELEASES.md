# Releases

## v0.2.0 - 2026-06-09

### 新增
- 增加个人设置能力：头像上传、资料编辑、密码修改、主题和侧边栏偏好。
- 增加即时聊天能力：私聊、群聊、会话列表、未读数、文件/图片消息、消息搜索和短时间撤回。
- 增加桌面端图标资源更新，统一应用、安装包和平台图标展示。

### 打包产物
- 后端开源包：`cat-tool-server-0.2.0.zip`
- Windows 桌面安装包：`cat-tool-desktop-0.2.0-windows-x64-setup.exe`
- Windows MSI 安装包：`cat-tool-desktop-0.2.0-windows-x64.msi`
- 后端开源包用于开源共享，包含后端源码和构建文件，不包含服务器运行用 JAR
- 服务器运行 JAR 由本地写入真实配置后单独构建并上传，不作为 GitHub Release 附件发布

### 安全
- 移除前端本地 `.env` 文件的 Git 跟踪，并提供 `electron-app/.env.example`。
- 后端使用 JAR 内部 `application.yml`，仓库只提交安全模板值，真实数据库密码、邮箱授权码和 JWT 密钥不要提交。
- `.gitignore` 排除本地环境变量、上传目录、日志、构建目录和发布产物。

### 迁移说明
- 后端使用 JAR 内部 `application.yml`，部署服务器前在本地写入真实配置并重新打包 JAR。服务器 JAR 不作为 GitHub Release 附件发布。
- 真实数据库密码、邮箱授权码和 JWT 密钥不要提交到 GitHub。
- GitHub Release 不上传后端 JAR、前端静态 ZIP 或桌面绿色 ZIP。
