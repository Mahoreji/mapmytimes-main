"use client";

import * as React from "react";
import Link from "next/link";
import { cn, formatDate, formatRelative, readingTimeLabel, slugify, truncate } from "@/lib/utils";
import { postCoverOrDefault } from "@/lib/assets";
import type { BlogPostSummaryResponse } from "@/types/blog";
import { Eye, Heart, MessageSquare, Clock, ArrowRight, Play, Video as VideoIcon, Share2, Check, Copy, Bookmark, BookmarkCheck } from "lucide-react";
import Image from "next/image";
import { postIsVideoPost } from "@/lib/video";

const SAVED_STORAGE_KEY = "mmt:saved-articles";

type SavedPostLite = {
  id: string;
  title: string;
  slug: string;
  excerpt?: string;
  featuredImageUrl?: string;
  publishedAt?: string;
  savedAt: number;
};

function readSavedLite(): SavedPostLite[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(SAVED_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (p): p is SavedPostLite =>
        !!p && typeof p === "object" && typeof p.id === "string" && typeof p.title === "string",
    );
  } catch {
    return [];
  }
}

function writeSavedLite(list: SavedPostLite[]) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(SAVED_STORAGE_KEY, JSON.stringify(list));
  } catch {
    // quota or disabled storage — fail silently
  }
}

export function Badge({
  children,
  variant = "default",
  className,
}: {
  children: React.ReactNode;
  variant?: "default" | "news" | "ink" | "outline";
  className?: string;
}) {
  const base =
    "inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-bold uppercase tracking-wider font-sans border-2 border-ink-950";
  const map = {
    default: "bg-white text-ink-950",
    news: "bg-news text-white",
    ink: "bg-ink-950 text-white",
    outline: "bg-transparent text-ink-950",
  } as const;
  return <span className={cn(base, map[variant], className)}>{children}</span>;
}

interface PostCardProps {
  post: BlogPostSummaryResponse;
  variant?: "hero" | "lg" | "md" | "sm" | "row";
  priority?: boolean;
  className?: string;
}

