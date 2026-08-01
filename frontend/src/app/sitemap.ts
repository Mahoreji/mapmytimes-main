import type { MetadataRoute } from "next";
import { SITE } from "@/lib/utils";
import { blogApi } from "@/lib/api/blogApi";
import type { BlogPostSummaryResponse, CategoryResponse, TagResponse } from "@/types/blog";
import type { PaginatedResponse } from "@/types/common";

const STATIC_PATHS: Array<{ path: string; priority: number; changefreq: MetadataRoute.Sitemap[number]["changeFrequency"] }> = [
  { path: "/", priority: 1.0, changefreq: "hourly" },
  { path: "/about", priority: 0.8, changefreq: "monthly" },
  { path: "/contact", priority: 0.8, changefreq: "monthly" },
  { path: "/careers", priority: 0.85, changefreq: "daily" },
  { path: "/sections", priority: 0.85, changefreq: "daily" },
  { path: "/explore", priority: 0.9, changefreq: "daily" },
  { path: "/search", priority: 0.6, changefreq: "weekly" },
  { path: "/login", priority: 0.3, changefreq: "yearly" },
  { path: "/signup", priority: 0.3, changefreq: "yearly" },
  { path: "/forgot-password", priority: 0.2, changefreq: "yearly" },
];

type SitemapEntry = MetadataRoute.Sitemap[number];

async function fetchAllPosts(): Promise<BlogPostSummaryResponse[]> {
  try {
    const first = await blogApi.posts.list({ page: 0, size: 100, status: "PUBLISHED" as const, sort: "publishedAt,desc" });
    if (!first) return [];
    if (Array.isArray(first)) return first.filter((p) => p.slug);
    const pr = first as PaginatedResponse<BlogPostSummaryResponse>;
    let out: BlogPostSummaryResponse[] = [...(pr.content || [])];
    if (pr.totalPages && pr.totalPages > 1) {
      const remaining = Math.min(pr.totalPages - 1, 9);
      for (let p = 1; p <= remaining; p++) {
        try {
          const page = (await blogApi.posts.list({ page: p, size: 100, status: "PUBLISHED" as const, sort: "publishedAt,desc" })) as PaginatedResponse<BlogPostSummaryResponse>;
          if (page?.content?.length) out = out.concat(page.content);
          if (page?.last) break;
        } catch {
          break;
        }
      }
    }
    return out.filter((x) => x && typeof x.slug === "string");
  } catch {
    return [];
  }
}

async function fetchCategories(): Promise<CategoryResponse[]> {
  try {
    const r = await blogApi.categories.list({ page: 0, size: 200 });
    if (!r) return [];
    if (Array.isArray(r)) return r.filter((c) => c.slug);
    const pr = r as PaginatedResponse<CategoryResponse>;
    return (pr.content || []).filter((c) => c && c.slug);
  } catch {
    return [];
  }
}

async function fetchTags(): Promise<TagResponse[]> {
  try {
    const r = await blogApi.tags.list({ page: 0, size: 300 });
    if (!r) return [];
    if (Array.isArray(r)) return r.filter((t) => t.slug);
    const pr = r as PaginatedResponse<TagResponse>;
    return (pr.content || []).filter((t) => t && t.slug);
  } catch {
    return [];
  }
}

function toDate(input: string | Date | undefined): Date {
  if (!input) return new Date();
  try {
    const d = input instanceof Date ? input : new Date(input);
    return Number.isNaN(d.getTime()) ? new Date() : d;
  } catch {
    return new Date();
  }
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const base = SITE.url.replace(/\/$/, "");
  const [posts, cats, tags] = await Promise.all([fetchAllPosts(), fetchCategories(), fetchTags()]);

  const entries: SitemapEntry[] = [];

  STATIC_PATHS.forEach((s) => {
    entries.push({
      url: `${base}${s.path}`,
      lastModified: new Date(),
      changeFrequency: s.changefreq,
      priority: s.priority,
    });
  });

  cats.forEach((c) => {
    entries.push({
      url: `${base}/category/${encodeURIComponent(c.slug)}`,
      lastModified: toDate(c.updatedAt || c.createdAt),
      changeFrequency: "daily",
      priority: 0.85,
    });
  });

  tags.forEach((t) => {
    entries.push({
      url: `${base}/tag/${encodeURIComponent(t.slug)}`,
      lastModified: toDate(t.updatedAt || t.createdAt),
      changeFrequency: "weekly",
      priority: 0.65,
    });
  });

  const seen = new Set<string>();
  posts.forEach((p) => {
    if (!p.slug || seen.has(p.slug)) return;
    seen.add(p.slug);
    entries.push({
      url: `${base}/news/${encodeURIComponent(p.slug)}`,
      lastModified: toDate(p.updatedAt || p.publishedAt || p.createdAt),
      changeFrequency: p.isTrending || p.isFeatured ? "hourly" : "weekly",
      priority: p.isFeatured ? 0.95 : p.isTrending ? 0.9 : 0.8,
    });
  });

  const authorIds = new Set<string>();
  posts.forEach((p) => {
    if (p.userId) authorIds.add(String(p.userId));
  });
  authorIds.forEach((id) => {
    entries.push({
      url: `${base}/author/${encodeURIComponent(id)}`,
      lastModified: new Date(),
      changeFrequency: "weekly",
      priority: 0.55,
    });
  });

  return entries;
}
