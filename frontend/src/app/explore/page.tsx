"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { blogApi } from "@/lib/api/blogApi";
import type { BlogPostSummaryResponse, CategoryResponse, TagResponse } from "@/types/blog";
import { PostCard, SectionTitle, Badge } from "@/components/posts/PostCard";
import { Button } from "@/components/ui/Button";
import { Compass, Flame, Clock, Sparkles } from "lucide-react";
import { Pagination } from "@/components/ui/Pagination";
import { useLanguage } from "@/lib/i18n/LanguageContext";

type Tab = "explore" | "feed";

export default function ExplorePage() {
  const { lang, t } = useLanguage();
  const [tab, setTab] = useState<Tab>("explore");
  const [posts, setPosts] = useState<BlogPostSummaryResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [tags, setTags] = useState<TagResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    const req =
      tab === "explore"
        ? blogApi.social.explore({ page, size: 12 })
        : blogApi.social.feed({ page, size: 12 }).catch(() => ({ content: [] }) as any);
    Promise.all([
      req,
      blogApi.categories.list({ size: 20 }).catch(() => [] as any),
      blogApi.tags.popular().catch(() => [] as any),
    ]).then(([list, cats, tg]) => {
      if (!active) return;
      const arr = Array.isArray(list)
        ? (list as BlogPostSummaryResponse[])
        : ((list as any).content ?? []);
      const catsArr = Array.isArray(cats)
        ? (cats as CategoryResponse[])
        : ((cats as any).content ?? []);
      setPosts(arr);
      setCategories(catsArr);
      setTags(Array.isArray(tg) ? (tg as TagResponse[]) : []);
      setTotalPages(Math.max(1, (list as any).totalPages ?? 1));
      setLoading(false);
    });
    return () => { active = false; };
  }, [tab, page, lang]);

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:py-10">
      <div className="border-b-4 border-ink-950 pb-5">
        <div className="ribbon text-xs mb-3">Discover</div>
        <h1 className="font-headline text-3xl sm:text-5xl uppercase leading-none">
          {t.explore.title}
        </h1>
        <p className="mt-3 max-w-2xl text-ink-700">
          {t.explore.description}
        </p>
        <div className="mt-5 inline-flex border-2 border-ink-950 shadow-hard-sm">
          {([
            ["explore", "Explore", <Compass className="h-4 w-4" key="ex" />],
            ["feed", "My Feed", <Sparkles className="h-4 w-4" key="fd" />],
          ] as const).map(([key, label, icon]) => (
            <button
              key={key}
              onClick={() => { setTab(key as Tab); setPage(0); }}
              className={
                "h-11 px-4 text-xs font-bold uppercase tracking-widest inline-flex items-center gap-2 transition-colors " +
                (tab === key ? "bg-ink-950 text-white" : "bg-white hover:bg-ink-900 hover:text-white")
              }
            >
              {icon}
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-8 grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-3 space-y-6">
          <SectionTitle
            eyebrow={tab === "explore" ? "Top stories" : "For you"}
            title={tab === "explore" ? "Editor's picks" : "Your personalised feed"}
          />
          {tab === "feed" && posts.length === 0 && !loading ? (
            <div className="border-2 border-ink-950 p-8 bg-white">
              <Sparkles className="h-8 w-8 text-news mb-2" />
              <h2 className="font-headline text-xl uppercase mb-1">Sign in to build your feed</h2>
              <p className="text-sm text-ink-700 mb-4">
                Follow journalists, sections, and topics to surface stories you care about.
              </p>
              <div className="flex gap-2">
                <Link href="/login"><Button variant="news" size="sm">Sign in</Button></Link>
                <Link href="/signup"><Button variant="outline" size="sm">Create account</Button></Link>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {loading
                ? Array.from({ length: 6 }).map((_, i) => (
                    <div key={i} className="animate-pulse border-2 border-ink-950 p-4 space-y-3">
                      <div className="aspect-[16/10] bg-ink-900/10 border-2 border-ink-950" />
                      <div className="h-5 w-3/4 bg-ink-900/20" />
                    </div>
                  ))
                : posts.map((p) => <PostCard key={p.id} post={p} variant="md" />)}
            </div>
          )}
          <Pagination page={page} setPage={setPage} totalPages={totalPages} />
        </div>

        <aside className="lg:col-span-1 space-y-6">
          <div className="border-2 border-ink-950 p-4">
            <div className="flex items-center gap-2 mb-4">
              <Flame className="h-4 w-4 text-news" />
              <h3 className="font-headline uppercase tracking-wide text-sm">Trending Topics</h3>
            </div>
            <ul className="flex flex-wrap gap-2">
              {tags.slice(0, 16).map((t) => (
                <li key={t.id}>
                  <Link
                    href={`/tag/${encodeURIComponent(t.slug)}`}
                    className={
                      "inline-flex items-center px-2 py-1 border-2 border-ink-950 text-xs font-bold uppercase tracking-widest " +
                      (selectedCategory === t.slug
                        ? "bg-news text-white"
                        : "bg-white hover:bg-ink-950 hover:text-white")
                    }
                    onClick={() => setSelectedCategory(t.slug)}
                  >
                    #{t.name}
                    {t.postCount != null ? (
                      <span className="ml-1 text-[10px] opacity-70">{t.postCount}</span>
                    ) : null}
                  </Link>
                </li>
              ))}
              {tags.length === 0 ? (
                <li className="text-sm text-ink-600">Loading topics…</li>
              ) : null}
            </ul>
          </div>

          <div className="border-2 border-ink-950 p-4">
            <div className="flex items-center gap-2 mb-4">
              <Clock className="h-4 w-4 text-ink-900" />
              <h3 className="font-headline uppercase tracking-wide text-sm">Sections</h3>
            </div>
            <ul className="flex flex-col">
              {categories.slice(0, 12).map((c) => (
                <li key={c.id}>
                  <Link
                    href={`/category/${encodeURIComponent(c.slug)}`}
                    className="flex items-center justify-between border-b border-ink-950/10 py-2.5 text-sm font-bold uppercase tracking-wider hover:bg-news hover:text-white hover:px-2 transition-all"
                  >
                    <span>{c.name}</span>
                    <span className="text-xs font-bold text-ink-600 group-hover:text-white/80">
                      {c.postCount ?? 0}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </aside>
      </div>
    </div>
  );
}
