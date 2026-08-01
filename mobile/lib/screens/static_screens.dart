// About, Contact, Careers, Login, Videos, Menu, Dashboard, Search, CareerApply screens
// ALL INTEGRATED with Riverpod backend providers
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher_string.dart';
import '../core/env.dart';
import '../core/theme/colors.dart';
import '../core/l10n/dict.dart';
import '../core/widgets/brand.dart';
import '../models/auth_models.dart';
import '../models/blog_models.dart';
import '../models/careers_models.dart';
import '../models/notification_models.dart';
import '../providers/index.dart';

// ---------------------------------------------------------------------------
// VIDEOS
// ---------------------------------------------------------------------------
class VideosScreen extends ConsumerStatefulWidget {
  const VideosScreen({super.key});
  @override
  ConsumerState<VideosScreen> createState() => _VideosScreenState();
}
class _VideosScreenState extends ConsumerState<VideosScreen> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  Widget _skel(double h, {double w = double.infinity}) => Container(
        height: h, width: w,
        decoration: BoxDecoration(
          border: Border.all(color: MmtColors.ink700, width: 2),
          color: MmtColors.ink600.withOpacity(0.12),
        ),
      );

  @override
  Widget build(BuildContext ctx) {
    super.build(ctx);
    final t = Dict.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final videosAsync = ref.watch(videoPostsProvider);

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
        videosAsync.when(
          loading: () => SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
              child: Wrap(
                spacing: 18, runSpacing: 18,
                children: [
                  for (int i = 0; i < 6; i++)
                    SizedBox(
                      width: (MediaQuery.of(ctx).size.width - 58) / 2,
                      child: _skel(180),
                    ),
                ],
              ),
            ),
          ),
          error: (e, _) => SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: _ErrorCard(
                msg: e.toString(),
                retry: () => ref.invalidate(videoPostsProvider),
                t: t,
              ),
            ),
          ),
          data: (list) {
            if (list.isEmpty) {
              return SliverToBoxAdapter(
                child: Padding(padding: const EdgeInsets.all(30), child: Center(child: Text(t.noStoriesYet))),
              );
            }
            return SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
                child: Wrap(
                  spacing: 18,
                  runSpacing: 18,
                  children: [
                    for (final p in list)
                      SizedBox(
                        width: (MediaQuery.of(ctx).size.width - 58) / 2,
                        child: InkWell(
                          onTap: () => ctx.push('/article/${p.slug}?id=${p.id}'),
                          child: _VideoCard(p: p, dark: dark),
                        ),
                      ),
                  ],
                ),
              ),
            );
          },
        ),
      ],
    );
  }
}

class _VideoCard extends StatelessWidget {
  final BlogPostSummaryResponse p;
  final bool dark;
  const _VideoCard({required this.p, required this.dark});

