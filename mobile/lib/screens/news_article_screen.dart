// ---------------- NEWS ARTICLE SCREEN ----------------
// INTEGRATED: postBySlugProvider + increment view
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:share_plus/share_plus.dart';
import '../core/env.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../models/blog_models.dart';
import '../providers/index.dart';

class NewsArticleScreen extends ConsumerStatefulWidget {
  const NewsArticleScreen({
    super.key,
    required this.slug,
    this.postId,
  });

  final String slug;
  final String? postId;

  @override
  ConsumerState<NewsArticleScreen> createState() => _NewsArticleScreenState();
}

class _NewsArticleScreenState extends ConsumerState<NewsArticleScreen> {
  bool _viewCounted = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_viewCounted) {
      _viewCounted = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        try {
          final pid = widget.postId;
          if (pid != null && pid.isNotEmpty) {
            ref.read(postViewIncrementProvider)(pid);
          }
        } catch (_) {}
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Dict.of(context);
    final postAsync = ref.watch(postBySlugProvider(widget.slug));

    return Scaffold(
      backgroundColor: MmtColors.background,
      body: postAsync.when(
        loading: () => const _ArticleSkeleton(),
        error: (e, _) => _ArticleError(
          msg: e.toString(),
          retry: () => ref.invalidate(postBySlugProvider(widget.slug)),
        ),
        data: (p) {
          if (p == null) {
            return _ArticleError(
              msg: 'No article found',
              retry: () => ref.invalidate(postBySlugProvider(widget.slug)),
            );
          }
          // Fire view count (fallback if only slug matched)
          if (!_viewCounted) {
            _viewCounted = true;
            try {
              ref.read(postViewIncrementProvider)(p.id);
            } catch (_) {}
          }
          return _ArticleBody(
            post: p,
            slug: widget.slug,
            postId: widget.postId ?? p.id,
            t: t,
          );
        },
      ),
    );
  }
}

class _ArticleBody extends StatelessWidget {
  const _ArticleBody({
    required this.post,
    required this.slug,
    required this.postId,
    required this.t,
  });

  final BlogPostResponse post;
  final String slug;
  final String postId;
  final Dict t;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;

