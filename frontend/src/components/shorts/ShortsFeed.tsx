"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import Link from "next/link";
import Image from "next/image";
import { blogApi } from "@/lib/api/blogApi";
import { BrandLogo } from "@/components/site/SiteHeader";
import {
  SITE,
  cn,
  formatCount,
  formatDate,
  initials,
} from "@/lib/utils";
import type {
  BlogPostSummaryResponse,
  CreateCommentRequest,
  PostCommentResponse,
} from "@/types/blog";
import type { ID } from "@/types/common";
import ShortCard from "./ShortCard";
import { extractShortMeta } from "@/lib/youtube";

export interface ShortCardActions {
  raiseLikeError: (err: unknown) => void;
  raiseShareSuccess?: (postId: ID) => void;
}

interface ShortsFeedProps {
  initialPosts?: BlogPostSummaryResponse[];
  initialPage?: number;
  initialTotalPages?: number;
  startAtIndex?: number;
  pageSize?: number;
  header?: React.ReactNode;
}

type LoadState = "idle" | "loading" | "done" | "error";

function useShortIds(
  posts: BlogPostSummaryResponse[],
  activeIndex: number,
  scope = 1,
): Map<string, "mounted" | "adjacent" | "idle"> {
  return useMemo(() => {
    const m = new Map<string, "mounted" | "adjacent" | "idle">();
    posts.forEach((p, i) => {
      const dist = Math.abs(i - activeIndex);
      let v: "mounted" | "adjacent" | "idle" = "idle";
      if (dist <= scope) v = "mounted";
      else if (dist <= scope + 1) v = "adjacent";
      m.set(String(p.id), v);
    });
    return m;
  }, [posts, activeIndex, scope]);
}

