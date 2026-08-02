// i18n dictionary — copy of frontend/src/lib/i18n/en.ts + hi.ts (lightweight)
// (For simplicity we use simple Map<String,String> + InheritedWidget provider;
//  if later needed, slang package can be enabled via build_runner.)

import 'package:flutter/widgets.dart';

enum LangCode { en, hi }

// ---------- Nested accessor classes (used as t.common.share, t.nav.shorts, etc) ----------
class HomeDict {
  final String heroEyebrow;
  final String heroTitle;
  final String heroBody;
  final String featured;
  final String trending;
  final String latest;
  final String categories;
  const HomeDict({
    required this.heroEyebrow,
    required this.heroTitle,
    required this.heroBody,
    required this.featured,
    required this.trending,
    required this.latest,
    required this.categories,
  });
}

class FooterDict {
  final String copyright;
  final String followUs;
  final String mission;
  final String contactNewsroom;
  final String joinAsJournalist;
  const FooterDict({
    required this.copyright,
    required this.followUs,
    required this.mission,
    required this.contactNewsroom,
    required this.joinAsJournalist,
  });
}

class NavDict {
  final String shorts;
  final String search;
  const NavDict({required this.shorts, required this.search});
}

class ShortsDict {
  final String empty;
  const ShortsDict({required this.empty});
}

class CommonDict {
  final String share;
  final String copyLink;
  final String linkCopied;
  final String refresh;
  final String retry;
  final String loadingError;
  final String languageSwitched;
  const CommonDict({
    required this.share,
    required this.copyLink,
    required this.linkCopied,
    required this.refresh,
    required this.retry,
    required this.loadingError,
    required this.languageSwitched,
  });
}

// =============================================================================
// Dict
// =============================================================================
class Dict {
  // Brand
  final String tagline;

  // Navbar / bottom nav
  final String navHome;
  final String news;
  final String videos;
  final String shorts;
  final String about;
  final String contact;
  final String careers;
  final String search;
  final String dashboard;

  // Home
  final String featuredReports;
  final String trendingNow;
  final String latestStories;
  final String categories;
  final String heroTitle;
  final String heroBody;
  final String viewAll;
  final String readMore;
  final String exploreMore;

  // Flat common
  final String loading;
  final String somethingWentWrong;
  final String retry;
  final String back;
  final String subscribe;
  final String subscribePlaceholder;
  final String subscribeSuccess;
  final String postedOn;
  final String byAuthor;
  final String copyright;
  final String followUs;

  // Footer-like mission
  final String mission;
  final String aboutRole;
  final String contactNewsroom;
  final String joinAsJournalist;

  // Auth
  final String signIn;
  final String join;
  final String email;
  final String password;
  final String forgotPassword;
  final String noAccountYet;

  // Careers
  final String openRoles;
  final String noOpenRoles;
  final String applyNow;

  // Shorts / videos
  final String watchMore;
  final String allVideos;

  // Errors
  final String noStoriesYet;
  final String noResultsFor;

  const Dict({
    required this.tagline,
    required this.navHome,
    required this.news,
    required this.videos,
    required this.shorts,
    required this.about,
    required this.contact,
    required this.careers,
    required this.search,
    required this.dashboard,
    required this.featuredReports,
    required this.trendingNow,
    required this.latestStories,
    required this.categories,
    required this.heroTitle,
    required this.heroBody,
    required this.viewAll,
    required this.readMore,
    required this.exploreMore,
    required this.loading,
    required this.somethingWentWrong,
    required this.retry,
    required this.back,
    required this.subscribe,
    required this.subscribePlaceholder,
    required this.subscribeSuccess,
    required this.postedOn,
    required this.byAuthor,
    required this.copyright,
    required this.followUs,
    required this.mission,
    required this.aboutRole,
    required this.contactNewsroom,
    required this.joinAsJournalist,
    required this.signIn,
    required this.join,
    required this.email,
    required this.password,
    required this.forgotPassword,
    required this.noAccountYet,
    required this.openRoles,
    required this.noOpenRoles,
    required this.applyNow,
    required this.watchMore,
    required this.allVideos,
    required this.noStoriesYet,
    required this.noResultsFor,
  });

