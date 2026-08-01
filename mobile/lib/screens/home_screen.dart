// ---------------- HOME SCREEN ----------------
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../core/env.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../core/widgets/brand.dart';
import '../models/blog_models.dart';
import '../services/blog_service.dart';
import '../widgets/news_card.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  final _service = BlogService.create();
  late final Future<_HomeData> _loader = _load();

  Future<_HomeData> _load() async {
    final featured = await _service.postsList(
      status: 'PUBLISHED',
      isFeatured: true,
      size: 3,
      sort: '-publishedAt',
    );
    final trending = await _service.postsList(
      status: 'PUBLISHED',
      isTrending: true,
      size: 6,
    );
    final latest = await _service.postsList(
      status: 'PUBLISHED',
      page: 1,
      size: 12,
      sort: '-publishedAt',
    );
    final shorts = await _service.postsList(
      status: 'PUBLISHED',
      postType: 'short',
      size: 8,
      sort: '-publishedAt',
    );
    final categories = await _service.categoriesList();
    return _HomeData(
      featured: featured.items,
      trending: trending.items,
      latest: latest.items,
      shorts: shorts.items,
      categories: categories,
    );
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final dark = Theme.of(context).brightness == Brightness.dark;
    final t = LangScope.of(context);

    return RefreshIndicator(
      onRefresh: () async {
        setState(() {});
      },
      color: MmtColors.news,
      backgroundColor: Colors.white,
      strokeWidth: 3,
      child: CustomScrollView(
        slivers: [
          _buildSliverAppBar(dark, t),
          SliverToBoxAdapter(
            child: Container(
              decoration: const BoxDecoration(
                border: Border(bottom: BorderSide(color: MmtColors.ink950, width: 2)),
              ),
              child: FutureBuilder<_HomeData>(
                future: _loader,
                builder: (ctx, snap) {
                  if (snap.connectionState == ConnectionState.waiting) {
                    return _loading(ctx, t);
                  }
                  if (snap.hasError) return _error(ctx, t, snap.error);
                  final data = snap.data!;
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _hero(ctx, t, dark, data),
                      _sectionEyebrow(t.featuredReports),
                      _featured(ctx, data.featured),
                      _sectionEyebrow(t.trendingNow),
                      _trending(ctx, t, data.trending),
                      _sectionEyebrow(t.latestStories),
                      _latest(ctx, data.latest),
                      _sectionEyebrow(t.categories),
                      _categories(ctx, data.categories),
                      const SizedBox(height: 32),
                      Container(
                        height: 2,
                        color: MmtColors.ink950,
                      ),
                      Padding(
                        padding: const EdgeInsets.fromLTRB(20, 24, 20, 120),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            SectionEyebrow(t.followUs),
                            const SizedBox(height: 24),
                            Text(
                              t.mission,
                              style: GoogleFonts.inter(
                                fontSize: 14,
                                height: 1.65,
                                color: dark ? Colors.white70 : MmtColors.textMuted,
                              ),
                            ),
                            const SizedBox(height: 24),
                            Text(
                              t.copyrightYear(DateTime.now().year),
                              style: GoogleFonts.inter(
                                fontSize: 12,
                                fontWeight: FontWeight.w600,
                                letterSpacing: 0.5,
                                color: dark ? Colors.white38 : MmtColors.textFaint,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSliverAppBar(bool dark, Dict t) {
    return SliverAppBar(
      pinned: true,
      floating: false,
      snap: false,
      expandedHeight: 140,
      collapsedHeight: 96,
      backgroundColor: dark ? MmtColors.ink950 : MmtColors.surface,
      surfaceTintColor: Colors.transparent,
      bottom: PreferredSize(
        preferredSize: const Size.fromHeight(2),
        child: Container(height: 2, color: MmtColors.ink950),
      ),
      flexibleSpace: FlexibleSpaceBar(
        titlePadding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
        centerTitle: false,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 44),
            const BrandLogo(size: 18, showTagline: false),
            const SizedBox(height: 8),
            SizedBox(
              height: 36,
              child: Row(
                children: [
                  Expanded(
                    child: InkWell(
                      onTap: () => context.push('/search'),
                      child: Container(
                        decoration: BoxDecoration(
                          color: dark ? MmtColors.ink900 : Colors.white,
                          border: const Border.fromBorderSide(
                            BorderSide(color: MmtColors.ink950, width: 2),
                          ),
                        ),
                        padding: const EdgeInsets.symmetric(horizontal: 10),
                        child: Row(
                          children: [
                            const Icon(Icons.search_rounded, size: 18),
                            const SizedBox(width: 8),
                            Text(
                              t.search,
                              style: GoogleFonts.inter(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: dark ? Colors.white54 : MmtColors.textMuted,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  InkWell(
                    onTap: () => LangScope.toggle(context),
                    child: Container(
                      height: 36,
                      width: 48,
                      alignment: Alignment.center,
                      decoration: const BoxDecoration(
                        color: MmtColors.news,
                        border: Border.fromBorderSide(
                          BorderSide(color: MmtColors.ink950, width: 2),
                        ),
                      ),
                      child: Text(
                        LangScope.codeOf(context) == LangCode.en ? 'EN' : 'हि',
                        style: GoogleFonts.getFont(
                          'Archivo Black',
                          fontSize: 13,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 0.6,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _hero(BuildContext ctx, Dict t, bool dark, _HomeData data) {
    final hero = (data.featured.isNotEmpty) ? data.featured.first : null;
    return Container(
      color: dark ? MmtColors.ink900 : MmtColors.news50,
      child: Container(
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: MmtColors.ink950, width: 2)),
        ),
        padding: const EdgeInsets.fromLTRB(20, 32, 20, 28),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: const BoxDecoration(
                color: MmtColors.news,
                border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
              ),
              child: Text(
                t.tagline.toUpperCase(),
                style: GoogleFonts.getFont(
                  'Archivo Black',
                  fontSize: 11,
                  letterSpacing: 2.2,
                  fontWeight: FontWeight.w900,
                  color: Colors.white,
                  height: 1.0,
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text(
              t.heroTitle,
              style: GoogleFonts.getFont(
                'Archivo Black',
                fontSize: 34,
                height: 1.0,
                letterSpacing: -0.8,
                fontWeight: FontWeight.w900,
                color: dark ? Colors.white : MmtColors.ink950,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              t.heroBody,
              style: GoogleFonts.inter(
                fontSize: 15,
                height: 1.6,
                color: dark ? Colors.white70 : MmtColors.ink900,
              ),
            ),
            if (hero != null) ...[
              const SizedBox(height: 24),
              NewsCardVertical(
                post: hero,
                onTap: () => _openArticle(ctx, hero),
                compact: false,
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _sectionEyebrow(String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 28, 20, 16),
      child: SectionEyebrow(title),
    );
  }

  Widget _featured(BuildContext ctx, List<BlogPostSummaryResponse> items) {
    final list = items.length > 1 ? items.sublist(math.min(1, items.length)) : items;
    if (list.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Column(
        children: [
          for (final p in list) ...[
            NewsCardVertical(
              post: p,
              compact: false,
              onTap: () => _openArticle(ctx, p),
            ),
            const SizedBox(height: 18),
          ],
        ],
      ),
    );
  }

  Widget _trending(BuildContext ctx, Dict t, List<BlogPostSummaryResponse> items) {
    if (items.isEmpty) return _empty(t.noStoriesYet);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Container(
        decoration: const BoxDecoration(
          color: Colors.transparent,
          border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 4, 12, 4),
          child: Column(
            children: [
              for (int i = 0; i < items.length; i++) ...[
                TrendingItem(
                  index: i,
                  post: items[i],
                  onTap: () => _openArticle(ctx, items[i]),
                ),
                if (i != items.length - 1)
                  const Divider(
                    height: 1,
                    thickness: 1,
                    color: MmtColors.divider,
                  ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _latest(BuildContext ctx, List<BlogPostSummaryResponse> items) {
    if (items.isEmpty) return _empty(LangScope.of(context).noStoriesYet);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Column(
        children: [
          for (final p in items) ...[
            NewsCardVertical(
              post: p,
              compact: true,
              onTap: () => _openArticle(ctx, p),
            ),
            const SizedBox(height: 18),
          ],
        ],
      ),
    );
  }

  Widget _categories(BuildContext ctx, List<CategoryResponse> cats) {
    if (cats.isEmpty) return const SizedBox.shrink();
    final shown = cats.take(14).toList(growable: false);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Wrap(
        spacing: 10,
        runSpacing: 10,
        children: shown.map((c) {
          return MmtChip(label: c.name.toUpperCase());
        }).toList(),
      ),
    );
  }

  Widget _empty(String label) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            border: Border.all(color: MmtColors.ink950, width: 2),
            color: Theme.of(ctx).brightness == Brightness.dark ? MmtColors.ink900 : MmtColors.chipBg,
          ),
          child: Text(label),
        ),
      );

  Widget _loading(BuildContext ctx, Dict t) => SizedBox(
        height: 240,
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const SizedBox(
                width: 36,
                height: 36,
                child: CircularProgressIndicator(
                  color: MmtColors.news,
                  strokeWidth: 4,
                ),
              ),
              const SizedBox(height: 16),
              Text(t.loading),
            ],
          ),
        ),
      );

  Widget _error(BuildContext ctx, Dict t, Object? err) => Padding(
        padding: const EdgeInsets.all(20),
        child: Container(
          decoration: BoxDecoration(
            border: Border.all(color: MmtColors.danger, width: 2),
            color: Theme.of(ctx).brightness == Brightness.dark ? MmtColors.ink900 : Colors.white,
          ),
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(t.somethingWentWrong),
              const SizedBox(height: 10),
              if (err != null)
                Text(
                  err.toString(),
                  maxLines: 6,
                  style: const TextStyle(fontSize: 12, color: MmtColors.textMuted),
                ),
              const SizedBox(height: 14),
              OutlinedButton(
                onPressed: () => setState(() {}),
                child: Text(t.retry),
              ),
            ],
          ),
        ),
      );

  void _openArticle(BuildContext ctx, BlogPostSummaryResponse p) {
    if (p.postType == PostType.short) {
      context.push('/shorts?startId=${p.id}&startSlug=${p.slug}');
      return;
    }
    context.push('/article/${p.slug}?id=${p.id}');
  }
}

// ----------- Bundled data class for home screen future -----------
class _HomeData {
  final List<BlogPostSummaryResponse> featured;
  final List<BlogPostSummaryResponse> trending;
  final List<BlogPostSummaryResponse> latest;
  final List<BlogPostSummaryResponse> shorts;
  final List<CategoryResponse> categories;
  const _HomeData({
    required this.featured,
    required this.trending,
    required this.latest,
    required this.shorts,
    required this.categories,
  });
}
