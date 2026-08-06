// =============================================================================
// Shared data models — mirror of:
//   frontend/src/types/blog.ts
//   frontend/src/types/common.ts
// =============================================================================

import 'package:flutter/foundation.dart';
import '../core/env.dart';

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
      code: (json['statusCode'] as num?)?.toInt() ?? (json['code'] as num?)?.toInt(),
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
    final list = (json['content'] as List<dynamic>?) ??
        (json['items'] as List<dynamic>?) ??
        <dynamic>[];
    return PaginatedResponse<T>(
      items: list.map((e) => itemParser(e)).toList(growable: false),
      page: (json['page'] as num?)?.toInt() ?? 1,
      size: (json['size'] as num?)?.toInt() ?? 20,
      total: (json['totalElements'] as num?)?.toInt() ??
          (json['total'] as num?)?.toInt(),
      totalPages: (json['totalPages'] as num?)?.toInt(),
      hasMore: json['last'] == true ? false : (json['hasMore'] as bool?),
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
    case 'story':
      return PostType.short;
    case 'page':
      return PostType.page;
    case 'blog':
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

class TocEntry {
  final String title;
  final String? slug;
  final int? level;
  final String? parentSlug;

  const TocEntry({
    required this.title,
    this.slug,
    this.level,
    this.parentSlug,
  });

  factory TocEntry.fromJson(Map<String, dynamic> j) => TocEntry(
        title: (j['title'] ?? j['text'] ?? j['name'] ?? j['heading'] ?? '').toString(),
        slug: (j['slug'] ?? j['id'] ?? j['anchor'])?.toString(),
        level: (j['level'] is num) ? (j['level'] as num).toInt() : int.tryParse((j['level'] ?? '').toString()),
        parentSlug: (j['parentSlug'] ?? j['parent'])?.toString(),
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
  final String? sectionSlug;
  final AuthorResponse? author;
  final List<CategoryResponse>? categories;
  final List<TagResponse>? tags;
  final List<TocEntry>? tableOfContents;
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
    this.sectionSlug,
    this.author,
    this.categories,
    this.tags,
    this.tableOfContents,
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

  String get cover => Env.resolveImgUrl(
        featuredImageUrl ??
            YouTubeUtil.thumbnailFor(videoUrl ?? shortVideoUrl) ??
            '',
      );
  String get shortVideo => Env.resolveImgUrl(shortVideoUrl ?? videoUrl);
  String? get youtubeVideoId => YouTubeUtil.extractVideoId(videoUrl ?? shortVideoUrl);

  String? get instagramMediaId =>
      InstagramUtil.extractMediaId(videoUrl ?? shortVideoUrl);
  bool get isInstagramReel =>
      InstagramUtil.isInstagramUrl(videoUrl ?? shortVideoUrl) &&
      InstagramUtil.isReelUrl(videoUrl ?? shortVideoUrl);
  bool get isInstagramPost =>
      InstagramUtil.isInstagramUrl(videoUrl ?? shortVideoUrl);

  factory BlogPostSummaryResponse.fromJson(Map<String, dynamic> j) {
    DateTime? tryDt(Object? v) {
      if (v == null) return null;
      if (v is String) return DateTime.tryParse(v);
      if (v is int) return DateTime.fromMillisecondsSinceEpoch(v);
      return null;
    }
    String? str(Object? v) {
      if (v == null) return null;
      if (v is String) return v.isEmpty ? null : v;
      return v.toString();
    }
    int? toInt(Object? v) {
      if (v == null) return null;
      if (v is num) return v.toInt();
      final s = v.toString();
      return int.tryParse(s);
    }
    bool toBool(Object? v, {bool fallback = false}) {
      if (v == null) return fallback;
      if (v is bool) return v;
      if (v is num) return v != 0;
      final s = v.toString().toLowerCase();
      if (s == 'true' || s == '1' || s == 'yes') return true;
      if (s == 'false' || s == '0' || s == 'no') return false;
      return fallback;
    }
    String? imageFrom(Object? v) {
      final s = str(v);
      if (s != null) return s;
      if (v is Map) {
        final m = Map<String, dynamic>.from(v as Map);
        return str(m['url']) ?? str(m['mediaUrl']) ?? str(m['fileUrl']) ?? str(m['imageUrl']) ?? str(m['src']) ?? str(m['source']);
      }
      return null;
    }

    String? extractCoverImage(Object? featuredImage, Object? featuredImageUrl, Object? cover, Object? coverImage) {
      final a = imageFrom(featuredImage);
      if (a != null && a.isNotEmpty) return a;
      final b = imageFrom(featuredImageUrl);
      if (b != null && b.isNotEmpty) return b;
      final c = imageFrom(cover);
      if (c != null && c.isNotEmpty) return c;
      final d = imageFrom(coverImage);
      if (d != null && d.isNotEmpty) return d;
      return null;
    }

    String slugify(String s) => s
        .toLowerCase()
        .replaceAll(RegExp(r'[^a-z0-9]+'), '-')
        .replaceAll(RegExp(r'^-|-$'), '');

    AuthorResponse? a;
    if (j['author'] != null) {
      if (j['author'] is Map) {
        a = AuthorResponse.fromJson(Map<String, dynamic>.from(j['author'] as Map));
      } else if (j['author'] is String) {
        final s = j['author'] as String;
        a = AuthorResponse(id: slugify(s), displayName: s);
      }
    } else {
      final fn = str(j['authorFirstName']) ?? '';
      final ln = str(j['authorLastName']) ?? '';
      final display = (fn + (fn.isNotEmpty && ln.isNotEmpty ? ' ' : '') + ln).trim().isNotEmpty
          ? (fn + (fn.isNotEmpty && ln.isNotEmpty ? ' ' : '') + ln).trim()
          : (str(j['authorName']) ?? str(j['authorDisplayName']) ?? str(j['userId']) ?? '');
      final av = str(j['authorAvatarUrl']) ?? str(j['authorAvatar']);
      final em = str(j['authorEmail']);
      final bio = str(j['authorBio']) ?? str(j['authorDescription']) ?? str(j['authorBiography']);
      if (display.isNotEmpty || av != null || em != null) {
        a = AuthorResponse(
          id: str(j['authorId']) ?? str(j['userId']) ?? (display.isNotEmpty ? slugify(display) : Object().hashCode.abs().toString()),
          displayName: display,
          email: em,
          avatarUrl: av,
          bio: bio,
        );
      }
    }

    List<CategoryResponse>? cats;
    if (j['categories'] != null && j['categories'] is List) {
      cats = (j['categories'] as List<dynamic>).map((e) {
        if (e is Map) {
          return CategoryResponse.fromJson(Map<String, dynamic>.from(e as Map));
        }
        final s = e.toString();
        final sl = slugify(s);
        return CategoryResponse(id: sl, name: s, slug: sl);
      }).toList(growable: false);
    }
    List<TagResponse>? tags;
    if (j['tags'] != null && j['tags'] is List) {
      tags = (j['tags'] as List<dynamic>).map((e) {
        if (e is Map) {
          return TagResponse.fromJson(Map<String, dynamic>.from(e as Map));
        }
        final s = e.toString();
        final sl = slugify(s);
        return TagResponse(id: sl, name: s, slug: sl);
      }).toList(growable: false);
    }
    List<TocEntry>? toc;
    if (j['tableOfContents'] != null && j['tableOfContents'] is List) {
      toc = (j['tableOfContents'] as List<dynamic>).map((e) {
        if (e is Map) {
          try {
            return TocEntry.fromJson(Map<String, dynamic>.from(e as Map));
          } catch (_) {}
        }
        final s = e.toString().trim();
        return TocEntry(title: s, slug: slugify(s));
      }).where((t) => t.title.isNotEmpty).toList(growable: false);
      if (toc.isEmpty) toc = null;
    }

    return BlogPostSummaryResponse(
      id: str(j['id']) ?? '',
      title: str(j['title']) ?? '',
      slug: str(j['slug']) ?? '',
      excerpt: str(j['excerpt']) ?? str(j['summary']) ?? str(j['description']),
      featuredImageUrl: extractCoverImage(j['featuredImage'], j['featuredImageUrl'], j['cover'], j['coverImage']),
      videoUrl: imageFrom(j['videoUrl']) ?? imageFrom(j['video']) ?? imageFrom(j['primaryVideoUrl']),
      shortVideoUrl: imageFrom(j['shortVideoUrl']) ?? imageFrom(j['shortVideo']) ?? imageFrom(j['shortsUrl']) ?? imageFrom(j['primaryVideoUrl']),
      sectionSlug: str(j['sectionSlug']) ?? str(j['section_slug']),
      author: a,
      categories: cats,
      tags: tags,
      tableOfContents: toc,
      status: statusFromString(str(j['status'])),
      postType: postTypeFromString(str(j['postType'])),
      language: str(j['language']) ?? 'en',
      readingTimeMinutes: toInt(j['readingTimeMinutes']) ?? toInt(j['readingTime']),
      viewCount: toInt(j['viewCount']) ?? toInt(j['views']),
      likeCount: toInt(j['likeCount']) ?? toInt(j['likes']),
      commentCount: toInt(j['commentCount']) ?? toInt(j['comments']),
      isFeatured: toBool(j['isFeatured']),
      isTrending: toBool(j['isTrending']),
      publishedAt: tryDt(j['publishedAt']) ?? tryDt(j['publishDate']),
      createdAt: tryDt(j['createdAt']) ?? tryDt(j['createdDate']),
      updatedAt: tryDt(j['updatedAt']) ?? tryDt(j['modifiedDate']),
    );
  }
}

class BlogPostResponse extends BlogPostSummaryResponse {
  final String? content;
  final String? contentHtml;
  final List<BlogMedia>? media;
  final List<Map<String, dynamic>>? contentBlocks;

  const BlogPostResponse({
    required super.id,
    required super.title,
    required super.slug,
    super.excerpt,
    super.featuredImageUrl,
    super.videoUrl,
    super.shortVideoUrl,
    super.sectionSlug,
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
    this.contentBlocks,
  });

  factory BlogPostResponse.fromJson(Map<String, dynamic> j) {
    final s = BlogPostSummaryResponse.fromJson(j);
    List<BlogMedia>? medias;
    if (j['media'] != null && j['media'] is List) {
      medias = (j['media'] as List<dynamic>)
          .where((e) => e != null && e is Map)
          .map((e) {
            try {
              return BlogMedia.fromJson(Map<String, dynamic>.from(e as Map));
            } catch (_) {
              return BlogMedia(id: 'fallback-${identityHashCode(e)}');
            }
          })
          .toList(growable: false);
      if (medias.isEmpty) medias = null;
    }
    String? asPlainText(Object? v) {
      if (v == null) return null;
      if (v is String) return v;
      if (v is List) {
        return v.map((e) {
          if (e is String) return e;
          if (e is Map && e['text'] is String) return e['text'] as String;
          return '';
        }).where((t) => t.isNotEmpty).join('\n');
      }
      if (v is Map) {
        if (v['text'] is String) return v['text'] as String;
        if (v['html'] is String) return v['html'] as String;
        if (v['content'] is String) return v['content'] as String;
      }
      return v.toString();
    }
    List<Map<String, dynamic>>? cbs;
    if (j['contentBlocks'] != null && j['contentBlocks'] is List) {
      cbs = <Map<String, dynamic>>[];
      for (final e in (j['contentBlocks'] as List<dynamic>)) {
        if (e is Map) {
          try { cbs.add(Map<String, dynamic>.from(e as Map)); }
          catch (_) {}
        }
      }
      if (cbs.isEmpty) cbs = null;
    }
    return BlogPostResponse(
      id: s.id,
      title: s.title,
      slug: s.slug,
      excerpt: s.excerpt,
      featuredImageUrl: s.featuredImageUrl,
      videoUrl: s.videoUrl,
      shortVideoUrl: s.shortVideoUrl,
      sectionSlug: s.sectionSlug,
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
      content: asPlainText(j['content']) ?? asPlainText(j['body']),
      contentHtml: asPlainText(j['contentHtml']) ?? asPlainText(j['bodyHtml']) ?? asPlainText(j['content']),
      media: medias,
      contentBlocks: cbs,
    );
  }
}

// =============================================================================
// Section
// =============================================================================
class SectionResponse {
  final String id;
  final String name;
  final String slug;
  final String? description;
  final String? icon;
  final String? accentColor;
  final int? sortOrder;
  final String? parentSectionId;
  final int? postCount;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const SectionResponse({
    required this.id,
    required this.name,
    required this.slug,
    this.description,
    this.icon,
    this.accentColor,
    this.sortOrder,
    this.parentSectionId,
    this.postCount,
    this.createdAt,
    this.updatedAt,
  });

  factory SectionResponse.fromJson(Map<String, dynamic> j) {
    DateTime? tryDt(Object? v) {
      if (v == null) return null;
      if (v is String) return DateTime.tryParse(v);
      if (v is int) return DateTime.fromMillisecondsSinceEpoch(v);
      return null;
    }
    String? str(Object? v) {
      if (v == null) return null;
      if (v is String) return v.isEmpty ? null : v;
      return v.toString();
    }
    int? toInt(Object? v) {
      if (v == null) return null;
      if (v is num) return v.toInt();
      return int.tryParse(v.toString());
    }
    return SectionResponse(
      id: str(j['id']) ?? '',
      name: (str(j['name']) ?? '').toUpperCase(),
      slug: str(j['slug']) ?? '',
      description: str(j['description']),
      icon: str(j['icon']),
      accentColor: str(j['accentColor']) ?? str(j['accent_color']),
      sortOrder: toInt(j['sortOrder']) ?? toInt(j['sort_order']),
      parentSectionId: str(j['parentSectionId']) ?? str(j['parent_section_id']),
      postCount: toInt(j['postCount']),
      createdAt: tryDt(j['createdAt']),
      updatedAt: tryDt(j['updatedAt']),
    );
  }
}

// =============================================================================
// YouTube URL helpers
// =============================================================================
class YouTubeUtil {
  YouTubeUtil._();

  static String? extractVideoId(String? url) {
    if (url == null || url.isEmpty) return null;
    final u = url.trim();
    final m1 = RegExp(r'youtu\.be\/([a-zA-Z0-9_-]{6,})').firstMatch(u);
    if (m1 != null && m1.groupCount >= 1) return m1.group(1);
    final m2 = RegExp(r'[?&]v=([a-zA-Z0-9_-]{6,})').firstMatch(u);
    if (m2 != null && m2.groupCount >= 1) return m2.group(1);
    final m3 = RegExp(r'embed\/([a-zA-Z0-9_-]{6,})').firstMatch(u);
    if (m3 != null && m3.groupCount >= 1) return m3.group(1);
    final m4 = RegExp(r'shorts\/([a-zA-Z0-9_-]{6,})').firstMatch(u);
    if (m4 != null && m4.groupCount >= 1) return m4.group(1);
    return null;
  }

  static String? thumbnailFor(String? url, {String quality = 'hqdefault'}) {
    final id = extractVideoId(url);
    if (id == null) return null;
    return 'https://img.youtube.com/vi/$id/$quality.jpg';
  }

  static String iframeEmbed(String videoId, {bool autoplay = false}) {
    final params = <String>[
      if (autoplay) 'autoplay=1',
      'rel=0',
      'modestbranding=1',
      'playsinline=1',
    ].join('&');
    return '''
<!DOCTYPE html>
<html>
<head>
<style>
html,body{margin:0;padding:0;height:100%;background:#000;overflow:hidden;}
iframe{position:absolute;inset:0;width:100%;height:100%;border:0;}
</style>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
</head>
<body>
<iframe src="https://www.youtube.com/embed/$videoId?$params" allow="accelerometer;autoplay;clipboard-write;encrypted-media;gyroscope;picture-in-picture" allowfullscreen></iframe>
</body>
</html>''';
  }
}

// =============================================================================
// Instagram Reels / Post URL helpers
// =============================================================================
class InstagramUtil {
  InstagramUtil._();

  static final RegExp _igRe = RegExp(
    r'instagram\.com\/(?:p|reel|reels|tv|stories\/[^/]+)\/([A-Za-z0-9_-]+)',
    caseSensitive: false,
  );

  static bool isInstagramUrl(String? url) {
    if (url == null || url.isEmpty) return false;
    return url.toLowerCase().contains('instagram.com');
  }

  static bool isReelUrl(String? url) {
    if (url == null || url.isEmpty) return false;
    final u = url.toLowerCase();
    return u.contains('/reel/') || u.contains('/reels/');
  }

  static String? extractMediaId(String? url) {
    if (url == null || url.isEmpty) return null;
    final u = url.trim();
    // Bare media ID (no dots, >= 8 chars alnum)
    final bare = RegExp(r'^[A-Za-z0-9_-]{8,}$').firstMatch(u);
    if (bare != null && !u.contains('.')) return u;
    final m = _igRe.firstMatch(u);
    if (m != null && m.groupCount >= 1) return m.group(1);
    return null;
  }

  static String iframeEmbed(String mediaId, {bool autoplay = false}) {
    final params = <String>[
      if (autoplay) 'autoplay=1',
      'mute=1',
      'hidecaption=1',
      'omitscript=1',
    ].join('&');
    return '''
<!DOCTYPE html>
<html>
<head>
<style>
html,body{margin:0;padding:0;height:100%;background:#0A0A0A;overflow:hidden;}
iframe{position:absolute;inset:0;width:100%;height:100%;border:0;}
</style>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
</head>
<body>
<iframe src="https://www.instagram.com/p/$mediaId/embed/?$params" allow="autoplay;clipboard-write;encrypted-media;picture-in-picture;web-share" allowfullscreen></iframe>
</body>
</html>''';
  }

  static String externalUrl(String mediaId, {bool isReel = true}) {
    return isReel
        ? 'https://www.instagram.com/reel/$mediaId/'
        : 'https://www.instagram.com/p/$mediaId/';
  }
}

class BlogMedia {
  final String id;
  final String? url;
  final String? mimeType;
  final String? type;
  final int? sizeBytes;
  final String? caption;
  final String? description;
  final String? alt;
  final int? width;
  final int? height;
  final int? sortOrder;
  final String? groupKey;
  final int? durationSeconds;
  final String? thumbnailUrl;

  const BlogMedia({
    required this.id,
    this.url,
    this.mimeType,
    this.type,
    this.sizeBytes,
    this.caption,
    this.description,
    this.alt,
    this.width,
    this.height,
    this.sortOrder,
    this.groupKey,
    this.durationSeconds,
    this.thumbnailUrl,
  });

  bool get isImage {
    final t = (type ?? '').toString().toUpperCase();
    if (t == 'IMAGE' || t == 'IMG' || t == 'PHOTO' || t == 'PICTURE') return true;
    final m = (mimeType ?? '').toString().toLowerCase();
    if (m.startsWith('image/')) return true;
    final u = (url ?? '').toString().toLowerCase();
    return u.endsWith('.jpg') || u.endsWith('.jpeg') || u.endsWith('.png') || u.endsWith('.gif') || u.endsWith('.webp') || u.endsWith('.avif');
  }

  factory BlogMedia.fromJson(Map<String, dynamic> j) => BlogMedia(
        id: j['id'].toString(),
        url: (j['mediaUrl'] as String?) ?? (j['url'] as String?) ?? (j['fileUrl'] as String?) ?? (j['imageUrl'] as String?) ?? (j['src'] as String?),
        mimeType: (j['mimeType'] as String?) ?? (j['type'] as String?) ?? (j['mediaType'] as String?),
        type: (j['type'] as String?) ?? (j['mediaType'] as String?),
        sizeBytes: (j['sizeBytes'] as num?)?.toInt(),
        caption: (j['caption'] as String?) ?? (j['subtitle'] as String?) ?? (j['name'] as String?),
        description: (j['description'] as String?) ?? (j['subtitle'] as String?) ?? (j['alt'] as String?),
        alt: (j['alt'] as String?) ?? (j['name'] as String?) ?? (j['caption'] as String?),
        width: (j['width'] as num?)?.toInt(),
        height: (j['height'] as num?)?.toInt(),
        sortOrder: (j['sortOrder'] as num?)?.toInt() ?? (j['displayOrder'] as num?)?.toInt() ?? (j['order'] as num?)?.toInt(),
        groupKey: (j['groupKey'] as String?) ?? (j['subtitleGroupIndex']?.toString()),
        durationSeconds: (j['durationSeconds'] as num?)?.toInt(),
        thumbnailUrl: (j['thumbnailUrl'] as String?) ?? (j['thumb'] as String?),
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
