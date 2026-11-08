# RabbitMQ 接入方案：订单事件推送 + 延迟超时取消

> 写给当前项目的参考方案。基线：Spring Boot 4.1.1 / Spring AMQP 4.1.1（`spring-boot-starter-amqp`，版本由 Boot BOM 管理）/ Spring Cloud 2025.1.2 / Jackson 3（`tools.jackson`）/ RabbitMQ 4.x。
>
> 结论：**只引入两个 MQ 场景**——① order-service 把「订单状态 / 派单」推送改成事件，push-service 异步消费；② 用 RabbitMQ 的 **TTL + 死信交换机（DLX）** 实现订单超时取消，替代每分钟全表扫描。支付结算这类需要同步结果的链路保持 HTTP 不动，本方案不引入 outbox / Seata。

---

## 1. 现状（以代码为准）

| 链路 | 当前实现 | 位置 |
|---|---|---|
| 订单状态推送 | 同步 HTTP 调 push-service | `OrderService.pushAndReturn`（`order-service/.../service/OrderService.java:264-273`） |
| 派单广播 | `for` 循环逐司机同步 HTTP | `OrderService.dispatch`（`:88-114`） |
| 支付成功推送 | 同步 HTTP | `PaymentService.confirmPaid`（`order-service/.../service/PaymentService.java:92-105`） |
| 推送内部接口 | `POST /internal/push/user/{userId}` | `push-service/.../PushInternalController.java` |
| WebSocket 发送 | 单机 `SessionRegistry` + `PushService` | `push-service/.../PushService.java` |
| 超时取消 | `@Scheduled(fixedDelay = 60_000)` 扫描待接单 | `order-service/.../task/OrderTimeoutTask.java:25` |

现有做法的真实问题：

1. **推送与订单主流程同步耦合。** 虽然调用方 `catch` 了异常，但每次状态变更仍要等待一次 HTTP 往返；push-service 变慢会直接拖慢下单、接单接口。
2. **广播派单是 N 次串行 HTTP**（最多 10 个司机），下单接口耗时随司机数线性增长。
3. **推送无缓冲、无重试。** push-service 重启期间的推送直接丢失，失败只打一条 warn。
4. **超时取消是全表轮询。** 单机每分钟扫一次；多实例时 N 倍空扫，且取消时刻最多滞后一分钟。
5. **没有事件契约。** 以后加积分、行程分享、评价提醒等消费方，只能在 order-service 里继续堆同步调用。

---

## 2. 目标架构

```
order-service :8084
    │  ① 状态/派单变化 → PushCommand 事件
    │  ② 下单 → 延迟消息
    ▼
RabbitMQ
    ├─ ta.order.events (topic)
    │     └─ routingKey: push.command ──► ta.push.command ──► push-service :8085
    │                                        └─ 失败重试 3 次后 ──► ta.push.command.dead
    │
    └─ ta.order.timeout.delay (TTL=下单超时窗口，无消费者)
          └─ 过期后死信到 ta.order.dlx / order.timeout
                └─ ta.order.timeout ──► order-service 超时取消消费者
                      └─ 取消成功后再发 ① 的状态事件
```

核心原则：

- **MQ 只负责「通知」和「定时」，不负责「查询」。** 需要同步结果的操作（附近司机、支付扣款、用户信息）继续走 HTTP。
- **业务规则留在领域服务。** 「排除忙碌司机」「哪些用户该收到消息」仍由 order-service 决定；push-service 只负责投递。
- **消费端必须幂等。** RabbitMQ 是 at-least-once，重复投递是正常现象。

### 2.1 拓扑一览

| 名称 | 类型 | 参数 / 绑定 | 所属 |
|---|---|---|---|
| `ta.order.events` | topic exchange | durable | order-service 声明 |
| `ta.push.command` | queue | `x-dead-letter-exchange=ta.push.dlx`，`x-dead-letter-routing-key=push.command.dead` | push-service 声明 |
| `ta.push.command` 绑定 | binding | `ta.order.events` ← `push.command` | push-service |
| `ta.push.dlx` | direct exchange | durable | push-service |
| `ta.push.command.dead` | queue | 绑定 `ta.push.dlx` / `push.command.dead` | push-service |
| `ta.order.timeout.delay` | queue | `x-message-ttl=<超时窗口>`，`x-dead-letter-exchange=ta.order.dlx`，`x-dead-letter-routing-key=order.timeout`；**不挂消费者** | order-service |
| `ta.order.dlx` | direct exchange | durable | order-service |
| `ta.order.timeout` | queue | `x-dead-letter-exchange=ta.order.dlx`，`x-dead-letter-routing-key=order.timeout.dead`；绑定 `order.timeout` | order-service |
| `ta.order.timeout.dead` | queue | 绑定 `ta.order.dlx` / `order.timeout.dead` | order-service |