function CategoryRibbons({
  post,
  compact = false,
}: {
  post: BlogPostSummaryResponse;
  compact?: boolean;
}) {
  const list = (post.categories || []).slice(0, 2);
  const computeFallback = () => {
    const title = (post.title || "").toString();
    const destination = post.destination ? post.destination.toString() : "";
    const travel = /tour|travel|trip|destination|resort|hotel|valley|houseboat|heritage|beach|hill|places|guide|stay|luxury|backwater|kerala|himalaya|ladakh|rajasthan/i.test(title) || /places|resort|stay|city|state|country|town|region|tour|destination/i.test(destination);
    const politics = /parliament|election|party|minister|bjp|congress|politic|chief minister|mla|mp|gov|government|policy|bill|assembly|lok.?sabha|rajya.?sabha|campaign|leadership|manifesto|constituency|poll|opposition|coalition|ministry|speaker|amendment|by.?election/i.test(title);
    const sport = /cricket|ipl|football|match|score|world cup|olymp|athlete|t20|odi|test match|league|tournament|player|champion|coach|wicket|goal|medal|sport|tennis|badminton|kabaddi|formula|f1|grand prix|boxing|wrestling|shooting|hockey|athletic|run/i.test(title);
    const business = /market|stocks|share|nse|bse|sensex|nifty|economy|gdp|inflation|rbi|bank|loan|interest rate|startup|funding|ipo|company|revenue|profit|quarterly|earnings|corporat|industry|gst|tax|trade|import|export|budget|fiscal|msme|billion|million|investment|mutual fund|business|deal|acquisition|merger|price|unemployment/i.test(title);
    const technology = /tech|ai|artificial intelligence|chatgpt|gpt|mobile|smartphone|app|software|startup|gadget|laptop|internet|cyber|data|google|meta|apple|microsoft|tesla|semiconductor|chip|iphone|android|electric vehicle|ev|drone|robot|genai|llm|cloud|crypto|blockchain|cybersec|cyber.?security|policy.?digital|digital.?india|upi|fintech|edtech|ecommerce|it.?services|5g|4g|broadband|deepfake|quantum/i.test(title);
    const world = /world|global|united nation|un |us[a]? |america|china|russia|ukraine|europe|asia|africa|canada|australia|japan|uk |britain|france|germany|israel|gaza|palestine|iran|saudi|dubai|uae|nato|wef|imf|world bank|climate|summit|cop |foreign|international|bilateral|embassy|consulate|refugee|migrant|nuclear|treaty|war|conflict|unsc/i.test(title);
    const culture = /culture|cinema|bollywood|hollywood|movie|film|song|music|art|literature|festival|dance|theatre|museum|painting|craft|heritage.?site|food|cuisine|fashion|design|book|author|poet|history|religion|ritual|tradition|monument|archaeolog|temple|mosque|church|gurudwara|langar|indian.?culture|carnatic|hindustani|sufi|ghazal|ghoomar|garba|diwali|holi|eid|christmas|baisakhi|onam|pongal/i.test(title);
    const opinion = /editorial|opinion|column|comment|analysis|essay|viewpoint|perspective|by .?|letter|debate|guest|explained|explainer|think|should|must|need to|why |what if|imagine|if we|let us|we need|policy.?brief|response|review/i.test(title);
    const india = /india|delhi|mumbai|bengaluru|chennai|hyderabad|kolkata|ahmedabad|pune|jaipur|lucknow|bhopal|patna|chandigarh|gujarat|maharashtra|karnataka|kerala|tamil.?nadu|rajasthan|punjab|haryana|uttar.?pradesh|bihar|jharkhand|odisha|west.?bengal|telangana|andhra|himachal|uttarakhand|goa|sikkim|assam|nagaland|manipur|tripura|meghalaya|mizoram|arunachal|jammu|kashmir|ladakh|chhattisgarh|madhya.?pradesh|national|govt.?of.?india|indian/i.test(title);
    const slug = travel
      ? "culture"
      : politics
      ? "politics"
      : sport
      ? "sports"
      : business
      ? "business"
      : technology
      ? "technology"
      : world
      ? "world"
      : culture
      ? "culture"
      : opinion
      ? "opinion"
      : india
      ? "india"
      : "india";
    const name = {
      india: "India",
      world: "World",
      business: "Business",
      technology: "Technology",
      sports: "Sports",
      politics: "Politics",
      culture: "Culture",
      opinion: "Opinion",
    }[slug];
    return { slug, name };
  };
  const normalized: Array<{ slug: string; name: string; key: string }> = [];
  list.forEach((c, i) => {
    const rawName = (c.name || "").toString().trim();
    if (!rawName) return;
    const rawSlug = (typeof c.slug === "string" && c.slug.trim()) || slugify(rawName);
    if (!rawSlug) return;
    normalized.push({ slug: rawSlug, name: rawName, key: String(c.id || rawSlug || rawName || i) });
  });
  if (normalized.length === 0) {
    const fb = computeFallback();
    normalized.push({ slug: fb.slug, name: fb.name, key: `fb-${post.id || post.slug}-${fb.slug}` });
  }
  const chipClass = compact
    ? "inline-flex items-center gap-1 px-2 py-0.5 bg-news text-white border-2 border-ink-950 shadow-[2px_2px_0_0_rgba(0,0,0,1)] font-bold uppercase tracking-widest text-[9px]"
    : "inline-flex items-center gap-1.5 px-2.5 py-1 bg-news text-white border-2 border-ink-950 shadow-[2px_2px_0_0_rgba(0,0,0,1)] font-bold uppercase tracking-widest text-[10px] sm:text-xs";
  return (
    <div className={cn("flex flex-wrap items-center", compact ? "gap-1.5" : "gap-2")}>
      {normalized.slice(0, 2).map((chip) => (
        <span
          key={chip.key}
          className={chipClass}
        >
          {chip.name}
        </span>
      ))}
    </div>
  );
}

