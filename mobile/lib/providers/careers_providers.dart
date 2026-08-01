// =============================================================================
// Careers providers — jobs, departments, submit application
// =============================================================================

import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/blog_models.dart';
import '../models/careers_models.dart';
import 'common_providers.dart';

typedef JobList = List<JobPostingSummaryResponse>;

final jobsListProvider = FutureProvider.autoDispose<JobList>((ref) async {
  final svc = ref.watch(careersServiceProvider);
  final page = await svc.jobsList(page: 1, size: 30, sortBy: 'createdAt', sortDir: 'DESC');
  return page.items;
});

final jobDetailProvider = FutureProvider.family.autoDispose<JobPostingResponse?, ID>((ref, id) async {
  final svc = ref.watch(careersServiceProvider);
  try { return await svc.jobGet(id); } catch (_) { return null; }
});

final departmentsProvider = FutureProvider.autoDispose<List<String>>((ref) async {
  final svc = ref.watch(careersServiceProvider);
  try { return await svc.departments(); } catch (_) { return <String>[]; }
});

final applyLoadingProvider = StateProvider.autoDispose<bool>((_) => false);
final applyErrorProvider = StateProvider.autoDispose<String?>((_) => null);
final applySuccessProvider = StateProvider.autoDispose<JobApplicationResponse?>((_) => null);

final submitApplicationProvider = Provider.autoDispose<Future<JobApplicationResponse?> Function(ApplyFormData)>((ref) {
  return (form) async {
    ref.read(applyLoadingProvider.notifier).state = true;
    ref.read(applyErrorProvider.notifier).state = null;
    try {
      final svc = ref.watch(careersServiceProvider);
      final app = await svc.submitApplication(form);
      ref.read(applySuccessProvider.notifier).state = app;
      return app;
    } catch (e) {
      ref.read(applyErrorProvider.notifier).state = e.toString();
      return null;
    } finally {
      ref.read(applyLoadingProvider.notifier).state = false;
    }
  };
});