常量统一定义在 `api/.../mq/MqTopology.java`，两个服务引用同一份，避免名字漂移：

```java
package com.insipidknight.toarrive.api.mq;

public final class MqTopology {

    private MqTopology() {
    }

    // 场景一：订单事件
    public static final String ORDER_EVENTS_EXCHANGE = "ta.order.events";
    public static final String PUSH_COMMAND_ROUTING_KEY = "push.command";
    public static final String PUSH_COMMAND_QUEUE = "ta.push.command";
    public static final String PUSH_DLX = "ta.push.dlx";
    public static final String PUSH_DEAD_ROUTING_KEY = "push.command.dead";
    public static final String PUSH_DEAD_QUEUE = "ta.push.command.dead";

    // 场景二：超时取消
    public static final String ORDER_TIMEOUT_DELAY_QUEUE = "ta.order.timeout.delay";
    public static final String ORDER_DLX = "ta.order.dlx";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
    public static final String ORDER_TIMEOUT_QUEUE = "ta.order.timeout";
    public static final String ORDER_TIMEOUT_DEAD_ROUTING_KEY = "order.timeout.dead";
    public static final String ORDER_TIMEOUT_DEAD_QUEUE = "ta.order.timeout.dead";
}
```

---

## 3. 场景一：订单状态事件 → push-service

### 3.1 消息契约

`api` 模块新增（`api/src/main/java/com/insipidknight/toarrive/api/dto/PushCommand.java`）：

```java
package com.insipidknight.toarrive.api.dto;

/**
 * 一条推送命令：发给某个用户的一条 WS 消息。
 * 复用 WsMessage，push-service 不需要理解任何订单字段。
 */
public record PushCommand(Long userId, WsMessage message) {
}
```

为什么**一个接收人一条消息**，而不是一条消息带接收人列表：

- 派单时每个司机看到的 `pickupDistanceMeters` 不同，本来就得一人一份；
- 单条消息失败重试/进死信时，不会把其他接收人一起重发；
- 消费端逻辑最简单。

代价是派单最多产生 10 条小消息，对 RabbitMQ 来说可以忽略。

### 3.2 生产端改造

新增 `order-service/.../mq/PushEventPublisher.java`：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PushEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    void initCallbacks() {
        rabbitTemplate.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                log.error("MQ 未确认 correlation={}, cause={}", correlation, cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned ->
                log.error("MQ 消息无法路由 exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
    }

    /**
     * 推送属于旁路通知：发送失败只记日志，不阻断订单主流程。
     */
    public void send(Long userId, WsMessage message) {
        if (userId == null) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    MqTopology.ORDER_EVENTS_EXCHANGE,
                    MqTopology.PUSH_COMMAND_ROUTING_KEY,
                    new PushCommand(userId, message),
                    new CorrelationData(UUID.randomUUID().toString()));
        } catch (Exception e) {
            log.warn("推送事件发送失败 userId={}, type={}: {}", userId, message.type(), e.getMessage());
        }
    }
}
```

改造点：

| 位置 | 改法 |
|---|---|
| `OrderService` 构造注入 | `PushClient` → `PushEventPublisher` |
| `OrderService.pushAndReturn` | 去掉 `pushClient.sendToUser(...)`，改为 `pushEventPublisher.send(notifyUserId, new WsMessage(...))` |
| `OrderService.dispatch` | 循环内 `pushEventPublisher.send(candidate.driverId(), ...)` |
| `PaymentService.confirmPaid` | 同样换成 `PushEventPublisher` |
| `OrderServiceApplication` | 删除 `@ImportHttpServices(group = "push-service", types = PushClient.class)` |

`pushAndReturn` 改造后：

```java
private OrderVO pushAndReturn(Order order, Long notifyUserId, String type) {
    OrderVO vo = toOrderVO(order);
    pushEventPublisher.send(notifyUserId,
            new WsMessage(type, order.getOrderNo(), order.getStatus().name(), vo));
    return vo;
}
```

**发布时机**：必须在数据库状态写入成功之后发布。当前这些方法都不是 `@Transactional`（用原子条件 UPDATE 保证并发），按顺序写即可。如果以后给它们加事务，必须改成 `TransactionSynchronization#afterCommit` 或 `@TransactionalEventListener(phase = AFTER_COMMIT)`，否则会出现「消息已发、事务回滚」的脏事件。

