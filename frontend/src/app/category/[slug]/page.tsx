"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { blogApi } from "@/lib/api/blogApi";
import type { BlogPostSummaryResponse, CategoryResponse } from "@/types/blog";
import { PostCard } from "@/components/posts/PostCard";
import { Button } from "@/components/ui/Button";
import { Pagination } from "@/components/ui/Pagination";
import { categoryImageOrDefault } from "@/lib/assets";
import { useLanguage } from "@/lib/i18n/LanguageContext";

export default function CategoryPage() {
  const { lang, t } = useLanguage();
  const params = useParams<{ slug: string }>();
  const slug = typeof params?.slug === "string" ? decodeURIComponent(params.slug) : "";

  const [category, setCategory] = useState<CategoryResponse | null>(null);
  const [posts, setPosts] = useState<BlogPostSummaryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [state, setState] = useState<"loading" | "ready" | "empty" | "error">("loading");

  const SIZE = 12;

  useEffect(() => {
    if (!slug) return;
    let active = true;
    setState("loading");
    Promise.all([
      blogApi.categories.bySlug(slug).catch(() => null),
      blogApi.posts.advancedSearch({
        page,
        size: SIZE,
        sortBy: "publishedAt",
        sortDir: "DESC",
        language: lang.toUpperCase() as any,
      } as any),
    ]).then(([cat, list]) => {
      if (!active) return;
      setCategory(cat);
      const arr = (list as any)?.content ?? [];
      const catSlugNorm = slug.toLowerCase();
      const strict = arr.filter((p: BlogPostSummaryResponse) =>
        p.categories?.some((c) => {
          const idMatch = cat?.id != null && c.id != null && String(c.id) === String(cat.id);
          const slugMatch = String(c.slug ?? "").toLowerCase() === catSlugNorm;
          return idMatch || slugMatch;
        }) ?? false,
      );
      setPosts(strict);
      const tpRaw = (list as any)?.totalPages ?? 1;
      const tp = Math.max(1, tpRaw != null && !Number.isNaN(Number(tpRaw)) ? Number(tpRaw) : 1);
      const strictPages = Math.max(1, Math.ceil(strict.length / Math.max(1, SIZE)));
      setTotalPages(Math.max(1, Math.min(tp, strictPages || 1)));
      setState(strict.length === 0 ? "empty" : "ready");
    }).catch(() => active && setState("error"));
    return () => {
      active = false;
    };
  }, [slug, page, lang]);

  const heading = useMemo(
    () => category?.name ?? slug.replace(/-/g, " ").replace(/\b\w/g, (c) => c.toUpperCase()),
    [category, slug],
  );

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:py-10">
      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-ink-600 mb-6">
        <Link href="/" className="hover:text-news">Home</Link>
        <span>/</span>
        <Link href="/sections" className="hover:text-news">Sections</Link>
        <span>/</span>
        <span className="text-ink-950">{heading}</span>
      </div>

      <section className="relative overflow-hidden border-4 border-ink-950 bg-ink-950 mb-8 shadow-hard">
        <img
          src={categoryImageOrDefault(slug)}
          alt=""
          aria-hidden
          className="absolute inset-0 h-full w-full object-cover opacity-35"
        />
        <div className="relative z-10 px-5 sm:px-8 py-8 sm:py-12 text-white">
          <div className="ribbon text-xs mb-3 w-fit">Category</div>
          <h1 className="font-headline text-4xl sm:text-6xl uppercase leading-none">{heading}</h1>
          {category?.description ? (
            <p className="mt-4 max-w-2xl text-white/80">{category.description}</p>
          ) : null}
        </div>
      </section>

      {state === "loading" ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="animate-pulse border-2 border-ink-950 bg-white p-4 space-y-3">
              <div className="aspect-[16/10] bg-ink-900/10 border-2 border-ink-950" />
              <div className="h-4 w-1/3 bg-ink-900/10" />
              <div className="h-5 w-2/3 bg-ink-900/20" />
            </div>
          ))}
        </div>
      ) : state === "empty" ? (
        <div className="border-2 border-ink-950 p-10 text-center bg-white">
          <h2 className="font-headline text-2xl uppercase mb-2">{t.category.noStories}</h2>
          <p className="text-sm text-ink-700 mb-4">
            Our journalists are working on breaking coverage for{" "}
            <span className="font-bold">{heading}</span>. Check back shortly.
          </p>
          <Link href="/explore">
            <Button variant="news">Browse all stories</Button>
          </Link>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {posts.map((p) => (
              <PostCard key={p.id} post={p} variant="md" />
            ))}
          </div>

          <Pagination page={page} setPage={setPage} totalPages={totalPages} />
        </>
      )}
    </div>
  );
}
