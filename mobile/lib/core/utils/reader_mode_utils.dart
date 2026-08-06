// Reader Mode Shared Utilities (P0-3 + Phase 1)
// Replicates BACKEND BlogPostServiceImpl.stripHtmlAndMarkdown() EXACT regex chain
// for parity between backend readingTime calc and mobile auto-suggest threshold.

import 'dart:math';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';

// ===========================================================================
// P0-3: Dual HTML + Markdown stripping (1:1 parity with backend Java regex)
// ===========================================================================

final RegExp _htmlTagRe = RegExp(r'<[^>]+>', multiLine: true);
final RegExp _mdLinkRe = RegExp(r'!?\[([^\]]*)\]\([^)]*\)');
final RegExp _mdHeadingRe = RegExp(r'^#{1,6}\s+', multiLine: true);
final RegExp _mdFormatRe = RegExp(r'(\*{1,3}|_{1,3}|`{1,3}|~~|> |\| |- )');
final RegExp _whitespaceRe = RegExp(r'\s+');

String stripHtmlAndMarkdown(String? raw) {
  if (raw == null) return '';
  String s = raw;
  s = s
      .replaceAll('&nbsp;', ' ')
      .replaceAll('&amp;', '&')
      .replaceAll('&quot;', '"')
      .replaceAll('&#39;', "'")
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>');
  s = s.replaceAllMapped(_mdLinkRe, (m) {
    final full = m.group(0) ?? '';
    if (full.startsWith('!')) return '';
    return ' ${m.group(1) ?? ''} ';
  });
  s = s.replaceAll(_htmlTagRe, ' ');
  s = s.replaceAll(_mdHeadingRe, '');
  s = s.replaceAll(_mdFormatRe, ' ');
  s = s.replaceAll(_whitespaceRe, ' ').trim();
  return s;
}

const int kWordsPerMinute = 200;
const int kAutoSuggestWordThreshold = 800;

int computeStrippedWordCount(String? content) {
  final stripped = stripHtmlAndMarkdown(content);
  if (stripped.isEmpty) return 0;
  return stripped.split(_whitespaceRe).where((w) => w.isNotEmpty).length;
}

int computeReadingTimeMinutes(String? content) {
  final words = computeStrippedWordCount(content);
  if (words <= 0) return 1;
  return max(1, (words / kWordsPerMinute).ceil());
}

// ===========================================================================
// Phase 1 Font Stacks — Sans + Serif with Hindi Devanagari fallbacks
// ===========================================================================

enum ReaderFontStack { sans, serif }

class ReaderFontSpec {
  final String displayName;
  final TextStyle Function({double fontSize, FontWeight fontWeight, double height}) builder;
  const ReaderFontSpec(this.displayName, this.builder);
}

final Map<ReaderFontStack, ReaderFontSpec> kReaderFonts = {
  ReaderFontStack.sans: ReaderFontSpec(
    'Sans (Default)',
    ({fontSize = 16, fontWeight = FontWeight.w400, height = 1.5}) {
      final base = GoogleFonts.inter(
        fontSize: fontSize,
        fontWeight: fontWeight,
        height: height,
      );
      return base.copyWith(
        fontFamily: base.fontFamily,
        fontFamilyFallback: [GoogleFonts.notoSansDevanagari().fontFamily!],
      );
    },
  ),
  ReaderFontStack.serif: ReaderFontSpec(
    'Serif (Long-form)',
    ({fontSize = 16, fontWeight = FontWeight.w400, height = 1.5}) {
      final base = GoogleFonts.notoSerif(
        fontSize: fontSize,
        fontWeight: fontWeight,
        height: height,
      );
      return base.copyWith(
        fontFamily: base.fontFamily,
        fontFamilyFallback: [GoogleFonts.notoSerifDevanagari().fontFamily!],
      );
    },
  ),
};

// ===========================================================================
// Phase 1 Typography Steps
// ===========================================================================

/// 5-step font sizes (logical px for base body copy)
const List<double> kReaderFontSizeSteps = [13.0, 15.0, 17.0, 19.0, 22.0];
const int kReaderFontSizeDefaultIdx = 2; // 17.0

/// Line spacing -> TextStyle.height multiplier
enum ReaderLineSpacing { compact, normal, relaxed }

const Map<ReaderLineSpacing, double> kReaderLineHeight = {
  ReaderLineSpacing.compact: 1.25,
  ReaderLineSpacing.normal: 1.50,
  ReaderLineSpacing.relaxed: 1.80,
};

