import type { BlogPostSummaryResponse } from "@/types/blog";

export type ShortVideoPlatform = "youtube-shorts" | "instagram-reels" | "unknown";

const IG_RE_STRICT =
  /instagram\.com\/(?:p|reel|reels|tv|stories\/[^/]+)\/([A-Za-z0-9_-]+)/;

export function getInstagramMediaId(
  input: string | null | undefined,
): string | null {
  if (!input) return null;
  const s = String(input).trim();
  if (!s) return null;
  if (/^[A-Za-z0-9_-]{8,}$/.test(s) && !s.includes(".")) return s;
  try {
    const u = new URL(s);
    if (u.hostname.includes("instagram.com")) {
      const m = u.pathname.match(IG_RE_STRICT);
      if (m) return m[1];
    }
  } catch {}
  const m = s.match(IG_RE_STRICT);
  return m ? m[1] : null;
}

export function igEmbedUrl(
  mediaId: string,
  opts?: {
    autoplay?: boolean;
    mute?: boolean;
    hideCaption?: boolean;
    omitscript?: boolean;
  },
): string {
  const o = { autoplay: true, mute: true, hideCaption: true, omitscript: true, ...(opts ?? {}) };
  const usp = new URLSearchParams();
  if (o.autoplay) usp.append("autoplay", "1");
  if (o.mute) usp.append("mute", "1");
  if (o.hideCaption) usp.append("hidecaption", "1");
  if (o.omitscript) usp.append("omitscript", "1");
  return `https://www.instagram.com/p/${encodeURIComponent(mediaId)}/embed/?${usp.toString()}`;
}

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
  platform: ShortVideoPlatform;
  videoId: string | null;
  caption: string;
  embedUrl: string;
  thumbnailUrl: string;
  originalUrl: string;
}

function _cleanCaption(post: any): string {
  const cleaned = String(post.content ?? "")
    .replace(/https?:\/\/\S+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  return cleaned || String(post.title ?? "").trim();
}

export function extractShortMeta(post: BlogPostSummaryResponse | any): ShortPostMeta {
  const youtubeVideoIdField: string | null =
    (post as any)?.youtubeVideoId ?? (post as any)?.videoId ?? null;
  const instagramMediaIdField: string | null =
    (post as any)?.instagramMediaId ??
    (post as any)?.reelId ??
    (post as any)?.igMediaId ??
    null;

  const primaryVideo: string = String(
    (post as any)?.primaryVideoUrl ?? (post as any)?.videoUrl ?? "",
  );
  const content: string = String(post.content ?? "");

  if (typeof instagramMediaIdField === "string" && instagramMediaIdField.trim()) {
    const id = getInstagramMediaId(instagramMediaIdField);
    if (id) {
      return {
        platform: "instagram-reels",
        videoId: id,
        caption: _cleanCaption(post),
        embedUrl: igEmbedUrl(id),
        thumbnailUrl: String(
          (post as any)?.featuredImageUrl ??
            (post as any)?.featuredImage?.url ??
            (post as any)?.coverImageUrl ??
            "",
        ),
        originalUrl: `https://www.instagram.com/reel/${id}/`,
      };
    }
  }

  if (typeof youtubeVideoIdField === "string" && youtubeVideoIdField.trim()) {
    const vid = getYouTubeVideoId(youtubeVideoIdField);
    if (vid) {
      const t = ytThumbnails(vid);
      return {
        platform: "youtube-shorts",
        videoId: vid,
        caption: _cleanCaption(post),
        embedUrl: ytEmbedUrl(vid),
        thumbnailUrl: t.best,
        originalUrl: `https://www.youtube.com/shorts/${vid}`,
      };
    }
  }

  const igContent = getInstagramMediaId(content) || getInstagramMediaId(primaryVideo);
  if (igContent) {
    return {
      platform: "instagram-reels",
      videoId: igContent,
      caption: _cleanCaption(post),
      embedUrl: igEmbedUrl(igContent),
      thumbnailUrl: String(
        (post as any)?.featuredImageUrl ??
          (post as any)?.featuredImage?.url ??
          (post as any)?.coverImageUrl ??
          "",
      ),
      originalUrl: `https://www.instagram.com/reel/${igContent}/`,
    };
  }

  const ytContent = getYouTubeVideoId(content) || getYouTubeVideoId(primaryVideo);
  if (ytContent) {
    const t = ytThumbnails(ytContent);
    return {
      platform: "youtube-shorts",
      videoId: ytContent,
      caption: _cleanCaption(post),
      embedUrl: ytEmbedUrl(ytContent),
      thumbnailUrl: t.best,
      originalUrl: `https://www.youtube.com/shorts/${ytContent}`,
    };
  }

  return {
    platform: "unknown",
    videoId: null,
    caption: String(post.title ?? "").trim(),
    embedUrl: "",
    thumbnailUrl: String(
      (post as any)?.featuredImageUrl ??
        (post as any)?.featuredImage?.url ??
        (post as any)?.coverImageUrl ??
        "",
    ),
    originalUrl: "",
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
