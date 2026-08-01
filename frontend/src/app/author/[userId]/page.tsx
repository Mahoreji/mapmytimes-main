"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { blogApi } from "@/lib/api/blogApi";
import type { BlogPostSummaryResponse } from "@/types/blog";
import type { UserProfileResponse } from "@/types/auth";
import { PostCard, SectionTitle } from "@/components/posts/PostCard";
import { Button } from "@/components/ui/Button";
import { User as UserIcon, Mic, AtSign, Calendar } from "lucide-react";
import { formatDate, initials } from "@/lib/utils";
import { avatarOrDefault } from "@/lib/assets";
import { Pagination } from "@/components/ui/Pagination";
import { useLanguage } from "@/lib/i18n/LanguageContext";

export default function AuthorPage() {
  const { lang } = useLanguage();
  const params = useParams<{ userId: string }>();
  const userId = typeof params?.userId === "string" ? decodeURIComponent(params.userId) : "";
  const [profile, setProfile] = useState<Partial<UserProfileResponse> | null>(null);
  const [posts, setPosts] = useState<BlogPostSummaryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;
    let active = true;
    setLoading(true);
    Promise.all([
      blogApi.posts.byUser(userId, { page, size: 12, sort: "publishedAt,desc", language: lang.toUpperCase() as any }).catch(() => ({ content: [] }) as any),
    ]).then(([list]) => {
      if (!active) return;
      const arr = (list.content ?? []) as BlogPostSummaryResponse[];
      const first = arr[0];
      if (first) {
        setProfile({
          userId: first.userId,
          firstName: first.authorFirstName,
          lastName: first.authorLastName,
          email: first.authorEmail,
          profileImageUrl: first.authorAvatarUrl,
        });
      }
      setPosts(arr);
      setTotalPages(Math.max(1, (list as any).totalPages ?? 1));
      setLoading(false);
    });
    return () => { active = false; };
  }, [userId, page, lang]);

  const name = profile
    ? `${profile.firstName ?? ""} ${profile.lastName ?? ""}`.trim() || "Staff Journalist"
    : "Staff Journalist";

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:py-10">
      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-ink-600 mb-6">
        <Link href="/" className="hover:text-news">Home</Link>
        <span>/</span>
        <span className="text-ink-950">Byline · {name}</span>
      </div>

      <section className="bg-ink-950 text-white border-2 border-ink-950 shadow-hard-sm p-6 sm:p-8">
        <div className="flex flex-col md:flex-row items-start md:items-center gap-5">
          <div className="h-24 w-24 sm:h-28 sm:w-28 flex-shrink-0 border-2 border-white bg-ink-800 overflow-hidden">
            <img src={avatarOrDefault(profile?.profileImageUrl)} alt={name} className="h-full w-full object-cover" />
          </div>
          <div className="flex-1">
            <div className="ribbon text-xs mb-2">Journalist</div>
            <h1 className="font-headline text-3xl sm:text-4xl uppercase leading-none">{name}</h1>
            <div className="mt-3 flex flex-wrap gap-5 text-xs uppercase tracking-widest text-white/75 font-semibold">
              {profile?.email ? (
                <span className="inline-flex items-center gap-2">
                  <AtSign className="h-3.5 w-3.5 text-news" />
                  {profile.email}
                </span>
              ) : null}
              <span className="inline-flex items-center gap-2">
                <Mic className="h-3.5 w-3.5 text-news" />
                {posts.length.toLocaleString("en-IN")} articles
              </span>
              {profile?.createdAt ? (
                <span className="inline-flex items-center gap-2">
                  <Calendar className="h-3.5 w-3.5 text-news" />
                  Since {formatDate(profile.createdAt)}
                </span>
              ) : null}
            </div>
          </div>
          <Link href="#articles">
            <Button variant="outline" className="bg-transparent text-white border-white hover:bg-white hover:text-ink-950">Read stories</Button>
          </Link>
        </div>
      </section>

      <section id="articles" className="mt-10">
        <SectionTitle eyebrow="Byline" title={`Latest by ${name}`} />
        <div className="mt-5 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {loading
            ? Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="animate-pulse border-2 border-ink-950 p-4 space-y-3">
                  <div className="aspect-[16/10] bg-ink-900/10 border-2 border-ink-950" />
                  <div className="h-5 w-3/4 bg-ink-900/20" />
                </div>
              ))
            : posts.map((p) => <PostCard key={p.id} post={p} variant="md" />)}
        </div>
        {!loading && posts.length === 0 ? (
          <div className="border-2 border-ink-950 p-10 text-center bg-white mt-5">
            <p className="font-semibold uppercase tracking-widest text-ink-700">
              No stories published yet.
            </p>
          </div>
        ) : null}
        <div className="mt-10">
          <Pagination page={page} setPage={setPage} totalPages={totalPages} />
        </div>
      </section>
    </div>
  );
}
