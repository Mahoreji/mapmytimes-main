// =============================================================================
// Storage providers — Saved articles, Recently viewed, Search history
// Implementation uses SharedPreferences (already preloaded in main.dart) +
// JSON encoding for portability. Can swap storage layer to Hive without API
// changes because all state access is behind a StateNotifier facade.
// =============================================================================

import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/blog_models.dart';
import 'common_providers.dart';

const _kSaved = 'mmt_saved_ids_v1';
const _kSavedMeta = 'mmt_saved_meta_v1';
const _kRecent = 'mmt_recent_ids_v1';
const _kThemeMode = 'mmt_theme_mode';
const _kFontScale = 'mmt_font_scale';

typedef SavedMetaMap = Map<String, Map<String, String>>;

class SavedArticlesNotifier extends StateNotifier<List<String>> {
  SavedArticlesNotifier(this.ref) : super(const <String>[]) {
    _load();
  }
  final Ref ref;

  SharedPreferences get _prefs => ref.read(sharedPreferencesProvider);

  Future<void> _load() async {
    final raw = _prefs.getStringList(_kSaved);
    if (raw != null) state = List<String>.from(raw);
  }

  bool isSaved(ID id) => state.contains(id);

  Future<void> toggle(ID id, {BlogPostSummaryResponse? meta}) async {
    List<String> next;
    if (state.contains(id)) {
      next = state.where((e) => e != id).toList(growable: false);
    } else {
      next = <String>[id, ...state];
    }
    state = next;
    await _prefs.setStringList(_kSaved, next);
    if (meta != null) {
      await _persistMeta(meta);
    }
  }

  Future<void> remove(ID id) async => toggle(id);

  Future<void> _persistMeta(BlogPostSummaryResponse p) async {
    try {
      final raw = _prefs.getString(_kSavedMeta) ?? '{}';
      final map = SavedMetaMap.from(jsonDecode(raw) as Map);
      map[p.id] = <String, String>{
        't': p.title,
        's': p.slug,
        'c': p.cover,
        'e': p.excerpt ?? '',
        'd': p.publishedAt?.toIso8601String() ?? '',
      };
      await _prefs.setString(_kSavedMeta, jsonEncode(map));
    } catch (_) {}
  }

  List<Map<String, String>> allMeta() {
    try {
      final raw = _prefs.getString(_kSavedMeta) ?? '{}';
      final map = SavedMetaMap.from(jsonDecode(raw) as Map);
      return state
          .map((id) => map[id])
          .whereType<Map<String, String>>()
          .toList(growable: false);
    } catch (_) {
      return const <Map<String, String>>[];
    }
  }
}

final savedArticlesNotifierProvider =
    StateNotifierProvider<SavedArticlesNotifier, List<String>>((ref) {
  return SavedArticlesNotifier(ref);
});

final isArticleSavedProvider = Provider.family<bool, ID>((ref, id) {
  return ref.watch(savedArticlesNotifierProvider).contains(id);
});

// ------------------- Recently viewed -------------------
class RecentlyViewedNotifier extends StateNotifier<List<String>> {
  RecentlyViewedNotifier(this.ref) : super(const <String>[]) {
    _load();
  }
  final Ref ref;

  SharedPreferences get _prefs => ref.read(sharedPreferencesProvider);

  void _load() {
    final raw = _prefs.getStringList(_kRecent);
    if (raw != null) state = List<String>.from(raw);
  }

  Future<void> push(ID id) async {
    final next = <String>[
      id,
      ...state.where((e) => e != id).take(19),
    ];
    state = next;
    await _prefs.setStringList(_kRecent, next);
  }
}

final recentlyViewedProvider =
    StateNotifierProvider<RecentlyViewedNotifier, List<String>>((ref) {
  return RecentlyViewedNotifier(ref);
});

// ------------------- Theme mode (light / dark / system) -------------------
enum MmtThemeMode { light, dark, system }

class ThemeModeNotifier extends StateNotifier<MmtThemeMode> {
  ThemeModeNotifier(this.ref) : super(MmtThemeMode.system) {
    _load();
  }
  final Ref ref;

  SharedPreferences get _prefs => ref.read(sharedPreferencesProvider);

  void _load() {
    final raw = _prefs.getString(_kThemeMode);
    switch (raw) {
      case 'light':
        state = MmtThemeMode.light;
        break;
      case 'dark':
        state = MmtThemeMode.dark;
        break;
      default:
        state = MmtThemeMode.system;
    }
  }

  Future<void> set(MmtThemeMode m) async {
    state = m;
    await _prefs.setString(_kThemeMode, m.name);
  }

  ThemeMode get flutterMode {
    switch (state) {
      case MmtThemeMode.light:
        return ThemeMode.light;
      case MmtThemeMode.dark:
        return ThemeMode.dark;
      case MmtThemeMode.system:
        return ThemeMode.system;
    }
  }
}

final themeModeNotifierProvider =
    StateNotifierProvider<ThemeModeNotifier, MmtThemeMode>((ref) {
  return ThemeModeNotifier(ref);
});

// ------------------- Font scale (reader-view accessibility) -------------------
class FontScaleNotifier extends StateNotifier<double> {
  FontScaleNotifier(this.ref) : super(1.0) {
    _load();
  }
  final Ref ref;

  SharedPreferences get _prefs => ref.read(sharedPreferencesProvider);

  void _load() {
    final raw = _prefs.getDouble(_kFontScale);
    if (raw != null) state = raw;
  }

  Future<void> set(double v) async {
    final clamped = v.clamp(0.85, 1.5);
    state = clamped;
    await _prefs.setDouble(_kFontScale, clamped);
  }

  Future<void> stepUp() async => set(state + 0.1);
  Future<void> stepDown() async => set(state - 0.1);
  Future<void> reset() async => set(1.0);
}

final fontScaleNotifierProvider =
    StateNotifierProvider<FontScaleNotifier, double>((ref) {
  return FontScaleNotifier(ref);
});
