import Script from "next/script";
import type { BlogPostResponse, TravelMeta } from "@/types/blog";
import { SITE } from "@/lib/utils";

export type ArticleJsonLdProps = {
  post: BlogPostResponse;
  canonical: string;
};

function authorName(p: BlogPostResponse): string {
  if (p.authorFirstName || p.authorLastName) return [p.authorFirstName, p.authorLastName].filter(Boolean).join(" ");
  return SITE.name;
}

function pubDate(p: BlogPostResponse): string {
  const d = p.publishedAt || p.createdAt || new Date().toISOString();
  return new Date(d).toISOString();
}

function modDate(p: BlogPostResponse): string {
  const d = p.updatedAt || p.publishedAt || p.createdAt || new Date().toISOString();
  return new Date(d).toISOString();
}

function imageUrl(p: BlogPostResponse): string {
  if (p.featuredImage?.url) return p.featuredImage.url;
  if (p.featuredImageUrl) return p.featuredImageUrl;
  return `${SITE.url.replace(/\/$/, "")}/assets/og/og-default.svg`;
}

function extractTextBlocks(contentBlocks: any[] | null | undefined): string[] {
  if (!Array.isArray(contentBlocks)) return [];
  const out: string[] = [];
  const walk = (node: any) => {
    if (!node || typeof node !== "object") return;
    if (typeof node.text === "string" && node.text.trim()) out.push(node.text.trim());
    if (Array.isArray(node.children)) node.children.forEach(walk);
    if (Array.isArray(node.content)) node.content.forEach(walk);
  };
  contentBlocks.forEach(walk);
  return out;
}

function placeFromTravel(tm: TravelMeta | undefined) {
  if (!tm) return null;
  const parts: string[] = [];
  if (tm.city) parts.push(tm.city);
  if (tm.state) parts.push(tm.state);
  if (tm.country) parts.push(tm.country);
  if (!tm.destination && parts.length === 0) return null;
  return { name: tm.destination || parts.join(", "), city: tm.city, state: tm.state, country: tm.country, geo: tm.coordinates };
}

export function ArticleJsonLd({ post, canonical }: ArticleJsonLdProps) {
  const base = SITE.url.replace(/\/$/, "");
  const title = post.seo?.metaTitle || post.title;
  const desc = (post.seo?.metaDescription || post.excerpt || "").replace(/\s+/g, " ").trim();
  const im = imageUrl(post);
  const published = pubDate(post);
  const modified = modDate(post);
  const author = authorName(post);
  const keywords = Array.from(
    new Set<string>([
      ...(post.seo?.keywords || []),
      ...(post.tags || []).map((t) => t.name),
      ...(post.categories || []).map((c) => c.name),
    ]),
  )
    .filter(Boolean)
    .slice(0, 40);
  const headline = title.slice(0, 110);
  const paragraphs = extractTextBlocks(post.contentBlocks).filter((p) => p.length > 20).slice(0, 6);
  const place = placeFromTravel(post.travelMeta);
  const isTravel = !!(post.destination || place || post.travelMeta);

  const newsArticle = {
    "@context": "https://schema.org",
    "@type": isTravel ? "TravelArticle" : "NewsArticle",
    "@id": `${canonical}#article`,
    isPartOf: { "@id": `${base}/#website` },
    headline,
    name: title,
    description: desc,
    mainEntityOfPage: { "@type": "WebPage", "@id": canonical, url: canonical },
    url: canonical,
    inLanguage: post.language || "en-IN",
    keywords,
    articleSection:
      (post.categories || [])
        .map((c) => c.name)
        .slice(0, 3)
        .join(", ") || "News",
    wordCount: (post.content || "").split(/\s+/).length,
    dateCreated: published,
    datePublished: published,
    dateModified: modified,
    thumbnailUrl: im,
    image: { "@type": "ImageObject", url: im },
    author: {
      "@type": "Person",
      name: author,
      url: post.userId ? `${base}/author/${encodeURIComponent(String(post.userId))}` : `${base}/about`,
      image: post.authorAvatarUrl ? { "@type": "ImageObject", url: post.authorAvatarUrl } : undefined,
    },
    publisher: { "@id": `${base}/#organization` },
    sourceOrganization: { "@id": `${base}/#organization` },
    copyrightHolder: { "@id": `${base}/#organization` },
    copyrightYear: new Date(published).getFullYear(),
    discussionUrl: `${canonical}#comments`,
    commentCount: Math.max(0, post.commentCount || 0),
    interactionStatistic: [
      {
        "@type": "InteractionCounter",
        interactionType: { "@type": "ReadAction" },
        userInteractionCount: Math.max(0, post.viewCount || 0),
      },
      {
        "@type": "InteractionCounter",
        interactionType: { "@type": "LikeAction" },
        userInteractionCount: Math.max(0, post.likeCount || 0),
      },
      {
        "@type": "InteractionCounter",
        interactionType: { "@type": "ShareAction" },
        userInteractionCount: Math.max(0, post.shareCount || 0),
      },
      {
        "@type": "InteractionCounter",
        interactionType: { "@type": "CommentAction" },
        userInteractionCount: Math.max(0, post.commentCount || 0),
      },
    ],
    articleBody: paragraphs.join("\n\n"),
    hasPart: paragraphs.length
      ? paragraphs.slice(0, 6).map((text, i) => ({
          "@type": "Article",
          "@id": `${canonical}#p${i + 1}`,
          articleBody: text,
        }))
      : undefined,
    about: isTravel && place
      ? {
          "@type": "Place",
          name: place.name,
          address: {
            "@type": "PostalAddress",
            addressLocality: place.city,
            addressRegion: place.state,
            addressCountry: place.country || "IN",
          },
          geo: place.geo
            ? {
                "@type": "GeoCoordinates",
                latitude: place.geo.lat,
                longitude: place.geo.lng,
              }
            : undefined,
        }
      : undefined,
    coverageStartTime: post.travelMeta?.travelDates?.start ? new Date(post.travelMeta.travelDates.start).toISOString() : undefined,
    coverageEndTime: post.travelMeta?.travelDates?.end ? new Date(post.travelMeta.travelDates.end).toISOString() : undefined,
  };

  const breadcrumb = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    "@id": `${canonical}#breadcrumb`,
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "Home", item: base },
      ...(post.categories || []).slice(0, 2).map((c, i) => ({
        "@type": "ListItem" as const,
        position: i + 2,
        name: c.name,
        item: `${base}/category/${encodeURIComponent(c.slug)}`,
      })),
      {
        "@type": "ListItem",
        position: (post.categories?.length || 0) + 2,
        name: title,
        item: canonical,
      },
    ].filter((x) => !!x),
  };

  return (
    <>
      <Script id={`jsonld-article-${post.id}`} type="application/ld+json" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: JSON.stringify(newsArticle) }} />
      <Script id={`jsonld-breadcrumb-${post.id}`} type="application/ld+json" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumb) }} />
    </>
  );
}
