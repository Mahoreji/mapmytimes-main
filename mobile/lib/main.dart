import 'dart:async';
import 'dart:ui';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:mapmytimes/core/env.dart';
import 'package:mapmytimes/core/l10n/dict.dart';
import 'package:mapmytimes/core/theme/index.dart';
import 'package:mapmytimes/providers/index.dart';
import 'package:mapmytimes/screens/app_shell.dart';
import 'package:mapmytimes/screens/home_screen.dart';
import 'package:mapmytimes/screens/news_article_screen.dart';
import 'package:mapmytimes/screens/shorts_feed.dart';
import 'package:mapmytimes/screens/static_screens.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'services/offline_storage_service.dart';

// Flutter Web Path-URL strategy (instead of Hash #/) for clean URLs + deep links.
// ignore: depend_on_referenced_packages
import 'package:flutter_web_plugins/url_strategy.dart';

final GlobalKey<NavigatorState> _rootNavigatorKey =
    GlobalKey<NavigatorState>(debugLabel: 'root');
final GlobalKey<NavigatorState> _shellNavigatorKey =
    GlobalKey<NavigatorState>(debugLabel: 'shell');

// Protected routes — require authenticated session
const Set<String> _protectedRoutes = <String>{'dashboard', 'career-apply'};
const Set<String> _protectedPathPrefixes = <String>{'/dashboard', '/careers/'};

bool _isProtected(String matchedLocation) {
  if (matchedLocation == '/dashboard') return true;
  if (matchedLocation.startsWith('/careers/') && matchedLocation.endsWith('/apply')) return true;
  return false;
}

bool _isAuthGate(String matchedLocation) => matchedLocation == '/login';

