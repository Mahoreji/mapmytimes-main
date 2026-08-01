// =============================================================================
// Auth state providers — login, logout, current user, guards
// =============================================================================

import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/auth_models.dart';
import 'common_providers.dart';

enum AuthStatus { unknown, authenticated, unauthenticated }

class AuthState {
  final AuthStatus status;
  final UserResponse? user;
  final bool loading;
  final String? error;

  const AuthState({
    this.status = AuthStatus.unknown,
    this.user,
    this.loading = false,
    this.error,
  });

  bool get isAuthenticated => status == AuthStatus.authenticated;
  AuthState copyWith({AuthStatus? status, UserResponse? user, bool? loading, String? error, bool clearUser = false, bool clearError = false}) => AuthState(
    status: status ?? this.status,
    user: clearUser ? null : (user ?? this.user),
    loading: loading ?? this.loading,
    error: clearError ? null : (error ?? this.error),
  );
}

class AuthController extends StateNotifier<AuthState> {
  AuthController(this.ref) : super(const AuthState()) {
    _bootstrap();
  }
  final Ref ref;

  Future<void> _bootstrap() async {
    try {
      final auth = ref.read(authServiceProvider);
      if (!auth.isLoggedIn) {
        state = state.copyWith(status: AuthStatus.unauthenticated, clearUser: true);
        return;
      }
      // Hydrate current user from /profile endpoint
      final me = await auth.me();
      state = state.copyWith(status: AuthStatus.authenticated, user: me, clearError: true);
    } catch (e) {
      state = state.copyWith(status: AuthStatus.unauthenticated, error: e.toString(), clearUser: true);
    }
  }

  Future<UserResponse?> login(LoginRequest req) async {
    state = state.copyWith(loading: true, clearError: true);
    try {
      final auth = ref.read(authServiceProvider);
      final res = await auth.login(req);
      UserResponse? u = res.user;
      try { u ??= await auth.me(); } catch (_) {}
      state = state.copyWith(loading: false, status: AuthStatus.authenticated, user: u, clearError: true);
      // Refresh unread, services tokens handled by AuthService setter already
      ref.invalidate(unreadCountProvider);
      return u;
    } catch (e) {
      state = state.copyWith(loading: false, error: e.toString());
      return null;
    }
  }

  Future<UserResponse?> register(RegisterRequest req) async {
    state = state.copyWith(loading: true, clearError: true);
    try {
      final auth = ref.read(authServiceProvider);
      final res = await auth.register(req);
      UserResponse? u = res.user;
      try { u ??= await auth.me(); } catch (_) {}
      state = state.copyWith(loading: false, status: AuthStatus.authenticated, user: u, clearError: true);
      return u;
    } catch (e) {
      state = state.copyWith(loading: false, error: e.toString());
      return null;
    }
  }

  Future<void> logout() async {
    state = state.copyWith(loading: true);
    try {
      await ref.read(authServiceProvider).logout();
    } catch (_) {}
    state = const AuthState(status: AuthStatus.unauthenticated);
    ref.invalidate(unreadCountProvider);
  }

  Future<void> refreshMe() async {
    try {
      final auth = ref.read(authServiceProvider);
      if (!auth.isLoggedIn) return;
      final u = await auth.me();
      state = state.copyWith(user: u, status: AuthStatus.authenticated);
    } catch (e) {
      state = state.copyWith(error: e.toString());
    }
  }
}

final authControllerProvider = StateNotifierProvider<AuthController, AuthState>(
  (ref) => AuthController(ref),
);

final currentUserProvider = Provider<UserResponse?>((ref) {
  return ref.watch(authControllerProvider).user;
});

final isAuthenticatedProvider = Provider<bool>((ref) {
  return ref.watch(authControllerProvider.select((s) => s.status == AuthStatus.authenticated));
});

final authLoadingProvider = Provider<bool>((ref) {
  return ref.watch(authControllerProvider.select((s) => s.loading));
});

final authErrorProvider = Provider<String?>((ref) {
  return ref.watch(authControllerProvider.select((s) => s.error));
});
