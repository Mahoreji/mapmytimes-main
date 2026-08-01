// =============================================================================
// Common providers — SharedPreferences + TokenStore + services
// MUST be overridden in ProviderScope (main.dart) with real values.
// =============================================================================

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/auth_service.dart';
import '../services/blog_service.dart';
import '../services/careers_service.dart';
import '../services/common.dart';
import '../services/support_and_notification.dart';

// ---- SharedPreferences is synchronously available (preloaded via main) ----
final sharedPreferencesProvider = Provider<SharedPreferences>((ref) {
  throw UnimplementedError('Override sharedPreferencesProvider in ProviderScope');
});

final tokenStoreProvider = Provider<TokenStore>((ref) {
  final prefs = ref.watch(sharedPreferencesProvider);
  return TokenStore(prefs);
});

// ---- HTTP services ----------------------------------------------------------
final blogServiceProvider = Provider<BlogService>((ref) {
  final tokenStore = ref.watch(tokenStoreProvider);
  final blog = BlogService.create();
  if (tokenStore.accessToken != null && tokenStore.accessToken!.isNotEmpty) {
    blog.setBearerToken(tokenStore.accessToken);
  }
  return blog;
});

final careersServiceProvider = Provider<CareersService>((ref) {
  final tokenStore = ref.watch(tokenStoreProvider);
  final c = CareersService.create();
  if (tokenStore.accessToken != null && tokenStore.accessToken!.isNotEmpty) {
    c.setBearerToken(tokenStore.accessToken);
  }
  return c;
});

final supportServiceProvider = Provider<SupportService>((ref) => SupportService.create());
final notificationServiceProvider = Provider<NotificationService>((ref) {
  final tokenStore = ref.watch(tokenStoreProvider);
  final n = NotificationService.create();
  if (tokenStore.accessToken != null && tokenStore.accessToken!.isNotEmpty) {
    n.setBearerToken(tokenStore.accessToken);
  }
  return n;
});

final authServiceProvider = Provider<AuthService>((ref) {
  final prefs = ref.watch(sharedPreferencesProvider);
  final store = ref.watch(tokenStoreProvider);
  final blog = ref.watch(blogServiceProvider);
  // IMPORTANT: AuthService must hydrate BlogService bearer
  final auth = AuthService.create(prefs: prefs, store: store, blog: blog);
  return auth;
});

final unreadCountProvider = FutureProvider.autoDispose<int>((ref) async {
  try {
    final svc = ref.watch(notificationServiceProvider);
    final uc = await svc.unreadCount();
    return uc.unread;
  } catch (_) {
    return 0;
  }
});
