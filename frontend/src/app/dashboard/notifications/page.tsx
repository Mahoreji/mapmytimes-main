"use client";

import { useEffect, useState } from "react";
import { PageHeader, Card } from "@/components/dashboard/Panels";
import { notificationApi } from "@/lib/api/notificationApi";
import type { Notification } from "@/types/notification";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/posts/PostCard";
import {
  Bell,
  Check,
  CheckCheck,
  Trash2,
  Inbox,
  Mail,
  RefreshCw,
} from "lucide-react";
import { cn, formatDateTime } from "@/lib/utils";
import { getApiError } from "@/lib/api/client";

export default function NotificationsPage() {
  const [items, setItems] = useState<Notification[]>([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  async function refresh() {
    setLoading(true);
    setErr("");
    try {
      const [list, u] = await Promise.all([
        notificationApi.list({ page: 0, size: 100 }).catch(() => [] as any),
        notificationApi.unreadCount().catch(() => ({ unread: 0 })),
      ]);
      const arr = Array.isArray(list) ? list : ((list as any).content ?? []);
      setItems(arr);
      setUnread(typeof u === "number" ? u : u.unread ?? 0);
    } catch (e) {
      setErr(getApiError(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void refresh();
  }, []);

  async function markRead(id: string) {
    try {
      await notificationApi.markRead(id);
      setItems((list) => list.map((n) => (n.id === id ? { ...n, read: true } : n)));
      setUnread((u) => Math.max(0, u - 1));
    } catch (e) {
      setErr(getApiError(e));
    }
  }

  async function markAll() {
    try {
      await notificationApi.markAllRead();
      setItems((list) => list.map((n) => ({ ...n, read: true })));
      setUnread(0);
    } catch (e) {
      setErr(getApiError(e));
    }
  }

  async function remove(id: string) {
    try {
      await notificationApi.delete(id);
      setItems((list) => list.filter((n) => n.id !== id));
    } catch (e) {
      setErr(getApiError(e));
    }
  }

  async function clearAll() {
    if (!confirm("Delete every notification in this list?")) return;
    try {
      await notificationApi.deleteAll();
      setItems([]);
      setUnread(0);
    } catch (e) {
      setErr(getApiError(e));
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Newsroom"
        title="Notifications & alerts"
        description="All alerts, pings, and approvals from across MapMyTimes — in one place."
        action={
          <div className="flex items-center gap-2">
            <Badge variant={unread > 0 ? "news" : "outline"} className="!py-1 !px-2">
              <Bell className="h-3.5 w-3.5" /> {unread} unread
            </Badge>
            <Button variant="outline" size="sm" onClick={refresh}>
              <RefreshCw className="h-4 w-4" />
              Refresh
            </Button>
          </div>
        }
      />

      {err ? (
        <div className="border-2 border-news bg-news-50 text-news-700 p-3 text-sm font-semibold">
          {err}
        </div>
      ) : null}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="md:col-span-1 space-y-4">
          <Card className="space-y-4">
            <div>
              <div className="ribbon text-xs mb-2">Quick actions</div>
              <h3 className="font-headline text-lg uppercase leading-none">Inbox</h3>
            </div>
            <div className="flex flex-col gap-2">
              <Button variant="news" size="md" onClick={markAll} disabled={unread === 0}>
                <CheckCheck className="h-4 w-4" />
                Mark all read
              </Button>
              <Button variant="outline" size="md" onClick={clearAll} disabled={items.length === 0}>
                <Trash2 className="h-4 w-4" />
                Clear all
              </Button>
            </div>
            <div className="pt-3 border-t-2 border-ink-950/10 space-y-2">
              <Metric label="Unread" value={unread} tone="news" />
              <Metric label="In inbox" value={items.length} />
            </div>
          </Card>

          <Card className="bg-ink-950 text-white space-y-3">
            <div className="flex items-center gap-2">
              <Mail className="h-4 w-4 text-news" />
              <h3 className="font-headline uppercase tracking-wide text-sm">Tips & submissions</h3>
            </div>
            <p className="text-sm text-white/75">
              Messages submitted via the public Contact form are routed to the editors. Check here
              if you&apos;re on the desk.
            </p>
          </Card>
        </div>

        <Card className="md:col-span-3 !p-0 overflow-hidden min-h-[520px]">
          {loading ? (
            <ul className="divide-y divide-ink-950/10">
              {Array.from({ length: 8 }).map((_, i) => (
                <li key={i} className="p-5 animate-pulse space-y-2">
                  <div className="h-4 w-1/3 bg-ink-900/10 rounded" />
                  <div className="h-4 w-5/6 bg-ink-900/10 rounded" />
                </li>
              ))}
            </ul>
          ) : items.length === 0 ? (
            <div className="p-16 text-center">
              <Inbox className="h-10 w-10 mx-auto text-ink-400 mb-3" />
              <h3 className="font-headline text-2xl uppercase mb-2">You&apos;re all caught up.</h3>
              <p className="text-sm text-ink-700">
                New alerts from the MapMyTimes newsroom will appear here as they arrive.
              </p>
            </div>
          ) : (
            <ul className="divide-y divide-ink-950/10">
              {items.map((n) => (
                <li
                  key={n.id}
                  className={cn(
                    "p-5 sm:p-6 grid grid-cols-1 sm:grid-cols-[auto_1fr_auto] gap-4 items-start",
                    !n.read && "bg-news-50/50",
                  )}
                >
                  <div
                    className={cn(
                      "h-10 w-10 border-2 border-ink-950 flex items-center justify-center flex-shrink-0",
                      !n.read ? "bg-news text-white" : "bg-white text-ink-800",
                    )}
                  >
                    <Bell className="h-4 w-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2 mb-1.5">
                      <span className="font-bold text-sm">{n.title}</span>
                      {!n.read ? (
                        <Badge variant="news" className="!py-0 !text-[9px]">New</Badge>
                      ) : null}
                      <span className="text-[10px] uppercase tracking-widest font-bold text-ink-600">
                        {n.type} · {n.channel}
                      </span>
                    </div>
                    <p className="text-sm text-ink-800 leading-relaxed whitespace-pre-wrap">
                      {n.message}
                    </p>
                    <p className="mt-1 text-[11px] uppercase tracking-widest text-ink-600 font-semibold">
                      {formatDateTime(n.createdAt)}
                      {n.sentAt && n.status === "SENT"
                        ? ` · Delivered ${formatDateTime(n.sentAt)}`
                        : n.status === "FAILED" && n.errorMessage
                          ? ` · Failed: ${n.errorMessage}`
                          : null}
                    </p>
                  </div>
                  <div className="flex items-center gap-1 justify-start sm:justify-end">
                    {!n.read ? (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => markRead(n.id)}
                        title="Mark as read"
                      >
                        <Check className="h-4 w-4" />
                        Read
                      </Button>
                    ) : null}
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => remove(n.id)}
                      title="Delete"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}

function Metric({
  label,
  value,
  tone = "ink",
}: {
  label: string;
  value: React.ReactNode;
  tone?: "ink" | "news";
}) {
  return (
    <div
      className={cn(
        "flex items-center justify-between px-3 py-2 border-2",
        tone === "news" ? "border-news text-news" : "border-ink-950/20 text-ink-800",
      )}
    >
      <span className="text-[10px] uppercase tracking-[0.25em] font-bold">{label}</span>
      <span className="font-headline text-xl leading-none tabular-nums">{value}</span>
    </div>
  );
}
