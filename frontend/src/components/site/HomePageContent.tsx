"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import type { BlogPostSummaryResponse, CategoryResponse } from "@/types/blog";
import { blogApi } from "@/lib/api/blogApi";
import { PostCard, SectionTitle, Badge } from "@/components/posts/PostCard";
import { VideoEmbed } from "@/components/posts/VideoEmbed";
import { BreakingNewsTickerLive } from "@/components/site/BreakingNewsTickerLive";
import { ArrowRight, Play } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils";
import { categoryImageOrDefault } from "@/lib/assets";
import { postIsVideoPost } from "@/lib/video";
import { useLanguage } from "@/lib/i18n/LanguageContext";

export function HomePageContent() {
  const { lang, t } = useLanguage();
  const [featured, setFeatured] = useState<BlogPostSummaryResponse[]>([]);
  const [trending, setTrending] = useState<BlogPostSummaryResponse[]>([]);
  const [latest, setLatest] = useState<BlogPostSummaryResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loadState, setLoadState] = useState<"loading" | "ready" | "error">("loading");

  useEffect(() => {
    let active = true;
    Promise.all([
      blogApi.posts.list({ page: 0, size: 5, isFeatured: true, sort: "publishedAt,desc", language: lang.toUpperCase() as any }).catch(() => ({ content: [] }) as any),
      blogApi.posts.list({ page: 0, size: 6, isTrending: true, sort: "publishedAt,desc", language: lang.toUpperCase() as any }).catch(() => ({ content: [] }) as any),
      blogApi.posts.list({ page: 0, size: 36, sort: "publishedAt,desc", postType: "BLOG", language: lang.toUpperCase() as any }).catch(() => ({ content: [] }) as any),
      blogApi.categories.list({ size: 20 }).catch(() => [] as any) as any,
    ])
      .then(([f, t, l, c]) => {
        if (!active) return;
        const fArr = (f.content ?? []) as BlogPostSummaryResponse[];
        const tArr = (t.content ?? []) as BlogPostSummaryResponse[];
        const lArr = (l.content ?? []) as BlogPostSummaryResponse[];
        const cArr = Array.isArray(c) ? (c as CategoryResponse[]) : ((c as any).content ?? []);
        setFeatured(fArr);
        setTrending(tArr);
        setLatest(lArr);
        setCategories(cArr);
        setLoadState("ready");
      })
      .catch(() => active && setLoadState("error"));
    return () => {
      active = false;
    };
  }, [lang]);

  const hero = featured[0] ?? trending[0] ?? latest[0];
  const restFeatured = featured.slice(1);
  const secondaryTrending = trending.length > 0 ? trending : latest.slice(0, 6);
  const secondaryFeatured = restFeatured.length > 0 ? restFeatured : latest.slice(0, 4);

  const categoryBuckets = useMemo(() => {
    if (categories.length === 0) return [];
    const allPosts = Array.from(new Map(
      [...featured, ...trending, ...latest].map((p) => [p.id, p]),
    ).values());
    return categories
      .map((cat) => {
        const catId = cat?.id;
        const catSlug = String(cat?.slug ?? "").toLowerCase();
        const strictPosts = allPosts.filter((p) => {
          if (!p) return false;
          return (
            p.categories?.some((pc) => {
              const idMatch = pc?.id != null && String(pc.id) === String(catId);
              const slugMatch = String(pc?.slug ?? "").toLowerCase() === catSlug;
              return idMatch || slugMatch;
            }) ?? false
          );
        });
        return { category: cat, posts: strictPosts };
      })
      .filter((row) => row.posts.length > 0);
  }, [categories, featured, trending, latest]);

  const videoPosts = useMemo(() => {
    const allPosts = Array.from(new Map(
      [...featured, ...trending, ...latest].map((p) => [p.id, p]),
    ).values());
    return allPosts
      .filter((p) => !!postIsVideoPost(p))
      .slice(0, 6);
  }, [featured, trending, latest]);

  const trendingVideo = videoPosts[0] ?? null;


  return (
    <>
      <div className="bg-ink-950">
        <BreakingNewsTickerLive />
      </div>

      <section className="mx-auto max-w-7xl px-4 py-4 sm:py-6 md:py-8 grid grid-cols-1 lg:grid-cols-3 gap-5 sm:gap-6">
        <div className="lg:col-span-2 space-y-6">
          {loadState === "loading" ? (
            <SkeletonCard variant="hero" />
          ) : hero ? (
            <PostCard post={hero} variant="hero" />
          ) : (
            <EmptyBlock>
              <SectionTitle eyebrow="Top Story" title="Breaking: Featured" />
              <p className="text-sm text-ink-700">
                No featured post published yet. Check back soon for breaking stories.
              </p>
            </EmptyBlock>
          )}

          <div>
            <SectionTitle
              eyebrow="Latest" title={t.home.featured} />
            <div className="mt-5 grid grid-cols-1 md:grid-cols-2 gap-4">
              {loadState === "loading"
                ? [0, 1, 2, 3].map((i) => <SkeletonCard key={i} variant="sm" />)
                : secondaryFeatured.map((p) => (
                    <PostCard key={p.id} post={p} variant="md" />
                  ))}
            </div>
          </div>
        </div>

        <aside className="lg:col-span-1 space-y-6">
          {(loadState === "loading" || trendingVideo) ? (
            <div className="border-2 border-ink-950 bg-white shadow-hard-sm p-4">
              <div className="flex items-end justify-between gap-3 mb-4">
                <div>
                  <div className="ribbon text-xs mb-2 shadow-hard-sm">Live</div>
                  <h3 className="font-headline text-xl uppercase leading-none underline-accent">
                    {t.home.trendingVideo}
                  </h3>
                </div>
                <Link
                  href="/explore"
                  className="shrink-0 text-[10px] sm:text-[11px] font-bold uppercase tracking-widest text-ink-600 hover:text-news"
                >
                  {t.home.allVideos} <ArrowRight className="inline ml-0.5 h-3 w-3" />
                </Link>
              </div>
              {loadState === "loading" ? (
                <div className="animate-pulse border-2 border-ink-950 aspect-video bg-ink-900/10" />
              ) : trendingVideo ? (
                <article className="group">
                  <VideoEmbed
                    url={trendingVideo.primaryVideoUrl ?? postIsVideoPost(trendingVideo)?.url}
                    showTitle={trendingVideo.title}
                    aspect="16:9"
                  />
                  <div className="mt-3 flex flex-col gap-1.5">
                    <Link href={`/news/${encodeURIComponent(trendingVideo.slug)}`}>
                      <h4 className="font-headline text-[15px] uppercase leading-none group-hover:text-news transition-colors line-clamp-2">
                        {trendingVideo.title}
                      </h4>
                    </Link>
                    <p className="text-[11px] uppercase tracking-widest font-bold text-ink-600">
                      {trendingVideo.viewCount?.toLocaleString("en-IN") ?? 0} views
                    </p>
                  </div>
                </article>
              ) : null}
            </div>
          ) : null}

          <div className="border-2 border-ink-950 bg-white shadow-hard-sm p-4">
            <SectionTitle eyebrow="Trending" title={t.home.trendingNow} />
            <ul className="mt-4 sm:mt-5 flex flex-col gap-4 sm:gap-5">
              {loadState === "loading"
                ? [0, 1, 2, 3, 4].map((i) => <SkeletonRow key={i} />)
                : secondaryTrending.slice(0, 5).map((p, i) => (
                    <li key={p.id} className="group relative">
                      <div className="relative z-10 grid grid-cols-[2.5rem_1fr] sm:grid-cols-[3rem_1fr] items-start gap-1.5 sm:gap-2.5">
                        <div className="shrink-0 flex items-start justify-center pt-1 sm:pt-1.5 pointer-events-none select-none">
                          <span className="inline-flex items-start font-headline text-3xl sm:text-4xl leading-none text-news/95 drop-shadow-[2px_2px_0_rgba(0,0,0,1)] -rotate-2">
                            {String(i + 1).padStart(2, "0")}
                          </span>
                        </div>
                        <PostCard post={p} variant="row" />
                      </div>
                    </li>
                  ))}
            </ul>
          </div>

          <div className="border-2 border-ink-950 bg-ink-950 text-white p-4 shadow-hard-sm">
            <div className="flex items-center justify-between">
            <h3 className="font-headline uppercase tracking-wide">
              Subscribe
            </h3>
            <Badge variant="news">Free</Badge>
            </div>
            <p className="text-sm text-white/75 mt-2">
              Get breaking-news alerts and top stories in your inbox — Journalism of Integrity,
              delivered daily.
            </p>
            <form
              className="mt-4 flex flex-col gap-2"
              onSubmit={(e) => {
                e.preventDefault();
                const f = e.currentTarget as HTMLFormElement;
                f.reset();
                alert("Thanks — you're subscribed!");
              }}
            >
              <input
                required type="email" placeholder="your@email.com"
                className="h-10 px-3 border-2 border-white bg-transparent placeholder:text-white/50 text-white outline-none focus:border-news"
              />
              <Button variant="news" type="submit" className="w-full">Subscribe</Button>
            </form>
          </div>
        </aside>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-8 sm:py-10 grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-3 space-y-6">
          <SectionTitle
            eyebrow="Latest News"
            title={t.home.latestStories}
            action={
              <Link href="/explore" className="inline-flex items-center gap-1 text-xs font-bold uppercase tracking-widest hover:text-news">
                {t.home.exploreAll} <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            }
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-5 md:auto-rows-fr">
            {loadState === "loading"
              ? [0, 1, 2, 3, 4, 5].map((i) => <SkeletonCard key={i} />)
              : latest.slice(0, 10).map((p) => (
                  <PostCard key={p.id} post={p} variant="lg" className="h-full flex flex-col" />
                ))}
          </div>
        </div>

        <aside className="lg:col-span-1 space-y-6">
          {categories.length > 0 || loadState === "loading" ? (
            <div className="relative isolate border-2 border-ink-950 bg-white shadow-hard-sm overflow-hidden">
              <div className="px-4 py-4 border-b-2 border-ink-950 flex items-end justify-between gap-3">
                <div className="min-w-0">
                  <div className="ribbon text-xs mb-2 shadow-hard-sm">Browse</div>
                  <h3 className="font-headline text-xl sm:text-2xl uppercase leading-none underline-accent">
                    {t.home.categories}
                  </h3>
                </div>
                {categories.length > 0 ? (
                  <Link
                    href="/sections"
                    className="shrink-0 inline-flex items-center gap-1 text-[10px] sm:text-[11px] font-bold uppercase tracking-widest text-ink-600 hover:text-news whitespace-nowrap"
                  >
                    View all <ArrowRight className="h-3 w-3" />
                  </Link>
                ) : null}
              </div>

              <ul className="grid grid-cols-1 gap-0 divide-y divide-ink-950/10">
                {loadState === "loading"
                  ? [0, 1, 2, 3, 4, 5].map((i) => (
                      <li key={i} className="px-4 py-2.5 flex items-center gap-3">
                        <div className="h-8 w-8 bg-ink-900/10 border border-ink-950/20 rounded-sm animate-pulse shrink-0" />
                        <div className="flex-1 flex items-center gap-2">
                          <div className="h-3.5 w-28 bg-ink-900/10 animate-pulse" />
                          <div className="ml-auto h-4 w-10 bg-ink-900/10 rounded-sm animate-pulse" />
                        </div>
                      </li>
                    ))
                  : categories.map((c) => {
                      const slug = String(c.slug ?? "").toLowerCase();
                      if (!slug) return null;
                      const count = c.postCount ?? 0;
                      return (
                        <li key={c.id ?? slug} className="group">
                          <Link
                            href={`/category/${encodeURIComponent(slug)}`}
                            className="flex items-center gap-3 px-4 py-2.5 sm:py-3 w-full transition-colors hover:bg-news/[0.04]"
                          >
                            <div className="w-8 h-8 shrink-0 border border-ink-950/20 bg-white p-1 rounded-sm overflow-hidden">
                              <img
                                src={categoryImageOrDefault(slug)}
                                alt={`${c.name} category`}
                                loading="lazy"
                                className="w-full h-full object-cover"
                                onError={(e) => { (e.currentTarget as HTMLImageElement).style.visibility = "hidden"; }}
                              />
                            </div>
                            <span className="flex-1 min-w-0 font-headline text-sm sm:text-[15px] uppercase tracking-tight truncate group-hover:text-news transition-colors">
                              {c.name}
                            </span>
                            {count > 0 ? (
                              <span className="shrink-0 inline-flex items-center justify-center min-w-[2rem] h-5 px-2 rounded-full bg-ink-950/[0.06] text-ink-700 text-[10px] font-bold">
                                {count}
                              </span>
                            ) : null}
                            <ArrowRight className="h-3.5 w-3.5 shrink-0 text-ink-500 group-hover:text-news group-hover:translate-x-0.5 transition-all" />
                          </Link>
                        </li>
                      );
                    })}
              </ul>

              {loadState === "ready" && categories.length === 0 ? (
                <div className="px-4 py-6 text-center text-[12px] text-ink-500 font-medium">
                  No categories available yet.
                </div>
              ) : null}
            </div>
          ) : null}
        </aside>
      </section>

      {(loadState === "ready" || loadState === "loading") && (videoPosts.length > 0 || loadState === "loading") ? (
        <section className="mx-auto max-w-7xl px-4 py-10">
          <SectionTitle
            eyebrow="Watch"
            title={t.home.videoNews}
            action={
              <Link
                href="/explore"
                className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-widest text-ink-600 hover:text-news"
              >
                {t.home.watchMore} <ArrowRight className="h-3.5 w-3.5" />
                <Play className="h-3.5 w-3.5 ml-0.5 fill-current" />
              </Link>
            }
          />
          {loadState === "loading" ? (
            <div className="mt-5 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:auto-rows-fr">
              {[0, 1, 2, 3].map((i) => <SkeletonCard key={i} />)}
            </div>
          ) : (
            <div className="mt-5 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:auto-rows-fr">
              {videoPosts.map((p) => (
                <PostCard key={p.id} post={p} variant="md" className="h-full flex flex-col" />
              ))}
            </div>
          )}
        </section>
      ) : null}

      {loadState === "ready" && categoryBuckets.length > 0 ? (
        <section className="mx-auto max-w-7xl px-4 pb-10 space-y-10 sm:space-y-12">
          {categoryBuckets.map(({ category, posts }, i) => {
            const slug = String(category.slug ?? "").toLowerCase();
            if (!slug) return null;
            const rows = posts.slice(0, 4);
            return (
              <div key={category.id ?? slug}>
                <SectionTitle
                  eyebrow="Category"
                  title={category.name}
                  action={
                    <Link
                      href={`/category/${encodeURIComponent(slug)}`}
                      className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-widest text-ink-600 hover:text-news"
                    >
                      Explore more <ArrowRight className="h-3.5 w-3.5" />
                    </Link>
                  }
                />
                <div className="mt-5 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:auto-rows-fr">
                  {rows.map((p) => (
                    <PostCard key={p.id} post={p} variant="md" className="h-full flex flex-col" />
                  ))}
                </div>
                {i < categoryBuckets.length - 1 ? (
                  <div className="mt-10 sm:mt-12 h-[2px] bg-gradient-to-r from-transparent via-ink-950/15 to-transparent" />
                ) : null}
              </div>
            );
          })}
        </section>
      ) : null}

      <section className="mx-auto max-w-7xl px-4 py-10">
        <div className="bg-news border-2 border-ink-950 shadow-hard p-5 sm:p-6 lg:p-8 text-white">
          <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
            <div>
              <div className="font-headline uppercase text-[10px] sm:text-xs tracking-[0.25em] sm:tracking-[0.3em] opacity-90">
                MAP MY TIMES — NEWSROOM
              </div>
              <h2 className="mt-2 font-headline text-xl sm:text-2xl lg:text-3xl uppercase leading-[1.02] tracking-tight">
                {t.home.heroTitle}
              </h2>
              <p className="mt-2 text-white/85 text-sm max-w-2xl leading-relaxed">
                {t.home.heroBody}
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2 sm:gap-3">
              <Link href="/about">
                <Button variant="ink" className="bg-white text-ink-950 border-white hover:bg-ink-950 hover:text-white transition-colors">About us</Button>
              </Link>
              <Link href="/contact">
                <Button variant="outline" className="bg-transparent border-white text-white hover:bg-white hover:text-ink-950 transition-colors">Contact newsroom</Button>
              </Link>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}

