import 'dart:ui';
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

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.child});
  final Widget child;
  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> with TickerProviderStateMixin {
  late final PageController _homeShortsCtl;
  int _hs = 0; // 0 = Home, 1 = Shorts (only applies when tab==0 main shell index)

  @override
  void initState() {
    super.initState();
    _homeShortsCtl = PageController(initialPage: 0, viewportFraction: 1.0);
  }
  @override
  void dispose() {
    _homeShortsCtl.dispose();
    super.dispose();
  }

  int get _index {
    final String loc = GoRouterState.of(context).uri.toString();
    if (loc.startsWith('/saved')) return 1;
    if (loc.startsWith('/videos')) return 2;
    if (loc.startsWith('/categories')) return 3;
    if (loc.startsWith('/profile')) return 4;
    if (loc.startsWith('/search')) return 5;
    if (loc.startsWith('/shorts')) return 6;
    if (loc == '/' || loc.isEmpty) return 0;
    return 99;
  }

  Future<void> _openSearch() async {
    try { FocusScope.of(context).unfocus(); } catch (_) {}
    if (!mounted) return;
    try { GoRouter.of(context).go('/search'); } catch (e) {
      try { context.go('/search'); } catch (_) {}
    }
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
    final int tab = _index;
    final bool homeShell = (tab == 0);
    final bool showTopTabs = homeShell; // segmented tabs ONLY on Home shell (swipe Home/Shorts)
    final bool homeSelected = (_hs == 0);
    final bool shortsSelected = (_hs == 1);

    // ---- Build body: Home shell = swipe PageView Home|Shorts; else = shell child ----
    final Widget body = homeShell
        ? PageView(
            controller: _homeShortsCtl,
            onPageChanged: (i) => mounted ? setState(() => _hs = i) : null,
            scrollDirection: Axis.horizontal,
            physics: const PageScrollPhysics(parent: ClampingScrollPhysics()),
            children: const [
              HomeScreen(),
              ShortsFeedScreen(),
            ],
          )
        : widget.child;

    return Scaffold(
      extendBody: true,
      extendBodyBehindAppBar: true,
      drawerEdgeDragWidth: 30,
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(102),
        child: ClipRRect(
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 22, sigmaY: 22, tileMode: TileMode.clamp),
            child: Container(
              color: (dark ? MmtColors.ink900 : Colors.white).withValues(alpha: dark ? 0.55 : 0.62),
              child: SafeArea(
                bottom: false,
                child: Stack(
                  clipBehavior: Clip.none,
                  children: [
                    // ===== LAYER 1: Row = [ LEFT block | MIDDLE (spacer + tabs + spacer) | RIGHT block ] =====
                    // Tabs float in CENTER region BETWEEN left & right blocks (Spacer weighted), true visual CENTER regardless of screen width
                    Padding(
                      padding: const EdgeInsets.fromLTRB(10, 6, 10, 2),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          // -------- LEFT BLOCK: Hamburger + Big Logo (no Expanded) --------
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            crossAxisAlignment: CrossAxisAlignment.center,
                            children: [
                              Builder(
                                builder: (ctx2) => InkWell(
                                  onTap: () => Scaffold.of(ctx2).openDrawer(),
                                  borderRadius: BorderRadius.circular(999),
                                  child: Container(
                                    height: 40,
                                    width: 40,
                                    alignment: Alignment.center,
                                    child: FaIcon(
                                      FontAwesomeIcons.bars,
                                      size: 20,
                                      color: dark ? Colors.white : MmtColors.ink950,
                                    ),
                                  ),
                                ),
                              ),
                              const SizedBox(width: 6),
                              const BrandLogo(size: 44),
                            ],
                          ),
                          // -------- MIDDLE: Home/Shorts tabs (Spacer wrapped for exact CENTER between left & right) --------
                          if (showTopTabs) ...[
                            const Spacer(),
                            Row(
                              mainAxisSize: MainAxisSize.min,
                              crossAxisAlignment: CrossAxisAlignment.center,
                              children: [
                                _SegmentTab(
                                  label: t.navHome,
                                  selected: homeSelected,
                                  dark: dark,
                                  onTap: () {
                                    setState(() => _hs = 0);
                                    _homeShortsCtl.animateToPage(0, duration: const Duration(milliseconds: 260), curve: Curves.easeOutCubic);
                                  },
                                ),
                                const SizedBox(width: 12),
                                _SegmentTab(
                                  label: t.shorts,
                                  selected: shortsSelected,
                                  dark: dark,
                                  onTap: () {
                                    setState(() => _hs = 1);
                                    _homeShortsCtl.animateToPage(1, duration: const Duration(milliseconds: 260), curve: Curves.easeOutCubic);
                                  },
                                ),
                              ],
                            ),
                            const Spacer(),
                          ] else
                            const Spacer(),
                          // -------- RIGHT BLOCK: Search (no Expanded) --------
                          const SizedBox(width: 10),
                          Material(
                            color: Colors.transparent,
                            child: InkWell(
                              onTap: _openSearch,
                              borderRadius: BorderRadius.circular(999),
                              child: Container(
                                height: 44,
                                width: 44,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  border: Border.all(color: dark ? Colors.white24 : MmtColors.ink200, width: 1),
                                  color: dark ? MmtColors.ink850.withValues(alpha: 0.60) : Colors.white.withValues(alpha: 0.80),
                                ),
                                alignment: Alignment.center,
                                child: FaIcon(
                                  FontAwesomeIcons.magnifyingGlass,
                                  size: 19,
                                  color: dark ? Colors.white : MmtColors.ink950,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                        ],
                      ),
                    ),
                    // ===== LAYER 2: UNDERLINE INDICATOR (at very bottom) =====
                    if (showTopTabs)
                      Positioned(
                        left: 0,
                        right: 0,
                        bottom: 8,
                        child: Padding(
                          padding: const EdgeInsets.only(left: 10 + 90, right: 10 + 40),
                          child: Align(
                            alignment: Alignment.center,
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                _Underline(visible: homeSelected),
                                const SizedBox(width: 12),
                                _Underline(visible: shortsSelected),
                              ],
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
      drawer: Drawer(
        width: MediaQuery.of(ctx).size.width * 0.86,
        backgroundColor: Colors.transparent,
        elevation: 0,
        child: ClipRRect(
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 26, sigmaY: 26, tileMode: TileMode.clamp),
            child: Container(
              decoration: BoxDecoration(
                color: (dark ? MmtColors.ink900 : Colors.white).withValues(alpha: dark ? 0.78 : 0.82),
                border: Border(
                  right: BorderSide(color: dark ? Colors.white24 : MmtColors.ink200, width: 1),
                ),
              ),
              child: ListView(
                padding: const EdgeInsets.fromLTRB(0, 56, 0, 22),
                children: [
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 22),
                    child: BrandLogo(size: 28, showTagline: true),
                  ),
                  const SizedBox(height: 24),
                  const Divider(color: MmtColors.ink950, thickness: 2, height: 2, indent: 22, endIndent: 22),
                  const SizedBox(height: 10),
                  _DrawerItem(icon: FaIcon(FontAwesomeIcons.house, size: 18, color: dark ? Colors.white : MmtColors.ink950), label: t.navHome, onTap: () => _goto('/')),
                  _DrawerItem(icon: FaIcon(FontAwesomeIcons.newspaper, size: 18, color: dark ? Colors.white : MmtColors.ink950), label: t.news, onTap: () => _goto('/news')),
                  _DrawerItem(icon: FaIcon(FontAwesomeIcons.video, size: 18, color: dark ? Colors.white : MmtColors.ink950), label: t.videos, onTap: () => _goto('/videos')),
                  _DrawerItem(
                    icon: FaIcon(FontAwesomeIcons.bowlFood, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                    label: t.shorts,
                    onTap: () {
                      Navigator.pop(ctx);
                      context.push('/shorts');
                    },
                  ),
                  const SizedBox(height: 6),
                  Divider(color: dark ? Colors.white12 : MmtColors.ink200, height: 1, thickness: 1, indent: 22, endIndent: 22),
                  const SizedBox(height: 6),
                  _DrawerItem(icon: FaIcon(FontAwesomeIcons.magnifyingGlass, size: 18, color: dark ? Colors.white : MmtColors.ink950), label: t.search, onTap: _openSearch),
                  _DrawerItem(
                    icon: FaIcon(FontAwesomeIcons.compass, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                    label: 'Explore Categories',
                    onTap: () => _goto('/categories'),
                  ),
                  _DrawerItem(
                    icon: FaIcon(FontAwesomeIcons.users, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                    label: t.ourTeam,
                    onTap: () => _goto('/our-team'),
                  ),
                  _DrawerItem(
                    icon: FaIcon(FontAwesomeIcons.shieldHalved, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                    label: t.verifyPress,
                    onTap: () => _goto('/verify-press'),
                  ),
                  _DrawerItem(
                    icon: FaIcon(FontAwesomeIcons.circleInfo, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                    label: t.about,
                    onTap: () {
                      Navigator.pop(ctx);
                      context.push('/about');
                    },
                  ),
                  _DrawerItem(
                    icon: FaIcon(FontAwesomeIcons.envelope, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                    label: t.contact,
                    onTap: () {
                      Navigator.pop(ctx);
                      context.push('/contact');
                    },
                  ),
                  _DrawerItem(
                    icon: FaIcon(FontAwesomeIcons.briefcase, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                    label: t.careers,
                    onTap: () {
                      Navigator.pop(ctx);
                      context.push('/careers');
                    },
                  ),
                  _DrawerItem(
                    icon: FaIcon(FontAwesomeIcons.gaugeHigh, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                    label: t.dashboard,
                    onTap: () {
                      Navigator.pop(ctx);
                      context.push('/dashboard');
                    },
                  ),
                  _DrawerItem(
                    icon: FaIcon(FontAwesomeIcons.rightToBracket, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                    label: t.signIn,
                    onTap: () {
                      Navigator.pop(ctx);
                      context.push('/login');
                    },
                  ),
                  const SizedBox(height: 22),
                  Divider(color: dark ? Colors.white12 : MmtColors.ink200, height: 1, thickness: 1, indent: 22, endIndent: 22),
                  const SizedBox(height: 14),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 22),
                    child: Text(t.footer.followUs, style: GoogleFonts.inter(fontSize: 12, fontWeight: FontWeight.w800, color: dark ? MmtColors.ink600 : MmtColors.ink700, letterSpacing: 0.3)),
                  ),
                  const SizedBox(height: 10),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 22),
                    child: Wrap(
                      spacing: 10, runSpacing: 10,
                      children: [
                        _socBtn(FontAwesomeIcons.facebookF, Env.socialFacebook, dark),
                        _socBtn(FontAwesomeIcons.xTwitter, Env.socialTwitter, dark),
                        _socBtn(FontAwesomeIcons.instagram, Env.socialInstagram, dark),
                        _socBtn(FontAwesomeIcons.youtube, Env.socialYoutube, dark),
                        _socBtn(FontAwesomeIcons.linkedinIn, Env.socialLinkedin, dark),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 22),
                    child: Text('© ${DateTime.now().year} MAPMYTOUR LLP, India · ${t.footer.copyright}', style: GoogleFonts.inter(fontSize: 10.5, fontWeight: FontWeight.w600, color: MmtColors.ink600)),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
      body: body,
      bottomNavigationBar: SafeArea(
        top: false,
        minimum: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(999),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20, tileMode: TileMode.clamp),
            child: Container(
              height: 66,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(999),
                color: (dark ? MmtColors.ink900 : Colors.white).withValues(alpha: dark ? 0.65 : 0.72),
                border: Border.all(
                  color: (dark ? Colors.white : MmtColors.ink950).withValues(alpha: dark ? 0.16 : 0.10),
                  width: 1,
                ),
                boxShadow: [
                  BoxShadow(
                    color: (dark ? Colors.black : MmtColors.ink950).withValues(alpha: 0.18),
                    offset: const Offset(0, 12),
                    blurRadius: 32,
                    spreadRadius: -8,
                  ),
                ],
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _CnnTab(
                    icon: FontAwesomeIcons.house,
                    selected: tab == 0,
                    dark: dark,
                    onTap: () => _onTab(0),
                  ),
                  _CnnTab(
                    icon: FontAwesomeIcons.bookmark,
                    selected: tab == 1,
                    dark: dark,
                    onTap: () => _onTab(1),
                  ),
                  _CnnTab(
                    icon: FontAwesomeIcons.video,
                    selected: tab == 2,
                    dark: dark,
                    big: true,
                    onTap: () => _onTab(2),
                  ),
                  _CnnTab(
                    icon: FontAwesomeIcons.compass,
                    selected: tab == 3,
                    dark: dark,
                    onTap: () => _onTab(3),
                  ),
                  _CnnTab(
                    icon: FontAwesomeIcons.user,
                    selected: tab == 4,
                    dark: dark,
                    onTap: () => _onTab(4),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _socBtn(FaIconData ic, String url, bool dark) {
    return InkWell(
      borderRadius: BorderRadius.circular(6),
      onTap: () async {
        try { await launchUrlString(url, mode: LaunchMode.externalApplication); } catch (_) {}
      },
      child: Container(
        width: 38, height: 38, alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border.all(color: dark ? Colors.white24 : MmtColors.ink950, width: 1.6),
          color: dark ? Colors.white.withValues(alpha: 0.06) : Colors.white,
          borderRadius: BorderRadius.circular(8),
        ),
        child: FaIcon(ic, size: 14, color: dark ? Colors.white : MmtColors.ink950),
      ),
    );
  }

  void _onTab(int i) {
    switch (i) {
      case 0:
        context.go('/');
        if (mounted) {
          setState(() => _hs = 0);
          if (_homeShortsCtl.hasClients) {
            _homeShortsCtl.animateToPage(0, duration: const Duration(milliseconds: 220), curve: Curves.easeOutCubic);
          }
        }
        break;
      case 1:
        context.go('/saved');
        break;
      case 2:
        context.go('/videos');
        break;
      case 3:
        context.go('/categories');
        break;
      case 4:
        context.go('/profile');
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
  final Widget icon;
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
      leading: icon,
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
  final Widget icon;
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
        child: icon,
      ),
    );
  }
}

// ---------- Header segmented tab (CNN Top Stories / Shorts style) ----------
class _SegmentTab extends StatelessWidget {
  final String label;
  final bool selected;
  final bool dark;
  final VoidCallback onTap;
  const _SegmentTab({required this.label, required this.selected, required this.dark, required this.onTap});

  @override
  Widget build(BuildContext ctx) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(4),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 2, vertical: 8),
        child: Text(
          label,
          style: GoogleFonts.inter(
            fontSize: 15,
            fontWeight: selected ? FontWeight.w800 : FontWeight.w600,
            color: selected ? (dark ? Colors.white : MmtColors.ink950) : (dark ? Colors.white60 : MmtColors.textMuted),
          ),
        ),
      ),
    );
  }
}

// ---------- Header underline indicator (CNN selected-tab underline) ----------
class _Underline extends StatelessWidget {
  final bool visible;
  const _Underline({required this.visible});

  @override
  Widget build(BuildContext ctx) {
    return Flexible(
      child: LayoutBuilder(
        builder: (ctx, c) {
          final width = c.maxWidth.isFinite ? c.maxWidth : 80.0;
          final target = visible ? width : 0.0;
          return Align(
            alignment: Alignment.centerLeft,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 220),
              curve: Curves.easeOutCubic,
              height: 2,
              width: target,
              decoration: const BoxDecoration(
                color: MmtColors.news,
                borderRadius: BorderRadius.zero,
              ),
            ),
          );
        },
      ),
    );
  }
}

// ---------- CNN-style footer pill tab (5 icons flat, selected = RED FILLED CAPSULE) ----------
class _CnnTab extends StatelessWidget {
  final FaIconData icon;
  final bool selected;
  final bool dark;
  final bool big;
  final VoidCallback onTap;
  const _CnnTab({
    required this.icon,
    required this.selected,
    required this.dark,
    required this.onTap,
    this.big = false,
  });

  @override
  Widget build(BuildContext ctx) {
    final double iconSize = big ? 26 : 20;
    final double capPad = big ? 10 : 2;
    final Color fg = selected
        ? Colors.white
        : (dark ? Colors.white70 : MmtColors.ink700);
    final Color bg = selected
        ? MmtColors.news
        : Colors.transparent;
    final List<BoxShadow> shadow = (big && selected)
        ? [
            BoxShadow(
              color: MmtColors.news.withValues(alpha: 0.42),
              offset: const Offset(0, 6),
              blurRadius: 16,
              spreadRadius: -2,
            ),
          ]
        : const [];
    return Expanded(
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: capPad),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          decoration: BoxDecoration(
            color: bg,
            borderRadius: BorderRadius.circular(999),
            border: big
                ? Border.all(color: selected ? Colors.white : Colors.transparent, width: selected ? 1.4 : 0)
                : null,
            boxShadow: shadow,
          ),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: onTap,
              borderRadius: BorderRadius.circular(999),
              child: Container(
                alignment: Alignment.center,
                child: FaIcon(icon, size: iconSize, color: fg),
              ),
            ),
          ),
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
          color: MmtColors.ink600.withValues(alpha: 0.12),
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
                    boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '⚠ ${t.common.loadingError}',
                        style: const TextStyle(color: MmtColors.news700, fontWeight: FontWeight.w900, fontSize: 14),
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
                            boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)],
                          ),
                          child: Text(t.common.retry.toUpperCase(),
                              style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 11, letterSpacing: 1.4),),
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