function ReadCta({ variant = "md", className }: { variant?: "hero" | "lg" | "md" | "sm" | "row"; className?: string }) {
  const sizes: Record<string, string> = {
    hero: "mt-1 inline-flex items-center gap-2 h-11 px-5 bg-white text-ink-950 border-2 border-ink-950 font-bold uppercase tracking-widest text-xs sm:text-sm hover:bg-news hover:text-white hover:border-news transition-colors",
    lg: "h-10 px-4 text-xs",
    md: "h-10 px-4 text-xs",
    sm: "h-9 px-3 text-[11px]",
    row: "h-8 px-3 text-[11px]",
  };
  return (
    <span
      aria-hidden
      className={cn(
        "inline-flex items-center gap-1.5 border-2 border-ink-950 bg-ink-950 text-white font-bold uppercase tracking-widest hover:bg-news hover:border-news hover:text-white transition-colors",
        sizes[variant] || sizes.md,
        className,
      )}
    >
      Full Story
      <ArrowRight className="w-3.5 h-3.5" />
    </span>
  );
}

function VideoBadge({ size = "sm", className }: { size?: "sm" | "md" | "lg"; className?: string }) {
  const sizes: Record<string, string> = {
    lg: "px-3 py-1.5 text-[11px] gap-2",
    md: "px-2.5 py-1 text-[10px] gap-1.5",
    sm: "px-2 py-0.5 text-[9px] gap-1",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center bg-news text-white border-2 border-ink-950 shadow-[2px_2px_0_0_rgba(0,0,0,1)] font-bold uppercase tracking-widest",
        sizes[size] ?? sizes.md,
        className,
      )}
    >
      <VideoIcon className="w-3 h-3 fill-white" />
      Video
    </span>
  );
}

