# MapMyTimes Mobile App (Flutter)

Companion app for iOS + Android — built with Flutter, Riverpod, go_router and Dio.

## Monorepo

- `../frontend/` — Next.js 15 web app
- `../backend/` — 6 Spring Boot / Go microservices
- `./mobile/` — this Flutter app

## Quick Start

Flutter SDK is required (≥ 3.22.0, Dart SDK ≥ 3.3.0).

```bash
# 1. Copy env template and fill values
cp .env.example .env

# 2. Install Dart packages
flutter pub get

# 3. Generate code (envied + slang — optional if using raw dict)
# dart run build_runner build --delete-conflicting-outputs

# 4. Run on iOS / Android
flutter run
```

First run will regenerate the `ios/` and `android/` platform folders (they are intentionally not committed — Flutter generates them idempotently from `pubspec.yaml`).

## Fonts

`pubspec.yaml` declares:

```yaml
fonts:
  - family: ArchivoBlack
    fonts:
      - asset: assets/fonts/ArchivoBlack-Regular.ttf
  - family: Inter
    fonts:
      - asset: assets/fonts/Inter-Regular.ttf    weight: 400
      - asset: assets/fonts/Inter-Medium.ttf     weight: 500
      - asset: assets/fonts/Inter-SemiBold.ttf   weight: 600
      - asset: assets/fonts/Inter-Bold.ttf       weight: 700
      - asset: assets/fonts/Inter-ExtraBold.ttf  weight: 800
```

Drop the Google Font TTFs into `assets/fonts/`. If you skip the files,
`google_fonts` package falls back to HTTP-loaded variants at runtime.

## Design System (Neo-brutalist)

Tokens live in `lib/core/theme/`:

- `colors.dart` — News Red `#E31E24` full 50–900 scale, Ink `#0A0A0A` blacks, `MmtTokens` (radius xs→full, spacing scale, hard `4×4` / `8×8` zero-blur box shadows)
- `text.dart` — `MmtText` statics — `Archivo Black` for `headlineDisplay / h1..h4 / eyebrow`, Inter for body
- `theme.dart` — Material3 `ThemeData` — 2 px black borders on cards/inputs/chips, zero corner radius, hard shadows, elevated/outlined buttons

## Routing (go_router)

App root uses ShellRoute with bottom-nav tabs (Home / News / Videos / Menu).
Shorts + Articles push on top as root routes (full-screen).

| Path                | Screen                     |
| ------------------- | -------------------------- |
| `/`                 | HomeScreen                 |
| `/news`             | NewsListScreen (tab 1)     |
| `/videos`           | VideosScreen (tab 2)       |
| `/menu`             | MenuScreen (tab 4)         |
| `/shorts`           | ShortsFeedScreen (full)    |
| `/article/:slug`    | NewsArticleScreen          |
| `/search`           | SearchScreen               |
| `/about`, `/contact`, `/careers`, `/careers/:id`, `/login`, `/dashboard` | Static screens |

## Backend integration (blog-service)

`lib/services/blog_service.dart` is a singleton Dio client with:

- `baseUrl = Env.apiBaseUrl` (default `https://api.mapmytour.in`, override in `.env`)
- `X-Source: mapmytimes-mobile` header, 15 s timeouts
- `PrettyDioLogger` in debug builds only
- `setBearerToken(String)` for authenticated calls
- Endpoints matching frontend `blogApi.ts`:
  - `postsList` (paginated + filters: category / tag / featured / trending / postType / language / sort)
  - `postById`, `postBySlug`
  - `postsSearch`
  - `incrementView(id)`
  - `likePost(id)` / `unlikePost(id)`
  - `categoriesList`, `tagsPopular`

Response envelope unwrapping helpers live in `lib/models/blog_models.dart`:

```dart
APIResponse<T>   { success, message, code, data }
PaginatedResponse<T> { items, page, size, total, totalPages, hasMore }
```

## i18n (EN / हि)

- `lib/core/l10n/dict.dart` — single `Dict` class with identical EN + हि maps.
- `LangScope` InheritedWidget + `AppLang ValueNotifier` drives rebuilds.
- `LangScope.toggle(context)` — flips EN↔हि + persists to SharedPreferences (`app_language` key).
- If later required, the `slang` + `slang_build_runner` packages are already declared in `pubspec.yaml`.

## OAuth / Deep links

Scheme in `.env`:

```
APP_LINK_SCHEME=mapmytimes
```

`flutter_web_auth_2` is wired in deps for upcoming Google / Facebook flows.

## Environment variables

See `.env.example` — all keys are optional. Defaults point to the production MapMyTimes backend.

| Key                  | Purpose                                   |
| -------------------- | ----------------------------------------- |
| `APP_NAME`           | label in system tray / about              |
| `SITE_NAME`          | SEO / brand copy                          |
| `SITE_URL`           | Share URL base                            |
| `SITE_CONTACT_EMAIL` / `SITE_CONTACT_PHONE` | Footer / About defaults |
| `API_BASE_URL`       | Blog service base URL                     |
| `AUTH_BASE_URL`      | Auth service base URL (defaults to API)   |
| `SOCIAL_FACEBOOK`, `SOCIAL_TWITTER`, `SOCIAL_INSTAGRAM`, `SOCIAL_YOUTUBE`, `SOCIAL_LINKEDIN` | Follow-Us drawer links |
| `GOOGLE_CLIENT_ID` / `FACEBOOK_CLIENT_ID` | OAuth client IDs |
| `APP_LINK_SCHEME`    | Custom URL scheme for deep links          |

## Build

```bash
# Android
flutter build apk --release
flutter build appbundle --release

# iOS (requires Xcode + signing)
flutter build ipa --release
```
