// About, Contact, Careers, Login, Videos, Menu, Dashboard screens (light)
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:url_launcher/url_launcher_string.dart';
import '../core/env.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../core/widgets/brand.dart';
import '../models/blog_models.dart';
import '../services/blog_service.dart';

// ---------------------------------------------------------------------------
// VIDEOS
// ---------------------------------------------------------------------------
class VideosScreen extends StatefulWidget {
  const VideosScreen({super.key});
  @override
  State<VideosScreen> createState() => _VideosScreenState();
}
class _VideosScreenState extends State<VideosScreen> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  final _svc = BlogService.create();
  late final Future<List<BlogPostSummaryResponse>> _load = _svc
      .postsList(status: 'PUBLISHED', postType: 'VIDEO', size: 20, sort: '-publishedAt')
      .then((p) => p.items);

  @override
  Widget build(BuildContext ctx) {
    super.build(ctx);
    final t = LangScope.of(ctx);
    return CustomScrollView(
      slivers: [
        SliverAppBar(
          pinned: true,
          title: Text(t.videos),
          bottom: PreferredSize(
            preferredSize: const Size.fromHeight(2),
            child: Container(height: 2, color: MmtColors.ink950),
          ),
        ),
        SliverToBoxAdapter(
          child: FutureBuilder<List<BlogPostSummaryResponse>>(
            future: _load,
            builder: (c, snap) {
              if (snap.connectionState == ConnectionState.waiting) {
                return const Center(
                  heightFactor: 10,
                  child: CircularProgressIndicator(color: MmtColors.news),
                );
              }
              final list = snap.data ?? <BlogPostSummaryResponse>[];
              if (list.isEmpty) return Padding(
                padding: const EdgeInsets.all(20),
                child: Center(child: Text(t.noStoriesYet)),
              );
              return Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
                child: Wrap(
                  spacing: 18,
                  runSpacing: 18,
                  children: [
                    for (final p in list)
                      SizedBox(
                        width: (MediaQuery.of(c).size.width - 58) / 2,
                        child: _VideoCard(p: p),
                      ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _VideoCard extends StatelessWidget {
  final BlogPostSummaryResponse p;
  const _VideoCard({required this.p});
  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final date = p.publishedAt != null
        ? DateFormat('dd MMM').format(p.publishedAt!.toLocal())
        : '';
    return Container(
      decoration: const BoxDecoration(
        border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
        boxShadow: [BoxShadow(color: MmtColors.ink950, offset: Offset(4, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AspectRatio(
            aspectRatio: 16 / 9,
            child: Container(
              decoration: BoxDecoration(
                color: dark ? MmtColors.ink800 : MmtColors.chipBg,
                image: p.cover.isNotEmpty
                    ? DecorationImage(
                        image: NetworkImage(p.cover),
                        fit: BoxFit.cover,
                      )
                    : null,
              ),
              child: const Center(
                child: Icon(
                  FontAwesomeIcons.solidCirclePlay,
                  color: MmtColors.news,
                  size: 42,
                  shadows: [Shadow(color: Colors.black26, blurRadius: 8)],
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(10),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  p.title,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                  style: GoogleFonts.getFont(
                    'Archivo Black',
                    fontSize: 14,
                    height: 1.2,
                    fontWeight: FontWeight.w900,
                    color: dark ? Colors.white : MmtColors.ink950,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  [
                    if (p.author?.name.isNotEmpty ?? false) p.author!.name,
                    if (date.isNotEmpty) date,
                    if ((p.viewCount ?? 0) > 0) '${p.viewCount} views',
                  ].join('  ·  '),
                  style: GoogleFonts.inter(
                    fontSize: 11,
                    fontWeight: FontWeight.w500,
                    color: dark ? Colors.white54 : MmtColors.textFaint,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Menu (bottom-nav last tab) — home screen quick actions
// ---------------------------------------------------------------------------
class MenuScreen extends StatelessWidget {
  const MenuScreen({super.key});

  @override
  Widget build(BuildContext ctx) {
    final t = LangScope.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final items = <(IconData, String, Widget)>[
      (FontAwesomeIcons.house, t.home, const SizedBox.shrink()),
      (FontAwesomeIcons.newspaper, t.news, const SizedBox.shrink()),
      (FontAwesomeIcons.video, t.videos, const SizedBox.shrink()),
      (FontAwesomeIcons.bowlFood, t.shorts, const ShortsFeedScreen()),
      (FontAwesomeIcons.magnifyingGlass, t.search, const SearchScreen()),
      (FontAwesomeIcons.circleInfo, t.about, const AboutScreen()),
      (FontAwesomeIcons.envelope, t.contact, const ContactScreen()),
      (FontAwesomeIcons.briefcase, t.careers, const CareersScreen()),
      (FontAwesomeIcons.gaugeHigh, t.dashboard, const DashboardScreen()),
      (FontAwesomeIcons.rightToBracket, t.signIn, const LoginScreen()),
    ];
    return CustomScrollView(
      slivers: [
        SliverAppBar(
          pinned: true,
          title: const Text('Menu'),
          bottom: PreferredSize(
            preferredSize: const Size.fromHeight(2),
            child: Container(height: 2, color: MmtColors.ink950),
          ),
        ),
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 24, 20, 120),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const BrandLogo(size: 22, showTagline: true),
                const SizedBox(height: 24),
                Container(
                  decoration: const BoxDecoration(
                    border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
                  ),
                  child: Column(
                    children: [
                      for (int i = 0; i < items.length; i++) ...[
                        ListTile(
                          dense: false,
                          leading: Icon(items[i].$1, size: 18, color: dark ? Colors.white : MmtColors.ink950),
                          title: Text(
                            items[i].$2,
                            style: GoogleFonts.inter(
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              color: dark ? Colors.white : MmtColors.ink950,
                            ),
                          ),
                          trailing: Icon(Icons.chevron_right, color: dark ? Colors.white38 : MmtColors.textFaint),
                          onTap: () {
                            if (items[i].$3 is SizedBox) return;
                            Navigator.of(ctx).push(
                              MaterialPageRoute(builder: (_) => items[i].$3),
                            );
                          },
                          shape: Border(
                            bottom: BorderSide(
                              color: i == items.length - 1 ? Colors.transparent : MmtColors.divider,
                              width: 1,
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 30),
                SectionEyebrow(t.followUs),
                const SizedBox(height: 18),
                Row(
                  children: [
                    _Social(icon: FontAwesomeIcons.facebookF, url: Env.facebook),
                    const SizedBox(width: 10),
                    _Social(icon: FontAwesomeIcons.xTwitter, url: Env.twitter),
                    const SizedBox(width: 10),
                    _Social(icon: FontAwesomeIcons.instagram, url: Env.instagram),
                    const SizedBox(width: 10),
                    _Social(icon: FontAwesomeIcons.youtube, url: Env.youtube),
                    const SizedBox(width: 10),
                    _Social(icon: FontAwesomeIcons.linkedinIn, url: Env.linkedin),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _Social extends StatelessWidget {
  final IconData icon;
  final String url;
  const _Social({required this.icon, required this.url});
  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    return InkWell(
      onTap: () async {
        await launchUrlString(url, mode: LaunchMode.externalApplication);
      },
      child: Container(
        width: 40,
        height: 40,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border.all(color: MmtColors.ink950, width: 2),
          color: dark ? MmtColors.ink900 : Colors.white,
        ),
        child: Icon(icon, size: 17, color: dark ? Colors.white : MmtColors.ink950),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ABOUT
// ---------------------------------------------------------------------------
class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});
  @override
  Widget build(BuildContext ctx) {
    final t = LangScope.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    return Scaffold(
      appBar: AppBar(title: Text(t.about), bottom: _divider()),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
        children: [
          SectionEyebrow(t.aboutRole),
          const SizedBox(height: 18),
          const BrandLogo(size: 28, showTagline: true),
          const SizedBox(height: 20),
          Text(
            t.mission,
            style: GoogleFonts.inter(
              fontSize: 15,
              height: 1.7,
              color: dark ? Colors.white70 : MmtColors.ink900,
            ),
          ),
          const SizedBox(height: 30),
          _row(FontAwesomeIcons.envelope, t.contactNewsroom, Env.contactEmail),
          const SizedBox(height: 10),
          _row(FontAwesomeIcons.penToSquare, t.joinAsJournalist, 'careers@mapmytimes.com'),
          const SizedBox(height: 10),
          _row(FontAwesomeIcons.phone, t.contact, Env.contactPhone),
        ],
      ),
    );
  }
  PreferredSizeWidget _divider() => PreferredSize(
    preferredSize: const Size.fromHeight(2),
    child: Container(height: 2, color: MmtColors.ink950),
  );
  Widget _row(IconData icon, String label, String value) {
    final dark = WidgetsBinding.instance.platformDispatcher.platformBrightness == Brightness.dark;
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: MmtColors.ink950, width: 2),
      ),
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Icon(icon, size: 18, color: dark ? Colors.white : MmtColors.ink950),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: GoogleFonts.inter(
                    fontSize: 11,
                    letterSpacing: 1.2,
                    fontWeight: FontWeight.w800,
                    color: MmtColors.news,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  value,
                  style: GoogleFonts.inter(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: dark ? Colors.white : MmtColors.ink950,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// CONTACT
// ---------------------------------------------------------------------------
class ContactScreen extends StatefulWidget {
  const ContactScreen({super.key});
  @override
  State<ContactScreen> createState() => _ContactScreenState();
}
class _ContactScreenState extends State<ContactScreen> {
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _msg = TextEditingController();

  @override
  Widget build(BuildContext ctx) {
    final t = LangScope.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    return Scaffold(
      appBar: AppBar(
        title: Text(t.contact),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
        children: [
          SectionEyebrow(t.contactNewsroom),
          const SizedBox(height: 20),
          _field(t.byAuthor('Name').replaceAll('by ', ''), _name, hint: 'Your name'),
          const SizedBox(height: 14),
          _field(t.email, _email, hint: Env.contactEmail, keyboard: TextInputType.emailAddress),
          const SizedBox(height: 14),
          _field(t.search.replaceAll('Search', 'Message'), _msg, lines: 6, hint: 'How can we help?'),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () {
                ScaffoldMessenger.of(ctx).showSnackBar(
                  SnackBar(
                    backgroundColor: MmtColors.news,
                    content: Text(t.subscribeSuccess),
                  ),
                );
              },
              child: const Text('SEND MESSAGE'),
            ),
          ),
          const SizedBox(height: 30),
          Container(
            decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2)),
            padding: const EdgeInsets.all(14),
            child: Column(
              children: [
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(FontAwesomeIcons.envelope),
                  title: Text(t.contactNewsroom,
                      style: GoogleFonts.inter(fontWeight: FontWeight.w800, letterSpacing: 1, fontSize: 12, color: MmtColors.news)),
                  subtitle: Text(Env.contactEmail,
                      style: GoogleFonts.inter(fontSize: 16, fontWeight: FontWeight.w700, color: dark ? Colors.white : MmtColors.ink950)),
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(FontAwesomeIcons.phone),
                  title: Text('PHONE',
                      style: GoogleFonts.inter(fontWeight: FontWeight.w800, letterSpacing: 1, fontSize: 12, color: MmtColors.news)),
                  subtitle: Text(Env.contactPhone,
                      style: GoogleFonts.inter(fontSize: 16, fontWeight: FontWeight.w700, color: dark ? Colors.white : MmtColors.ink950)),
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(FontAwesomeIcons.locationDot),
                  title: Text('ADDRESS',
                      style: GoogleFonts.inter(fontWeight: FontWeight.w800, letterSpacing: 1, fontSize: 12, color: MmtColors.news)),
                  subtitle: Text('MAPMYTOUR LLP, India',
                      style: GoogleFonts.inter(fontSize: 16, fontWeight: FontWeight.w700, color: dark ? Colors.white : MmtColors.ink950)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _field(String label, TextEditingController c, {String? hint, int lines = 1, TextInputType? keyboard}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label.toUpperCase(),
          style: GoogleFonts.inter(
            fontSize: 11,
            fontWeight: FontWeight.w800,
            letterSpacing: 1.2,
            color: MmtColors.news,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: c,
          maxLines: lines,
          keyboardType: keyboard,
          decoration: InputDecoration(hintText: hint),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// CAREERS
// ---------------------------------------------------------------------------
class CareersScreen extends StatefulWidget {
  const CareersScreen({super.key});
  @override
  State<CareersScreen> createState() => _CareersScreenState();
}
class _CareersScreenState extends State<CareersScreen> {
  late final Future<List<JobResponse>> _load = _fetch();

  Future<List<JobResponse>> _fetch() async {
    try {
      final dio = BlogService.create().dio;
      final r = await dio.get('/api/v1/careers/jobs?size=100&status=OPEN');
      if (r.data is Map<String, dynamic> && (r.data as Map)['data'] != null) {
        final d = (r.data as Map)['data'];
        if (d is List) {
          return d.map((j) => JobResponse.fromJson(Map<String, dynamic>.from(j as Map))).toList(growable: false);
        }
        if (d is Map && d['items'] is List) {
          return (d['items'] as List)
              .map((j) => JobResponse.fromJson(Map<String, dynamic>.from(j as Map)))
              .toList(growable: false);
        }
      }
    } catch (_) {}
    return CareersScreenStateExt._fallback;
  }

  @override
  Widget build(BuildContext ctx) {
    final t = LangScope.of(ctx);
    return Scaffold(
      appBar: AppBar(
        title: Text(t.careers),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: FutureBuilder<List<JobResponse>>(
        future: _load,
        builder: (c, snap) {
          final list = snap.data ?? _fallback;
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
            children: [
              SectionEyebrow(t.openRoles.replaceAll(RegExp(r'[0-9]'), '').trim()),
              const SizedBox(height: 8),
              Text(
                '${list.length} ${t.openRoles.contains('role') ? (list.length == 1 ? 'open role' : 'open roles') : t.openRoles}',
                style: GoogleFonts.getFont('Archivo Black', fontSize: 28, fontWeight: FontWeight.w900, letterSpacing: -0.4),
              ),
              const SizedBox(height: 24),
              for (final job in list) ...[
                _JobCard(job: job, onApply: () {
                  ScaffoldMessenger.of(c).showSnackBar(
                    SnackBar(content: Text(t.applyNow + ': ${job.title}')),
                  );
                }),
                const SizedBox(height: 16),
              ],
            ],
          );
        },
      ),
    );
  }
}

class _JobCard extends StatelessWidget {
  final JobResponse job;
  final VoidCallback onApply;
  const _JobCard({required this.job, required this.onApply});
  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    return Container(
      decoration: const BoxDecoration(
        border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
        boxShadow: [BoxShadow(color: MmtColors.ink950, offset: Offset(4, 4))],
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              if (job.department != null) _chip(job.department!, MmtColors.news, Colors.white),
              if (job.employmentType != null) _chip(job.employmentType!, dark ? MmtColors.ink800 : Colors.white, dark ? Colors.white : MmtColors.ink950),
              if (job.remote == true) _chip('REMOTE', MmtColors.ink950, Colors.white),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            job.title,
            style: GoogleFonts.getFont(
              'Archivo Black',
              fontSize: 22,
              fontWeight: FontWeight.w900,
              letterSpacing: -0.2,
              color: dark ? Colors.white : MmtColors.ink950,
            ),
          ),
          const SizedBox(height: 8),
          if (job.location != null)
            Row(
              children: [
                const Icon(FontAwesomeIcons.locationDot, size: 14, color: MmtColors.news),
                const SizedBox(width: 8),
                Text(
                  job.location!,
                  style: GoogleFonts.inter(fontWeight: FontWeight.w600, fontSize: 13),
                ),
              ],
            ),
          if (job.experienceLevel != null) ...[
            const SizedBox(height: 4),
            Row(
              children: [
                const Icon(FontAwesomeIcons.layerGroup, size: 14, color: MmtColors.news),
                const SizedBox(width: 8),
                Text(
                  job.experienceLevel!,
                  style: GoogleFonts.inter(fontWeight: FontWeight.w600, fontSize: 13),
                ),
              ],
            ),
          ],
          if (job.description?.isNotEmpty ?? false) ...[
            const SizedBox(height: 14),
            Text(
              job.description!,
              maxLines: 4,
              overflow: TextOverflow.ellipsis,
              style: GoogleFonts.inter(
                fontSize: 14,
                height: 1.55,
                color: dark ? Colors.white70 : MmtColors.textMuted,
              ),
            ),
          ],
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: onApply,
              child: const Text('APPLY NOW'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _chip(String label, Color bg, Color fg) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
    decoration: BoxDecoration(
      color: bg,
      border: const Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
    ),
    child: Text(
      label.toUpperCase(),
      style: GoogleFonts.inter(
        fontSize: 10,
        fontWeight: FontWeight.w800,
        letterSpacing: 1.2,
        color: fg,
        height: 1.0,
      ),
    ),
  );
}

// ---------------------------------------------------------------------------
// LOGIN SCREEN
// ---------------------------------------------------------------------------
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}
class _LoginScreenState extends State<LoginScreen> {
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool obscure = true;

  @override
  Widget build(BuildContext ctx) {
    final t = LangScope.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    return Scaffold(
      appBar: AppBar(
        title: Text(t.signIn),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
        children: [
          const BrandLogo(size: 22, showTagline: true),
          const SizedBox(height: 28),
          Text(
            t.noAccountYet.replaceAll('account', 'session'),
            style: GoogleFonts.getFont(
              'Archivo Black',
              fontSize: 24,
              fontWeight: FontWeight.w900,
              letterSpacing: -0.3,
              color: dark ? Colors.white : MmtColors.ink950,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            t.signIn,
            style: GoogleFonts.inter(fontSize: 15, color: dark ? Colors.white70 : MmtColors.textMuted),
          ),
          const SizedBox(height: 24),
          _in(t.email, _email, keyboard: TextInputType.emailAddress, hint: Env.contactEmail),
          const SizedBox(height: 14),
          _in(t.password, _password, keyboard: TextInputType.visiblePassword, obscure: obscure, suffix: IconButton(
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 40),
            onPressed: () => setState(() => obscure = !obscure),
            icon: Icon(obscure ? Icons.visibility_off_outlined : Icons.visibility_outlined),
          )),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: () {},
              child: Text(t.forgotPassword),
            ),
          ),
          const SizedBox(height: 6),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () {
                ScaffoldMessenger.of(ctx).showSnackBar(
                  SnackBar(content: Text(t.signIn + ': ${_email.text.isEmpty ? '…' : _email.text}')),
                );
              },
              child: Text(t.signIn.toUpperCase()),
            ),
          ),
          const SizedBox(height: 20),
          Row(
            children: const [
              Expanded(child: Divider(color: MmtColors.divider, thickness: 1)),
              Padding(
                padding: EdgeInsets.symmetric(horizontal: 12),
                child: Text('OR'),
              ),
              Expanded(child: Divider(color: MmtColors.divider, thickness: 1)),
            ],
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () {},
              icon: const Icon(FontAwesomeIcons.google, size: 17),
              label: const Text('CONTINUE WITH GOOGLE'),
            ),
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () {},
              icon: const Icon(FontAwesomeIcons.facebook, size: 17, color: Color(0xFF1877F2)),
              label: const Text('CONTINUE WITH FACEBOOK'),
            ),
          ),
          const SizedBox(height: 24),
          Text(
            t.noAccountYet,
            style: GoogleFonts.inter(fontSize: 14, color: dark ? Colors.white70 : MmtColors.textMuted),
          ),
          const SizedBox(height: 6),
          OutlinedButton(
            onPressed: () {
              ScaffoldMessenger.of(ctx).showSnackBar(
                SnackBar(content: Text(t.join)),
              );
            },
            child: Text(t.join.toUpperCase()),
          ),
        ],
      ),
    );
  }

  Widget _in(String label, TextEditingController c, {TextInputType? keyboard, bool obscure = false, Widget? suffix, String? hint}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label.toUpperCase(),
          style: GoogleFonts.inter(fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1.2, color: MmtColors.news),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: c,
          obscureText: obscure,
          keyboardType: keyboard,
          decoration: InputDecoration(
            hintText: hint,
            suffixIcon: suffix,
          ),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// CAREER DETAIL (placeholder — deep-linkable /careers/:id route)
// ---------------------------------------------------------------------------
class CareerDetailScreen extends StatelessWidget {
  const CareerDetailScreen({super.key, required this.jobId});
  final String jobId;

  @override
  Widget build(BuildContext ctx) {
    final t = LangScope.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final JobResponse fallback = (CareersScreenStateExt._fallback.firstWhere(
      (j) => j.id == jobId,
      orElse: () => CareersScreenStateExt._fallback.first,
    ));

    return Scaffold(
      backgroundColor: dark ? MmtColors.ink950 : MmtColors.background,
      appBar: AppBar(
        title: Text(fallback.title),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
        children: [
          Text(
            fallback.department ?? 'Open role',
            style: GoogleFonts.inter(
              fontSize: 11,
              fontWeight: FontWeight.w800,
              letterSpacing: 1.6,
              color: MmtColors.news,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            fallback.title,
            style: GoogleFonts.getFont(
              'Archivo Black',
              fontSize: 28,
              color: dark ? Colors.white : MmtColors.ink950,
            ),
          ),
          const SizedBox(height: 18),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              if (fallback.employmentType != null)
                _JobCard.chipStatic(fallback.employmentType!, MmtColors.news, Colors.white),
              if (fallback.remote == true)
                _JobCard.chipStatic('REMOTE', MmtColors.ink950, Colors.white),
              if (fallback.experienceLevel != null)
                _JobCard.chipStatic(fallback.experienceLevel!, dark ? Colors.white : MmtColors.ink950, dark ? MmtColors.ink950 : Colors.white),
              if (fallback.location != null)
                _JobCard.chipStatic(fallback.location!, MmtColors.ink100, MmtColors.ink950),
            ],
          ),
          const SizedBox(height: 22),
          Container(
            decoration: BoxDecoration(
              color: dark ? const Color(0xFF121212) : Colors.white,
              border: const Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
              boxShadow: const [BoxShadow(color: MmtColors.ink950, offset: Offset(4, 4))],
            ),
            padding: const EdgeInsets.all(16),
            child: Text(
              fallback.description ?? 'Role description is not currently available for this opening.',
              style: GoogleFonts.inter(
                fontSize: 15,
                height: 1.65,
                color: dark ? Colors.white70 : MmtColors.ink700,
              ),
            ),
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () {
                ScaffoldMessenger.of(ctx).showSnackBar(
                  SnackBar(content: Text('${t.applyNow}: ${fallback.title}')),
                );
              },
              child: Text(t.applyNow.toUpperCase()),
            ),
          ),
        ],
      ),
    );
  }
}

// Internal helper extension to reach _fallback from sibling detail screen.
// ignore: prefer-single-declaration-per-file
extension CareersScreenStateExt on _CareersScreenState {
  static List<JobResponse> get _fallback => <JobResponse>[
        JobResponse(
          id: '1',
          slug: 'senior-political-correspondent',
          title: 'Senior Political Correspondent',
          department: 'Newsroom',
          employmentType: 'Full-time',
          experienceLevel: 'Sr. 5+ yrs',
          location: 'Bhopal · Hybrid',
          remote: false,
          description: 'Cover state politics and government affairs. Pitch, report and file 2–3 investigative stories every week. Work closely with the desk to fact-check and headline long-form analysis for the homepage and app.',
        ),
        JobResponse(
          id: '2',
          slug: 'video-editor-shorts',
          title: 'Video Editor — Shorts / Reels',
          department: 'Video',
          employmentType: 'Full-time',
          experienceLevel: '2–4 yrs',
          location: 'Remote · India',
          remote: true,
          description: 'Edit short-form videos (30–90 s) for MapMyTimes Shorts feed. Sync subtitles, sound design and kinetic typography. Coordinate with anchors and reporters to publish ~14 titles per week.',
        ),
        JobResponse(
          id: '3',
          slug: 'product-engineer-flutter',
          title: 'Product Engineer — Flutter',
          department: 'Engineering',
          employmentType: 'Full-time',
          experienceLevel: '3–6 yrs',
          location: 'Remote · India',
          remote: true,
          description: 'Build the MapMyTimes iOS/Android companion app with Riverpod, Dio and go_router. Drive the design system port (neo-brutalist tokens, Archivo Black headlines, hard shadows) and integrate blog + auth + notification microservices.',
        ),
        JobResponse(
          id: '4',
          slug: 'community-intern',
          title: 'Community & Audience Intern',
          department: 'Audience',
          employmentType: 'Internship',
          experienceLevel: '0–1 yrs',
          location: 'Bhopal · On-site',
          remote: false,
          description: 'Curate breaking news wires, monitor reader comments and publish 2–3 newsletters each month. Coordinate with social team on trending reports and WhatsApp channel distribution.',
        ),
      ];
}

// ---------------------------------------------------------------------------
// DASHBOARD (journalist)
// ---------------------------------------------------------------------------
class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});
  @override
  Widget build(BuildContext ctx) {
    final t = LangScope.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final tiles = <(IconData, String, Color)>[
      (FontAwesomeIcons.penToSquare, 'Write story', MmtColors.news),
      (FontAwesomeIcons.layerGroup, 'My posts', MmtColors.ink950),
      (FontAwesomeIcons.comments, 'Moderation', MmtColors.ink950),
      (FontAwesomeIcons.bell, 'Notifications', MmtColors.ink950),
      (FontAwesomeIcons.gear, 'Settings', MmtColors.ink950),
    ];
    return Scaffold(
      appBar: AppBar(
        title: Text(t.dashboard),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
        children: [
          Container(
            decoration: const BoxDecoration(
              color: MmtColors.news,
              border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
              boxShadow: [BoxShadow(color: MmtColors.ink950, offset: Offset(4, 4))],
            ),
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  t.tagline.toUpperCase(),
                  style: GoogleFonts.getFont(
                    'Archivo Black',
                    fontSize: 10,
                    letterSpacing: 2.2,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  'Publish stories that matter.',
                  style: GoogleFonts.getFont(
                    'Archivo Black',
                    fontSize: 24,
                    fontWeight: FontWeight.w900,
                    letterSpacing: -0.3,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  'Editor’s dashboard — compose, SEO, publish schedule.',
                  style: GoogleFonts.inter(fontSize: 14, color: Colors.white70),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          Wrap(
            spacing: 14,
            runSpacing: 14,
            children: [
              for (final (ic, label, color) in tiles)
                SizedBox(
                  width: (MediaQuery.of(ctx).size.width - 54) / 2,
                  child: InkWell(
                    onTap: () {
                      ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(content: Text(label)));
                    },
                    child: Container(
                      decoration: BoxDecoration(
                        color: color == MmtColors.news ? MmtColors.news50 : (dark ? MmtColors.ink900 : Colors.white),
                        border: const Border.fromBorderSide(
                          BorderSide(color: MmtColors.ink950, width: 2),
                        ),
                      ),
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          Container(
                            width: 36,
                            height: 36,
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: color,
                              border: const Border.fromBorderSide(
                                BorderSide(color: MmtColors.ink950, width: 2),
                              ),
                            ),
                            child: Icon(ic, size: 16, color: color == MmtColors.news ? Colors.white : Colors.white),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              label,
                              style: GoogleFonts.inter(
                                fontSize: 14,
                                fontWeight: FontWeight.w800,
                                color: dark ? Colors.white : MmtColors.ink950,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// SEARCH SCREEN (imported from app_shell, defined here to keep files small)
// ---------------------------------------------------------------------------
class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});
  @override
  State<SearchScreen> createState() => _SearchScreenState();
}
class _SearchScreenState extends State<SearchScreen> {
  final _svc = BlogService.create();
  final _q = TextEditingController();
  Future<List<BlogPostSummaryResponse>>? _fut;

  @override
  Widget build(BuildContext ctx) {
    final t = LangScope.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    return Scaffold(
      appBar: AppBar(
        title: TextField(
          controller: _q,
          autofocus: true,
          style: GoogleFonts.inter(color: dark ? Colors.white : MmtColors.ink950, fontSize: 15, fontWeight: FontWeight.w600),
          decoration: InputDecoration(
            hintText: t.search.replaceAll('Search', 'Search news…'),
            border: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            enabledBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            focusedBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.news, width: 3), borderRadius: BorderRadius.zero),
            filled: true,
            fillColor: dark ? MmtColors.ink900 : Colors.white,
          ),
          onSubmitted: (_) => setState(() {
            _fut = _svc.postsSearch(_q.text, size: 30).then((p) => p.items);
          }),
        ),
        actions: [
          TextButton(
            onPressed: () => setState(() {
              _fut = _svc.postsSearch(_q.text, size: 30).then((p) => p.items);
            }),
            child: Text(t.search.toUpperCase()),
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: _fut == null
          ? Center(child: Padding(padding: const EdgeInsets.all(30), child: Text(t.search.replaceAll('Search', 'Type to search news…'))))
          : FutureBuilder<List<BlogPostSummaryResponse>>(
              future: _fut!,
              builder: (c, snap) {
                if (snap.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator(color: MmtColors.news));
                }
                final list = snap.data ?? <BlogPostSummaryResponse>[];
                if (list.isEmpty) {
                  return Center(
                    child: Padding(
                      padding: const EdgeInsets.all(30),
                      child: Text('${t.noResultsFor} "${_q.text}"'),
                    ),
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
                  itemCount: list.length,
                  itemBuilder: (c, i) {
                    final p = list[i];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 18),
                      child: InkWell(
                        onTap: () => Navigator.of(c).push(MaterialPageRoute(
                          builder: (_) => NewsArticleScreen(postId: p.id, slug: p.slug),
                        )),
                        child: _SearchTile(p: p),
                      ),
                    );
                  },
                );
              },
            ),
    );
  }
}

class _SearchTile extends StatelessWidget {
  final BlogPostSummaryResponse p;
  const _SearchTile({required this.p});
  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final date = p.publishedAt != null
        ? DateFormat('dd MMM yyyy').format(p.publishedAt!.toLocal())
        : '';
    return Container(
      decoration: const BoxDecoration(
        border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
        boxShadow: [BoxShadow(color: MmtColors.ink950, offset: Offset(4, 4))],
      ),
      padding: const EdgeInsets.all(12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 100,
            height: 82,
            decoration: const BoxDecoration(
              border: Border(right: BorderSide(color: MmtColors.ink950, width: 2)),
            ),
            child: p.cover.isEmpty
                ? Container(color: dark ? MmtColors.ink800 : MmtColors.chipBg)
                : Image.network(p.cover, fit: BoxFit.cover, errorBuilder: (_, __, ___) => Container(color: MmtColors.chipBg)),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  p.title,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                  style: GoogleFonts.getFont(
                    'Archivo Black',
                    fontSize: 15,
                    height: 1.18,
                    fontWeight: FontWeight.w900,
                    color: dark ? Colors.white : MmtColors.ink950,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  [
                    if (p.author?.name.isNotEmpty ?? false) p.author!.name,
                    if (date.isNotEmpty) date,
                  ].join('  ·  '),
                  style: GoogleFonts.inter(
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: dark ? Colors.white54 : MmtColors.textFaint,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// Needed for DateFormat import
// ignore: unused_import
import 'package:intl/intl.dart';
