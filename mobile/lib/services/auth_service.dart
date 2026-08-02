// =============================================================================
// AuthService — MapMyTimes auth-service (Java Spring Boot)
// Endpoints: /api/v1/auth/*  +  /api/v1/user/*
// Mirror: frontend/src/lib/api/authApi.ts
// =============================================================================

import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../core/env.dart';
import '../models/auth_models.dart';
import '../models/blog_models.dart';
import 'blog_service.dart';
import 'common.dart';

class AuthService {
  AuthService._(this._dio, this._store, this._blog);
  final Dio _dio;
  final TokenStore _store;
  final BlogService? _blog;
  static const _authV1 = '/api/v1/auth';
  static const _userV1 = '/api/v1/user';

  static AuthService create({
    required SharedPreferences prefs,
    required TokenStore store,
    required BlogService blog,
    Dio? existing,
  }) {
    final s = AuthService._(
      existing ?? createDio(base: Env.authBaseUrl.isEmpty ? Env.apiBaseUrl : Env.authBaseUrl),
      store,
      blog,
    );
    s._applyStoredToken();
    return s;
  }

  // --- Token -------------------------------------------------------------
  void _applyStoredToken() {
    final t = _store.accessToken;
    if (t != null && t.isNotEmpty) setBearerToken(t);
  }

  void setBearerToken(String? token) {
    if (token == null || token.isEmpty) {
      _dio.options.headers.remove('Authorization');
      _blog?.setBearerToken(null);
      return;
    }
    _dio.options.headers['Authorization'] = 'Bearer $token';
    _blog?.setBearerToken(token);
  }

