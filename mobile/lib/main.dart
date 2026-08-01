import 'dart:async';
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
            name: 'news-root',
            pageBuilder: (context, state) => NoTransitionPage<void>(
              key: state.pageKey,
              child: const NewsListScreen(),
            ),
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
            pageBuilder: (context, state) => NoTransitionPage<void>(
              key: state.pageKey,
              child: const MenuScreen(),
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
          ),
        ),
      ),
      GoRoute(
        path: '/shorts',
        name: 'shorts-feed',
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
        path: '/search',
        name: 'search',
        parentNavigatorKey: _rootNavigatorKey,
        pageBuilder: (context, state) => MaterialPage<void>(
          key: state.pageKey,
          child: const SearchScreen(),
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
    ));

    await Env.load();

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

  @override
  Widget build(BuildContext context) {
    final GoRouter router = ref.watch(routerProvider);
    final LangCode lang = LangScope.codeOf(context);

    return MaterialApp.router(
      title: Env.appName,
      debugShowCheckedModeBanner: Env.isDebug,
      routerConfig: router,
      theme: MmtTheme.build(),
      darkTheme: MmtTheme.build(brightness: Brightness.dark),
      themeMode: ThemeMode.light,
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
