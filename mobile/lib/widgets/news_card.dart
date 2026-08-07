// ---------- News Card (vertical) ----------
import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import 'package:share_plus/share_plus.dart';
import '../../core/env.dart';
import '../../core/theme/colors.dart';
import '../../models/blog_models.dart';
import '../../providers/storage_providers.dart';

// ============================================================================
// CardShareButton — SHARE action (solid NEWS RED circle, no blur illusion)
// ============================================================================
class CardShareButton extends StatefulWidget {
  final BlogPostSummaryResponse post;
  final double iconSize;
  const CardShareButton({
    super.key,
    required this.post,
    this.iconSize = 18,
  });

  @override
  State<CardShareButton> createState() => _CardShareButtonState();
}

class _CardShareButtonState extends State<CardShareButton> {
  bool _copied = false;

  static const double _diameter = 34;
  static const Color _bg = MmtColors.news;
  static const Color _fg = Colors.white;

  Future<String> _buildUrl() async {
    final slug = widget.post.slug.trim().isNotEmpty
        ? Uri.encodeComponent(widget.post.slug)
        : widget.post.id.toString();
    final site = Env.siteUrl.trim();
    final base = Uri.tryParse(site.isEmpty ? 'https://mapmytimes.com' : site);
    final origin =
        base != null && base.hasScheme && base.host.isNotEmpty
            ? '${base.scheme}://${base.host}${base.hasPort ? ':${base.port}' : ''}'
            : 'https://mapmytimes.com';
    return '$origin/news/$slug';
  }

  Future<void> _onTap() async {
    try {
      final url = await _buildUrl();
      try {
        await Share.shareUri(Uri.parse(url));
        return;
      } catch (_) {}
      await Clipboard.setData(ClipboardData(text: url));
      if (!mounted) return;
      setState(() => _copied = true);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('Link copied to clipboard'),
          duration: const Duration(milliseconds: 1400),
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
        ),
      );
      await Future<void>.delayed(const Duration(milliseconds: 1600));
      if (mounted) setState(() => _copied = false);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: _onTap,
        borderRadius: BorderRadius.circular(999),
        splashColor: _fg.withValues(alpha: 0.18),
        highlightColor: _fg.withValues(alpha: 0.10),
        child: Container(
          height: _diameter,
          width: _diameter,
          decoration: BoxDecoration(
            color: _bg,
            border: Border.all(
              color: _fg.withValues(alpha: 0.22),
              width: MmtTokens.glassHairline,
            ),
            borderRadius: BorderRadius.circular(999),
            boxShadow: const [
              BoxShadow(
                color: Color(0x2B000000),
                blurRadius: 6,
                offset: Offset(0, 2),
              ),
            ],
          ),
          alignment: Alignment.center,
          child: Icon(
            _copied ? Icons.check_rounded : Icons.share_rounded,
            size: widget.iconSize,
            color: _fg,
          ),
        ),
      ),
    );
  }
}

// ============================================================================
// CardSaveButton — SAVE/BOOKMARK action (solid NEWS RED, NO cutout illusion!)
// ============================================================================
class CardSaveButton extends ConsumerWidget {
  final BlogPostSummaryResponse post;
  final double iconSize;
  const CardSaveButton({
    super.key,
    required this.post,
    this.iconSize = 18,
  });

  static const double _diameter = 34;
  static const Color _bg = MmtColors.news;
  static const Color _fg = Colors.white;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isSaved = ref.watch(isArticleSavedProvider(post.id));
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () {
          ref
              .read(savedArticlesNotifierProvider.notifier)
              .toggle(post.id, meta: post);
        },
        borderRadius: BorderRadius.circular(999),
        splashColor: _fg.withValues(alpha: 0.18),
        highlightColor: _fg.withValues(alpha: 0.10),
        child: Container(
          height: _diameter,
          width: _diameter,
          decoration: BoxDecoration(
            color: _bg,
            border: Border.all(
              color: _fg.withValues(alpha: isSaved ? 0.30 : 0.22),
              width: MmtTokens.glassHairline,
            ),
            borderRadius: BorderRadius.circular(999),
            boxShadow: [
              BoxShadow(
                color: isSaved
                    ? MmtColors.news.withValues(alpha: 0.32)
                    : const Color(0x2B000000),
                blurRadius: isSaved ? 10 : 6,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          alignment: Alignment.center,
          child: FaIcon(
            isSaved
                ? FontAwesomeIcons.solidBookmark
                : FontAwesomeIcons.bookmark,
            size: iconSize,
            color: _fg,
          ),
        ),
      ),
    );
  }
}

