"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { blogApi } from "@/lib/api/blogApi";
import type { BlogPostSummaryResponse } from "@/types/blog";
import { PostCard } from "@/components/posts/PostCard";
import { Button } from "@/components/ui/Button";
import { Search } from "lucide-react";
import { Pagination } from "@/components/ui/Pagination";
import { useLanguage } from "@/lib/i18n/LanguageContext";

function SearchPageInner() {
  const { lang, t } = useLanguage();
  const sp = useSearchParams();
  const qp = sp?.get("q") ?? "";
  const [query, setQuery] = useState(qp);
  const [submitted, setSubmitted] = useState(qp);
  const [posts, setPosts] = useState<BlogPostSummaryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setQuery(qp);
    setSubmitted(qp);
  }, [qp]);

  useEffect(() => {
    if (!submitted) {
      setPosts([]);
      return;
    }
    let active = true;
    setLoading(true);
    blogApi.posts
      .search(submitted, { page, size: 12, sort: "publishedAt,desc", language: lang.toUpperCase() as any })
      .then((r) => {
        if (!active) return;
        setPosts(r.content ?? []);
        setTotalPages(Math.max(1, r.totalPages ?? 1));
        setTotalElements(Number(r.totalElements ?? 0));
      })
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [submitted, page, lang]);

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const v = query.trim();
    const url = new URL(window.location.href);
    if (v) url.searchParams.set("q", v);
    else url.searchParams.delete("q");
    window.history.replaceState({}, "", url.toString());
    setSubmitted(v);
    setPage(0);
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:py-10">
      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-ink-600 mb-6">
        <Link href="/" className="hover:text-news">Home</Link>
        <span>/</span>
        <span className="text-ink-950">Search</span>
      </div>

      <div className="border-b-4 border-ink-950 pb-5">
        <div className="ribbon text-xs mb-3">Search</div>
        <h1 className="font-headline text-3xl sm:text-5xl uppercase leading-none mb-5">
          {submitted ? `Results for “${submitted}”` : t.search.title}
        </h1>
        <form onSubmit={onSubmit} className="flex gap-3 max-w-2xl items-stretch border-2 border-ink-950 bg-white shadow-hard-sm hover:shadow-hard transition-shadow">
          <div className="flex-1 flex items-center gap-2 px-3">
            <Search className="h-4 w-4 text-ink-600" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t.search.placeholder}
              className="flex-1 h-12 outline-none bg-transparent"
            />
          </div>
          <Button type="submit" variant="news" className="!rounded-none">{t.common.search}</Button>
        </form>
        {submitted ? (
          <p className="mt-3 text-sm font-semibold uppercase tracking-widest text-ink-600">
            {loading
              ? "Searching…"
              : `${totalElements.toLocaleString("en-IN")} story${
                  totalElements === 1 ? "" : "ies"
                } found`}
          </p>
        ) : null}
      </div>

      <div className="mt-8">
        {!submitted ? (
          <div className="border-2 border-ink-950 p-8 bg-white">
            <h2 className="font-headline text-2xl uppercase mb-3">
              {t.search.description}
            </h2>
            <p className="text-sm text-ink-700">
              {t.search.empty}
            </p>
          </div>
        ) : loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="animate-pulse border-2 border-ink-950 p-4 space-y-3">
                <div className="aspect-[16/10] bg-ink-900/10 border-2 border-ink-950" />
                <div className="h-5 w-3/4 bg-ink-900/20" />
                <div className="h-4 w-full bg-ink-900/10" />
              </div>
            ))}
          </div>
        ) : posts.length === 0 ? (
          <div className="border-2 border-ink-950 p-10 text-center bg-white">
            <h2 className="font-headline text-2xl uppercase mb-2">{t.search.noResults}</h2>
            <p className="text-sm text-ink-700 mb-4">
              {t.search.tryOther} <Link href="/explore" className="underline text-news">{t.nav.explore}</Link>.
            </p>
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
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={
      <div className="mx-auto max-w-7xl px-4 py-10">
        <div className="animate-pulse space-y-6">
          <div className="h-14 w-1/3 bg-ink-900/10" />
          <div className="h-12 border-2 border-ink-950/20 max-w-2xl" />
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 pt-6">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="border-2 border-ink-950/10 p-4 space-y-3">
                <div className="aspect-[16/10] bg-ink-900/5" />
                <div className="h-5 w-3/4 bg-ink-900/10" />
              </div>
            ))}
          </div>
        </div>
      </div>
    }>
      <SearchPageInner />
    </Suspense>
  );
}
