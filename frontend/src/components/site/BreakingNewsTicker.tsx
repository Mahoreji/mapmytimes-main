"use client";

import Link from "next/link";
import { cn } from "@/lib/utils";
import type { BlogPostSummaryResponse } from "@/types/blog";
import { useState } from "react";

export function BreakingNewsTicker({
  items,
  className,
}: {
  items?: BlogPostSummaryResponse[];
  className?: string;
}) {
  const fallback = [
    "BREAKING: Election Commission releases latest voter-turnout data across key states.",
    "Markets update: Indices open higher as IT and banking stocks lead momentum.",
    "Sports: Team secures last-minute victory in a record-breaking domestic fixture.",
    "Tech: New data protection rules come into effect for digital platforms.",
    "Weather: Heavy rainfall alert issued for central India over the next 48 hours.",
    "World: International summit concludes with joint climate-action agreement.",
  ];
  const headlines =
    items && items.length > 0
      ? items.map((p) => p.title)
      : fallback;

  const loop = [...headlines, ...headlines];

  const [paused, setPaused] = useState(false);

  return (
    <div
      className={cn(
        "relative w-full border-y-2 border-ink-950 bg-news text-white overflow-hidden",
        className,
      )}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
    >
      <div className="mx-auto max-w-7xl px-0 sm:px-4 flex">
        <div className="flex flex-shrink-0 items-center bg-ink-950 text-white px-4 py-2 sm:py-3 border-r-2 border-ink-950">
          <div className="flex items-center gap-2 sm:gap-3">
            <span className="relative flex h-2.5 w-2.5">
              <span className="absolute inset-0 rounded-full bg-news opacity-75 animate-ping" />
              <span className="relative rounded-full h-2.5 w-2.5 bg-news" />
            </span>
            <span className="font-headline uppercase text-xs sm:text-sm tracking-widest">
              Breaking
            </span>
          </div>
        </div>

        <div className="relative flex-1 overflow-hidden py-2 sm:py-3">
          <div
            className={cn(
              "ticker-track text-sm font-semibold uppercase tracking-wide will-change-transform",
              paused && "[animation-play-state:paused]",
            )}
          >
            {loop.map((h, i) => (
              <span key={i} className="inline-flex items-center gap-6">
                {items && items.length > 0 && items[i % items.length] ? (
                  <Link
                    href={`/news/${encodeURIComponent(items[i % items.length].slug)}`}
                    className="hover:underline decoration-white/60"
                  >
                    {h}
                  </Link>
                ) : (
                  <span>{h}</span>
                )}
                <span aria-hidden className="text-white/60">◆</span>
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
