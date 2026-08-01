import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher_string.dart';
import '../core/env.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../core/widgets/brand.dart';
import '../models/blog_models.dart';
import '../providers/index.dart';
import 'home_screen.dart';
import 'shorts_feed.dart';
import 'news_article_screen.dart';
import 'static_screens.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.child});
  final Widget child;
  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int get _index {
    final String loc = GoRouterState.of(context).uri.toString();
    if (loc.startsWith('/news')) return 1;
    if (loc.startsWith('/videos')) return 2;
    if (loc.startsWith('/menu')) return 4;
    return 0;
  }

  Future<void> _openSearch() async {
    context.push('/search');
  }

  Future<void> _launchSocial(String url) async {
    try {
      if (await canLaunchUrlString(url)) {
        await launchUrlString(url, mode: LaunchMode.externalApplication);
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext ctx) {
    final t = LangScope.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;

    return Scaffold(
      drawer: Drawer(
        backgroundColor: dark ? MmtColors.ink900 : Colors.white,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.zero,
          side: BorderSide(color: MmtColors.ink950, width: 2),
        ),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(0, 60, 0, 20),
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 20),
              child: BrandLogo(size: 22, showTagline: true),
            ),
            const SizedBox(height: 24),
            const Divider(color: MmtColors.ink950, thickness: 2, height: 2),
            _DrawerItem(icon: FontAwesomeIcons.house, label: t.home, onTap: () => _goto('/')),
            _DrawerItem(icon: FontAwesomeIcons.newspaper, label: t.news, onTap: () => _goto('/news')),
            _DrawerItem(icon: FontAwesomeIcons.video, label: t.videos, onTap: () => _goto('/videos')),
            _DrawerItem(
              icon: FontAwesomeIcons.bowlFood,
              label: t.shorts,
              onTap: () {
                Navigator.pop(ctx);
                context.push('/shorts');
              },
            ),
            const Divider(color: MmtColors.divider, height: 1, thickness: 1),
            _DrawerItem(icon: FontAwesomeIcons.magnifyingGlass, label: t.search, onTap: _openSearch),
            _DrawerItem(
              icon: FontAwesomeIcons.circleInfo,
              label: t.about,
              onTap: () {
                Navigator.pop(ctx);
                context.push('/about');
              },
            ),
            _DrawerItem(
              icon: FontAwesomeIcons.envelope,
              label: t.contact,
              onTap: () {
                Navigator.pop(ctx);
                context.push('/contact');
              },
            ),
            _DrawerItem(
              icon: FontAwesomeIcons.briefcase,
              label: t.careers,
              onTap: () {
                Navigator.pop(ctx);
                context.push('/careers');
              },
            ),
            _DrawerItem(
              icon: FontAwesomeIcons.gaugeHigh,
              label: t.dashboard,
              onTap: () {
                Navigator.pop(ctx);
                context.push('/dashboard');
              },
            ),
            _DrawerItem(
              icon: FontAwesomeIcons.rightToBracket,
              label: t.signIn,
              onTap: () {
                Navigator.pop(ctx);
                context.push('/login');
              },
            ),
            const Divider(color: MmtColors.divider, height: 1, thickness: 1),
            const SizedBox(height: 20),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Text(
                t.followUs,
                style: GoogleFonts.getFont(
                  'Archivo Black',
                  fontSize: 12,
                  letterSpacing: 1.8,
                  color: dark ? Colors.white70 : MmtColors.textMuted,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
            const SizedBox(height: 12),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Row(
                children: [
                  _SocialIcon(icon: FontAwesomeIcons.facebookF, onTap: () => _launchSocial(Env.facebook)),
                  const SizedBox(width: 10),
                  _SocialIcon(icon: FontAwesomeIcons.xTwitter, onTap: () => _launchSocial(Env.twitter)),
                  const SizedBox(width: 10),
                  _SocialIcon(icon: FontAwesomeIcons.instagram, onTap: () => _launchSocial(Env.instagram)),
                  const SizedBox(width: 10),
                  _SocialIcon(icon: FontAwesomeIcons.youtube, onTap: () => _launchSocial(Env.youtube)),
                  const SizedBox(width: 10),
                  _SocialIcon(icon: FontAwesomeIcons.linkedinIn, onTap: () => _launchSocial(Env.linkedin)),
                ],
              ),
            ),
            const SizedBox(height: 30),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Text(
                t.copyrightYear(DateTime.now().year),
                style: GoogleFonts.inter(
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  color: dark ? Colors.white38 : MmtColors.textFaint,
                ),
              ),
            ),
          ],
        ),
      ),
      body: widget.child,
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          border: Border(top: BorderSide(color: MmtColors.ink950, width: 2)),
        ),
        child: BottomNavigationBar(
          currentIndex: _index,
          onTap: _onTab,
          type: BottomNavigationBarType.fixed,
          showSelectedLabels: true,
          showUnselectedLabels: true,
          elevation: 0,
          backgroundColor: dark ? MmtColors.ink900 : Colors.white,
          items: [
            BottomNavigationBarItem(
              icon: const Icon(FontAwesomeIcons.house),
              label: t.home,
            ),
            BottomNavigationBarItem(
              icon: const Icon(FontAwesomeIcons.newspaper),
              label: t.news,
            ),
            BottomNavigationBarItem(
              icon: const Icon(FontAwesomeIcons.video),
              label: t.videos,
            ),
            BottomNavigationBarItem(
              icon: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => context.push('/shorts'),
                child: const Icon(FontAwesomeIcons.bowlFood),
              ),
              label: t.shorts,
            ),
            BottomNavigationBarItem(
              icon: const Icon(Icons.menu_rounded),
              label: 'Menu',
            ),
          ],
        ),
      ),
    );
  }

  void _onTab(int i) {
    switch (i) {
      case 0:
        context.go('/');
        break;
      case 1:
        context.go('/news');
        break;
      case 2:
        context.go('/videos');
        break;
      case 3:
        context.push('/shorts');
        break;
      case 4:
        context.go('/menu');
        break;
    }
  }

  void _goto(String path) {
    Navigator.pop(context);
    context.go(path);
  }
}