function CardCover({
  post,
  className,
  coverSize = "md",
}: {
  post: BlogPostSummaryResponse;
  className?: string;
  coverSize?: "sm" | "md" | "lg" | "hero";
}) {
  const video = postIsVideoPost(post);
  const playSize =
    coverSize === "hero"
      ? "w-20 h-20 p-5"
      : coverSize === "lg"
        ? "w-16 h-16 p-4"
        : coverSize === "md"
          ? "w-14 h-14 p-3.5"
          : "w-10 h-10 p-2.5";
  const iconSize =
    coverSize === "hero"
      ? "w-10 h-10"
      : coverSize === "lg"
        ? "w-8 h-8"
        : coverSize === "md"
          ? "w-6 h-6"
          : "w-4 h-4";
  const badgeSize: "sm" | "md" | "lg" =
    coverSize === "hero" ? "lg" : coverSize === "sm" ? "sm" : "md";
  return (
    <div className={cn("relative overflow-hidden bg-ink-800 w-full h-full", className)}>
      <img
        src={video?.thumbnailUrl ?? postCoverOrDefault(post.featuredImageUrl)}
        alt={post.title}
        className="h-full w-full object-cover group-hover:scale-[1.03] transition-transform duration-500"
        loading={coverSize === "hero" ? undefined : "lazy"}
      />
      {video ? (
        <>
          <div className="absolute inset-0 bg-gradient-to-t from-ink-950/55 via-transparent to-transparent pointer-events-none" />
          <div className="absolute top-2 left-2 z-10">
            <VideoBadge size={badgeSize} />
          </div>
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-[1]">
            <div
              className={cn(
                "rounded-full bg-news text-white border-2 border-ink-950 shadow-[3px_3px_0_0_rgba(0,0,0,1)] flex items-center justify-center",
                playSize,
              )}
            >
              <Play className={cn(iconSize, "ml-0.5 fill-white")} />
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}

function CardShareButton({
  post,
  size = "md",
  dark = false,
  className,
}: {
  post: BlogPostSummaryResponse;
  size?: "sm" | "md" | "lg";
  dark?: boolean;
  className?: string;
}) {
  const [copied, setCopied] = React.useState(false);
  const href = `/news/${encodeURIComponent(post.slug)}`;
  const sizes: Record<string, string> = {
    sm: "h-8 w-8 [&>svg]:w-4 [&>svg]:h-4",
    md: "h-9 w-9 [&>svg]:w-4.5 [&>svg]:h-4.5",
    lg: "h-10 w-10 [&>svg]:w-5 [&>svg]:h-5",
  };
  async function onShare(e: React.MouseEvent<HTMLButtonElement>) {
    e.preventDefault();
    e.stopPropagation();
    const shareTitle = post.title || "MapMyTimes";
    const shareText = post.excerpt || shareTitle;
    try {
      let absolute = href;
      if (typeof window !== "undefined") {
        absolute = new URL(href, window.location.origin).toString();
      }
      if (typeof navigator !== "undefined" && typeof (navigator as any).share === "function") {
        await (navigator as any).share({ title: shareTitle, text: shareText, url: absolute });
        return;
      }
      if (typeof navigator !== "undefined" && typeof (navigator as any).clipboard?.writeText === "function") {
        await (navigator as any).clipboard.writeText(absolute);
        setCopied(true);
        window.setTimeout(() => setCopied(false), 1800);
      }
    } catch {
    }
  }
  return (
    <span className="relative z-[2] shrink-0 inline-flex" aria-hidden={false}>
      <span
        aria-hidden
        className="absolute inset-0 rounded-full transition-all duration-200"
        style={{
          backgroundColor: "#E31E24",
          border: "0.8px solid rgba(10, 10, 10, 0.9)",
          boxShadow: "0 2px 8px rgba(227, 30, 36, 0.25)",
        }}
      />
      <button
        type="button"
        onClick={onShare}
        aria-label={`Share ${post.title || "this article"}`}
        title="Share article"
        className={cn(
          "relative z-10 flex items-center justify-center rounded-full transition-opacity active:opacity-70 text-white",
          sizes[size] ?? sizes.md,
          className,
        )}
      >
        {copied ? (
          <Check className="w-4 h-4" />
        ) : (
          <Share2 className="w-4 h-4" />
        )}
      </button>
    </span>
  );
}

function CardSaveButton({
  post,
  size = "md",
  dark = false,
  className,
}: {
  post: BlogPostSummaryResponse;
  size?: "sm" | "md" | "lg";
  dark?: boolean;
  className?: string;
}) {
  const [saved, setSaved] = React.useState(false);
  const [toast, setToast] = React.useState(false);
  const sizes: Record<string, string> = {
    sm: "h-8 w-8 [&>svg]:w-4 [&>svg]:h-4",
    md: "h-9 w-9 [&>svg]:w-4.5 [&>svg]:h-4.5",
    lg: "h-10 w-10 [&>svg]:w-5 [&>svg]:h-5",
  };

  React.useEffect(() => {
    setSaved(readSavedLite().some((p) => String(p.id) === String(post.id)));
  }, [post.id]);

  function onSave(e: React.MouseEvent<HTMLButtonElement>) {
    e.preventDefault();
    e.stopPropagation();
    const current = readSavedLite();
    const existsIdx = current.findIndex((p) => String(p.id) === String(post.id));
    let next: SavedPostLite[];
    if (existsIdx >= 0) {
      next = current.filter((_, i) => i !== existsIdx);
      setSaved(false);
    } else {
      const meta: SavedPostLite = {
        id: String(post.id),
        title: post.title || "Untitled",
        slug: post.slug || String(post.id),
        excerpt: post.excerpt ?? undefined,
        featuredImageUrl: post.featuredImageUrl ?? undefined,
        publishedAt: post.publishedAt ?? post.createdAt ?? undefined,
        savedAt: Date.now(),
      };
      next = [meta, ...current];
      setSaved(true);
      setToast(true);
      window.setTimeout(() => setToast(false), 1600);
    }
    writeSavedLite(next);
  }

  return (
    <span className="relative z-[2] shrink-0 inline-flex" aria-hidden={false}>
      <span
        aria-hidden
        className="absolute inset-0 rounded-full transition-all duration-300"
        style={{
          backgroundColor: "#E31E24",
          border: saved
            ? "1.2px solid rgba(255, 255, 255, 0.85)"
            : "0.8px solid rgba(10, 10, 10, 0.9)",
          boxShadow: saved
            ? "0 3px 12px rgba(227, 30, 36, 0.45)"
            : "0 2px 8px rgba(227, 30, 36, 0.25)",
        }}
      />
      <button
        type="button"
        onClick={onSave}
        aria-label={saved ? `Remove ${post.title || "this article"} from saved` : `Save ${post.title || "this article"} for later`}
        title={saved ? "Remove from saved" : "Save for later"}
        className={cn(
          "relative z-10 flex items-center justify-center rounded-full transition-opacity active:opacity-70 text-white",
          sizes[size] ?? sizes.md,
          className,
        )}
      >
        {saved ? (
          <BookmarkCheck className="w-4 h-4 fill-current" />
        ) : (
          <Bookmark className="w-4 h-4" />
        )}
      </button>
    </span>
  );
}

export function PostCard({ post, variant = "md", className }: PostCardProps) {
  const href = `/news/${encodeURIComponent(post.slug)}`;
  const cat = (post.categories || [])[0];

  if (variant === "hero") {
    return (
      <article className={cn("group relative overflow-hidden border-2 border-ink-950 bg-ink-950 text-white shadow-hard card-hover", className)}>
        <Link href={href} className="block relative">
          <div className="aspect-[16/9] sm:aspect-[16/10] overflow-hidden bg-ink-800">
            <CardCover post={post} coverSize="hero" />
          </div>
          <div className="absolute inset-0 bg-gradient-to-t from-ink-950 via-ink-950/65 to-ink-950/15 pointer-events-none" />
          <div className="absolute inset-0 p-3 sm:p-5 md:p-6 lg:p-7 flex flex-col justify-end gap-2.5 sm:gap-3.5">
            <div className="flex flex-wrap items-center gap-1.5">
              {post.isFeatured ? <Badge variant="news" className="!text-[10px]">Featured</Badge> : null}
              {post.isTrending ? <Badge variant="ink" className="!text-[10px]">Trending</Badge> : null}
              <CategoryRibbons post={post} compact />
            </div>
            <h2 className="font-headline text-2xl sm:text-3xl md:text-4xl lg:text-[40px] uppercase leading-[0.92] tracking-tight line-clamp-6">
              {post.title}
            </h2>
            {post.excerpt ? (
              <p className="text-[12px] sm:text-[13px] text-white/85 leading-relaxed line-clamp-2 sm:line-clamp-3 max-w-3xl">
                {post.excerpt}
              </p>
            ) : null}
            <div className="flex flex-wrap items-center gap-3 justify-between pt-1">
              <PostMeta post={post} dark compact />
              <div className="inline-flex items-center gap-2">
                <ReadCta variant="hero" />
                <CardShareButton post={post} size="lg" dark />
                <CardSaveButton post={post} size="lg" dark />
              </div>
            </div>
          </div>
        </Link>
      </article>
    );
  }

  if (variant === "lg") {
    return (
      <article
        className={cn(
          "group flex flex-col sm:flex-row bg-white border-2 border-ink-950 hover:shadow-hard-sm transition-shadow card-hover overflow-hidden h-full w-full",
          className,
        )}
      >
        <Link
          href={href}
          className="sm:w-[42%] sm:shrink-0 w-full block aspect-[16/10] sm:aspect-[4/3] sm:border-r-2 border-b-2 sm:border-b-0 border-ink-950 overflow-hidden bg-ink-800"
        >
          <CardCover post={post} coverSize="lg" />
        </Link>
        <div className="flex-1 flex flex-col gap-2 sm:gap-2.5 p-3 sm:p-4 h-full w-full min-h-0">
          <div className="flex flex-wrap items-center gap-2">
            {post.isTrending ? <Badge variant="news">Trending</Badge> : null}
            {post.isFeatured ? <Badge variant="ink">Featured</Badge> : null}
            <CategoryRibbons post={post} />
          </div>
          <Link href={href}>
            <h3 className="font-headline text-[15px] sm:text-lg uppercase leading-[1.05] tracking-tight group-hover:text-news transition-colors line-clamp-3 sm:line-clamp-4">
              {post.title}
            </h3>
          </Link>
          {post.excerpt ? (
            <p className="text-[12px] sm:text-[13px] text-ink-800 line-clamp-2 sm:line-clamp-3 leading-relaxed">
              {post.excerpt}
            </p>
          ) : null}
          <PostMeta post={post} compact />
          <div className="pt-1 mt-auto inline-flex items-center gap-2 flex-wrap">
            <Link href={href} className="inline-flex shrink-0">
              <ReadCta variant="lg" />
            </Link>
            <CardShareButton post={post} size="md" />
            <CardSaveButton post={post} size="md" />
          </div>
        </div>
      </article>
    );
  }

  if (variant === "row") {
    return (
      <article className={cn("group flex gap-3 items-stretch bg-white relative h-full w-full", className)}>
        <Link href={href} className="flex-shrink-0 w-20 sm:w-24 aspect-[4/3] border-2 border-ink-950 overflow-hidden bg-ink-800 shadow-hard-sm">
          <CardCover post={post} coverSize="sm" />
        </Link>
        <div className="min-w-0 flex-1 flex flex-col gap-1.5 py-0.5">
          <div className="flex flex-wrap items-center gap-1.5">
            {cat ? (
              <span
                className="ribbon !text-[10px]"
              >
                {cat.name}
              </span>
            ) : null}
            {post.isTrending ? <Badge variant="news" className="!py-0 !px-1.5 !text-[10px]">Trending</Badge> : null}
          </div>
          <Link href={href}>
            <h4 className="font-headline text-[15px] sm:text-base uppercase leading-[1.05] tracking-tight line-clamp-3 group-hover:text-news transition-colors">
              {post.title}
            </h4>
          </Link>
          <div className="flex flex-wrap items-center justify-between gap-2 mt-1">
            <div className="text-[11px] text-ink-600 font-semibold uppercase tracking-wide">
              {formatRelative(post.publishedAt ?? post.createdAt)}
            </div>
            <div className="inline-flex items-center gap-2 shrink-0">
              <Link href={href} className="hover:text-news text-[10px] font-bold uppercase tracking-widest inline-flex items-center gap-1 text-ink-700">
                Read <ArrowRight className="w-3 h-3" />
              </Link>
              <CardShareButton post={post} size="sm" />
              <CardSaveButton post={post} size="sm" />
            </div>
          </div>
        </div>
      </article>
    );
  }

  if (variant === "sm") {
    return (
      <article
        className={cn(
          "group flex flex-col bg-white border-2 border-ink-950 hover:shadow-hard-sm transition-shadow card-hover h-full w-full",
          className,
        )}
      >
        <Link href={href} className="block aspect-[16/10] overflow-hidden bg-ink-800 border-b-2 border-ink-950">
          <CardCover post={post} coverSize="sm" />
        </Link>
        <div className="p-3 sm:p-4 flex flex-col gap-2 sm:gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <CategoryRibbons post={post} />
            {post.isTrending ? <Badge variant="news" className="!py-0">Trending</Badge> : null}
            {post.isFeatured ? <Badge variant="ink" className="!py-0">Featured</Badge> : null}
          </div>
          <Link href={href}>
            <h4 className="font-headline text-sm sm:text-base uppercase leading-[1.05] tracking-tight group-hover:text-news transition-colors">
              {truncate(post.title, 85)}
            </h4>
          </Link>
          {post.excerpt ? (
            <p className="text-[12px] sm:text-sm text-ink-700 line-clamp-2 leading-relaxed">{post.excerpt}</p>
          ) : null}
          <PostMeta post={post} compact />
          <div className="pt-1 mt-auto inline-flex items-center gap-2 flex-wrap">
            <Link href={href} className="inline-flex">
              <ReadCta variant="sm" />
            </Link>
            <CardShareButton post={post} size="sm" />
            <CardSaveButton post={post} size="sm" />
          </div>
        </div>
      </article>
    );
  }

  return (
    <article
      className={cn(
        "group flex flex-col bg-white border-2 border-ink-950 hover:shadow-hard transition-shadow card-hover h-full w-full",
        className,
      )}
    >
      <Link href={href} className="block aspect-[16/10] overflow-hidden bg-ink-800 border-b-2 border-ink-950">
        <CardCover post={post} coverSize="md" />
      </Link>
      <div className="p-2.5 sm:p-3.5 flex flex-col gap-1.5 sm:gap-2.5">
        <div className="flex flex-wrap items-center gap-1.5">
          {post.isFeatured ? <Badge variant="news" className="!text-[10px]">Featured</Badge> : null}
          <CategoryRibbons post={post} compact />
          {post.isTrending ? <Badge variant="ink" className="!py-0 !text-[9px]">Trending</Badge> : null}
        </div>
        <Link href={href}>
          <h3 className="font-headline text-sm sm:text-lg uppercase leading-[1.05] tracking-tight group-hover:text-news transition-colors">
            {post.title}
          </h3>
        </Link>
        {post.excerpt ? (
          <p className="text-[11px] sm:text-[12px] text-ink-800 line-clamp-2 leading-relaxed">{post.excerpt}</p>
        ) : null}
        <PostMeta post={post} compact />
        <div className="pt-0.5 mt-auto inline-flex items-center gap-2 flex-wrap">
          <Link href={href} className="inline-flex">
            <ReadCta variant="md" />
          </Link>
          <CardShareButton post={post} size="md" />
          <CardSaveButton post={post} size="md" />
        </div>
      </div>
    </article>
  );
}

function PostMeta({
  post,
  dark = false,
  compact = false,
}: {
  post: BlogPostSummaryResponse;
  dark?: boolean;
  compact?: boolean;
}) {
  const c = dark ? "text-white/80" : "text-ink-600";
  const authorHref = post.userId ? `/author/${encodeURIComponent(post.userId)}` : null;
  return (
    <div
      className={cn(
        "flex flex-wrap items-center gap-x-3 gap-y-1 text-xs font-semibold uppercase tracking-wide",
        c,
      )}
    >
      <span className={authorHref ? "hover:underline" : ""}>
        {post.authorFirstName} {post.authorLastName}
      </span>
      <span aria-hidden className={dark ? "text-white/40" : "text-ink-400"}>•</span>
      <span>{formatDate(post.publishedAt ?? post.createdAt)}</span>
      <span aria-hidden className={dark ? "text-white/40" : "text-ink-400"}>•</span>
      <span className="inline-flex items-center gap-1">
        <Clock className="w-3.5 h-3.5" />
        {(() => {
          const raw = (post as any).readingTime as number | undefined;
          const minutes = raw && raw > 0 ? Math.max(1, Math.round(raw)) : Math.max(2, 1 + Math.round((post.viewCount || 0) / 250));
          return readingTimeLabel(minutes) || `${minutes} min read`;
        })()}
      </span>
      {!compact ? (
        <>
          {post.viewCount != null ? (
            <>
              <span aria-hidden className={dark ? "text-white/40" : "text-ink-400"}>•</span>
              <span className="inline-flex items-center gap-1">
                <Eye className="w-3.5 h-3.5" />
                {post.viewCount.toLocaleString("en-IN")}
              </span>
            </>
          ) : null}
          {post.likeCount != null ? (
            <>
              <span aria-hidden className={dark ? "text-white/40" : "text-ink-400"}>•</span>
              <span className="inline-flex items-center gap-1">
                <Heart className="w-3.5 h-3.5" />
                {post.likeCount.toLocaleString("en-IN")}
              </span>
            </>
          ) : null}
          {post.commentCount != null ? (
            <>
              <span aria-hidden className={dark ? "text-white/40" : "text-ink-400"}>•</span>
              <span className="inline-flex items-center gap-1">
                <MessageSquare className="w-3.5 h-3.5" />
                {post.commentCount.toLocaleString("en-IN")}
              </span>
            </>
          ) : null}
        </>
      ) : null}
    </div>
  );
}

export function SectionTitle({
  eyebrow,
  title,
  action,
  className,
}: {
  eyebrow?: string;
  title: string;
  action?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex items-end justify-between gap-4 border-b-4 border-ink-950 pb-3", className)}>
      <div className="min-w-0">
        {eyebrow ? (
          <div className="ribbon text-xs mb-2 shadow-hard-sm">{eyebrow}</div>
        ) : null}
        <h2 className="font-headline text-xl sm:text-2xl md:text-3xl uppercase leading-[0.95] tracking-tight underline-accent">{title}</h2>
      </div>
      {action}
    </div>
  );
}
