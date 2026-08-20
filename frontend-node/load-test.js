#!/usr/bin/env node
/**
 * 并发登录压测脚本（受控爬坡式）
 *
 * 用法：
 *   node load-test.js [总请求数] [每批并发数]
 *   例如：node load-test.js 500 100   —— 模拟 500 人登录，每批最多 100 人同时发起
 *
 * 说明：
 *   真实场景里“500人同时登录”不会在同一毫秒内一次性建立 500 个 TCP 连接，
 *   而是分批到达。一次性打满 500 个 socket 会触发操作系统的 TCP backlog 限制
 *   （表现为 ECONNRESET），这不是后端业务能力问题。所以本脚本用“受控并发”，
 *   每批最多 N 个同时发起，更贴近真实负载，也更能反映后端真实吞吐。
 *
 * 前置：Spring Boot 已启动（8080），数据库里有账号 admin/123456。
 */
const http = require('http');

const TOTAL = parseInt(process.argv[2]) || 500;      // 总请求数
const BATCH = parseInt(process.argv[3]) || 100;      // 每批并发数
const HOST = '127.0.0.1';
const PORT = 8080;

function loginOnce() {
  return new Promise((resolve) => {
    const body = JSON.stringify({ username: 'admin', password: '123456' });
    const start = Date.now();
    const req = http.request({
      host: HOST, port: PORT, path: '/api/login', method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) }
    }, (res) => {
      let data = '';
      res.on('data', (c) => (data += c));
      res.on('end', () => {
        const ms = Date.now() - start;
        let ok = false;
        try { ok = JSON.parse(data).success === true; } catch (e) {}
        resolve({ ok, ms, status: res.statusCode });
      });
    });
    req.on('error', () => resolve({ ok: false, ms: Date.now() - start, status: 0 }));
    req.write(body);
    req.end();
  });
}

async function main() {
  console.log(`开始压测：总 ${TOTAL} 次登录，每批 ${BATCH} 个并发...`);
  const startAll = Date.now();
  const results = [];

  const batches = Math.ceil(TOTAL / BATCH);
  for (let b = 0; b < batches; b++) {
    const n = Math.min(BATCH, TOTAL - results.length);
    const batchStart = Date.now();
    const rs = await Promise.all(Array.from({ length: n }, () => loginOnce()));
    results.push(...rs);
    console.log(`  第 ${b + 1}/${batches} 批 (${n} 个) 完成，耗时 ${Date.now() - batchStart}ms`);
  }

  const totalMs = Date.now() - startAll;
  const okCount = results.filter((r) => r.ok).length;
  const failCount = results.length - okCount;
  const times = results.map((r) => r.ms).sort((a, b) => a - b);
  const avg = times.reduce((a, b) => a + b, 0) / times.length;
  const p50 = times[Math.floor(times.length * 0.5)];
  const p90 = times[Math.floor(times.length * 0.9)];
  const p99 = times[Math.floor(times.length * 0.99)];
  const max = times[times.length - 1];

  console.log('\n===== 压测结果 =====');
  console.log(`总请求数   : ${results.length}`);
  console.log(`成功       : ${okCount}`);
  console.log(`失败       : ${failCount}`);
  console.log(`总耗时     : ${totalMs}ms`);
  console.log(`吞吐量     : ${Math.round(results.length / (totalMs / 1000))} req/s`);
  console.log(`平均耗时   : ${avg.toFixed(1)}ms`);
  console.log(`P50        : ${p50}ms`);
  console.log(`P90        : ${p90}ms`);
  console.log(`P99        : ${p99}ms`);
  console.log(`最慢       : ${max}ms`);
}

main().catch((e) => { console.error(e); process.exit(1); });
