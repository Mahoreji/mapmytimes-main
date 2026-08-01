"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import type { BlogPostSummaryResponse, CategoryResponse } from "@/types/blog";
import { blogApi } from "@/lib/api/blogApi";
import { SectionTitle, PostCard, Badge } from "@/components/posts/PostCard";
import { Button } from "@/components/ui/Button";
import {
  MAIN_SECTIONS,
  MAIN_SECTION_SLUGS,
  MAIN_SECTION_META,
  MAIN_SECTION_HREF,
} from "@/lib/sections";
import { ArrowRight, Compass, Flame, Home } from "lucide-react";
import { SITE } from "@/lib/utils";
import { useLanguage } from "@/lib/i18n/LanguageContext";

export default function SectionsHubPage() {
  const { lang, t } = useLanguage();
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [latest, setLatest] = useState<BlogPostSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    Promise.all([
      blogApi.categories.list({ size: 20 }).catch(() => [] as any),
      blogApi.posts.list({ page: 0, size: 8, sort: "publishedAt,desc", language: lang.toUpperCase() as any }).catch(() => ({ content: [] }) as any),
    ]).then(([cats, posts]) => {
      if (!active) return;
      const catsArr = Array.isArray(cats)
        ? (cats as CategoryResponse[])
        : ((cats as any).content ?? []);
      setCategories(catsArr);
      const arr = (posts as any)?.content ?? [];
      setLatest(arr);
      setLoading(false);
    });
    return () => { active = false; };
  }, [lang]);

  const sectionData = useMemo(() => {
    const bySlug = new Map<string, CategoryResponse>();
    categories.forEach((c) => {
      if (c?.slug && !bySlug.has(String(c.slug).toLowerCase()))
        bySlug.set(String(c.slug).toLowerCase(), c);
    });
    return MAIN_SECTIONS.map((entry, i) => {
      const fallback: CategoryResponse = {
        id: `main-${entry.slug}`,
        slug: entry.slug,
        name: entry.meta.name,
        description: entry.meta.tagline,
      } as CategoryResponse;
      const existing = bySlug.get(entry.slug.toLowerCase()) || bySlug.get(entry.slug);
      return { ...entry, data: existing || fallback, count: (existing as any)?.postCount ?? 0 };
    });
  }, [categories]);

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:py-10">
      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-ink-600 mb-6">
        <Link href="/" className="hover:text-news inline-flex items-center gap-1.5">
          <Home className="h-3 w-3" /> Home
        </Link>
        <span>/</span>
        <span className="text-ink-950 inline-flex items-center gap-1.5">
          <Compass className="h-3 w-3" /> All Sections
        </span>
      </div>

      <section className="relative overflow-hidden border-4 border-ink-950 bg-ink-950 mb-10 shadow-hard">
        <div className="absolute inset-0 pointer-events-none opacity-20"
          style={{
            backgroundImage:
              "repeating-linear-gradient(45deg,#fff 0 1px,transparent 1px 20px),repeating-linear-gradient(-45deg,#fff 0 1px,transparent 1px 20px)",
          }}
        />
        <div className="relative z-10 px-5 sm:px-10 py-10 sm:py-14 text-white">
          <div className="inline-flex items-center gap-2 border-2 border-white/30 px-3 py-1 mb-5 shadow-[3px_3px_0_0_rgba(255,255,255,0.15)]">
            <Compass className="h-3.5 w-3.5 text-news" />
            <span className="text-[10px] sm:text-[11px] font-bold uppercase tracking-[0.3em] text-white/80">
              Navigate
            </span>
          </div>
          <h1 className="font-headline text-4xl sm:text-6xl lg:text-7xl uppercase leading-none max-w-4xl">
            {t.sections.title}
          </h1>
          <p className="mt-5 max-w-2xl text-white/80 text-[15px] sm:text-lg leading-relaxed">
            {t.sections.description}
          </p>
          <div className="mt-7 flex flex-wrap items-center gap-3">
            <Badge variant="outline" className="!border-white/50 !text-white !bg-transparent !shadow-none">
              8 Main sections
            </Badge>
            <Badge variant="outline" className="!border-white/50 !text-white !bg-transparent !shadow-none">
              National + Global
            </Badge>
            <Badge variant="outline" className="!border-news !text-news !bg-news/10 !shadow-none">
              Updated 24×7
            </Badge>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5 md:auto-rows-fr mb-14">
        {loading
          ? Array.from({ length: 8 }).map((_, i) => (
              <div
                key={i}
                className="border-2 border-ink-950 bg-white p-5 shadow-hard-sm animate-pulse h-full flex flex-col gap-4"
              >
                <div className="flex items-start justify-between">
                  <div className="w-20 h-20 border-2 border-ink-950 bg-ink-900/10" />
                  <div className="h-6 w-14 border-2 border-ink-950 bg-ink-900/10" />
                </div>
                <div className="h-7 w-2/3 bg-ink-900/15 rounded-sm" />
                <div className="h-4 w-full bg-ink-900/10 rounded-sm" />
                <div className="h-4 w-5/6 bg-ink-900/10 rounded-sm" />
                <div className="flex-1" />
                <div className="h-10 w-full border-2 border-ink-950 bg-ink-900/10" />
              </div>
            ))
          : sectionData.map((entry, idx) => {
              const { meta, data, count, slug, order } = entry;
              const href = MAIN_SECTION_HREF(slug);
              const altBg = idx % 2 === 0 ? "bg-white" : "bg-ink-50/50";
              return (
                <Link
                  key={slug}
                  href={href}
                  className={`group relative isolate border-2 border-ink-950 ${altBg} shadow-hard-sm hover:shadow-[8px_8px_0_0_rgba(0,0,0,1)] transition-all duration-200 hover:-translate-y-[3px] hover:-translate-x-[3px] h-full w-full flex flex-col overflow-hidden`}
                >
                  <div className="border-b-2 border-ink-950 p-4 sm:p-5 flex items-start justify-between gap-3 bg-gradient-to-br from-white to-ink-50/40 relative">
                    <div className="relative w-20 h-20 border-2 border-ink-950 bg-white p-2 shadow-[4px_4px_0_0_rgba(0,0,0,1)] group-hover:shadow-[5px_5px_0_0_rgba(0,0,0,1)] group-hover:-translate-y-[1px] group-hover:-translate-x-[1px] transition-all">
                      <img
                        src={meta.icon}
                        alt={`${meta.name} section`}
                        className="w-full h-full object-cover"
                        loading="lazy"
                        onError={(e) => { (e.currentTarget as HTMLImageElement).style.visibility = "hidden"; }}
                      />
                    </div>
                    <span className="inline-flex items-center gap-1 border-2 border-ink-950 bg-ink-950 text-white text-[10px] sm:text-[11px] font-bold uppercase tracking-[0.2em] px-2.5 py-1 leading-none whitespace-nowrap shadow-[2px_2px_0_0_rgba(0,0,0,1)]">
                      {meta.short}
                    </span>
                  </div>

                  <div className="flex-1 flex flex-col p-4 sm:p-5 gap-3">
                    <div className="flex items-center justify-between gap-2 flex-wrap">
                      <h2 className="font-headline text-2xl sm:text-3xl uppercase leading-none tracking-tight group-hover:text-news transition-colors">
                        {meta.name}
                      </h2>
                      <span className="inline-flex items-center gap-1 border-2 border-ink-950 bg-white text-ink-950 text-[10px] sm:text-[11px] font-bold uppercase tracking-widest px-2.5 py-1 shadow-[2px_2px_0_0_rgba(0,0,0,1)]">
                        {count > 0 ? `${count} stories` : "Latest stories"}
                      </span>
                    </div>

                    <p className="text-[13px] sm:text-sm leading-snug text-ink-700 line-clamp-2 max-w-none">
                      {data.description || meta.tagline}
                    </p>

                    <div className="flex-1" />

                    <div className="flex items-center justify-between gap-3 flex-wrap pt-1">
                      <span className="inline-flex items-center gap-1.5 text-[10px] sm:text-[11px] font-bold uppercase tracking-widest text-ink-500">
                        <span className="h-1.5 w-1.5 rounded-full bg-news" />
                        <span>Section {String(order + 1).padStart(2, "0")}</span>
                      </span>
                    </div>
                  </div>

                  <div className="border-t-2 border-ink-950 p-3 sm:p-4 bg-ink-950/[0.02]">
                    <span className="group/btn inline-flex items-center justify-center gap-2 w-full h-11 px-4 border-2 border-ink-950 bg-ink-950 text-white text-xs sm:text-[13px] font-bold uppercase tracking-widest shadow-[3px_3px_0_0_rgba(0,0,0,1)] group-hover:bg-news group-hover:border-news group-hover:text-white group-hover:shadow-[4px_4px_0_0_rgba(0,0,0,1)] group-hover:-translate-y-[1px] group-hover:-translate-x-[1px] transition-all">
                      Browse {meta.name}
                      <ArrowRight className="w-4 h-4 group-hover/btn:translate-x-0.5 transition-transform" />
                    </span>
                  </div>
                </Link>
              );
            })}
      </div>

      <section className="relative border-4 border-ink-950 bg-gradient-to-br from-white via-white to-ink-50/40 shadow-hard-sm p-6 sm:p-8 mb-14">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-5">
          <div className="max-w-2xl">
            <div className="inline-flex items-center gap-2 mb-3">
              <Flame className="h-4 w-4 text-news" />
              <span className="text-[11px] font-bold uppercase tracking-[0.3em] text-ink-600">
                Where to start
              </span>
            </div>
            <h3 className="font-headline text-2xl sm:text-3xl uppercase leading-none">
              Don&apos;t know where to begin?
            </h3>
            <p className="mt-3 text-ink-700 leading-relaxed">
              Start with our India desk for the national pulse, jump to Technology for the latest
              in AI and innovation, or head to Culture for arts, cinema, food and heritage.
            </p>
          </div>
          <div className="flex flex-wrap gap-3 lg:justify-end">
            <Link href={MAIN_SECTION_HREF("india")}>
              <Button variant="news" size="lg" className="inline-flex items-center gap-2">
                Start with India <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
            <Link href={MAIN_SECTION_HREF("technology")}>
              <Button variant="ink" size="lg" className="inline-flex items-center gap-2">
                Tech &amp; AI
              </Button>
            </Link>
            <Link href="/explore">
              <Button variant="outline" size="lg" className="inline-flex items-center gap-2">
                <Compass className="h-4 w-4" /> Explore
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {latest.length > 0 && (
        <section className="mb-10">
          <SectionTitle
            eyebrow="Latest"
            title="Fresh from the newsroom"
            action={
              <Link
                href="/explore"
                className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-widest text-ink-600 hover:text-news"
              >
                Read them all <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            }
          />
          <div className="mt-5 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {latest.slice(0, 4).map((p) => (
              <PostCard key={p.id} post={p} variant="md" className="h-full flex flex-col" />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
