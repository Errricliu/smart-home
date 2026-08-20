# Smart Home · 智能家居控制中心

一个学习用的智能家居项目，采用**前后端分离 + 跨语言互通**架构：Node.js 托管前端页面，Spring Boot 提供业务 API，MySQL 存储数据。边做功能边理解 Spring 的 IoC / DI 原理。

## 架构

```
浏览器 -> main.html(前端 JS)
              ↓ fetch /api/*  /avatars/*
        Node.js (3000, 代理)
              ↓ 转发
   Spring Boot (8080, JSON API)
              ↓
           MySQL (3306)
```

- **前端**：`frontend-node/`（Node.js，纯内置模块，无需 npm install）
- **后端**：`src/main/java/com/smarthome/`（Spring Boot）
- **数据库**：MySQL 8.0（Docker Compose 管理）

## 已实现功能

- **宣传首页**：导航栏 + 产品介绍 + 功能特性 + 登录/注册弹窗
- **登录/登出**：token 存 localStorage，刷新不掉登录态
- **会话过期**：30 分钟无操作自动失效（滑动过期），前端收到 401 自动跳登录
- **统一鉴权拦截器**：AuthInterceptor 拦截所有 /api/**，从 `Authorization: Bearer <token>` 取 token，未登录返回 401
- **防暴力破解**：连续错 5 次锁 5 分钟 + 防账号枚举（时间差抹平）
- **注册**：账号/密码/手机号校验 + 用户名、手机号查重
- **角色权限**：普通用户(USER) / 管理员(ADMIN)，管理员可看全平台用户和设备
- **隐私脱敏**：管理员看他人手机号/邮箱/姓名时脱敏（`139****5678`、`l***@example.com`、`张*`），仅自己看完整
- **个人资料**：用户ID(补零显示)、姓名(全平台唯一)、性别、生日(不超今天)、手机号、邮箱、简介
- **头像上传**：JPG/PNG/WebP/GIF，最大 2MB，即时预览
- **设备管理**：添加/删除/开关设备，设备类型(灯/空调/电视/窗帘/音箱/传感器)，按 ownerId 隔离归属
- **并发能力**：Tomcat 线程池 + HikariCP 连接池调优，附带压测脚本

演示账号：
- 管理员 `admin` / `123456`（可看用户管理页）
- 普通用户 `demo_user` / `123456`

## 运行方式

### 1. 启动 MySQL（Docker）

```bash
docker compose up -d
```

### 2. 启动后端（Spring Boot）

```bash
gradle bootRun
```

### 3. 启动前端（Node.js）

```bash
cd frontend-node
node server.js
```

浏览器访问 http://localhost:3000

## 局域网部署（手机等设备访问）

Node 服务默认监听 `0.0.0.0`，启动时会打印局域网地址：

```
前端服务已启动: http://localhost:3000
局域网访问: http://192.168.31.191:3000
```

手机连**同一个 WiFi**，浏览器打开上面打印的局域网地址即可。
架构上手机访问的是 Node(3000)，Node 再转发给本机 Spring Boot(8080)，后端无需额外配置。

## 压测

```bash
cd frontend-node
node load-test.js 500 100   # 500 次登录，每批 100 并发
```

> 真实场景「500 人同时登录」是分批到达的，一次性打满 500 个 socket 会触发
> 操作系统 TCP backlog 限制（ECONNRESET），属正常现象。脚本用「受控爬坡」模拟真实负载。

## 代码结构（按分层职责）

```
com.smarthome
├── SmartHomeApplication   启动入口
├── model/                 实体层
│   ├── User              用户（含 role 角色字段）
│   ├── Device            设备（含设备类型/状态枚举）
│   ├── Gender            性别枚举
│   └── Role              角色枚举（USER/ADMIN）
├── dto/                   表单 DTO + 校验注解
├── repository/            数据访问层（接口，Spring Data JPA 动态实现）
│   ├── UserRepository
│   └── DeviceRepository
├── service/               业务逻辑层
│   ├── AuthService        登录/登出/防暴力破解/会话缓存
│   ├── UserService        注册/改资料/姓名查重
│   ├── DeviceService      设备增删改查 + 归属校验
│   └── FileStorageService 头像存储
├── controller/            Web 层（纯 JSON API，无服务端模板）
│   ├── ApiController      登录/注册/资料/头像/管理员
│   ├── DeviceController   设备增删改查/开关
│   └── PingController     健康检查
├── exception/             自定义异常
│   └── LoginLockedException
└── config/
    ├── AppConfig          @Bean 声明 PasswordEncoder
    ├── AuthInterceptor    登录拦截器（统一鉴权，AOP 思想）
    ├── DataInitializer    启动初始化演示账号/设备
    ├── WebConfig          注册拦截器 + 静态资源映射
    └── GlobalExceptionHandler 全局异常（返回 JSON）
```

## 核心：Spring 的 IoC / DI 怎么运作

### 1. 容器怎么知道有哪些类？

**组件扫描（Component Scan）**。`@SpringBootApplication` 含 `@ComponentScan`，启动时从 `com.smarthome` 包向下扫描所有类，用类加载器加载 `.class`，检查 `@Component/@Service/@Repository/@Controller` 注解，登记进"候选名单"。

### 2. "一层一层，先从没依赖的开始" —— 依赖解析

创建 bean 时若构造方法需要别的 bean，容器会**先递归创建被依赖者**。以本项目为例：

```
ApiController 需要 AuthService + UserService + FileStorageService
  -> AuthService 需要 UserRepository + PasswordEncoder
       -> UserRepository：接口，由 Spring Data JPA 生成代理
       -> PasswordEncoder：AppConfig 的 @Bean 方法 new 出来
```

**没有依赖的先创建**，这正是"分层存储"。

### 3. 两种 DI 方式

- **注解**：`@Service/@Repository/@Controller` 靠扫描发现，靠构造方法参数注入
- **配置**：`PasswordEncoder` 来自第三方库，在 `AppConfig` 用 `@Bean` 手工声明

### 4. 接口怎么有实现？（动态代理）

`UserRepository`/`DeviceRepository` 只是接口，运行时能调用是因为 **Spring Data JPA 用动态代理生成了实现**并注册进容器。

### 5. 登录状态怎么保持 + 拦截器鉴权（AOP）

1. 登录成功 → 生成 token 存内存 Map（token → 会话对象，含过期时间）
2. 前端把 token 存 localStorage，之后每次请求带 `Authorization: Bearer <token>` 头
3. **AuthInterceptor** 拦截所有 `/api/**`，统一从 Header 取 token 鉴权，把用户塞进 request
4. Controller 通过 `@RequestAttribute` 直接拿用户，不再重复写鉴权代码

这就是 **AOP（面向切面编程）**：把「鉴权」这个横切所有接口的通用逻辑，从每个方法里抽出来放到拦截器统一处理，Controller 只写业务。会话过期用「滑动过期」——30 分钟内有过操作就续期，长时间不操作才失效。

## 下一步可优化方向

1. 会话外置 Redis（当前存内存，重启即丢）
2. 密码修改功能
3. 引入 Spring Security 做完整认证/授权（RBAC）
4. 设备电量/运行状态详情、场景联动
5. HTTPS + 域名部署
6. 接口文档（Swagger/OpenAPI）
