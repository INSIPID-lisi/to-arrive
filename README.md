# to-arrive

基于 Spring Cloud Alibaba 的网约车后端，覆盖乘客注册登录、司机入驻审核、下单接单、位置上报与附近司机、支付分账、WebSocket 实时推送、订单超时取消与评价。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 / 构建 | Java 21、Maven 3.9（自带 `mvnw`） |
| 基础框架 | Spring Boot 4.1.1、Spring Cloud 2025.1.2 |
| 微服务治理 | Spring Cloud Alibaba 2025.1.0.0、Nacos 注册/配置中心、Sentinel 熔断降级 |
| 网关 | Spring Cloud Gateway（WebFlux） |
| 数据访问 | MyBatis-Plus 3.5.13、MySQL 8.x |
| 缓存 / 会话 | Redis（JWT 会话、司机位置 GEO） |
| 实时通信 | WebSocket |
| 认证 | JWT（jjwt 0.12.6）+ 网关统一鉴权 |
| HTTP 客户端 | Spring RestClient + Apache HttpClient 5 连接池 |

## 模块与端口

| 模块 | 端口 | 职责 |
| --- | --- | --- |
| `gateway-service` | 8080 | 统一入口、JWT 鉴权、路由转发、WebSocket 代理、请求签名 |
| `user-service` | 8081 | 注册登录、用户资料、司机入驻与审核、上下线 |
| `location-service` | 8082 | 司机位置上报、附近司机查询（Redis GEO） |
| `payment-service` | 8083 | 钱包、充值、支付、司机入账与平台抽成 |
| `order-service` | 8084 | 订单状态机、派单、超时取消、支付对账、评价 |
| `push-service` | 8085 | WebSocket 连接管理、站内实时推送 |
| `api` | - | 跨服务 DTO、`@HttpExchange` 客户端契约 |
| `common-core` | - | `Result`、错误码、JWT、角色与签名工具 |
| `common-web` | - | MVC 配置、拦截器、全局异常、服务入口隔离、HTTP 客户端池 |
| `common-mybatis` | - | MyBatis-Plus 配置与字段自动填充 |

## 架构与请求链路

```text
Client
  │  HTTP / WebSocket
  ▼
gateway-service :8080
  ├── lb://user-service      :8081
  ├── lb://location-service  :8082
  ├── lb://payment-service   :8083
  ├── lb://order-service     :8084
  └── lb:ws://push-service   :8085
```

1. 网关统一鉴权：JWT + Redis 会话校验，仅登录/注册在白名单内。
2. 网关清除客户端伪造的 `X-User-*`，写入可信身份头，并对 `method + path + userId + timestamp` 做 HMAC 签名。
3. 下游校验网关签名，拒绝绕过网关的直连（401）；`/internal/**` 额外校验调用方签名与接口白名单（401/403）。
4. 服务间调用由 `RestClient` 拦截器自动签名，只有白名单内的调用方和目标路径可通过。
5. Nacos 负责注册发现与共享配置，Sentinel 对核心接口做熔断降级。

## 目录结构

```text
to-arrive/
├── api/                    # 跨服务契约
├── common-core/            # 基础工具与通用模型
├── common-web/             # Web 公共装配与入口隔离
├── common-mybatis/         # 持久层公共配置
├── gateway-service/        # 网关
├── user-service/           # 用户与司机
├── location-service/       # 位置服务
├── payment-service/        # 钱包与支付
├── order-service/          # 订单与评价
├── push-service/           # WebSocket 推送
└── nacos-config/           # 共享配置模板
```

## 快速开始

### 1. 环境依赖

- JDK 21、Maven 3.9+
- MySQL 8.x（默认端口 `3307`）
- Redis 7.x（默认 `6379`）
- Nacos 3.x（默认 `8848`）
- Sentinel Dashboard（默认 `8089`，可选）

### 2. 环境变量

```bash
# 必填
export DB_PASSWORD=<数据库/Redis 密码>
export JWT_DEV_SECRET=<JWT 密钥>
export GATEWAY_SIGNING_SECRET=<网关签名密钥>
export INTERNAL_SIGNING_SECRET=<服务间签名密钥>

# 可选，均有默认值
export NACOS_SERVER_ADDR=localhost:8848
export SENTINEL_DASHBOARD=localhost:8089
export REDIS_HOST=localhost
export REDIS_PORT=6379
export TA_DB_HOST=localhost
export TA_DB_PORT=3307
export TA_DB_USERNAME=root
```

### 3. 初始化数据库

创建三个库，并分别执行对应目录下的 SQL：

| 数据库 | 初始化脚本 |
| --- | --- |
| `user_db` | `user-service/src/main/resources/database/init/` |
| `order_db` | `order-service/src/main/resources/database/init/` |
| `payment_db` | `payment-service/src/main/resources/database/init/` |

### 4. 发布共享配置

在 Nacos 控制台新建配置：

- dataId：`ta-common.yaml`
- group：`TA_GROUP`
- 格式：`YAML`
- 内容：`nacos-config/ta-common.yaml`

### 5. 启动服务

```bash
# 首次先安装公共模块
./mvnw install -DskipTests

# 依次启动（建议先启动基础设施，再启动业务服务，最后启动网关）
./mvnw -pl user-service      spring-boot:run
./mvnw -pl location-service  spring-boot:run
./mvnw -pl payment-service   spring-boot:run
./mvnw -pl push-service      spring-boot:run
./mvnw -pl order-service     spring-boot:run
./mvnw -pl gateway-service   spring-boot:run
```

所有外部请求统一从 `http://localhost:8080` 进入。

## 主要接口

| 分组 | 前缀 | 说明 |
| --- | --- | --- |
| 认证 | `/api/auth/**` | 注册、登录、登出、注销、改密 |
| 用户 | `/api/user/**` | 个人资料查询与修改 |
| 司机 | `/api/driver/**` | 入驻申请、审核状态、上线/下线 |
| 管理端 | `/api/admin/**` | 司机审核、用户启停 |
| 位置 | `/api/location/**` | 位置上报、附近司机、司机位置 |
| 订单 | `/api/order/**` | 下单、接单、到达、开始、完成、取消、支付、评价 |
| 钱包 | `/api/wallet/**` | 余额查询、充值 |
| 实时推送 | `/ws` | WebSocket 连接，握手时校验 JWT |

`/internal/**` 仅供服务间调用，不通过网关暴露。

## 测试

```bash
./mvnw test
```

- 单元测试离线可跑。
- `@SpringBootTest` 集成测试需要 Nacos、MySQL、Redis 可用，并配置上述环境变量。
