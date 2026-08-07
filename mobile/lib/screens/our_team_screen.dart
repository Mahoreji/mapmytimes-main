import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import '../core/l10n/dict.dart';
import '../core/theme/index.dart';
import '../models/blog_models.dart';
import '../providers/index.dart';

const Map<String, String> _deptHindiBio = {
  'FOUNDER': 'मैपमाईटाइम्स की स्थापना और संपादकीय नेतृत्व।',
  'EDITOR_IN_CHIEF': 'संपादकीय नीति, निष्पक्षता और गुणवत्ता की ज़िम्मेदारी।',
  'MANAGING_DIRECTOR': 'रणनीति, प्रबंधन और विकास की ज़िम्मेदारी।',
  'CHIEF_MARKETING_OFFICER': 'ब्रांड, मार्केटिंग और डिजिटल रणनीति की अगुवाई।',
  'CHIEF_TECHNOLOGY_OFFICER': 'टेक्नोलॉजी, प्रोडक्ट डेवलपमेंट और इनोवेशन।',
  'CHIEF_OPERATING_OFFICER': 'ऑपरेशन्स, टीम मैनेजमेंट और प्रोसेस की ज़िम्मेदारी।',
  'OPERATIONS_HEAD': 'ऑपरेशन्स, टीम मैनेजमेंट और प्रोसेस की ज़िम्मेदारी।',
  'BUREAU_CHIEF': 'ब्यूरो शहर/क्षेत्र की खबरों और टीम का नेतृत्व।',
  'GROUND_REPORTER': 'ग्राउंड रिपोर्टिंग और ज़मीनी खबरों की कवरेज।',
  'SENIOR_GROUND_REPORTER': 'ग्राउंड रिपोर्टिंग और ब्रेकिंग न्यूज़ कवरेज।',
  'INVESTIGATIONS_EDITOR': 'अनुसंधान, जाँचपड़ताल और गहरी रिपोर्टिंग।',
  'PRINCIPAL_CORRESPONDENT': 'महत्वपूर्ण बीट, रिपोर्टिंग और विशेष कवरेज।',
  'FEATURES_JOURNALIST': 'फीचर, कल्चर, लाइफस्टाइल और कहानी लेखन।',
  'CITY_BEAT_REPORTER': 'शहर की हर छोटी-बड़ी खबर ताज़ा और सटीक।',
  'VIDEO_JOURNALIST': 'वीडियो खबरें, पैकेज और ऑन-ग्राउंड शूटिंग।',
  'CAMERAMAN': 'वीडियोग्राफी और कैमरा ऑपरेशन्स।',
  'PHOTO_EDITOR': 'फोटोग्राफी और इमेज एडिटिंग डेस्क।',
  'COPY_EDITOR': 'कॉपी, भाषा, सच्चाई और फैक्ट चेकिंग।',
  'NEWS_DESK': 'न्यूज़ डेस्क, बुलेटिन और संपादन।',
  'SUB_EDITOR': 'समाचारों का संपादन, फैक्ट-चेक और भाषा शुद्धता।',
  'CHIEF_EDITOR': 'संपादकीय नीति और पूरी न्यूज़रूम का नेतृत्व।',
  'COLUMNIST': 'विशेष स्तंभ, विश्लेषण और राय लेखन।',
  'CARTOONIST': 'कार्टून, इलस्ट्रेशन और विज़ुअल स्टोरीटेलिंग।',
  'PRODUCER': 'वीडियो, शो और बुलेटिन प्रोडक्शन।',
  'ASSOCIATE_PRODUCER': 'शो और वीडियो प्रोडक्शन की सहायता।',
  'ARCHIVIST': 'संग्रह, अभिलेख और डिजिटल लाइब्रेरी प्रबंधन।',
  'ADMIN_STAFF': 'प्रशासनिक सहायता और ऑफिस ऑपरेशन्स।',
  'ACCOUNTS': 'वित्त, लेखा और भुगतान प्रबंधन।',
  'HR': 'मानव संसाधन, भर्ती और कर्मचारी विकास।',
  'PUBLIC_RELATIONS': 'सार्वजनिक संबंध और मीडिया आउटरीच।',
  'EVENT_MANAGER': 'इवेंट, कार्यक्रम और आयोजन प्रबंधन।',
  'TRAINEE': 'प्रशिक्षणार्थी — शिक्षण और क्षेत्र अनुभव।',
  'INTERN': 'इंटर्न — समाचार शिक्षण और प्रारंभिक अनुभव।',
  'DIGITAL_MARKETING': 'डिजिटल मार्केटिंग और सोशल मीडिया रणनीति।',
  'AUDIENCE_ENGAGEMENT': 'पाठकों और दर्शकों से जुड़ाव और समुदाय निर्माण।',
  'UX_DESIGNER': 'यूजर एक्सपीरियंस और इंटरफ़ेस डिज़ाइन।',
  'PRODUCT_MANAGER': 'प्रोडक्ट रोडमैप और फीचर्स का नेतृत्व।',
  'SOFTWARE_ENGINEER': 'सॉफ्टवेयर डेवलपमेंट और प्लेटफ़ॉर्म इंजीनियरिंग।',
  'DATA_ANALYST': 'डेटा विश्लेषण और दर्शक अंतर्दृष्टि।',
  'LEGAL_COUNSEL': 'कानूनी सलाह और मीडिया कानून अनुपालन।',
  'FACT_CHECKER': 'फ़ैक्ट-चेकिंग, पड़ताल और सत्यापन।',
  '_DEFAULT': 'पत्रकारिता और समाचार विभाग में योगदान।',
};

