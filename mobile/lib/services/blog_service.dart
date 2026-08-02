// -------------------------------------------------------------------------
// BlogService — API client for MapMyTimes blog-service (Spring Boot Java).
// All endpoints are 1:1 mirror of frontend/src/lib/api/blogApi.ts
// Base URL: Env.apiBaseUrl = https://api.mapmytimes.com (overridable in .env)
// -------------------------------------------------------------------------

import 'package:dio/dio.dart';
import '../models/blog_models.dart';
import 'common.dart';

class BlogService {
  BlogService._(this.dio);
  final Dio dio;

  // ---------------------------------------------------------------------------
  // Init
  // ---------------------------------------------------------------------------
  static BlogService create({Dio? existing}) {
    final d = existing ?? createDio();
    return BlogService._(d);
  }

  void setBearerToken(String? token) {
    if (token == null || token.isEmpty) {
      dio.options.headers.remove('Authorization');
    } else {
      dio.options.headers['Authorization'] = 'Bearer $token';
    }
  }

  // ---------------------------------------------------------------------------
  // Query builders
  // ---------------------------------------------------------------------------
  static String _query(Map<String, Object?> params) {
    final parts = <String>[];
    params.forEach((k, v) {
      if (v == null) return;
      if (v is Iterable) {
        for (final e in v) {
          parts.add('$k=${Uri.encodeQueryComponent(e.toString())}');
        }
        return;
      }
      final s = v.toString();
      if (s.isEmpty) return;
      parts.add('$k=${Uri.encodeQueryComponent(s)}');
    });
    return parts.isEmpty ? '' : '?${parts.join('&')}';
  }

  static Map<String, String> _sortParts(String sort) {
    if (sort.isEmpty) return const {'sortBy': 'createdAt', 'sortDirection': 'DESC'};
    final dir = sort.startsWith('-') ? 'DESC' : 'ASC';
    final field = sort.startsWith('-') ? sort.substring(1) : sort;
    if (field.isEmpty) return const {'sortBy': 'createdAt', 'sortDirection': 'DESC'};
    return {'sortBy': field, 'sortDirection': dir};
  }

  // ---------------------------------------------------------------------------
  // Helpers: unwrap envelope APIResponse<PaginatedResponse<T>> etc
  // ---------------------------------------------------------------------------
  APIResponse<T> _unwrapEnvelope<T>(
    Response r,
    T Function(Object? json) dataParser,
  ) {
    if (r.data is Map<String, dynamic>) {
      return APIResponse.fromJson(r.data as Map<String, dynamic>, dataParser);
    }
    // Some Java services return raw object if envelope disabled
    return APIResponse(
      success: r.statusCode != null && r.statusCode! >= 200 && r.statusCode! < 300,
      code: r.statusCode,
      data: dataParser(r.data),
    );
  }

  PaginatedResponse<T> _paginated<T>(Object? json, T Function(Object? json) itemFromJson) {
    if (json == null) return PaginatedResponse(items: [], page: 1, size: 20);
    if (json is Map<String, dynamic>) {
      return PaginatedResponse.fromJson(json, itemFromJson);
    }
    if (json is List) {
      return PaginatedResponse(
        items: json.map((e) => itemFromJson(e)).toList(growable: false),
        page: 1,
        size: json.length,
        total: json.length,
        totalPages: 1,
        hasMore: false,
      );
    }
    return PaginatedResponse(items: [], page: 1, size: 20);
  }

  // ===========================================================================
  // POSTS
  // ===========================================================================
  static const postsV1 = '/api/v1/blog/posts';
  static const catsV1 = '/api/v1/blog/categories';
  static const sectionsV1 = '/api/v1/blog/sections';
  static const tagsV1 = '/api/v1/blog/tags';
  static const likesV1 = '/api/v1/blog/likes';

  Future<PaginatedResponse<BlogPostSummaryResponse>> postsList({
    int page = 1,
    int size = 20,
    String? categoryId,
    String? sectionSlug,
    String? tagId,
    String? status = 'PUBLISHED',
    String? postType,
    bool? isFeatured,
    bool? isTrending,
    String? userId,
    String? language,
    String sort = '-publishedAt',
  }) async {
    final s = _sortParts(sort);
    final q = _query({
      'page': page,
      'size': size,
      if (categoryId != null) 'category': categoryId,
      if (sectionSlug != null) 'sectionSlug': sectionSlug,
      if (tagId != null) 'tag': tagId,
      if (status != null) 'status': status,
      if (postType != null) 'postType': postType,
      if (isFeatured != null) 'isFeatured': isFeatured,
      if (isTrending != null) 'isTrending': isTrending,
      if (userId != null) 'userId': userId,
      if (language != null) 'language': language,
      'sortBy': s['sortBy'],
      'sortDirection': s['sortDirection'],
    });
    final r = await dio.get('$postsV1/search$q');
    final env = _unwrapEnvelope(
      r,
      (Object? json) => _paginated<BlogPostSummaryResponse>(
        json,
        (Object? j) => BlogPostSummaryResponse.fromJson(Map<String, dynamic>.from(j as Map)),
      ),
    );
    return env.data ?? PaginatedResponse(items: const [], page: page, size: size);
  }

