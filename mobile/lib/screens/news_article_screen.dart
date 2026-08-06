// ---------------- NEWS ARTICLE SCREEN ----------------
// INTEGRATED: postBySlugProvider + increment view + save + TTS listen + related + READER MODE
import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:share_plus/share_plus.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import '../core/env.dart';
import '../core/tts_service.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../core/widgets/brand.dart';
import '../core/api/reader_api.dart';
import '../core/utils/reader_mode_utils.dart' as rmu;
import '../models/blog_models.dart';
import '../providers/index.dart';
import '../widgets/editorial_components.dart';
import '../services/offline_storage_service.dart';

// ---------------------------------------------------------------------------
// Local slug helper (used for heading ValueKey + TOC anchor match, fallback if imported one not in scope)
// ---------------------------------------------------------------------------
String _slug(String s) {
  if (s.trim().isEmpty) return 'empty-${Object().hashCode.abs()}';
  return s.toLowerCase().trim()
    .replaceAll(RegExp(r'[^a-z0-9\s-]'), '')
    .replaceAll(RegExp(r'\s+'), '-')
    .replaceAll(RegExp(r'-{2,}'), '-')
    .replaceAll(RegExp(r'^-|-$'), '');
}


class NewsArticleScreen extends ConsumerStatefulWidget {
  const NewsArticleScreen({
    super.key,
    required this.slug,
    this.postId,
    this.resumePercent,
  });

  final String slug;
  final String? postId;
  final int? resumePercent;

  @override
  ConsumerState<NewsArticleScreen> createState() => _NewsArticleScreenState();
}

class _NewsArticleScreenState extends ConsumerState<NewsArticleScreen> {
  bool _viewCounted = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_viewCounted) {
      _viewCounted = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        try {
          final pid = widget.postId;
          if (pid != null && pid.isNotEmpty) {
            ref.read(postViewIncrementProvider)(pid);
          }
        } catch (_) {}
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Dict.of(context);
    final AsyncValue<BlogPostResponse?> postAsync;
    final invalidate = () {
      ref.invalidate(postBySlugProvider(widget.slug));
      if (widget.postId != null) ref.invalidate(postByIdProvider(widget.postId!));
    };
    if (widget.postId != null && widget.postId!.isNotEmpty) {
      postAsync = ref.watch(postByIdProvider(widget.postId!));
    } else {
      postAsync = ref.watch(postBySlugProvider(widget.slug));
    }

    return Scaffold(
      backgroundColor: MmtColors.background,
      body: postAsync.when(
        loading: () => const _ArticleSkeleton(),
        error: (e, _) => _ArticleError(
          msg: e.toString(),
          retry: invalidate,
        ),
        data: (p) {
          if (p == null) {
            return _ArticleError(
              msg: 'No article found',
              retry: invalidate,
            );
          }
          if (!_viewCounted) {
            _viewCounted = true;
            try {
              ref.read(postViewIncrementProvider)(p.id);
              ref.read(recentlyViewedProvider.notifier).push(p.id);
            } catch (_) {}
          }
          return _ArticleBody(
            post: p,
            slug: widget.slug,
            postId: widget.postId ?? p.id,
            t: t,
            resumePercent: widget.resumePercent,
          );
        },
      ),
    );
  }
}

class _ArticleBody extends ConsumerStatefulWidget {
  const _ArticleBody({
    required this.post,
    required this.slug,
    required this.postId,
    required this.t,
    this.resumePercent,
  });

  final BlogPostResponse post;
  final String slug;
  final String postId;
  final Dict t;
  final int? resumePercent;

  @override
  ConsumerState<_ArticleBody> createState() => _ArticleBodyState();
}

class _ArticleBodyState extends ConsumerState<_ArticleBody> with WidgetsBindingObserver {
  late final TtsService _tts;
  bool _ttsPlaying = false;
  bool _ttsReady = false;
  int _ttsSpeedIdx = 1;

  // =========================================================================
  // READER MODE STATE — Phase 1
  // =========================================================================
  bool _isReaderMode = false;
  rmu.ReaderPrefs _readerPrefs = rmu.ReaderPrefs.defaults;
  double _scrollProgress = 0.0;
  bool _showAutoSuggest = false;
  final ScrollController _scrollCtrl = ScrollController();

  Timer? _progressDebounce;
  int _lastSentProgress = -1;
  bool _resumeBannerVisible = false;
  int? _resumePercent;
  final ReaderApi _readerApi = ReaderApi.instance;

  // =========================================================================
  // HIGHLIGHTS STATE
  // =========================================================================
  List<Map<String, dynamic>> _highlights = const <Map<String, dynamic>>[];
  bool _highlightsLoaded = false;
  bool _isSavingHighlight = false;

