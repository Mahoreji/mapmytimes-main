#!/usr/bin/env python3
"""Reverse proxy for MapMyTimes mobile web preview.

Forwards http://localhost:5555/<path> → https://api.mapmytimes.com/<path>
and injects fully permissive CORS headers (including OPTIONS preflight),
so the TRAE integrated browser (which enforces strict CORS, no
--disable-web-security flag possible) can still hit the production API.
"""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib import request as urlreq
from urllib.error import HTTPError, URLError
import ssl, sys, os

PORT = 5555
TARGET = "https://api.mapmytimes.com"

CORS = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS",
    "Access-Control-Allow-Headers": "*",
    "Access-Control-Expose-Headers": "*",
    "Access-Control-Max-Age": "86400",
}

# MacOS system CA pool; don't skip verification
_SSL = ssl.create_default_context()


def _env():
    return {k: v for k, v in os.environ.items() if k.startswith(("http_", "https_", "HTTP_", "HTTPS_", "no_proxy", "NO_PROXY"))}


class Handler(BaseHTTPRequestHandler):
    server_version = "MMTCorsProxy/1.0"
    log_format = '[proxy %(code)s] %(method)s %(path)s → %(size)s B in %(ms)dms'

    def log_message(self, fmt, *args):  # quieter
        sys.stderr.write(fmt % args + "\n")
        sys.stderr.flush()

    # ---------- CORS helpers ------------------------------------------------
    def _send_cors(self, status, extra_headers=None):
        self.send_response_only(status) if False else None
        for k, v in CORS.items():
            self.send_header(k, v)
        if extra_headers:
            for k, v in extra_headers.items():
                if k.lower().startswith("access-control-"):
                    continue
                if isinstance(v, (list, tuple)):
                    for x in v:
                        self.send_header(k, x)
                else:
                    self.send_header(k, v)

    # ---------- OPTIONS preflight ------------------------------------------
    def do_OPTIONS(self):  # noqa: N802
        self.send_response(204)
        for k, v in CORS.items():
            self.send_header(k, v)
        self.send_header("Content-Length", "0")
        self.end_headers()

    # ---------- Main upstream call -----------------------------------------
    def _proxy(self):
        import time
        t0 = time.time()
        url = TARGET + self.path
        method = self.command
        # read request body (if any)
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length > 0 else None

        # copy incoming headers, drop hop-by-hop
        skip = {
            "host", "content-length", "accept-encoding", "connection",
            "proxy-authorization", "proxy-authenticate", "te", "trailer",
            "transfer-encoding", "upgrade",
        }
        hdrs = {"Host": "api.mapmytimes.com", "Accept-Encoding": "identity"}
        for k, v in self.headers.items():
            if k.lower() in skip:
                continue
            hdrs[k] = v

        if body is not None and length:
            hdrs["Content-Length"] = str(length)

        req = urlreq.Request(url, data=body, headers=hdrs, method=method)
        try:
            with urlreq.urlopen(req, timeout=60, context=_SSL) as resp:
                data = resp.read()
                ms = int((time.time() - t0) * 1000)
                # forward status + response headers (strip conflicting CORS)
                self.send_response(resp.status)
                for k, v in CORS.items():
                    self.send_header(k, v)
                seen_cl = False
                for k, v in resp.headers.items():
                    lk = k.lower()
                    if lk.startswith("access-control-"):
                        continue
                    if lk in ("transfer-encoding", "connection", "content-encoding", "keep-alive"):
                        continue
                    if lk == "content-length":
                        seen_cl = True
                        self.send_header(k, str(len(data)))
                        continue
                    self.send_header(k, v)
                if not seen_cl:
                    self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                if data:
                    self.wfile.write(data)
                sys.stderr.write(
                    f"[proxy {resp.status}] {method} {self.path} → {len(data)} B in {ms}ms\n"
                )
                sys.stderr.flush()
        except HTTPError as he:
            try:
                err = he.read()
            except Exception:
                err = b""
            ms = int((time.time() - t0) * 1000)
            self.send_response(he.code or 502)
            for k, v in CORS.items():
                self.send_header(k, v)
            self.send_header("Content-Type", he.headers.get("Content-Type", "application/json"))
            self.send_header("Content-Length", str(len(err)))
            self.end_headers()
            if err:
                self.wfile.write(err)
            sys.stderr.write(
                f"[proxy {he.code}] {method} {self.path} → ERROR ({len(err)} B, {ms}ms): {he.reason}\n"
            )
        except (URLError, TimeoutError, OSError) as e:
            ms = int((time.time() - t0) * 1000)
            payload = ('{"error":"BAD_GATEWAY","detail":%s}' % repr(str(e))).encode()
            self.send_response(502)
            for k, v in CORS.items():
                self.send_header(k, v)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
            sys.stderr.write(
                f"[proxy 502] {method} {self.path} → UPSTREAM FAIL ({ms}ms): {e!r}\n"
            )
            sys.stderr.flush()

    do_GET = do_HEAD = do_POST = do_PUT = do_DELETE = do_PATCH = _proxy


def main():
    srv = ThreadingHTTPServer(("127.0.0.1", PORT), Handler)
    sys.stderr.write(
        f"\n✅ MapMyTimes CORS Proxy running → http://localhost:{PORT}\n"
        f"   All requests forwarded to {TARGET}\n"
        f"   Set MMT_API_BASE=\"http://localhost:{PORT}\" for Flutter web.\n\n"
    )
    sys.stderr.flush()
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        srv.server_close()
        sys.stderr.write("\nStopped.\n")


if __name__ == "__main__":
    main()
