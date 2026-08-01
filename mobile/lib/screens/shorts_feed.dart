import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../core/widgets/brand.dart';
import '../models/blog_models.dart';
import '../services/blog_service.dart';

class ShortsFeedScreen extends StatefulWidget {
  final int startIndex;
  const ShortsFeedScreen({super.key, this.startIndex = 0});

  @override
  State<ShortsFeedScreen> createState() => _ShortsFeedScreenState();
}

class _ShortsFeedScreenState extends State<ShortsFeedScreen> {
  late final Future<List<BlogPostSummaryResponse>> _load = BlogService.create()
      .postsList(postType: 'SHORT', status: 'PUBLISHED', size: 20, sort: '-publishedAt')
      .then((p) => p.items);

  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final t = LangScope.of(ctx);
    return Scaffold(
      backgroundColor: dark ? MmtColors.ink950 : MmtColors.ink900,
      appBar: AppBar(
        backgroundColor: MmtColors.ink950,
        foregroundColor: Colors.white,
        title: Text('Shorts'),
        titleTextStyle: GoogleFonts.getFont(
          'Archivo Black',
          fontSize: 20,
          fontWeight: FontWeight.w900,
          color: Colors.white,
        ),
        leading: const BackButton(color: Colors.white),
      ),
      body: FutureBuilder<List<BlogPostSummaryResponse>>(
        future: _load,
        builder: (c, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator(color: MmtColors.news));
          }
          final items = snap.data ?? <BlogPostSummaryResponse>[];
          if (items.isEmpty) {
            return Center(
              child: Container(
                margin: const EdgeInsets.all(20),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  border: Border.all(color: MmtColors.news, width: 2),
                  color: MmtColors.ink900,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const BrandLogo(size: 22, invert: true),
                    const SizedBox(height: 16),
                    Text(
                      t.watchMore,
                      style: GoogleFonts.inter(color: Colors.white),
                    ),
                  ],
                ),
              ),
            );
          }
          return PageView.builder(
            scrollDirection: Axis.vertical,
            itemCount: items.length,
            itemBuilder: (c, i) {
              final p = items[i];
              return _ShortTile(post: p);
            },
          );
        },
      ),
    );
  }
}

class _ShortTile extends StatelessWidget {
  final BlogPostSummaryResponse post;
  const _ShortTile({required this.post});

  @override
  Widget build(BuildContext ctx) {
    return Container(
      width: double.infinity,
      height: double.infinity,
      color: MmtColors.ink950,
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Spacer(),
          Container(
            width: 12,
            height: 12,
            decoration: const BoxDecoration(
              color: MmtColors.news,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            post.title,
            style: GoogleFonts.getFont(
              'Archivo Black',
              fontSize: 26,
              height: 1.08,
              letterSpacing: -0.4,
              fontWeight: FontWeight.w900,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 14),
          if (post.excerpt != null && post.excerpt!.isNotEmpty)
            Text(
              post.excerpt!,
              maxLines: 6,
              style: GoogleFonts.inter(
                fontSize: 15,
                height: 1.55,
                color: Colors.white70,
              ),
            ),
          const SizedBox(height: 18),
          Wrap(
            spacing: 10,
            children: [
              if (post.author?.name.isNotEmpty ?? false)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.1),
                    border: Border.all(color: Colors.white24, width: 1),
                  ),
                  child: Text(
                    post.author!.name,
                    style: GoogleFonts.inter(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                    ),
                  ),
                ),
              if ((post.likeCount ?? 0) > 0)
                _metaChip(Icons.favorite_border_rounded, post.likeCount!),
              if ((post.viewCount ?? 0) > 0)
                _metaChip(Icons.remove_red_eye_outlined, post.viewCount!),
              if ((post.commentCount ?? 0) > 0)
                _metaChip(Icons.mode_comment_outlined, post.commentCount!),
            ],
          ),
          const SizedBox(height: 44),
        ],
      ),
    );
  }

  Widget _metaChip(IconData icon, int count) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.1),
          border: Border.all(color: Colors.white24, width: 1),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 14, color: Colors.white),
            const SizedBox(width: 6),
            Text(
              '$count',
              style: GoogleFonts.inter(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: Colors.white,
              ),
            ),
          ],
        ),
      );
}
