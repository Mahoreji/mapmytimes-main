// ignore: avoid_web_libraries_in_flutter
import 'dart:html' as html;
import 'tts_service.dart';

TtsService createTtsService() => _WebSpeechTtsService();

class _WebSpeechTtsService implements TtsService {
  html.SpeechSynthesisUtterance? _current;
  String _language = 'en-IN';
  double _rate = 1.0;
  double _volume = 1.0;
  html.SpeechSynthesisVoice? _voice;
  void Function()? _onComplete;
  void Function()? _onCancel;
  void Function(dynamic)? _onError;

  static List<html.SpeechSynthesisVoice> _voices = <html.SpeechSynthesisVoice>[];
  static bool _voicesLoaded = false;

  static Future<void> _ensureVoices() async {
    if (_voicesLoaded) return;
    final synth = html.window.speechSynthesis;
    if (synth == null) return;
    // ignore: unnecessary_cast
    final listRaw = synth.getVoices() as List<html.SpeechSynthesisVoice>?;
    List<html.SpeechSynthesisVoice> list = listRaw ?? <html.SpeechSynthesisVoice>[];
    if (list.isEmpty) {
      await Future<void>.delayed(const Duration(milliseconds: 200));
      // ignore: unnecessary_cast
      final lr2 = synth.getVoices() as List<html.SpeechSynthesisVoice>?;
      list = lr2 ?? <html.SpeechSynthesisVoice>[];
    }
    _voices = list;
    _voicesLoaded = true;
  }

  html.SpeechSynthesisVoice? _pickVoice(String lang) {
    if (_voices.isEmpty) return null;
    final langL = lang.toLowerCase();
    // exact match first (e.g. en-IN)
    for (final v in _voices) {
      final code = (v.lang ?? '').toLowerCase();
      if (code == langL) return v;
    }
    // prefix match (e.g. en-US when en-IN not available)
    final prefix = langL.split('-').first;
    for (final v in _voices) {
      final code = (v.lang ?? '').toLowerCase();
      if (code.startsWith('${prefix}_') || code.startsWith('$prefix-')) return v;
    }
    // last resort: return any default voice
    for (final v in _voices) {
      if (v.defaultValue ?? false) return v;
    }
    return _voices.first;
  }

  @override
  Future<void> init({String language = 'en-IN', required double speechRate}) async {
    _language = language;
    _rate = speechRate;
    await _ensureVoices();
    _voice = _pickVoice(language);
  }

  @override
  Future<void> setSpeechRate(double rate) async {
    _rate = rate;
    if (_current != null) _current!.rate = rate;
  }

  @override
  Future<void> setVolume(double volume) async {
    _volume = volume;
    if (_current != null) _current!.volume = volume;
  }

  @override
  Future<void> setLanguage(String lang) async {
    _language = lang;
    await _ensureVoices();
    _voice = _pickVoice(lang);
    if (_current != null) _current!.lang = lang;
  }

  @override
  Future<bool> speak(String text) async {
    final synth = html.window.speechSynthesis;
    if (synth == null) return false;
    stop();
    await _ensureVoices();
    _voice = _pickVoice(_language);
    final utter = html.SpeechSynthesisUtterance(text);
    utter.lang = _voice?.lang ?? _language;
    utter.rate = _rate;
    utter.volume = _volume;
    utter.pitch = 1.0;
    if (_voice != null) utter.voice = _voice;

    void onEnd(html.Event e) {
      _current = null;
      _onComplete?.call();
    }

    void onErr(html.Event e) {
      _current = null;
      final dyn = e;
      _onError?.call(dyn);
    }

    void onCancel(html.Event e) {
      _current = null;
      _onCancel?.call();
    }

    utter.addEventListener('end', onEnd);
    utter.addEventListener('error', onErr);
    utter.addEventListener('pause', onCancel);
    utter.addEventListener('abort', onCancel);

    _current = utter;
    synth.speak(utter);
    return true;
  }

  @override
  Future<void> stop() async {
    final synth = html.window.speechSynthesis;
    if (synth == null) return;
    synth.cancel();
    _current = null;
  }

  @override
  void setCompletionHandler(void Function() onComplete) => _onComplete = onComplete;

  @override
  void setCancelHandler(void Function() onCancel) => _onCancel = onCancel;

  @override
  void setErrorHandler(void Function(dynamic) onError) => _onError = onError;

  @override
  void dispose() {
    final synth = html.window.speechSynthesis;
    synth?.cancel();
    _current = null;
  }
}
