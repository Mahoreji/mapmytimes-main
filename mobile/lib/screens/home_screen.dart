// ---------------- HOME SCREEN ----------------
// INTEGRATED: Riverpod providers (featured/trending/latest/shorts/categories)
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:url_launcher/url_launcher.dart';
import '../core/api/reader_api.dart';
import '../core/env.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../core/widgets/brand.dart';
import '../models/blog_models.dart';
import '../providers/index.dart';
import '../widgets/news_card.dart';
import '../widgets/editorial_components.dart';
import 'static_screens.dart' show kMmtSections, SectionTile;

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> with AutomaticKeepAliveClientMixin {
  late final Future<List<ReaderContinueItem>> _continueReadingFuture;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _continueReadingFuture = ReaderApi.instance.getLatestReadingProgress(limit: 20);
  }

  Future<void> _onRefresh() async {
    ref.invalidate(featuredPostsProvider);
    ref.invalidate(trendingPostsProvider);
    ref.invalidate(latestPostsProvider(1));
    ref.invalidate(shortsFeedProvider);
    ref.invalidate(categoriesProvider);
    ref.invalidate(videoPostsProvider);
    await Future.wait(<Future<void>>[
      ref.read(featuredPostsProvider.future),
      ref.read(trendingPostsProvider.future),
      ref.read(latestPostsProvider(1).future),
      ref.read(shortsFeedProvider.future),
      ref.read(categoriesProvider.future),
      ref.read(videoPostsProvider.future),
    ]);
  }

  Widget _skel(double h, {double w = double.infinity}) => ClipRRect(
        borderRadius: MmtTokens.glassRadiusMd(),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurSm, sigmaY: MmtTokens.glassBlurSm),
          child: Container(
            height: h,
            width: w,
            decoration: BoxDecoration(
              borderRadius: MmtTokens.glassRadiusMd(),
              border: Border.all(color: Colors.black.withValues(alpha: 0.06), width: MmtTokens.glassHairline),
              color: MmtColors.ink600.withValues(alpha: 0.08),
              boxShadow: MmtTokens.glassShadowSm(),
            ),
          ),
        ),
      );

  Widget _sectionErr(String msg, VoidCallback retry) => Padding(
        padding: const EdgeInsets.all(12),
        child: ClipRRect(
          borderRadius: MmtTokens.glassRadiusMd(),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurMd, sigmaY: MmtTokens.glassBlurMd),
            child: Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                borderRadius: MmtTokens.glassRadiusMd(),
                border: Border.all(color: MmtColors.news.withValues(alpha: 0.20), width: MmtTokens.glassHairline),
                color: MmtColors.news50.withValues(alpha: 0.80),
                boxShadow: MmtTokens.glassShadowMd(),
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
                    borderRadius: MmtTokens.glassRadiusSm(),
                    child: ClipRRect(
                      borderRadius: MmtTokens.glassRadiusSm(),
                      child: BackdropFilter(
                        filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurSm, sigmaY: MmtTokens.glassBlurSm),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                          decoration: BoxDecoration(
                            borderRadius: MmtTokens.glassRadiusSm(),
                            border: Border.all(color: MmtColors.ink950.withValues(alpha: 0.10), width: MmtTokens.glassHairline),
                            color: Colors.white.withValues(alpha: 0.90),
                            boxShadow: MmtTokens.glassShadowSm(),
                          ),
                          child: Text(
                            Dict.of(context).common.retry.toUpperCase(),
                            style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 11, letterSpacing: 1.4),
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
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
    final videos = ref.watch(videoPostsProvider);
    final unread = ref.watch(unreadCountProvider);

    return RefreshIndicator(
      onRefresh: _onRefresh,
      color: MmtColors.news,
      backgroundColor: Colors.white,
      strokeWidth: 3,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(child: SizedBox(height: MediaQuery.of(context).padding.top + 2)),
          // Breaking News Banner (uses first trending or featured post)
          trending.when(
            loading: () => const SliverToBoxAdapter(child: SizedBox.shrink()),
            error: (_, __) => const SliverToBoxAdapter(child: SizedBox.shrink()),
            data: (list) {
              if (list.isEmpty) {
                return featured.when(
                  loading: () => const SliverToBoxAdapter(child: SizedBox.shrink()),
                  error: (_, __) => const SliverToBoxAdapter(child: SizedBox.shrink()),
                  data: (fl) => fl.isEmpty ? const SliverToBoxAdapter(child: SizedBox.shrink()) : SliverToBoxAdapter(
                    child: BreakingBanner(headline: fl.first.title, publishedAt: fl.first.publishedAt, onTap: () => _openPost(context, fl.first)),
                  ),
                );
              }
              return SliverToBoxAdapter(
                child: BreakingBanner(headline: list.first.title, publishedAt: list.first.publishedAt, onTap: () => _openPost(context, list.first)),
              );
            },
          ),
          // Featured Reports (Tier split: Hero + 2-col Secondary Grid)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 24, 18, 6),
              child: SectionEyebrow(t.home.featured),
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
            data: (list) {
              if (list.isEmpty) return const SliverToBoxAdapter(child: SizedBox.shrink());
              final rest = list.sublist(1);
              return SliverPadding(
                padding: const EdgeInsets.fromLTRB(18, 12, 18, 0),
                sliver: SliverList(
                  delegate: SliverChildListDelegate.fixed(<Widget>[
                    HeroStoryCard(post: list[0], onTap: () => _openPost(context, list[0])),
                    if (rest.isNotEmpty) ...[
                      const SizedBox(height: 22),
                      Row(
                        children: [
                          SectionEyebrow('Across Our Coverage'),
                        ],
                      ),
                      const SizedBox(height: 4),
                      for (int i = 0; i < rest.length && i < 4; i++) ...[
                        NewsCardHorizontal(post: rest[i], onTap: () => _openPost(context, rest[i])),
                      ],
                    ],
                  ]),
                ),
              );
            },
          ),
          // Trending Now (Text only list rows with index)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 28, 18, 10),
              child: Row(
                children: [
                  SectionEyebrow(t.home.trending),
                  const Spacer(),
                  const FaIcon(FontAwesomeIcons.fire, size: 16, color: MmtColors.news),
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
                  separatorBuilder: (_, __) => const SizedBox(height: 2),
                  itemBuilder: (_, i) => TextOnlyStoryRow(
                    index: i + 1,
                    post: list[i],
                    onTap: () => _openPost(context, list[i]),
                  ),
                ),
              );
            },
          ),
          // Continue Reading Shelf
          FutureBuilder<List<ReaderContinueItem>>(
            future: _continueReadingFuture,
            builder: (ctx, snap) {
              final items = snap.data ?? const <ReaderContinueItem>[];
              final filtered = items.where((e) => e.scrollPercent >= 5 && e.scrollPercent <= 95).toList(growable: false);
              if (filtered.isEmpty) {
                return const SliverToBoxAdapter(child: SizedBox.shrink());
              }
              return SliverList.list(children: <Widget>[
                Padding(
                  padding: const EdgeInsets.fromLTRB(18, 28, 18, 10),
                  child: Row(
                    children: [
                      const SectionEyebrow('CONTINUE READING'),
                      const Spacer(),
                      FaIcon(FontAwesomeIcons.bookOpen, size: 14, color: MmtColors.news),
                    ],
                  ),
                ),
                SizedBox(
                  height: 248,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 18),
                    itemCount: filtered.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 14),
                    itemBuilder: (_, i) {
                      final item = filtered[i];
                      final post = item.post;
                      final slug = post?.slug ?? item.postId;
                      final cover = post?.cover;
                      final title = post?.title ?? '(Untitled)';
                      final rt = item.readingTimeMinutes ?? post?.readingTimeMinutes ?? 7;
                      final remaining = (rt * (1 - item.scrollPercent / 100)).ceil();
                      final frac = item.scrollPercent / 100;
                      return SizedBox(
                        width: 240,
                        child: InkWell(
                          borderRadius: BorderRadius.circular(16),
                          onTap: () {
                            final params = <String, String>{
                              'id': item.postId,
                              'resumePercent': item.scrollPercent.toString(),
                            };
                            ctx.push('/article/${Uri.encodeComponent(slug)}?id=${Uri.encodeComponent(item.postId)}&resumePercent=${item.scrollPercent}');
                          },
                          child: Container(
                            clipBehavior: Clip.antiAlias,
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: Colors.black.withValues(alpha: 0.06), width: 1.5),
                              color: Colors.white,
                              boxShadow: [
                                BoxShadow(color: MmtColors.ink950.withValues(alpha: 0.08), blurRadius: 10, offset: const Offset(0, 4)),
                              ],
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                AspectRatio(
                                  aspectRatio: 16 / 10,
                                  child: cover != null && cover.isNotEmpty
                                      ? Image.network(cover, fit: BoxFit.cover, errorBuilder: (_, __, ___) => Container(
                                          decoration: BoxDecoration(
                                            gradient: LinearGradient(colors: [MmtColors.news50, MmtColors.ink200]),
                                          ),
                                          child: Center(child: FaIcon(FontAwesomeIcons.newspaper, size: 28, color: MmtColors.news700)),
                                        ))
                                      : Container(
                                          decoration: BoxDecoration(
                                            gradient: LinearGradient(colors: [MmtColors.news50, MmtColors.ink200]),
                                          ),
                                          child: Center(child: FaIcon(FontAwesomeIcons.newspaper, size: 28, color: MmtColors.news700)),
                                        ),
                                ),
                                Padding(
                                  padding: const EdgeInsets.fromLTRB(10, 8, 10, 6),
                                  child: Text(
                                    title,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                    style: GoogleFonts.inter(
                                      fontWeight: FontWeight.w900,
                                      fontSize: 13,
                                      height: 1.28,
                                      color: MmtColors.ink950,
                                    ),
                                  ),
                                ),
                                const Spacer(),
                                Padding(
                                  padding: const EdgeInsets.fromLTRB(10, 0, 10, 10),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      SizedBox(
                                        height: 3,
                                        width: double.infinity,
                                        child: Stack(
                                          children: [
                                            Container(color: MmtColors.ink200),
                                            FractionallySizedBox(
                                              widthFactor: frac,
                                              child: Container(color: const Color(0xFFE31E24)),
                                            ),
                                          ],
                                        ),
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        '~$remaining MIN LEFT',
                                        style: TextStyle(
                                          fontSize: 10,
                                          fontWeight: FontWeight.w900,
                                          letterSpacing: 0.8,
                                          color: MmtColors.ink700,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ]);
            },
          ),
          // Latest Stories
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 28, 18, 10),
              child: SectionEyebrow(t.home.latest),
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
            error: (e, _) => SliverToBoxAdapter(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 18), child: _sectionErr(e.toString(), () => ref.invalidate(latestPostsProvider(1))))),
            data: (list) => SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 18),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, mainAxisSpacing: 14, crossAxisSpacing: 14, childAspectRatio: 0.77),
                delegate: SliverChildBuilderDelegate(
                  (_, i) => NewsCardVertical(post: list[i], compact: true, onTap: () => _openPost(context, list[i])),
                  childCount: list.length > 12 ? 12 : list.length,
                ),
              ),
            ),
          ),
          // For You (personalized picks — latest 4 + recently viewed signal)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 28, 18, 10),
              child: Row(
                children: [
                  SectionEyebrow('For You'),
                  const SizedBox(width: 10),
                  FaIcon(FontAwesomeIcons.bolt, size: 14, color: MmtColors.news),
                ],
              ),
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
            error: (_, __) => const SliverToBoxAdapter(child: SizedBox.shrink()),
            data: (list) {
              if (list.isEmpty) return const SliverToBoxAdapter(child: SizedBox.shrink());
              final picks = list.take(list.length > 6 ? 6 : list.length).toList(growable: false);
              return SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 18),
                sliver: SliverGrid(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, mainAxisSpacing: 16, crossAxisSpacing: 16, childAspectRatio: 0.68),
                  delegate: SliverChildBuilderDelegate(
                    (_, i) => PersonalizedPickCard(post: picks[i], onTap: () => _openPost(context, picks[i])),
                    childCount: picks.length,
                  ),
                ),
              );
            },
          ),
          // ---------------- NEW: TOP SECTIONS GRID (with Explore More) ----------------
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 30, 18, 8),
              child: Row(
                children: [
                  SectionEyebrow('Top Sections'),
                  const SizedBox(width: 8),
                  Text('8', style: GoogleFonts.getFont('Archivo Black', fontSize: 12, fontWeight: FontWeight.w900, color: MmtColors.ink700)),
                  const Spacer(),
                  InkWell(
                    onTap: () => context.go('/categories'),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('Explore More', style: GoogleFonts.archivoBlack(fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 0.8, color: MmtColors.news, decoration: TextDecoration.none)),
                        const SizedBox(width: 4),
                        const FaIcon(FontAwesomeIcons.arrowRightLong, size: 13, color: MmtColors.news),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(18, 8, 18, 4),
            sliver: SliverGrid(
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                mainAxisSpacing: 14,
                crossAxisSpacing: 14,
                childAspectRatio: 2.0,
              ),
              delegate: SliverChildBuilderDelegate(
                (_, i) => SectionTile(s: kMmtSections[i], dark: dark),
                childCount: kMmtSections.length,
              ),
            ),
          ),
          // ---------------- NEW: SHORTS HORIZONTAL ROW ----------------
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 26, 18, 8),
              child: Row(
                children: [
                  SectionEyebrow('Shorts'),
                  const SizedBox(width: 8),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(999),
                    child: BackdropFilter(
                      filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurSm, sigmaY: MmtTokens.glassBlurSm),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: MmtColors.ink950.withValues(alpha: 0.80),
                          border: Border.all(color: Colors.white.withValues(alpha: 0.16), width: MmtTokens.glassHairline),
                          borderRadius: BorderRadius.circular(999),
                          boxShadow: MmtTokens.glassShadowSm(),
                        ),
                        child: const FaIcon(FontAwesomeIcons.bolt, size: 11, color: MmtColors.news),
                      ),
                    ),
                  ),
                  const Spacer(),
                  InkWell(
                    onTap: () {
                      // Since shorts is page 1 of home shell PageView, we need to communicate up.
                      // Fallback: also go /shorts route (which has same ShortsFeed)
                      context.go('/shorts');
                    },
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('Explore More', style: GoogleFonts.archivoBlack(fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 0.8, color: MmtColors.news, decoration: TextDecoration.none)),
                        const SizedBox(width: 4),
                        const FaIcon(FontAwesomeIcons.arrowRightLong, size: 13, color: MmtColors.news),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          shorts.when(
            loading: () => SliverToBoxAdapter(
              child: SizedBox(height: 210, child: ListView.separated(
                padding: const EdgeInsets.symmetric(horizontal: 18),
                scrollDirection: Axis.horizontal,
                itemCount: 4,
                separatorBuilder: (_, __) => const SizedBox(width: 14),
                itemBuilder: (_, __) => _skel(210, w: 140),
              )),
            ),
            error: (e, _) => SliverToBoxAdapter(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 18), child: _sectionErr(e.toString(), () => ref.invalidate(shortsFeedProvider)))),
            data: (list) {
              if (list.isEmpty) return const SliverToBoxAdapter(child: SizedBox.shrink());
              final preview = list.take(list.length > 5 ? 5 : list.length).toList();
              return SliverToBoxAdapter(
                child: SizedBox(
                  height: 230,
                  child: ListView.separated(
                    padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 4),
                    scrollDirection: Axis.horizontal,
                    itemCount: preview.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 14),
                    itemBuilder: (_, i) {
                      final s = preview[i];
                      return SizedBox(
                        width: 150,
                        child: InkWell(
                          onTap: () {
                            // Deep open: full shorts route + index.
                            context.go('/shorts');
                          },
                          borderRadius: BorderRadius.circular(18),
                          child: Container(
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(18),
                              color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                              border: Border.all(color: MmtColors.ink950, width: 2),
                              boxShadow: const [BoxShadow(color: Colors.black, offset: Offset(4, 4), blurRadius: 0)],
                              image: s.cover.isNotEmpty ? DecorationImage(image: NetworkImage(s.cover), fit: BoxFit.cover) : null,
                            ),
                            child: Stack(
                              children: [
                                Container(decoration: BoxDecoration(borderRadius: BorderRadius.circular(18), gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Colors.transparent, Colors.black.withValues(alpha: 0.1), Colors.black.withValues(alpha: 0.82)]))),
                                Positioned(
                                  top: 10, right: 10,
                                  child: Container(
                                    width: 34, height: 34,
                                    decoration: BoxDecoration(shape: BoxShape.circle, color: MmtColors.news.withValues(alpha: 0.95), border: Border.all(color: Colors.white, width: 1)),
                                    child: const Center(child: FaIcon(FontAwesomeIcons.play, size: 12, color: Colors.white)),
                                  ),
                                ),
                                Positioned(
                                  bottom: 10, left: 10, right: 10,
                                  child: Text(
                                    s.title,
                                    style: const TextStyle(color: Colors.white, fontSize: 12.5, fontWeight: FontWeight.w800, height: 1.3),
                                    maxLines: 3, overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
              );
            },
          ),
          // ---------------- NEW: VIDEOS HORIZONTAL ROW ----------------
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 26, 18, 8),
              child: Row(
                children: [
                  SectionEyebrow('Videos'),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(color: Colors.white, border: Border.all(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.circular(999)),
                    child: const FaIcon(FontAwesomeIcons.youtube, size: 11, color: Color(0xFFFF0000)),
                  ),
                  const Spacer(),
                  InkWell(
                    onTap: () => context.go('/videos'),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('Explore More', style: GoogleFonts.archivoBlack(fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 0.8, color: MmtColors.news, decoration: TextDecoration.none)),
                        const SizedBox(width: 4),
                        const FaIcon(FontAwesomeIcons.arrowRightLong, size: 13, color: MmtColors.news),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          videos.when(
            loading: () => SliverToBoxAdapter(
              child: SizedBox(height: 230, child: ListView.separated(
                padding: const EdgeInsets.symmetric(horizontal: 18),
                scrollDirection: Axis.horizontal,
                itemCount: 4,
                separatorBuilder: (_, __) => const SizedBox(width: 16),
                itemBuilder: (_, __) => _skel(230, w: 270),
              )),
            ),
            error: (e, _) => SliverToBoxAdapter(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 18), child: _sectionErr(e.toString(), () => ref.invalidate(videoPostsProvider)))),
            data: (list) {
              if (list.isEmpty) return const SliverToBoxAdapter(child: SizedBox.shrink());
              final preview = list.take(list.length > 5 ? 5 : list.length).toList();
              return SliverToBoxAdapter(
                child: SizedBox(
                  height: 285,
                  child: ListView.separated(
                    padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 4),
                    scrollDirection: Axis.horizontal,
                    itemCount: preview.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 16),
                    itemBuilder: (_, i) {
                      final p = preview[i];
                      return SizedBox(
                        width: 300,
                        child: InkWell(
                          onTap: () => context.push('/article/${p.slug}?id=${p.id}'),
                          borderRadius: BorderRadius.circular(18),
                          child: Container(
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(18),
                              color: dark ? MmtColors.ink800 : Colors.white,
                              border: Border.all(color: MmtColors.ink950, width: 2),
                              boxShadow: const [BoxShadow(color: Colors.black, offset: Offset(4, 4), blurRadius: 0)],
                            ),
                            clipBehavior: Clip.antiAlias,
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Stack(
                                  children: [
                                    AspectRatio(
                                      aspectRatio: 16 / 9,
                                      child: Container(
                                        decoration: BoxDecoration(
                                          color: MmtColors.ink600.withValues(alpha: 0.25),
                                          image: p.cover.isNotEmpty ? DecorationImage(image: NetworkImage(p.cover), fit: BoxFit.cover, onError: (_, __) {}) : null,
                                        ),
                                      ),
                                    ),
                                    Positioned.fill(child: Center(
                                      child: Container(
                                        width: 50, height: 50,
                                        decoration: BoxDecoration(shape: BoxShape.circle, color: const Color(0xFFFF0000).withValues(alpha: 0.95), border: Border.all(color: Colors.white, width: 2), boxShadow: const [BoxShadow(color: Colors.black45, blurRadius: 12)]),
                                        child: const Center(child: FaIcon(FontAwesomeIcons.play, size: 17, color: Colors.white)),
                                      ),
                                    )),
                                  ],
                                ),
                                Expanded(
                                  child: Padding(
                                    padding: const EdgeInsets.fromLTRB(12, 10, 12, 6),
                                    child: Text(
                                      p.title,
                                      style: GoogleFonts.getFont('Archivo Black', fontSize: 13, fontWeight: FontWeight.w900, height: 1.25, color: dark ? Colors.white : MmtColors.ink950),
                                      maxLines: 2, overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                ),
                                Padding(
                                  padding: const EdgeInsets.fromLTRB(12, 0, 12, 10),
                                  child: Row(
                                    children: [
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                                        decoration: BoxDecoration(color: MmtColors.news50, border: Border.all(color: MmtColors.ink950, width: 1.5), borderRadius: BorderRadius.circular(999)),
                                        child: const Text('VIDEO', style: TextStyle(color: MmtColors.news, fontWeight: FontWeight.w900, fontSize: 9.5, letterSpacing: 0.8)),
                                      ),
                                      const Spacer(),
                                      if (p.publishedAt != null)
                                        Text(
                                          '${p.publishedAt!.day.toString().padLeft(2,'0')} ${['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'][p.publishedAt!.month-1]}',
                                          style: const TextStyle(fontSize: 10.5, fontWeight: FontWeight.w800, color: MmtColors.ink700),
                                        ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
              );
            },
          ),
          // Categories — horizontal scroll rail (with Explore More)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 28, 18, 12),
              child: Row(
                children: [
                  SectionEyebrow(t.home.categories),
                  const Spacer(),
                  InkWell(
                    onTap: () => context.go('/categories'),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('Explore More', style: GoogleFonts.archivoBlack(fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 0.8, color: MmtColors.news, decoration: TextDecoration.none)),
                        const SizedBox(width: 4),
                        const FaIcon(FontAwesomeIcons.arrowRightLong, size: 13, color: MmtColors.news),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          cats.when(
            loading: () => SliverToBoxAdapter(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 18), child: Wrap(spacing: 8, runSpacing: 8, children: List<Widget>.generate(6, (i) => _skel(28, w: 80))))),
            error: (e, _) => const SliverToBoxAdapter(child: SizedBox.shrink()),
            data: (list) => SliverToBoxAdapter(
              child: SizedBox(
                height: 44,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 18),
                  itemCount: list.length,
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemBuilder: (_, i) => MmtChip(
                    label: list[i].name,
                    selected: i == 0,
                    onTap: () {
                      final slug = list[i].slug;
                      context.push('/category/$slug');
                    },
                  ),
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
                  SectionEyebrow(t.footer.followUs),
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
                      style: const TextStyle(fontSize: 10.5, fontWeight: FontWeight.w600, color: MmtColors.ink600),),
                ],
              ),
            ),
          ),
          const SliverToBoxAdapter(child: SizedBox(height: 20)),
        ],
      ),
    );
  }

  Widget _socBtn(FaIconData ic, String url) {
    return InkWell(
      onTap: () async {
        try { await launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication); } catch (_) {}
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
      backgroundColor: Colors.transparent,
      foregroundColor: dark ? Colors.white : MmtColors.ink950,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      flexibleSpace: ClipRRect(
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(MmtTokens.radiusLg)),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurLg, sigmaY: MmtTokens.glassBlurLg, tileMode: TileMode.clamp),
          child: Container(
            decoration: BoxDecoration(
              color: (dark ? MmtColors.ink950 : Colors.white).withValues(alpha: dark ? 0.60 : 0.72),
              borderRadius: BorderRadius.vertical(bottom: Radius.circular(MmtTokens.radiusLg)),
              border: Border(
                bottom: BorderSide(color: (dark ? Colors.white : MmtColors.ink950).withValues(alpha: dark ? 0.08 : 0.06), width: MmtTokens.glassHairline),
              ),
              boxShadow: MmtTokens.glassShadowHeader(),
            ),
          ),
        ),
      ),
      titleSpacing: 16,
      title: Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Row(
          children: [
            const BrandLogo(size: 14),
            const Spacer(),
            InkWell(
              onTap: () async {
                try { FocusScope.of(context).unfocus(); } catch (_) {}
                await Future<void>.delayed(const Duration(milliseconds: 50));
                if (context.mounted) GoRouter.of(context).go('/search');
              },
              borderRadius: MmtTokens.glassRadiusPill(),
              child: ClipRRect(
                borderRadius: MmtTokens.glassRadiusPill(),
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurSm, sigmaY: MmtTokens.glassBlurSm),
                  child: Container(
                    height: 36,
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    alignment: Alignment.centerLeft,
                    decoration: BoxDecoration(
                      borderRadius: MmtTokens.glassRadiusPill(),
                      border: Border.all(color: MmtColors.ink950.withValues(alpha: 0.10), width: MmtTokens.glassHairline),
                      color: Colors.white.withValues(alpha: 0.85),
                      boxShadow: MmtTokens.glassShadowSm(),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.search, size: 16, color: MmtColors.ink700),
                        const SizedBox(width: 8),
                        Text(t.nav.search, style: const TextStyle(fontSize: 12, color: MmtColors.ink600, fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 10),
            InkWell(
              onTap: () {
                final s = LangScope.of(context);
                LangScope.toggle(context);
                final newT = Dict.of(context);
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                  backgroundColor: MmtColors.ink950,
                  content: Text(newT.common.languageSwitched, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
                  duration: const Duration(milliseconds: 1200),
                ),);
                }
              },
              borderRadius: MmtTokens.glassRadiusSm(),
              child: ClipRRect(
                borderRadius: MmtTokens.glassRadiusSm(),
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurSm, sigmaY: MmtTokens.glassBlurSm),
                  child: Container(
                    width: 48,
                    height: 36,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: MmtColors.news.withValues(alpha: 0.92),
                      borderRadius: MmtTokens.glassRadiusSm(),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.20), width: MmtTokens.glassHairline),
                      boxShadow: [
                        BoxShadow(color: MmtColors.news.withValues(alpha: 0.25), blurRadius: 10, offset: const Offset(0, 3)),
                      ],
                    ),
                    child: Text(LangScope.codeOf(context).name.toUpperCase(),
                        style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 12, color: Colors.white, letterSpacing: 0.6),),
                  ),
                ),
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
      ctx.push('/shorts-player${'?startSlug=${Uri.encodeQueryComponent(p.slug)}'}');
    } else {
      final sp = <String, String>{};
      sp['id'] = p.id;
      ctx.push('/article/${Uri.encodeQueryComponent(p.slug ?? p.id ?? '')}', extra: sp.isEmpty ? null : sp);
    }
  }
}


