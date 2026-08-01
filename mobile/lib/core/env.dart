import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Environment helper — reads from .env file, same schema as frontend SITE.*
/// Copy of: $repo/frontend/src/lib/utils.ts (SITE object patterns)
class Env {
  Env._();

  // -------------------------------------------------------------------------
  // Init: load env file at startup (before runApp)
  // -------------------------------------------------------------------------
  static Future<void> load() async {
    try {
      await dotenv.load(fileName: _file);
    } catch (_) {
      // .env is optional — fall back to compile-time defaults
    }
  }

  static String get _file {
    if (kReleaseMode) return '.env';
    return '.env';
  }

  static String _get(String key, String fallback) {
    try {
      final x = dotenv.maybeGet(key);
      if (x != null && x.isNotEmpty) return x;
    } catch (_) {}
    return fallback;
  }

  // -------------------------------------------------------------------------
  // Site / Brand
  // -------------------------------------------------------------------------
  static String get appName => _get('APP_NAME', 'MapMyTimes');
  static String get envName => _get('APP_ENV', 'dev');

  static String get siteName => _get('SITE_NAME', 'MapMyTimes');
  static String get siteUrl => _get('SITE_URL', 'https://mapmytimes.com');
  static String get tagline => 'JOURNALISM OF INTEGRITY';

  static String get contactEmail =>
      _get('SITE_CONTACT_EMAIL', 'admin@mapmytimes.com');
  static String get contactPhone =>
      _get('SITE_CONTACT_PHONE', '+91 9893989395');

  // -------------------------------------------------------------------------
  // API base — same as frontend's NEXT_PUBLIC_API_BASE_URL
  // -------------------------------------------------------------------------
  static String get apiBaseUrl =>
      _get('API_BASE_URL', 'https://api.mapmytour.in');
  static String get authBaseUrl =>
      _get('AUTH_BASE_URL', _get('API_BASE_URL', 'https://api.mapmytour.in'));

  // -------------------------------------------------------------------------
  // Socials (open via url_launcher)
  // -------------------------------------------------------------------------
  static String get facebook =>
      _get('SOCIAL_FACEBOOK', 'https://facebook.com/mapmytimes');
  static String get twitter =>
      _get('SOCIAL_TWITTER', 'https://x.com/mapmytimes');
  static String get instagram =>
      _get('SOCIAL_INSTAGRAM', 'https://instagram.com/mapmytimes');
  static String get youtube =>
      _get('SOCIAL_YOUTUBE', 'https://youtube.com/@mapmytimes');
  static String get linkedin =>
      _get('SOCIAL_LINKEDIN', 'https://linkedin.com/company/mapmytimes');

  // -------------------------------------------------------------------------
  // OAuth / Deep linking
  // -------------------------------------------------------------------------
  static String get googleClientId => _get('GOOGLE_CLIENT_ID', '');
  static String get facebookClientId => _get('FACEBOOK_CLIENT_ID', '');
  static String get linkScheme => _get('APP_LINK_SCHEME', 'mapmytimes');

  static bool get isDebug => !kReleaseMode;
}