class NewsCardVertical extends ConsumerWidget {
  final BlogPostSummaryResponse post;
  final VoidCallback? onTap;
  final bool showCategory;
  final bool compact;
  const NewsCardVertical({
    super.key,
    required this.post,
    this.onTap,
    this.showCategory = true,
    this.compact = false,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final date = post.publishedAt != null
        ? DateFormat('dd MMM yyyy').format(post.publishedAt!.toLocal())
        : '';
    final catName = (post.categories?.isNotEmpty ?? false) ? post.categories!.first.name : '';

    final title = Text(
      post.title,
      maxLines: compact ? 2 : 3,
      overflow: TextOverflow.ellipsis,
      style: GoogleFonts.getFont(
        'Archivo Black',
        fontSize: compact ? 15 : 18,
        height: 1.15,
        letterSpacing: -0.1,
        fontWeight: FontWeight.w900,
        color: dark ? Colors.white : MmtColors.ink950,
      ),
    );

    final excerpt = (post.excerpt ?? '').trim();
    final excerptText = excerpt.isEmpty
        ? null
        : Text(
            excerpt,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: GoogleFonts.inter(
              fontSize: 13,
              height: 1.5,
              color: dark ? Colors.white70 : MmtColors.textMuted,
            ),
          );

    final meta = Wrap(
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: 10,
      runSpacing: 4,
      children: [
        if (post.author?.name.isNotEmpty ?? false)
          Text(
            post.author!.name,
            style: GoogleFonts.inter(
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: dark ? Colors.white70 : MmtColors.ink950,
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
        if ((post.readingTimeMinutes ?? 0) > 0)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: dark ? MmtColors.ink800 : MmtColors.chipBg,
              border: Border.all(color: MmtColors.ink950, width: 1),
            ),
            child: Text(
              '${post.readingTimeMinutes} MIN',
              style: GoogleFonts.inter(
                fontSize: 10,
                fontWeight: FontWeight.w800,
                letterSpacing: 1.2,
                color: dark ? Colors.white70 : MmtColors.ink950,
              ),
            ),
          ),
      ],
    );

    return InkWell(
      onTap: onTap,
      splashFactory: NoSplash.splashFactory,
      hoverColor: Colors.transparent,
      borderRadius: MmtTokens.glassRadiusMd(),
      child: ClipRRect(
        borderRadius: MmtTokens.glassRadiusMd(),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurMd, sigmaY: MmtTokens.glassBlurMd),
          child: Container(
            decoration: BoxDecoration(
              color: dark ? MmtColors.ink900.withValues(alpha: 0.72) : Colors.white.withValues(alpha: 0.82),
              borderRadius: MmtTokens.glassRadiusMd(),
              border: Border.all(
                color: dark ? Colors.white.withValues(alpha: 0.10) : MmtColors.ink950.withValues(alpha: 0.08),
                width: MmtTokens.glassHairline,
              ),
              boxShadow: compact ? MmtTokens.glassShadowSm() : MmtTokens.glassShadowMd(),
            ),
            clipBehavior: Clip.antiAlias,
            child: Stack(
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    ClipRRect(
                      borderRadius: const BorderRadius.vertical(top: Radius.circular(MmtTokens.radiusMd - 0.8)),
                      child: AspectRatio(
                        aspectRatio: compact ? (4 / 3) : (16 / 9),
                        child: post.cover.isEmpty
                            ? Container(
                                color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                                child: const Icon(Icons.article_rounded, size: 36),
                              )
                            : CachedNetworkImage(
                                imageUrl: post.cover,
                                fit: BoxFit.cover,
                                placeholder: (ctx, _) => Container(
                                  color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                                ),
                                errorWidget: (ctx, _, __) => Container(
                                  color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                                  child: const Icon(Icons.broken_image_rounded),
                                ),
                              ),
                      ),
                    ),
                    Expanded(
                      child: Container(
                        constraints: BoxConstraints(minHeight: compact ? 78 : 110),
                        padding: EdgeInsets.all(compact ? 10 : 16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            if (showCategory && catName.isNotEmpty) ...[
                              Text(
                                catName.toUpperCase(),
                                style: GoogleFonts.inter(
                                  fontSize: compact ? 9 : 11,
                                  fontWeight: FontWeight.w900,
                                  letterSpacing: 1.0,
                                  height: 1.0,
                                  color: MmtColors.news,
                                ),
                              ),
                              const SizedBox(height: 6),
                            ],
                            title,
                            if (excerptText != null && !compact) ...[
                              const SizedBox(height: 10),
                              excerptText,
                            ],
                            if (!compact) const SizedBox(height: 12) else const Spacer(),
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.center,
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Flexible(
                                  fit: FlexFit.loose,
                                  child: compact
                                      ? Text(
                                          date,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: GoogleFonts.inter(
                                            fontSize: 10.5,
                                            fontWeight: FontWeight.w600,
                                            letterSpacing: 0.1,
                                            color: dark ? Colors.white54 : MmtColors.textFaint,
                                          ),
                                        )
                                      : meta,
                                ),
                                const SizedBox(width: 6),
                                Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    CardShareButton(post: post, iconSize: 18),
                                    const SizedBox(width: 10),
                                    CardSaveButton(post: post, iconSize: 18),
                                  ],
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ---------- Trending row item (Number + Title) ----------
class TrendingItem extends ConsumerWidget {
  final int index;
  final BlogPostSummaryResponse post;
  final VoidCallback? onTap;
  const TrendingItem({
    super.key,
    required this.index,
    required this.post,
    this.onTap,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return InkWell(
      onTap: onTap,
      borderRadius: MmtTokens.glassRadiusMd(),
      child: ClipRRect(
        borderRadius: MmtTokens.glassRadiusMd(),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurSm, sigmaY: MmtTokens.glassBlurSm),
          child: Container(
            margin: const EdgeInsets.symmetric(vertical: 4),
            padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 12),
            decoration: BoxDecoration(
              color: dark ? MmtColors.ink900.withValues(alpha: 0.55) : Colors.white.withValues(alpha: 0.70),
              borderRadius: MmtTokens.glassRadiusMd(),
              border: Border.all(
                color: dark ? Colors.white.withValues(alpha: 0.08) : MmtColors.ink950.withValues(alpha: 0.06),
                width: MmtTokens.glassHairline,
              ),
              boxShadow: MmtTokens.glassShadowSm(),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                ClipRRect(
                  borderRadius: MmtTokens.glassRadiusSm(),
                  child: BackdropFilter(
                    filter: ImageFilter.blur(sigmaX: MmtTokens.glassBlurSm, sigmaY: MmtTokens.glassBlurSm),
                    child: Container(
                      width: 44,
                      padding: const EdgeInsets.symmetric(vertical: 6),
                      decoration: BoxDecoration(
                        color: MmtColors.news.withValues(alpha: 0.92),
                        borderRadius: MmtTokens.glassRadiusSm(),
                        border: Border.all(color: Colors.white.withValues(alpha: 0.22), width: MmtTokens.glassHairline),
                        boxShadow: [
                          BoxShadow(
                            color: MmtColors.news.withValues(alpha: 0.25),
                            blurRadius: 10,
                            offset: const Offset(0, 3),
                          ),
                        ],
                      ),
                      child: Text(
                        '${index + 1}',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.getFont(
                          'Archivo Black',
                          fontSize: 22,
                          fontWeight: FontWeight.w900,
                          color: Colors.white,
                          height: 1.0,
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        post.title,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: GoogleFonts.getFont(
                          'Archivo Black',
                          fontSize: 15,
                          height: 1.2,
                          letterSpacing: -0.05,
                          fontWeight: FontWeight.w900,
                          color: dark ? Colors.white : MmtColors.ink950,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Flexible(
                            fit: FlexFit.loose,
                            child: Text(
                              [
                                if (post.author?.name.isNotEmpty ?? false) post.author!.name,
                                if (post.publishedAt != null)
                                  DateFormat('dd MMM yyyy').format(post.publishedAt!.toLocal()),
                                if ((post.viewCount ?? 0) > 0) '${post.viewCount} views',
                              ].join('  ·  '),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: GoogleFonts.inter(
                                fontSize: 12,
                                fontWeight: FontWeight.w500,
                                color: dark ? Colors.white54 : MmtColors.textFaint,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              CardShareButton(post: post, iconSize: 18),
                              const SizedBox(width: 10),
                              CardSaveButton(post: post, iconSize: 18),
                            ],
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
