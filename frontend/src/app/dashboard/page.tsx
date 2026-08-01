"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { PageHeader, Card, StatCard } from "@/components/dashboard/Panels";
import { blogApi } from "@/lib/api/blogApi";
import { notificationApi } from "@/lib/api/notificationApi";
import type { BlogStatsResponse } from "@/types/blog";
import type { BlogPostSummaryResponse } from "@/types/blog";
import type { Notification } from "@/types/notification";
import {
  FileText,
  Eye,
  MessageSquare,
  Heart,
  Bell,
  Plus,
  ChevronRight,
  CheckCircle2,
} from "lucide-react";
import { Button } from "@/components/ui/Button";
import { PostCard } from "@/components/posts/PostCard";
import { cn, formatRelative } from "@/lib/utils";

export default function DashboardHome() {
  const sp = useSearchParams();
  const [stats, setStats] = useState<BlogStatsResponse | null>(null);
  const [recent, setRecent] = useState<BlogPostSummaryResponse[]>([]);
  const [unread, setUnread] = useState(0);
  const [alerts, setAlerts] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [justVerified, setJustVerified] = useState(false);

  useEffect(() => {
    if (sp?.get("verified") === "1") setJustVerified(true);
    let active = true;
    Promise.all([
      blogApi.settings.stats().catch(() => null),
      blogApi.posts.mine({ page: 0, size: 6 }).catch(() => ({ content: [] }) as any),
      notificationApi.unreadCount().catch(() => ({ unread: 0 })),
      notificationApi.list({ page: 0, size: 5 }).catch(() => [] as any),
    ]).then(([s, r, u, a]) => {
      if (!active) return;
      setStats(s);
      setRecent((r.content ?? []) as BlogPostSummaryResponse[]);
      setUnread(typeof u === "number" ? u : u.unread ?? 0);
      const arr = Array.isArray(a) ? a : ((a as any).content ?? []);
      setAlerts(arr.slice(0, 5));
      setLoading(false);
    });
    return () => { active = false; };
  }, [sp]);

  return (
    <>
      <PageHeader
        eyebrow="Newsroom"
        title="Your dashboard"
        description="Publish stories, manage comments, and follow the pulse of MapMyTimes — all in one place."
        action={
          <Link href="/dashboard/posts/new">
            <Button variant="news" size="lg">
              <Plus className="h-4 w-4" />
              Write a story
            </Button>
          </Link>
        }
      />

      {justVerified ? (
        <Card className="bg-ink-950 text-white border-news !p-0 overflow-hidden">
          <div className="flex items-start gap-4 p-5 sm:p-6">
            <div className="h-11 w-11 rounded-full bg-news text-white flex items-center justify-center border-2 border-white flex-shrink-0">
              <CheckCircle2 className="h-6 w-6" />
            </div>
            <div className="min-w-0 flex-1">
              <h2 className="font-headline text-2xl uppercase leading-none">
                Welcome to the MapMyTimes newsroom.
              </h2>
              <p className="mt-2 text-sm text-white/80">
                Your email is verified. Start writing your first story, customize your profile, and
                tune in to newsroom alerts.
              </p>
              <div className="mt-4 flex flex-wrap gap-2">
                <Link href="/dashboard/posts/new">
                  <Button variant="news">Write first story</Button>
                </Link>
                <Link href="/dashboard/settings">
                  <Button
                    variant="outline"
                    className="bg-transparent text-white border-white hover:bg-white hover:text-ink-950"
                  >
                    Set up profile
                  </Button>
                </Link>
              </div>
            </div>
          </div>
        </Card>
      ) : null}

      <section className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
        <StatCard
          tone="ink"
          label="Posts"
          value={loading ? "—" : (stats?.totalPosts ?? 0).toLocaleString("en-IN")}
          icon={<FileText className="h-5 w-5" />}
        />
        <StatCard
          tone="news"
          label="Published"
          value={loading ? "—" : (stats?.publishedPosts ?? 0).toLocaleString("en-IN")}
          icon={<FileText className="h-5 w-5" />}
        />
        <StatCard
          tone="outline"
          label="Views"
          value={loading ? "—" : (stats?.totalViews ?? 0).toLocaleString("en-IN")}
          icon={<Eye className="h-5 w-5" />}
        />
        <StatCard
          tone="outline"
          label="Comments"
          value={loading ? "—" : (stats?.totalComments ?? 0).toLocaleString("en-IN")}
          icon={<MessageSquare className="h-5 w-5" />}
        />
        <StatCard
          tone="ink"
          label="Unread alerts"
          value={loading ? "—" : unread.toLocaleString("en-IN")}
          icon={<Bell className="h-5 w-5" />}
        />
      </section>

      <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2 space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <div className="ribbon text-xs mb-2">Recent</div>
              <h2 className="font-headline text-xl uppercase leading-none">Your recent stories</h2>
            </div>
            <Link
              href="/dashboard/posts"
              className="text-xs font-bold uppercase tracking-widest hover:text-news inline-flex items-center gap-1"
            >
              All posts <ChevronRight className="h-3.5 w-3.5" />
            </Link>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="animate-pulse border-2 border-ink-950 p-3 space-y-2">
                  <div className="aspect-[16/10] bg-ink-900/10 border-2 border-ink-950" />
                  <div className="h-4 w-2/3 bg-ink-900/20" />
                </div>
              ))}
            </div>
          ) : recent.length === 0 ? (
            <div className="border-2 border-ink-950 border-dashed p-8 text-center">
              <h3 className="font-headline text-xl uppercase mb-2">You haven&apos;t published yet</h3>
              <p className="text-sm text-ink-700 mb-4">
                Drafts and published stories will appear here once you create them.
              </p>
              <Link href="/dashboard/posts/new">
                <Button variant="news">
                  <Plus className="h-4 w-4" />
                  Write your first story
                </Button>
              </Link>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {recent.map((p) => (
                <div key={p.id} className="group">
                  <PostCard post={p} variant="sm" />
                  <div className="mt-1 flex items-center justify-between px-1 text-[11px] uppercase tracking-widest font-bold text-ink-600">
                    <span
                      className={cn(
                        "px-1.5 py-0.5 border-2",
                        p.status === "PUBLISHED"
                          ? "border-news text-news bg-news-50"
                          : p.status === "DRAFT"
                            ? "border-ink-950 text-ink-950"
                            : p.status === "SCHEDULED"
                              ? "border-ink-700 text-ink-800"
                              : "border-ink-600 text-ink-700",
                      )}
                    >
                      {p.status}
                    </span>
                    <Link
                      href={`/dashboard/posts/${encodeURIComponent(p.id)}`}
                      className="hover:text-news"
                    >
                      Edit →
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <div className="ribbon text-xs mb-2">Newsroom</div>
              <h2 className="font-headline text-xl uppercase leading-none">Recent alerts</h2>
            </div>
            <Link
              href="/dashboard/notifications"
              className="text-xs font-bold uppercase tracking-widest hover:text-news"
            >
              View all
            </Link>
          </div>
          <ul className="flex flex-col divide-y divide-ink-950/10 -mx-2">
            {loading
              ? [0, 1, 2, 3].map((i) => (
                  <li key={i} className="p-3 space-y-2">
                    <div className="h-3 w-2/3 bg-ink-900/10 animate-pulse" />
                    <div className="h-3 w-1/2 bg-ink-900/10 animate-pulse" />
                  </li>
                ))
              : alerts.length === 0
                ? (
                  <li className="p-6 text-center text-sm text-ink-600">
                    <Bell className="h-6 w-6 mx-auto text-ink-400 mb-2" />
                    No alerts right now.
                  </li>
                )
                : alerts.map((n) => (
                    <li key={n.id} className="px-2 py-3 flex gap-3 items-start hover:bg-ink-900/5">
                      <span
                        className={cn(
                          "mt-1 h-2 w-2 rounded-full flex-shrink-0",
                          n.read ? "bg-ink-400" : "bg-news animate-pulseDot",
                        )}
                      />
                      <div className="min-w-0">
                        <div className="text-xs font-bold uppercase tracking-wider">{n.title}</div>
                        <p className="text-xs text-ink-700 mt-0.5 line-clamp-2">{n.message}</p>
                        <div className="mt-1 text-[10px] uppercase tracking-widest text-ink-600 font-semibold">
                          {formatRelative(n.createdAt)}
                        </div>
                      </div>
                    </li>
                  ))}
          </ul>
        </Card>
      </section>
    </>
  );
}
