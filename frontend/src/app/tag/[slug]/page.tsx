"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { blogApi } from "@/lib/api/blogApi";
import type { BlogPostSummaryResponse, TagResponse } from "@/types/blog";
import { PostCard, Badge } from "@/components/posts/PostCard";
import { Button } from "@/components/ui/Button";
import { Pagination } from "@/components/ui/Pagination";
import { ASSETS } from "@/lib/assets";
import { useLanguage } from "@/lib/i18n/LanguageContext";

export default function TagPage() {
  const { lang, t } = useLanguage();
  const params = useParams<{ slug: string }>();
  const slug = typeof params?.slug === "string" ? decodeURIComponent(params.slug) : "";

  const [tag, setTag] = useState<TagResponse | null>(null);
  const [posts, setPosts] = useState<BlogPostSummaryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [state, setState] = useState<"loading" | "ready" | "empty">("loading");

  useEffect(() => {
    if (!slug) return;
    let active = true;
    setState("loading");
    Promise.all([
      blogApi.tags.bySlug(slug).catch(() => null),
      blogApi.posts.advancedSearch({ page, size: 12, sortBy: "publishedAt", sortDir: "DESC", language: lang.toUpperCase() as any } as any),
    ]).then(([tg, list]) => {
      if (!active) return;
      setTag(tg);
      const arr = (list as any)?.content ?? [];
      const ids = tg?.id ? arr.filter((p: BlogPostSummaryResponse) =>
        p.tags?.some((t) => t.id === tg.id || t.slug === slug)
      ) : arr;
      const used = ids.length > 0 ? ids : arr;
      setPosts(used);
      setTotalPages(Math.max(1, (list as any)?.totalPages ?? 1));
      setState(used.length === 0 ? "empty" : "ready");
    });
    return () => { active = false; };
  }, [slug, page, lang]);

  const heading = tag?.name ?? slug.replace(/-/g, " ");

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:py-10">
      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-ink-600 mb-6">
        <Link href="/" className="hover:text-news">Home</Link>
        <span>/</span>
        <span className="text-ink-950">#{heading}</span>
      </div>

      <section className="relative overflow-hidden border-4 border-ink-950 bg-ink-950 mb-8 shadow-hard">
        <img
          src={ASSETS.placeholders.categoryHero}
          alt=""
          aria-hidden
          className="absolute inset-0 h-full w-full object-cover opacity-40"
        />
        <div className="relative z-10 px-5 sm:px-8 py-8 sm:py-12 text-white flex flex-wrap items-center gap-4">
          <Badge variant="news" className="!border-white !bg-news !text-white">#{heading}</Badge>
          <h1 className="font-headline text-3xl sm:text-5xl uppercase leading-none">
            {t.tag.storiesTagged} &ldquo;{heading}&rdquo;
          </h1>
        </div>
      </section>

      {state === "loading" ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="animate-pulse border-2 border-ink-950 p-4 space-y-3">
              <div className="aspect-[16/10] bg-ink-900/10 border-2 border-ink-950" />
              <div className="h-5 w-2/3 bg-ink-900/20" />
            </div>
          ))}
        </div>
      ) : state === "empty" ? (
        <div className="border-2 border-ink-950 p-10 text-center bg-white">
          <h2 className="font-headline text-2xl uppercase mb-2">{t.tag.noStories}</h2>
          <p className="text-sm text-ink-700 mb-4">
            Try a different search term or topic.
          </p>
          <Link href="/explore">
            <Button variant="news">Explore topics</Button>
          </Link>
        </div>
      ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {posts.map((p) => (
                <PostCard key={p.id} post={p} variant="md" />
              ))}
            </div>
            <div className="mt-10">
              <Pagination page={page} setPage={setPage} totalPages={totalPages} />
            </div>
          </>
        )}
    </div>
  );
}
