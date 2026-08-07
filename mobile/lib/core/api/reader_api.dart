import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../env.dart';
import '../../services/common.dart';
import '../../models/blog_models.dart';

class ReaderContinueItem {
  final String postId;
  final int scrollPercent;
  final DateTime updatedAt;
  final BlogPostSummaryResponse? post;
  final int? readingTimeMinutes;

  const ReaderContinueItem({
    required this.postId,
    required this.scrollPercent,
    required this.updatedAt,
    this.post,
    this.readingTimeMinutes,
  });
}

class ReaderApi {
  ReaderApi._();
  static final ReaderApi instance = ReaderApi._();

  final Dio _dio = createDio(base: Env.apiBaseUrl);

  static String? _authTokenFromPrefs;
  static const _kAuthTokenKey = 'mmt.auth.accessToken';
  static const _kFallbackAuthTokenKey = 'auth_token';

  Future<String?> _authToken() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final t = prefs.getString(_kAuthTokenKey) ?? prefs.getString(_kFallbackAuthTokenKey);
      _authTokenFromPrefs = t;
      return t;
    } catch (_) {
      return _authTokenFromPrefs;
    }
  }

  void _setAuthHeader(String? token) {
    if (token == null || token.isEmpty) {
      _dio.options.headers.remove('Authorization');
    } else {
      _dio.options.headers['Authorization'] = 'Bearer $token';
    }
  }

  Future<void> upsertReadingProgress({
    required String postId,
    required int scrollPercent,
    String? authToken,
  }) async {
    try {
      final token = authToken ?? await _authToken();
      _setAuthHeader(token);
      await _dio.post(
        '/api/v1/reading-progress/me',
        data: <String, dynamic>{
          'postId': postId,
          'scrollPercent': scrollPercent,
        },
      );
    } catch (_) {}
  }

  Future<int?> getReadingProgressForPost(String postId, {String? authToken}) async {
    try {
      final token = authToken ?? await _authToken();
      _setAuthHeader(token);
      final r = await _dio.get('/api/v1/reading-progress/me/post/$postId');
      final data = r.data;
      if (data is Map<String, dynamic>) {
        final sp = data['scrollPercent'] ?? data['scroll_percent'];
        if (sp is int) return sp;
        if (sp is num) return sp.round();
      }
      if (r.data is int) return r.data as int;
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<List<ReaderContinueItem>> getLatestReadingProgress({
    int limit = 20,
    String? authToken,
  }) async {
    try {
      final token = authToken ?? await _authToken();
      if (token != null && token.isNotEmpty) {
        _setAuthHeader(token);
        final r = await _dio.get(
          '/api/v1/reading-progress/me/latest',
          queryParameters: <String, dynamic>{'limit': limit},
        );
        final raw = r.data;
        List<dynamic> list = const [];
        if (raw is List) {
          list = raw;
        } else if (raw is Map<String, dynamic>) {
          final d = raw['data'] ?? raw['items'] ?? raw['content'];
          if (d is List) list = d;
        }
        return list.map((e) {
          if (e is! Map<String, dynamic>) return null;
          final pid = (e['id'] ?? e['postId'] ?? e['post_id'] ?? '').toString();
          final spv = e['scrollPercent'] ?? e['scroll_percent'];
          final sp = spv is int ? spv : (spv is num ? spv.round() : 0);
          if (sp < 5 || sp > 95) return null;
          final ua = e['updatedAt'] ?? e['updated_at'];
          DateTime dt;
          if (ua is DateTime) {
            dt = ua;
          } else if (ua is String) {
            dt = DateTime.tryParse(ua) ?? DateTime.now();
          } else {
            dt = DateTime.now();
          }
          BlogPostSummaryResponse? post;
          try {
            post = BlogPostSummaryResponse.fromJson(e);
          } catch (_) {}
          final rtm = e['readingTimeMinutes'] ?? e['readingTime'];
          int? rt;
          if (rtm is int) rt = rtm;
          if (rtm is num) rt = rtm.round();
          return ReaderContinueItem(
            postId: pid,
            scrollPercent: sp,
            updatedAt: dt,
            post: post,
            readingTimeMinutes: rt ?? post?.readingTimeMinutes,
          );
        }).whereType<ReaderContinueItem>().toList(growable: false);
      } else {
        final prefs = await SharedPreferences.getInstance();
        final keys = prefs.getKeys();
        final prefix = 'mmt:reader:progress:';
        final results = <ReaderContinueItem>[];
        for (final k in keys) {
          if (!k.startsWith(prefix)) continue;
          final postId = k.substring(prefix.length);
          if (postId.isEmpty) continue;
          final v = prefs.getInt(k);
          if (v == null) continue;
          if (v < 5 || v > 95) continue;
          results.add(ReaderContinueItem(
            postId: postId,
            scrollPercent: v,
            updatedAt: DateTime.now(),
          ),);
        }
        results.sort((a, b) => b.updatedAt.compareTo(a.updatedAt));
        return results.take(limit).toList(growable: false);
      }
    } catch (_) {
      return const [];
    }
  }

  Future<Map<String, dynamic>?> getReaderPrefs({String? authToken}) async {
    try {
      final token = authToken ?? await _authToken();
      _setAuthHeader(token);
      final r = await _dio.get('/api/v1/users/me/reader-preferences');
      final data = r.data;
      if (data is Map<String, dynamic>) {
        final inner = data['data'] ?? data['preferences'] ?? data;
        if (inner is Map<String, dynamic>) return inner;
        return data;
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<void> upsertReaderPrefs(Map<String, dynamic> prefs, {String? authToken}) async {
    try {
      final token = authToken ?? await _authToken();
      _setAuthHeader(token);
      await _dio.put(
        '/api/v1/users/me/reader-preferences',
        data: prefs,
      );
    } catch (_) {}
  }

  Future<Map<String, dynamic>?> createHighlight({
    required String postId,
    required int paragraphIndex,
    required int charStart,
    required int charEnd,
    required String excerpt,
    String? authToken,
  }) async {
    try {
      final token = authToken ?? await _authToken();
      _setAuthHeader(token);
      final r = await _dio.post<dynamic>(
        '/api/v1/highlights/me',
        data: <String, dynamic>{
          'postId': postId,
          'paragraphIndex': paragraphIndex,
          'charStart': charStart,
          'charEnd': charEnd,
          'excerpt': excerpt,
        },
      );
      final data = r.data;
      if (data is Map<String, dynamic>) {
        final inner = data['data'] ?? data;
        if (inner is Map<String, dynamic>) return inner;
        return data;
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<List<Map<String, dynamic>>> getHighlightsForPost(String postId, {String? authToken}) async {
    try {
      final token = authToken ?? await _authToken();
      _setAuthHeader(token);
      final r = await _dio.get<dynamic>('/api/v1/highlights/me/post/$postId');
      final raw = r.data;
      List<dynamic> list = const [];
      if (raw is List) {
        list = raw;
      } else if (raw is Map<String, dynamic>) {
        final d = raw['data'] ?? raw['items'] ?? raw['content'];
        if (d is List) list = d;
      }
      return list
          .whereType<Map<String, dynamic>>()
          .toList(growable: false);
    } catch (_) {
      return const [];
    }
  }

  Future<bool> deleteHighlight(String highlightId, {String? authToken}) async {
    try {
      final token = authToken ?? await _authToken();
      _setAuthHeader(token);
      await _dio.delete<dynamic>('/api/v1/highlights/me/$highlightId');
      return true;
    } catch (_) {
      return false;
    }
  }
}