String _hindiBioFor(String? dept, String? designation) {
  if (designation != null && designation.isNotEmpty) {
    final key = designation
        .toUpperCase()
        .replaceAll(RegExp(r'[^A-Z0-9]+'), '_')
        .replaceAll(RegExp(r'^_|_$'), '');
    if (_deptHindiBio.containsKey(key)) return _deptHindiBio[key]!;
  }
  return _deptHindiBio[dept] ?? _deptHindiBio['_DEFAULT']!;
}

class OurTeamScreen extends ConsumerStatefulWidget {
  const OurTeamScreen({super.key});
  @override
  ConsumerState<OurTeamScreen> createState() => _OurTeamScreenState();
}

class _OurTeamScreenState extends ConsumerState<OurTeamScreen> {
  Widget _skel(double h, {double w = double.infinity}) => Container(
        height: h,
        width: w,
        decoration: BoxDecoration(
          border: Border.all(color: MmtColors.ink700, width: 2),
          color: MmtColors.ink600.withValues(alpha: 0.12),
        ),
      );

  Widget _cardSkel() => Container(
    decoration: BoxDecoration(
      color: MmtColors.surface,
      borderRadius: BorderRadius.circular(18),
      border: Border.all(color: MmtColors.ink950.withValues(alpha: 0.08), width: 1),
      boxShadow: [
        BoxShadow(
          color: MmtColors.ink950.withValues(alpha: 0.06),
          offset: const Offset(0, 6),
          blurRadius: 20,
          spreadRadius: 0,
        ),
      ],
    ),
    padding: const EdgeInsets.all(14),
    child: Column(
    mainAxisSize: MainAxisSize.min,
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      Container(
        decoration: BoxDecoration(
          color: MmtColors.ink100,
          borderRadius: BorderRadius.circular(14),
        ),
        child: const AspectRatio(aspectRatio: 3.0 / 4.0, child: SizedBox.expand()),
      ),
      const SizedBox(height: 14),
      _skel(15, w: 180),
      const SizedBox(height: 6),
      _skel(11, w: 170),
      const SizedBox(height: 10),
      _skel(10, w: double.infinity),
      const SizedBox(height: 4),
      _skel(10, w: 200),
      const SizedBox(height: 12),
      _skel(10, w: 80),
    ],
    ),
  );

  Future<void> _onRefresh() async {
    ref.invalidate(staffListProvider);
    await ref.read(staffListProvider.future);
  }

  Widget _headerSection(BuildContext ctx, Brightness mode, Dict t) {
    final dark = mode == Brightness.dark;
    final ink = dark ? Colors.white : MmtColors.ink950;
    final faint = dark ? Colors.white70 : MmtColors.ink700;
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 6),
        child: Column(
          children: [
            Text(
              'OUR STRENGTH',
              style: GoogleFonts.inter(
                fontWeight: FontWeight.w900,
                fontSize: 12,
                letterSpacing: 3,
                color: MmtColors.news,
              ),
            ),
            const SizedBox(height: 10),
            RichText(
              textAlign: TextAlign.center,
              text: TextSpan(
                style: GoogleFonts.getFont(
                  'Archivo Black',
                  fontSize: 34,
                  height: 1.05,
                  letterSpacing: -0.2,
                  color: ink,
                ),
                children: [
                  const TextSpan(text: 'Meet Our '),
                  TextSpan(
                    text: 'Team',
                    style: TextStyle(color: MmtColors.news),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Container(
              width: 76,
              height: 4,
              decoration: BoxDecoration(
                color: MmtColors.news,
                borderRadius: BorderRadius.circular(4),
              ),
            ),
            const SizedBox(height: 18),
            Text(
              'MapMyTimes की मज़बूत टीम जो आपके लिए दिन-रात काम करती है, ताकि आपको मिले सच्ची, निष्पक्ष और ज़मीनी खबरें।',
              textAlign: TextAlign.center,
              style: GoogleFonts.inter(
                fontSize: 14.5,
                fontWeight: FontWeight.w500,
                height: 1.6,
                color: faint,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _featuresStrip(BuildContext ctx, Brightness mode) {
    final dark = mode == Brightness.dark;
    final ink = dark ? Colors.white : MmtColors.ink950;
    final faint = dark ? Colors.white70 : MmtColors.ink600;
    final items = [
      _FeatureStripItem(icon: Icons.shield_outlined, title: 'सच के प्रति प्रतिबद्ध', body: 'हमारी पहली प्राथमिकता है सच्ची और निष्पक्ष पत्रकारिता।'),
      _FeatureStripItem(icon: Icons.group_outlined, title: 'टीम वर्क', body: 'एकजुट टीम जो आपके लिए दिन-रात काम करती है।'),
      _FeatureStripItem(icon: Icons.public_outlined, title: 'ज़मीनी जुड़ाव', body: 'देश के हर कोने से जुड़कर लाते हैं आपके लिए असली खबरें।'),
      _FeatureStripItem(icon: Icons.my_location_outlined, title: 'हमारा लक्ष्य', body: 'जनता की आवाज़ को बुलंदी देना और बदलाव की मिसाल बनना।'),
    ];
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 10, 20, 120),
        child: Container(
          decoration: BoxDecoration(
            color: dark ? MmtColors.ink900 : const Color(0xFFF5F5F5),
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: (dark ? MmtColors.ink700 : MmtColors.ink950).withValues(alpha: dark ? 0.3 : 0.08)),
          ),
          child: Column(
            children: [
              for (int i = 0; i < items.length; i++) ...[
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        width: 56,
                        height: 56,
                        decoration: BoxDecoration(
                          color: MmtColors.news.withValues(alpha: 0.06),
                          borderRadius: BorderRadius.circular(18),
                          border: Border.all(color: MmtColors.news.withValues(alpha: 0.22), width: 2),
                        ),
                        child: Icon(items[i].icon, color: MmtColors.news, size: 24),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              items[i].title,
                              style: GoogleFonts.getFont(
                                'Archivo Black',
                                fontSize: 17,
                                letterSpacing: 0.2,
                                color: ink,
                                height: 1.1,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              items[i].body,
                              style: GoogleFonts.inter(
                                fontSize: 12.5,
                                fontWeight: FontWeight.w500,
                                height: 1.55,
                                color: faint,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                if (i < items.length - 1)
                  Divider(
                    height: 1,
                    thickness: 1,
                    color: (dark ? MmtColors.ink700 : MmtColors.ink200).withValues(alpha: dark ? 0.5 : 0.8),
                  ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext ctx) {
    final mode = Theme.of(ctx).brightness;
    final dark = mode == Brightness.dark;
    final t = Dict.of(ctx);
    final listAsync = ref.watch(staffListProvider);

    return Scaffold(
      body: RefreshIndicator(
        color: MmtColors.news,
        onRefresh: _onRefresh,
        child: CustomScrollView(
          slivers: [
            SliverAppBar(
              pinned: true,
              floating: false,
              backgroundColor: dark ? MmtColors.ink950 : MmtColors.surface,
              surfaceTintColor: Colors.transparent,
              title: Text(t.ourTeam),
              titleTextStyle: GoogleFonts.getFont(
                'Archivo Black',
                fontSize: 22,
                fontWeight: FontWeight.w900,
                letterSpacing: -0.2,
                color: dark ? Colors.white : MmtColors.ink950,
              ),
              bottom: PreferredSize(
                preferredSize: const Size.fromHeight(2),
                child: Container(height: 2, color: MmtColors.ink950),
              ),
            ),
            _headerSection(ctx, mode, t),
            listAsync.when(
              loading: () => SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 20),
                  child: GridView(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 4,
                    mainAxisSpacing: 12,
                    crossAxisSpacing: 10,
                    mainAxisExtent: 440,
                  ),
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  children: [
                    for (int i = 0; i < 8; i++) _cardSkel(),
                  ],
                ),
                ),
              ),
              error: (e, _) => SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      border: Border.all(color: MmtColors.ink950, width: 2),
                      color: MmtColors.news50,
                      boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '⚠ ${t.common.loadingError}',
                          style: const TextStyle(color: MmtColors.news700, fontWeight: FontWeight.w900, fontSize: 14),
                        ),
                        const SizedBox(height: 8),
                        Text(e.toString(), style: const TextStyle(fontSize: 12, color: MmtColors.ink700)),
                        const SizedBox(height: 14),
                        InkWell(
                          onTap: _onRefresh,
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                            decoration: BoxDecoration(
                              border: Border.all(color: MmtColors.ink950, width: 2),
                              color: Colors.white,
                              boxShadow: const [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)],
                            ),
                            child: Text(
                              t.common.retry.toUpperCase(),
                              style: GoogleFonts.inter(fontWeight: FontWeight.w900, fontSize: 11, letterSpacing: 1.4),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              data: (items) {
                if (items.isEmpty) {
                  return SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.all(30),
                      child: Center(child: Text(t.noStoriesYet, style: MmtText.body(mode: mode))),
                    ),
                  );
                }
                return SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 10, 16, 30),
                    child: GridView(
                      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 4,
                        mainAxisSpacing: 12,
                        crossAxisSpacing: 10,
                        mainAxisExtent: 440,
                      ),
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      children: [
                        for (final s in items)
                          _StaffCard(s: s, dark: dark, mode: mode),
                      ],
                    ),
                  ),
                );
              },
            ),
            _featuresStrip(ctx, mode),
          ],
        ),
      ),
    );
  }
}

class _FeatureStripItem {
  final IconData icon;
  final String title;
  final String body;
  _FeatureStripItem({required this.icon, required this.title, required this.body});
}

class _StaffCard extends StatelessWidget {
  final StaffListCardDTO s;
  final bool dark;
  final Brightness mode;
  const _StaffCard({required this.s, required this.dark, required this.mode});

  String _initials(String name) {
    final parts = name.trim().split(RegExp(r'\s+')).where((w) => w.isNotEmpty).toList();
    if (parts.isEmpty) return 'MM';
    if (parts.length == 1) return parts[0][0].toUpperCase();
    return '${parts[0][0].toUpperCase()}${parts[1][0].toUpperCase()}';
  }

  @override
  Widget build(BuildContext ctx) {
    final cardBg = dark ? MmtColors.ink900 : Colors.white;
    final ink = dark ? Colors.white : MmtColors.ink950;
    final faint = dark ? Colors.white70 : MmtColors.ink600;
    final deptStr = s.department != null && s.department!.isNotEmpty
        ? s.department!.replaceAll(RegExp(r'[_\s]+'), ' ').split(' ').map((w) => w.isNotEmpty ? '${w[0].toUpperCase()}${w.substring(1).toLowerCase()}' : '').join(' ')
        : '';
    final subline = (s.designation != null && s.designation!.isNotEmpty) ? s.designation! : deptStr;
    final bio = _hindiBioFor(s.department, s.designation);
    final initials = _initials(s.fullName);
    final hasPhoto = (s.photoUrl ?? '').isNotEmpty;
    final iconFg = dark ? Colors.white70 : MmtColors.ink600;
    final iconBorder = dark ? MmtColors.ink700 : MmtColors.ink200;

    return Container(
      decoration: BoxDecoration(
        color: cardBg,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: (dark ? MmtColors.ink700 : MmtColors.ink950).withValues(alpha: dark ? 0.3 : 0.08), width: 1),
        boxShadow: [
          BoxShadow(
            color: MmtColors.ink950.withValues(alpha: 0.06),
            offset: const Offset(0, 6),
            blurRadius: 20,
            spreadRadius: 0,
          ),
        ],
      ),
      padding: const EdgeInsets.all(10),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            decoration: BoxDecoration(
              color: dark ? MmtColors.ink800 : MmtColors.ink100,
              borderRadius: BorderRadius.circular(12),
            ),
            clipBehavior: Clip.antiAlias,
            child: AspectRatio(
              aspectRatio: 3.0 / 4.0,
              child: Stack(
                fit: StackFit.expand,
                children: [
                  if (hasPhoto)
                    Image.network(
                      s.photoUrl!,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => _InitialsPlaceholder(dark: dark, initials: initials),
                    )
                  else
                    _InitialsPlaceholder(dark: dark, initials: initials),
                ],
              ),
            ),
          ),
          const SizedBox(height: 10),
          Text(
            s.fullName,
            textAlign: TextAlign.left,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: GoogleFonts.getFont(
              'Archivo Black',
              fontSize: 13,
              fontWeight: FontWeight.w900,
              letterSpacing: 0.1,
              color: ink,
              height: 1.15,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            subline,
            textAlign: TextAlign.left,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: GoogleFonts.inter(
              fontSize: 10.5,
              fontWeight: FontWeight.w800,
              color: MmtColors.news,
              height: 1.25,
            ),
          ),
          const SizedBox(height: 7),
          Expanded(
            child: Text(
              bio,
              textAlign: TextAlign.left,
              maxLines: 4,
              overflow: TextOverflow.ellipsis,
              style: GoogleFonts.inter(
                fontSize: 10,
                fontWeight: FontWeight.w500,
                color: faint,
                height: 1.45,
              ),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Container(
                width: 30,
                height: 30,
                decoration: BoxDecoration(
                  color: cardBg,
                  borderRadius: BorderRadius.circular(7),
                  border: Border.all(color: iconBorder, width: 1),
                ),
                child: FaIcon(FontAwesomeIcons.envelope, size: 12, color: iconFg),
              ),
              const SizedBox(width: 8),
              Container(
                width: 30,
                height: 30,
                decoration: BoxDecoration(
                  color: cardBg,
                  borderRadius: BorderRadius.circular(7),
                  border: Border.all(color: iconBorder, width: 1),
                ),
                child: FaIcon(FontAwesomeIcons.linkedinIn, size: 12, color: iconFg),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _InitialsPlaceholder extends StatelessWidget {
  final bool dark;
  final String initials;
  const _InitialsPlaceholder({required this.dark, required this.initials});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: dark
          ? const LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [MmtColors.ink800, MmtColors.ink900],
            )
          : const LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFFFEF2F2), Color(0xFFFFF7ED)],
            ),
      ),
      child: Stack(
        children: [
          Positioned(
            right: -30,
            top: -30,
            child: Container(
              width: 140,
              height: 140,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    MmtColors.news.withValues(alpha: dark ? 0.20 : 0.14),
                    Colors.transparent,
                  ],
                  stops: const [0.0, 1.0],
                ),
              ),
            ),
          ),
          Center(
            child: Text(
              initials,
              style: GoogleFonts.getFont(
                'Archivo Black',
                fontSize: 54,
                fontWeight: FontWeight.w900,
                letterSpacing: 1.5,
                color: dark
                    ? MmtColors.news.withValues(alpha: 0.85)
                    : MmtColors.news.withValues(alpha: 0.75),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
