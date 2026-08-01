"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { PageHeader, Card } from "@/components/dashboard/Panels";
import { blogApi } from "@/lib/api/blogApi";
import type { PostCommentResponse } from "@/types/blog";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/posts/PostCard";
import {
  ShieldCheck,
  Ban,
  MessageSquareText,
  Eye,
  User as UserIcon,
  Filter,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { cn, formatRelative, initials } from "@/lib/utils";
import { getApiError } from "@/lib/api/client";

type Tab = "PENDING" | "ALL";

export default function CommentModPage() {
  const [tab, setTab] = useState<Tab>("PENDING");
  const [items, setItems] = useState<PostCommentResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    const req =
      tab === "PENDING"
        ? blogApi.comments.pending({ page, size: 20 })
        : blogApi.comments.list({ page, size: 20, sort: "createdAt,desc" });
    req
      .then((r) => {
        if (!active) return;
        setItems(r.content ?? []);
        setTotalPages(Math.max(1, r.totalPages ?? 1));
        setTotal(Number(r.totalElements ?? 0));
      })
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [tab, page]);

  async function approve(id: string) {
    try {
      const c = await blogApi.comments.approve(id);
      setItems((list) => list.map((x) => (x.id === id ? ({ ...x, status: "APPROVED" } as PostCommentResponse) : x)));
      setToast("Approved.");
      setTimeout(() => setToast(""), 2500);
    } catch (e) {
      alert(getApiError(e));
    }
  }

  async function reject(id: string) {
    if (!confirm("Reject and hide this comment?")) return;
    try {
      await blogApi.comments.reject(id);
      setItems((list) => list.map((x) => (x.id === id ? ({ ...x, status: "REJECTED" } as PostCommentResponse) : x)));
      setToast("Rejected.");
      setTimeout(() => setToast(""), 2500);
    } catch (e) {
      alert(getApiError(e));
    }
  }

  async function remove(id: string) {
    if (!confirm("Delete this comment permanently?")) return;
    try {
      await blogApi.comments.delete(id);
      setItems((list) => list.filter((x) => x.id !== id));
      setToast("Deleted.");
      setTimeout(() => setToast(""), 2500);
    } catch (e) {
      alert(getApiError(e));
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Moderation"
        title="Comment moderation"
        description="Approve or reject reader comments before they appear under MapMyTimes stories."
        action={
          <div className="inline-flex border-2 border-ink-950 shadow-hard-sm">
            <span className="h-10 px-3 inline-flex items-center gap-2 text-xs font-bold uppercase tracking-widest border-r-2 border-ink-950 bg-white">
              <Filter className="h-3.5 w-3.5" />
              Queue
            </span>
            {(
              [
                ["PENDING", "Pending"],
                ["ALL", "All comments"],
              ] as const
            ).map(([k, label]) => (
              <button
                key={k}
                onClick={() => {
                  setTab(k);
                  setPage(0);
                }}
                className={cn(
                  "h-10 px-4 text-xs font-bold uppercase tracking-widest",
                  tab === k
                    ? "bg-news text-white"
                    : "bg-white hover:bg-ink-950 hover:text-white",
                )}
              >
                {label}
              </button>
            ))}
          </div>
        }
      />

      {toast ? (
        <div className="border-2 border-ink-950 bg-ink-950 text-white p-3 text-sm font-semibold">
          {toast}
        </div>
      ) : null}

      <Card className="!p-0 overflow-hidden">
        <div className="grid grid-cols-2 sm:grid-cols-4 divide-x-2 divide-ink-950/10 border-b-2 border-ink-950">
          <Stat label="In queue" value={tab === "PENDING" ? total : "—"} tone="news" />
          <Stat label="Page" value={`${page + 1} / ${totalPages}`} />
          <Stat label="Total in view" value={items.length} />
          <Stat label="Total results" value={total.toLocaleString("en-IN")} />
        </div>

        {loading ? (
          <ul className="divide-y divide-ink-950/10">
            {Array.from({ length: 6 }).map((_, i) => (
              <li key={i} className="p-5 animate-pulse space-y-3">
                <div className="h-4 w-1/3 bg-ink-900/10 rounded" />
                <div className="h-4 w-5/6 bg-ink-900/10 rounded" />
                <div className="h-4 w-3/6 bg-ink-900/10 rounded" />
              </li>
            ))}
          </ul>
        ) : items.length === 0 ? (
          <div className="p-10 text-center">
            <ShieldCheck className="h-10 w-10 mx-auto text-ink-400 mb-2" />
            <h3 className="font-headline text-2xl uppercase mb-2">
              {tab === "PENDING" ? "All clear 🎉" : "No comments yet"}
            </h3>
            <p className="text-sm text-ink-700 max-w-xl mx-auto">
              {tab === "PENDING"
                ? "Nothing needs moderation right now. When readers comment on your stories, they'll appear here."
                : "No comments exist in this view yet."}
            </p>
          </div>
        ) : (
          <ul className="divide-y divide-ink-950/10">
            {items.map((c) => (
              <li key={c.id} className="p-5 grid grid-cols-1 lg:grid-cols-[auto_1fr_auto] gap-4">
                <div className="flex items-start gap-3">
                  <div className="h-10 w-10 flex-shrink-0 rounded-full bg-news text-white font-bold flex items-center justify-center text-sm border-2 border-ink-950">
                    {c.authorFirstName || c.authorLastName ? (
                      initials(c.authorFirstName, c.authorLastName)
                    ) : (
                      <UserIcon className="h-4 w-4" />
                    )}
                  </div>
                </div>
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2 mb-1.5">
                    <span className="font-bold text-sm">
                      {c.authorFirstName || c.authorLastName
                        ? `${c.authorFirstName} ${c.authorLastName}`
                        : "Anonymous"}
                    </span>
                    <Badge
                      variant={
                        c.status === "APPROVED"
                          ? "ink"
                          : c.status === "PENDING"
                            ? "news"
                            : "outline"
                      }
                      className="!text-[9px]"
                    >
                      {c.status}
                    </Badge>
                    <span className="text-[11px] uppercase tracking-widest text-ink-600 font-semibold">
                      {formatRelative(c.createdAt)}
                    </span>
                    {c.postId ? (
                      <Link
                        href={`/news/_?post=${encodeURIComponent(c.postId)}`}
                        className="text-[11px] font-bold uppercase tracking-widest hover:text-news inline-flex items-center gap-1"
                        onClick={(e) => {
                          // Fallback — the URL has no slug. Redirect to homepage instead and tell them via note?
                          e.preventDefault();
                        }}
                        title="Open post (public link requires slug)"
                      >
                        <Eye className="h-3.5 w-3.5" />
                        Post
                      </Link>
                    ) : null}
                  </div>
                  <p className="text-sm text-ink-800 whitespace-pre-wrap leading-relaxed">
                    {c.content}
                  </p>
                </div>
                <div className="flex items-center justify-start lg:justify-end gap-1.5 flex-wrap">
                  {c.status !== "APPROVED" ? (
                    <Button
                      size="sm"
                      variant="news"
                      onClick={() => approve(c.id)}
                      title="Approve & publish"
                    >
                      <ShieldCheck className="h-4 w-4" />
                      Approve
                    </Button>
                  ) : null}
                  {c.status !== "REJECTED" ? (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => reject(c.id)}
                      title="Reject"
                    >
                      <Ban className="h-4 w-4" />
                      Reject
                    </Button>
                  ) : null}
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => remove(c.id)}
                    title="Delete"
                    className="text-news-700 hover:bg-news hover:text-white hover:border-news"
                  >
                    Delete
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}

        <div className="border-t-2 border-ink-950 p-4 flex items-center justify-between text-xs font-bold uppercase tracking-widest text-ink-600">
          <Link href="/dashboard/posts" className="hover:text-news inline-flex items-center gap-1">
            <MessageSquareText className="h-3.5 w-3.5" />
            Manage posts
          </Link>
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              <ChevronLeft className="h-4 w-4" /> Prev
            </Button>
            <Button
              size="sm"
              variant="outline"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            >
              Next <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </Card>
    </>
  );
}

function Stat({
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
        "p-4 sm:p-5",
        tone === "news" ? "bg-news text-white" : "bg-white text-ink-950",
      )}
    >
      <div className="text-[10px] uppercase tracking-[0.25em] font-bold opacity-85">
        {label}
      </div>
      <div className="mt-1 font-headline text-2xl sm:text-3xl uppercase leading-none tabular-nums">
        {value}
      </div>
    </div>
  );
}
