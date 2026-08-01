import type { ID } from "./common";

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  phone?: string;
  agreeToTerms: boolean;
}

export interface RegisterResponse {
  userId?: string;
  email?: string;
  message: string;
  otpSent?: boolean;
  requiresVerification?: boolean;
}

export interface VerifyEmailRequest {
  email: string;
  otp: string;
}

export interface VerifyEmailResponse {
  message: string;
  verified: boolean;
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string;
  user?: UserResponse;
}

export type VerifyOtpRequest = VerifyEmailRequest;
export type VerifyOtpResponse = VerifyEmailResponse;

export interface ResendVerificationRequest {
  email: string;
}

export interface SendOtpRequest {
  email: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginWithOtpRequest {
  email: string;
  otp: string;
}

export interface UserResponse {
  id?: ID;
  userId?: ID;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  phoneNumber?: string;
  profileImageUrl?: string;
  avatarUrl?: string;
  isVerified?: boolean;
  emailVerified?: boolean;
  roles?: string[];
  role?: string;
  twoFactorEnabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
  preferences?: Record<string, any>;
}

export interface AuthResponse {
  isAuthenticated: boolean;
  email?: string;
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
  tokenType?: string;
  sessionId?: string;
  deviceId?: string;
  requiresTwoFactor?: boolean;
  twoFactorToken?: string;
  user?: UserResponse;
  message?: string;
}

export type LoginResponse = AuthResponse;

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  message?: string;
}

export interface ForgotPasswordStep1Request {
  email: string;
}
export type ForgotPasswordRequest = ForgotPasswordStep1Request;

export interface ForgotPasswordStep2Request {
  email?: string;
  otp?: string;
  token?: string;
  newPassword?: string;
}

export interface ForgotPasswordResponse {
  message: string;
  otpSent?: boolean;
  token?: string;
  verified?: boolean;
}

export interface ResetPasswordRequest {
  token?: string;
  email?: string;
  otp?: string;
  newPassword: string;
}

export interface ResetPasswordResponse {
  message: string;
  reset: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface DeleteAccountRequest {
  password?: string;
  reason?: string;
}

export interface AccountStatusResponse {
  userId: ID;
  email: string;
  isVerified: boolean;
  twoFactorEnabled: boolean;
  status: "ACTIVE" | "DISABLED" | "PENDING_VERIFICATION";
}

export interface DeviceResponse {
  id: ID;
  deviceId?: string;
  name?: string;
  os?: string;
  browser?: string;
  ipAddress?: string;
  location?: string;
  lastActiveAt?: string;
  isCurrent?: boolean;
  createdAt?: string;
}

export interface SessionResponse {
  id: ID;
  sessionId?: string;
  userId?: ID;
  deviceId?: string;
  device?: DeviceResponse;
  ipAddress?: string;
  location?: string;
  startedAt?: string;
  lastActiveAt?: string;
  expiresAt?: string;
  isCurrent?: boolean;
}

export interface TwoFactorStatusResponse {
  enabled: boolean;
  backupCodesRemaining?: number;
  qrCodeUrl?: string;
  secret?: string;
  message?: string;
}

export interface TwoFactorEnableRequest {
  code: string;
  secret?: string;
}

export interface NotificationSettings {
  emailEnabled?: boolean;
  pushEnabled?: boolean;
  smsEnabled?: boolean;
  newsAlerts?: boolean;
  replyAlerts?: boolean;
  marketing?: boolean;
  [k: string]: any;
}

export interface UserProfileResponse extends UserResponse {
  userId: ID;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
  profileImageUrl?: string;
  isVerified: boolean;
  roles?: string[];
  createdAt?: string;
  updatedAt?: string;
  preferences?: Record<string, any>;
  notificationSettings?: NotificationSettings;
}

export interface UserProfileUpdateRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  phoneNumber?: string;
  bio?: string;
  location?: string;
  website?: string;
  socialLinks?: Record<string, string>;
  preferences?: Record<string, any>;
}

export type UpdateUserProfileRequest = UserProfileUpdateRequest;

export interface ImageUploadResponse {
  imageUrl: string;
  avatarUrl?: string;
  profileImageUrl?: string;
  message: string;
}

export interface ImageDeleteResponse {
  message: string;
  deleted: boolean;
}

export interface UserListResponse {
  userId: ID;
  firstName: string;
  lastName: string;
  email: string;
  isVerified: boolean;
  profileImageUrl?: string;
  createdAt?: string;
}

export interface OAuth2LoginRequest {
  email: string;
  firstName?: string;
  lastName?: string;
  avatarUrl?: string;
  profileImageUrl?: string;
  provider: "google" | "facebook" | string;
  providerId: string;
  ipAddress?: string;
  userAgent?: string;
}

export interface OAuth2RedirectQuery {
  email?: string;
  firstName?: string;
  lastName?: string;
  picture?: string;
  provider?: string;
  providerId?: string;
  success?: string;
  error?: string;
  error_description?: string;
}

