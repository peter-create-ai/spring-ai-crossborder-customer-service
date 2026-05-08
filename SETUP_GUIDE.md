# OmniMerchant 项目环境配置与启动指南

## 一、环境准备

### 1.1 必需软件
| 软件 | 版本要求 | 下载地址 |
|------|---------|---------|
| Docker Desktop | 最新版 | https://www.docker.com/products/docker-desktop/ |
| JDK | 17+ | https://adoptium.net/ |
| Maven | 3.8+ | https://maven.apache.org/download.cgi |
| Node.js | 18+ | https://nodejs.org/ |

### 1.2 验证环境
```powershell
# 验证 Docker
docker --version
docker-compose --version

# 验证 Java
java -version

# 验证 Maven
mvn -version

# 验证 Node.js
node --version
npm --version
```

---

## 二、配置步骤

### 2.1 配置 AI API 密钥
编辑 `.env` 文件，填入你的 API 密钥：

```env
# 在 .env 文件中填入
OPENAI_API_KEY=sk-xxxxxx
ANTHROPIC_API_KEY=sk-ant-xxxxxx
DEEPSEEK_API_KEY=sk-xxxxxx
```

**获取 API Key**：
- OpenAI: https://platform.openai.com/account/api-keys
- Anthropic: https://console.anthropic.com/settings/keys
- DeepSeek: https://platform.deepseek.com/api_keys

---

## 三、启动中间件

### 3.1 启动 Docker 容器

在项目根目录执行：

```powershell
# 启动所有中间件（MySQL/Redis/PostgreSQL/RocketMQ）
docker-compose up -d
```

### 3.2 验证容器状态
```powershell
# 查看运行状态
docker-compose ps
```

应该看到以下容器健康状态为 `healthy`：
- `omni-mysql` (端口 3306)
- `omni-redis` (端口 6379)
- `omni-postgres` (端口 5432)
- `omni-rocketmq-namesrv` (端口 9876)
- `omni-rocketmq-broker`

### 3.3 初始化数据库

**初始化 MySQL 业务库**：
```powershell
# 方式 1：使用容器内 MySQL 客户端
docker exec -i omni-mysql mysql -uroot -proot123 < sql/db_main.sql

# 方式 2：使用 MySQL 工具（如 Navicat/DBeaver）连接 localhost:3306
# 用户名：root
# 密码：root123
# 数据库：omni_merchant
# 执行 sql/db_main.sql
```

**初始化 PostgreSQL 向量库**：
```powershell
# 使用 psql 执行
docker exec -i omni-postgres psql -U omnimerchant -d omni_merchant < sql/db_vector.sql
```

---

## 四、编译与启动后端

### 4.1 编译项目
```powershell
# 在项目根目录执行
mvn clean install -DskipTests
```

### 4.2 启动后端应用

#### 方式 1：命令行启动
```powershell
cd omni-merchant-bootstrap
mvn spring-boot:run
```

#### 方式 2：IDE 启动
在 IDEA/Eclipse 中运行：
`omni-merchant-bootstrap/src/main/java/com/omnimerchant/OmniMerchantApplication.java`

### 4.3 验证后端启动
访问：
- 健康检查：http://localhost:8090/api/health
- Druid 监控：http://localhost:8090/druid/
  - 用户名：admin
  - 密码：admin123

---

## 五、安装与启动前端

### 5.1 安装依赖
```powershell
cd omnimerchant-web
npm install
```

### 5.2 启动开发服务器
```powershell
npm run dev
```

### 5.3 访问前端
浏览器打开：http://localhost:5173

---

## 六、测试完整流程

### 6.1 访问管理后台
1. 打开 http://localhost:5173
2. 登录管理后台（在登录页选择 "管理后台"）
   - 用户名：admin
   - 密码：admin123

### 6.2 创建测试租户
1. 进入租户管理
2. 点击"创建租户"
3. 填入测试信息
4. 保存并获取租户编码

### 6.3 测试聊天
1. 使用租户编码登录聊天界面
2. 发送测试消息（如 "Where is my order?"）
3. 观察 AI 响应

---

## 七、常见问题排查

### 7.1 端口被占用
如果端口 3306/6379/5432/8090/5173 被占用：
```powershell
# 修改 docker-compose.yml 中的端口映射
# 修改 application-dev.yml 中的端口配置
```

### 7.2 Docker 启动失败
```powershell
# 查看容器日志
docker-compose logs mysql
docker-compose logs postgres
```

### 7.3 Maven 依赖下载慢
配置阿里云 Maven 镜像，编辑 `~/.m2/settings.xml`：
```xml
<mirrors>
    <mirror>
        <id>aliyun</id>
        <url>https://maven.aliyun.com/repository/public</url>
        <mirrorOf>*</mirrorOf>
    </mirror>
</mirrors>
```

### 7.4 AI 调用失败
检查：
1. API Key 是否正确
2. 网络连接是否正常
3. API 额度是否充足

---

## 八、停止服务

### 8.1 停止后端
在运行终端按 `Ctrl + C`

### 8.2 停止前端
在运行终端按 `Ctrl + C`

### 8.3 停止中间件
```powershell
# 停止但保留数据
docker-compose down

# 停止并删除数据
docker-compose down -v
```

---

## 九、开发建议

### 推荐 IDE
- 后端：IntelliJ IDEA
- 前端：VS Code

### 项目结构说明
```
omni-merchant/
├── omni-merchant-common/       # 通用模块（DTO、异常、工具类）
├── omni-merchant-tenant/       # 多租户管理
├── omni-merchant-agent/        # AI Agent 主模块
├── omni-merchant-intent/       # 意图识别
├── omni-merchant-knowledge/    # 知识库 RAG
├── omni-merchant-channel/      # 渠道对接
├── omni-merchant-message/      # 消息队列
├── omni-merchant-bootstrap/    # Spring Boot 启动入口
├── omnimerchant-web/          # Vue 3 前端
├── sql/                       # 数据库建表脚本
└── docker-compose.yml         # 中间件编排
```

---

## 十、下一步

配置完成后，你可以：
1. 体验核心聊天功能
2. 阅读源码了解架构
3. 根据需要修改代码
4. 部署到服务器（参考 docs/deployment.md）

祝你玩得愉快！🚀
