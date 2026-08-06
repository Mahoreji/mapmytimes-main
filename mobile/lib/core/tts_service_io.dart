import 'dart:io';
import 'package:flutter_tts/flutter_tts.dart';
import 'tts_service.dart';

TtsService createTtsService() => _FlutterTtsService();

class _FlutterTtsService implements TtsService {
  final FlutterTts _tts = FlutterTts();

  @override
  Future<void> init({String language = 'en-IN', required double speechRate}) async {
    await setLanguage(language);
    await setSpeechRate(speechRate);
    await setVolume(1.0);
  }

  @override
  Future<void> setSpeechRate(double rate) {
    double r = rate;
    if (Platform.isIOS) r = r / 2;
    return _tts.setSpeechRate(r);
  }

  @override
  Future<void> setVolume(double volume) => _tts.setVolume(volume);

  @override
  Future<void> setLanguage(String lang) => _tts.setLanguage(lang);

  @override
  Future<bool> speak(String text) async {
    final r = await _tts.speak(text);
    return r == 1;
  }

  @override
  Future<void> stop() => _tts.stop();

  @override
  void setCompletionHandler(void Function() onComplete) => _tts.setCompletionHandler(onComplete);

  @override
  void setCancelHandler(void Function() onCancel) => _tts.setCancelHandler(onCancel);

  @override
  void setErrorHandler(void Function(dynamic) onError) => _tts.setErrorHandler(onError);

  @override
  void dispose() {}
}