  // =========================================================================
  // OFFLINE STATE
  // =========================================================================
  bool _isCached = false;
  bool _isSavingOffline = false;
  CachedArticle? _cachedOverride;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _tts = TtsService();
    _initTts();
    _initTtsSpeed();
    _initReaderPrefs();
    _scrollCtrl.addListener(_onScroll);
    _initOfflineCheck();
    unawaited(Future<void>.microtask(_loadHighlights));
    final rp = widget.resumePercent;
    if (rp != null && rp >= 5 && rp <= 95) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        setState(() {
          _isReaderMode = true;
          _resumePercent = rp;
          _resumeBannerVisible = false;
        });
        if (_scrollCtrl.hasClients) {
          final max = _scrollCtrl.position.maxScrollExtent;
          if (max > 0) {
            _scrollCtrl.jumpTo(max * rp / 100);
          }
        }
      });
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _progressDebounce?.cancel();
    _flushProgressNow();
    _tts.stop();
    _tts.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  Future<void> _initOfflineCheck() async {
    final os = OfflineStorageService.instance;
    final cached = os.getArticle(widget.postId);
    if (mounted) {
      setState(() {
        _isCached = cached != null;
        _cachedOverride = cached;
      });
    }
  }

  Future<void> _loadHighlights() async {
    if (_highlightsLoaded) return;
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('mmt.auth.accessToken') ?? prefs.getString('auth_token');
      if (token == null || token.isEmpty) {
        if (mounted) setState(() => _highlightsLoaded = true);
        return;
      }
      final list = await _readerApi.getHighlightsForPost(widget.postId, authToken: token);
      if (mounted) {
        setState(() {
          _highlights = list;
          _highlightsLoaded = true;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _highlightsLoaded = true);
    }
  }

  Future<void> _createHighlight({
    required int paragraphIndex,
    required int charStart,
    required int charEnd,
    required String excerpt,
  }) async {
    if (_isSavingHighlight) return;
    setState(() => _isSavingHighlight = true);
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('mmt.auth.accessToken') ?? prefs.getString('auth_token');
      final os = OfflineStorageService.instance;
      final results = await Connectivity().checkConnectivity();
      final isOffline = results.every((r) => r == ConnectivityResult.none);
      if (token == null || token.isEmpty || isOffline) {
        await os.enqueuePendingHighlight(
          postId: widget.postId,
          paragraphIndex: paragraphIndex,
          charStart: charStart,
          charEnd: charEnd,
          excerpt: excerpt,
        );
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            backgroundColor: MmtColors.ink950,
            content: Text(isOffline ? 'Highlight saved for sync when online' : 'Sign in to sync highlights',
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
            duration: const Duration(milliseconds: 1600),
          ));
        }
        return;
      }
      final created = await _readerApi.createHighlight(
        postId: widget.postId,
        paragraphIndex: paragraphIndex,
        charStart: charStart,
        charEnd: charEnd,
        excerpt: excerpt,
        authToken: token,
      );
      if (created != null && mounted) {
        setState(() {
          _highlights = <Map<String, dynamic>>[..._highlights, created];
        });
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          backgroundColor: MmtColors.ink950,
          content: Text('Highlight created', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
          duration: Duration(milliseconds: 1200),
        ));
      }
    } finally {
      if (mounted) setState(() => _isSavingHighlight = false);
    }
  }

  Future<void> _deleteHighlight(String highlightId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('mmt.auth.accessToken') ?? prefs.getString('auth_token');
      if (token != null && token.isNotEmpty) {
        await _readerApi.deleteHighlight(highlightId, authToken: token);
      }
      if (mounted) {
        setState(() {
          _highlights = _highlights.where((h) => (h['id'] ?? '').toString() != highlightId).toList(growable: false);
        });
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          backgroundColor: MmtColors.ink950,
          content: Text('Highlight removed', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
          duration: Duration(milliseconds: 1200),
        ));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          backgroundColor: MmtColors.ink950,
          content: Text('Could not remove highlight', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
          duration: Duration(milliseconds: 1200),
        ));
      }
    }
  }

  Future<void> _toggleOffline() async {
    if (_isSavingOffline) return;
    setState(() => _isSavingOffline = true);
    final os = OfflineStorageService.instance;
    try {
      if (_isCached) {
        await os.removeArticle(widget.postId);
        if (mounted) {
          setState(() {
            _isCached = false;
            _cachedOverride = null;
          });
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            backgroundColor: MmtColors.ink950,
            content: Text('Removed from offline', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
            duration: Duration(milliseconds: 1400),
          ));
        }
      } else {
        final cats = (widget.post.categories ?? <CategoryResponse>[])
            .map((c) => <String, dynamic>{'id': c.id, 'name': c.name, 'slug': c.slug})
            .toList(growable: false);
        final saved = await os.saveArticle(
          postId: widget.postId,
          title: widget.post.title,
          cover: widget.post.cover,
          content: widget.post.content ?? '',
          contentHtml: widget.post.contentHtml ?? '',
          readingTimeMinutes: widget.post.readingTimeMinutes ?? 7,
          authorName: widget.post.author?.name ?? 'MapMyTimes',
          categories: cats,
        );
        if (mounted) {
          setState(() {
            _isCached = saved != null;
            _cachedOverride = saved;
          });
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            backgroundColor: MmtColors.ink950,
            content: Text(saved != null ? 'Article saved offline' : 'Could not save right now',
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
            duration: const Duration(milliseconds: 1400),
          ));
        }
      }
    } finally {
      if (mounted) setState(() => _isSavingOffline = false);
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused || state == AppLifecycleState.inactive) {
      _flushProgressNow();
      _tts.stop();
      if (mounted) setState(() => _ttsPlaying = false);
    }
  }

  Future<void> _initReaderPrefs() async {
    final prefs = await rmu.ReaderPrefs.load();
    final dismissed = await rmu.ReaderPrefs.isAutoSuggestDismissed(widget.postId);
    final words = rmu.computeStrippedWordCount(
      widget.post.content ?? widget.post.contentHtml ?? '',
    );
    if (mounted) {
      setState(() {
        _readerPrefs = prefs;
        if (!dismissed && words >= rmu.kAutoSuggestWordThreshold) {
          _showAutoSuggest = true;
        }
      });
    }
    unawaited(_syncRemoteReaderPrefs());
    unawaited(_loadRemoteProgress());
  }

  Future<void> _syncRemoteReaderPrefs() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('mmt.auth.accessToken') ?? prefs.getString('auth_token');
      if (token == null || token.isEmpty) return;
      final remote = await _readerApi.getReaderPrefs(authToken: token);
      if (remote == null || !mounted) return;
      final fontSizeIdx = (remote['fontSizeIdx'] ?? remote['font_size_idx']) as int?;
      final fontStackIdx = (remote['fontStack'] ?? remote['font_stack']) as int?;
      final lineSpacingIdx = (remote['lineSpacing'] ?? remote['line_spacing']) as int?;
      final themeIdx = (remote['theme']) as int?;
      final next = _readerPrefs.copyWith(
        fontSizeIdx: fontSizeIdx ?? _readerPrefs.fontSizeIdx,
        fontStack: (fontStackIdx != null && fontStackIdx >= 0 && fontStackIdx < rmu.ReaderFontStack.values.length)
            ? rmu.ReaderFontStack.values[fontStackIdx]
            : _readerPrefs.fontStack,
        lineSpacing: (lineSpacingIdx != null && lineSpacingIdx >= 0 && lineSpacingIdx < rmu.ReaderLineSpacing.values.length)
            ? rmu.ReaderLineSpacing.values[lineSpacingIdx]
            : _readerPrefs.lineSpacing,
        theme: (themeIdx != null && themeIdx >= 0 && themeIdx < rmu.ReaderTheme.values.length)
            ? rmu.ReaderTheme.values[themeIdx]
            : _readerPrefs.theme,
      );
      if (mounted) {
        setState(() => _readerPrefs = next);
        unawaited(next.save());
      }
    } catch (_) {}
  }

  Future<void> _loadRemoteProgress() async {
    final rp = widget.resumePercent;
    if (rp != null && rp >= 5 && rp <= 95) {
      return;
    }
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('mmt.auth.accessToken') ?? prefs.getString('auth_token');
      int? result;
      if (token != null && token.isNotEmpty) {
        result = await _readerApi.getReadingProgressForPost(widget.postId, authToken: token);
      } else {
        result = prefs.getInt('mmt:reader:progress:${widget.postId}');
      }
      if (result != null && result >= 5 && result <= 95 && mounted) {
        setState(() {
          _resumeBannerVisible = true;
          _resumePercent = result;
        });
      }
    } catch (_) {}
  }

  void _jumpToResume() {
    if (_scrollCtrl.hasClients && _resumePercent != null) {
      _scrollCtrl.jumpTo(_scrollCtrl.position.maxScrollExtent * _resumePercent! / 100);
    }
    if (mounted) setState(() => _resumeBannerVisible = false);
  }

  void _flushProgressNow() {
    if (!mounted) return;
    final percent = (_scrollProgress * 100).round();
    if (percent == _lastSentProgress) return;
    _lastSentProgress = percent;
    unawaited(() async {
      try {
        final prefs = await SharedPreferences.getInstance();
        final token = prefs.getString('mmt.auth.accessToken') ?? prefs.getString('auth_token');
        if (token != null && token.isNotEmpty) {
          await _readerApi.upsertReadingProgress(
            postId: widget.postId,
            scrollPercent: percent,
            authToken: token,
          );
        } else {
          await prefs.setInt('mmt:reader:progress:${widget.postId}', percent);
        }
      } catch (_) {}
    }());
  }

  void _onScroll() {
    if (!mounted || !_scrollCtrl.hasClients) return;
    final max = _scrollCtrl.position.maxScrollExtent;
    if (max <= 0) return setState(() => _scrollProgress = 0);
    final progress = (_scrollCtrl.offset / max).clamp(0.0, 1.0);
    setState(() => _scrollProgress = progress);
    final pct = (progress * 100).round();
    if ((pct - _lastSentProgress).abs() >= 5) {
      _progressDebounce?.cancel();
      _flushProgressNow();
      return;
    }
    if (_progressDebounce?.isActive ?? false) _progressDebounce!.cancel();
    _progressDebounce = Timer(const Duration(seconds: 5), _flushProgressNow);
  }

  // =========================================================================
  // READER MODE PERSISTENCE + BOTTOM SHEET (Aa Icon)
  // =========================================================================
  Future<void> _updatePrefs(rmu.ReaderPrefs next) async {
    setState(() => _readerPrefs = next);
    unawaited(next.save());
    unawaited(() async {
      try {
        final prefs = await SharedPreferences.getInstance();
        final token = prefs.getString('mmt.auth.accessToken') ?? prefs.getString('auth_token');
        if (token == null || token.isEmpty) return;
        await _readerApi.upsertReaderPrefs(<String, dynamic>{
          'fontSizeIdx': next.fontSizeIdx,
          'fontStack': next.fontStack.index,
          'lineSpacing': next.lineSpacing.index,
          'theme': next.theme.index,
        }, authToken: token);
      } catch (_) {}
    }());
  }

  static const List<double> _kTtsSpeeds = <double>[0.75, 1.0, 1.25, 1.5, 2.0];

  Future<void> _initTtsSpeed() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final idx = prefs.getInt('mmt:tts:speedIdx');
      if (idx != null && idx >= 0 && idx < _kTtsSpeeds.length && mounted) {
        setState(() => _ttsSpeedIdx = idx);
      }
    } catch (_) {}
  }

  Future<void> _setTtsSpeed(int idx) async {
    if (idx < 0 || idx >= _kTtsSpeeds.length) return;
    if (!mounted) return;
    final wasPlaying = _ttsPlaying;
    setState(() => _ttsSpeedIdx = idx);
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt('mmt:tts:speedIdx', idx);
    } catch (_) {}
    var rate = _kTtsSpeeds[idx];
    try {
      await _tts.setSpeechRate(rate);
      if (wasPlaying) {
        await _tts.stop();
        final text = '${widget.post.title}. ${widget.post.excerpt ?? ''} ${_stripHtml((widget.post.content ?? widget.post.contentHtml ?? '').toString())}';
        final ok = await _tts.speak(text);
        if (ok == true && mounted) setState(() => _ttsPlaying = true);
      }
    } catch (_) {}
  }

  void _openTypographySheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.white,
      elevation: 24,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        side: BorderSide(color: MmtColors.ink950, width: 2),
      ),
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx2, setLocal) => DraggableScrollableSheet(
            initialChildSize: 0.6,
            minChildSize: 0.5,
            maxChildSize: 0.85,
            expand: false,
            builder: (_, sc) => SingleChildScrollView(
              controller: sc,
              padding: const EdgeInsets.fromLTRB(22, 18, 22, 32),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Container(width: 42, height: 42, alignment: Alignment.center,
                    decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: MmtColors.news),
                    child: const FaIcon(FontAwesomeIcons.textHeight, size: 15, color: Colors.white)),
                  const SizedBox(width: 12),
                  Text('TYPOGRAPHY'.toUpperCase(),
                    style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 15, letterSpacing: 1.3)),
                  const Spacer(),
                  IconButton(onPressed: () => Navigator.pop(ctx2), icon: const Icon(Icons.close, size: 22)),
                ]),
                const SizedBox(height: 22),
                // ---- Font size 5-step ----
                _sheetLabel('FONT SIZE'),
                const SizedBox(height: 10),
                Row(children: List.generate(rmu.kReaderFontSizeSteps.length, (i) {
                  final selected = _readerPrefs.fontSizeIdx == i;
                  return Expanded(child: Padding(
                    padding: EdgeInsets.only(right: i == 4 ? 0 : 8),
                    child: InkWell(
                      onTap: () {
                        final next = _readerPrefs.copyWith(fontSizeIdx: i);
                        _updatePrefs(next);
                        setLocal(() {});
                      },
                      child: Container(
                        height: 44,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: selected ? MmtColors.news : Colors.white,
                          border: Border.all(color: MmtColors.ink950, width: 1.8),
                        ),
                        child: Text('Aa',
                          style: GoogleFonts.inter(
                            fontWeight: FontWeight.w800,
                            fontSize: 11 + i.toDouble() * 1.2,
                            color: selected ? Colors.white : MmtColors.ink950,
                          )),
                      ),
                    ),
                  ));
                })),
                const SizedBox(height: 24),
                // ---- Font stack ----
                _sheetLabel('FONT FAMILY'),
                const SizedBox(height: 10),
                ...rmu.ReaderFontStack.values.map((f) {
                  final selected = _readerPrefs.fontStack == f;
                  final spec = rmu.kReaderFonts[f]!;
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: InkWell(
                      onTap: () {
                        final next = _readerPrefs.copyWith(fontStack: f);
                        _updatePrefs(next);
                        setLocal(() {});
                      },
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                        decoration: BoxDecoration(
                          color: selected ? MmtColors.news50 : Colors.white,
                          border: Border.all(color: selected ? MmtColors.news : MmtColors.ink950, width: selected ? 2.4 : 1.8),
                        ),
                        child: Row(children: [
                          Icon(selected ? Icons.radio_button_checked : Icons.radio_button_off,
                            color: selected ? MmtColors.news : MmtColors.ink700, size: 18),
                          const SizedBox(width: 12),
                          Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Text(spec.displayName, style: GoogleFonts.inter(fontWeight: FontWeight.w800, fontSize: 13)),
                            const SizedBox(height: 4),
                            Text('The quick brown fox · जल्दी लोमड़ी',
                              style: spec.builder(fontSize: 14, fontWeight: FontWeight.w400, height: 1.3)),
                          ]),
                        ]),
                      ),
                    ),
                  );
                }),
                const SizedBox(height: 24),
                // ---- Line spacing ----
                _sheetLabel('LINE SPACING'),
                const SizedBox(height: 10),
                Row(children: rmu.ReaderLineSpacing.values.map((ls) {
                  final selected = _readerPrefs.lineSpacing == ls;
                  return Expanded(child: Padding(
                    padding: EdgeInsets.only(right: ls == rmu.ReaderLineSpacing.relaxed ? 0 : 8),
                    child: InkWell(
                      onTap: () {
                        final next = _readerPrefs.copyWith(lineSpacing: ls);
                        _updatePrefs(next);
                        setLocal(() {});
                      },
                      child: Container(
                        height: 44,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: selected ? MmtColors.news : Colors.white,
                          border: Border.all(color: MmtColors.ink950, width: 1.8),
                        ),
                        child: Text(rmu.kReaderLineSpacingLabels[ls]!,
                          style: GoogleFonts.inter(
                            fontWeight: FontWeight.w800, fontSize: 11,
                            letterSpacing: 0.8,
                            color: selected ? Colors.white : MmtColors.ink950)),
                      ),
                    ),
                  ));
                }).toList()),
                const SizedBox(height: 24),
                // ---- Theme Light / Dark ----
                _sheetLabel('THEME'),
                const SizedBox(height: 10),
                Row(children: rmu.ReaderTheme.values.map((t) {
                  final spec = rmu.kReaderThemes[t]!;
                  final selected = _readerPrefs.theme == t;
                  final isLast = t == rmu.ReaderTheme.values.last;
                  return Expanded(child: Padding(
                    padding: EdgeInsets.only(right: isLast ? 0 : 10),
                    child: InkWell(
                      onTap: () {
                        final next = _readerPrefs.copyWith(theme: t);
                        _updatePrefs(next);
                        setLocal(() {});
                      },
                      child: Container(
                        height: 90,
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: spec.bg,
                          border: Border.all(color: selected ? MmtColors.news : MmtColors.ink950, width: selected ? 2.4 : 1.8),
                          boxShadow: selected ? [BoxShadow(color: MmtColors.news.withOpacity(0.3), blurRadius: 12)] : null,
                        ),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Row(children: [
                            Icon(selected ? Icons.check_circle : Icons.circle_outlined,
                              color: selected ? MmtColors.news : spec.fg.withOpacity(0.5), size: 16),
                            const Spacer(),
                            Container(height: 6, width: 18, color: MmtColors.news),
                          ]),
                          const Spacer(),
                          Text(spec.label, style: GoogleFonts.inter(color: spec.fg, fontWeight: FontWeight.w900, fontSize: 12, letterSpacing: 0.6)),
                          const SizedBox(height: 6),
                          Container(height: 3, color: spec.fg.withOpacity(0.7)),
                          const SizedBox(height: 4),
                          Container(height: 3, width: 80, color: spec.fg.withOpacity(0.45)),
                        ]),
                      ),
                    ),
                  ));
                }).toList()),
                const SizedBox(height: 24),
                _sheetLabel('SPEED'),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: List.generate(_kTtsSpeeds.length, (i) {
                    final selected = _ttsSpeedIdx == i;
                    final label = i == 0
                        ? '0.75x'
                        : i == 1
                            ? '1x'
                            : i == 2
                                ? '1.25x'
                                : i == 3
                                    ? '1.5x'
                                    : '2x';
                    return InkWell(
                      onTap: () {
                        _setTtsSpeed(i);
                        setLocal(() {});
                      },
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                        decoration: BoxDecoration(
                          color: selected ? MmtColors.news : Colors.white,
                          border: Border.all(color: MmtColors.ink950, width: 1.8),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(label,
                          style: GoogleFonts.inter(
                            fontWeight: FontWeight.w900,
                            fontSize: 12,
                            letterSpacing: 0.6,
                            color: selected ? Colors.white : MmtColors.ink950,
                          )),
                      ),
                    );
                  }),
                ),
                const SizedBox(height: 20),
              ]),
            ),
          ),
        );
      },
    );
  }

  Widget _sheetLabel(String t) =>
    Text(t, style: GoogleFonts.inter(fontSize: 10.5, fontWeight: FontWeight.w900, letterSpacing: 1.4, color: MmtColors.ink600));

  Future<void> _initTts() async {
    try {
      await _tts.init(
        language: 'en-IN',
        speechRate: _kTtsSpeeds[_ttsSpeedIdx],
      );
      _tts.setCompletionHandler(() { if (mounted) setState(() => _ttsPlaying = false); });
      _tts.setCancelHandler(() { if (mounted) setState(() => _ttsPlaying = false); });
      _tts.setErrorHandler((_) { if (mounted) setState(() => _ttsPlaying = false); });
      if (mounted) setState(() => _ttsReady = true);
    } catch (_) {}
  }

  Future<void> _toggleTts() async {
    if (_ttsPlaying) {
      await _tts.stop();
      if (mounted) setState(() => _ttsPlaying = false);
      return;
    }
    try {
      await _tts.setSpeechRate(_kTtsSpeeds[_ttsSpeedIdx]);
      final text = '${widget.post.title}. ${widget.post.excerpt ?? ''} ${_stripHtml((widget.post.content ?? widget.post.contentHtml ?? '').toString())}';
      final ok = await _tts.speak(text);
      if (ok == true && mounted) setState(() => _ttsPlaying = true);
    } catch (e) {
      if (mounted) {
        final synthAvailable = _ttsReady;
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(synthAvailable
              ? 'Listen unavailable right now'
              : 'TTS not available on this device'),
          backgroundColor: MmtColors.ink950));
      }
    }
  }

  Future<void> _shareWhatsAppFirst() async {
    final url = '${Env.siteUrl}/news/${widget.slug}';
    final fullText = '${widget.post.title}\n\n$url';
    try {
      await Share.share(fullText, subject: widget.post.title, sharePositionOrigin: Rect.zero);
    } catch (_) {
      await Clipboard.setData(ClipboardData(text: url));
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(widget.t.common.linkCopied), backgroundColor: MmtColors.ink950));
    }
  }

  void _toggleSaved(BuildContext c) {
    final summary = BlogPostSummaryResponse(
      id: widget.postId,
      slug: widget.slug,
      title: widget.post.title,
      featuredImageUrl: widget.post.cover,
      excerpt: widget.post.excerpt,
      publishedAt: widget.post.publishedAt,
      status: PostStatus.published,
      postType: PostType.article,
    );
    ref.read(savedArticlesNotifierProvider.notifier).toggle(widget.postId, meta: summary);
    final saved = ref.read(savedArticlesNotifierProvider).contains(widget.postId);
    ScaffoldMessenger.of(c).showSnackBar(SnackBar(
      backgroundColor: MmtColors.ink950,
      content: Text(saved ? 'Article saved offline' : 'Removed from saved', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
      duration: const Duration(milliseconds: 1400),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final fontScale = ref.watch(fontScaleNotifierProvider);
    final isSaved = ref.watch(isArticleSavedProvider(widget.postId));
    final latestAsync = ref.watch(latestPostsProvider(1));

    final catIds = (widget.post.categories ?? <CategoryResponse>[]).map((c) => c.id).toSet();
    final related = latestAsync.whenOrNull(data: (list) {
      final filt = list.where((p) => (p.id != widget.postId) && (p.categories ?? []).any((c) => catIds.contains(c.id))).toList(growable: false);
      return (filt.isNotEmpty ? filt : list.where((p) => p.id != widget.postId).take(4)).take(4).toList(growable: false);
    });

    final readingTheme = rmu.kReaderThemes[_readerPrefs.theme]!;
    final readingFont = rmu.kReaderFonts[_readerPrefs.fontStack]!;
    final readingFontSize = rmu.kReaderFontSizeSteps[_readerPrefs.fontSizeIdx];
    final readingLineH = rmu.kReaderLineHeight[_readerPrefs.lineSpacing]!;

    final Brightness materialBright = Theme.of(context).brightness;
    final Color standardBg = (materialBright == Brightness.dark) ? MmtColors.ink950 : MmtColors.background;
    final bool standardDark = standardBg.computeLuminance() < 0.5;
    final bool readerDark = readingTheme.bg.computeLuminance() < 0.5;
    final bool dark = _isReaderMode ? readerDark : standardDark;

    final Widget standardView = _buildStandardView(
      dark: standardDark, fontScale: fontScale, related: related, isSaved: isSaved,
    );

    final Widget readerView = _buildReaderView(
      theme: readingTheme, fontSpec: readingFont,
      fontSize: readingFontSize, lineHeight: readingLineH,
      dark: readerDark,
    );

    return Stack(children: [
      Scaffold(
        backgroundColor: _isReaderMode ? readingTheme.bg : standardBg,
        body: Column(children: [
          if (_resumeBannerVisible && _resumePercent != null)
            MaterialBanner(
              content: Text('Continue from $_resumePercent%',
                style: GoogleFonts.inter(fontWeight: FontWeight.w700, fontSize: 13, color: MmtColors.ink950)),
              backgroundColor: Colors.white,
              dividerColor: MmtColors.ink950,
              leading: Container(width: 34, height: 34, alignment: Alignment.center,
                decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 1.8), color: MmtColors.news),
                child: const FaIcon(FontAwesomeIcons.bookmark, size: 13, color: Colors.white)),
              actions: [
                TextButton(
                  onPressed: _jumpToResume,
                  child: Text('RESUME', style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 11, letterSpacing: 1.2, color: MmtColors.news)),
                ),
                TextButton(
                  onPressed: () => setState(() => _resumeBannerVisible = false),
                  child: Text('DISMISS', style: GoogleFonts.inter(fontWeight: FontWeight.w800, fontSize: 11, letterSpacing: 1.2, color: MmtColors.ink700)),
                ),
              ],
            ),
          Expanded(
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 220),
              switchInCurve: Curves.easeOutCubic,
              switchOutCurve: Curves.easeInCubic,
              transitionBuilder: (child, anim) {
                return FadeTransition(
                  opacity: anim,
                  child: ScaleTransition(
                    scale: Tween(begin: 0.992, end: 1.0).animate(anim),
                    child: child,
                  ),
                );
              },
              child: KeyedSubtree(
                key: ValueKey(_isReaderMode ? 'reader' : 'standard'),
                child: _isReaderMode ? readerView : standardView,
              ),
            ),
          ),
        ]),
      ),
      // Red fixed progress bar (top) — visible ONLY in Reader Mode
      if (_isReaderMode) Positioned(
        top: 0, left: 0, right: 0,
        child: IgnorePointer(
            child: Container(
              height: 3,
              color: readingTheme.chromeBorder.withOpacity(0.08),
              child: FractionallySizedBox(
              alignment: Alignment.centerLeft,
              widthFactor: _scrollProgress,
              child: Container(color: rmu.kMmtNewsRed,
                child: const SizedBox.expand()),
            ),
          ),
        ),
      ),
      // Auto-suggest Reader Mode prompt (dismissible)
      if (_showAutoSuggest && !_isReaderMode)
        Positioned(
          bottom: 24, left: 16, right: 16,
          child: _ReaderModeAutoSuggestCard(
            onEnterReader: () {
              rmu.ReaderPrefs.markAutoSuggestDismissed(widget.postId);
              setState(() { _showAutoSuggest = false; _isReaderMode = true; });
            },
            onDismiss: () {
              rmu.ReaderPrefs.markAutoSuggestDismissed(widget.postId);
              setState(() => _showAutoSuggest = false);
            },
          ),
        ),
    ]);
  }

  // ==========================================================================
  // Standard view = existing article layout (unchanged behavior)
  // ==========================================================================
  Widget _buildStandardView({
    required bool dark,
    required double fontScale,
    required List<BlogPostSummaryResponse>? related,
    required bool isSaved,
  }) {
    final summary = MediaQuery(
      data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(fontScale)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        _HeroCarousel(items: _collectHeroImages(), dark: dark),
        if (_collectHeroImages().isNotEmpty) const SizedBox(height: 22),
        if ((widget.post.categories ?? <CategoryResponse>[]).isNotEmpty)
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final c in widget.post.categories ?? <CategoryResponse>[])
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(color: MmtColors.news, border: Border.all(color: MmtColors.ink950, width: 2)),
                  child: Text(c.name.toString().toUpperCase(), style: GoogleFonts.inter(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 10, letterSpacing: 1.4, height: 1.0)),
                ),
            ],
          ),
        if ((widget.post.categories ?? <CategoryResponse>[]).isNotEmpty) const SizedBox(height: 18),
        Text(widget.post.title, style: GoogleFonts.archivoBlack(fontSize: 26, height: 1.05, color: dark ? Colors.white : MmtColors.ink950, letterSpacing: -0.2)),
        if ((widget.post.excerpt ?? '').isNotEmpty) ...[
          const SizedBox(height: 10),
          Text(widget.post.excerpt ?? '', style: GoogleFonts.inter(fontSize: 15, fontStyle: FontStyle.italic, fontWeight: FontWeight.w500, height: 1.6, color: dark ? Colors.white70 : MmtColors.ink600)),
        ],
        const SizedBox(height: 18),
        Container(height: 2, color: dark ? Colors.white30 : MmtColors.ink950),
        const SizedBox(height: 14),
        // Meta row
        Wrap(spacing: 12, runSpacing: 8, crossAxisAlignment: WrapCrossAlignment.center, children: [
          Text('${widget.t.byAuthor} ${widget.post.author?.name ?? 'MapMyTimes'}', style: GoogleFonts.inter(fontSize: 11.5, color: dark ? Colors.white70 : MmtColors.ink700, fontWeight: FontWeight.w700)),
          Text('·', style: TextStyle(fontWeight: FontWeight.w900, color: dark ? Colors.white54 : MmtColors.ink600)),
          Text(_fmtDate(widget.post.publishedAt ?? widget.post.createdAt), style: GoogleFonts.inter(fontSize: 11.5, color: dark ? Colors.white70 : MmtColors.ink700, fontWeight: FontWeight.w600)),
          Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4), decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2)),
            child: Text('${widget.post.readingTimeMinutes ?? 7} MIN READ', style: GoogleFonts.inter(fontSize: 10, fontWeight: FontWeight.w900, letterSpacing: 1.2, color: dark ? Colors.white : MmtColors.ink950, height: 1.0))),
          if ((widget.post.viewCount ?? 0) > 0)
            Row(mainAxisSize: MainAxisSize.min, children: [
              Icon(Icons.visibility_outlined, size: 15, color: dark ? Colors.white70 : MmtColors.ink700),
              const SizedBox(width: 4),
              Text(_fmtViews(widget.post.viewCount), style: GoogleFonts.inter(fontSize: 11, fontWeight: FontWeight.w700, color: dark ? Colors.white70 : MmtColors.ink700)),
            ]),
        ]),
        const SizedBox(height: 14),
        // TABLE OF CONTENTS ACCORDION (TOC) — backend dynamic entries
        if ((widget.post.tableOfContents ?? <TocEntry>[]).isNotEmpty)
          _TocAccordion(
            entries: widget.post.tableOfContents ?? <TocEntry>[],
            dark: dark,
            scrollController: _scrollCtrl,
          ),
        if ((widget.post.tableOfContents ?? <TocEntry>[]).isNotEmpty) const SizedBox(height: 2),
        // Video embed: Instagram Reels/Post first, then YouTube, then generic external link
        if (widget.post.instagramMediaId != null)
          _InstagramEmbed(
            mediaId: widget.post.instagramMediaId!,
            videoUrl: widget.post.videoUrl ?? widget.post.shortVideoUrl ?? '',
            isReel: widget.post.isInstagramReel,
          )
        else if (widget.post.youtubeVideoId != null)
          _YoutubeEmbed(videoId: widget.post.youtubeVideoId!, videoUrl: widget.post.videoUrl ?? widget.post.shortVideoUrl ?? '')
        else if ((widget.post.videoUrl ?? '').isNotEmpty && !kIsWeb)
          _OpenVideoLink(videoUrl: widget.post.videoUrl ?? widget.post.shortVideoUrl ?? ''),
        if (widget.post.instagramMediaId != null || widget.post.youtubeVideoId != null || (widget.post.videoUrl ?? '').isNotEmpty) const SizedBox(height: 14),
        // Font size stepper
        Container(
          decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), color: dark ? MmtColors.ink900 : Colors.white),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: Row(children: [
            FaIcon(FontAwesomeIcons.textHeight, size: 13, color: dark ? Colors.white60 : MmtColors.ink700),
            const SizedBox(width: 10),
            Text('TEXT SIZE', style: GoogleFonts.inter(fontSize: 10.5, fontWeight: FontWeight.w900, letterSpacing: 1.2, color: dark ? Colors.white60 : MmtColors.ink700, height: 1.0)),
            const Spacer(),
            _fontBtn(context, 'A−', () => ref.read(fontScaleNotifierProvider.notifier).stepDown(), dark: dark),
            const SizedBox(width: 6),
            _fontBtn(context, 'A', () => ref.read(fontScaleNotifierProvider.notifier).reset(), fill: (fontScale - 1.0).abs() < 0.02, dark: dark),
            const SizedBox(width: 6),
            _fontBtn(context, 'A+', () => ref.read(fontScaleNotifierProvider.notifier).stepUp(), dark: dark),
          ]),
        ),
        const SizedBox(height: 20),
        // Body paragraphs
        ..._renderBody(widget.post, context, fontScale, dark),
      ]),
    );

    return CustomScrollView(
      controller: _scrollCtrl,
      slivers: [
      SliverAppBar(
        leadingWidth: 68,
        backgroundColor: dark ? MmtColors.ink900 : Colors.white,
        foregroundColor: dark ? Colors.white : MmtColors.ink950,
        elevation: 0,
        surfaceTintColor: Colors.transparent,
        scrolledUnderElevation: 1,
        shadowColor: (dark ? Colors.white : MmtColors.ink950).withOpacity(0.08),
        titleSpacing: 2,
        title: Text(
          'MAP MY TIMES',
          style: GoogleFonts.archivoBlack(
            color: dark ? Colors.white : MmtColors.ink950,
            fontSize: 12,
            letterSpacing: 0.6,
          ),
        ),
        leading: Padding(
          padding: const EdgeInsets.fromLTRB(12, 8, 0, 8),
          child: InkWell(
            onTap: () => GoRouter.of(context).canPop() ? context.pop() : context.go('/'),
            child: Container(decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), color: dark ? MmtColors.ink900 : Colors.white),
              child: Icon(Icons.chevron_left, color: dark ? Colors.white : MmtColors.ink950)),
          ),
        ),
        actions: [
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.fromLTRB(0, 8, 10, 8),
            physics: const NeverScrollableScrollPhysics(),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                InkWell(
                  onTap: _openTypographySheet,
                  child: Container(width: 38, height: 40, margin: const EdgeInsets.only(right: 7), alignment: Alignment.center,
                    decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), color: dark ? MmtColors.ink900 : Colors.white),
                    child: Text('Aa', style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 12.5, color: dark ? Colors.white : MmtColors.ink950)),
                  ),
                ),
                InkWell(
                  onTap: () => setState(() => _isReaderMode = !_isReaderMode),
                  child: Container(width: 38, height: 40, margin: const EdgeInsets.only(right: 7), alignment: Alignment.center,
                    decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), color: _isReaderMode ? MmtColors.news : (dark ? MmtColors.ink900 : Colors.white)),
                    child: FaIcon(FontAwesomeIcons.bookOpen, size: 14.5, color: _isReaderMode ? Colors.white : MmtColors.news),
                  ),
                ),
                InkWell(
                  onTap: _toggleTts,
                  child: Container(width: 38, height: 40, margin: const EdgeInsets.only(right: 7), alignment: Alignment.center,
                    decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), color: _ttsPlaying ? MmtColors.news : (dark ? MmtColors.ink900 : Colors.white)),
                    child: _ttsPlaying
                        ? const FaIcon(FontAwesomeIcons.pause, size: 13.5, color: Colors.white)
                        : FaIcon(FontAwesomeIcons.microphone, size: 13.5, color: _ttsReady ? (dark ? Colors.white : MmtColors.ink950) : (dark ? Colors.white54 : MmtColors.ink600)),
                  ),
                ),
                InkWell(
                  onTap: _isSavingOffline ? null : _toggleOffline,
                  child: Container(width: 38, height: 40, margin: const EdgeInsets.only(right: 7), alignment: Alignment.center,
                    decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), color: _isCached ? const Color(0xFF10B981) : (dark ? MmtColors.ink900 : Colors.white)),
                    child: _isSavingOffline
                        ? SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: dark ? Colors.white : MmtColors.ink950))
                        : _isCached
                            ? const FaIcon(FontAwesomeIcons.check, size: 13.5, color: Colors.white)
                            : FaIcon(FontAwesomeIcons.cloudDownload, size: 13.5, color: dark ? Colors.white : MmtColors.ink950),
                  ),
                ),
                InkWell(
                  onTap: () => _toggleSaved(context),
                  child: Container(width: 38, height: 40, margin: const EdgeInsets.only(right: 7), alignment: Alignment.center,
                    decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), color: isSaved ? MmtColors.news : (dark ? MmtColors.ink900 : Colors.white)),
                    child: FaIcon(isSaved ? FontAwesomeIcons.solidBookmark : FontAwesomeIcons.bookmark, size: 13.5, color: isSaved ? Colors.white : (dark ? Colors.white : MmtColors.ink950)),
                  ),
                ),
                InkWell(
                  onTap: _shareWhatsAppFirst,
                  child: Container(width: 38, height: 40, alignment: Alignment.center,
                    decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), color: dark ? MmtColors.ink900 : Colors.white),
                    child: FaIcon(FontAwesomeIcons.shareNodes, size: 13.5, color: dark ? Colors.white : MmtColors.ink950),
                  ),
                ),
              ],
            ),
          ),
        ],
        floating: false,
        pinned: true,
      ),
      SliverToBoxAdapter(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 30),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            summary,
            const SizedBox(height: 22),
            // Tags
            if ((widget.post.tags ?? <TagResponse>[]).isNotEmpty)
              Wrap(spacing: 8, runSpacing: 8, children: [
                for (final tag in widget.post.tags ?? <TagResponse>[])
                  Container(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6), decoration: BoxDecoration(color: dark ? MmtColors.news.withOpacity(0.15) : MmtColors.news50, border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2)),
                    child: Text('#${tag.name}', style: GoogleFonts.inter(fontWeight: FontWeight.w800, fontSize: 11, color: dark ? Colors.white : MmtColors.ink950))),
              ]),
            if ((widget.post.tags ?? <TagResponse>[]).isNotEmpty) const SizedBox(height: 22),
            // Quick action row
            Row(children: [
              Expanded(child: InkWell(
                onTap: _shareWhatsAppFirst,
                child: Container(padding: const EdgeInsets.symmetric(vertical: 12),
                  decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), boxShadow: [BoxShadow(offset: const Offset(3, 3), color: dark ? Colors.white30 : MmtColors.ink950)], color: dark ? MmtColors.ink900 : Colors.white),
                  alignment: Alignment.center,
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    FaIcon(FontAwesomeIcons.shareNodes, color: dark ? Colors.white : MmtColors.ink950, size: 14),
                    const SizedBox(width: 8),
                    Text(widget.t.common.share.toUpperCase(), style: GoogleFonts.inter(fontWeight: FontWeight.w900, letterSpacing: 1.4, fontSize: 11, color: dark ? Colors.white : MmtColors.ink950)),
                  ]),
                ),
              )),
              const SizedBox(width: 12),
              Expanded(child: InkWell(
                onTap: () async {
                  final url = '${Env.siteUrl}/news/${widget.slug}';
                  await Clipboard.setData(ClipboardData(text: url));
                  if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(backgroundColor: MmtColors.ink950, content: Text(widget.t.common.linkCopied, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)), duration: const Duration(milliseconds: 1200)));
                },
                child: Container(padding: const EdgeInsets.symmetric(vertical: 12),
                  decoration: BoxDecoration(border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 2), boxShadow: [BoxShadow(offset: const Offset(3, 3), color: dark ? Colors.white30 : MmtColors.ink950)], color: dark ? MmtColors.ink900 : Colors.white),
                  alignment: Alignment.center,
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(Icons.copy_all_outlined, size: 16, color: dark ? Colors.white : MmtColors.ink950),
                    const SizedBox(width: 8),
                    Text(widget.t.common.copyLink.toUpperCase(), style: GoogleFonts.inter(fontWeight: FontWeight.w900, letterSpacing: 1.4, fontSize: 11, color: dark ? Colors.white : MmtColors.ink950)),
                  ]),
                ),
              )),
            ]),
            const SizedBox(height: 30),
            // Author Bio Card (WRITTEN BY + MORE STORIES)
            _AuthorCard(
              author: widget.post.author,
              dark: dark,
              onMoreStories: () {
                final name = widget.post.author?.displayName ?? widget.post.author?.name;
                if (name?.isNotEmpty == true) {
                  context.push('/section/author?name=${Uri.encodeComponent(name!)}');
                }
              },
            ),
            const SizedBox(height: 26),
            // Comments Section Placeholder (COMMENTS (0) JOIN THE CONVERSATION)
            _CommentsSection(count: widget.post.commentCount ?? 0, dark: dark,
              onSignIn: () => context.push('/auth?redirect=article-${widget.slug}'),
              onJoin: () => context.push('/auth?register=1&redirect=article-${widget.slug}')),
            const SizedBox(height: 38),
            // Related Stories
            if (related != null && related.isNotEmpty) ...[
              const SectionEyebrow('Related Stories'),
              const SizedBox(height: 14),
              GridView.count(crossAxisCount: 2, shrinkWrap: true, physics: const NeverScrollableScrollPhysics(), mainAxisSpacing: 14, crossAxisSpacing: 14, childAspectRatio: 0.58,
                children: [for (final r in related) SecondaryGridCard(post: r, onTap: () { context.pop(); context.push('/article/${r.slug}?id=${r.id}'); })],
              ),
              const SizedBox(height: 24),
            ],
            Container(height: 2, color: MmtColors.ink950),
            const SizedBox(height: 18),
            Text('© ${DateTime.now().year} MAPMYTOUR LLP, India · ${widget.t.footer.copyright}', style: GoogleFonts.inter(fontSize: 10.5, color: MmtColors.ink600, fontWeight: FontWeight.w600)),
            const SizedBox(height: 20),
          ]),
        ),
      ),
    ]);
  }

  // ==========================================================================
  // READER MODE VIEW — Kindle-style centered max-width, theme bg/fg, body focus
  // ==========================================================================
  Widget _buildReaderView({
    required rmu.ReaderThemeSpec theme,
    required rmu.ReaderFontSpec fontSpec,
    required double fontSize,
    required double lineHeight,
    required bool dark,
  }) {
    final headlineStyle = GoogleFonts.archivoBlack(
      color: theme.fg,
      fontSize: fontSize + 12,
      height: 1.08,
      letterSpacing: -0.2,
    );
    const readerMaxWidth = 680.0;

    final paragraphs = _renderBodyReader(
      fontSpec: fontSpec, fontSize: fontSize, lineHeight: lineHeight, fg: theme.fg,
    );

    return CustomScrollView(
      controller: _scrollCtrl,
      slivers: [
        SliverAppBar(
          leadingWidth: 68,
          backgroundColor: theme.bg,
          foregroundColor: theme.fg,
          elevation: 0,
          surfaceTintColor: Colors.transparent,
          scrolledUnderElevation: 1,
          shadowColor: theme.chromeBorder.withOpacity(0.12),
          titleSpacing: 2,
          title: Container(
            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
            decoration: BoxDecoration(color: theme.chromeRed, borderRadius: BorderRadius.circular(3)),
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              Container(width: 2, height: 10, color: Colors.white),
              const SizedBox(width: 5),
              Text('MAP MY TIMES', style: GoogleFonts.archivoBlack(color: Colors.white, fontSize: 10, letterSpacing: 0.3)),
            ]),
          ),
          leading: Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 0, 8),
            child: InkWell(
              onTap: () => GoRouter.of(context).canPop() ? context.pop() : context.go('/'),
              child: Container(decoration: BoxDecoration(border: Border.all(color: theme.chromeBorder, width: 2), color: theme.bg),
                child: Icon(Icons.chevron_left, color: theme.fg)),
            ),
          ),
          actions: [
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.fromLTRB(0, 8, 10, 8),
              physics: const NeverScrollableScrollPhysics(),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  InkWell(
                    onTap: _openTypographySheet,
                    child: Container(width: 38, height: 40, margin: const EdgeInsets.only(right: 7), alignment: Alignment.center,
                      decoration: BoxDecoration(border: Border.all(color: theme.chromeBorder, width: 2), color: theme.bg),
                      child: Text('Aa', style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 12.5, color: theme.fg)),
                    ),
                  ),
                  InkWell(
                    onTap: () => setState(() => _isReaderMode = false),
                    child: Container(width: 38, height: 40, margin: const EdgeInsets.only(right: 7), alignment: Alignment.center,
                      decoration: BoxDecoration(border: Border.all(color: theme.chromeBorder, width: 2), color: theme.chromeRed),
                      child: const Icon(Icons.close, size: 16, color: Colors.white),
                    ),
                  ),
                  InkWell(
                    onTap: _toggleTts,
                    child: Container(width: 38, height: 40, margin: const EdgeInsets.only(right: 7), alignment: Alignment.center,
                      decoration: BoxDecoration(border: Border.all(color: theme.chromeBorder, width: 2), color: _ttsPlaying ? theme.chromeRed : theme.bg),
                      child: _ttsPlaying
                          ? const FaIcon(FontAwesomeIcons.pause, size: 13.5, color: Colors.white)
                          : FaIcon(FontAwesomeIcons.headphones, size: 13.5, color: theme.fg),
                    ),
                  ),
                  InkWell(
                    onTap: _isSavingOffline ? null : _toggleOffline,
                    child: Container(width: 38, height: 40, margin: const EdgeInsets.only(right: 7), alignment: Alignment.center,
                      decoration: BoxDecoration(border: Border.all(color: theme.chromeBorder, width: 2), color: _isCached ? const Color(0xFF10B981) : theme.bg),
                      child: _isSavingOffline
                          ? SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: theme.chromeRed))
                          : _isCached
                              ? const FaIcon(FontAwesomeIcons.check, size: 13.5, color: Colors.white)
                              : FaIcon(FontAwesomeIcons.cloudDownload, size: 13.5, color: theme.fg),
                    ),
                  ),
                  InkWell(
                    onTap: _shareWhatsAppFirst,
                    child: Container(width: 38, height: 40, alignment: Alignment.center,
                      decoration: BoxDecoration(border: Border.all(color: theme.chromeBorder, width: 2), color: theme.bg),
                      child: FaIcon(FontAwesomeIcons.shareNodes, size: 13.5, color: theme.fg),
                    ),
                  ),
                ],
              ),
            ),
          ],
          floating: false,
          pinned: true,
        ),
        SliverToBoxAdapter(
          child: Center(
            child: Container(
              constraints: const BoxConstraints(maxWidth: readerMaxWidth),
              padding: EdgeInsets.fromLTRB(
                MediaQuery.of(context).padding.left + 22,
                14,
                MediaQuery.of(context).padding.right + 22,
                50,
              ),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                // READER MODE eyebrow banner
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(color: theme.chromeRed.withOpacity(0.1), borderRadius: BorderRadius.circular(3)),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    Container(width: 3, height: 14, color: theme.chromeRed),
                    const SizedBox(width: 7),
                    Text('READER MODE', style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 10, letterSpacing: 1.5, color: theme.chromeRed)),
                    const SizedBox(width: 9),
                    Text('${widget.post.readingTimeMinutes ?? rmu.computeReadingTimeMinutes(widget.post.content ?? widget.post.contentHtml)} MIN READ',
                      style: GoogleFonts.inter(fontWeight: FontWeight.w800, fontSize: 10, letterSpacing: 1.0, color: theme.fg.withOpacity(0.65))),
                  ]),
                ),
                const SizedBox(height: 18),
                // CATEGORY CHIPS (compact)
                if ((widget.post.categories ?? <CategoryResponse>[]).isNotEmpty)
                  Wrap(spacing: 6, runSpacing: 6, children: [
                    for (final c in widget.post.categories ?? <CategoryResponse>[])
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4.5),
                        decoration: BoxDecoration(color: theme.chromeRed, border: Border.all(color: theme.chromeBorder, width: 1.6)),
                        child: Text(c.name.toString().toUpperCase(),
                          style: GoogleFonts.inter(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 9.5, letterSpacing: 1.3, height: 1.0)),
                      ),
                  ]),
                if ((widget.post.categories ?? <CategoryResponse>[]).isNotEmpty) const SizedBox(height: 18),
                // HEADLINE
                Text(widget.post.title, style: headlineStyle),
                const SizedBox(height: 16),
                // BYLINE
                Text.rich(
                  TextSpan(children: [
                    TextSpan(text: 'By ${widget.post.author?.name ?? 'MapMyTimes'}',
                      style: fontSpec.builder(fontSize: fontSize - 2.5, fontWeight: FontWeight.w700, height: lineHeight).copyWith(color: theme.fg)),
                    WidgetSpan(child: SizedBox(width: 10, height: fontSize, child: Center(child: Text('·', style: TextStyle(color: theme.fg.withOpacity(0.5), fontWeight: FontWeight.w900, fontSize: fontSize - 3))))),
                    TextSpan(text: _fmtDate(widget.post.publishedAt ?? widget.post.createdAt),
                      style: fontSpec.builder(fontSize: fontSize - 2.5, fontWeight: FontWeight.w500, height: lineHeight).copyWith(color: theme.fg.withOpacity(0.7))),
                  ]),
                ),
                const SizedBox(height: 8),
                Divider(color: theme.chromeBorder.withOpacity(0.25), thickness: 1, height: 20),
                const SizedBox(height: 10),
                // EXCERPT (deck) — italic serif
                if ((widget.post.excerpt ?? '').isNotEmpty) ...[
                  Text(widget.post.excerpt!,
                    style: fontSpec.builder(fontSize: fontSize - 0.5, fontWeight: FontWeight.w400, height: lineHeight)
                        .copyWith(color: theme.fg.withOpacity(0.8), fontStyle: FontStyle.italic)),
                  const SizedBox(height: 20),
                ],
                // HERO PHOTO CAROUSEL (Instagram-style swipeable, top)
                _HeroCarousel(items: _collectHeroImages(), dark: theme.bg.computeLuminance() < 0.5),
                if (_collectHeroImages().isNotEmpty) const SizedBox(height: 22),
                // TABLE OF CONTENTS ACCORDION (TOC) — backend dynamic entries
                if ((widget.post.tableOfContents ?? <TocEntry>[]).isNotEmpty) ...[
                  _TocAccordion(
                    entries: widget.post.tableOfContents ?? <TocEntry>[],
                    dark: theme.bg.computeLuminance() < 0.5,
                    scrollController: _scrollCtrl,
                  ),
                  const SizedBox(height: 22),
                ],
                // VIDEO EMBEDS (aspect ratios preserved, centered, narrow)
                if (widget.post.youtubeVideoId != null) ...[
                  _YoutubeEmbed(videoId: widget.post.youtubeVideoId!, videoUrl: widget.post.videoUrl ?? widget.post.shortVideoUrl ?? ''),
                  const SizedBox(height: 20),
                ] else if (widget.post.instagramMediaId != null) ...[
                  _InstagramEmbed(mediaId: widget.post.instagramMediaId!,
                    videoUrl: widget.post.videoUrl ?? widget.post.shortVideoUrl ?? '',
                    isReel: widget.post.isInstagramReel),
                  const SizedBox(height: 20),
                ],
                // BODY — with reader font + line height
                ...paragraphs,
                const SizedBox(height: 28),
                Divider(color: theme.chromeBorder.withOpacity(0.2), thickness: 1, height: 30),
                // Share / Save row (end of article)
                Row(children: [
                  Expanded(child: InkWell(
                    onTap: _shareWhatsAppFirst,
                    child: Container(padding: const EdgeInsets.symmetric(vertical: 12),
                      decoration: BoxDecoration(border: Border.all(color: theme.chromeBorder, width: 1.8), color: theme.bg),
                      alignment: Alignment.center,
                      child: Row(mainAxisSize: MainAxisSize.min, children: [
                        FaIcon(FontAwesomeIcons.shareNodes, color: theme.fg, size: 13),
                        const SizedBox(width: 8),
                        Text('SHARE', style: GoogleFonts.inter(fontWeight: FontWeight.w900, letterSpacing: 1.4, fontSize: 10.5, color: theme.fg)),
                      ]),
                    ),
                  )),
                  const SizedBox(width: 10),
                  Expanded(child: InkWell(
                    onTap: () => _toggleSaved(context),
                    child: Container(padding: const EdgeInsets.symmetric(vertical: 12),
                      decoration: BoxDecoration(border: Border.all(color: theme.chromeBorder, width: 1.8), color: isSavedArticle(ref, widget.postId) ? theme.chromeRed : theme.bg),
                      alignment: Alignment.center,
                      child: Row(mainAxisSize: MainAxisSize.min, children: [
                        FaIcon(isSavedArticle(ref, widget.postId) ? FontAwesomeIcons.solidBookmark : FontAwesomeIcons.bookmark,
                          color: isSavedArticle(ref, widget.postId) ? Colors.white : theme.fg, size: 13),
                        const SizedBox(width: 8),
                        Text('SAVE', style: GoogleFonts.inter(fontWeight: FontWeight.w900, letterSpacing: 1.4, fontSize: 10.5,
                          color: isSavedArticle(ref, widget.postId) ? Colors.white : theme.fg)),
                      ]),
                    ),
                  )),
                ]),
                const SizedBox(height: 30),
                // Author Bio Card (WRITTEN BY) — Reader themed (light/dark bg auto)
                _AuthorCard(
                  author: widget.post.author,
                  dark: theme.bg.computeLuminance() < 0.5,
                  onMoreStories: () {
                    final name = widget.post.author?.displayName ?? widget.post.author?.name;
                    if (name?.isNotEmpty == true) {
                      context.push('/section/author?name=${Uri.encodeComponent(name!)}');
                    }
                  },
                ),
                const SizedBox(height: 26),
                // Comments Section Placeholder (Reader Mode)
                _CommentsSection(
                  count: widget.post.commentCount ?? 0,
                  dark: theme.bg.computeLuminance() < 0.5,
                  onSignIn: () => context.push('/auth?redirect=article-${widget.slug}'),
                  onJoin: () => context.push('/auth?register=1&redirect=article-${widget.slug}'),
                ),
                const SizedBox(height: 32),
                Center(
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    Container(width: 3, height: 12, color: theme.chromeRed),
                    const SizedBox(width: 8),
                    Text('END OF STORY', style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 10.5, letterSpacing: 1.6, color: theme.fg.withOpacity(0.55))),
                  ]),
                ),
                const SizedBox(height: 20),
                Center(
                  child: Text('© ${DateTime.now().year} MAPMYTOUR LLP, India',
                    style: GoogleFonts.inter(fontSize: 10.5, color: theme.fg.withOpacity(0.45), fontWeight: FontWeight.w600)),
                ),
              ]),
            ),
          ),
        ),
      ],
    );
  }

  bool isSavedArticle(WidgetRef ref, String postId) =>
      ref.watch(isArticleSavedProvider(postId));

  List<Widget> _renderBodyReader({
    required rmu.ReaderFontSpec fontSpec,
    required double fontSize,
    required double lineHeight,
    required Color fg,
  }) {
    final dark = fg.computeLuminance() < 0.5;
    final List<Widget> out = <Widget>[];
    if (widget.post.contentBlocks != null && widget.post.contentBlocks!.isNotEmpty) {
      final cbWidgets = _renderContentBlocks(
        widget.post.contentBlocks!,
        dark: dark,
        readerMode: true,
        fg: fg,
        fontSize: fontSize,
        lineHeight: lineHeight,
      );
      out.addAll(cbWidgets);
      if (out.isNotEmpty) return out;
    }

    final raw = (_cachedOverride?.content ?? widget.post.content ?? widget.post.contentHtml ?? '').toString();
    if (raw.isEmpty) return <Widget>[];
    final paragraphs = raw.split(RegExp(r'\n{2,}|<br\s*/?>')).where((s) => s.trim().isNotEmpty).toList(growable: false);
    final highlightColor = const Color(0xFFE31E24).withOpacity(0.18);

    List<Map<String, dynamic>> highlightsForParagraph(int pIdx) {
      return _highlights.where((h) {
        final v = h['paragraphIndex'] ?? h['paragraph_index'];
        return (v is int ? v : int.tryParse(v?.toString() ?? '-1') ?? -1) == pIdx;
      }).toList(growable: false);
    }

    TextSpan buildHighlightedSpan({
      required String text,
      required int paragraphIndex,
      required TextStyle baseStyle,
    }) {
      final hls = highlightsForParagraph(paragraphIndex);
      if (hls.isEmpty) {
        return TextSpan(text: text, style: baseStyle);
      }
      final ranges = hls.map((h) {
        final cs = h['charStart'] ?? h['char_start'];
        final ce = h['charEnd'] ?? h['char_end'];
        final s = cs is int ? cs : (int.tryParse(cs?.toString() ?? '0') ?? 0);
        final e = ce is int ? ce : (int.tryParse(ce?.toString() ?? '0') ?? 0);
        final hid = (h['id'] ?? '').toString();
        final exc = (h['excerpt'] ?? '').toString();
        return (start: s.clamp(0, text.length), end: e.clamp(0, text.length), id: hid, excerpt: exc, h: h);
      }).where((r) => r.start < r.end && r.start >= 0).toList(growable: false)
        ..sort((a, b) => a.start.compareTo(b.start));

      if (ranges.isEmpty) return TextSpan(text: text, style: baseStyle);

      final children = <InlineSpan>[];
      int cursor = 0;
      for (final r in ranges) {
        if (cursor < r.start) {
          children.add(TextSpan(text: text.substring(cursor, r.start), style: baseStyle));
        }
        final hiStyle = baseStyle.copyWith(backgroundColor: highlightColor);
        final spanText = text.substring(r.start, r.end);
        children.add(TextSpan(
          text: spanText,
          style: hiStyle,
          recognizer: TapGestureRecognizer()
            ..onTap = () {
              showDialog<void>(
                context: context,
                builder: (dCtx) => AlertDialog(
                  title: Text('Highlight', style: GoogleFonts.archivoBlack(fontSize: 16)),
                  content: Text(r.excerpt.isEmpty ? spanText : r.excerpt,
                    style: GoogleFonts.inter(fontSize: 14, color: MmtColors.ink800)),
                  actions: [
                    TextButton(onPressed: () => Navigator.pop(dCtx), child: const Text('CLOSE')),
                    TextButton(
                      onPressed: () {
                        Navigator.pop(dCtx);
                        unawaited(_deleteHighlight(r.id));
                      },
                      style: TextButton.styleFrom(foregroundColor: MmtColors.news),
                      child: const Text('REMOVE HIGHLIGHT'),
                    ),
                  ],
                ),
              );
            },
        ));
        cursor = r.end;
      }
      if (cursor < text.length) {
        children.add(TextSpan(text: text.substring(cursor), style: baseStyle));
      }
      return TextSpan(children: children, style: baseStyle);
    }

    for (int pIdx = 0; pIdx < paragraphs.length; pIdx++) {
      final text = paragraphs[pIdx];
      final stripped = rmu.stripHtmlAndMarkdown(text.trim());
      if (stripped.isEmpty) continue;
      final isHeading = text.trim().startsWith(RegExp(r'^#{1,4}\s')) || text.trim().startsWith(RegExp(r'<h[1-4]', caseSensitive: false));
      final displayText = stripped.replaceFirstMapped(RegExp(r'^#{1,4}\s'), (_) => '');
      final baseFontSize = isHeading ? fontSize + 3 : fontSize;
      final weight = isHeading ? FontWeight.w800 : FontWeight.w400;
      final baseStyle = fontSpec.builder(fontSize: baseFontSize, fontWeight: weight, height: lineHeight).copyWith(color: fg);

      final selectable = SelectableText.rich(
        TextSpan(
          children: [
            buildHighlightedSpan(
              text: displayText,
              paragraphIndex: pIdx,
              baseStyle: baseStyle,
            ),
          ],
        ),
        cursorColor: MmtColors.news,
        selectionControls: materialTextSelectionControls,
        contextMenuBuilder: (ctx, selectable) {
          final items = <ContextMenuButtonItem>[];
          if (selectable.currentTextEditingValue.selection.isValid &&
              !selectable.currentTextEditingValue.selection.isCollapsed) {
            items.add(ContextMenuButtonItem(
              label: 'Highlight',
              onPressed: () {
                final sel = selectable.currentTextEditingValue.selection;
                final start = sel.start;
                final end = sel.end;
                if (start < 0 || end <= start) return;
                final excerpt = displayText.substring(start.clamp(0, displayText.length), end.clamp(0, displayText.length));
                unawaited(_createHighlight(
                  paragraphIndex: pIdx,
                  charStart: start,
                  charEnd: end,
                  excerpt: excerpt,
                ));
              },
            ));
          }
          final highlightIds = highlightsForParagraph(pIdx).map((h) => (h['id'] ?? '').toString()).toSet();
          final sel = selectable.currentTextEditingValue.selection;
          if (sel.isValid && !sel.isCollapsed) {
            final s = sel.start.clamp(0, displayText.length);
            final e = sel.end.clamp(0, displayText.length);
            for (final h in highlightsForParagraph(pIdx)) {
              final cs = h['charStart'] ?? h['char_start'];
              final ce = h['charEnd'] ?? h['char_end'];
              final hStart = cs is int ? cs : (int.tryParse(cs?.toString() ?? '-1') ?? -1);
              final hEnd = ce is int ? ce : (int.tryParse(ce?.toString() ?? '-1') ?? -1);
              final overlap = !(e <= hStart || s >= hEnd);
              final hid = (h['id'] ?? '').toString();
              if (overlap && hid.isNotEmpty && highlightIds.contains(hid)) {
                items.add(ContextMenuButtonItem(
                  label: 'Remove Highlight',
                  onPressed: () => unawaited(_deleteHighlight(hid)),
                ));
                break;
              }
            }
          }
          if (selectable.copyEnabled) {
            items.add(ContextMenuButtonItem(
              type: ContextMenuButtonType.copy,
              onPressed: () {
                final v = selectable.currentTextEditingValue;
                Clipboard.setData(ClipboardData(text: v.selection.textInside(v.text)));
              },
            ));
          }
          if (selectable.selectAllEnabled) {
            items.add(ContextMenuButtonItem(
              type: ContextMenuButtonType.selectAll,
              onPressed: () {
                final v = selectable.currentTextEditingValue;
                selectable.userUpdateTextEditingValue(
                  v.copyWith(selection: TextSelection(baseOffset: 0, extentOffset: v.text.length)),
                  SelectionChangedCause.toolbar,
                );
              },
            ));
          }
          return AdaptiveTextSelectionToolbar.buttonItems(
            anchors: selectable.contextMenuAnchors,
            buttonItems: items,
          );
        },
      );

      out.add(Padding(
        padding: EdgeInsets.only(bottom: isHeading ? 10 : 16),
        child: selectable,
      ));
    }
    return out;
  }


  Widget _fontBtn(BuildContext c, String label, VoidCallback onTap, {bool fill = false, required bool dark}) {
    return InkWell(
      onTap: onTap,
      child: Container(
        constraints: const BoxConstraints(minWidth: 36),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border.all(color: dark ? Colors.white70 : MmtColors.ink950, width: 1.8),
          color: fill ? (dark ? Colors.white : MmtColors.ink950) : (dark ? MmtColors.ink900 : Colors.white),
        ),
        child: Text(label, style: GoogleFonts.inter(fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 0.6, color: fill ? (dark ? MmtColors.ink950 : Colors.white) : (dark ? Colors.white : MmtColors.ink950), height: 1.0)),
      ),
    );
  }

  List<Widget> _renderBody(BlogPostResponse p, BuildContext context, double scale, bool dark) {
    if (p.contentBlocks != null && p.contentBlocks!.isNotEmpty) {
      final out = <Widget>[];
      out.addAll(_renderContentBlocks(p.contentBlocks!, dark: dark, readerMode: false));
      final raw = (p.content ?? p.contentHtml ?? '').toString().trim();
      if (raw.isNotEmpty && !RegExp(r'<[a-zA-Z]').hasMatch(raw)) {
        final rawSplits = raw.split(RegExp(r'\n\s*\n\s*')).where((s) => s.trim().length > 2).toList();
        if (rawSplits.length > 2) {
          out.add(const SizedBox(height: 16));
          for (int i = 0; i < rawSplits.length; i++) {
            final s = rawSplits[i].trim();
            if (s.isEmpty) continue;
            if (out.isEmpty) {
              out.add(Padding(
                padding: const EdgeInsets.only(bottom: 14),
                child: _DropCapParagraph(text: s, dark: dark),
              ));
            } else {
              out.add(Padding(
                padding: const EdgeInsets.only(bottom: 14),
                child: Text(s, style: GoogleFonts.inter(fontSize: 15, height: 1.75, color: dark ? Colors.white70 : MmtColors.ink850, fontWeight: FontWeight.w400)),
              ));
            }
          }
        }
      }
      if (out.isNotEmpty) return out;
    }
    final raw = (p.content ?? p.contentHtml ?? '').toString();
    if (raw.isEmpty) return <Widget>[];
    return _parseHtmlBlocks(raw, dark: dark, scale: scale, readerMode: false);
  }

  List<Widget> _renderContentBlocks(List<Map<String, dynamic>> blocks, {required bool dark, required bool readerMode, Color? fg, double? fontSize, double? lineHeight}) {
    final out = <Widget>[];
    for (int i = 0; i < blocks.length; i++) {
      final b = blocks[i];
      final type = ((b['type'] ?? b['blockType'] ?? b['kind'] ?? 'text') as String?).toString().toLowerCase();
      final title = ((b['title'] ?? b['heading'] ?? b['headline'] ?? b['subtitle'] ?? '') as String?).toString().trim();
      final sub = ((b['subtitle'] ?? b['description'] ?? b['caption'] ?? '') as String?).toString().trim();
      final contentRaw = b['content'] ?? b['text'] ?? b['body'] ?? b['paragraph'] ?? b['description'] ?? '';
      final String contentText = contentRaw is List
          ? contentRaw.map((x) { if (x is Map) return (x['text'] ?? x['bullet'] ?? x['content'] ?? x['item'] ?? '').toString(); return x.toString(); }).where((s) => s.trim().isNotEmpty).join('\n')
          : contentRaw.toString();
      // images extraction — backend may send List OR single {url:..} Map
      dynamic _imgVal = b['images'] ?? b['image'] ?? b['gallery'] ?? b['photos'] ?? b['media'];
      List? imagesRaw;
      if (_imgVal is List) {
        imagesRaw = _imgVal;
      } else if (_imgVal is Map) {
        // Wrap single image map into a list
        imagesRaw = <dynamic>[_imgVal];
      } else {
        imagesRaw = null;
      }
      final imgList = <(String url, String? cap)>[];
      if (imagesRaw != null) {
        for (final im in imagesRaw) {
          if (im is Map) {
            final u = (im['url'] ?? im['mediaUrl'] ?? im['imageUrl'] ?? im['src'] ?? im['fileUrl'] ?? '').toString();
            final c = (im['caption'] ?? im['alt'] ?? im['title'] ?? '').toString();
            if (u.isNotEmpty) imgList.add((u, c.isEmpty ? null : c));
          } else if (im is String && im.isNotEmpty) {
            imgList.add((im, null));
          }
        }
      }
      // Single cover image in block fallback
      if (imgList.isEmpty && (b.containsKey('imageUrl') || b.containsKey('coverImage') || b.containsKey('banner') || b.containsKey('photo'))) {
        final u = (b['imageUrl'] ?? b['coverImage'] ?? b['banner'] ?? b['photo'] ?? '').toString();
        if (u.isNotEmpty) imgList.add((u, null));
      }
      // bullets extraction — backend may send List OR legacy map wrapper
      dynamic _bulVal = b['bulletItems'] ?? b['bullets'] ?? b['items'] ?? b['points'] ?? b['list'];
      List? bulletsRaw;
      if (_bulVal is List) {
        bulletsRaw = _bulVal;
      } else if (_bulVal is Map) {
        // try extracting common list keys from map wrapper
        bulletsRaw = (_bulVal['items'] ?? _bulVal['list'] ?? _bulVal['data'] ?? _bulVal['bullets']) as List?;
      } else {
        bulletsRaw = null;
      }
      final bullets = <String>[];
      if (bulletsRaw != null) {
        for (final bi in bulletsRaw) {
          if (bi is Map) {
            final s = (bi['text'] ?? bi['content'] ?? bi['item'] ?? bi['bullet'] ?? '').toString().trim();
            if (s.isNotEmpty) bullets.add(s);
          } else if (bi is String && bi.trim().isNotEmpty) {
            bullets.add(bi.trim());
          }
        }
      }
      // Section separator / spacing
      final bool isFirstBlock = out.isEmpty;

      // ========== TYPE: headingImageBulletSection ==========
      if (title.isNotEmpty) {
        final lv = (type.contains('heading') || type.startsWith('h')) ? (int.tryParse(type.replaceAll(RegExp(r'[^0-9]'), '')) ?? 2) : 2;
        final double sz = switch(lv) { 1 => 26, 2 => 22, 3 => 19, _ => 17 };
        out.add(Padding(
          padding: EdgeInsets.fromLTRB(0, isFirstBlock ? 0 : 22, 0, 10),
          child: Text(title, style: GoogleFonts.archivoBlack(
            color: readerMode ? (fg ?? MmtColors.ink950) : (dark ? Colors.white : MmtColors.ink950),
            fontSize: sz * (readerMode && fontSize != null ? fontSize / 18 : 1.0),
            height: 1.18,
            letterSpacing: -0.2,
          )),
        ));
      } else if (sub.isNotEmpty) {
        out.add(Padding(
          padding: EdgeInsets.fromLTRB(0, isFirstBlock ? 0 : 14, 0, 8),
          child: Text(sub, style: GoogleFonts.inter(
            color: readerMode ? (fg ?? MmtColors.ink850) : (dark ? Colors.white70 : MmtColors.ink800),
            fontSize: readerMode ? ((fontSize ?? 15) - 1) : 14.5,
            fontWeight: FontWeight.w600,
            height: (lineHeight ?? 1.6).toDouble(),
            fontStyle: FontStyle.italic,
          )),
        ));
      }

      // Render images (1-2 = inline single; 3+ = small photo row)
      for (final im in imgList.take(4)) {
        out.add(_InlineArticleImage(src: im.$1, caption: im.$2, dark: dark));
        out.add(const SizedBox(height: 14));
      }

      // Bullets list
      if (bullets.isNotEmpty) {
        out.add(Container(
          margin: const EdgeInsets.fromLTRB(0, 4, 0, 14),
          padding: const EdgeInsets.only(left: 4),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: List.generate(bullets.length, (bi) {
            final txt = bullets[bi];
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                SizedBox(width: 22, child: Text('•', style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 14, color: readerMode ? (fg ?? MmtColors.news) : MmtColors.news, height: 1.55))),
                Expanded(child: Text(txt, style: GoogleFonts.inter(
                  fontSize: readerMode ? (fontSize ?? 15) : 15,
                  height: (lineHeight ?? 1.75).toDouble(),
                  color: readerMode ? (fg ?? MmtColors.ink850) : (dark ? Colors.white70 : MmtColors.ink850),
                  fontWeight: FontWeight.w400,
                ))),
              ]),
            );
          })),
        ));
      }

      // Paragraph text
      if (contentText.trim().isNotEmpty) {
        final paras = contentText.split(RegExp(r'\n\s*\n\s*')).where((s) => s.trim().length > 1).toList();
        if (paras.isEmpty) paras.add(contentText.trim());
        for (int pi = 0; pi < paras.length; pi++) {
          final pure = paras[pi].trim();
          if (pure.isEmpty) continue;
          final bool firstPara = out.isEmpty && pi == 0;
          Widget body;
          TextStyle baseStyle;
          if (readerMode) {
            baseStyle = TextStyle(fontFamily: fontMap[1], color: fg ?? MmtColors.ink850, fontSize: fontSize ?? 15, height: (lineHeight ?? 1.75).toDouble(), fontWeight: FontWeight.w400);
          } else {
            baseStyle = GoogleFonts.inter(fontSize: 15, height: 1.75, color: dark ? Colors.white70 : MmtColors.ink850, fontWeight: FontWeight.w400);
          }
          if (firstPara && !readerMode) {
            body = _DropCapParagraph(text: pure, dark: dark);
          } else if (firstPara && readerMode) {
            body = _DropCapParagraph(text: pure, dark: dark, readerMode: true, fg: fg, fontSize: fontSize, lineHeight: lineHeight);
          } else {
            body = Text(pure, style: baseStyle);
          }
          out.add(Padding(padding: const EdgeInsets.only(bottom: 14), child: body));
        }
      }

      // Quote block
      if (type.contains('quote') || type == 'pullQuote' || type == 'pullquote') {
        final q = (title.isNotEmpty ? title : contentText).trim();
        if (q.isNotEmpty) {
          out.add(Container(
            margin: const EdgeInsets.fromLTRB(0, 10, 0, 18),
            decoration: BoxDecoration(
              color: readerMode ? (fg ?? MmtColors.news50).withOpacity(0.05) : MmtColors.news50,
              border: Border(left: BorderSide(width: 6, color: MmtColors.news)),
            ),
            padding: const EdgeInsets.fromLTRB(18, 16, 18, 16),
            child: Text(q, style: GoogleFonts.inter(
              fontSize: readerMode ? ((fontSize ?? 15) * 1.02) : 16,
              height: (lineHeight ?? 1.55).toDouble(),
              color: readerMode ? (fg ?? MmtColors.ink850) : (dark ? Colors.white70 : MmtColors.ink800),
              fontStyle: FontStyle.italic,
              fontWeight: FontWeight.w500,
            )),
          ));
        }
      }

      // HR block / divider
      if (type == 'divider' || type == 'hr' || type == 'separator') {
        out.add(Container(margin: const EdgeInsets.fromLTRB(0, 22, 0, 22), height: 2, width: double.infinity, color: MmtColors.ink950));
      }
    }
    return out;
  }

  List<Widget> _parseHtmlBlocks(String html, {required bool dark, required double scale, required bool readerMode, Color? fg, double? fontSize, double? lineHeight, List<Map<String, dynamic>> Function(int)? highlightProvider, void Function(bool)? onHighlightStateChange}) {
    if (html.trim().isEmpty) return <Widget>[];
    final String work = html.trim();
    final hasHtml = RegExp(r'<[a-zA-Z/]').hasMatch(work);
    if (!hasHtml) {
      final paras = work.split(RegExp(r'\n\s*\n\s*')).where((s) => s.trim().length > 2).toList(growable: false);
      if (paras.length > 1) {
        final out = <Widget>[];
        for (int pi = 0; pi < paras.length; pi++) {
          final pure = paras[pi].trim();
          if (pure.isEmpty) continue;
          final bool firstPara = pi == 0;
          final List<Map<String, dynamic>>? hls = (highlightProvider == null) ? null : highlightProvider(pi);
          TextStyle baseStyle;
          if (readerMode) {
            baseStyle = TextStyle(fontFamily: fontMap[1], color: fg ?? MmtColors.ink850, fontSize: fontSize ?? 15, height: (lineHeight ?? 1.75).toDouble(), fontWeight: FontWeight.w400);
          } else {
            baseStyle = GoogleFonts.inter(fontSize: 15, height: 1.75, color: dark ? Colors.white70 : MmtColors.ink850, fontWeight: FontWeight.w400);
          }
          Widget body;
          if (firstPara && !readerMode && (hls == null || hls.isEmpty)) {
            body = _DropCapParagraph(text: pure, dark: dark);
          } else if (firstPara && readerMode && (hls == null || hls.isEmpty)) {
            body = _DropCapParagraph(text: pure, dark: dark, readerMode: true, fg: fg, fontSize: fontSize, lineHeight: lineHeight);
          } else if (hls != null && hls.isNotEmpty) {
            final spans = _applyHighlightsToText(pure, hls, baseStyle, const Color(0xFFE31E24).withOpacity(0.22));
            body = RichText(text: TextSpan(children: spans, style: baseStyle));
          } else {
            body = Text(pure, style: baseStyle);
          }
          out.add(Padding(padding: const EdgeInsets.only(bottom: 14), child: body));
        }
        return out;
      }
    }
    final matches = RegExp(
      r'<(h[1-6]|p|div|figure|img|blockquote|table|hr|ul|ol|iframe|figcaption)\b[^>]*>',
      caseSensitive: false,
      multiLine: false,
    ).allMatches(work);

    final blocks = <Map<String, dynamic>>[];
    final startIndices = matches.map((m) => m.start).toList()..sort();
    void emitBlock(int start, int end) {
      if (end <= start) return;
      final slice = work.substring(start, end).trim();
      if (slice.isEmpty) return;
      final firstTag = RegExp(r'^<\s*([a-zA-Z0-9]+)\b').firstMatch(slice)?.group(1)?.toLowerCase();
      if (firstTag != null) {
        blocks.add({'tag': firstTag, 'html': slice});
      } else {
        blocks.add({'tag': 'p', 'html': '<p>$slice</p>'});
      }
    }
    int lastCursor = 0;
    for (final s in startIndices) {
      if (s > lastCursor) emitBlock(lastCursor, s);
      lastCursor = s;
    }
    if (lastCursor < work.length) emitBlock(lastCursor, work.length);

    final out = <Widget>[];
    final isFirstPara = !readerMode;
    int paraIdx = 0;
    final theme = ThemeData.fallback();
    final Brightness brightness = dark ? Brightness.dark : Brightness.light;
    for (int bIdx = 0; bIdx < blocks.length; bIdx++) {
      final block = blocks[bIdx];
      final tag = block['tag'].toString();
      final blockHtml = block['html'].toString();
      bool paraConsumed = false;
      switch (tag) {
        case 'h1': case 'h2': case 'h3': case 'h4': case 'h5': case 'h6':
          final int lv = int.tryParse(tag.substring(1)) ?? 2;
          final txt = _stripHtml(_extractInnerTag(blockHtml, tag.toLowerCase()));
          if (txt.isEmpty) break;
          final double size = switch(lv) {
            1 => 30, 2 => 25, 3 => 21, 4 => 18, 5 => 16.5, _ => 15,
          };
          out.add(Padding(
            padding: EdgeInsets.fromLTRB(0, bIdx == 0 ? 0 : 18, 0, 10),
            child: Text(txt,
              key: ValueKey('h$lv-${_slug(txt)}'),
              style: GoogleFonts.archivoBlack(
                color: readerMode ? (fg ?? MmtColors.ink950) : (dark ? Colors.white : MmtColors.ink950),
                fontSize: size * (readerMode ? 0.95 : 1.0) * (fontSize == null ? 1 : fontSize / 18),
                height: 1.18,
                letterSpacing: -0.2,
              )),
          ));
          break;
        case 'figure': case 'img':
          final imgSrc = _extractImageSrc(blockHtml);
          if (imgSrc != null && imgSrc.isNotEmpty) {
            final cap = RegExp(r'<figcaption[^>]*>([\s\S]*?)</figcaption>', caseSensitive: false).firstMatch(blockHtml)?.group(1);
            out.add(_InlineArticleImage(src: imgSrc, caption: cap == null ? null : _stripHtml(cap), dark: dark));
            out.add(const SizedBox(height: 18));
          }
          break;
        case 'blockquote':
          final q = _stripHtml(_extractInnerTag(blockHtml, 'blockquote'));
          if (q.isNotEmpty) {
            out.add(Container(
              margin: const EdgeInsets.fromLTRB(0, 10, 0, 18),
              decoration: BoxDecoration(
                color: readerMode ? (fg ?? MmtColors.news50).withOpacity(0.05) : (MmtColors.news50),
                border: Border(left: BorderSide(width: 6, color: MmtColors.news)),
              ),
              padding: const EdgeInsets.fromLTRB(18, 16, 18, 16),
              child: Text(q, style: GoogleFonts.inter(
                fontSize: readerMode ? ((fontSize ?? 15) * 1.02) : 16,
                height: (lineHeight ?? 1.55).toDouble(),
                color: readerMode ? (fg ?? MmtColors.ink850) : (dark ? Colors.white70 : MmtColors.ink800),
                fontStyle: FontStyle.italic,
                fontWeight: FontWeight.w500,
              )),
            ));
          }
          break;
        case 'hr':
          out.add(Container(
            margin: const EdgeInsets.fromLTRB(0, 22, 0, 22),
            height: 2, width: double.infinity, color: MmtColors.ink950,
          ));
          break;
        case 'ul': case 'ol':
          final bullets = _extractListItems(blockHtml);
          if (bullets.isNotEmpty) {
            out.add(Container(
              margin: const EdgeInsets.fromLTRB(0, 6, 0, 16),
              padding: const EdgeInsets.only(left: 4),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: List.generate(bullets.length, (i) {
                final itemText = bullets[i];
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    SizedBox(width: 22,
                      child: Text(tag == 'ol' ? '${i+1}.' : '•',
                        style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 14,
                          color: readerMode ? (fg ?? MmtColors.news) : MmtColors.news, height: 1.55))),
                    Expanded(child: Text(itemText, style: GoogleFonts.inter(
                      fontSize: readerMode ? (fontSize ?? 15) : 15,
                      height: (lineHeight ?? 1.75).toDouble(),
                      color: readerMode ? (fg ?? MmtColors.ink850) : (dark ? Colors.white70 : MmtColors.ink850), fontWeight: FontWeight.w400,
                    ))),
                  ]),
                );
              })),
            ));
          }
          break;
        case 'table':
          final tbl = _parseHtmlTable(blockHtml);
          if (tbl != null && tbl.isNotEmpty) {
            out.add(SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Container(
                margin: const EdgeInsets.fromLTRB(0, 8, 0, 18),
                decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2)),
                child: Column(mainAxisSize: MainAxisSize.min, children: List.generate(tbl.length, (ri) {
                  final row = tbl[ri];
                  final bool head = ri == 0;
                  return DecoratedBox(
                    decoration: BoxDecoration(color: head ? MmtColors.ink950 : (ri.isEven ? (dark ? MmtColors.ink800.withOpacity(0.15) : MmtColors.news50.withOpacity(0.35)) : Colors.transparent), border: Border(bottom: BorderSide(color: MmtColors.ink950, width: 1))),
                    child: Row(mainAxisSize: MainAxisSize.min, children: List.generate(row.length, (ci) {
                      final cellText = row[ci];
                      return Container(
                        constraints: const BoxConstraints(minWidth: 88),
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                        decoration: BoxDecoration(border: Border(right: ci == row.length - 1 ? BorderSide.none : BorderSide(color: MmtColors.ink950, width: 1))),
                        child: Text(cellText, style: GoogleFonts.inter(fontWeight: head ? FontWeight.w900 : FontWeight.w500, fontSize: 12.5, color: head ? Colors.white : (dark ? Colors.white70 : MmtColors.ink850), height: 1.45)),
                      );
                    })),
                  );
                })),
              ),
            ));
          }
          break;
        case 'iframe':
          final yt = _extractYouTubeIframe(blockHtml);
          if (yt != null) {
            out.add(Padding(
              padding: const EdgeInsets.fromLTRB(0, 4, 0, 18),
              child: _YoutubeThumbCta(videoId: yt, videoUrl: 'https://www.youtube.com/watch?v=$yt'),
            ));
          }
          break;
        case 'p': case 'div': default:
          final inner = _extractInnerTag(blockHtml, tag == 'div' ? 'div' : 'p');
          final pure = _stripHtml(inner);
          if (pure.isEmpty) break;
          final bool firstPara = paraConsumed ? false : (paraIdx == 0);
          paraConsumed = true;
          if (firstPara) paraIdx++;
          final List<Map<String, dynamic>>? hls = (highlightProvider == null) ? null : highlightProvider(paraIdx - 1);
          TextStyle baseStyle;
          if (readerMode) {
            baseStyle = TextStyle(fontFamily: fontMap[(readerMode ? 1 : 0)], color: fg ?? MmtColors.ink850, fontSize: fontSize ?? 15, height: (lineHeight ?? 1.75).toDouble(), fontWeight: FontWeight.w400);
          } else {
            baseStyle = GoogleFonts.inter(fontSize: 15, height: 1.75, color: dark ? Colors.white70 : MmtColors.ink850, fontWeight: FontWeight.w400);
          }
          Widget body;
          if (firstPara && !readerMode) {
            body = _DropCapParagraph(text: pure, dark: dark);
          } else if (firstPara && readerMode && (hls == null || hls.isEmpty)) {
            body = _DropCapParagraph(text: pure, dark: dark, fg: fg, fontSize: fontSize, lineHeight: lineHeight, readerMode: true);
          } else if (hls != null && hls.isNotEmpty) {
            final spans = _applyHighlightsToText(pure, hls, baseStyle, const Color(0xFFE31E24).withOpacity(0.22));
            body = RichText(text: TextSpan(children: spans, style: baseStyle));
          } else {
            body = Text(pure, style: baseStyle);
          }
          out.add(Padding(padding: const EdgeInsets.only(bottom: 14), child: body));
          break;
      }
    }
    return out;
  }

  static List<List<String>>? _parseHtmlTable(String html) {
    final trRe = RegExp(r'<tr\b[^>]*>([\s\S]*?)</tr>', caseSensitive: false);
    final cellRe = RegExp(r'<t[dh]\b[^>]*>([\s\S]*?)</t[dh]>', caseSensitive: false);
    final rows = trRe.allMatches(html).map((rowMatch) {
      return cellRe.allMatches(rowMatch.group(1) ?? '').map((cm) => _stripHtmlStatic(cm.group(1) ?? '')).toList();
    }).where((r) => r.isNotEmpty).toList();
    if (rows.isEmpty) return null;
    final m = rows.map((r) => r.length).reduce((a, b) => a > b ? a : b);
    for (final r in rows) {
      while (r.length < m) r.add('');
    }
    return rows;
  }

  static final Map<int, String> fontMap = {0: 'Inter', 1: 'Playfair', 2: 'Merriweather', 3: 'Georgia', 4: 'Atkinson'};

  static List<InlineSpan> _applyHighlightsToText(String text, List<Map<String, dynamic>> hls, TextStyle base, Color hl) {
    final ranges = <({int s, int e, String id})>[];
    for (final h in hls) {
      final cs = h['charStart'] ?? h['char_start'];
      final ce = h['charEnd'] ?? h['char_end'];
      final s = cs is int ? cs : int.tryParse('$cs') ?? -1;
      final e = ce is int ? ce : int.tryParse('$ce') ?? -1;
      if (s < 0 || e <= s) continue;
      final id = (h['id'] ?? '').toString();
      ranges.add((s: s.clamp(0, text.length), e: e.clamp(0, text.length), id: id));
    }
    ranges.sort((a, b) => a.s.compareTo(b.s));
    final List<InlineSpan> out = [];
    int cursor = 0;
    for (final r in ranges) {
      if (r.s < cursor) continue;
      if (r.s > cursor) out.add(TextSpan(text: text.substring(cursor, r.s), style: base));
      final ts = TextStyle(background: Paint()..color = hl, color: base.color, fontSize: base.fontSize, height: base.height, fontWeight: base.fontWeight, fontFamily: base.fontFamily);
      out.add(TextSpan(text: text.substring(r.s, r.e), style: ts));
      cursor = r.e;
    }
    if (cursor < text.length) out.add(TextSpan(text: text.substring(cursor), style: base));
    return out;
  }

  static final RegExp _imgSrcRe = RegExp(
    r'<img\b[^>]*\bsrc\s*=\s*"([^"]+)"',
    caseSensitive: false,
  );
  static final RegExp _imgSrcRe2 = RegExp(
    r"<img\b[^>]*\bsrc\s*=\s*'([^']+)'",
    caseSensitive: false,
  );
  static String? _extractImageSrc(String html) {
    return _imgSrcRe.firstMatch(html)?.group(1) ?? _imgSrcRe2.firstMatch(html)?.group(1);
  }
  static String? _extractYouTubeIframe(String html) {
    try {
      final s = html.replaceAll('\n', ' ').replaceAll('\r', ' ');
      final re1 = RegExp(r'youtube\.com/embed/([A-Za-z0-9_-]{6,})', caseSensitive: false);
      final m1 = re1.firstMatch(s)?.group(1);
      if (m1 != null) return m1;
      final re2 = RegExp(r'youtube\.com/watch\?[^\s>]*?v=([A-Za-z0-9_-]{6,})', caseSensitive: false);
      final m2 = re2.firstMatch(s)?.group(1);
      if (m2 != null) return m2;
      final re3 = RegExp(r'youtu\.be/([A-Za-z0-9_-]{6,})', caseSensitive: false);
      final m3 = re3.firstMatch(s)?.group(1);
      if (m3 != null) return m3;
    } catch (_) {}
    return null;
  }
  static List<String> _extractListItems(String html) {
    return RegExp(r'<li\b[^>]*>([\s\S]*?)</li>', caseSensitive: false)
      .allMatches(html)
      .map((m) => _stripHtmlStatic(m.group(1) ?? ''))
      .where((s) => s.trim().isNotEmpty)
      .toList(growable: false);
  }
  static String _extractInnerTag(String html, String tag) {
    final s = html.toLowerCase();
    final tagL = tag.toLowerCase();
    final openRe = RegExp('<$tagL\\b[^>]*>', caseSensitive: false);
    final closeRe = RegExp('</$tagL\\s*>', caseSensitive: false);
    final openMatch = openRe.firstMatch(s);
    final closeMatch = closeRe.allMatches(s);
    if (openMatch == null) return html;
    int idxOpenEnd = openMatch.end;
    int idxCloseStart = html.length;
    if (closeMatch.isNotEmpty) idxCloseStart = closeMatch.last.start;
    if (idxCloseStart < idxOpenEnd) return html.substring(idxOpenEnd);
    return html.substring(idxOpenEnd, idxCloseStart);
  }

  static final RegExp _stripRE = RegExp(r'<[^>]*>', multiLine: true, caseSensitive: false);
  static String _stripHtmlStatic(String s) {
    return s.replaceAll(_stripRE, ' ')
        .replaceAll('&nbsp;', ' ')
        .replaceAll('&amp;', '&')
        .replaceAll('&quot;', '"')
        .replaceAll('&lt;', '<')
        .replaceAll('&gt;', '>')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }

  static String _stripHtml(String s) => _stripHtmlStatic(s);

  // ===========================================================================
  // HERO IMAGE CAROUSEL — multi photo swipeable (Instagram-style)
  // ===========================================================================
  List<({String url, String? caption, String? alt})> _collectHeroImages() {
    final List<({String url, String? caption, String? alt})> out = <({String url, String? caption, String? alt})>[];
    final seen = <String>{};

    void addImage(String url, {String? caption, String? alt}) {
      final u = (url ?? '').trim();
      if (u.isEmpty) return;
      final key = u.toLowerCase();
      if (!seen.add(key)) return;
      out.add((url: u, caption: caption, alt: alt));
    }

    final rawCover = (widget.post.featuredImageUrl
        ?? YouTubeUtil.thumbnailFor(widget.post.videoUrl ?? widget.post.shortVideoUrl)
        ?? '').toString().trim();
    if (rawCover.isNotEmpty) {
      final featCap = widget.post.excerpt;
      addImage(rawCover, caption: featCap, alt: widget.post.title);
    }

    final medias = widget.post.media ?? const <BlogMedia>[];
    if (medias.isNotEmpty) {
      final sorted = medias.where((m) {
        if (!m.isImage) return false;
        final u = (m.url ?? '').trim();
        return u.isNotEmpty;
      }).toList(growable: false);
      sorted.sort((a, b) {
        final sa = a.sortOrder;
        final sb = b.sortOrder;
        if (sa != null && sb != null) return sa.compareTo(sb);
        if (sa != null) return -1;
        if (sb != null) return 1;
        return 0;
      });
      for (final m in sorted) {
        final u = (m.url ?? '').trim();
        if (u.isEmpty) continue;
        addImage(u, caption: m.caption ?? m.description, alt: m.alt ?? m.caption);
      }
    }

    final youtubeThumb = YouTubeUtil.thumbnailFor(
      widget.post.videoUrl ?? widget.post.shortVideoUrl,
    );
    if (youtubeThumb != null && youtubeThumb.isNotEmpty) {
      addImage(
        youtubeThumb,
        caption: widget.post.videoUrl?.isNotEmpty == true ? 'Video thumbnail' : null,
        alt: widget.post.title,
      );
    }
    return out;
  }

  static String _fmtViews(int? v) {
    if (v == null) return '';
    if (v >= 1000000) {
      return '${(v / 1000000).toStringAsFixed(1).replaceAll('.0', '')}M';
    }
    if (v >= 1000) {
      return '${(v / 1000).toStringAsFixed(1).replaceAll('.0', '')}K';
    }
    return v.toString();
  }

  static String _fmtDate(DateTime? d) {
    if (d == null) return '';
    final now = DateTime.now();
    final diff = now.difference(d);
    if (diff.inDays > 30) {
      return '${d.day} ${_month(d.month)} ${d.year}';
    }
    if (diff.inDays > 0) return '${diff.inDays}d ago';
    if (diff.inHours > 0) return '${diff.inHours}h ago';
    return 'Just now';
  }

  static String _month(int m) {
    return const ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'][m - 1];
  }
}

