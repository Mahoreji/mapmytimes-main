// =============================================================================
// Editorial components — Reuters-inspired, MapMyTimes-branded
//   * BreakingBanner     — red "BREAKING" takeover with animated red dot + time
//   * PulsingLiveDot     — red pulsing microphone / live indicator
//   * HeroStoryCard      — tier-1 hero: very large 16:9 cover + bold headline
//   * SecondaryGridCard  — tier-2 secondary: compact 2-col grid thumbnail
//   * TextOnlyStoryRow   — tier-3 tertiary: text-only list, no image
// =============================================================================

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../core/theme/colors.dart';
import '../models/blog_models.dart';

// ============================================================================
// Pulsing Live Dot — microphone motif red pulsing indicator
// ============================================================================
class PulsingLiveDot extends StatefulWidget {
  final bool pulse;
  final double size;
  const PulsingLiveDot({
    super.key,
    this.pulse = true,
    this.size = 8,
  });

  @override
  State<PulsingLiveDot> createState() => _PulsingLiveDotState();
}

class _PulsingLiveDotState extends State<PulsingLiveDot>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c;
  late final Animation<double> _a;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    );
    _a = Tween<double>(begin: 1.0, end: 2.2).animate(
      CurvedAnimation(parent: _c, curve: Curves.easeOut),
    );
    if (widget.pulse) {
      _c.repeat(reverse: true);
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _a,
      builder: (_, __) {
        return SizedBox(
          width: widget.size * 2.4,
          height: widget.size * 2.4,
          child: Stack(
            alignment: Alignment.center,
            children: [
              Container(
                width: widget.size * _a.value,
                height: widget.size * _a.value,
                decoration: BoxDecoration(
                  color: MmtColors.news.withValues(
                    alpha: (1 - (_a.value - 1) / 1.2).clamp(0.0, 1.0) * 0.35,
                  ),
                  shape: BoxShape.circle,
                ),
              ),
              Container(
                width: widget.size,
                height: widget.size,
                decoration: const BoxDecoration(
                  color: MmtColors.news,
                  shape: BoxShape.circle,
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

// ============================================================================
// Breaking News Banner
// ============================================================================
class BreakingBanner extends StatelessWidget {
  final String headline;
  final DateTime? publishedAt;
  final VoidCallback? onTap;
  const BreakingBanner({
    super.key,
    required this.headline,
    this.publishedAt,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final timeStr = publishedAt != null
        ? DateFormat('hh:mm a · dd MMM').format(publishedAt!.toLocal())
        : '';
    return InkWell(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: MmtColors.news,
          border: const Border(
            bottom: BorderSide(color: MmtColors.ink950, width: 2),
          ),
          boxShadow: const [
            BoxShadow(
              color: Color(0x40E31E24),
              offset: Offset(0, 6),
              blurRadius: 20,
            ),
          ],
        ),
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
              decoration: BoxDecoration(
                color: MmtColors.ink950,
                border: Border.all(color: MmtColors.ink950, width: 2),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const PulsingLiveDot(size: 5),
                  const SizedBox(width: 6),
                  Text(
                    'BREAKING',
                    style: GoogleFonts.getFont(
                      'Archivo Black',
                      color: Colors.white,
                      fontSize: 10.5,
                      letterSpacing: 1.8,
                      fontWeight: FontWeight.w900,
                      height: 1.0,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                headline,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: GoogleFonts.getFont(
                  'Archivo Black',
                  color: Colors.white,
                  fontSize: 13.5,
                  height: 1.18,
                  fontWeight: FontWeight.w900,
                  letterSpacing: -0.1,
                ),
              ),
            ),
            if (timeStr.isNotEmpty) ...[
              const SizedBox(width: 12),
              Text(
                timeStr,
                style: GoogleFonts.inter(
                  color: Colors.white70,
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.4,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

// ============================================================================
// Hero Story Card — tier-1, 16:9 big image + very bold headline
// ============================================================================
class HeroStoryCard extends StatelessWidget {
  final BlogPostSummaryResponse post;
  final VoidCallback? onTap;
  const HeroStoryCard({
    super.key,
    required this.post,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final catName =
        (post.categories?.isNotEmpty ?? false) ? post.categories!.first.name : '';
    final date = post.publishedAt != null
        ? DateFormat('dd MMM yyyy').format(post.publishedAt!.toLocal())
        : '';
    return InkWell(
      onTap: onTap,
      splashFactory: NoSplash.splashFactory,
      child: Container(
        decoration: const BoxDecoration(
          border: Border.fromBorderSide(
            BorderSide(color: MmtColors.ink950, width: 2),
          ),
          boxShadow: [
            BoxShadow(
              color: MmtColors.ink950,
              offset: Offset(6, 6),
              blurRadius: 0,
            ),
          ],
        ),
        clipBehavior: Clip.hardEdge,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AspectRatio(
              aspectRatio: 16 / 9,
              child: post.cover.isEmpty
                  ? Container(color: dark ? MmtColors.ink800 : MmtColors.chipBg)
                  : CachedNetworkImage(
                      imageUrl: post.cover,
                      fit: BoxFit.cover,
                      errorWidget: (_, __, ___) => Container(
                        color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                        child: const Icon(Icons.broken_image_rounded),
                      ),
                    ),
            ),
            Container(
              width: double.infinity,
              color: dark ? MmtColors.ink900 : Colors.white,
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (catName.isNotEmpty) ...[
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
                      color: MmtColors.news,
                      child: Text(
                        catName.toUpperCase(),
                        style: GoogleFonts.inter(
                          fontSize: 11,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 1.0,
                          color: Colors.white,
                          height: 1.0,
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                  ],
                  Text(
                    post.title,
                    style: GoogleFonts.getFont(
                      'Archivo Black',
                      fontSize: 22,
                      height: 1.08,
                      letterSpacing: -0.3,
                      fontWeight: FontWeight.w900,
                      color: dark ? Colors.white : MmtColors.ink950,
                    ),
                  ),
                  const SizedBox(height: 12),
                  if ((post.excerpt ?? '').isNotEmpty)
                    Text(
                      post.excerpt!,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.inter(
                        fontSize: 14,
                        height: 1.5,
                        fontWeight: FontWeight.w400,
                        color: dark ? Colors.white70 : MmtColors.textMuted,
                      ),
                    ),
                  const SizedBox(height: 16),
                  Wrap(
                    spacing: 12,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      if ((post.readingTimeMinutes ?? 0) > 0)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                          decoration: BoxDecoration(
                            border: Border.all(color: MmtColors.ink950, width: 2),
                            color: dark ? MmtColors.ink800 : Colors.white,
                          ),
                          child: Text(
                            '${post.readingTimeMinutes} MIN READ',
                            style: GoogleFonts.inter(
                              fontSize: 10.5,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 1.3,
                              color: dark ? Colors.white70 : MmtColors.ink950,
                              height: 1.0,
                            ),
                          ),
                        ),
                      Text(
                        [
                          if (post.author?.name.isNotEmpty ?? false) post.author!.name,
                          if (date.isNotEmpty) date,
                          if ((post.viewCount ?? 0) > 0) '${post.viewCount} views',
                        ].join('  ·  '),
                        style: GoogleFonts.inter(
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                          color: dark ? Colors.white54 : MmtColors.textFaint,
                        ),
                      ),
                    ],
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

// ============================================================================
// Secondary Grid Card — tier-2 compact 2-col thumbnail cards
// ============================================================================
class SecondaryGridCard extends StatelessWidget {
  final BlogPostSummaryResponse post;
  final VoidCallback? onTap;
  const SecondaryGridCard({
    super.key,
    required this.post,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final catName =
        (post.categories?.isNotEmpty ?? false) ? post.categories!.first.name : '';
    return InkWell(
      onTap: onTap,
      splashFactory: NoSplash.splashFactory,
      child: Container(
        decoration: const BoxDecoration(
          border: Border.fromBorderSide(
            BorderSide(color: MmtColors.ink950, width: 2),
          ),
          boxShadow: [
            BoxShadow(
              color: MmtColors.ink950,
              offset: Offset(3, 3),
              blurRadius: 0,
            ),
          ],
        ),
        clipBehavior: Clip.hardEdge,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AspectRatio(
              aspectRatio: 4 / 3,
              child: post.cover.isEmpty
                  ? Container(color: dark ? MmtColors.ink800 : MmtColors.chipBg)
                  : CachedNetworkImage(
                      imageUrl: post.cover,
                      fit: BoxFit.cover,
                      errorWidget: (_, __, ___) =>
                          Container(color: dark ? MmtColors.ink800 : MmtColors.chipBg),
                    ),
            ),
            Expanded(
              child: Container(
                constraints: const BoxConstraints(minHeight: 92),
                padding: const EdgeInsets.all(12),
                color: dark ? MmtColors.ink900 : Colors.white,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (catName.isNotEmpty) ...[
                      Text(
                        catName.toUpperCase(),
                        style: GoogleFonts.inter(
                          fontSize: 10,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 1.2,
                          color: MmtColors.news,
                          height: 1.0,
                        ),
                      ),
                      const SizedBox(height: 8),
                    ],
                    Text(
                      post.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.getFont(
                        'Archivo Black',
                        fontSize: 14,
                        height: 1.2,
                        fontWeight: FontWeight.w900,
                        letterSpacing: -0.1,
                        color: dark ? Colors.white : MmtColors.ink950,
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Spacer(),
                    if ((post.readingTimeMinutes ?? 0) > 0)
                      Text(
                        '${post.readingTimeMinutes} MIN',
                        style: GoogleFonts.inter(
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 1.2,
                          color: dark ? Colors.white54 : MmtColors.textFaint,
                          height: 1.0,
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ============================================================================
// Text Only Story Row — tier-3, image-free list rows for lower priority
// ============================================================================
class TextOnlyStoryRow extends StatelessWidget {
  final BlogPostSummaryResponse post;
  final int? index;
  final VoidCallback? onTap;
  const TextOnlyStoryRow({
    super.key,
    required this.post,
    this.index,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final catName =
        (post.categories?.isNotEmpty ?? false) ? post.categories!.first.name : '';
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: MmtColors.divider, width: 1)),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (index != null) ...[
              SizedBox(
                width: 28,
                child: Text(
                  '$index',
                  style: GoogleFonts.getFont(
                    'Archivo Black',
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                    color: MmtColors.news,
                    height: 1.0,
                  ),
                ),
              ),
              const SizedBox(width: 8),
            ],
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (catName.isNotEmpty) ...[
                    Text(
                      catName.toUpperCase(),
                      style: GoogleFonts.inter(
                        fontSize: 10,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 1.2,
                        color: MmtColors.news,
                        height: 1.0,
                      ),
                    ),
                    const SizedBox(height: 8),
                  ],
                  Text(
                    post.title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: GoogleFonts.getFont(
                      'Archivo Black',
                      fontSize: 15,
                      height: 1.18,
                      letterSpacing: -0.1,
                      fontWeight: FontWeight.w900,
                      color: dark ? Colors.white : MmtColors.ink950,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    [
                      if (post.author?.name.isNotEmpty ?? false) post.author!.name,
                      if (post.publishedAt != null)
                        DateFormat('dd MMM').format(post.publishedAt!.toLocal()),
                    ].join('  ·  '),
                    style: GoogleFonts.inter(
                      fontSize: 11.5,
                      fontWeight: FontWeight.w500,
                      color: dark ? Colors.white54 : MmtColors.textFaint,
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

// ============================================================================
// News Card Horizontal (CNN "Across Our Coverage" layout — TEXT LEFT + THUMB RIGHT)
//   Tier 2 high-signal story: news WITH IMAGE side-by-side not stacked
//   Left 2/3: red category | 3-line bold title | 1-ln excerpt (if any) | meta (author · date · views)
//   Right 1/3: 112×96 thumbnail (BoxFit.cover ClipRRect 8 radius)
// ============================================================================
class NewsCardHorizontal extends StatelessWidget {
  final BlogPostSummaryResponse post;
  final VoidCallback? onTap;
  const NewsCardHorizontal({
    super.key,
    required this.post,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final catName =
        (post.categories?.isNotEmpty ?? false) ? post.categories!.first.name : '';
    final date = post.publishedAt != null
        ? DateFormat('dd MMM yyyy').format(post.publishedAt!.toLocal())
        : '';
    final excerpt = (post.excerpt ?? '').trim();
    return InkWell(
      onTap: onTap,
      splashFactory: NoSplash.splashFactory,
      child: Container(
        clipBehavior: Clip.hardEdge,
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: dark ? MmtColors.ink800.withValues(alpha: 0.75) : MmtColors.divider,
              width: 1,
            ),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // ---------- LEFT: TEXT REGION 66% ----------
            Expanded(
              child: Container(
                padding: const EdgeInsets.only(right: 14),
                decoration: BoxDecoration(
                  border: Border(
                    left: BorderSide(
                      color: MmtColors.news,
                      width: 2.5,
                    ),
                  ),
                ),
                child: Padding(
                  padding: const EdgeInsets.only(left: 10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (catName.isNotEmpty) ...[
                        Text(
                          catName.toUpperCase(),
                          style: GoogleFonts.inter(
                            fontSize: 10.5,
                            fontWeight: FontWeight.w900,
                            letterSpacing: 1.1,
                            color: MmtColors.news,
                            height: 1.0,
                          ),
                        ),
                        const SizedBox(height: 8),
                      ],
                      Text(
                        post.title,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: GoogleFonts.getFont(
                          'Archivo Black',
                          fontSize: 15.5,
                          height: 1.22,
                          letterSpacing: -0.08,
                          fontWeight: FontWeight.w900,
                          color: dark ? Colors.white : MmtColors.ink950,
                        ),
                      ),
                      if (excerpt.isNotEmpty) ...[
                        const SizedBox(height: 7),
                        Text(
                          excerpt,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: GoogleFonts.inter(
                            fontSize: 12.5,
                            height: 1.4,
                            fontWeight: FontWeight.w400,
                            color: dark ? Colors.white70 : MmtColors.textMuted,
                          ),
                        ),
                      ],
                      const SizedBox(height: 10),
                      Text(
                        [
                          if (post.author?.name.isNotEmpty ?? false) post.author!.name,
                          if (date.isNotEmpty) date,
                          if ((post.viewCount ?? 0) > 0) '${post.viewCount} views',
                        ].join('  ·  '),
                        style: GoogleFonts.inter(
                          fontSize: 11.5,
                          fontWeight: FontWeight.w500,
                          color: dark ? Colors.white54 : MmtColors.textFaint,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            // ---------- RIGHT: THUMBNAIL IMAGE (fixed 112 x 96) ----------
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Container(
                width: 112,
                height: 96,
                color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                child: post.cover.isEmpty
                    ? const Icon(Icons.article_rounded, size: 30)
                    : CachedNetworkImage(
                        imageUrl: post.cover,
                        fit: BoxFit.cover,
                        placeholder: (ctx, _) => Container(
                          color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                        ),
                        errorWidget: (_, __, ___) => Container(
                          color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                          child: const Icon(Icons.broken_image_rounded),
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