  TokenStore get store => _store;
  bool get isLoggedIn => _store.hasSession;

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------
  String _qs(Map<String, Object?> params) {
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

  APIResponse<T> _env<T>(Response r, T Function(Object?) p) => parseEnvelope<T>(r, p);
  T _unwrap<T>(APIResponse<T> e) => unwrapEnvelope<T>(e);

  // ===========================================================================
  // REGISTER / VERIFY
  // ===========================================================================
  Future<AuthResponse> register(RegisterRequest req) async {
    final r = await _dio.post('$_authV1/register', data: req.toJson());
    final env = _env<AuthResponse>(r, (Object? j) => AuthResponse.fromJson(Map<String, dynamic>.from(j as Map)));
    final auth = _unwrap(env);
    await _persistAuth(auth);
    return auth;
  }

  Future<UserResponse> verifyEmail(VerifyEmailRequest req) async {
    final r = await _dio.post('$_authV1/verify-email', data: req.toJson());
    final env = _env<UserResponse>(r, (Object? j) => j == null ? throw 'no-user' : UserResponse.fromJson(Map<String, dynamic>.from(j as Map)));
    return _unwrap(env);
  }

  Future<String> resendVerification(ResendVerificationRequest req) async {
    final r = await _dio.post('$_authV1/send-verification-otp', data: req.toJson());
    final env = _env<String>(r, (Object? j) => (j is Map ? ((j['message'] ?? '')) : (j ?? '')).toString());
    try { return _unwrap(env); } catch (_) { return 'Verification OTP sent'; }
  }

  Future<Map<String, bool>> checkEmail(String email) async {
    final q = _qs({'email': email});
    final r = await _dio.get('$_authV1/check-email$q');
    final env = _env<Map<String, dynamic>>(r, (Object? j) => j is Map ? Map<String, dynamic>.from(j) : <String, dynamic>{});
    final m = _unwrap(env);
    return <String, bool>{
      'available': (m['available'] as bool?) ?? false,
      'exists': (m['exists'] as bool?) ?? true,
    };
  }

  // ===========================================================================
  // LOGIN
  // ===========================================================================
  Future<AuthResponse> login(LoginRequest req) async {
    final r = await _dio.post('$_authV1/login', data: req.toJson());
    final env = _env<AuthResponse>(r, (Object? j) => AuthResponse.fromJson(Map<String, dynamic>.from(j as Map)));
    final auth = _unwrap(env);
    await _persistAuth(auth);
    return auth;
  }

  Future<AuthResponse> loginWithOtp(LoginWithOtpRequest req) async {
    final r = await _dio.post('$_authV1/login-otp', data: req.toJson());
    final env = _env<AuthResponse>(r, (Object? j) => AuthResponse.fromJson(Map<String, dynamic>.from(j as Map)));
    final a = _unwrap(env);
    await _persistAuth(a);
    return a;
  }

  Future<String> sendLoginOtp(SendOtpRequest req) async {
    final r = await _dio.post('$_authV1/send-otp', data: req.toJson());
    final env = _env<String>(r, (Object? j) => (j is Map ? ((j['message'] ?? '')) : (j ?? '')).toString());
    try { return _unwrap(env); } catch (_) { return 'OTP sent'; }
  }

  Future<AuthResponse> refresh() async {
    final rt = _store.refreshToken;
    if (rt == null || rt.isEmpty) throw Exception('No refresh token');
    final r = await _dio.post('$_authV1/refresh', data: RefreshTokenRequest(rt).toJson());
    final env = _env<AuthResponse>(r, (Object? j) => AuthResponse.fromJson(Map<String, dynamic>.from(j as Map)));
    final a = _unwrap(env);
    await _persistAuth(a);
    return a;
  }

  // ===========================================================================
  // LOGOUT
  // ===========================================================================
  Future<void> logout() async {
    try {
      await _dio.post('$_authV1/logout', data: _store.userId == null ? null : <String, dynamic>{'sessionId': _store.userId});
    } catch (_) {}
    await _store.clear();
    setBearerToken(null);
  }

  Future<void> logoutAll() async {
    try { await _dio.post('$_authV1/logout-all'); } catch (_) {}
    await _store.clear();
    setBearerToken(null);
  }

  // ===========================================================================
  // PASSWORD
  // ===========================================================================
  Future<String> forgotPasswordStep1(ForgotPasswordStep1Request req) async {
    final r = await _dio.post('$_authV1/forgot-password/step1', data: req.toJson());
    final env = _env<String>(r, (Object? j) => (j is Map ? ((j['message'] ?? '')) : (j ?? '')).toString());
    try { return _unwrap(env); } catch (_) { return 'Reset link/OTP sent'; }
  }

  Future<String> forgotPasswordStep2(ForgotPasswordStep2Request req) async {
    final r = await _dio.post('$_authV1/forgot-password/step2', data: req.toJson());
    final env = _env<String>(r, (Object? j) => (j is Map ? ((j['message'] ?? '')) : (j ?? '')).toString());
    try { return _unwrap(env); } catch (_) { return 'OTP verified'; }
  }

  Future<String> resetPassword(ResetPasswordRequest req) async {
    final r = await _dio.post('$_authV1/reset-password', data: req.toJson());
    final env = _env<String>(r, (Object? j) => (j is Map ? ((j['message'] ?? '')) : (j ?? '')).toString());
    try { return _unwrap(env); } catch (_) { return 'Password reset'; }
  }

  Future<String> changePassword(ChangePasswordRequest req) async {
    final r = await _dio.post('$_authV1/change-password', data: req.toJson());
    final env = _env<String>(r, (Object? j) => (j is Map ? ((j['message'] ?? '')) : (j ?? '')).toString());
    try { return _unwrap(env); } catch (_) { return 'Password changed'; }
  }

  // ===========================================================================
  // PROFILE / ME
  // ===========================================================================
  Future<UserResponse> me() async {
    final r = await _dio.get('$_authV1/profile');
    final env = _env<UserResponse>(r, (Object? j) => UserResponse.fromJson(Map<String, dynamic>.from(j as Map)));
    final u = _unwrap(env);
    await _store.setSession(
      access: _store.accessToken ?? '',
      userId: u.id,
      email: u.email,
      name: u.displayName,
      refresh: _store.refreshToken,
    );
    return u;
  }

  Future<UserResponse> updateProfile(Map<String, dynamic> patch) async {
    final r = await _dio.put('$_userV1/profile', data: patch);
    final env = _env<UserResponse>(r, (Object? j) => UserResponse.fromJson(Map<String, dynamic>.from(j as Map)));
    return _unwrap(env);
  }

  Future<AccountStatusResponse> accountStatus() async {
    final r = await _dio.get('$_authV1/account-status');
    final env = _env<AccountStatusResponse>(r, (Object? j) => AccountStatusResponse.fromJson(Map<String, dynamic>.from(j as Map)));
    return _unwrap(env);
  }

  // ===========================================================================
  // OAUTH2 — external providers (google / facebook) via flutter_web_auth_2 URL
  // ===========================================================================
  Uri oauth2AuthorizeUrl(String provider, {String? redirectScheme}) {
    final scheme = redirectScheme ?? Env.linkScheme;
    final q = <String, Object?>{
      'provider': provider,
      'redirect_uri': '$scheme://oauth-callback',
    };
    return Uri.parse('${Env.authBaseUrl.isEmpty ? Env.apiBaseUrl : Env.authBaseUrl}$_authV1/oauth2/authorize${_qs(q).replaceFirst('?', '')}');
  }

  // ===========================================================================
  // INTERNAL: persist tokens to SharedPreferences + hydrate BlogService bearer
  // ===========================================================================
  Future<void> _persistAuth(AuthResponse auth) async {
    setBearerToken(auth.accessToken);
    await _store.setSession(
      access: auth.accessToken,
      refresh: auth.refreshToken,
      userId: auth.user?.id,
      email: auth.user?.email,
      name: auth.user?.displayName,
    );
  }
}
