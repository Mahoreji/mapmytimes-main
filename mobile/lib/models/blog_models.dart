// =============================================================================
// Shared data models — mirror of:
//   frontend/src/types/blog.ts
//   frontend/src/types/common.ts
// =============================================================================

import 'package:flutter/foundation.dart';

typedef ID = String;

// =============================================================================
// Envelope — { success, message, code, data: T }
// =============================================================================
class APIResponse<T> {
  final bool? success;
  final String? message;
  final int? code;
  final T? data;

  const APIResponse({this.success, this.message, this.code, this.data});

  factory APIResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Object? json) dataParser,
  ) {
    return APIResponse<T>(
      success: json['success'] as bool?,
      message: json['message'] as String?,
      code: (json['code'] as num?)?.toInt(),
      data: json['data'] == null ? null : dataParser(json['data']),
    );
  }
}

class PaginatedResponse<T> {
  final List<T> items;
  final int page;
  final int size;
  final int? total;
  final int? totalPages;
  final bool? hasMore;

  const PaginatedResponse({
    required this.items,
    required this.page,
    required this.size,
    this.total,
    this.totalPages,
    this.hasMore,
  });

  factory PaginatedResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Object? json) itemParser,
  ) {
    final list = (json['items'] as List<dynamic>?) ?? <dynamic>[];
    return PaginatedResponse<T>(
      items: list.map((e) => itemParser(e)).toList(growable: false),
      page: (json['page'] as num?)?.toInt() ?? 1,
      size: (json['size'] as num?)?.toInt() ?? 20,
      total: (json['total'] as num?)?.toInt(),
      totalPages: (json['totalPages'] as num?)?.toInt(),
      hasMore: json['hasMore'] as bool?,
    );
  }
}

// =============================================================================
// Enums
// =============================================================================
enum PostStatus { draft, pending, published, rejected, archived, scheduled }
enum PostType { article, video, short, page }

PostStatus statusFromString(String? s) {
  switch ((s ?? '').toLowerCase()) {
    case 'draft':
      return PostStatus.draft;
    case 'pending':
      return PostStatus.pending;
    case 'published':
      return PostStatus.published;
    case 'rejected':
      return PostStatus.rejected;
    case 'archived':
      return PostStatus.archived;
    case 'scheduled':
      return PostStatus.scheduled;
    default:
      return PostStatus.draft;
  }
}

PostType postTypeFromString(String? s) {
  switch ((s ?? '').toLowerCase()) {
    case 'video':
      return PostType.video;
    case 'short':
      return PostType.short;
    case 'page':
      return PostType.page;
    case 'article':
    default:
      return PostType.article;
  }
}

// =============================================================================
// Author / Category / Tag
// =============================================================================
class AuthorResponse {
  final String id;
  final String? displayName;
  final String? firstName;
  final String? lastName;
  final String? avatarUrl;
  final String? email;
  final String? role;
  final String? bio;
  final String? jobTitle;

  const AuthorResponse({
    required this.id,
    this.displayName,
    this.firstName,
    this.lastName,
    this.avatarUrl,
    this.email,
    this.role,
    this.bio,
    this.jobTitle,
  });

  String get name {
    final d = displayName ?? '';
    if (d.isNotEmpty) return d;
    final f = firstName ?? '';
    final l = lastName ?? '';
    if (f.isEmpty && l.isEmpty) {
      final e = email ?? '';
      return e.isNotEmpty ? e : '—';
    }
    return '$f $l'.trim();
  }

  factory AuthorResponse.fromJson(Map<String, dynamic> j) => AuthorResponse(
        id: j['id'].toString(),
        displayName: j['displayName'] as String?,
        firstName: j['firstName'] as String?,
        lastName: j['lastName'] as String?,
        avatarUrl: j['avatarUrl'] as String? ?? j['profileImageUrl'] as String?,
        email: j['email'] as String?,
        role: j['role'] as String?,
        bio: j['bio'] as String?,
        jobTitle: j['jobTitle'] as String?,
      );
}

class CategoryResponse {
  final String id;
  final String name;
  final String slug;
  final String? description;
  final String? parentCategoryId;
  final int? postCount;

  const CategoryResponse({
    required this.id,
    required this.name,
    required this.slug,
    this.description,
    this.parentCategoryId,
    this.postCount,
  });

