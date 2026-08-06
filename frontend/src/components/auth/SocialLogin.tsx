"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { SITE } from "@/lib/utils";
import { socialAuthUrls } from "@/lib/api/authApi";

export function SocialDivider({ label = "or continue with" }: { label?: string }) {
  return (
    <div className="flex items-center gap-3 py-1 text-[11px] uppercase tracking-widest text-ink-600 font-semibold">
      <span className="h-px flex-1 bg-ink-950/10" />
      {label}
      <span className="h-px flex-1 bg-ink-950/10" />
    </div>
  );
}

function initiateSocialLogin(initiateUrl: string, setBusy: (b: boolean) => void, setErr: (s: string) => void) {
  setBusy(true);
  setErr("");
  try {
    if (typeof window === "undefined" || !initiateUrl) {
      throw new Error("OAuth initiate URL is empty");
    }
    // NO fetch() call!  The backend OAuth2Controller now returns HTTP 302
    // (redirect) by default, so a plain browser navigation will send the GET,
    // receive the Location header, and land on Google/Facebook consent page
    // directly.  This completely avoids CORS which was producing the
    // "FAILED TO FETCH" red banner on signup/login pages on port 3001.
    if (!/^https?:\/\//i.test(initiateUrl)) {
      throw new Error("Invalid OAuth initiate URL");
    }
    window.location.assign(initiateUrl);
  } catch (e) {
    setBusy(false);
    const msg = e instanceof Error ? e.message : String(e ?? "error");
    setErr(msg.startsWith("HTTP") || /failed/i.test(msg) ? "FAILED TO FETCH" : msg.toUpperCase());
  }
}

