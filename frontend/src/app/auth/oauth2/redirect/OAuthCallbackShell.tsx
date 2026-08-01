"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth/AuthProvider";

type Status = "loading" | "success" | "error";

export const providerLabels: Record<string, string> = {
  google: "Google",
  facebook: "Facebook",
};

export type OAuthCallbackStyle = {
  badge: string;
  ring: string;
  dot: string;
};

export const providerStyles: Record<string, OAuthCallbackStyle> = {
  google: {
    badge: "border-[#4285F4]/20 bg-[#4285F4]/5 text-[#4285F4]",
    ring: "ring-[#4285F4]/20",
    dot: "bg-[#4285F4]",
  },
  facebook: {
    badge: "border-[#1877F2]/20 bg-[#1877F2]/5 text-[#1877F2]",
    ring: "ring-[#1877F2]/20",
    dot: "bg-[#1877F2]",
  },
};

function ProviderIcon({ provider }: { provider: string }) {
  const p = (provider || "").toLowerCase();
  if (p === "google") {
    return (
      <svg viewBox="0 0 48 48" className="h-6 w-6" aria-hidden="true">
        <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.8 32.4 29.4 35.5 24 35.5c-6.4 0-11.5-5.1-11.5-11.5S17.6 12.5 24 12.5c2.9 0 5.5 1.1 7.5 2.9l5.6-5.6C33.7 6.3 29.1 4.5 24 4.5 13.2 4.5 4.5 13.2 4.5 24S13.2 43.5 24 43.5c11.2 0 20.3-8 20.3-19.6 0-1.2-.1-2.2-.3-3.4z" />
        <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.9 18.9 12.5 24 12.5c2.9 0 5.5 1.1 7.5 2.9l5.6-5.6C33.7 6.3 29.1 4.5 24 4.5 16.3 4.5 9.7 8.9 6.3 14.7z" />
        <path fill="#4CAF50" d="M24 43.5c5.3 0 10.2-2 13.9-5.4l-6.4-5.3c-2 1.4-4.5 2.3-7.5 2.3-5.4 0-9.9-3.1-11.3-7.5l-6.5 5C9.5 38.8 16.1 43.5 24 43.5z" />
        <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.3-2.3 4.3-4.3 5.5l6.4 5.3C40 36.1 44.3 30.8 44.3 24c0-1.2-.1-2.2-.3-3.4L43.6 20.5z" />
      </svg>
    );
  }
  if (p === "facebook") {
    return (
      <svg viewBox="0 0 24 24" className="h-6 w-6 fill-[#1877F2]" aria-hidden="true">
        <path d="M24 12.073C24 5.454 18.627 0 12 0S0 5.454 0 12.073C0 18.118 4.387 23.144 10.125 24v-8.438H7.078v-3.49h3.047V9.426c0-3.006 1.791-4.667 4.532-4.667 1.313 0 2.685.234 2.685.234v2.954H15.7c-1.492 0-1.957.925-1.957 1.874v2.254h3.327l-.533 3.49h-2.794V24C19.613 23.144 24 18.118 24 12.073z" />
      </svg>
    );
  }
  return <div className="h-6 w-6 rounded-full bg-ink-950/10" />;
}

export type OAuthCallbackShellProps = {
  provider: string;
  email: string;
  firstName: string;
  lastName: string;
  picture?: string;
  providerId: string;
  success?: string;
  error?: string;
  errorDescription?: string;
  displayName: string;
  style: OAuthCallbackStyle;
};

