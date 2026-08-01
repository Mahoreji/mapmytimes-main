"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { Mic } from "lucide-react";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { SITE } from "@/lib/utils";
import { BrandLogo } from "@/components/site/SiteHeader";

export default function AuthShell({
  eyebrow,
  title,
  subtitle,
  children,
  footer,
}: {
  eyebrow: string;
  title: string;
  subtitle?: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <AuthGuard requireGuest redirectTo="/dashboard">
      <div className="min-h-[calc(100vh-200px)] grid grid-cols-1 lg:grid-cols-2 bg-white border-b-2 border-ink-950">
        <div className="hidden lg:flex flex-col justify-between bg-ink-950 text-white p-10 xl:p-14 border-r-2 border-ink-950">
          <Link href="/" aria-label="MapMyTimes — Home" className="inline-block w-full flex items-start justify-start mb-6">
            <BrandLogo className="h-56 sm:h-64 md:h-72 xl:h-80 w-auto max-w-full drop-shadow-[0_6px_0_rgba(227,30,36,0.35)]" />
          </Link>

          <div className="space-y-5 max-w-2xl">
            <div className="ribbon text-xs">Journalists · Editors · Newsroom</div>
            <h2 className="font-headline text-4xl xl:text-5xl uppercase leading-none">
              Publish stories that matter.
            </h2>
            <p className="text-white/75 leading-relaxed">
              Your MapMyTimes account gives you access to the editor&apos;s dashboard, post
              composer, comment moderation, and personalised notifications.
            </p>
          </div>

          <ul className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm mt-10">
            {[
              ["Post composer", "Rich editor, SEO, media"],
              ["Moderation", "Approve or reject comments"],
              ["Notifications", "Real-time newsroom alerts"],
            ].map(([k, v]) => (
              <li key={k} className="border border-white/15 p-3">
                <div className="font-bold uppercase tracking-widest text-xs text-news">{k}</div>
                <div className="mt-1 text-white/75 text-xs">{v}</div>
              </li>
            ))}
          </ul>
        </div>

        <div className="flex items-center justify-center p-6 sm:p-10 lg:p-14">
          <div className="w-full max-w-md space-y-6">
            <div className="space-y-2">
              <div className="ribbon text-xs w-fit">{eyebrow}</div>
              <h1 className="font-headline text-3xl sm:text-4xl uppercase leading-none">
                {title}
              </h1>
              {subtitle ? (
                <p className="text-sm text-ink-700 leading-relaxed">{subtitle}</p>
              ) : null}
            </div>

            <div className="border-2 border-ink-950 bg-white shadow-hard-sm p-5 sm:p-6 space-y-5">
              {children}
            </div>

            {footer}
          </div>
        </div>
      </div>
    </AuthGuard>
  );
}

export function AuthAlert({
  type,
  children,
}: {
  type: "error" | "info" | "success";
  children: ReactNode;
}) {
  const cls =
    type === "error"
      ? "border-news bg-news-50 text-news-700"
      : type === "success"
        ? "border-ink-950 bg-ink-900 text-white"
        : "border-ink-950 bg-white text-ink-800";
  return (
    <div className={`border-2 ${cls} p-3 text-sm`}>
      {type === "error" ? (
        <div className="flex gap-2 items-start">
          <span aria-hidden>⚠️</span>
          <div>{children}</div>
        </div>
      ) : type === "success" ? (
        <div className="flex gap-2 items-start">
          <Mic className="h-4 w-4 mt-0.5 text-news flex-shrink-0" />
          <div>{children}</div>
        </div>
      ) : (
        children
      )}
    </div>
  );
}
