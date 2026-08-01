// ---------------------- ARTICLE SCREEN ----------------------
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import 'package:share_plus/share_plus.dart';
import '../core/env.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../models/blog_models.dart';
import '../services/blog_service.dart';

class NewsArticleScreen extends StatefulWidget {
  final String postId;
  final String slug;
  const NewsArticleScreen({super.key, required this.postId, required this.slug});

  @override
  State<NewsArticleScreen> createState() => _NewsArticleScreenState();
}

class _NewsArticleScreenState extends State<NewsArticleScreen> {
  late final Future<BlogPostResponse?> _load = () async {
    final svc = BlogService.create();
    await svc.incrementView(widget.postId);
    return await svc.postById(widget.postId);
  }();

  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final t = LangScope.of(ctx);
    return Scaffold(
      backgroundColor: dark ? MmtColors.ink950 : MmtColors.surfaceLight,
      appBar: AppBar(
        elevation: 0,
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        actions: [
          IconButton(
            tooltip: t.search,
            icon: const Icon(Icons.share_rounded),
            onPressed: _share,
          ),
          IconButton(
            tooltip: 'Copy link',
            icon: const Icon(Icons.link_rounded),
            onPressed: _copyLink,
          ),
          const SizedBox(width: 8),
        ],
      ),
      extendBodyBehindAppBar: false,
      body: FutureBuilder<BlogPostResponse?>(
        future: _load,
        builder: (c, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator(color: MmtColors.news));
          }
          if (snap.hasError) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Text(t.somethingWentWrong),
              ),
            );
          }
          final p = snap.data;
          if (p == null) {
            return Center(child: Text(t.noStoriesYet));
          }
          return _body(c, p, t, dark);
        },
      ),
    );
  }

  Widget _body(BuildContext ctx, BlogPostResponse p, Dict t, bool dark) {
    final date = p.publishedAt != null
        ? DateFormat('EEEE · dd MMMM yyyy · hh:mm a').format(p.publishedAt!.toLocal())
        : '';
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (p.cover.isNotEmpty)
            Container(
              decoration: const BoxDecoration(
                border: Border(bottom: BorderSide(color: MmtColors.ink950, width: 2)),
              ),
              child: CachedNetworkImage(
                imageUrl: p.cover,
                width: double.infinity,
                height: 260,
                fit: BoxFit.cover,
                errorWidget: (_, __, ___) => Container(
                  height: 260,
                  color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                  child: const Icon(Icons.broken_image),
                ),
              ),
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 40),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 10,
                  runSpacing: 8,
                  children: [
                    for (final c in (p.categories ?? <CategoryResponse>[]))
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                        decoration: const BoxDecoration(
                          color: MmtColors.news,
                          border: Border.fromBorderSide(
                            BorderSide(color: MmtColors.ink950, width: 2),
                          ),
                        ),
                        child: Text(
                          c.name.toUpperCase(),
                          style: GoogleFonts.inter(
                            fontSize: 11,
                            fontWeight: FontWeight.w800,
                            letterSpacing: 1.2,
                            color: Colors.white,
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                Text(
                  p.title,
                  style: GoogleFonts.getFont(
                    'Archivo Black',
                    fontSize: 28,
                    height: 1.06,
                    letterSpacing: -0.5,
                    fontWeight: FontWeight.w900,
                    color: dark ? Colors.white : MmtColors.ink950,
                  ),
                ),
                if (p.excerpt?.isNotEmpty ?? false) ...[
                  const SizedBox(height: 14),
                  Text(
                    p.excerpt!,
                    style: GoogleFonts.inter(
                      fontSize: 16,
                      height: 1.6,
                      fontStyle: FontStyle.italic,
                      color: dark ? Colors.white70 : MmtColors.textMuted,
                    ),
                  ),
                ],
                const SizedBox(height: 20),
                Container(
                  height: 2,
                  width: double.infinity,
                  color: MmtColors.ink950,
                ),
                const SizedBox(height: 16),
                Wrap(
                  spacing: 12,
                  runSpacing: 10,
                  children: [
                    if (p.author?.name.isNotEmpty ?? false)
                      Text(
                        t.authorLabel(p.author!.name),
                        style: GoogleFonts.inter(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: dark ? Colors.white : MmtColors.ink950,
                        ),
                      ),
                    if (date.isNotEmpty)
                      Text(
                        date,
                        style: GoogleFonts.inter(
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                          color: dark ? Colors.white54 : MmtColors.textFaint,
                        ),
                      ),
                    if ((p.readingTimeMinutes ?? 0) > 0)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
                        decoration: BoxDecoration(
                          border: Border.all(color: MmtColors.ink950, width: 1.5),
                          color: dark ? MmtColors.ink900 : MmtColors.chipBg,
                        ),
                        child: Text(
                          '${p.readingTimeMinutes} MIN READ',
                          style: GoogleFonts.inter(
                            fontSize: 10,
                            fontWeight: FontWeight.w800,
                            letterSpacing: 1.2,
                            color: dark ? Colors.white : MmtColors.ink950,
                          ),
                        ),
                      ),
                    if ((p.viewCount ?? 0) > 0)
                      Text(
                        '${p.viewCount} views',
                        style: GoogleFonts.inter(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: dark ? Colors.white54 : MmtColors.textFaint,
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 24),
                if ((p.contentHtml?.isNotEmpty ?? false) || (p.content?.isNotEmpty ?? false))
                  _renderBody(p, dark),
                const SizedBox(height: 28),
                if (p.tags?.isNotEmpty ?? false) ...[
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final tag in p.tags!)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            border: Border.all(color: MmtColors.ink950, width: 1.5),
                            color: dark ? MmtColors.ink900 : MmtColors.chipBg,
                          ),
                          child: Text(
                            '#${tag.name}',
                            style: GoogleFonts.inter(
                              fontSize: 12,
                              fontWeight: FontWeight.w700,
                              color: dark ? Colors.white : MmtColors.ink950,
                            ),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 20),
                ],
                Container(height: 2, width: double.infinity, color: MmtColors.ink950),
                const SizedBox(height: 20),
                Text(
                  t.copyrightYear(DateTime.now().year),
                  style: GoogleFonts.inter(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.3,
                    color: dark ? Colors.white38 : MmtColors.textFaint,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _renderBody(BlogPostResponse p, bool dark) {
    final plain = p.content ?? '';
    final html = p.contentHtml ?? '';
    // If the API returns plain paragraphs separated by \n\n, render them
    // as styled <p> equivalents. No webview_html dependency yet — keep text only.
    final String text = (html.isNotEmpty) ? _stripHtml(html) : plain;
    final paragraphs = text
        .split(RegExp(r'\n{2,}'))
        .map((s) => s.replaceAll(RegExp(r'\n'), ' ').trim())
        .where((s) => s.isNotEmpty)
        .toList(growable: false);
    return Column(
      children: [
        for (final para in paragraphs) ...[
          Text(
            para,
            style: GoogleFonts.inter(
              fontSize: 16,
              height: 1.75,
              color: dark ? Colors.white : MmtColors.ink900,
            ),
          ),
          const SizedBox(height: 18),
        ],
      ],
    );
  }

  String _stripHtml(String s) {
    return s
        .replaceAll(RegExp(r'<br\s*/?>'), '\n')
        .replaceAll(RegExp(r'</p>'), '\n\n')
        .replaceAll(RegExp(r'<[^>]+>'), '')
        .trim();
  }

  Future<void> _share() async {
    final t = LangScope.of(context);
    final url = '${Env.siteUrl}/news/${Uri.encodeComponent(widget.slug)}';
    try {
      await Share.share('${widget.postId} — MapMyTimes\n$url', subject: t.readMore);
    } catch (_) {}
  }

  Future<void> _copyLink() async {
    final url = '${Env.siteUrl}/news/${Uri.encodeComponent(widget.slug)}';
    await Clipboard.setData(ClipboardData(text: url));
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Link copied: $url')),
    );
  }
}
