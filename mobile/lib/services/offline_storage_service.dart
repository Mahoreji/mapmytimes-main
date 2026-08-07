import 'dart:async';
import 'dart:io';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:path_provider/path_provider.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:http/http.dart' as http;
import '../core/api/reader_api.dart';

const int kOfflineArticleCap = 50;
const String _boxArticles = 'offline_articles';
const String _boxPendingHighlights = 'pending_highlights';

class CachedArticle {
  final String postId;
  final String title;
  final String cover;
  final String? coverLocalPath;
  final String content;
  final String contentHtml;
  final int readingTimeMinutes;
  final String authorName;
  final String categoriesJson;
  final int savedAt;
  final int lastAccessedAt;

  const CachedArticle({
    required this.postId,
    required this.title,
    required this.cover,
    this.coverLocalPath,
    required this.content,
    required this.contentHtml,
    required this.readingTimeMinutes,
    required this.authorName,
    required this.categoriesJson,
    required this.savedAt,
    required this.lastAccessedAt,
  });

  CachedArticle copyWith({
    String? postId,
    String? title,
    String? cover,
    String? coverLocalPath,
    String? content,
    String? contentHtml,
    int? readingTimeMinutes,
    String? authorName,
    String? categoriesJson,
    int? savedAt,
    int? lastAccessedAt,
  }) {
    return CachedArticle(
      postId: postId ?? this.postId,
      title: title ?? this.title,
      cover: cover ?? this.cover,
      coverLocalPath: coverLocalPath ?? this.coverLocalPath,
      content: content ?? this.content,
      contentHtml: contentHtml ?? this.contentHtml,
      readingTimeMinutes: readingTimeMinutes ?? this.readingTimeMinutes,
      authorName: authorName ?? this.authorName,
      categoriesJson: categoriesJson ?? this.categoriesJson,
      savedAt: savedAt ?? this.savedAt,
      lastAccessedAt: lastAccessedAt ?? this.lastAccessedAt,
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'postId': postId,
        'title': title,
        'cover': cover,
        'coverLocalPath': coverLocalPath,
        'content': content,
        'contentHtml': contentHtml,
        'readingTimeMinutes': readingTimeMinutes,
        'authorName': authorName,
        'categoriesJson': categoriesJson,
        'savedAt': savedAt,
        'lastAccessedAt': lastAccessedAt,
      };

  factory CachedArticle.fromJson(Map<String, dynamic> j) => CachedArticle(
        postId: (j['postId'] ?? j['post_id'] ?? '').toString(),
        title: (j['title'] ?? '').toString(),
        cover: (j['cover'] ?? '').toString(),
        coverLocalPath: j['coverLocalPath'] as String?,
        content: (j['content'] ?? '').toString(),
        contentHtml: (j['contentHtml'] ?? j['content_html'] ?? '').toString(),
        readingTimeMinutes: (j['readingTimeMinutes'] as num?)?.toInt() ?? (j['reading_time_minutes'] as num?)?.toInt() ?? 5,
        authorName: (j['authorName'] ?? j['author_name'] ?? 'MapMyTimes').toString(),
        categoriesJson: (j['categoriesJson'] ?? j['categories_json'] ?? '[]').toString(),
        savedAt: (j['savedAt'] as num?)?.toInt() ?? (j['saved_at'] as num?)?.toInt() ?? 0,
        lastAccessedAt: (j['lastAccessedAt'] as num?)?.toInt() ?? (j['last_accessed_at'] as num?)?.toInt() ?? 0,
      );
}

class PendingHighlight {
  final String id;
  final String postId;
  final int paragraphIndex;
  final int charStart;
  final int charEnd;
  final String excerpt;
  final int createdAt;

  const PendingHighlight({
    required this.id,
    required this.postId,
    required this.paragraphIndex,
    required this.charStart,
    required this.charEnd,
    required this.excerpt,
    required this.createdAt,
  });

  Map<String, dynamic> toJson() => <String, dynamic>{
        'id': id,
        'postId': postId,
        'paragraphIndex': paragraphIndex,
        'charStart': charStart,
        'charEnd': charEnd,
        'excerpt': excerpt,
        'createdAt': createdAt,
      };