  // ---------- Nested accessors (sugar for t.common.share etc) ----------
  CommonDict get common => CommonDict(
        share: 'Share',
        copyLink: 'Copy link',
        linkCopied: 'Link copied to clipboard',
        refresh: retry,
        retry: retry,
        loadingError: somethingWentWrong,
        languageSwitched: 'Language switched',
      );

  HomeDict get home => HomeDict(
        heroEyebrow: tagline,
        heroTitle: heroTitle,
        heroBody: heroBody,
        featured: featuredReports,
        trending: trendingNow,
        latest: latestStories,
        categories: categories,
      );

  NavDict get nav => NavDict(shorts: shorts, search: search);

  ShortsDict get shortsDict => ShortsDict(empty: noStoriesYet);

  FooterDict get footer => FooterDict(
        copyright: copyright,
        followUs: followUs,
        mission: mission,
        contactNewsroom: contactNewsroom,
        joinAsJournalist: joinAsJournalist,
      );

  // ---------- String helpers ----------
  String copyrightYear(int year) => copyright.replaceAll('%YEAR%', year.toString());
  String authorLabel(String who) => '$byAuthor $who';
  String minRead(int? n) => n == null ? '' : '$n min read';

  // ---------- Convenience: Dict.of(ctx) === LangScope.of(ctx) ----------
  static Dict of(BuildContext ctx) => LangScope.of(ctx);
}

const Dict en = Dict(
  tagline: 'JOURNALISM OF INTEGRITY',
  navHome: 'Home',
  news: 'News',
  videos: 'Videos',
  shorts: 'Shorts',
  about: 'About',
  contact: 'Contact',
  careers: 'Careers',
  search: 'Search',
  dashboard: 'Dashboard',
  featuredReports: 'Featured Reports',
  trendingNow: 'Trending Now',
  latestStories: 'Latest Stories',
  categories: 'Categories',
  heroTitle: 'Journalism of Integrity. Stories that matter.',
  heroBody: 'Independent reporting, verified facts, and investigations that hold power to account — powered by MAPMYTOUR LLP.',
  viewAll: 'View all',
  readMore: 'Read more',
  exploreMore: 'Explore more',
  loading: 'Loading…',
  somethingWentWrong: 'Something went wrong.',
  retry: 'Retry',
  back: 'Back',
  subscribe: 'Subscribe',
  subscribePlaceholder: 'your@email.com',
  subscribeSuccess: 'Thanks — you\'re subscribed.',
  postedOn: 'Posted on',
  byAuthor: 'by',
  copyright: '© %YEAR% MAPMYTOUR LLP, India',
  followUs: 'Follow us',
  mission: 'MapMyTimes is an independent news platform committed to verified, unflinching journalism — reports, investigations, and storytelling that serves the public good.',
  aboutRole: 'About MapMyTimes',
  contactNewsroom: 'Contact Newsroom',
  joinAsJournalist: 'Join as Journalist',
  signIn: 'Sign in',
  join: 'Join',
  email: 'Email',
  password: 'Password',
  forgotPassword: 'Forgot password?',
  noAccountYet: 'Don\'t have an account?',
  openRoles: 'Open roles',
  noOpenRoles: 'No open roles right now.',
  applyNow: 'Apply now',
  watchMore: 'Watch more',
  allVideos: 'All videos',
  noStoriesYet: 'No stories published in this section yet.',
  noResultsFor: 'No results for',
);

