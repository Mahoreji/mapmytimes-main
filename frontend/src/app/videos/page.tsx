"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { blogApi } from "@/lib/api/blogApi";
import type { BlogPostSummaryResponse } from "@/types/blog";
import { PostCard, SectionTitle, Badge } from "@/components/posts/PostCard";
import { Button } from "@/components/ui/Button";
import { Pagination } from "@/components/ui/Pagination";
import { useLanguage } from "@/lib/i18n/LanguageContext";
import { PlayCircle, Flame } from "lucide-react";
import { postIsVideoPost } from "@/lib/video";

export default function VideosPage() {
  const { lang, t } = useLanguage();
  const [posts, setPosts] = useState<BlogPostSummaryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    void (async () => {
      try {
        const res = await blogApi.posts.list({ page, size: 18, postType: "BLOG" as any });
        const arr = ((res as any)?.content ?? []).filter((p: BlogPostSummaryResponse) => !!postIsVideoPost(p));
        if (active) {
          setPosts(arr);
          setTotalPages(Math.max(1, (res as any)?.totalPages ?? 1));
        }
      } catch (err) {
        if (active) {
          setPosts([]);
          setTotalPages(1);
        }
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [page, lang]);

  const featuredVideo = posts[0] ?? null;
  const grid = posts.slice(1);

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:py-10">
      <div className="border-b-4 border-ink-950 pb-5">
        <div className="ribbon text-xs mb-3 inline-flex items-center gap-1.5">
          <PlayCircle className="w-3.5 h-3.5" />
          {t.home.trendingVideo}
        </div>
        <h1 className="font-headline text-3xl sm:text-5xl uppercase leading-[0.95] tracking-tight">
          {t.home.videoNews}
        </h1>
        <p className="mt-3 max-w-2xl text-ink-700 leading-relaxed">
          Watch the latest video reports, ground coverage, interviews, and explainers from MapMyTimes.
        </p>
      </div>

      <div className="mt-8 space-y-10">
        {featuredVideo ? (
          <section>
            <SectionTitle
              eyebrow={(t.common.watchMore ?? "Watch") + " — #1"}
              title={featuredVideo.title}
              action={
                <Link href={`/news/${encodeURIComponent(featuredVideo.slug)}`}>
                  <Button variant="ink" size="sm">
                    <PlayCircle className="w-4 h-4 mr-1.5" />
                    {t.home.watchMore}
                  </Button>
                </Link>
              }
            />
            <div className="mt-5">
              <PostCard post={featuredVideo} variant="hero" />
            </div>
          </section>
        ) : null}

        <section>
          <SectionTitle
            eyebrow="Latest"
            title={t.home.videoNews}
            action={
              featuredVideo ? (
                <Link href="/shorts">
                  <Button variant="outline" size="sm">
                    <Flame className="w-4 h-4 mr-1.5" /> Shorts
                  </Button>
                </Link>
              ) : null
            }
          />
          <div className="mt-5 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {(loading ? Array.from({ length: 6 }) : grid).map((p, i) =>
              p ? (
                <PostCard key={(p as BlogPostSummaryResponse).id} post={p as BlogPostSummaryResponse} variant="md" />
              ) : (
                <div key={`sk-vid-${i}`} className="border-2 border-ink-950 bg-white h-full">
                  <div className="aspect-[16/10] border-b-2 border-ink-950 bg-ink-800 animate-pulse" />
                  <div className="p-4 space-y-2">
                    <div className="h-3 w-1/3 bg-ink-200 animate-pulse" />
                    <div className="h-5 w-4/5 bg-ink-950 animate-pulse" />
                    <div className="h-3 w-full bg-ink-300 animate-pulse" />
                    <div className="h-9 w-2/3 bg-ink-200 animate-pulse mt-2" />
                  </div>
                </div>
              ),
            )}
          </div>

          {!loading && grid.length === 0 && !featuredVideo ? (
            <div className="mt-6 border-2 border-ink-950 p-8 bg-white text-center">
              <PlayCircle className="h-10 w-10 text-news mx-auto mb-2" />
              <h2 className="font-headline text-2xl uppercase mb-1">No videos yet</h2>
              <p className="text-sm text-ink-700">
                Video stories are coming soon. Meanwhile check out our latest reports or Shorts vertical feed.
              </p>
              <div className="mt-4 flex flex-wrap justify-center gap-2">
                <Link href="/"><Button variant="news" size="sm">Latest stories</Button></Link>
                <Link href="/shorts"><Button variant="outline" size="sm">Watch Shorts</Button></Link>
              </div>
            </div>
          ) : null}
        </section>

        {totalPages > 1 ? (
          <Pagination
            page={page}
            setPage={(p) => {
              setPage(p);
              if (typeof window !== "undefined") window.scrollTo({ top: 0, behavior: "smooth" });
            }}
            totalPages={totalPages}
          />
        ) : null}
      </div>
    </div>
  );
}
