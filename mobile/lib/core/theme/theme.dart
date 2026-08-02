import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:google_fonts/google_fonts.dart';
import 'colors.dart';
import 'text.dart';

/// Complete Material 3 Neo-brutalist theme for MapMyTimes mobile app.
/// Matches 1:1 frontend tailwind tokens.
class MmtTheme {
  MmtTheme._();

  static ThemeData light() => build(Brightness.light);
  static ThemeData dark() => build(Brightness.dark);

  static ThemeData build(Brightness mode) {
    final isDark = mode == Brightness.dark;
    final bg = isDark ? MmtColors.ink950 : MmtColors.surfaceLight;
    final fg = isDark ? Colors.white : MmtColors.textBody;
    final surface = isDark ? MmtColors.ink900 : MmtColors.surface;

    return ThemeData(
      useMaterial3: true,
      brightness: mode,
      colorScheme: ColorScheme.fromSeed(
        seedColor: MmtColors.news,
        brightness: mode,
        primary: MmtColors.news,
        onPrimary: Colors.white,
        secondary: MmtColors.ink950,
        onSecondary: Colors.white,
        surface: surface,
        onSurface: fg,
        error: MmtColors.danger,
      ),
      primaryColor: MmtColors.news,
      scaffoldBackgroundColor: bg,
      dividerColor: isDark ? MmtColors.ink700 : MmtColors.divider,
      textTheme: TextTheme(
        displayLarge: MmtText.headlineDisplay(mode: mode),
        displayMedium: MmtText.headlineDisplay(mode: mode, size: 36),
        headlineLarge: MmtText.h1(mode: mode),
        headlineMedium: MmtText.h2(mode: mode),
        headlineSmall: MmtText.h3(mode: mode),
        titleLarge: MmtText.h3(mode: mode),
        titleMedium: MmtText.h4(mode: mode),
        titleSmall: MmtText.eyebrow(mode: mode),
        bodyLarge: MmtText.bodyLg(mode: mode),
        bodyMedium: MmtText.body(mode: mode),
        bodySmall: MmtText.bodySm(mode: mode),
        labelLarge: MmtText.button(mode: mode),
        labelMedium: MmtText.caption(mode: mode),
        labelSmall: MmtText.overline(mode: mode),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: bg,
        foregroundColor: fg,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: GoogleFonts.getFont(
          'Archivo Black',
          fontSize: 22,
          fontWeight: FontWeight.w900,
          letterSpacing: -0.2,
          color: fg,
        ),
        shape: const Border(
          bottom: BorderSide(color: MmtColors.ink950, width: 2),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: MmtColors.news,
          foregroundColor: Colors.white,
          disabledBackgroundColor: MmtColors.news300,
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.zero,
            side: BorderSide(color: MmtColors.ink950, width: 2),
          ),
          elevation: 0,
          minimumSize: const Size(64, 52),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          textStyle: MmtText.button(mode: mode),
          shadowColor: Colors.transparent,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          backgroundColor: surface,
          foregroundColor: MmtColors.ink950,
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.zero,
          ),
          side: const BorderSide(color: MmtColors.ink950, width: 2),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          textStyle: MmtText.button(mode: mode).copyWith(color: MmtColors.ink950),
          elevation: 0,
          minimumSize: const Size(64, 52),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: MmtColors.news,
          textStyle: GoogleFonts.inter(
            fontSize: 14,
            fontWeight: FontWeight.w700,
            letterSpacing: 0.4,
          ),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.zero,
          ),
        ),
      ),
      cardTheme: CardThemeData(
        color: surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.zero,
          side: BorderSide(color: MmtColors.ink950, width: 2),
        ),
        margin: EdgeInsets.zero,
        clipBehavior: Clip.hardEdge,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: MmtColors.chipBg,
        selectedColor: MmtColors.news,
        disabledColor: MmtColors.chipBg,
        labelStyle: GoogleFonts.inter(
          fontSize: 12,
          fontWeight: FontWeight.w700,
          letterSpacing: 0.5,
          color: MmtColors.chipText,
        ),
        side: const BorderSide(color: MmtColors.ink950, width: 2),
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.zero,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: surface,
        isDense: true,
        contentPadding: const EdgeInsets.fromLTRB(14, 16, 14, 16),
        hintStyle: MmtText.bodySm(mode: mode, color: MmtColors.textFaint),
        labelStyle: MmtText.caption(mode: mode),
        errorStyle: MmtText.caption(mode: mode, color: MmtColors.danger, weight: FontWeight.w700),
        border: _inputBorder(MmtColors.ink950),
        enabledBorder: _inputBorder(MmtColors.ink950),
        focusedBorder: _inputBorder(MmtColors.news, 3),
        errorBorder: _inputBorder(MmtColors.danger),
        focusedErrorBorder: _inputBorder(MmtColors.danger, 3),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: surface,
        selectedItemColor: MmtColors.news,
        unselectedItemColor: isDark ? Colors.white54 : MmtColors.textMuted,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
        selectedLabelStyle: MmtText.caption(mode: mode, color: MmtColors.news, weight: FontWeight.w700),
        unselectedLabelStyle: MmtText.caption(mode: mode),
      ),
      listTileTheme: ListTileThemeData(
        tileColor: surface,
        shape: const Border(
          bottom: BorderSide(color: MmtColors.divider, width: 1),
        ),
        iconColor: fg,
        textColor: fg,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      ),
      dividerTheme: const DividerThemeData(
        color: MmtColors.divider,
        thickness: 1,
        space: 0,
      ),
      iconTheme: IconThemeData(color: fg, size: 22),
      pageTransitionsTheme: const PageTransitionsTheme(builders: {
        TargetPlatform.android: ZoomPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
      },),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: MmtColors.ink950,
        contentTextStyle: MmtText.body(mode: Brightness.light, color: Colors.white),
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.zero,
          side: BorderSide(color: MmtColors.news, width: 2),
        ),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  static InputBorder _inputBorder(Color c, [double width = 2]) =>
      OutlineInputBorder(
        borderRadius: BorderRadius.zero,
        borderSide: BorderSide(color: c, width: width),
        gapPadding: 0,
      );
}
