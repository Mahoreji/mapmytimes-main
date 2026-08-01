import { NextRequest, NextResponse } from "next/server";
import { SITE } from "@/lib/utils";

const CSP_POLICY = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-eval' 'unsafe-inline'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob: https: http:",
  "font-src 'self' data:",
  "connect-src 'self' https: http:",
  "media-src 'self' https: http:",
  "frame-ancestors 'none'",
  "base-uri 'self'",
  `form-action 'self' ${SITE.url.replace(/\/$/, "")}`,
  "object-src 'none'",
  "worker-src 'self' blob:",
  "frame-src 'self' https:",
].join("; ");

const SECURITY_HEADERS: Record<string, string> = {
  "X-Content-Type-Options": "nosniff",
  "X-DNS-Prefetch-Control": "on",
  "X-Download-Options": "noopen",
  "X-Frame-Options": "SAMEORIGIN",
  "X-Permitted-Cross-Domain-Policies": "none",
  "Referrer-Policy": "strict-origin-when-cross-origin",
  "Strict-Transport-Security": "max-age=31536000; includeSubDomains; preload",
  "Permissions-Policy":
    "camera=(), microphone=(), geolocation=(), interest-cohort=(), payment=(), usb=(), bluetooth=()",
  "Content-Security-Policy": CSP_POLICY,
};

const AUTH_REQUIRED_PREFIXES = ["/dashboard"];
const AUTH_WALL_PATHS = ["/login", "/signup", "/forgot-password", "/reset-password", "/verify"];
const PUBLIC_STATIC = /^\/(_next|assets|favicon|sitemap\.xml|robots\.txt|manifest\.webmanifest|api)\b/;

function makeRid(): string {
  if (typeof globalThis.crypto !== "undefined" && "randomUUID" in globalThis.crypto) {
    return globalThis.crypto.randomUUID();
  }
  const h = (n: number) => Math.random().toString(16).slice(2, 2 + n);
  return `${h(8)}-${h(4)}-4${h(3)}-${h(4)}-${h(12)}`;
}

export function middleware(req: NextRequest) {
  const path = req.nextUrl.pathname;
  const res = NextResponse.next();

  if (!PUBLIC_STATIC.test(path)) {
    Object.entries(SECURITY_HEADERS).forEach(([k, v]) => {
      res.headers.set(k, v);
    });
    res.headers.set("X-Request-Id", makeRid());
  }

  if (AUTH_REQUIRED_PREFIXES.some((p) => path.startsWith(p))) {
    const token =
      req.cookies.get("mmt.auth.access")?.value ||
      req.headers.get("authorization")?.replace(/^Bearer\s+/i, "");
    if (!token) {
      const login = new URL("/login", req.nextUrl.origin);
      login.searchParams.set("next", path);
      return NextResponse.redirect(login, 307);
    }
  }

  if (AUTH_WALL_PATHS.includes(path)) {
    const token =
      req.cookies.get("mmt.auth.access")?.value ||
      req.headers.get("authorization")?.replace(/^Bearer\s+/i, "");
    if (token) {
      return NextResponse.redirect(new URL("/dashboard", req.nextUrl.origin), 307);
    }
  }

  return res;
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt|manifest.webmanifest|assets/.*).*)"],
};
