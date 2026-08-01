import type { ID, PostStatus, PostType, Visibility } from "./common";

export interface FeaturedImage {
  url: string;
  alt?: string;
  caption?: string;
  width?: number;
  height?: number;
}

export interface SEOData {
  metaTitle?: string;
  metaDescription?: string;
  keywords?: string[];
  ogImage?: string;
  canonicalUrl?: string;
  noIndex?: boolean;
}

export interface TravelMeta {
  destination?: string;
  city?: string;
  state?: string;
  country?: string;
  coordinates?: { lat: number; lng: number };
  travelDates?: { start: string; end: string };
}

export interface CategoryResponse {
  id: ID;
  name: string;
  slug: string;
  description?: string;
  parentCategoryId?: ID | null;
  subCategories?: CategoryResponse[];
  postCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface TagResponse {
  id: ID;
  name: string;
  slug: string;
  description?: string;
  postCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PostMediaResponse {
  id: ID;
  postId: ID;
  url: string;
  type: "IMAGE" | "VIDEO" | "AUDIO" | "DOCUMENT";
  mimeType?: string;
  caption?: string;
  description?: string;
  subtitle?: string;
  groupKey?: string;
  sortOrder?: number;
  createdAt?: string;
}

export interface PostCommentResponse {
  id: ID;
  postId: ID;
  parentCommentId?: ID | null;
  userId?: ID;
  authorFirstName?: string;
  authorLastName?: string;
  authorAvatarUrl?: string;
  content: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  likeCount?: number;
  createdAt?: string;
  updatedAt?: string;
  replies?: PostCommentResponse[];
}

export interface PostLikeResponse {
  id: ID;
  postId: ID;
  userId?: ID;
  userFirstName?: string;
  userLastName?: string;
  userAvatarUrl?: string;
  createdAt?: string;
}

export interface BlogPostSummaryResponse {
  id: ID;
  title: string;
  slug: string;
  excerpt?: string;
  status: PostStatus;
  viewCount: number;
  userId: ID;
  categories: CategoryResponse[];
  tags: TagResponse[];
  postType: PostType;
  likeCount: number;
  commentCount: number;
  featuredImageUrl?: string;
  primaryVideoUrl?: string | null;
  media?: PostMediaResponse[];
  destination?: string;
  authorEmail?: string;
  authorFirstName?: string;
  authorLastName?: string;
  authorAvatarUrl?: string;
  isFeatured?: boolean;
  isTrending?: boolean;
  createdAt?: string;
  updatedAt?: string;
  publishedAt?: string;
  scheduledAt?: string;
}

export interface BlogPostResponse extends BlogPostSummaryResponse {
  content: string;
  readingTime?: number;
  featuredImage?: FeaturedImage;
  contentBlocks?: any[] | null;
  tableOfContents?: any[] | null;
  travelMeta?: TravelMeta;
  seo?: SEOData;
  visibility: Visibility;
  language?: string;
  shareCount: number;
  bookmarkCount: number;
  allowComments: boolean;
  allowLikes: boolean;
  isFeatured: boolean;
  isTrending: boolean;
  comments: PostCommentResponse[];
  media: PostMediaResponse[];
  relatedPosts?: BlogPostSummaryResponse[];
  scheduledAt?: string;
  updatedAt?: string;
}

export interface CreateBlogPostRequest {
  title: string;
  content: string;
  slug?: string;
  excerpt?: string;
  readingTime?: number;
  featuredImage?: string | FeaturedImage;
  primaryVideoUrl?: string | null;
  contentBlocks?: any[];
  tableOfContents?: any[];
  travelMeta?: TravelMeta;
  seo?: SEOData;
  visibility?: Visibility;
  language?: string;
  isFeatured?: boolean;
  isTrending?: boolean;
  scheduledAt?: string;
  postType?: PostType;
  userId?: string;
  authorEmail?: string;
  authorFirstName?: string;
  authorLastName?: string;
  authorAvatarUrl?: string;
  categories?: string[];
  tags?: string[];
  allowComments?: boolean;
  allowLikes?: boolean;
  mediaFiles?: File[];
  mediaCaptions?: string[];
  mediaDescriptions?: string[];
  mediaSubtitles?: string[];
  mediaGroups?: string[];
  groupedMediaFiles?: File[];
}

export interface UpdateBlogPostRequest extends Partial<CreateBlogPostRequest> {}

export interface PublishBlogPostRequest {
  publishAt?: string;
}

export interface BlogPostSearchRequest {
  query?: string;
  categoryIds?: string[];
  tagIds?: string[];
  authorId?: string;
  status?: PostStatus;
  postType?: PostType;
  isFeatured?: boolean;
  isTrending?: boolean;
  dateFrom?: string;
  dateTo?: string;
  sortBy?: string;
  sortDir?: "ASC" | "DESC";
}

export interface BlogStatsResponse {
  totalPosts: number;
  publishedPosts: number;
  draftPosts: number;
  scheduledPosts: number;
  totalViews: number;
  totalComments: number;
  totalLikes: number;
  totalAuthors: number;
  totalCategories: number;
  totalTags: number;
}

export interface BlogSettingsResponse {
  settingKey: string;
  settingValue: string;
  description?: string;
  updatedAt?: string;
}

export interface CreateCommentRequest {
  postId: ID;
  parentCommentId?: ID;
  content: string;
}

export interface UpdateCommentRequest {
  content: string;
}

export interface ApproveCommentRequest {
  reason?: string;
}
