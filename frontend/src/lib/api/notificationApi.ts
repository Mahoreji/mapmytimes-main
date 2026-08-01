import { http, unwrap } from "@/lib/api/client";
import type { APIResponse, PaginationParams } from "@/types/common";
import type {
  Notification,
  UnreadCountResponse,
  SendNotificationRequest,
  ContactFormRequest,
  NotificationStatsResponse,
} from "@/types/notification";

const BASE = "/api/v1/notification";
const BASE_PLURAL = "/api/v1/notifications";

export const notificationApi = {
  health: () =>
    http
      .get<any>("/api/v1/health")
      .then((r) => r.data)
      .catch(() => ({ status: "unknown" })),

  contactForm: (body: ContactFormRequest) =>
    http.post<APIResponse<{ message: string }>>(`${BASE}/contact-form`, body).then(unwrap),

  list: (params: PaginationParams = {}) =>
    http
      .get<APIResponse<{ content: Notification[] } | Notification[]>>(`${BASE}${q(params)}`)
      .then(unwrap),

  listPlural: (params: PaginationParams = {}) =>
    http
      .get<APIResponse<{ content: Notification[] } | Notification[]>>(`${BASE_PLURAL}${q(params)}`)
      .then(unwrap),

  unreadCount: () =>
    http
      .get<APIResponse<UnreadCountResponse> | UnreadCountResponse>(`${BASE}/unread-count`)
      .then((r) => {
        const d = r.data as any;
        if ("data" in d) return d.data as UnreadCountResponse;
        return d as UnreadCountResponse;
      })
      .catch(() => ({ unread: 0 })),

  markRead: (id: string) =>
    http.patch<APIResponse<Notification>>(`${BASE}/${id}/read`).then(unwrap),

  markAllRead: () =>
    http.patch<APIResponse<{ message: string }>>(`${BASE}/read-all`).then(unwrap),

  delete: (id: string) =>
    http.delete<APIResponse<void>>(`${BASE}/${id}`).then(unwrap),

  deleteAll: () =>
    http.delete<APIResponse<void>>(`${BASE}`).then(unwrap),

  send: (body: SendNotificationRequest) =>
    http.post<APIResponse<Notification>>(`${BASE}/send`, body).then(unwrap),

  sendInstant: (body: SendNotificationRequest) =>
    http.post<APIResponse<Notification>>(`${BASE}/send/instant`, body).then(unwrap),

  stats: () =>
    http.get<APIResponse<NotificationStatsResponse>>(`${BASE}/stats`).then(unwrap),
};

function q(p: Record<string, any>) {
  const usp = new URLSearchParams();
  Object.entries(p).forEach(([k, v]) => {
    if (v === undefined || v === null || v === "") return;
    usp.append(k, String(v));
  });
  const s = usp.toString();
  return s ? `?${s}` : "";
}