// =============================================================================
// Hero Image Carousel — Instagram-style swipeable photos with page dots
// =============================================================================
class _HeroCarousel extends StatefulWidget {
  final List<({String url, String? caption, String? alt})> items;
  final bool dark;

  const _HeroCarousel({required this.items, required this.dark});

  @override
  State<_HeroCarousel> createState() => _HeroCarouselState();
}

class _HeroCarouselState extends State<_HeroCarousel> {
  final PageController _ctrl = PageController(viewportFraction: 1.0);
  int _idx = 0;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final items = widget.items;
    if (items.isEmpty) return const SizedBox.shrink();
    final dark = widget.dark;
    return Column(children: [
      ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Container(
          decoration: BoxDecoration(
            border: Border.all(color: MmtColors.ink950, width: 2),
            color: dark ? MmtColors.ink900 : MmtColors.news50,
          ),
          child: AspectRatio(
            aspectRatio: 4 / 3,
            child: Stack(children: [
              PageView.builder(
                controller: _ctrl,
                itemCount: items.length,
                onPageChanged: (i) => setState(() => _idx = i),
                itemBuilder: (_, i) {
                  final item = items[i];
                  final alt = item.alt;
                  final resolved = Env.resolveImgUrl(item.url);
                  if (resolved.isEmpty) {
                    return Container(
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: dark
                              ? [MmtColors.ink900, MmtColors.ink700]
                              : [MmtColors.news50, MmtColors.news100],
                        ),
                      ),
                      child: Center(
                        child: FaIcon(
                          FontAwesomeIcons.solidImage,
                          size: 56,
                          color: dark ? Colors.white30 : MmtColors.news300,
                        ),
                      ),
                    );
                  }
                  return Image.network(
                    resolved,
                    fit: BoxFit.cover,
                    width: double.infinity,
                    height: double.infinity,
                    semanticLabel: alt,
                    loadingBuilder: (BuildContext ctx, Widget child, ImageChunkEvent? ev) {
                      if (ev == null) return child;
                      final expected = ev.expectedTotalBytes ?? 0;
                      final progress = expected > 0
                          ? ev.cumulativeBytesLoaded / expected
                          : null;
                      return Container(
                        color: dark ? MmtColors.ink800 : MmtColors.news50,
                        child: Center(
                          child: SizedBox(
                            width: 28,
                            height: 28,
                            child: CircularProgressIndicator(
                              value: progress,
                              strokeWidth: 2.5,
                              color: MmtColors.news,
                            ),
                          ),
                        ),
                      );
                    },
                    errorBuilder: (_, __, ___) => Container(
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: dark
                              ? [MmtColors.ink900, MmtColors.ink700]
                              : [MmtColors.news50, MmtColors.news100],
                        ),
                      ),
                      child: Center(
                        child: FaIcon(
                          FontAwesomeIcons.solidImage,
                          size: 56,
                          color: dark ? Colors.white30 : MmtColors.news300,
                        ),
                      ),
                    ),
                  );
                },
              ),
              if (items.length > 1) ...[
                Positioned(
                  left: 0, right: 0, bottom: 0,
                  child: Container(
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [Colors.transparent, Colors.black54],
                      ),
                    ),
                    padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(items.length, (i) {
                        final active = i == _idx;
                        return AnimatedContainer(
                          duration: const Duration(milliseconds: 180),
                          curve: Curves.easeOutCubic,
                          margin: EdgeInsets.symmetric(horizontal: i == 0 ? 0 : 4),
                          width: active ? 22 : 8,
                          height: 8,
                          decoration: BoxDecoration(
                            color: active ? MmtColors.news : Colors.white.withValues(alpha: 0.55),
                            borderRadius: BorderRadius.circular(999),
                          ),
                        );
                      }),
                    ),
                  ),
                ),
              ],
            ]),
          ),
        ),
      ),
      if (items.isNotEmpty && items[_idx].caption?.isNotEmpty == true) ...[
        const SizedBox(height: 8),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Text(
            items[_idx].caption!,
            style: GoogleFonts.inter(
              fontSize: 12,
              height: 1.4,
              fontWeight: FontWeight.w500,
              color: dark ? Colors.white70 : MmtColors.ink600,
              fontStyle: FontStyle.italic,
            ),
          ),
        ),
      ],
    ]);
  }
}

