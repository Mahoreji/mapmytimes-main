"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { blogApi } from "@/lib/api/blogApi";
import type {
  BlogPostResponse,
  PostCommentResponse,
  BlogPostSummaryResponse,
} from "@/types/blog";
import { PostCard, SectionTitle, Badge } from "@/components/posts/PostCard";
import { Button } from "@/components/ui/Button";
import { Textarea } from "@/components/ui/Input";
import { useAuth } from "@/lib/auth/AuthProvider";
import { tokenStorage } from "@/lib/auth/token-storage";
import {
  Eye,
  Heart,
  MessageSquare,
  Share2,
  Clock,
  Bookmark,
  ChevronLeft,
  Send,
  User as UserIcon,
  Twitter,
  Facebook,
  Linkedin,
  Link as LinkIcon,
  List,
  Copy,
  BookOpen,
  Type as TypeIcon,
  X as XIcon,
  Check as CheckIcon,
  Circle as CircleIcon,
  Radio as RadioIcon,
  Trash2,
  Highlighter,
} from "lucide-react";
import {
  cn,
  formatDate,
  formatRelative,
  initials,
  readingTimeLabel,
  SITE,
  formatDateTime,
} from "@/lib/utils";
import { avatarOrDefault } from "@/lib/assets";
import { ArticleSeoMeta } from "./SeoMeta";
import { VideoEmbed } from "@/components/posts/VideoEmbed";
import { findPostPrimaryVideo, parseVideoUrl } from "@/lib/video";
import { useLanguage } from "@/lib/i18n/LanguageContext";
import {
  applyHighlightSpans,
  createHighlight,
  deleteHighlight,
  listHighlights,
  resolveSelection,
  type HighlightResponse,
  type SelectionPoint,
} from "@/lib/highlights";