  factory PendingHighlight.fromJson(Map<String, dynamic> j) => PendingHighlight(
        id: (j['id'] ?? '').toString(),
        postId: (j['postId'] ?? j['post_id'] ?? '').toString(),
        paragraphIndex: (j['paragraphIndex'] as num?)?.toInt() ?? (j['paragraph_index'] as num?)?.toInt() ?? 0,
        charStart: (j['charStart'] as num?)?.toInt() ?? (j['char_start'] as num?)?.toInt() ?? 0,
        charEnd: (j['charEnd'] as num?)?.toInt() ?? (j['char_end'] as num?)?.toInt() ?? 0,
        excerpt: (j['excerpt'] ?? '').toString(),
        createdAt: (j['createdAt'] as num?)?.toInt() ?? (j['created_at'] as num?)?.toInt() ?? 0,
      );
}

class OfflineStorageService {
  OfflineStorageService._();
  static final OfflineStorageService instance = OfflineStorageService._();

  bool _initialized = false;
  Box<String>? _articlesBox;
  Box<String>? _pendingBox;
  final Connectivity _connectivity = Connectivity();
  StreamSubscription<List<ConnectivityResult>>? _connSub;
  bool _draining = false;

  Future<void> init() async {
    if (_initialized) return;
    if (!kIsWeb) {
      await Hive.initFlutter();
    }
    _articlesBox = await Hive.openBox<String>(_boxArticles);
    _pendingBox = await Hive.openBox<String>(_boxPendingHighlights);
    _initialized = true;
    _connSub = _connectivity.onConnectivityChanged.listen((results) {
      if (results.any((r) => r != ConnectivityResult.none)) {
        unawaited(drainPendingHighlights());
      }
    });
    unawaited(drainPendingHighlights());
  }

  Future<String> _localCoverDir() async {
    if (kIsWeb) return '';
    final dir = await getApplicationDocumentsDirectory();
    final coversDir = Directory('${dir.path}/offline_covers');
    if (!await coversDir.exists()) {
      await coversDir.create(recursive: true);
    }
    return coversDir.path;
  }

  Future<String?> _downloadCover(String remoteUrl) async {
    if (kIsWeb || remoteUrl.isEmpty) return null;
    try {
      final uri = Uri.tryParse(remoteUrl);
      if (uri == null) return null;
      final resp = await http.get(uri).timeout(const Duration(seconds: 15));
      if (resp.statusCode != 200) return null;
      final bytes = resp.bodyBytes;
      if (bytes.isEmpty) return null;
      final dir = await _localCoverDir();
      final safeName = base64Url.encode(utf8.encode(remoteUrl)).replaceAll('/', '_').replaceAll('=', '');
      final ext = _guessExt(remoteUrl);
      final file = File('$dir/$safeName$ext');
      await file.writeAsBytes(bytes);
      return file.path;
    } catch (_) {
      return null;
    }
  }

  String _guessExt(String url) {
    final u = url.toLowerCase();
    if (u.contains('.png')) return '.png';
    if (u.contains('.webp')) return '.webp';
    if (u.contains('.gif')) return '.gif';
    return '.jpg';
  }

  int get articleCount {
    final b = _articlesBox;
    return b == null ? 0 : b.length;
  }

  int get cap => kOfflineArticleCap;

  bool isCached(String postId) {
    final b = _articlesBox;
    return b != null && b.containsKey(postId);
  }

  CachedArticle? getArticle(String postId) {
    final b = _articlesBox;
    if (b == null) return null;
    final raw = b.get(postId);
    if (raw == null) return null;
    try {
      final map = jsonDecode(raw) as Map<String, dynamic>;
      final art = CachedArticle.fromJson(map);
      unawaited(_touch(postId, art));
      return art;
    } catch (_) {
      return null;
    }
  }

  Future<void> _touch(String postId, CachedArticle current) async {
    final b = _articlesBox;
    if (b == null) return;
    final updated = current.copyWith(lastAccessedAt: DateTime.now().millisecondsSinceEpoch);
    await b.put(postId, jsonEncode(updated.toJson()));
  }

  List<CachedArticle> listArticles() {
    final b = _articlesBox;
    if (b == null) return const [];
    final out = <CachedArticle>[];
    for (final k in b.keys) {
      final raw = b.get(k);
      if (raw == null) continue;
      try {
        out.add(CachedArticle.fromJson(jsonDecode(raw) as Map<String, dynamic>));
      } catch (_) {}
    }
    out.sort((a, b) => b.savedAt.compareTo(a.savedAt));
    return out;
  }

