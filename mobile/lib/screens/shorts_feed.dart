// ---------------- SHORTS FEED SCREEN
// INTEGRATED: shortsFeedProvider (Riverpod)
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../models/blog_models.dart';
import '../providers/index.dart';

class ShortsFeedScreen extends ConsumerStatefulWidget {
  const ShortsFeedScreen({
    super.key,
    this.startPostId,
    this.startPostSlug,
  });

  final String? startPostId;
  final String? startPostSlug;

  @override
  ConsumerState<ShortsFeedScreen> createState() => _ShortsFeedScreenState();
}

class _ShortsFeedScreenState extends ConsumerState<ShortsFeedScreen> {
  late final PageController _pc;
  int _idx = 0;

  @override
  void initState() {
    super.initState();
    _pc = PageController();
  }

  @override
  void dispose() {
    _pc.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = Dict.of(context);
    final async = ref.watch(shortsFeedProvider);

    return Scaffold(
      backgroundColor: MmtColors.ink950,
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        elevation: 0,
        surfaceTintColor: Colors.transparent,
        scrolledUnderElevation: 0,
        systemOverlayStyle: const SystemUiOverlayStyle(statusBarBrightness: Brightness.dark),
        title: Text(
          t.nav.shorts.toUpperCase(),
          style: GoogleFonts.archivoBlack(fontSize: 16, letterSpacing: 1.4, color: Colors.white),
        ),
        leading: IconButton(
          onPressed: () => context.canPop() ? context.pop() : context.go('/'),
          icon: const Icon(Icons.close, color: Colors.white),
        ),
      ),
      body: async.when(
        loading: () => const Center(
          child: Padding(
            padding: EdgeInsets.all(32),
            child: CircularProgressIndicator(color: MmtColors.news, strokeWidth: 3),
          ),
        ),
        error: (e, _) => _ShortsError(
          msg: e.toString(),
          retry: () => ref.invalidate(shortsFeedProvider),
        ),
        data: (posts) {
          if (posts.isEmpty) {
            return _ShortsEmpty(
              onRefresh: () => ref.refresh(shortsFeedProvider),
              t: t,
            );
          }
          return PageView.builder(
            scrollDirection: Axis.vertical,
            controller: _pc,
            itemCount: posts.length,
            onPageChanged: (i) => setState(() => _idx = i),
            itemBuilder: (_, i) => _ShortTile(
              post: posts[i],
              index: i,
              total: posts.length,
            ),
          );
        },
      ),
      floatingActionButton: (async.valueOrNull != null && (async.valueOrNull?.isNotEmpty ?? false))
          ? FloatingActionButton.extended(
              onPressed: () => ref.refresh(shortsFeedProvider),
              backgroundColor: MmtColors.news,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.refresh),
              label: Text(t.common.refresh),
            )
          : null,
    );
  }
}

class _ShortTile extends StatelessWidget {
  const _ShortTile({
    required this.post,
    required this.index,
    required this.total,
  });