export default function ArticlePage() {
  const { lang } = useLanguage();
  const params = useParams<{ slug: string }>();
  const searchParams = useSearchParams();
  const slug = typeof params?.slug === "string" ? decodeURIComponent(params.slug) : "";
  const auth = useAuth();
  const resumeQueryPct = useMemo(() => {
    const raw = searchParams?.get("resume");
    if (!raw) return null;
    const n = parseInt(raw, 10);
    if (Number.isNaN(n) || n < 5 || n > 95) return null;
    return n;
  }, [searchParams]);

  const [post, setPost] = useState<BlogPostResponse | null>(null);
  const [comments, setComments] = useState<PostCommentResponse[]>([]);
  const [liked, setLiked] = useState<boolean>(false);
  const [likeCount, setLikeCount] = useState(0);
  const [state, setState] = useState<"loading" | "ready" | "notFound">("loading");
  const [commentText, setCommentText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [viewed, setViewed] = useState(false);
  const [progress, setProgress] = useState(0);
  const [tocOpen, setTocOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [related, setRelated] = useState<BlogPostSummaryResponse[]>([]);

  const [translatedMap, setTranslatedMap] = useState<Record<string, string> | null>(null);
  const [translateMode, setTranslateMode] = useState<"native" | "translated">("native");
  const [translateLoading, setTranslateLoading] = useState(false);
  const [translateError, setTranslateError] = useState<string | null>(null);

  // ========================================================================
  // READER MODE (Phase 1 Website)
  // ========================================================================
  type ReaderFontStack = "sans" | "serif";
  type ReaderTheme = "light" | "dark" | "sepia";
  type ReaderLineSpacing = "compact" | "normal" | "relaxed";

  const READER_FONT_SIZES = [13, 15, 17, 19, 22];
  const READER_FONT_DEFAULT_IDX = 2;
  const READER_LH: Record<ReaderLineSpacing, number> = {
    compact: 1.45,
    normal: 1.7,
    relaxed: 2.05,
  };
  const READER_LH_LABEL: Record<ReaderLineSpacing, string> = {
    compact: "Compact",
    normal: "Normal",
    relaxed: "Relaxed",
  };
  const READER_STACK_LABEL: Record<ReaderFontStack, string> = {
    sans: "Sans (Default)",
    serif: "Serif (Long-form)",
  };
  const READER_BG: Record<ReaderTheme, string> = {
    light: "#ffffff",
    dark: "#0A0A0A",
    sepia: "#F4ECD8",
  };
  const READER_FG: Record<ReaderTheme, string> = {
    light: "#0A0A0A",
    dark: "#ffffff",
    sepia: "#5B4636",
  };
  const LS_KEY = "mmt:reader:prefs";
  const LS_PROGRESS_PREFIX = "mmt:reader:progress:";
  const LS_META_PREFIX = "mmt:reader:meta:";
  const persistProgressMeta = () => {
    try {
      if (!post?.id) return;
      const meta = {
        slug: post.slug,
        title: post.title,
        excerpt: (post as any).excerpt ?? "",
        cover: (post as any).featuredImageUrl,
        featuredImageUrl: (post as any).featuredImageUrl,
        readingTimeMinutes: (post as any).readingTimeMinutes ?? (post as any).readingTime,
        viewCount: (post as any).viewCount ?? 0,
        categories: (post as any).categories ?? [],
      };
      localStorage.setItem(LS_META_PREFIX + post.id, JSON.stringify(meta));
      localStorage.setItem(LS_META_PREFIX + post.id + ":ts", String(Date.now()));
    } catch {}
  };
  useEffect(() => {
    if (post?.id) persistProgressMeta();
  }, [post?.id, post?.title, post?.slug, (post as any)?.featuredImageUrl, (post as any)?.readingTimeMinutes]);

  const [isReaderMode, setIsReaderMode] = useState(false);
  const [showAaPanel, setShowAaPanel] = useState(false);
  const [readerFontIdx, setReaderFontIdx] = useState(READER_FONT_DEFAULT_IDX);
  const [readerStack, setReaderStack] = useState<ReaderFontStack>("sans");
  const [readerLH, setReaderLH] = useState<ReaderLineSpacing>("normal");
  const [readerTheme, setReaderTheme] = useState<ReaderTheme>("light");
  const [readerAutoDismissed, setReaderAutoDismissed] = useState(false);
  const [showReaderSuggest, setShowReaderSuggest] = useState(false);
  const [lastSentProgress, setLastSentProgress] = useState<number>(-1);
  const [resumeBannerVisible, setResumeBannerVisible] = useState<boolean>(false);
  const [resumePercent, setResumePercent] = useState<number | null>(null);

  // ========================================================================
  // HIGHLIGHTS (Build4 V1)
  // ========================================================================
  const [highlights, setHighlights] = useState<HighlightResponse[]>([]);
  const [hlSelection, setHlSelection] = useState<SelectionPoint | null>(null);
  const [hlBusy, setHlBusy] = useState<boolean>(false);
  const [hlDeleting, setHlDeleting] = useState<string | null>(null);
  const readerBodyRef = useRef<HTMLDivElement | null>(null);
  const floatToolbarRef = useRef<HTMLDivElement | null>(null);
  const deletePopoverRef = useRef<HTMLDivElement | null>(null);
  const [deletePopover, setDeletePopover] = useState<{ hl: HighlightResponse; x: number; y: number } | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      const raw = localStorage.getItem(LS_KEY);
      if (raw) {
        const p = JSON.parse(raw) as any;
        if (typeof p?.fontIdx === "number") setReaderFontIdx(Math.max(0, Math.min(READER_FONT_SIZES.length - 1, p.fontIdx)));
        if (p?.stack === "sans" || p?.stack === "serif") setReaderStack(p.stack);
        if (p?.lh === "compact" || p?.lh === "normal" || p?.lh === "relaxed") setReaderLH(p.lh);
        if (p?.theme === "light" || p?.theme === "dark" || p?.theme === "sepia") setReaderTheme(p.theme);
      }
    } catch {}
    if (auth.isAuthenticated && post?.id) {
      const token = tokenStorage.access;
      if (token) {
        fetch(`${SITE.apiBase.replace(/\/$/, "")}/api/v1/users/me/reader-preferences`, {
          headers: { Authorization: `Bearer ${token}` },
        }).then((r) => {
          if (!r.ok) return;
          return r.json();
        }).then((data) => {
          const prefs = data?.data ?? data;
          if (!prefs) return;
          if (typeof prefs?.fontSizeIdx === "number") setReaderFontIdx(Math.max(0, Math.min(READER_FONT_SIZES.length - 1, prefs.fontSizeIdx)));
          if (prefs?.fontStack === "sans" || prefs?.fontStack === "serif") setReaderStack(prefs.fontStack);
          if (prefs?.lineSpacing === "compact" || prefs?.lineSpacing === "normal" || prefs?.lineSpacing === "relaxed") setReaderLH(prefs.lineSpacing);
          if (prefs?.theme === "light" || prefs?.theme === "dark" || prefs?.theme === "sepia") setReaderTheme(prefs.theme);
          try {
            localStorage.setItem(
              LS_KEY,
              JSON.stringify({
                fontIdx: typeof prefs?.fontSizeIdx === "number" ? prefs.fontSizeIdx : readerFontIdx,
                stack: prefs?.fontStack ?? readerStack,
                lh: prefs?.lineSpacing ?? readerLH,
                theme: prefs?.theme ?? readerTheme,
              }),
            );
          } catch {}
        }).catch(() => {});
      }
    }
    if (post) {
      const words = computeStrippedWordCount(post.content ?? (post as any).contentHtml ?? "");
      const dismissedKey = `mmt:reader:suggest:${post.id}`;
      let dismissed = false;
      try { dismissed = localStorage.getItem(dismissedKey) === "1"; } catch {}
      setReaderAutoDismissed(dismissed);
      setShowReaderSuggest(!dismissed && words >= 800);
    }
  }, [post?.id, auth.isAuthenticated]);

  useEffect(() => {
    if (!post?.id) return;
    if (typeof window === "undefined") return;
    setResumeBannerVisible(false);
    setResumePercent(null);
    setLastSentProgress(-1);
    if (resumeQueryPct != null) {
      return;
    }
    const loadProgress = async () => {
      let percent: number | null = null;
      if (auth.isAuthenticated) {
        const token = tokenStorage.access;
        if (token) {
          try {
            const r = await fetch(`${SITE.apiBase.replace(/\/$/, "")}/api/v1/reading-progress/me/post/${post.id}`, {
              headers: { Authorization: `Bearer ${token}` },
            });
            if (r.ok) {
              const data = await r.json();
              const raw = data?.data?.scrollPercent ?? data?.scrollPercent;
              if (typeof raw === "number") percent = Math.round(raw);
            }
          } catch {}
        }
      }
      if (percent == null) {
        try {
          const raw = localStorage.getItem(LS_PROGRESS_PREFIX + post.id);
          if (raw) {
            const n = parseInt(raw, 10);
            if (!isNaN(n)) percent = n;
          }
        } catch {}
      }
      if (percent != null && percent >= 5 && percent <= 95) {
        setResumePercent(percent);
        setResumeBannerVisible(true);
      }
    };
    void loadProgress();
  }, [post?.id, auth.isAuthenticated, resumeQueryPct]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (resumeQueryPct == null) return;
    if (state !== "ready" || !post?.id) return;
    setIsReaderMode(true);
    setResumeBannerVisible(false);
    setResumePercent(resumeQueryPct);
    const raf = requestAnimationFrame(() => {
      const root = document.documentElement;
      const max = Math.max(
        1,
        root.scrollHeight - root.clientHeight,
        document.body ? document.body.scrollHeight - window.innerHeight : 0,
      );
      const target = Math.max(0, Math.min(max, (max * resumeQueryPct) / 100));
      window.scrollTo({ top: target, behavior: "auto" });
    });
    return () => cancelAnimationFrame(raf);
  }, [state, post?.id, resumeQueryPct]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const prefsObj = { fontIdx: readerFontIdx, stack: readerStack, lh: readerLH, theme: readerTheme };
    try {
      localStorage.setItem(LS_KEY, JSON.stringify(prefsObj));
    } catch {}
    if (auth.isAuthenticated) {
      const token = tokenStorage.access;
      if (token) {
        const body = {
          fontSizeIdx: readerFontIdx,
          fontStack: readerStack,
          lineSpacing: readerLH,
          theme: readerTheme,
        };
        fetch(`${SITE.apiBase.replace(/\/$/, "")}/api/v1/users/me/reader-preferences`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(body),
        }).catch(() => {});
      }
    }
  }, [readerFontIdx, readerStack, readerLH, readerTheme, auth.isAuthenticated]);

  function dismissReaderSuggest(andEnter: boolean) {
    if (!post) return;
    const dismissedKey = `mmt:reader:suggest:${post.id}`;
    try { localStorage.setItem(dismissedKey, "1"); } catch {}
    setReaderAutoDismissed(true);
    setShowReaderSuggest(false);
    if (andEnter) setIsReaderMode(true);
  }

  // Word-count utility (P0-3 parity with backend Java + mobile Dart regex)
  function stripHtmlAndMarkdown(raw: string): string {
    if (!raw) return "";
    let s = String(raw);
    s = s
      .replace(/&nbsp;/g, " ").replace(/&amp;/g, "&").replace(/&quot;/g, "\"")
      .replace(/&#39;/g, "'").replace(/&lt;/g, "<").replace(/&gt;/g, ">");
    // Pass 1: markdown links + images, preserve link text only
    s = s.replace(/!?\[([^\]]*)\]\([^)]*\)/g, (_m, text) => ` ${text} `);
    s = s.replace(/!\[[^\]]*\]\([^)]*\)/g, "");
    // Pass 2: HTML tags
    s = s.replace(/<[^>]+>/g, " ");
    // Pass 3: Markdown headings prefix (#, ##, etc.)
    s = s.replace(/^\s*#{1,6}\s+/gm, "");
    // Pass 4: Markdown formatting chars
    s = s.replace(/(\*{1,3}|_{1,3}|`{1,3}|~~|>\s|\|\s|-\s)/g, " ");
    // Collapse whitespace
    s = s.replace(/\s+/g, " ").trim();
    return s;
  }

  function computeStrippedWordCount(content: string): number {
    const stripped = stripHtmlAndMarkdown(content || "");
    if (!stripped) return 0;
    return stripped.split(/\s+/).filter(Boolean).length;
  }

  const readerFontPx = READER_FONT_SIZES[readerFontIdx];
  const readerLineHeight = READER_LH[readerLH];
  const readerBg = READER_BG[readerTheme];
  const readerFg = READER_FG[readerTheme];
  const readerChrome = "#E31E24";
  const readerFgMuted = readerTheme === "dark" ? "rgba(255,255,255,0.62)" : readerTheme === "sepia" ? "rgba(91,70,54,0.62)" : "rgba(10,10,10,0.62)";
  const readerBorder = readerTheme === "dark" ? "rgba(255,255,255,0.18)" : readerTheme === "sepia" ? "rgba(91,70,54,0.22)" : "rgba(10,10,10,0.18)";

  const postLang = (typeof (post as any)?.language === "string" ? (post as any).language.toLowerCase() : "en") as "en" | "hi";
  const userLang = lang as "en" | "hi";
  const canTranslate = post && postLang !== userLang;

  useEffect(() => {
    let debounceTimer: ReturnType<typeof setTimeout> | null = null;
    const flushProgress = (pct: number) => {
      setLastSentProgress(pct);
      if (!post?.id) return;
      persistProgressMeta();
      try {
        localStorage.setItem(LS_PROGRESS_PREFIX + post.id, String(pct));
      } catch {}
      if (auth.isAuthenticated) {
        const token = tokenStorage.access;
        if (token) {
          fetch(`${SITE.apiBase.replace(/\/$/, "")}/api/v1/reading-progress/me`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({ postId: post.id, scrollPercent: pct }),
          }).catch(() => {});
        }
      }
    };
    function onScroll() {
      const el = document.documentElement;
      const total = el.scrollHeight - el.clientHeight;
      const pct = total <= 0 ? 0 : Math.round(Math.max(0, Math.min(100, (el.scrollTop / total) * 100)));
      setProgress(pct);
      if (!post?.id) return;
      if (lastSentProgress === -1 || Math.abs(pct - lastSentProgress) >= 5) {
        if (debounceTimer) { clearTimeout(debounceTimer); debounceTimer = null; }
        flushProgress(pct);
      } else {
        if (debounceTimer) clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => flushProgress(pct), 5000);
      }
    }
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      window.removeEventListener("scroll", onScroll);
      if (debounceTimer) clearTimeout(debounceTimer);
    };
  }, [state, post?.id, auth.isAuthenticated, lastSentProgress]);

  // ========================================================================
  // HIGHLIGHTS EFFECTS + HANDLERS
  // ========================================================================
  useEffect(() => {
    if (!post?.id || !isReaderMode) return;
    let active = true;
    setHighlights([]);
    listHighlights(post.id)
      .then((list) => { if (active) setHighlights(list); })
      .catch(() => { if (active) setHighlights([]); });
    return () => { active = false; };
  }, [post?.id, isReaderMode, auth.isAuthenticated]);

  useEffect(() => {
    if (!isReaderMode) return;
    const root = readerBodyRef.current;
    if (!root) return;
    const id = window.requestAnimationFrame(() => {
      applyHighlightSpans(root, highlights, (hl, ev) => {
        const x = ev.clientX;
        const y = ev.clientY;
        setDeletePopover({ hl, x, y });
      });
    });
    return () => window.cancelAnimationFrame(id);
  }, [highlights, isReaderMode, translateMode, translatedMap]);

  useEffect(() => {
    if (!isReaderMode) { setHlSelection(null); return; }

    function updateFromSelection(ev?: Event) {
      const sel = window.getSelection?.() ?? null;
      if (!sel || sel.rangeCount === 0 || sel.isCollapsed) {
        if (ev && ev.type === "mouseup") {
          // Wait a tick for browser selection to finalize on triple-click etc.
          setTimeout(() => {
            const s2 = window.getSelection?.() ?? null;
            if (!s2 || s2.rangeCount === 0 || s2.isCollapsed) return;
            const root = readerBodyRef.current;
            if (!root) return;
            const sp = resolveSelection(root, s2);
            if (sp) setHlSelection(sp);
          }, 10);
          return;
        }
        return;
      }
      const root = readerBodyRef.current;
      if (!root) return;
      const inReader =
        root.contains(sel.anchorNode) && root.contains(sel.focusNode);
      if (!inReader) return;
      const sp = resolveSelection(root, sel);
      if (sp) setHlSelection(sp);
    }
    function onDocMouseDown(ev: MouseEvent) {
      const el = ev.target as HTMLElement | null;
      if (!el) return;
      if (floatToolbarRef.current && floatToolbarRef.current.contains(el)) return;
      if (deletePopoverRef.current && deletePopoverRef.current.contains(el)) return;
      if (el.closest?.("span[data-mmt-highlight]")) return;
      if (deletePopover) setDeletePopover(null);
      // Don't immediately clear — the mouseup may establish a selection.
    }
    document.addEventListener("selectionchange", updateFromSelection);
    window.addEventListener("mouseup", updateFromSelection);
    document.addEventListener("mousedown", onDocMouseDown, true);
    return () => {
      document.removeEventListener("selectionchange", updateFromSelection);
      window.removeEventListener("mouseup", updateFromSelection);
      document.removeEventListener("mousedown", onDocMouseDown, true);
    };
  }, [isReaderMode, deletePopover]);

  async function doCreateHighlight() {
    if (!post?.id || !hlSelection) return;
    if (!auth.isAuthenticated) {
      setHlSelection(null);
      window.getSelection?.()?.removeAllRanges();
      alert("Please sign in to highlight text.");
      return;
    }
    setHlBusy(true);
    try {
      const created = await createHighlight({
        postId: post.id,
        paragraphIndex: hlSelection.paragraphIndex,
        charStart: hlSelection.charStart,
        charEnd: hlSelection.charEnd,
        excerpt: hlSelection.excerpt,
      });
      setHighlights((list) => [...list, created]);
      setHlSelection(null);
      window.getSelection?.()?.removeAllRanges();
    } catch (e: any) {
      console.warn("create highlight failed:", e?.message ?? e);
    } finally {
      setHlBusy(false);
    }
  }

  async function doDeleteHighlight(hl: HighlightResponse) {
    setHlDeleting(hl.id);
    try {
      await deleteHighlight(hl.id);
      setHighlights((list) => list.filter((x) => x.id !== hl.id));
    } catch (e: any) {
      console.warn("delete highlight failed:", e?.message ?? e);
    } finally {
      setHlDeleting(null);
      setDeletePopover(null);
    }
  }

  useEffect(() => {
    if (!deletePopover) return;
    function onClick(ev: MouseEvent) {
      const el = ev.target as HTMLElement | null;
      if (!el) return;
      if (deletePopoverRef.current && deletePopoverRef.current.contains(el)) return;
      setDeletePopover(null);
    }
    document.addEventListener("click", onClick, true);
    return () => document.removeEventListener("click", onClick, true);
  }, [deletePopover]);

  useEffect(() => {
    if (!slug) return;
    let active = true;
    setState("loading");
    Promise.all([
      blogApi.posts.bySlug(slug).catch(() => null),
    ]).then(async ([p]) => {
      if (!active) return;
      if (!p) {
        setState("notFound");
        return;
      }
      setPost(p);
      if (p.title) {
        document.title = `${p.title} — MapMyTimes`;
      }
      setLikeCount(p.likeCount ?? 0);
      try {
        const [approved, myLike, rel] = await Promise.all([
          blogApi.comments.byPostApproved(p.id).catch(() => [] as any),
          auth.isAuthenticated ? blogApi.likes.check(p.id).catch(() => false) : Promise.resolve(false),
          Array.isArray(p.relatedPosts) && p.relatedPosts.length > 0
            ? Promise.resolve(p.relatedPosts as any[])
            : (p.categories?.[0]?.slug
                ? blogApi.posts.advancedSearch({ categoryIds: p.categories[0].id ? [p.categories[0].id] : undefined, page: 1, size: 5, language: lang.toUpperCase() as any } as any).catch(() => null)
                : Promise.resolve(null)
              ).then((res: any) => {
                const list: any[] = res?.data ?? res ?? [];
                return list.filter((x: any) => x?.id && x.id !== p.id).slice(0, 4);
              }),
        ]);
        setComments(approved ?? []);
        setLiked(!!myLike);
        setRelated(rel ?? []);
      } catch {}
      setState("ready");
      if (!viewed) {
        void blogApi.posts.incrementView(p.id);
        setViewed(true);
      }
    });
    return () => { active = false; };
  }, [slug, auth.isAuthenticated, viewed, lang]);

  async function toggleLike() {
    if (!auth.isAuthenticated) return;
    if (!post) return;
    try {
      if (liked) {
        await blogApi.likes.unlike(post.id);
        setLiked(false);
        setLikeCount((n) => Math.max(0, n - 1));
      } else {
        await blogApi.likes.post(post.id);
        setLiked(true);
        setLikeCount((n) => n + 1);
      }
    } catch {}
  }

  async function submitComment(e: React.FormEvent) {
    e.preventDefault();
    if (!auth.isAuthenticated || !post) return;
    const t = commentText.trim();
    if (!t) return;
    setSubmitting(true);
    try {
      const c = await blogApi.comments.create({ postId: post.id, content: t });
      setComments((list) => [...list, { ...c, status: "PENDING" } as PostCommentResponse]);
      setCommentText("");
    } finally {
      setSubmitting(false);
    }
  }

  function onShare() {
    if (!post) return;
    const url = `${SITE.url}/news/${encodeURIComponent(post.slug)}`;
    if (navigator.share) {
      navigator.share({ title: post.title, text: post.excerpt, url }).catch(() => {});
    } else {
      void copyUrl(url);
    }
  }

  async function copyUrl(u?: string) {
    const url = u ?? `${SITE.url}/news/${encodeURIComponent(post?.slug ?? "")}`;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {}
  }

  function safeDecode(encoded: string): string {
    if (!encoded) return encoded;
    let out = encoded
      .replace(/%3ए/g, ":")
      .replace(/%3aे/g, ":")
      .replace(/%3A|%3a/g, ":")
      .replace(/%2सी|%2cी|%2Cी/g, ",")
      .replace(/%2C|%2c/g, ",")
      .replace(/%26/g, "&")
      .replace(/%20/g, " ")
      .replace(/%3[Bb]/g, ";")
      .replace(/%3[Ff]/g, "?")
      .replace(/%2[Ff]/g, "/")
      .replace(/%28/g, "(").replace(/%29/g, ")")
      .replace(/%21/g, "!").replace(/%22/g, "\"")
      .replace(/%27/g, "'").replace(/%60/g, "`")
      .replace(/%2[Bb]/g, "+").replace(/%3[Dd]/g, "=")
      .replace(/%24/g, "$").replace(/%40/g, "@").replace(/%23/g, "#")
      .replace(/%25/g, "%")
      .replace(/%5[Bb]/g, "[").replace(/%5[Dd]/g, "]")
      .replace(/%7[Bb]/g, "{").replace(/%7[Dd]/g, "}")
      .replace(/%3[Cc]/g, "<").replace(/%3[Ee]/g, ">")
      .replace(/%C2%AB|%c2%ab/g, "«").replace(/%C2%BB|%c2%bb/g, "»")
      .replace(/%E2%80%93/g, "–").replace(/%E2%80%94/g, "—")
      .replace(/%E2%80%98/g, "‘").replace(/%E2%80%99/g, "’")
      .replace(/%E2%80%9C/g, "“").replace(/%E2%80%9D/g, "”")
      .replace(/%E2%80%A6/g, "…");
    try {
      const txt = document.createElement("textarea");
      txt.innerHTML = out
        .replace(/&lt;/g, "<").replace(/&gt;/g, ">")
        .replace(/&amp;/g, "&").replace(/&quot;/g, "\"")
        .replace(/&apos;/g, "'").replace(/&#39;/g, "'")
        .replace(/&#x27;/g, "'").replace(/&#x2F;/g, "/")
        .replace(/&#38;/g, "&").replace(/&#x3D;/g, "=")
        .replace(/&#x22;/g, "\"").replace(/&#x3C;/g, "<")
        .replace(/&#x3E;/g, ">").replace(/&#x20;/g, " ");
      out = txt.value;
    } catch {}
    for (let i = 0; i < 3; i++) {
      try {
        const next = decodeURIComponent(out);
        if (next === out) break;
        out = next;
      } catch { break; }
    }
    return out;
  }

  async function doTranslate() {
    if (!post || !canTranslate) return;
    if (translatedMap && translateMode === "translated") {
      setTranslateMode("translated");
      return;
    }
    if (translatedMap) {
      setTranslateMode("translated");
      setTranslateError(null);
      return;
    }
    try {
      setTranslateLoading(true);
      setTranslateError(null);
      const items = gatherTranslateItems(post, headings);
      const res = await blogApi.translate.run({ sourceLang: postLang, targetLang: userLang, items });
      const map: Record<string, string> = {};
      if (res?.items && Array.isArray(res.items)) {
        for (const it of res.items) {
          if (it?.id && it?.translated) {
            map[it.id] = safeDecode(it.translated);
          }
        }
      }
      setTranslatedMap(map);
      setTranslateMode("translated");
      setTranslateError(null);
    } catch (err: any) {
      setTranslateMode("native");
      setTranslateError(err?.message || "Translation failed. Please try again.");
    } finally {
      setTranslateLoading(false);
    }
  }

  const articleUrl = useMemo(
    () => `${SITE.url}/news/${encodeURIComponent(post?.slug ?? "")}`,
    [post?.slug],
  );

  const headings = useMemo(() => extractHeadings(post), [post]);

  const seo = post?.seo ?? {};
  const metaTitle = seo.metaTitle ?? post?.title ?? "";
  const metaDesc = seo.metaDescription ?? post?.excerpt ?? "";
  const ogImage =
    seo.ogImage ??
    (typeof (post as any)?.featuredImage === "string"
      ? (post as any).featuredImage
      : (post as any)?.featuredImage?.url ?? (post as any)?.featuredImageUrl);

  const primaryVideo = useMemo(() => {
    if (!post) return null;
    const direct = parseVideoUrl((post as any).primaryVideoUrl ?? null);
    if (direct) return direct;
    return findPostPrimaryVideo((post as any).media ?? [], post.contentBlocks ?? null);
  }, [post]);

  const authorUrl = post?.userId ? `/author/${encodeURIComponent(post.userId)}` : null;

  if (state === "loading") {
    return (
      <div className="mx-auto max-w-6xl px-4 py-10">
        <div className="animate-pulse space-y-6">
          <div className="aspect-[16/9] bg-ink-900/10 border-2 border-ink-950" />
          <div className="h-4 w-1/3 bg-ink-900/10" />
          <div className="h-10 w-full bg-ink-900/20" />
          <div className="h-5 w-2/3 bg-ink-900/10" />
          <div className="h-96 bg-ink-900/5 border-2 border-ink-950" />
        </div>
      </div>
    );
  }

  if (state === "notFound" || !post) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20 text-center">
        <div className="ribbon mb-4">404</div>
        <h1 className="font-headline text-4xl sm:text-5xl uppercase mb-3">Story not found</h1>
        <p className="text-ink-700 mb-6">
          This article may have been moved or unpublished by the newsroom.
        </p>
        <Link href="/">
          <Button variant="news">Back to homepage</Button>
        </Link>
      </div>
    );
  }

  return (
    <>
      <ArticleSeoMeta post={post} />
      {/* Fixed red progress bar (top) */}
      <div
        className="fixed top-0 left-0 right-0 z-[60] h-1 pointer-events-none"
        style={{ background: isReaderMode ? `${readerBorder}` : undefined }}
      >
        <div
          className="h-full transition-[width] duration-100"
          style={{
            width: `${progress}%`,
            background: isReaderMode ? readerChrome : undefined,
            backgroundColor: !isReaderMode ? undefined : undefined,
            ...(!isReaderMode ? { backgroundColor: "var(--color-news, #E31E24)" } : {}),
          }}
        />
      </div>

      {/* Google Fonts + Noto Devanagari inline (for Reader Mode typography stacks) */}
      <style jsx global>{`
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;700;800;900&family=Noto+Sans+Devanagari:wght@400;500;700;800&family=Noto+Serif:wght@400;500;700;800&family=Noto+Serif+Devanagari:wght@400;500;700;800&family=Archivo+Black&display=swap');

        .reader-body * { color: inherit !important; }
        .reader-body a { color: inherit !important; text-decoration: underline; text-decoration-color: ${readerChrome}; text-underline-offset: 2px; }
        .reader-body h1, .reader-body h2, .reader-body h3, .reader-body h4 {
          font-family: 'Archivo Black', 'Noto Sans Devanagari', sans-serif !important;
          letter-spacing: -0.01em !important;
          line-height: 1.12 !important;
          margin-top: 1.6em !important;
          margin-bottom: 0.6em !important;
          text-transform: uppercase !important;
        }
        .reader-body h1 { font-size: ${readerFontPx + 14}px !important; }
        .reader-body h2 { font-size: ${readerFontPx + 7}px !important; }
        .reader-body h3 { font-size: ${readerFontPx + 3}px !important; }
        .reader-body p, .reader-body li, .reader-body blockquote, .reader-body figcaption {
          font-family: inherit !important;
          font-size: ${readerFontPx}px !important;
          line-height: ${readerLineHeight} !important;
          margin-bottom: ${readerFontPx * 1.1}px !important;
          color: inherit !important;
          max-width: none !important;
        }
        .reader-body blockquote {
          border-left: 3px solid ${readerChrome};
          padding: 0.2em 1.1em !important;
          font-style: italic;
          opacity: 0.92;
        }
        .reader-body ul, .reader-body ol {
          padding-left: 1.4em !important;
          margin-bottom: ${readerFontPx * 1.1}px !important;
        }
        .reader-body img, .reader-body figure {
          max-width: 100% !important;
          height: auto !important;
          margin: ${readerFontPx * 1.5}px auto !important;
          border-radius: 6px;
          overflow: hidden;
          display: block;
        }
        .reader-body iframe {
          max-width: 100% !important;
          width: 100% !important;
          border-radius: 6px;
          border: 0;
          aspect-ratio: 16 / 9;
        }
        .reader-body hr { border-color: ${readerBorder}; border-top-width: 1px; }
      `}</style>

      <article
        className={cn(
          "transition-[background-color,color] duration-200",
          isReaderMode ? "min-h-screen" : "mx-auto max-w-7xl px-4 py-5 sm:py-7",
        )}
        style={isReaderMode ? {
          backgroundColor: readerBg,
          color: readerFg,
          fontFamily: readerStack === "serif"
            ? "'Noto Serif', 'Noto Serif Devanagari', Georgia, 'Times New Roman', serif"
            : "'Inter', 'Noto Sans Devanagari', -apple-system, BlinkMacSystemFont, sans-serif",
        } : undefined}
      >
        <div className={cn(!isReaderMode ? "" : "mx-auto")} style={isReaderMode ? { maxWidth: "1120px", padding: "28px 20px 48px" } : undefined}>
        <nav className={cn("flex items-center gap-2 text-xs font-bold uppercase tracking-widest mb-4",
          isReaderMode ? "text-ink-600" : "text-ink-600")}
          style={isReaderMode ? { color: readerFgMuted } : undefined}>
          <Link href="/" className={cn(isReaderMode ? "hover:text-news" : "hover:text-news")}
            style={isReaderMode ? { color: readerFgMuted } : undefined}
          ><ChevronLeft className="h-3.5 w-3.5 inline -mt-0.5" /> Home</Link>
          {post.categories?.[0] ? (
            <>
              <span>/</span>
              <Link
                href={`/category/${encodeURIComponent(post.categories[0].slug)}`}
                className="hover:text-news"
                style={isReaderMode ? { color: readerFgMuted } : undefined}
              >
                {post.categories[0].name}
              </Link>
            </>
          ) : null}
          <span>/</span>
          <span className={cn("truncate max-w-[50vw]")} style={isReaderMode ? { color: readerFg } : { color: "#0A0A0A" }}>
            {translateMode === "translated" ? (translatedMap?.["title"] ?? post.title) : post.title}
          </span>
        </nav>

        {canTranslate ? (
          <div className="mb-4 space-y-3">
            <div className="inline-flex items-center gap-1 border-2 border-ink-950 shadow-hard-sm bg-white p-0.5 h-9">
              <button type="button" onClick={() => setTranslateMode("native")}
                className={cn("h-8 px-2.5 text-[11px] font-bold uppercase tracking-widest transition-colors",
                  translateMode === "native" ? "bg-ink-950 text-white" : "hover:bg-ink-50")}>
                Native · {postLang === "hi" ? "हिन्दी" : "English"}
              </button>
              <button type="button"
                onClick={() => void doTranslate()}
                disabled={translateLoading}
                className={cn("h-8 px-2.5 text-[11px] font-bold uppercase tracking-widest transition-colors",
                  translateMode === "translated" ? "bg-news text-white" : "hover:bg-ink-50 disabled:opacity-50")}>
                {translateLoading ? "Translating…" : <>✨ AI · {userLang === "hi" ? "हिन्दी" : "English"}</>}
              </button>
            </div>
            {translateMode === "translated" ? (
              <div role="alert" className="border-2 border-news bg-news-50/60 p-3 sm:p-4 shadow-hard-sm text-xs sm:text-sm">
                <div className="font-headline uppercase tracking-wider text-news text-[11px] sm:text-xs mb-1">⚠️ AI Translation Beta · Disclaimer</div>
                <div className="text-ink-800 leading-relaxed">
                  This translation is auto-generated by Google Translate AI and may contain inaccuracies (names, legal, financial, factual, quotes). The authoritative journalistic source is the <strong>Native ({postLang === "hi" ? "हिन्दी" : "English"})</strong> version above. MapMyTimes is not liable for translation errors. Toggle back to Native for the definitive article.
                </div>
              </div>
            ) : null}
            {translateError ? (
              <div role="alert" className="border-2 border-ink-950 bg-ink-50 p-3 text-xs text-ink-900">
                Translation unavailable right now — showing native version. {String(translateError)}
              </div>
            ) : null}
          </div>
        ) : null}

        <div
          className={cn(
            isReaderMode ? "mx-auto" : "grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-8",
          )}
          style={isReaderMode ? { maxWidth: "100%" } : undefined}
        >
          <div
            className={cn(isReaderMode ? "w-full" : "lg:col-span-9 space-y-6 sm:space-y-8")}
            style={!isReaderMode ? undefined : {}}
          >
            <header className="space-y-4 sm:space-y-5">
              <div className="flex flex-wrap items-center gap-2 justify-between">
                <div className="flex flex-wrap items-center gap-2">
                {post.isFeatured ? <Badge variant="news">Featured</Badge> : null}
                {post.isTrending ? <Badge variant="ink">Trending</Badge> : null}
                {post.categories?.map((c) => (
                  <Link
                    key={c.id}
                    href={`/category/${encodeURIComponent(c.slug)}`}
                    className="ribbon text-[10px] sm:text-xs"
                  >
                    {c.name}
                  </Link>
                ))}
                </div>
                <div className="flex items-center gap-1.5 ml-auto sm:ml-4 mt-2 sm:mt-0">
                  <button
                    type="button"
                    onClick={() => setShowAaPanel((v) => !v)}
                    title="Typography"
                    aria-label="Typography settings"
                    className={cn(
                      "inline-flex h-9 items-center gap-1.5 border-2 px-2.5 text-[11px] font-black uppercase tracking-widest transition-colors",
                      isReaderMode
                        ? "border-ink-950/30 hover:border-news"
                        : "border-ink-950 bg-white hover:bg-ink-50",
                    )}
                    style={isReaderMode ? { borderColor: readerBorder, color: readerFg } : undefined}
                  >
                    <TypeIcon className="h-3.5 w-3.5" />
                    Aa
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsReaderMode((v) => !v)}
                    aria-pressed={isReaderMode}
                    title={isReaderMode ? "Exit Reader Mode" : "Enter Reader Mode"}
                    className={cn(
                      "inline-flex h-9 items-center gap-1.5 border-2 px-2.5 text-[11px] font-black uppercase tracking-widest transition-all",
                      isReaderMode
                        ? "bg-news text-white border-news shadow-hard-sm"
                        : "border-ink-950 bg-white hover:bg-ink-50",
                    )}
                    style={!isReaderMode ? {} : undefined}
                  >
                    <BookOpen className="h-3.5 w-3.5" />
                    {isReaderMode ? "Reader" : "Reader"}
                  </button>
                </div>
              </div>
              <h1 className="font-headline text-3xl sm:text-4xl md:text-5xl lg:text-6xl uppercase leading-[0.95] tracking-tight">
                {translateMode === "translated" ? (translatedMap?.["title"] ?? post.title) : post.title}
              </h1>
              {post.excerpt ? (
                <p className="text-base sm:text-lg md:text-xl text-ink-800 leading-snug max-w-3xl">
                  {translateMode === "translated" ? (translatedMap?.["excerpt"] ?? post.excerpt) : post.excerpt}
                </p>
              ) : null}

              <div className="flex flex-wrap items-center gap-4 border-y-2 border-ink-950 py-3 sm:py-4 text-xs font-bold uppercase tracking-widest text-ink-600">
                <span className="inline-flex items-center gap-1.5">
                  {formatDate(post.publishedAt ?? post.createdAt)}
                  {post.updatedAt && post.updatedAt !== post.createdAt
                    ? ` · Updated ${formatDateTime(post.updatedAt)}`
                    : ""}
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <Eye className="h-4 w-4" />
                  {(post.viewCount ?? 0).toLocaleString("en-IN")} views
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <MessageSquare className="h-4 w-4" />
                  {(post.commentCount ?? 0)} comments
                </span>
                {post.readingTime ? (
                  <span className="inline-flex items-center gap-1.5">
                    <Clock className="h-4 w-4" />
                    {readingTimeLabel(post.readingTime)}
                  </span>
                ) : null}
                {post.language ? (
                  <span className="inline-flex items-center gap-1.5 px-1.5 border border-ink-950/30">
                    {post.language.toUpperCase()}
                  </span>
                ) : null}
              </div>
            </header>

            {primaryVideo ? (
              <figure className="relative">
                <VideoEmbed
                  video={primaryVideo}
                  aspect="16:9"
                  showTitle={post?.title}
                  showCaption={(post as any)?.featuredImage?.caption ?? post?.excerpt ?? undefined}
                />
              </figure>
            ) : ogImage ? (
              <figure className="relative">
                <div className="relative overflow-hidden border-2 border-ink-950 shadow-hard aspect-[16/9] bg-ink-800">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={ogImage}
                    alt={(post as any)?.featuredImage?.alt ?? post.title}
                    className="h-full w-full object-cover"
                  />
                </div>
                {(post as any)?.featuredImage?.caption ? (
                  <figcaption className="mt-2 text-xs uppercase tracking-widest text-ink-600 font-semibold">
                    {(post as any).featuredImage.caption}
                  </figcaption>
                ) : null}
              </figure>
            ) : null}

            {headings.length > 0 ? (
              <div className="lg:hidden border-2 border-ink-950 bg-white">
                <button
                  type="button"
                  onClick={() => setTocOpen((v) => !v)}
                  className="w-full flex items-center justify-between px-3 py-2 text-xs font-bold uppercase tracking-widest"
                >
                  <span className="inline-flex items-center gap-2">
                    <List className="h-4 w-4" /> Table of contents
                  </span>
                  <ChevronLeft className={cn("h-4 w-4 transition-transform", !tocOpen && "-rotate-90")} />
                </button>
                {tocOpen ? (
                  <ol className="border-t-2 border-ink-950 px-3 py-2 space-y-1.5 text-sm">
                    {headings.map((h, i) => (
                      <li key={i}>
                        <a
                          href={`#${h.id}`}
                          onClick={() => setTocOpen(false)}
                          className={cn("hover:text-news", h.level >= 3 && "pl-3 text-ink-700 text-xs")}
                        >
                          {translateMode === "translated" ? (translatedMap?.[`toc-${i}`] ?? h.text) : h.text}
                        </a>
                      </li>
                    ))}
                  </ol>
                ) : null}
              </div>
            ) : null}

            <div ref={readerBodyRef} style={isReaderMode ? { maxWidth: "680px", margin: "0 auto", width: "100%" } : undefined} className={cn(isReaderMode ? "reader-body" : "")}>
            <ArticleBody
              content={post.content}
              blocks={post.contentBlocks ?? null}
              ogImage={ogImage}
              media={(post as any)?.media ?? []}
              excerpt={post.excerpt ?? null}
              lede={(post as any)?.lede ?? null}
              primaryVideo={primaryVideo}
              translations={translatedMap}
              translateMode={translateMode}
            />
            </div>

            {post.tags && post.tags.length > 0 ? (
              <div className="flex flex-wrap gap-2 pt-3 border-t-2 border-ink-950">
                <span className="text-xs font-bold uppercase tracking-widest text-ink-600 h-7 flex items-center mr-1">Tags:</span>
                {post.tags.filter(t => t && (t.name || t.slug)).map((t) => (
                  <Link
                    key={t.id ?? t.slug ?? String(t)}
                    href={`/tag/${encodeURIComponent(t.slug ?? t.name ?? String(t))}`}
                    className="inline-flex items-center px-2.5 py-1 h-7 border-2 border-ink-950 text-xs font-bold uppercase tracking-[0.15em] hover:bg-ink-950 hover:text-white"
                  >
                    #{t.name ?? t.slug ?? "tag"}
                  </Link>
                ))}
              </div>
            ) : null}

            <div className="border-t-2 border-ink-950 pt-5">
              <div className="flex flex-wrap items-center justify-between gap-3 mb-3">
                <div className="text-xs font-bold uppercase tracking-[0.18em] text-ink-600">
                  Enjoy this story? Share it
                </div>
                {comments.length > 0 ? (
                  <a href="#comments" className="text-xs font-bold uppercase tracking-[0.18em] text-news hover:underline">
                    Read {comments.length} comment{comments.length === 1 ? "" : "s"} ↓
                  </a>
                ) : (
                  <a href="#comments" className="text-xs font-bold uppercase tracking-[0.18em] text-ink-950 hover:text-news">
                    Join the conversation ↓
                  </a>
                )}
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <ActionIcon
                  active={liked}
                  onClick={toggleLike}
                  label={auth.isAuthenticated ? (liked ? "Unlike" : "Like") : "Sign in to like"}
                  count={likeCount}
                  icon={<Heart className={cn("h-4 w-4", liked && "fill-news text-news")} />}
                  disabled={!auth.isAuthenticated}
                />
                <ActionIcon
                  onClick={() => window.open(`https://twitter.com/intent/tweet?text=${encodeURIComponent(metaTitle)}&url=${encodeURIComponent(articleUrl)}`, "_blank", "noopener")}
                  label="Tweet"
                  icon={<Twitter className="h-4 w-4" />}
                />
                <ActionIcon
                  onClick={() => window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(articleUrl)}`, "_blank", "noopener")}
                  label="Facebook"
                  icon={<Facebook className="h-4 w-4" />}
                />
                <ActionIcon
                  onClick={() => window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(articleUrl)}`, "_blank", "noopener")}
                  label="LinkedIn"
                  icon={<Linkedin className="h-4 w-4" />}
                />
                <ActionIcon
                  onClick={onShare}
                  label="Share"
                  icon={<Share2 className="h-4 w-4" />}
                />
                <ActionIcon
                  onClick={() => void copyUrl()}
                  label={copied ? "Copied!" : "Copy link"}
                  icon={<Copy className="h-4 w-4" />}
                />
                <ActionIcon
                  href="#comments"
                  label={`${comments.length} comments`}
                  icon={<MessageSquare className="h-4 w-4" />}
                  count={comments.length || undefined}
                />
              </div>
            </div>

            {!isReaderMode && <AuthorCard post={post} />}

            {!isReaderMode && post.media && post.media.filter(m => m && m.url).length > 0 ? (
              <section>
                <SectionTitle eyebrow="Gallery" title="Photos & Media" />
                <div className="mt-5 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                  {post.media.filter(m => m && m.url).map((m, i) => (
                    <figure key={m.id ?? m.url ?? i}>
                      <a
                        href={m.url}
                        target="_blank"
                        rel="noreferrer"
                        className="block aspect-square overflow-hidden border-2 border-ink-950 bg-ink-800 group"
                      >
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img
                          src={m.url}
                          alt={m.caption ?? (typeof (m as any).alt === "string" ? (m as any).alt : post.title)}
                          className="h-full w-full object-cover group-hover:scale-105 transition-transform"
                        />
                      </a>
                      {m.caption ? (
                        <figcaption className="mt-1 text-[10px] uppercase tracking-widest text-ink-600 font-semibold leading-tight">
                          {m.caption}
                        </figcaption>
                      ) : null}
                    </figure>
                  ))}
                </div>
              </section>
            ) : null}

            {!isReaderMode && (
            <section id="comments">
              <SectionTitle
                eyebrow="Conversation"
                title={`Comments (${comments.length})`}
                action={
                  <span className="text-xs text-ink-600 uppercase tracking-widest font-bold">
                    {post.allowComments ? "Moderated" : "Closed"}
                  </span>
                }
              />
              {auth.isAuthenticated && post.allowComments ? (
                <form
                  onSubmit={submitComment}
                  className="mt-5 border-2 border-ink-950 p-4 bg-white space-y-3"
                >
                  <div className="flex items-center gap-3">
                    <div className="h-9 w-9 rounded-full bg-news text-white font-bold flex items-center justify-center text-sm border-2 border-ink-950">
                      {auth.user
                        ? initials(auth.user.firstName, auth.user.lastName)
                        : <UserIcon className="h-4 w-4" />}
                    </div>
                    <div className="text-xs font-bold uppercase tracking-widest text-ink-700">
                      Join the conversation as{" "}
                      <span className="text-ink-950">
                        {auth.user?.firstName} {auth.user?.lastName}
                      </span>
                    </div>
                  </div>
                  <Textarea
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    placeholder="Share your take, respectfully. Comments are moderated before publication."
                    required
                  />
                  <div className="flex items-center justify-between">
                    <p className="text-[11px] text-ink-600 font-semibold uppercase tracking-widest">
                      Comments are moderated & may take time to appear.
                    </p>
                    <Button type="submit" variant="news" size="sm" disabled={submitting}>
                      <Send className="h-4 w-4" />
                      {submitting ? "Sending…" : "Post comment"}
                    </Button>
                  </div>
                </form>
              ) : (
                <div className="mt-5 border-2 border-ink-950 p-4 bg-white text-sm text-ink-700 flex items-center justify-between gap-4">
                  <div>
                    {!auth.isAuthenticated
                      ? "Sign in to join the conversation and comment on this story."
                      : "Comments have been closed by the editors."}
                  </div>
                  {!auth.isAuthenticated ? (
                    <div className="flex gap-2 flex-shrink-0">
                      <Link href="/login">
                        <Button variant="outline" size="sm">Sign in</Button>
                      </Link>
                      <Link href="/signup">
                        <Button variant="news" size="sm">Join</Button>
                      </Link>
                    </div>
                  ) : null}
                </div>
              )}

              <ul className="mt-6 flex flex-col gap-4">
                {comments.length === 0 ? (
                  <li className="text-sm text-ink-600 italic">
                    No approved comments yet. Be the first to weigh in.
                  </li>
                ) : (
                  comments.map((c) => (
                    <li
                      key={c.id}
                      className={cn(
                        "flex gap-3 border-l-4 pl-4 py-2",
                        c.status === "PENDING"
                          ? "border-news bg-news-50/50"
                          : "border-ink-950",
                      )}
                    >
                      <div className="flex-shrink-0 h-10 w-10 rounded-full bg-ink-950 text-white font-bold flex items-center justify-center text-xs">
                        {initials(c.authorFirstName, c.authorLastName)}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <div className="text-sm font-bold uppercase tracking-wider">
                            {c.authorFirstName} {c.authorLastName}
                          </div>
                          <div className="text-[11px] text-ink-600 uppercase tracking-widest font-semibold">
                            {formatRelative(c.createdAt)}
                          </div>
                          {c.status === "PENDING" ? (
                            <span className="px-1.5 py-0.5 border-2 border-news text-news text-[10px] font-bold uppercase tracking-widest">
                              Awaiting moderation
                            </span>
                          ) : null}
                        </div>
                        <p className="mt-1 text-sm text-ink-800 whitespace-pre-wrap">{c.content}</p>
                      </div>
                    </li>
                  ))
                )}
              </ul>
            </section>
            )}
          </div>

          {!isReaderMode && (
          <aside className="lg:col-span-3 space-y-8 sm:space-y-10 lg:space-y-14">
            {headings.length > 0 ? (
              <div className="hidden lg:block sticky top-20 z-30 isolate max-h-[calc(100vh-6rem)] overflow-y-auto border-2 border-ink-950 bg-white p-4 shadow-hard-sm">
                <div className="text-xs font-bold uppercase tracking-widest mb-3 inline-flex items-center gap-2">
                  <List className="h-4 w-4" /> On this page
                </div>
                <ol className="space-y-1.5 text-sm pr-1">
                  {headings.map((h, i) => (
                    <li key={i}>
                      <a
                        href={`#${h.id}`}
                        className={cn(
                          "block py-0.5 hover:text-news border-l-2 pl-2 transition-colors",
                          h.level >= 3 ? "text-xs text-ink-700 border-white" : "font-semibold border-ink-950/20",
                        )}
                      >
                        {translateMode === "translated" ? (translatedMap?.[`toc-${i}`] ?? h.text) : h.text}
                      </a>
                    </li>
                  ))}
                </ol>
              </div>
            ) : null}

            {related.length > 0 ? (
              <div className="relative isolate pt-1 pl-1">
                <div className="flex items-end justify-between gap-3 border-b-4 border-ink-950 pb-2">
                  <div className="min-w-0">
                    <div className="ribbon text-[10px] sm:text-xs mb-2 shadow-hard-sm">Read next</div>
                    <h2
                      className="font-headline text-xl sm:text-2xl uppercase leading-tight tracking-tight relative pb-2 [&::after]:content-[''] [&::after]:absolute [&::after]:left-0 [&::after]:bottom-0 [&::after]:w-12 [&::after]:h-1.5 [&::after]:bg-news [&::after]:shadow-[4px_0_0_0_#0a0a0a]"
                    >
                      Related stories
                    </h2>
                  </div>
                </div>
                <ul className="mt-4 space-y-4 sm:space-y-5">
                  {related.map((rp, i) => (
                    <li key={rp.id ?? i} className="group relative isolate pt-1 pl-1">
                      <Link href={`/news/${encodeURIComponent(rp.slug)}`} className="flex gap-3">
                        <div className="relative flex-shrink-0 w-24 sm:w-28 aspect-[4/3] overflow-hidden border-2 border-ink-950 bg-ink-800 shadow-hard-sm">
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img
                            src={(rp as any).featuredImageUrl ?? (rp as any).featuredImage?.url ?? (rp as any).coverPhotoUrl ?? ""}
                            alt={rp.title}
                            className="h-full w-full object-cover group-hover:scale-[1.04] transition-transform"
                            onError={(e) => { e.currentTarget.style.opacity = "0"; }}
                          />
                          <div className="absolute top-1 left-1 h-5 w-5 bg-news text-white font-bold text-[10px] flex items-center justify-center border border-ink-950">
                            {i + 1}
                          </div>
                        </div>
                        <div className="min-w-0 pt-0.5">
                          <h4 className="font-headline text-sm leading-tight uppercase line-clamp-3 group-hover:text-news">
                            {rp.title}
                          </h4>
                          <p className="mt-1.5 text-[10px] font-bold uppercase tracking-widest text-ink-600">
                            {formatRelative((rp as any).publishedAt ?? rp.createdAt)}
                          </p>
                        </div>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}

            <div className="relative isolate pt-1 pl-1 border-2 border-ink-950 bg-ink-950 text-white p-5 shadow-hard-sm">
              <div className="ribbon mb-3">Newsletter</div>
              <h4 className="font-headline text-xl uppercase leading-tight mb-2">Join MapMyTimes</h4>
              <p className="text-sm text-white/70 mb-4 leading-relaxed">
                Get breaking-news alerts and top stories in your inbox — Journalism of Integrity, delivered daily.
              </p>
              <form onSubmit={(e) => e.preventDefault()} className="flex flex-col gap-2">
                <input
                  type="email"
                  required
                  placeholder="your@email.com"
                  className="h-10 px-3 text-sm bg-white text-ink-950 outline-none border-2 border-white placeholder:text-ink-600 focus:border-news"
                />
                <Button type="submit" variant="news" className="!h-10">Subscribe →</Button>
              </form>
            </div>
          </aside>
          )}
        </div>
        {isReaderMode && (
          <div style={{ maxWidth: 680, margin: "40px auto 0", textAlign: "center" }}>
            <div className="inline-flex items-center gap-2 mb-3">
              <div style={{ width: 3, height: 12, background: readerChrome }} />
              <span
                style={{
                  fontFamily: "'Inter', sans-serif",
                  fontWeight: 900,
                  fontSize: 11,
                  letterSpacing: "0.16em",
                  color: readerFgMuted,
                  textTransform: "uppercase",
                }}
              >
                End of story
              </span>
            </div>
            <p style={{ fontFamily: "'Inter', sans-serif", fontSize: 10.5, fontWeight: 600, color: readerFgMuted }}>
              © {new Date().getFullYear()} MAPMYTOUR LLP, India
            </p>
          </div>
        )}
        </div>
      </article>

      {/* ===================================================================== */}
      {/* TYPOGRAPHY SIDE PANEL (Aa icon) — sticks right, visible when showAaPanel */}
      {/* ===================================================================== */}
      {showAaPanel ? (
        <>
          {/* backdrop */}
          <div
            aria-hidden="true"
            onClick={() => setShowAaPanel(false)}
            className="fixed inset-0 z-[65] bg-black/40 backdrop-blur-[2px]"
          />
          {/* panel */}
          <aside
            className="fixed right-0 top-0 z-[70] h-full w-[360px] max-w-[90vw] shadow-2xl overflow-y-auto"
            style={{
              backgroundColor: readerTheme === "dark" ? "#0A0A0A" : "#ffffff",
              color: readerTheme === "dark" ? "#ffffff" : "#0A0A0A",
              borderLeft: "2px solid #0A0A0A",
              fontFamily: "'Inter', 'Noto Sans Devanagari', sans-serif",
            }}
          >
            <div className="sticky top-0 z-10 flex items-center justify-between px-5 py-4"
              style={{
                backgroundColor: readerTheme === "dark" ? "#0A0A0A" : "#fff",
                borderBottom: `1px solid ${readerTheme === "dark" ? "rgba(255,255,255,0.12)" : "rgba(10,10,10,0.12)"}`,
              }}
            >
              <div className="flex items-center gap-2.5">
                <div className="inline-flex h-9 w-9 items-center justify-center border-2"
                  style={{ borderColor: "#0A0A0A", background: readerChrome }}
                >
                  <TypeIcon className="h-4 w-4" style={{ color: "#fff" }} />
                </div>
                <div>
                  <div className="text-[10px] font-black uppercase tracking-[0.2em] opacity-60">Reader Mode</div>
                  <div className="text-sm font-black uppercase tracking-[0.12em]">Typography</div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowAaPanel(false)}
                aria-label="Close"
                className="inline-flex h-9 w-9 items-center justify-center border-2 transition-colors hover:bg-ink-950/5"
                style={{ borderColor: "#0A0A0A" }}
              >
                <XIcon className="h-4 w-4" />
              </button>
            </div>

            <div className="px-5 py-6 space-y-7">
              {/* --- Font size 5-step --- */}
              <section>
                <div className="text-[10.5px] font-black uppercase tracking-[0.22em] mb-3" style={{ opacity: 0.58 }}>Font size</div>
                <div className="grid grid-cols-5 gap-1.5">
                  {READER_FONT_SIZES.map((px, i) => {
                    const active = readerFontIdx === i;
                    return (
                      <button
                        key={px}
                        type="button"
                        onClick={() => setReaderFontIdx(i)}
                        className="h-11 border-2 flex items-center justify-center transition-colors"
                        style={{
                          borderColor: active ? readerChrome : readerTheme === "dark" ? "rgba(255,255,255,0.28)" : "rgba(10,10,10,0.6)",
                          background: active ? readerChrome : "transparent",
                          color: active ? "#fff" : "inherit",
                        }}
                      >
                        <span style={{ fontSize: 10 + i * 1.2, fontWeight: 900 }}>Aa</span>
                      </button>
                    );
                  })}
                </div>
                <div className="mt-2 flex items-center justify-between text-[10.5px] font-bold uppercase tracking-widest opacity-60">
                  <span>Small</span>
                  <span>{readerFontPx}px</span>
                  <span>X-Large</span>
                </div>
              </section>

              {/* --- Font family stack --- */}
              <section>
                <div className="text-[10.5px] font-black uppercase tracking-[0.22em] mb-3" style={{ opacity: 0.58 }}>Font family</div>
                <div className="space-y-2">
                  {(["sans", "serif"] as ReaderFontStack[]).map((s) => {
                    const active = readerStack === s;
                    const sampleStack = s === "serif"
                      ? "'Noto Serif', 'Noto Serif Devanagari', Georgia, serif"
                      : "'Inter', 'Noto Sans Devanagari', -apple-system, sans-serif";
                    return (
                      <button
                        key={s}
                        type="button"
                        onClick={() => setReaderStack(s)}
                        className="w-full text-left px-3.5 py-3.5 border-2 transition-colors"
                        style={{
                          borderColor: active ? readerChrome : readerTheme === "dark" ? "rgba(255,255,255,0.22)" : "rgba(10,10,10,0.55)",
                          background: active
                            ? (readerTheme === "dark" ? "rgba(227,30,36,0.08)" : "rgba(227,30,36,0.06)")
                            : "transparent",
                        }}
                      >
                        <div className="flex items-center gap-2.5 mb-1.5">
                          {active ? (
                            <CheckIcon className="h-4 w-4" style={{ color: readerChrome, flexShrink: 0 }} />
                          ) : (
                            <CircleIcon className="h-4 w-4" style={{ opacity: 0.35, flexShrink: 0 }} />
                          )}
                          <div className="text-[12px] font-black uppercase tracking-[0.14em]">
                            {READER_STACK_LABEL[s]}
                          </div>
                        </div>
                        <div className="pl-[26px] text-[15px]" style={{ fontFamily: sampleStack, lineHeight: 1.45 }}>
                          The quick brown fox jumps over the lazy dog.<br/>
                          तेज़ लोमड़ी आलसी कूकर के ऊपर से कूदी।
                        </div>
                      </button>
                    );
                  })}
                </div>
              </section>

              {/* --- Line spacing --- */}
              <section>
                <div className="text-[10.5px] font-black uppercase tracking-[0.22em] mb-3" style={{ opacity: 0.58 }}>Line spacing</div>
                <div className="grid grid-cols-3 gap-2">
                  {(["compact", "normal", "relaxed"] as ReaderLineSpacing[]).map((l) => {
                    const active = readerLH === l;
                    return (
                      <button
                        key={l}
                        type="button"
                        onClick={() => setReaderLH(l)}
                        className="h-11 border-2 text-[11px] font-black uppercase tracking-[0.15em] transition-colors"
                        style={{
                          borderColor: active ? readerChrome : readerTheme === "dark" ? "rgba(255,255,255,0.28)" : "rgba(10,10,10,0.6)",
                          background: active ? readerChrome : "transparent",
                          color: active ? "#fff" : "inherit",
                        }}
                      >
                        {READER_LH_LABEL[l]}
                      </button>
                    );
                  })}
                </div>
              </section>

              {/* --- Theme Light/Dark/Sepia --- */}
              <section>
                <div className="text-[10.5px] font-black uppercase tracking-[0.22em] mb-3" style={{ opacity: 0.58 }}>Theme</div>
                <div className="grid grid-cols-3 gap-3">
                  {(["light", "dark", "sepia"] as ReaderTheme[]).map((t) => {
                    const active = readerTheme === t;
                    const bg = READER_BG[t];
                    const fg = READER_FG[t];
                    const label = t === "dark" ? "Dark" : t === "sepia" ? "Sepia" : "Light";
                    return (
                      <button
                        key={t}
                        type="button"
                        onClick={() => setReaderTheme(t)}
                        className="h-[112px] text-left p-3 border-2 transition-all relative overflow-hidden"
                        style={{
                          background: bg,
                          color: fg,
                          borderColor: active ? readerChrome : "#0A0A0A",
                          boxShadow: active ? `0 8px 24px -8px ${readerChrome}66` : undefined,
                        }}
                      >
                        <div className="flex items-start justify-between">
                          {active ? (
                            <CheckIcon className="h-4 w-4" style={{ color: readerChrome, flexShrink: 0 }} />
                          ) : (
                            <RadioIcon className="h-4 w-4" style={{ opacity: 0.35, flexShrink: 0 }} />
                          )}
                          <div className="h-1.5 w-6" style={{ background: readerChrome }} />
                        </div>
                        <div className="mt-6 space-y-2">
                          <div className="text-[12px] font-black uppercase tracking-[0.15em]">{label}</div>
                          <div style={{ height: 3, background: fg, opacity: 0.7, width: "100%" }} />
                          <div style={{ height: 3, background: fg, opacity: 0.45, width: "72%" }} />
                        </div>
                      </button>
                    );
                  })}
                </div>
              </section>

              {/* Reader Mode quick toggle at bottom */}
              <button
                type="button"
                onClick={() => { setIsReaderMode(true); setShowAaPanel(false); }}
                className="w-full h-12 text-white font-black uppercase tracking-[0.18em] text-xs transition-colors flex items-center justify-center gap-2 border-2"
                style={{
                  background: readerChrome,
                  borderColor: "#0A0A0A",
                  boxShadow: "4px 4px 0 0 #0A0A0A",
                }}
              >
                <BookOpen className="h-4 w-4" />
                {isReaderMode ? "Reader Mode — Active" : "Enter Reader Mode"}
              </button>
            </div>
          </aside>
        </>
      ) : null}

      {/* ===================================================================== */}
      {/* AUTO-SUGGEST READER MODE CARD (bottom of viewport for long articles >= 800w) */}
      {/* ===================================================================== */}
      {showReaderSuggest && !isReaderMode ? (
        <div className="fixed bottom-6 left-4 right-4 sm:left-1/2 sm:-translate-x-1/2 sm:max-w-lg z-[75]">
          <div
            className="border-2 border-ink-950 bg-white px-4 py-3.5 shadow-hard-sm flex items-center gap-3"
            style={{ boxShadow: "4px 4px 0 0 #0A0A0A" }}
          >
            <div className="flex-shrink-0 inline-flex h-10 w-10 items-center justify-center border-2 border-ink-950" style={{ background: "#E31E24" }}>
              <BookOpen className="h-4 w-4 text-white" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-[11px] font-black uppercase tracking-[0.18em] text-ink-950 leading-none">
                Read distraction-free
              </div>
              <div className="text-[11.5px] text-ink-700 mt-1 leading-snug">
                Switch to Reader Mode for clean typography & custom fonts.
              </div>
            </div>
            <div className="flex items-center gap-1.5 flex-shrink-0">
              <button
                type="button"
                onClick={() => dismissReaderSuggest(true)}
                className="h-9 px-3 text-[11px] font-black uppercase tracking-[0.18em] text-white border-2 border-ink-950"
                style={{ background: "#E31E24" }}
              >
                Yes
              </button>
              <button
                type="button"
                onClick={() => dismissReaderSuggest(false)}
                aria-label="Dismiss Reader Mode suggestion"
                className="h-9 w-9 flex items-center justify-center border-2 border-ink-950 bg-white text-ink-950 hover:bg-ink-950 hover:text-white transition-colors"
              >
                <XIcon className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {resumeBannerVisible && resumePercent != null ? (
        <div className="fixed top-20 right-4 sm:top-24 sm:right-8 z-[60] sm:max-w-lg">
          <div className="rounded border-2 border-ink-950 bg-white text-ink-950 shadow-hard-sm p-4 sm:p-5">
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 inline-flex h-9 w-9 items-center justify-center border-2 border-ink-950" style={{ background: "#E31E24" }}>
                <BookOpen className="h-4 w-4 text-white" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="text-[11px] font-black uppercase tracking-[0.18em] text-news leading-none mb-1">
                  Continue reading
                </div>
                <div className="text-sm text-ink-800 leading-snug mb-3">
                  Continue from <strong className="text-ink-950">{resumePercent}%</strong>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setResumeBannerVisible(false);
                      const el = document.documentElement;
                      const target = (el.scrollHeight * (resumePercent ?? 0)) / 100;
                      window.scrollTo({ top: target, behavior: "instant" });
                    }}
                    className="h-9 px-3 text-[11px] font-black uppercase tracking-[0.18em] text-white border-2 border-ink-950"
                    style={{ background: "#E31E24" }}
                  >
                    Resume
                  </button>
                  <button
                    type="button"
                    onClick={() => setResumeBannerVisible(false)}
                    className="h-9 px-3 text-[11px] font-black uppercase tracking-[0.18em] text-ink-950 border-2 border-ink-950 bg-white hover:bg-ink-950 hover:text-white transition-colors"
                  >
                    Dismiss
                  </button>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setResumeBannerVisible(false)}
                aria-label="Dismiss resume banner"
                className="flex-shrink-0 h-8 w-8 flex items-center justify-center border border-ink-950/30 text-ink-700 hover:bg-ink-950 hover:text-white transition-colors"
              >
                <XIcon className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {/* ===================================================================== */}
      {/* FLOATING HIGHLIGHT TOOLBAR (appears above selection in reader mode) */}
      {/* ===================================================================== */}
      {isReaderMode && hlSelection
        ? (() => {
            const rect = hlSelection.rect;
            const scrollY = typeof window !== "undefined" ? window.scrollY : 0;
            const scrollX = typeof window !== "undefined" ? window.scrollX : 0;
            const vw = typeof window !== "undefined" ? window.innerWidth : 1024;
            let top = rect.top + scrollY - 52;
            const left = Math.max(
              8 + scrollX,
              Math.min(
                (vw - 250) + scrollX,
                rect.left + scrollX + (rect.width / 2) - 125,
              ),
            );
            return (
              <div
                ref={floatToolbarRef}
                className="fixed z-[90] pointer-events-auto"
                style={{ top: `${Math.max(8, top - scrollY)}px`, left: `${left - scrollX}px` }}
              >
                <div
                  className="flex items-center gap-1 rounded-sm border-2 border-ink-950 px-2 py-1.5 shadow-hard-sm bg-white"
                  style={{ boxShadow: "3px 3px 0 0 #0A0A0A" }}
                >
                  <button
                    type="button"
                    onClick={doCreateHighlight}
                    disabled={hlBusy}
                    className="inline-flex items-center gap-1.5 h-8 px-3 text-[11px] font-black uppercase tracking-[0.18em] text-white border-2 border-ink-950 disabled:opacity-60 hover:opacity-90 transition-opacity"
                    style={{ background: "#E31E24" }}
                  >
                    {hlBusy
                      ? <CircleIcon className="h-3.5 w-3.5 animate-spin" />
                      : <Highlighter className="h-3.5 w-3.5" />}
                    Highlight
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setHlSelection(null);
                      window.getSelection?.()?.removeAllRanges();
                    }}
                    aria-label="Cancel highlight"
                    className="h-8 w-8 inline-flex items-center justify-center border-2 border-ink-950 bg-white text-ink-950 hover:bg-ink-950 hover:text-white transition-colors"
                  >
                    <XIcon className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            );
          })()
        : null}

      {/* ===================================================================== */}
      {/* HIGHLIGHT DELETE POPOVER (on click of existing highlight) */}
      {/* ===================================================================== */}
      {deletePopover
        ? (() => {
            const popW = 260;
            const vw = typeof window !== "undefined" ? window.innerWidth : 1024;
            const vh = typeof window !== "undefined" ? window.innerHeight : 768;
            const left = Math.max(12, Math.min(vw - popW - 12, deletePopover.x + 12));
            const top = Math.max(60, Math.min(vh - 120, deletePopover.y + 14));
            const excerpt =
              (deletePopover.hl.excerpt ?? "").length > 120
                ? (deletePopover.hl.excerpt ?? "").slice(0, 120) + "…"
                : (deletePopover.hl.excerpt ?? "");
            return (
              <div
                ref={deletePopoverRef}
                className="fixed z-[95]"
                style={{ left: `${left}px`, top: `${top}px` }}
              >
                <div
                  className="w-[260px] border-2 border-ink-950 bg-white shadow-hard-sm p-3"
                  style={{ boxShadow: "3px 3px 0 0 #0A0A0A" }}
                >
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div className="inline-flex items-center gap-1.5 text-[10px] font-black uppercase tracking-[0.2em] text-news">
                      <Highlighter className="h-3.5 w-3.5" /> Highlight
                    </div>
                    <button
                      type="button"
                      onClick={() => setDeletePopover(null)}
                      aria-label="Close"
                      className="h-6 w-6 inline-flex items-center justify-center border border-ink-950/30 text-ink-700 hover:bg-ink-950 hover:text-white transition-colors"
                    >
                      <XIcon className="h-3 w-3" />
                    </button>
                  </div>
                  {excerpt ? (
                    <p
                      className="text-xs text-ink-800 leading-snug mb-3 p-2 rounded-sm"
                      style={{ background: "rgba(227, 30, 36, 0.10)" }}
                    >
                      {excerpt}
                    </p>
                  ) : null}
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => doDeleteHighlight(deletePopover.hl)}
                      disabled={hlDeleting === deletePopover.hl.id}
                      className="flex-1 inline-flex items-center justify-center gap-1.5 h-8 px-2 text-[11px] font-black uppercase tracking-[0.18em] text-white border-2 border-ink-950 disabled:opacity-60"
                      style={{ background: "#0A0A0A" }}
                    >
                      {hlDeleting === deletePopover.hl.id
                        ? <CircleIcon className="h-3.5 w-3.5 animate-spin" />
                        : <Trash2 className="h-3.5 w-3.5" />}
                      Remove
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeletePopover(null)}
                      className="h-8 px-2 text-[11px] font-black uppercase tracking-[0.18em] text-ink-950 border-2 border-ink-950 bg-white hover:bg-ink-950 hover:text-white transition-colors"
                    >
                      Keep
                    </button>
                  </div>
                </div>
              </div>
            );
          })()
        : null}
    </>
  );
}

