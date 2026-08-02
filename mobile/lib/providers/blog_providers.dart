// =============================================================================
// Blog data providers — featured, trending, latest, shorts, videos, search, categories,
// postBySlug. Uses Riverpod AsyncValue for loading/error/data states.
// =============================================================================

import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/blog_models.dart';
import 'common_providers.dart';

typedef PostList = List<BlogPostSummaryResponse>;
typedef CatList = List<CategoryResponse>;
typedef SectionList = List<SectionResponse>;

// ----------------- Featured Reports (isFeatured=true, size 10) ----------------
final featuredPostsProvider = FutureProvider.autoDispose<PostList>((ref) async {
  final svc = ref.watch(blogServiceProvider);
  final page = await svc.postsList(isFeatured: true, page: 1, size: 10, status: 'PUBLISHED');
  return page.items;
});

// ----------------- Trending Now (isTrending=true, size 8) -------------------
final trendingPostsProvider = FutureProvider.autoDispose<PostList>((ref) async {
  final svc = ref.watch(blogServiceProvider);
  final page = await svc.postsList(isTrending: true, page: 1, size: 8, status: 'PUBLISHED');
  return page.items;
});

// ----------------- Latest Stories (page, size 12) ---------------------------
final latestPostsProvider = FutureProvider.family.autoDispose<PostList, int>((ref, page) async {
  final svc = ref.watch(blogServiceProvider);
  final res = await svc.postsList(page: page, size: 12, status: 'PUBLISHED', sort: '-publishedAt');
  return res.items;
});

// ----------------- News list (tab 1, status=PUBLISHED, size 30) --------------
final newsListProvider = FutureProvider.autoDispose<PostList>((ref) async {
  final svc = ref.watch(blogServiceProvider);
  final res = await svc.postsList(page: 1, size: 30, status: 'PUBLISHED', sort: '-publishedAt');
  return res.items;
});

// ----------------- Videos (postType=VIDEO, size 20) --------------------------
final videoPostsProvider = FutureProvider.autoDispose<PostList>((ref) async {
  final svc = ref.watch(blogServiceProvider);
  final res = await svc.postsList(postType: 'VIDEO', page: 1, size: 20, status: 'PUBLISHED', sort: '-publishedAt');
  return res.items;
});

// ----------------- Shorts feed (postType=SHORT, size 20) --------------------
final shortsFeedProvider = FutureProvider.autoDispose<PostList>((ref) async {
  final svc = ref.watch(blogServiceProvider);
  final res = await svc.postsList(postType: 'SHORT', page: 1, size: 20, status: 'PUBLISHED', sort: '-publishedAt');
  return res.items;
});

// ----------------- Categories ------------------------------------------------
final categoriesProvider = FutureProvider.autoDispose<CatList>((ref) async {
  final svc = ref.watch(blogServiceProvider);
  final all = await svc.categoriesList();
  return all;
});

// ----------------- Sections --------------------------------------------------
final sectionsProvider = FutureProvider.autoDispose<SectionList>((ref) async {
  final svc = ref.watch(blogServiceProvider);
  try {
    final all = await svc.sectionsList();
    if (all.isNotEmpty) return all;
  } catch (_) {}
  return const <SectionResponse>[];
});

// ----------------- Posts by Section Slug -------------------------------------
final sectionPostsProvider = FutureProvider.family.autoDispose<PostList, String>((ref, sectionSlug) async {
  final svc = ref.watch(blogServiceProvider);
  final res = await svc.postsList(sectionSlug: sectionSlug, page: 1, size: 30, status: 'PUBLISHED', sort: '-publishedAt');
  return res.items;
});

// ----------------- Single post by slug or id ---------------------------------
final postByIdProvider = FutureProvider.family.autoDispose<BlogPostResponse?, String>((ref, id) async {
  final svc = ref.watch(blogServiceProvider);
  try {
    final r = await svc.postById(id);
    if (r != null) return r;
  } catch (e, st) {
    print('postById PROVIDER id=$id ERROR: $e\n$st');
  }
  throw Exception('Could not load article by ID: $id');
});

final postBySlugProvider = FutureProvider.family.autoDispose<BlogPostResponse?, String>((ref, slug) async {
  final svc = ref.watch(blogServiceProvider);
  try {
    final r = await svc.postBySlug(slug);
    if (r != null) return r;
  } catch (e, st) {
    print('postBySlug PROVIDER slug=$slug ERROR: $e\n$st');
  }
  // Try by ID if slug is actually UUID/ID
  try {
    final r = await svc.postById(slug);
    if (r != null) return r;
  } catch (e, st) {
    print('postById PROVIDER fallback slug=$slug ERROR: $e\n$st');
  }
  throw Exception('Could not load article (slug/ID: $slug)');
});

// ----------------- Search ----------------------------------------------------
class SearchQuery {
  final String keyword;
  final int page;
  final int size;
  const SearchQuery(this.keyword, {this.page = 1, this.size = 30});
  @override bool operator ==(Object other) =>
      other is SearchQuery && other.keyword == keyword && other.page == page && other.size == size;
  @override int get hashCode => Object.hash(keyword, page, size);
}

final searchPostsProvider = FutureProvider.family.autoDispose<PostList, SearchQuery>((ref, q) async {
  if (q.keyword.trim().isEmpty) return const <BlogPostSummaryResponse>[];
  final svc = ref.watch(blogServiceProvider);
  final res = await svc.postsSearch(q.keyword, page: q.page, size: q.size);
  return res.items;
});

// ----------------- View increment (call via ref.read, not watch) -------------
final postViewIncrementProvider = Provider.autoDispose<void Function(ID id)>((ref) {
  final svc = ref.watch(blogServiceProvider);
  return (ID id) async => svc.incrementView(id);
});
