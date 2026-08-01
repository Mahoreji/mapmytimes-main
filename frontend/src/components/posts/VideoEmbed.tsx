"use client";

import * as React from "react";
import { Play, ExternalLink, Youtube, Instagram, VideoIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { parseVideoUrl, type ParsedVideo, type VideoPlatform } from "@/lib/video";

interface VideoEmbedProps {
  url?: string | null;
  video?: ParsedVideo | null;
  aspect?: "16:9" | "9:16" | "4:3" | "1:1";
  className?: string;
  /** If true -> load iframe immediately; else -> click to load (GDPR friendly) */
  autoplay?: boolean;
  showTitle?: string | null;
  showCaption?: string | null;
}

export function VideoEmbed({
  url,
  video,
  aspect = "16:9",
  className,
  autoplay = false,
  showTitle,
  showCaption,
}: VideoEmbedProps) {
  const parsed = video ?? parseVideoUrl(url ?? null);
  const [active, setActive] = React.useState<boolean>(Boolean(autoplay));
  if (!parsed) return null;

  const aspectCls =
    aspect === "16:9"
      ? "aspect-video"
      : aspect === "9:16"
        ? "aspect-[9/16]"
        : aspect === "4:3"
          ? "aspect-[4/3]"
          : "aspect-square";

  const thumb = parsed.thumbnailUrl;
  return (
    <figure className={cn("group relative border-2 border-ink-950 shadow-hard-sm bg-ink-950 overflow-hidden", className)}>
      <div className={cn("relative w-full overflow-hidden bg-ink-900", aspectCls)}>
        {active ? (
          <iframe
            src={parsed.embedUrl + (parsed.embedUrl.includes("?") ? "&autoplay=1" : "?autoplay=1")}
            title={showTitle ?? parsed.id}
            className="absolute inset-0 h-full w-full"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            allowFullScreen
            referrerPolicy="no-referrer-when-downgrade"
          />
        ) : (
          <button
            type="button"
            onClick={() => setActive(true)}
            className="absolute inset-0 block w-full h-full text-left"
            aria-label={`Play video from ${labelForPlatform(parsed.platform)}`}
          >
            {thumb ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={thumb}
                alt={showTitle ?? "Video thumbnail"}
                className="absolute inset-0 h-full w-full object-cover opacity-90 group-hover:opacity-100 group-hover:scale-[1.015] transition-all duration-500"
                loading="lazy"
              />
            ) : (
              <div className="absolute inset-0 bg-gradient-to-br from-ink-800 via-ink-950 to-black" />
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-ink-950/80 via-ink-950/10 to-transparent" />
            <div className="absolute top-3 left-3 flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 bg-news text-white font-bold uppercase tracking-widest px-2.5 py-1 text-[10px] border-2 border-ink-950 shadow-hard-sm">
                <PlatformIcon platform={parsed.platform} className="h-3.5 w-3.5" />
                Video
              </span>
            </div>
            <div className="absolute top-3 right-3 flex items-center gap-1.5">
              <a
                href={parsed.url}
                target="_blank"
                rel="noreferrer noopener nofollow"
                onClick={(e) => e.stopPropagation()}
                className="bg-white/95 border-2 border-ink-950 text-ink-950 p-1.5 shadow-hard-sm hover:bg-ink-950 hover:text-white transition-colors"
                aria-label="Open on origin platform"
              >
                <ExternalLink className="h-3.5 w-3.5" />
              </a>
            </div>
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="rounded-full bg-news border-2 border-ink-950 text-white p-4 shadow-hard card-hover">
                <Play className="h-8 w-8 ml-1 fill-white" />
              </div>
            </div>
            {showTitle ? (
              <div className="absolute bottom-0 left-0 right-0 p-4 text-white">
                <figcaption className="font-headline uppercase text-lg sm:text-xl leading-none drop-shadow-hard">
                  {showTitle}
                </figcaption>
                {showCaption ? (
                  <p className="mt-2 text-xs sm:text-sm text-white/90 max-w-2xl line-clamp-2">
                    {showCaption}
                  </p>
                ) : null}
              </div>
            ) : showCaption ? (
              <div className="absolute bottom-0 left-0 right-0 p-3 text-white">
                <p className="text-xs text-white/90 line-clamp-2">{showCaption}</p>
              </div>
            ) : null}
          </button>
        )}
      </div>
    </figure>
  );
}

function labelForPlatform(p: VideoPlatform): string {
  switch (p) {
    case "youtube":
    case "youtube-shorts":
      return "YouTube";
    case "instagram":
    case "instagram-reels":
      return "Instagram";
    case "vimeo":
      return "Vimeo";
    default:
      return "Video";
  }
}

function PlatformIcon({ platform, className }: { platform: VideoPlatform; className?: string }) {
  if (platform === "youtube" || platform === "youtube-shorts") {
    return <Youtube className={className} />;
  }
  if (platform === "instagram" || platform === "instagram-reels") {
    return <Instagram className={className} />;
  }
  return <VideoIcon className={className} />;
}
