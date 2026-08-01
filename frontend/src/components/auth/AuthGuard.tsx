"use client";

import { useEffect } from "react";
import { useAuth } from "@/lib/auth/AuthProvider";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import { cn } from "@/lib/utils";

const PUBLIC_AUTH_PATHS = new Set(["/login", "/signup", "/verify", "/forgot-password", "/reset-password"]);

export function AuthGuard({
  children,
  requireAuth = false,
  requireGuest = false,
  redirectTo,
  className,
}: {
  children: React.ReactNode;
  requireAuth?: boolean;
  requireGuest?: boolean;
  redirectTo?: string;
  className?: string;
}) {
  const { status, isAuthenticated } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "loading") return;
    if (requireAuth && !isAuthenticated) {
      const target = redirectTo ?? `/login?next=${encodeURIComponent(pathname || "/dashboard")}`;
      router.replace(target);
    } else if (requireGuest && isAuthenticated) {
      router.replace(redirectTo ?? "/dashboard");
    }
  }, [status, isAuthenticated, requireAuth, requireGuest, redirectTo, router, pathname]);

  if (status === "loading") {
    return (
      <div className={cn("flex min-h-[40vh] items-center justify-center", className)}>
        <div className="flex items-center gap-3 text-sm text-ink-700">
          <span className="h-3 w-3 rounded-full bg-news animate-pulseDot" />
          Loading…
        </div>
      </div>
    );
  }

  if (requireAuth && !isAuthenticated) {
    return (
      <div className={cn("flex min-h-[40vh] flex-col items-center justify-center gap-4", className)}>
        <h2 className="font-headline text-2xl uppercase tracking-wide">Sign in required</h2>
        <p className="text-sm text-ink-700">
          This page is only available to journalists and editors.
        </p>
        <Link
          href={redirectTo ?? "/login"}
          className="bg-news text-white font-sans font-semibold px-4 py-2 shadow-hard-sm hover:shadow-hard transition-shadow"
        >
          Go to sign in
        </Link>
      </div>
    );
  }

  if (requireGuest && isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}

export function isAuthPublicPage(path: string) {
  return PUBLIC_AUTH_PATHS.has(path);
}