  @override
  Widget build(BuildContext ctx) {
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
// MENU (bottom-nav last tab) — shows user info, logout if authenticated
// ---------------------------------------------------------------------------
class MenuScreen extends ConsumerWidget {
  const MenuScreen({super.key});

  @override
  Widget build(BuildContext ctx, WidgetRef ref) {
    final t = Dict.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final authState = ref.watch(authControllerProvider);
    final currentUser = authState.user;
    final isAuthed = authState.isAuthenticated;

    final menuItems = <(IconData, String, String)>[
      (FontAwesomeIcons.house, t.home, '/'),
      (FontAwesomeIcons.newspaper, t.news, '/news'),
      (FontAwesomeIcons.video, t.videos, '/videos'),
      (FontAwesomeIcons.magnifyingGlass, t.search, '/search'),
      (FontAwesomeIcons.circleInfo, t.about, '/about'),
      (FontAwesomeIcons.envelope, t.contact, '/contact'),
      (FontAwesomeIcons.briefcase, t.careers, '/careers'),
    ];

    final extraItems = <(IconData, String, String)>[
      if (isAuthed) (FontAwesomeIcons.gaugeHigh, t.dashboard, '/dashboard'),
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
                // User card
                if (isAuthed) ...[
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      border: Border.all(color: MmtColors.ink950, width: 2),
                      color: MmtColors.news50,
                      boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 48, height: 48,
                          decoration: BoxDecoration(
                            color: MmtColors.news,
                            border: Border.all(color: MmtColors.ink950, width: 2),
                          ),
                          alignment: Alignment.center,
                          child: Text(
                            (currentUser?.displayName ?? currentUser?.email ?? 'U').substring(0, 1).toUpperCase(),
                            style: GoogleFonts.archivoBlack(color: Colors.white, fontSize: 22),
                          ),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                currentUser?.displayName ?? currentUser?.email ?? 'Signed in',
                                style: GoogleFonts.archivoBlack(
                                  fontSize: 16,
                                  color: dark ? Colors.white : MmtColors.ink950,
                                ),
                              ),
                              if ((currentUser?.email ?? '').isNotEmpty)
                                Text(
                                  currentUser!.email!,
                                  style: GoogleFonts.inter(fontSize: 12, color: MmtColors.ink600),
                                ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 14),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: () async {
                        await ref.read(authControllerProvider.notifier).logout();
                      },
                      icon: const Icon(FontAwesomeIcons.rightFromBracket, size: 16),
                      label: Text('${t.signIn} OUT'),
                      style: OutlinedButton.styleFrom(
                        side: const BorderSide(color: MmtColors.ink950, width: 2),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                ] else ...[
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: () => ctx.push('/login'),
                      icon: const Icon(FontAwesomeIcons.rightToBracket, size: 16),
                      label: Text(t.signIn.toUpperCase()),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: MmtColors.news,
                        foregroundColor: Colors.white,
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                ],
                Container(
                  decoration: const BoxDecoration(
                    border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
                  ),
                  child: Column(
                    children: [
                      for (int i = 0; i < menuItems.length; i++)
                        _MenuTile(
                          ic: menuItems[i].$1,
                          label: menuItems[i].$2,
                          path: menuItems[i].$3,
                          dark: dark,
                          last: false,
                        ),
                      for (int i = 0; i < extraItems.length; i++)
                        _MenuTile(
                          ic: extraItems[i].$1,
                          label: extraItems[i].$2,
                          path: extraItems[i].$3,
                          dark: dark,
                          last: false,
                        ),
                      _MenuTile(
                        ic: FontAwesomeIcons.bowlFood,
                        label: t.shorts,
                        path: '/shorts',
                        dark: dark,
                        last: true,
                      ),
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

class _MenuTile extends StatelessWidget {
  final IconData ic;
  final String label;
  final String path;
  final bool dark;
  final bool last;
  const _MenuTile({required this.ic, required this.label, required this.path, required this.dark, required this.last});

  @override
  Widget build(BuildContext ctx) {
    return ListTile(
      dense: false,
      leading: Icon(ic, size: 18, color: dark ? Colors.white : MmtColors.ink950),
      title: Text(
        label,
        style: GoogleFonts.inter(
          fontSize: 15, fontWeight: FontWeight.w700,
          color: dark ? Colors.white : MmtColors.ink950,
        ),
      ),
      trailing: Icon(Icons.chevron_right, color: dark ? Colors.white38 : MmtColors.textFaint),
      onTap: () {
        if (path.startsWith('/shorts')) {
          ctx.push(path);
        } else if (path.startsWith('/search') || path.startsWith('/about') || path.startsWith('/contact') || path.startsWith('/careers') || path.startsWith('/dashboard') || path.startsWith('/login')) {
          ctx.push(path);
        } else {
          ctx.go(path);
        }
      },
      shape: Border(
        bottom: BorderSide(
          color: last ? Colors.transparent : MmtColors.divider,
          width: 1,
        ),
      ),
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
    final t = Dict.of(ctx);
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
          _row(FontAwesomeIcons.envelope, t.contactNewsroom, Env.contactEmail, dark),
          const SizedBox(height: 10),
          _row(FontAwesomeIcons.penToSquare, t.joinAsJournalist, 'careers@mapmytimes.com', dark),
          const SizedBox(height: 10),
          _row(FontAwesomeIcons.phone, t.contact, Env.contactPhone, dark),
        ],
      ),
    );
  }
  PreferredSizeWidget _divider() => PreferredSize(
    preferredSize: const Size.fromHeight(2),
    child: Container(height: 2, color: MmtColors.ink950),
  );
  Widget _row(IconData icon, String label, String value, bool dark) {
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
                  label.toUpperCase(),
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
// CONTACT — submit to SupportService
// ---------------------------------------------------------------------------
class ContactScreen extends ConsumerStatefulWidget {
  const ContactScreen({super.key});
  @override
  ConsumerState<ContactScreen> createState() => _ContactScreenState();
}
class _ContactScreenState extends ConsumerState<ContactScreen> {
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _phone = TextEditingController();
  final _subject = TextEditingController();
  final _msg = TextEditingController();
  bool _submitting = false;
  bool _submitted = false;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _phone.dispose();
    _subject.dispose();
    _msg.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final t = Dict.of(context);
    if (_name.text.trim().isEmpty || _email.text.trim().isEmpty || _subject.text.trim().isEmpty || _msg.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(backgroundColor: MmtColors.news700, content: Text('Please fill Name, Email, Subject and Message')),
      );
      return;
    }
    setState(() { _submitting = true; _submitted = false; });
    try {
      final supportSvc = ref.read(supportServiceProvider);
      final lang = LangScope.codeOf(context);
      await supportSvc.submitContactForm(ContactFormRequest(
        name: _name.text.trim(),
        email: _email.text.trim(),
        phone: _phone.text.trim().isEmpty ? null : _phone.text.trim(),
        subject: _subject.text.trim(),
        message: _msg.text.trim(),
        source: 'mapmytimes-mobile',
        preferredLanguage: lang.name,
      ));
      if (mounted) {
        setState(() { _submitted = true; });
        _name.clear(); _email.clear(); _phone.clear(); _subject.clear(); _msg.clear();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(backgroundColor: MmtColors.news, content: Text(t.subscribeSuccess)),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(backgroundColor: MmtColors.news700, content: Text('${t.somethingWentWrong}: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext ctx) {
    final t = Dict.of(ctx);
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
          if (_submitted)
            Container(
              padding: const EdgeInsets.all(12),
              margin: const EdgeInsets.only(bottom: 20),
              decoration: BoxDecoration(
                border: Border.all(color: MmtColors.ink950, width: 2),
                color: MmtColors.news50,
              ),
              child: Row(children: [
                const Icon(Icons.check_circle, color: MmtColors.news),
                const SizedBox(width: 10),
                Expanded(child: Text(t.subscribeSuccess, style: const TextStyle(fontWeight: FontWeight.w700))),
              ]),
            ),
          _field('Name', _name, hint: 'Your name'),
          const SizedBox(height: 14),
          _field(t.email, _email, hint: Env.contactEmail, keyboard: TextInputType.emailAddress),
          const SizedBox(height: 14),
          _field('Phone (optional)', _phone, hint: '+91…', keyboard: TextInputType.phone),
          const SizedBox(height: 14),
          _field('Subject', _subject, hint: 'How can we help?'),
          const SizedBox(height: 14),
          _field('Message', _msg, lines: 6, hint: 'Your message…'),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _submitting ? null : _submit,
              child: _submitting
                  ? const SizedBox(height: 16, width: 16, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                  : const Text('SEND MESSAGE'),
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
                  title: Text(t.contactNewsroom.toUpperCase(),
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
          decoration: InputDecoration(
            hintText: hint,
            border: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            enabledBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            focusedBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.news, width: 3), borderRadius: BorderRadius.zero),
            filled: true,
            fillColor: Colors.white,
          ),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// CAREERS LIST
// ---------------------------------------------------------------------------
class CareersScreen extends ConsumerWidget {
  const CareersScreen({super.key});

  @override
  Widget build(BuildContext ctx, WidgetRef ref) {
    final t = Dict.of(ctx);
    final jobsAsync = ref.watch(jobsListProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(t.careers),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: jobsAsync.when(
        loading: () => ListView(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
          children: [
            SectionEyebrow(t.openRoles),
            const SizedBox(height: 24),
            for (int i = 0; i < 4; i++) ...[
              Container(
                height: 180,
                decoration: BoxDecoration(
                  border: Border.all(color: MmtColors.ink700, width: 2),
                  color: MmtColors.ink100,
                ),
              ),
              const SizedBox(height: 16),
            ],
          ],
        ),
        error: (e, _) => ListView(
          padding: const EdgeInsets.all(20),
          children: [
            _ErrorCard(msg: e.toString(), retry: () => ref.invalidate(jobsListProvider), t: t),
          ],
        ),
        data: (list) {
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
            children: [
              SectionEyebrow(t.openRoles),
              const SizedBox(height: 8),
              Text(
                '${list.length} ${t.openRoles}',
                style: GoogleFonts.getFont('Archivo Black', fontSize: 28, fontWeight: FontWeight.w900, letterSpacing: -0.4),
              ),
              const SizedBox(height: 24),
              if (list.isEmpty)
                Center(child: Text(t.noOpenRoles, style: const TextStyle(fontSize: 15, color: MmtColors.textMuted)))
              else
                for (final job in list) ...[
                  InkWell(
                    onTap: () => ctx.push('/careers/${job.id}'),
                    child: _JobCard(
                      job: job,
                      onApply: () => ctx.push('/careers/${job.id}/apply'),
                    ),
                  ),
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
  final JobPostingSummaryResponse job;
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
              if ((job.department ?? '').isNotEmpty)
                _chip(job.department!, MmtColors.news, Colors.white),
              if ((job.jobType ?? '').isNotEmpty)
                _chip(job.jobType!, dark ? MmtColors.ink800 : Colors.white, dark ? Colors.white : MmtColors.ink950),
              if (job.remote == true)
                _chip('REMOTE', MmtColors.ink950, Colors.white),
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
          if ((job.location ?? '').isNotEmpty)
            Row(
              children: [
                const Icon(FontAwesomeIcons.locationDot, size: 14, color: MmtColors.news),
                const SizedBox(width: 8),
                Text(job.location!, style: GoogleFonts.inter(fontWeight: FontWeight.w600, fontSize: 13)),
              ],
            ),
          if ((job.experienceLevel ?? '').isNotEmpty) ...[
            const SizedBox(height: 4),
            Row(
              children: [
                const Icon(FontAwesomeIcons.layerGroup, size: 14, color: MmtColors.news),
                const SizedBox(width: 8),
                Text(job.experienceLevel!, style: GoogleFonts.inter(fontWeight: FontWeight.w600, fontSize: 13)),
              ],
            ),
          ],
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: onApply,
              style: ElevatedButton.styleFrom(backgroundColor: MmtColors.news, foregroundColor: Colors.white),
              child: Text(Dict.of(ctx).applyNow.toUpperCase()),
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
// CAREER DETAIL
// ---------------------------------------------------------------------------
class CareerDetailScreen extends ConsumerWidget {
  const CareerDetailScreen({super.key, required this.jobId});
  final String jobId;

  @override
  Widget build(BuildContext ctx, WidgetRef ref) {
    final t = Dict.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final jobAsync = ref.watch(jobDetailProvider(jobId));

    return Scaffold(
      backgroundColor: dark ? MmtColors.ink950 : MmtColors.background,
      appBar: AppBar(
        title: Text(t.careers),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: jobAsync.when(
        loading: () => ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Container(height: 30, width: 120, decoration: BoxDecoration(color: MmtColors.news50, border: Border.all(color: MmtColors.ink950, width: 2))),
            const SizedBox(height: 14),
            Container(height: 60, decoration: BoxDecoration(color: MmtColors.ink100, border: Border.all(color: MmtColors.ink950, width: 2))),
            const SizedBox(height: 20),
            Container(height: 220, decoration: BoxDecoration(color: MmtColors.ink100, border: Border.all(color: MmtColors.ink950, width: 2))),
          ],
        ),
        error: (e, _) => ListView(
          padding: const EdgeInsets.all(20),
          children: [_ErrorCard(msg: e.toString(), retry: () => ref.invalidate(jobDetailProvider(jobId)), t: t)],
        ),
        data: (j) {
          if (j == null) {
            return ListView(
              padding: const EdgeInsets.all(30),
              children: const [Center(child: Text('Job opening not found.'))],
            );
          }
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
            children: [
              Text(
                (j.department ?? 'Open role').toUpperCase(),
                style: GoogleFonts.inter(
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 1.6,
                  color: MmtColors.news,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                j.title,
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
                  if ((j.jobType ?? '').isNotEmpty)
                    _JobCard(
                      job: j,
                      onApply: () {},
                    )._chip(j.jobType!, MmtColors.news, Colors.white),
                  if (j.remote == true)
                    _JobCard(job: j, onApply: (){})._chip('REMOTE', MmtColors.ink950, Colors.white),
                  if ((j.experienceLevel ?? '').isNotEmpty)
                    _JobCard(job: j, onApply: (){})._chip(j.experienceLevel!, dark ? Colors.white : MmtColors.ink950, dark ? MmtColors.ink950 : Colors.white),
                  if ((j.location ?? '').isNotEmpty)
                    _JobCard(job: j, onApply: (){})._chip(j.location!, MmtColors.ink100, MmtColors.ink950),
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
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if ((j.description ?? '').isNotEmpty) ...[
                      _sectionTitle('About the role'),
                      const SizedBox(height: 8),
                      Text(
                        j.description!,
                        style: GoogleFonts.inter(fontSize: 15, height: 1.65, color: dark ? Colors.white70 : MmtColors.ink700),
                      ),
                    ],
                    if ((j.responsibilities ?? '').isNotEmpty) ...[
                      const SizedBox(height: 18),
                      _sectionTitle('Responsibilities'),
                      const SizedBox(height: 8),
                      Text(j.responsibilities!, style: GoogleFonts.inter(fontSize: 14.5, height: 1.65, color: dark ? Colors.white70 : MmtColors.ink700)),
                    ],
                    if ((j.requirements ?? '').isNotEmpty) ...[
                      const SizedBox(height: 18),
                      _sectionTitle('Requirements'),
                      const SizedBox(height: 8),
                      Text(j.requirements!, style: GoogleFonts.inter(fontSize: 14.5, height: 1.65, color: dark ? Colors.white70 : MmtColors.ink700)),
                    ],
                    if ((j.benefits ?? '').isNotEmpty) ...[
                      const SizedBox(height: 18),
                      _sectionTitle('Benefits'),
                      const SizedBox(height: 8),
                      Text(j.benefits!, style: GoogleFonts.inter(fontSize: 14.5, height: 1.65, color: dark ? Colors.white70 : MmtColors.ink700)),
                    ],
                    if ((j.skills ?? <String>[]).isNotEmpty) ...[
                      const SizedBox(height: 18),
                      _sectionTitle('Skills'),
                      const SizedBox(height: 10),
                      Wrap(
                        spacing: 8, runSpacing: 8,
                        children: [
                          for (final s in j.skills!)
                            _JobCard(job: j, onApply: (){})._chip(s, MmtColors.news50, MmtColors.ink950),
                        ],
                      ),
                    ],
                    if ((j.salaryRange ?? '').isNotEmpty) ...[
                      const SizedBox(height: 18),
                      _sectionTitle('Salary range'),
                      const SizedBox(height: 8),
                      Text(j.salaryRange!, style: GoogleFonts.archivoBlack(fontSize: 20, color: MmtColors.news)),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () => ctx.push('/careers/${j.id}/apply'),
                  style: ElevatedButton.styleFrom(backgroundColor: MmtColors.news, foregroundColor: Colors.white),
                  child: Text(t.applyNow.toUpperCase()),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _sectionTitle(String s) => Text(
    s.toUpperCase(),
    style: GoogleFonts.inter(fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 1.4, color: MmtColors.news),
  );
}

// ---------------------------------------------------------------------------
// CAREER APPLY (NEW) — form with multipart application submit
// ---------------------------------------------------------------------------
class CareerApplyScreen extends ConsumerStatefulWidget {
  const CareerApplyScreen({super.key, required this.jobId});
  final String jobId;
  @override
  ConsumerState<CareerApplyScreen> createState() => _CareerApplyScreenState();
}
class _CareerApplyScreenState extends ConsumerState<CareerApplyScreen> {
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _phone = TextEditingController();
  final _cover = TextEditingController();
  final _currCtc = TextEditingController();
  final _expCtc = TextEditingController();
  final _notice = TextEditingController();
  final _li = TextEditingController();
  final _portfolio = TextEditingController();

  @override
  void dispose() {
    _name.dispose(); _email.dispose(); _phone.dispose(); _cover.dispose();
    _currCtc.dispose(); _expCtc.dispose(); _notice.dispose();
    _li.dispose(); _portfolio.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final t = Dict.of(context);
    if (_name.text.trim().isEmpty || _email.text.trim().isEmpty || _phone.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(backgroundColor: MmtColors.news700, content: Text('Please fill Name, Email and Phone')),
      );
      return;
    }
    try {
      final submitFn = ref.read(submitApplicationProvider);
      final resp = await submitFn(ApplyFormData(
        jobId: widget.jobId,
        applicantName: _name.text.trim(),
        applicantEmail: _email.text.trim(),
        applicantPhone: _phone.text.trim(),
        coverLetter: _cover.text.trim().isEmpty ? null : _cover.text.trim(),
        currentCtc: _currCtc.text.trim().isEmpty ? null : _currCtc.text.trim(),
        expectedCtc: _expCtc.text.trim().isEmpty ? null : _expCtc.text.trim(),
        noticePeriod: _notice.text.trim().isEmpty ? null : _notice.text.trim(),
        linkedinUrl: _li.text.trim().isEmpty ? null : _li.text.trim(),
        portfolioUrl: _portfolio.text.trim().isEmpty ? null : _portfolio.text.trim(),
      ));
      if (mounted) {
        if (resp != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(backgroundColor: MmtColors.news, content: Text('${t.applyNow}: Submitted (ID: ${resp.id})')),
          );
          Navigator.of(context).pop();
        } else {
          final err = ref.read(applyErrorProvider);
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(backgroundColor: MmtColors.news700, content: Text(err ?? t.somethingWentWrong)),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(backgroundColor: MmtColors.news700, content: Text(e.toString())),
        );
      }
    }
  }

  @override
  Widget build(BuildContext ctx) {
    final t = Dict.of(ctx);
    final loading = ref.watch(applyLoadingProvider);
    final err = ref.watch(applyErrorProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(t.applyNow),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
        children: [
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(border: Border.all(color: MmtColors.ink950, width: 2), color: MmtColors.news50),
            child: Row(
              children: [
                const Icon(FontAwesomeIcons.briefcase, color: MmtColors.news),
                const SizedBox(width: 12),
                Expanded(child: Text('Job ID: ${widget.jobId}', style: const TextStyle(fontWeight: FontWeight.w800))),
              ],
            ),
          ),
          if (err != null) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(border: Border.all(color: MmtColors.news700, width: 2), color: MmtColors.news50),
              child: Text(err, style: const TextStyle(color: MmtColors.news700, fontWeight: FontWeight.w700)),
            ),
          ],
          const SizedBox(height: 20),
          _formField('Full Name *', _name),
          const SizedBox(height: 14),
          _formField('Email *', _email, keyboard: TextInputType.emailAddress),
          const SizedBox(height: 14),
          _formField('Phone *', _phone, keyboard: TextInputType.phone),
          const SizedBox(height: 14),
          _formField('Cover Letter', _cover, lines: 4, hint: 'Why are you a good fit?'),
          const SizedBox(height: 14),
          _formField('Current CTC', _currCtc),
          const SizedBox(height: 14),
          _formField('Expected CTC', _expCtc),
          const SizedBox(height: 14),
          _formField('Notice Period', _notice, hint: 'e.g. 30 days / Immediate'),
          const SizedBox(height: 14),
          _formField('LinkedIn URL', _li, keyboard: TextInputType.url),
          const SizedBox(height: 14),
          _formField('Portfolio URL', _portfolio, keyboard: TextInputType.url),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: loading ? null : _submit,
              style: ElevatedButton.styleFrom(backgroundColor: MmtColors.news, foregroundColor: Colors.white),
              child: loading
                  ? const SizedBox(height: 16, width: 16, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                  : Text(t.applyNow.toUpperCase()),
            ),
          ),
        ],
      ),
    );
  }

  Widget _formField(String label, TextEditingController c, {int lines = 1, TextInputType? keyboard, String? hint}) {
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
          keyboardType: keyboard,
          maxLines: lines,
          decoration: InputDecoration(
            hintText: hint,
            border: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            enabledBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            focusedBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.news, width: 3), borderRadius: BorderRadius.zero),
            filled: true,
            fillColor: Colors.white,
          ),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// LOGIN SCREEN — integrates authControllerProvider
// ---------------------------------------------------------------------------
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key, this.returnTo});
  final String? returnTo;

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}
class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool obscure = true;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final t = Dict.of(context);
    if (_email.text.trim().isEmpty || _password.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(backgroundColor: MmtColors.news700, content: Text('Enter email and password')),
      );
      return;
    }
    final ctrl = ref.read(authControllerProvider.notifier);
    final u = await ctrl.login(LoginRequest(
      email: _email.text.trim(),
      password: _password.text,
    ));
    if (mounted && u != null) {
      // Navigate to returnTo OR home
      final r = widget.returnTo;
      if (r != null && r.isNotEmpty) {
        context.go(r);
      } else {
        context.go('/');
      }
    }
  }

  @override
  Widget build(BuildContext ctx) {
    final t = Dict.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final authLoading = ref.watch(authLoadingProvider);
    final authErr = ref.watch(authErrorProvider);
    final authed = ref.watch(isAuthenticatedProvider);

    // Already logged in — redirect
    if (authed) {
      final r = widget.returnTo;
      Future.microtask(() => context.go(r ?? '/'));
    }

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
            'Welcome back.',
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
          if (authErr != null)
            Container(
              padding: const EdgeInsets.all(12),
              margin: const EdgeInsets.only(bottom: 16),
              decoration: BoxDecoration(border: Border.all(color: MmtColors.news700, width: 2), color: MmtColors.news50),
              child: Text(authErr, style: const TextStyle(color: MmtColors.news700, fontWeight: FontWeight.w700, fontSize: 12.5)),
            ),
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
              onPressed: authLoading ? null : _submit,
              style: ElevatedButton.styleFrom(backgroundColor: MmtColors.news, foregroundColor: Colors.white),
              child: authLoading
                  ? const SizedBox(height: 16, width: 16, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                  : Text(t.signIn.toUpperCase()),
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
              onPressed: () {
                ScaffoldMessenger.of(ctx).showSnackBar(const SnackBar(content: Text('Google login coming soon')));
              },
              icon: const Icon(FontAwesomeIcons.google, size: 17),
              label: const Text('CONTINUE WITH GOOGLE'),
            ),
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () {
                ScaffoldMessenger.of(ctx).showSnackBar(const SnackBar(content: Text('Facebook login coming soon')));
              },
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
            border: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            enabledBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            focusedBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.news, width: 3), borderRadius: BorderRadius.zero),
            filled: true,
            fillColor: Colors.white,
          ),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// DASHBOARD — auth-guarded via GoRouter redirect; shows current user
// ---------------------------------------------------------------------------
class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});
  @override
  Widget build(BuildContext ctx, WidgetRef ref) {
    final t = Dict.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final user = ref.watch(currentUserProvider);

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
                  'Welcome, ${user?.displayName ?? user?.firstName ?? user?.email ?? 'Journalist'}',
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
                  user?.email ?? 'Editor’s dashboard — compose, SEO, publish schedule.',
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
                      ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(content: Text('$label — coming soon')));
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
                            child: Icon(ic, size: 16, color: Colors.white),
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
// SEARCH SCREEN — debounced searchPostsProvider
// ---------------------------------------------------------------------------
class _QNotifier extends StateNotifier<String> {
  _QNotifier() : super('');
  void set(String s) => state = s;
}
final _queryProvider = StateNotifierProvider<_QNotifier, String>((_) => _QNotifier());
Timer? _debounce;

class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key});
  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}
class _SearchScreenState extends ConsumerState<SearchScreen> {
  final _q = TextEditingController();
  String _submitted = '';

  @override
  void dispose() {
    _q.dispose();
    super.dispose();
  }

  void _onInput(String v) {
    ref.read(_queryProvider.notifier).set(v);
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 450), () {
      if (mounted) {
        setState(() => _submitted = v);
      }
    });
  }

  void _forceSearch() {
    setState(() => _submitted = _q.text);
  }

  @override
  Widget build(BuildContext ctx) {
    final t = Dict.of(ctx);
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final resultsAsync = ref.watch(searchPostsProvider(SearchQuery(_submitted)));

    return Scaffold(
      appBar: AppBar(
        title: TextField(
          controller: _q,
          autofocus: true,
          onChanged: _onInput,
          onSubmitted: (_) => _forceSearch(),
          style: GoogleFonts.inter(color: dark ? Colors.white : MmtColors.ink950, fontSize: 15, fontWeight: FontWeight.w600),
          decoration: InputDecoration(
            hintText: '${t.search} news…',
            border: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            enabledBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.ink950, width: 2), borderRadius: BorderRadius.zero),
            focusedBorder: const OutlineInputBorder(borderSide: BorderSide(color: MmtColors.news, width: 3), borderRadius: BorderRadius.zero),
            filled: true,
            fillColor: dark ? MmtColors.ink900 : Colors.white,
          ),
        ),
        actions: [
          TextButton(
            onPressed: _forceSearch,
            child: Text(t.search.toUpperCase()),
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(height: 2, color: MmtColors.ink950),
        ),
      ),
      body: _submitted.trim().isEmpty
          ? Center(child: Padding(padding: const EdgeInsets.all(30), child: Text('Type to search news…')))
          : resultsAsync.when(
              loading: () => const Center(child: CircularProgressIndicator(color: MmtColors.news)),
              error: (e, _) => Padding(
                padding: const EdgeInsets.all(30),
                child: Center(child: _ErrorCard(msg: e.toString(), retry: _forceSearch, t: t)),
              ),
              data: (list) {
                if (list.isEmpty) {
                  return Center(
                    child: Padding(
                      padding: const EdgeInsets.all(30),
                      child: Text('${t.noResultsFor} "$_submitted"'),
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
                        onTap: () => ctx.push('/article/${p.slug}?id=${p.id}'),
                        child: _SearchTile(p: p, dark: dark),
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
  final bool dark;
  const _SearchTile({required this.p, required this.dark});

  @override
  Widget build(BuildContext ctx) {
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

// ---------------------------------------------------------------------------
// Shared error card (used by most screens)
// ---------------------------------------------------------------------------
class _ErrorCard extends StatelessWidget {
  final String msg;
  final VoidCallback retry;
  final Dict t;
  const _ErrorCard({required this.msg, required this.retry, required this.t});

  @override
  Widget build(BuildContext ctx) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        border: Border.all(color: MmtColors.ink950, width: 2),
        color: MmtColors.news50,
        boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('⚠ ${t.common.loadingError}', style: const TextStyle(color: MmtColors.news700, fontWeight: FontWeight.w900, fontSize: 14)),
          const SizedBox(height: 8),
          Text(msg, style: const TextStyle(fontSize: 12.5, color: MmtColors.ink700, fontWeight: FontWeight.w600)),
          const SizedBox(height: 14),
          InkWell(
            onTap: retry,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                border: Border.all(color: MmtColors.ink950, width: 2),
                color: Colors.white,
                boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)],
              ),
              child: Text(t.common.retry.toUpperCase(),
                  style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 11, letterSpacing: 1.4)),
            ),
          ),
        ],
      ),
    );
  }
}