  Future<CachedArticle?> saveArticle({
    required String postId,
    required String title,
    required String cover,
    required String content,
    required String contentHtml,
    required int readingTimeMinutes,
    required String authorName,
    required List<Map<String, dynamic>> categories,
  }) async {
    final b = _articlesBox;
    if (b == null) return null;
    await _evictIfNeeded();
    final coverLocal = cover.isNotEmpty ? await _downloadCover(cover) : null;
    final now = DateTime.now().millisecondsSinceEpoch;
    final art = CachedArticle(
      postId: postId,
      title: title,
      cover: cover,
      coverLocalPath: coverLocal,
      content: content,
      contentHtml: contentHtml,
      readingTimeMinutes: readingTimeMinutes,
      authorName: authorName,
      categoriesJson: jsonEncode(categories),
      savedAt: now,
      lastAccessedAt: now,
    );
    await b.put(postId, jsonEncode(art.toJson()));
    return art;
  }

  Future<void> _evictIfNeeded() async {
    final b = _articlesBox;
    if (b == null) return;
    while (b.length >= kOfflineArticleCap) {
      final list = listArticles();
      if (list.isEmpty) break;
      list.sort((a, b) => a.lastAccessedAt.compareTo(b.lastAccessedAt));
      final victim = list.first;
      await removeArticle(victim.postId);
    }
  }

  Future<void> removeArticle(String postId) async {
    final b = _articlesBox;
    if (b == null) return;
    final raw = b.get(postId);
    if (raw != null && !kIsWeb) {
      try {
        final map = jsonDecode(raw) as Map<String, dynamic>;
        final art = CachedArticle.fromJson(map);
        if (art.coverLocalPath != null) {
          final f = File(art.coverLocalPath!);
          if (await f.exists()) {
            try {
              await f.delete();
            } catch (_) {}
          }
        }
      } catch (_) {}
    }
    await b.delete(postId);
  }

  Future<void> clearAllArticles() async {
    final b = _articlesBox;
    if (b == null) return;
    if (!kIsWeb) {
      for (final k in List<dynamic>.from(b.keys)) {
        try {
          final raw = b.get(k.toString());
          if (raw != null) {
            final map = jsonDecode(raw) as Map<String, dynamic>;
            final art = CachedArticle.fromJson(map);
            if (art.coverLocalPath != null) {
              final f = File(art.coverLocalPath!);
              if (await f.exists()) {
                try {
                  await f.delete();
                } catch (_) {}
              }
            }
          }
        } catch (_) {}
      }
    }
    await b.clear();
  }

  List<PendingHighlight> listPendingHighlights() {
    final b = _pendingBox;
    if (b == null) return const [];
    final out = <PendingHighlight>[];
    for (final k in b.keys) {
      final raw = b.get(k);
      if (raw == null) continue;
      try {
        out.add(PendingHighlight.fromJson(jsonDecode(raw) as Map<String, dynamic>));
      } catch (_) {}
    }
    out.sort((a, b) => a.createdAt.compareTo(b.createdAt));
    return out;
  }

  Future<void> enqueuePendingHighlight({
    required String postId,
    required int paragraphIndex,
    required int charStart,
    required int charEnd,
    required String excerpt,
  }) async {
    final b = _pendingBox;
    if (b == null) return;
    final id = 'ph_${DateTime.now().millisecondsSinceEpoch}_${paragraphIndex}_$charStart';
    final ph = PendingHighlight(
      id: id,
      postId: postId,
      paragraphIndex: paragraphIndex,
      charStart: charStart,
      charEnd: charEnd,
      excerpt: excerpt.length > 200 ? excerpt.substring(0, 200) : excerpt,
      createdAt: DateTime.now().millisecondsSinceEpoch,
    );
    await b.put(id, jsonEncode(ph.toJson()));
  }

  Future<void> drainPendingHighlights() async {
    final b = _pendingBox;
    if (b == null || _draining) return;
    final results = await _connectivity.checkConnectivity();
    if (results.every((r) => r == ConnectivityResult.none)) return;
    _draining = true;
    try {
      final list = listPendingHighlights();
      for (final ph in list) {
        try {
          final ok = await ReaderApi.instance.createHighlight(
            postId: ph.postId,
            paragraphIndex: ph.paragraphIndex,
            charStart: ph.charStart,
            charEnd: ph.charEnd,
            excerpt: ph.excerpt,
          );
          if (ok != null) {
            await b.delete(ph.id);
          }
        } catch (_) {}
      }
    } finally {
      _draining = false;
    }
  }

  Future<void> clearPendingHighlights() async {
    await _pendingBox?.clear();
  }

  void dispose() {
    _connSub?.cancel();
  }
}
