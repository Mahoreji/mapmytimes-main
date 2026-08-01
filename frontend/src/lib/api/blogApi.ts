import { http, unwrap } from "@/lib/api/client";
import type { APIResponse, PaginatedResponse, PaginationParams, PostStatus, PostType, ID } from "@/types/common";
import type {
  BlogPostResponse,
  BlogPostSummaryResponse,
  CreateBlogPostRequest,
  UpdateBlogPostRequest,
  BlogPostSearchRequest,
  PublishBlogPostRequest,
  CategoryResponse,
  TagResponse,
  PostCommentResponse,
  PostLikeResponse,
  PostMediaResponse,
  CreateCommentRequest,
  UpdateCommentRequest,
  ApproveCommentRequest,
  BlogStatsResponse,
  BlogSettingsResponse,
} from "@/types/blog";
import type { LanguageCode } from "@/lib/i18n/types";

const POSTS = "/api/v1/blog/posts";
const CATS = "/api/v1/blog/categories";
const TAGS = "/api/v1/blog/tags";
const COMMENTS = "/api/v1/blog/comments";
const LIKES = "/api/v1/blog/likes";
const MEDIA = "/api/v1/blog/media";
const SOCIAL = "/api/v1/social";
const SETTINGS = "/api/v1/blog/settings";

function toQuery(params: Record<string, any>) {
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === "") return;
    if (Array.isArray(v)) v.forEach((x) => usp.append(k, String(x)));
    else usp.append(k, String(v));
  });
  const q = usp.toString();
  return q ? `?${q}` : "";
}

