import { useEffect } from "react";
import Head from "next/head";
import type { BlogPostResponse } from "@/types/blog";
import { SITE } from "@/lib/utils";
import { ArticleJsonLd } from "@/components/seo/ArticleJsonLd";

export function ArticleSeoMeta({ post }: { post: BlogPostResponse | null }) {
  useEffect(() => {
    if (!post) return;
    if (post.title) document.title = `${post.seo?.metaTitle || post.title} | ${SITE.name}`;
  }, [post]);

  if (!post) return null;

  const base = SITE.url.replace(/\/$/, "");
  const canonical = `${base}/news/${encodeURIComponent(post.slug)}`;
  const title = post.seo?.metaTitle || post.title;
  const desc = (post.seo?.metaDescription || post.excerpt || title).replace(/\s+/g, " ").trim().slice(0, 220);
  const image =
    post.featuredImage?.url ||
    post.featuredImageUrl ||
    `${base}/assets/og/og-default.svg`;
  const published = post.publishedAt || post.createdAt;
  const modified = post.updatedAt || published;
  const keywords = Array.from(
    new Set<string>([
      ...(post.seo?.keywords || []),
      ...(post.tags || []).map((t) => t.name),
      ...(post.categories || []).map((c) => c.name),
    ]),
  )
    .filter(Boolean)
    .slice(0, 40)
    .join(", ");
  const author = [post.authorFirstName, post.authorLastName].filter(Boolean).join(" ") || SITE.name;
  const noIndex = !!post.seo?.noIndex;
  const isTravel = !!(post.destination || post.travelMeta);
  const tm = post.travelMeta;
  const placeName = tm?.destination || [tm?.city, tm?.state, tm?.country].filter(Boolean).join(", ") || post.destination || "India";
  const geoLat = tm?.coordinates?.lat;
  const geoLng = tm?.coordinates?.lng;
  const geoCountry = tm?.country || "IN";
  const geoRegion = tm?.state || "";
  const section = (post.categories || [])[0]?.name || (isTravel ? "Travel" : "News");

  const ogImageAlt = `${title} — ${SITE.name}`;

  return (
    <>
      <Head>
        <title>{title}</title>
        <meta name="description" content={desc} />
        <meta name="keywords" content={keywords} />
        <meta name="author" content={author} />
        <meta name="robots" content={noIndex ? "noindex, nofollow" : "index, follow, max-image-preview:large, max-snippet:-1"} />
        <meta name="googlebot" content={noIndex ? "noindex, nofollow" : "index, follow, max-image-preview:large, max-snippet:-1"} />
        <meta name="googlebot-news" content={noIndex ? "noindex" : "index, follow, max-image-preview:large"} />
        <link rel="canonical" href={post.seo?.canonicalUrl || canonical} />

        <meta property="og:type" content={isTravel ? "article" : "article"} />
        <meta property="og:site_name" content={SITE.name} />
        <meta property="og:url" content={canonical} />
        <meta property="og:title" content={title} />
        <meta property="og:description" content={desc} />
        <meta property="og:image" content={image} />
        <meta property="og:image:alt" content={ogImageAlt} />
        <meta property="og:image:secure_url" content={image} />
        <meta property="og:image:type" content={/\.svg$/i.test(image) ? "image/svg+xml" : "image/jpeg"} />
        <meta property="og:locale" content={post.language || "en_IN"} />
        <meta property="article:section" content={section} />
        {(post.tags || []).slice(0, 16).map((t) => (
          <meta key={t.id || t.slug || t.name} property="article:tag" content={t.name} />
        ))}
        {published ? <meta property="article:published_time" content={new Date(published).toISOString()} /> : null}
        {modified ? <meta property="article:modified_time" content={new Date(modified).toISOString()} /> : null}
        <meta property="article:author" content={author} />
        <meta property="article:publisher" content={SITE.name} />
        <meta property="article:tag" content={keywords} />
        {geoCountry ? <meta property="place:location:country_name" content={geoCountry} /> : null}
        {geoRegion ? <meta property="place:location:region" content={geoRegion} /> : null}
        {placeName ? <meta property="place:location:locality" content={placeName} /> : null}
        {typeof geoLat === "number" ? <meta property="place:location:latitude" content={String(geoLat)} /> : null}
        {typeof geoLng === "number" ? <meta property="place:location:longitude" content={String(geoLng)} /> : null}

        <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:site" content="@mapmytimes" />
        <meta name="twitter:creator" content="@mapmytimes" />
        <meta name="twitter:title" content={title} />
        <meta name="twitter:description" content={desc} />
        <meta name="twitter:image" content={image} />
        <meta name="twitter:image:alt" content={ogImageAlt} />

        <meta name="geo.country" content={geoCountry} />
        {geoRegion ? <meta name="geo.region" content={geoRegion} /> : null}
        <meta name="geo.placename" content={placeName} />
        {typeof geoLat === "number" && typeof geoLng === "number" ? (
          <>
            <meta name="geo.position" content={`${geoLat};${geoLng}`} />
            <meta name="ICBM" content={`${geoLat}, ${geoLng}`} />
          </>
        ) : null}

        <meta name="article:section" content={section} />
        <meta name="article:author" content={author} />
        {published ? <meta name="article:published_time" content={new Date(published).toISOString()} /> : null}
        {modified ? <meta name="article:modified_time" content={new Date(modified).toISOString()} /> : null}

        <meta name="DC.title" content={title} />
        <meta name="DC.creator" content={author} />
        <meta name="DC.publisher" content={SITE.name} />
        <meta name="DC.description" content={desc} />
        <meta name="DC.subject" content={keywords} />
        <meta name="DC.type" content="Text; Article; NewsArticle" />
        <meta name="DC.format" content="text/html" />
        <meta name="DC.identifier" content={canonical} />
        <meta name="DC.source" content={SITE.url} />
        <meta name="DC.language" content={post.language || "en-IN"} />
        {published ? <meta name="DC.date" content={new Date(published).toISOString()} /> : null}
        {placeName ? <meta name="DC.coverage.spatial" content={placeName} /> : null}
        <meta name="DC.rights" content={`© ${new Date().getFullYear()} MAPMYTOUR LLP, All Rights Reserved.`} />

        <meta name="news_keywords" content={keywords} />
        <meta name="google-news-publisher" content={SITE.name} />
        <meta name="classification" content="General News, Travel, Destination Guides, Investigative Journalism" />
        <meta name="place" content={placeName} />
        <meta name="article-tag" content={keywords} />
      </Head>

      <ArticleJsonLd post={post} canonical={canonical} />
    </>
  );
}
