# Smart Home

这是智能家居项目迁移到 Spring Boot 后的第一步：先让浏览器可以成功请求后端。

## 当前目标

这一阶段只验证一件事：

浏览器请求 -> Spring Boot 后端 -> 返回成功响应

暂时不接入登录、用户、家具、日志、MySQL、MyBatis。这样做是为了先理解 Spring Boot Web 项目的最小结构，以及 Spring 容器如何把对象组织起来。

## 当前目录职责

`src/main/java/com/smarthome`

项目的 Java 主包。Spring Boot 会从这里开始扫描组件。

`src/main/java/com/smarthome/controller`

Controller 层，负责接收浏览器或前端发来的 HTTP 请求，并返回 HTTP 响应。当前只有一个 `PingController`，用于确认后端可以正常被访问。

`src/main/java/com/smarthome/service`

Service 层，负责组织业务逻辑。当前只有一个 `SystemStatusService`，用于演示 Controller 如何通过构造方法依赖 Service。

`src/main/java/com/smarthome/repository`

Repository 层，负责提供数据。当前还没有连接数据库，只用 `SystemStatusRepository` 演示 Service 如何依赖数据来源。

## 当前请求链路

```text
浏览器
  -> PingController
  -> SystemStatusService
  -> SystemStatusRepository
  -> 返回 JSON
```

这里的 `PingController`、`SystemStatusService`、`SystemStatusRepository` 都不是我们手动 `new` 出来的，而是 Spring 根据注解扫描后放进容器，再根据构造方法参数完成依赖注入。

`src/main/resources`

项目配置目录。当前只有 `application.properties`，以后会在这里配置端口、数据库连接等信息。

`src/test/java/com/smarthome`

测试代码目录。当前测试会确认 Spring Boot 能启动，并且 `/api/ping` 接口能返回成功。

## 运行方式

如果你的电脑已经安装 Gradle，可以在项目根目录运行：

```bash
gradle bootRun
```

如果 Gradle 提示类似 `Failed to load native library 'libnative-platform.dylib'`，可以临时把 Gradle 缓存放到项目目录里运行：

```bash
GRADLE_USER_HOME=.gradle-home gradle bootRun
```

启动成功后，浏览器访问：

```text
http://localhost:8080/api/ping
```

应该看到类似这样的返回：

```json
{
  "success": true,
  "message": "Smart Home backend is running",
  "timestamp": "2026-08-04T00:00:00Z"
}
```

## 测试方式

```bash
gradle test
```

如果普通 `gradle test` 遇到 Gradle 缓存问题，也可以使用：

```bash
GRADLE_USER_HOME=.gradle-home gradle test
```
