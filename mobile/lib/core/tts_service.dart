import 'tts_service_stub.dart'
    if (dart.library.io) 'tts_service_io.dart'
    if (dart.library.html) 'tts_service_web.dart';

abstract class TtsService {
  factory TtsService() => createTtsService();

  Future<void> init({String language = 'en-IN', required double speechRate});
  Future<void> setSpeechRate(double rate);
  Future<void> setVolume(double volume);
  Future<void> setLanguage(String lang);
  Future<bool> speak(String text);
  Future<void> stop();
  void setCompletionHandler(void Function() onComplete);
  void setCancelHandler(void Function() onCancel);
  void setErrorHandler(void Function(dynamic) onError);
  void dispose();
}