  factory CategoryResponse.fromJson(Map<String, dynamic> j) => CategoryResponse(
        id: j['id'].toString(),
        name: (j['name'] as String?) ?? '',
        slug: (j['slug'] as String?) ?? '',
        description: j['description'] as String?,
        parentCategoryId: j['parentCategoryId'] as String?,
        postCount: (j['postCount'] as num?)?.toInt(),
      );
}

class TagResponse {
  final String id;
  final String name;
  final String slug;
  final String? description;
  final int? postCount;

  const TagResponse({
    required this.id,
    required this.name,
    required this.slug,
    this.description,
    this.postCount,
  });

  factory TagResponse.fromJson(Map<String, dynamic> j) => TagResponse(
        id: j['id'].toString(),
        name: (j['name'] as String?) ?? '',
        slug: (j['slug'] as String?) ?? '',
        description: j['description'] as String?,
        postCount: (j['postCount'] as num?)?.toInt(),
      );
}

// =============================================================================
// Blog Post (summary + detail)
// =============================================================================
class BlogPostSummaryResponse {
  final String id;
  final String title;
  final String slug;
  final String? excerpt;
  final String? featuredImageUrl;
  final String? videoUrl;
  final String? shortVideoUrl;
  final AuthorResponse? author;
  final List<CategoryResponse>? categories;
  final List<TagResponse>? tags;
  final PostStatus status;
  final PostType postType;
  final String language;
  final int? readingTimeMinutes;
  final int? viewCount;
  final int? likeCount;
  final int? commentCount;
  final bool isFeatured;
  final bool isTrending;
  final DateTime? publishedAt;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const BlogPostSummaryResponse({
    required this.id,
    required this.title,
    required this.slug,
    this.excerpt,
    this.featuredImageUrl,
    this.videoUrl,
    this.shortVideoUrl,
    this.author,
    this.categories,
    this.tags,
    required this.status,
    required this.postType,
    this.language = 'en',
    this.readingTimeMinutes,
    this.viewCount,
    this.likeCount,
    this.commentCount,
    this.isFeatured = false,
    this.isTrending = false,
    this.publishedAt,
    this.createdAt,
    this.updatedAt,
  });

  String get cover => featuredImageUrl ?? '';
  String get shortVideo => shortVideoUrl ?? videoUrl ?? '';

