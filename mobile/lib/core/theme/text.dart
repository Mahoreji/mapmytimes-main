import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'colors.dart';

/// Text style constructors — version WITHOUT BuildContext (pure brightness-based)
/// so we can safely build ThemeData during startup when no BuildContext exists.
class MmtText {
  MmtText._();

  // ---------------- Headlines (Archivo Black) ----------------
  static TextStyle headlineDisplay({required Brightness mode, double? size = 48}) {
    return GoogleFonts.getFont(
      'Archivo Black',
      fontSize: size,
      height: 0.98,
      letterSpacing: size! > 36 ? -1.0 : -0.3,
      fontWeight: FontWeight.w900,
      color: _fg(mode),
    );
  }

  static TextStyle h1({required Brightness mode}) => GoogleFonts.getFont(
        'Archivo Black',
        fontSize: 28,
        height: 1.08,
        letterSpacing: -0.4,
        fontWeight: FontWeight.w900,
        color: _fg(mode),
      );

  static TextStyle h2({required Brightness mode}) => GoogleFonts.getFont(
        'Archivo Black',
        fontSize: 22,
        height: 1.1,
        letterSpacing: -0.2,
        fontWeight: FontWeight.w900,
        color: _fg(mode),
      );

  static TextStyle h3({required Brightness mode}) => GoogleFonts.getFont(
        'Archivo Black',
        fontSize: 18,
        height: 1.18,
        fontWeight: FontWeight.w900,
        color: _fg(mode),
      );

  static TextStyle h4({required Brightness mode}) => GoogleFonts.getFont(
        'Archivo Black',
        fontSize: 15,
        height: 1.25,
        letterSpacing: 0.1,
        fontWeight: FontWeight.w900,
        color: _fg(mode),
      );

  static TextStyle eyebrow({required Brightness mode, Color? color}) =>
      GoogleFonts.getFont(
        'Archivo Black',
        fontSize: 11,
        height: 1.0,
        letterSpacing: 1.8,
        fontWeight: FontWeight.w900,
        color: color ?? (mode == Brightness.dark ? Colors.white70 : MmtColors.textMuted),
      );

  // ---------------- Body / Inter ----------------
  static TextStyle bodyLg({required Brightness mode, Color? color, FontWeight? weight}) =>
      GoogleFonts.inter(
        fontSize: 17,
        height: 1.55,
        fontWeight: weight ?? FontWeight.w400,
        color: color ?? _fg(mode),
      );

  static TextStyle body({required Brightness mode, Color? color, FontWeight? weight}) =>
      GoogleFonts.inter(
        fontSize: 15,
        height: 1.55,
        fontWeight: weight ?? FontWeight.w400,
        color: color ?? _fg(mode),
      );

  static TextStyle bodySm({required Brightness mode, Color? color, FontWeight? weight}) =>
      GoogleFonts.inter(
        fontSize: 13,
        height: 1.5,
        fontWeight: weight ?? FontWeight.w400,
        color: color ?? _fgMuted(mode),
      );

  static TextStyle caption({required Brightness mode, Color? color, FontWeight? weight}) =>
      GoogleFonts.inter(
        fontSize: 12,
        height: 1.3,
        fontWeight: weight ?? FontWeight.w500,
        color: color ?? _fgMuted(mode),
      );

  static TextStyle overline({required Brightness mode, Color? color}) => GoogleFonts.inter(
        fontSize: 11,
        height: 1.0,
        letterSpacing: 1.3,
        fontWeight: FontWeight.w700,
        color: color ?? MmtColors.news,
      );

  static TextStyle button({required Brightness mode}) => GoogleFonts.inter(
        fontSize: 14,
        height: 1.0,
        letterSpacing: 0.5,
        fontWeight: FontWeight.w700,
        color: Colors.white,
      );

  static Color _fg(Brightness m) =>
      m == Brightness.dark ? Colors.white : MmtColors.textBody;
  static Color _fgMuted(Brightness m) =>
      m == Brightness.dark ? Colors.white60 : MmtColors.textMuted;
}
