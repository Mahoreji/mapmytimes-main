"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import {
  LayoutDashboard,
  FileText,
  MessageSquareText,
  Bell,
  Settings as SettingsIcon,
  LogOut,
  Plus,
  Mic,
  User as UserIcon,
  Briefcase,
  ShieldCheck,
} from "lucide-react";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { useAuth } from "@/lib/auth/AuthProvider";
import { cn, initials, SITE } from "@/lib/utils";
import { avatarOrDefault } from "@/lib/assets";
import { PageHeader, Card, StatCard } from "@/components/dashboard/Panels";

export default function DashboardLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const auth = useAuth();

  const role = auth.user?.role;
  const hasPressIdAccess =
    role === "PRESS_REPORTER" ||
    role === "STAFF_ADMIN" ||
    role === "ADMIN" ||
    role === "SUPER_ADMIN";

  const NAV = [
    { href: "/dashboard",         label: "Dashboard",   icon: <LayoutDashboard className="h-4 w-4" /> },
    ...(hasPressIdAccess
      ? [
          {
            href: "/dashboard/my-id",
            label: "My ID Card",
            icon: <ShieldCheck className="h-4 w-4" />,
          },
        ]
      : []),
    { href: "/dashboard/posts",   label: "My Posts",    icon: <FileText className="h-4 w-4" /> },
    { href: "/dashboard/comments", label: "Moderation", icon: <MessageSquareText className="h-4 w-4" /> },
    { href: "/dashboard/applications", label: "My Apps", icon: <Briefcase className="h-4 w-4" /> },
    { href: "/dashboard/notifications", label: "Alerts", icon: <Bell className="h-4 w-4" /> },
    { href: "/dashboard/settings", label: "Settings", icon: <SettingsIcon className="h-4 w-4" /> },
  ];

  return (
    <AuthGuard requireAuth>
      <div className="min-h-[calc(100vh-200px)] bg-ink-900/5">
        <div className="mx-auto max-w-7xl px-4 py-6 grid grid-cols-1 lg:grid-cols-[240px_1fr] gap-6">
          <aside className="h-fit lg:sticky lg:top-28">
            <div className="border-2 border-ink-950 bg-white shadow-hard-sm overflow-hidden">
              <div className="bg-ink-950 text-white p-4 border-b-2 border-ink-950 flex items-center gap-3">
                <div className="h-11 w-11 rounded-full bg-ink-800 flex items-center justify-center font-bold border-2 border-white overflow-hidden flex-shrink-0">
                  <img src={avatarOrDefault(auth.user?.profileImageUrl)} alt="" className="h-full w-full object-cover" />
                </div>
                <div className="min-w-0">
                  <div className="font-headline uppercase text-xs tracking-widest">Newsroom</div>
                  <div className="text-sm font-bold truncate">
                    {auth.user?.firstName} {auth.user?.lastName}
                  </div>
                </div>
              </div>
              <nav className="p-2 flex flex-col gap-1">
                {NAV.map((n) => {
                  const active =
                    pathname === n.href ||
                    (n.href !== "/dashboard" && pathname?.startsWith(n.href + "/"));
                  return (
                    <Link
                      key={n.href}
                      href={n.href}
                      className={cn(
                        "h-10 px-3 text-xs font-bold uppercase tracking-widest flex items-center gap-2 border-2 transition-colors",
                        active
                          ? "bg-news text-white border-news"
                          : "bg-white text-ink-950 border-transparent hover:border-ink-950",
                      )}
                    >
                      {n.icon}
                      {n.label}
                    </Link>
                  );
                })}
                <div className="mt-2">
                  <Link
                    href="/dashboard/posts/new"
                    className="h-10 px-3 text-xs font-bold uppercase tracking-widest flex items-center justify-center gap-2 bg-ink-950 text-white hover:bg-news transition-colors border-2 border-ink-950"
                  >
                    <Plus className="h-4 w-4" />
                    New Post
                  </Link>
                </div>
                <button
                  type="button"
                  onClick={() => void auth.logout()}
                  className="mt-2 h-10 px-3 text-xs font-bold uppercase tracking-widest flex items-center gap-2 border-2 border-transparent hover:border-news hover:bg-news-50 text-news-700"
                >
                  <LogOut className="h-4 w-4" />
                  Sign out
                </button>
              </nav>
              <div className="border-t-2 border-ink-950 p-3 bg-white">
                <div className="text-[10px] uppercase tracking-[0.25em] font-bold text-ink-600">
                  {SITE.name}
                </div>
                <div className="mt-1 flex items-center gap-1.5 text-[11px] uppercase tracking-widest text-ink-700">
                  <Mic className="h-3 w-3 text-news" />
                  {SITE.tagline}
                </div>
              </div>
            </div>
          </aside>

          <div className="space-y-6 min-w-0">
            {children}
          </div>
        </div>
      </div>
    </AuthGuard>
  );
}