  factory BlogPostSummaryResponse.fromJson(Map<String, dynamic> j) {
    DateTime? tryDt(Object? v) {
      if (v == null) return null;
      if (v is String) return DateTime.tryParse(v);
      return null;
    }

    AuthorResponse? a;
    if (j['author'] != null && j['author'] is Map) {
      a = AuthorResponse.fromJson(Map<String, dynamic>.from(j['author'] as Map));
    }

    List<CategoryResponse>? cats;
    if (j['categories'] != null && j['categories'] is List) {
      cats = (j['categories'] as List<dynamic>)
          .map((e) => CategoryResponse.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(growable: false);
    }
    List<TagResponse>? tags;
    if (j['tags'] != null && j['tags'] is List) {
      tags = (j['tags'] as List<dynamic>)
          .map((e) => TagResponse.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(growable: false);
    }

    return BlogPostSummaryResponse(
      id: j['id'].toString(),
      title: (j['title'] as String?) ?? '',
      slug: (j['slug'] as String?) ?? '',
      excerpt: j['excerpt'] as String? ?? j['summary'] as String?,
      featuredImageUrl:
          j['featuredImageUrl'] as String? ?? j['featuredImage'] as String? ?? j['coverImage'] as String?,
      videoUrl: j['videoUrl'] as String?,
      shortVideoUrl: j['shortVideoUrl'] as String?,
      author: a,
      categories: cats,
      tags: tags,
      status: statusFromString(j['status'] as String?),
      postType: postTypeFromString(j['postType'] as String?),
      language: (j['language'] as String?) ?? 'en',
      readingTimeMinutes: (j['readingTimeMinutes'] as num?)?.toInt(),
      viewCount: (j['viewCount'] as num?)?.toInt(),
      likeCount: (j['likeCount'] as num?)?.toInt(),
      commentCount: (j['commentCount'] as num?)?.toInt(),
      isFeatured: (j['isFeatured'] as bool?) ?? false,
      isTrending: (j['isTrending'] as bool?) ?? false,
      publishedAt: tryDt(j['publishedAt']),
      createdAt: tryDt(j['createdAt']),
      updatedAt: tryDt(j['updatedAt']),
    );
  }
}

class BlogPostResponse extends BlogPostSummaryResponse {
  final String? content;
  final String? contentHtml;
  final List<BlogMedia>? media;

  const BlogPostResponse({
    required super.id,
    required super.title,
    required super.slug,
    super.excerpt,
    super.featuredImageUrl,
    super.videoUrl,
    super.shortVideoUrl,
    super.author,
    super.categories,
    super.tags,
    required super.status,
    required super.postType,
    super.language = 'en',
    super.readingTimeMinutes,
    super.viewCount,
    super.likeCount,
    super.commentCount,
    super.isFeatured = false,
    super.isTrending = false,
    super.publishedAt,
    super.createdAt,
    super.updatedAt,
    this.content,
    this.contentHtml,
    this.media,
  });

  factory BlogPostResponse.fromJson(Map<String, dynamic> j) {
    final s = BlogPostSummaryResponse.fromJson(j);
    List<BlogMedia>? medias;
    if (j['media'] != null && j['media'] is List) {
      medias = (j['media'] as List<dynamic>)
          .map((e) => BlogMedia.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(growable: false);
    }
    return BlogPostResponse(
      id: s.id,
      title: s.title,
      slug: s.slug,
      excerpt: s.excerpt,
      featuredImageUrl: s.featuredImageUrl,
      videoUrl: s.videoUrl,
      shortVideoUrl: s.shortVideoUrl,
      author: s.author,
      categories: s.categories,
      tags: s.tags,
      status: s.status,
      postType: s.postType,
      language: s.language,
      readingTimeMinutes: s.readingTimeMinutes,
      viewCount: s.viewCount,
      likeCount: s.likeCount,
      commentCount: s.commentCount,
      isFeatured: s.isFeatured,
      isTrending: s.isTrending,
      publishedAt: s.publishedAt,
      createdAt: s.createdAt,
      updatedAt: s.updatedAt,
      content: j['content'] as String? ?? j['body'] as String?,
      contentHtml: j['contentHtml'] as String? ?? j['bodyHtml'] as String?,
      media: medias,
    );
  }
}

class BlogMedia {
  final String id;
  final String? url;
  final String? mimeType;
  final int? sizeBytes;
  final String? caption;
  final int? width;
  final int? height;
  final int? durationSeconds;
  final String? thumbnailUrl;

  const BlogMedia({
    required this.id,
    this.url,
    this.mimeType,
    this.sizeBytes,
    this.caption,
    this.width,
    this.height,
    this.durationSeconds,
    this.thumbnailUrl,
  });

  factory BlogMedia.fromJson(Map<String, dynamic> j) => BlogMedia(
        id: j['id'].toString(),
        url: j['url'] as String? ?? j['fileUrl'] as String?,
        mimeType: j['mimeType'] as String? ?? j['type'] as String?,
        sizeBytes: (j['sizeBytes'] as num?)?.toInt(),
        caption: j['caption'] as String?,
        width: (j['width'] as num?)?.toInt(),
        height: (j['height'] as num?)?.toInt(),
        durationSeconds: (j['durationSeconds'] as num?)?.toInt(),
        thumbnailUrl: j['thumbnailUrl'] as String?,
      );
}

class JobResponse {
  final String id;
  final String title;
  final String slug;
  final String? department;
  final String? employmentType;
  final String? experienceLevel;
  final String? location;
  final String? description;
  final DateTime? deadline;
  final bool? remote;
  final bool? published;

  const JobResponse({
    required this.id,
    required this.title,
    required this.slug,
    this.department,
    this.employmentType,
    this.experienceLevel,
    this.location,
    this.description,
    this.deadline,
    this.remote,
    this.published,
  });

  factory JobResponse.fromJson(Map<String, dynamic> j) => JobResponse(
        id: j['id'].toString(),
        title: (j['title'] as String?) ?? '',
        slug: (j['slug'] as String?) ?? '',
        department: j['department'] as String?,
        employmentType: j['employmentType'] as String?,
        experienceLevel: j['experienceLevel'] as String?,
        location: j['location'] as String?,
        description: j['description'] as String? ?? j['aboutRole'] as String?,
        deadline: DateTime.tryParse((j['deadline']?.toString()) ?? ''),
        remote: j['remote'] as bool?,
        published: j['published'] as bool?,
      );
}
