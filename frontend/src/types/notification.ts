import type { ID } from "./common";

export type NotificationType =
  | "SYSTEM"
  | "NEWSLETTER"
  | "COMMENT"
  | "LIKE"
  | "MENTION"
  | "APPROVAL"
  | "PUBLISH"
  | "CONTACT_FORM";

export type NotificationStatus = "PENDING" | "SENT" | "FAILED" | "READ";
export type ChannelType = "EMAIL" | "SMS" | "WHATSAPP" | "PUSH" | "IN_APP";

export interface Notification {
  id: ID;
  userId?: string;
  recipient?: string;
  type: NotificationType;
  channel: ChannelType;
  title: string;
  message: string;
  payload?: Record<string, any>;
  status: NotificationStatus;
  read: boolean;
  scheduledAt?: string;
  sentAt?: string;
  failedAt?: string;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UnreadCountResponse {
  unread: number;
}

export interface SendNotificationRequest {
  userId?: string;
  recipient?: string;
  type?: NotificationType;
  channel?: ChannelType;
  title: string;
  message: string;
  payload?: Record<string, any>;
  scheduledAt?: string;
}

export interface ContactFormRequest {
  name: string;
  email: string;
  phone?: string;
  subject?: string;
  message: string;
  source?: string;
}

export interface NotificationStatsResponse {
  total: number;
  sent: number;
  pending: number;
  failed: number;
  byType?: Record<string, number>;
  byChannel?: Record<string, number>;
}