final routerProvider = Provider<GoRouter>((ref) {
  // Listen to auth state changes so GoRouter re-evaluates redirects
  final authListenable = ValueNotifier<int>(0);
  ref.listen<AuthState>(authControllerProvider, (previous, next) {
    authListenable.value = authListenable.value + 1;
  });

  return GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: '/',
    debugLogDiagnostics: Env.isDebug,
    refreshListenable: authListenable,
    redirect: (BuildContext context, GoRouterState state) async {
      final AuthStatus status = ref.read(authControllerProvider).status;
      // Wait for bootstrap if needed (only block protected route during unknown briefly)
      bool known = status != AuthStatus.unknown;
      bool authenticated = ref.read(isAuthenticatedProvider);

      final to = state.matchedLocation;
      final goingToProtected = _isProtected(to);
      final goingToLogin = _isAuthGate(to);

      // Not yet logged in and going to protected → push login with deep-link return
      if (known && !authenticated && goingToProtected) {
        final returnTo = Uri.encodeQueryComponent(to.isEmpty ? '/' : to);
        return '/login?returnTo=$returnTo';
      }
      // Logged in and going to login screen → send to returnTo or home
      if (known && authenticated && goingToLogin) {
        return state.uri.queryParameters['returnTo'] ?? '/';
      }
      return null;
    },
    routes: <RouteBase>[
      ShellRoute(
        navigatorKey: _shellNavigatorKey,
        builder: (BuildContext context, GoRouterState state, Widget child) {
          return AppShell(child: child);
        },
        routes: <RouteBase>[
          GoRoute(
            path: '/',
            name: 'home',
            pageBuilder: (context, state) => NoTransitionPage<void>(
              key: state.pageKey,
              child: const HomeScreen(),
            ),
          ),
          GoRoute(
            path: '/news',
            name: 'news',
            redirect: (BuildContext ctx, GoRouterState st) => '/',
          ),
          GoRoute(
            path: '/videos',
            name: 'videos',
            pageBuilder: (context, state) => NoTransitionPage<void>(
              key: state.pageKey,
              child: const VideosScreen(),
            ),
          ),
          GoRoute(
            path: '/menu',
            name: 'menu',
            redirect: (BuildContext ctx, GoRouterState st) => '/profile',
          ),
          GoRoute(
            path: '/shorts',
            name: 'shorts-shell',
            pageBuilder: (context, state) => NoTransitionPage<void>(
              key: state.pageKey,
              child: const ShortsFeedScreen(),
            ),
          ),
          GoRoute(
            path: '/search',
            name: 'search-shell',
            pageBuilder: (context, state) => NoTransitionPage<void>(
              key: state.pageKey,
              child: const SearchScreen(),
            ),
          ),
          GoRoute(
            path: '/saved',
            name: 'saved',
            pageBuilder: (context, state) => NoTransitionPage<void>(
              key: state.pageKey,
              child: const SavedScreen(),
            ),
          ),
          GoRoute(
            path: '/categories',
            name: 'categories',
            pageBuilder: (context, state) => NoTransitionPage<void>(
              key: state.pageKey,
              child: const ExploreCategoriesScreen(),
            ),
          ),
          GoRoute(
            path: '/profile',
            name: 'profile',
            pageBuilder: (context, state) => NoTransitionPage<void>(
              key: state.pageKey,
              child: const ProfileScreen(),
            ),
          ),
        ],
      ),
      GoRoute(
        path: '/article/:slug',
        name: 'article',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          child: NewsArticleScreen(
            slug: state.pathParameters['slug']!,
            postId: state.uri.queryParameters['id'],
            resumePercent: () {
              final rp = state.uri.queryParameters['resumePercent'];
              if (rp == null) return null;
              final n = int.tryParse(rp);
              return n;
            }(),
          ),
        ),
      ),
      GoRoute(
        path: '/shorts-player',
        name: 'shorts-fullscreen',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) {
          final startId = state.uri.queryParameters['startId'];
          final startSlug = state.uri.queryParameters['startSlug'];
          return MaterialPage<void>(
            key: state.pageKey,
            fullscreenDialog: true,
            child: ShortsFeedScreen(
              startPostId: startId,
              startPostSlug: startSlug,
            ),
          );
        },
      ),
      GoRoute(
        path: '/category/:slug',
        name: 'category-feed',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          child: CategoryFeedScreen(
            slug: state.pathParameters['slug']!,
          ),
        ),
      ),
      GoRoute(
        path: '/section/:slug',
        name: 'section-feed',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          child: SectionFeedScreen(
            slug: state.pathParameters['slug']!,
          ),
        ),
      ),
      GoRoute(
        path: '/about',
        name: 'about',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          child: const AboutScreen(),
        ),
      ),
      GoRoute(
        path: '/contact',
        name: 'contact',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          child: const ContactScreen(),
        ),
      ),
      GoRoute(
        path: '/careers',
        name: 'careers',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          child: const CareersScreen(),
        ),
      ),
      GoRoute(
        path: '/careers/:id',
        name: 'career-detail',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          child: CareerDetailScreen(
            jobId: state.pathParameters['id']!,
          ),
        ),
      ),
      GoRoute(
        path: '/careers/:id/apply',
        name: 'career-apply',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          fullscreenDialog: true,
          child: CareerApplyScreen(jobId: state.pathParameters['id']!),
        ),
      ),
      GoRoute(
        path: '/login',
        name: 'login',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          fullscreenDialog: true,
          child: LoginScreen(
            returnTo: state.uri.queryParameters['returnTo'],
          ),
        ),
      ),
      GoRoute(
        path: '/dashboard',
        name: 'dashboard',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          child: const DashboardScreen(),
        ),
      ),
    ],
    errorBuilder: (BuildContext context, GoRouterState state) {
      final Brightness mode = Theme.of(context).brightness;
      return Scaffold(
        backgroundColor: MmtColors.background,
        appBar: AppBar(
          title: const Text('404'),
          backgroundColor: MmtColors.news500,
          foregroundColor: Colors.white,
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(MmtTokens.lg),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  '404 — Page not found',
                  style: MmtText.h2(mode: mode),
                ),
                const SizedBox(height: MmtTokens.md),
                OutlinedButton.icon(
                  onPressed: () => context.go('/'),
                  icon: const Icon(Icons.arrow_back_rounded),
                  label: const Text('Go home'),
                ),
              ],
            ),
          ),
        ),
      );
    },
  );
});

