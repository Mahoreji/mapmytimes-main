"use client";

import { useEffect, useState } from "react";
import { blogApi } from "@/lib/api/blogApi";
import type { BlogPostSummaryResponse } from "@/types/blog";
import { BreakingNewsTicker } from "@/components/site/BreakingNewsTicker";

export function BreakingNewsTickerLive() {
  const [posts, setPosts] = useState<BlogPostSummaryResponse[] | undefined>(undefined);
  useEffect(() => {
    let active = true;
    void blogApi.posts
      .list({ page: 0, size: 8, sort: "publishedAt,desc", postType: "BLOG" })
      .then((p) => {
        if (active) setPosts(p.content ?? undefined);
      })
      .catch(() => {
        if (active) setPosts([]);
      });
    return () => {
      active = false;
    };
  }, []);
  return <BreakingNewsTicker items={posts} />;
}