// =============================================================================
// Inline Article Image (inside body paragraphs, between blocks)
// =============================================================================
class _InlineArticleImage extends StatelessWidget {
  final String src;
  final String? caption;
  final bool dark;
  const _InlineArticleImage({required this.src, this.caption, required this.dark});

  @override
  Widget build(BuildContext context) {
    final resolved = Env.resolveImgUrl(src);
    return Container(
      margin: const EdgeInsets.fromLTRB(0, 4, 0, 4),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        AspectRatio(
          aspectRatio: 16 / 10,
          child: Container(
            decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)]),
            child: ClipRect(
              child: resolved.isEmpty
                  ? Container(
                      decoration: BoxDecoration(gradient: LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: dark ? [MmtColors.ink900, MmtColors.ink700] : [MmtColors.news50, MmtColors.news100])),
                      child: const Center(child: FaIcon(FontAwesomeIcons.solidImage, size: 44, color: MmtColors.news300)))
                  : Image.network(
                      resolved,
                      fit: BoxFit.cover,
                      loadingBuilder: (BuildContext c, Widget child, ImageChunkEvent? ev) {
                        if (ev == null) return child;
                        final expected = ev.expectedTotalBytes ?? 0;
                        final progress = expected > 0 ? ev.cumulativeBytesLoaded / expected : null;
                        return Container(
                          decoration: BoxDecoration(gradient: LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: dark ? [MmtColors.ink900, MmtColors.ink700] : [MmtColors.news50, MmtColors.news100])),
                          child: Center(child: SizedBox(width: 26, height: 26, child: CircularProgressIndicator(strokeWidth: 2.5, value: progress, color: MmtColors.news))),
                        );
                      },
                      errorBuilder: (_, __, ___) => Container(
                        decoration: BoxDecoration(gradient: LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: dark ? [MmtColors.ink900, MmtColors.ink700] : [MmtColors.news50, MmtColors.news100])),
                        child: const Center(child: FaIcon(FontAwesomeIcons.solidImage, size: 44, color: MmtColors.news300)),
                      ),
                    ),
            ),
          ),
        ),
        if (caption != null && caption!.trim().isNotEmpty) ...[
          const SizedBox(height: 10),
          Padding(
            padding: const EdgeInsets.only(left: 2),
            child: Text(caption!.trim(), style: GoogleFonts.inter(fontSize: 11.5, color: dark ? Colors.white54 : MmtColors.ink800.withOpacity(0.6), fontStyle: FontStyle.italic, fontWeight: FontWeight.w500, height: 1.45)),
          ),
        ],
      ]),
    );
  }
}

