"use client";

import { useEffect, useRef, useState } from "react";
import { Bell, Check, CheckCheck, Trash2, X } from "lucide-react";
import Link from "next/link";
import { Button, IconButton } from "@/components/ui/Button";
import { useAuth } from "@/lib/auth/AuthProvider";
import { notificationApi } from "@/lib/api/notificationApi";
import { cn, formatRelative } from "@/lib/utils";
import type { Notification } from "@/types/notification";

export function NotificationBell() {
  const { isAuthenticated } = useAuth();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  async function refreshCount() {
    if (!isAuthenticated) return;
    try {
      const r = await notificationApi.unreadCount();
      setUnread(typeof r === "number" ? r : r.unread ?? 0);
    } catch {}
  }

  async function openPanel() {
    if (!isAuthenticated) {
      return;
    }
    setOpen(true);
    setLoading(true);
    try {
      const [list, _] = await Promise.all([
        notificationApi.list({ page: 0, size: 20 }).catch(() => [] as any),
        refreshCount(),
      ]);
      const arr = Array.isArray(list)
        ? list
        : ((list as any)?.content as Notification[]) ?? [];
      setItems(arr.slice(0, 20));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!isAuthenticated) {
      setUnread(0);
      setItems([]);
      return;
    }
    void refreshCount();
    const t = setInterval(refreshCount, 60_000);
    return () => clearInterval(t);
  }, [isAuthenticated]);

  useEffect(() => {
    if (!open) return;
    function onDoc(e: MouseEvent) {
      if (!ref.current?.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [open]);

  async function markRead(id: string) {
    try {
      await notificationApi.markRead(id);
      setItems((list) =>
        list.map((n) => (n.id === id ? { ...n, read: true } : n)),
      );
      setUnread((u) => Math.max(0, u - 1));
    } catch {}
  }

  async function markAll() {
    try {
      await notificationApi.markAllRead();
      setItems((list) => list.map((n) => ({ ...n, read: true })));
      setUnread(0);
    } catch {}
  }

  async function remove(id: string) {
    try {
      await notificationApi.delete(id);
      setItems((list) => list.filter((n) => n.id !== id));
    } catch {}
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => (open ? setOpen(false) : void openPanel())}
        aria-label="Notifications"
        className={cn(
          "relative h-10 w-10 flex items-center justify-center border-2 border-ink-950 hover:shadow-hard-sm transition-shadow",
          open && "bg-ink-950 text-white",
        )}
      >
        <Bell className="h-4 w-4" />
        {unread > 0 ? (
          <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-news text-white text-[10px] font-bold flex items-center justify-center border-2 border-white">
            {unread > 99 ? "99+" : unread}
          </span>
        ) : null}
      </button>

      {open ? (
        <div className="absolute right-0 mt-2 w-[360px] sm:w-[420px] max-h-[80vh] flex flex-col border-2 border-ink-950 bg-white shadow-hard z-50">
          <div className="border-b-2 border-ink-950 px-4 py-3 flex items-center justify-between bg-ink-950 text-white">
            <div className="flex items-center gap-2">
              <Bell className="h-4 w-4 text-news" />
              <h3 className="font-headline uppercase tracking-wider text-sm">
                Notifications
              </h3>
              <BadgeNews n={unread} />
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={() => void markAll()}
                title="Mark all read"
                className="h-8 w-8 inline-flex items-center justify-center hover:bg-white/10"
                aria-label="Mark all read"
              >
                <CheckCheck className="h-4 w-4" />
              </button>
              <button
                onClick={() => setOpen(false)}
                className="h-8 w-8 inline-flex items-center justify-center hover:bg-white/10"
                aria-label="Close"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto">
            {loading ? (
              <div className="p-6 text-sm text-ink-600 text-center">Loading…</div>
            ) : items.length === 0 ? (
              <div className="p-8 text-sm text-ink-600 text-center flex flex-col gap-2">
                <Bell className="h-8 w-8 mx-auto text-ink-400" />
                You&apos;re all caught up.
              </div>
            ) : (
              <ul className="divide-y divide-ink-950/10">
                {items.map((n) => (
                  <li
                    key={n.id}
                    className={cn(
                      "px-4 py-3 hover:bg-ink-900/5 relative group",
                      !n.read && "bg-news-50/70",
                    )}
                  >
                    <div className="flex items-start gap-3">
                      <div
                        className={cn(
                          "mt-1 h-2 w-2 rounded-full flex-shrink-0",
                          n.read ? "bg-transparent" : "bg-news animate-pulseDot",
                        )}
                      />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-start justify-between gap-2">
                          <div className="text-xs font-bold uppercase tracking-wider text-ink-800">
                            {n.title}
                          </div>
                          <span className="text-[10px] uppercase tracking-widest text-ink-600 whitespace-nowrap">
                            {formatRelative(n.createdAt)}
                          </span>
                        </div>
                        <p className="text-sm text-ink-800 mt-0.5">{n.message}</p>
                      </div>
                    </div>
                    <div className="mt-2 flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      {!n.read ? (
                        <button
                          onClick={() => void markRead(n.id)}
                          title="Mark as read"
                          className="h-7 w-7 inline-flex items-center justify-center border border-ink-950/20 hover:bg-ink-950 hover:text-white"
                          aria-label="Mark as read"
                        >
                          <Check className="h-3.5 w-3.5" />
                        </button>
                      ) : null}
                      <button
                        onClick={() => void remove(n.id)}
                        title="Delete"
                        className="h-7 w-7 inline-flex items-center justify-center border border-ink-950/20 hover:bg-news hover:text-white"
                        aria-label="Delete"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="border-t-2 border-ink-950 p-3 flex items-center justify-between">
            <Link
              href="/dashboard/notifications"
              className="text-xs font-bold uppercase tracking-widest hover:text-news"
            >
              View all
            </Link>
            <Button
              size="sm"
              variant="outline"
              onClick={() => void markAll()}
              className="!text-[11px]"
            >
              <CheckCheck className="h-3.5 w-3.5" />
              Mark all read
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function BadgeNews({ n }: { n: number }) {
  if (n <= 0) return null;
  return (
    <span className="ml-1 px-1.5 h-5 min-w-[20px] rounded-sm bg-news text-white text-[11px] font-bold inline-flex items-center justify-center">
      {n > 99 ? "99+" : n}
    </span>
  );
}
