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
      _get('SITE_CONTACT_PHONE', '+91 80859 27274');

  // -------------------------------------------------------------------------
  // API base — same as frontend's NEXT_PUBLIC_API_BASE_URL
  // -------------------------------------------------------------------------
  static String get apiBaseUrl {
    const fromDefine = String.fromEnvironment('API_BASE_URL');
    if (fromDefine.isNotEmpty) return fromDefine;

    final fromEnv = dotenv.maybeGet('API_BASE_URL');
    if (fromEnv != null && fromEnv.isNotEmpty) return fromEnv;

    if (kIsWeb) {
      try {
        final host = Uri.base.host;
        final port = Uri.base.hasPort ? Uri.base.port : 0;
        if (host == 'localhost' || host == '127.0.0.1') {
          if (port == 3000 || port == 3001 || port == 3002 || port == 5173 || port == 5174 || port == 5555 || port == 8080) {
            return 'http://localhost:5052';
          }
        }
      } catch (_) {}
    }
    return 'https://api.mapmytimes.com';
  }

  static String get authBaseUrl {
    const fromDefine = String.fromEnvironment('AUTH_BASE_URL');
    if (fromDefine.isNotEmpty) return fromDefine;

    final fromEnv = dotenv.maybeGet('AUTH_BASE_URL');
    if (fromEnv != null && fromEnv.isNotEmpty) return fromEnv;

    if (kIsWeb) {
      try {
        final host = Uri.base.host;
        if (host == 'localhost' || host == '127.0.0.1') {
          final port = Uri.base.hasPort ? Uri.base.port : 0;
          if (port == 3000 || port == 3001 || port == 3002 || port == 5173 || port == 5174 || port == 5555 || port == 8080) {
            return 'http://localhost:5051';
          }
        }
      } catch (_) {}
    }
    // Production/staging: Auth is co-located with the API ingress, so use the
    // same base URL as other REST calls.
    return apiBaseUrl;
  }

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

    final fromEnv = dotenv.maybeGet('UPLOADS_BASE_URL');
    if (fromEnv != null && fromEnv.isNotEmpty) return fromEnv;

    if (kIsWeb) {
      try {
        final host = Uri.base.host;
        if (host == 'localhost' || host == '127.0.0.1') {
          return 'http://localhost:5052/uploads';
        }
      } catch (_) {}
    }
    return 'https://api.mapmytimes.com/uploads';
  }

  /// Known origin hosts that return images without CORS headers (direct browser
  /// fetch fails with net::ERR_FAILED). We re-route these through the local
  /// CORS proxy (http://localhost:5555) which forwards the request with proper
  /// server-to-server headers AND attaches Access-Control-Allow-Origin: * on
  /// the response.
  static const List<String> _noCorsHosts = [
    'mapnytimes.s3.ap-south-1.amazonaws.com',
    'mapnytimes.s3.amazonaws.com',
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
    var s = (raw ?? '').trim();
    if (s.isEmpty) return '';
    // Already data uri / blob → load verbatim.
    if (s.startsWith('data:') || s.startsWith('blob:')) return s;

    // Extract actual URL from common malformed strings like:
    //   "{url: https://.../file.jpg}", "{https://...}", or "url: https://..."
    if ((!s.startsWith('http') && !s.startsWith('/')) &&
        (s.contains('https://') || s.contains('http://'))) {
      final reg = RegExp(r'https?://[^}\]"><,)\s]+');
      final m = reg.firstMatch(s);
      if (m != null) {
        final extracted = m.group(0) ?? '';
        // Remove trailing punctuation / braces if any
        s = extracted.replaceAll(RegExp(r'[}\]\),]+$'), '');
      }
    }

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
