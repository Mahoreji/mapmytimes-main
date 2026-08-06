export type VideoPlatform = "youtube" | "youtube-shorts" | "instagram" | "instagram-reels" | "vimeo" | "unknown";

export interface ParsedVideo {
  platform: VideoPlatform;
  id: string;
  url: string;
  embedUrl: string;
  thumbnailUrl?: string;
  durationHint?: string;
}

const YT_RE =
  /(?:youtube\.com\/(?:watch\?v=|embed\/|shorts\/|v\/)|youtu\.be\/)([A-Za-z0-9_-]{6,})/;
const IG_RE =
  /instagram\.com\/(?:p|reel|reels|tv)\/([A-Za-z0-9_-]+)/;
const VIMEO_RE = /vimeo\.com\/(?:video\/)?(\d{6,})/;

export function parseVideoUrl(raw: string | null | undefined): ParsedVideo | null {
  if (!raw) return null;
  const s = String(raw).trim();
  if (!s) return null;

  try {
    const yt = s.match(YT_RE);
    if (yt) {
      const id = yt[1];
      return {
        platform: s.includes("/shorts/") ? "youtube-shorts" : "youtube",
        id,
        url: `https://www.youtube.com/watch?v=${id}`,
        embedUrl: `https://www.youtube-nocookie.com/embed/${id}?rel=0&modestbranding=1`,
        thumbnailUrl: `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
      };
    }

    const ig = s.match(IG_RE);
    if (ig) {
      const id = ig[1];
      const isReel = s.includes("/reel") || s.includes("/reels");
      return {
        platform: isReel ? "instagram-reels" : "instagram",
        id,
        url: isReel
          ? `https://www.instagram.com/reel/${id}/`
          : `https://www.instagram.com/p/${id}/`,
        embedUrl: `https://www.instagram.com/p/${id}/embed/?hidecaption=1&omitscript=1`,
        durationHint: isReel ? "9:16" : "1:1",
      };
    }

    const vm = s.match(VIMEO_RE);
    if (vm) {
      const id = vm[1];
      return {
        platform: "vimeo",
        id,
        url: `https://vimeo.com/${id}`,
        embedUrl: `https://player.vimeo.com/video/${id}`,
        thumbnailUrl: `https://vumbnail.com/${id}.jpg`,
      };
    }

    return {
      platform: "unknown",
      id: encodeURIComponent(s),
      url: s,
      embedUrl: s,
    };
  } catch {
    return null;
  }
}

export function findPostPrimaryVideo(media: unknown[] | null | undefined, contentBlocks: unknown[] | null | undefined): ParsedVideo | null {
  const firstMedia = (media ?? [])
    .map((m) => {
      const anyM = m as Record<string, unknown>;
      const type = String(anyM.mediaType ?? anyM.type ?? "").toUpperCase();
      const url = String(anyM.mediaUrl ?? anyM.url ?? anyM.videoUrl ?? "");
      if (!url) return null;
      if (type === "VIDEO") return parseVideoUrl(url);
      const parsed = parseVideoUrl(url);
      if (parsed && parsed.platform !== "unknown") return parsed;
      return null;
    })
    .find(Boolean) as ParsedVideo | null | undefined;
  if (firstMedia) return firstMedia;

  for (const b of contentBlocks ?? []) {
    const anyB = b as Record<string, unknown>;
    const d = (anyB.data ?? anyB) as Record<string, unknown>;
    const url = String(d.url ?? d.src ?? d.videoUrl ?? d.href ?? "");
    if (!url) continue;
    const p = parseVideoUrl(url);
    if (p && p.platform !== "unknown") return p;
  }
  return null;
}

export function postIsVideoPost(
  post: unknown,
): ParsedVideo | null {
  if (!post || typeof post !== "object") return null;
  const p = post as Record<string, unknown>;
  const direct = parseVideoUrl(
    String(p.videoUrl ?? p.primaryVideoUrl ?? ""),
  );
  if (direct) return direct;
  const media = p.media as unknown[] | null | undefined;
  const blocks = p.contentBlocks as unknown[] | null | undefined;
  return findPostPrimaryVideo(media, blocks);
}