export default function ShortsFeed({
  initialPosts,
  initialPage = 0,
  initialTotalPages,
  startAtIndex = 0,
  pageSize = 12,
  header,
}: ShortsFeedProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  const cardsRef = useRef<Map<string, HTMLElement>>(new Map());
  const [posts, setPosts] = useState<BlogPostSummaryResponse[]>(initialPosts ?? []);
  const [page, setPage] = useState<number>(initialPage);
  const [totalPages, setTotalPages] = useState<number | null>(
    initialTotalPages ?? null,
  );
  const [loadState, setLoadState] = useState<LoadState>(
    (initialPosts || []).length > 0 ? "idle" : "loading",
  );
  const [loadError, setLoadError] = useState<string>("");
  const [activeIndex, setActiveIndex] = useState<number>(startAtIndex);
  const [commentsOpen, setCommentsOpen] = useState(false);
  const [commentsFor, setCommentsFor] = useState<BlogPostSummaryResponse | null>(null);
  const [toast, setToast] = useState<{ text: string; tone?: "ok" | "err" } | null>(null);

  const focusMap = useShortIds(posts, activeIndex, 1);

  const openComments = useCallback((post: BlogPostSummaryResponse) => {
    setCommentsFor(post);
    setCommentsOpen(true);
    setTimeout(() => {
      document.documentElement.style.overflow = "hidden";
      document.body.style.overflow = "hidden";
    }, 0);
  }, []);

  const closeComments = useCallback(() => {
    setCommentsOpen(false);
    document.documentElement.style.overflow = "";
    document.body.style.overflow = "";
  }, []);

  useEffect(() => {
    return () => {
      document.documentElement.style.overflow = "";
      document.body.style.overflow = "";
    };
  }, []);

  const showToast = useCallback(
    (text: string, tone?: "ok" | "err") => {
      setToast({ text, tone });
      setTimeout(() => setToast(null), 2400);
    },
    [],
  );

  const loadPage = useCallback(
    async (nextPage: number) => {
      setLoadState("loading");
      setLoadError("");
      try {
        const res = await blogApi.posts.list({
          postType: "STORY",
          page: nextPage,
          size: pageSize,
          sort: "publishedAt,DESC",
          status: "PUBLISHED",
        } as any);
        const content = res?.content || [];
        setPosts((prev) => {
          const seen = new Set<string>();
          const merged: BlogPostSummaryResponse[] = [];
          [...prev, ...content].forEach((p) => {
            const k = String(p.id);
            if (seen.has(k)) return;
            seen.add(k);
            merged.push(p);
          });
          return merged;
        });
        setTotalPages(typeof res.totalPages === "number" ? res.totalPages : null);
        setPage(nextPage);
        setLoadState((content.length === 0 || (typeof res.totalPages === "number" && nextPage + 1 >= res.totalPages)) ? "done" : "idle");
      } catch (e: any) {
        setLoadState("error");
        setLoadError(e?.message || "Failed to load Shorts");
      }
    },
    [pageSize],
  );

  // Initial load if no initial posts
  useEffect(() => {
    if ((initialPosts ?? []).length > 0) return;
    void loadPage(0);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // IntersectionObserver -> activeIndex + load next
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const entries = new Map<Element, number>();
    const cardObserver = new IntersectionObserver(
      (list) => {
        list.forEach((e) => {
          entries.set(e.target, e.intersectionRatio);
        });
        let bestEl: Element | null = null;
        let bestRatio = 0.65;
        for (const [el, r] of entries.entries()) {
          if (r > bestRatio) {
            bestRatio = r;
            bestEl = el;
          }
        }
        if (bestEl) {
          const id = (bestEl as HTMLElement).getAttribute("data-short-index");
          if (id != null) {
            const n = Number(id);
            if (!Number.isNaN(n)) setActiveIndex(n);
          }
        }
      },
      {
        root: container,
        threshold: [0, 0.25, 0.5, 0.65, 0.8, 1],
      },
    );

    posts.forEach((p, i) => {
      const el = cardsRef.current.get(String(p.id));
      if (!el) return;
      if (!el.hasAttribute("data-short-index")) {
        el.setAttribute("data-short-index", String(i));
      }
      cardObserver.observe(el);
    });

    // Sentinel for infinite scroll
    const sentinel = sentinelRef.current;
    let sentinelObserver: IntersectionObserver | null = null;
    if (sentinel) {
      sentinelObserver = new IntersectionObserver(
        (list) => {
          const isVisible = list.some((e) => e.isIntersecting);
          if (!isVisible) return;
          if (loadState !== "idle") return;
          if (totalPages != null && page + 1 >= totalPages) {
            setLoadState("done");
            return;
          }
          void loadPage(page + 1);
        },
        { root: container, threshold: 0.1 },
      );
      sentinelObserver.observe(sentinel);
    }

    return () => {
      cardObserver.disconnect();
      sentinelObserver?.disconnect?.();
    };
  }, [posts, loadState, page, totalPages, loadPage]);

  // Scroll to startAtIndex after mount once
  useEffect(() => {
    if (!startAtIndex || startAtIndex <= 0) return;
    const firstId = posts[startAtIndex]?.id ?? null;
    if (!firstId) return;
    const el = cardsRef.current.get(String(firstId));
    if (!el || !containerRef.current) return;
    containerRef.current.scrollTo({
      top: el.offsetTop,
      behavior: "instant" as ScrollBehavior,
    });
    setActiveIndex(startAtIndex);
  }, [startAtIndex, posts]);

  // register card ref
  const registerCard = useCallback((id: string, el: HTMLElement | null) => {
    if (!el) return;
    cardsRef.current.set(id, el);
  }, []);

  const actions: ShortCardActions = {
    raiseLikeError: (err) => {
      const msg =
        (err as any)?.response?.data?.message ||
        (err as any)?.message ||
        "Please sign in to like.";
      showToast(msg, "err");
    },
    raiseShareSuccess: () => showToast("Link copied to clipboard", "ok"),
  };

  return (
    <div className="fixed inset-0 z-[55] w-full h-full bg-black text-white overflow-hidden">
      {header}

      <div
        ref={containerRef}
        className="shorts-scroll w-full h-full overflow-y-scroll overflow-x-hidden snap-y snap-mandatory scroll-smooth no-scrollbar relative bg-black"
        style={{ scrollSnapType: "y mandatory" }}
      >
        {loadState === "loading" && posts.length === 0 && (
          <div className="snap-start min-h-[100dvh] flex flex-col items-center justify-center">
            <div className="flex flex-col items-center gap-5">
              <BrandLogo variant="inverted" className="h-14 w-auto opacity-75" />
              <div className="h-11 w-11 rounded-full border-[3px] border-white/15 border-t-news animate-spin" />
              <p className="text-white/80 text-sm font-semibold">
                Loading latest Shorts…
              </p>
            </div>
          </div>
        )}

        {loadState === "error" && posts.length === 0 && (
          <div className="snap-start min-h-[100dvh] flex flex-col items-center justify-center px-6">
            <div className="max-w-sm text-center rounded-2xl bg-white/5 ring-1 ring-white/10 p-8">
              <div className="mx-auto h-12 w-12 rounded-full bg-red-500/80 flex items-center justify-center text-white mb-4">
                <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" /></svg>
              </div>
              <h3 className="text-white font-black text-xl">Couldn't load Shorts</h3>
              <p className="mt-2 text-white/70 text-sm">
                {loadError || "Something went wrong on our end."}
              </p>
              <button
                type="button"
                className="mt-5 rounded-sm bg-news px-5 py-3 text-sm font-black uppercase tracking-wide"
                onClick={() => loadPage(0)}
              >
                Try again
              </button>
            </div>
          </div>
        )}

        {loadState !== "loading" && posts.length === 0 && (
          <div className="snap-start min-h-[100dvh] flex flex-col items-center justify-center px-6">
            <div className="max-w-sm text-center rounded-2xl bg-white/5 ring-1 ring-white/10 p-8">
              <BrandLogo variant="inverted" className="h-12 w-auto mx-auto opacity-80 mb-4" />
              <h3 className="text-white font-black text-xl">No Shorts yet</h3>
              <p className="mt-2 text-white/70 text-sm">
                We're working hard on bringing you the first batch of news Shorts.
                Check back soon or read the top stories in the meantime.
              </p>
              <div className="mt-6 grid grid-cols-2 gap-3">
                <Link href="/" className="inline-flex items-center justify-center rounded-sm border-2 border-white/15 px-4 py-3 text-sm font-black uppercase tracking-wide">
                  Latest news
                </Link>
                <Link href="/sections" className="inline-flex items-center justify-center rounded-sm bg-news px-4 py-3 text-sm font-black uppercase tracking-wide">
                  All sections
                </Link>
              </div>
            </div>
          </div>
        )}

        {posts.map((post, i) => (
          <div
            key={String(post.id)}
            ref={(el) => registerCard(String(post.id), el)}
          >
            <ShortCard
              post={post}
              index={i}
              focus={focusMap.get(String(post.id)) ?? "idle"}
              isActive={i === activeIndex}
              actions={actions}
              openComments={openComments}
            />
          </div>
        ))}

        <div ref={sentinelRef} className="snap-start min-h-[100dvh] flex items-center justify-center">
          {loadState === "done" && posts.length > 0 ? (
            <div className="text-center max-w-sm px-6">
              <div className="inline-flex items-center gap-2 rounded-full bg-white/10 ring-1 ring-white/10 text-white/85 text-xs font-bold uppercase tracking-widest px-4 py-2">
                🎬 That's all for now
              </div>
              <p className="mt-4 text-white/75 text-sm">
                You're all caught up. New news Shorts drop throughout the day — refresh
                to see what's new.
              </p>
              <button
                type="button"
                onClick={() => window.location.reload()}
                className="mt-5 inline-flex items-center gap-2 rounded-sm bg-news px-5 py-3 text-sm font-black uppercase tracking-wide"
              >
                ↻ Refresh Shorts
              </button>
            </div>
          ) : loadState === "loading" ? (
            <div className="flex flex-col items-center gap-3">
              <div className="h-9 w-9 rounded-full border-[3px] border-white/15 border-t-news animate-spin" />
              <p className="text-white/75 text-sm font-semibold">Loading more…</p>
            </div>
          ) : null}
        </div>
      </div>

      {/* Fixed top bar */}
      <div className="pointer-events-none absolute top-0 inset-x-0 z-40">
        <div className="pointer-events-auto px-4 pt-3 pb-10 flex items-center justify-between bg-gradient-to-b from-black/60 via-black/20 to-transparent">
          <Link href="/" className="flex items-center">
            <BrandLogo variant="inverted" className="h-9 w-auto" />
          </Link>
          <div className="flex items-center gap-2">
            <Link
              href="/search"
              className="h-10 w-10 inline-flex items-center justify-center rounded-full bg-white/10 ring-1 ring-white/15 text-white hover:bg-white/15"
              aria-label="Search news"
            >
              <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </Link>
            <Link
              href="/"
              className="hidden sm:inline-flex items-center gap-1.5 h-10 rounded-full bg-white text-ink-950 px-4 text-xs font-black uppercase tracking-widest hover:bg-white/95"
            >
              Read News
              <span aria-hidden>→</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Comment drawer */}
      <CommentDrawer
        open={commentsOpen}
        post={commentsFor}
        onClose={closeComments}
        onNotify={showToast}
      />

      {/* Toast */}
      {toast && (
        <div className="pointer-events-none fixed top-4 left-1/2 -translate-x-1/2 z-[60]">
          <div className={cn(
            "pointer-events-auto px-4 py-2.5 rounded-full text-sm font-bold shadow-2xl ring-1 backdrop-blur-md",
            toast.tone === "err"
              ? "bg-red-500/95 text-white ring-red-400/40"
              : "bg-white text-ink-950 ring-white/50",
          )}>
            {toast.text}
          </div>
        </div>
      )}

      <style jsx global>{`
        .no-scrollbar::-webkit-scrollbar { display: none; }
        .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
        @keyframes shortsprogress { from { width: 0%; } to { width: 100%; } }
      `}</style>
    </div>
  );
}

