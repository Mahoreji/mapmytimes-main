// =============================================================================
// TokenStore — persisted auth tokens in SharedPreferences, + envelope unwrap helpers
// shared between AuthService and all data-dependent service layer
// =============================================================================

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:pretty_dio_logger/pretty_dio_logger.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../core/env.dart';
import '../models/blog_models.dart';

class TokenStore {
  TokenStore(this._prefs);
  final SharedPreferences _prefs;
  static const _kAccessToken = 'mmt.auth.accessToken';
  static const _kRefreshToken = 'mmt.auth.refreshToken';
  static const _kUserId = 'mmt.auth.userId';
  static const _kEmail = 'mmt.auth.email';
  static const _kName = 'mmt.auth.displayName';

  // --- getters ---------------------------------------------------------------
  String? get accessToken => _prefs.getString(_kAccessToken);
  String? get refreshToken => _prefs.getString(_kRefreshToken);
  String? get userId => _prefs.getString(_kUserId);
  String? get email => _prefs.getString(_kEmail);
  String? get displayName => _prefs.getString(_kName);
  bool get hasSession => (accessToken ?? '').isNotEmpty;

  // --- setters -----------------------------------------------------------
  Future<void> setSession({
    required String access,
    String? refresh,
    String? userId,
    String? email,
    String? name,
  }) async {
    await Future.wait([
      _prefs.setString(_kAccessToken, access),
      if (refresh != null) _prefs.setString(_kRefreshToken, refresh),
      if (userId != null) _prefs.setString(_kUserId, userId),
      if (email != null) _prefs.setString(_kEmail, email),
      if (name != null) _prefs.setString(_kName, name),
    ]);
  }

  Future<void> clear() async {
    await Future.wait([
      _prefs.remove(_kAccessToken),
      _prefs.remove(_kRefreshToken),
      _prefs.remove(_kUserId),
      _prefs.remove(_kEmail),
      _prefs.remove(_kName),
    ]);
  }
}

// ---------------------------------------------------------------------------
// Unwrap envelope — match frontend http.unwrap() contract — throw if envelope says failure
// ---------------------------------------------------------------------------
T unwrapEnvelope<T>(APIResponse<T> env) {
  final ok = env.success ?? (env.code != null && env.code! >= 200 && env.code! < 300);
  if (!ok) {
    throw Exception(env.message ?? 'Request failed (code=${env.code})');
  }
  if (env.data is T && env.data != null) return env.data as T;
  // If envelope returned null / missing
  try { return null as T; } catch (_) {
    throw Exception('Envelope had no data');
  }
}

APIResponse<T> parseEnvelope<T>(
  Response<dynamic> r,
  T Function(Object? json) dataParser,
) {
  final json = r.data;
  if (json is Map<String, dynamic>) {
    return APIResponse.fromJson(json, dataParser);
  }
  return APIResponse<T>(
    success: r.statusCode != null && r.statusCode! >= 200 && r.statusCode! < 300,
    code: r.statusCode,
    message: null,
    data: dataParser(r.data),
  );
}

PaginatedResponse<T> parsePaginated<T>(
  Object? json,
  T Function(Object? json) itemFromJson, {
  int fallbackPage = 1,
  int fallbackSize = 20,
}) {
  if (json == null) {
    return PaginatedResponse<T>(items: const [], page: fallbackPage, size: fallbackSize);
  }
  if (json is Map<String, dynamic>) {
    return PaginatedResponse.fromJson(json, itemFromJson);
  }
  if (json is List) {
    return PaginatedResponse<T>(
      items: json.map((e) => itemFromJson(e)).toList(growable: false),
      page: fallbackPage,
      size: json.length,
      total: json.length,
      totalPages: 1,
      hasMore: false,
    );
  }
  if (json is Map && json.containsKey('content')) {
    final list = (json['content'] as List<dynamic>?) ?? <dynamic>[];
    return PaginatedResponse<T>(
      items: list.map((e) => itemFromJson(e)).toList(growable: false),
      page: (json['page'] as num?)?.toInt() ?? fallbackPage,
      size: (json['size'] as num?)?.toInt() ?? fallbackSize,
      total: (json['totalElements'] ?? json['total']) as int?,
      totalPages: (json['totalPages'] as num?)?.toInt(),
      hasMore: json['hasMore'] as bool? ?? false,
    );
  }
  return PaginatedResponse<T>(items: const [], page: fallbackPage, size: fallbackSize);
}

Dio createDio({String? base, Duration connect = const Duration(seconds: 15), Map<String, String>? extra}) {
  final d = Dio(BaseOptions(
    baseUrl: base ?? Env.apiBaseUrl,
    connectTimeout: connect,
    receiveTimeout: const Duration(seconds: 25),
    sendTimeout: const Duration(seconds: 25),
    headers: <String, dynamic>{
      'Accept': 'application/json',
      'X-Request-Source': 'api-gateway',
      'X-Source': 'mapmytimes-mobile',
      if (extra != null) ...extra,
    },
  ),);
  if (kIsWeb) {
    d.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        options.headers.removeWhere((k, _) {
          final u = k.toUpperCase();
          if (u == 'X-REQUEST-SOURCE' || u == 'X-SOURCE') return false;
          return u.startsWith('X-');
        });
        return handler.next(options);
      },
    ),);
  }
  if (Env.isDebug) {
    try {
      d.interceptors.add(PrettyDioLogger(
        requestHeader: true,
        responseBody: false,
        error: true,
        compact: true,
        maxWidth: 120,
      ),);
    } catch (_) {}
  }
  return d;
}
