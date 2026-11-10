# Rest Client 远程接口调用自动重试

> 基于 `RestTemplate(Spring Boot2)/RestClient(Spring Boot3)` + Resilience4j 的 **HTTP 客户端声明式自动重试** Spring Boot 2/3 Starter

---

## 目录

- [1. 组件简介](#1-组件简介)
- [2. 快速开始](#2-快速开始)
- [3. 配置项说明](#3-配置项说明)
- [4. 使用方式](#4-使用方式)
- [5. 重试策略配置（Resilience4j）](#5-重试策略配置resilience4j)
- [6. 高级用法](#6-高级用法)
- [7. 完整配置示例](#7-完整配置示例)
- [8. 常见问题与注意事项](#8-常见问题与注意事项)

---

## 1. 组件简介

`retryclient-spring-boot[2/3]-starter` 解决的是微服务/分布式场景下 **HTTP 调用失败自动重试** 的问题。

引入依赖后，所有 `RestTemplate/RestClient` 的请求都会自动经过一个重试拦截器，按照你在配置文件里声明的策略（策略名、错误处理器、路径匹配）进行重试；命中 `gx.retryclient.mapping` 的请求使用指定策略，未命中的请求统一使用名为 `default` 的策略。

<font style="color:#FF0000;font-weight: bold;">特别说明：要想使用的 RestTemplate / RestClient 能够根据规则自动重试，你必须将其注册为Bean</font>

### 1.1 核心能力

| 能力 | 说明 |
| --- | --- |
| **声明式重试** | 无需手写重试循环，配置 `resilience4j.retry` 即可 |
| **按路径/方法精细控制** | 通过 `gx.retryclient.mapping` 把「HTTP 方法 + 路径」映射到重试策略名，并可单独指定错误处理器 |
| **指定接口关闭重试** | mapping 的 `name` 设为 `none` 或 `disabled`，命中该规则时直接发起请求、不重试 |
| **错误处理器可插拔** | 全局 `gx.retryclient.retry-handler-class` 或 mapping 级的 `retry-handler-class`，由状态码决定是否抛出异常触发重试 |
| **统一错误处理** | 默认处理器把 4xx/5xx 响应转为 `RetryClientException`，从而触发重试 |
| **响应体可重复读** | `repeatread=true` 时自动包装响应，解决 InputStream 只能读一次的问题 |
| **可选超时统一配置** | `gx.retryclient.timeout.*` 自动作用于容器自动创建的 `RestClient.Builder` |
| **零侵入** | 业务代码只需注入 `RestClient`，写法与普通 RestClient 完全一致 |

### 1.2 技术栈

| 项目 | 版本/说明 |
| --- | --- |
| Java | **8 / 21** |
| Spring Boot | **3.x** |
| Resilience4j | **2.3.0 / 2.4.0** |

### 1.3 自动配置开关

组件由 `gx.retryclient.enabled` 控制，**默认开启**（`matchIfMissing = true`）：

- 设为 `true` 或不配置 → 启用自动重试
- 设为 `false` → 关闭整个组件（所有 Bean 均不注册）

---

## 2. 快速开始

### 2.1 引入依赖

```xml
<!--Spring Boot2-->
<dependency>
    <groupId>net.greatsoft.component</groupId>
    <artifactId>retryclient-spring-boot2-starter</artifactId>
    <version>1.0.0</version>
</dependency>
<!--Spring Boot3-->
<dependency>
    <groupId>net.greatsoft.component</groupId>
    <artifactId>retryclient-spring-boot3-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2.2 添加 Resilience4j 重试策略

组件本身**不包含**重试策略定义，必须在你的 `application.yml` 中声明。未命中 `mapping` 的请求会使用名为 `default` 的策略，因此至少要提供 `configs.default`（否则运行时找不到策略会抛异常，见 [第 8 节](#8-常见问题与注意事项)）：

```yaml
resilience4j:
  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 2s
        fail-after-max-attempts: true
```

### 2.3 注入并使用 RestClient / RestTemplate

```java
@Service
public class UserService {

  private final RestClient restClient;

  public UserService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Map<String, Object> queryUser() {
    return restClient.get()
        .uri("/api/query/{id}", 666)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }
}
@Service
public class UserService {

  private final RestTemplate restTemplate;

  public UserService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> createUser2() {
    return this.restTemplate.getForObject("/api/query/{id}", Map.class, 666) ;
  }
}
```

就这么多 —— 请求失败时会**自动重试**。

---

## 3. 配置项说明

### 3.1 组件配置（`gx.retryclient.*`）

由 `RetryProperties` 绑定，支持 IDEA 配置提示（`spring-boot-configuration-processor` 已启用）。

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `gx.retryclient.enabled` | Boolean | `true` | 组件总开关，`false` 时关闭全部自动配置 |
| `gx.retryclient.mapping` | Map<String, RetryConfig> | `{}` | **路径 → 重试配置**映射，key 格式 `方法:路径`，value 含 `name`（必填）与 `retry-handler-class`（可选），见 3.2 |
| `gx.retryclient.repeatread` | Boolean | `false` | 是否开启**响应体可重复读**（强烈建议开启，见 3.3） |
| `gx.retryclient.retry-handler-class` | Class<? extends RetryClientErrorHandler> | `DefaultRetryClientErrorHandler` | **全局**错误处理器实现类，mapping 未单独指定时使用，见 3.4 |
| `gx.retryclient.timeout.enabled` | Boolean | `false`（仅显式设为 `true` 才生效） | 是否启用统一超时配置;Spring Boot2不支持 |
| `gx.retryclient.timeout.connect-timeout` | Long | `1000` | 连接超时（毫秒）;Spring Boot2不支持 |
| `gx.retryclient.timeout.read-timeout` | Long | `1000` | 读取超时（毫秒）;Spring Boot2不支持 |

> ⚠️ `mapping` 的 value 已由「策略名字符串」改为 `RetryConfig` 对象，**必须**用 `name` 属性指定策略名。旧写法 `'[get:/api/query/{id}]': queryuser` 无法绑定为 `RetryConfig`，启动时即会报配置绑定错误。

### 3.2 mapping 的 key / value 格式（重要）

key 必须严格为 **`方法:路径`** 两段，用**第一个冒号**分隔；value 为 `RetryConfig` 对象：

```yaml
gx:
  retryclient:
    mapping:
      '[get:/api/query/{id}]':          # ✅ GET 请求匹配 /api/query/{id}
        name: queryuser                 # 必填：Resilience4j 策略实例名
      '[post:/api/user]':               # ✅ POST 请求匹配 /api/user
        name: usercreate
        retry-handler-class: com.pack.UserCreateErrorHandler   # 可选：该路径专用错误处理器
```

**key 的三条规则**：

1. **key 只能包含一个冒号**。路径不能包含完整的URI
2. **path 支持 Ant 风格通配符**（底层用 `AntPathMatcher`）：
   - `/**` 匹配任意层级
   - `/*` 匹配单层级
   - `?` 匹配单字符
   - `{id}` 匹配路径变量
3. **方法名不区分大小写**（源码 `equalsIgnoreCase`），但建议统一小写。

**value 的三条规则**：

1. **`name` 必填**，对应 Resilience4j 的策略实例：优先使用 `resilience4j.retry.instances.<name>`；若未单独定义，则按 `configs.default` 自动创建；留空会在请求时抛异常。
2. **`name: none` / `name: disabled`**（不区分大小写）表示命中该规则时**不重试**，直接执行请求。
3. **`retry-handler-class` 可选**，用于覆盖全局处理器；指定的类**必须是 Spring Bean**（见 3.4）。

**`RetryConfig` 属性一览**（`RetryProperties.RetryConfig`）：

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `name` | String | 无，**必填** | Resilience4j 重试策略名。优先匹配 `resilience4j.retry.instances.<name>`，未定义时按 `configs.default` 自动创建；值为 `none`/`disabled` 时跳过重试；留空则请求时抛异常 |
| `retry-handler-class` | Class&lt;? extends RetryClientErrorHandler&gt; | `null`（沿用全局 `gx.retryclient.retry-handler-class`，最终默认 `DefaultRetryClientErrorHandler`） | 该路径专用的错误处理器实现类，优先级高于全局配置；**必须是已注册的 Spring Bean**，否则请求时抛 `NoSuchBeanDefinitionException` |

对应的 Java 定义：

```java
public static class RetryConfig {
    private String name ;                                              // 策略名
    private Class<? extends RetryClientErrorHandler> retryHandlerClass ; // 可选，路径级处理器
}
```

> 属性名采用 Spring Boot 宽松绑定，YAML 中写 `retry-handler-class`，对应 Java 字段 `retryHandlerClass`。

**未匹配任何规则时**，请求使用名为 `default` 的策略，也就是说 `resilience4j.retry.configs.default`（或 `instances.default`）是必需的。

> 注意：规则内部存放在 `HashMap` 中，若多条规则同时匹配同一请求，最终命中哪一条是不确定的，应避免规则重叠。

### 3.3 repeatread（响应体可重复读）

这是**最容易被忽略但影响最大**的开关。

原因：重试拦截器发现 4xx/5xx 时，会把响应体读出来交给错误处理器：

```java
String responseBody = StreamUtils.copyToString(httpResponse.getBody(), StandardCharsets.UTF_8) ;
```

`InputStream` **只能读一次**。若不开启 `repeatread`，错误响应体已被拦截器消费；当自定义处理器未抛异常、把响应交还业务方时，`body(...)` 将拿到**空内容**。

开启后，组件会追加一个 `StreamClientHttpRequestInterceptor`，它把响应体缓存为 `byte[]`（`ClientHttpResponseWrapper`），从而实现多次读取：

```yaml
gx:
  retryclient:
    repeatread: true      # ✅ 推荐始终开启
```

### 3.4 错误处理器（retry-handler-class）

错误处理器负责「看到 4xx/5xx 后是否抛异常」，**只有抛出异常才会触发重试**。组件选择处理器的优先级：

1. mapping 命中规则中的 `retry-handler-class`
2. 全局 `gx.retryclient.retry-handler-class`（默认 `DefaultRetryClientErrorHandler`）

拦截器通过 `ApplicationContext.getBean(处理器类)` 获取实例，因此：

- 默认处理器 `DefaultRetryClientErrorHandler` 已由组件注册为 Bean；
- **自定义处理器必须自行注册为 Bean**，且配置的类名要与 Bean 的实际类型一致，否则请求时会抛 `NoSuchBeanDefinitionException`。

```java
public class UserCreateErrorHandler implements RetryClientErrorHandler {
  @Override
  public void handle(String responseBody, HttpHeaders headers, HttpStatusCode statusCode) {
    if (statusCode.is5xxServerError()) {
      throw new RetryClientException("服务端异常: " + responseBody);  // 5xx 才重试
    }
    // 4xx 不抛异常 → 不重试，原响应返回业务方
  }
}

@Configuration
public class HandlerConfig {
  @Bean
  UserCreateErrorHandler userCreateErrorHandler() {
    return new UserCreateErrorHandler();
  }
}
```

```yaml
gx:
  retryclient:
    retry-handler-class: com.pack.GlobalErrorHandler          # 全局生效
    mapping:
      '[post:/api/user]':
        name: usercreate
        retry-handler-class: com.pack.UserCreateErrorHandler  # 仅该路径覆盖
```

### 3.5 timeout 的作用范围（重要）

仅对Spring Boot3支持；

---

## 4. 使用方式

### 4.1 Spring Boot3

方式1：

```java
@Bean
RestClient restClient(RestClient.Builder builder) {
  return builder.baseUrl("http://localhost:7000")
      .build() ;
}
```

方式2：

```java
@Bean
RestClient restClient() {
  return RestClient.builder()
      .requestFactory(requestFactory)
      .baseUrl("http://localhost:7000")
      .build() ;
}
```

方式3：

```java
@Bean
RestClient restClient() {
  return RestClient.create("http://localhost:7000") ;
}
```

如上3种方式注册的Bean，系统都能自动的进行失败重试。

### 4.2 Spring Boot2

方式1：

```java
@Bean
RestTemplate restTemplate(RestTemplateBuilder b) {
  return b.rootUri("http://localhost:7000")
      .build() ;
}
```

方式2：

```java
@Bean
RestTemplate restTemplate() {
  return new RestTemplate() ;
}
```

方式3：

```java
@Bean
RestTemplate restTemplate() {
  RestTemplateBuilder b = new RestTemplateBuilder() ;
  return b.rootUri("http://localhost:7000")
      .build() ;
}
```

---

## 5. 重试策略配置（Resilience4j）

### 5.1 配置结构

```yaml
resilience4j:
  retry:
    configs:              # 策略模板（可复用）
      default:
        max-attempts: 3
        wait-duration: 2s
        fail-after-max-attempts: true
        ignore-exceptions:
          - java.net.http.HttpTimeoutException
          - java.net.SocketTimeoutException
    instances:            # 具体策略实例（mapping 中的 name 必须指向这里）
      queryuser:
        base-config: default
```

### 5.2 关键参数

| 参数 | 说明 |
| --- | --- |
| `max-attempts` | **总尝试次数**（含首次）。设为 3 = 首次 + 2 次重试 |
| `wait-duration` | 每次重试前的等待时间（ISO-8601，如 `500ms`、`2s`） |
| `fail-after-max-attempts` | `true`：达到上限后抛出 `MaxRetriesExceededException`（组件会再包装为 `RuntimeException` 抛出，`cause` 链中可拿到 `RetryClientException`）；`false`：返回最后一次结果 |
| `retry-exceptions` | 仅对这些异常重试 |
| `ignore-exceptions` | 对这些异常**不重试**（如超时异常，避免无意义重试） |
| `retry-on-exception` | 自定义谓词，灵活判断 |
| `interval-function` | 指数/函数式退避，替代固定 `wait-duration` |
| `base-config` | 实例继承模板，实现配置复用 |

---

## 6. 高级用法

### 6.1 自定义错误处理器

默认处理器 `DefaultRetryClientErrorHandler` 在 4xx/5xx 时抛 `RetryClientException`：

```java
if (statusCode.is4xxClientError() || statusCode.is5xxServerError()) {
    throw new RetryClientException(getErrorMessage(statusCode.value(), responseBody));
}
```

如需自定义（例如对 404 不重试、或解析业务错误码），实现 `RetryClientErrorHandler`、注册为 **Spring Bean**，再通过配置指定：

```java
public class NoRetryOn4xxHandler implements RetryClientErrorHandler {
  @Override
  public void handle(String responseBody, HttpHeaders headers, HttpStatusCode statusCode) {
    if (statusCode.is5xxServerError()) {
      // 5xx 才重试
      throw new RetryClientException("服务端异常: " + responseBody);
    }
    // 4xx 直接放行，不重试
  }
}

@Bean
NoRetryOn4xxHandler noRetryOn4xxHandler() {
  return new NoRetryOn4xxHandler();
}
```

```yaml
gx:
  retryclient:
    retry-handler-class: com.pack.NoRetryOn4xxHandler   # 全局生效
    # 也可在 mapping 的某条规则里指定，仅对该路径生效
```

<font style="color:#FF0000;font-weight: bold;">注意：① 必须抛出异常才能重试；② 处理器是按具体类从容器中查找的，必须注册为 Bean（不能用一个匿名类 / lambda 形式的 `RetryClientErrorHandler` Bean 代替）。</font>

### 6.2 组合使用 RestClient 拦截器

组件**不排斥**你自定义拦截器。二者在容器路径下是**叠加**关系：

```java
@Bean
RestClient restClient(RestClient.Builder builder) {
  return builder
      .baseUrl("http://localhost:7000")
      .requestInterceptor((request, body, execution) -> {
        // 你的自定义拦截器（如加签、埋点）
        return execution.execute(request, body);
      })
      .build();
}
```

---

## 7. 完整配置示例

以下示例可直接作为业务项目 `application.yml` 的模板：

```yaml
# ============ Resilience4j 重试策略（必须） ============
resilience4j:
  retry:
    configs:
      default:
        max-attempts: 3                 # 共 3 次（含首次）
        wait-duration: 2s               # 每次间隔 2 秒
        fail-after-max-attempts: true   # 超限抛异常
        ignore-exceptions:              # 超时类不重试
          - java.net.http.HttpTimeoutException
          - java.net.SocketTimeoutException
    instances:
      queryuser:
        base-config: default            # 继承 default
      # 可继续扩展更多策略…

# ============ retryclient 组件配置 ============
gx:
  retryclient:
    enabled: true                       # 总开关（默认 true）
    repeatread: true                    # ✅ 响应体可重复读，推荐开启
    mapping:
      '[get:/api/query/{id}]':              # GET 请求 → queryuser 策略
        name: queryuser
      # 单独关闭某接口的重试：
      # '[get:/api/health]':
      #   name: none
      # 单独指定错误处理器：
      # '[post:/api/user]':
      #   name: usercreate
      #   retry-handler-class: com.pack.UserCreateErrorHandler
    # 全局错误处理器（可选，默认 DefaultRetryClientErrorHandler）
    # retry-handler-class: com.pack.GlobalErrorHandler
    timeout:
      enabled: true                     # 启用统一超时
      connect-timeout: 2000             # 连接超时 2s
      read-timeout: 3000                # 读取超时 3s
```

### 7.1 配套 Java 代码

**配置类**（`com.pack.retryclient.test.ClientConfig`）：

```java
@Configuration
public class ClientConfig {

  @Bean
  RestClient restClient(RestClient.Builder builder) {
    return builder.baseUrl("http://localhost:7000").build();
  }
}
```

**服务类**（`com.pack.retryclient.test.UserService`）：

```java
@Service
public class UserService {

  private final RestClient restClient;
  public UserService(RestClient restClient) { this.restClient = restClient; }

  public Map<String, Object> createUser2() {
    ParameterizedTypeReference<Map<String, Object>> bodyType = new ParameterizedTypeReference<>() {};
    return restClient.get().uri("/api/query/{id}", 666).retrieve().body(bodyType);
  }
}
```

**控制器**（`com.pack.retryclient.test.UserController`）：

```java
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;
  public UserController(UserService userService) { this.userService = userService; }

  @GetMapping("/query")
  public Object query() { return this.userService.createUser2(); }
}
```

### 7.2 验证测试

```java
@SpringBootTest
public class ClientRetryClientTest {

  @Resource private UserService userService;

  @Test
  public void testRetryClient2() {
    // 若目标服务返回 5xx，此处会自动重试 3 次（间隔 2s）
    this.userService.createUser2();
  }
}
```

---

## 8. 常见问题与注意事项

### Q1：接口返回失败但业务拿到的 body 是空的？

**原因**：拦截器会把响应体读出来交给错误处理器：

```java
String responseBody = StreamUtils.copyToString(httpResponse.getBody(), StandardCharsets.UTF_8) ;
errorHandler.handle(responseBody, headers, statusCode) ;
```

`InputStream` **只能读一次**。组件只在 4xx/5xx 时读取响应体并交给错误处理器：默认处理器会抛 `RetryClientException` 触发重试；若你自定义的处理器**不抛异常**、而是把错误响应交还业务方，在 `repeatread=false` 时业务拿到将是**空内容**。

> 💡 说明：2xx 响应不会进入错误处理器，也不会被读取，因此 `repeatread` 主要影响**错误响应**与**自定义处理器**的场景。

**解决**：

```yaml
gx:
  retryclient:
    repeatread: true
```

### Q2：mapping 配置了却不生效？

按以下顺序排查：

1. **key 是否只含一个冒号**？多冒号会导致该条被静默丢弃。
2. **YAML 中 key 是否正确加引号**？`'[get:/api/**]'` 必须带方括号和单引号。
3. **value 是否写了 `name`**？value 现在是对象，`'[get:/api/**]': queryuser` 这种旧写法在启动时就会配置绑定失败。
4. **方法名是否匹配**？大小写不敏感，但需是合法 HTTP 方法。
5. **路径是否匹配**？确认 Ant 通配符写法正确，注意 `/api/query/{id}` 与 `/api/query/**` 的区别。
6. **是否真的调用了对应路径**？请求 URI 的 path 部分才参与匹配，query 参数不影响。
7. **是否以为命中了规则**？未匹配时会静默使用 `default` 策略，看起来“像生效了”，实际走的是默认实例。

### Q3：设置了 timeout 但超时没生效？

**原因**：`ClientHttpRequestFactorySettings` 只作用于**容器自动配置**的 `RestClient.Builder（Spring Boot3）`。Spring Boo2将不会生效

**解决**：
- 若用 `RestClient.Builder` 注入方式 → 确认 `gx.retryclient.timeout.enabled=true`
- 若用 `RestClient.builder()` / `RestClient.create()` → 需在 `RequestFactory` 上**自行设置**超时

### Q4：想对某些错误码不重试？

**方案一**：在 `resilience4j.retry.instances.<name>.ignore-exceptions` 中排除 `RetryClientException`（会同时排除所有 4xx/5xx）。

**方案二（推荐）**：实现并注册自定义 `RetryClientErrorHandler`，只在需要重试的状态码上抛异常（见 3.4 / 6.1）。

**方案三**：该接口完全不需要重试时，把 mapping 的 `name` 设为 `none`。

### Q5：多个 RestClient Bean 会互相影响吗？

不会。重试策略的匹配依据是**运行时请求的 path 和 method**，而非 Bean。因此：

- 多个 `RestClient`（不同 baseUrl）可以共存
- 同一路径规则对所有 RestClient 统一生效
- 若不同 RestClient 需要不同重试策略，需在**调用路径**上做区分

### Q6：配置了 retry-handler-class 却不生效？

组件是按**具体类**从 Spring 容器取处理器的（`context.getBean(处理器类)`），请确认：

1. 配置的是**全限定类名**，且实现了 `RetryClientErrorHandler`；
2. 该类已注册为 Spring Bean（`@Component` 或 `@Bean` 方法），且 Bean 类型就是配置的那个类；
3. mapping 命中时，规则内的 `retry-handler-class` 优先级高于全局 `gx.retryclient.retry-handler-class`。

若配置的类不是 Bean，请求时会抛 `NoSuchBeanDefinitionException`。

