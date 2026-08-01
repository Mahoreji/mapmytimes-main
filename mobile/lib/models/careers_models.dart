// =============================================================================
// Careers service models — mirror of frontend/src/types/careers.ts
// =============================================================================

import 'blog_models.dart';

class JobPostingSummaryResponse {
  final ID id;
  final String title;
  final String? department;
  final String? jobType;
  final String? experienceLevel;
  final String? location;
  final bool? remote;
  final String? publishedAt;
  final DateTime? createdAt;

  const JobPostingSummaryResponse({
    required this.id,
    required this.title,
    this.department,
    this.jobType,
    this.experienceLevel,
    this.location,
    this.remote,
    this.publishedAt,
    this.createdAt,
  });

  factory JobPostingSummaryResponse.fromJson(Map<String, dynamic> j) => JobPostingSummaryResponse(
    id: (j['id'] ?? j['jobId'] ?? '') as ID,
    title: (j['title'] ?? '') as String,
    department: j['department'] as String?,
    jobType: (j['jobType'] ?? j['employmentType']) as String?,
    experienceLevel: (j['experienceLevel'] ?? j['level']) as String?,
    location: j['location'] as String?,
    remote: j['remote'] as bool? ?? (j['location']?.toString().toLowerCase().startsWith('remote') ?? false),
    publishedAt: (j['publishedAt'] ?? j['postedAt']) as String?,
    createdAt: j['createdAt'] == null ? null : DateTime.tryParse(j['createdAt'].toString()),
  );
}

class JobPostingResponse extends JobPostingSummaryResponse {
  final String? description;
  final String? responsibilities;
  final String? requirements;
  final String? benefits;
  final List<String>? skills;
  final String? salaryRange;

  const JobPostingResponse({
    required super.id,
    required super.title,
    super.department,
    super.jobType,
    super.experienceLevel,
    super.location,
    super.remote,
    super.publishedAt,
    super.createdAt,
    this.description,
    this.responsibilities,
    this.requirements,
    this.benefits,
    this.skills,
    this.salaryRange,
  });

  factory JobPostingResponse.fromJson(Map<String, dynamic> j) => JobPostingResponse(
    id: (j['id'] ?? j['jobId'] ?? '') as ID,
    title: (j['title'] ?? '') as String,
    department: j['department'] as String?,
    jobType: (j['jobType'] ?? j['employmentType']) as String?,
    experienceLevel: (j['experienceLevel'] ?? j['level']) as String?,
    location: j['location'] as String?,
    remote: j['remote'] as bool?,
    publishedAt: (j['publishedAt'] ?? j['postedAt']) as String?,
    createdAt: j['createdAt'] == null ? null : DateTime.tryParse(j['createdAt'].toString()),
    description: (j['description'] ?? j['body'] ?? j['about']) as String?,
    responsibilities: j['responsibilities'] as String?,
    requirements: j['requirements'] as String?,
    benefits: j['benefits'] as String?,
    skills: (j['skills'] as List<dynamic>?)?.map((e) => e.toString()).toList(growable: false),
    salaryRange: (j['salaryRange'] ?? j['salary']) as String?,
  );
}

class ApplyFormData {
  final ID jobId;
  final String applicantName;
  final String applicantEmail;
  final String applicantPhone;
  final String? coverLetter;
  final String? currentCtc;
  final String? expectedCtc;
  final String? noticePeriod;
  final String? linkedinUrl;
  final String? portfolioUrl;
  final List<int>? resumeBytes;
  final String? resumeFilename;

  const ApplyFormData({
    required this.jobId,
    required this.applicantName,
    required this.applicantEmail,
    required this.applicantPhone,
    this.coverLetter,
    this.currentCtc,
    this.expectedCtc,
    this.noticePeriod,
    this.linkedinUrl,
    this.portfolioUrl,
    this.resumeBytes,
    this.resumeFilename,
  });
}

class JobApplicationResponse {
  final ID id;
  final ID jobId;
  final String? applicantName;
  final String? applicantEmail;
  final String? status;
  final String? createdAt;
  const JobApplicationResponse({
    required this.id,
    required this.jobId,
    this.applicantName,
    this.applicantEmail,
    this.status,
    this.createdAt,
  });
  factory JobApplicationResponse.fromJson(Map<String, dynamic> j) => JobApplicationResponse(
    id: (j['id'] ?? j['applicationId'] ?? '') as ID,
    jobId: (j['jobId'] ?? '') as ID,
    applicantName: j['applicantName'] as String?,
    applicantEmail: j['applicantEmail'] as String?,
    status: (j['status'] ?? 'SUBMITTED') as String?,
    createdAt: (j['createdAt'] ?? j['submittedAt']) as String?,
  );
}
