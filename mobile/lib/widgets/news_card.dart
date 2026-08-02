// ---------- News Card (vertical) ----------
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../../core/theme/colors.dart';
import '../../models/blog_models.dart';

class NewsCardVertical extends StatelessWidget {
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
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final date = post.publishedAt != null
        ? DateFormat('dd MMM yyyy').format(post.publishedAt!.toLocal())
        : '';
    final catName = (post.categories?.isNotEmpty ?? false) ? post.categories!.first.name : '';

    final chip = Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
      decoration: const BoxDecoration(
        color: MmtColors.news,
        border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
      ),
      child: Text(
        (catName.isNotEmpty ? catName : 'NEWS').toUpperCase(),
        style: GoogleFonts.inter(
          fontSize: 11,
          fontWeight: FontWeight.w800,
          letterSpacing: 1.0,
          color: Colors.white,
          height: 1.0,
        ),
      ),
    );

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
      child: Container(
        decoration: BoxDecoration(
          color: dark ? MmtColors.ink900 : Colors.white,
          border: const Border.fromBorderSide(
            BorderSide(color: MmtColors.ink950, width: 2),
          ),
          boxShadow: const [
            BoxShadow(
              color: MmtColors.ink950,
              offset: Offset(4, 4),
              blurRadius: 0,
            ),
          ],
        ),
        clipBehavior: Clip.hardEdge,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AspectRatio(
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
            Expanded(
              child: Container(
                constraints: BoxConstraints(minHeight: compact ? 78 : 110),
                padding: EdgeInsets.all(compact ? 10 : 16),
                color: dark ? MmtColors.ink900 : Colors.white,
                child: compact
                    ? Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (showCategory && catName.isNotEmpty) ...[
                            Text(
                              catName.toUpperCase(),
                              style: GoogleFonts.inter(
                                fontSize: 9,
                                fontWeight: FontWeight.w900,
                                letterSpacing: 1.0,
                                height: 1.0,
                                color: MmtColors.news,
                              ),
                            ),
                            const SizedBox(height: 6),
                          ],
                          Text(
                            post.title,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: GoogleFonts.getFont(
                              'Archivo Black',
                              fontSize: 12,
                              height: 1.18,
                              letterSpacing: -0.05,
                              fontWeight: FontWeight.w900,
                              color: dark ? Colors.white : MmtColors.ink950,
                            ),
                          ),
                          const SizedBox(height: 8),
                          const Spacer(),
                          Text(
                            date,
                            style: GoogleFonts.inter(
                              fontSize: 10.5,
                              fontWeight: FontWeight.w600,
                              letterSpacing: 0.1,
                              color: dark ? Colors.white54 : MmtColors.textFaint,
                            ),
                          ),
                        ],
                      )
                    : Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (showCategory && catName.isNotEmpty) ...[
                            chip,
                            const SizedBox(height: 12),
                          ],
                          Flexible(fit: FlexFit.loose, child: title),
                          if (excerptText != null) ...[
                            const SizedBox(height: 10),
                            Flexible(fit: FlexFit.loose, child: excerptText),
                          ],
                          const Spacer(),
                          meta,
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

// ---------- Trending row item (Number + Title) ----------
class TrendingItem extends StatelessWidget {
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
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 44,
              padding: const EdgeInsets.symmetric(vertical: 6),
              decoration: const BoxDecoration(
                color: MmtColors.news,
                border: Border.fromBorderSide(
                  BorderSide(color: MmtColors.ink950, width: 2),
                ),
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
                  Text(
                    [
                      if (post.author?.name.isNotEmpty ?? false) post.author!.name,
                      if (post.publishedAt != null)
                        DateFormat('dd MMM yyyy').format(post.publishedAt!.toLocal()),
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
            ),
          ],
        ),
      ),
    );
  }
}