function EmptyBlock({ children }: { children: React.ReactNode }) {
  return (
    <div className="border-2 border-ink-950 p-6 bg-white">
      <div className="space-y-2">{children}</div>
    </div>
  );
}

function SkeletonCard({ variant = "md" }: { variant?: "hero" | "md" | "sm" }) {
  const sz =
    variant === "hero"
      ? "aspect-[16/10]"
      : variant === "sm"
        ? "aspect-[16/10]"
        : "aspect-[16/10]";
  return (
    <div className="border-2 border-ink-950 bg-white animate-pulse">
      <div className={cn(sz, "bg-ink-900/10 border-b-2 border-ink-950")} />
      <div className="p-4 space-y-2">
        <div className="h-3 w-1/4 bg-ink-900/10" />
        <div className="h-5 w-3/4 bg-ink-900/20" />
        <div className="h-4 w-full bg-ink-900/10" />
      </div>
    </div>
  );
}

function SkeletonRow() {
  return (
    <div className="flex gap-3 items-start">
      <div className="w-24 aspect-square bg-ink-900/10 border-2 border-ink-950 animate-pulse" />
      <div className="flex-1 space-y-2">
        <div className="h-3 w-1/4 bg-ink-900/10 animate-pulse" />
        <div className="h-4 w-full bg-ink-900/20 animate-pulse" />
        <div className="h-4 w-2/3 bg-ink-900/10 animate-pulse" />
      </div>
    </div>
  );
}
