const http = require('http');
const https = require('https');
const { URL } = require('url');

// Disable strict SSL verification for debug-only localhost proxy.
// This avoids macOS cert verification failures when hitting the real backend.
https.globalAgent = new https.Agent({ rejectUnauthorized: false });

const PORT = 5555;
const TARGET_HOST = 'api.mapmytimes.com';
const S3_HOST = 'mapnytimes.s3.ap-south-1.amazonaws.com';
const UPLOADS_REMOTE_HOST = 'api.mapmytimes.com';

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Accept, Accept-Language, Authorization, Content-Type, Content-Length, X-Requested-With, X-Source, responseType, User-Agent, Origin, Referer, Range',
  'Access-Control-Expose-Headers': 'ETag, Content-Length, Content-Type, Link, X-Total-Count, Accept-Ranges, Content-Range, Cache-Control',
  'Access-Control-Max-Age': '86400',
};

const server = http.createServer((req, res) => {
  // Preflight (OPTIONS) → 204 with all CORS headers
  if (req.method === 'OPTIONS') {
    res.writeHead(204, CORS_HEADERS);
    return res.end();
  }

  // Attach CORS to every response
  for (const [k, v] of Object.entries(CORS_HEADERS)) res.setHeader(k, v);

  // -------- ROUTE DISPATCH: pick upstream host based on URL path --------
  // /s3/*           → mapnytimes S3 bucket (strip /s3 prefix)
  // /uploads/*      → api.mapmytimes.com/uploads/* (passthrough)
  // everything else → api.mapmytimes.com (original API proxy)
  const raw = req.url ?? '/';
  let targetHost = TARGET_HOST;
  let targetPath = raw;
  let isImage = false;

  if (raw.startsWith('/s3/')) {
    targetHost = S3_HOST;
    // Strip "/s3" prefix and preserve the rest of the path + query
    // e.g.  /s3/blog-posts/uuid/foo.jpg  →   /blog-posts/uuid/foo.jpg
    const after = raw.substring('/s3'.length);
    targetPath = after.startsWith('/') ? after : '/' + after;
    isImage = true;
  } else if (raw.startsWith('/uploads/')) {
    targetHost = UPLOADS_REMOTE_HOST;
    isImage = true;
  }

  const upstreamUrl = new URL(targetPath, `https://${targetHost}`);
  console.log(`[proxy ${req.method}] ${raw} → ${targetHost}${upstreamUrl.pathname}${upstreamUrl.search ?? ''}`);

  const skipHeaders = new Set([
    'host', 'accept-encoding', 'content-length', 'connection',
    'user-agent', 'referer', 'origin',
    'cf-connecting-ip', 'cf-ipcountry', 'cf-ray', 'cf-visitor',
  ]);
  // Upstream backend sits behind Cloudflare + backend Spring Security that blocks
  // non-browser user-agents (e.g. "Dart/3.x (http)") AND also inspects the Origin
  // header — localhost origins (http://localhost:xxxxx from Flutter web-dev) are
  // not in the backend CORS allowlist → 403. We spoof a real desktop Chrome UA
  // and strip Origin/Referer/Sec-Fetch-* browser metadata headers so the
  // forwarded request looks like a normal server-to-server API call.
  const upstreamHeaders = {
    host: targetHost,
    'accept-encoding': 'identity',
    accept: isImage
      ? 'image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8'
      : 'application/json',
    referer: isImage
      ? `https://${targetHost}/`
      : 'https://mapmytimes.com/',
    'cache-control': isImage ? 'public, max-age=86400' : 'no-cache',
    pragma: isImage ? 'no-cache' : 'no-cache',
    'user-agent':
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
  };
  for (const [k, v] of Object.entries(req.headers)) {
    const lk = k.toLowerCase();
    if (skipHeaders.has(lk)) continue;
    if (lk.startsWith('sec-fetch-')) continue;
    if (lk.startsWith('sec-ch-ua')) continue;
    if (lk === 'accept') continue;
    if (lk === 'referer') continue;
    if (lk === 'cookie' || lk === 'cookie2') continue;
    if (lk === 'dnt') continue;
    if (lk.startsWith('upgrade-')) continue;
    if (lk.startsWith('priority')) continue;
    if (v === undefined || v === null) continue;
    upstreamHeaders[k] = v;
  }
  if (req.headers['content-length'] !== undefined && req.headers['content-length'] !== null) {
    upstreamHeaders['content-length'] = req.headers['content-length'];
  }
  // Pass through Range headers for partial image fetches
  if (req.headers['range'] && !upstreamHeaders['range']) {
    upstreamHeaders['range'] = req.headers['range'];
  }

  const upstreamReq = https.request(
    {
      method: req.method,
      hostname: targetHost,
      port: 443,
      path: upstreamUrl.pathname + upstreamUrl.search,
      headers: upstreamHeaders,
      timeout: 60_000,
      rejectUnauthorized: false,
    },
    (upstreamRes) => {
      // Forward upstream status + response headers (merge our CORS on top)
      for (const [k, v] of Object.entries(upstreamRes.headers)) {
        if (k.toLowerCase().startsWith('access-control-')) continue;
        try { res.setHeader(k, v); } catch {}
      }
      for (const [k, v] of Object.entries(CORS_HEADERS)) res.setHeader(k, v);
      const sc = upstreamRes.statusCode ?? 502;
      res.writeHead(sc);
      let upstreamBytes = 0;
      upstreamRes.on('data', (c) => { upstreamBytes += c.length; });
      upstreamRes.on('end', () => {
        console.log(`  ↩ HTTP ${sc} | ${upstreamBytes}B | ${raw}`);
      });
      upstreamRes.pipe(res);
    },
  );

  upstreamReq.on('timeout', () => {
    upstreamReq.destroy(new Error('Upstream timeout'));
  });

  upstreamReq.on('error', (err) => {
    console.error('[proxy error]', err.message);
    try {
      res.writeHead(502, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'BAD_GATEWAY', detail: err.message }));
    } catch {}
  });

  req.pipe(upstreamReq);
});

server.listen(PORT, () => {
  console.log(`\n✅ MapMyTimes CORS Proxy running → http://localhost:${PORT}`);
  console.log(`   /api/*        → https://${TARGET_HOST}/api/*`);
  console.log(`   /uploads/*    → https://${UPLOADS_REMOTE_HOST}/uploads/*`);
  console.log(`   /s3/*         → https://${S3_HOST}/*`);
  console.log(`\n   Set MMT_API_BASE="http://localhost:${PORT}" for Flutter web.\n`);
});