/* -------------------- Comment drawer -------------------- */

interface CommentDrawerProps {
  open: boolean;
  post: BlogPostSummaryResponse | null;
  onClose: () => void;
  onNotify: (text: string, tone?: "ok" | "err") => void;
}

function CommentDrawer({ open, post, onClose, onNotify }: CommentDrawerProps) {
  const [comments, setComments] = useState<PostCommentResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [input, setInput] = useState<string>("");
  const [submitting, setSubmitting] = useState<boolean>(false);
  const postId = post?.id ?? null;

  useEffect(() => {
    if (!open || !postId) return;
    let cancelled = false;
    setLoading(true);
    setComments([]);
    (async () => {
      try {
        const list = await blogApi.comments.byPostApproved(postId as ID);
        if (!cancelled) setComments(Array.isArray(list) ? list : []);
      } catch (e: any) {
        if (!cancelled) onNotify(e?.message || "Couldn't load comments", "err");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [open, postId, onNotify]);

  const submit = useCallback(async () => {
    if (!postId || !input.trim() || submitting) return;
    setSubmitting(true);
    try {
      const body: CreateCommentRequest = {
        postId: postId as ID,
        content: input.trim(),
      } as any;
      const c = await blogApi.comments.create(body);
      setComments((prev) => [c, ...prev]);
      setInput("");
      onNotify("Comment posted", "ok");
    } catch (e: any) {
      onNotify(
        (e as any)?.response?.data?.message ||
        (e as any)?.message ||
        "Please sign in to comment.",
        "err",
      );
    } finally {
      setSubmitting(false);
    }
  }, [input, postId, submitting, onNotify]);

  if (!open) return null;
  const caption = post ? extractShortMeta(post).caption || post.title : "";

  return (
    <div
      className="fixed inset-0 z-[70] pointer-events-none transition-opacity duration-300 opacity-100"
      aria-hidden={!open}
    >
      <div
        className="absolute inset-0 bg-black/65 pointer-events-auto transition-opacity opacity-100"
        onClick={onClose}
      />
      <div
        className={cn(
          "pointer-events-auto absolute inset-x-0 bottom-0 top-[60px] sm:top-16 sm:left-auto sm:right-4 sm:bottom-4 sm:top-[auto] sm:w-[440px] sm:max-w-[92vw] sm:h-[82vh] rounded-t-3xl sm:rounded-3xl bg-white text-ink-950 shadow-2xl ring-1 ring-ink-950/5 transition-transform duration-300 flex flex-col overflow-hidden",
          open ? "translate-y-0" : "translate-y-full sm:translate-y-[110%]",
        )}
      >
        <div className="shrink-0 px-5 pt-4 pb-3 border-b border-ink-950/8 flex items-center justify-between">
          <div>
            <div className="text-[10px] font-black uppercase tracking-widest text-ink-500">
              Comments
            </div>
            <div className="text-[15px] font-black text-ink-950 leading-snug line-clamp-2 max-w-[26rem]">
              {caption || "News short"}
            </div>
            <div className="mt-1 text-[11px] font-semibold text-ink-500">
              {formatCount(comments.length)} comments
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="h-9 w-9 rounded-full bg-ink-950/5 hover:bg-ink-950/10 flex items-center justify-center text-ink-950"
            aria-label="Close comments"
          >
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          </button>
        </div>

        <div className="flex-1 min-h-0 overflow-y-auto px-5 py-4 space-y-4">
          {loading ? (
            <div className="py-10 flex flex-col items-center gap-3 text-ink-500">
              <div className="h-9 w-9 rounded-full border-[3px] border-ink-950/10 border-t-news animate-spin" />
              <div className="text-xs font-semibold uppercase tracking-widest">Loading comments…</div>
            </div>
          ) : comments.length === 0 ? (
            <div className="py-10 flex flex-col items-center gap-3 text-ink-500 text-center px-4">
              <div className="h-14 w-14 rounded-full bg-ink-950/5 flex items-center justify-center">
                <svg viewBox="0 0 24 24" className="h-7 w-7 text-ink-600" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
              </div>
              <p className="font-bold text-ink-700">Be the first to comment</p>
              <p className="text-sm">
                Share your take on this story. Keep it respectful and on-topic.
              </p>
            </div>
          ) : (
            comments.map((c) => <CommentRow key={String(c.id)} comment={c} />)
          )}
        </div>

        <div className="shrink-0 border-t border-ink-950/8 px-4 py-3 bg-ink-950/[0.02]">
          <div className="flex items-end gap-2">
            <div className="flex-1 min-w-0">
              <label htmlFor="shorts-comment-input" className="sr-only">Write a comment</label>
              <textarea
                id="shorts-comment-input"
                rows={2}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Add a comment…"
                className="w-full resize-none rounded-2xl bg-white ring-1 ring-ink-950/10 focus:ring-2 focus:ring-news px-4 py-2.5 text-[14px] text-ink-950 placeholder:text-ink-500 focus:outline-none"
                onKeyDown={(e) => {
                  if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
                    e.preventDefault();
                    void submit();
                  }
                }}
              />
            </div>
            <button
              type="button"
              onClick={() => void submit()}
              disabled={submitting || !input.trim()}
              className={cn(
                "shrink-0 h-11 rounded-full px-5 text-[12px] font-black uppercase tracking-widest transition",
                submitting || !input.trim()
                  ? "bg-ink-950/10 text-ink-500 cursor-not-allowed"
                  : "bg-news text-white hover:bg-news/90",
              )}
            >
              Post
            </button>
          </div>
          <p className="mt-2 text-[10px] text-ink-500 font-semibold">
            ⌘ / Ctrl + Enter to post · Comments are moderated.
          </p>
        </div>
      </div>
    </div>
  );
}

function CommentRow({ comment }: { comment: PostCommentResponse }) {
  const name =
    comment.authorFirstName || comment.authorLastName
      ? `${comment.authorFirstName || ""} ${comment.authorLastName || ""}`.trim()
      : "Reader";
  const av = initials(comment.authorFirstName || "", comment.authorLastName || "") || "R";
  return (
    <div className="flex gap-3">
      <div className="shrink-0 h-9 w-9 rounded-full bg-ink-950/10 text-ink-950 text-[11px] font-black flex items-center justify-center overflow-hidden">
        {comment.authorAvatarUrl ? (
          <img src={comment.authorAvatarUrl} className="h-full w-full object-cover" alt="" />
        ) : (
          <span>{av}</span>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-[13px] font-black text-ink-950">{name}</span>
          <span className="text-[10px] font-semibold uppercase tracking-widest text-ink-500">
            {formatDate(comment.createdAt)}
          </span>
        </div>
        <p className="mt-1 text-[13.5px] leading-relaxed text-ink-900 whitespace-pre-wrap break-words">
          {comment.content}
        </p>
        <div className="mt-2 flex items-center gap-3 text-[11px] font-bold text-ink-500">
          <button type="button" className="hover:text-news">
            ♥ {formatCount(comment.likeCount || 0)}
          </button>
          <button type="button" className="hover:text-ink-950">
            Reply
          </button>
        </div>
      </div>
    </div>
  );
}