  final BlogPostSummaryResponse post;
  final int index;
  final int total;

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        // Cover
        Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              colors: [MmtColors.ink950, MmtColors.news800, MmtColors.news700],
              begin: Alignment.topCenter,
              end: Alignment.bottomRight,
            ),
          ),
          child: post.cover.isNotEmpty
              ? Image.network(
                  post.cover,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => const SizedBox.shrink(),
                )
              : null,
        ),
        // Gradient text overlay
        Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              colors: [Colors.transparent, Colors.black54, Colors.black87],
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
            ),
          ),
        ),
        // Content
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 80, 20, 40),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.end,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      width: 6,
                      height: 6,
                      decoration: const BoxDecoration(shape: BoxShape.circle, color: MmtColors.news),
                    ),
                    const SizedBox(width: 10),
                    Text(
                      'SHORTS · ${index + 1}/$total',
                      style: const TextStyle(
                        color: MmtColors.news,
                        fontWeight: FontWeight.w900,
                        letterSpacing: 1.6,
                        fontSize: 10,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Text(
                  post.title,
                  style: GoogleFonts.archivoBlack(
                    color: Colors.white,
                    fontSize: 24,
                    height: 1.08,
                    letterSpacing: -0.2,
                  ),
                ),
                if ((post.excerpt ?? '').isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Text(
                    post.excerpt ?? '',
                    maxLines: 6,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: Colors.white.withValues(alpha: 0.75),
                      fontSize: 13,
                      height: 1.6,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
                const SizedBox(height: 18),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    _ShortChip(
                      label: post.author?.name ?? 'MapMyTimes',
                      ic: Icons.person_outline,
                    ),
                    if ((post.likeCount ?? 0) > 0)
                      _ShortChip(
                        label: _fmt(post.likeCount),
                        ic: Icons.favorite_border,
                      ),
                    if ((post.viewCount ?? 0) > 0)
                      _ShortChip(
                        label: _fmt(post.viewCount),
                        ic: Icons.visibility_outlined,
                      ),
                    if ((post.commentCount ?? 0) > 0)
                      _ShortChip(
                        label: _fmt(post.commentCount),
                        ic: Icons.chat_bubble_outline_rounded,
                      ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  static String _fmt(int? v) {
    if (v == null) return '';
    if (v >= 1000000) {
      return '${(v / 1000000).toStringAsFixed(1).replaceAll('.0', '')}M';
    }
    if (v >= 1000) {
      return '${(v / 1000).toStringAsFixed(1).replaceAll('.0', '')}K';
    }
    return v.toString();
  }
}

class _ShortChip extends StatelessWidget {
  const _ShortChip({required this.label, required this.ic});
  final String label;
  final IconData ic;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(border: Border.all(color: Colors.white38, width: 2)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(ic, size: 14, color: Colors.white70),
          const SizedBox(width: 6),
          Text(
            label,
            style: const TextStyle(fontSize: 11, color: Colors.white, fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

class _ShortsError extends StatelessWidget {
  const _ShortsError({required this.msg, required this.retry});
  final String msg;
  final VoidCallback retry;

  @override
  Widget build(BuildContext context) {
    final t = Dict.of(context);
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Center(
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            border: Border.all(color: MmtColors.news, width: 2),
            color: MmtColors.news50,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                '⚠ ${t.common.loadingError}',
                style: const TextStyle(
                  fontWeight: FontWeight.w900,
                  fontSize: 16,
                  color: MmtColors.news,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                msg,
                style: const TextStyle(fontSize: 13, color: MmtColors.ink700, fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 18),
              InkWell(
                onTap: retry,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
                  decoration: BoxDecoration(
                    border: Border.all(color: MmtColors.ink950, width: 2),
                    color: Colors.white,
                    boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)],
                  ),
                  child: Text(
                    t.common.retry.toUpperCase(),
                    style: GoogleFonts.inter(
                      fontWeight: FontWeight.w900,
                      fontSize: 11,
                      letterSpacing: 1.4,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ShortsEmpty extends StatelessWidget {
  const _ShortsEmpty({required this.onRefresh, required this.t});
  final VoidCallback onRefresh;
  final Dict t;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(32),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.motion_photos_on_outlined, color: Colors.white54, size: 56),
            const SizedBox(height: 18),
            Text(
              t.shortsDict.empty,
              style: const TextStyle(fontWeight: FontWeight.w900, color: Colors.white, fontSize: 18),
            ),
            const SizedBox(height: 18),
            InkWell(
              onTap: onRefresh,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.white70, width: 2),
                  color: MmtColors.news,
                ),
                child: Text(
                  t.common.refresh.toUpperCase(),
                  style: GoogleFonts.inter(
                    fontWeight: FontWeight.w900,
                    color: Colors.white,
                    letterSpacing: 1.4,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
