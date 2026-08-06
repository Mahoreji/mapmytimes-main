// =============================================================================
// NotificationService + SupportService contact form (customer-support + notification go
// Mirror: frontend/src/lib/api/notificationApi.ts   (line 21 contact-form
// =============================================================================

import 'package:dio/dio.dart';
import '../core/env.dart';
import '../models/blog_models.dart';
import '../models/notification_models.dart';
import 'common.dart';

class SupportService {
  SupportService._(this._dio);
  final Dio _dio;

  static SupportService create({Dio? existing}) =>
      SupportService._(existing ?? createDio(base: Env.apiBaseUrl));

  void setBearerToken(String? token) {
    if (token == null || token.isEmpty) {
      _dio.options.headers.remove('Authorization');
    } else {
      _dio.options.headers['Authorization'] = 'Bearer $token';
    }
  }

  Future<String> submitContactForm(ContactFormRequest req) async {
    try {
      const url = '/api/v1/notification/contact-form';
      final r = await _dio.post(url, data: req.toJson());
      if (r.data is Map<String, dynamic>) {
        final env = APIResponse<String>.fromJson(
          r.data as Map<String, dynamic>,
          (Object? j) => (j is Map ? (j['message'] ?? '') : (j ?? '')).toString(),
        );
        try { return unwrapEnvelope(env); } catch (_) { return env.message ?? 'Message sent'; }
      }
      return 'Message sent';
    } catch (e) {
      return 'Message sent';
    }
  }
}

class NotificationService {
  NotificationService._(this._dio);
  final Dio _dio;
  static const _base = '/api/v1/notification';

  static NotificationService create({Dio? existing}) =>
      NotificationService._(existing ?? createDio(base: Env.apiBaseUrl));

  void setBearerToken(String? token) {
    if (token == null || token.isEmpty) {
      _dio.options.headers.remove('Authorization');
    } else {
      _dio.options.headers['Authorization'] = 'Bearer $token';
    }
  }

  APIResponse<T> _env<T>(Response r, T Function(Object?) p) => parseEnvelope<T>(r, p);
  T _unwrap<T>(APIResponse<T> e) => unwrapEnvelope(e);

  String _q(Map<String, Object?> p) {
    final parts = <String>[];
    p.forEach((k, v) {
      if (v == null) return;
      if (v is Iterable) { for (final e in v) parts.add('$k=${Uri.encodeQueryComponent(e.toString())}'); return; }
      final s = v.toString(); if (s.isEmpty) return;
      parts.add('$k=${Uri.encodeQueryComponent(s)}');
    });
    return parts.isEmpty ? '' : '?${parts.join('&')}';
  }

  Future<Map<String, dynamic>> health() async {
    try {
      final r = await _dio.get('/api/v1/health');
      return (r.data is Map) ? Map<String, dynamic>.from(r.data as Map) : <String, dynamic>{'status': 'ok'};
    } catch (_) {
      return <String, dynamic>{'status': 'unknown'};
    }
  }

  Future<List<NotificationItem>> list({int page = 1, int size = 50}) async {
    final q = _q({'page': page, 'size': size});
    final r = await _dio.get('$_base$q');
    final env = _env<List<NotificationItem>>(r, (Object? j) {
      List l = const [];
      if (j is List) l = j;
      else if (j is Map<String, dynamic>) {
        l = (j['items'] ?? j['notifications'] ?? j['content'] ?? const []) as List;
      }
      return l
          .map((e) => NotificationItem.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(growable: false);
    });
    try { return _unwrap(env); } catch (_) { return <NotificationItem>[]; }
  }

  Future<UnreadCountResponse> unreadCount() async {
    try {
      final r = await _dio.get('$_base/unread-count');
      if (r.data is Map<String, dynamic>) {
        final m = r.data as Map<String, dynamic>;
        final inner = m['data'] is Map<String, dynamic> ? (m['data'] as Map<String, dynamic>) : m;
        return UnreadCountResponse.fromJson(Map<String, dynamic>.from(inner));
      }
      return const UnreadCountResponse();
    } catch (_) {
      return const UnreadCountResponse();
    }
  }

  Future<void> markRead(ID id) async {
    try { await _dio.patch('$_base/$id/read'); } catch (_) {}
  }

  Future<void> markAllRead() async {
    try { await _dio.patch('$_base/read-all'); } catch (_) {}
  }
}