const Map<ReaderLineSpacing, String> kReaderLineSpacingLabels = {
  ReaderLineSpacing.compact: 'Compact',
  ReaderLineSpacing.normal: 'Normal',
  ReaderLineSpacing.relaxed: 'Relaxed',
};

// ===========================================================================
// Phase 1 Theme (reading canvas)
// ===========================================================================

enum ReaderTheme { light, dark, sepia }

class ReaderThemeSpec {
  final String label;
  final Color bg;
  final Color fg;
  final Color chromeRed;
  final Color chromeBorder;
  const ReaderThemeSpec(this.label, this.bg, this.fg, this.chromeRed, this.chromeBorder);
}

const Color kMmtNewsRed = Color(0xFFE31E24);
const Color kMmtInk950 = Color(0xFF0A0A0A);

const Map<ReaderTheme, ReaderThemeSpec> kReaderThemes = {
  ReaderTheme.light: ReaderThemeSpec(
    'Light',
    Colors.white,
    kMmtInk950,
    kMmtNewsRed,
    kMmtInk950,
  ),
  ReaderTheme.dark: ReaderThemeSpec(
    'Dark',
    kMmtInk950,
    Colors.white,
    kMmtNewsRed,
    Colors.white,
  ),
  ReaderTheme.sepia: ReaderThemeSpec(
    'Sepia',
    Color(0xFFF4ECD8),
    Color(0xFF5B4636),
    kMmtNewsRed,
    Color(0xFF5B4636),
  ),
};

// ===========================================================================
// Phase 1 Persistence (local device — shared_prefs, account sync = Phase 2)
// ===========================================================================

const _kPrefPrefix = 'mmt:reader:';
const _kFontSizeIdx = '${_kPrefPrefix}fontSizeIdx';
const _kFontStack = '${_kPrefPrefix}fontStack';
const _kLineSpacing = '${_kPrefPrefix}lineSpacing';
const _kTheme = '${_kPrefPrefix}theme';
const _kDismissedAutoSuggest = '${_kPrefPrefix}dismissedSuggest:';

class ReaderPrefs {
  final int fontSizeIdx;
  final ReaderFontStack fontStack;
  final ReaderLineSpacing lineSpacing;
  final ReaderTheme theme;
  const ReaderPrefs({
    required this.fontSizeIdx,
    required this.fontStack,
    required this.lineSpacing,
    required this.theme,
  });

  static const ReaderPrefs defaults = ReaderPrefs(
    fontSizeIdx: kReaderFontSizeDefaultIdx,
    fontStack: ReaderFontStack.sans,
    lineSpacing: ReaderLineSpacing.normal,
    theme: ReaderTheme.light,
  );

  static Future<ReaderPrefs> load() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return ReaderPrefs(
        fontSizeIdx:
            prefs.getInt(_kFontSizeIdx) ?? defaults.fontSizeIdx,
        fontStack: ReaderFontStack.values[
            prefs.getInt(_kFontStack) ?? defaults.fontStack.index],
        lineSpacing: ReaderLineSpacing.values[
            prefs.getInt(_kLineSpacing) ?? defaults.lineSpacing.index],
        theme:
            ReaderTheme.values[prefs.getInt(_kTheme) ?? defaults.theme.index],
      );
    } catch (_) {
      return defaults;
    }
  }

  Future<void> save() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt(_kFontSizeIdx, fontSizeIdx);
      await prefs.setInt(_kFontStack, fontStack.index);
      await prefs.setInt(_kLineSpacing, lineSpacing.index);
      await prefs.setInt(_kTheme, theme.index);
    } catch (_) {}
  }

  ReaderPrefs copyWith({
    int? fontSizeIdx,
    ReaderFontStack? fontStack,
    ReaderLineSpacing? lineSpacing,
    ReaderTheme? theme,
  }) {
    return ReaderPrefs(
      fontSizeIdx: fontSizeIdx ?? this.fontSizeIdx,
      fontStack: fontStack ?? this.fontStack,
      lineSpacing: lineSpacing ?? this.lineSpacing,
      theme: theme ?? this.theme,
    );
  }

  // ---------- Auto-suggest dismiss state ----------
  static Future<bool> isAutoSuggestDismissed(String postId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getBool('$_kDismissedAutoSuggest$postId') ?? false;
    } catch (_) {
      return false;
    }
  }

  static Future<void> markAutoSuggestDismissed(String postId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('$_kDismissedAutoSuggest$postId', true);
    } catch (_) {}
  }
}