export default function OAuthCallbackShell(props: OAuthCallbackShellProps) {
  const {
    provider,
    email,
    firstName,
    lastName,
    picture,
    providerId,
    success: successParam,
    error: errorParam,
    errorDescription,
    displayName,
    style,
  } = props;

  const router = useRouter();
  const auth = useAuth();

  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string>("");

  useEffect(() => {
    let cancelled = false;
    let t: ReturnType<typeof setTimeout> | undefined;

    async function run() {
      if (errorParam) {
        setStatus("error");
        setError(
          errorDescription ||
            errorParam === "access_denied"
              ? "You cancelled the sign-in. Please try again."
              : `${providerLabels[provider] || "OAuth"} sign-in failed.`,
        );
        return;
      }

      if (successParam !== "true") {
        setStatus("error");
        setError("Invalid OAuth response. Please try signing in again.");
        return;
      }

      if (!email || !provider || !providerId) {
        setStatus("error");
        setError("Your sign-in data is incomplete. Please try again.");
        return;
      }

      try {
        await auth.oauth2Callback({
          email,
          firstName,
          lastName,
          avatarUrl: picture,
          profileImageUrl: picture,
          provider,
          providerId,
        });
        if (cancelled) return;
        setStatus("success");
        t = setTimeout(() => {
          router.replace("/dashboard");
        }, 800);
      } catch (e: any) {
        if (cancelled) return;
        const msg =
          e?.response?.data?.message ||
          e?.message ||
          "Sign-in failed on our end. Please try again.";
        setStatus("error");
        setError(msg);
      }
    }

    run();
    return () => {
      cancelled = true;
      if (t) clearTimeout(t);
    };
  }, [
    auth,
    email,
    firstName,
    lastName,
    picture,
    provider,
    providerId,
    successParam,
    errorParam,
    errorDescription,
    router,
  ]);

  return (
    <div className="flex flex-col items-center text-center">
      <div className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-[11px] font-bold uppercase tracking-widest ${style.badge}`}>
        <ProviderIcon provider={provider} />
        <span>{providerLabels[provider] || provider || "OAuth"} sign-in</span>
      </div>

      {displayName && status === "loading" && (
        <p className="mt-6 text-sm text-ink-600">
          Welcome back, <span className="font-semibold text-ink-950">{displayName}</span>
        </p>
      )}

      <div className="mt-8 relative h-20 w-20">
        {status === "loading" && (
          <>
            <span className={`absolute inset-0 rounded-full ${style.dot} opacity-20 animate-ping`} />
            <span className={`absolute inset-0 rounded-full ${style.dot} opacity-30 animate-pulse`} />
            <div className="absolute inset-0 flex items-center justify-center">
              <svg className="h-9 w-9 animate-spin text-white" viewBox="0 0 50 50" aria-hidden="true">
                <circle cx="25" cy="25" r="20" fill="none" stroke="currentColor" strokeOpacity="0.15" strokeWidth="6" />
                <path fill="currentColor" d="M43.9 21c-.4-2.2-1.3-4.3-2.6-6.1l-3.5 3.5c1.3 2.7 1.9 5.7 1.7 8.6H43.9c.1-2.1-.1-4.1-.9-6z" />
              </svg>
            </div>
          </>
        )}
        {status === "success" && (
          <div className={`h-20 w-20 rounded-full ${style.dot} flex items-center justify-center ring-4 ring-white shadow-lg`}>
            <svg viewBox="0 0 24 24" className="h-11 w-11 text-white" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
        )}
        {status === "error" && (
          <div className="h-20 w-20 rounded-full bg-red-500 flex items-center justify-center ring-4 ring-white shadow-lg">
            <svg viewBox="0 0 24 24" className="h-11 w-11 text-white" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </div>
        )}
      </div>

      <h1 className="mt-7 text-2xl font-black tracking-tight">
        {status === "loading" && "Signing you in…"}
        {status === "success" && "Signed in successfully"}
        {status === "error" && "Couldn't complete sign-in"}
      </h1>

      <p className="mt-3 text-sm text-ink-600 max-w-sm">
        {status === "loading" && (
          <>We're finishing your {providerLabels[provider] || "OAuth"} sign-in. You'll be taken to your dashboard in a moment.</>
        )}
        {status === "success" && <>Redirecting you to your dashboard…</>}
        {status === "error" && <>{error || "Something went wrong. Please try again."}</>}
      </p>

      {status === "success" && (
        <Link
          href="/dashboard"
          className="mt-7 inline-flex items-center gap-2 rounded-sm bg-news px-5 py-3 text-sm font-bold uppercase tracking-wide text-white hover:bg-news/90"
        >
          Go to dashboard now →
        </Link>
      )}

      {status === "error" && (
        <div className="mt-7 w-full grid grid-cols-2 gap-3">
          <Link
            href="/login"
            className="inline-flex items-center justify-center rounded-sm border-2 border-ink-950/15 bg-white px-4 py-3 text-sm font-bold uppercase tracking-wide text-ink-950 hover:bg-ink-950/5"
          >
            Back to login
          </Link>
          <Link
            href="/signup"
            className="inline-flex items-center justify-center rounded-sm bg-news px-4 py-3 text-sm font-bold uppercase tracking-wide text-white hover:bg-news/90"
          >
            Create account
          </Link>
        </div>
      )}

      {status === "loading" && (
        <div className="mt-8 w-full">
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-ink-950/10">
            <div className={`h-full ${style.dot} animate-[oas_1.8s_ease-in-out_infinite]`} style={{ width: "60%" }} />
          </div>
        </div>
      )}

      <style jsx>{`
        @keyframes oas {
          0% { transform: translateX(-60%); }
          50% { transform: translateX(160%); }
          100% { transform: translateX(260%); }
        }
      `}</style>
    </div>
  );
}