export function SocialLoginButtons({
  mode = "login",
  size = "lg",
  block = true,
  vertical = true,
}: {
  mode?: "login" | "signup";
  size?: "sm" | "md" | "lg";
  block?: boolean;
  vertical?: boolean;
}) {
  const [origin, setOrigin] = useState<string>("");
  const [host, setHost] = useState<string>("");
  const [googleBusy, setGoogleBusy] = useState(false);
  const [facebookBusy, setFacebookBusy] = useState(false);
  const [err, setErr] = useState<string>("");
  useEffect(() => {
    if (typeof window !== "undefined") {
      setOrigin(window.location.origin);
      setHost(window.location.hostname);
    }
  }, []);

  const redirectUri = origin ? origin.replace(/\/$/, "") + "/auth/oauth2/redirect" : "";

  const authApiBase: string = useMemo(() => {
    const isLocal =
      host === "localhost" ||
      host === "127.0.0.1" ||
      host.endsWith(".local");
    if (isLocal) {
      // LOCAL DEV: OAuth lives on the dedicated auth-service Tomcat port 8081,
      // NOT on the blog-service 8090.  Blog service has NO OAuth2Controller so
      // routing there produced the <APIResponse> 404 XML document the user saw.
      return "http://localhost:8081";
    }
    // Production / staging — auth endpoints sit behind the same ingress as the
    // rest of the REST API.
    return SITE.apiBase;
  }, [host]);

  const urls = useMemo(
    () => socialAuthUrls(authApiBase, redirectUri || undefined),
    [authApiBase, redirectUri],
  );

  const sizeClasses =
    size === "sm" ? "py-2 text-xs" : size === "md" ? "py-2.5 text-sm" : "py-3 text-sm";
  const blockClass = block ? "w-full" : "min-w-[220px]";
  const anyBusy = googleBusy || facebookBusy;

  return (
    <div className={vertical ? "space-y-3" : "grid grid-cols-2 gap-3"}>
      {err ? (
        <div className="col-span-full border-2 border-red-500/40 bg-red-50 text-red-700 text-xs uppercase tracking-widest font-bold p-2">
          {err}
        </div>
      ) : null}
      <button
        type="button"
        onClick={(e) => {
          e.preventDefault();
          initiateSocialLogin(urls.google, setGoogleBusy, setErr);
        }}
        disabled={anyBusy}
        className={`${blockClass} ${sizeClasses} inline-flex items-center justify-center gap-3 border-2 border-ink-950/15 bg-white text-ink-950 font-bold uppercase tracking-wide rounded-sm hover:border-ink-950/40 hover:-translate-y-0.5 transition disabled:opacity-60 disabled:hover:translate-y-0 disabled:cursor-not-allowed`}
      >
        <svg viewBox="0 0 48 48" className="h-5 w-5" aria-hidden="true">
          <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.8 32.4 29.4 35.5 24 35.5c-6.4 0-11.5-5.1-11.5-11.5S17.6 12.5 24 12.5c2.9 0 5.5 1.1 7.5 2.9l5.6-5.6C33.7 6.3 29.1 4.5 24 4.5 13.2 4.5 4.5 13.2 4.5 24S13.2 43.5 24 43.5c11.2 0 20.3-8 20.3-19.6 0-1.2-.1-2.2-.3-3.4z" />
          <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.9 18.9 12.5 24 12.5c2.9 0 5.5 1.1 7.5 2.9l5.6-5.6C33.7 6.3 29.1 4.5 24 4.5 16.3 4.5 9.7 8.9 6.3 14.7z" />
          <path fill="#4CAF50" d="M24 43.5c5.3 0 10.2-2 13.9-5.4l-6.4-5.3c-2 1.4-4.5 2.3-7.5 2.3-5.4 0-9.9-3.1-11.3-7.5l-6.5 5C9.5 38.8 16.1 43.5 24 43.5z" />
          <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.3-2.3 4.3-4.3 5.5l6.4 5.3C40 36.1 44.3 30.8 44.3 24c0-1.2-.1-2.2-.3-3.4L43.6 20.5z" />
        </svg>
        {googleBusy ? "Redirecting to Google…" : "Continue with Google"}
      </button>
      <button
        type="button"
        onClick={(e) => {
          e.preventDefault();
          initiateSocialLogin(urls.facebook, setFacebookBusy, setErr);
        }}
        disabled={anyBusy}
        className={`${blockClass} ${sizeClasses} inline-flex items-center justify-center gap-3 border-2 border-[#1877F2]/20 bg-[#1877F2] text-white font-bold uppercase tracking-wide rounded-sm hover:bg-[#166FE5] hover:-translate-y-0.5 transition disabled:opacity-60 disabled:hover:translate-y-0 disabled:cursor-not-allowed`}
      >
        <svg viewBox="0 0 24 24" className="h-5 w-5" fill="currentColor" aria-hidden="true">
          <path d="M24 12.073C24 5.454 18.627 0 12 0S0 5.454 0 12.073C0 18.118 4.387 23.144 10.125 24v-8.438H7.078v-3.49h3.047V9.426c0-3.006 1.791-4.667 4.532-4.667 1.313 0 2.685.234 2.685.234v2.954H15.7c-1.492 0-1.957.925-1.957 1.874v2.254h3.327l-.533 3.49h-2.794V24C19.613 23.144 24 18.118 24 12.073z" />
        </svg>
        {facebookBusy ? "Redirecting to Facebook…" : "Continue with Facebook"}
      </button>
    </div>
  );
}

export function OtpModeToggle({
  mode,
  setMode,
}: {
  mode: "password" | "otp";
  setMode: (m: "password" | "otp") => void;
}) {
  return (
    <div className="flex items-center justify-between text-[11px] font-bold uppercase tracking-widest">
      <span className="text-ink-600">
        {mode === "password" ? "Have your password?" : "Prefer a one-time code?"}
      </span>
      <button
        type="button"
        onClick={() => setMode(mode === "password" ? "otp" : "password")}
        className="text-news hover:underline"
      >
        {mode === "password" ? "Use OTP" : "Use password"}
      </button>
    </div>
  );
}

export function LoginNoticeBanner({ text }: { text: string }) {
  const [open, setOpen] = useState(true);
  useEffect(() => {
    const t = setTimeout(() => setOpen(false), 6000);
    return () => clearTimeout(t);
  }, []);
  if (!open) return null;
  return (
    <div className="mb-4 border-2 border-ink-950/15 bg-ink-950 text-white p-3 text-xs flex items-start justify-between gap-2">
      <div>
        <span className="uppercase tracking-widest font-bold mr-2">Tip</span>
        {text}
      </div>
      <button onClick={() => setOpen(false)} className="opacity-60 hover:opacity-100">
        ×
      </button>
    </div>
  );
}
