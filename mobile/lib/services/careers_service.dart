// =============================================================================
// CareersService — /api/v1/jobs + /api/v1/applications (blog/careers endpoints
// Mirror: frontend/src/lib/api/careersApi.ts
// =============================================================================

import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';
import '../core/env.dart';
import '../models/blog_models.dart';
import '../models/careers_models.dart';
import 'common.dart';

class CareersService {
  CareersService._(this._dio);
  final Dio _dio;
  static const _jobsV1 = '/api/v1/jobs';
  static const _appsV1 = '/api/v1/applications';

  static CareersService create({Dio? existing}) =>
      CareersService._(existing ?? createDio(base: Env.apiBaseUrl));

  void setBearerToken(String? token) {
    if (token == null || token.isEmpty) {
      _dio.options.headers.remove('Authorization');
    } else {
      _dio.options.headers['Authorization'] = 'Bearer $token';
    }
  }

  String _q(Map<String, Object?> p) {
    final parts = <String>[];
    p.forEach((k, v) {
      if (v == null) return;
      if (v is Iterable) { for (final e in v) {
        parts.add('$k=${Uri.encodeQueryComponent(e.toString())}');
      } return; }
      final s = v.toString(); if (s.isEmpty) return;
      parts.add('$k=${Uri.encodeQueryComponent(s)}');
    });
    return parts.isEmpty ? '' : '?${parts.join('&')}';
  }

  APIResponse<T> _env<T>(Response r, T Function(Object?) p) => parseEnvelope<T>(r, p);
  T _unwrap<T>(APIResponse<T> e) => unwrapEnvelope(e);

  PaginatedResponse<JobPostingSummaryResponse> _paginatedJobs(Object? j, {int page = 1, int size = 20}) =>
      parsePaginated<JobPostingSummaryResponse>(
        j,
        (Object? e) => JobPostingSummaryResponse.fromJson(Map<String, dynamic>.from(e as Map)),
        fallbackPage: page,
        fallbackSize: size,
      );

  // ===========================================================================
  // JOBS
  // ===========================================================================
  Future<PaginatedResponse<JobPostingSummaryResponse>> jobsList({
    int page = 1,
    int size = 20,
    String? sortBy,
    String? sortDir = 'DESC',
    String? department,
    String? jobType,
    String? experienceLevel,
  }) async {
    final q = _q(<String, Object?>{
      'page': page,
      'size': size,
      if (sortBy != null) 'sortBy': sortBy,
      if (sortDir != null) 'sortDir': sortDir,
      if (department != null) 'department': department,
      if (jobType != null) 'jobType': jobType,
      if (experienceLevel != null) 'experienceLevel': experienceLevel,
    });
    final r = await _dio.get('$_jobsV1$q');
    final env = _env<PaginatedResponse<JobPostingSummaryResponse>>(
      r,
      (Object? j) => _paginatedJobs(j, page: page, size: size),
    );
    return _unwrap(env);
  }

  Future<PaginatedResponse<JobPostingSummaryResponse>> jobsSearch(String query, {int page = 1, int size = 20}) async {
    final q = _q({'query': query, 'page': page, 'size': size});
    final r = await _dio.get('$_jobsV1/search$q');
    final env = _env<PaginatedResponse<JobPostingSummaryResponse>>(
      r, (Object? j) => _paginatedJobs(j, page: page, size: size),
    );
    return _unwrap(env);
  }

  Future<JobPostingResponse> jobGet(ID id) async {
    final r = await _dio.get('$_jobsV1/$id');
    final env = _env<JobPostingResponse>(
      r,
      (Object? j) => JobPostingResponse.fromJson(Map<String, dynamic>.from(j as Map)),
    );
    return _unwrap(env);
  }

  Future<List<String>> departments() async {
    final r = await _dio.get('$_jobsV1/departments');
    final env = _env<List<dynamic>>(r, (Object? j) {
      if (j is List) return j;
      if (j is Map) {
        if (j['departments'] is List) return j['departments'] as List<dynamic>;
        if (j['data'] is List) return j['data'] as List<dynamic>;
      }
      return <dynamic>[];
    });
    final list = _unwrap(env);
    return list.map((e) => e.toString()).toList(growable: false);
  }

  // ===========================================================================
  // APPLICATIONS
  // ===========================================================================
  Future<JobApplicationResponse> submitApplication(ApplyFormData form) async {
    final fd = FormData();
    fd.fields.add(MapEntry('jobId', form.jobId));
    fd.fields.add(MapEntry('applicantName', form.applicantName));
    fd.fields.add(MapEntry('applicantEmail', form.applicantEmail));
    fd.fields.add(MapEntry('applicantPhone', form.applicantPhone));
    if (form.coverLetter != null) fd.fields.add(MapEntry('coverLetter', form.coverLetter!));
    if (form.currentCtc != null) fd.fields.add(MapEntry('currentCtc', form.currentCtc!));
    if (form.expectedCtc != null) fd.fields.add(MapEntry('expectedCtc', form.expectedCtc!));
    if (form.noticePeriod != null) fd.fields.add(MapEntry('noticePeriod', form.noticePeriod!));
    if (form.linkedinUrl != null) fd.fields.add(MapEntry('linkedinUrl', form.linkedinUrl!));
    if (form.portfolioUrl != null) fd.fields.add(MapEntry('portfolioUrl', form.portfolioUrl!));
    if (form.resumeBytes != null && form.resumeFilename != null) {
      fd.files.add(MapEntry(
        'resume',
        MultipartFile.fromBytes(
          form.resumeBytes!,
          filename: form.resumeFilename,
          contentType: MediaType.parse('application/octet-stream'),
        ),
      ),);
    }
    final r = await _dio.post(_appsV1, data: fd);
    final env = _env<JobApplicationResponse>(r,
      (Object? j) => JobApplicationResponse.fromJson(Map<String, dynamic>.from(j as Map)),);
    return _unwrap(env);
  }

  Future<PaginatedResponse<JobApplicationResponse>> myApplications({int page = 1, int size = 20}) async {
    final q = _q({'page': page, 'size': size});
    final r = await _dio.get('$_appsV1/my$q');
    final env = _env<PaginatedResponse<JobApplicationResponse>>(
      r,
      (Object? j) => parsePaginated<JobApplicationResponse>(
        j, (Object? e) => JobApplicationResponse.fromJson(Map<String, dynamic>.from(e as Map)),
        fallbackPage: page, fallbackSize: size,
      ),
    );
    return _unwrap(env);
  }
}
