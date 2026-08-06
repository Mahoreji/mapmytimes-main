"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { Bookmark, BookmarkX, ArrowLeft, BookOpen, Sparkles } from "lucide-react";
import { SITE } from "@/lib/utils";

type SavedPost = {
  id: string;
  title: string;
  slug: string;
  excerpt?: string;
  featuredImageUrl?: string;
  publishedAt?: string;
  savedAt: number;
};

const STORAGE_KEY = "mmt:saved-articles";

function readSaved(): SavedPost[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (p): p is SavedPost =>
        !!p && typeof p === "object" && typeof p.id === "string" && typeof p.title === "string",
    );
  } catch {
    return [];
  }
}

function writeSaved(list: SavedPost[]) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
  } catch {
    // quota or disabled storage — fail silently
  }
}

export default function SavedPage() {
  const [items, setItems] = useState<SavedPost[]>([]);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    setItems(readSaved());
    setHydrated(true);
  }, []);

  const remove = (id: string) => {
    const next = items.filter((p) => p.id !== id);
    setItems(next);
    writeSaved(next);
  };

  const clearAll = () => {
    setItems([]);
    writeSaved([]);
  };

  return (
    <div>
      <section className="bg-ink-950 text-white border-b-4 border-news">
        <div className="mx-auto max-w-5xl px-4 py-10 sm:py-14">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-white/70 hover:text-white"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to today&apos;s stories
          </Link>
          <div className="mt-6 flex flex-wrap items-end justify-between gap-4">
            <div>
              <div className="ribbon text-xs mb-3">Saved</div>
              <h1 className="font-headline text-4xl sm:text-6xl uppercase leading-none">
                Your reading list.
              </h1>
              <p className="mt-4 max-w-2xl text-white/80 text-lg leading-relaxed">
                {hydrated
                  ? items.length === 0
                    ? "No stories saved yet. Tap the bookmark icon on any article to save it for later."
                    : items.length === 1
                      ? "One story saved and ready to read — anytime, anywhere, even offline."
                      : `${items.length} stories saved and ready to read — anytime, anywhere, even offline.`
                  : "Checking your saved stories…"}
              </p>
            </div>
            {hydrated && items.length > 0 && (
              <div className="flex flex-wrap gap-2">
                <Link href="/explore">
                  <Button variant="outline" className="bg-transparent border-white text-white hover:bg-white hover:text-ink-950">
                    Discover more
                  </Button>
                </Link>
                <Button
                  variant="ghost"
                  className="text-white/70 hover:text-white hover:bg-white/10"
                  onClick={clearAll}
                >
                  Clear all
                </Button>
              </div>
            )}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-5xl px-4 py-10 sm:py-14">
        {!hydrated ? (
          <div className="animate-pulse space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="h-28 bg-ink-100 border-2 border-ink-950" />
            ))}
          </div>
        ) : items.length === 0 ? (
          <div className="border-2 border-dashed border-ink-950/30 bg-ink-50 px-6 py-16 text-center">
            <div className="h-16 w-16 mx-auto bg-white border-2 border-ink-950 inline-flex items-center justify-center text-news">
              <Bookmark className="h-8 w-8" />
            </div>
            <h2 className="font-headline uppercase text-2xl sm:text-3xl mt-6 text-ink-950">
              Nothing saved yet.
            </h2>
            <p className="mt-3 max-w-xl mx-auto text-ink-700">
              Your reading list lives on this device. Tap{" "}
              <span className="inline-flex items-center gap-1 font-bold">
                <Bookmark className="h-4 w-4" />
                Save
              </span>{" "}
              on any story to keep it here.
            </p>
            <div className="mt-8 flex flex-wrap justify-center gap-3">
              <Link href="/">
                <Button variant="news" size="lg">
                  <Sparkles className="h-4 w-4 mr-2" />
                  Read today&apos;s paper
                </Button>
              </Link>
              <Link href="/explore">
                <Button variant="outline" size="lg">
                  <BookOpen className="h-4 w-4 mr-2" />
                  Explore sections
                </Button>
              </Link>
            </div>
          </div>
        ) : (
          <ul className="space-y-4">
            {items.map((p) => (
              <li
                key={p.id}
                className="group border-2 border-ink-950 bg-white hover:shadow-hard-sm transition-shadow flex flex-col sm:flex-row"
              >
                {p.featuredImageUrl ? (
                  <div className="sm:w-56 w-full flex-shrink-0 border-b-2 sm:border-b-0 sm:border-r-2 border-ink-950 bg-ink-100 aspect-[16/10] sm:aspect-auto overflow-hidden">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={p.featuredImageUrl}
                      alt=""
                      className="h-full w-full object-cover"
                      loading="lazy"
                    />
                  </div>
                ) : null}
                <div className="flex-1 p-5 flex flex-col">
                  <Link href={`/news/${p.slug}`} className="block">
                    <h3 className="font-headline uppercase text-xl leading-snug group-hover:text-news transition-colors">
                      {p.title}
                    </h3>
                  </Link>
                  {p.excerpt ? (
                    <p className="mt-2 text-sm text-ink-700 line-clamp-2">{p.excerpt}</p>
                  ) : null}
                  <div className="mt-auto pt-5 flex flex-wrap items-center justify-between gap-3">
                    <div className="text-xs font-bold uppercase tracking-widest text-ink-600">
                      Saved{" "}
                      {new Date(p.savedAt).toLocaleDateString(undefined, {
                        day: "2-digit",
                        month: "short",
                        year: "numeric",
                      })}
                      {p.publishedAt ? ` · Published ${new Date(p.publishedAt).toLocaleDateString()}` : ""}
                    </div>
                    <div className="flex items-center gap-2">
                      <Link href={`/news/${p.slug}`}>
                        <Button variant="outline" size="sm">
                          Read now
                        </Button>
                      </Link>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-ink-700 hover:text-news"
                        onClick={() => remove(p.id)}
                      >
                        <BookmarkX className="h-4 w-4 mr-1.5" />
                        Remove
                      </Button>
                    </div>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
