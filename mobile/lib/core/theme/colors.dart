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

  // Border radii — news platform prefers sharp squares with small rounding
  static const double radiusXs = 2.0;
  static const double radiusSm = 4.0;
  static const double radiusMd = 8.0;
  static const double radiusLg = 12.0;
  static const double radiusXl = 16.0;
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

  // Shadows — hard neobrutal — always dark & bold news drop
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

  // Borders — neobrutal 2px solid everywhere
  static const double borderWidth = 2.0;
  static BorderSide get inkBorder => const BorderSide(color: MmtColors.ink950, width: borderWidth);
  static BorderSide get newsBorder => const BorderSide(color: MmtColors.news, width: borderWidth);
  static BorderSide get dividerBorder => const BorderSide(color: MmtColors.divider, width: 1.0);
  static BorderSide get mutedBorder => const BorderSide(color: Color(0xFFE1E1E1), width: borderWidth);

  // Timings
  static const Duration fast = Duration(milliseconds: 120);
  static const Duration baseDur = Duration(milliseconds: 200);
  static const Duration slow = Duration(milliseconds: 500);
}
