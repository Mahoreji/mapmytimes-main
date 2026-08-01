// ---------------- HOME SCREEN ----------------
// INTEGRATED: Riverpod providers (featured/trending/latest/shorts/categories)
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../core/env.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../core/widgets/brand.dart';
import '../models/blog_models.dart';
import '../providers/index.dart';
import '../widgets/news_card.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  Future<void> _onRefresh() async {
    ref.invalidate(featuredPostsProvider);
    ref.invalidate(trendingPostsProvider);
    ref.invalidate(latestPostsProvider(1));
    ref.invalidate(shortsFeedProvider);
    ref.invalidate(categoriesProvider);
    await Future.wait(<Future<void>>[
      ref.read(featuredPostsProvider.future),
      ref.read(trendingPostsProvider.future),
      ref.read(latestPostsProvider(1).future),
      ref.read(shortsFeedProvider.future),
      ref.read(categoriesProvider.future),
    ]);
  }

  Widget _skel(double h, {double w = double.infinity}) => Container(
        height: h,
        width: w,
        decoration: BoxDecoration(
          border: Border.all(color: MmtColors.ink700, width: 2),
          color: MmtColors.ink600.withOpacity(0.15),
        ),
      );

  Widget _sectionErr(String msg, VoidCallback retry) => Padding(
        padding: const EdgeInsets.all(12),
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            border: Border.all(color: MmtColors.ink950, width: 2),
            color: MmtColors.news50,
            boxShadow: const BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('⚠ ${Dict.of(context).common.loadingError}', style: TextStyle(color: MmtColors.news700, fontWeight: FontWeight.w900, fontSize: 13)),
              const SizedBox(height: 6),
              Text(msg, style: const TextStyle(fontSize: 12, color: MmtColors.ink700, fontWeight: FontWeight.w600)),
              const SizedBox(height: 10),
              InkWell(
                onTap: retry,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                  decoration: BoxDecoration(
                    border: Border.all(color: MmtColors.ink950, width: 2),
                    color: Colors.white,
                    boxShadow: const BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950),
                  ),
                  child: Text(Dict.of(context).common.retry.toUpperCase(),
                      style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 11, letterSpacing: 1.4)),
                ),
              ),
            ],
          ),
        ),
      );

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final dark = Theme.of(context).brightness == Brightness.dark;
    final t = Dict.of(context);

    final featured = ref.watch(featuredPostsProvider);
    final trending = ref.watch(trendingPostsProvider);
    final latest = ref.watch(latestPostsProvider(1));
    final shorts = ref.watch(shortsFeedProvider);
    final cats = ref.watch(categoriesProvider);
    final unread = ref.watch(unreadCountProvider);

    return RefreshIndicator(
      onRefresh: _onRefresh,
      color: MmtColors.news,
      backgroundColor: Colors.white,
      strokeWidth: 3,
      child: CustomScrollView(
        slivers: [
          _buildSliverAppBar(dark, t, unread.value),
          SliverToBoxAdapter(
            child: Container(
              decoration: BoxDecoration(
                color: MmtColors.news50,
                border: Border(bottom: BorderSide(color: MmtColors.ink950, width: 2)),
              ),
              padding: const EdgeInsets.fromLTRB(18, 20, 18, 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SectionEyebrow(label: t.home.heroEyebrow),
                  const SizedBox(height: 12),
                  Text(t.home.heroTitle,
                      style: GoogleFonts.archivoBlack(
                        fontSize: 30,
                        height: 1.02,
                        color: dark ? Colors.white : MmtColors.ink950,
                        letterSpacing: -0.4,
                      )),
                  const SizedBox(height: 10),
                  Text(t.home.heroBody, style: TextStyle(fontSize: 13, color: dark ? MmtColors.ink600 : MmtColors.ink700, height: 1.6, fontWeight: FontWeight.w500)),
                ],
              ),
            ),
          ),
          // Featured Reports
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 24, 18, 6),
              child: SectionEyebrow(label: t.home.featured),
            ),
          ),
          featured.when(
            loading: () => SliverPadding(
              padding: const EdgeInsets.fromLTRB(18, 12, 18, 0),
              sliver: SliverList.separated(
                itemCount: 2,
                separatorBuilder: (_, __) => const SizedBox(height: 14),
                itemBuilder: (_, __) => _skel(240),
              ),
            ),
            error: (e, _) => SliverToBoxAdapter(child: _sectionErr(e.toString(), () => ref.invalidate(featuredPostsProvider))),
            data: (list) => SliverPadding(
              padding: const EdgeInsets.fromLTRB(18, 12, 18, 0),
              sliver: SliverList.separated(
                itemCount: list.length,
                separatorBuilder: (_, __) => const SizedBox(height: 14),
                itemBuilder: (_, i) => NewsCardVertical(list[i], onTap: () => _openPost(context, list[i])),
              ),
            ),
          ),
          // Trending Now
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 28, 18, 10),
              child: Row(
                children: [
                  SectionEyebrow(label: t.home.trending),
                  const Spacer(),
                  Icon(FontAwesomeIcons.fire, size: 16, color: MmtColors.news),
                ],
              ),
            ),
          ),
          trending.when(
            loading: () => SliverPadding(
              padding: const EdgeInsets.fromLTRB(18, 0, 18, 0),
              sliver: SliverList.separated(
                itemCount: 4,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (_, __) => _skel(76),
              ),
            ),
            error: (e, _) => SliverToBoxAdapter(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 18), child: _sectionErr(e.toString(), () => ref.invalidate(trendingPostsProvider)))),
            data: (list) {
              if (list.isEmpty) {
                return const SliverToBoxAdapter(child: SizedBox.shrink());
              }
              return SliverPadding(
                padding: const EdgeInsets.fromLTRB(18, 0, 18, 0),
                sliver: SliverList.separated(
                  itemCount: list.length > 6 ? 6 : list.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 10),
                  itemBuilder: (_, i) => InkWell(
                    onTap: () => _openPost(context, list[i]),
                    child: TrendingItem(i + 1, list[i]),
                  ),
                ),
              );
            },
          ),
          // Latest Stories
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 28, 18, 10),
              child: SectionEyebrow(label: t.home.latest),
            ),
          ),
          latest.when(
            loading: () => SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 18),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, mainAxisSpacing: 14, crossAxisSpacing: 14, childAspectRatio: 0.62),
                delegate: SliverChildBuilderDelegate((_, __) => _skel(180), childCount: 4),
              ),
            ),
            error: (e, _) => SliverToBoxAdapter(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 18), child: _sectionErr(e.toString(), () => ref.invalidate(latestPostsProvider(1)))),
            data: (list) => SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 18),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, mainAxisSpacing: 14, crossAxisSpacing: 14, childAspectRatio: 0.58),
                delegate: SliverChildBuilderDelegate(
                  (_, i) => NewsCardVertical(list[i], compact: true, onTap: () => _openPost(context, list[i])),
                  childCount: list.length > 12 ? 12 : list.length,
                ),
              ),
            ),
          ),
          // Categories
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 28, 18, 12),
              child: SectionEyebrow(label: t.home.categories),
            ),
          ),
          cats.when(
            loading: () => SliverToBoxAdapter(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 18), child: Wrap(spacing: 8, runSpacing: 8, children: List<Widget>.generate(6, (i) => _skel(28, w: 80))))),
            error: (e, _) => const SliverToBoxAdapter(child: SizedBox.shrink()),
            data: (list) => SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 18),
                child: Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: list
                      .asMap()
                      .entries
                      .map((e) => MmtChip(
                            label: e.value.name,
                            selected: e.key == 0,
                            onTap: () {
                              final slug = e.value.slug;
                              if (slug != null) context.push('/category/$slug');
                            },
                          ))
                      .toList(growable: false),
                ),
              ),
            ),
          ),
          // Footer
          const SliverToBoxAdapter(child: SizedBox(height: 32)),
          SliverToBoxAdapter(
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 18),
              height: 2,
              color: MmtColors.ink950,
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 22, 18, 30),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SectionEyebrow(label: t.footer.followUs),
                  const SizedBox(height: 16),
                  Text(t.footer.mission, style: TextStyle(fontSize: 12.5, color: dark ? MmtColors.ink600 : MmtColors.ink700, height: 1.7, fontWeight: FontWeight.w500)),
                  const SizedBox(height: 18),
                  Row(
                    children: [
                      _socBtn(FontAwesomeIcons.facebookF, Env.socialFacebook),
                      const SizedBox(width: 10),
                      _socBtn(FontAwesomeIcons.xTwitter, Env.socialTwitter),
                      const SizedBox(width: 10),
                      _socBtn(FontAwesomeIcons.instagram, Env.socialInstagram),
                      const SizedBox(width: 10),
                      _socBtn(FontAwesomeIcons.youtube, Env.socialYoutube),
                      const SizedBox(width: 10),
                      _socBtn(FontAwesomeIcons.linkedinIn, Env.socialLinkedin),
                    ],
                  ),
                  const SizedBox(height: 22),
                  Text('© ${DateTime.now().year} MAPMYTOUR LLP, India · ${t.footer.copyright}',
                      style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w600, color: MmtColors.ink600)),
                ],
              ),
            ),
          ),
          const SliverToBoxAdapter(child: SizedBox(height: 20)),
        ],
      ),
    );
  }

  Widget _socBtn(IconData ic, String url) {
    return InkWell(
      onTap: () async {
        try { await Env.launchUrlExternal(url); } catch (_) {}
      },
      child: Container(
        width: 36,
        height: 36,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border.all(color: MmtColors.ink950, width: 2),
          color: Colors.white,
        ),
        child: FaIcon(ic, size: 14, color: MmtColors.ink950),
      ),
    );
  }

  SliverAppBar _buildSliverAppBar(bool dark, Dict t, int? unread) {
    final Brightness mode = dark ? Brightness.dark : Brightness.light;
    return SliverAppBar(
      pinned: true,
      floating: false,
      backgroundColor: dark ? MmtColors.ink950 : Colors.white,
      foregroundColor: dark ? Colors.white : MmtColors.ink950,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      bottom: PreferredSize(
        preferredSize: const Size.fromHeight(2),
        child: Container(height: 2, color: MmtColors.ink950),
      ),
      titleSpacing: 16,
      title: Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Row(
          children: [
            const BrandLogo(size: 14),
            const Spacer(),
            InkWell(
              onTap: () => context.push('/search'),
              child: Container(
                height: 36,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                alignment: Alignment.centerLeft,
                decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: Colors.white),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.search, size: 16, color: MmtColors.ink700),
                    const SizedBox(width: 8),
                    Text(t.nav.search, style: TextStyle(fontSize: 12, color: MmtColors.ink600, fontWeight: FontWeight.w600)),
                  ],
                ),
              ),
            ),
            const SizedBox(width: 10),
            InkWell(
              onTap: () {
                final s = LangScope.of(context);
                LangScope.ofState(context).toggle();
                final newT = Dict.of(context);
                if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                  backgroundColor: MmtColors.ink950,
                  content: Text(newT.common.languageSwitched, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
                  duration: const Duration(milliseconds: 1200),
                ));
              },
              child: Container(
                width: 48,
                height: 36,
                alignment: Alignment.center,
                decoration: BoxDecoration(color: MmtColors.news, border: Border.all(color: MmtColors.ink950, width: 2)),
                child: Text(LangScope.codeOf(context).name.toUpperCase(),
                    style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 12, color: Colors.white, letterSpacing: 0.6)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _openPost(BuildContext ctx, BlogPostSummaryResponse p) {
    final PostType type = p.postType ?? PostType.article;
    if (type == PostType.short) {
      ctx.push('/shorts${p.slug != null ? '?startSlug=${Uri.encodeQueryComponent(p.slug!)}' : (p.id != null ? '?startId=${Uri.encodeQueryComponent(p.id!)}' : '')}');
    } else {
      final sp = <String, String>{};
      if (p.id != null) sp['id'] = p.id!;
      ctx.push('/article/${Uri.encodeQueryComponent(p.slug ?? p.id ?? '')}', extra: sp.isEmpty ? null : sp);
    }
  }
}