const Dict hi = Dict(
  tagline: 'ईमानदारी की पत्रकारिता',
  navHome: 'होम',
  news: 'समाचार',
  videos: 'वीडियो',
  shorts: 'शॉर्ट्स',
  about: 'परिचय',
  contact: 'संपर्क',
  careers: 'नौकरियाँ',
  search: 'खोजें',
  dashboard: 'डैशबोर्ड',
  featuredReports: 'विशेष रिपोर्टें',
  trendingNow: 'आज की हॉट खबरें',
  latestStories: 'ताज़ा खबरें',
  categories: 'श्रेणियाँ',
  heroTitle: 'ईमानदारी की पत्रकारिता। मायने रखने वाली कहानियाँ।',
  heroBody: 'स्वतंत्र रिपोर्टिंग, सत्यापित तथ्य, और जाँच पड़ताल जो सत्ता को जवाबदेह बनाती है — MAPMYTOUR LLP द्वारा संचालित।',
  viewAll: 'सभी देखें',
  readMore: 'और पढ़ें',
  exploreMore: 'और देखें',
  loading: 'लोड हो रहा है…',
  somethingWentWrong: 'कुछ गड़बड़ हो गया।',
  retry: 'फिर कोशिश करें',
  back: 'वापस',
  subscribe: 'सब्सक्राइब करें',
  subscribePlaceholder: 'aap@email.com',
  subscribeSuccess: 'धन्यवाद — सब्सक्राइब हो गया।',
  postedOn: 'प्रकाशित तिथि',
  byAuthor: 'लेखक',
  copyright: '© %YEAR% MAPMYTOUR LLP, भारत',
  followUs: 'हमें फ़ॉलो करें',
  mission: 'MapMyTimes एक स्वतंत्र समाचार मंच है जो सत्यापित, अडिग पत्रकारिता, रिपोर्ट, जाँच-पड़ताल और जनता की भलाई के लिए कहानियाँ प्रस्तुत करता है।',
  aboutRole: 'MapMyTimes के बारे में',
  contactNewsroom: 'न्यूज़रूम से संपर्क',
  joinAsJournalist: 'पत्रकार के रूप में शामिल हों',
  signIn: 'साइन इन',
  join: 'जुड़ें',
  email: 'ईमेल',
  password: 'पासवर्ड',
  forgotPassword: 'पासवर्ड भूल गए?',
  noAccountYet: 'अब तक खाता नहीं बनाया?',
  openRoles: 'खुली भूमिकाएँ',
  noOpenRoles: 'अभी कोई खुली भूमिका नहीं।',
  applyNow: 'अभी आवेदन करें',
  watchMore: 'और देखें',
  allVideos: 'सभी वीडियो',
  noStoriesYet: 'इस श्रेणी में अभी कोई कहानी नहीं।',
  noResultsFor: 'कोई परिणाम नहीं मिला:',
);

// ---------------------------------------------------------------------------
// App language provider — ValueNotifier<LangCode> + Inherited accessor
// ---------------------------------------------------------------------------
class AppLang extends ValueNotifier<LangCode> {
  AppLang._(this._d, super.value);
  static final AppLang instance = AppLang._(en, LangCode.en);

  Dict _d;
  Dict get t => _d;

  void setLang(LangCode l) {
    if (value == l) return;
    value = l;
    _d = (l == LangCode.hi) ? hi : en;
    notifyListeners();
  }

  void toggle() => setLang(value == LangCode.en ? LangCode.hi : LangCode.en);
}

class _LangScope extends InheritedWidget {
  const _LangScope({required super.child, required this.lang});
  final AppLang lang;
  @override
  bool updateShouldNotify(covariant _LangScope oldWidget) =>
      lang.value != oldWidget.lang.value;
}

class LangScope extends StatefulWidget {
  const LangScope({super.key, required this.child, this.initialLang});
  final Widget child;
  final LangCode? initialLang;
  @override
  State<LangScope> createState() => _LangScopeState();

  static Dict of(BuildContext ctx) {
    final s = ctx.dependOnInheritedWidgetOfExactType<_LangScope>();
    return s?.lang.t ?? en;
  }

  static LangCode codeOf(BuildContext ctx) {
    final s = ctx.dependOnInheritedWidgetOfExactType<_LangScope>();
    return s?.lang.value ?? LangCode.en;
  }

  static void toggle(BuildContext ctx) {
    final s = ctx.getInheritedWidgetOfExactType<_LangScope>();
    s?.lang.toggle();
  }
}

class _LangScopeState extends State<LangScope> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (widget.initialLang != null) {
        AppLang.instance.setLang(widget.initialLang!);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<LangCode>(
      valueListenable: AppLang.instance,
      builder: (_, __, ___) => _LangScope(
        lang: AppLang.instance,
        child: widget.child,
      ),
    );
  }
}