export const blogApi = {
  posts: {
    list: (params: PaginationParams & { categoryId?: string; tagId?: string; status?: PostStatus; postType?: PostType; isFeatured?: boolean; isTrending?: boolean; authorId?: string; language?: LanguageCode } = {}) =>
      http.get<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>>(`${POSTS}${toQuery(params)}`).then(unwrap),

    get: (postId: ID) =>
      http.get<APIResponse<BlogPostResponse>>(`${POSTS}/${postId}`).then(unwrap),

    bySlug: (slug: string) =>
      http.get<APIResponse<BlogPostResponse>>(`${POSTS}/slug/${encodeURIComponent(slug)}`).then(unwrap),

    search: (q: string, params: PaginationParams & { language?: LanguageCode; category?: string; tag?: string } = {}) =>
      http
        .get<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>>(
          `${POSTS}/search${toQuery({ keyword: q, ...params })}`,
        )
        .then(unwrap),

    advancedSearch: (body: (BlogPostSearchRequest & PaginationParams & { language?: LanguageCode }) = {}) =>
      http
        .post<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>>(
          `${POSTS}/search${toQuery({ page: body.page, size: body.size, sort: body.sort })}`,
          body,
        )
        .then(unwrap),

    byUser: (userId: ID, params: PaginationParams & { language?: LanguageCode } = {}) =>
      http.get<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>>(`${POSTS}/user/${userId}${toQuery(params)}`).then(unwrap),

    mine: (params: PaginationParams & { status?: PostStatus; language?: LanguageCode } = {}) =>
      http.get<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>>(`${POSTS}/my-posts${toQuery(params)}`).then(unwrap),

    byStatus: (status: PostStatus, params: PaginationParams & { language?: LanguageCode } = {}) =>
      http.get<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>>(`${POSTS}/status${toQuery({ status, ...params })}`).then(unwrap),

    create: (form: CreateBlogPostRequest) => {
      const fd = buildPostForm(form);
      return http
        .post<APIResponse<BlogPostResponse>>(`${POSTS}/create`, fd, {
          headers: { "Content-Type": "multipart/form-data" },
        })
        .then(unwrap);
    },

    update: (postId: ID, form: UpdateBlogPostRequest | Partial<CreateBlogPostRequest>) => {
      if (hasFileParts(form)) {
        const fd = buildPostForm(form);
        return http
          .put<APIResponse<BlogPostResponse>>(`${POSTS}/${postId}`, fd, {
            headers: { "Content-Type": "multipart/form-data" },
          })
          .then(unwrap);
      }
      return http.put<APIResponse<BlogPostResponse>>(`${POSTS}/${postId}`, form).then(unwrap);
    },

    publish: (postId: ID, body: PublishBlogPostRequest = {}) =>
      http.post<APIResponse<BlogPostResponse>>(`${POSTS}/${postId}/publish`, body).then(unwrap),

    unpublish: (postId: ID) =>
      http.post<APIResponse<BlogPostResponse>>(`${POSTS}/${postId}/unpublish`, undefined).then(unwrap),

    delete: (postId: ID) =>
      http.delete<APIResponse<void>>(`${POSTS}/${postId}`).then(unwrap),

    like: (postId: ID) =>
      http.post<APIResponse<PostLikeResponse>>(`${POSTS}/${postId}/like`).then(unwrap),

    unlike: (postId: ID) =>
      http.delete<APIResponse<void>>(`${POSTS}/${postId}/like`).then(unwrap),

    incrementView: (postId: ID) =>
      http.put<APIResponse<void>>(`${POSTS}/${postId}/view`).then(unwrap).catch(() => undefined),

    likesList: (postId: ID) =>
      http.get<APIResponse<PostLikeResponse[]>>(`${POSTS}/${postId}/likes`).then(unwrap),

    myLikes: () =>
      http.get<APIResponse<BlogPostSummaryResponse[]>>(`${POSTS}/my-likes`).then(unwrap),
  },

  categories: {
    list: (params: PaginationParams = {}) =>
      http.get<APIResponse<PaginatedResponse<CategoryResponse> | CategoryResponse[]>>(`${CATS}${toQuery(params)}`).then(unwrap),

    hierarchy: () =>
      http.get<APIResponse<CategoryResponse[]>>(`${CATS}/hierarchy`).then(unwrap),

    get: (id: ID) =>
      http.get<APIResponse<CategoryResponse>>(`${CATS}/${id}`).then(unwrap),

    bySlug: (slug: string) =>
      http.get<APIResponse<CategoryResponse>>(`${CATS}/slug/${encodeURIComponent(slug)}`).then(unwrap),

    create: (body: { name: string; slug?: string; description?: string; parentCategoryId?: string }) =>
      http.post<APIResponse<CategoryResponse>>(`${CATS}`, body).then(unwrap),

    update: (id: ID, body: { name?: string; slug?: string; description?: string; parentCategoryId?: string | null }) =>
      http.put<APIResponse<CategoryResponse>>(`${CATS}/${id}`, body).then(unwrap),

    delete: (id: ID) =>
      http.delete<APIResponse<void>>(`${CATS}/${id}`).then(unwrap),
  },

  tags: {
    list: (params: PaginationParams = {}) =>
      http.get<APIResponse<PaginatedResponse<TagResponse> | TagResponse[]>>(`${TAGS}${toQuery(params)}`).then(unwrap),

    popular: () =>
      http.get<APIResponse<TagResponse[]>>(`${TAGS}/popular`).then(unwrap),

    get: (id: ID) =>
      http.get<APIResponse<TagResponse>>(`${TAGS}/${id}`).then(unwrap),

    bySlug: (slug: string) =>
      http.get<APIResponse<TagResponse>>(`${TAGS}/slug/${encodeURIComponent(slug)}`).then(unwrap),

    create: (body: { name: string; slug?: string; description?: string }) =>
      http.post<APIResponse<TagResponse>>(`${TAGS}`, body).then(unwrap),

    update: (id: ID, body: { name?: string; slug?: string; description?: string }) =>
      http.put<APIResponse<TagResponse>>(`${TAGS}/${id}`, body).then(unwrap),

    delete: (id: ID) =>
      http.delete<APIResponse<void>>(`${TAGS}/${id}`).then(unwrap),
  },

  comments: {
    list: (params: PaginationParams = {}) =>
      http.get<APIResponse<PaginatedResponse<PostCommentResponse>>>(`${COMMENTS}${toQuery(params)}`).then(unwrap),

    get: (id: ID) =>
      http.get<APIResponse<PostCommentResponse>>(`${COMMENTS}/${id}`).then(unwrap),

    byPost: (postId: ID) =>
      http.get<APIResponse<PostCommentResponse[]>>(`${COMMENTS}/post/${postId}`).then(unwrap),

    byPostApproved: (postId: ID) =>
      http.get<APIResponse<PostCommentResponse[]>>(`${COMMENTS}/post/${postId}/approved`).then(unwrap),

    pending: (params: PaginationParams = {}) =>
      http.get<APIResponse<PaginatedResponse<PostCommentResponse>>>(`${COMMENTS}/pending${toQuery(params)}`).then(unwrap),

    byUser: (userId: ID, params: PaginationParams = {}) =>
      http.get<APIResponse<PaginatedResponse<PostCommentResponse>>>(`${COMMENTS}/user/${userId}${toQuery(params)}`).then(unwrap),

    mine: (params: PaginationParams = {}) =>
      http.get<APIResponse<PaginatedResponse<PostCommentResponse>>>(`${COMMENTS}/my-comments${toQuery(params)}`).then(unwrap),

    create: (body: CreateCommentRequest) =>
      http.post<APIResponse<PostCommentResponse>>(`${COMMENTS}`, body).then(unwrap),

    update: (id: ID, body: UpdateCommentRequest) =>
      http.put<APIResponse<PostCommentResponse>>(`${COMMENTS}/${id}`, body).then(unwrap),

    approve: (id: ID, body: ApproveCommentRequest = {}) =>
      http.post<APIResponse<PostCommentResponse>>(`${COMMENTS}/${id}/approve`, body).then(unwrap),

    reject: (id: ID, body: ApproveCommentRequest = {}) =>
      http.post<APIResponse<PostCommentResponse>>(`${COMMENTS}/${id}/reject`, body).then(unwrap),

    delete: (id: ID) =>
      http.delete<APIResponse<void>>(`${COMMENTS}/${id}`).then(unwrap),
  },

  translate: {
    run: async (body: {
      sourceLang: "en" | "hi";
      targetLang: "en" | "hi";
      items: { id: string; text: string }[];
    }) => {
      const res = await fetch("/api/blog/translate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
        cache: "no-store",
      });
      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        const err: any = new Error(
          `Translate failed (${res.status}) ${txt.slice(0, 200)}`,
        );
        err.status = res.status;
        throw err;
      }
      const envelope = await res.json();
      if (envelope && typeof envelope === "object" && "data" in envelope) {
        return envelope.data as {
          sourceLang: string;
          targetLang: string;
          items: { id: string; source: string; translated: string }[];
          disclaimer: string;
          translated: boolean;
        };
      }
      return envelope as unknown as {
        sourceLang: string;
        targetLang: string;
        items: { id: string; source: string; translated: string }[];
        disclaimer: string;
        translated: boolean;
      };
    },
  },

  likes: {
    post: (postId: ID) =>
      http.post<APIResponse<PostLikeResponse>>(`${LIKES}/${postId}`).then(unwrap),

    unlike: (postId: ID) =>
      http.delete<APIResponse<void>>(`${LIKES}/${postId}`).then(unwrap),

    list: (postId: ID) =>
      http.get<APIResponse<PostLikeResponse[]>>(`${LIKES}/post/${postId}`).then(unwrap),

    count: (postId: ID) =>
      http.get<APIResponse<number>>(`${LIKES}/post/${postId}/count`).then(unwrap),

    check: (postId: ID) =>
      http.get<APIResponse<boolean>>(`${LIKES}/post/${postId}/check`).then(unwrap),

    mine: () =>
      http.get<APIResponse<PostLikeResponse[]>>(`${LIKES}/my-likes`).then(unwrap),
  },

  media: {
    upload: (file: File, extras?: { postId?: string; caption?: string; description?: string; subtitle?: string; groupKey?: string }) => {
      const fd = new FormData();
      fd.append("file", file);
      if (extras?.postId) fd.append("postId", extras.postId);
      if (extras?.caption) fd.append("caption", extras.caption);
      if (extras?.description) fd.append("description", extras.description);
      if (extras?.subtitle) fd.append("subtitle", extras.subtitle);
      if (extras?.groupKey) fd.append("groupKey", extras.groupKey);
      return http
        .post<APIResponse<PostMediaResponse>>(`${MEDIA}`, fd, {
          headers: { "Content-Type": "multipart/form-data" },
        })
        .then(unwrap);
    },

    get: (id: ID) =>
      http.get<APIResponse<PostMediaResponse>>(`${MEDIA}/${id}`).then(unwrap),

    byPost: (postId: ID) =>
      http.get<APIResponse<PostMediaResponse[]>>(`${MEDIA}/post/${postId}`).then(unwrap),

    update: (id: ID, extras: Partial<{ file: File; caption: string; description: string; subtitle: string; groupKey: string }>) => {
      const fd = new FormData();
      if (extras.file) fd.append("file", extras.file);
      if (extras.caption) fd.append("caption", extras.caption);
      if (extras.description) fd.append("description", extras.description);
      if (extras.subtitle) fd.append("subtitle", extras.subtitle);
      if (extras.groupKey) fd.append("groupKey", extras.groupKey);
      return http
        .put<APIResponse<PostMediaResponse>>(`${MEDIA}/${id}`, fd, {
          headers: { "Content-Type": "multipart/form-data" },
        })
        .then(unwrap);
    },

    delete: (id: ID) =>
      http.delete<APIResponse<void>>(`${MEDIA}/${id}`).then(unwrap),
  },

  social: {
    feed: (params: PaginationParams = {}) =>
      http.get<APIResponse<PaginatedResponse<BlogPostSummaryResponse> | BlogPostSummaryResponse[]>>(`${SOCIAL}/feed${toQuery(params)}`).then(unwrap),

    explore: (params: PaginationParams = {}) =>
      http.get<APIResponse<PaginatedResponse<BlogPostSummaryResponse> | BlogPostSummaryResponse[]>>(`${SOCIAL}/explore${toQuery(params)}`).then(unwrap),
  },

  settings: {
    list: () =>
      http.get<APIResponse<BlogSettingsResponse[]>>(`${SETTINGS}`).then(unwrap),

    map: () =>
      http.get<APIResponse<Record<string, string>>>(`${SETTINGS}/map`).then(unwrap),

    get: (key: string) =>
      http.get<APIResponse<BlogSettingsResponse>>(`${SETTINGS}/${encodeURIComponent(key)}`).then(unwrap),

    create: (body: { settingKey: string; settingValue: string; description?: string }) =>
      http.post<APIResponse<BlogSettingsResponse>>(`${SETTINGS}`, body).then(unwrap),

    update: (key: string, body: { settingValue: string; description?: string }) =>
      http.put<APIResponse<BlogSettingsResponse>>(`${SETTINGS}/${encodeURIComponent(key)}`, body).then(unwrap),

    delete: (key: string) =>
      http.delete<APIResponse<void>>(`${SETTINGS}/${encodeURIComponent(key)}`).then(unwrap),

    stats: () =>
      http.get<APIResponse<BlogStatsResponse>>(`${SETTINGS}/stats`).then(unwrap),
  },
};