// =============================================================================
// Drop Cap First Paragraph — large red first letter (newspaper style)
// =============================================================================
class _DropCapParagraph extends StatelessWidget {
  final String text;
  final bool dark;
  final bool readerMode;
  final Color? fg;
  final double? fontSize;
  final double? lineHeight;
  const _DropCapParagraph({required this.text, required this.dark, this.readerMode = false, this.fg, this.fontSize, this.lineHeight});

  @override
  Widget build(BuildContext context) {
    final t = text.trim();
    if (t.isEmpty) return const SizedBox.shrink();
    final firstChar = String.fromCharCode(t.codeUnitAt(0));
    final rest = t.length > 1 ? t.substring(1) : '';
    final double fs = readerMode ? (fontSize ?? 15) : 15;
    final double lh = (lineHeight ?? 1.75).toDouble();
    final dropCapSize = fs * 3.6;
    final dropCapHeight = dropCapSize * 1.05;
    return Wrap(
      crossAxisAlignment: WrapCrossAlignment.start,
      runAlignment: WrapAlignment.start,
      alignment: WrapAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(right: 6, top: 6, bottom: 4),
          child: SizedBox(
            height: dropCapHeight,
            child: Align(
              alignment: Alignment.topLeft,
              child: Text(firstChar.toUpperCase(), style: GoogleFonts.archivoBlack(
                color: MmtColors.news,
                fontSize: dropCapSize,
                height: 0.95,
                fontWeight: FontWeight.w900,
              )),
            ),
          ),
        ),
        Text(rest.trimLeft(), style: GoogleFonts.inter(
          color: readerMode ? (fg ?? MmtColors.ink850) : (dark ? Colors.white70 : MmtColors.ink850),
          fontSize: fs,
          height: lh,
          fontWeight: FontWeight.w400,
        )),
      ],
    );
  }
}

