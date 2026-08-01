export type JobType = "FULL_TIME" | "PART_TIME" | "INTERNSHIP" | "CONTRACT" | "FREELANCE";
export type ExperienceLevel = "FRESHER" | "JUNIOR" | "MID" | "SENIOR" | "LEAD";
export type ApplicationStatus =
  | "APPLIED"
  | "UNDER_REVIEW"
  | "SHORTLISTED"
  | "INTERVIEW"
  | "INTERVIEW_SCHEDULED"
  | "REJECTED"
  | "SELECTED"
  | "WITHDRAWN";

export interface JobPostingSummaryResponse {
  id: string;
  title: string;
  department: string;
  location: string;
  jobType: JobType;
  experienceLevel: ExperienceLevel;
  salaryMin?: number | null;
  salaryMax?: number | null;
  salaryCurrency?: string | null;
  isActive: boolean;
  applicationDeadline?: string | null;
  createdAt?: string | null;
}

export interface JobPostingResponse {
  id: string;
  title: string;
  department: string;
  location: string;
  jobType: JobType;
  experienceLevel: ExperienceLevel;
  description: string;
  requirements?: string | null;
  responsibilities?: string | null;
  salaryMin?: number | null;
  salaryMax?: number | null;
  salaryCurrency?: string | null;
  isActive: boolean;
  applicationDeadline?: string | null;
  postedBy?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  totalApplications?: number | null;
}

export interface JobApplicationSummaryResponse {
  id: string;
  jobId: string;
  jobTitle: string;
  applicantName: string;
  applicantEmail: string;
  status: ApplicationStatus;
  appliedAt?: string | null;
  updatedAt?: string | null;
}

export interface JobApplicationResponse extends JobApplicationSummaryResponse {
  applicantPhone: string;
  resumeUrl?: string | null;
  resumeS3Key?: string | null;
  resumeOriginalFileName?: string | null;
  coverLetter?: string | null;
  currentCtc?: string | null;
  expectedCtc?: string | null;
  noticePeriod?: string | null;
  yearsOfExperience?: number | null;
  adminNotes?: string | null;
  rejectionReason?: string | null;
  interviewScheduledAt?: string | null;
  applicantId: string;
}

export interface ApplyFormData {
  jobId: string;
  applicantName: string;
  applicantEmail: string;
  applicantPhone: string;
  coverLetter?: string;
  currentCtc?: string;
  expectedCtc?: string;
  noticePeriod?: string;
  yearsOfExperience?: number;
  resume: File;
}

export const JOB_TYPE_LABELS: Record<JobType, string> = {
  FULL_TIME: "Full Time",
  PART_TIME: "Part Time",
  INTERNSHIP: "Internship",
  CONTRACT: "Contract",
  FREELANCE: "Freelance",
};

export const EXPERIENCE_LABELS: Record<ExperienceLevel, string> = {
  FRESHER: "Fresher",
  JUNIOR: "Junior",
  MID: "Mid",
  SENIOR: "Senior",
  LEAD: "Lead",
};

export const APPLICATION_STATUS_META: Record<
  ApplicationStatus,
  { label: string; tone: "default" | "news" | "ink" | "green" | "amber" | "red" | "blue" }
> = {
  APPLIED:             { label: "Applied",             tone: "default" },
  UNDER_REVIEW:        { label: "Under Review",        tone: "blue" },
  SHORTLISTED:         { label: "Shortlisted",         tone: "amber" },
  INTERVIEW:           { label: "Interview",           tone: "amber" },
  INTERVIEW_SCHEDULED: { label: "Interview Scheduled", tone: "green" },
  REJECTED:            { label: "Rejected",            tone: "red" },
  SELECTED:            { label: "Selected",            tone: "green" },
  WITHDRAWN:           { label: "Withdrawn",           tone: "ink" },
};