Future<void> main() async {
  runZonedGuarded<Future<void>>(() async {
    WidgetsFlutterBinding.ensureInitialized();

    // --- PRODUCTION ERROR SHIELDS: No yellow/black overflow stripes in Release mode ---
    ErrorWidget.builder = (FlutterErrorDetails details) {
      final bool isDebug = kDebugMode;
      FlutterError.dumpErrorToConsole(details, forceReport: isDebug);
      if (!kReleaseMode) {
        return ErrorWidget.withDetails(message: 'Error: ${details.exception}');
      }
      return Material(
        color: MmtColors.chipBg,
        child: Center(
          child: Icon(Icons.newspaper_rounded, size: 40, color: MmtColors.news.withValues(alpha: 0.65)),
        ),
      );
    };
    FlutterError.onError = (FlutterErrorDetails details) {
      if (kReleaseMode) {
        FlutterError.dumpErrorToConsole(details, forceReport: false);
      } else {
        FlutterError.presentError(details);
      }
    };
    PlatformDispatcher.instance.onError = (Object error, StackTrace stack) {
      debugPrint('PLATFORM error: $error\n$stack');
      return true;
    };

    // Use clean path-style URLs on Web (no /#/ hash fragment).
    // IMPORTANT: This also ensures deep links + browser refresh on any path works.
    if (kIsWeb) {
      usePathUrlStrategy();
    }

    await SystemChrome.setPreferredOrientations(<DeviceOrientation>[
      DeviceOrientation.portraitUp,
      DeviceOrientation.portraitDown,
    ]);

    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: MmtColors.ink950,
      statusBarIconBrightness: Brightness.light,
      statusBarBrightness: Brightness.dark,
      systemNavigationBarColor: MmtColors.ink950,
      systemNavigationBarIconBrightness: Brightness.light,
      systemNavigationBarDividerColor: Colors.transparent,
    ),);

    await Env.load();

    unawaited(OfflineStorageService.instance.init());

    // -------------------------------------------------------------------------
    // Preload synchronous dependencies so Riverpod overrides can use real values
    //  1) SharedPreferences (used by language restore + token store)
    //  2) Initial language code (EN/हि)
    // -------------------------------------------------------------------------
    final SharedPreferences prefs = await SharedPreferences.getInstance();
    final String? storedCode = prefs.getString('app_language');
    final LangCode initialLang = LangCode.values.firstWhere(
      (c) => c.name == (storedCode ?? LangCode.en.name),
      orElse: () => LangCode.en,
    );

    runApp(
      ProviderScope(
      // Override providers with preloaded synchronous values to avoid async gaps
      overrides: <Override>[
        sharedPreferencesProvider.overrideWithValue(prefs),
      ],
      child: LangScope(
        initialLang: initialLang,
        child: const MapMyTimesApp(),
        ),
      ),
    );
  }, (Object error, StackTrace stack) {
    debugPrint('RUNZONED error: $error\n$stack');
  });
}

class MapMyTimesApp extends ConsumerStatefulWidget {
  const MapMyTimesApp({super.key});

  @override
  ConsumerState<MapMyTimesApp> createState() => _MapMyTimesAppState();
}

class _MapMyTimesAppState extends ConsumerState<MapMyTimesApp> {
  @override
  void initState() {
    super.initState();
    // Kick off auth bootstrap (restore tokens + hydrate /profile).
    // Riverpod StateNotifier constructor calls _bootstrap() automatically.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // ignore: unused_result
      ref.refresh(authControllerProvider);
    });
  }

  ThemeMode _toTheme(MmtThemeMode m) {
    switch (m) {
      case MmtThemeMode.light:
        return ThemeMode.light;
      case MmtThemeMode.dark:
        return ThemeMode.dark;
      case MmtThemeMode.system:
        return ThemeMode.system;
    }
  }

  @override
  Widget build(BuildContext context) {
    final GoRouter router = ref.watch(routerProvider);
    final LangCode lang = LangScope.codeOf(context);
    final MmtThemeMode mode = ref.watch(themeModeNotifierProvider);

    return MaterialApp.router(
      title: Env.appName,
      debugShowCheckedModeBanner: false,
      routerConfig: router,
      theme: MmtTheme.build(Brightness.light),
      darkTheme: MmtTheme.build(Brightness.dark),
      themeMode: _toTheme(mode),
      locale: Locale(lang.name),
      supportedLocales: const <Locale>[
        Locale('en'),
        Locale('hi'),
      ],
      localizationsDelegates: const <LocalizationsDelegate<dynamic>>[
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
    );
  }
}