// =============================================================================
// TOC (Table of Contents) Accordion — exactly like web screenshot
// =============================================================================
class _TocAccordion extends StatefulWidget {
  final List<TocEntry> entries;
  final bool dark;
  final ScrollController? scrollController;
  const _TocAccordion({required this.entries, required this.dark, this.scrollController});

  @override
  State<_TocAccordion> createState() => _TocAccordionState();
}
class _TocAccordionState extends State<_TocAccordion> {
  bool open = false;
  @override
  Widget build(BuildContext context) {
    if (widget.entries.isEmpty) return const SizedBox.shrink();
    const radius = Radius.circular(0);
    final ink = MmtColors.ink950;
    final fg = widget.dark ? Colors.white : MmtColors.ink950;
    return Container(
      margin: const EdgeInsets.fromLTRB(0, 4, 0, 18),
      decoration: BoxDecoration(border: Border.all(color: ink, width: 2), borderRadius: const BorderRadius.all(radius)),
      child: Column(children: [
        Material(color: Colors.transparent, child: InkWell(
          onTap: () => setState(() => open = !open),
          child: Container(
            padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
            child: Row(children: [
              FaIcon(FontAwesomeIcons.list, size: 14, color: fg),
              const SizedBox(width: 10),
              Expanded(child: Text('TABLE OF CONTENTS', style: GoogleFonts.archivoBlack(color: fg, fontSize: 13.5, letterSpacing: 0.8, height: 1.0))),
              AnimatedRotation(turns: open ? 0.5 : 0, duration: const Duration(milliseconds: 160), child: Icon(Icons.keyboard_arrow_down_rounded, size: 22, color: fg)),
            ]),
          ),
        )),
        if (open)
          Container(
            decoration: const BoxDecoration(border: Border(top: BorderSide(color: MmtColors.ink950, width: 1.5))),
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 14),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: List.generate(widget.entries.length, (i) {
              final e = widget.entries[i];
              final indent = ((e.level ?? 1) - 1).clamp(0, 4) * 14.0;
              return Padding(
                padding: EdgeInsets.only(left: indent, top: i == 0 ? 0 : 6, bottom: 4),
                child: Material(color: Colors.transparent, child: InkWell(
                  onTap: () {
                    final ScrollController? sc = widget.scrollController;
                    bool scrolled = false;
                    if (sc != null && sc.hasClients) {
                      try {
                        final pos = sc.position;
                        final totalExtent = pos.maxScrollExtent - pos.minScrollExtent;
                        final jumpTo = (i / (widget.entries.isEmpty ? 1 : widget.entries.length)) * totalExtent;
                        sc.animateTo(jumpTo.clamp(pos.minScrollExtent, pos.maxScrollExtent),
                          duration: const Duration(milliseconds: 420), curve: Curves.easeOutCubic);
                        scrolled = true;
                      } catch (_) {}
                    }
                    if (!scrolled) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(backgroundColor: ink,
                        content: Text(e.title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
                        duration: const Duration(milliseconds: 900)));
                    }
                  },
                  borderRadius: BorderRadius.circular(4),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 6),
                    child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Container(margin: const EdgeInsets.only(top: 8, right: 8), width: 5, height: 5, decoration: const BoxDecoration(shape: BoxShape.circle, color: MmtColors.news)),
                      Expanded(child: Text(e.title, style: GoogleFonts.inter(fontWeight: FontWeight.w700, fontSize: 13.5, color: fg, height: 1.35))),
                    ]),
                  ),
                )),
              );
            })),
          ),
      ]),
    );
  }
}

