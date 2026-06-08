#!/bin/bash
# 猫爪工具 - 云服务器部署脚本
# 使用方法: chmod +x deploy.sh && ./deploy.sh

APP_NAME="cat-tool"
APP_VERSION="1.0.0"
JAR_FILE="-.jar"
DEPLOY_DIR="/opt/"
LOG_DIR="/var/log/"

echo "=== 猫爪工具部署脚本 ==="

# 1. 创建目录
echo "1. 创建部署目录..."
sudo mkdir -p 
sudo mkdir -p 

# 2. 复制JAR文件
echo "2. 复制JAR文件..."
sudo cp target/ /

# 3. 创建环境配置文件
echo "3. 创建环境配置..."
sudo tee /env.sh > /dev/null << 'EOF'
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=cat_tool
export DB_USER=root
export DB_PASS=your_password
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=
export JWT_SECRET=your_jwt_secret_base64
export JAVA_HOME=/usr/lib/jvm/java-21
EOF

# 4. 创建启动脚本
echo "4. 创建启动脚本..."
sudo tee /start.sh > /dev/null << 'EOF'
#!/bin/bash
source /opt/cat-tool/env.sh
cd /opt/cat-tool
nohup java -jar cat-tool-1.0.0.jar \
  --spring.profiles.active=prod \
  > /var/log/cat-tool/app.log 2>&1 &
echo $! > /opt/cat-tool/app.pid
echo "猫爪工具已启动，PID: "
EOF

sudo chmod +x /start.sh

# 5. 创建停止脚本
echo "5. 创建停止脚本..."
sudo tee /stop.sh > /dev/null << 'EOF'
#!/bin/bash
if [ -f /opt/cat-tool/app.pid ]; then
    PID=
    kill 32872
    rm /opt/cat-tool/app.pid
    echo "猫爪工具已停止"
else
    echo "未找到PID文件"
fi
EOF

sudo chmod +x /stop.sh

echo "=== 部署完成 ==="
echo "启动: /start.sh"
echo "停止: /stop.sh"
echo "日志: tail -f /app.log"