function ActionIcon({
  icon,
  label,
  onClick,
  href,
  count,
  active,
  disabled,
}: {
  icon: React.ReactNode;
  label: string;
  onClick?: () => void;
  href?: string;
  count?: number;
  active?: boolean;
  disabled?: boolean;
}) {
  const cls = cn(
    "h-10 px-2.5 inline-flex items-center gap-1.5 border-2 border-ink-950 text-sm font-bold uppercase tracking-wider transition-colors disabled:opacity-50 no-underline shrink-0",
    active ? "bg-news text-white border-news" : "bg-white hover:bg-ink-950 hover:text-white text-ink-950",
  );
  const body = (
    <>
      {icon}
      {typeof count === "number" && count > 0 ? (
        <span className="tabular-nums">{count.toLocaleString("en-IN")}</span>
      ) : null}
    </>
  );
  if (href) {
    return (
      <a href={href} className={cls} title={label} aria-label={label}>
        {body}
      </a>
    );
  }
  return (
    <button
      type="button"
      onClick={onClick}
      title={label}
      aria-label={label}
      disabled={disabled}
      className={cls}
    >
      {body}
    </button>
  );
}

function AuthorCard({ post }: { post: BlogPostResponse }) {
  const authorUrl = post.userId ? `/author/${encodeURIComponent(post.userId)}` : null;
  const name = `${post.authorFirstName ?? ""} ${post.authorLastName ?? ""}`.trim() || "MapMyTimes Newsroom";
  const bio = (post as any)?.authorBio ?? (post as any)?.author?.bio;
  const avatar = avatarOrDefault((post as any)?.authorAvatar ?? (post as any)?.author?.avatarUrl);
  return (
    <section className="border-2 border-ink-950 bg-white p-4 sm:p-5 shadow-hard-sm">
      <div className="flex gap-4 items-start">
        <Link href={authorUrl ?? "#"} className="flex-shrink-0">
          <img
            src={avatar}
            alt={name}
            className="h-14 w-14 sm:h-16 sm:w-16 rounded-full border-2 border-ink-950 object-cover bg-news"
            onError={(e) => { e.currentTarget.style.display = "none"; }}
          />
        </Link>
        <div className="min-w-0 flex-1">
          <div className="text-[10px] font-bold uppercase tracking-widest text-ink-600">Written by</div>
          <Link href={authorUrl ?? "#"} className="font-headline text-lg sm:text-xl uppercase leading-none hover:text-news">
            {name}
          </Link>
          {bio ? (
            <p className="mt-2 text-sm text-ink-800 leading-relaxed">{bio}</p>
          ) : (
            <p className="mt-2 text-sm text-ink-700 leading-relaxed italic">
              Journalist at MapMyTimes. Independent reporting, verified facts — Journalism of Integrity.
            </p>
          )}
          <div className="mt-3 flex items-center gap-2">
            <Link href={authorUrl ?? "#"}>
              <Button variant="outline" size="sm">More stories →</Button>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

type TranslateItem = { id: string; text: string };

function gatherTranslateItems(post: BlogPostResponse | null, headings: HeadingInfo[]): TranslateItem[] {
  const items: TranslateItem[] = [];
  if (!post) return items;

  if (post.title) items.push({ id: "title", text: post.title });
  if (post.excerpt) items.push({ id: "excerpt", text: post.excerpt });
  const anyLede = (post as any)?.lede ?? (post as any)?.openingLede ?? null;
  if (anyLede && typeof anyLede === "string" && anyLede.trim()) {
    items.push({ id: "lede", text: anyLede });
  } else if (post.excerpt) {
    items.push({ id: "lede", text: post.excerpt });
  }

  headings.forEach((h, i) => {
    if (h.text) items.push({ id: `toc-${i}`, text: h.text });
  });

  const blocks = (post as any)?.contentBlocks ?? (post as any)?.blocks;
  if (Array.isArray(blocks) && blocks.length > 0) {
    blocks.forEach((b, i) => {
      const t = String(b?.type ?? "paragraph").toLowerCase();
      const d = b?.data ?? b;
      const html = String(d?.text ?? d?.content ?? d?.html ?? d?.value ?? "");

      if (/heading|^h[1-6]$|subhead/i.test(t)) {
        const txt = html || d?.heading || d?.title || "";
        if (txt) items.push({ id: `h-${i}`, text: txt });
      } else if (/image|media|picture|bullet|section|gallery|photo|card|tile/i.test(t)) {
        const caption = String(
          d?.caption ?? d?.name ?? d?.title ?? d?.heading ?? d?.headline ?? d?.label ?? d?.sectionName ?? ""
        ).trim();
        if (caption) items.push({ id: `cap-${i}`, text: caption });
        const subtitle = String(d?.subtitle ?? d?.subheading ?? d?.subhead ?? d?.kicker ?? "").trim();
        if (subtitle) items.push({ id: `sub-${i}-0`, text: subtitle });
        const description = String(
          d?.description ?? d?.body ?? d?.text ?? d?.content ?? d?.paragraph ?? d?.summary ?? d?.details ?? ""
        ).trim();
        if (description) {
          const paras = description.replace(/\r\n?/g, "\n").split(/\n{2,}/).map(s => s.trim()).filter(Boolean);
          paras.forEach((p, j) => {
            if (p) items.push({ id: `p-${i}-${j}`, text: p });
          });
        }
        const bulletList: string[] = [];
        if (Array.isArray(d?.bulletItems)) {
          d.bulletItems.forEach((bi: any) => {
            const tx = String(bi?.text ?? bi ?? "").trim();
            if (tx) bulletList.push(tx);
          });
        }
        if (bulletList.length === 0 && Array.isArray(d?.bullets)) {
          d.bullets.forEach((b: any) => {
            const tx = String(b ?? "").trim();
            if (tx) bulletList.push(tx);
          });
        }
        bulletList.forEach((bl, j) => {
          items.push({ id: `bul-${i}-${j}`, text: bl });
        });
      } else if (t === "list" || t === "unordered-list" || t === "ordered-list") {
        const listItems = Array.isArray(d?.items) ? d.items : [];
        listItems.forEach((it: any, j: number) => {
          const tx = typeof it === "string" ? it : it?.content ?? it?.text ?? "";
          if (tx) items.push({ id: `bul-${i}-${j}`, text: tx });
        });
      } else if (/quote|pullquote|blockquote/i.test(t)) {
        const quote = html || d?.text || d?.caption || d?.quote || "";
        if (quote) items.push({ id: `p-${i}`, text: quote });
      } else if (html) {
        items.push({ id: `p-${i}`, text: html });
      }
    });
  }

  if (typeof post.content === "string" && post.content.trim()) {
    const rawContent = post.content;
    if (rawContent.trim().startsWith("<") && /<\/[a-z]+>/i.test(rawContent)) {
      let hc = 0;
      rawContent.replace(/<h([2-4])(\s+[^>]*)?>([\s\S]*?)<\/h\1>/gi, (_m, _lvl, _attrs, inner) => {
        const idx = hc++;
        const txt = String(inner || "").replace(/<[^>]+>/g, "").trim();
        if (txt) items.push({ id: `htmlh-${idx}`, text: txt });
        return "";
      });
      let pc = 0;
      rawContent.replace(/<p(\s+[^>]*)?>([\s\S]*?)<\/p>/gi, (_m, _a, inner) => {
        const idx = pc++;
        const txt = String(inner || "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
        if (txt) items.push({ id: `htmlp-${idx}`, text: txt });
        return "";
      });
      let bc = 0;
      rawContent.replace(/<blockquote(\s+[^>]*)?>([\s\S]*?)<\/blockquote>/gi, (_m, _a, inner) => {
        const idx = bc++;
        const txt = String(inner || "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
        if (txt) items.push({ id: `htmlbq-${idx}`, text: txt });
        return "";
      });
      let listCur = 0;
      rawContent.replace(/<(ul|ol)(\s+[^>]*)?>([\s\S]*?)<\/\1>/gi, (_m, _kind, _a, inside) => {
        const outerIdx = listCur++;
        let liIdx = 0;
        String(inside || "").replace(/<li(\s+[^>]*)?>([\s\S]*?)<\/li>/gi, (_mm: string, _la: string, liInner: string) => {
          const idx = liIdx++;
          const txt = String(liInner || "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
          if (txt) items.push({ id: `htmlli-${outerIdx}-${idx}`, text: txt });
          return "";
        });
        return "";
      });
    } else {
      const raw = String(rawContent || "")
        .replace(/\r\n?/g, "\n")
        .replace(/[ \t]+/g, " ")
        .trim();
      if (raw) {
        const headingRe = /^(#{1,4}\s+|[0-9]{1,2}\.\s+)?([A-Z0-9][A-Za-z0-9 \-:'’”"/,&()]{2,90})([:])?$/;
        const ulRe = /^\s*[-*•]\s+(.+)$/;
        const olRe = /^\s*\d+[.)]\s+(.+)$/;
        type Chunk = { kind: "p" | "h" | "ul" | "ol"; items?: string[]; text?: string };
        const chunks: Chunk[] = [];
        const groups = raw.split(/\n{2,}/);
        let pendingList: { kind: "ul" | "ol"; items: string[] } | null = null;
        function flushList() {
          if (pendingList) { chunks.push({ ...pendingList }); pendingList = null; }
        }
        for (const group of groups) {
          const lines = group.split("\n").map(l => l.trim()).filter(Boolean);
          if (lines.length === 0) continue;
          if (lines.every(l => ulRe.test(l)) || lines.every(l => olRe.test(l))) {
            flushList();
            const kind = ulRe.test(lines[0]) ? "ul" : "ol";
            chunks.push({
              kind,
              items: lines.map(l => (kind === "ul" ? l.match(ulRe)?.[1] : l.match(olRe)?.[1]) ?? l).filter(Boolean) as string[],
            });
            continue;
          }
          for (const line of lines) {
            if (ulRe.test(line) || olRe.test(line)) {
              const kind = ulRe.test(line) ? "ul" : "ol";
              const it = (kind === "ul" ? line.match(ulRe)?.[1] : line.match(olRe)?.[1]) ?? line;
              if (!pendingList || pendingList.kind !== kind) { flushList(); pendingList = { kind, items: [] }; }
              pendingList.items.push(it); continue;
            }
            flushList();
            const isShort = line.length <= 100;
            const noEndPunct = !/[.?!…"]$/.test(line);
            const allCaps = line === line.toUpperCase() && line.replace(/[^A-Za-z]/g, "").length >= 3;
            const m = headingRe.exec(line);
            if (m || (isShort && noEndPunct && (allCaps || /^Table of contents|^Introduction$|^Conclusion$|^Why|^How|^Best|^Top|^Famous|^Local|^Budget/i.test(line)))) {
              chunks.push({ kind: "h", text: m?.[2] ?? line }); continue;
            }
            const last = chunks[chunks.length - 1];
            if (last && last.kind === "p" && last.text && last.text.length + line.length < 1200) {
              last.text = `${last.text}\n${line}`;
            } else {
              chunks.push({ kind: "p", text: line });
            }
          }
        }
        flushList();
        chunks.forEach((c, i) => {
          if (c.kind === "h") {
            const t = String(c.text || "").trim();
            if (t) items.push({ id: `htmlh-${i}`, text: t });
          } else if (c.kind === "p") {
            const t = String(c.text || "").trim();
            if (t) items.push({ id: `htmlp-${i}`, text: t });
          } else if (c.kind === "ul") {
            (c.items ?? []).forEach((it, j) => {
              const t = String(it || "").trim();
              if (t) items.push({ id: `htmlul-${i}-${j}`, text: t });
            });
          } else if (c.kind === "ol") {
            (c.items ?? []).forEach((it, j) => {
              const t = String(it || "").trim();
              if (t) items.push({ id: `htmlol-${i}-${j}`, text: t });
            });
          }
        });
      }
    }
  }

  const media = (post as any)?.media ?? [];
  if (Array.isArray(media)) {
    media.forEach((m, i) => {
      if (m?.caption && typeof m.caption === "string") {
        items.push({ id: `mcap-${i}`, text: m.caption });
      }
      if (m?.description && typeof m.description === "string") {
        items.push({ id: `mdesc-${i}`, text: m.description });
      }
    });
  }

  const unique = new Map<string, TranslateItem>();
  items.forEach((it) => {
    if (!unique.has(it.id) && it.text && it.text.trim().length > 0) {
      unique.set(it.id, it);
    }
  });
  return Array.from(unique.values());
}

type HeadingInfo = { id: string; text: string; level: number };

function slugify(s: string) {
  return s
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\u0900-\u097F\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-");
}

function extractHeadings(post: BlogPostResponse | null): HeadingInfo[] {
  if (!post) return [];
  const out: HeadingInfo[] = [];
  const seen = new Map<string, number>();

  function push(text: string, level: number) {
    const t = String(text || "").trim().replace(/<[^>]+>/g, "");
    if (!t) return;
    let base = slugify(t) || "section";
    const n = seen.get(base) ?? 0;
    seen.set(base, n + 1);
    const id = n > 0 ? `${base}-${n}` : base;
    out.push({ id, text: t.slice(0, 120), level: Math.min(6, Math.max(2, level)) });
  }

  const blocks = (post as any)?.contentBlocks ?? (post as any)?.blocks;
  if (Array.isArray(blocks) && blocks.length > 0) {
    for (const b of blocks) {
      const type = String(b?.type ?? "");
      const d = b?.data ?? b;
      if (/heading/i.test(type) || /h[1-6]/i.test(type)) {
        const lvl = Number(String(type).replace(/\D/g, "")) || (d?.level ?? 2);
        push(d?.text ?? d?.content ?? d?.heading ?? "", Number(lvl) || 2);
      } else if (
        /image|media|picture|bullet|section|gallery|photo|card/i.test(type)
      ) {
        const heading =
          d?.title ?? d?.caption ?? d?.name ?? d?.heading ?? d?.headline;
        if (heading) push(String(heading), 2);
      }
    }
  }

  if (out.length === 0 && typeof post.content === "string" && post.content) {
    const re = /<h([1-6])(?:\s+[^>]*)?>([\s\S]*?)<\/h\1>/gi;
    let m: RegExpExecArray | null;
    while ((m = re.exec(post.content)) !== null) {
      const lvl = Number(m[1]) || 2;
      const txt = m[2] || "";
      if (txt) push(txt, lvl);
    }
    if (out.length === 0) {
      const paras = post.content.split(/\n{2,}|\n(?=[A-Z][A-Za-z0-9 &:'’”\-/,()]{3,80}(\n|$))/g).slice(0, 12);
      for (const p of paras) {
        const t = p.replace(/<[^>]+>/g, "").trim();
        if (t.length >= 3 && t.length <= 90 && /^[A-Z0-9"“'(]/.test(t) && !/[.?!]\s*$/.test(t) && t.split(" ").length <= 14) {
          push(t, 2);
        }
        if (out.length >= 10) break;
      }
    }
  }

  if (out.length === 0) {
    const media = (post as any)?.media ?? [];
    if (Array.isArray(media)) {
      for (const m of media) {
        const n = m?.name ?? m?.caption ?? m?.alt;
        if (n && typeof n === "string") push(n, 2);
        if (out.length >= 10) break;
      }
    }
  }

  return out.slice(0, 20);
}

function ArticleBody({
  content,
  blocks,
  ogImage,
  media,
  excerpt,
  lede,
  primaryVideo,
  translations,
  translateMode,
}: {
  content: string;
  blocks: any[] | null;
  ogImage?: string | null;
  media?: any[];
  excerpt?: string | null;
  lede?: string | null;
  primaryVideo?: import("@/lib/video").ParsedVideo | null;
  translations?: Record<string, string> | null;
  translateMode: "native" | "translated";
}) {
  const useTr = translateMode === "translated" && translations;
  const tr = (id: string, fallback: string) => useTr ? (translations?.[id] ?? fallback) : fallback;
  const hasBlocks = Array.isArray(blocks) && blocks.length > 0;
  const hasMedia = Array.isArray(media) && media.length > 0;

  const openingLede = useMemo<string | null>(() => {
    const candidates: string[] = [];
    if (lede) candidates.push(String(lede).trim());
    if (hasBlocks) {
      for (const raw of blocks) {
        const d = raw?.data ?? raw;
        const t = String(raw?.type ?? "").toLowerCase();
        if (/image|media|picture|bullet|section|gallery|photo|card|tile/i.test(t)) {
          const desc = String(d?.description ?? d?.body ?? d?.text ?? d?.content ?? "").trim();
          if (desc && desc.length >= 80) {
            const paras = desc.replace(/\r\n?/g, "\n").split(/\n{2,}/).map(s => s.trim()).filter(Boolean);
            const strong = paras.find(p => p.length >= 100);
            if (strong) candidates.push(strong);
            else if (paras.length) candidates.push(paras[0]);
            break;
          }
        }
      }
    }
    if (!hasBlocks && hasMedia) {
      for (const m of media ?? []) {
        const desc = String(m?.description ?? m?.subtitle ?? m?.body ?? m?.text ?? m?.content ?? "").trim();
        if (desc && desc.length >= 80) {
          const paras = desc.replace(/\r\n?/g, "\n").split(/\n{2,}/).map(s => s.trim()).filter(Boolean);
          const strong = paras.find(p => p.length >= 100);
          if (strong) { candidates.push(strong); break; }
          if (paras.length) { candidates.push(paras[0]); break; }
        }
      }
    }
    if (excerpt) candidates.push(String(excerpt).trim());
    if (typeof content === "string" && content.trim()) {
      const plain = content.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
      if (plain.length >= 60) candidates.push(plain);
    }
    const picked = candidates.find(c => c.length >= 80) ?? candidates[0] ?? null;
    if (!picked) return null;
    if (/<[a-z]/i.test(picked)) return picked;
    const sentences = picked.match(/[^.!?]+[.!?]+(\s|$)/g) ?? [picked];
    const merged = sentences.slice(0, Math.min(4, sentences.length)).map(s => s.trim()).join(" ");
    const min = Math.min(picked.length, Math.max(300, merged.length));
    return picked.length > min ? picked.slice(0, min).replace(/\s+\S*$/, "").trim() + "…" : picked;
  }, [lede, excerpt, content, blocks, media, hasBlocks, hasMedia]);

  const DropCapLede = (() => {
    if (!openingLede) return null;
    const trLede = tr("lede", openingLede);
    const plain = trLede.replace(/<[^>]+>/g, "");
    const firstChar = plain.slice(0, 1) || "";
    const rest = plain.slice(1);
    const asHtml = /<[a-z]/i.test(trLede);
    if (!firstChar) return null;
    if (asHtml) {
      return (
        <p
          className="article-lede mb-7 sm:mb-9 text-base sm:text-lg md:text-xl text-ink-800 leading-[1.85] md:leading-[2.05] font-serif overflow-hidden [&_a]:text-news [&_a]:underline
            first-letter:font-headline first-letter:text-news first-letter:font-black first-letter:float-left first-letter:leading-[0.85] first-letter:line-height-none
            first-letter:text-[2.6rem] sm:first-letter:text-[3.2rem] md:first-letter:text-[3.7rem] first-letter:mr-2 sm:first-letter:mr-2.5 first-letter:mt-1.5 sm:first-letter:mt-1 first-letter:tracking-tighter first-letter:clear-both"
          dangerouslySetInnerHTML={{ __html: trLede }}
        />
      );
    }
    return (
      <p className="article-lede mb-7 sm:mb-9 text-base sm:text-lg md:text-xl text-ink-800 leading-[1.85] md:leading-[2.05] overflow-hidden">
        <span
          className="font-headline text-news font-black float-left leading-[0.85] line-height-none text-[2.6rem] sm:text-[3.2rem] md:text-[3.7rem] mr-2 sm:mr-2.5 mt-1.5 sm:mt-1 tracking-tighter select-none clear-both"
          aria-hidden="true"
        >
          {firstChar}
        </span>
        <span className="inline align-baseline">{rest}</span>
      </p>
    );
  })();

  const idTables = useMemo(() => {
    // Pure, order-based id assignment — idempotent across StrictMode double-render.
    const h2Ids = new Map<string, string>(); // key: normalized text → final id
    const subIds = new Map<string, string>();

    function walk(items: { type?: any; data?: any }[], scopeH2: Map<string, number>, scopeSub: Map<string, number>) {
      for (const raw of items) {
        const t = String(raw?.type ?? "paragraph").toLowerCase();
        const d = raw?.data ?? raw;
        const html = String(d?.text ?? d?.content ?? d?.html ?? d?.value ?? "");
        if (/heading|^h[1-6]$|subhead/i.test(t)) {
          const lvl = Number(String(t).replace(/\D/g, "")) || d?.level || 2;
          const txt = String(html || d?.heading || d?.title || "").trim();
          if (!txt) continue;
          const base = slugify(txt) || (lvl <= 2 ? "section" : "subheading");
          const scope = lvl <= 2 ? scopeH2 : scopeSub;
          const n = scope.get(base) ?? 0;
          scope.set(base, n + 1);
          const id = n > 0 ? `${base}-${n}` : base;
          (lvl <= 2 ? h2Ids : subIds).set(txt.toLowerCase(), id);
        } else if (/image|media|picture|bullet|section|gallery|photo|card|tile/i.test(t)) {
          const caption = String(
            d?.title ?? d?.caption ?? d?.name ?? d?.heading ?? d?.headline ?? d?.label ?? d?.sectionName ?? ""
          ).trim();
          if (caption) {
            const base = slugify(caption);
            if (base) {
              const n = scopeH2.get(base) ?? 0;
              scopeH2.set(base, n + 1);
              const id = n > 0 ? `${base}-${n}` : base;
              h2Ids.set(caption.toLowerCase(), id);
            }
          }
        }
      }
    }
    const scopeH2 = new Map<string, number>();
    const scopeSub = new Map<string, number>();
    if (Array.isArray(blocks) && blocks.length > 0) walk(blocks, scopeH2, scopeSub);
    else if (Array.isArray(media) && media.length > 0) walk(media.map((m) => ({ type: "media", data: m })), scopeH2, scopeSub);

    // Fallback lookup: deterministic idempotent generation that works per-call without shared mutable counter.
    // Build ordered list to compute duplicates correctly.
    const orderedH2: string[] = [];
    for (const [k, v] of scopeH2) {
      for (let i = 0; i < v; i++) orderedH2.push(k);
    }
    const orderedSub: string[] = [];
    for (const [k, v] of scopeSub) {
      for (let i = 0; i < v; i++) orderedSub.push(k);
    }
    const h2Counter = new Map<string, number>();
    const subCounter = new Map<string, number>();
    return {
      getH2: (t: string, fb: string) => {
        const base = slugify(String(t || "")) || fb || "section";
        const key = String(t || "").trim().toLowerCase();
        const hit = h2Ids.get(key);
        if (hit) return hit;
        // Fallback: scopeH2-based order for this base
        const n = scopeH2.get(base) ?? 0;
        if (n > 0) {
          // Use h2Counter for deterministic suffix for unregistered duplicates
          const local = h2Counter.get(base) ?? 0;
          h2Counter.set(base, local + 1);
          return local > 0 ? `${base}-${local}` : base;
        }
        // Not in scope at all → plain base (will be unique)
        return base;
      },
      getSub: (t: string, fb: string) => {
        const base = slugify(String(t || "")) || fb || "subheading";
        const key = String(t || "").trim().toLowerCase();
        const hit = subIds.get(key);
        if (hit) return hit;
        const n = scopeSub.get(base) ?? 0;
        if (n > 0) {
          const local = subCounter.get(base) ?? 0;
          subCounter.set(base, local + 1);
          return local > 0 ? `${base}-${local}` : base;
        }
        return base;
      },
    };
  }, [content, blocks, media]);

  const mediaByUrl = useMemo(() => {
    const m = new Map<string, any>();
    (media ?? []).forEach((item) => {
      const u = item?.url;
      if (u) m.set(String(u), item);
    });
    return m;
  }, [media]);

  function getH2Id(t: string, fallback: string) {
    return idTables.getH2(t, fallback);
  }
  function getSubId(t: string, fallback: string) {
    return idTables.getSub(t, fallback);
  }

  function renderBlock(b: any, i: number, ledeMatch?: string | null): React.ReactNode {
    const t = String(b?.type ?? "paragraph").toLowerCase();
    const d = b?.data ?? b;
    const html = String(d?.text ?? d?.content ?? d?.html ?? d?.value ?? "");

    const hasImgUrl = Boolean(d?.url ?? d?.file?.url ?? d?.src ?? d?.image?.url ?? d?.imgUrl);
    const anyUrl = String(d?.url ?? d?.videoUrl ?? d?.src ?? d?.href ?? d?.mediaUrl ?? "");
    const rawType = String(d?.type ?? d?.mediaType ?? "").toUpperCase();
    const isVideoLike =
      t === "video" ||
      rawType === "VIDEO" ||
      /reel|youtube|instagram|vimeo|youtu\.be|shorts|embed/i.test(String(t) + " " + String(anyUrl)) ||
      (anyUrl && Boolean(parseVideoUrl(anyUrl)));

    if (isVideoLike && anyUrl) {
      const parsed = parseVideoUrl(anyUrl);
      if (parsed) {
        const caption = String(
          d?.caption ?? d?.title ?? d?.name ?? d?.heading ?? d?.description ?? "",
        ).trim();
        const isCover =
          String(parsed.url) === String(primaryVideo?.url) ||
          /cover/i.test(caption || "");
        if (isCover) return null;
        return (
          <section key={i} className="my-6 sm:my-8">
            <VideoEmbed
              video={parsed}
              aspect={parsed.platform === "youtube-shorts" || parsed.platform === "instagram-reels" ? "9:16" : "16:9"}
              showTitle={caption.length <= 120 ? caption : undefined}
              showCaption={caption.length > 120 ? caption : undefined}
              className="max-w-4xl"
            />
          </section>
        );
      }
    }

    const isMediaLike =
      t === "media" ||
      t === "image" ||
      t === "picture" ||
      /image|media|picture|bullet|gallery|photo|section|card|tile/i.test(t) ||
      (t === "paragraph" && !html && hasImgUrl);

    if (isMediaLike && hasImgUrl) {
      const url = d?.url ?? d?.file?.url ?? d?.src ?? d?.image?.url ?? d?.imgUrl;
      if (!url) return null;
      const matchMedia = mediaByUrl.get(String(url));
      const rawCaption =
        d?.caption ?? d?.name ?? d?.title ?? d?.heading ?? d?.headline ??
        matchMedia?.caption ?? matchMedia?.name ?? matchMedia?.title ??
        d?.label ?? d?.sectionName;
      const isCover =
        /cover/i.test(
          rawCaption ?? d?.alt ?? matchMedia?.alt ?? d?.description ?? ""
        ) || String(url) === String(ogImage);
      if (isCover) return null;
      const caption = typeof rawCaption === "string" ? rawCaption.trim() : "";
      const description = String(
        d?.description ?? matchMedia?.description ??
        d?.body ?? matchMedia?.body ??
        d?.text ?? matchMedia?.text ??
        d?.content ?? matchMedia?.content ??
        d?.paragraph ?? d?.summary ?? d?.details ??
        (Array.isArray(d?.bullets) ? d.bullets.map(String).join("\n\n") : "") ??
        ""
      ).trim();
      const subtitle = String(
        d?.subtitle ?? matchMedia?.subtitle ??
        d?.subheading ?? d?.subhead ?? d?.kicker ?? ""
      ).trim();
      const alt =
        d?.alt ?? matchMedia?.alt ?? caption ??
        d?.altText ?? d?.alternativeText ?? "";

      const isHeading =
        caption &&
        caption.length <= 120 &&
        !/[.?!;]$/.test(caption) &&
        /[A-Za-z]/.test(caption);

      const headingId = isHeading ? getH2Id(caption, `media-${i}`) : null;

      const bodyParagraphs: string[] = [];
      if (subtitle) bodyParagraphs.push(subtitle);
      if (description) {
        description
          .replace(/\r\n?/g, "\n")
          .split(/\n{2,}/)
          .forEach((p) => {
            const t = p.trim();
            if (t) bodyParagraphs.push(t);
          });
      }
      if (ledeMatch && bodyParagraphs.length > 0) {
        const ledeNorm = String(ledeMatch).replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").replace(/[.?!…]+/g, "").toLowerCase();
        const deduped: string[] = [];
        for (const p of bodyParagraphs) {
          const pNorm = p.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").replace(/[.?!…]+/g, "").toLowerCase();
          if (!pNorm) continue;
          const lcs = (a: string, b: string) => {
            const A = a.slice(0, 160);
            const B = b.slice(0, 160);
            const shared: string[] = [];
            let ai = 0;
            for (const token of B.split(/\s+/).filter(Boolean)) {
              const idx = A.indexOf(token, ai);
              if (idx >= 0) { shared.push(token); ai = idx + token.length; }
            }
            return shared.length;
          };
          if (lcs(pNorm, ledeNorm) >= 6 && pNorm.length < ledeNorm.length * 1.4) continue;
          deduped.push(p);
        }
        bodyParagraphs.length = 0;
        bodyParagraphs.push(...deduped);
      }

      const bulletList: string[] = [];
      if (Array.isArray(d?.bulletItems)) {
        for (const bi of d.bulletItems) {
          const t = String(bi?.text ?? bi ?? "").trim();
          if (t) bulletList.push(t);
        }
      }
      if (bulletList.length === 0 && Array.isArray(d?.bullets)) {
        for (const b of d.bullets) {
          const t = String(b ?? "").trim();
          if (t) bulletList.push(t);
        }
      }

      const trCaption = caption ? tr(`cap-${i}`, caption) : "";
      const trSubtitle = subtitle ? tr(`sub-${i}-0`, subtitle) : "";
      const subOffset = subtitle ? 1 : 0;

      return (
        <section key={i} className={cn("my-6 sm:my-8")} id={headingId ?? undefined}>
          {isHeading ? (
            <h2
              className="group relative scroll-mt-24 font-headline uppercase leading-[0.95] mt-10 mb-4 text-2xl sm:text-3xl"
            >
              {trCaption}
              <a
                href={`#${headingId}`}
                className="ml-2 opacity-0 group-hover:opacity-100 transition-opacity text-news"
              >
                #
              </a>
            </h2>
          ) : null}

          <figure className={isHeading ? "mt-0" : ""}>
            <div className="overflow-hidden border-2 border-ink-950 shadow-hard-sm">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={url}
                alt={alt}
                className="w-full h-auto"
                loading="lazy"
                decoding="async"
              />
            </div>
            {caption && !isHeading ? (
              <figcaption className="mt-2 text-xs uppercase tracking-widest text-ink-600 font-semibold">
                {trCaption}
              </figcaption>
            ) : null}
          </figure>

          {bodyParagraphs.length > 0 ? (
            <div className="mt-4 space-y-4">
              {bodyParagraphs.map((p, j) => {
                const isSub = j === 0 && subtitle;
                const pid = isSub ? `sub-${i}-0` : `p-${i}-${j - subOffset}`;
                const tp = tr(pid, p);
                if (tp.startsWith("<") && /<\/[a-z]+>/i.test(tp)) {
                  return (
                    <div
                      key={j}
                      className="text-base sm:text-lg text-ink-800 leading-[1.9] [&_a]:text-news [&_a]:underline"
                      dangerouslySetInnerHTML={{ __html: tp }}
                    />
                  );
                }
                const looksLikeHeading =
                  tp.length <= 100 &&
                  !/[.?!…"]$/.test(tp) &&
                  /^[A-Z0-9"“'(#]/.test(tp);
                if (looksLikeHeading) {
                  const subId = getSubId(tp, `sub-${i}-${j}`);
                  return (
                    <h3
                      key={j}
                      id={subId}
                      className="scroll-mt-24 font-headline uppercase mt-5 mb-2 text-xl sm:text-2xl"
                    >
                      {tp}
                    </h3>
                  );
                }
                return (
                  <p
                    key={j}
                    className="text-base sm:text-lg text-ink-800 leading-[1.9]"
                  >
                    {tp}
                  </p>
                );
              })}
            </div>
          ) : null}

          {bulletList.length > 0 ? (
            <ul className="mt-4 ml-6 list-disc space-y-2 text-base sm:text-lg text-ink-800 leading-[1.75] marker:text-news">
              {bulletList.map((b, j) => {
                const tb = tr(`bul-${i}-${j}`, b);
                return (
                  <li
                    key={j}
                    className={tb.startsWith("<") && /<\/[a-z]+>/i.test(tb) ? "[&_a]:text-news [&_a]:underline" : ""}
                    {...(tb.startsWith("<") && /<\/[a-z]+>/i.test(tb)
                      ? { dangerouslySetInnerHTML: { __html: tb } }
                      : { children: tb })}
                  />
                );
              })}
            </ul>
          ) : null}
        </section>
      );
    }

    if (/heading|^h[1-6]$|subhead/i.test(t)) {
      const lvl = Math.max(2, Math.min(4, Number(String(t).replace(/\D/g, "")) || d?.level || 2));
      const Tag = (`h${lvl}`) as keyof JSX.IntrinsicElements;
      const txt = html || d?.heading || d?.title || "";
      const id = lvl <= 2 ? getH2Id(txt, `h-${i}`) : getSubId(txt, `h-${i}`);
      const trTxt = tr(`h-${i}`, txt);
      return (
        <Tag
          key={i}
          id={id}
          className={cn(
            "group relative scroll-mt-24 font-headline uppercase leading-[0.95] mt-8 mb-3",
            lvl === 2 ? "text-2xl sm:text-3xl" : lvl === 3 ? "text-xl sm:text-2xl" : "text-lg sm:text-xl",
          )}
        >
          {trTxt || "Untitled"}
          <a href={`#${id}`} className="ml-2 opacity-0 group-hover:opacity-100 transition-opacity text-news">#</a>
        </Tag>
      );
    }

    if (t === "list" || t === "unordered-list") {
      const items = Array.isArray(d?.items) ? d.items : [];
      if (items.length === 0) return null;
      return (
        <ul key={i} className="my-5 list-disc pl-6 space-y-1 marker:text-news">
          {items.map((it: any, j: number) => {
            const raw = typeof it === "string" ? it : it?.content ?? it?.text ?? "";
            return (
              <li key={j} className="text-base sm:text-lg leading-relaxed text-ink-800">
                {tr(`bul-${i}-${j}`, raw)}
              </li>
            );
          })}
        </ul>
      );
    }
    if (t === "ordered-list") {
      const items = Array.isArray(d?.items) ? d.items : [];
      if (items.length === 0) return null;
      return (
        <ol key={i} className="my-5 list-decimal pl-6 space-y-1 marker:text-news">
          {items.map((it: any, j: number) => {
            const raw = typeof it === "string" ? it : it?.content ?? it?.text ?? "";
            return (
              <li key={j} className="text-base sm:text-lg leading-relaxed text-ink-800">
                {tr(`bul-${i}-${j}`, raw)}
              </li>
            );
          })}
        </ol>
      );
    }

    if (/quote|pullquote|blockquote/i.test(t)) {
      const raw = html || d?.text || d?.caption || d?.quote || "";
      return (
        <blockquote
          key={i}
          className="my-6 sm:my-8 border-l-4 border-news pl-4 italic text-lg sm:text-xl text-ink-800"
        >
          {tr(`p-${i}`, raw)}
        </blockquote>
      );
    }

    if (/divider|separator|hr/i.test(t)) {
      return <hr key={i} className="my-6 border-ink-950 border-t-2" />;
    }

    if (/video|embed/i.test(t)) {
      const url = d?.url ?? d?.src ?? d?.embedUrl;
      if (!url) return null;
      return (
        <figure key={i} className="my-6 sm:my-8">
          <div className="aspect-video overflow-hidden border-2 border-ink-950 bg-black">
            <iframe src={url} className="w-full h-full" allowFullScreen title="embedded" />
          </div>
        </figure>
      );
    }

    if (!html) return null;

    const trHtml = tr(`p-${i}`, html);
    return (
      <p
        key={i}
        className="text-base sm:text-lg text-ink-800 leading-[1.85] first-letter:font-headline first-letter:text-4xl sm:first-letter:text-5xl first-letter:font-black first-letter:mr-1 first-letter:mt-1 first-letter:float-left first-letter:text-news"
        dangerouslySetInnerHTML={{ __html: trHtml }}
      />
    );
  }

  if (hasBlocks) {
    return (
      <>
        <div className="space-y-6 sm:space-y-7 article-body">
          {DropCapLede}
          {blocks.map((b, i) => renderBlock(b, i, openingLede))}
        </div>
        {typeof content === "string" && content.trim() ? (
          <HtmlOrTextBody content={content} ogImage={ogImage} getId={getH2Id} translations={translations} translateMode={translateMode} />
        ) : null}
      </>
    );
  }

  if (!hasBlocks && hasMedia) {
    const items = (media ?? []).filter((m) => {
      if (!m?.url) return false;
      if (String(m?.url) === String(ogImage)) return false;
      if (/cover/i.test(m?.caption ?? m?.name ?? m?.alt ?? "")) return false;
      return true;
    });
    return (
      <>
        <div className="space-y-6 sm:space-y-7 article-body">
          {DropCapLede}
          {items.map((m, i) =>
            renderBlock({ type: "media", data: { ...m } }, i, openingLede),
          )}
        </div>
        {typeof content === "string" && content.trim() ? (
          <HtmlOrTextBody content={content} ogImage={ogImage} getId={getH2Id} translations={translations} translateMode={translateMode} />
        ) : null}
      </>
    );
  }

  return (
    <>
      {DropCapLede}
      <HtmlOrTextBody content={content} ogImage={ogImage} getId={getH2Id} skipFirstParagraph={!!openingLede} translations={translations} translateMode={translateMode} />
    </>
  );
}

const HINDI_AAM_GLOSSARY: [string, string][] = [
  ["शीर्ष", "टॉप"], ["मुख्य आकर्षण", "हाइलाइट्स"], ["आकर्षण", "हाइलाइट्स"],
  ["अन्वेषण", "देखें"], ["खोज", "सर्च"], ["यात्रा", "टूर"], ["भ्रमण", "टूर"],
  ["प्रसिद्ध", "फेमस"], ["जाना माना", "फेमस"], ["विख्यात", "फेमस"],
  ["स्थानीय", "लोकल"], ["स्थानीय भोजन", "लोकल फूड"],
  ["सुझाव", "टिप्स"], ["यात्रा सुझाव", "ट्रैवल टिप्स"], ["युक्तियाँ", "टिप्स"], ["युक्ति", "टिप"],
  ["बजट विभाजन", "बजट ब्रेकडाउन"], ["विभाजन", "ब्रेकडाउन"],
  ["परिचय", "इंट्रो"], ["निष्कर्ष", "कन्क्लूजन"], ["सर्वोत्तम", "बेस्ट"],
  ["स्मारक", "मोनुमेंट"], ["किलों", "फोर्ट्स"], ["किला", "फोर्ट"], ["किले", "फोर्ट्स"], ["मंदिर", "टेम्पल"],
  ["समुद्री", "बीच"], ["तट", "बीच"],
  ["राष्ट्रीय उद्यान", "नेशनल पार्क"], ["उद्यान", "पार्क"],
  ["आरक्षण", "बुकिंग"], ["आवास", "होटल"], ["विश्राम गृह", "गेस्टहाउस"],
  ["परिवहन", "ट्रांसपोर्ट"], ["सार्वजनिक परिवहन", "पब्लिक ट्रांसपोर्ट"],
  ["रेलगाड़ी", "ट्रेन"], ["हवाई जहाज", "फ्लाइट"], ["विमान", "फ्लाइट"],
  ["सड़क", "रोड"], ["मार्ग", "रूट"], ["मार्गदर्शक", "गाइड"],
  ["प्रस्तावना", "इंट्रो"], ["अनुशंसा", "रिकमेंडेशन"],
  ["अनुभव", "एक्सपीरियंस"], ["अद्भुत", "अमेजिंग"], ["बहुत सुंदर", "बेहतरीन"],
  ["समय", "टाइम"], ["घूमने का सबसे अच्छा समय", "बेस्ट टाइम टू विज़िट"],
  ["संगमरमर की चमक", "संगमरमर की चमक"], ["संगमरमरी आवरण", "संगमरमर की चमक"], ["संगमरमरी चमक", "संगमरमर की चमक"],
  ["पुरानी दिल्ली की अराजकता", "ओल्ड दिल्ली का हंगामा"], ["पुरानी दिल्ली की अव्यवस्था", "ओल्ड दिल्ली का हंगामा"],
  ["बलुआ पत्थर की चमक", "गुलाबी बलुआ पत्थर की चमक"],
  ["अधिक बार", "कई बार"], ["कई गिनती", "बहुत बार"], ["कम गिनती", "कम बार"], ["गिनती से अधिक बार", "कई सालों से"],
  ["परिणाम देना बंद नहीं करता", "कभी भी निराश नहीं करता"], ["परिणाम देना बंद", "मज़ा लेना बंद"],
  ["अव्यवस्था", "हंगामा"], ["अराजकता", "हंगामा"], ["अव्यवस्थित", "बिखरा हुआ"],
  ["प्रसिद्ध स्थानीय बाज़ार", "फेमस लोकल मार्केट"], ["प्रसिद्ध स्थानीय बाजार", "फेमस लोकल मार्केट"],
  ["स्थानीय व्यंजन", "लोकल फूड"], ["प्रसिद्ध स्थानीय भोजन", "फेमस लोकल फूड"],
  ["कैसे पहुंचें", "हाउ टू रीच"], ["टॉप चीज़ें करने योग्य", "टॉप थिंग्स टू डू"],
  ["करने योग्य शीर्ष चीज़ें", "टॉप थिंग्स टू डू"],
  ["संपर्क", "कॉन्टैक्ट"], ["शिकायत", "कंप्लेंट"],
  ["जानकारी", "इन्फो"], ["विवरण", "डिटेल्स"],
  ["अपडेट", "अपडेट"], ["समाचार", "न्यूज"], ["खबर", "न्यूज"],
  ["सुरक्षित", "सेफ"], ["सावधान", "केयरफुल"],
  ["स्वास्थ्य", "हेल्थ"], ["खाना", "फूड"], ["भोजन", "फूड"],
  ["पानी", "वॉटर"], ["रेस्तरां", "रेस्टोरेंट"], ["धावा", "ढाबा"],
  ["दुकान", "शॉप"], ["बाज़ार", "मार्केट"], ["बाजार", "मार्केट"], ["शहर", "सिटी"],
  ["गाँव", "विलेज"], ["देश", "कंट्री"], ["विदेश", "फॉरेन"],
  ["आयोजन", "इवेंट"], ["कार्यक्रम", "शेड्यूल"],
  ["भविष्यवाणी", "फोरकास्ट"], ["मौसम", "वेदर"], ["तापमान", "टेम्प्रेचर"],
  ["अनुमानित", "अप्रॉक्स"], ["लगभग", "अराउंड"],
  ["कई", "बहुत सारे"],
  ["अधिक", "ज़्यादा"], ["अधिकतर", "ज़्यादातर"], ["अधिकांश", "ज़्यादातर"],
  ["न्यून", "कम"], ["न्यूनतम", "कम से कम"], ["अधिकतम", "ज़्यादा से ज़्यादा"],
  ["किंतु", "पर"], ["यद्यपि", "हालाँकि"],
  ["फलस्वरूप", "इस वजह से"], ["अतः", "तो"], ["परन्तु", "पर"],
  ["शीघ्र", "जल्दी"], ["जल्दी से", "जल्दी जल्दी"],
  ["धीरे", "स्लो"], ["वेग से", "फास्ट"],
  ["कभी कभी", "कभी कभी"], ["कभी नहीं", "कभी नहीं"], ["प्रायः", "अक्सर"], ["अधिकांशतः", "ज़्यादातर"],
  ["पहले", "पहले"], ["बाद", "बाद"], ["साथ ही", "साथ ही"], ["इसके अलावा", "इसके अलावा"],
  ["देखा", "देखा"], ["किया", "किया"], ["चलाया", "किया"], ["किया गया", "हो गया"], ["बनाया", "बनाया"],
  ["अच्छा", "बढ़िया"], ["बहुत अच्छा", "बढ़िया"], ["सुंदर", "प्यारा"],
  ["निःशुल्क", "फ्री"], ["मुफ्त", "फ्री"], ["शुल्क", "फीस"],
  ["कीमत", "प्राइस"], ["मूल्य", "प्राइस"], ["दाम", "रेट"],
  ["निश्चित", "फिक्स्ड"], ["अनिश्चित", "टेंटेटिव"],
  ["विचार", "थॉट"], ["राय", "अपिनियन"], ["मत", "व्यू"],
  ["लक्ष्य", "गोल"], ["उद्देश्य", "प्लान"], ["योजना", "प्लान"],
  ["प्रबंधन", "मैनेजमेंट"], ["विकास", "ग्रोथ"],
  ["अनुरोध", "रिक्वेस्ट"], ["सहायता", "सपोर्ट"], ["मदद", "हेल्प"],
  ["आसानी से", "आसानी से"], ["मुश्किल से", "मुश्किल से"],
  ["बाहर", "बाहर"], ["अंदर", "अंदर"], ["पास", "पास"], ["दूर", "दूर"], ["आगे", "आगे"], ["पीछे", "पीछे"],
  ["दोपहर", "दोपहर"], ["सुबह", "सुबह"], ["शाम", "शाम"], ["रात", "रात"],
];

function sanitizeDevanagari(text: string): string {
  if (!text) return text;
  let out: string;
  try {
    out = text.normalize("NFC");
  } catch {
    out = text;
  }
  out = out
    .replace(/\u200D/g, "")
    .replace(/\u200C/g, "")
    .replace(/\u200B/g, "")
    .replace(/\u00AD/g, "")
    .replace(/\uFEFF/g, "")
    .replace(/\u2060/g, "");
  const sb: string[] = [];
  let lastWasBase = false;
  let inCluster = false;
  for (let i = 0; i < out.length; i++) {
    const cp = out.charCodeAt(i);
    if (cp >= 0xd800 && cp <= 0xdbff) {
      sb.push(out[i]);
      if (i + 1 < out.length) { sb.push(out[i + 1]); i++; }
      lastWasBase = true;
      inCluster = false;
      continue;
    }
    const isDevanagariIndependentVowel =
      (cp >= 0x0904 && cp <= 0x0914) ||
      cp === 0x0905 || cp === 0x0906 || cp === 0x0907 || cp === 0x0908 ||
      cp === 0x0909 || cp === 0x090A || cp === 0x090B || cp === 0x090C ||
      cp === 0x090F || cp === 0x0910 || cp === 0x0913 || cp === 0x0914;
    const isDevanagariConsonant = (cp >= 0x0915 && cp <= 0x0939);
    const isDependentVowelSign =
      (cp >= 0x093A && cp <= 0x094F) || cp === 0x0955 || cp === 0x0956 ||
      cp === 0x0957 || cp === 0x0962 || cp === 0x0963;
    const isSpacingSign =
      (cp >= 0x0900 && cp <= 0x0903) ||
      cp === 0x0951 || cp === 0x0952 || cp === 0x0953 || cp === 0x0954;
    const isVirama = cp === 0x094D;
    const isNukta = cp === 0x093C;
    const isCombining = isDependentVowelSign || isSpacingSign || isVirama || isNukta;
    if (isCombining && !lastWasBase && !inCluster) {
      if (sb.length === 0 || /\s/.test(sb[sb.length - 1])) {
        sb.push("अ");
      } else {
        continue;
      }
    }
    sb.push(out[i]);
    if (isDevanagariConsonant || isDevanagariIndependentVowel) {
      inCluster = true;
      lastWasBase = true;
    } else if (isVirama) {
      inCluster = true;
      lastWasBase = true;
    } else if (isCombining) {
      inCluster = true;
      lastWasBase = true;
    } else if (cp === 0x20 || cp === 0x09 || cp === 0x0A || cp === 0x0D) {
      inCluster = false;
      lastWasBase = false;
    } else {
      inCluster = false;
      lastWasBase = false;
    }
  }
  let s = sb.join("");
  s = s
    .replace(/([\u0915-\u0939\u0904-\u0914])(\u0905+)([\u093A-\u094F\u0962\u0963\u0902\u0903])/g, "$1$3")
    .replace(/([\u093A-\u094F\u0902\u0903])(\u0905+)([\u093A-\u094F\u0902\u0903])/g, "$1$3")
    .replace(/([^\u0900-\u097F])\u093C/g, "$1")
    .replace(/(?<![\u0915-\u0939])\u094D(?![\u0915-\u0939])/g, "")
    .replace(/([\u0902\u0903\u093C\u093E-\u094F\u0951-\u0954])\1+/g, "$1")
    .replace(/([\u0915-\u0939][\u093A-\u094F]?)(\u0905)([\u0902\u0903\u093C\u093A-\u094F])/g, "$1$3")
    .replace(/([\u0904-\u0939][\u0900-\u0954]?)(\u0905)(?=[\u0900-\u0954])/g, "$1")
    .replace(/([\u0915-\u0939])\u0905([\u093C\u0902\u0903])/g, "$1$2")
    .replace(/ +/g, " ")
    .trim();
  return s;
}

function normalizeHindiPunctuation(text: string): string {
  if (!text) return text;
  return text
    .replace(/([,])+/g, ",")
    .replace(/([।])+/g, "।")
    .replace(/([!])+/g, "!")
    .replace(/([?])+/g, "?")
    .replace(/(\.){3,}/g, "…")
    .replace(/\s+([,;:.!?।])/g, "$1")
    .replace(/(["'“”])\s+([,;:.!?।])/g, "$1$2")
    .replace(/\s+/g, " ")
    .trim();
}

function applyHindiAamGlossary(text: string): string {
  if (!text) return text;
  let out = sanitizeDevanagari(text);
  for (const [from, to] of HINDI_AAM_GLOSSARY) {
    if (from) out = out.split(from).join(to);
  }
  out = normalizeHindiPunctuation(out);
  out = sanitizeDevanagari(out);
  return out;
}

function HtmlOrTextBody({
  content,
  ogImage,
  getId,
  skipFirstParagraph,
  translations,
  translateMode,
}: {
  content: string;
  ogImage?: string | null;
  getId: (t: string, fb: string) => string;
  skipFirstParagraph?: boolean;
  translations?: Record<string, string> | null;
  translateMode: "native" | "translated";
}) {
  const useTr = translateMode === "translated" && translations;
  const tr = (id: string, fallback: string) => {
    const raw = useTr ? (translations?.[id] ?? fallback) : fallback;
    if (!raw) return raw;
    return /[\u0900-\u097F]/.test(raw) ? applyHindiAamGlossary(raw) : raw;
  };

  if (!content) {
    return (
      <div className="border-2 border-dashed border-ink-950/40 p-6 text-sm text-ink-600 italic">
        {tr("html-empty", "Full story coming soon. The MapMyTimes newsroom is putting the finishing touches on this report. In the meantime, enjoy the photo gallery above and subscribe below for updates.")}
      </div>
    );
  }

  if (content.trim().startsWith("<") && /<\/[a-z]+>/i.test(content)) {
    let processed = content;
    let hc = 0;
    processed = processed.replace(/<h([2-4])(\s+[^>]*)?>([\s\S]*?)<\/h\1>/gi, (_m, lvl, attrs, inner) => {
      const idx = hc++;
      const txt = String(inner || "").replace(/<[^>]+>/g, "");
      const id = getId(txt, `h-${idx}`);
      const tTxt = tr(`htmlh-${idx}`, inner);
      return `<h${lvl}${attrs || ""} id="${id}" class="scroll-mt-24">${tTxt}</h${lvl}>`;
    });
    let pc = 0;
    processed = processed.replace(/<p(\s+[^>]*)?>([\s\S]*?)<\/p>/gi, (_m, attrs, inner) => {
      const idx = pc++;
      const tTxt = tr(`htmlp-${idx}`, inner);
      return `<p${attrs || ""}>${tTxt}</p>`;
    });
    let bc = 0;
    processed = processed.replace(/<blockquote(\s+[^>]*)?>([\s\S]*?)<\/blockquote>/gi, (_m, attrs, inner) => {
      const idx = bc++;
      const tTxt = tr(`htmlbq-${idx}`, inner);
      return `<blockquote${attrs || ""}>${tTxt}</blockquote>`;
    });
    let licCur = 0;
    processed = processed.replace(/<(ul|ol)(\s+[^>]*)?>([\s\S]*?)<\/\1>/gi, (m, kind, attrs, inside) => {
      const outerIdx = licCur++;
      let liIdx = 0;
      const replacedInside = inside.replace(/<li(\s+[^>]*)?>([\s\S]*?)<\/li>/gi, (_mm: string, _lattrs: string, liInner: string) => {
        const idx = liIdx++;
        const tTxt = tr(`htmlli-${outerIdx}-${idx}`, liInner);
        return `<li>${tTxt}</li>`;
      });
      return `<${kind}${attrs || ""}>${replacedInside}</${kind}>`;
    });
    return (
      <div
        className="article-body prose-custom text-base sm:text-lg text-ink-800 leading-[1.9] space-y-5 [&>h2]:font-headline [&>h2]:text-2xl sm:[&>h2]:text-3xl [&>h2]:uppercase [&>h2]:mt-8 [&>h3]:font-headline [&>h3]:text-xl sm:[&>h3]:text-2xl [&>h3]:uppercase [&>h3]:mt-6 [&>h4]:font-headline [&>h4]:uppercase [&_a]:text-news [&_a]:underline [&_blockquote]:border-l-4 [&_blockquote]:border-news [&_blockquote]:pl-4 [&_blockquote]:italic [&_blockquote]:my-6 [&_img]:border-2 [&_img]:border-ink-950 [&_img]:my-6 [&_img]:shadow-hard-sm [&_img]:h-auto [&_ul]:list-disc [&_ul]:pl-6 [&_ul]:space-y-1 [&_ul]:marker:text-news [&_ol]:list-decimal [&_ol]:pl-6 [&_ol]:space-y-1 [&_ol]:marker:text-news [&_p]:leading-[1.9] [&_p]:first:font-headline:first-letter:text-4xl:first-letter:sm:text-5xl:first-letter:font-black:first-letter:text-news:first-letter:float-left:first-letter:mr-2:first-letter:mt-1:first-letter:leading-none"
        dangerouslySetInnerHTML={{ __html: processed }}
      />
    );
  }

  // Plain-text with aggressive paragraph splitting, heading detection, and list detection.
  const raw = String(content || "")
    .replace(/\r\n?/g, "\n")
    .replace(/[ \t]+/g, " ")
    .trim();

  if (!raw) return null;

  // Split on double newlines, then further split long paragraphs at single-newline heading candidates.
  const chunks: { kind: "p" | "h" | "ul" | "ol"; level?: number; items?: string[]; text?: string }[] = [];
  const groups = raw.split(/\n{2,}/);
  const headingRe = /^(#{1,4}\s+|[0-9]{1,2}\.\s+)?([A-Z0-9][A-Za-z0-9 \-:'’”"/,&()]{2,90})([:])?$/;
  const ulRe = /^\s*[-*•]\s+(.+)$/;
  const olRe = /^\s*\d+[.)]\s+(.+)$/;

  let pendingList: { kind: "ul" | "ol"; items: string[] } | null = null;

  function flushList() {
    if (pendingList) {
      chunks.push({ ...pendingList });
      pendingList = null;
    }
  }

  for (const group of groups) {
    const lines = group.split("\n").map(l => l.trim()).filter(Boolean);
    if (lines.length === 0) continue;

    // Detect pure list group
    if (lines.every(l => ulRe.test(l)) || lines.every(l => olRe.test(l))) {
      flushList();
      const kind = ulRe.test(lines[0]) ? "ul" : "ol";
      chunks.push({
        kind,
        items: lines.map(l => (kind === "ul" ? l.match(ulRe)?.[1] : l.match(olRe)?.[1]) ?? l).filter(Boolean) as string[],
      });
      continue;
    }

    // Detect mixed heading + paragraphs inside group
    for (const line of lines) {
      // Ad-hoc list inside group
      if (ulRe.test(line) || olRe.test(line)) {
        const kind = ulRe.test(line) ? "ul" : "ol";
        const item = (kind === "ul" ? line.match(ulRe)?.[1] : line.match(olRe)?.[1]) ?? line;
        if (!pendingList || pendingList.kind !== kind) {
          flushList();
          pendingList = { kind, items: [] };
        }
        pendingList.items.push(item);
        continue;
      }
      flushList();

      // Detect heading candidates
      const isShort = line.length <= 100;
      const noEndPunct = !/[.?!…"]$/.test(line);
      const hasUpperCase = /[A-Z]/.test(line);
      const allCaps = line === line.toUpperCase() && line.replace(/[^A-Za-z]/g, "").length >= 3;
      const m = headingRe.exec(line);
      if (m || (isShort && noEndPunct && (allCaps || /^Table of contents|^Introduction$|^Conclusion$|^Why|^How|^Best|^Top|^Famous|^Local|^Budget/i.test(line)))) {
        const level = m?.[1]?.startsWith("#")
          ? Math.min(4, Math.max(2, m[1].trim().length + 1))
          : m?.[2]
            ? 2
            : allCaps ? 2 : 3;
        const text = m?.[2] ?? line;
        chunks.push({ kind: "h", level, text });
        continue;
      }

      // Otherwise paragraph — merge consecutive paragraphs? Keep as one per line or append to last p.
      const last = chunks[chunks.length - 1];
      if (last && last.kind === "p" && last.text && last.text.length + line.length < 1200) {
        last.text = `${last.text}\n${line}`;
      } else {
        chunks.push({ kind: "p", text: line });
      }
    }
  }
  flushList();

  if (skipFirstParagraph) {
    // Remove the first paragraph chunk (it's been rendered as drop-cap lede already)
    const firstP = chunks.findIndex((c) => c.kind === "p");
    if (firstP >= 0) chunks.splice(firstP, 1);
  }

  return (
    <div className="space-y-5 sm:space-y-6 article-body">
      {chunks.map((c, i) => {
        if (c.kind === "h") {
          const lvl = Math.min(4, Math.max(2, c.level ?? 2));
          const Tag = (`h${lvl}`) as keyof JSX.IntrinsicElements;
          const orig = c.text ?? "";
          const txt = tr(`htmlh-${i}`, orig);
          const id = getId(orig, `h-${i}`);
          return (
            <Tag
              key={i}
              id={id}
              className={cn(
                "group scroll-mt-24 font-headline uppercase leading-[0.95] mt-7 mb-3",
                lvl === 2 ? "text-2xl sm:text-3xl" : lvl === 3 ? "text-xl sm:text-2xl" : "text-lg sm:text-xl",
              )}
            >
              {txt}
              <a href={`#${id}`} className="ml-2 opacity-0 group-hover:opacity-100 text-news">#</a>
            </Tag>
          );
        }
        if (c.kind === "ul") {
          return (
            <ul key={i} className="my-4 list-disc pl-6 space-y-1 marker:text-news">
              {c.items?.map((it, j) => (
                <li key={j} className="text-base sm:text-lg leading-relaxed text-ink-800">
                  {tr(`htmlul-${i}-${j}`, it)}
                </li>
              ))}
            </ul>
          );
        }
        if (c.kind === "ol") {
          return (
            <ol key={i} className="my-4 list-decimal pl-6 space-y-1 marker:text-news">
              {c.items?.map((it, j) => (
                <li key={j} className="text-base sm:text-lg leading-relaxed text-ink-800">
                  {tr(`htmlol-${i}-${j}`, it)}
                </li>
              ))}
            </ol>
          );
        }
        const orig = c.text ?? "";
        const txt = tr(`htmlp-${i}`, orig);
        return (
          <p
            key={i}
            className={cn(
              "text-base sm:text-lg text-ink-800 leading-[1.9]",
              i === 0 && "first-letter:font-headline first-letter:text-4xl sm:first-letter:text-5xl first-letter:font-black first-letter:text-news first-letter:float-left first-letter:mr-2 first-letter:mt-1 first-letter:leading-none",
            )}
            dangerouslySetInnerHTML={{ __html: txt }}
          />
        );
      })}
    </div>
  );
}