### 3.3 消费端改造

`push-service` 新增拓扑声明：

```java
@Configuration
public class PushMqConfig {

    @Bean
    public TopicExchange orderEventsExchange() {
        return ExchangeBuilder.topicExchange(MqTopology.ORDER_EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange pushDlx() {
        return ExchangeBuilder.directExchange(MqTopology.PUSH_DLX).durable(true).build();
    }

    @Bean
    public Queue pushCommandQueue() {
        return QueueBuilder.durable(MqTopology.PUSH_COMMAND_QUEUE)
                .deadLetterExchange(MqTopology.PUSH_DLX)
                .deadLetterRoutingKey(MqTopology.PUSH_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding pushCommandBinding(@Qualifier("pushCommandQueue") Queue pushCommandQueue,
                                      @Qualifier("orderEventsExchange") TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(pushCommandQueue)
                .to(orderEventsExchange)
                .with(MqTopology.PUSH_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Queue pushDeadQueue() {
        return QueueBuilder.durable(MqTopology.PUSH_DEAD_QUEUE).build();
    }

    @Bean
    public Binding pushDeadBinding(@Qualifier("pushDeadQueue") Queue pushDeadQueue,
                                   @Qualifier("pushDlx") DirectExchange pushDlx) {
        return BindingBuilder.bind(pushDeadQueue).to(pushDlx).with(MqTopology.PUSH_DEAD_ROUTING_KEY);
    }
}
```

> 一个配置类里声明了多个 `Queue` / 交换机时，`Binding` 方法的参数要用 `@Qualifier` 指定 bean 名。项目根 pom 没有显式开启 `-parameters`，不要依赖「参数名 = bean 名」的隐式匹配。

JSON 转换器复用项目现有的 `JsonMapper`（Boot 4 自动装配，且已应用 Long → String 规则）：

```java
@Configuration
public class RabbitMessageConfig {

    @Bean
    public MessageConverter messageConverter(JsonMapper jsonMapper) {
        // Spring AMQP 4.x 的 Jackson 3 转换器；Jackson2JsonMessageConverter 已弃用
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
```

消费者：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PushCommandListener {

    private final PushService pushService;

    @RabbitListener(queues = MqTopology.PUSH_COMMAND_QUEUE)
    public void onPushCommand(PushCommand command) {
        // 不在监听器里吞异常：抛出后由容器重试，耗尽后进死信队列
        pushService.sendToUser(command.userId(), command.message());
    }
}
```

### 3.4 关键改动：让发送失败可以被重试

现在 `PushService.sendToUser` 把发送异常 catch 后仅打日志，如果直接接 MQ，监听器永远不会失败，重试/死信形同虚设。改法：

```java
public void sendToUser(Long userId, WsMessage message) {
    WebSocketSession session = sessionRegistry.get(userId);
    if (session == null || !session.isOpen()) {
        return;                       // 用户不在线不算失败，消息直接丢弃
    }
    try {
        String payload = jsonMapper.writeValueAsString(message);
        synchronized (session) {
            session.sendMessage(new TextMessage(payload));
        }
    } catch (Exception e) {
        throw new IllegalStateException("WebSocket 推送失败 userId=" + userId, e);
    }
}
```

语义划分：

| 情况 | 处理 |
|---|---|
| 用户不在线 / 会话已关闭 | 正常返回，消息不重投（实时通知没有补发价值） |
| 序列化失败 / IO 异常 | 抛异常 → 容器重试 → 进死信，便于排查 |

如果希望保留 `POST /internal/push` 作为过渡期回退路径，可先留着；稳定后删除 `PushInternalController` 与 `api` 的 `PushClient`，避免出现两条推送路径。

### 3.5 重复与顺序

- **重复投递**：WS 收到两条相同消息不会破坏状态，客户端可按 `orderNo + status` 去重。若某些消息不能重复（如支付成功），再加 Redis `mq:dedupe:{messageId}`（SETNX + 24h TTL），本项目当前不需要。
- **顺序**：单队列默认单消费者时基本有序；一旦某条消息重试，后一条可能先被消费。订单状态的最终权威仍是数据库，客户端展示最新状态即可。不要用 MQ 保证状态机顺序，状态机已经由原子 UPDATE 兜底。

---

## 4. 场景二：延迟消息做超时取消

### 4.1 为什么用 TTL + DLX

RabbitMQ 4.x **没有原生延迟消息**，只有两条路：

| 方案 | 说明 | 取舍 |
|---|---|---|
| **TTL + 死信交换机（推荐）** | 延迟队列设消息 TTL，过期后自动死信到消费队列 | 无需插件，标准功能；只适合固定延迟 |
| `rabbitmq_delayed_message_exchange` 插件 | 声明 `x-delayed-message` 交换机，按 `x-delay` 头延迟 | 语义最直观，但演示机要额外装插件，Docker 镜像默认未启用 |

本项目只需要一个固定的「待接单超时窗口」，选 TTL + DLX。

**必须用队列级 TTL（`x-message-ttl`），不要用逐消息 TTL。** RabbitMQ 只在消息到达队头时判断过期，如果前面有一条 TTL 更长的消息，后面的短 TTL 消息会被阻塞。队列级 TTL 下所有消息同寿命，天然按入队顺序过期。

### 4.2 拓扑声明（order-service）

```java
@Configuration
public class OrderMqConfig {

