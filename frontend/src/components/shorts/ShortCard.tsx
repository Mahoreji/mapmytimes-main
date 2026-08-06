"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import type { BlogPostSummaryResponse } from "@/types/blog";
import { blogApi } from "@/lib/api/blogApi";
import {
  extractShortMeta,
  ytThumbnails,
  loadYouTubeIframeApi,
  type ShortVideoPlatform,
} from "@/lib/youtube";
import { SITE, formatCount, initials, cn } from "@/lib/utils";
import type { ShortCardActions } from "./ShortsFeed";

type ShortLikeState = { liked: boolean; count: number };

interface ShortCardProps {
  post: BlogPostSummaryResponse;
  index: number;
  focus: "mounted" | "adjacent" | "idle";
  isActive: boolean;
  actions: ShortCardActions;
  openComments: (post: BlogPostSummaryResponse) => void;
}

function _platformLabel(p: ShortVideoPlatform): string {
  if (p === "instagram-reels") return "REEL";
  if (p === "youtube-shorts") return "SHORTS";
  return "VIDEO";
}

export default function ShortCard({
  post,
  isActive,
  focus,
  actions,
  openComments,
}: ShortCardProps) {
  const meta = extractShortMeta(post);
  const thumb = ytThumbnails(meta.videoId);
  const thumbSrcInitial =
    meta.platform === "instagram-reels"
      ? meta.thumbnailUrl || thumb.best
      : thumb.maxres || thumb.best;

  const iframeRef = useRef<HTMLIFrameElement | null>(null);
  const playerRef = useRef<any>(null);
  const [mountPlayer, setMountPlayer] = useState<boolean>(focus !== "idle");
  const [muted, setMuted] = useState<boolean>(true);
  const [thumbSrc, setThumbSrc] = useState<string>(thumbSrcInitial);
  const [thumbLoaded, setThumbLoaded] = useState<boolean>(false);
  const [like, setLike] = useState<ShortLikeState>({
    liked: false,
    count: Number(post.likeCount) || 0,
  });
  const viewRef = useRef<{ counted: boolean; enterAt: number | null }>({
    counted: false,
    enterAt: null,
  });
  const createdPlayerRef = useRef<boolean>(false);

  // focus -> mount
  useEffect(() => {
    setMountPlayer(focus !== "idle");
  }, [focus]);

  // Initial like check
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const ok = await blogApi.likes.check(post.id);
        if (!cancelled) setLike((s) => ({ ...s, liked: !!ok }));
      } catch {
        /* ignore guest */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [post.id]);

  // isActive drives play/pause + view counting
  useEffect(() => {
    if (isActive) {
      viewRef.current.enterAt = Date.now();
      const t = setTimeout(() => {
        if (!viewRef.current.counted) {
          viewRef.current.counted = true;
          blogApi.posts.incrementView(post.id).catch(() => undefined);
        }
      }, 1800);
      if (meta.platform === "youtube-shorts" && playerRef.current && typeof playerRef.current.playVideo === "function") {
        try { playerRef.current.playVideo(); } catch {}
      }
      return () => {
        clearTimeout(t);
        if (meta.platform === "youtube-shorts" && playerRef.current && typeof playerRef.current.pauseVideo === "function") {
          try { playerRef.current.pauseVideo(); } catch {}
        }
      };
    } else {
      viewRef.current.enterAt = null;
      if (meta.platform === "youtube-shorts" && playerRef.current && typeof playerRef.current.pauseVideo === "function") {
        try { playerRef.current.pauseVideo(); } catch {}
      }
    }
    return undefined;
  }, [isActive, post.id, meta.platform]);

  // mount + create YT.Player once mountPlayer true (YouTube only; Instagram uses plain iframe)
  useEffect(() => {
    if (!mountPlayer || !meta.videoId) return;
    if (meta.platform !== "youtube-shorts") return;
    if (createdPlayerRef.current) return;
    if (!iframeRef.current) return;

    let cancelled = false;
    (async () => {
      try {
        await loadYouTubeIframeApi();
        if (cancelled) return;
        if (!iframeRef.current || !window.YT?.Player) return;
        createdPlayerRef.current = true;
        const player = new window.YT.Player(iframeRef.current, {
          videoId: meta.videoId as string,
          width: "100%",
          height: "100%",
          playerVars: {
            autoplay: isActive ? 1 : 0,
            mute: muted ? 1 : 0,
            loop: 1,
            playlist: meta.videoId as string,
            rel: 0,
            controls: 0,
            fs: 0,
            modestbranding: 1,
            playsinline: 1,
            enablejsapi: 1,
            disablekb: 1,
            iv_load_policy: 3,
            origin: typeof window !== "undefined" ? window.location.origin : undefined,
          } as any,
          events: {
            onReady: (e: any) => {
              playerRef.current = e.target;
              try {
                if (muted) e.target.mute();
                else e.target.unMute();
                if (isActive) e.target.playVideo();
              } catch {}
            },
            onStateChange: (_e: any) => {},
            onError: (_e: any) => {},
          },
        });
        playerRef.current = player;
      } catch (err) {
        console.warn("YT player init failed for", meta.videoId, err);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [mountPlayer, meta.videoId, meta.platform]); // eslint-disable-line react-hooks/exhaustive-deps

  // Sync mute state to the already-created player
  useEffect(() => {
    const p = playerRef.current;
    if (!p) return;
    try {
      if (muted) p.mute();
      else p.unMute();
    } catch {}
  }, [muted]);

  const toggleMute = useCallback((e?: any) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();
    setMuted((m) => !m);
  }, []);

  const onToggleLike = useCallback(async (e: any) => {
    e.preventDefault?.();
    e.stopPropagation?.();
    const was = like.liked;
    setLike((s) => ({
      liked: !was,
      count: Math.max(0, Number(s.count) + (was ? -1 : 1)),
    }));
    try {
      if (was) await blogApi.posts.unlike(post.id);
      else await blogApi.posts.like(post.id);
    } catch (err: any) {
      setLike((s) => ({ liked: was, count: Math.max(0, Number(s.count) + (was ? 1 : -1)) }));
      actions.raiseLikeError(err);
    }
  }, [like.liked, post.id, actions]);

  const onShare = useCallback((e: any) => {
    e.preventDefault?.();
    e.stopPropagation?.();
    const origin = typeof window !== "undefined" ? window.location.origin : SITE.url;
    const url = `${origin.replace(/\/$/, "")}/shorts/${post.id}`;
    const text = meta.caption || post.title || "";
    const anyNav = navigator as any;
    if (anyNav?.share) {
      anyNav
        .share({ title: post.title, text, url })
        .then(() => undefined)
        .catch(() => undefined);
    } else if (navigator.clipboard?.writeText) {
      navigator.clipboard
        .writeText(url)
        .then(() => actions.raiseShareSuccess?.(post.id))
        .catch(() => undefined);
    } else {
      window.prompt("Copy link:", url);
    }
  }, [meta.caption, post.id, post.title, actions]);

  const onOpenComments = useCallback((e: any) => {
    e.preventDefault?.();
    e.stopPropagation?.();
    openComments(post);
  }, [openComments, post]);

  const authorInitials = initials(post.authorFirstName, post.authorLastName);
  const authorDisplay =
    post.authorFirstName || post.authorLastName
      ? `${post.authorFirstName || ""} ${post.authorLastName || ""}`.trim()
      : post.authorEmail
        ? post.authorEmail.split("@")[0]
        : "MapMyTimes";

  return (
    <article
      className="shorts-card snap-start snap-always relative w-full h-[100dvh] min-h-[640px] w-full bg-black overflow-hidden focus:outline-none"
      data-short-id={String(post.id)}
      aria-label={String(post.title || meta.caption || "News short")}
    >
      {/* Background gradient fallback + thumbnail */}
      <div className="absolute inset-0 bg-gradient-to-b from-ink-950 via-black to-ink-950">
        {thumbSrc && (
          <img
            key={thumbSrc}
            src={thumbSrc}
            alt=""
            loading={focus === "idle" ? "lazy" : "eager"}
            decoding="async"
            onError={() => {
              if (meta.platform === "instagram-reels") {
                if (thumbSrc && thumbSrc !== thumb.best) {
                  setThumbSrc(thumb.best);
                } else {
                  setThumbSrc("");
                }
              } else {
                if (thumbSrc !== thumb.best) {
                  setThumbSrc(thumb.best);
                } else {
                  setThumbSrc("");
                }
              }
            }}
            onLoad={() => setThumbLoaded(true)}
            className={cn(
              "absolute inset-0 h-full w-full object-cover select-none pointer-events-none",
              thumbLoaded ? "opacity-90" : "opacity-0",
              mountPlayer && createdPlayerRef.current ? "opacity-60" : "opacity-90",
            )}
            draggable={false}
          />
        )}
      </div>

      {/* IFrame Player — branched by platform */}
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="relative h-full w-full overflow-hidden shorts-video-clip">
          {mountPlayer && meta.videoId ? (
            <div className="absolute inset-0">
              {meta.platform === "instagram-reels" ? (
                <iframe
                  key={`ig-${meta.videoId}`}
                  src={meta.embedUrl}
                  title="Instagram Reel"
                  allow="autoplay; clipboard-write; encrypted-media; picture-in-picture; web-share"
                  allowFullScreen={false}
                  className="h-[180%] w-[180%] top-[-40%] left-[-40%] pointer-events-none"
                  loading="lazy"
                  referrerPolicy="no-referrer-when-downgrade"
                />
              ) : (
                <iframe
                  ref={iframeRef}
                  title="YouTube short"
                  allow="autoplay; clipboard-write; encrypted-media; picture-in-picture; web-share"
                  allowFullScreen={false}
                  className="h-[180%] w-[180%] top-[-40%] left-[-40%] pointer-events-none"
                  loading="lazy"
                />
              )}
            </div>
          ) : null}
        </div>
      </div>

      {/* Mute button badge */}
      <button
        type="button"
        onClick={toggleMute}
        className="absolute top-3 right-3 z-[46] flex items-center justify-center h-10 w-10 rounded-full bg-black/50 backdrop-blur-md ring-1 ring-white/15 text-white hover:bg-black/60"
        aria-label={muted ? "Unmute video" : "Mute video"}
      >
        {muted ? (
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
            <line x1="23" y1="9" x2="17" y2="15" />
            <line x1="17" y1="9" x2="23" y2="15" />
          </svg>
        ) : (
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
            <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
            <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
          </svg>
        )}
      </button>

      {/* Vertical gradient + overlays */}
      <div className="absolute inset-0 z-20 pointer-events-none bg-gradient-to-t from-black/85 via-black/10 to-black/40" />

      {/* Left side: story / section pill — platform specific accent */}
      <div className="absolute top-3 left-3 z-[45] flex items-center gap-2 pointer-events-none">
        <div
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full text-white text-[10px] font-black uppercase tracking-widest px-2.5 py-1 shadow-[0_4px_16px_rgba(0,0,0,0.25)]",
            meta.platform === "instagram-reels"
              ? "bg-[conic-gradient(from_210deg,#833AB4,#FD1D1D,#F77737,#FCAF45,#FFDC80,#F77737,#FD1D1D,#833AB4)]"
              : "bg-news",
          )}
        >
          <span className="h-1.5 w-1.5 rounded-full bg-white animate-pulse" />
          {_platformLabel(meta.platform)}
        </div>
        {(post.categories || []).slice(0, 1).map((c, i) => (
          <span
            key={String((c as any).id ?? (c as any).slug ?? (c as any).name ?? `c-${i}`)}
            className="hidden sm:inline-flex rounded-full bg-white/10 text-white text-[10px] font-semibold uppercase tracking-widest px-2.5 py-1 ring-1 ring-white/15 backdrop-blur-md"
          >
            {c.name}
          </span>
        ))}
      </div>

      {/* Right action rail */}
      <div className="absolute right-3 bottom-24 z-[45] flex flex-col items-center gap-4">
        <button
          type="button"
          onClick={onToggleLike}
          className="flex flex-col items-center gap-1 text-white group"
          aria-label={like.liked ? "Unlike short" : "Like short"}
        >
          <span className={cn(
            "flex h-12 w-12 items-center justify-center rounded-full ring-1 ring-white/15 backdrop-blur-md transition",
            like.liked
              ? "bg-news/95 text-white shadow-[0_8px_24px_rgba(255,74,23,0.35)]"
              : "bg-black/40 hover:bg-black/55 group-active:scale-95",
          )}>
            <svg viewBox="0 0 24 24" className="h-6 w-6" fill={like.liked ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
            </svg>
          </span>
          <span className="text-[11px] font-bold text-white/90 drop-shadow">
            {formatCount(like.count)}
          </span>
        </button>

        <button
          type="button"
          onClick={onOpenComments}
          className="flex flex-col items-center gap-1 text-white group"
          aria-label="Open comments"
        >
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-black/40 ring-1 ring-white/15 backdrop-blur-md hover:bg-black/55 group-active:scale-95 transition">
            <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
          </span>
          <span className="text-[11px] font-bold text-white/90 drop-shadow">
            {formatCount(post.commentCount)}
          </span>
        </button>

        <button
          type="button"
          onClick={onShare}
          className="flex flex-col items-center gap-1 text-white group"
          aria-label="Share short"
        >
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-black/40 ring-1 ring-white/15 backdrop-blur-md hover:bg-black/55 group-active:scale-95 transition">
            <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" />
              <polyline points="16 6 12 2 8 6" />
              <line x1="12" y1="2" x2="12" y2="15" />
            </svg>
          </span>
          <span className="text-[11px] font-bold text-white/90 drop-shadow">
            Share
          </span>
        </button>

        <div className="mt-1">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-white text-ink-950 text-[13px] font-black ring-2 ring-white/40 shadow-[0_8px_24px_rgba(0,0,0,0.25)]">
            {post.authorAvatarUrl ? (
              <img
                src={post.authorAvatarUrl}
                alt=""
                className="h-full w-full rounded-full object-cover"
              />
            ) : (
              <span>{authorInitials || "MM"}</span>
            )}
          </div>
          <div className="mt-1 text-center text-[9px] font-black uppercase tracking-wider text-white/85">
            Views
          </div>
          <div className="text-center text-[11px] font-black text-white drop-shadow">
            {formatCount(post.viewCount)}
          </div>
        </div>
      </div>

      {/* Bottom caption + author */}
      <div className="absolute left-0 right-0 bottom-0 z-[45] px-4 pb-6 pr-24 pointer-events-none">
        <div className="flex items-center gap-3 mb-3 pointer-events-auto">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white text-ink-950 text-xs font-black ring-1 ring-white/30 overflow-hidden">
            {post.authorAvatarUrl ? (
              <img
                src={post.authorAvatarUrl}
                alt=""
                className="h-full w-full object-cover"
              />
            ) : (
              <span>{authorInitials || "MM"}</span>
            )}
          </div>
          <div className="flex flex-col">
            <span className="text-white text-[13px] font-black drop-shadow">
              @{authorDisplay.replace(/\s+/g, "") || "mapmytimes"}
            </span>
            <span className="text-white/70 text-[11px] font-semibold drop-shadow">
              {authorDisplay}
            </span>
          </div>
          <button
            type="button"
            className="ml-2 inline-flex items-center gap-1 rounded-full bg-white px-3 py-1 text-[11px] font-black uppercase tracking-wider text-ink-950 hover:bg-white/90"
            onClick={(e) => {
              e.preventDefault?.();
              e.stopPropagation?.();
            }}
          >
            Follow
          </button>
        </div>

        <Link
          href={`/shorts/${post.id}`}
          className="block pointer-events-auto group"
        >
          <h2 className="text-white text-[15px] sm:text-[17px] font-black leading-[1.25] drop-shadow-[0_2px_8px_rgba(0,0,0,0.75)] line-clamp-3">
            {meta.caption || post.title || "Watch this short"}
          </h2>
          {(post.tags || []).length > 0 && (
            <div className="mt-2 flex flex-wrap gap-1.5">
              {(post.tags || []).slice(0, 4).map((t, idx) => (
                <span
                  key={String((t as any).id ?? (t as any).slug ?? (t as any).name ?? `t-${idx}`)}
                  className="inline-flex items-center rounded-full bg-white/10 text-white text-[10px] font-bold px-2 py-0.5 ring-1 ring-white/15 backdrop-blur-md"
                >
                  #{(t as any).name || (t as any).slug || "tag"}
                </span>
              ))}
            </div>
          )}
        </Link>
      </div>

      {/* Progress bar at bottom */}
      <div className="absolute bottom-0 left-0 right-0 z-[44] h-0.5 bg-white/10 overflow-hidden pointer-events-none">
        <div
          className={cn(
            "h-full bg-news transition-[width] duration-700 ease-out",
            isActive ? "w-full" : "w-0",
          )}
          style={isActive ? { animation: "shortsprogress 18s linear forwards" } : { width: 0 }}
        />
      </div>

      {/* Broken video banner */}
      {!meta.videoId && (
        <div className="absolute inset-0 z-40 flex flex-col items-center justify-center text-center px-6 pointer-events-none">
          <div className="rounded-2xl bg-black/55 backdrop-blur-md ring-1 ring-white/10 p-6 max-w-sm">
            <div className="mx-auto h-12 w-12 rounded-full bg-white/10 flex items-center justify-center text-white mb-3">
              <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" /></svg>
            </div>
            <p className="text-white/85 text-sm font-semibold">
              Missing video source (YouTube Short or Instagram Reel).
            </p>
            <p className="mt-2 text-white/70 text-xs leading-relaxed">
              Editors: paste a YouTube Short or Instagram Reel URL in <code className="px-1 bg-white/10 rounded">primaryVideoUrl</code> / post content, or set fields <code className="px-1 bg-white/10 rounded">youtubeVideoId</code> or <code className="px-1 bg-white/10 rounded">instagramMediaId</code>.
            </p>
          </div>
        </div>
      )}
    </article>
  );
}