function hasFileParts(form: any): boolean {
  if (!form) return false;
  return (
    (Array.isArray(form.mediaFiles) && form.mediaFiles.length > 0) ||
    (Array.isArray(form.groupedMediaFiles) && form.groupedMediaFiles.length > 0)
  );
}

function buildPostForm(form: Record<string, any>): FormData {
  const fd = new FormData();
  const jsonFields = [
    "featuredImage",
    "contentBlocks",
    "tableOfContents",
    "travelMeta",
    "seo",
    "categories",
    "tags",
    "mediaCaptions",
    "mediaDescriptions",
    "mediaSubtitles",
    "mediaGroups",
  ];
  Object.entries(form).forEach(([k, v]) => {
    if (v === undefined || v === null) return;
    if (k === "mediaFiles" && Array.isArray(v)) {
      (v as File[]).forEach((f) => fd.append("mediaFiles", f));
      return;
    }
    if (k === "groupedMediaFiles" && Array.isArray(v)) {
      (v as File[]).forEach((f) => fd.append("groupedMediaFiles", f));
      return;
    }
    if (jsonFields.includes(k) && typeof v === "object") {
      fd.append(k, JSON.stringify(v));
      return;
    }
    fd.append(k, typeof v === "boolean" ? (v ? "true" : "false") : String(v));
  });
  return fd;
}
