import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../core/l10n/dict.dart';
import '../core/theme/index.dart';
import '../providers/index.dart';

class VerifyPressScreen extends ConsumerStatefulWidget {
  const VerifyPressScreen({super.key});
  @override
  ConsumerState<VerifyPressScreen> createState() => _VerifyPressScreenState();
}

class _VerifyPressScreenState extends ConsumerState<VerifyPressScreen> {
  final _formKey = GlobalKey<FormState>();
  final _idController = TextEditingController();
  bool _submitted = false;
  String? _activeId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final id = GoRouterState.of(context).uri.queryParameters['id'];
      if (id != null && id.isNotEmpty) {
        _idController.text = id.toUpperCase();
        _submitted = true;
        _activeId = id.toUpperCase();
      }
    });
  }

  @override
  void dispose() {
    _idController.dispose();
    super.dispose();
  }

  void _submit() {
    if (_formKey.currentState?.validate() ?? false) {
      setState(() {
        _submitted = true;
        _activeId = _idController.text.toUpperCase();
      });
    }
  }

  @override
  Widget build(BuildContext ctx) {
    final dark = Theme.of(ctx).brightness == Brightness.dark;
    final mode = Theme.of(ctx).brightness;
    final t = Dict.of(ctx);

    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            pinned: true,
            floating: false,
            backgroundColor: dark ? MmtColors.ink950 : MmtColors.surface,
            surfaceTintColor: Colors.transparent,
            leading: IconButton(
              onPressed: () => ctx.canPop() ? ctx.pop() : ctx.go('/'),
              icon: FaIcon(
                FontAwesomeIcons.arrowLeft,
                size: 18,
                color: dark ? Colors.white : MmtColors.ink950,
              ),
            ),
            title: Text(t.verifyPress),
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
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 24, 20, 120),
              child: Column(
                children: [
                  Container(
                    padding: const EdgeInsets.all(18),
                    decoration: BoxDecoration(
                      border: Border.all(color: MmtColors.ink950, width: 2),
                      color: dark ? MmtColors.ink850 : Colors.white,
                      boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
                    ),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            t.verifyPress.toUpperCase(),
                            style: GoogleFonts.inter(
                              fontWeight: FontWeight.w900,
                              fontSize: 12,
                              letterSpacing: 1.4,
                              color: dark ? Colors.white : MmtColors.ink950,
                            ),
                          ),
                          const SizedBox(height: 14),
                          TextFormField(
                            controller: _idController,
                            textCapitalization: TextCapitalization.characters,
                            style: GoogleFonts.robotoMono(
                              fontSize: 16,
                              fontWeight: FontWeight.w700,
                              color: dark ? Colors.white : MmtColors.ink950,
                              letterSpacing: 1.2,
                            ),
                            decoration: InputDecoration(
                              hintText: 'e.g. MP-28-PM-07-08-26-000001',
                              helperText: 'Format: STATE-RTO-INITIALS-DD-MM-YY-######',
                              helperMaxLines: 2,
                              helperStyle: GoogleFonts.inter(
                                fontSize: 11,
                                fontWeight: FontWeight.w600,
                                color: dark ? Colors.white54 : MmtColors.ink600,
                                height: 1.35,
                              ),
                              hintStyle: GoogleFonts.robotoMono(
                                fontSize: 15,
                                fontWeight: FontWeight.w500,
                                color: dark ? Colors.white38 : MmtColors.textFaint,
                              ),
                              filled: true,
                              fillColor: dark ? MmtColors.ink900 : MmtColors.chipBg,
                              border: const OutlineInputBorder(
                                borderRadius: BorderRadius.zero,
                                borderSide: BorderSide(color: MmtColors.ink950, width: 2),
                              ),
                              enabledBorder: const OutlineInputBorder(
                                borderRadius: BorderRadius.zero,
                                borderSide: BorderSide(color: MmtColors.ink950, width: 2),
                              ),
                              focusedBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.zero,
                                borderSide: BorderSide(color: MmtColors.news, width: 2),
                              ),
                              contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                              prefixIcon: Padding(
                                padding: const EdgeInsets.only(left: 12, right: 10),
                                child: FaIcon(
                                  FontAwesomeIcons.idCard,
                                  size: 18,
                                  color: dark ? Colors.white54 : MmtColors.ink600,
                                ),
                              ),
                            ),
                            validator: (v) {
                              if (v == null || v.trim().isEmpty) return 'Enter Press ID';
                              return null;
                            },
                            onFieldSubmitted: (_) => _submit(),
                          ),
                          const SizedBox(height: 16),
                          SizedBox(
                            width: double.infinity,
                            child: InkWell(
                              onTap: _submit,
                              child: Container(
                                padding: const EdgeInsets.symmetric(vertical: 14),
                                decoration: const BoxDecoration(
                                  color: MmtColors.news,
                                  border: Border.fromBorderSide(BorderSide(color: MmtColors.ink950, width: 2)),
                                  boxShadow: [BoxShadow(offset: Offset(3, 3), color: MmtColors.ink950)],
                                ),
                                alignment: Alignment.center,
                                child: Text(
                                  t.verifyPress.toUpperCase(),
                                  style: GoogleFonts.inter(
                                    fontWeight: FontWeight.w900,
                                    fontSize: 13,
                                    letterSpacing: 1.6,
                                    color: Colors.white,
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (_submitted && _activeId != null) ...[
                    const SizedBox(height: 24),
                    _buildResultCard(ctx, dark, mode, t, _activeId!),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildResultCard(BuildContext ctx, bool dark, Brightness mode, Dict t, String id) {
    final async = ref.watch(staffVerifyProvider(id));
    return async.when(
      loading: () => Container(
        padding: const EdgeInsets.all(18),
        decoration: BoxDecoration(
          border: Border.all(color: MmtColors.ink950, width: 2),
          color: dark ? MmtColors.ink850 : Colors.white,
          boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
        ),
        child: Column(
          children: [
            Container(height: 42, color: MmtColors.ink200),
            const SizedBox(height: 16),
            Container(height: 140, color: MmtColors.ink200),
            const SizedBox(height: 14),
            for (int i = 0; i < 4; i++) ...[
              Container(height: 14, margin: const EdgeInsets.only(bottom: 8), color: MmtColors.ink200),
            ],
          ],
        ),
      ),
      error: (e, _) => Container(
        padding: const EdgeInsets.all(16),
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
              onTap: () => ref.invalidate(staffVerifyProvider(id)),
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
      data: (d) {
        if (d == null) {
          return Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              border: Border.all(color: MmtColors.ink950, width: 2),
              color: dark ? MmtColors.ink850 : Colors.white,
              boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
            ),
            child: Column(
              children: [
                FaIcon(FontAwesomeIcons.circleXmark, size: 56, color: MmtColors.danger),
                const SizedBox(height: 14),
                Text(
                  'NOT FOUND',
                  style: GoogleFonts.getFont(
                    'Archivo Black',
                    fontSize: 22,
                    color: MmtColors.danger,
                    letterSpacing: 1.0,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'No press record found for ID: $id',
                  textAlign: TextAlign.center,
                  style: MmtText.body(mode: mode),
                ),
              ],
            ),
          );
        }
        final bool verified = d.status.toUpperCase() == 'ACTIVE' || d.status.toUpperCase() == 'VERIFIED';
        final headerColor = verified ? const Color(0xFF15803D) : MmtColors.danger;
        String validTillStr = '—';
        if (d.validTill != null) {
          if (d.validTill is DateTime) {
            validTillStr = DateFormat('dd MMM yyyy').format(d.validTill as DateTime);
          } else {
            validTillStr = d.validTill.toString();
          }
        }
        return Container(
          decoration: BoxDecoration(
            border: Border.all(color: MmtColors.ink950, width: 2),
            boxShadow: const [BoxShadow(offset: Offset(4, 4), color: MmtColors.ink950)],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 16),
                color: headerColor,
                child: Column(
                  children: [
                    FaIcon(
                      verified ? FontAwesomeIcons.shieldHalved : FontAwesomeIcons.circleExclamation,
                      size: 36,
                      color: Colors.white,
                    ),
                    const SizedBox(height: 10),
                    Text(
                      verified ? 'VERIFIED' : 'EXPIRED',
                      style: GoogleFonts.getFont(
                        'Archivo Black',
                        fontSize: 28,
                        color: Colors.white,
                        letterSpacing: 2.0,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      verified ? 'Press ID is valid and active' : 'Press ID has expired or been revoked',
                      style: GoogleFonts.inter(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: Colors.white.withValues(alpha: 0.88),
                      ),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(18),
                child: Column(
                  children: [
                    Container(
                      width: 140,
                      height: 140,
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        border: Border.all(color: MmtColors.ink950, width: 2),
                        color: Colors.white,
                      ),
                      child: Container(
                        decoration: BoxDecoration(
                          border: Border.all(color: MmtColors.ink950, width: 1),
                          color: dark ? MmtColors.ink700 : MmtColors.chipBg,
                        ),
                        child: Center(
                          child: FaIcon(
                            FontAwesomeIcons.qrcode,
                            size: 72,
                            color: dark ? Colors.white54 : MmtColors.ink700,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 18),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        border: Border.all(color: MmtColors.ink950, width: 1.5),
                        color: dark ? MmtColors.ink900 : MmtColors.chipBg,
                      ),
                      child: Text(
                        d.idNumber,
                        style: GoogleFonts.robotoMono(
                          fontSize: 14,
                          fontWeight: FontWeight.w800,
                          color: dark ? Colors.white : MmtColors.ink950,
                          letterSpacing: 1.0,
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    _row('Full Name', d.fullName, dark, mode),
                    const SizedBox(height: 10),
                    _row('Designation', d.designation ?? '', dark, mode),
                    const SizedBox(height: 10),
                    _row('Location', [d.city, d.state].where((x) => x != null && x.isNotEmpty).join(', '), dark, mode),
                    const SizedBox(height: 10),
                    _row('Valid Till', validTillStr, dark, mode),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                          decoration: BoxDecoration(
                            border: Border.all(color: MmtColors.ink950, width: 1.5),
                            color: verified ? const Color(0xFF15803D) : MmtColors.danger,
                          ),
                          child: Text(
                            d.status.toUpperCase(),
                            style: GoogleFonts.inter(
                              fontSize: 11,
                              fontWeight: FontWeight.w900,
                              color: Colors.white,
                              letterSpacing: 1.4,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 20),
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        border: Border.all(color: MmtColors.ink950, width: 1.5),
                        color: dark ? MmtColors.ink900.withValues(alpha: 0.5) : MmtColors.surfaceLight,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'VERIFICATION TERMS',
                            style: GoogleFonts.inter(
                              fontSize: 10,
                              fontWeight: FontWeight.w900,
                              letterSpacing: 1.4,
                              color: dark ? Colors.white60 : MmtColors.textMuted,
                            ),
                          ),
                          const SizedBox(height: 10),
                          for (final term in [
                            'This verification confirms active press accreditation.',
                            'ID is non-transferable and for official use only.',
                            'Valid for journalistic reporting purposes.',
                            'Subject to MAPMYTOUR LLP terms and policies.',
                          ])
                            Padding(
                              padding: const EdgeInsets.only(bottom: 6),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Container(
                                    width: 14,
                                    height: 14,
                                    margin: const EdgeInsets.only(right: 8, top: 2),
                                    decoration: BoxDecoration(
                                      shape: BoxShape.circle,
                                      border: Border.all(color: MmtColors.news, width: 1.5),
                                      color: MmtColors.news,
                                    ),
                                    child: Center(
                                      child: FaIcon(FontAwesomeIcons.check, size: 8, color: Colors.white),
                                    ),
                                  ),
                                  Expanded(
                                    child: Text(
                                      term,
                                      style: GoogleFonts.inter(
                                        fontSize: 11,
                                        fontWeight: FontWeight.w500,
                                        height: 1.4,
                                        color: dark ? Colors.white70 : MmtColors.ink700,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _row(String label, String value, bool dark, Brightness mode) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 88,
          child: Text(
            label.toUpperCase(),
            style: GoogleFonts.inter(
              fontSize: 10,
              fontWeight: FontWeight.w800,
              letterSpacing: 1.0,
              color: dark ? Colors.white54 : MmtColors.textMuted,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            value,
            style: GoogleFonts.inter(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: dark ? Colors.white : MmtColors.ink950,
            ),
          ),
        ),
      ],
    );
  }
}