  Future<BlogPostResponse?> postById(ID id) async {
    final r = await dio.get('$postsV1/$id');
    final env = _unwrapEnvelope(
      r,
      (Object? j) => BlogPostResponse.fromJson(Map<String, dynamic>.from(j as Map)),
    );
    return env.data;
  }

  Future<BlogPostResponse?> postBySlug(String slug) async {
    final r = await dio.get('$postsV1/slug/${Uri.encodeComponent(slug)}');
    final env = _unwrapEnvelope(
      r,
      (Object? j) => BlogPostResponse.fromJson(Map<String, dynamic>.from(j as Map)),
    );
    return env.data;
  }

  Future<PaginatedResponse<BlogPostSummaryResponse>> postsSearch(
    String keyword, {
    int page = 1,
    int size = 20,
    String? language,
    String? category,
    String? sectionSlug,
    String? tag,
    String sort = '-publishedAt',
  }) async {
    final s = _sortParts(sort);
    final q = _query({
      'keyword': keyword,
      'page': page,
      'size': size,
      if (language != null) 'language': language,
      if (category != null) 'category': category,
      if (sectionSlug != null) 'sectionSlug': sectionSlug,
      if (tag != null) 'tag': tag,
      'sortBy': s['sortBy'],
      'sortDirection': s['sortDirection'],
    });
    final r = await dio.get('$postsV1/search$q');
    final env = _unwrapEnvelope(
      r,
      (Object? json) => _paginated<BlogPostSummaryResponse>(
        json,
        (j) => BlogPostSummaryResponse.fromJson(Map<String, dynamic>.from(j as Map)),
      ),
    );
    return env.data ?? PaginatedResponse(items: const [], page: page, size: size);
  }

  Future<void> incrementView(ID postId) async {
    try {
      await dio.put('$postsV1/$postId/view');
    } catch (_) {}
  }

  Future<void> likePost(ID postId) async => await dio.post('$postsV1/$postId/like');
  Future<void> unlikePost(ID postId) async => await dio.delete('$postsV1/$postId/like');

  // ===========================================================================
  // CATEGORIES
  // ===========================================================================
  Future<List<CategoryResponse>> categoriesList() async {
    final r = await dio.get('$catsV1${_query({'size': 200})}');
    final env = _unwrapEnvelope(
      r,
      (Object? json) {
        if (json is List) {
          return json.map((j) => CategoryResponse.fromJson(Map<String, dynamic>.from(j as Map))).toList(growable: false);
        }
        if (json is Map<String, dynamic>) {
          final p = PaginatedResponse.fromJson(
            json,
            (j) => CategoryResponse.fromJson(Map<String, dynamic>.from(j as Map)),
          );
          return p.items;
        }
        return <CategoryResponse>[];
      },
    );
    return env.data ?? <CategoryResponse>[];
  }

  // ===========================================================================
  // SECTIONS
  // ===========================================================================
  Future<List<SectionResponse>> sectionsList() async {
    final r = await dio.get('$sectionsV1${_query({'size': 200})}');
    final env = _unwrapEnvelope(
      r,
      (Object? json) {
        if (json is List) {
          return json.map((j) => SectionResponse.fromJson(Map<String, dynamic>.from(j as Map))).toList(growable: false);
        }
        if (json is Map<String, dynamic>) {
          final p = PaginatedResponse.fromJson(
            json,
            (j) => SectionResponse.fromJson(Map<String, dynamic>.from(j as Map)),
          );
          return p.items;
        }
        return <SectionResponse>[];
      },
    );
    return env.data ?? <SectionResponse>[];
  }

  // ===========================================================================
  // TAGS
  // ===========================================================================
  Future<List<TagResponse>> tagsPopular() async {
    final r = await dio.get('$tagsV1/popular');
    final env = _unwrapEnvelope(
      r,
      (Object? json) {
        if (json is List) {
          return json.map((j) => TagResponse.fromJson(Map<String, dynamic>.from(j as Map))).toList(growable: false);
        }
        return <TagResponse>[];
      },
    );
    return env.data ?? <TagResponse>[];
  }
}
