# Smart Home

智能家居项目（Spring Boot）。当前阶段实现了网页登录 / 登出 / 个人资料修改，目的是**边做功能边理解 Spring 的 IoC / DI 到底是怎么运作的**。

## 已实现功能

- 登录：输入账号密码，校验成功进入个人资料页
- 登出：清除登录状态，回到登录页
- 个人资料（登录后展示 / 修改 / 保存）：
  - 用户ID：程序赋予，仅展示不可修改
  - 账号：注册后不可修改
  - 真实姓名、昵称、性别（男/女/保密）
  - 生日：日历选择，不能超过今天（`@PastOrPresent` + 页面 `max` 属性双重限制）
  - 手机号：格式校验 `^1[3-9]\d{9}$`（`@Pattern`）
  - 邮箱：格式校验（`@Email`）
  - 个人简介：选填
- 安全基础：密码用 BCrypt 哈希存储（不存明文）；登录状态用 token（Cookie + 内存缓存）保持
- 演示账号：`admin` / `123456`（启动时自动创建）

## 运行方式

```bash
gradle bootRun
```

浏览器访问 http://localhost:8080/login

> 注意：本项目根目录的 `gradle.properties` 里关闭了校园代理
> （`proxy-dku.oit.duke.edu:3128`），否则依赖下载会 403。若换了网络环境，
> 可删除该文件里的 `systemProp.*proxy*` 配置。

## 代码结构（按分层职责）

```
com.smarthome
├── SmartHomeApplication  启动入口：Spring 从这里开始扫描组件
├── model/User            用户实体（对应数据库 users 表）
│   └── Gender            性别枚举（男/女/保密）
├── dto/ProfileForm       个人资料表单 DTO：承接网页表单 + 校验注解
├── repository/           数据访问层
│   ├── UserRepository    接口，方法由 Spring Data JPA 动态生成实现
│   └── SystemStatusRepository
├── service/              业务逻辑层
│   ├── AuthService       登录/登出/会话缓存
│   ├── UserService       注册/查看/修改资料
│   └── SystemStatusService
├── controller/           Web 层
│   ├── AuthController    登录/登出页面
│   ├── ProfileController 个人资料页
│   └── PingController
└── config/
    ├── AppConfig         用 @Bean 手工声明 PasswordEncoder
    └── DataInitializer   启动时造演示账号
```

## 核心：Spring 的 IoC / DI 是怎么实现的（对应你的理解）

### 1. 容器从哪里看到"一整份类加载对象名单"？

答案是**组件扫描（Component Scan）**。`SmartHomeApplication` 上的
`@SpringBootApplication` 内部包含 `@ComponentScan`。应用启动时：

1. 从启动类所在的包 `com.smarthome` 开始，向下递归扫描所有类；
2. 用类加载器加载每个 `.class`，检查类上是否有 `@Component` /
   `@Service` / `@Repository` / `@Controller` 等注解；
3. 有注解的类被登记进一份"候选名单"（这些就是将来要管理的 bean）。

所以"名单"不是凭空有的，而是**启动时靠注解扫描 + 类加载器**实时收集出来的。
这正是你理解的"classloader 把注解挂在类加载对象上"的前半段来源。

### 2. "一层一层，先从没有依赖的开始存" —— 依赖解析

扫描拿到名单后，容器要逐个创建 bean。你的直觉是对的，真实机制是：

- 创建某个 bean 时，如果它的**构造方法需要别的 bean**（依赖），
  容器会**先递归去创建那个被依赖的 bean**；
- 于是形成一条"依赖链"：被依赖的（没有依赖的）先创建，依赖它的后创建。

以本项目为例，创建 `AuthController` 时：

```text
AuthController 需要 AuthService
  -> AuthService 需要 UserRepository + PasswordEncoder
       -> UserRepository：接口，由 Spring Data JPA 生成代理
       -> PasswordEncoder：由 AppConfig 的 @Bean 方法 new 出来
```

所以最终创建顺序是：`PasswordEncoder` → `UserRepository` → `AuthService` → `AuthController`。
**没有依赖的先被创建，这正是你说的"分层存储"。**

### 3. 两种 DI 方式，本项目都用了

- **注解方式**：`@Service` / `@Repository` / `@Controller` 标注的类，
  靠扫描发现，靠**构造方法参数**注入依赖（这是最推荐、最直观的方式）。
- **配置方式**：`PasswordEncoder` 来自第三方库，自己没贴注解，于是在
  `AppConfig` 里用 `@Configuration` + `@Bean` 方法手工声明，交给容器管理。

### 4. 一个关键点：接口怎么有实现？（动态代理）

`UserRepository` 只是一个接口，一行实现代码都没写。运行时能调用
`save` / `findByUsername`，是因为 **Spring Data JPA 在启动时用动态代理
生成了这个接口的实现对象**，并注册进容器。这是 IoC 之上的一层"魔法"，
但它仍然是"扫描 → 生成 → 注入"这条主线的一部分。

### 5. 登录状态怎么"保持"？

真实的 Web 是无状态的（HTTP 请求之间没有记忆）。本项目用最朴素的方式模拟：

1. 登录成功 → 生成一个随机 `token`，放进内存 Map（token → username）；
2. 把 token 写进浏览器的 Cookie；
3. 之后每次请求，浏览器自动带上 Cookie，后端从 Cookie 取出 token，
   反查 Map 得到当前用户。

真实项目会把这份状态放到 Redis / Spring Session 里（可跨进程、可过期），
但**核心思路完全一样**。这是下一步可以优化的方向。

## 下一步可优化方向（由简到难）

1. 会话过期：给 token 加有效期，到期自动失效
2. 密码修改：个人页增加"修改密码"功能
3. 用 MySQL 替换 H2（改依赖 + 配置即可，代码几乎不用动）
4. 引入 Spring Security 处理更完整的认证 / 授权
5. 会话状态外置到 Redis

## 测试

```bash
gradle test
```