    return CustomScrollView(
      slivers: [
        SliverAppBar(
          leadingWidth: 64,
          leading: Padding(
            padding: const EdgeInsets.all(8),
            child: InkWell(
              onTap: () => GoRouter.of(context).canPop() ? context.pop() : context.go('/'),
              child: Container(
                decoration: BoxDecoration(
                  border: Border.all(color: MmtColors.ink950, width: 2),
                  color: Colors.white,
                ),
                child: const Icon(Icons.chevron_left, color: MmtColors.ink950),
              ),
            ),
          ),
          expandedHeight: 260,
          floating: false,
          pinned: true,
          stretch: true,
          flexibleSpace: FlexibleSpaceBar(
            stretchModes: const [StretchMode.zoomBackground],
            background: Container(
              decoration: BoxDecoration(
                color: MmtColors.ink800,
                border: Border(bottom: BorderSide(color: MmtColors.ink950, width: 2)),
              ),
              child: Stack(
                fit: StackFit.expand,
                children: [
                  if (post.cover.isNotEmpty)
                    Image.network(post.cover, fit: BoxFit.cover, errorBuilder: (_, __, ___) => const SizedBox.shrink())
                  else
                    Container(
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: [MmtColors.news50, MmtColors.news300],
                        ),
                      ),
                    ),
                  Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [Colors.transparent, Colors.black26],
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 30),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Categories chips
                if ((post.categories ?? <CategoryResponse>[]).isNotEmpty)
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final c in post.categories ?? <CategoryResponse>[])
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: MmtColors.news,
                            border: Border.all(color: MmtColors.ink950, width: 2),
                          ),
                          child: Text(
                            c.name.toString().toUpperCase(),
                            style: GoogleFonts.inter(
                              color: Colors.white,
                              fontWeight: FontWeight.w900,
                              fontSize: 10,
                              letterSpacing: 1.4,
                              height: 1.0,
                            ),
                          ),
                        ),
                    ],
                  ),
                if ((post.categories ?? <CategoryResponse>[]).isNotEmpty)
                  const SizedBox(height: 18),
                Text(
                  post.title,
                  style: GoogleFonts.archivoBlack(
                    fontSize: 26,
                    height: 1.05,
                    color: dark ? Colors.white : MmtColors.ink950,
                    letterSpacing: -0.2,
                  ),
                ),
                if ((post.excerpt ?? '').isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Text(
                    post.excerpt ?? '',
                    style: GoogleFonts.inter(
                      fontSize: 15,
                      fontStyle: FontStyle.italic,
                      fontWeight: FontWeight.w500,
                      height: 1.6,
                      color: MmtColors.ink600,
                    ),
                  ),
                ],
                const SizedBox(height: 18),
                Container(height: 2, color: MmtColors.ink950),
                const SizedBox(height: 14),
                // Meta
                Wrap(
                  spacing: 12,
                  runSpacing: 8,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Text(
                      '${t.byAuthor} ${post.author?.name ?? 'MapMyTimes'}',
                      style: GoogleFonts.inter(
                        fontSize: 11.5,
                        color: MmtColors.ink700,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const Text('·', style: TextStyle(fontWeight: FontWeight.w900, color: MmtColors.ink600)),
                    Text(
                      _fmtDate(post.publishedAt ?? post.createdAt),
                      style: GoogleFonts.inter(
                        fontSize: 11.5,
                        color: MmtColors.ink700,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2)),
                      child: Text(
                        '${post.readingTimeMinutes ?? 7} MIN READ',
                        style: GoogleFonts.inter(
                          fontSize: 10,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.2,
                        ),
                      ),
                    ),
                    if ((post.viewCount ?? 0) > 0)
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.visibility_outlined, size: 15, color: MmtColors.ink700),
                          const SizedBox(width: 4),
                          Text(
                            _fmtViews(post.viewCount),
                            style: GoogleFonts.inter(
                              fontSize: 11,
                              fontWeight: FontWeight.w700,
                              color: MmtColors.ink700,
                            ),
                          ),
                        ],
                      ),
                  ],
                ),
                const SizedBox(height: 22),
                // Body paragraphs
                ..._renderBody(post, context),
                const SizedBox(height: 22),
                // Tags
                if ((post.tags ?? <TagResponse>[]).isNotEmpty)
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final tag in post.tags ?? <TagResponse>[])
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: MmtColors.news50,
                            border: Border.all(color: MmtColors.ink950, width: 2),
                          ),
                          child: Text(
                            '#${tag.name}',
                            style: GoogleFonts.inter(
                              fontWeight: FontWeight.w800,
                              fontSize: 11,
                              color: MmtColors.ink950,
                            ),
                          ),
                        ),
                    ],
                  ),
                if ((post.tags ?? <TagResponse>[]).isNotEmpty)
                  const SizedBox(height: 22),
                Container(height: 2, color: MmtColors.ink950),
                const SizedBox(height: 18),
                // Actions
                Row(
                  children: [
                    Expanded(
                      child: InkWell(
                        onTap: () async {
                          final url = '${Env.siteUrl}/news/$slug';
                          await Share.share('${post.title}\n\n$url');
                        },
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          decoration: BoxDecoration(
                            border: Border.all(color: MmtColors.ink950, width: 2),
                            boxShadow: const BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950),
                            color: Colors.white,
                          ),
                          alignment: Alignment.center,
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.adaptive.share, color: MmtColors.ink950, size: 16),
                              const SizedBox(width: 8),
                              Text(
                                t.common.share.toUpperCase(),
                                style: GoogleFonts.inter(
                                  fontWeight: FontWeight.w900,
                                  letterSpacing: 1.4,
                                  fontSize: 11,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: InkWell(
                        onTap: () async {
                          final url = '${Env.siteUrl}/news/$slug';
                          await Clipboard.setData(ClipboardData(text: url));
                          if (context.mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                backgroundColor: MmtColors.ink950,
                                content: Text(
                                  t.common.linkCopied,
                                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700),
                                ),
                                duration: const Duration(milliseconds: 1200),
                              ),
                            );
                          }
                        },
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          decoration: BoxDecoration(
                            border: Border.all(color: MmtColors.ink950, width: 2),
                            boxShadow: const BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950),
                            color: Colors.white,
                          ),
                          alignment: Alignment.center,
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.copy_all_outlined, size: 16, color: MmtColors.ink950),
                              const SizedBox(width: 8),
                              Text(
                                t.common.copyLink.toUpperCase(),
                                style: GoogleFonts.inter(
                                  fontWeight: FontWeight.w900,
                                  letterSpacing: 1.4,
                                  fontSize: 11,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                Text(
                  '© ${DateTime.now().year} MAPMYTOUR LLP, India · ${t.footer.copyright}',
                  style: GoogleFonts.inter(fontSize: 10.5, color: MmtColors.ink600, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 20),
              ],
            ),
          ),
        ),
      ],
    );
  }

  List<Widget> _renderBody(BlogPostResponse p, BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final raw = (p.content ?? p.contentHtml ?? '').toString();
    if (raw.isEmpty) return <Widget>[];
    final paragraphs = raw
        .split(RegExp(r'\n{2,}|<br\s*/?>'))
        .where((s) => s.trim().isNotEmpty)
        .toList(growable: false);
    return paragraphs.map((text) {
      final stripped = _stripHtml(text.trim());
      if (stripped.isEmpty) return const SizedBox.shrink();
      return Padding(
        padding: const EdgeInsets.only(bottom: 14),
        child: Text(
          stripped,
          style: GoogleFonts.inter(
            fontSize: 15,
            height: 1.75,
            color: dark ? Colors.white70 : MmtColors.ink850,
            fontWeight: FontWeight.w400,
          ),
        ),
      );
    }).toList(growable: false);
  }

  static final RegExp _stripRE = RegExp(r'<[^>]*>', multiLine: true, caseSensitive: false);

  static String _stripHtml(String s) {
    return s
        .replaceAll(_stripRE, ' ')
        .replaceAll('&nbsp;', ' ')
        .replaceAll('&amp;', '&')
        .replaceAll('&quot;', '"')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }

  static String _fmtViews(int? v) {
    if (v == null) return '';
    if (v >= 1000000) {
      return '${(v / 1000000).toStringAsFixed(1).replaceAll('.0', '')}M';
    }
    if (v >= 1000) {
      return '${(v / 1000).toStringAsFixed(1).replaceAll('.0', '')}K';
    }
    return v.toString();
  }

  static String _fmtDate(DateTime? d) {
    if (d == null) return '';
    final now = DateTime.now();
    final diff = now.difference(d);
    if (diff.inDays > 30) {
      return '${d.day} ${_month(d.month)} ${d.year}';
    }
    if (diff.inDays > 0) return '${diff.inDays}d ago';
    if (diff.inHours > 0) return '${diff.inHours}h ago';
    return 'Just now';
  }

  static String _month(int m) {
    return const ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'][m - 1];
  }
}

