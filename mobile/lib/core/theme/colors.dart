import 'package:flutter/material.dart';

/// MapMyTimes — Neo-Brutalist color palette (1:1 copy of frontend tailwind.config.ts)
class MmtColors {
  MmtColors._();

  // News Red — #E31E24 (primary brand)
  static const Color news = Color(0xFFE31E24);
  static const Color news50 = Color(0xFFFDECEE);
  static const Color news100 = Color(0xFFFBD8DC);
  static const Color news200 = Color(0xFFF5AFB6);
  static const Color news300 = Color(0xFFEE858F);
  static const Color news400 = Color(0xFFE85B69);
  static const Color news500 = Color(0xFFE31E24);
  static const Color news600 = Color(0xFFD0121A);
  static const Color news700 = Color(0xFFA80D14);
  static const Color news800 = Color(0xFF800A10);
  static const Color news900 = Color(0xFF58070A);

  // Ink black family — background / surface dark
  static const Color ink950 = Color(0xFF0A0A0A);
  static const Color ink900 = Color(0xFF121212);
  static const Color ink850 = Color(0xFF171717);
  static const Color ink800 = Color(0xFF1A1A1A);
  static const Color ink700 = Color(0xFF242424);
  static const Color ink600 = Color(0xFF2E2E2E);
  static const Color ink200 = Color(0xFFD4D4D4);
  static const Color ink100 = Color(0xFFE5E5E5);

  // Semantic
  static const Color background = Color(0xFFFFFBF8);
  static const Color surfaceLight = Color(0xFFFFFBF8);
  static const Color surface = Color(0xFFFFFFFF);
  static const Color divider = Color(0xFFECECEC);
  static const Color textBody = Color(0xFF1A1A1A);
  static const Color textMuted = Color(0xFF525252);
  static const Color textFaint = Color(0xFF7A7A7A);
  static const Color chipBg = Color(0xFFF4F4F5);
  static const Color chipText = Color(0xFF2E2E2E);
  static const Color success = Color(0xFF15803D);
  static const Color warning = Color(0xFFB45309);
  static const Color danger = Color(0xFFB91C1C);
}

/// Design tokens (spacing, radii, shadows)
class MmtTokens {
  MmtTokens._();

  // Border radii — Liquid Glass: organic rounded shapes (prev sharp squares)
  static const double radiusXs = 4.0;
  static const double radiusSm = 8.0;
  static const double radiusMd = 14.0;
  static const double radiusLg = 20.0;
  static const double radiusXl = 28.0;
  static const double radiusFull = 999.0;

  // Spacing scale
  static const double xs = 4.0;
  static const double sm = 8.0;
  static const double md = 12.0;
  static const double base = 16.0;
  static const double lg = 20.0;
  static const double xl = 24.0;
  static const double xxl = 32.0;
  static const double sectionGutter = 40.0;

  // ================ LIQUID GLASS TOKENS ================
  // Blur sigmas for BackdropFilter / ImageFilter.blur
  static const double glassBlurSm = 12.0;
  static const double glassBlurMd = 20.0;
  static const double glassBlurLg = 30.0;

  // Frosted surface alpha backgrounds
  static Color glassBgLight(double alpha) => Colors.white.withValues(alpha: alpha);
  static Color glassBgDark(double alpha) => MmtColors.ink950.withValues(alpha: alpha);

  // Hairline border 0.8 px, translucent
  static BorderSide glassBorderLight() => BorderSide(color: Colors.white.withValues(alpha: 0.18), width: 0.8);
  static BorderSide glassBorderDark() => BorderSide(color: MmtColors.ink950.withValues(alpha: 0.12), width: 0.8);
  static BorderSide glassBorder({required bool dark}) => dark ? glassBorderLight() : glassBorderDark();

  // Corners: Liquid Glass smooth radius helpers
  static BorderRadius glassRadiusSm() => BorderRadius.circular(radiusSm);
  static BorderRadius glassRadiusMd() => BorderRadius.circular(radiusMd);
  static BorderRadius glassRadiusLg() => BorderRadius.circular(radiusLg);
  static BorderRadius glassRadiusXl() => BorderRadius.circular(radiusXl);
  static BorderRadius glassRadiusPill() => BorderRadius.circular(radiusFull);

  // Ambient soft ambient drop (not hard neobrutal) — soft diffuse for glass cards
  static List<BoxShadow> glassShadowSm() => [
        BoxShadow(color: MmtColors.ink950.withValues(alpha: 0.10), offset: const Offset(0, 2), blurRadius: 6, spreadRadius: 0),
      ];
  static List<BoxShadow> glassShadowMd() => [
        BoxShadow(color: MmtColors.ink950.withValues(alpha: 0.14), offset: const Offset(0, 6), blurRadius: 18, spreadRadius: 0),
        BoxShadow(color: MmtColors.ink950.withValues(alpha: 0.06), offset: const Offset(0, 2), blurRadius: 4, spreadRadius: 0),
      ];
  static List<BoxShadow> glassShadowNav() => [
        BoxShadow(color: MmtColors.ink950.withValues(alpha: 0.22), offset: const Offset(0, -4), blurRadius: 28, spreadRadius: 0),
      ];
  static List<BoxShadow> glassShadowHeader() => [
        BoxShadow(color: MmtColors.ink950.withValues(alpha: 0.10), offset: const Offset(0, 4), blurRadius: 18, spreadRadius: 0),
      ];

  // Compat aliases — keep hard shadows for places still using brutalist style
  static const List<BoxShadow> hard = [
    BoxShadow(
      color: MmtColors.ink950,
      offset: Offset(8, 8),
      blurRadius: 0,
      spreadRadius: 0,
    ),
  ];

  static const List<BoxShadow> hardSm = [
    BoxShadow(
      color: MmtColors.ink950,
      offset: Offset(4, 4),
      blurRadius: 0,
      spreadRadius: 0,
    ),
  ];

  static const List<BoxShadow> ribbon = [
    BoxShadow(
      color: MmtColors.news600,
      offset: Offset(0, 4),
      blurRadius: 0,
    ),
    BoxShadow(
      color: Color(0x40E31E24),
      offset: Offset(0, 8),
      blurRadius: 24,
    ),
  ];

  // Borders — brutalist compat + new hairline glass 0.8px
  static const double borderWidth = 2.0;
  static const double glassHairline = 0.8;
  static BorderSide get inkBorder => const BorderSide(color: MmtColors.ink950, width: borderWidth);
  static BorderSide get newsBorder => const BorderSide(color: MmtColors.news, width: borderWidth);
  static BorderSide get dividerBorder => const BorderSide(color: MmtColors.divider, width: 1.0);
  static BorderSide get mutedBorder => const BorderSide(color: Color(0xFFE1E1E1), width: borderWidth);

  // Timings
  static const Duration fast = Duration(milliseconds: 120);
  static const Duration baseDur = Duration(milliseconds: 200);
  static const Duration slow = Duration(milliseconds: 500);
}
