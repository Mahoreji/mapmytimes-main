import type { BlogPostSummaryResponse } from "@/types/blog";

export function getYouTubeVideoId(input: string | null | undefined): string | null {
  if (!input) return null;
  const s = String(input).trim();
  if (!s) return null;
  if (/^[a-zA-Z0-9_-]{11}$/.test(s)) return s;
  try {
    const u = new URL(s);
    if (u.hostname === "youtu.be" || u.hostname === "www.youtu.be" || u.hostname === "m.youtu.be") {
      const m = u.pathname.replace(/^\//, "").match(/^([a-zA-Z0-9_-]{11})/);
      if (m) return m[1];
    }
    if (u.hostname.includes("youtube.com") || u.hostname.includes("youtube-nocookie.com")) {
      const v = u.searchParams.get("v");
      if (v && /^[a-zA-Z0-9_-]{11}$/.test(v)) return v;
      const path = u.pathname || "";
      const patterns = [
        /\/embed\/([a-zA-Z0-9_-]{11})/i,
        /\/shorts\/([a-zA-Z0-9_-]{11})/i,
        /\/v\/([a-zA-Z0-9_-]{11})/i,
        /\/live\/([a-zA-Z0-9_-]{11})/i,
      ];
      for (const re of patterns) {
        const mm = path.match(re);
        if (mm) return mm[1];
      }
    }
  } catch {}
  const m = s.match(/(?:youtu\.be\/|youtube\.com(?:\/embed\/|\/v\/|\/shorts\/|\/watch\?v=|\/watch\?feature=player_embedded&v=))([a-zA-Z0-9_-]{11})/);
  return m ? m[1] : null;
}

export interface ShortPostMeta {
  videoId: string | null;
  caption: string;
}

export function extractShortMeta(post: BlogPostSummaryResponse | any): ShortPostMeta {
  const rawId = (post as any)?.youtubeVideoId ?? (post as any)?.videoId ?? null;
  if (typeof rawId === "string" && rawId.trim()) {
    const vid = getYouTubeVideoId(rawId);
    if (vid) {
      const rest = String(post.content ?? post.title ?? "").trim();
      return { videoId: vid, caption: rest };
    }
  }
  const fromContent = getYouTubeVideoId(post.content ?? "");
  if (fromContent) {
    const cleaned = String(post.content ?? "")
      .replace(/https?:\/\/\S+/g, " ")
      .replace(/\s+/g, " ")
      .trim();
    return {
      videoId: fromContent,
      caption: cleaned || String(post.title ?? "").trim(),
    };
  }
  const fromMedia = getYouTubeVideoId(post.primaryVideoUrl ?? "");
  return {
    videoId: fromMedia,
    caption: String(post.title ?? "").trim(),
  };
}

export function ytThumbnails(videoId: string | null | undefined) {
  if (!videoId) {
    return {
      maxres: "",
      hq: "",
      mq: "",
      sd: "",
      default: "",
      best: "",
    };
  }
  const vid = encodeURIComponent(videoId);
  const maxres = `https://i.ytimg.com/vi/${vid}/maxresdefault.jpg`;
  const hq = `https://i.ytimg.com/vi/${vid}/hqdefault.jpg`;
  const mq = `https://i.ytimg.com/vi/${vid}/mqdefault.jpg`;
  const sd = `https://i.ytimg.com/vi/${vid}/sddefault.jpg`;
  const def = `https://i.ytimg.com/vi/${vid}/default.jpg`;
  return {
    maxres,
    hq,
    mq,
    sd,
    default: def,
    best: hq,
  };
}

export function ytEmbedUrl(videoId: string, opts?: {
  mute?: boolean;
  autoplay?: boolean;
  loop?: boolean;
  rel?: number;
  controls?: number;
  fs?: number;
  modestbranding?: number;
  playsinline?: number;
  enablejsapi?: number;
  start?: number;
  origin?: string;
}): string {
  const o = {
    mute: 1,
    autoplay: 0,
    loop: 1,
    rel: 0,
    controls: 0,
    fs: 0,
    modestbranding: 1,
    playsinline: 1,
    enablejsapi: 1,
    ...(opts ?? {}),
  };
  const usp = new URLSearchParams();
  Object.entries(o).forEach(([k, v]) => {
    if (v === undefined || v === null) return;
    usp.append(k, String(v));
  });
  return `https://www.youtube.com/embed/${encodeURIComponent(videoId)}?${usp.toString()}`;
}

declare global {
  interface Window {
    YT?: any;
    onYouTubeIframeAPIReady?: () => void;
  }
}

let ytApiPromise: Promise<void> | null = null;
export function loadYouTubeIframeApi(): Promise<void> {
  if (typeof window === "undefined") return Promise.resolve();
  if (window.YT && typeof window.YT.Player === "function") return Promise.resolve();
  if (ytApiPromise) return ytApiPromise;
  ytApiPromise = new Promise<void>((resolve, reject) => {
    const existing = document.getElementById("youtube-iframe-api");
    const prevReady = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => {
      if (typeof prevReady === "function") {
        try { prevReady(); } catch {}
      }
      resolve();
    };
    const onErr = () => {
      ytApiPromise = null;
      reject(new Error("Failed to load YouTube IFrame API"));
    };
    if (existing) {
      const t = setTimeout(onErr, 8000);
      const tick = () => {
        if (window.YT && typeof window.YT.Player === "function") {
          clearTimeout(t);
          resolve();
        } else {
          setTimeout(tick, 250);
        }
      };
      tick();
      return;
    }
    const s = document.createElement("script");
    s.id = "youtube-iframe-api";
    s.src = "https://www.youtube.com/iframe_api";
    s.async = true;
    s.defer = true;
    s.onerror = onErr;
    const t = setTimeout(onErr, 10000);
    const resolved = { current: false };
    const orig = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => {
      if (resolved.current) return;
      resolved.current = true;
      clearTimeout(t);
      try { orig?.(); } catch {}
      resolve();
    };
    document.head.appendChild(s);
  });
  return ytApiPromise;
}