// ---------- Drawer list item ----------
class _DrawerItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  const _DrawerItem({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 2),
      horizontalTitleGap: 16,
      leading: Icon(icon, size: 18, color: dark ? Colors.white : MmtColors.ink950),
      title: Text(
        label,
        style: GoogleFonts.inter(
          fontSize: 15,
          fontWeight: FontWeight.w700,
          color: dark ? Colors.white : MmtColors.ink950,
        ),
      ),
      onTap: onTap,
      shape: const Border(bottom: BorderSide(color: MmtColors.divider, width: 1)),
    );
  }
}

// ---------- Social icon button (drawer footer) ----------
class _SocialIcon extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  const _SocialIcon({required this.icon, required this.onTap});

  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    return InkWell(
      onTap: onTap,
      child: Container(
        height: 36,
        width: 36,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border.all(color: dark ? Colors.white24 : MmtColors.ink950, width: 2),
          color: dark ? MmtColors.ink950 : Colors.white,
        ),
        child: Icon(
          icon,
          size: 15,
          color: dark ? Colors.white70 : MmtColors.ink950,
        ),
      ),
    );
  }
}

// ========================================================================
// Standalone News List screen (route: /news) used from shell bottom nav tab 1
// INTEGRATED: newsListProvider Riverpod, RefreshIndicator, error retry cards
// ========================================================================
class NewsListScreen extends ConsumerStatefulWidget {
  const NewsListScreen({super.key});
  @override
  ConsumerState<NewsListScreen> createState() => _NewsListScreenState();
}