class _ArticleSkeleton extends StatelessWidget {
  const _ArticleSkeleton();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          Container(
            height: 260,
            decoration: BoxDecoration(
              color: MmtColors.ink200,
              border: Border(bottom: BorderSide(color: MmtColors.ink950, width: 2)),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  height: 28,
                  width: 120,
                  decoration: BoxDecoration(
                    border: Border.all(color: MmtColors.ink700, width: 2),
                    color: MmtColors.news50,
                  ),
                ),
                const SizedBox(height: 18),
                Container(
                  height: 80,
                  decoration: BoxDecoration(
                    border: Border.all(color: MmtColors.ink700, width: 2),
                    color: MmtColors.ink100,
                  ),
                ),
                const SizedBox(height: 16),
                Container(height: 16, decoration: BoxDecoration(color: MmtColors.ink200)),
                const SizedBox(height: 10),
                Container(
                  height: 16,
                  width: 280,
                  decoration: BoxDecoration(color: MmtColors.ink200),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ArticleError extends StatelessWidget {
  const _ArticleError({required this.msg, required this.retry});
  final String msg;
  final VoidCallback retry;

  @override
  Widget build(BuildContext context) {
    final t = Dict.of(context);
    return Scaffold(
      appBar: AppBar(
        backgroundColor: MmtColors.news,
        foregroundColor: Colors.white,
        title: const Text('Error'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
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
                    style: TextStyle(
                      fontWeight: FontWeight.w900,
                      fontSize: 14,
                      color: MmtColors.news700,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    msg,
                    style: const TextStyle(fontSize: 12.5, color: MmtColors.ink700, fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 16),
                  InkWell(
                    onTap: retry,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        border: Border.all(color: MmtColors.ink950, width: 2),
                        color: Colors.white,
                        boxShadow: const BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950),
                      ),
                      child: Text(
                        t.common.retry.toUpperCase(),
                        style: GoogleFonts.inter(
                          fontSize: 11,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.4,
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
}
