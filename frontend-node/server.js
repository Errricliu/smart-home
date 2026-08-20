/**
 * Node.js 前端服务（代理模式）
 *
 * 职责：
 *   1. 托管静态页面 main.html（浏览器打开 http://localhost:3000 即可访问）
 *   2. 把 /api/* 请求原样转发给 Spring Boot 后端（http://127.0.0.1:8080）
 *
 * 架构（跨语言互通）：
 *   浏览器 -> main.html(JS) -> 本服务(3000) -> Spring Boot(8080) -> MySQL
 *
 * 说明：只用了 Node 内置的 http / fs / path 模块，无需 npm install。
 * 原来的 mysql2 / bcrypt 依赖已去掉——数据库和密码校验都交给 Java 后端了。
 *
 * 启动方式：node server.js   （或 npm start）
 * 注意：先启动 Spring Boot（gradlew bootRun），再启动本服务。
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

// Spring Boot 后端地址（必填）
const BACKEND = { host: '127.0.0.1', port: 8080 };
// 本服务监听端口
const PORT = 3000;
// 监听地址：0.0.0.0 表示监听所有网卡，让局域网内其他设备（手机等）也能访问
const HOST = '0.0.0.0';

const server = http.createServer((req, res) => {
  // 1. 托管前端页面
  if (req.method === 'GET' && (req.url === '/' || req.url === '/main.html')) {
    const filePath = path.join(__dirname, 'main.html');
    fs.readFile(filePath, (err, html) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end('<h1>500: main.html not found</h1>');
        return;
      }
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(html);
    });
    return;
  }

  // 2. /api/* 和 /avatars/* 转发给 Spring Boot
  if (req.url.startsWith('/api/') || req.url.startsWith('/avatars/')) {
    const headers = { ...req.headers };
    // 把 Host 头改成后端地址，避免 Java 端拿到的是本服务的域名
    headers.host = `${BACKEND.host}:${BACKEND.port}`;

    const proxyReq = http.request(
      {
        host: BACKEND.host,
        port: BACKEND.port,
        path: req.url,
        method: req.method,
        headers
      },
      (proxyRes) => {
        // 后端响应原样回传给浏览器
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        proxyRes.pipe(res);
      }
    );

    proxyReq.on('error', (err) => {
      console.error('[proxy] 转发失败:', err.message);
      // 关键：响应头可能已在 pipe 过程中发出（后端中途重启等场景），
      // 此时不能再 writeHead，否则抛 ERR_HTTP_HEADERS_SENT 让进程崩溃。
      // 未发送头才返回 502，已发送则直接销毁连接。
      if (!res.headersSent) {
        res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: '后端服务未启动，请先运行 Spring Boot' }));
      } else {
        res.destroy();
      }
    });

    // 把浏览器的请求体传给后端
    req.pipe(proxyReq);
    return;
  }

  // 3. 其他一律 404
  res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify({ success: false, message: 'Not Found' }));
});

server.listen(PORT, HOST, () => {
  console.log(`前端服务已启动: http://localhost:${PORT}`);
  console.log(`API 转发到 Spring Boot: http://${BACKEND.host}:${BACKEND.port}`);
  // 打印局域网地址，方便手机等设备访问
  const nets = require('os').networkInterfaces();
  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      if (net.family === 'IPv4' && !net.internal) {
        console.log(`局域网访问: http://${net.address}:${PORT}`);
      }
    }
  }
});
