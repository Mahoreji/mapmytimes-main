import Link from "next/link";
import { Suspense } from "react";
import { BrandLogo } from "@/components/site/SiteHeader";
import OAuthCallbackShell from "./OAuthCallbackShell";

export const dynamic = "force-dynamic";
export const fetchCache = "force-no-store";
export const dynamicParams = true;
export const revalidate = 0;

type OAuthSearchParams = {
  provider?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  picture?: string;
  providerId?: string;
  success?: string;
  error?: string;
  error_description?: string;
};

type OAuthRedirectPageProps = {
  searchParams: Promise<OAuthSearchParams>;
};

const providerLabels: Record<string, string> = {
  google: "Google",
  facebook: "Facebook",
};

const providerStyles: Record<string, { badge: string; ring: string; dot: string }> = {
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

export default async function OAuth2RedirectPage({ searchParams: spPromise }: OAuthRedirectPageProps) {
  const sp = await spPromise;
  const provider = (sp.provider || "").toLowerCase() || "unknown";
  const email = sp.email ?? "";
  const firstName = sp.firstName ?? "";
  const lastName = sp.lastName ?? "";
  const picture = sp.picture;
  const providerId = sp.providerId ?? "";
  const successParam = sp.success;
  const errorParam = sp.error;
  const errorDescription = sp.error_description;

  const displayName = firstName && lastName
    ? `${firstName} ${lastName}`.trim()
    : firstName || lastName || email || "";

  const style = providerStyles[provider] ?? {
    badge: "border-ink-950/15 bg-ink-950/5 text-ink-950",
    ring: "ring-ink-950/10",
    dot: "bg-ink-950/50",
  };

  return (
    <div className="min-h-screen w-full bg-gradient-to-br from-ink-950 via-ink-950 to-ink-950 text-ink-100">
      <div className="min-h-screen flex flex-col items-center justify-center px-5 py-12">
        <Link
          href="/"
          className="group flex items-center justify-center mb-10"
          aria-label="MapMyTimes home"
        >
          <BrandLogo className="h-16 w-auto drop-shadow-[0_6px_0_rgba(255,255,255,0.04)]" variant="inverted" />
        </Link>

        <div className={`w-full max-w-md rounded-2xl bg-white text-ink-950 shadow-2xl ring-1 ${style.ring} ring-inset p-8`}>
          <Suspense fallback={<OAuthSkeleton style={style} />}>
            <OAuthCallbackShell
              provider={provider}
              email={email}
              firstName={firstName}
              lastName={lastName}
              picture={picture}
              providerId={providerId}
              success={successParam}
              error={errorParam}
              errorDescription={errorDescription}
              displayName={displayName}
              style={style}
            />
          </Suspense>
        </div>

        <p className="mt-8 text-xs text-ink-500 max-w-md text-center">
          Secured by MapMyTimes · By continuing you agree to our{" "}
          <Link href="/terms" className="underline hover:text-ink-300">Terms</Link> &{" "}
          <Link href="/privacy" className="underline hover:text-ink-300">Privacy Policy</Link>.
        </p>
      </div>
    </div>
  );
}

function OAuthSkeleton({ style }: { style: { badge: string; ring: string; dot: string } }) {
  return (
    <div className="flex flex-col items-center text-center">
      <div className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-[11px] font-bold uppercase tracking-widest ${style.badge}`}>
        <div className="h-6 w-6 animate-pulse rounded-full bg-current/20" />
        <span>OAuth sign-in</span>
      </div>
      <div className="mt-8 relative h-20 w-20">
        <span className={`absolute inset-0 rounded-full ${style.dot} opacity-20 animate-ping`} />
        <div className="absolute inset-0 flex items-center justify-center">
          <svg className="h-9 w-9 animate-spin text-white" viewBox="0 0 50 50" aria-hidden="true">
            <circle cx="25" cy="25" r="20" fill="none" stroke="currentColor" strokeOpacity="0.15" strokeWidth="6" />
          </svg>
        </div>
      </div>
      <h1 className="mt-7 text-2xl font-black tracking-tight">Signing you in…</h1>
      <p className="mt-3 text-sm text-ink-600 max-w-sm">Loading OAuth response…</p>
    </div>
  );
}