    @Bean
    public Queue orderTimeoutDelayQueue(
            @Value("${app.order.accept-timeout-minutes:10}") long timeoutMinutes) {
        return QueueBuilder.durable(MqTopology.ORDER_TIMEOUT_DELAY_QUEUE)
                .ttl((int) Duration.ofMinutes(timeoutMinutes).toMillis())
                .deadLetterExchange(MqTopology.ORDER_DLX)
                .deadLetterRoutingKey(MqTopology.ORDER_TIMEOUT_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange orderDlx() {
        return ExchangeBuilder.directExchange(MqTopology.ORDER_DLX).durable(true).build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(MqTopology.ORDER_TIMEOUT_QUEUE)
                .deadLetterExchange(MqTopology.ORDER_DLX)
                .deadLetterRoutingKey(MqTopology.ORDER_TIMEOUT_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderTimeoutBinding(@Qualifier("orderTimeoutQueue") Queue orderTimeoutQueue,
                                       @Qualifier("orderDlx") DirectExchange orderDlx) {
        return BindingBuilder.bind(orderTimeoutQueue)
                .to(orderDlx)
                .with(MqTopology.ORDER_TIMEOUT_ROUTING_KEY);
    }

    @Bean
    public Queue orderTimeoutDeadQueue() {
        return QueueBuilder.durable(MqTopology.ORDER_TIMEOUT_DEAD_QUEUE).build();
    }

    @Bean
    public Binding orderTimeoutDeadBinding(@Qualifier("orderTimeoutDeadQueue") Queue orderTimeoutDeadQueue,
                                           @Qualifier("orderDlx") DirectExchange orderDlx) {
        return BindingBuilder.bind(orderTimeoutDeadQueue)
                .to(orderDlx)
                .with(MqTopology.ORDER_TIMEOUT_DEAD_ROUTING_KEY);
    }
}
```

注意：延迟队列**不能挂 `@RabbitListener`**，否则消息会被直接消费而不是等待过期。

### 4.3 生产端：下单后投递延迟消息

`OrderService.createOrder` 落库后追加：

```java
orderMapper.insert(order);
dispatch(order);
publishTimeout(order.getOrderNo());
```

```java
private void publishTimeout(String orderNo) {
    try {
        // 发到默认交换机，routingKey 即队列名
        rabbitTemplate.convertAndSend("", MqTopology.ORDER_TIMEOUT_DELAY_QUEUE,
                new OrderTimeoutMessage(orderNo));
    } catch (Exception e) {
        // 投递失败时依赖兜底扫描，不影响下单
        log.warn("超时延迟消息发送失败 orderNo={}: {}", orderNo, e.getMessage());
    }
}
```

`OrderTimeoutMessage` 放在 `api` 的 dto 包：

```java
public record OrderTimeoutMessage(String orderNo) {
}
```

### 4.4 消费端：原子取消 + 再发状态事件

先抽出「取消单笔待接单订单」的方法，扫描兜底和 MQ 消费共用：

```java
public void cancelIfPending(String orderNo) {
    Order order = orderMapper.selectByOrderNo(orderNo);
    if (order == null) {
        return;                                  // 已逻辑删除
    }
    if (orderMapper.cancelBySystem(order.getId()) == 0) {
        return;                                  // 已被接单 / 已取消，幂等跳过
    }
    Order updated = requireOrder(orderNo);
    pushEventPublisher.send(updated.getPassengerId(),
            new WsMessage("ORDER_CANCELLED", orderNo, updated.getStatus().name(), new OrderVO(updated)));
    log.info("订单超时取消 orderNo={}", orderNo);
}
```

监听器：

```java
@Component
@RequiredArgsConstructor
public class OrderTimeoutListener {

    private final OrderService orderService;

    @RabbitListener(queues = MqTopology.ORDER_TIMEOUT_QUEUE)
    public void onTimeout(OrderTimeoutMessage message) {
        orderService.cancelIfPending(message.orderNo());
    }
}
```

**幂等锚点就是现有的 `cancelBySystem`**：

```sql
UPDATE orders SET status = 6, ... WHERE id = ? AND status = 1 AND delete_at = '1970-01-01 00:00:00'
```

影响 0 行说明订单已经不在待接单态（被接走、乘客取消、已删除），直接返回。重复投递、消息迟到、用户先取消后消息才到，都不会造成错误状态。

### 4.5 保留兜底扫描

延迟消息可能因为 broker 不可用、发布失败而丢失。业务上「订单永远挂在待接单」比「晚一分钟取消」严重得多，所以**不要删掉 `OrderTimeoutTask`**，改成低频兜底：

```java
@Scheduled(fixedDelay = 300_000)   // 5 分钟兜底；精确取消由 MQ 负责
public void cancelTimeoutOrders() {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(acceptTimeoutMinutes);
    orderService.cancelTimeoutOrders(cutoff);
}
```

`OrderService.cancelTimeoutOrders(cutoff)` 内部改为逐单调用 `cancelIfPending(orderNo)`，两条路径共用同一套原子取消 + 事件推送逻辑，重复执行天然安全。

这就是本方案对「消息可能丢」的回答：**MQ 提供时效，扫描提供兜底**，比引入本地消息表/outbox 更符合当前项目体量。

### 4.6 TTL 与配置的坑

- 队列参数**创建后不可修改**。`app.order.accept-timeout-minutes` 从 10 改成别的值，已存在的 `ta.order.timeout.delay` 不会更新 TTL，必须删除队列让它重建（或换一个带版本的名字，如 `ta.order.timeout.delay.15m`）。
- 延迟队列积压不会影响过期判定（没有消费者，消息只在队头过期出队），但如果消息量极大仍会占用内存/磁盘，演示环境无需担心。
- 延迟消息的 TTL 从**进入队列**开始计算，不是从发布开始。
- 消息体必须持久化（Spring AMQP 默认 `deliveryMode=PERSISTENT`），否则 broker 重启会丢延迟消息。

---

## 5. 可靠性配置

### 5.1 两个服务共用的 `application.yaml`

```yaml
spring:
  rabbitmq:
    host: ${ta.mq.host}
    port: ${ta.mq.port}
    username: ${ta.mq.username}
    password: ${ta.mq.password}
    virtual-host: /
    publisher-confirm-type: correlated   # 发布确认：broker 收到才回 ack
    publisher-returns: true              # 不可路由的消息回调给发布方
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: auto           # 监听方法正常返回才 ack，抛异常 nack
        prefetch: 10
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1s
          multiplier: 2
          max-interval: 10s
        default-requeue-rejected: false  # 重试耗尽后拒绝，按队列 DLX 进死信
```

`application-dev.yaml` 增加：

```yaml
ta:
  mq:
    host: localhost
    port: 5672
    username: ta
    password: ${RABBITMQ_PASSWORD}
```

### 5.2 各种「确认」的语义

| 机制 | 保证 | 不保证 |
|---|---|---|
| publisher confirm | broker 已收到并落盘/入队 | 消费者已处理 |
| publisher return | 消息至少被一个队列路由 | 消费成功 |
| consumer ack（auto） | 监听方法正常返回 | 业务副作用只执行一次 |
| 死信队列 | 失败消息可追溯、可人工重放 | 自动修复业务 |

RabbitMQ 的标准语义是 **at-least-once**，不是 exactly-once。所以：

- 推送重复 → 无害，客户端按 `orderNo + status` 忽略旧状态；
- 超时取消重复 → `cancelBySystem` 的 0 行判断兜底；
- 如果以后加入「发优惠券」这类不可重复的消费者，必须单独做去重表或 Redis 幂等键。

### 5.3 本地基础设施

仓库目前没有 `docker-compose.yml`，可以临时用：

```bash
docker run -d --name ta-rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=ta \
  -e RABBITMQ_DEFAULT_PASS=ta \
  rabbitmq:4.1-management
```

管理台：<http://localhost:15672>（账号 `ta/ta`），可在 Exchanges / Queues 页直接观察消息流入、死信和手动重发。

---

## 6. 改动清单

### 6.1 依赖

`order-service/pom.xml`、`push-service/pom.xml` 各加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

不需要额外指定版本，Boot BOM 管理（当前解析到 Spring AMQP 4.1.1）。

### 6.2 新增文件

| 模块 | 文件 | 作用 |
|---|---|---|
| `api` | `.../api/dto/PushCommand.java` | 推送命令消息体 |
| `api` | `.../api/dto/OrderTimeoutMessage.java` | 超时消息体 |
| `api` | `.../api/mq/MqTopology.java` | 交换机/队列/路由键常量 |
| order-service | `.../order/mq/PushEventPublisher.java` | 状态/派单事件发布 |
| order-service | `.../order/mq/OrderMqConfig.java` | 延迟队列 & 死信拓扑 |
| order-service | `.../order/mq/OrderTimeoutListener.java` | 超时取消消费者 |
| order-service | `.../order/mq/RabbitMessageConfig.java` | `JacksonJsonMessageConverter` |
| push-service | `.../push/PushMqConfig.java` | 推送队列 & 死信拓扑 |
| push-service | `.../push/PushCommandListener.java` | 推送消费者 |
| push-service | `.../push/RabbitMessageConfig.java` | 同一个 JSON 转换器 |

### 6.3 修改文件

| 文件 | 改动 |
|---|---|
| `order-service/.../service/OrderService.java` | 注入 `PushEventPublisher`、`RabbitTemplate`；`pushAndReturn` / `dispatch` 改为发事件；新增 `cancelIfPending`、`publishTimeout`；`cancelTimeoutOrders` 复用 `cancelIfPending` |
| `order-service/.../service/PaymentService.java` | `PushClient` → `PushEventPublisher` |
| `order-service/.../OrderServiceApplication.java` | 移除 `PushClient` 的 `@ImportHttpServices` |
| `order-service/.../task/OrderTimeoutTask.java` | `fixedDelay` 改为 5 分钟，注释说明是兜底 |
| `order-service/.../OrderMqConfig` 相关 yaml | 增加 `ta.mq.*`、`spring.rabbitmq.*`、`app.order.accept-timeout-minutes` |
| `push-service/.../PushService.java` | 发送异常改为抛出，让 MQ 能重试 |
| `push-service/.../application.yaml` | 增加 `spring.rabbitmq.*` |
| `push-service/.../PushInternalController.java` | 过渡期保留，稳定后删除 |
| `api/.../client/PushClient.java` | 过渡期保留，稳定后删除 |

### 6.4 测试

| 测试 | 覆盖点 |
|---|---|
| `PushEventPublisherTest` | mock `RabbitTemplate`，验证 exchange/routingKey/消息体；发送异常被吞掉不抛出 |
| `PushCommandListenerTest` | 正常调用推送；`PushService` 抛异常时监听器向上抛（触发重试） |
| `OrderTimeoutListenerTest` | `cancelBySystem` 返回 0 → 不推事件；返回 1 → 推 `ORDER_CANCELLED` |
| `PaymentServiceTest`（改造） | 用 `PushEventPublisher` mock 替换 `PushClient` |
| `PushServiceTest`（补充） | 发送异常时抛出；离线/关闭会话仍不抛 |

离线单测继续用 Mockito，不依赖 broker；如需真实链路验证，另起 `@SpringBootTest` + 本地 RabbitMQ。

---

## 7. 验证与演示脚本

1. 启动 RabbitMQ，打开管理台，确认两个 exchange、四条业务队列已声明。
2. 启动全部服务（先 push-service，确保推送队列存在）。
3. 乘客下单：
   - 管理台看到 `ta.order.events` 有消息流入；
   - 附近司机 WS 收到 `ORDER_DISPATCH`；
   - 延迟队列 `ta.order.timeout.delay` 中出现一条消息（10 分钟后消失）。
4. 司机接单：乘客 WS 收到 `ORDER_ACCEPTED`；10 分钟后延迟消息到达 `ta.order.timeout`，消费端 `cancelBySystem` 返回 0，无任何状态变化——**演示幂等**。
5. 新下一单不接单：把 `app.order.accept-timeout-minutes` 临时调成 1~2 分钟（记得删延迟队列让它重建），到点后：
   - 订单 `status=6`、`cancel_by=3`；
   - 乘客 WS 收到 `ORDER_CANCELLED`。
6. **缓冲演示**：停掉 push-service，让订单状态变化若干次；管理台看到消息在 `ta.push.command` 堆积；重启 push-service 后消息被消费，WS 恢复接收。
7. **死信演示**：在管理台手动向 `ta.push.command` 发一条格式非法的 JSON；重试 3 次后进入 `ta.push.command.dead`，业务线程不受影响。

---

## 8. 已知限制与刻意不做的事

| 项 | 说明 |
|---|---|
| 消息丢失 | 无 outbox：订单入库成功但发布失败时，状态推送会丢（旁路通知可接受）；超时消息丢失由 5 分钟兜底扫描弥补 |
| 恰好一次 | 不追求；靠消费端幂等（原子 UPDATE / 客户端去重） |
| 严格顺序 | 不保证；以数据库状态为权威 |
| 延迟精度 | TTL 到期判定依赖队头，固定 TTL 下接近精确，但不承诺毫秒级 |
| 多 TTL | 一个延迟队列只能一个固定 TTL；若以后要 10/15/30 分钟多档，按档位建多个延迟队列 |
| broker 高可用 | 演示用单节点；生产需镜像队列或 quorum queue，不在本轮范围 |
| MQ 替代支付调用 | 不做。支付需要同步结果，现有 PAYING + 补偿链路已经解决一致性问题 |
| 本地消息表 / Seata | 不做。对当前体量性价比低，思路留作面试讨论 |

---

## 9. 实施顺序

- [ ] **Phase 1：基础设施** —— 依赖、`ta.mq.*` 配置、JSON 转换器、`MqTopology` 常量；启动 RabbitMQ。
- [ ] **Phase 2：推送事件化** —— `PushEventPublisher` + push-service 队列/消费者；改造 `OrderService`、`PaymentService`；跑通下单/接单/派单三条推送。
- [ ] **Phase 3：删除同步路径** —— 移除 `PushClient` 注入与 `PushInternalController`（可保留一个版本回退）。
- [ ] **Phase 4：延迟取消** —— 拓扑声明、`publishTimeout`、`OrderTimeoutListener`、`cancelIfPending`；扫描任务降频为兜底。
- [ ] **Phase 5：验证与补测** —— 管理台演示缓冲/死信/幂等；补离线单测；更新 README 启动说明。

---

## 10. 面试可讲的点

- **为什么用 MQ**：把「通知」从订单主链路剥离，削峰、解耦、失败可重试；同步 HTTP 只保留需要即时结果的调用。
- **为什么不是 exactly-once**：RabbitMQ 只保证至少一次；一致性靠消费端幂等——超时取消复用 `WHERE status = 1` 的原子 UPDATE，重复消息影响 0 行。
- **为什么 TTL + DLX 而不是插件**：标准功能、零额外部署；清楚队列级 TTL 与逐消息 TTL 的队头阻塞区别。
- **消息丢了怎么办**：延迟消息用低频扫描兜底；旁路通知允许丢；如果需要强一致再上 outbox，但当前体量不值得。
- **为什么不用 MQ 做支付**：支付需要同步返回结果，异步化会把「扣款是否成功」变成新的分布式状态问题，现有 PAYING 状态机 + 补偿任务已经覆盖。
