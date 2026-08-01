import { http, unwrap } from "@/lib/api/client";
import type { APIResponse, PaginatedResponse, PaginationParams } from "@/types/common";
import type {
  ApplyFormData,
  JobApplicationResponse,
  JobApplicationSummaryResponse,
  JobPostingResponse,
  JobPostingSummaryResponse,
} from "@/types/careers";

const JOBS = "/api/v1/jobs";
const APPLICATIONS = "/api/v1/applications";

function toQuery(params: Record<string, unknown>) {
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === "") return;
    if (Array.isArray(v)) v.forEach((x) => usp.append(k, String(x)));
    else usp.append(k, String(v));
  });
  const q = usp.toString();
  return q ? `?${q}` : "";
}

export const careersApi = {
  jobs: {
    list: (
      params: PaginationParams & {
        sortBy?: string;
        sortDir?: string;
        department?: string;
        jobType?: string;
        experienceLevel?: string;
      } = {},
    ) =>
      http
        .get<APIResponse<PaginatedResponse<JobPostingSummaryResponse>>>(
          `${JOBS}${toQuery(params)}`,
        )
        .then(unwrap),

    search: (
      query: string,
      params: PaginationParams = {},
    ) =>
      http
        .get<APIResponse<PaginatedResponse<JobPostingSummaryResponse>>>(
          `${JOBS}/search${toQuery({ query, ...params })}`,
        )
        .then(unwrap),

    get: (id: string) =>
      http.get<APIResponse<JobPostingResponse>>(`${JOBS}/${id}`).then(unwrap),

    departments: () =>
      http.get<APIResponse<string[]>>(`${JOBS}/departments`).then(unwrap),
  },

  applications: {
    my: (params: PaginationParams = {}) =>
      http
        .get<APIResponse<PaginatedResponse<JobApplicationSummaryResponse>>>(
          `${APPLICATIONS}/my${toQuery({ page: params.page, size: params.size })}`,
        )
        .then(unwrap),

    get: (id: string) =>
      http.get<APIResponse<JobApplicationResponse>>(`${APPLICATIONS}/${id}`).then(unwrap),

    withdraw: (id: string) =>
      http.patch<APIResponse<void>>(`${APPLICATIONS}/${id}/withdraw`).then(unwrap),

    submit: (form: ApplyFormData) => {
      const fd = new FormData();
      fd.append("jobId", form.jobId);
      fd.append("applicantName", form.applicantName);
      fd.append("applicantEmail", form.applicantEmail);
      fd.append("applicantPhone", form.applicantPhone);
      if (form.coverLetter) fd.append("coverLetter", form.coverLetter);
      if (form.currentCtc) fd.append("currentCtc", form.currentCtc);
      if (form.expectedCtc) fd.append("expectedCtc", form.expectedCtc);
      if (form.noticePeriod) fd.append("noticePeriod", form.noticePeriod);
      if (form.yearsOfExperience !== undefined && form.yearsOfExperience !== null)
        fd.append("yearsOfExperience", String(form.yearsOfExperience));
      fd.append("resume", form.resume);

      return http
        .post<APIResponse<JobApplicationResponse>>(`${APPLICATIONS}`, fd, {
          headers: { "Content-Type": "multipart/form-data" },
          timeout: 120_000,
        })
        .then(unwrap);
    },
  },
};
