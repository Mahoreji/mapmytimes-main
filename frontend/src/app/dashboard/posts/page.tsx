"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { PageHeader, Card } from "@/components/dashboard/Panels";
import { blogApi } from "@/lib/api/blogApi";
import type { BlogPostSummaryResponse } from "@/types/blog";
import type { PostStatus } from "@/types/common";
import { Badge } from "@/components/posts/PostCard";
import { Button } from "@/components/ui/Button";
import {
  Plus,
  Eye,
  Pencil,
  Trash2,
  Upload,
  Calendar,
  Filter,
  Search,
} from "lucide-react";
import { cn, formatDate, truncate, formatDateTime } from "@/lib/utils";
import { getApiError } from "@/lib/api/client";

const STATUS_FILTERS: (PostStatus | "ALL")[] = [
  "ALL",
  "DRAFT",
  "PUBLISHED",
  "SCHEDULED",
  "ARCHIVED",
];

export default function MyPostsPage() {
  const sp = useSearchParams();
  const router = useRouter();
  const initialStatus = (sp?.get("status") || "ALL") as PostStatus | "ALL";
  const [status, setStatus] = useState<PostStatus | "ALL">(initialStatus);
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<BlogPostSummaryResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<string>("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    const req =
      status === "ALL"
        ? blogApi.posts.mine({ page, size: 12, sort: "updatedAt,desc" })
        : blogApi.posts.mine({ page, size: 12, sort: "updatedAt,desc", status });
    req
      .then((r) => {
        if (!active) return;
        setItems(r.content ?? []);
        setTotal(Number(r.totalElements ?? 0));
        setTotalPages(Math.max(1, r.totalPages ?? 1));
      })
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [status, page]);

  const filtered = useMemo(() => {
    const t = q.trim().toLowerCase();
    if (!t) return items;
    return items.filter(
      (p) =>
        p.title.toLowerCase().includes(t) ||
        p.slug.toLowerCase().includes(t) ||
        (p.excerpt || "").toLowerCase().includes(t),
    );
  }, [items, q]);

  async function remove(id: string, title: string) {
    if (!confirm(`Delete "${title}"? This cannot be undone.`)) return;
    try {
      await blogApi.posts.delete(id);
      setItems((list) => list.filter((p) => p.id !== id));
      setToast(`“${truncate(title, 40)}” deleted.`);
      setTimeout(() => setToast(""), 4000);
    } catch (e) {
      alert(getApiError(e));
    }
  }

  async function unpublish(id: string) {
    try {
      const r = await blogApi.posts.unpublish(id);
      setItems((list) => list.map((p) => (p.id === id ? { ...p, status: (r as any).status ?? "DRAFT" } : p)));
      setToast("Unpublished.");
      setTimeout(() => setToast(""), 3500);
    } catch (e) {
      alert(getApiError(e));
    }
  }

  async function publish(id: string) {
    try {
      const r = await blogApi.posts.publish(id);
      setItems((list) =>
        list.map((p) =>
          p.id === id
            ? { ...p, status: (r as any).status ?? "PUBLISHED", publishedAt: (r as any).publishedAt ?? new Date().toISOString() }
            : p,
        ),
      );
      setToast("Published 🎉");
      setTimeout(() => setToast(""), 3500);
    } catch (e) {
      alert(getApiError(e));
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Stories"
        title="My posts"
        description="All your drafts, scheduled, and published stories in one place."
        action={
          <Link href="/dashboard/posts/new">
            <Button variant="news" size="lg">
              <Plus className="h-4 w-4" />
              New story
            </Button>
          </Link>
        }
      />

      <Card className="!p-0 overflow-hidden">
        <div className="p-4 sm:p-5 border-b-2 border-ink-950 space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <div className="inline-flex items-center gap-1 border-2 border-ink-950 shadow-hard-sm">
              <span className="h-10 px-3 inline-flex items-center gap-2 text-xs font-bold uppercase tracking-widest border-r-2 border-ink-950 bg-white">
                <Filter className="h-3.5 w-3.5" />
                Status
              </span>
              {STATUS_FILTERS.map((s) => (
                <button
                  key={s}
                  onClick={() => {
                    setStatus(s);
                    setPage(0);
                    const url = new URL(window.location.href);
                    if (s === "ALL") url.searchParams.delete("status");
                    else url.searchParams.set("status", s);
                    window.history.replaceState({}, "", url.toString());
                  }}
                  className={cn(
                    "h-10 px-3 text-xs font-bold uppercase tracking-widest transition-colors",
                    status === s
                      ? "bg-news text-white"
                      : "bg-white hover:bg-ink-950 hover:text-white",
                  )}
                >
                  {s === "ALL" ? "All" : s.replace("_", " ")}
                </button>
              ))}
            </div>
            <div className="ml-auto border-2 border-ink-950 flex items-center px-2 gap-2 h-10 shadow-hard-sm">
              <Search className="h-4 w-4 text-ink-600" />
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Search your stories…"
                className="h-8 w-48 sm:w-64 bg-transparent outline-none text-sm"
              />
            </div>
          </div>
          <div className="flex items-center gap-3 text-xs font-bold uppercase tracking-widest text-ink-600">
            <span>
              {loading
                ? "Loading…"
                : `${filtered.length.toLocaleString("en-IN")} / ${total.toLocaleString(
                    "en-IN",
                  )} total`}
            </span>
            <span className="text-ink-300">·</span>
            {toast ? <span className="text-news">{toast}</span> : null}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-ink-950 text-white text-[11px] uppercase tracking-widest">
              <tr>
                <th className="text-left font-bold px-4 py-3 w-[42%]">Story</th>
                <th className="text-left font-bold px-4 py-3">Status</th>
                <th className="text-left font-bold px-4 py-3">
                  <span className="inline-flex items-center gap-1.5">
                    <Eye className="h-3.5 w-3.5" />
                    Views
                  </span>
                </th>
                <th className="text-left font-bold px-4 py-3">
                  <span className="inline-flex items-center gap-1.5">
                    <Calendar className="h-3.5 w-3.5" />
                    Last updated
                  </span>
                </th>
                <th className="text-right font-bold px-4 py-3 pr-5">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ink-950/10">
              {loading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td className="p-4">
                      <div className="h-4 w-2/3 bg-ink-900/10 rounded" />
                    </td>
                    <td className="p-4">
                      <div className="h-4 w-16 bg-ink-900/10 rounded" />
                    </td>
                    <td className="p-4">
                      <div className="h-4 w-10 bg-ink-900/10 rounded" />
                    </td>
                    <td className="p-4">
                      <div className="h-4 w-28 bg-ink-900/10 rounded" />
                    </td>
                    <td className="p-4 pr-5">
                      <div className="h-4 w-28 ml-auto bg-ink-900/10 rounded" />
                    </td>
                  </tr>
                ))
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={5} className="p-10 text-center">
                    <Upload className="h-8 w-8 mx-auto text-ink-400 mb-2" />
                    <h3 className="font-headline text-xl uppercase mb-1">
                      {status === "ALL" ? "No posts yet" : `No ${status.toLowerCase()} stories`}
                    </h3>
                    <p className="text-sm text-ink-600 mb-4">
                      {status === "ALL"
                        ? "Write your first story and start publishing."
                        : "Create a new post with the matching status, or change the filter."}
                    </p>
                    <Link href="/dashboard/posts/new">
                      <Button variant="news">
                        <Plus className="h-4 w-4" />
                        New story
                      </Button>
                    </Link>
                  </td>
                </tr>
              ) : (
                filtered.map((p) => (
                  <tr key={p.id} className="hover:bg-ink-900/5 align-top">
                    <td className="px-4 py-4">
                      <div className="flex items-start gap-3">
                        {p.featuredImageUrl ? (
                          <Link href={`/news/${encodeURIComponent(p.slug)}`} className="flex-shrink-0">
                            {/* eslint-disable-next-line @next/next/no-img-element */}
                            <img
                              src={p.featuredImageUrl}
                              alt=""
                              className="h-16 w-24 object-cover border-2 border-ink-950"
                            />
                          </Link>
                        ) : (
                          <div className="h-16 w-24 flex-shrink-0 bg-ink-900/10 border-2 border-ink-950 flex items-center justify-center text-ink-600">
                            <Upload className="h-5 w-5" />
                          </div>
                        )}
                        <div className="min-w-0">
                          <Link
                            href={`/dashboard/posts/${encodeURIComponent(p.id)}`}
                            className="font-bold leading-tight line-clamp-2 hover:text-news block"
                          >
                            {p.title}
                          </Link>
                          <div className="mt-1 text-[11px] uppercase tracking-widest text-ink-600 font-semibold flex flex-wrap items-center gap-2">
                            <Link href={`/news/${encodeURIComponent(p.slug)}`} className="hover:text-news">
                              /{p.slug}
                            </Link>
                            {p.categories?.slice(0, 2).map((c) => (
                              <Badge key={c.id} variant="outline" className="!py-0 !text-[9px]">
                                {c.name}
                              </Badge>
                            ))}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <span
                        className={cn(
                          "inline-flex items-center px-2 py-1 border-2 text-[10px] font-bold uppercase tracking-widest",
                          p.status === "PUBLISHED"
                            ? "border-news text-news bg-news-50"
                            : p.status === "DRAFT"
                              ? "border-ink-950 text-ink-950 bg-white"
                              : p.status === "SCHEDULED"
                                ? "border-ink-700 text-ink-800 bg-ink-900/5"
                                : "border-ink-600 text-ink-700 bg-white",
                        )}
                      >
                        {p.status}
                      </span>
                    </td>
                    <td className="px-4 py-4 tabular-nums text-ink-700 font-semibold">
                      {(p.viewCount ?? 0).toLocaleString("en-IN")}
                    </td>
                    <td className="px-4 py-4 text-ink-700 text-xs font-semibold uppercase tracking-wide">
                      <div>{formatDate(p.updatedAt ?? p.publishedAt ?? p.createdAt)}</div>
                      {p.publishedAt && p.status === "PUBLISHED" ? (
                        <div className="text-[10px] text-ink-500 mt-0.5 tracking-widest">
                          Published {formatDateTime(p.publishedAt)}
                        </div>
                      ) : null}
                      {p.scheduledAt ? (
                        <div className="text-[10px] text-news-700 mt-0.5 tracking-widest font-bold">
                          Scheduled {formatDateTime(p.scheduledAt)}
                        </div>
                      ) : null}
                    </td>
                    <td className="px-4 py-4 pr-5 text-right">
                      <div className="inline-flex items-center gap-1">
                        {p.status === "PUBLISHED" ? (
                          <button
                            onClick={() => unpublish(p.id)}
                            className="h-8 px-2 text-[10px] font-bold uppercase tracking-widest border-2 border-ink-950 hover:bg-ink-950 hover:text-white"
                          >
                            Unpublish
                          </button>
                        ) : (
                          <button
                            onClick={() => publish(p.id)}
                            className="h-8 px-2 text-[10px] font-bold uppercase tracking-widest border-2 border-news bg-news text-white hover:bg-news-700"
                          >
                            Publish
                          </button>
                        )}
                        <a
                          href={`/news/${encodeURIComponent(p.slug)}`}
                          target="_blank"
                          rel="noreferrer"
                          title="Preview"
                          className="h-8 w-8 inline-flex items-center justify-center border-2 border-ink-950 hover:bg-ink-950 hover:text-white"
                        >
                          <Eye className="h-3.5 w-3.5" />
                        </a>
                        <Link
                          href={`/dashboard/posts/${encodeURIComponent(p.id)}`}
                          title="Edit"
                          className="h-8 w-8 inline-flex items-center justify-center border-2 border-ink-950 hover:bg-ink-950 hover:text-white"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </Link>
                        <button
                          onClick={() => remove(p.id, p.title)}
                          title="Delete"
                          className="h-8 w-8 inline-flex items-center justify-center border-2 border-news text-news hover:bg-news hover:text-white"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="border-t-2 border-ink-950 p-4 flex items-center justify-between gap-2 text-xs font-bold uppercase tracking-widest text-ink-600">
          <div>
            Page {page + 1} / {totalPages}
          </div>
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Prev
            </Button>
            <Button
              size="sm"
              variant="outline"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            >
              Next
            </Button>
          </div>
        </div>
      </Card>
    </>
  );
}