class _NewsListScreenState extends ConsumerState<NewsListScreen>
    with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  Widget _skel(double h, {double w = double.infinity}) => Container(
        height: h,
        width: w,
        decoration: BoxDecoration(
          border: Border.all(color: MmtColors.ink700, width: 2),
          color: MmtColors.ink600.withOpacity(0.12),
        ),
      );

  Future<void> _onRefresh() async {
    ref.invalidate(newsListProvider);
    await ref.read(newsListProvider.future);
  }

  @override
  Widget build(BuildContext ctx) {
    super.build(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final t = Dict.of(ctx);
    final listAsync = ref.watch(newsListProvider);

    return RefreshIndicator(
      color: MmtColors.news,
      onRefresh: _onRefresh,
      child: CustomScrollView(
        slivers: [
          SliverAppBar(
            pinned: true,
            floating: false,
            backgroundColor: dark ? MmtColors.ink950 : MmtColors.surface,
            surfaceTintColor: Colors.transparent,
            title: Text(t.news),
            titleTextStyle: GoogleFonts.getFont(
              'Archivo Black',
              fontSize: 22,
              fontWeight: FontWeight.w900,
              letterSpacing: -0.2,
              color: dark ? Colors.white : MmtColors.ink950,
            ),
            bottom: PreferredSize(
              preferredSize: const Size.fromHeight(2),
              child: Container(height: 2, color: MmtColors.ink950),
            ),
          ),
          listAsync.when(
            loading: () => SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
                child: Column(
                  children: [
                    for (int i = 0; i < 6; i++) ...[
                      _skel(104),
                      const SizedBox(height: 18),
                    ],
                  ],
                ),
              ),
            ),
            error: (e, _) => SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    border: Border.all(color: MmtColors.ink950, width: 2),
                    color: MmtColors.news50,
                    boxShadow: const BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '⚠ ${t.common.loadingError}',
                        style: TextStyle(color: MmtColors.news700, fontWeight: FontWeight.w900, fontSize: 14),
                      ),
                      const SizedBox(height: 8),
                      Text(e.toString(), style: const TextStyle(fontSize: 12, color: MmtColors.ink700)),
                      const SizedBox(height: 14),
                      InkWell(
                        onTap: _onRefresh,
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                          decoration: BoxDecoration(
                            border: Border.all(color: MmtColors.ink950, width: 2),
                            color: Colors.white,
                            boxShadow: const BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950),
                          ),
                          child: Text(t.common.retry.toUpperCase(),
                              style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 11, letterSpacing: 1.4)),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            data: (items) {
              if (items.isEmpty) {
                return SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.all(30),
                    child: Center(child: Text(t.noStoriesYet)),
                  ),
                );
              }
              return SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
                  child: Column(
                    children: [
                      for (final p in items) ...[
                        Padding(
                          padding: const EdgeInsets.only(bottom: 18),
                          child: InkWell(
                            onTap: () {
                              ctx.push('/article/${p.slug}?id=${p.id}');
                            },
                            child: _NewsListTile(p: p),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}

class _NewsListTile extends StatelessWidget {
  final BlogPostSummaryResponse p;
  const _NewsListTile({required this.p});

  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final cat = (p.categories?.isNotEmpty ?? false)
        ? p.categories!.first.name.toUpperCase()
        : 'NEWS';
    final date = p.publishedAt != null
        ? DateFormat('dd MMM yyyy').format(p.publishedAt!.toLocal())
        : '';
    return Container(
      decoration: const BoxDecoration(
        border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
        boxShadow: [BoxShadow(color: MmtColors.ink950, offset: Offset(4, 4))],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 124,
            height: 104,
            decoration: const BoxDecoration(
              border: Border(
                right: BorderSide(color: MmtColors.ink950, width: 2),
              ),
            ),
            child: p.cover.isEmpty
                ? Container(color: dark ? MmtColors.ink800 : MmtColors.chipBg)
                : CachedNetworkImage(imageUrl: p.cover, fit: BoxFit.cover),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    color: MmtColors.news,
                    child: Text(
                      cat,
                      style: GoogleFonts.inter(
                        fontSize: 10,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 1.0,
                        color: Colors.white,
                        height: 1.0,
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    p.title,
                    maxLines: 3,
                    overflow: TextOverflow.ellipsis,
                    style: GoogleFonts.getFont(
                      'Archivo Black',
                      fontSize: 16,
                      height: 1.15,
                      fontWeight: FontWeight.w900,
                      letterSpacing: -0.1,
                      color: dark ? Colors.white : MmtColors.ink950,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    [
                      if (p.author?.name.isNotEmpty ?? false) p.author!.name,
                      if (date.isNotEmpty) date,
                      if ((p.viewCount ?? 0) > 0) '${p.viewCount} views',
                    ].join('  ·  '),
                    style: GoogleFonts.inter(
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: dark ? Colors.white54 : MmtColors.textFaint,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
