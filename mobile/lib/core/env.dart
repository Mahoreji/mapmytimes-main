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
      _get('API_BASE_URL', 'https://api.mapmytimes.com');
  static String get authBaseUrl =>
      _get('AUTH_BASE_URL', _get('API_BASE_URL', 'https://api.mapmytimes.com'));

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

  // Aliases — screens use socialXxx naming
  static String get socialFacebook => facebook;
  static String get socialTwitter => twitter;
  static String get socialInstagram => instagram;
  static String get socialYoutube => youtube;
  static String get socialLinkedin => linkedin;

  // -------------------------------------------------------------------------
  // OAuth / Deep linking
  // -------------------------------------------------------------------------
  static String get googleClientId => _get('GOOGLE_CLIENT_ID', '');
  static String get facebookClientId => _get('FACEBOOK_CLIENT_ID', '');
  static String get linkScheme => _get('APP_LINK_SCHEME', 'mapmytimes');

  // -------------------------------------------------------------------------
  // Uploads / Media — reads from --dart-define (String.fromEnvironment),
  // which takes precedence over dotenv.
  // -------------------------------------------------------------------------
  static String get uploadsBaseUrl {
    const fromDefine = String.fromEnvironment('UPLOADS_BASE_URL');
    if (fromDefine.isNotEmpty) return fromDefine;
    return _get('UPLOADS_BASE_URL', 'https://api.mapmytimes.com/uploads');
  }

  /// Known origin hosts that return images without CORS headers (direct browser
  /// fetch fails with net::ERR_FAILED). We re-route these through the local
  /// CORS proxy (http://localhost:5555) which forwards the request with proper
  /// server-to-server headers AND attaches Access-Control-Allow-Origin: * on
  /// the response.
  static const List<String> _noCorsHosts = [
    'mapmytour-bucket-prod.s3.ap-south-1.amazonaws.com',
    'mapmytour-bucket-prod.s3.amazonaws.com',
  ];

  /// Resolve a raw image URL coming from the backend (featuredImageUrl /
  /// coverImage / etc.) into a URL we can actually load in Flutter web:
  ///
  ///  * empty                 → ''
  ///  * /uploads/x.jpg        → UPLOADS_BASE_URL/uploads/x.jpg OR via proxy
  ///  * /any/abs/path.jpg     → UPLOADS_BASE_URL prefixed
  ///  * http(s)://host/path   → if host is in _noCorsHosts → go via /s3 proxy
  ///                             otherwise return as-is.
  static String resolveImgUrl(String? raw) {
    final s = (raw ?? '').trim();
    if (s.isEmpty) return '';
    // Already data uri / blob → load verbatim.
    if (s.startsWith('data:') || s.startsWith('blob:')) return s;

    // Absolute path (starts with / but not protocol)
    if (s.startsWith('/') && !s.startsWith('//')) {
      final base = uploadsBaseUrl;
      // If base is localhost proxy AND path starts with /uploads/, proxy path
      // will be handled by cors-proxy.js passthrough.
      final baseTrim = base.endsWith('/')
          ? base.substring(0, base.length - 1)
          : base;
      if (s.startsWith('/uploads/')) {
        // Use api base proxy (localhost:5555 path passthrough) — our proxy
        // routes /uploads/* to the real backend.
        const apiBase = String.fromEnvironment('API_BASE_URL');
        if (apiBase.isNotEmpty && apiBase.contains('localhost')) {
          final a = apiBase.endsWith('/')
              ? apiBase.substring(0, apiBase.length - 1)
              : apiBase;
          return a + s;
        }
        return baseTrim + s.substring('/uploads'.length);
      }
      // Any other abs path — just assume relative to api base via proxy
      const apiBase = String.fromEnvironment('API_BASE_URL');
      if (apiBase.isNotEmpty && apiBase.contains('localhost')) {
        final a = apiBase.endsWith('/')
            ? apiBase.substring(0, apiBase.length - 1)
            : apiBase;
        return a + s;
      }
      return baseTrim + s;
    }

    // Network URL: check for CORS-blocked hosts and re-route.
    if (s.startsWith('http://') || s.startsWith('https://')) {
      try {
        final uri = Uri.parse(s);
        if (_noCorsHosts.contains(uri.host)) {
          // Route through localhost CORS proxy at /s3/<remaining-path>
          // Use API_BASE proxy if set (localhost)
          const apiBase = String.fromEnvironment('API_BASE_URL');
          String prefix;
          if (apiBase.isNotEmpty && apiBase.contains('localhost')) {
            final a = apiBase.endsWith('/')
                ? apiBase.substring(0, apiBase.length - 1)
                : apiBase;
            prefix = a;
          } else {
            prefix = apiBaseUrl.endsWith('/')
                ? apiBaseUrl.substring(0, apiBaseUrl.length - 1)
                : apiBaseUrl;
          }
          final rest = Uri(
            path: uri.path,
            query: uri.query.isEmpty ? null : uri.query,
            fragment: uri.fragment.isEmpty ? null : uri.fragment,
          ).toString();
          return '$prefix/s3$rest';
        }
      } catch (_) {}
      return s;
    }

    // Fallback: relative path from uploads base.
    final b = uploadsBaseUrl;
    return b.endsWith('/') ? '$b$s' : '$b/$s';
  }

  static bool get isDebug => !kReleaseMode;
}
