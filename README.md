# OmniMerchant

跨境电商多语言 AI 客服 Agent 平台。基于 Spring AI ReAct Agent，提供 7×24 智能客服，支持订单查询、物流追踪、退换货政策 RAG、多语言翻译、人工升级。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.2.5 |
| AI | Spring AI (OpenAI / Anthropic / DeepSeek) | 1.0.0-M6 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL 8.0 / PostgreSQL 16 + pgvector | — |
| 缓存 | Redis 7 | — |
| 消息 | RocketMQ | 5.1 |
| 熔断 | Resilience4j | 2.2.0 |
| 前端 | Vue 3 + Element Plus + TypeScript | 3.5 / 2.9 / 5.7 |
| 构建 | Vite 6 / Maven 3.9 | — |

## 项目结构

```
omnimerchant/
├── omni-merchant-common/       # 共享模块：DTO、异常、JWT 工具、TraceId
├── omni-merchant-tenant/       # 租户管理：CRUD、多租户上下文、拦截器
├── omni-merchant-agent/        # Agent 核心：ReAct、工具调用、模型路由、限流、计费
├── omni-merchant-intent/       # 意图识别模块
├── omni-merchant-knowledge/    # 知识库：RAG 混合检索、文档管理、向量索引
├── omni-merchant-channel/      # 渠道接入模块
├── omni-merchant-message/      # 消息模块：RocketMQ 消费 Token 用量
├── omni-merchant-bootstrap/    # 启动模块：配置、过滤器、全局异常处理
├── omnimerchant-web/           # Vue 3 前端：聊天、管理后台
├── sql/                        # 建表脚本（MySQL + PGVector）
├── docker-compose.yml          # 本地开发中间件
└── Dockerfile                  # 后端多阶段构建
```

## 快速启动

### 环境要求

- Java 17+
- Maven 3.8+
- Docker Desktop
- OpenAI API Key（必须），Anthropic / DeepSeek Key（可选）

### 1. 启动中间件

```bash
docker-compose up -d
```

启动 MySQL、Redis、PostgreSQL (pgvector)、RocketMQ (namesrv + broker + dashboard)。

### 2. 初始化数据库

```bash
# MySQL 建表
docker exec -i omni-mysql mysql -uomnimerchant -pomnimerchant123 omni_merchant < sql/db_main.sql

# PGVector 建表
docker exec -i omni-postgres psql -U omnimerchant -d omni_merchant < sql/db_vector.sql
```

### 3. 配置环境变量

```bash
export OPENAI_API_KEY=sk-your-key
export ANTHROPIC_API_KEY=sk-your-key      # 可选
export DEEPSEEK_API_KEY=sk-your-key       # 可选
```

### 4. 启动后端

```bash
mvn compile -pl omni-merchant-bootstrap -am
mvn spring-boot:run -pl omni-merchant-bootstrap -Dspring-boot.run.profiles=dev
```

应用默认运行在 `http://localhost:8090`。

### 5. 启动前端（可选）

```bash
cd omnimerchant-web
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，API 请求自动代理到 `localhost:8090`。

### 6. 验证

```bash
# 健康检查
curl http://localhost:8090/api/health

# 管理员登录
curl -X POST http://localhost:8090/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@omnimerchant.com","password":"admin123"}'

# 测试对话
curl -X POST http://localhost:8090/api/test/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{"message":"hello"}'
```

## API 概览

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| POST | `/api/admin/login` | 管理员登录，返回 JWT |

### 管理接口（需 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/tenants` | 租户列表 |
| POST | `/api/tenants` | 创建租户 |
| GET/PUT/DELETE | `/api/tenants/{id}` | 租户详情/更新/删除 |
| GET | `/api/knowledge/docs` | 知识文档列表 |
| POST | `/api/knowledge/docs` | 创建文档 |
| GET/PUT/DELETE | `/api/knowledge/docs/{docUuid}` | 文档详情/更新/删除 |
| GET | `/api/conversations` | 会话列表 |
| GET | `/api/conversations/{uuid}` | 会话详情 |
| GET | `/api/conversations/{uuid}/messages` | 会话消息回放 |
| GET | `/api/billing/usage` | 当月用量 |
| GET | `/api/billing/usage/range` | 按日期范围查询用量 |

### 对话接口（X-Tenant-Id 头）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/stream` | SSE 流式对话（核心接口） |
| POST | `/api/test/chat` | 测试对话（非流式） |

## 核心能力

**ReAct Agent** — 思考-行动-观察循环，自动调用工具获取真实数据，最多 5 轮迭代。置信度低于 75% 或金额争议 > $100 自动升级人工。

**6 大业务工具** — `queryOrder`（订单查询）、`trackLogistics`（物流追踪）、`refundPolicyRAG`（退换货政策）、`productSearchRAG`（商品搜索）、`translate`（多语言翻译）、`escalateToHuman`（人工升级）。

**混合检索 RAG** — HNSW 向量检索 + BM25 关键词检索 + RRF 融合 + Cross-Encoder BGE Reranker 重排序。支持退换货政策和商品信息双知识库。

**多语言** — Lingua 自动识别 12 种语言，中转英语处理（非英语→翻译为英语→LLM 处理→翻译回原语言），降低 Token 成本。

**模型路由** — 根据意图和复杂度自动选择模型：简单请求走 gpt-4o-mini（低成本），中等复杂度走 claude-haiku-4-5（降级），兜底走 deepseek-chat（最低成本）。

**三层限流** — Token 速率限制（Redis Lua 令牌桶）→ 模型并发限制（信号量）→ 熔断降级（Resilience4j），Redis 故障时自动放行保证可用性。

**多租户隔离** — X-Tenant-Id 请求头 + MyBatis-Plus TenantLineInnerInterceptor 自动注入 WHERE tenant_id = ?。PGVector 查询手动带 tenant_id。异步任务通过 TransmittableThreadLocal 传递租户上下文。

## 配置参考

核心配置项（`application-dev.yml`）：

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/omni_merchant?...
      username: omnimerchant
      password: omnimerchant123
  data.redis:
    host: localhost
    port: 6379
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat.options: {model: gpt-4o-mini, temperature: 0.3}
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat.options: {model: claude-haiku-4-5-20251001, temperature: 0.3}

omnimerchant:
  llm.deepseek:
    api-key: ${DEEPSEEK_API_KEY}
    base-url: https://api.deepseek.com
    model: deepseek-chat
  knowledge.reranker:
    url: http://localhost:8001/rerank
    timeout-seconds: 5

admin:
  email: ${ADMIN_EMAIL:}
  password: ${ADMIN_PASSWORD:}
  jwt-secret: ${JWT_SECRET:...}
```

完整配置参见 `omni-merchant-bootstrap/src/main/resources/application-dev.yml`。

## Docker 部署

```bash
# 构建并启动全部服务（后端 + 前端 + 中间件）
export ADMIN_PASSWORD=your-password
export JWT_SECRET=your-256-bit-secret
export OPENAI_API_KEY=sk-xxx
docker-compose up -d --build
```

服务端口：
- 前端：`80` / `443`
- 后端：`8090`
- MySQL：`3306`
- Redis：`6379`
- PostgreSQL：`5432`
- RocketMQ Dashboard：`18080`

## License

MIT