// =============================================================================
// Author Bio Card — WRITTEN BY with MORE STORIES button, exactly as web screenshot
// =============================================================================
class _AuthorCard extends StatelessWidget {
  final AuthorResponse? author;
  final bool dark;
  final String? authorSlugRoute;
  final VoidCallback? onMoreStories;
  const _AuthorCard({this.author, required this.dark, this.authorSlugRoute, this.onMoreStories});

  @override
  Widget build(BuildContext context) {
    final ink = MmtColors.ink950;
    final name = author?.displayName?.trim().isNotEmpty == true ? author!.displayName!.trim().toUpperCase() : 'MAPMYTIMES';
    final bio = author?.bio?.trim() ?? 'Journalist at MapMyTimes. Independent reporting, verified facts — Journalism of Integrity.';
    final initials = name.split(RegExp(r'\s+')).where((e) => e.isNotEmpty).take(2).map((e) => e.characters.first.toUpperCase()).join().padLeft(1, 'M');
    return Container(
      margin: const EdgeInsets.fromLTRB(0, 10, 0, 6),
      decoration: BoxDecoration(border: Border.all(color: ink, width: 2), boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)], color: Colors.white),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 18),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Container(
            width: 74, height: 74,
            decoration: BoxDecoration(shape: BoxShape.circle, color: MmtColors.news, border: Border.all(color: ink, width: 2)),
            child: Center(child: Text(initials, style: GoogleFonts.archivoBlack(color: Colors.white, fontSize: 26, height: 1.0))),
          ),
          const SizedBox(width: 14),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('WRITTEN BY', style: GoogleFonts.inter(color: MmtColors.ink600, fontWeight: FontWeight.w800, letterSpacing: 1.2, fontSize: 11.5, height: 1.0)),
            const SizedBox(height: 8),
            Text(name, style: GoogleFonts.archivoBlack(color: ink, fontSize: 20, height: 1.0, letterSpacing: -0.3)),
            const SizedBox(height: 12),
            Text(bio, style: GoogleFonts.inter(fontSize: 13.5, color: dark ? MmtColors.ink700 : MmtColors.ink700, fontStyle: FontStyle.italic, height: 1.5, fontWeight: FontWeight.w400)),
          ])),
        ]),
        const SizedBox(height: 18),
        Center(child: InkWell(
          onTap: onMoreStories,
          child: Container(
            constraints: const BoxConstraints(minWidth: 200),
            padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 14),
            decoration: BoxDecoration(border: Border.all(color: ink, width: 2.2)),
            alignment: Alignment.center,
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              Text('MORE STORIES', style: GoogleFonts.archivoBlack(color: ink, fontSize: 14, letterSpacing: 1.0, height: 1.0)),
              const SizedBox(width: 10),
              Icon(Icons.arrow_forward_rounded, size: 18, color: ink),
            ]),
          ),
        )),
      ]),
    );
  }
}

