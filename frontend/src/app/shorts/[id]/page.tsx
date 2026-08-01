import { Metadata } from "next";
import { Suspense } from "react";
import { notFound } from "next/navigation";
import ShortsFeed from "@/components/shorts/ShortsFeed";
import { blogApi } from "@/lib/api/blogApi";
import { SITE } from "@/lib/utils";
import { ytThumbnails, extractShortMeta } from "@/lib/youtube";

interface ShortDeepLinkProps {
  params: { id: string };
}

export async function generateMetadata({
  params,
}: ShortDeepLinkProps): Promise<Metadata> {
  const id = params.id;
  let title = `Short · ${SITE.name}`;
  let description = `Watch this Short on ${SITE.name}.`;
  let image = "";
  try {
    const p = await blogApi.posts.get(id);
    const meta = extractShortMeta(p as any);
    title = meta.caption || p.title || title;
    description = meta.caption || p.excerpt || description;
    if (p.featuredImageUrl) image = p.featuredImageUrl;
    else if (meta.videoId) image = ytThumbnails(meta.videoId).maxres || ytThumbnails(meta.videoId).best;
  } catch {}
  return {
    title,
    description,
    alternates: { canonical: `${SITE.url}/shorts/${id}` },
    openGraph: {
      title, description,
      url: `${SITE.url}/shorts/${id}`,
      type: "video.other",
      siteName: SITE.name,
      ...(image ? { images: [{ url: image }] } : {}),
    },
    twitter: {
      card: "summary_large_image",
      title, description,
      ...(image ? { images: [image] } : {}),
    },
  };
}

export default async function ShortDeepLinkPage({ params }: ShortDeepLinkProps) {
  const id = params.id;
  let startIndex = 0;
  let initial: any[] = [];
  let totalPages = 0;
  try {
    const [post, list] = await Promise.all([
      blogApi.posts.get(id).catch(() => null),
      blogApi.posts.list({
        postType: "STORY",
        status: "PUBLISHED",
        page: 0,
        size: 12,
        sort: "publishedAt,DESC",
      } as any).catch(() => null),
    ]);
    if (!post) return notFound();
    const rest = (list?.content || []) as any[];
    const ids = new Set<string>();
    const merged: any[] = [];
    [post, ...rest].forEach((p) => {
      const k = String(p?.id);
      if (!k || ids.has(k)) return;
      ids.add(k);
      merged.push(p);
    });
    startIndex = 0;
    initial = merged;
    totalPages = list?.totalPages ?? 1;
  } catch {
    return notFound();
  }

  return (
    <div className="w-full bg-black min-h-screen">
      <Suspense fallback={null}>
        <ShortsFeed
          initialPosts={initial as any[]}
          startAtIndex={startIndex}
          initialPage={0}
          initialTotalPages={totalPages}
          pageSize={12}
        />
      </Suspense>
    </div>
  );
}
