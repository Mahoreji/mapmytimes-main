import { http, unwrap } from "@/lib/api/client";
import type { APIResponse } from "@/types/common";
import type {
  RegisterRequest,
  RegisterResponse,
  VerifyEmailRequest,
  VerifyEmailResponse,
  LoginRequest,
  LoginWithOtpRequest,
  AuthResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
  ResendVerificationRequest,
  SendOtpRequest,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ForgotPasswordStep2Request,
  ResetPasswordRequest,
  ResetPasswordResponse,
  ChangePasswordRequest,
  DeleteAccountRequest,
  UserProfileResponse,
  UserProfileUpdateRequest,
  ImageUploadResponse,
  ImageDeleteResponse,
  AccountStatusResponse,
  SessionResponse,
  TwoFactorStatusResponse,
  TwoFactorEnableRequest,
  NotificationSettings,
  OAuth2LoginRequest,
} from "@/types/auth";

const AUTH = "/api/v1/auth";
const USER = "/api/v1/user";

export const authApi = {
  register: (body: RegisterRequest) =>
    http.post<APIResponse<RegisterResponse>>(`${AUTH}/register`, body).then(unwrap),

  verifyEmail: (body: VerifyEmailRequest) =>
    http.post<APIResponse<VerifyEmailResponse>>(`${AUTH}/verify-email`, body).then(unwrap),
  sendVerificationOtp: (body: ResendVerificationRequest) =>
    http
      .post<APIResponse<{ message: string }>>(`${AUTH}/send-verification-otp`, body)
      .then(unwrap),
  resendVerification: (body: ResendVerificationRequest) =>
    http
      .post<APIResponse<{ message: string }>>(`${AUTH}/resend-verification`, body)
      .then(unwrap),

  verifyOtp: (body: VerifyEmailRequest) =>
    http.post<APIResponse<VerifyEmailResponse>>(`${AUTH}/verify-email`, body).then(unwrap),
  resendOtp: (body: ResendVerificationRequest) =>
    http
      .post<APIResponse<{ message: string }>>(`${AUTH}/send-verification-otp`, body)
      .then(unwrap),

  checkEmail: (email: string) =>
    http
      .get<APIResponse<{ available: boolean; exists: boolean; email: string }>>(
        `${AUTH}/check-email?email=${encodeURIComponent(email)}`,
      )
      .then(unwrap),

  login: (body: LoginRequest) =>
    http.post<APIResponse<AuthResponse>>(`${AUTH}/login`, body).then(unwrap),
  loginWithOtp: (body: LoginWithOtpRequest) =>
    http.post<APIResponse<AuthResponse>>(`${AUTH}/login-otp`, body).then(unwrap),
  sendLoginOtp: (body: SendOtpRequest) =>
    http.post<APIResponse<{ message: string; sent?: boolean }>>(`${AUTH}/send-otp`, body).then(unwrap),

  refresh: (body: RefreshTokenRequest) =>
    http.post<RefreshTokenResponse>(`${AUTH}/refresh`, body).then((r) => r.data),

  logout: (sessionId?: string) =>
    http
      .post<APIResponse<string>>(
        `${AUTH}/logout`,
        sessionId ? { sessionId } : undefined,
      )
      .then((r) => unwrap(r) ?? "")
      .catch(() => ""),
  logoutAll: () =>
    http
      .post<APIResponse<string>>(`${AUTH}/logout-all`, undefined)
      .then((r) => unwrap(r) ?? "")
      .catch(() => ""),

  forgotPasswordStep1: (body: ForgotPasswordRequest) =>
    http
      .post<APIResponse<ForgotPasswordResponse>>(`${AUTH}/forgot-password/step1`, body)
      .then(unwrap),
  forgotPasswordStep2: (body: ForgotPasswordStep2Request) =>
    http
      .post<APIResponse<ForgotPasswordResponse>>(`${AUTH}/forgot-password/step2`, body)
      .then(unwrap),
  forgotPassword: (body: ForgotPasswordRequest) =>
    authApi.forgotPasswordStep1(body),

  resetPassword: (body: ResetPasswordRequest) =>
    http.post<APIResponse<ResetPasswordResponse>>(`${AUTH}/reset-password`, body).then(unwrap),

  changePassword: (body: ChangePasswordRequest) =>
    http
      .post<APIResponse<{ message: string }>>(`${AUTH}/change-password`, body)
      .then(unwrap),

  accountStatus: () =>
    http.get<APIResponse<AccountStatusResponse>>(`${AUTH}/account-status`).then(unwrap),

  deleteAccount: (body: DeleteAccountRequest) =>
    http
      .delete<APIResponse<{ message: string }>>(`${USER}/account`, { data: body })
      .then(unwrap),

  profile: () => http.get<APIResponse<UserProfileResponse>>(`${AUTH}/profile`).then(unwrap),

  updateProfile: (body: UserProfileUpdateRequest) =>
    http.put<APIResponse<UserProfileResponse>>(`${USER}/profile`, body).then(unwrap),

  preferences: () =>
    http.get<APIResponse<Record<string, any>>>(`${USER}/preferences`).then(unwrap),
  updatePreferences: (body: Record<string, any>) =>
    http.patch<APIResponse<Record<string, any>>>(`${USER}/preferences`, body).then(unwrap),

  notificationSettings: () =>
    http
      .get<APIResponse<NotificationSettings>>(`${USER}/notifications/settings`)
      .then(unwrap),
  updateNotificationSettings: (body: NotificationSettings) =>
    http
      .put<APIResponse<NotificationSettings>>(`${USER}/notifications/settings`, body)
      .then(unwrap),

  uploadProfileImage: (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return http
      .post<APIResponse<ImageUploadResponse>>(`${USER}/avatar`, fd, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then(unwrap);
  },
  deleteProfileImage: () =>
    http.delete<APIResponse<ImageDeleteResponse>>(`${USER}/avatar`).then(unwrap),

  listSessions: () =>
    http.get<APIResponse<SessionResponse[]>>(`${AUTH}/sessions`).then(unwrap).catch(() => [] as any),
  terminateSession: (sessionId: string) =>
    http
      .delete<APIResponse<{ message: string }>>(`${AUTH}/sessions/${encodeURIComponent(sessionId)}`)
      .then(unwrap),

  twoFactorStatus: () =>
    http
      .get<APIResponse<TwoFactorStatusResponse>>(`${AUTH}/2fa/status`)
      .then(unwrap)
      .catch(() => ({ enabled: false } as any)),
  twoFactorSetup: () =>
    http
      .get<APIResponse<TwoFactorStatusResponse>>(`${AUTH}/2fa/setup`)
      .then(unwrap)
      .catch(() => ({ enabled: false } as any)),
  twoFactorEnable: (body: TwoFactorEnableRequest) =>
    http.post<APIResponse<TwoFactorStatusResponse>>(`${AUTH}/2fa/enable`, body).then(unwrap),
  twoFactorDisable: (code: string) =>
    http
      .post<APIResponse<TwoFactorStatusResponse>>(`${AUTH}/2fa/disable`, { code })
      .then(unwrap),
  twoFactorBackupCodes: () =>
    http
      .get<APIResponse<{ codes: string[]; remaining: number }>>(`${AUTH}/2fa/backup-codes`)
      .then(unwrap)
      .catch(() => ({ codes: [], remaining: 0 } as any)),
};

export const socialAuthUrls = (baseApi: string, redirectUri?: string) => {
  const base = baseApi.replace(/\/$/, "");
  const build = (provider: string) => {
    let u = `${base}${AUTH}/oauth2/login/${provider}`;
    if (redirectUri) {
      u += `?redirect_uri=${encodeURIComponent(redirectUri)}`;
    }
    return u;
  };
  return {
    google: build("google"),
    facebook: build("facebook"),
  };
};

export const oauth2 = {
  callback: (body: OAuth2LoginRequest) =>
    http.post<APIResponse<AuthResponse>>(`${AUTH}/oauth2/callback`, body).then(unwrap),
  status: () =>
    http.get<APIResponse<Record<string, boolean>>>(`${AUTH}/oauth2/status`).then(unwrap),
  link: (body: OAuth2LoginRequest) =>
    http.post<APIResponse<{ message: string }>>(`${AUTH}/oauth2/link`, body).then(unwrap),
  unlink: (provider: "google" | "facebook" | string) =>
    http
      .delete<APIResponse<{ message: string }>>(`${AUTH}/oauth2/unlink/${provider}`)
      .then(unwrap),
};