// =============================================================================
// Comments Section Placeholder (0 comments, Join the conversation ↓ + SIGN IN + JOIN CTA)
// =============================================================================
class _CommentsSection extends StatelessWidget {
  final int count;
  final bool dark;
  final VoidCallback? onSignIn;
  final VoidCallback? onJoin;
  const _CommentsSection({this.count = 0, required this.dark, this.onSignIn, this.onJoin});

  @override
  Widget build(BuildContext context) {
    final ink = MmtColors.ink950;
    final news = MmtColors.news;
    return Padding(
      padding: const EdgeInsets.only(top: 22),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Container(
          padding: const EdgeInsets.fromLTRB(0, 0, 0, 6),
          decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: MmtColors.ink950, width: 2))),
          child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Container(
              padding: const EdgeInsets.fromLTRB(14, 8, 22, 8),
              decoration: BoxDecoration(color: news, borderRadius: const BorderRadius.only(topRight: Radius.circular(0))),
              clipBehavior: Clip.antiAlias,
              transform: Matrix4.skewX(-0.18),
              child: Transform(
                transform: Matrix4.skewX(0.18),
                child: Text('CONVERSATION', style: GoogleFonts.archivoBlack(color: Colors.white, fontSize: 14, letterSpacing: 0.8, height: 1.0)),
              ),
            ),
          ]),
        ),
        const SizedBox(height: 14),
        Row(children: [
          Text('COMMENTS ($count)', style: GoogleFonts.archivoBlack(color: dark ? Colors.white : ink, fontSize: 22, letterSpacing: 0, height: 1.0)),
          const Spacer(),
          Text('MODERATED', style: GoogleFonts.inter(fontSize: 11.5, fontWeight: FontWeight.w800, letterSpacing: 0.8, color: MmtColors.ink800.withOpacity(0.6))),
        ]),
        const SizedBox(height: 8),
        Container(height: 3.5, width: 140, decoration: BoxDecoration(border: Border.all(color: ink, width: 1.2)),
          child: Row(children: [
            Container(color: news, height: double.infinity, width: 90),
          ])),
        const SizedBox(height: 22),
        Container(
          padding: const EdgeInsets.fromLTRB(18, 22, 18, 22),
          decoration: BoxDecoration(border: Border.all(color: ink, width: 2)),
          child: Row(crossAxisAlignment: CrossAxisAlignment.center, children: [
            Expanded(child: Text('Sign in to join the conversation and comment on this story.', style: GoogleFonts.inter(fontSize: 14.5, fontWeight: FontWeight.w600, color: ink, height: 1.45))),
            const SizedBox(width: 12),
            Row(children: [
              InkWell(onTap: onSignIn,
                child: Container(padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12), decoration: BoxDecoration(border: Border.all(color: ink, width: 2), color: Colors.white),
                  child: Text('SIGN IN', style: GoogleFonts.archivoBlack(color: ink, fontSize: 13, letterSpacing: 0.8, height: 1.0)))),
              const SizedBox(width: 10),
              InkWell(onTap: onJoin,
                child: Container(padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12), decoration: BoxDecoration(border: Border.all(color: ink, width: 2), color: news, boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)]),
                  child: Text('JOIN', style: GoogleFonts.archivoBlack(color: Colors.white, fontSize: 13, letterSpacing: 0.8, height: 1.0)))),
            ]),
          ]),
        ),
      ]),
    );
  }
}

// =============================================================================
// Reader Mode — Auto-suggest card (for articles >= 800 words, dismissible)
// =============================================================================
class _ReaderModeAutoSuggestCard extends StatelessWidget {
  final VoidCallback onEnterReader;
  final VoidCallback onDismiss;
  const _ReaderModeAutoSuggestCard({required this.onEnterReader, required this.onDismiss});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(color: MmtColors.ink950, width: 2),
          boxShadow: const [BoxShadow(color: MmtColors.ink950, offset: Offset(4, 4))],
          borderRadius: BorderRadius.circular(10),
        ),
        child: Row(children: [
          Container(width: 40, height: 40, alignment: Alignment.center,
            decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 1.8), color: MmtColors.news),
            child: const FaIcon(FontAwesomeIcons.bookOpen, size: 15, color: Colors.white)),
          const SizedBox(width: 12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('READ DISTRACTION-FREE',
              style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 11.5, letterSpacing: 0.9, color: MmtColors.ink950, height: 1.0)),
            const SizedBox(height: 4),
            Text('Switch to Reader Mode for clean typography and custom fonts.',
              style: GoogleFonts.inter(fontSize: 11, fontWeight: FontWeight.w500, color: MmtColors.ink600, height: 1.35)),
          ])),
          const SizedBox(width: 8),
          InkWell(onTap: onEnterReader,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
              decoration: BoxDecoration(color: MmtColors.news, border: Border.all(color: MmtColors.ink950, width: 1.8)),
              child: Text('YES', style: GoogleFonts.inter(color: Colors.white, fontWeight: FontWeight.w900, letterSpacing: 1.4, fontSize: 10.5, height: 1.0)),
            ),
          ),
          const SizedBox(width: 6),
          InkWell(onTap: onDismiss,
            child: Container(width: 36, height: 36, alignment: Alignment.center,
              decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 1.8), color: Colors.white),
              child: const Icon(Icons.close, size: 16, color: MmtColors.ink950),
            ),
          ),
        ]),
      ),
    );
  }
}

// =============================================================================
// YouTube embed + video link opener widgets
// =============================================================================
class _YoutubeEmbed extends StatefulWidget {
  const _YoutubeEmbed({required this.videoId, required this.videoUrl});
  final String videoId;
  final String videoUrl;

  @override
  State<_YoutubeEmbed> createState() => _YoutubeEmbedState();
}

class _YoutubeEmbedState extends State<_YoutubeEmbed> {
  WebViewController? _ctrl;
  bool _loaded = false;

  @override
  void initState() {
    super.initState();
    if (!kIsWeb) {
      try {
        final html = YouTubeUtil.iframeEmbed(widget.videoId);
        _ctrl = WebViewController()
          ..setJavaScriptMode(JavaScriptMode.unrestricted)
          ..setBackgroundColor(Colors.black)
          ..setNavigationDelegate(NavigationDelegate(
            onPageFinished: (_) { if (mounted) setState(() => _loaded = true); },
            onNavigationRequest: (req) {
              final u = req.url.toLowerCase();
              if (u.contains('youtube.com') || u.contains('youtu.be') || u.startsWith('data:') || u.startsWith('about:')) {
                return NavigationDecision.navigate;
              }
              return NavigationDecision.prevent;
            },
          ))
          ..loadHtmlString(html);
      } catch (_) {}
    }
  }

  @override
  Widget build(BuildContext context) {
    if (kIsWeb) {
      return _YoutubeThumbCta(videoId: widget.videoId, videoUrl: widget.videoUrl);
    }
    return Container(
      decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: Colors.black),
      child: AspectRatio(
        aspectRatio: 16 / 9,
        child: Stack(fit: StackFit.expand, children: [
          if (_ctrl != null) WebViewWidget(controller: _ctrl!),
          if (!_loaded)
            Container(color: Colors.black, child: const Center(child: SizedBox(width: 28, height: 28, child: CircularProgressIndicator(color: MmtColors.news, strokeWidth: 3)))),
        ]),
      ),
    );
  }
}

class _YoutubeThumbCta extends StatelessWidget {
  const _YoutubeThumbCta({required this.videoId, required this.videoUrl});
  final String videoId;
  final String videoUrl;

  @override
  Widget build(BuildContext context) {
    final thumb = 'https://img.youtube.com/vi/$videoId/hqdefault.jpg';
    return GestureDetector(
      onTap: () async {
        try { await launchUrl(Uri.parse(videoUrl), mode: LaunchMode.externalApplication); } catch (_) {}
      },
      child: Container(
        decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: Colors.black),
        child: AspectRatio(
          aspectRatio: 16 / 9,
          child: Stack(fit: StackFit.expand, children: [
            Image.network(thumb, fit: BoxFit.cover, errorBuilder: (_, __, ___) => Container(color: Colors.black87)),
            Container(color: Colors.black26),
            Center(child: Container(
              width: 68, height: 48,
              decoration: BoxDecoration(color: const Color(0xFFFF0000), borderRadius: BorderRadius.circular(10), boxShadow: const [BoxShadow(color: Colors.black45, blurRadius: 14, offset: Offset(0, 3))]),
              alignment: Alignment.center,
              child: const Padding(padding: EdgeInsets.only(left: 6), child: Icon(Icons.play_arrow, color: Colors.white, size: 30)),
            )),
            Positioned(left: 10, bottom: 10, child: Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4), decoration: BoxDecoration(color: const Color(0xCC000000), borderRadius: BorderRadius.circular(4)), child: Row(mainAxisSize: MainAxisSize.min, children: const [
              FaIcon(FontAwesomeIcons.youtube, size: 11, color: Colors.white), SizedBox(width: 6), Text('WATCH ON YOUTUBE', style: TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.w900, letterSpacing: 0.8)),
            ]))),
          ]),
        ),
      ),
    );
  }
}

// =============================================================================
// Instagram embed + thumbnail CTA
// =============================================================================
class _InstagramEmbed extends StatefulWidget {
  const _InstagramEmbed({
    required this.mediaId,
    required this.videoUrl,
    this.isReel = true,
  });

  final String mediaId;
  final String videoUrl;
  final bool isReel;

  @override
  State<_InstagramEmbed> createState() => _InstagramEmbedState();
}

class _InstagramEmbedState extends State<_InstagramEmbed> {
  WebViewController? _ctrl;
  bool _loaded = false;

  @override
  void initState() {
    super.initState();
    if (!kIsWeb) {
      try {
        final html = InstagramUtil.iframeEmbed(widget.mediaId);
        _ctrl = WebViewController()
          ..setJavaScriptMode(JavaScriptMode.unrestricted)
          ..setBackgroundColor(const Color(0xFF0A0A0A))
          ..setNavigationDelegate(NavigationDelegate(
            onPageFinished: (_) {
              if (mounted) setState(() => _loaded = true);
            },
            onNavigationRequest: (req) {
              final u = req.url.toLowerCase();
              if (u.contains('instagram.com') ||
                  u.contains('cdninstagram.com') ||
                  u.startsWith('data:') ||
                  u.startsWith('about:')) {
                return NavigationDecision.navigate;
              }
              return NavigationDecision.prevent;
            },
          ))
          ..loadHtmlString(html);
      } catch (_) {}
    }
  }

  @override
  Widget build(BuildContext context) {
    if (kIsWeb) {
      return _InstagramThumbCta(mediaId: widget.mediaId, videoUrl: widget.videoUrl, isReel: widget.isReel);
    }
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: MmtColors.ink950, width: 2),
        color: const Color(0xFF0A0A0A),
      ),
      child: AspectRatio(
        aspectRatio: widget.isReel ? 9 / 16 : 1 / 1,
        child: Stack(fit: StackFit.expand, children: [
          if (_ctrl != null) WebViewWidget(controller: _ctrl!),
          if (!_loaded)
            Container(
              color: const Color(0xFF0A0A0A),
              child: const Center(
                child: SizedBox(
                  width: 28,
                  height: 28,
                  child: CircularProgressIndicator(
                    color: Color(0xFFE1306C),
                    strokeWidth: 3,
                  ),
                ),
              ),
            ),
        ]),
      ),
    );
  }
}

class _InstagramThumbCta extends StatelessWidget {
  const _InstagramThumbCta({
    required this.mediaId,
    required this.videoUrl,
    this.isReel = true,
  });

  final String mediaId;
  final String videoUrl;
  final bool isReel;

  @override
  Widget build(BuildContext context) {
    final gradientColors = isReel
        ? const [Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFF77737), Color(0xFFFCAF45)]
        : const [Color(0xFF405DE6), Color(0xFF5851DB), Color(0xFF833AB4), Color(0xFFC13584), Color(0xFFE1306C), Color(0xFFFD1D1D), Color(0xFFF77737), Color(0xFFFCAF45)];
    final openUrl = videoUrl.isNotEmpty ? videoUrl : InstagramUtil.externalUrl(mediaId, isReel: isReel);
    return GestureDetector(
      onTap: () async {
        try {
          await launchUrl(Uri.parse(openUrl), mode: LaunchMode.externalApplication);
        } catch (_) {}
      },
      child: Container(
        decoration: BoxDecoration(
          border: Border.all(color: MmtColors.ink950, width: 2),
        ),
        child: AspectRatio(
          aspectRatio: isReel ? 9 / 16 : 1 / 1,
          child: Stack(fit: StackFit.expand, children: [
            Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: gradientColors,
                ),
              ),
            ),
            Container(color: Colors.black26),
            Center(
              child: Container(
                width: 68,
                height: 68,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.12),
                  shape: BoxShape.circle,
                  border: Border.all(color: Colors.white, width: 2),
                  boxShadow: const [
                    BoxShadow(color: Colors.black45, blurRadius: 14, offset: Offset(0, 3))
                  ],
                ),
                alignment: Alignment.center,
                child: const Padding(
                  padding: EdgeInsets.only(left: 4),
                  child: Icon(Icons.play_arrow, color: Colors.white, size: 34),
                ),
              ),
            ),
            Positioned(
              left: 10,
              bottom: 10,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xCC000000),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Row(mainAxisSize: MainAxisSize.min, children: const [
                  FaIcon(FontAwesomeIcons.instagram, size: 11, color: Colors.white),
                  SizedBox(width: 6),
                  Text(
                    'WATCH ON INSTAGRAM',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 10,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 0.8,
                    ),
                  ),
                ]),
              ),
            ),
          ]),
        ),
      ),
    );
  }
}

class _OpenVideoLink extends StatelessWidget {
  const _OpenVideoLink({required this.videoUrl});
  final String videoUrl;
  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () async { try { await launchUrl(Uri.parse(videoUrl), mode: LaunchMode.externalApplication); } catch (_) {} },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: const Color(0xFFFF0000)),
        child: Row(children: const [
          FaIcon(FontAwesomeIcons.youtube, size: 18, color: Colors.white),
          SizedBox(width: 12),
          Expanded(child: Text('Open attached video in external player', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w800, fontSize: 13))),
          Icon(Icons.open_in_new, color: Colors.white, size: 18),
        ]),
      ),
    );
  }
}

class _ArticleSkeleton extends StatelessWidget {
  const _ArticleSkeleton();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          Container(
            height: 260,
            decoration: const BoxDecoration(
              color: MmtColors.ink200,
              border: Border(bottom: BorderSide(color: MmtColors.ink950, width: 2)),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  height: 28,
                  width: 120,
                  decoration: BoxDecoration(
                    border: Border.all(color: MmtColors.ink700, width: 2),
                    color: MmtColors.news50,
                  ),
                ),
                const SizedBox(height: 18),
                Container(
                  height: 80,
                  decoration: BoxDecoration(
                    border: Border.all(color: MmtColors.ink700, width: 2),
                    color: MmtColors.ink100,
                  ),
                ),
                const SizedBox(height: 16),
                Container(height: 16, decoration: const BoxDecoration(color: MmtColors.ink200)),
                const SizedBox(height: 10),
                Container(
                  height: 16,
                  width: 280,
                  decoration: const BoxDecoration(color: MmtColors.ink200),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ArticleError extends StatelessWidget {
  const _ArticleError({required this.msg, required this.retry});
  final String msg;
  final VoidCallback retry;

  @override
  Widget build(BuildContext context) {
    final t = Dict.of(context);
    return Scaffold(
      appBar: AppBar(
        backgroundColor: MmtColors.news,
        foregroundColor: Colors.white,
        title: const Text('Error'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                border: Border.all(color: MmtColors.ink950, width: 2),
                color: MmtColors.news50,
                boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '⚠ ${t.common.loadingError}',
                    style: const TextStyle(
                      fontWeight: FontWeight.w900,
                      fontSize: 14,
                      color: MmtColors.news700,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    msg,
                    style: const TextStyle(fontSize: 12.5, color: MmtColors.ink700, fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 16),
                  InkWell(
                    onTap: retry,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        border: Border.all(color: MmtColors.ink950, width: 2),
                        color: Colors.white,
                        boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)],
                      ),
                      child: Text(
                        t.common.retry.toUpperCase(),
                        style: GoogleFonts.inter(
                          fontSize: 11,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.4,
                        ),
                      ),
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
