// ---------------- NEWS ARTICLE SCREEN ----------------
// INTEGRATED: postBySlugProvider + increment view + save + TTS listen + related
import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:share_plus/share_plus.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:url_launcher/url_launcher.dart';
import '../core/env.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../core/widgets/brand.dart';
import '../models/blog_models.dart';
import '../providers/index.dart';
import '../widgets/editorial_components.dart';

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
    final AsyncValue<BlogPostResponse?> postAsync;
    final invalidate = () {
      ref.invalidate(postBySlugProvider(widget.slug));
      if (widget.postId != null) ref.invalidate(postByIdProvider(widget.postId!));
    };
    if (widget.postId != null && widget.postId!.isNotEmpty) {
      postAsync = ref.watch(postByIdProvider(widget.postId!));
    } else {
      postAsync = ref.watch(postBySlugProvider(widget.slug));
    }

    return Scaffold(
      backgroundColor: MmtColors.background,
      body: postAsync.when(
        loading: () => const _ArticleSkeleton(),
        error: (e, _) => _ArticleError(
          msg: e.toString(),
          retry: invalidate,
        ),
        data: (p) {
          if (p == null) {
            return _ArticleError(
              msg: 'No article found',
              retry: invalidate,
            );
          }
          // Fire view count + push to recently viewed (fallback if only slug matched)
          if (!_viewCounted) {
            _viewCounted = true;
            try {
              ref.read(postViewIncrementProvider)(p.id);
              ref.read(recentlyViewedProvider.notifier).push(p.id);
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

class _ArticleBody extends ConsumerStatefulWidget {
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
  ConsumerState<_ArticleBody> createState() => _ArticleBodyState();
}

class _ArticleBodyState extends ConsumerState<_ArticleBody> {
  final FlutterTts _tts = FlutterTts();
  bool _ttsPlaying = false;
  bool _ttsReady = false;

  @override
  void initState() {
    super.initState();
    _initTts();
  }

  @override
  void dispose() {
    _tts.stop();
    super.dispose();
  }

  Future<void> _initTts() async {
    try {
      await _tts.setLanguage('en-IN');
      await _tts.setSpeechRate(0.95);
      await _tts.setVolume(1.0);
      _tts.setCompletionHandler(() { if (mounted) setState(() => _ttsPlaying = false); });
      _tts.setCancelHandler(() { if (mounted) setState(() => _ttsPlaying = false); });
      _tts.setErrorHandler((_) { if (mounted) setState(() => _ttsPlaying = false); });
      if (mounted) setState(() => _ttsReady = true);
    } catch (_) {}
  }

  Future<void> _toggleTts() async {
    if (_ttsPlaying) {
      await _tts.stop();
      if (mounted) setState(() => _ttsPlaying = false);
      return;
    }
    try {
      final text = '${widget.post.title}. ${widget.post.excerpt ?? ''} ${_stripHtml((widget.post.content ?? widget.post.contentHtml ?? '').toString())}';
      final ok = await _tts.speak(text);
      if (ok == 1 && mounted) setState(() => _ttsPlaying = true);
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Listen unavailable right now'), backgroundColor: MmtColors.ink950));
    }
  }

  Future<void> _shareWhatsAppFirst() async {
    final url = '${Env.siteUrl}/news/${widget.slug}';
    final fullText = '${widget.post.title}\n\n$url';
    try {
      await Share.share(fullText, subject: widget.post.title, sharePositionOrigin: Rect.zero);
    } catch (_) {
      await Clipboard.setData(ClipboardData(text: url));
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(widget.t.common.linkCopied), backgroundColor: MmtColors.ink950));
    }
  }

  void _toggleSaved(BuildContext c) {
    final summary = BlogPostSummaryResponse(
      id: widget.postId,
      slug: widget.slug,
      title: widget.post.title,
      featuredImageUrl: widget.post.cover,
      excerpt: widget.post.excerpt,
      publishedAt: widget.post.publishedAt,
      status: PostStatus.published,
      postType: PostType.article,
    );
    ref.read(savedArticlesNotifierProvider.notifier).toggle(widget.postId, meta: summary);
    final saved = ref.read(savedArticlesNotifierProvider).contains(widget.postId);
    ScaffoldMessenger.of(c).showSnackBar(SnackBar(
      backgroundColor: MmtColors.ink950,
      content: Text(saved ? 'Article saved offline' : 'Removed from saved', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
      duration: const Duration(milliseconds: 1400),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final fontScale = ref.watch(fontScaleNotifierProvider);
    final isSaved = ref.watch(isArticleSavedProvider(widget.postId));
    final latestAsync = ref.watch(latestPostsProvider(1));

    final catIds = (widget.post.categories ?? <CategoryResponse>[]).map((c) => c.id).toSet();
    final related = latestAsync.whenOrNull(data: (list) {
      final filt = list.where((p) => (p.id != widget.postId) && (p.categories ?? []).any((c) => catIds.contains(c.id))).toList(growable: false);
      return (filt.isNotEmpty ? filt : list.where((p) => p.id != widget.postId).take(4)).take(4).toList(growable: false);
    });

    final summary = MediaQuery(
      data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(fontScale)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        if ((widget.post.categories ?? <CategoryResponse>[]).isNotEmpty)
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final c in widget.post.categories ?? <CategoryResponse>[])
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(color: MmtColors.news, border: Border.all(color: MmtColors.ink950, width: 2)),
                  child: Text(c.name.toString().toUpperCase(), style: GoogleFonts.inter(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 10, letterSpacing: 1.4, height: 1.0)),
                ),
            ],
          ),
        if ((widget.post.categories ?? <CategoryResponse>[]).isNotEmpty) const SizedBox(height: 18),
        Text(widget.post.title, style: GoogleFonts.archivoBlack(fontSize: 26, height: 1.05, color: dark ? Colors.white : MmtColors.ink950, letterSpacing: -0.2)),
        if ((widget.post.excerpt ?? '').isNotEmpty) ...[
          const SizedBox(height: 10),
          Text(widget.post.excerpt ?? '', style: GoogleFonts.inter(fontSize: 15, fontStyle: FontStyle.italic, fontWeight: FontWeight.w500, height: 1.6, color: MmtColors.ink600)),
        ],
        const SizedBox(height: 18),
        Container(height: 2, color: MmtColors.ink950),
        const SizedBox(height: 14),
        // Meta row
        Wrap(spacing: 12, runSpacing: 8, crossAxisAlignment: WrapCrossAlignment.center, children: [
          Text('${widget.t.byAuthor} ${widget.post.author?.name ?? 'MapMyTimes'}', style: GoogleFonts.inter(fontSize: 11.5, color: MmtColors.ink700, fontWeight: FontWeight.w700)),
          const Text('·', style: TextStyle(fontWeight: FontWeight.w900, color: MmtColors.ink600)),
          Text(_fmtDate(widget.post.publishedAt ?? widget.post.createdAt), style: GoogleFonts.inter(fontSize: 11.5, color: MmtColors.ink700, fontWeight: FontWeight.w600)),
          Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4), decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2)),
            child: Text('${widget.post.readingTimeMinutes ?? 7} MIN READ', style: GoogleFonts.inter(fontSize: 10, fontWeight: FontWeight.w900, letterSpacing: 1.2))),
          if ((widget.post.viewCount ?? 0) > 0)
            Row(mainAxisSize: MainAxisSize.min, children: [
              const Icon(Icons.visibility_outlined, size: 15, color: MmtColors.ink700),
              const SizedBox(width: 4),
              Text(_fmtViews(widget.post.viewCount), style: GoogleFonts.inter(fontSize: 11, fontWeight: FontWeight.w700, color: MmtColors.ink700)),
            ]),
        ]),
        const SizedBox(height: 14),
        // YouTube video embed
        if (widget.post.youtubeVideoId != null)
          _YoutubeEmbed(videoId: widget.post.youtubeVideoId!, videoUrl: widget.post.videoUrl ?? widget.post.shortVideoUrl ?? '')
        else if ((widget.post.videoUrl ?? '').isNotEmpty && !kIsWeb)
          _OpenVideoLink(videoUrl: widget.post.videoUrl ?? widget.post.shortVideoUrl ?? ''),
        if (widget.post.youtubeVideoId != null || (widget.post.videoUrl ?? '').isNotEmpty) const SizedBox(height: 14),
        // Font size stepper
        Container(
          decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: dark ? MmtColors.ink900 : Colors.white),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: Row(children: [
            FaIcon(FontAwesomeIcons.textHeight, size: 13, color: dark ? Colors.white60 : MmtColors.ink700),
            const SizedBox(width: 10),
            Text('TEXT SIZE', style: GoogleFonts.inter(fontSize: 10.5, fontWeight: FontWeight.w900, letterSpacing: 1.2, color: dark ? Colors.white60 : MmtColors.ink700, height: 1.0)),
            const Spacer(),
            _fontBtn(context, 'A−', () => ref.read(fontScaleNotifierProvider.notifier).stepDown()),
            const SizedBox(width: 6),
            _fontBtn(context, 'A', () => ref.read(fontScaleNotifierProvider.notifier).reset(), fill: (fontScale - 1.0).abs() < 0.02),
            const SizedBox(width: 6),
            _fontBtn(context, 'A+', () => ref.read(fontScaleNotifierProvider.notifier).stepUp()),
          ]),
        ),
        const SizedBox(height: 20),
        // Body paragraphs
        ..._renderBody(widget.post, context, fontScale, dark),
      ]),
    );

    return CustomScrollView(slivers: [
      SliverAppBar(
        leadingWidth: 64,
        leading: Padding(
          padding: const EdgeInsets.all(8),
          child: InkWell(
            onTap: () => GoRouter.of(context).canPop() ? context.pop() : context.go('/'),
            child: Container(decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: Colors.white),
              child: const Icon(Icons.chevron_left, color: MmtColors.ink950)),
          ),
        ),
        actions: [
          Padding(padding: const EdgeInsets.all(8), child: InkWell(
            onTap: _toggleTts,
            child: Container(width: 42, height: 42, alignment: Alignment.center,
              decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: _ttsPlaying ? MmtColors.news : Colors.white),
              child: _ttsPlaying
                  ? const FaIcon(FontAwesomeIcons.pause, size: 14, color: Colors.white)
                  : FaIcon(FontAwesomeIcons.microphone, size: 14, color: _ttsReady ? MmtColors.ink950 : MmtColors.ink600),
            ),
          )),
          Padding(padding: const EdgeInsets.fromLTRB(0, 8, 0, 8), child: InkWell(
            onTap: () => _toggleSaved(context),
            child: Container(width: 42, height: 42, alignment: Alignment.center,
              decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: isSaved ? MmtColors.news : Colors.white),
              child: FaIcon(isSaved ? FontAwesomeIcons.solidBookmark : FontAwesomeIcons.bookmark, size: 14, color: isSaved ? Colors.white : MmtColors.ink950),
            ),
          )),
          Padding(padding: const EdgeInsets.fromLTRB(0, 8, 10, 8), child: InkWell(
            onTap: _shareWhatsAppFirst,
            child: Container(width: 42, height: 42, alignment: Alignment.center,
              decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: Colors.white),
              child: FaIcon(FontAwesomeIcons.shareNodes, size: 14, color: MmtColors.ink950),
            ),
          )),
        ],
        expandedHeight: 260,
        floating: false,
        pinned: true,
        stretch: true,
        flexibleSpace: FlexibleSpaceBar(
          stretchModes: const [StretchMode.zoomBackground],
          background: Container(
            decoration: BoxDecoration(
              color: dark ? MmtColors.ink900 : MmtColors.chipBg,
              border: const Border(bottom: BorderSide(color: MmtColors.ink950, width: 2)),
            ),
            child: Stack(fit: StackFit.expand, children: [
              if (widget.post.cover.isNotEmpty)
                Image.network(
                  widget.post.cover,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: dark
                            ? [MmtColors.ink900, MmtColors.ink700]
                            : [MmtColors.news50, MmtColors.news100],
                      ),
                    ),
                    child: Center(
                      child: FaIcon(
                        FontAwesomeIcons.newspaper,
                        size: 58,
                        color: dark ? Colors.white30 : MmtColors.news300,
                      ),
                    ),
                  ),
                )
              else
                Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: dark ? [MmtColors.ink900, MmtColors.ink700] : [MmtColors.news50, MmtColors.news100],
                    ),
                  ),
                  child: Center(
                    child: FaIcon(
                      FontAwesomeIcons.newspaper,
                      size: 58,
                      color: dark ? Colors.white30 : MmtColors.news300,
                    ),
                  ),
                ),
              Container(
                decoration: const BoxDecoration(
                  gradient: LinearGradient(colors: [Colors.transparent, Colors.black26], begin: Alignment.topCenter, end: Alignment.bottomCenter),
                ),
              ),
            ]),
          ),
        ),
      ),
      SliverToBoxAdapter(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 30),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            summary,
            const SizedBox(height: 22),
            // Tags
            if ((widget.post.tags ?? <TagResponse>[]).isNotEmpty)
              Wrap(spacing: 8, runSpacing: 8, children: [
                for (final tag in widget.post.tags ?? <TagResponse>[])
                  Container(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6), decoration: BoxDecoration(color: MmtColors.news50, border: Border.all(color: MmtColors.ink950, width: 2)),
                    child: Text('#${tag.name}', style: GoogleFonts.inter(fontWeight: FontWeight.w800, fontSize: 11, color: MmtColors.ink950))),
              ]),
            if ((widget.post.tags ?? <TagResponse>[]).isNotEmpty) const SizedBox(height: 22),
            // Quick action row
            Row(children: [
              Expanded(child: InkWell(
                onTap: _shareWhatsAppFirst,
                child: Container(padding: const EdgeInsets.symmetric(vertical: 12),
                  decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)], color: Colors.white),
                  alignment: Alignment.center,
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    const FaIcon(FontAwesomeIcons.shareNodes, color: MmtColors.ink950, size: 14),
                    const SizedBox(width: 8),
                    Text(widget.t.common.share.toUpperCase(), style: GoogleFonts.inter(fontWeight: FontWeight.w900, letterSpacing: 1.4, fontSize: 11)),
                  ]),
                ),
              )),
              const SizedBox(width: 12),
              Expanded(child: InkWell(
                onTap: () async {
                  final url = '${Env.siteUrl}/news/${widget.slug}';
                  await Clipboard.setData(ClipboardData(text: url));
                  if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(backgroundColor: MmtColors.ink950, content: Text(widget.t.common.linkCopied, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)), duration: const Duration(milliseconds: 1200)));
                },
                child: Container(padding: const EdgeInsets.symmetric(vertical: 12),
                  decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)], color: Colors.white),
                  alignment: Alignment.center,
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    const Icon(Icons.copy_all_outlined, size: 16, color: MmtColors.ink950),
                    const SizedBox(width: 8),
                    Text(widget.t.common.copyLink.toUpperCase(), style: GoogleFonts.inter(fontWeight: FontWeight.w900, letterSpacing: 1.4, fontSize: 11)),
                  ]),
                ),
              )),
            ]),
            const SizedBox(height: 30),
            // Related Stories
            if (related != null && related.isNotEmpty) ...[
              const SectionEyebrow('Related Stories'),
              const SizedBox(height: 14),
              GridView.count(crossAxisCount: 2, shrinkWrap: true, physics: const NeverScrollableScrollPhysics(), mainAxisSpacing: 14, crossAxisSpacing: 14, childAspectRatio: 0.58,
                children: [for (final r in related) SecondaryGridCard(post: r, onTap: () { context.pop(); context.push('/article/${r.slug}?id=${r.id}'); })],
              ),
              const SizedBox(height: 24),
            ],
            Container(height: 2, color: MmtColors.ink950),
            const SizedBox(height: 18),
            Text('© ${DateTime.now().year} MAPMYTOUR LLP, India · ${widget.t.footer.copyright}', style: GoogleFonts.inter(fontSize: 10.5, color: MmtColors.ink600, fontWeight: FontWeight.w600)),
            const SizedBox(height: 20),
          ]),
        ),
      ),
    ]);
  }

  Widget _fontBtn(BuildContext c, String label, VoidCallback onTap, {bool fill = false}) {
    return InkWell(
      onTap: onTap,
      child: Container(
        constraints: const BoxConstraints(minWidth: 36),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border.all(color: MmtColors.ink950, width: 1.8),
          color: fill ? MmtColors.ink950 : (Theme.of(c).brightness == Brightness.dark ? MmtColors.ink950 : Colors.white),
        ),
        child: Text(label, style: GoogleFonts.inter(fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 0.6, color: fill ? Colors.white : (Theme.of(c).brightness == Brightness.dark ? Colors.white : MmtColors.ink950), height: 1.0)),
      ),
    );
  }

  List<Widget> _renderBody(BlogPostResponse p, BuildContext context, double scale, bool dark) {
    final raw = (p.content ?? p.contentHtml ?? '').toString();
    if (raw.isEmpty) return <Widget>[];
    final paragraphs = raw.split(RegExp(r'\n{2,}|<br\s*/?>')).where((s) => s.trim().isNotEmpty).toList(growable: false);
    return paragraphs.map((text) {
      final stripped = _stripHtml(text.trim());
      if (stripped.isEmpty) return const SizedBox.shrink();
      return Padding(
        padding: const EdgeInsets.only(bottom: 14),
        child: Text(stripped,
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

// =============================================================================
// YouTube embed + video link opener widgets
// =============================================================================
class _YoutubeEmbed extends StatefulWidget {
  const _YoutubeEmbed({required this.videoId, required this.videoUrl});
  final String videoId;
  final String videoUrl;

  @override
  State<_YoutubeEmbed> createState() => _YoutubeEmbedState();
}

class _YoutubeEmbedState extends State<_YoutubeEmbed> {
  WebViewController? _ctrl;
  bool _loaded = false;

  @override
  void initState() {
    super.initState();
    if (!kIsWeb) {
      try {
        final html = YouTubeUtil.iframeEmbed(widget.videoId);
        _ctrl = WebViewController()
          ..setJavaScriptMode(JavaScriptMode.unrestricted)
          ..setBackgroundColor(Colors.black)
          ..setNavigationDelegate(NavigationDelegate(
            onPageFinished: (_) { if (mounted) setState(() => _loaded = true); },
            onNavigationRequest: (req) {
              final u = req.url.toLowerCase();
              if (u.contains('youtube.com') || u.contains('youtu.be') || u.startsWith('data:') || u.startsWith('about:')) {
                return NavigationDecision.navigate;
              }
              return NavigationDecision.prevent;
            },
          ))
          ..loadHtmlString(html);
      } catch (_) {}
    }
  }

  @override
  Widget build(BuildContext context) {
    if (kIsWeb) {
      return _YoutubeThumbCta(videoId: widget.videoId, videoUrl: widget.videoUrl);
    }
    return Container(
      decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: Colors.black),
      child: AspectRatio(
        aspectRatio: 16 / 9,
        child: Stack(fit: StackFit.expand, children: [
          if (_ctrl != null) WebViewWidget(controller: _ctrl!),
          if (!_loaded)
            Container(color: Colors.black, child: const Center(child: SizedBox(width: 28, height: 28, child: CircularProgressIndicator(color: MmtColors.news, strokeWidth: 3)))),
        ]),
      ),
    );
  }
}

class _YoutubeThumbCta extends StatelessWidget {
  const _YoutubeThumbCta({required this.videoId, required this.videoUrl});
  final String videoId;
  final String videoUrl;

  @override
  Widget build(BuildContext context) {
    final thumb = 'https://img.youtube.com/vi/$videoId/hqdefault.jpg';
    return GestureDetector(
      onTap: () async {
        try { await launchUrl(Uri.parse(videoUrl), mode: LaunchMode.externalApplication); } catch (_) {}
      },
      child: Container(
        decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: Colors.black),
        child: AspectRatio(
          aspectRatio: 16 / 9,
          child: Stack(fit: StackFit.expand, children: [
            Image.network(thumb, fit: BoxFit.cover, errorBuilder: (_, __, ___) => Container(color: Colors.black87)),
            Container(color: Colors.black26),
            Center(child: Container(
              width: 68, height: 48,
              decoration: BoxDecoration(color: const Color(0xFFFF0000), borderRadius: BorderRadius.circular(10), boxShadow: const [BoxShadow(color: Colors.black45, blurRadius: 14, offset: Offset(0, 3))]),
              alignment: Alignment.center,
              child: const Padding(padding: EdgeInsets.only(left: 6), child: Icon(Icons.play_arrow, color: Colors.white, size: 30)),
            )),
            Positioned(left: 10, bottom: 10, child: Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4), decoration: BoxDecoration(color: const Color(0xCC000000), borderRadius: BorderRadius.circular(4)), child: Row(mainAxisSize: MainAxisSize.min, children: const [
              FaIcon(FontAwesomeIcons.youtube, size: 11, color: Colors.white), SizedBox(width: 6), Text('WATCH ON YOUTUBE', style: TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.w900, letterSpacing: 0.8)),
            ]))),
          ]),
        ),
      ),
    );
  }
}

class _OpenVideoLink extends StatelessWidget {
  const _OpenVideoLink({required this.videoUrl});
  final String videoUrl;
  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () async { try { await launchUrl(Uri.parse(videoUrl), mode: LaunchMode.externalApplication); } catch (_) {} },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: const Color(0xFFFF0000)),
        child: Row(children: const [
          FaIcon(FontAwesomeIcons.youtube, size: 18, color: Colors.white),
          SizedBox(width: 12),
          Expanded(child: Text('Open attached video in external player', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w800, fontSize: 13))),
          Icon(Icons.open_in_new, color: Colors.white, size: 18),
        ]),
      ),
    );
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
            decoration: const BoxDecoration(
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
                Container(height: 16, decoration: const BoxDecoration(color: MmtColors.ink200)),
                const SizedBox(height: 10),
                Container(
                  height: 16,
                  width: 280,
                  decoration: const BoxDecoration(color: MmtColors.ink200),
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
                boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '⚠ ${t.common.loadingError}',
                    style: const TextStyle(
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
                        boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)],
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
