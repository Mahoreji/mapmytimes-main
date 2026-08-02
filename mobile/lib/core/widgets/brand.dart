import 'package:flutter/material.dart';
import '../theme/colors.dart';

/// Common brand widgets:
///   * Brand logo (text + optional red square mark, fallback until we add raster icon)
///   * MmtChip — neobrutal 2px border chip
///   * MmtCard — news card w/ hard neobrutal border
///   * SectionEyebrow — red dot + Archivo Black uppercase heading
///   * NeobrutalIconBtn — 2px border square icon button
class BrandWidgets {
  BrandWidgets._();
}

/// Raster mapmytimes brand logo loaded from assets/icons/mapmytimes-logo.png.
/// Falls back to the legacy typographic mark (red "M" square + text) if the
/// asset cannot be loaded at runtime, so the UI never shows a blank gap.
class BrandLogo extends StatelessWidget {
  final double size;
  final bool invert;
  final bool showTagline;
  const BrandLogo({
    super.key,
    this.size = 28,
    this.invert = false,
    this.showTagline = false,
  });

  static const _asset = 'assets/icons/mapmytimes-logo.png';

  @override
  Widget build(BuildContext context) {
    final fg = invert ? Colors.white : MmtColors.ink950;
    final tagline = showTagline
        ? Padding(
            padding: const EdgeInsets.only(top: 6),
            child: Text(
              'JOURNALISM · OF · INTEGRITY',
              style: TextStyle(
                fontSize: size * 0.42,
                fontWeight: FontWeight.w700,
                letterSpacing: 1.4,
                color: fg.withValues(alpha: invert ? 0.75 : 0.65),
                fontFamily: 'Inter',
                height: 1.0,
              ),
            ),
          )
        : null;

    final image = Image.asset(
      _asset,
      height: size,
      excludeFromSemantics: true,
      filterQuality: FilterQuality.high,
      isAntiAlias: true,
      errorBuilder: (_, __, ___) => _TypographicFallback(size: size, invert: invert),
      color: invert ? Colors.white : null,
      colorBlendMode: invert ? BlendMode.srcIn : null,
    );

    if (tagline == null) return image;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [image, tagline],
    );
  }
}

/// Fallback rendered when assets/icons/mapmytimes-logo.png is unavailable.
class _TypographicFallback extends StatelessWidget {
  final double size;
  final bool invert;
  const _TypographicFallback({required this.size, required this.invert});

  @override
  Widget build(BuildContext context) {
    final fg = invert ? Colors.white : MmtColors.ink950;
    final mark = Container(
      width: size,
      height: size,
      decoration: const BoxDecoration(
        color: MmtColors.news,
        border: Border.fromBorderSide(
          BorderSide(color: MmtColors.ink950, width: 2),
        ),
      ),
      child: Center(
        child: Text(
          'M',
          style: TextStyle(
            fontSize: size * 0.62,
            fontWeight: FontWeight.w900,
            color: Colors.white,
            fontFamily: 'Archivo Black',
            height: 1.0,
          ),
        ),
      ),
    );
    final name = Text(
      'MapMyTimes',
      style: TextStyle(
        fontSize: size,
        fontWeight: FontWeight.w900,
        fontFamily: 'Archivo Black',
        color: fg,
        height: 1.0,
        letterSpacing: -0.6,
      ),
    );
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [mark, const SizedBox(width: 12), name],
    );
  }
}

class SectionEyebrow extends StatelessWidget {
  final String title;
  final Color? dotColor;
  final Color? textColor;
  const SectionEyebrow(
    this.title, {
    super.key,
    this.dotColor,
    this.textColor,
  });

  @override
  Widget build(BuildContext context) {
    final mode = Theme.of(context).brightness;
    final dark = mode == Brightness.dark;
    return Row(
      children: [
        Container(
          width: 6,
          height: 6,
          decoration: BoxDecoration(
            color: dotColor ?? MmtColors.news,
            borderRadius: BorderRadius.circular(999),
          ),
        ),
        const SizedBox(width: 10),
        Text(
          title.toUpperCase(),
          style: TextStyle(
            fontFamily: 'Archivo Black',
            fontSize: 12,
            letterSpacing: 1.8,
            fontWeight: FontWeight.w900,
            color: textColor ?? (dark ? Colors.white : MmtColors.ink900),
            height: 1.0,
          ),
        ),
      ],
    );
  }
}

class MmtChip extends StatelessWidget {
  final String label;
  final VoidCallback? onTap;
  final bool selected;
  final Color? bg;
  final Color? fg;
  const MmtChip({
    super.key,
    required this.label,
    this.onTap,
    this.selected = false,
    this.bg,
    this.fg,
  });

  @override
  Widget build(BuildContext context) {
    final backgroundColor =
        bg ?? (selected ? MmtColors.news : MmtColors.chipBg);
    final foregroundColor = fg ??
        (selected
            ? Colors.white
            : (Theme.of(context).brightness == Brightness.dark
                ? Colors.white
                : MmtColors.chipText));
    final child = Container(
      decoration: BoxDecoration(
        color: backgroundColor,
        border: const Border.fromBorderSide(
          BorderSide(color: MmtColors.ink950, width: 2),
        ),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 12,
          fontFamily: 'Inter',
          fontWeight: FontWeight.w700,
          letterSpacing: 0.6,
          color: foregroundColor,
          height: 1.0,
        ),
      ),
    );
    if (onTap == null) return child;
    return InkWell(
      onTap: onTap,
      splashFactory: NoSplash.splashFactory,
      hoverColor: Colors.transparent,
      child: child,
    );
  }
}
