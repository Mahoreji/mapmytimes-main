"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { PageHeader, Card } from "@/components/dashboard/Panels";
import { blogApi } from "@/lib/api/blogApi";
import type {
  BlogPostResponse,
  CategoryResponse,
  TagResponse,
  CreateBlogPostRequest,
  PostMediaResponse,
} from "@/types/blog";
import type { PostStatus, PostType, Visibility } from "@/types/common";
import { Button } from "@/components/ui/Button";
import { Input, Textarea, Checkbox } from "@/components/ui/Input";
import { getApiError } from "@/lib/api/client";
import { useAuth } from "@/lib/auth/AuthProvider";
import {
  Eye,
  Upload,
  Calendar,
  Search,
  Plus,
  Trash2,
  Save,
  Send,
  Hash,
  Layers,
  X,
  Play,
} from "lucide-react";
import { cn, formatDate, slugify, truncate } from "@/lib/utils";
import { VideoEmbed } from "@/components/posts/VideoEmbed";
import {
  getYouTubeVideoId,
  ytThumbnails,
} from "@/lib/youtube";

export default function EditPostPage() {
  const params = useParams<{ id?: string }>();
  const router = useRouter();
  const id = typeof params?.id === "string" && params.id !== "new" ? params.id : null;
  const isNew = !id;
  const auth = useAuth();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ kind: "ok" | "err"; text: string } | null>(null);

  const [title, setTitle] = useState("");
  const [slug, setSlug] = useState("");
  const [excerpt, setExcerpt] = useState("");
  const [content, setContent] = useState("");
  const [blocks, setBlocks] = useState<any[] | null>(null);
  const [readingTime, setReadingTime] = useState<number | "">("");
  const [language, setLanguage] = useState("en");
  const [visibility, setVisibility] = useState<Visibility>("PUBLIC");
  const [postType, setPostType] = useState<PostType>("BLOG");
  const [isFeatured, setIsFeatured] = useState(false);
  const [isTrending, setIsTrending] = useState(false);
  const [allowComments, setAllowComments] = useState(true);
  const [allowLikes, setAllowLikes] = useState(true);
  const [scheduledAt, setScheduledAt] = useState("");

  const [featuredImageUrl, setFeaturedImageUrl] = useState("");
  const [featuredImageFile, setFeaturedImageFile] = useState<File | null>(null);
  const [featuredImageAlt, setFeaturedImageAlt] = useState("");
  const [featuredImageCaption, setFeaturedImageCaption] = useState("");
  const [primaryVideoUrl, setPrimaryVideoUrl] = useState("");
  const [ytUrlInput, setYtUrlInput] = useState("");


  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [tags, setTags] = useState<TagResponse[]>([]);
  const [selectedCats, setSelectedCats] = useState<string[]>([]);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [newTag, setNewTag] = useState("");
  const [newCat, setNewCat] = useState("");

  const [mediaFiles, setMediaFiles] = useState<File[]>([]);
  const [existingMedia, setExistingMedia] = useState<PostMediaResponse[]>([]);

  const [seoTitle, setSeoTitle] = useState("");
  const [seoDesc, setSeoDesc] = useState("");
  const [seoKeywords, setSeoKeywords] = useState("");

  useEffect(() => {
    let active = true;
    Promise.all([
      blogApi.categories.list({ size: 100 }).catch(() => [] as any),
      blogApi.tags.list({ size: 100 }).catch(() => [] as any),
    ]).then(([cats, tgs]) => {
      if (!active) return;
      const cArr = Array.isArray(cats)
        ? cats
        : ((cats as any).content ?? []);
      const tArr = Array.isArray(tgs)
        ? tgs
        : ((tgs as any).content ?? []);
      setCategories(cArr);
      setTags(tArr);
    });
    if (isNew) {
      setLoading(false);
      return;
    }
    blogApi.posts
      .get(id!)
      .then((p) => {
        if (!active) return;
        setTitle(p.title);
        setSlug(p.slug);
        setExcerpt(p.excerpt ?? "");
        setContent(p.content ?? "");
        setBlocks(p.contentBlocks ?? null);
        setReadingTime(p.readingTime ?? "");
        setLanguage(p.language ?? "en");
        setVisibility(p.visibility);
        setPostType(p.postType ?? "BLOG");
        setIsFeatured(p.isFeatured);
        setIsTrending(p.isTrending);
        setAllowComments(p.allowComments);
        setAllowLikes(p.allowLikes);
        setScheduledAt(p.scheduledAt ? p.scheduledAt.slice(0, 16) : "");
        setFeaturedImageUrl(
          typeof p.featuredImage === "string"
            ? p.featuredImage
            : (p.featuredImage as any)?.url ?? p.featuredImageUrl ?? "",
        );
        setFeaturedImageAlt((p.featuredImage as any)?.alt ?? "");
        setFeaturedImageCaption((p.featuredImage as any)?.caption ?? "");
        setPrimaryVideoUrl((p as any).primaryVideoUrl ?? "");
        {
          const directId = (p as any).youtubeVideoId || (p as any).videoId;
          const candidates: string[] = [];
          if (directId) candidates.push(String(directId));
          if ((p as any).primaryVideoUrl) candidates.push(String((p as any).primaryVideoUrl));
          if (p.content) candidates.push(String(p.content));
          let vid: string | null = null;
          for (const s of candidates) {
            vid = getYouTubeVideoId(s);
            if (vid) break;
          }
          if (vid) setYtUrlInput(vid);
          else if ((p as any).primaryVideoUrl) setYtUrlInput(String((p as any).primaryVideoUrl));
        }
        setSelectedCats(p.categories.map((c) => c.id));
        setSelectedTags(p.tags.map((t) => t.id));
        setExistingMedia(p.media ?? []);
        setSeoTitle(p.seo?.metaTitle ?? "");
        setSeoDesc(p.seo?.metaDescription ?? "");
        setSeoKeywords((p.seo?.keywords ?? []).join(", "));
        setLoading(false);
      })
      .catch(() => {
        if (!active) return;
        setLoading(false);
        setMessage({ kind: "err", text: "Could not load this post." });
      });
    return () => {
      active = false;
    };
  }, [id, isNew]);

  const canPublish = useMemo(
    () => title.trim().length > 0 && (content.trim().length > 0 || (postType === "STORY" && !!getYouTubeVideoId(ytUrlInput || primaryVideoUrl))),
    [title, content, postType, ytUrlInput, primaryVideoUrl],
  );

  const youtubeVid = useMemo<string | null>(() => {
    const direct = (content as any)?.youtubeVideoId || (content as any)?.videoId;
    if (direct) return direct as string;
    return getYouTubeVideoId(ytUrlInput) ||
      getYouTubeVideoId(primaryVideoUrl) ||
      getYouTubeVideoId(content);
  }, [ytUrlInput, primaryVideoUrl, content]);

  const youtubeThumbs = useMemo(() => ytThumbnails(youtubeVid), [youtubeVid]);

  function updateYoutubeInput(next: string) {
    setYtUrlInput(next);
    const videoId = getYouTubeVideoId(next);
    const watchUrl = videoId ? `https://www.youtube.com/watch?v=${videoId}` : next;
    setPrimaryVideoUrl(watchUrl);
    if (postType === "STORY") {
      // Option A: store the YouTube URL in the content field so extractShortMeta can parse it
      // when backend doesn't yet have a dedicated youtubeVideoId field.
      // Keep any existing caption/title text appended after the URL if the user wrote one.
      const rest = (content || "")
        .replace(/\b(https?:\/\/)?(www\.|m\.)?(youtube\.com\/(watch\?v=|embed\/|shorts\/|v\/)[a-zA-Z0-9_-]+|youtu\.be\/[a-zA-Z0-9_-]+)\S*/gi, " ")
        .replace(/\s+/g, " ")
        .trim();
      if (videoId) {
        const combined = [
          `https://www.youtube.com/watch?v=${videoId}`,
          title.trim() || rest,
        ].filter(Boolean).join("\n\n");
        setContent(combined);
      } else if (!next.trim() && !rest) {
        // leave as-is
      }
    }
  }

  function flash(kind: "ok" | "err", text: string) {
    setMessage({ kind, text });
    setTimeout(() => setMessage(null), 5000);
  }

  function buildPayload(): CreateBlogPostRequest {
    const user = auth.user;
    return {
      title: title.trim(),
      slug: slug.trim() || slugify(title),
      excerpt: excerpt.trim() || undefined,
      content: content.trim(),
      contentBlocks: blocks ?? undefined,
      // P0-2: readingTime REMOVED from client payload. Backend auto-computes from word count.
      language,
      visibility,
      postType,
      isFeatured,
      isTrending,
      allowComments,
      allowLikes,
      scheduledAt: scheduledAt ? new Date(scheduledAt).toISOString() : undefined,
      featuredImage: featuredImageUrl
        ? JSON.stringify({
            url: featuredImageFile ? undefined : featuredImageUrl,
            alt: featuredImageAlt,
            caption: featuredImageCaption,
          })
        : undefined,
      primaryVideoUrl: primaryVideoUrl.trim() || undefined,
      categories: selectedCats,
      tags: selectedTags,
      mediaFiles: mediaFiles.length > 0 ? mediaFiles : undefined,
      seo: seoTitle || seoDesc || seoKeywords
        ? {
            metaTitle: seoTitle || undefined,
            metaDescription: seoDesc || undefined,
            keywords: seoKeywords
              .split(",")
              .map((s) => s.trim())
              .filter(Boolean),
          }
        : undefined,
      userId: user?.userId as string | undefined,
      authorEmail: user?.email,
      authorFirstName: user?.firstName,
      authorLastName: user?.lastName,
      authorAvatarUrl: user?.profileImageUrl,
    };
  }

  async function save(andPublish: boolean, andSchedule = false) {
    if (!canPublish) {
      flash("err", "Title and body are required.");
      return;
    }
    setSaving(true);
    try {
      const payload = buildPayload();
      let saved: BlogPostResponse;
      if (isNew) {
        saved = await blogApi.posts.create(payload);
      } else {
        saved = await blogApi.posts.update(id!, payload);
      }
      if (andSchedule && saved && scheduledAt) {
        // Schedule is stored via scheduledAt field already in the payload above.
      }
      if (andPublish && saved) {
        if (scheduledAt && andSchedule) {
          flash("ok", "Story scheduled.");
        } else {
          saved = await blogApi.posts.publish(saved.id);
          flash("ok", "Published 🎉");
        }
      } else {
        flash("ok", "Saved.");
      }
      if (saved?.id && isNew) {
        router.replace(`/dashboard/posts/${encodeURIComponent(saved.id)}`);
      }
    } catch (e) {
      flash("err", getApiError(e) || "Could not save your changes.");
    } finally {
      setSaving(false);
    }
  }

  async function removeMedia(id: string) {
    try {
      await blogApi.media.delete(id);
      setExistingMedia((list) => list.filter((m) => m.id !== id));
    } catch (e) {
      flash("err", getApiError(e));
    }
  }

  async function addQuickCategory() {
    const name = newCat.trim();
    if (!name) return;
    try {
      const c = await blogApi.categories.create({ name, slug: slugify(name) });
      setCategories((list) => [...list, c]);
      setSelectedCats((ids) => [...ids, c.id]);
      setNewCat("");
    } catch (e) {
      flash("err", getApiError(e));
    }
  }

  async function addQuickTag() {
    const name = newTag.trim();
    if (!name) return;
    try {
      const t = await blogApi.tags.create({ name, slug: slugify(name) });
      setTags((list) => [...list, t]);
      setSelectedTags((ids) => [...ids, t.id]);
      setNewTag("");
    } catch (e) {
      flash("err", getApiError(e));
    }
  }

  function previewLink(): string | undefined {
    return slug ? `/news/${encodeURIComponent(slug)}` : undefined;
  }

  if (loading) {
    return (
      <Card>
        <div className="animate-pulse space-y-4">
          <div className="h-8 w-2/3 bg-ink-900/10" />
          <div className="h-10 w-full bg-ink-900/10" />
          <div className="h-64 w-full bg-ink-900/10" />
        </div>
      </Card>
    );
  }

  return (
    <>
      <PageHeader
        eyebrow={isNew ? "New story" : "Edit story"}
        title={isNew ? "Write a new story" : "Edit your story"}
        description="Compose, preview, schedule, and publish journalism of integrity — all in one place."
        action={
          <div className="flex flex-wrap gap-2">
            {previewLink() ? (
              <a href={previewLink()} target="_blank" rel="noreferrer">
                <Button variant="outline" size="sm">
                  <Eye className="h-4 w-4" />
                  Preview
                </Button>
              </a>
            ) : null}
            <Button
              variant="outline"
              size="sm"
              onClick={() => save(false)}
              disabled={saving}
            >
              <Save className="h-4 w-4" />
              {saving ? "Saving…" : "Save draft"}
            </Button>
            <Button
              variant="news"
              size="sm"
              onClick={() => save(true, !!scheduledAt)}
              disabled={saving || !canPublish}
            >
              {scheduledAt ? (
                <><Calendar className="h-4 w-4" /> {saving ? "Scheduling…" : "Schedule"}</>
              ) : (
                <><Send className="h-4 w-4" /> {saving ? "Publishing…" : "Publish"}</>
              )}
            </Button>
          </div>
        }
      />

      {message ? (
        <div
          className={cn(
            "border-2 p-4 text-sm font-semibold flex items-start gap-3",
            message.kind === "ok"
              ? "border-ink-950 bg-ink-950 text-white"
              : "border-news bg-news-50 text-news-700",
          )}
        >
          <span aria-hidden>{message.kind === "ok" ? "✅" : "⚠️"}</span>
          <div>{message.text}</div>
        </div>
      ) : null}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <Card className="space-y-4">
            <Input
              label="Headline"
              value={title}
              onChange={(e) => {
                setTitle(e.target.value);
                if (!slug) setSlug(slugify(e.target.value));
              }}
              placeholder="Your most important sentence. Be bold, be precise."
              className="!h-14 !text-xl !font-bold"
            />
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                label="Slug"
                value={slug}
                onChange={(e) => setSlug(slugify(e.target.value))}
                placeholder="your-headline-here"
                hint="Used in the article URL."
              />
              <Input
                label="Reading time (minutes)"
                type="number"
                min={1}
                value={readingTime}
                readOnly
                disabled
                hint="Auto-calculated from article word count (≈200 wpm). Recomputed whenever you save the body."
                className="!opacity-90 [&>input]:!bg-ink-900/5 [&>input]:!cursor-not-allowed"
                trailingIcon={
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-news text-white text-[9px] font-black uppercase tracking-widest mr-3">
                    AUTO
                  </span>
                }
              />
            </div>
            <Textarea
              label="Deck / Excerpt"
              value={excerpt}
              onChange={(e) => setExcerpt(e.target.value)}
              placeholder="One or two sentences. Used on listings and social cards."
            />
            <Textarea
              label="Body (Markdown / HTML / plain text)"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Begin your story. You can use HTML or Markdown. Paragraphs separated by blank lines render automatically."
              className="!min-h-[420px] !text-base leading-8"
            />
            <p className="text-xs text-ink-600 font-semibold uppercase tracking-widest">
              Tip: paste HTML, or write plain paragraphs. Headings, blockquotes, lists, and images render on the public site.
            </p>
          </Card>

          <Card className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <div className="ribbon text-xs mb-2">Featured image</div>
                <h3 className="font-headline text-xl uppercase leading-none">Cover & media gallery</h3>
              </div>
              <Layers className="h-5 w-5 text-ink-600" />
            </div>

            <div className="border-2 border-dashed border-ink-950 p-5 flex flex-col sm:flex-row items-start sm:items-center gap-4">
              <div className="h-32 w-52 bg-ink-900/10 border-2 border-ink-950 flex-shrink-0 overflow-hidden">
                {featuredImageUrl || featuredImageFile ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={
                      featuredImageFile
                        ? URL.createObjectURL(featuredImageFile)
                        : featuredImageUrl
                    }
                    alt=""
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="h-full w-full flex items-center justify-center text-ink-600">
                    <Upload className="h-6 w-6" />
                  </div>
                )}
              </div>
              <div className="flex-1 space-y-3 w-full">
                <Input
                  type="file"
                  accept="image/*"
                  onChange={(e) =>
                    setFeaturedImageFile(e.target.files?.[0] ?? null)
                  }
                  label="Upload cover image"
                />
                <Input
                  label="Or cover image URL"
                  value={featuredImageUrl}
                  onChange={(e) => setFeaturedImageUrl(e.target.value)}
                  placeholder="https://…"
                />
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <Input
                    label="Alt text"
                    value={featuredImageAlt}
                    onChange={(e) => setFeaturedImageAlt(e.target.value)}
                    placeholder="Describe the image for screen readers"
                  />
                  <Input
                    label="Caption"
                    value={featuredImageCaption}
                    onChange={(e) => setFeaturedImageCaption(e.target.value)}
                  />
                </div>
              </div>
            </div>

            <div className="border-2 border-ink-950/10 p-4">
              <div className="flex items-center justify-between mb-3">
                <div className="text-xs font-bold uppercase tracking-widest">
                  Additional media gallery
                </div>
                <span className="text-xs text-ink-600">
                  {mediaFiles.length} file(s) queued · {existingMedia.length} existing
                </span>
              </div>
              <Input
                type="file"
                multiple
                accept="image/*,video/*"
                onChange={(e) => setMediaFiles(Array.from(e.target.files ?? []))}
                label="Upload media"
              />
              {(mediaFiles.length > 0 || existingMedia.length > 0) ? (
                <div className="mt-4 grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-6 gap-3">
                  {mediaFiles.map((f, i) => (
                    <div key={`new-${i}`} className="relative aspect-square border-2 border-ink-950 bg-ink-800">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={URL.createObjectURL(f)}
                        alt=""
                        className="h-full w-full object-cover"
                      />
                      <span className="absolute top-1 left-1 bg-ink-950 text-white text-[10px] px-1.5 py-0.5 uppercase tracking-widest">
                        New
                      </span>
                      <button
                        type="button"
                        className="absolute top-1 right-1 h-6 w-6 bg-news text-white inline-flex items-center justify-center"
                        onClick={() =>
                          setMediaFiles((list) => list.filter((_, idx) => idx !== i))
                        }
                      >
                        <X className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  ))}
                  {existingMedia.map((m) => (
                    <div key={m.id} className="relative aspect-square border-2 border-ink-950 bg-ink-800">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img src={m.url} alt={m.caption ?? ""} className="h-full w-full object-cover" />
                      <button
                        type="button"
                        onClick={() => removeMedia(m.id)}
                        className="absolute top-1 right-1 h-6 w-6 bg-news text-white inline-flex items-center justify-center"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
          </Card>

          <Card className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <div className="ribbon text-xs mb-2">Embed</div>
                <h3 className="font-headline text-lg uppercase leading-none">
                  Primary Video News
                </h3>
                <p className="mt-1.5 text-[11px] text-ink-600 font-medium">
                  Paste YouTube / YouTube Shorts / Instagram Reels / Vimeo URL.
                </p>
              </div>
              <Play className="h-5 w-5 text-news fill-news/10" />
            </div>

            <div className="grid grid-cols-1 gap-3">
              <Input
                label="Video URL (YouTube / Instagram / Vimeo)"
                value={primaryVideoUrl}
                onChange={(e) => setPrimaryVideoUrl(e.target.value)}
                placeholder="https://www.youtube.com/watch?v=… or https://www.instagram.com/reel/…"
                className="!font-mono"
              />
              {primaryVideoUrl ? (
                <VideoEmbed
                  url={primaryVideoUrl}
                  aspect="16:9"
                  showTitle={title || undefined}
                />
              ) : null}
              <ul className="text-[10px] text-ink-600 font-semibold uppercase tracking-widest space-y-1 border-t border-ink-950/10 pt-3">
                <li>• YouTube: https://www.youtube.com/watch?v=abcd123 or https://youtu.be/abcd123</li>
                <li>• Shorts: https://www.youtube.com/shorts/abcd123</li>
                <li>• Instagram: https://www.instagram.com/reel/XYZ123/ or /p/XYZ123/</li>
                <li>• Vimeo: https://vimeo.com/123456789</li>
                <li className="text-news">• Click-to-load privacy first: iframe only loads when user taps play.</li>
              </ul>
            </div>
          </Card>
        </div>

        <div className="lg:col-span-1 space-y-6">
          <Card className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <div className="ribbon text-xs mb-2">Publishing</div>
                <h3 className="font-headline text-lg uppercase leading-none">Controls</h3>
              </div>
              <Calendar className="h-4 w-4 text-ink-600" />
            </div>
            <div className="space-y-3">
              <div>
                <div className="text-[11px] font-bold uppercase tracking-widest text-ink-700 mb-1.5">
                  Status
                </div>
                <div className="grid grid-cols-3 gap-2">
                  {(["DRAFT", "SCHEDULED", "PUBLISHED"] as PostStatus[]).map((s) => (
                    <div
                      key={s}
                      className={cn(
                        "px-2 py-2 border-2 text-center text-[10px] font-bold uppercase tracking-widest",
                        s === "DRAFT" && !scheduledAt
                          ? "bg-ink-950 text-white border-ink-950"
                          : s === "SCHEDULED" && scheduledAt
                            ? "bg-news text-white border-news"
                            : s === "PUBLISHED" && !scheduledAt && !isNew
                              ? "bg-ink-950 text-white border-ink-950"
                              : "bg-white border-ink-950/20 text-ink-700",
                      )}
                    >
                      {s}
                    </div>
                  ))}
                </div>
              </div>
              <Input
                label="Schedule publish (optional)"
                type="datetime-local"
                value={scheduledAt}
                onChange={(e) => setScheduledAt(e.target.value)}
              />
              <div>
                <div className="text-[11px] font-bold uppercase tracking-widest text-ink-700 mb-1.5">
                  Post type
                </div>
                <div className="grid grid-cols-3 gap-2">
                  {([
                    { v: "BLOG", label: "Article", hint: "News story, long-form, opinion" },
                    { v: "SOCIAL", label: "Social", hint: "Quick posts, social cards" },
                    { v: "STORY", label: "Short", hint: "YouTube Shorts / 9:16 video" },
                  ] as { v: PostType; label: string; hint?: string }[]).map((t) => (
                    <button
                      key={t.v}
                      type="button"
                      onClick={() => setPostType(t.v)}
                      className={cn(
                        "flex flex-col items-center justify-center gap-0.5 py-2 border-2 transition-colors",
                        postType === t.v
                          ? "bg-ink-950 text-white border-ink-950"
                          : "bg-white text-ink-800 border-ink-950/20 hover:border-ink-950",
                      )}
                    >
                      <span className="text-[11px] font-black uppercase tracking-widest leading-none">
                        {t.label}
                      </span>
                      {t.hint && postType === t.v && (
                        <span className="mt-0.5 text-[9px] font-semibold uppercase tracking-widest opacity-80 leading-tight px-2 text-center">
                          {t.hint}
                        </span>
                      )}
                    </button>
                  ))}
                </div>
              </div>

              {postType === "STORY" && (
                <div className="pt-4 mt-2 border-t border-ink-950/10 space-y-3">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="text-[11px] font-bold uppercase tracking-widest text-ink-700">
                        YouTube Short
                      </div>
                      <div className="text-[11px] text-ink-500 mt-0.5">
                        Paste a YouTube link or the 11-character video ID.
                        <span className="hidden sm:inline"> Supports <code className="px-1 bg-ink-950/5 rounded text-[10px]">youtu.be/X</code>, <code className="px-1 bg-ink-950/5 rounded text-[10px]">/watch?v=</code>, <code className="px-1 bg-ink-950/5 rounded text-[10px]">/shorts/</code>, <code className="px-1 bg-ink-950/5 rounded text-[10px]">/embed/</code>.</span>
                      </div>
                    </div>
                    <Link
                      href={`/shorts`}
                      target="_blank"
                      className="text-[11px] font-bold uppercase tracking-widest text-news hover:text-news/80 inline-flex items-center gap-1 shrink-0"
                    >
                      Preview feed <Play className="h-3.5 w-3.5" />
                    </Link>
                  </div>
                  <Input
                    label="YouTube URL or video ID"
                    placeholder="https://www.youtube.com/watch?v=XXXXXXXXXXX or just the 11-character ID"
                    value={ytUrlInput}
                    onChange={(e) => updateYoutubeInput(e.target.value)}
                  />
                  <div className="grid grid-cols-[150px,1fr] gap-3 items-start">
                    <div className="relative aspect-[9/16] overflow-hidden rounded-lg ring-1 ring-ink-950/10 bg-black">
                      {youtubeVid && (youtubeThumbs.maxres || youtubeThumbs.best) ? (
                        <img
                          src={youtubeThumbs.maxres || youtubeThumbs.best}
                          alt=""
                          className="absolute inset-0 h-full w-full object-cover"
                          onError={(e: any) => {
                            const el = e.currentTarget;
                            if (el && youtubeThumbs.best && el.src !== youtubeThumbs.best) {
                              el.src = youtubeThumbs.best;
                            } else {
                              el.style.display = "none";
                            }
                          }}
                        />
                      ) : (
                        <div className="absolute inset-0 flex flex-col items-center justify-center text-white/70 text-center px-3">
                          <Play className="h-8 w-8 mb-1 text-white/50" />
                          <span className="text-[10px] font-bold uppercase tracking-widest">
                            Thumbnail preview
                          </span>
                        </div>
                      )}
                      {youtubeVid && (
                        <div className="absolute bottom-1.5 right-1.5 rounded bg-black/80 px-1.5 py-0.5 text-[9px] font-black uppercase tracking-widest text-white/90">
                          9:16
                        </div>
                      )}
                    </div>
                    <div className="flex flex-col gap-2">
                      {youtubeVid ? (
                        <>
                          <div className="flex items-center gap-2">
                            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 text-emerald-700 text-[10px] font-black uppercase tracking-widest px-2 py-1 ring-1 ring-emerald-500/20">
                              ✓ Video ID found
                            </span>
                            <code className="text-[11px] font-mono bg-ink-950/5 rounded px-1.5 py-0.5 text-ink-700 break-all">
                              {youtubeVid}
                            </code>
                          </div>
                          <div className="text-[11px] text-ink-500 leading-relaxed">
                            Title field becomes the Short caption shown on the video overlay.
                            Categories + tags are still used for homepage placement and the /shorts feed list page.
                          </div>
                          <div className="flex flex-wrap gap-2 text-[10px] font-semibold text-ink-500">
                            <span className="inline-flex items-center gap-1 rounded-full bg-ink-950/5 px-2 py-0.5">
                              Uses existing PUT /…/view for view counting
                            </span>
                            <span className="inline-flex items-center gap-1 rounded-full bg-ink-950/5 px-2 py-0.5">
                              POST /…/like · DELETE /…/like
                            </span>
                            <span className="inline-flex items-center gap-1 rounded-full bg-ink-950/5 px-2 py-0.5">
                              POST /blog/comments
                            </span>
                          </div>
                        </>
                      ) : (
                        <div className="rounded-lg border border-dashed border-ink-950/15 p-3 text-[11px] text-ink-500 leading-relaxed">
                          Paste a YouTube Short URL (or just the 11-letter video ID) to see a preview.
                          <div className="mt-2 flex flex-col gap-1 text-[10px] font-semibold">
                            <span>
                              Example short link:
                              <code className="ml-1 px-1 bg-ink-950/5 rounded text-ink-700">https://youtube.com/shorts/dQw4w9WgXcQ</code>
                            </span>
                            <span>
                              Example ID only:
                              <code className="ml-1 px-1 bg-ink-950/5 rounded text-ink-700">dQw4w9WgXcQ</code>
                            </span>
                          </div>
                        </div>
                      )}
                      {truncate(title, 90) && youtubeVid && (
                        <div className="rounded-lg bg-black p-3 text-[12px] font-black text-white leading-snug line-clamp-3 shadow-inner">
                          🗞️ {truncate(title, 160)}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}
              <div>
                <div className="text-[11px] font-bold uppercase tracking-widest text-ink-700 mb-1.5">
                  Visibility
                </div>
                <div className="grid grid-cols-3 gap-2">
                  {(["PUBLIC", "UNLISTED", "PRIVATE"] as Visibility[]).map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setVisibility(t)}
                      className={cn(
                        "h-9 text-[10px] font-bold uppercase tracking-widest border-2 transition-colors",
                        visibility === t
                          ? "bg-ink-950 text-white border-ink-950"
                          : "bg-white text-ink-800 border-ink-950/20 hover:border-ink-950",
                      )}
                    >
                      {t}
                    </button>
                  ))}
                </div>
              </div>
              <div className="pt-2 space-y-2 border-t border-ink-950/10">
                <Checkbox
                  label="Mark as Featured (hero/top shelf)."
                  checked={isFeatured}
                  onChange={(e) => setIsFeatured(e.target.checked)}
                />
                <Checkbox
                  label="Mark as Trending."
                  checked={isTrending}
                  onChange={(e) => setIsTrending(e.target.checked)}
                />
                <Checkbox
                  label="Allow comments (moderated)."
                  checked={allowComments}
                  onChange={(e) => setAllowComments(e.target.checked)}
                />
                <Checkbox
                  label="Allow likes."
                  checked={allowLikes}
                  onChange={(e) => setAllowLikes(e.target.checked)}
                />
              </div>
            </div>
          </Card>

          <Card className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <div className="ribbon text-xs mb-2">Taxonomy</div>
                <h3 className="font-headline text-lg uppercase leading-none">Sections & tags</h3>
              </div>
              <Hash className="h-4 w-4 text-ink-600" />
            </div>
            <div className="space-y-3">
              <div className="border-2 border-ink-950/10 p-3 max-h-52 overflow-y-auto space-y-1">
                {categories.map((c) => (
                  <label
                    key={c.id}
                    className="flex items-center gap-2 text-sm py-1 cursor-pointer hover:bg-ink-900/5 px-1"
                  >
                    <input
                      type="checkbox"
                      checked={selectedCats.includes(c.id)}
                      onChange={(e) =>
                        setSelectedCats((ids) =>
                          e.target.checked
                            ? [...ids, c.id]
                            : ids.filter((x) => x !== c.id),
                        )
                      }
                      className="accent-news"
                    />
                    <span className="flex-1 truncate">{c.name}</span>
                    {c.postCount != null ? (
                      <span className="text-[10px] text-ink-500 font-bold">{c.postCount}</span>
                    ) : null}
                  </label>
                ))}
                {categories.length === 0 ? (
                  <p className="text-xs text-ink-600">No categories created yet.</p>
                ) : null}
              </div>
              <div className="flex gap-2">
                <Input
                  value={newCat}
                  onChange={(e) => setNewCat(e.target.value)}
                  placeholder="New section"
                  className="!h-9"
                />
                <Button variant="outline" size="sm" onClick={addQuickCategory}>
                  <Plus className="h-4 w-4" />
                  Add
                </Button>
              </div>

              <div className="border-2 border-ink-950/10 p-3 max-h-52 overflow-y-auto flex flex-wrap gap-2">
                {tags.map((t) => {
                  const on = selectedTags.includes(t.id);
                  return (
                    <button
                      key={t.id}
                      type="button"
                      onClick={() =>
                        setSelectedTags((ids) =>
                          on ? ids.filter((x) => x !== t.id) : [...ids, t.id],
                        )
                      }
                      className={cn(
                        "px-2 py-1 text-[11px] font-bold uppercase tracking-widest border-2",
                        on
                          ? "bg-news text-white border-news"
                          : "bg-white text-ink-800 border-ink-950/20 hover:border-ink-950",
                      )}
                    >
                      #{t.name}
                    </button>
                  );
                })}
                {tags.length === 0 ? (
                  <p className="text-xs text-ink-600">No tags yet.</p>
                ) : null}
              </div>
              <div className="flex gap-2">
                <Input
                  value={newTag}
                  onChange={(e) => setNewTag(e.target.value)}
                  placeholder="New tag"
                  className="!h-9"
                />
                <Button variant="outline" size="sm" onClick={addQuickTag}>
                  <Plus className="h-4 w-4" />
                  Add
                </Button>
              </div>
            </div>
          </Card>

          <Card className="space-y-4">
            <div>
              <div className="ribbon text-xs mb-2">SEO</div>
              <h3 className="font-headline text-lg uppercase leading-none">Search & share</h3>
            </div>
            <Input
              label="Meta title"
              value={seoTitle}
              onChange={(e) => setSeoTitle(e.target.value)}
              placeholder="Defaults to headline."
            />
            <Textarea
              label="Meta description"
              value={seoDesc}
              onChange={(e) => setSeoDesc(e.target.value)}
              placeholder="Shown in search results and link previews."
            />
            <Input
              label="Keywords (comma-separated)"
              value={seoKeywords}
              onChange={(e) => setSeoKeywords(e.target.value)}
              placeholder="india, election, 2026"
            />
            <div className="border-2 border-ink-950/10 p-3 bg-ink-900/5">
              <div className="text-[11px] uppercase tracking-[0.2em] font-bold text-ink-700">
                Preview (approx.)
              </div>
              <div className="mt-2 text-sm font-bold line-clamp-1">
                {seoTitle || title || "Untitled story"}
              </div>
              <div className="mt-1 text-xs text-ink-600 line-clamp-2">
                {seoDesc || excerpt || "Your MapMyTimes story."}
              </div>
            </div>
          </Card>

          <div className="flex items-center gap-2">
            <Link href="/dashboard/posts">
              <Button variant="outline" size="sm">
                ← Back to my posts
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </>
  );
}
